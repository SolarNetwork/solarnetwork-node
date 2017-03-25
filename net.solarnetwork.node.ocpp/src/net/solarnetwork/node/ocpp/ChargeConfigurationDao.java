/* ==================================================================
 * ChargeConfigurationDao.java - 25/03/2017 11:49:59 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp;

/**
 * DAO for a global singleton {@link ChargeConfiguration} entity.
 * 
 * @author matt
 * @version 1.0
 * @since 0.6
 */
public interface ChargeConfigurationDao {

	/**
	 * The EventAdmin topic used to post events when the charge configuration
	 * has been updated.
	 */
	String EVENT_TOPIC_CHARGE_CONFIGURATION_UPDATED = "net/solarnetwork/node/ocpp/CHARGE_CONF_UPDATED";

	/**
	 * Store (create or update) a the charge configuration.
	 * 
	 * @param config
	 *        The {@link ChargeConfiguration} to store.
	 */
	void storeChargeConfiguration(ChargeConfiguration config);

	/**
	 * Get the {@link ChargeConfiguration}.
	 * 
	 * @return The charge configuration, or <em>null</em> if not available.
	 */
	ChargeConfiguration getChargeConfiguration();

}
