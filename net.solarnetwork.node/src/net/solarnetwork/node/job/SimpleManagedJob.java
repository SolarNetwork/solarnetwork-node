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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.MappableSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicCronExpressionSettingSpecifier;
import net.solarnetwork.settings.support.KeyedSmartQuotedTemplateMapper;
import net.solarnetwork.support.PrefixedMessageSource;
import net.solarnetwork.support.TemplatedMessageSource;
import net.solarnetwork.util.ObjectUtils;

/**
 * Simple implementation of {@link ManagedJob}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleManagedJob implements ManagedJob, SettingsChangeObserver, ServiceLifecycleObserver {

	private static final Logger LOG = LoggerFactory.getLogger(SimpleManagedTriggerAndJobDetail.class);

	/**
	 * The regular expression used to delegate properties to the delegate
	 * {@code settingSpecifierProvider}.
	 */
	public static final String JOB_DETAIL_PROPERTY_MAPPING_REGEX = "jobDetail\\.jobDataMap\\['([a-zA-Z0-9_]*)'\\](.*)";

	private static final KeyedSmartQuotedTemplateMapper MAPPER;
	static {
		MAPPER = new KeyedSmartQuotedTemplateMapper();
		MAPPER.setTemplate("jobDetail.jobDataMap['%s']");
	}

	private final JobService jobService;
	private SettingSpecifierProvider settingSpecifierProvider;
	private String name;
	private String triggerScheduleExpression;
	private MessageSource messageSource;
	private Map<String, SimpleServiceProviderConfiguration> serviceProviderConfigurations;

	private String simplePrefix;

	/**
	 * Constructor.
	 * 
	 * @param jobService
	 *        the job service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleManagedJob(JobService jobService) {
		super();
		this.jobService = ObjectUtils.requireNonNullArgument(jobService, "jobService");
	}

	@Override
	public JobService getJobService() {
		return jobService;
	}

	@Override
	public String toString() {
		return String.format("SimpleManagedJob{%s @ %s}", jobName(), getTriggerScheduleExpression());
	}

	public Trigger getTrigger() {
		final String expression = getTriggerScheduleExpression();
		if ( expression != null ) {
			try {
				try {
					long ms = Long.parseLong(expression);
					return new PeriodicTrigger(ms);
				} catch ( NumberFormatException e ) {
					// ignore
				}
				return new org.springframework.scheduling.support.CronTrigger(expression);
			} catch ( IllegalArgumentException e ) {
				LOG.warn("Error parsing cron expression [{}]: {}", expression, e.getMessage());
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
		return jobService.getDisplayName();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>();
		result.add(new BasicCronExpressionSettingSpecifier("triggerScheduleExpression",
				triggerScheduleExpression));
		for ( SettingSpecifier spec : getSettingSpecifierProvider().getSettingSpecifiers() ) {
			if ( spec instanceof MappableSpecifier ) {
				MappableSpecifier keyedSpec = (MappableSpecifier) spec;
				if ( simplePrefix != null ) {
					result.add(keyedSpec.mappedTo(simplePrefix));
				} else {
					result.add(keyedSpec.mappedWithMapper(MAPPER));
				}
			} else {
				result.add(spec);
			}
		}
		return result;
	}

	@Override
	public MessageSource getMessageSource() {
		if ( messageSource == null ) {
			TemplatedMessageSource tSource = new TemplatedMessageSource();
			tSource.setDelegate(getSettingSpecifierProvider().getMessageSource());
			tSource.setRegex(JOB_DETAIL_PROPERTY_MAPPING_REGEX);
			messageSource = tSource;
		}
		return messageSource;
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		SettingSpecifierProvider ssp = getSettingSpecifierProvider();
		if ( ssp instanceof SettingsChangeObserver ) {
			((SettingsChangeObserver) ssp).configurationChanged(properties);
		}
	}

	@Override
	public void serviceDidStartup() {
		if ( jobService instanceof ServiceLifecycleObserver ) {
			ServiceLifecycleObserver observer = (ServiceLifecycleObserver) jobService;
			try {
				observer.serviceDidStartup();
			} catch ( Exception e ) {
				LOG.error("Error delegating job {} lifecycle startup: {}", jobName(), e.toString(), e);
			}
		}
	}

	@Override
	public void serviceDidShutdown() {
		if ( jobService instanceof ServiceLifecycleObserver ) {
			ServiceLifecycleObserver observer = (ServiceLifecycleObserver) jobService;
			try {
				observer.serviceDidShutdown();
			} catch ( Exception e ) {
				LOG.error("Error delegating job {} lifecycle shutdown: {}", jobName(), e.toString(), e);
			}
		}
	}

	public SettingSpecifierProvider getSettingSpecifierProvider() {
		if ( settingSpecifierProvider != null ) {
			return settingSpecifierProvider;
		}

		// FIXME: this is not needed in this way anymore
		SettingSpecifierProvider ssp = jobService;
		PrefixedMessageSource pSource = new PrefixedMessageSource();
		String prefix = "jobService.";
		pSource.setPrefix(prefix);
		pSource.setDelegate(ssp.getMessageSource());
		messageSource = pSource;
		settingSpecifierProvider = ssp;
		simplePrefix = prefix;

		return settingSpecifierProvider;
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
					LOG.error("Error configuring job {} service provider {}: {}", jobName(), me.getKey(),
							e.toString());
				}
			}
		}
		return result;
	}

	public void setSettingSpecifierProvider(SettingSpecifierProvider settingSpecifierProvider) {
		this.settingSpecifierProvider = settingSpecifierProvider;
	}

	public void setServiceProviderConfigurations(
			Map<String, SimpleServiceProviderConfiguration> serviceProviderConfigurations) {
		this.serviceProviderConfigurations = serviceProviderConfigurations;
	}

	private String jobName() {
		String name = getName();
		if ( name == null || name.isEmpty() ) {
			name = String.format("%s job", jobService.getClass().getSimpleName());
		}
		return name;
	}

	/**
	 * Set the trigger schedule expression.
	 * 
	 * <p>
	 * This can be either an integer number representing a millisecond frequency
	 * or else a cron expression.
	 * </p>
	 * 
	 * @return the trigger schedule expression
	 */
	@Override
	public String getTriggerScheduleExpression() {
		return triggerScheduleExpression;
	}

	/**
	 * SEt the trigger schedule expression.
	 * 
	 * @param triggerScheduleExpression
	 *        the trigger schedule expression to set
	 */
	public void setTriggerScheduleExpression(String triggerScheduleExpression) {
		this.triggerScheduleExpression = triggerScheduleExpression;
	}

	/**
	 * Get the job name.
	 * 
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Set the job name.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

}
