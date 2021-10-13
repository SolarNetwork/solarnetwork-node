/* ==================================================================
 * ManagedJob.java - 13/10/2021 4:10:47 PM
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

import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * API for a managed job.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface ManagedJob extends SettingSpecifierProvider, ServiceProvider {

	/**
	 * Get a name for this job.
	 * 
	 * @return the job name
	 */
	String getName();

	/**
	 * Get the JobDetail.
	 * 
	 * @return the jobDetail
	 */
	JobService getJobService();

	/**
	 * Get the desired trigger schedule expression.
	 * 
	 * <p>
	 * This might be a cron expression or a plain number representing a
	 * millisecond period.
	 * </p>
	 * 
	 * @return the schedule expression, never {@literal null}
	 */
	String getTriggerScheduleExpression();

}
