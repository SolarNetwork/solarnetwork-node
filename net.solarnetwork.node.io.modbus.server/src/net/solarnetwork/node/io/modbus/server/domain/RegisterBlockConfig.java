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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
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
 * <b>Note that the {@link RegisterBlockType#Coil} and
 * {@link RegisterBlockType#Discrete} imply each configured measurement uses one
 * on/off register.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class RegisterBlockConfig {

	/** The default value for the {@code blockType} property. */
	public static final RegisterBlockType DEFAULT_BLOCK_TYPE = RegisterBlockType.Holding;

	private int startAddress;
	private RegisterBlockType blockType = DEFAULT_BLOCK_TYPE;
	private MeasurementConfig[] measurementConfigs;

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> result = new ArrayList<>(6);

		result.add(new BasicTextFieldSettingSpecifier(prefix + "startAddress", "0"));

		// drop-down menu for block type
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "blockTypeKey", String.valueOf(DEFAULT_BLOCK_TYPE.getCode()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( RegisterBlockType e : RegisterBlockType.values() ) {
			propTypeTitles.put(String.valueOf(e.getCode()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		result.add(propTypeSpec);

		MeasurementConfig[] measConfs = getMeasurementConfigs();
		List<MeasurementConfig> measConfsList = (measConfs != null ? Arrays.asList(measConfs)
				: Collections.<MeasurementConfig> emptyList());
		result.add(SettingsUtil.dynamicListSettingSpecifier(prefix + "measurementConfigs", measConfsList,
				new SettingsUtil.KeyedListCallback<MeasurementConfig>() {

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
	public RegisterBlockType getBlockType() {
		return blockType;
	}

	/**
	 * Set the block type.
	 * 
	 * @param blockType
	 *        the type to set; if {@literal null} then will be forced to
	 *        {@link #DEFAULT_BLOCK_TYPE}
	 */
	public void setBlockType(RegisterBlockType blockType) {
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
			setBlockType(RegisterBlockType.forCode(Integer.valueOf(key)));
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
