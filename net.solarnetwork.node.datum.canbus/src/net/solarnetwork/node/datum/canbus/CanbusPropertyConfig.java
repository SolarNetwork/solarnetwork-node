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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.NumberDatumSamplePropertyConfig;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property to be set via a CAN bus address.
 * 
 * <p>
 * The {@link #getConfig()} value represents the CAN bus address to read from.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class CanbusPropertyConfig extends NumberDatumSamplePropertyConfig<Integer> {

	/** The default {@code address} property value. */
	public static final int DEFAULT_ADDRESS = 0;

	private String name;

	/**
	 * Default constructor.
	 */
	public CanbusPropertyConfig() {
		super(null, GeneralDatumSamplesType.Instantaneous, DEFAULT_ADDRESS);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param address
	 *        the CAN bus address
	 */
	public CanbusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType, int address) {
		super(name, datumPropertyType, address);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));
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

		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "address", String.valueOf(DEFAULT_ADDRESS)));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "unitIntercept",
				DEFAULT_INTERCEPT.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "unitSlope", DEFAULT_SLOPE.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "slope", DEFAULT_SLOPE.toString()));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "intercept", DEFAULT_INTERCEPT.toString()));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));
		return results;
	}

	/**
	 * Get the register address to start reading data from.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}, returning {@literal 0} if that
	 * returns {@literal null}.
	 * </p>
	 * 
	 * @return the register address
	 */
	public int getAddress() {
		Integer addr = getConfig();
		return (addr != null ? addr : 0);
	}

	/**
	 * Set the register address to start reading data from.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(Integer)}.
	 * </p>
	 * 
	 * @param address
	 *        the register address to set
	 */
	public void setAddress(int address) {
		setConfig(address);
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

}
