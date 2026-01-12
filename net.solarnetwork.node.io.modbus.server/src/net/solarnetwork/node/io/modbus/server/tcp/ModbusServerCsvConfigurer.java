/* ==================================================================
 * ModbusServerCsvConfigurer.java - 9/03/2022 11:48:43 AM
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.tcp;

import net.solarnetwork.node.io.modbus.server.impl.BaseModbusServerCsvConfigurer;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.service.OptionalService;

/**
 * Service that can configure {@link ModbusServer} instances via CSV resources.
 *
 * @author matt
 * @version 1.2
 */
public class ModbusServerCsvConfigurer extends BaseModbusServerCsvConfigurer {

	/**
	 * Constructor.
	 *
	 * @param settingsService
	 *        the settings service
	 * @param identityService
	 *        the identity service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusServerCsvConfigurer(SettingsService settingsService,
			OptionalService<IdentityService> identityService) {
		super(settingsService, identityService, ModbusServer.SETTING_UID, "tcp");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.modbus.server.csv";
	}

	@Override
	public String getDisplayName() {
		return "Modbus Server (TCP) CSV Configurer";
	}

}
