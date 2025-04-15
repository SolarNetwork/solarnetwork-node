/* ==================================================================
 * LocalStateInfo.java - 15/04/2025 11:32:35 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.support;

import java.math.BigInteger;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.node.domain.LocalState;
import net.solarnetwork.node.domain.LocalStateType;
import net.solarnetwork.util.NumberUtils;

/**
 * Local State information.
 *
 * @author matt
 * @version 1.0
 * @since 4.11
 */
public class LocalStateInfo {

	private String key;
	private String type;
	private String value;

	/**
	 * Constructor.
	 */
	public LocalStateInfo() {
		super();
	}

	/**
	 * Create a new {@link LocalState} entity from the information in this
	 * object.
	 *
	 * @return the new entity, or {@code null} if one cannot be created
	 */
	public LocalState toLocalState() {
		final String key = getKey();
		if ( key == null || key.isEmpty() ) {
			return null;
		}
		final LocalStateType type;
		final Object val;
		try {
			if ( this.type == null || this.type.isEmpty() ) {
				String valTmp = getValue();
				if ( valTmp != null ) {
					valTmp = valTmp.trim();
				}
				if ( valTmp == null || valTmp.isEmpty() || "true".equalsIgnoreCase(valTmp)
						|| "false".equalsIgnoreCase(valTmp) ) {
					type = LocalStateType.Boolean;
				} else if ( valTmp.startsWith("{") ) {
					type = LocalStateType.Mapping;
				} else {
					Number n = NumberUtils.parseNumber(valTmp);
					if ( n == null ) {
						type = LocalStateType.String;
					} else {
						if ( n instanceof Float ) {
							type = LocalStateType.Float32;
						} else if ( n instanceof Double ) {
							type = LocalStateType.Float64;
						} else if ( n instanceof Integer ) {
							type = LocalStateType.Int32;
						} else if ( n instanceof Long ) {
							type = LocalStateType.Int64;
						} else if ( n instanceof BigInteger ) {
							type = LocalStateType.Integer;
						} else {
							type = LocalStateType.Decimal;
						}
					}
				}
			} else {
				type = LocalStateType.forKey(getType());
			}
			if ( type == LocalStateType.Mapping ) {
				// convert to Map instance
				val = JsonUtils.getStringMap(getValue());
			} else {
				val = getValue();
			}
		} catch ( Exception e ) {
			// ignore
			return null;
		}

		return new LocalState(key, type, val);
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the key.
	 *
	 * @param key
	 *        the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type.
	 *
	 * @param type
	 *        the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the value.
	 *
	 * @param value
	 *        the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

}
