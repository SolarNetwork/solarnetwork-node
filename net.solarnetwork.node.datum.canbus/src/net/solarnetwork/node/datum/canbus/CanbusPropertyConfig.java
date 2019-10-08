/* ==================================================================
 * CanbusPropertyConfig.java - 25/09/2019 4:12:13 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.canbus;

import static java.util.stream.Collectors.toMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.NumberDatumSamplePropertyConfig;
import net.solarnetwork.node.io.canbus.CanbusSignalReference;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.util.ArrayUtils;

/**
 * Configuration for a single datum property to be set via a CAN bus message.
 * 
 * <p>
 * The {@link #getConfig()} value represents the CAN message bit offset to read
 * from.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class CanbusPropertyConfig extends NumberDatumSamplePropertyConfig<Integer>
		implements CanbusSignalReference {

	/** The default {@code bitLength} property value. */
	public static final int DEFAULT_BIT_LENGTH = 32;

	/** The default {@code bitOffset} property value. */
	public static final int DEFAULT_BIT_OFFSET = 0;

	/** The default {@code dataType} property value. */
	public static final BitDataType DEFAULT_DATA_TYPE = BitDataType.Int32;

	private CanbusMessageConfig parent;

	private BitDataType dataType = DEFAULT_DATA_TYPE;
	private String unit;
	private int bitLength = DEFAULT_BIT_LENGTH;
	private KeyValuePair[] localizedNames;

	/**
	 * Default constructor.
	 */
	public CanbusPropertyConfig() {
		super(null, GeneralDatumSamplesType.Instantaneous, DEFAULT_BIT_OFFSET);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param bitOffset
	 *        the CAN message bit offset
	 */
	public CanbusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType, int bitOffset) {
		super(name, datumPropertyType, bitOffset);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyKey", ""));

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", String.valueOf(DEFAULT_PROPERTY_TYPE.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( GeneralDatumSamplesType e : GeneralDatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		// drop-down menu for dataType
		BasicMultiValueSettingSpecifier dataTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "dataTypeKey", DEFAULT_DATA_TYPE.getKey());
		Map<String, String> dataTypeTitles = new LinkedHashMap<String, String>(3);
		for ( BitDataType e : BitDataType.values() ) {
			dataTypeTitles.put(e.getKey(), e.getDescription());
		}
		dataTypeSpec.setValueTitles(dataTypeTitles);
		results.add(dataTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "unit", ""));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "bitOffset",
				String.valueOf(DEFAULT_BIT_OFFSET)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "bitLength",
				String.valueOf(DEFAULT_BIT_LENGTH)));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "slope", DEFAULT_SLOPE.toString()));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "intercept", DEFAULT_INTERCEPT.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		// localized names list
		KeyValuePair[] names = getLocalizedNames();
		List<KeyValuePair> namesList = (names != null ? Arrays.asList(names)
				: Collections.<KeyValuePair> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier(prefix + "localizedNames", namesList,
				new SettingsUtil.KeyedListCallback<KeyValuePair>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(KeyValuePair value, int index,
							String key) {
						List<SettingSpecifier> nameSettings = new ArrayList<>(2);
						nameSettings.add(new BasicTextFieldSettingSpecifier(key + ".key", ""));
						nameSettings.add(new BasicTextFieldSettingSpecifier(key + ".value", ""));
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								nameSettings);
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return results;
	}

	/**
	 * Generate a list of setting values from this instance.
	 * 
	 * @param providerId
	 *        the setting provider key to use
	 * @param instanceId
	 *        the setting provider instance key to use
	 * @param prefix
	 *        a prefix to append to all setting keys
	 * @return the list of setting values, never {@literal null}
	 */
	public List<SettingValueBean> toSettingValues(String providerId, String instanceId, String prefix) {
		List<SettingValueBean> settings = new ArrayList<>(16);

		settings.add(
				new SettingValueBean(providerId, instanceId, prefix + "propertyKey", getPropertyKey()));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "propertyTypeKey",
				getPropertyTypeKey()));
		settings.add(
				new SettingValueBean(providerId, instanceId, prefix + "dataTypeKey", getDataTypeKey()));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "unit", unit));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "bitOffset",
				String.valueOf(getBitOffset())));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "bitLength",
				String.valueOf(bitLength)));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "slope",
				getSlope().toPlainString()));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "intercept",
				getIntercept().toPlainString()));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "decimalScale",
				String.valueOf(getDecimalScale())));

		KeyValuePair[] names = getLocalizedNames();
		int len = (names != null ? names.length : 0);
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "localizedNamesCount",
				String.valueOf(len)));
		for ( int i = 0; i < len; i++ ) {
			settings.add(new SettingValueBean(providerId, instanceId,
					prefix + "localizedNames[" + i + "].key", names[i].getKey()));
			settings.add(new SettingValueBean(providerId, instanceId,
					prefix + "localizedNames[" + i + "].value", names[i].getValue()));
		}

		return settings;
	}

	@Override
	public int getAddress() {
		CanbusMessageConfig p = getParent();
		return (p != null ? p.getAddress() : -1);
	}

	@Override
	public ByteOrdering getByteOrdering() {
		CanbusMessageConfig p = getParent();
		return (p != null ? p.getByteOrdering() : CanbusMessageConfig.DEFAULT_BYTE_ORDERING);
	}

	/**
	 * Get the data type.
	 * 
	 * @return the type, never {@literal null}; defaults to
	 *         {@link #DEFAULT_DATA_TYPE}
	 */
	@Override
	public BitDataType getDataType() {
		return dataType;
	}

	/**
	 * Set the data type.
	 * 
	 * @param dataType
	 *        the type to set
	 */
	public void setDataType(BitDataType dataType) {
		if ( dataType == null ) {
			dataType = DEFAULT_DATA_TYPE;
		}
		this.dataType = dataType;
	}

	/**
	 * Get the data type as a key value.
	 * 
	 * @return the type as a key
	 */
	public String getDataTypeKey() {
		BitDataType type = getDataType();
		return (type != null ? type.getKey() : null);
	}

	/**
	 * Set the data type as a string value.
	 * 
	 * @param dataType
	 *        the type to set
	 */
	public void setDataTypeKey(String key) {
		try {
			setDataType(BitDataType.forKey(key));
		} catch ( IllegalArgumentException e ) {
			setDataType(DEFAULT_DATA_TYPE);
		}
	}

	/**
	 * Get the unit of the property value.
	 * 
	 * <p>
	 * This represents the physical unit of the value, as unit term as described
	 * in <a href="http://unitsofmeasure.org/ucum.html">The Unified Code for
	 * Units of Measure</a>.
	 * </p>
	 * 
	 * @return the property unit, or {@literal null} if not known
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Set the unit of the property value.
	 * 
	 * @param unit
	 *        the unit to use
	 * @see <a href="http://unitsofmeasure.org/ucum.html">The Unified Code for
	 *      Units of Measure</a>
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * Get the offset of the least significant bit of the property value,
	 * relative to the least significant bit of the full message data value.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}, returning {@literal 0} if that
	 * returns {@literal null}.
	 * </p>
	 * 
	 * @return the bit offset; defaults to {@link #DEFAULT_BIT_OFFSET}
	 */
	@Override
	public int getBitOffset() {
		Integer c = getConfig();
		return (c != null ? c.intValue() : DEFAULT_BIT_OFFSET);
	}

	/**
	 * Set the offset of the least significant bit of the property value,
	 * relative to the least significant bit of the full message data value.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(Integer)}.
	 * </p>
	 * 
	 * @param bitOffset
	 *        the bit offset to use
	 */
	public void setBitOffset(int bitOffset) {
		setConfig(bitOffset);
	}

	/**
	 * Get the length of the property value within the full message data value.
	 * 
	 * @return the bit length; defaults to {@link #DEFAULT_BIT_LENGTH}
	 */
	@Override
	public int getBitLength() {
		return bitLength;
	}

	/**
	 * Set the length of the property value within the full message data value.
	 * 
	 * @param bitLength
	 *        the bit length to use
	 */
	public void setBitLength(int bitLength) {
		this.bitLength = bitLength;
	}

	/**
	 * Get the localized names.
	 * 
	 * <p>
	 * The {@code key} of each pair is an IETF BCP 47 language tag. The
	 * {@code value} is the associated name in the given language.
	 * </p>
	 * 
	 * @return the localized names
	 */
	public KeyValuePair[] getLocalizedNames() {
		return localizedNames;
	}

	/**
	 * Set the localized names to use.
	 * 
	 * @param localizedNames
	 *        the names to use
	 */
	public void setLocalizedNames(KeyValuePair[] localizedNames) {
		this.localizedNames = localizedNames;
	}

	/**
	 * Get the number of configured {@code localizedNames} elements.
	 * 
	 * @return the number of {@code localizedNames} elements
	 */
	public int getLocalizedNamesCount() {
		KeyValuePair[] names = this.localizedNames;
		return (names == null ? 0 : names.length);
	}

	/**
	 * Adjust the number of configured {@code localizedNames} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new {@link KeyValuePair}
	 * instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code localizedNames} elements.
	 */
	public void setLocalizedNamesCount(int count) {
		this.localizedNames = ArrayUtils.arrayWithLength(this.localizedNames, count, KeyValuePair.class,
				null);
	}

	/**
	 * Get a mapping of language codes to associated localized name values.
	 * 
	 * <p>
	 * This turns the {@code localizedNames} array into a {@code Map} of
	 * language codes with associated name values. Duplicate language codes are
	 * ignored.
	 * </p>
	 * 
	 * @return a mapping of localized names, never {@literal null}
	 */
	public Map<String, String> getLocalizedNamesMap() {
		KeyValuePair[] names = getLocalizedNames();
		if ( names == null || names.length < 1 ) {
			return Collections.emptyMap();
		}
		return Arrays.stream(names)
				.collect(toMap(KeyValuePair::getKey, KeyValuePair::getValue, (l, r) -> l));
	}

	/**
	 * Get the parent message.
	 * 
	 * @return the parent the parent message
	 */
	public CanbusMessageConfig getParent() {
		return parent;
	}

	/**
	 * Set the parent message.
	 * 
	 * <p>
	 * Configuring the parent message allows this instance to function as a
	 * {@link CanbusSignalReference}.
	 * </p>
	 * 
	 * @param parent
	 *        the parent to set
	 */
	public void setParent(CanbusMessageConfig parent) {
		this.parent = parent;
	}

}
