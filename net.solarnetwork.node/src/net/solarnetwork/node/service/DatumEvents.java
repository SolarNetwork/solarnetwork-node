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

package net.solarnetwork.node.service;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.MutableDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.util.ClassUtils;

/**
 * Support for {@link NodeDatum} {@link Event} handling.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public final class DatumEvents {

	private static final Logger log = LoggerFactory.getLogger(DatumEvents.class);

	private DatumEvents() {
		// can't construct me
	}

	/**
	 * A property name for a {@code Datum} instance associated with an event.
	 * 
	 * @since 2.0
	 */
	public static final String DATUM_PROPERTY = "_Datum";

	/**
	 * Create an event with a datum class and property map.
	 * 
	 * @param topic
	 *        the event topic
	 * @param clazz
	 *        the datum class
	 * @param datumMap
	 *        the datum properties
	 * @return the new event, or {@literal null} if {@code datumMap} is
	 *         {@literal null} or empty
	 */
	public static Event datumEvent(String topic, Class<? extends Datum> clazz, Map<String, ?> datumMap) {
		if ( datumMap == null || datumMap.isEmpty() ) {
			return null;
		}
		Map<String, Object> props = new LinkedHashMap<>(datumMap.size());
		for ( Entry<String, ?> me : datumMap.entrySet() ) {
			String k = me.getKey();
			Object o = datumMap.get(k);
			if ( o instanceof Map<?, ?> ) {
				for ( Entry<?, ?> e : ((Map<?, ?>) o).entrySet() ) {
					props.put(e.getKey().toString(), e.getValue());
				}
			} else {
				props.put(k, o);
			}
		}
		String[] types = DatumEvents.datumTypes(clazz);
		if ( types != null && types.length > 0 ) {
			props.put(Datum.DATUM_TYPE_PROPERTY, types[0]);
			props.put(Datum.DATUM_TYPES_PROPERTY, types);
		}
		if ( log.isTraceEnabled() ) {
			log.trace("Created {} event with props {}", topic, props);
		}
		return new Event(topic, props);
	}

	/**
	 * Create an event with a datum.
	 * 
	 * @param topic
	 *        the event topic
	 * @param datum
	 *        the datum to add as the {@link NodeDatum#DATUM_PROPERTY} event
	 *        property
	 * @return the new event instance
	 */
	public static Event datumEvent(String topic, NodeDatum datum) {
		if ( datum == null ) {
			return new Event(topic, Collections.emptyMap());
		}
		Map<String, Object> props;
		String[] datumTypes = datumTypes(datum.getClass());
		if ( datumTypes != null && datumTypes.length > 0 ) {
			props = new LinkedHashMap<>(3);
			props.put(NodeDatum.DATUM_TYPE_PROPERTY, datumTypes[0]);
			props.put(NodeDatum.DATUM_TYPES_PROPERTY, datumTypes);
			props.put(DATUM_PROPERTY, datum);
		} else {
			props = Collections.singletonMap(DATUM_PROPERTY, datum);
		}
		if ( log.isTraceEnabled() ) {
			log.trace("Created {} event with props {}", topic, props);
		}
		return new Event(topic, props);
	}

	/**
	 * Create a map out of event properties, unwrapping datum properties.
	 * 
	 * <p>
	 * If a {@link NodeDatum} is found on the {@link NodeDatum#DATUM_PROPERTY}
	 * event property, then the result of {@link NodeDatum#asSimpleMap()} will
	 * be added to the returned map rather than the datum itself.
	 * </p>
	 * 
	 * @param event
	 *        the event to get a map of event properties for
	 * @return the event properties as a map, never {@literal null}
	 */
	public static Map<String, Object> datumEventMap(Event event) {
		String[] propNames = event.getPropertyNames();
		Map<String, Object> map = new LinkedHashMap<String, Object>(propNames.length);
		for ( String propName : propNames ) {
			Object val = event.getProperty(propName);
			// if this is an event with a Datum property, unwrap the datum into top-level properties
			if ( DATUM_PROPERTY.equals(propName) && val instanceof NodeDatum ) {
				map.putAll(((NodeDatum) val).asSimpleMap());
			} else if ( val != null ) {
				map.put(propName, val);
			}
		}
		return map;
	}

	/**
	 * Create a new {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
	 * {@link Event} object out of a {@link NodeDatum}.
	 * 
	 * <p>
	 * This method calls {@link #datumEvent(String, NodeDatum)}.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to create the event for
	 * @return the new event instance
	 * @see DatumEvents#datumEvent(String, NodeDatum)
	 */
	public static Event datumCapturedEvent(NodeDatum datum) {
		return datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, datum);
	}

	/**
	 * A cache of datum type mappings.
	 * 
	 * <p>
	 * The {@link #datumTypes(Class)} method populates this cache.
	 * </p>
	 */
	private static final ConcurrentMap<Class<?>, String[]> DATUM_TYPE_CACHE = new ConcurrentHashMap<Class<?>, String[]>();

	/**
	 * Get an array of datum types for a class.
	 * 
	 * <p>
	 * This method caches the results for performance.
	 * </p>
	 * 
	 * @param clazz
	 *        the datum class to get the types for
	 * @return the types
	 */
	public static String[] datumTypes(Class<?> clazz) {
		String[] result = DATUM_TYPE_CACHE.get(clazz);
		if ( result != null ) {
			return result;
		}
		Set<Class<?>> interfaces = ClassUtils.getAllNonJavaInterfacesForClassAsSet(clazz);

		// remove all but Datum extensions, excluding MutableDatum
		for ( Iterator<Class<?>> itr = interfaces.iterator(); itr.hasNext(); ) {
			Class<?> c = itr.next();
			if ( !Datum.class.isAssignableFrom(c) || MutableDatum.class.isAssignableFrom(c) ) {
				itr.remove();
			}
		}

		result = new String[interfaces.size()];
		int i = 0;
		for ( Class<?> intf : interfaces ) {
			result[i] = intf.getName();
			i++;
		}
		DATUM_TYPE_CACHE.putIfAbsent(clazz, result);
		return result;
	}

}
