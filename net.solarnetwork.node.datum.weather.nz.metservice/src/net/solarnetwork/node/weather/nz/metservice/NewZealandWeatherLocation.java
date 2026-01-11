/* ==================================================================
 * NewZealandWeatherLocation.java - 28/05/2016 7:48:16 am
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.nz.metservice;

import java.io.Serial;
import net.solarnetwork.domain.SimpleLocation;

/**
 * NewZealand specific weather location info.
 *
 * @author matt
 * @version 1.2
 */
public class NewZealandWeatherLocation extends SimpleLocation
		implements Comparable<NewZealandWeatherLocation> {

	@Serial
	private static final long serialVersionUID = -3320716966519243496L;

	/** The island name. */
	private String island;

	/** The key. */
	private String key;

	/**
	 * Constructor.
	 */
	public NewZealandWeatherLocation() {
		super();
	}

	/**
	 * Set the island this location is on.
	 *
	 * @param island
	 *        The island to set.
	 */
	public void setIsland(String island) {
		this.island = island;
	}

	/**
	 * Get a unique key for this location.
	 *
	 * @return The key.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set a unique key for this location.
	 *
	 * @param key
	 *        The key to set.
	 */
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		NewZealandWeatherLocation other = (NewZealandWeatherLocation) obj;
		if ( key == null ) {
			if ( other.key != null ) {
				return false;
			}
		} else if ( !key.equalsIgnoreCase(other.key) ) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(NewZealandWeatherLocation o) {
		// sort first by island
		if ( island == null && o.island != null ) {
			return -1;
		} else if ( island != null && o.island == null ) {
			return 1;
		}
		int order = (island == o.island ? 0 : island.compareToIgnoreCase(o.island));
		if ( order != 0 ) {
			return order;
		}

		// island the same, sort next by name

		String n1 = getName();
		String n2 = o.getName();
		if ( n1 == null && n2 != null ) {
			return -1;
		} else if ( n1 != null && n2 == null ) {
			return 1;
		}
		order = (n1 == n2 ? 0 : n1.compareToIgnoreCase(n2));
		return order;
	}

}
