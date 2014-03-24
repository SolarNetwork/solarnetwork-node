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
import java.util.List;
import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.KeyedSmartQuotedTemplateMapper;
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
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl>
 * <dt>settingSpecifierProvider</dt>
 * <dd>The {@link SettingSpecifierProvider} that this class proxies all methods
 * for.</dd>
 * <dt>trigger</dt>
 * <dd>The job trigger.</dd>
 * <dt>jobDetail</dt>
 * <dd>The job detail.</dd>
 * <dt>messageSource</dt>
 * <dd>A {@link MessageSource} to use for
 * {@link SettingSpecifierProvider#getMessageSource()}. If not configured, and a
 * {@code settingSpecifierProvider} is configured, then a
 * {@link TemplatedMessageSource} will automatically be created using that
 * provider as its delegate and a mapping regular expression of
 * {@code jobDetail\.jobDataMap\['([a-zA-Z0-9_]*)'\](.*)}. This essentially
 * allows the delegate provider to configure properties as if they were directly
 * named after its own settings, without needing to wrap each key with
 * {@code jobDetail.jobDataMap[...]}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 */
public class SimpleManagedTriggerAndJobDetail implements ManagedTriggerAndJobDetail {

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
		return settingSpecifierProvider.getSettingUID();
	}

	@Override
	public String getDisplayName() {
		return settingSpecifierProvider.getDisplayName();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		if ( trigger instanceof CronTrigger ) {
			CronTrigger ct = (CronTrigger) trigger;
			result.add(new BasicTextFieldSettingSpecifier("trigger.cronExpression", ct
					.getCronExpression()));
		}
		for ( SettingSpecifier spec : settingSpecifierProvider.getSettingSpecifiers() ) {
			if ( spec instanceof KeyedSettingSpecifier<?> ) {
				KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
				result.add(keyedSpec.mappedWithMapper(MAPPER));
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
			tSource.setDelegate(settingSpecifierProvider.getMessageSource());
			tSource.setRegex(JOB_DETAIL_PROPERTY_MAPPING_REGEX);
			messageSource = tSource;
		}
		return messageSource;
	}

	public SettingSpecifierProvider getSettingSpecifierProvider() {
		return settingSpecifierProvider;
	}

	public void setSettingSpecifierProvider(SettingSpecifierProvider settingSpecifierProvider) {
		this.settingSpecifierProvider = settingSpecifierProvider;
	}

}
