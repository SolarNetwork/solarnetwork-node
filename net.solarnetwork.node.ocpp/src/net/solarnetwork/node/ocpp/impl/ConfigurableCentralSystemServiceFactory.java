/* ==================================================================
 * ConfigurableCentralSystemServiceFactory.java - 6/06/2015 7:53:18 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.impl;

import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.AddressingFeature;
import org.osgi.framework.Version;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.ocpp.CentralSystemServiceFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.OptionalService;
import ocpp.v15.cs.BootNotificationRequest;
import ocpp.v15.cs.BootNotificationResponse;
import ocpp.v15.cs.CentralSystemService;
import ocpp.v15.cs.CentralSystemService_Service;
import ocpp.v15.cs.RegistrationStatus;
import ocpp.v15.support.HMACHandler;
import ocpp.v15.support.WSAddressingFromHandler;

/**
 * Implementation of {@link CentralSystemServiceFactory} that allows configuring
 * the service.
 * 
 * @author matt
 * @version 1.1
 */
public class ConfigurableCentralSystemServiceFactory
		implements CentralSystemServiceFactory, SettingSpecifierProvider {

	/** The name used to schedule the {@link HeartbeatJob} as. */
	public static final String HEARTBEAT_JOB_NAME = "OCPP_Heartbeat";

	/**
	 * The job and trigger group used to schedule the {@link HeartbeatJob} with.
	 * Note the trigger name will be the {@code url} property value.
	 */
	public static final String SCHEDULER_GROUP = "OCPP";

	private String url = "http://localhost:9000/";
	private String uid = "OCPP Central System";
	private String groupUID;
	private String chargePointModel = "SolarNode";
	private String chargePointVendor = "SolarNetwork";
	private String firmwareVersion;

	private MessageSource messageSource;
	private OptionalService<IdentityService> identityService;
	private Scheduler scheduler;

	private CentralSystemService service;
	private boolean useFromAddress;
	private final WSAddressingFromHandler fromHandler = new WSAddressingFromHandler();
	private final HMACHandler hmacHandler = new HMACHandler();
	private BootNotificationResponse bootNotificationResponse;
	private Throwable bootNotificationError;
	private SimpleTrigger heartbeatTrigger;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public CentralSystemService service() {
		CentralSystemService client = getServiceInternal();
		postBootNotificationIfNeeded();
		configureHeartbeatIfNeeded();
		return client;
	}

	private synchronized void postBootNotificationIfNeeded() {
		if ( bootNotificationResponse == null ) {
			try {
				postBootNotification();
			} catch ( RuntimeException e ) {
				bootNotificationError = e;
				if ( log.isDebugEnabled() ) {
					log.debug("Error posting BootNotification message to {}", url, e);
				} else {
					log.warn("Error posting BootNotification message to {}: {}", url, e.getMessage());
				}
			}
		}
	}

	private synchronized void configureHeartbeatIfNeeded() {
		if ( heartbeatTrigger == null ) {
			// we use the heartbeat job to also re-try the initial boot notification if that has failed
			configureHeartbeat(30, SimpleTrigger.REPEAT_INDEFINITELY);
		}
	}

	/**
	 * Initialize the OCPP client. Call this once after all properties
	 * configured.
	 */
	public void startup() {
		log.info("Starting up OCPP service {}", url);
		postBootNotificationIfNeeded();
		configureHeartbeatIfNeeded();
	}

	/**
	 * Shutdown the OCPP client, releasing any associated resources.
	 */
	public void shutdown() {
		configureHeartbeat(0, 0);
		bootNotificationResponse = null;
		bootNotificationError = null;
	}

	private CentralSystemService getServiceInternal() {
		CentralSystemService result = service;
		if ( result == null ) {
			URL wsdl = CentralSystemService.class
					.getResource("ocpp_centralsystemservice_1.5_final.wsdl");
			QName name = new QName("urn://Ocpp/Cs/2012/06/", "CentralSystemService");
			CentralSystemService client = new CentralSystemService_Service(wsdl, name)
					.getCentralSystemServiceSoap12(new AddressingFeature());
			((BindingProvider) client).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
					this.url);
			result = client;
			setupFromHandler(client, useFromAddress);
			service = client;
		}
		return result;
	}

	private void setupFromHandler(final CentralSystemService client, final boolean use) {
		if ( client == null ) {
			return;
		}

		BindingProvider bindingProvider = (BindingProvider) client;
		boolean modified = false;
		@SuppressWarnings("rawtypes")
		List<Handler> chain = bindingProvider.getBinding().getHandlerChain();
		if ( use ) {
			boolean foundFrom = false;
			boolean foundHmac = false;
			for ( Handler<?> h : chain ) {
				if ( h == fromHandler ) {
					foundFrom = true;
				} else if ( h == hmacHandler ) {
					foundHmac = true;
				}
			}
			if ( !foundFrom ) {
				chain.add(fromHandler);
				modified = true;
			}
			if ( !foundHmac ) {
				chain.add(hmacHandler);
				modified = true;
			}
		} else {
			for ( @SuppressWarnings("rawtypes")
			Iterator<Handler> itr = chain.iterator(); itr.hasNext(); ) {
				Handler<?> h = itr.next();
				if ( h == fromHandler || h == hmacHandler ) {
					itr.remove();
					modified = true;
					break;
				}
			}
		}
		if ( modified ) {
			bindingProvider.getBinding().setHandlerChain(chain);
		}
	}

	/**
	 * Get the ChargeBoxIdentity value to use. This method returns the
	 * {@code Principal#getName()} returned by
	 * {@link IdentityService#getNodePrincipal()}.
	 * 
	 * @return The node's {@link Principal} name value, or an empty string if
	 *         none available.
	 */
	@Override
	public String chargeBoxIdentity() {
		IdentityService ident = (identityService != null ? identityService.service() : null);
		if ( ident == null ) {
			log.debug("IdentityService not available; cannot get ChargeBoxIdentity value");
			return "";
		}
		Principal nodePrincipal = ident.getNodePrincipal();
		if ( nodePrincipal == null ) {
			log.debug("Node Principal not available; cannot get ChargeBoxIdentity value");
			return "";
		}
		return nodePrincipal.getName();
	}

	@Override
	public boolean isBootNotificationPosted() {
		return (bootNotificationResponse != null);
	}

	@Override
	public boolean postBootNotification() {
		IdentityService ident = (identityService != null ? identityService.service() : null);
		if ( ident == null ) {
			log.debug("IdentityService not available; cannot post BootNotification");
			return false;
		}
		Long nodeId = ident.getNodeId();
		if ( nodeId == null ) {
			log.debug("Node ID not available; cannot post BootNotification");
			return false;
		}
		CentralSystemService client = getServiceInternal();
		if ( client == null ) {
			log.debug("CentralSystemService not available; cannot post BootNotification");
			return false;
		}
		synchronized ( this ) {
			BootNotificationRequest req = new BootNotificationRequest();
			req.setChargePointModel(this.chargePointModel);
			req.setChargePointVendor(this.chargePointVendor);
			req.setChargePointSerialNumber(nodeId.toString());
			req.setFirmwareVersion(this.firmwareVersion);
			BootNotificationResponse res = client.bootNotification(req, chargeBoxIdentity());
			if ( res == null ) {
				log.warn("No response from BootNotificationRequest");
				return false;
			}
			setBootNotificationResponse(res);
		}
		return true;
	}

	private void setBootNotificationResponse(BootNotificationResponse response) {
		if ( response == bootNotificationResponse ) {
			return;
		}
		if ( response == null ) {
			bootNotificationResponse = null;
			return;
		}
		log.info("OCPP BootNotification reply: {} @ {}; heartbeat {}s", response.getStatus(),
				response.getCurrentTime(), response.getHeartbeatInterval());
		if ( RegistrationStatus.ACCEPTED == response.getStatus() && configureHeartbeat(
				response.getHeartbeatInterval(), SimpleTrigger.REPEAT_INDEFINITELY) ) {
			bootNotificationResponse = response;
		} else {
			bootNotificationResponse = response;
		}
	}

	private synchronized boolean configureHeartbeat(final int heartbeatInterval, final int repeatCount) {
		Scheduler sched = scheduler;
		if ( sched == null ) {
			log.warn("No scheduler avaialable, cannot schedule heartbeat job");
			return false;
		}
		final JobKey jobKey = new JobKey(HEARTBEAT_JOB_NAME, SCHEDULER_GROUP);
		final long repeatInterval = heartbeatInterval * 1000L;
		SimpleTrigger trigger = heartbeatTrigger;
		if ( trigger != null ) {
			// check if heartbeatInterval actually changed
			if ( trigger.getRepeatInterval() == repeatInterval
					&& trigger.getRepeatCount() == repeatCount ) {
				log.debug("Heartbeat interval unchanged at {}s", heartbeatInterval);
				return true;
			}
			// trigger has changed!
			if ( heartbeatInterval == 0 ) {
				try {
					sched.unscheduleJob(trigger.getKey());
				} catch ( SchedulerException e ) {
					log.error("Error unscheduling OCPP heartbeat job", e);
				} finally {
					heartbeatTrigger = null;
				}
			} else {
				trigger = TriggerBuilder.newTrigger().withIdentity(trigger.getKey()).forJob(jobKey)
						.usingJobData(new JobDataMap(Collections.singletonMap("service", this)))
						.withSchedule(repeatCount < 1
								? SimpleScheduleBuilder.repeatSecondlyForever(heartbeatInterval)
								: SimpleScheduleBuilder.repeatSecondlyForTotalCount(repeatCount,
										heartbeatInterval))
						.build();
				try {
					sched.rescheduleJob(trigger.getKey(), trigger);
				} catch ( SchedulerException e ) {
					log.error("Error rescheduling OCPP heartbeat job", e);
				} finally {
					heartbeatTrigger = trigger;
				}
			}
			return true;
		} else if ( heartbeatInterval < 1 ) {
			// nothing to do
			return true;
		}

		synchronized ( sched ) {
			try {
				JobDetail jobDetail = sched.getJobDetail(jobKey);
				if ( jobDetail == null ) {
					jobDetail = JobBuilder.newJob(HeartbeatJob.class).withIdentity(jobKey).storeDurably()
							.build();
					sched.addJob(jobDetail, true);
				}
				final TriggerKey triggerKey = new TriggerKey(this.url, SCHEDULER_GROUP);
				trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey)
						.startAt(new Date(System.currentTimeMillis() + repeatInterval))
						.usingJobData(new JobDataMap(Collections.singletonMap("service", this)))
						.withSchedule((repeatCount < 1
								? SimpleScheduleBuilder.repeatSecondlyForever(heartbeatInterval)
								: SimpleScheduleBuilder.repeatSecondlyForTotalCount(repeatCount,
										heartbeatInterval))
												.withMisfireHandlingInstructionNextWithExistingCount())
						.build();
				sched.scheduleJob(trigger);
				heartbeatTrigger = trigger;
				return true;
			} catch ( Exception e ) {
				log.error("Error scheduling OCPP heartbeat job", e);
				return false;
			}
		}
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.central";
	}

	@Override
	public String getDisplayName() {
		return "OCPP Central System";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ConfigurableCentralSystemServiceFactory defaults = new ConfigurableCentralSystemServiceFactory();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTextFieldSettingSpecifier("uid", String.valueOf(defaults.uid)));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.groupUID));
		results.add(new BasicTextFieldSettingSpecifier("url", defaults.url));
		results.add(new BasicTextFieldSettingSpecifier("chargePointModel", defaults.chargePointModel));
		results.add(new BasicTextFieldSettingSpecifier("chargePointVendor", defaults.chargePointVendor));

		results.add(new BasicToggleSettingSpecifier("useFromAddress", defaults.useFromAddress));
		results.add(new BasicTextFieldSettingSpecifier("fromHandler.fromURL",
				defaults.fromHandler.getFromURL()));
		results.add(new BasicTextFieldSettingSpecifier("fromHandler.dynamicFromPath",
				defaults.fromHandler.getDynamicFromPath()));
		results.add(new BasicTextFieldSettingSpecifier("fromHandler.networkInterfaceName",
				defaults.fromHandler.getNetworkInterfaceName()));
		results.add(new BasicToggleSettingSpecifier("fromHandler.preferIPv4Address",
				defaults.fromHandler.isPreferIPv4Address()));

		results.add(
				new BasicTextFieldSettingSpecifier("hmacHandler.secret", HMACHandler.DEFAULT_SECRET));
		results.add(new BasicTextFieldSettingSpecifier("hmacHandler.maximumTimeSkew",
				String.valueOf(hmacHandler.getMaximumTimeSkew())));

		return results;
	}

	private String getInfoMessage() {
		StringBuilder buf = new StringBuilder();
		BootNotificationResponse bootResponse = bootNotificationResponse;
		Throwable bootError = bootNotificationError;
		if ( bootResponse != null ) {
			if ( bootResponse.getStatus() == RegistrationStatus.ACCEPTED ) {
				buf.append(messageSource.getMessage("status.accepted",
						new Object[] {
								(bootResponse.getCurrentTime() != null
										? bootResponse.getCurrentTime().toString() : "N/A"),
								bootResponse.getHeartbeatInterval() / 60 },
						Locale.getDefault()));
			} else {
				buf.append(messageSource.getMessage("status.rejected",
						new Object[] { chargeBoxIdentity(), bootResponse.getCurrentTime() },
						Locale.getDefault()));
			}
		} else if ( bootError != null ) {
			while ( bootError.getCause() != null ) {
				bootError = bootError.getCause();
			}
			buf.append(messageSource.getMessage("status.error", new Object[] { bootError.getMessage() },
					Locale.getDefault()));
		}
		return (buf.length() > 0 ? buf.toString() : "N/A");
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	// Accessors

	@Override
	public String getUID() {
		return getUid();
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	/**
	 * Get the absolute web service URL to the OCPP central system.
	 * 
	 * @return The absolute web service URL.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the absolute web service URL to the OCPP central system.
	 * 
	 * @param url
	 *        The absolute web service URL.
	 */
	public void setUrl(String url) {
		if ( url == null || !url.equals(this.url) ) {
			final boolean restart = this.service != null;
			if ( restart ) {
				shutdown();
				this.service = null;
			}
			this.url = url;
			if ( restart ) {
				startup();
			}
		}
	}

	/**
	 * Set the {@link MessageSource} to use for settings.
	 * 
	 * @param messageSource
	 *        The message source to use.
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Set the {@link IdentityService} to use for identifying the SolarNode to
	 * the OCPP server.
	 * 
	 * @param identityService
	 *        The {@link IdentityService} to use.
	 */
	public void setIdentityService(OptionalService<IdentityService> identityService) {
		this.identityService = identityService;
	}

	/**
	 * Set the model name.
	 * 
	 * @param chargePointModel
	 *        The model name to set.
	 */
	public void setChargePointModel(String chargePointModel) {
		this.chargePointModel = chargePointModel;
	}

	/**
	 * Set the vendor name.
	 * 
	 * @param chargePointVendor
	 *        The vendor name to set.
	 */
	public void setChargePointVendor(String chargePointVendor) {
		this.chargePointVendor = chargePointVendor;
	}

	/**
	 * Set the version.
	 * 
	 * @param firmwareVersion
	 *        The version to set.
	 */
	public void setFirmwareVersion(String chargePointVersion) {
		this.firmwareVersion = chargePointVersion;
	}

	/**
	 * Set the version as an OSGi version. This will pass
	 * {@link Version#toString()} to {@link #setFirmwareVersion(String)}.
	 * 
	 * @param version
	 *        The version to set.
	 */
	public void setVersion(Version version) {
		setFirmwareVersion(version == null ? "" : version.toString());
	}

	/**
	 * Set the Scheduler to use for the {@link HeartbeatJob}.
	 * 
	 * @param scheduler
	 *        The scheduler to use.
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * Get the flag to add WS-Addressing {@code From} headers into outbound OCPP
	 * messages.
	 * 
	 * @return Boolean
	 */
	public boolean isUseFromAddress() {
		return useFromAddress;
	}

	/**
	 * Set the flag to add WS-Addressing {@code From} headers into outbound OCPP
	 * messages. If set to <em>true</em> then the {@code fromAddress} property
	 * will be used as the address value. If that property is not configured, a
	 * default HTTP URL will be constructed out of the system's local IP address
	 * with a default path of {@code /ocpp} added.
	 * 
	 * @param useFromAddress
	 *        Boolean
	 */
	public void setUseFromAddress(boolean useFromAddress) {
		this.useFromAddress = useFromAddress;
		setupFromHandler(getServiceInternal(), useFromAddress);
	}

	public WSAddressingFromHandler getFromHandler() {
		return fromHandler;
	}

	public HMACHandler getHmacHandler() {
		return hmacHandler;
	}

}
