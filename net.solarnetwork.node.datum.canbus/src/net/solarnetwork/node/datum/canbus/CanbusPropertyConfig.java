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
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.NumberDatumSamplePropertyConfig;
import net.solarnetwork.node.io.canbus.CanbusSignalReference;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
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
 * @version 2.0
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
	private String normalizedUnit;
	private int bitLength = DEFAULT_BIT_LENGTH;
	private KeyValuePair[] localizedNames;
	private KeyValuePair[] valueLabels;

	/**
	 * Default constructor.
	 */
	public CanbusPropertyConfig() {
		super(null, DatumSamplesType.Instantaneous, DEFAULT_BIT_OFFSET);
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
	public CanbusPropertyConfig(String name, DatumSamplesType datumPropertyType, int bitOffset) {
		super(name, datumPropertyType, bitOffset);
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
	 * @param dataType
	 *        the data type
	 * @param bitLength
	 *        the bit length
	 * @param unit
	 *        the unit
	 * @param normalizedUnit
	 *        the normalized unit
	 */
	public CanbusPropertyConfig(String name, DatumSamplesType datumPropertyType, int bitOffset,
			BitDataType dataType, int bitLength, String unit, String normalizedUnit) {
		this(name, datumPropertyType, bitOffset);
		setDataType(dataType);
		setBitLength(bitLength);
		setUnit(unit);
		setNormalizedUnit(normalizedUnit);
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
		for ( DatumSamplesType e : DatumSamplesType.values() ) {
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
		results.add(new BasicTextFieldSettingSpecifier(prefix + "normalizedUnit", ""));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "bitOffset",
				String.valueOf(DEFAULT_BIT_OFFSET)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "bitLength",
				String.valueOf(DEFAULT_BIT_LENGTH)));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "slope", DEFAULT_SLOPE.toString()));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "intercept", DEFAULT_INTERCEPT.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));

		// value labels list
		KeyValuePair[] labels = getValueLabels();
		List<KeyValuePair> labelsList = (labels != null ? Arrays.asList(labels)
				: Collections.<KeyValuePair> emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier(prefix + "valueLabels", labelsList,
				new SettingUtils.KeyedListCallback<KeyValuePair>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(KeyValuePair value, int index,
							String key) {
						List<SettingSpecifier> labelSettings = new ArrayList<>(2);
						labelSettings.add(new BasicTextFieldSettingSpecifier(key + ".key", ""));
						labelSettings.add(new BasicTextFieldSettingSpecifier(key + ".value", ""));
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								labelSettings);
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		// localized names list
		KeyValuePair[] names = getLocalizedNames();
		List<KeyValuePair> namesList = (names != null ? Arrays.asList(names)
				: Collections.<KeyValuePair> emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier(prefix + "localizedNames", namesList,
				new SettingUtils.KeyedListCallback<KeyValuePair>() {

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
		if ( normalizedUnit != null && !normalizedUnit.isEmpty() ) {
			settings.add(new SettingValueBean(providerId, instanceId, prefix + "normalizedUnit",
					normalizedUnit));
		}
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

		KeyValuePair[] labels = getValueLabels();
		int len = (labels != null ? labels.length : 0);
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "valueLabelsCount",
				String.valueOf(len)));
		for ( int i = 0; i < len; i++ ) {
			settings.add(new SettingValueBean(providerId, instanceId,
					prefix + "valueLabels[" + i + "].key", labels[i].getKey()));
			settings.add(new SettingValueBean(providerId, instanceId,
					prefix + "valueLabels[" + i + "].value", labels[i].getValue()));
		}

		KeyValuePair[] names = getLocalizedNames();
		len = (names != null ? names.length : 0);
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
	 * @param key
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
	 * Get the desired normalized unit of the property value.
	 * 
	 * <p>
	 * This represents the desired normalized physical unit of the value, as
	 * unit term as described in
	 * <a href="http://unitsofmeasure.org/ucum.html">The Unified Code for Units
	 * of Measure</a>.
	 * </p>
	 * 
	 * @return the property normalized unit, or {@literal null} if a standard
	 *         normalization should be used
	 */
	public String getNormalizedUnit() {
		return normalizedUnit;
	}

	/**
	 * Set the desired normalized unit of the property value.
	 * 
	 * @param normalizedUnit
	 *        the normalized unit to use
	 * @see <a href="http://unitsofmeasure.org/ucum.html">The Unified Code for
	 *      Units of Measure</a>
	 */
	public void setNormalizedUnit(String normalizedUnit) {
		this.normalizedUnit = normalizedUnit;
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
	 * This is an alias for {@link #setConfig(Object)}.
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
		KeyValuePair[] names = getLocalizedNames();
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
		setLocalizedNames(
				ArrayUtils.arrayWithLength(this.localizedNames, count, KeyValuePair.class, null));
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

	/**
	 * Get the value labels.
	 * 
	 * <p>
	 * The {@code key} of each pair is a value. The {@code value} is the
	 * associated label to use for that value.
	 * </p>
	 * 
	 * @return the labels
	 * @since 1.1
	 */
	public KeyValuePair[] getValueLabels() {
		return valueLabels;
	}

	/**
	 * Set the value labels.
	 * 
	 * @param valueLabels
	 *        the labels to use
	 * @since 1.1
	 */
	public void setValueLabels(KeyValuePair[] valueLabels) {
		this.valueLabels = valueLabels;
	}

	/**
	 * Get the number of configured {@code valueLables} elements.
	 * 
	 * @return the number of {@code valueLables} elements
	 * @since 1.1
	 */
	public int getValueLabelsCount() {
		KeyValuePair[] labels = getValueLabels();
		return (labels == null ? 0 : labels.length);
	}

	/**
	 * Adjust the number of configured {@code valueLables} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new {@link KeyValuePair}
	 * instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code valueLables} elements.
	 * @since 1.1
	 */
	public void setValueLabelsCount(int count) {
		setValueLabels(ArrayUtils.arrayWithLength(getValueLabels(), count, KeyValuePair.class, null));
	}

	/**
	 * Get a mapping of values with associated labels.
	 * 
	 * <p>
	 * This turns the {@code valueLabels} array into a {@code Map} of values
	 * with associated labels. Duplicate values are ignored.
	 * </p>
	 * 
	 * @return a mapping of value labels, never {@literal null}
	 */
	public Map<String, String> getValueLabelsMap() {
		KeyValuePair[] names = getValueLabels();
		if ( names == null || names.length < 1 ) {
			return Collections.emptyMap();
		}
		return Arrays.stream(names)
				.collect(toMap(KeyValuePair::getKey, KeyValuePair::getValue, (l, r) -> l));
	}

	/**
	 * Store a value label mapping.
	 * 
	 * <p>
	 * No mapping will be stored if either {@code value} or {@code label} are
	 * {@literal null}.
	 * </p>
	 * 
	 * @param value
	 *        the value
	 * @param label
	 *        the label
	 * @since 1.1
	 */
	public void putValueLabel(Object value, String label) {
		if ( value == null || label == null ) {
			return;
		}
		KeyValuePair[] labels = getValueLabels();
		String valueString = value.toString();
		if ( labels != null ) {
			for ( KeyValuePair kv : labels ) {
				if ( valueString.equals(kv.getKey()) ) {
					kv.setValue(label);
					return;
				}
			}
		}
		KeyValuePair[] newLabels = new KeyValuePair[(labels != null ? labels.length : 0) + 1];
		if ( labels != null ) {
			System.arraycopy(labels, 0, newLabels, 0, labels.length);
		}
		KeyValuePair kv = new KeyValuePair(valueString, label);
		newLabels[newLabels.length - 1] = kv;
		setValueLabels(newLabels);
	}

}
