/* ==================================================================
 * UnitConfig.java - 17/09/2020 3:59:10 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.util.ArrayUtils;

/**
 * Configuration for a single Modbus unit.
 * 
 * @author matt
 * @version 1.0
 */
public class UnitConfig {

	private int unitId;
	private RegisterBlockConfig[] registerBlockConfigs;

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> result = new ArrayList<>(6);

		result.add(new BasicTextFieldSettingSpecifier(prefix + "unitId", "0"));

		RegisterBlockConfig[] blockConfs = getRegisterBlockConfigs();
		List<RegisterBlockConfig> blockConfsList = (blockConfs != null ? Arrays.asList(blockConfs)
				: Collections.emptyList());
		result.add(SettingsUtil.dynamicListSettingSpecifier(prefix + "registerBlockConfigs",
				blockConfsList, new SettingsUtil.KeyedListCallback<RegisterBlockConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(RegisterBlockConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								value.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Get the unit ID.
	 * 
	 * @return the unit ID
	 */
	public int getUnitId() {
		return unitId;
	}

	/**
	 * Set the unit ID.
	 * 
	 * @param unitId
	 *        the unit ID to set
	 */
	public void setUnitId(int unitId) {
		this.unitId = unitId;
	}

	/**
	 * Get the register block configurations.
	 * 
	 * @return the register block configurations
	 */
	public RegisterBlockConfig[] getRegisterBlockConfigs() {
		return registerBlockConfigs;
	}

	/**
	 * Set the register block configurations to use.
	 * 
	 * @param registerBlockConfigs
	 *        the configurations to use
	 */
	public void setRegisterBlockConfigs(RegisterBlockConfig[] registerBlockConfigs) {
		this.registerBlockConfigs = registerBlockConfigs;
	}

	/**
	 * Get the number of configured {@code registerBlockConfigs} elements.
	 * 
	 * @return the number of {@code registerBlockConfigs} elements
	 */
	public int getRegisterBlockConfigsCount() {
		RegisterBlockConfig[] confs = this.registerBlockConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code RegisterBlockConfig} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link RegisterBlockConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code registerBlockConfigs} elements.
	 */
	public void setRegisterBlockConfigsCount(int count) {
		this.registerBlockConfigs = ArrayUtils.arrayWithLength(this.registerBlockConfigs, count,
				RegisterBlockConfig.class, null);
	}

}
