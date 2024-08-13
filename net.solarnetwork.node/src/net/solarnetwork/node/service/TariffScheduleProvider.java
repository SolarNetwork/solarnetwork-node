/* ==================================================================
 * TariffScheduleProvider.java - 6/08/2024 6:47:38 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service;

import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.service.Identifiable;

/**
 * API for a service that provides a tariff schedule.
 *
 * @author matt
 * @version 1.0
 * @since 3.17
 */
public interface TariffScheduleProvider extends Identifiable {

	/**
	 * Get the tariff schedule offered by this service.
	 *
	 * @return the tariff schedule, or {@literal null} if not available
	 */
	TariffSchedule tariffSchedule();

}
