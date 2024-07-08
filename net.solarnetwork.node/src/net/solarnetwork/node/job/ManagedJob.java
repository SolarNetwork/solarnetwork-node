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

import java.util.Collection;
import java.util.Collections;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * API for a managed job.
 *
 * <p>
 * A managed job is {@link JobService} that needs to be performed on a schedule,
 * such as a repeating frequency or a cron schedule. The actual execution of the
 * task is handled externally; this API represents only the information needed
 * for some external service to execute the task. At runtime it is expected for
 * managed job instances to be detected and scheduled for execution.
 * </p>
 *
 * <p>
 * Managed jobs must provide a unique ID for each job, via the inherited
 * {@link Identifiable#getUid()} method. Additionally the job must provide a
 * settings UID via {@link SettingSpecifierProvider#getSettingUid()}.
 * </p>
 *
 * <p>
 * Additional services related to the job can be defined via the
 * {@link ServiceProvider} API. Those services are expected to registered with
 * the system runtime by the external service the job is scheduled by.
 * </p>
 *
 * @author matt
 * @version 1.1
 * @since 2.0
 */
public interface ManagedJob extends Identifiable, ServiceProvider, SettingSpecifierProvider {

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
	String getSchedule();

	/**
	 * Get the schedule setting key.
	 *
	 * <p>
	 * This defaults to {@literal schedule} to match the {@link #getSchedule()}
	 * property. However this can be changed to any setting, so that the
	 * schedule setting key can be different. This can be used to bundle
	 * multiple job schedules into the same setting UID, using different
	 * schedule setting keys for different jobs.
	 * </p>
	 *
	 * @return the schedule setting key, defaults to {@literal schedule}
	 */
	default String getScheduleSettingKey() {
		return "schedule";
	}

	/**
	 * Get a collection of service configurations.
	 *
	 * @return A collection of configuration objects.
	 */
	@Override
	default Collection<ServiceConfiguration> getServiceConfigurations() {
		return Collections.emptyList();
	}

	@Override
	default public <T> T unwrap(Class<T> type) {
		T result = SettingSpecifierProvider.super.unwrap(type);
		if ( result == null ) {
			JobService js = getJobService();
			if ( js != null ) {
				result = js.unwrap(type);
			}
		}
		return result;
	}

}
