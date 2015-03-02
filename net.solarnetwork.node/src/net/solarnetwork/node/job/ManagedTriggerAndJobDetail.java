/* ==================================================================
 * ManagedTriggerAndJobDetail.java - Jul 22, 2013 6:53:49 AM
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

import net.solarnetwork.node.settings.SettingSpecifierProvider;
import org.quartz.JobDetail;
import org.quartz.Trigger;

/**
 * A bean that combines a trigger and a job, designed to be managed via
 * settings.
 * 
 * @author matt
 * @version 1.1
 */
public interface ManagedTriggerAndJobDetail extends SettingSpecifierProvider, ServiceProvider {

	/**
	 * Get the Trigger.
	 * 
	 * @return the trigger
	 */
	Trigger getTrigger();

	/**
	 * Get the JobDetail.
	 * 
	 * @return the jobDetail
	 */
	JobDetail getJobDetail();

}
