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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.springframework.util.PathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumHistorian;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.DatumQueueProcessObserver;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Default implementation of {@link DatumService}.
 *
 * <p>
 * This service listens for {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
 * events to track datum.
 * </p>
 *
 * @author matt
 * @version 3.0
 */
public class DefaultDatumService
		implements DatumService, EventHandler, InstructionHandler, DatumQueueProcessObserver {

	/** The service name to retrieve the latest datum. */
	public static final String SETUP_SERVICE_LATEST_DATUM = "/setup/datum/latest";

	/** The default history raw count. */
	public static final int DEFAFULT_HISTORY_RAW_COUNT = 5;

	private final PathMatcher pathMatcher;
	private final ObjectMapper objectMapper;
	private final OptionalService<DatumMetadataService> datumMetadataService;
	private final InMemoryHistorian history = new InMemoryHistorian();
	private final InMemoryHistorian unfiltered = new InMemoryHistorian();

	/**
	 * Constructor.
	 *
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param objectMapper
	 *        the object mapper to use
	 * @param datumMetadataService
	 *        the datum metadata service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DefaultDatumService(PathMatcher pathMatcher, ObjectMapper objectMapper,
			OptionalService<DatumMetadataService> datumMetadataService) {
		super();
		this.pathMatcher = requireNonNullArgument(pathMatcher, "pathMatcher");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.datumMetadataService = requireNonNullArgument(datumMetadataService, "datumMetadataService");
	}

	private class InMemoryHistorian implements DatumHistorian {

		private DatumHistory history = new DatumHistory(
				new DatumHistory.Configuration(DEFAFULT_HISTORY_RAW_COUNT));

		@Override
		public <T extends NodeDatum> Collection<T> latest(Set<String> sourceIdFilter, Class<T> type) {
			return offset(sourceIdFilter, 0, type);
		}

		@Override
		public <T extends NodeDatum> T latest(String sourceId, Class<T> type) {
			return offset(sourceId, 0, type);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends NodeDatum> T offset(String sourceId, int offset, Class<T> type) {
			NodeDatum result = history.offset(sourceId, offset);
			return (result != null && type.isAssignableFrom(result.getClass()) ? (T) result : null);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends NodeDatum> Collection<T> offset(Set<String> sourceIdFilter, int offset,
				Class<T> type) {
			List<T> result = new ArrayList<>();
			for ( NodeDatum d : history.offset(offset) ) {
				if ( !type.isAssignableFrom(d.getClass()) ) {
					continue;
				}
				if ( sourceIdFilter != null && !sourceIdFilter.isEmpty() ) {
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

		@SuppressWarnings("unchecked")
		@Override
		public <T extends NodeDatum> Collection<T> offset(Set<String> sourceIdFilter, Instant timestamp,
				int offset, Class<T> type) {
			List<T> result = new ArrayList<>();
			for ( NodeDatum d : history.offset(timestamp, offset) ) {
				if ( !type.isAssignableFrom(d.getClass()) ) {
					continue;
				}
				if ( sourceIdFilter != null && !sourceIdFilter.isEmpty() ) {
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

		@SuppressWarnings("unchecked")
		@Override
		public <T extends NodeDatum> T offset(String sourceId, Instant timestamp, int offset,
				Class<T> type) {
			NodeDatum result = history.offset(sourceId, timestamp, offset);
			return (result != null && type.isAssignableFrom(result.getClass()) ? (T) result : null);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends NodeDatum> Collection<T> slice(String sourceId, int offset, int count,
				Class<T> type) {
			List<T> result = new ArrayList<>(history.getConfig().getRawCount());
			for ( NodeDatum d : history.slice(sourceId, offset, count) ) {
				if ( type == null || type.isAssignableFrom(d.getClass()) ) {
					result.add((T) d);
				}
			}
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends NodeDatum> Collection<T> slice(String sourceId, Instant timestamp, int offset,
				int count, Class<T> type) {
			List<T> result = new ArrayList<>(history.getConfig().getRawCount());
			for ( NodeDatum d : history.slice(sourceId, timestamp, offset, count) ) {
				if ( type == null || type.isAssignableFrom(d.getClass()) ) {
					result.add((T) d);
				}
			}
			return result;
		}

		/**
		 * Add a datum to the history.
		 *
		 * @param datum
		 *        the datum
		 */
		private void add(NodeDatum datum) {
			history.add(datum);
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
		private void setHistoryRawCount(int rawCount) {
			if ( rawCount < 1 ) {
				return;
			}
			history = new DatumHistory(new DatumHistory.Configuration(rawCount));
		}
	}

	@Override
	public void datumQueueWillProcess(DatumQueue queue, NodeDatum datum, Stage stage, boolean persist) {
		assert datum != null;
		assert stage != null;
		switch (stage) {
			case PreFilter:
				unfiltered.add(datum);
				break;

			case PostFilter:
				history.add(datum);
				break;
		}
	}

	@Override
	public void handleEvent(final Event event) {
		final String topic = event.getTopic();
		if ( !DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED.equals(topic) ) {
			return;
		}
		Object d = event.getProperty(DatumEvents.DATUM_PROPERTY);
		if ( d instanceof NodeDatum ) {
			history.add((NodeDatum) d);
		}
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		final String topic = instruction.getParameterValue(InstructionHandler.PARAM_SERVICE);
		if ( !SETUP_SERVICE_LATEST_DATUM.equals(topic) ) {
			return null;
		}
		Set<String> sourceIdFilters = null;
		String serviceIdFilterParam = instruction.getParameterValue(PARAM_SERVICE_ARGUMENT);
		if ( serviceIdFilterParam != null && !serviceIdFilterParam.isEmpty() ) {
			// try as JSON array first
			try {
				String[] sourceIds = objectMapper.readValue(serviceIdFilterParam, String[].class);
				if ( sourceIds != null && sourceIds.length > 0 ) {
					sourceIdFilters = new LinkedHashSet<>(Arrays.asList(sourceIds));
				}
			} catch ( IOException e ) {
				// ignore, and try as plain delimited list
				sourceIdFilters = StringUtils.commaDelimitedStringToSet(serviceIdFilterParam);
			}
		}
		Collection<NodeDatum> latest = latest(sourceIdFilters, NodeDatum.class);
		return InstructionUtils.createStatus(instruction, InstructionState.Completed, Instant.now(),
				Collections.singletonMap(InstructionHandler.PARAM_SERVICE_RESULT, latest));
	}

	@Override
	public <T extends NodeDatum> Collection<T> latest(Set<String> sourceIdFilter, Class<T> type) {
		return history.latest(sourceIdFilter, type);
	}

	@Override
	public <T extends NodeDatum> T latest(String sourceId, Class<T> type) {
		return history.latest(sourceId, type);
	}

	@Override
	public <T extends NodeDatum> T offset(String sourceId, int offset, Class<T> type) {
		return history.offset(sourceId, offset, type);
	}

	@Override
	public <T extends NodeDatum> Collection<T> offset(Set<String> sourceIdFilter, int offset,
			Class<T> type) {
		return history.offset(sourceIdFilter, offset, type);
	}

	@Override
	public <T extends NodeDatum> Collection<T> offset(Set<String> sourceIdFilter, Instant timestamp,
			int offset, Class<T> type) {
		return history.offset(sourceIdFilter, timestamp, offset, type);
	}

	@Override
	public <T extends NodeDatum> T offset(String sourceId, Instant timestamp, int offset,
			Class<T> type) {
		return history.offset(sourceId, timestamp, offset, type);
	}

	@Override
	public <T extends NodeDatum> Collection<T> slice(String sourceId, int offset, int count,
			Class<T> type) {
		return history.slice(sourceId, offset, count, type);
	}

	@Override
	public <T extends NodeDatum> Collection<T> slice(String sourceId, Instant timestamp, int offset,
			int count, Class<T> type) {
		return history.slice(sourceId, timestamp, offset, count, type);
	}

	@Override
	public DatumHistorian unfiltered() {
		return unfiltered;
	}

	@Override
	public DatumMetadataOperations datumMetadata(String sourceId) {
		DatumMetadataService service = OptionalService.service(datumMetadataService);
		return (service != null ? service.getSourceMetadata(sourceId) : null);
	}

	@Override
	public Collection<DatumMetadataOperations> datumMetadata(Set<String> sourceIdFilter) {
		DatumMetadataService service = OptionalService.service(datumMetadataService);
		if ( service == null ) {
			return Collections.emptyList();
		}
		Set<String> metaSourceIds = service.availableSourceMetadataSourceIds();
		if ( metaSourceIds == null || metaSourceIds.isEmpty() ) {
			return Collections.emptyList();
		}
		List<DatumMetadataOperations> result = new ArrayList<>(metaSourceIds.size());
		for ( String sourceId : metaSourceIds ) {
			boolean match = false;
			if ( sourceIdFilter == null || sourceIdFilter.isEmpty() ) {
				match = true;
			} else {
				for ( String filter : sourceIdFilter ) {
					if ( pathMatcher.match(filter, sourceId) ) {
						match = true;
						break;
					}
				}
			}
			if ( match ) {
				DatumMetadataOperations meta = service.getSourceMetadata(sourceId);
				if ( meta != null ) {
					result.add(meta);
				}
			}
		}
		return result;
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
		history.setHistoryRawCount(rawCount);
	}

}
