/* ==================================================================
 * DefaultSetupService.java - Jun 1, 2010 2:19:02 PM
 *
 * Copyright 2007-2010 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.setup.impl;

import static net.solarnetwork.node.setup.SetupSettings.KEY_NODE_ID;
import static net.solarnetwork.service.OptionalService.service;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.xml.xpath.XPathExpression;
import org.apache.commons.codec.binary.Base64InputStream;
import org.osgi.service.event.Event;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.codec.JavaBeanXmlSerializer;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.domain.NetworkIdentity;
import net.solarnetwork.node.backup.BackupManager;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.NodeAppConfiguration;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.PKIService;
import net.solarnetwork.node.service.SystemService;
import net.solarnetwork.node.service.support.XmlServiceSupport;
import net.solarnetwork.node.setup.InvalidVerificationCodeException;
import net.solarnetwork.node.setup.SetupException;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.node.setup.SetupSettings;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.util.DateUtils;

/**
 * Implementation of {@link SetupService}.
 *
 * <p>
 * The configurable properties of this class are:
 * </p>
 *
 * <dl class="class-properties">
 * <dt>backupManager</dt>
 * <dd>An optional {@link BackupManager} to trigger an immediate backup after
 * associating.</dd>
 *
 * <dt>settingDao</dt>
 * <dd>The {@link SettingDao} to use for querying/storing application state
 * information.</dd>
 *
 * <dt>hostName</dt>
 * <dd>The host name to use for the SolarNet remote service. Defaults to
 * {@link #DEFAULT_HOST_NAME}. This will be overridden by the application
 * setting value for the key
 * {@link SetupSettings#KEY_SOLARNETWORK_HOST_NAME}.</dd>
 *
 * <dt>hostPort</dt>
 * <dd>The host port to use for the SolarNet remote service. Defaults to
 * {@link #DEFAULT_HOST_PORT}. This will be overridden by the application
 * setting value for the key
 * {@link SetupSettings#KEY_SOLARNETWORK_HOST_PORT}.</dd>
 *
 * <dt>forceTLS</dt>
 * <dd>If {@literal true} then use TLS (SSL) even on a port other than
 * {@code 443} (the default TLS port). Defaults to {@literal false}.</dd>
 *
 * <dt>solarInUrlPrefix</dt>
 * <dd>The URL prefix for the SolarIn service. Defaults to
 * {@link DEFAULT_SOLARIN_URL_PREFIX}.</dd>
 * </dl>
 *
 * @author matt
 * @version 2.4
 */
public class DefaultSetupService extends XmlServiceSupport
		implements SetupService, IdentityService, InstructionHandler, PingTest {

	/** The default value for the {@code hostName} property. */
	public static final String DEFAULT_HOST_NAME = "in.solarnetwork.net";

	/** The default value for the {@code hostPort} property. */
	public static final Integer DEFAULT_HOST_PORT = 443;

	/** The default value for the {@code solarInUrlPrefix} property. */
	public static final String DEFAULT_SOLARIN_URL_PREFIX = "/solarin";

	/**
	 * The default value for the {@code solarUserUrlPrefix} property.
	 *
	 * @since 1.11
	 */
	public static final String DEFAULT_SOLARUSER_URL_PREFIX = "/solaruser";

	/**
	 * Instruction topic for sending a renewed certificate to a node.
	 *
	 * @since 1.5
	 */
	public static final String INSTRUCTION_TOPIC_RENEW_CERTIFICATE = "RenewCertificate";

	/**
	 * Instruction parameter for certificate data. Since instruction parameters
	 * are limited in length, there can be more than one parameter of the same
	 * key, with the full data being the concatenation of all parameter values.
	 *
	 * @since 1.5
	 */
	public static final String INSTRUCTION_PARAM_CERTIFICATE = "Certificate";

	/**
	 * Get the URL path for the app configuration endpoint.
	 *
	 * @since 1.8
	 */
	public static final String NETWORK_APP_CONFIGURATION_URL_PATH = "/api/v1/pub/config";

	/**
	 * The {@literal service} instruction parameter value for identity
	 * information configuration.
	 *
	 * @since 2.1
	 */
	public static final String IDENTITY_SERVICE_NAME = "/setup/identity";

	/** A pattern to extract a node ID from a certificate subject name. */
	public static final Pattern DEFAULT_SUBJECT_NAME_NODE_ID_PATTERN = Pattern
			.compile("UID\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

	/** The {@code nodeCertificateExpireWarningDays} property default value. */
	public static final int DEFAULT_CERT_EXPIRE_WARNING_DAYS = 14;

	private static final long APP_CONFIG_CACHE_MS = 24 * 60 * 60 * 1000L; // 1 day

	// The keys used in the verification code xml
	private static final String VERIFICATION_CODE_HOST_NAME = "host";
	private static final String VERIFICATION_CODE_HOST_PORT = "port";
	private static final String VERIFICATION_CODE_CONFIRMATION_KEY = "confirmationKey";
	private static final String VERIFICATION_CODE_IDENTITY_KEY = "identityKey";
	private static final String VERIFICATION_CODE_TERMS_OF_SERVICE = "termsOfService";
	private static final String VERIFICATION_CODE_EXPIRATION_KEY = "expiration";
	private static final String VERIFICATION_CODE_SECURITY_PHRASE = "securityPhrase";
	private static final String VERIFICATION_CODE_NODE_ID_KEY = "networkId";
	private static final String VERIFICATION_CODE_NODE_CERT = "networkCertificate";
	private static final String VERIFICATION_CODE_NODE_CERT_STATUS = "networkCertificateStatus";
	private static final String VERIFICATION_CODE_NODE_CERT_DN_KEY = "networkCertificateSubjectDN";
	private static final String VERIFICATION_CODE_USER_NAME_KEY = "username";
	private static final String VERIFICATION_CODE_FORCE_TLS = "forceTLS";
	private static final String VERIFICATION_URL_SOLARUSER = "solarUserServiceURL";
	private static final String VERIFICATION_URL_SOLARQUERY = "solarQueryServiceURL";
	private static final String VERIFICATION_URL_SOLARIN_MQTT = "solarInMqttServiceURL";

	private static final String SOLAR_NET_IDENTITY_PATH = "/identity.do";
	private static final String SOLAR_NET_REG_PATH = "/associate.xml";
	private static final String SOLAR_IN_RENEW_CERT_URL = "/api/v1/sec/cert/renew";

	private static enum SolarApplication {

		SolarIn("solarin"),
		SolarUser("solaruser");

		private final String key;

		private SolarApplication(String key) {
			this.key = key;
		}
	}

	private final SetupIdentityDao setupIdentityDao;
	private OptionalService<BackupManager> backupManager;
	private OptionalService<SystemService> systemService;
	private PKIService pkiService;
	private String solarInUrlPrefix = DEFAULT_SOLARIN_URL_PREFIX;
	private String solarUserUrlPrefix = DEFAULT_SOLARUSER_URL_PREFIX;
	private int nodeCertificateExpireWarningDays = DEFAULT_CERT_EXPIRE_WARNING_DAYS;

	private NodeAppConfiguration appConfiguration = new NodeAppConfiguration();

	/**
	 * Default constructor.
	 *
	 * @param setupIdentityDao
	 *        the identity DAO to use
	 */
	public DefaultSetupService(SetupIdentityDao setupIdentityDao) {
		super();
		this.setupIdentityDao = setupIdentityDao;
		setConnectionTimeout(60000);
		setDisplayName("Setup Service");
	}

	private Map<String, XPathExpression> getNodeAssociationPropertyMapping() {
		Map<String, String> xpathMap = new HashMap<String, String>();
		xpathMap.put(VERIFICATION_CODE_NODE_ID_KEY, "/*/@networkId");
		xpathMap.put(VERIFICATION_CODE_NODE_CERT_DN_KEY, "/*/@networkCertificateSubjectDN");
		xpathMap.put(VERIFICATION_CODE_NODE_CERT_STATUS, "/*/@networkCertificateStatus");
		xpathMap.put(VERIFICATION_CODE_NODE_CERT, "/*/@networkCertificate");
		xpathMap.put(VERIFICATION_CODE_USER_NAME_KEY, "/*/@username");
		xpathMap.put(VERIFICATION_CODE_CONFIRMATION_KEY, "/*/@confirmationKey");
		return getXPathExpressionMap(xpathMap);
	}

	private Map<String, XPathExpression> getIdentityPropertyMapping() {
		Map<String, String> identityXpathMap = new HashMap<String, String>();
		identityXpathMap.put(VERIFICATION_CODE_HOST_NAME, "/*/@host");
		identityXpathMap.put(VERIFICATION_CODE_HOST_PORT, "/*/@port");
		identityXpathMap.put(VERIFICATION_CODE_FORCE_TLS, "/*/@forceTLS");
		identityXpathMap.put(VERIFICATION_CODE_IDENTITY_KEY, "/*/@identityKey");
		identityXpathMap.put(VERIFICATION_CODE_TERMS_OF_SERVICE, "/*/@termsOfService");
		identityXpathMap.put(VERIFICATION_CODE_SECURITY_PHRASE, "/*/@securityPhrase");
		identityXpathMap.put(VERIFICATION_URL_SOLARUSER,
				"/*/networkServiceURLs/entry[@key='solaruser']/value/@value");
		identityXpathMap.put(VERIFICATION_URL_SOLARQUERY,
				"/*/networkServiceURLs/entry[@key='solarquery']/value/@value");
		identityXpathMap.put(VERIFICATION_URL_SOLARIN_MQTT,
				"/*/networkServiceURLs/entry[@key='solarin-mqtt']/value/@value");
		return getXPathExpressionMap(identityXpathMap);
	}

	private boolean isForceTLS() {
		return setupIdentityDao.getSetupIdentityInfo().isSolarNetForceTls();
	}

	private int getPort() {
		Integer port = getSolarNetHostPort();
		if ( port == null ) {
			return 443;
		}
		return port.intValue();
	}

	@Override
	public Long getNodeId() {
		return setupIdentityDao.getSetupIdentityInfo().getNodeId();
	}

	@Override
	public Principal getNodePrincipal() {
		if ( pkiService == null ) {
			return null;
		}
		X509Certificate nodeCert = pkiService.getNodeCertificate();
		if ( nodeCert == null ) {
			log.debug("No node certificate available, cannot get node principal");
			return null;
		}
		return nodeCert.getSubjectX500Principal();
	}

	@Override
	public String getSolarNetHostName() {
		return setupIdentityDao.getSetupIdentityInfo().getSolarNetHostName();
	}

	@Override
	public Integer getSolarNetHostPort() {
		Integer port = setupIdentityDao.getSetupIdentityInfo().getSolarNetHostPort();
		return (port == null ? 443 : port);
	}

	@Override
	public String getSolarNetSolarInUrlPrefix() {
		return solarInUrlPrefix;
	}

	@Override
	public String getSolarInBaseUrl() {
		final int port = getPort();
		final String host = getSolarNetHostName();
		if ( host == null ) {
			throw new SetupException(
					"SolarNet host not configured. Perhaps this node is not yet set up?");
		}
		return "http" + (port == 443 || isForceTLS() ? "s" : "") + "://" + host
				+ (port == 443 || port == 80 ? "" : (":" + port)) + solarInUrlPrefix;
	}

	@Override
	public String getSolarInMqttUrl() {
		String result = null;
		NodeAppConfiguration config = getAppConfiguration();
		if ( config != null ) {
			result = config.getNetworkServiceUrls()
					.get(NetworkIdentity.SOLARIN_MQTT_NETWORK_SERVICE_KEY);
		}
		return result;
	}

	@Override
	public NetworkAssociationDetails decodeVerificationCode(String verificationCode)
			throws InvalidVerificationCodeException {
		log.debug("Decoding verification code {}", verificationCode);

		NetworkAssociationDetails details = new NetworkAssociationDetails();

		try {
			JavaBeanXmlSerializer helper = new JavaBeanXmlSerializer();
			InputStream in = new GZIPInputStream(
					new Base64InputStream(new ByteArrayInputStream(verificationCode.getBytes())));
			Map<String, Object> result = helper.parseXml(in);

			// Get the host server
			String hostName = (String) result.get(VERIFICATION_CODE_HOST_NAME);
			if ( hostName == null ) {
				// Use the default
				log.debug("Property {} not found in verfication code", VERIFICATION_CODE_HOST_NAME);
				throw new InvalidVerificationCodeException("Missing host");
			}
			details.setHost(hostName);

			// Get the host port
			String hostPort = (String) result.get(VERIFICATION_CODE_HOST_PORT);
			if ( hostPort == null ) {
				log.debug("Property {} not found in verfication code", VERIFICATION_CODE_HOST_PORT);
				throw new InvalidVerificationCodeException("Missing port");
			}
			try {
				details.setPort(Integer.valueOf(hostPort));
			} catch ( NumberFormatException e ) {
				throw new InvalidVerificationCodeException("Invalid host port value: " + hostPort, e);
			}

			// Get the confirmation Key
			String confirmationKey = (String) result.get(VERIFICATION_CODE_CONFIRMATION_KEY);
			if ( confirmationKey == null ) {
				throw new InvalidVerificationCodeException("Missing confirmation code");
			}
			details.setConfirmationKey(confirmationKey);

			// Get the identity key
			String identityKey = (String) result.get(VERIFICATION_CODE_IDENTITY_KEY);
			if ( identityKey == null ) {
				throw new InvalidVerificationCodeException("Missing identity key");
			}
			details.setIdentityKey(identityKey);

			// Get the user name
			String userName = (String) result.get(VERIFICATION_CODE_USER_NAME_KEY);
			if ( userName == null ) {
				throw new InvalidVerificationCodeException("Missing username");
			}
			details.setUsername(userName);

			// Get the expiration
			String expiration = (String) result.get(VERIFICATION_CODE_EXPIRATION_KEY);
			if ( expiration == null ) {
				throw new InvalidVerificationCodeException(VERIFICATION_CODE_EXPIRATION_KEY
						+ " not found in verification code: " + verificationCode);
			}
			try {
				details.setExpiration(DateTimeFormatter.ISO_INSTANT.parse(expiration, Instant::from));
			} catch ( DateTimeParseException e ) {
				throw new InvalidVerificationCodeException("Invalid expiration date value", e);
			}

			// Get the TLS setting
			String forceSSL = (String) result.get(VERIFICATION_CODE_FORCE_TLS);
			details.setForceTLS(forceSSL == null ? false : Boolean.valueOf(forceSSL));

			return details;
		} catch ( InvalidVerificationCodeException e ) {
			throw e;
		} catch ( Exception e ) {
			// Runtime/IO errors can come from webFormGetForBean
			throw new InvalidVerificationCodeException(
					"Error while trying to decode verfication code: " + verificationCode, e);
		}
	}

	@Override
	public NetworkAssociation retrieveNetworkAssociation(NetworkAssociationDetails details) {
		NetworkAssociationDetails association = new NetworkAssociationDetails();
		NetworkAssociationRequest req = new NetworkAssociationRequest();
		req.setUsername(details.getUsername());
		req.setKey(details.getConfirmationKey());
		webFormGetForBean(PropertyAccessorFactory.forBeanPropertyAccess(req), association,
				getAbsoluteUrl(details, SolarApplication.SolarIn, SOLAR_NET_IDENTITY_PATH), null,
				getIdentityPropertyMapping());
		return association;
	}

	private String getAbsoluteUrl(NetworkAssociationDetails details, SolarApplication app, String path) {
		StringBuilder buf = new StringBuilder();
		if ( app != SolarApplication.SolarIn ) {
			Map<String, String> urls = (details != null && details.getNetworkServiceURLs() != null
					? details.getNetworkServiceURLs()
					: Collections.emptyMap());
			String base = urls.get(app.key);
			if ( base != null ) {
				buf.append(base);
			}
		}
		if ( buf.length() < 1 ) {
			buf.append("http").append(details.getPort() == 443 || details.isForceTLS() ? "s" : "")
					.append("://");
			buf.append(details.getHost()).append(":").append(details.getPort());
			switch (app) {
				case SolarUser:
					buf.append(solarUserUrlPrefix);
					break;

				default:
					buf.append(solarInUrlPrefix);
					break;
			}
		}
		buf.append(path);
		return buf.toString();
	}

	@Override
	public NetworkCertificate acceptNetworkAssociation(final NetworkAssociationDetails details)
			throws SetupException {
		log.debug("Associating with SolarNet service {}", details);

		try {
			// Get confirmation code from the server
			NetworkAssociationDetails req = new NetworkAssociationDetails(details.getUsername(),
					details.getConfirmationKey(), details.getKeystorePassword());
			final NetworkCertificate result = new NetworkAssociationDetails();
			webFormPostForBean(PropertyAccessorFactory.forBeanPropertyAccess(req), result,
					getAbsoluteUrl(details, SolarApplication.SolarUser, SOLAR_NET_REG_PATH), null,
					getNodeAssociationPropertyMapping());

			SetupIdentityInfo oldInfo = setupIdentityDao.getSetupIdentityInfo();
			SetupIdentityInfo info = new SetupIdentityInfo(result.getNetworkId(),
					result.getConfirmationKey(), details.getHost(), details.getPort(),
					details.isForceTLS(),
					details.getKeystorePassword() != null ? details.getKeystorePassword()
							: oldInfo.getKeyStorePassword());
			setupIdentityDao.saveSetupIdentityInfo(info);

			if ( result.getNetworkCertificateStatus() == null ) {
				// create the node's CSR based on the given subjectDN
				log.debug("Creating node CSR for subject {}", result.getNetworkCertificateSubjectDN());
				pkiService.generateNodeSelfSignedCertificate(result.getNetworkCertificateSubjectDN());
			} else if ( details.getKeystorePassword() != null ) {
				log.debug("Saving node certificate for subject {}",
						result.getNetworkCertificateSubjectDN());
				pkiService.savePKCS12Keystore(result.getNetworkCertificate(),
						details.getKeystorePassword());
			}

			makeBackup();

			// post NETWORK_ASSOCIATION_ACCEPTED event
			Map<String, Object> props = new HashMap<String, Object>(2);
			if ( result.getNetworkId() != null ) {
				props.put(KEY_NODE_ID, result.getNetworkId());
			}
			postEvent(new Event(SetupService.TOPIC_NETWORK_ASSOCIATION_ACCEPTED, props));

			return result;
		} catch ( Exception e ) {
			log.error("Error while confirming server details: {}", details, e);
			// Runtime errors can come from webFormGetForBean
			throw new SetupException("Error while confirming server details: " + details, e);
		}
	}

	private void makeBackup() {
		BackupManager mgr = (backupManager == null ? null : backupManager.service());
		if ( mgr == null ) {
			return;
		}
		log.info("Requesting background backup.");
		mgr.createAsynchronousBackup();
	}

	@Override
	public boolean handlesTopic(String topic) {
		return (INSTRUCTION_TOPIC_RENEW_CERTIFICATE.equalsIgnoreCase(topic)
				|| TOPIC_SYSTEM_CONFIGURE.equalsIgnoreCase(topic));
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		final String topic = (instruction != null ? instruction.getTopic() : null);
		if ( INSTRUCTION_TOPIC_RENEW_CERTIFICATE.equalsIgnoreCase(topic) ) {
			return processRenewCertificateInstruction(instruction);
		} else if ( TOPIC_SYSTEM_CONFIGURE.equalsIgnoreCase(topic) ) {
			return processSystemConfigureInstruction(instruction);
		}
		return null;
	}

	private InstructionStatus processRenewCertificateInstruction(Instruction instruction) {
		PKIService pki = pkiService;
		if ( pki == null ) {
			return null;
		}
		String[] certParts = instruction.getAllParameterValues(INSTRUCTION_PARAM_CERTIFICATE);
		if ( certParts == null ) {
			log.warn("Certificate not provided with renew instruction");
			return InstructionUtils.createStatus(instruction, InstructionState.Declined);
		}
		String cert = org.springframework.util.StringUtils.arrayToDelimitedString(certParts, "");
		log.debug("Got certificate renewal instruction with certificate data: {}", cert);
		try {
			pki.saveNodeSignedCertificate(cert);
			if ( log.isInfoEnabled() ) {
				X509Certificate nodeCert = pki.getNodeCertificate();
				log.info("Installed node certificate {}, valid to {}", nodeCert.getSerialNumber(),
						nodeCert.getNotAfter());
			}
			final SystemService systemService = service(getSystemService());
			if ( systemService != null ) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(1000);
						} catch ( InterruptedException e ) {
							// ignore this
						}
						systemService.exit(true); // this will exit "soon"
					}

				}).start();
			}
			return InstructionUtils.createStatus(instruction, InstructionState.Completed);
		} catch ( CertificateException e ) {
			log.error("Failed to install renewed certificate", e);
		}
		return null;
	}

	private InstructionStatus processSystemConfigureInstruction(Instruction instruction) {
		final String service = instruction.getParameterValue(PARAM_SERVICE);
		if ( !IDENTITY_SERVICE_NAME.equals(service) ) {
			return null;
		}
		// get the info, sans password
		Long nodeId = getNodeId();
		String solarInBaseUrl = null;
		try {
			solarInBaseUrl = getSolarInBaseUrl();
		} catch ( SetupException e ) {
			// ignore
		}

		X509Certificate cert = null;
		;
		if ( pkiService != null ) {
			cert = pkiService.getNodeCertificate();
		}

		SystemConfigureIdentityInfo identInfo = new SystemConfigureIdentityInfo(nodeId, solarInBaseUrl,
				cert);
		return InstructionUtils.createStatus(instruction, InstructionState.Completed,
				Collections.singletonMap(PARAM_SERVICE_RESULT, identInfo));
	}

	@Override
	public void renewNetworkCertificate(String password) throws SetupException {
		final String keystore = pkiService.generatePKCS12KeystoreString(password);
		final String url = getSolarInBaseUrl() + SOLAR_IN_RENEW_CERT_URL;
		Map<String, String> data = new HashMap<String, String>(2);
		data.put("keystore", keystore);
		data.put("password", password);
		try {
			String result = postXWWWFormURLEncodedDataForString(url, data);
			if ( result == null || !result.matches(".*(?i)\"success\"\\s*:\\s*true.*") ) {
				String message = "Unknown error.";
				if ( result != null ) {
					Pattern pat = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"",
							Pattern.CASE_INSENSITIVE);
					Matcher m = pat.matcher(message);
					if ( m.find() ) {
						message = m.group(1);
					}
				}
				throw new SetupException(message);
			}
		} catch ( IOException e ) {
			throw new SetupException("Error communicating with SolarNet: " + e.getMessage());
		}
	}

	private Map<String, Object> fetchNetworkConfiguration(String appBaseUrl) throws IOException {
		if ( appBaseUrl == null ) {
			return Collections.emptyMap();
		}
		String url = appBaseUrl;
		if ( url.endsWith("/") ) {
			url = url.substring(0, url.length() - 1);
		}
		url += NETWORK_APP_CONFIGURATION_URL_PATH;
		URLConnection conn = getURLConnection(url, "GET", "application/json");
		String json = FileCopyUtils.copyToString(getUnicodeReaderFromURLConnection(conn));
		return JsonUtils.getStringMap(json);
	}

	private Map<String, String> fetchNetworkServiceUrls(String appBaseUrl) throws IOException {
		Map<String, Object> result = fetchNetworkConfiguration(appBaseUrl);
		Object data = result.get("data");
		if ( data instanceof Map<?, ?> ) {
			@SuppressWarnings("unchecked")
			Map<String, Object> appConfig = (Map<String, Object>) data;
			Object serviceUrls = appConfig.get("serviceUrls");
			if ( serviceUrls instanceof Map<?, ?> ) {
				@SuppressWarnings("unchecked")
				Map<String, String> urls = (Map<String, String>) serviceUrls;
				return urls;
			}
		}
		return Collections.emptyMap();
	}

	@Override
	public synchronized NodeAppConfiguration getAppConfiguration() {
		NodeAppConfiguration config = this.appConfiguration;
		if ( config.getCreated() + APP_CONFIG_CACHE_MS > System.currentTimeMillis() ) {
			return config;
		}
		// try to refresh from network
		try {
			NetworkAssociationDetails association = new NetworkAssociationDetails();
			webFormGetForBean(null, association, getSolarInBaseUrl() + SOLAR_NET_IDENTITY_PATH, null,
					getIdentityPropertyMapping());

			Map<String, String> networkServiceUrls = new LinkedHashMap<String, String>();
			if ( association.getSolarQueryServiceURL() != null ) {
				try {
					Map<String, String> urls = fetchNetworkServiceUrls(
							association.getSolarQueryServiceURL());
					networkServiceUrls.putAll(urls);
				} catch ( IOException e ) {
					log.warn("Network error fetching SolarQuery app configuration: " + e.getMessage());
				}
			}
			if ( association.getSolarUserServiceURL() != null ) {
				try {
					Map<String, String> urls = fetchNetworkServiceUrls(
							association.getSolarUserServiceURL());
					networkServiceUrls.putAll(urls);
				} catch ( IOException e ) {
					log.warn("Network error fetching SolarUser app configuration: " + e.getMessage());
				}
			}
			// add SolarIn URLs last, which should not be overridden
			networkServiceUrls.putAll(association.getNetworkServiceURLs());
			config = new NodeAppConfiguration(networkServiceUrls);
			this.appConfiguration = config;
		} catch ( Exception e ) {
			// don't re-throw from here
			log.warn("Error retrieving SolarIn identity info: " + e.getMessage());
		}
		return config;
	}

	@Override
	public String getPingTestId() {
		return "net.solarnetwork.node.setup.impl.DefaultSetupService";
	}

	@Override
	public String getPingTestName() {
		return getDisplayName();
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 15000;
	}

	@Override
	public Result performPingTest() throws Exception {
		final X509Certificate nodeCert = (pkiService != null ? pkiService.getNodeCertificate() : null);
		if ( nodeCert == null ) {
			return new PingTestResult(true, "Node certificate not available.");
		}
		final Instant now = Instant.now();
		final ZoneId zone = ZoneId.systemDefault();
		String certDisplayName = String.format("%s (0x%x)", nodeCert.getSubjectDN(),
				nodeCert.getSerialNumber());
		Instant expires = nodeCert.getNotAfter().toInstant();
		boolean ok = true;
		String msg = null;

		if ( expires.isBefore(now) ) {
			ok = false;
			msg = getMessageSource().getMessage("error.nodeCertExpired",
					new Object[] { certDisplayName,
							DateUtils.DISPLAY_DATE_LONG_TIME_SHORT.format(expires.atZone(zone)) },
					Locale.getDefault());
		} else {
			long daysToExpire = ChronoUnit.DAYS.between(now.atZone(zone), expires.atZone(zone));
			msg = getMessageSource().getMessage("msg.nodeCertExpiring",
					new Object[] { certDisplayName, daysToExpire,
							DateUtils.DISPLAY_DATE_LONG_TIME_SHORT.format(expires.atZone(zone)) },
					Locale.getDefault());
			int maxDays = getNodeCertificateExpireWarningDays();
			if ( maxDays > 0 && daysToExpire <= maxDays ) {
				ok = false;
			}
		}
		return new PingTestResult(ok, msg);
	}

	/**
	 * Set the URL path prefix to use for the SolarIn application.
	 *
	 * @param solarInUrlPrefix
	 *        the prefix to use; defaults to {@link #DEFAULT_SOLARIN_URL_PREFIX}
	 */
	public void setSolarInUrlPrefix(String solarInUrlPrefix) {
		this.solarInUrlPrefix = solarInUrlPrefix;
	}

	/**
	 * Set the URL path prefix to use for the SolarUser application.
	 *
	 * @param solarUserUrlPrefix
	 *        the prefix to use; defaults to
	 *        {@link #DEFAULT_SOLARUSER_URL_PREFIX}
	 * @since 1.11
	 */
	public void setSolarUserUrlPrefix(String solarUserUrlPrefix) {
		this.solarUserUrlPrefix = solarUserUrlPrefix;
	}

	/**
	 * Set the PKI service to use.
	 *
	 * @param pkiService
	 *        the service to use
	 */
	public void setPkiService(PKIService pkiService) {
		this.pkiService = pkiService;
	}

	/**
	 * Set the backup manager service to use.
	 *
	 * @param backupManager
	 *        the service to use
	 */
	public void setBackupManager(OptionalService<BackupManager> backupManager) {
		this.backupManager = backupManager;
	}

	/**
	 * Get the number of days before treating an expiring node certificate as a
	 * {@link PingTest} failure.
	 *
	 * @return the nodeCertificateExpireWarningDays the number of days; defaults
	 *         to {@link DefaultSetupService#DEFAULT_CERT_EXPIRE_WARNING_DAYS}
	 * @since 2.2
	 */
	public int getNodeCertificateExpireWarningDays() {
		return nodeCertificateExpireWarningDays;
	}

	/**
	 * Set the number of days before treating an expiring node certificate as a
	 * {@link PingTest} failure.
	 *
	 * @param nodeCertificateExpireWarningDays
	 *        the nodeCertificateExpireWarningDays the number of days, or
	 *        {@literal 0} to disable the warning
	 * @since 2.2
	 */
	public void setNodeCertificateExpireWarningDays(int nodeCertificateExpireWarningDays) {
		this.nodeCertificateExpireWarningDays = nodeCertificateExpireWarningDays;
	}

	/**
	 * Get an optional system service.
	 *
	 * @return the service
	 * @since 2.3
	 */
	public OptionalService<SystemService> getSystemService() {
		return systemService;
	}

	/**
	 * Set the optional system service.
	 *
	 * @param systemService
	 *        the service to set
	 * @since 2.3
	 */
	public void setSystemService(OptionalService<SystemService> systemService) {
		this.systemService = systemService;
	}

}
