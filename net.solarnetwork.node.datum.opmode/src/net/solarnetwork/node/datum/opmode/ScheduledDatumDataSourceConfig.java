/* ==================================================================
 * ScheduledDatumDataSourceConfig.java - 20/12/2018 4:38:48 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.opmode;

import java.util.concurrent.ScheduledFuture;

/**
 * Runtime configuration for active data source configuration.
 * 
 * @author matt
 * @version 2.0
 */
public class ScheduledDatumDataSourceConfig {

	private final DatumDataSourceScheduleConfig config;
	private final ScheduledFuture<?> task;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *        the configuration
	 * @param trigger
	 *        the trigger
	 */
	public ScheduledDatumDataSourceConfig(DatumDataSourceScheduleConfig config,
			ScheduledFuture<?> trigger) {
		super();
		this.config = config;
		this.task = trigger;
	}

	public DatumDataSourceScheduleConfig getConfig() {
		return config;
	}

	public ScheduledFuture<?> getTask() {
		return task;
	}

}
