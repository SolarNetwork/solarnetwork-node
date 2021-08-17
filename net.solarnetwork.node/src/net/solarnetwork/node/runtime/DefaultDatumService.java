/* ==================================================================
 * DefaultDatumService.java - 18/08/2021 7:37:07 AM
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

package net.solarnetwork.node.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.springframework.util.PathMatcher;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.DatumService;
import net.solarnetwork.node.domain.Datum;

/**
 * Default implementation of {@link DatumService}.
 * 
 * <p>
 * This service listens for {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
 * events to track datum.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultDatumService implements DatumService, EventHandler {

	/** The default history raw count. */
	public static final int DEFAFULT_HISTORY_RAW_COUNT = 5;

	private final PathMatcher pathMatcher;
	private DatumHistory history = new DatumHistory(
			new DatumHistory.Configuration(DEFAFULT_HISTORY_RAW_COUNT));

	/**
	 * Constructor.
	 * 
	 * @param pathMatcher
	 *        the path matcher to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DefaultDatumService(PathMatcher pathMatcher) {
		super();
		if ( pathMatcher == null ) {
			throw new IllegalArgumentException("The pathMatcher argument must not be null.");
		}
		this.pathMatcher = pathMatcher;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Datum> Collection<T> latest(Set<String> sourceIdFilter, Class<T> type) {
		List<T> result = new ArrayList<>();
		for ( Datum d : history.latest() ) {
			if ( !type.isAssignableFrom(d.getClass()) ) {
				continue;
			}
			if ( sourceIdFilter != null ) {
				for ( String filter : sourceIdFilter ) {
					if ( pathMatcher.match(filter, d.getSourceId()) ) {
						result.add((T) d);
						break;
					}
				}
			} else {
				result.add((T) d);
			}
		}
		return result;
	}

	@Override
	public void handleEvent(final Event event) {
		final String topic = event.getTopic();
		if ( !DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED.equals(topic) ) {
			return;
		}
		Object d = event.getProperty(Datum.DATUM_PROPERTY);
		if ( d instanceof Datum ) {
			history.add((Datum) d);
		}
	}

	/**
	 * Change the history raw count.
	 * 
	 * <p>
	 * Calling this resets the entire history.
	 * </p>
	 * 
	 * @param rawCount
	 *        the raw count to set
	 */
	public void setHistoryRawCount(int rawCount) {
		if ( rawCount < 1 ) {
			return;
		}
		history = new DatumHistory(new DatumHistory.Configuration(rawCount));
	}

}
