/* ==================================================================
 * SimpleNodeControlInfoDatumDataSource.java - 9/04/2021 9:00:59 AM
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

package net.solarnetwork.node.datum.control;

import static net.solarnetwork.service.OptionalService.service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Data source for controls, supporting both scheduling polling and real-time
 * persisting of changes.
 * 
 * @author matt
 * @version 2.0
 */
public class NodeControlInfoDatumDataSource extends BaseIdentifiable
		implements SettingSpecifierProvider, EventHandler {

	/** The default {@code eventMode} property value: {@literal Change}. */
	public static final ControlEventMode DEFAULT_EVENT_MODE = ControlEventMode.Change;

	private static final Logger log = LoggerFactory.getLogger(NodeControlInfoDatumDataSource.class);

	private final OptionalService<DatumQueue> datumQueue;
	private Executor executor;
	private Pattern controlIdRegex;
	private ControlEventMode eventMode = DEFAULT_EVENT_MODE;

	/**
	 * Constructor.
	 * 
	 * @param datumQueue
	 *        the queue
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public NodeControlInfoDatumDataSource(OptionalService<DatumQueue> datumQueue) {
		super();
		if ( datumQueue == null ) {
			throw new IllegalArgumentException("The datumQueue argument must not be null.");
		}
		this.datumQueue = datumQueue;
	}

	@Override
	public void handleEvent(Event event) {
		final String topic = event.getTopic();
		if ( NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED.equals(topic) ) {
			if ( eventMode == ControlEventMode.Change ) {
				return;
			}
		} else if ( NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED.equals(topic) ) {
			if ( eventMode == ControlEventMode.Capture ) {
				return;
			}
		} else {
			return;
		}
		final Object val = event.getProperty(DatumEvents.DATUM_PROPERTY);
		if ( val == null || !(val instanceof NodeControlInfo) ) {
			return;
		}
		final NodeControlInfo info = (NodeControlInfo) val;
		final Pattern controlIdRegex = getControlIdRegex();
		if ( controlIdRegex != null && (info.getControlId() == null
				|| !controlIdRegex.matcher(info.getControlId()).find()) ) {
			log.debug("Ignoring control info: ID {} does not match pattern {}", info.getControlId(),
					controlIdRegex);
			return;
		}
		Runnable task = new Runnable() {

			@Override
			public void run() {
				persistDatum(info);
			}

		};
		Executor e = this.executor;
		if ( e != null ) {
			e.execute(task);
		} else {
			task.run();
		}
	}

	private void persistDatum(NodeControlInfo info) {
		final DatumQueue queue = service(datumQueue);
		if ( queue == null ) {
			log.warn("DatumQueue not available to offer control info {}; discarding", info);
			return;
		}
		NodeDatum datum;
		if ( info instanceof NodeDatum ) {
			datum = (NodeDatum) info;
		} else {
			datum = new SimpleNodeControlInfoDatum(info, Instant.now());
		}
		log.debug("Offering control datum {}", datum);
		queue.offer(datum);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.control";
	}

	@Override
	public String getDisplayName() {
		return "Control Datum Data Source";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(3);
		results.addAll(baseIdentifiableSettings(""));
		results.add(new BasicTextFieldSettingSpecifier("controlIdRegexValue", ""));

		// drop-down menu for event mode
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				"eventModeValue", DEFAULT_EVENT_MODE.name());
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		MessageSource ms = getMessageSource();
		for ( ControlEventMode e : ControlEventMode.values() ) {
			String desc = e.toString();
			if ( ms != null ) {
				desc = ms.getMessage("eventMode." + e.name(), null, e.name(), Locale.getDefault());
			}
			propTypeTitles.put(e.name(), desc);
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		return results;
	}

	/**
	 * Set an executor to use for internal tasks.
	 * 
	 * @param executor
	 *        the executor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Get the control ID regular expression.
	 * 
	 * @return the control ID expression, or {@literal null} for including all
	 *         control IDs
	 */
	public Pattern getControlIdRegex() {
		return controlIdRegex;
	}

	/**
	 * Set the control ID regular expression.
	 * 
	 * @param controlIdRegex
	 *        a pattern to match against control IDs; if defined then this datum
	 *        will only be generated for controls with matching control ID
	 *        values; if {@literal null} then generate datum for all controls
	 */
	public void setControlIdRegex(Pattern controlIdRegex) {
		this.controlIdRegex = controlIdRegex;
	}

	/**
	 * Get the control ID regular expression as a string.
	 * 
	 * @return the control ID expression string, or {@literal null} for
	 *         including all control IDs
	 */
	public String getControlIdRegexValue() {
		Pattern p = getControlIdRegex();
		return (p != null ? p.pattern() : null);
	}

	/**
	 * Set the control ID regular expression as a string.
	 * 
	 * <p>
	 * Errors compiling {@code controlIdRegex} into a {@link Pattern} will be
	 * silently ignored, causing the regular expression to be set to
	 * {@literal null}.
	 * </p>
	 * 
	 * @param controlIdRegex
	 *        a pattern to match against control IDs; if defined then this datum
	 *        will only be generated for controls with matching control ID
	 *        values; if {@literal null} then generate datum for all controls
	 */
	public void setControlIdRegexValue(String controlIdRegex) {
		Pattern p = null;
		if ( controlIdRegex != null ) {
			try {
				p = Pattern.compile(controlIdRegex, Pattern.CASE_INSENSITIVE);
			} catch ( PatternSyntaxException e ) {
				// ignore
			}
		}
		setControlIdRegex(p);
	}

	/**
	 * Get the control event mode.
	 * 
	 * @return the mode, never {@literal null}
	 */
	public ControlEventMode getEventMode() {
		return eventMode;
	}

	/**
	 * Set the control event mode.
	 * 
	 * @param eventMode
	 *        the mode to set; if {@literal null} then
	 *        {@link #DEFAULT_EVENT_MODE} will be set instead
	 */
	public void setEventMode(ControlEventMode eventMode) {
		if ( eventMode == null ) {
			eventMode = DEFAULT_EVENT_MODE;
		}
		this.eventMode = eventMode;
	}

	/**
	 * Get the control event mode string value.
	 * 
	 * @return the mode as a string, never {@literal null}
	 */
	public String getEventModeValue() {
		return getEventMode().name();
	}

	/**
	 * Set the control event mode as a string value.
	 * 
	 * @param value
	 *        the mode to set; if {@literal null} then
	 *        {@link #DEFAULT_EVENT_MODE} will be set instead
	 */
	public void setEventModeValue(String value) {
		ControlEventMode mode;
		try {
			mode = ControlEventMode.valueOf(value);
		} catch ( IllegalArgumentException | NullPointerException e ) {
			mode = DEFAULT_EVENT_MODE;
		}
		setEventMode(mode);
	}

}
