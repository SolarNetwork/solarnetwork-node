/* ==================================================================
 * RegisterBlockConfig.java - 17/09/2020 3:58:34 PM
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Configuration for a block of Modbus registers of a certain type (coil,
 * holding, etc.).
 *
 * <p>
 * The block starts at address {@link #getStartAddress()}, and then occupies as
 * many registers as specified by all the configured
 * {@link #getMeasurementConfigs()}.
 * </p>
 *
 * <p>
 * <b>Note</b> that the {@link ModbusRegisterBlockType#Coil} and
 * {@link ModbusRegisterBlockType#Discrete} imply each configured measurement
 * uses one on/off register.
 * </p>
 *
 * @author matt
 * @version 2.3
 */
public class RegisterBlockConfig {

	/** The default value for the {@code blockType} property. */
	public static final ModbusRegisterBlockType DEFAULT_BLOCK_TYPE = ModbusRegisterBlockType.Holding;

	/**
	 * A setting type pattern for a register block configuration element.
	 *
	 * <p>
	 * The pattern has two capture groups: the block configuration index and the
	 * property setting name.
	 * </p>
	 *
	 * @since 2.2
	 */
	public static final Pattern BLOCK_SETTING_PATTERN = Pattern
			.compile(".+".concat(Pattern.quote(".registerBlockConfigs[")).concat("(\\d+)\\]\\.(.*)"));

	private int startAddress;
	private ModbusRegisterBlockType blockType = DEFAULT_BLOCK_TYPE;
	private MeasurementConfig[] measurementConfigs;

	/**
	 * Constructor.
	 */
	public RegisterBlockConfig() {
		super();
	}

	/**
	 * Populate a setting as a configuration value, if possible.
	 *
	 * @param config
	 *        the overall configuration
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as a property
	 *         configuration value
	 * @since 2.2
	 */
	public static boolean populateFromSetting(UnitConfig config, Setting setting) {
		Matcher m = BLOCK_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		if ( idx >= config.getRegisterBlockConfigsCount() ) {
			config.setRegisterBlockConfigsCount(idx + 1);
		}
		RegisterBlockConfig blockConfig = config.getRegisterBlockConfigs()[idx];

		if ( MeasurementConfig.populateFromSetting(blockConfig, setting) ) {
			return true;
		}

		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "startAddress":
					blockConfig.setStartAddress(Integer.parseInt(val));
					break;
				case "blockTypeKey":
					blockConfig.setBlockTypeKey(val);
					break;
				default:
					// ignore
			}
		}
		return true;
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

		String info = registerInfo(messageSource);
		if ( info != null ) {
			result.add(new BasicTitleSettingSpecifier("addressInfo", info, true, messageSource != null));
		}

		result.add(new BasicTextFieldSettingSpecifier(prefix + "startAddress", "0"));

		// drop-down menu for block type
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "blockTypeKey", String.valueOf(DEFAULT_BLOCK_TYPE.getCode()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( ModbusRegisterBlockType e : ModbusRegisterBlockType.values() ) {
			propTypeTitles.put(String.valueOf(e.getCode()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		result.add(propTypeSpec);

		MeasurementConfig[] measConfs = getMeasurementConfigs();
		List<MeasurementConfig> measConfsList = (measConfs != null ? Arrays.asList(measConfs)
				: Collections.<MeasurementConfig> emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier(prefix + "measurementConfigs", measConfsList,
				new SettingUtils.KeyedListCallback<MeasurementConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(MeasurementConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								MeasurementConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return result;
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
	 * @param blockIdx
	 *        the block configuration index
	 * @return the settings
	 * @since 2.2
	 */
	public List<SettingValueBean> toSettingValues(String providerId, String instanceId, int unitIdx,
			int blockIdx) {
		List<SettingValueBean> settings = new ArrayList<>(2);
		addSetting(settings, providerId, instanceId, unitIdx, blockIdx, "blockTypeKey",
				getBlockTypeKey());
		addSetting(settings, providerId, instanceId, unitIdx, blockIdx, "startAddress",
				getStartAddress());
		addSetting(settings, providerId, instanceId, unitIdx, blockIdx, "measurementConfigsCount",
				getMeasurementConfigsCount());
		if ( measurementConfigs != null ) {
			int i = 0;
			for ( MeasurementConfig measConfig : measurementConfigs ) {
				settings.addAll(
						measConfig.toSettingValues(providerId, instanceId, unitIdx, blockIdx, i++));
			}
		}
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			int unitIdx, int blockIdx, String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId,
				format("unitConfigs[%d].registerBlockConfigs[%d].%s", unitIdx, blockIdx, key),
				val.toString()));
	}

	private String registerInfo(MessageSource messageSource) {
		MeasurementConfig[] configs = getMeasurementConfigs();
		if ( configs == null || configs.length < 1 ) {
			return null;
		}
		StringBuilder buf = new StringBuilder();
		int address = getStartAddress();
		if ( messageSource != null ) {
			buf.append(messageSource.getMessage("addressInfo.start", null, Locale.getDefault()));
		}
		for ( MeasurementConfig config : configs ) {
			if ( buf.length() > 0 ) {
				buf.append("\n");
			}
			int size = config.getSize();
			if ( messageSource != null ) {
				buf.append(messageSource.getMessage("addressInfo.row",
						new Object[] { String.format("0x%1$04x", address), address, size,
								config.getSourceId(), config.getPropertyName(), config.getDataType() },
						Locale.getDefault()));
			} else {
				buf.append(String.format("0x%1$04x %1$05d - %2$s.%3$s - %4$s (%5$d)", address,
						config.getSourceId(), config.getPropertyName(), config.getDataType(), size));
			}
			address += size;
		}
		if ( messageSource != null ) {
			buf.append(messageSource.getMessage("addressInfo.end", null, Locale.getDefault()));
		}
		return buf.toString();
	}

	/**
	 * Get the count of registers this block represents.
	 *
	 * @return the count of registers
	 */
	public int getBlockLength() {
		final MeasurementConfig[] configs = getMeasurementConfigs();
		if ( configs == null || configs.length == 0 ) {
			return 0;
		}
		if ( blockType.getBitCount() == 1 ) {
			return configs.length;
		}
		int count = 0;
		for ( MeasurementConfig c : configs ) {
			count += c.getSize();
		}
		return count;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RegisterBlockConfig{");
		if ( blockType != null ) {
			builder.append("blockType=");
			builder.append(blockType);
			builder.append(", ");
		}
		builder.append("startAddress=");
		builder.append(startAddress);
		builder.append(", ");
		if ( measurementConfigs != null ) {
			builder.append("measurementConfigs=");
			builder.append(Arrays.toString(measurementConfigs));
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the register block starting address.
	 *
	 * @return the starting address
	 */
	public int getStartAddress() {
		return startAddress;
	}

	/**
	 * Set the register block starting address.
	 *
	 * @param startAddress
	 *        the address to set; if &lt; {@literal 0} will be forced to
	 *        {@literal 0}
	 */
	public void setStartAddress(int startAddress) {
		if ( startAddress < 0 ) {
			startAddress = 0;
		}
		this.startAddress = startAddress;
	}

	/**
	 * Get the block type.
	 *
	 * @return the block type, never {@literal null}
	 */
	public ModbusRegisterBlockType getBlockType() {
		return blockType;
	}

	/**
	 * Set the block type.
	 *
	 * @param blockType
	 *        the type to set; if {@literal null} then will be forced to
	 *        {@link #DEFAULT_BLOCK_TYPE}
	 */
	public void setBlockType(ModbusRegisterBlockType blockType) {
		if ( blockType == null ) {
			blockType = DEFAULT_BLOCK_TYPE;
		}
		this.blockType = blockType;
	}

	/**
	 * Get the block type, as a key value.
	 *
	 * @return the block type key
	 */
	public String getBlockTypeKey() {
		return String.valueOf(blockType.getCode());
	}

	/**
	 * Set the block type, as a key value.
	 *
	 * @param key
	 *        the block type to set
	 */
	public void setBlockTypeKey(String key) {
		try {
			setBlockType(ModbusRegisterBlockType.forCode(Integer.valueOf(key)));
		} catch ( IllegalArgumentException | NullPointerException e ) {
			// ignore
		}
	}

	/**
	 * Get the measurement configurations.
	 *
	 * @return the measurement configurations
	 */
	public MeasurementConfig[] getMeasurementConfigs() {
		return measurementConfigs;
	}

	/**
	 * Set the measurement configurations to use.
	 *
	 * @param measurementConfigs
	 *        the configurations to use
	 */
	public void setMeasurementConfigs(MeasurementConfig[] measurementConfigs) {
		this.measurementConfigs = measurementConfigs;
	}

	/**
	 * Get the number of configured {@code measurementConfigs} elements.
	 *
	 * @return the number of {@code measurementConfigs} elements
	 */
	public int getMeasurementConfigsCount() {
		MeasurementConfig[] confs = this.measurementConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code MeasurementConfig} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link MeasurementConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code measurementConfigs} elements.
	 */
	public void setMeasurementConfigsCount(int count) {
		this.measurementConfigs = ArrayUtils.arrayWithLength(this.measurementConfigs, count,
				MeasurementConfig.class, null);
	}

}
