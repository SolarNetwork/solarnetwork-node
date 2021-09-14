/* ==================================================================
 * CanbusMessageConfig.java - 29/09/2019 8:43:50 am
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Configuration for reading datum properties from a CAN bus message.
 * 
 * <p>
 * A single CAN bus message can contain one or more property values.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class CanbusMessageConfig {

	/** The default {@code address} property value. */
	public static final int DEFAULT_ADDRESS = 0;

	/** The default {@code byteOrdering} property value. */
	public static final ByteOrdering DEFAULT_BYTE_ORDERING = ByteOrdering.BigEndian;

	/** The default {@code interval} property value. */
	public static final int DEFAULT_INTERVAL = 60000;

	private int address = DEFAULT_ADDRESS;
	private String name;
	private ByteOrdering byteOrdering = DEFAULT_BYTE_ORDERING;
	private int interval;
	private CanbusPropertyConfig[] propConfigs;

	/**
	 * Constructor.
	 */
	public CanbusMessageConfig() {
		super();
		setPropConfigsCount(1);
	}

	/**
	 * Constructor.
	 * 
	 * @param address
	 *        the CAN address
	 * @param byteOrdering
	 *        the byte ordering
	 */
	public CanbusMessageConfig(int address, ByteOrdering byteOrdering) {
		super();
		setAddress(address);
		setByteOrdering(byteOrdering);
	}

	/**
	 * Add a new property configuration.
	 * 
	 * @param config
	 *        the configuration to add
	 * @return this object, to allow method chaining
	 */
	public CanbusMessageConfig addPropConfig(CanbusPropertyConfig config) {
		config.setParent(this);
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, getPropConfigsCount() + 1,
				CanbusPropertyConfig.class, new ObjectFactory<CanbusPropertyConfig>() {

					@Override
					public CanbusPropertyConfig getObject() throws BeansException {
						return config;
					}
				});
		return this;
	}

	/**
	 * Get settings suitable for configuring this instance.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "address", String.valueOf(DEFAULT_ADDRESS)));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "interval",
				String.valueOf(DEFAULT_INTERVAL)));

		// drop-down menu for byteOrderingCode
		BasicMultiValueSettingSpecifier byteOrderingSpec = new BasicMultiValueSettingSpecifier(
				prefix + "byteOrderingCode", String.valueOf(DEFAULT_BYTE_ORDERING.getCode()));
		Map<String, String> byteOrderingTitles = new LinkedHashMap<String, String>(3);
		for ( ByteOrdering e : ByteOrdering.values() ) {
			byteOrderingTitles.put(Character.toString(e.getCode()), e.getDescription());
		}
		byteOrderingSpec.setValueTitles(byteOrderingTitles);
		results.add(byteOrderingSpec);

		// property config list
		CanbusPropertyConfig[] confs = getPropConfigs();
		List<CanbusPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<CanbusPropertyConfig> emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier(prefix + "propConfigs", confsList,
				new SettingUtils.KeyedListCallback<CanbusPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(CanbusPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								value.settings(key + "."));
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
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "address",
				String.valueOf(address)));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "name", name));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "interval",
				String.valueOf(interval)));
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "byteOrderingCode",
				String.valueOf(byteOrdering.getCode())));

		int len = (propConfigs != null ? propConfigs.length : 0);
		settings.add(new SettingValueBean(providerId, instanceId, prefix + "propConfigsCount",
				String.valueOf(len)));
		if ( len > 0 ) {
			for ( int i = 0; i < len; i++ ) {
				settings.addAll(propConfigs[i].toSettingValues(providerId, instanceId,
						prefix + "propConfigs[" + i + "]."));
			}
		}
		return settings;
	}

	/**
	 * Get the CAN bus address to read data from.
	 * 
	 * @return the address; defaults to {@link #DEFAULT_ADDRESS}
	 */
	public int getAddress() {
		return address;
	}

	/**
	 * Set the CAN bus address to read data from.
	 * 
	 * @param address
	 *        the address to set
	 */
	public void setAddress(int address) {
		this.address = address;
	}

	/**
	 * Get a friendly name for the address.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the friendly name for the address.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the byte ordering.
	 * 
	 * @return the byte ordering, never {@literal null}; defaults to
	 *         {@link #DEFAULT_BYTE_ORDERING}
	 */
	public ByteOrdering getByteOrdering() {
		return byteOrdering;
	}

	/**
	 * Set the byte ordering.
	 * 
	 * @param byteOrdering
	 *        the byte ordering to set
	 */
	public void setByteOrdering(ByteOrdering byteOrdering) {
		this.byteOrdering = byteOrdering != null ? byteOrdering : DEFAULT_BYTE_ORDERING;
	}

	/**
	 * Get the byte ordering code.
	 * 
	 * <p>
	 * This returns the configured {@link #getByteOrdering()}
	 * {@link ByteOrdering#getCode()} value as a string.
	 * </p>
	 * 
	 * @return the byte ordering code
	 */
	public String getByteOrderingCode() {
		ByteOrdering order = getByteOrdering();
		return Character.toString(order.getCode());
	}

	/**
	 * Set the byte ordering via a code value.
	 * 
	 * <p>
	 * This uses the first character of {@code code} as a {@link ByteOrdering}
	 * code value to call {@link #setByteOrdering(ByteOrdering)}. If
	 * {@code code} is not valid, then {@link #DEFAULT_BYTE_ORDERING} will be
	 * set instead.
	 * </p>
	 * 
	 * @param code
	 *        the byte ordering code to set
	 */
	public void setByteOrderingCode(String code) {
		if ( code == null || code.length() < 1 ) {
			return;
		}
		try {
			setByteOrdering(ByteOrdering.forCode(code.charAt(0)));
		} catch ( IllegalArgumentException e ) {
			setByteOrdering(DEFAULT_BYTE_ORDERING);
		}
	}

	/**
	 * Get the property configurations.
	 * 
	 * @return the property configurations; never {@literal null}
	 */
	public CanbusPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Set the property configurations to use.
	 * 
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(CanbusPropertyConfig[] propConfigs) {
		if ( propConfigs == null || propConfigs.length < 1 ) {
			throw new IllegalArgumentException("At least one property configuration is required.");
		}
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		CanbusPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link CanbusPropertyConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count,
				CanbusPropertyConfig.class, null);
	}

	/**
	 * Get the minimum interval to limit message updates to.
	 * 
	 * @return the interval to subscribe to message updates, in milliseconds, or
	 *         {@literal 0} for no limit
	 */
	public int getInterval() {
		return interval;
	}

	/**
	 * Set the minimum interval to limit message updates to.
	 * 
	 * @param interval
	 *        the interval to subscribe to message updates, in milliseconds, or
	 *        {@literal 0} for no limit
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}

}
