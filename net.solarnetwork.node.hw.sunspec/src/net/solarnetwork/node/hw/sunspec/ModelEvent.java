/* ==================================================================
 * ModelEvent.java - 22/05/2018 6:09:33 AM
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

package net.solarnetwork.node.hw.sunspec;

import java.util.Set;

/**
 * API for a model event.
 * 
 * @author matt
 * @version 1.1
 */
public interface ModelEvent {

	/**
	 * Get the event bitmask index.
	 * 
	 * @return the bitmask index
	 */
	int getIndex();

	/**
	 * Get a description of the event.
	 * 
	 * @return a description
	 */
	String getDescription();

	/**
	 * Get a SunSpec "bitfield16" value from a set of events.
	 * 
	 * @param events
	 *        the events to get the bit field value for; can be {@literal null}
	 * @return the bit field value
	 * @since 1.1
	 */
	static int bitField16Value(Set<ModelEvent> events) {
		return (int) (bitField32Value(events) & 0xFFFF);
	}

	/**
	 * Get a SunSpec "bitfield32" value from a set of events.
	 * 
	 * @param events
	 *        the events to get the bit field value for; can be {@literal null}
	 * @return the bit field value
	 * @since 1.1
	 */
	static long bitField32Value(Set<ModelEvent> events) {
		long b = 0;
		if ( events != null ) {
			for ( ModelEvent event : events ) {
				int idx = event.getIndex();
				b |= (1 << idx);
			}
		}
		return b;
	}

}
