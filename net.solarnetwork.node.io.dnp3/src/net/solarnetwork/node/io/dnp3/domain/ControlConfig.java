/* ==================================================================
 * ControlConfig.java - 22/02/2019 5:21:41 pm
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

package net.solarnetwork.node.io.dnp3.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A configuration for a DNP3 control integration with a
 * {@link net.solarnetwork.node.service.NodeControlProvider} control value.
 *
 * <p>
 * This configuration maps a control value to a DNP3 measurement.
 * </p>
 *
 * @author matt
 * @version 2.1
 */
public class ControlConfig {

	/** The default control type. */
	public static final ControlType DEFAULT_TYPE = ControlType.Analog;

	private String controlProviderUid;
	private String controlId;
	private ControlType type = DEFAULT_TYPE;

	/**
	 * Default constructor.
	 */
	public ControlConfig() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param controlProviderUid
	 *        the
	 *        {@link net.solarnetwork.node.service.NodeControlProvider#getUid()}
	 *        to collect from
	 * @param controlId
	 *        the control ID a
	 *        {@link net.solarnetwork.domain.NodeControlInfo#getControlId()} to
	 *        collect from
	 * @param type
	 *        the DNP3 control type
	 */
	public ControlConfig(String controlProviderUid, String controlId, ControlType type) {
		super();
		this.controlProviderUid = controlProviderUid;
		this.controlId = controlId;
		this.type = type;
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(3);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "controlProviderUid", "", false,
				"(objectClass=net.solarnetwork.node.service.NodeControlProvider)"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "controlId", ""));

		// drop-down menu for control type
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "typeKey", Character.toString(DEFAULT_TYPE.getCode()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( ControlType e : ControlType.values() ) {
			propTypeTitles.put(Character.toString(e.getCode()), e.getTitle());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		return results;
	}

	/**
	 * Get the control provider UID.
	 *
	 * @return the UID
	 */
	public String getControlProviderUid() {
		return controlProviderUid;
	}

	/**
	 * Set the control provider UID.
	 *
	 * @param dataSourceUid
	 *        the UID to set
	 */
	public void setControlProviderUid(String dataSourceUid) {
		this.controlProviderUid = dataSourceUid;
	}

	/**
	 * Get the control ID.
	 *
	 * @return the ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 *
	 * @param controlId
	 *        the ID to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the control type.
	 *
	 * @return the type
	 */
	public ControlType getType() {
		return type;
	}

	/**
	 * Set the control type.
	 *
	 * @param type
	 *        the type to set
	 */
	public void setType(ControlType type) {
		this.type = type;
	}

	/**
	 * Get the control type key.
	 *
	 * <p>
	 * This returns the configured {@link #getType()}
	 * {@link ControlType#getCode()} value as a string. If the type is not
	 * available, {@link #DEFAULT_TYPE} will be returned.
	 * </p>
	 *
	 * @return the control type key
	 */
	public String getTypeKey() {
		ControlType type = getType();
		if ( type == null ) {
			type = DEFAULT_TYPE;
		}
		return Character.toString(type.getCode());
	}

	/**
	 * Set the control type via a key value.
	 *
	 * <p>
	 * This uses the first character of {@code key} as a {@link ControlType}
	 * code value to call {@link #setType(ControlType)}. If there is any problem
	 * parsing the type, {@link #DEFAULT_TYPE} is set.
	 * </p>
	 *
	 * @param key
	 *        the control type key to set
	 */
	public void setTypeKey(String key) {
		ControlType type = null;
		if ( key != null && key.length() > 0 ) {
			try {
				type = ControlType.forCode(key.charAt(0));
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		if ( type == null ) {
			type = DEFAULT_TYPE;
		}
		setType(type);
	}

}
