/* ==================================================================
 * LogDatumGenerator.java - 21/10/2022 10:09:35 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.log;

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.DatumSourceIdProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Generate datum from log events.
 *
 * @author matt
 * @version 1.1
 */
public class LogDatumGenerator extends BaseIdentifiable
		implements EventHandler, SettingSpecifierProvider, DatumSourceIdProvider {

	/** The EventAdmin topic for log events. */
	public static final String EVENT_ADMIN_LOG_TOPIC = "net/solarnetwork/Log";

	/** The {@code sourceId} property default value. */
	public static final String DEFAULT_SOURCE_ID = "log";

	/** The default source ID prefix. */
	public static final String DEFAULT_SOURCE_ID_PREFIX = DEFAULT_SOURCE_ID + "/";

	private final OptionalService<DatumQueue> datumQueue;
	private String sourceId = DEFAULT_SOURCE_ID;

	/**
	 * Constructor.
	 *
	 * @param datumQueue
	 *        the datum queue
	 */
	public LogDatumGenerator(OptionalService<DatumQueue> datumQueue) {
		super();
		this.datumQueue = requireNonNullArgument(datumQueue, "datumQueue");
		setDisplayName("Log Datum Generator");
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.log";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(2);
		results.add(new BasicTextFieldSettingSpecifier("sourceId", DEFAULT_SOURCE_ID));
		return results;
	}

	@Override
	public void handleEvent(Event event) {
		if ( event == null || !EVENT_ADMIN_LOG_TOPIC.equals(event.getTopic()) ) {
			return;
		}
		final String sourceId = getSourceId();
		if ( sourceId == null || sourceId.isEmpty() ) {
			return;
		}
		handleEventLog(event, sourceId);
	}

	private void handleEventLog(Event event, String sourceId) {
		final DatumQueue queue = service(datumQueue);
		if ( queue == null ) {
			return;
		}
		Object ts = event.getProperty("ts");
		Object name = event.getProperty("name");
		Object level = event.getProperty("level");
		Object msg = event.getProperty("msg");
		if ( ts instanceof Long && name != null && level != null && msg != null ) {
			Map<String, Object> placeholders = new HashMap<>(4);
			placeholders.put("logLevel", level);
			DatumSamples s = new DatumSamples();

			Object priority = event.getProperty("priority");
			if ( priority instanceof Integer ) {
				s.putInstantaneousSampleValue("priority", (Integer) priority);
				placeholders.put("logPriority", priority);
			}
			s.putStatusSampleValue("name", name);
			placeholders.put("logName", name);
			placeholders.put("logNameSlashed", name.toString().replace('.', '/'));
			s.putStatusSampleValue("level", level);
			s.putStatusSampleValue("msg", msg);
			s.putStatusSampleValue("exMsg", event.getProperty("exMsg"));
			Object st = event.getProperty("exSt");
			if ( st instanceof String[] ) {
				String stString = Arrays.stream((String[]) st).collect(Collectors.joining("\n"));
				s.putStatusSampleValue("exSt", stString);
			}

			String resolvedSourceId = resolvePlaceholders(sourceId, placeholders);
			if ( resolvedSourceId != null && (DEFAULT_SOURCE_ID.equals(resolvedSourceId)
					|| resolvedSourceId.startsWith(DEFAULT_SOURCE_ID_PREFIX)) ) {
				SimpleDatum d = SimpleDatum.nodeDatum(resolvedSourceId, Instant.ofEpochMilli((Long) ts),
						s);
				queue.offer(d);
			}
		}
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
