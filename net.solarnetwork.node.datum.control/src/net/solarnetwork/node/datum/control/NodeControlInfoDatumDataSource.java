/* ==================================================================
 * NodeControlInfoDatumDataSource.java - 9/04/2021 9:00:59 AM
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

import static java.util.Collections.singleton;
import static net.solarnetwork.util.OptionalService.service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodeControlInfoDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.OptionalService;

/**
 * Data source for controls, supporting both scheduling polling and real-time
 * persisting of changes.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeControlInfoDatumDataSource extends BaseIdentifiable
		implements SettingSpecifierProvider, EventHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeControlInfoDatumDataSource.class);

	private OptionalService<DatumDao<GeneralNodeDatum>> datumDao;
	private Executor executor;
	private Pattern controlIdRegex;

	@Override
	public void handleEvent(Event event) {
		final String topic = event.getTopic();
		if ( !(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED.equals(topic)
				|| NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED.equals(topic)) ) {
			return;
		}
		final Object val = event.getProperty(Datum.DATUM_PROPERTY);
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
		final DatumDao<GeneralNodeDatum> dao = service(datumDao);
		if ( dao == null ) {
			log.warn("DAO not available to persist control info {}; discarding", info);
			return;
		}
		GeneralNodeDatum datum;
		if ( info instanceof GeneralNodeDatum ) {
			datum = (GeneralNodeDatum) info;
		} else {
			datum = new GeneralNodeControlInfoDatum(singleton(info));
		}
		log.debug("Persisting control datum {}", datum);
		dao.storeDatum(datum);
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.control";
	}

	@Override
	public String getDisplayName() {
		return "Control Datum Data Source";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(3);
		result.addAll(baseIdentifiableSettings(""));
		result.add(new BasicTextFieldSettingSpecifier("controlIdRegexValue", ""));
		return result;
	}

	/**
	 * Set the datum DAO to use for real-time persisting of changes.
	 * 
	 * @param datumDao
	 *        the DAO to set
	 */
	public void setDatumDao(OptionalService<DatumDao<GeneralNodeDatum>> datumDao) {
		this.datumDao = datumDao;
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

}
