/* ==================================================================
 * MetricEventDataExtractor.java - 17/07/2024 3:59:07 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.metrics.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.osgi.service.event.Event;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.node.metrics.domain.Metric;

/**
 * Function to extract event data from a Metric entity event.
 *
 * @author matt
 * @version 1.0
 */
public final class MetricEventDataExtractor implements Function<Event, Map<String, ?>> {

	/**
	 * Constructor.
	 */
	public MetricEventDataExtractor() {
		super();
	}

	@Override
	public Map<String, ?> apply(Event event) {
		if ( event == null ) {
			return null;
		}
		Object entity = event.getProperty(GenericDao.ENTITY_EVENT_ENTITY_PROPERTY);
		if ( entity instanceof Metric ) {
			Metric m = (Metric) entity;
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put("timestamp", m.getTimestamp());
			data.put("type", m.getType());
			data.put("name", m.getName());
			data.put("value", m.getValue());
			return data;
		}
		return null;
	}

}
