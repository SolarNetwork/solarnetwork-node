/* ==================================================================
 * StatAction.java - 13/08/2018 9:41:31 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.os.stat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enumeration of supported helper script action commands.
 * 
 * @author matt
 * @version 1.1
 */
public enum StatAction {

	/** Percentage CPU use. */
	CpuUse("cpu-use"),

	/** Percentage file system use. */
	FilesystemUse("fs-use"),

	/** Percentage memory use. */
	MemoryUse("mem-use"),

	/** Network use. */
	NetworkTraffic("net-traffic"),

	/** System load. */
	SystemLoad("sys-load"),

	/** System up time. */
	SystemUptime("sys-up");

	/**
	 * An unmodifiable set of all action names.
	 * 
	 * @since 1.1
	 */
	public static final Set<String> ALL_ACTIONS = createAllActions();

	private final String action;

	private StatAction(String action) {
		this.action = action;
	}

	private static Set<String> createAllActions() {
		Set<String> s = EnumSet.allOf(StatAction.class).stream().map(e -> e.getAction())
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return Collections.unmodifiableSet(s);
	}

	/**
	 * Get the action value.
	 * 
	 * @return the action value
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Get an enumeration for a given action value.
	 * 
	 * @param action
	 *        the action to get the enum value for
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code action} is not supported
	 */
	public static StatAction forAction(String action) {
		try {
			// try enum value first, for convenience
			return StatAction.valueOf(action);
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		for ( StatAction e : StatAction.values() ) {
			if ( action.equalsIgnoreCase(e.action) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Action [" + action + "] is not supported");
	}

}
