/* ==================================================================
 * DatumDataSourceInvokerJob.java - 20/12/2018 2:25:11 PM
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;

/**
 * Job to execute a single datum data source schedule configuration.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumDataSourceInvokerJob implements Runnable {

	private final DatumDataSourceScheduleService service;
	private final DatumDataSourceScheduleConfig config;

	public DatumDataSourceInvokerJob(DatumDataSourceScheduleService service,
			DatumDataSourceScheduleConfig config) {
		super();
		this.service = requireNonNullArgument(service, "service");
		this.config = requireNonNullArgument(config, "config");
	}

	@Override
	public void run() {
		service.invokeScheduleConfig(config);
	}

}
