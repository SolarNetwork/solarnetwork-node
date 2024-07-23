/* ==================================================================
 * SimpleManagedJob.java - 13/10/2021 4:19:15 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.job;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.GroupSettingSpecifier;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.MappableSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicCronExpressionSettingSpecifier;
import net.solarnetwork.support.PrefixedMessageSource;

/**
 * Simple implementation of {@link ManagedJob}.
 *
 * @author matt
 * @version 1.2
 */
public class SimpleManagedJob extends BaseIdentifiable
		implements ManagedJob, SettingsChangeObserver, ServiceLifecycleObserver {

	/** The {@code scheduleSettingKey} property default value. */
	public static final String DEFAULT_SCHEDULE_SETTING_KEY = "schedule";

	private static final String JOB_SERVICE_SETTING_PREFIX = "jobService.";
	private static final String DEFAULT_VALUE_PROPERTY = "defaultValue";

	private static final Logger log = LoggerFactory.getLogger(SimpleManagedJob.class);

	private final JobService jobService;
	private String scheduleSettingKey = DEFAULT_SCHEDULE_SETTING_KEY;
	private String schedule;
	private Map<String, SimpleServiceProviderConfiguration> serviceProviderConfigurations;

	private PropertyAccessor jobServiceAccessor;
	private boolean ignoreLegacySchedule = false;

	/**
	 * Constructor.
	 *
	 * @param jobService
	 *        the job service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleManagedJob(JobService jobService) {
		this(jobService, null);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This constructor can be used to configure a default schedule that can be
	 * overwritten by either the legacy support method
	 * {@link #setTriggerCronExpression(String)} or the modern replacement
	 * {@link #setSchedule(String)}.
	 * </p>
	 *
	 * @param jobService
	 *        the job service
	 * @param schedule
	 *        the default schedule to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleManagedJob(JobService jobService, String schedule) {
		super();
		this.jobService = requireNonNullArgument(jobService, "jobService");
		this.schedule = schedule;
		setUid(UUID.randomUUID().toString());
	}

	@Override
	public JobService getJobService() {
		return jobService;
	}

	@Override
	public String toString() {
		return String.format("SimpleManagedJob{%s @ %s}", jobName(), getSchedule());
	}

	/**
	 * Get the job trigger.
	 *
	 * @return the trigger
	 */
	public Trigger getTrigger() {
		final String expression = getSchedule();
		if ( expression != null ) {
			try {
				try {
					long ms = Long.parseLong(expression);
					return new PeriodicTrigger(ms);
				} catch ( NumberFormatException e ) {
					// ignore
				}
				return new CronTrigger(expression);
			} catch ( IllegalArgumentException e ) {
				log.warn("Error parsing cron expression [{}]: {}", expression, e.getMessage());
			}
		}
		return null;
	}

	@Override
	public String getSettingUid() {
		return jobService.getSettingUid();
	}

	@Override
	public String getDisplayName() {
		String name = jobService.getDisplayName();
		return (name != null && !name.isEmpty() ? name : super.getDisplayName());
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>();
		result.add(new BasicCronExpressionSettingSpecifier(getScheduleSettingKey(), getSchedule()));
		for ( SettingSpecifier spec : jobService.getSettingSpecifiers() ) {
			if ( spec instanceof MappableSpecifier ) {
				MappableSpecifier keyedSpec = (MappableSpecifier) spec;
				SettingSpecifier mappedSpec = keyedSpec.mappedTo(JOB_SERVICE_SETTING_PREFIX);
				// if legacy settings are active, then map default values to active values for UI
				if ( jobServiceAccessor != null ) {
					if ( keyedSpec instanceof KeyedSettingSpecifier<?> ) {
						populateLegacySettingDefaultValue(
								((KeyedSettingSpecifier<?>) keyedSpec).getKey(),
								(KeyedSettingSpecifier<?>) mappedSpec);
					} else if ( mappedSpec instanceof GroupSettingSpecifier ) {
						handleLegacyGroupSettingSpecifier((GroupSettingSpecifier) mappedSpec);
					}
				}
				result.add(mappedSpec);
			} else {
				result.add(spec);
			}
		}
		return result;

	}

	private void handleLegacyGroupSettingSpecifier(final GroupSettingSpecifier group) {
		for ( SettingSpecifier spec : group.getGroupSettings() ) {
			if ( spec instanceof GroupSettingSpecifier ) {
				GroupSettingSpecifier child = (GroupSettingSpecifier) spec;
				handleLegacyGroupSettingSpecifier(child);
			} else if ( spec instanceof KeyedSettingSpecifier<?> ) {
				KeyedSettingSpecifier<?> child = (KeyedSettingSpecifier<?>) spec;
				String propKey = child.getKey();
				if ( propKey.startsWith(JOB_SERVICE_SETTING_PREFIX) ) {
					propKey = propKey.substring(JOB_SERVICE_SETTING_PREFIX.length());
				}
				populateLegacySettingDefaultValue(propKey, child);
			}
		}

	}

	private void populateLegacySettingDefaultValue(final String propKey,
			final KeyedSettingSpecifier<?> spec) {
		PropertyAccessor specAccessor = PropertyAccessorFactory.forBeanPropertyAccess(spec);
		if ( specAccessor.isWritableProperty(DEFAULT_VALUE_PROPERTY) ) {
			if ( jobServiceAccessor.isReadableProperty(propKey) ) {
				try {
					Object newDefaultValue = jobServiceAccessor.getPropertyValue(propKey);
					specAccessor.setPropertyValue(DEFAULT_VALUE_PROPERTY, newDefaultValue);
				} catch ( Exception e ) {
					// ignore
				}
			} else {
				log.warn(
						"Unable to map [{}] legacy setting property [{}] default value: property not readable",
						getSettingUid(), propKey);
			}
		}
	}

	@Override
	public MessageSource getMessageSource() {
		if ( super.getMessageSource() == null ) {
			PrefixedMessageSource pSource = new PrefixedMessageSource();
			pSource.setPrefix(JOB_SERVICE_SETTING_PREFIX);
			pSource.setDelegate(jobService.getMessageSource());
			setMessageSource(pSource);
		}
		return super.getMessageSource();
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		Object scheduleVal = (properties != null ? properties.get(getScheduleSettingKey()) : null);
		if ( scheduleVal != null ) {
			String newSchedule = scheduleVal.toString();
			setSchedule(newSchedule);
		}
		if ( jobService instanceof SettingsChangeObserver ) {
			((SettingsChangeObserver) jobService).configurationChanged(properties);
		}
	}

	@Override
	public void serviceDidStartup() {
		if ( jobService instanceof ServiceLifecycleObserver ) {
			ServiceLifecycleObserver delegate = (ServiceLifecycleObserver) jobService;
			try {
				delegate.serviceDidStartup();
			} catch ( Exception e ) {
				log.error("Error delegating job {} lifecycle startup: {}", jobName(), e.toString(), e);
			}
		}
	}

	@Override
	public void serviceDidShutdown() {
		if ( jobService instanceof ServiceLifecycleObserver ) {
			ServiceLifecycleObserver delegate = (ServiceLifecycleObserver) jobService;
			try {
				delegate.serviceDidShutdown();
			} catch ( Exception e ) {
				log.error("Error delegating job {} lifecycle shutdown: {}", jobName(), e.toString(), e);
			}
		}
	}

	@Override
	public Collection<ServiceConfiguration> getServiceConfigurations() {
		Collection<ServiceConfiguration> result = null;
		if ( serviceProviderConfigurations != null && serviceProviderConfigurations.size() > 0 ) {
			PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(jobService);
			for ( Map.Entry<String, SimpleServiceProviderConfiguration> me : serviceProviderConfigurations
					.entrySet() ) {
				try {
					Object o = bean.getPropertyValue(me.getKey());
					if ( o != null ) {
						SimpleServiceProviderConfiguration conf = me.getValue();
						SimpleServiceProviderConfiguration ser = new SimpleServiceProviderConfiguration();
						ser.setService(o);
						ser.setInterfaces(conf.getInterfaces());
						ser.setProperties(conf.getProperties());
						if ( result == null ) {
							result = new ArrayList<>();
						}
						result.add(ser);
					}
				} catch ( InvalidPropertyException | PropertyAccessException e ) {
					log.error("Error configuring job {} service provider {}: {}", jobName(), me.getKey(),
							e.toString());
				}
			}
		}
		return result;
	}

	/**
	 * Set a mapping of service provider configurations.
	 *
	 * <p>
	 * When this job is registered at runtime, these services will also be
	 * registered.
	 * </p>
	 *
	 * @param serviceProviderConfigurations
	 *        the configurations to set
	 */
	public void setServiceProviderConfigurations(
			Map<String, SimpleServiceProviderConfiguration> serviceProviderConfigurations) {
		this.serviceProviderConfigurations = serviceProviderConfigurations;
	}

	private String jobName() {
		String name = getDisplayName();
		if ( name == null || name.isEmpty() ) {
			name = String.format("%s job", jobService.getClass().getSimpleName());
		}
		return name;
	}

	/**
	 * A getter property to maintain backwards-compatibility with legacy
	 * SolarNode 1.x settings.
	 *
	 * <p>
	 * This method exists to support SolarNode 1.x settings like
	 * {@literal jobDetail.x}.
	 * </p>
	 *
	 * @return this instance
	 */
	public SimpleManagedJob getJobDetail() {
		return this;
	}

	/**
	 * A getter property to maintain backwards-compatibility with legacy
	 * SolarNode 1.x settings.
	 * <p>
	 * This method exists to support SolarNode 1.x settings like
	 * {@literal jobDetail.jobDataMap['datumDataSource'].x}.
	 * </p>
	 *
	 * @return a Map instance that exposes JavaBean properties on the configured
	 *         {@link JobService}
	 */
	public synchronized Map<String, Object> getJobDataMap() {
		if ( jobServiceAccessor == null ) {
			jobServiceAccessor = PropertyAccessorFactory.forBeanPropertyAccess(jobService);
		}
		return new AbstractMap<String, Object>() {

			@Override
			public Object get(Object key) {
				if ( key == null ) {
					return null;
				}
				if ( jobServiceAccessor.isReadableProperty(key.toString()) ) {
					return jobServiceAccessor.getPropertyValue(key.toString());
				}
				return null;
			}

			@Override
			public Object put(String key, Object value) {
				if ( key == null ) {
					return null;
				}
				Object result = null;
				if ( jobServiceAccessor.isWritableProperty(key) ) {
					if ( jobServiceAccessor.isReadableProperty(key) ) {
						result = jobServiceAccessor.getPropertyValue(key);
					}
					jobServiceAccessor.setPropertyValue(key, value);
				}
				return result;
			}

			@Override
			public Set<Entry<String, Object>> entrySet() {
				return Collections.emptySet();
			}
		};
	}

	@Override
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Set the trigger schedule expression.
	 *
	 * <p>
	 * Once this method has been called all calls to the legacy schedule setter
	 * method {@link #setTriggerCronExpression(String)} will be ignored.
	 * </p>
	 *
	 * @param schedule
	 *        the trigger schedule expression to set
	 */
	public void setSchedule(String schedule) {
		this.ignoreLegacySchedule = true;
		this.schedule = schedule;
	}

	/**
	 * Set the trigger schedule expression, using the legacy setting property
	 * name.
	 *
	 * <p>
	 * This method is to maintain backwards-compatibility with the SolarNode 1.x
	 * settings. <b>Note</b> that once {@link #setSchedule(String)} is called
	 * this method will have no effect.
	 * </p>
	 *
	 * @param schedule
	 *        the schedule to set
	 */
	public void setTriggerCronExpression(String schedule) {
		if ( !ignoreLegacySchedule ) {
			this.schedule = schedule;
		}
	}

	/**
	 * Get the schedule setting key.
	 *
	 * @return the setting key; defaults to
	 *         {@link #DEFAULT_SCHEDULE_SETTING_KEY}
	 */
	@Override
	public String getScheduleSettingKey() {
		return scheduleSettingKey;
	}

	/**
	 * Set the schedule setting key.
	 *
	 * <p>
	 * This defaults to {@literal schedule} to match the
	 * {@link #setSchedule(String)} property. However this can be changed to any
	 * setting, so that the schedule setting key can be different. This can be
	 * used to bundle mutliple job schedules into the same setting UID, using
	 * different schedule setting keys for different jobs.
	 * </p>
	 *
	 * @param scheduleSettingKey
	 *        the setting key to set
	 */
	public void setScheduleSettingKey(String scheduleSettingKey) {
		this.scheduleSettingKey = scheduleSettingKey;
	}

}
