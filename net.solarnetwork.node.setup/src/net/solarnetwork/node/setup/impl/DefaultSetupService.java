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
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.xml.xpath.XPathExpression;

import net.solarnetwork.domain.BasicNetworkIdentity;
import net.solarnetwork.domain.BasicRegistrationReceipt;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.SetupSettings;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.InvalidVerificationCodeException;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.node.setup.SolarNetHostDetails;
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

/**
 * Implementation of {@link SetupService}.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>settingDao</dt>
 *   <dd>The {@link SettingDao} to use for querying/storing application
 *   state information.</dd>
 *   
 *   <dt>hostName</dt>
 *   <dd>The host name to use for the SolarNet remote service. Defaults
 *   to {@link #DEFAULT_HOST_NAME}. This will be overridden by the 
 *   application setting value for the key 
 *   {@link SetupSettings#KEY_SOLARNETWORK_HOST_NAME}.</dd>
 *   
 *   <dt>hostPort</dt>
 *   <dd>The host port to use for the SolarNet remote service. Defaults
 *   to {@link #DEFAULT_HOST_PORT}. This will be overridden by the 
 *   application setting value for the key 
 *   {@link SetupSettings#KEY_SOLARNETWORK_HOST_PORT}.</dd>
 *   
 *   <dt>forceTLS</dt>
 *   <dd>If <em>true</em> then use TLS (SSL) even on a port other than
 *   {@code 443} (the default TLS port). Defaults to <em>false</em>.</dd>
 *   
 *   <dt>solarInUrlPrefix</dt>
 *   <dd>The URL prefix for the SolarIn service. Defaults to 
 *   {@link DEFAULT_SOLARIN_URL_PREFIX}.</dd>
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
	private static final String VERIFICATION_CODE_HOST_NAME = "hostName";// TODO make sure this key is correct
	private static final String VERIFICATION_CODE_HOST_PORT = "hostPort";// TODO make sure this key is correct
	private static final String VERIFICATION_CODE_CONFIRMATION_KEY = "confirmationKey";
	private static final String VERIFICATION_CODE_EXPIRATION_KEY = "expiration";
	private static final String VERIFICATION_CODE_NODE_ID_KEY = "nodeId";
	private static final String VERIFICATION_CODE_USER_NAME_KEY = "username";

	private static final String SOLAR_NET_IDENTITY_URL = "/solarin/identity.do";

	private static final String SOLAR_NET_REG_URL = "/solarreg/associate.xml";
	
	private SettingDao settingDao;
	private String hostName = DEFAULT_HOST_NAME;
	private Integer hostPort = DEFAULT_HOST_PORT;
	private Boolean forceTLS = Boolean.FALSE;
	private String solarInUrlPrefix = DEFAULT_SOLARIN_URL_PREFIX;
	private Long nodeId; // TODO: remove once setup fully implemented
	
	/** Stores the xpath mappings for the node association confirmation response. */
	private Map<String, XPathExpression> receiptPropertyMapping;
	/** Stores the xpath mappings for the server identity response. */;
	private Map<String, XPathExpression> identityPropertyMapping;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Initialize after all properties configured.
	 */
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
		
		// Set up the xpath configuration for the node association confirmation response
		Map<String, String> xpathMap = new HashMap<String, String>();
		xpathMap.put("username", "/*/@username");
		xpathMap.put("confirmationCode", "/*/@confirmationCode");
		this.receiptPropertyMapping = this.getXPathExpressionMap(xpathMap);
		
		// Set up the xpath configuration for the server identity response
		Map<String, String> identityXpathMap = new HashMap<String, String>();
		identityXpathMap.put("identityKey", "/*/@identityKey");
		identityXpathMap.put("termsOfService", "/*/@termsOfService");
		this.identityPropertyMapping = this.getXPathExpressionMap(identityXpathMap);
	}

	@Override
	public Long getNodeId() {
		// do we have a node ID?
		String nodeId = getSetting(KEY_NODE_ID);
		if ( nodeId == null ) {
			return this.nodeId; // TODO return null once setup fully implemented, keeping for backwards compatability for now (when not using TLS)
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

	/* (non-Javadoc)
	 * @see net.solarnetwork.node.IdentityService#getSolarInBaseUrl()
	 */
	@Override
	public String getSolarInBaseUrl() {
		return "http" +(hostPort == 443 || (forceTLS != null && forceTLS.booleanValue()) ? "s" : "")
			+"://"
			+hostName
			+(hostPort == 443 || hostPort == 80 ? "" : (":" +hostPort))
			+solarInUrlPrefix;
	}

	@Override
	public SolarNetHostDetails decodeVerificationCode(String verificationCode) throws InvalidVerificationCodeException {
		if ( log.isDebugEnabled() ) {
			log.debug("Decoding verification code " + verificationCode);
		}
		
		SolarNetHostDetails details = new SolarNetHostDetails();
		
		try {
			JavaBeanXmlSerializer helper = new JavaBeanXmlSerializer();
			InputStream in = new GZIPInputStream(new Base64InputStream(new ByteArrayInputStream(verificationCode.getBytes())));
			Map<String, Object> result = helper.parseXml(in);
			
			// Get the host server
			String hostName = (String) result.get(VERIFICATION_CODE_HOST_NAME);
			if (hostName == null) {
				// Use the default
				if (log.isDebugEnabled()) {
					log.debug(MessageFormat.format("Property: {0} not found in verfication code, using default host: {1}", VERIFICATION_CODE_HOST_NAME, this.getSolarNetHostName()));
				}
				details.setHostName(this.getSolarNetHostName());
			} else {
				details.setHostName(hostName);
			}
			
			// Get the host port
			String hostPort = (String) result.get(VERIFICATION_CODE_HOST_PORT);
			if (hostPort == null) {
				// Use the default
				if (log.isDebugEnabled()) {
					log.debug(MessageFormat.format("Property: {0} not found in verfication code, using default port: {1}", VERIFICATION_CODE_HOST_PORT, this.getSolarNetHostPort()));
				}
				details.setHostPort(this.getSolarNetHostPort());
			} else {
				try {
					details.setHostPort(Integer.parseInt(hostPort));
				} catch (NumberFormatException e) {
					throw new InvalidVerificationCodeException(MessageFormat.format("Invalid host port: {0} found in verification code: {1}", hostPort, verificationCode), e);
				}
			}
			
			// Get the NodeID
			String nodeId = (String) result.get(VERIFICATION_CODE_NODE_ID_KEY);
			if (nodeId == null) {
				throw new InvalidVerificationCodeException(VERIFICATION_CODE_NODE_ID_KEY + " not found in verification code: " + verificationCode);
			} else {
				try {
					details.setNodeId(Long.parseLong((String) result.get(VERIFICATION_CODE_NODE_ID_KEY)));
				} catch (NumberFormatException e) {
					throw new InvalidVerificationCodeException(MessageFormat.format("Invalid node ID: {0} found in verification code: {1}", nodeId, verificationCode), e);
				}
			}
			
			// Get the confirmation Key
			String confirmationKey = (String) result.get(VERIFICATION_CODE_CONFIRMATION_KEY);
			if (confirmationKey == null) {
				throw new InvalidVerificationCodeException(VERIFICATION_CODE_CONFIRMATION_KEY + " not found in verification code: " + verificationCode);
			} else {
				details.setConfirmationKey(confirmationKey);
			}
			
			// Get the user name
			String userName = (String) result.get(VERIFICATION_CODE_USER_NAME_KEY);
			if (userName == null) {
				throw new InvalidVerificationCodeException(VERIFICATION_CODE_USER_NAME_KEY + " not found in verification code: " + verificationCode);
			} else {
				details.setUserName(userName);
			}
			
			// Get the expiration
			String expiration = (String) result.get(VERIFICATION_CODE_EXPIRATION_KEY);
			if (expiration == null) {
				throw new InvalidVerificationCodeException(VERIFICATION_CODE_EXPIRATION_KEY+ " not found in verification code: " + verificationCode);
			} else {
				try {
					DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
					DateTime expirationDate = fmt.parseDateTime(expiration);
					details.setExpiration(expirationDate);
				} catch (IllegalArgumentException e) {
					throw new InvalidVerificationCodeException(MessageFormat.format("Invalid expiration: {0} found in verification code: {1}", expiration, verificationCode), e);
				}
			}

			details.setForceTLS(forceTLS == null ? false : forceTLS.booleanValue());
			return details;
		} catch (IOException e) {
			throw new InvalidVerificationCodeException("Error while trying to decode verfication code: " + verificationCode, e);
		} catch (Exception e) {
			// Runtime errors can come from webFormGetForBean
			throw new InvalidVerificationCodeException("Error while trying to decode verfication code: " + verificationCode, e);
		}
	}
	
	@Override
	public void populateServerIdentity(SolarNetHostDetails details) {

		// Get identity code from the server and store in details
		BasicNetworkIdentity identity = new BasicNetworkIdentity();
		this.webFormGetForBean(null, identity, this.getAbsoluteUrl(details, SOLAR_NET_IDENTITY_URL), null, this.getIdentityPropertyMapping());
		
		details.setIdentity(identity.getIdentityKey());
		details.setTos(identity.getTermsOfService());
	}
	
	/**
	 * Given a relative URL constructs an absolute URL using the supplied details.
	 * 
	 * @param details Contains the host details
	 * @param url The relative URL
	 * @return the absolute URL
	 */
	private String getAbsoluteUrl(SolarNetHostDetails details, String url) {
		return "http://" + details.getHostName() + ":" + details.getHostPort() + url;
	}

	/**
	 * Sends the confirmation message to the SolarNet server to confirm the node association.
	 * @throws Exception 
	 */
	@Override
	public void acceptSolarNetHost(SolarNetHostDetails details) throws Exception {
		if ( log.isDebugEnabled() ) {
			log.debug("Associating with SolarNet service " +details);
		}
		
		try {
		
			// Get confirmation code from the server
			NodeAssociationConfirmationBean bean = this.getNodeAssociationConfirmationBean(details);
			BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
			BasicRegistrationReceipt receipt = new BasicRegistrationReceipt();
			this.webFormPostForBean(beanWrapper, receipt, this.getAbsoluteUrl(details, SOLAR_NET_REG_URL), null, this.getReceiptPropertyMapping());

			// Store the confirmation code and settings on the node
			this.settingDao.storeSetting(KEY_CONFIRMATION_CODE, receipt.getConfirmationCode());
			this.settingDao.storeSetting(KEY_USER_NAME, receipt.getUsername());
			this.settingDao.storeSetting(KEY_NODE_ID, bean.getNodeId());
			this.settingDao.storeSetting(KEY_SOLARNETWORK_HOST_NAME, details.getHostName());
			this.settingDao.storeSetting(KEY_SOLARNETWORK_HOST_PORT, Integer.toString(details.getHostPort()));
			
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("Error while confirming server details: " + details, e);
			}
			// Runtime errors can come from webFormGetForBean
			throw new Exception("Error while confirming server details: " + details, e);
		}
	}
	
	/**
	 * Creates a NodeAssociationConfirmationBean based on the values in the supplied SolarNetHostDetails object.
	 * 
	 * @param details Contains the details to store in the returned bean.
	 * @return the NodeAssociationConfirmationBean with the appropriate details.
	 */
	private NodeAssociationConfirmationBean getNodeAssociationConfirmationBean(SolarNetHostDetails details) {
		NodeAssociationConfirmationBean bean = new NodeAssociationConfirmationBean();
		bean.setKey(details.getConfirmationKey());
		bean.setNodeId(details.getNodeId());
		bean.setUsername(details.getUserName());
		return bean;
	}

	private String getSetting(String key) {
		return settingDao.getSetting(key, SETUP_TYPE_KEY);
	}

	/**
	 * @param settingDao the settingDao to set
	 */
	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

	/**
	 * @param hostName the hostName to set
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * @param hostPort the hostPort to set
	 */
	public void setHostPort(Integer hostPort) {
		this.hostPort = hostPort;
	}	
	
	/**
	 * @param nodeId the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @param solarInUrlPrefix the solarInUrlPrefix to set
	 */
	public void setSolarInUrlPrefix(String solarInUrlPrefix) {
		this.solarInUrlPrefix = solarInUrlPrefix;
	}

	/**
	 * @return the forceTLS
	 */
	public Boolean getForceTLS() {
		return forceTLS;
	}

	/**
	 * @param forceTLS the forceTLS to set
	 */
	public void setForceTLS(Boolean forceTLS) {
		this.forceTLS = forceTLS;
	}

	/**
	 * @return the receiptPropertyMapping
	 */
	public Map<String, XPathExpression> getReceiptPropertyMapping() {
		return receiptPropertyMapping;
	}

	/**
	 * @return the identityPropertyMapping
	 */
	public Map<String, XPathExpression> getIdentityPropertyMapping() {
		return identityPropertyMapping;
	}
	
}
