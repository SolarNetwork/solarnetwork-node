/* ==================================================================
 * PowerComponents.java - 9/08/2019 1:30:58 pm
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

package net.solarnetwork.node.control.esi.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Components of power.
 *
 * @author matt
 * @version 2.0
 */
public class PowerComponents {

	private Long realPower;
	private Long reactivePower;

	/**
	 * Default constructor.
	 */
	public PowerComponents() {
		super();
	}

	/**
	 * Construct with values.
	 *
	 * @param realPower
	 *        the real power, in watts (W)
	 * @param reactivePower
	 *        the reactive power, in in volt-amps-reactive (VAR)
	 */
	public PowerComponents(Long realPower, Long reactivePower) {
		super();
		this.realPower = realPower;
		this.reactivePower = reactivePower;
	}

	/**
	 * Add settings for this class to a list.
	 *
	 * @param prefix
	 *        an optional prefix to use for all setting keys
	 * @param results
	 *        the list to add settings to
	 */
	public static void addSettings(String prefix, List<SettingSpecifier> results) {
		if ( prefix == null ) {
			prefix = "";
		}
		results.add(new BasicTextFieldSettingSpecifier(prefix + "realPower", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "reactivePower", ""));
	}

	/**
	 * Get the power components data as a Map.
	 *
	 * @return a map of the properties of this class
	 */
	public Map<String, Object> asMap() {
		Map<String, Object> map = new LinkedHashMap<>(8);
		map.put("realPower", getRealPower());
		map.put("reactivePower", getReactivePower());
		return map;
	}

	/**
	 * Create a copy of this instance.
	 *
	 * @return the new copy
	 */
	public PowerComponents copy() {
		PowerComponents c = new PowerComponents();
		c.setRealPower(getRealPower());
		c.setReactivePower(getReactivePower());
		return c;
	}

	@Override
	public int hashCode() {
		return Objects.hash(reactivePower, realPower);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof PowerComponents) ) {
			return false;
		}
		PowerComponents other = (PowerComponents) obj;
		return Objects.equals(reactivePower, other.reactivePower)
				&& Objects.equals(realPower, other.realPower);
	}

	@Override
	public String toString() {
		return "PowerComponents{realPower=" + realPower + ", reactivePower=" + reactivePower + "}";
	}

	/**
	 * Derive a simple apparent power value from the configured real and
	 * reactive power values.
	 *
	 * @return the apparent power
	 */
	public double derivedApparentPower() {
		double p = realPower != null ? realPower.doubleValue() : 0.0;
		double q = reactivePower != null ? reactivePower.doubleValue() : 0.0;
		return Math.sqrt(p * p + q * q);
	}

	/**
	 * Get the real power.
	 *
	 * @return the real power, in watts (W), or {@literal null} if not available
	 */
	public Long getRealPower() {
		return realPower;
	}

	/**
	 * Set the real power.
	 *
	 * @param realPower
	 *        the power to set, in watts (W)
	 */
	public void setRealPower(Long realPower) {
		this.realPower = realPower;
	}

	/**
	 * Get the reactive power.
	 *
	 * @return the reactive power, in volt-amps-reactive (VAR), or
	 *         {@literal null} if not available
	 */
	public Long getReactivePower() {
		return reactivePower;
	}

	/**
	 * Set the reactive power.
	 *
	 * @param reactivePower
	 *        the power to set, in volt-amps-reactive (VAR)
	 */
	public void setReactivePower(Long reactivePower) {
		this.reactivePower = reactivePower;
	}

}
