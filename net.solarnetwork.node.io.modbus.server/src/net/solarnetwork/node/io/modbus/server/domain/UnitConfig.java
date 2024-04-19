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

import static java.lang.String.format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Configuration for a single Modbus unit.
 *
 * @author matt
 * @version 2.3
 */
public class UnitConfig {

	/**
	 * A setting type pattern for a unit configuration element.
	 *
	 * <p>
	 * The pattern has two capture groups: the unit configuration index and the
	 * property setting name.
	 * </p>
	 *
	 * @since 2.2
	 */
	public static final Pattern UNIT_SETTING_PATTERN = Pattern
			.compile(Pattern.quote("unitConfigs[").concat("(\\d+)\\]\\.(.*)"));

	private int unitId;
	private RegisterBlockConfig[] registerBlockConfigs;

	/**
	 * Constructor.
	 */
	public UnitConfig() {
		super();
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public List<SettingSpecifier> settings(String prefix) {
		return settings(prefix, null);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @param messageSource
	 *        the message source to use, or {@literal null}
	 * @return the settings, never {@literal null}
	 * @since 2.1
	 */
	public List<SettingSpecifier> settings(String prefix, MessageSource messageSource) {
		List<SettingSpecifier> result = new ArrayList<>(6);

		result.add(new BasicTextFieldSettingSpecifier(prefix + "unitId", "0"));

		RegisterBlockConfig[] blockConfs = getRegisterBlockConfigs();
		List<RegisterBlockConfig> blockConfsList = (blockConfs != null ? Arrays.asList(blockConfs)
				: Collections.emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier(prefix + "registerBlockConfigs",
				blockConfsList, new SettingUtils.KeyedListCallback<RegisterBlockConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(RegisterBlockConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								value.settings(key + ".", messageSource));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Populate a setting as a property configuration value, if possible.
	 *
	 * @param config
	 *        the overall configuration
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as a property
	 *         configuration value
	 * @since 2.1
	 */
	public static boolean populateFromSetting(ModbusServerConfig config, Setting setting) {
		Matcher m = UNIT_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		List<UnitConfig> unitConfigs = config.getUnitConfigs();
		if ( !(idx < unitConfigs.size()) ) {
			unitConfigs.add(idx, new UnitConfig());
		}
		UnitConfig unitConfig = unitConfigs.get(idx);

		if ( RegisterBlockConfig.populateFromSetting(unitConfig, setting) ) {
			return true;
		}

		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "unitId":
					unitConfig.setUnitId(Integer.valueOf(val));
					break;
				default:
					// ignore
			}
		}
		return true;
	}

	/**
	 * Generate a list of setting values.
	 *
	 * @param providerId
	 *        the setting provider ID
	 * @param instanceId
	 *        the factory instance ID
	 * @param unitIdx
	 *        the unit configuration index
	 * @return the settings
	 * @since 2.2
	 */
	public List<SettingValueBean> toSettingValues(String providerId, String instanceId, int unitIdx) {
		List<SettingValueBean> settings = new ArrayList<>(2);
		addSetting(settings, providerId, instanceId, unitIdx, "unitId", getUnitId());
		addSetting(settings, providerId, instanceId, unitIdx, "registerBlockConfigsCount",
				getRegisterBlockConfigsCount());
		if ( registerBlockConfigs != null ) {
			int i = 0;
			for ( RegisterBlockConfig blockConfig : registerBlockConfigs ) {
				settings.addAll(blockConfig.toSettingValues(providerId, instanceId, unitIdx, i++));
			}
		}
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			int i, String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId, format("unitConfigs[%d].%s", i, key),
				val.toString()));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UnitConfig{unitId=");
		builder.append(unitId);
		builder.append(", ");
		if ( registerBlockConfigs != null ) {
			builder.append("registerBlockConfigs=");
			builder.append(Arrays.toString(registerBlockConfigs));
		}
		builder.append("}");
		return builder.toString();
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
