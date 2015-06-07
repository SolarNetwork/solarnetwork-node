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

package net.solarnetwork.node.ocpp;

import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.OptionalService;
import ocpp.v15.BootNotificationRequest;
import ocpp.v15.BootNotificationResponse;
import ocpp.v15.CentralSystemService;
import ocpp.v15.CentralSystemService_Service;
import ocpp.v15.RegistrationStatus;
import org.osgi.framework.Version;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * Implementation of {@link CentralSystemServiceFactory} that allows configuring
 * the service.
 * 
 * @author matt
 * @version 1.0
 */
public class ConfigurableCentralSystemServiceFactory implements CentralSystemServiceFactory,
		SettingSpecifierProvider {

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
	private BootNotificationResponse bootNotificationResponse;
	private SimpleTrigger heartbeatTrigger;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public CentralSystemService service() {
		CentralSystemService client = getServiceInternal();
		if ( bootNotificationResponse == null ) {
			try {
				postBootNotification();
			} catch ( RuntimeException e ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Error posting BootNotification message to {}", url, e);
				} else {
					log.warn("Error posting BootNotification message to {}: {}", url, e.getMessage());
				}
			}
		}
		return client;
	}

	/**
	 * Initialize the OCPP client. Call this once after all properties
	 * configured.
	 */
	public void startup() {
		log.info("Starting up OCPP service {}", url);
		service();
	}

	/**
	 * Shutdown the OCPP client, releasing any associated resources.
	 */
	public void shutdown() {
		configureHeartbeat(0);
		bootNotificationResponse = null;
	}

	private CentralSystemService getServiceInternal() {
		CentralSystemService result = service;
		if ( result == null ) {
			URL wsdl = CentralSystemService.class
					.getResource("ocpp_centralsystemservice_1.5_final.wsdl");
			QName name = new QName("urn://Ocpp/Cs/2012/06/", "CentralSystemService");
			CentralSystemService client = new CentralSystemService_Service(wsdl, name)
					.getCentralSystemServiceSoap12(new AddressingFeature());
			((BindingProvider) client).getRequestContext().put(
					BindingProvider.ENDPOINT_ADDRESS_PROPERTY, this.url);
			result = client;
			service = client;
		}
		return service;
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

	private boolean postBootNotification() {
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
			log.debug("Node ID not available; cannot post BootNotification");
			return false;
		}

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
		if ( RegistrationStatus.ACCEPTED == response.getStatus()
				&& configureHeartbeat(response.getHeartbeatInterval()) ) {
			bootNotificationResponse = response;
		}
	}

	private boolean configureHeartbeat(int heartbeatInterval) {
		Scheduler sched = scheduler;
		if ( sched == null ) {
			log.warn("No scheduler avaialable, cannot schedule heartbeat job");
			return false;
		}
		final long repeatInterval = heartbeatInterval * 1000L;
		SimpleTrigger trigger = heartbeatTrigger;
		if ( trigger != null ) {
			// check if heartbeatInterval actually changed
			if ( trigger.getRepeatInterval() == repeatInterval ) {
				log.debug("Heartbeat interval unchanged at {}s", heartbeatInterval);
				return true;
			}
			// trigger has changed!
			if ( heartbeatInterval == 0 ) {
				try {
					sched.unscheduleJob(trigger.getName(), trigger.getGroup());
				} catch ( SchedulerException e ) {
					log.error("Error unscheduling OCPP heartbeat job", e);
				} finally {
					heartbeatTrigger = null;
				}
			} else {
				trigger.setRepeatInterval(repeatInterval);
			}
			return true;
		} else if ( heartbeatInterval < 1 ) {
			// nothing to do
			return true;
		}

		synchronized ( sched ) {
			try {
				JobDetail jobDetail = sched.getJobDetail(HEARTBEAT_JOB_NAME, SCHEDULER_GROUP);
				if ( jobDetail == null ) {
					JobDetail jd = new JobDetail();
					jd.setJobClass(HeartbeatJob.class);
					jd.setName(HEARTBEAT_JOB_NAME);
					jd.setGroup(SCHEDULER_GROUP);
					jd.setDurability(true);
					sched.addJob(jd, true);
					jobDetail = jd;
				}
				SimpleTriggerFactoryBean t = new SimpleTriggerFactoryBean();
				t.setName(this.url);
				t.setGroup(SCHEDULER_GROUP);
				t.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
				t.setRepeatInterval(repeatInterval);
				t.setStartDelay(repeatInterval);
				t.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT);
				t.setJobDataAsMap(Collections.singletonMap("service", this));
				t.setJobDetail(jobDetail);
				t.afterPropertiesSet();
				trigger = t.getObject();
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
		return getDefaultSettingSpecifiers();
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	private static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		ConfigurableCentralSystemServiceFactory defaults = new ConfigurableCentralSystemServiceFactory();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);
		results.add(new BasicTextFieldSettingSpecifier("uid", String.valueOf(defaults.uid)));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.groupUID));
		results.add(new BasicTextFieldSettingSpecifier("url", defaults.url));
		results.add(new BasicTextFieldSettingSpecifier("chargePointModel", defaults.chargePointModel));
		results.add(new BasicTextFieldSettingSpecifier("chargePointVendor", defaults.chargePointVendor));
		return results;
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

}
