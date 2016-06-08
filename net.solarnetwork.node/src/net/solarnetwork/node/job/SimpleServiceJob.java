/* ==================================================================
 * SimpleServiceJob.java - 27/06/2015 1:30:49 pm
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

package net.solarnetwork.node.job;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.settings.MappableSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;
import org.springframework.context.MessageSource;

/**
 * Simple {@link StatefulJob} to execute {@link JobService#executeJobService()}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleServiceJob extends AbstractJob implements StatefulJob, SettingSpecifierProvider {

	private JobService service;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		service.executeJobService();
	}

	@Override
	public String getSettingUID() {
		return service.getSettingUID();
	}

	@Override
	public String getDisplayName() {
		return service.getDisplayName();
	}

	@Override
	public MessageSource getMessageSource() {
		return service.getMessageSource();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		for ( SettingSpecifier spec : service.getSettingSpecifiers() ) {
			if ( spec instanceof MappableSpecifier ) {
				MappableSpecifier keyedSpec = (MappableSpecifier) spec;
				result.add(keyedSpec.mappedTo("service."));
			} else {
				result.add(spec);
			}
		}
		return result;
	}

	public JobService getService() {
		return service;
	}

	public void setService(JobService service) {
		this.service = service;
	}

}
