/* ==================================================================
 * DatumEvents.java - 8/04/2021 7:41:41 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.osgi.service.event.Event;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.Datum;

/**
 * Support for {@link Datum} {@link Event} handling.
 * 
 * @author matt
 * @version 1.0
 * @since 1.81
 */
public interface DatumEvents {

	/**
	 * Create an event with a datum.
	 * 
	 * @param topic
	 *        the event topic
	 * @param datum
	 *        the datum to add as the {@link Datum#DATUM_PROPERTY} event
	 *        property
	 * @return the new event instance
	 */
	static Event datumEvent(String topic, Datum datum) {
		return new Event(topic, Collections.singletonMap(Datum.DATUM_PROPERTY, datum));
	}

	/**
	 * Create a map out of event properties, unwrapping datum properties.
	 * 
	 * <p>
	 * If a {@link Datum} is found on the {@link Datum#DATUM_PROPERTY} event
	 * property, then the result of {@link Datum#asSimpleMap()} will be added to
	 * the returned map rather than the datum itself.
	 * </p>
	 * 
	 * @param event
	 *        the event to get a map of event properties for
	 * @return the event properties as a map, never {@literal null}
	 */
	static Map<String, Object> datumEventMap(Event event) {
		String[] propNames = event.getPropertyNames();
		Map<String, Object> map = new LinkedHashMap<String, Object>(propNames.length);
		for ( String propName : propNames ) {
			Object val = event.getProperty(propName);
			// if this is an event with a Datum property, unwrap the datum into top-level properties
			if ( Datum.DATUM_PROPERTY.equals(propName) && val instanceof Datum ) {
				map.putAll(((Datum) val).asSimpleMap());
			} else if ( val != null ) {
				map.put(propName, val);
			}
		}
		return map;
	}

	/**
	 * Create a new {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
	 * {@link Event} object out of a {@link Datum}.
	 * 
	 * <p>
	 * This method calls {@link #datumEvent(String, Datum)}.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to create the event for
	 * @return the new event instance
	 * @see DatumEvents#datumEvent(String, Datum)
	 */
	default Event datumCapturedEvent(Datum datum) {
		return datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, datum);
	}

}
