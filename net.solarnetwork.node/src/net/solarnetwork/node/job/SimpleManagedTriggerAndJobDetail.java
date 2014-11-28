/* ==================================================================
 * SimpleManagedTriggerAndJobDetail.java - Jul 21, 2013 1:08:03 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
import java.util.Set;
import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.KeyedSmartQuotedTemplateMapper;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.solarnetwork.node.util.TemplatedMessageSource;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.context.MessageSource;

/**
 * Extension of {@link SimpleTriggerAndJobDetail} that supports a
 * {@link SettingSpecifierProvider} to manage the job at runtime.
 * 
 * <p>
 * There are two basic ways to configure this class. The first way involves
 * configuring the {@code settingSpecifierProvider} property manually. In this
 * case the keyed settings and message keys will be dynamically adjusted to work
 * with a {@code jobDetail.jobDataMap['%s']} style pattern, where the {@code %s}
 * will be replaced by the top-level property names of all keys.
 * </p>
 * 
 * <p>
 * The second way involves leaving the {@code settingSpecifierProvider} not
 * configured, and letting this class find the first available
 * {@link SettingSpecifierProvider} instance in the configured
 * {@link JobDetail#getJobDataMap()}. In this case the keyed settings provided
 * by that {@link SettingSpecifierProvider} and all message keys will be
 * prefixed with {@code jobDetail.jobDataMap['$mapKey'].} where {@code $mapKey}
 * is the associated key of that object in the JobDataMap.
 * </p>
 * 
 * <p>
 * In most situations the later approach is simplest to set up.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl>
 * <dt>settingSpecifierProvider</dt>
 * <dd>The {@link SettingSpecifierProvider} that this class proxies all methods
 * for. If not configured, then {@link JobDetail#getJobDataMap()} will be
 * examined and the first value that implements {@link SettingSpecifierProvider}
 * will be used.</dd>
 * 
 * <dt>trigger</dt>
 * <dd>The job trigger.</dd>
 * 
 * <dt>jobDetail</dt>
 * <dd>The job detail.</dd>
 * 
 * <dt>serviceProviderConfigurations</dt>
 * <dd>An optional mapping of {@code jobDetail} keys to associated
 * {@link SimpleServiceProviderConfiguration} objects. The object on the given
 * key will be extracted from the {@code jobDetail} map and that object will be
 * returned as a {@link ServiceProvider.ServiceConfiguration} instance when
 * {@link #getServiceConfigurations()} is called. This allows services used by
 * the job to be exposed as services themselves in the runtime.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.2
 */
public class SimpleManagedTriggerAndJobDetail implements ManagedTriggerAndJobDetail, ServiceProvider {

	/**
	 * The regular expression used to delegate properties to the delegate
	 * {@code settingSpecifierProvider}.
	 */
	public static final String JOB_DETAIL_PROPERTY_MAPPING_REGEX = "jobDetail\\.jobDataMap\\['([a-zA-Z0-9_]*)'\\](.*)";

	private static final KeyedSmartQuotedTemplateMapper MAPPER = getMapper();

	private SettingSpecifierProvider settingSpecifierProvider;
	private Trigger trigger;
	private JobDetail jobDetail;
	private MessageSource messageSource;
	private String simplePrefix;
	private Map<String, SimpleServiceProviderConfiguration> serviceProviderConfigurations;

	private static KeyedSmartQuotedTemplateMapper getMapper() {
		KeyedSmartQuotedTemplateMapper result = new KeyedSmartQuotedTemplateMapper();
		result.setTemplate("jobDetail.jobDataMap['%s']");
		return result;
	}

	@Override
	public String toString() {
		return "ManagedTriggerAndJobDetail{job=" + jobDetail.getName() + ",trigger=" + trigger.getName()
				+ '}';
	}

	@Override
	public Trigger getTrigger() {
		return trigger;
	}

	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}

	@Override
	public JobDetail getJobDetail() {
		return jobDetail;
	}

	public void setJobDetail(JobDetail jobDetail) {
		this.jobDetail = jobDetail;
	}

	@Override
	public String getSettingUID() {
		return getSettingSpecifierProvider().getSettingUID();
	}

	@Override
	public String getDisplayName() {
		return getSettingSpecifierProvider().getDisplayName();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		if ( trigger instanceof CronTrigger ) {
			CronTrigger ct = (CronTrigger) trigger;
			result.add(new BasicTextFieldSettingSpecifier("trigger.cronExpression", ct
					.getCronExpression()));
		}
		for ( SettingSpecifier spec : getSettingSpecifierProvider().getSettingSpecifiers() ) {
			if ( spec instanceof KeyedSettingSpecifier<?> ) {
				KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
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

	@SuppressWarnings("unchecked")
	public SettingSpecifierProvider getSettingSpecifierProvider() {
		if ( settingSpecifierProvider != null ) {
			return settingSpecifierProvider;
		}
		for ( Map.Entry<String, Object> me : (Set<Map.Entry<String, Object>>) jobDetail.getJobDataMap()
				.entrySet() ) {
			Object o = me.getValue();
			if ( o instanceof SettingSpecifierProvider ) {
				SettingSpecifierProvider ssp = (SettingSpecifierProvider) o;
				PrefixedMessageSource pSource = new PrefixedMessageSource();
				String prefix = "jobDetail.jobDataMap['" + me.getKey() + "'].";
				pSource.setPrefix(prefix);
				pSource.setDelegate(ssp.getMessageSource());
				messageSource = pSource;
				settingSpecifierProvider = ssp;
				simplePrefix = prefix;
				break;
			}
		}
		return settingSpecifierProvider;
	}

	@Override
	public Collection<ServiceConfiguration> getServiceConfigurations() {
		Collection<ServiceConfiguration> result = null;
		if ( jobDetail != null && serviceProviderConfigurations != null
				&& serviceProviderConfigurations.size() > 0 ) {
			for ( Map.Entry<String, SimpleServiceProviderConfiguration> me : serviceProviderConfigurations
					.entrySet() ) {
				Object o = jobDetail.getJobDataMap().get(me.getKey());
				if ( o != null ) {
					SimpleServiceProviderConfiguration conf = me.getValue();
					SimpleServiceProviderConfiguration ser = new SimpleServiceProviderConfiguration();
					ser.setService(o);
					ser.setInterfaces(conf.getInterfaces());
					ser.setProperties(conf.getProperties());
					if ( result == null ) {
						result = new ArrayList<ServiceProvider.ServiceConfiguration>();
					}
					result.add(ser);
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

}
