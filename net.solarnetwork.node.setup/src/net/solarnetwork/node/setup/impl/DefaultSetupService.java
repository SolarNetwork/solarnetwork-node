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
import static net.solarnetwork.node.SetupSettings.KEY_SOLARNETWORK_HOST_NAME;
import static net.solarnetwork.node.SetupSettings.KEY_SOLARNETWORK_HOST_PORT;
import static net.solarnetwork.node.SetupSettings.KEY_USER_NAME;
import static net.solarnetwork.node.SetupSettings.SETUP_TYPE_KEY;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.xml.xpath.XPathExpression;
import net.solarnetwork.domain.BasicNetworkIdentity;
import net.solarnetwork.domain.BasicRegistrationReceipt;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.SetupSettings;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.InvalidVerificationCodeException;
import net.solarnetwork.node.setup.SetupException;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.node.support.XmlServiceSupport;
import net.solarnetwork.util.JavaBeanXmlSerializer;
import org.apache.commons.codec.binary.Base64InputStream;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
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
 * @version $Id$
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
	private static final String VERIFICATION_CODE_NODE_ID_KEY = "nodeId";
	private static final String VERIFICATION_CODE_USER_NAME_KEY = "username";
	private static final String VERIFICATION_CODE_FORCE_TLS = "forceTLS";

	private static final String SOLAR_NET_IDENTITY_URL = "/solarin/identity.do";
	private static final String SOLAR_NET_REG_URL = "/solarreg/associate.xml";

	private PlatformTransactionManager transactionManager;
	private SettingDao settingDao;
	private String hostName = DEFAULT_HOST_NAME;
	private Integer hostPort = DEFAULT_HOST_PORT;
	private Boolean forceTLS = Boolean.FALSE;
	private String solarInUrlPrefix = DEFAULT_SOLARIN_URL_PREFIX;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void init() {
		super.init();

		// cache host name value from settings
		String host = getSetting(KEY_SOLARNETWORK_HOST_NAME);
		if ( host != null ) {
			hostName = host;
		}
		String port = getSetting(KEY_SOLARNETWORK_HOST_PORT);
		if ( port != null ) {
			hostPort = Integer.valueOf(port);
		}
	}

	private Map<String, XPathExpression> getNodeAssociationPropertyMapping() {
		Map<String, String> xpathMap = new HashMap<String, String>();
		xpathMap.put(VERIFICATION_CODE_NODE_ID_KEY, "/*/@nodeId");
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
		return getXPathExpressionMap(identityXpathMap);
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
		return hostName;
	}

	@Override
	public Integer getSolarNetHostPort() {
		return hostPort;
	}

	@Override
	public String getSolarNetSolarInUrlPrefix() {
		return solarInUrlPrefix;
	}

	@Override
	public String getSolarInBaseUrl() {
		return "http" + (hostPort == 443 || (forceTLS != null && forceTLS.booleanValue()) ? "s" : "")
				+ "://" + hostName + (hostPort == 443 || hostPort == 80 ? "" : (":" + hostPort))
				+ solarInUrlPrefix;
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

			// Get the NodeID
			String nodeId = (String) result.get(VERIFICATION_CODE_NODE_ID_KEY);
			if ( nodeId == null ) {
				throw new InvalidVerificationCodeException(VERIFICATION_CODE_NODE_ID_KEY
						+ " not found in verification code: " + verificationCode);
			}
			try {
				details.setNodeId(Long.valueOf(nodeId));
			} catch ( NumberFormatException e ) {
				throw new InvalidVerificationCodeException("Invalid host node ID value: " + hostPort, e);
			}

			// Get the confirmation Key
			String confirmationKey = (String) result.get(VERIFICATION_CODE_CONFIRMATION_KEY);
			if ( confirmationKey == null ) {
				throw new InvalidVerificationCodeException("Missing confirmation code");
			}
			details.setConfirmationKey(confirmationKey);

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
			details.setForceTLS(forceTLS == null ? false : forceTLS.booleanValue());

			// Get the security phrase
			String phrase = (String) result.get(VERIFICATION_CODE_SECURITY_PHRASE);
			if ( phrase == null ) {
				throw new InvalidVerificationCodeException(VERIFICATION_CODE_SECURITY_PHRASE
						+ " not found in verification code: " + verificationCode);
			}
			details.setSecurityPhrase(phrase);

			return details;
		} catch ( Exception e ) {
			// Runtime/IO errors can come from webFormGetForBean
			throw new InvalidVerificationCodeException("Error while trying to decode verfication code: "
					+ verificationCode, e);
		}
	}

	@Override
	public void populateServerIdentity(NetworkAssociationDetails details) {
		// Get identity code from the server and store in details
		BasicNetworkIdentity identity = new BasicNetworkIdentity();
		this.webFormGetForBean(null, identity, this.getAbsoluteUrl(details, SOLAR_NET_IDENTITY_URL),
				null, getIdentityPropertyMapping());

		details.setIdentityKey(identity.getIdentityKey());
		details.setTermsOfService(identity.getTermsOfService());
	}

	/**
	 * Given a relative URL constructs an absolute URL using the supplied
	 * details.
	 * 
	 * @param details
	 *        Contains the host details
	 * @param url
	 *        The relative URL
	 * @return the absolute URL
	 */
	private String getAbsoluteUrl(NetworkAssociationDetails details, String url) {
		return "http://" + details.getHost() + ":" + details.getPort() + url;
	}

	@Override
	public void acceptSolarNetHost(final NetworkAssociationDetails details) throws SetupException {
		log.debug("Associating with SolarNet service {}", details);

		try {
			// Get confirmation code from the server
			final NodeAssociationConfirmationBean bean = getNodeAssociationConfirmationBean(details);
			final BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
			final BasicRegistrationReceipt receipt = new BasicRegistrationReceipt();
			webFormPostForBean(beanWrapper, receipt, getAbsoluteUrl(details, SOLAR_NET_REG_URL), null,
					getNodeAssociationPropertyMapping());

			final TransactionTemplate tt = new TransactionTemplate(transactionManager);
			tt.execute(new TransactionCallbackWithoutResult() {

				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// Store the confirmation code and settings on the node
					settingDao.storeSetting(KEY_CONFIRMATION_CODE, receipt.getConfirmationCode());
					settingDao.storeSetting(KEY_USER_NAME, receipt.getUsername());
					settingDao.storeSetting(KEY_NODE_ID, bean.getNodeId());
					settingDao.storeSetting(KEY_SOLARNETWORK_HOST_NAME, details.getHost());
					settingDao.storeSetting(KEY_SOLARNETWORK_HOST_PORT, details.getPort().toString());
				}
			});

		} catch ( Exception e ) {
			log.error("Error while confirming server details: {}", details, e);
			// Runtime errors can come from webFormGetForBean
			throw new SetupException("Error while confirming server details: " + details, e);
		}
	}

	/**
	 * Creates a NodeAssociationConfirmationBean based on the values in the
	 * supplied SolarNetHostDetails object.
	 * 
	 * @param details
	 *        Contains the details to store in the returned bean.
	 * @return the NodeAssociationConfirmationBean with the appropriate details.
	 */
	private NodeAssociationConfirmationBean getNodeAssociationConfirmationBean(
			NetworkAssociationDetails details) {
		NodeAssociationConfirmationBean bean = new NodeAssociationConfirmationBean();
		bean.setKey(details.getConfirmationKey());
		bean.setNodeId(details.getNodeId());
		bean.setUsername(details.getUsername());
		return bean;
	}

	private String getSetting(String key) {
		return settingDao.getSetting(key, SETUP_TYPE_KEY);
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public void setHostPort(Integer hostPort) {
		this.hostPort = hostPort;
	}

	public void setSolarInUrlPrefix(String solarInUrlPrefix) {
		this.solarInUrlPrefix = solarInUrlPrefix;
	}

	public void setForceTLS(Boolean forceTLS) {
		this.forceTLS = forceTLS;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

}
