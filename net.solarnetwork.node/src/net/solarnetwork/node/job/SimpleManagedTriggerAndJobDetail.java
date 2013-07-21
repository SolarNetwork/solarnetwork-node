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
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleManagedTriggerAndJobDetail implements ManagedTriggerAndJobDetail {

	private SettingSpecifierProvider settingSpecifierProvider;
	private Trigger trigger;
	private JobDetail jobDetail;
	private MessageSource messageSource;

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
				result.add(keyedSpec.mappedWithPlaceholer("jobDetail.jobDataMap['%s']"));
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
			tSource.setRegex("jobDetail\\.jobDataMap\\['(.*)'\\](.*)");
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
