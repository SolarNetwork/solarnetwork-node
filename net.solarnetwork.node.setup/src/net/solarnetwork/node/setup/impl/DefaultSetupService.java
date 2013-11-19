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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.setup.impl;

import static net.solarnetwork.node.SetupSettings.KEY_CONFIRMATION_CODE;
import static net.solarnetwork.node.SetupSettings.KEY_NODE_ID;
import static net.solarnetwork.node.SetupSettings.KEY_SOLARNETWORK_FORCE_TLS;
import static net.solarnetwork.node.SetupSettings.KEY_SOLARNETWORK_HOST_NAME;
import static net.solarnetwork.node.SetupSettings.KEY_SOLARNETWORK_HOST_PORT;
import static net.solarnetwork.node.SetupSettings.SETUP_TYPE_KEY;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.xml.xpath.XPathExpression;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.SetupSettings;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.InvalidVerificationCodeException;
import net.solarnetwork.node.setup.PKIService;
import net.solarnetwork.node.setup.SetupException;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.node.support.XmlServiceSupport;
import net.solarnetwork.util.JavaBeanXmlSerializer;
import org.apache.commons.codec.binary.Base64InputStream;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implementation of {@link SetupService}.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>settingDao</dt>
 * <dd>The {@link SettingDao} to use for querying/storing application state
 * information.</dd>
 * 
 * <dt>hostName</dt>
 * <dd>The host name to use for the SolarNet remote service. Defaults to
 * {@link #DEFAULT_HOST_NAME}. This will be overridden by the application
 * setting value for the key {@link SetupSettings#KEY_SOLARNETWORK_HOST_NAME}.</dd>
 * 
 * <dt>hostPort</dt>
 * <dd>The host port to use for the SolarNet remote service. Defaults to
 * {@link #DEFAULT_HOST_PORT}. This will be overridden by the application
 * setting value for the key {@link SetupSettings#KEY_SOLARNETWORK_HOST_PORT}.</dd>
 * 
 * <dt>forceTLS</dt>
 * <dd>If <em>true</em> then use TLS (SSL) even on a port other than {@code 443}
 * (the default TLS port). Defaults to <em>false</em>.</dd>
 * 
 * <dt>solarInUrlPrefix</dt>
 * <dd>The URL prefix for the SolarIn service. Defaults to
 * {@link DEFAULT_SOLARIN_URL_PREFIX}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultSetupService extends XmlServiceSupport implements SetupService, IdentityService {

	/** The default value for the {@code hostName} property. */
	public static final String DEFAULT_HOST_NAME = "in.solarnetwork.net";

	/** The default value for the {@code hostPort} property. */
	public static final Integer DEFAULT_HOST_PORT = 443;

	/** The default value for the {@code solarInUrlPrefix} property. */
	public static final String DEFAULT_SOLARIN_URL_PREFIX = "/solarin";

	// The keys used in the verification code xml
	private static final String VERIFICATION_CODE_HOST_NAME = "host";
	private static final String VERIFICATION_CODE_HOST_PORT = "port";
	private static final String VERIFICATION_CODE_CONFIRMATION_KEY = "confirmationKey";
	private static final String VERIFICATION_CODE_IDENTITY_KEY = "identityKey";
	private static final String VERIFICATION_CODE_TERMS_OF_SERVICE = "termsOfService";
	private static final String VERIFICATION_CODE_EXPIRATION_KEY = "expiration";
	private static final String VERIFICATION_CODE_SECURITY_PHRASE = "securityPhrase";
	private static final String VERIFICATION_CODE_NODE_ID_KEY = "networkId";
	private static final String VERIFICATION_CODE_NODE_CERT_DN_KEY = "networkCertificateSubjectDN";
	private static final String VERIFICATION_CODE_USER_NAME_KEY = "username";
	private static final String VERIFICATION_CODE_FORCE_TLS = "forceTLS";

	private static final String SOLAR_NET_IDENTITY_URL = "/solarin/identity.do";
	private static final String SOLAR_NET_REG_URL = "/solaruser/associate.xml";

	private PKIService pkiService;
	private PlatformTransactionManager transactionManager;
	private SettingDao settingDao;
	private String solarInUrlPrefix = DEFAULT_SOLARIN_URL_PREFIX;

	private Map<String, XPathExpression> getNodeAssociationPropertyMapping() {
		Map<String, String> xpathMap = new HashMap<String, String>();
		xpathMap.put(VERIFICATION_CODE_NODE_ID_KEY, "/*/@networkId");
		xpathMap.put(VERIFICATION_CODE_NODE_CERT_DN_KEY, "/*/@networkCertificateSubjectDN");
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
		return getXPathExpressionMap(identityXpathMap);
	}

	private boolean isForceTLS() {
		String force = getSetting(KEY_SOLARNETWORK_FORCE_TLS);
		if ( force == null ) {
			return false;
		}
		return Boolean.parseBoolean(force);
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
		String nodeId = getSetting(KEY_NODE_ID);
		if ( nodeId == null ) {
			return null;
		}
		return Long.valueOf(nodeId);
	}

	@Override
	public String getSolarNetHostName() {
		return getSetting(KEY_SOLARNETWORK_HOST_NAME);
	}

	@Override
	public Integer getSolarNetHostPort() {
		String port = getSetting(KEY_SOLARNETWORK_HOST_PORT);
		if ( port == null ) {
			return 443;
		}
		return Integer.valueOf(port);
	}

	@Override
	public String getSolarNetSolarInUrlPrefix() {
		return solarInUrlPrefix;
	}

	@Override
	public String getSolarInBaseUrl() {
		final int port = getPort();
		return "http" + (port == 443 || isForceTLS() ? "s" : "") + "://" + getSolarNetHostName()
				+ (port == 443 || port == 80 ? "" : (":" + port)) + solarInUrlPrefix;
	}

	@Override
	public NetworkAssociationDetails decodeVerificationCode(String verificationCode)
			throws InvalidVerificationCodeException {
		log.debug("Decoding verification code {}", verificationCode);

		NetworkAssociationDetails details = new NetworkAssociationDetails();

		try {
			JavaBeanXmlSerializer helper = new JavaBeanXmlSerializer();
			InputStream in = new GZIPInputStream(new Base64InputStream(new ByteArrayInputStream(
					verificationCode.getBytes())));
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
				DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
				DateTime expirationDate = fmt.parseDateTime(expiration);
				details.setExpiration(expirationDate.toDate());
			} catch ( IllegalArgumentException e ) {
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
			throw new InvalidVerificationCodeException("Error while trying to decode verfication code: "
					+ verificationCode, e);
		}
	}

	@Override
	public NetworkAssociation retrieveNetworkAssociation(NetworkAssociationDetails details) {
		NetworkAssociationDetails association = new NetworkAssociationDetails();
		NetworkAssociationRequest req = new NetworkAssociationRequest();
		req.setUsername(details.getUsername());
		req.setKey(details.getConfirmationKey());
		webFormGetForBean(new BeanWrapperImpl(req), association,
				getAbsoluteUrl(details, SOLAR_NET_IDENTITY_URL), null, getIdentityPropertyMapping());
		return association;
	}

	private String getAbsoluteUrl(NetworkAssociationDetails details, String url) {
		return "http" + (details.getPort() == 443 || details.isForceTLS() ? "s" : "") + "://"
				+ details.getHost() + ":" + details.getPort() + url;
	}

	@Override
	public NetworkCertificate acceptNetworkAssociation(final NetworkAssociationDetails details)
			throws SetupException {
		log.debug("Associating with SolarNet service {}", details);

		try {
			// Get confirmation code from the server
			NetworkAssociationRequest req = new NetworkAssociationRequest();
			req.setUsername(details.getUsername());
			req.setKey(details.getConfirmationKey());
			final BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(req);
			final NetworkCertificate result = new NetworkAssociationDetails();
			webFormPostForBean(beanWrapper, result, getAbsoluteUrl(details, SOLAR_NET_REG_URL), null,
					getNodeAssociationPropertyMapping());

			final TransactionTemplate tt = new TransactionTemplate(transactionManager);
			tt.execute(new TransactionCallbackWithoutResult() {

				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// Store the confirmation code and settings on the node
					saveSetting(KEY_CONFIRMATION_CODE, result.getConfirmationKey());
					saveSetting(KEY_NODE_ID, result.getNetworkId().toString());
					saveSetting(KEY_SOLARNETWORK_HOST_NAME, details.getHost());
					saveSetting(KEY_SOLARNETWORK_HOST_PORT, details.getPort().toString());
					saveSetting(KEY_SOLARNETWORK_FORCE_TLS, String.valueOf(details.isForceTLS()));
				}
			});

			// create the node's CSR based on the given subjectDN
			log.debug("Creating node CSR for subject {}", result.getNetworkCertificateSubjectDN());

			pkiService.generateNodeSelfSignedCertificate(result.getNetworkCertificateSubjectDN());

			return result;
		} catch ( Exception e ) {
			log.error("Error while confirming server details: {}", details, e);
			// Runtime errors can come from webFormGetForBean
			throw new SetupException("Error while confirming server details: " + details, e);
		}
	}

	private String getSetting(String key) {
		return (settingDao == null ? null : settingDao.getSetting(key, SETUP_TYPE_KEY));
	}

	private void saveSetting(String key, String value) {
		if ( settingDao == null ) {
			return;
		}
		settingDao.storeSetting(key, SETUP_TYPE_KEY, value);
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

	public void setSolarInUrlPrefix(String solarInUrlPrefix) {
		this.solarInUrlPrefix = solarInUrlPrefix;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setPkiService(PKIService pkiService) {
		this.pkiService = pkiService;
	}

}
