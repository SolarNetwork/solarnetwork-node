/* ==================================================================
 * LoadShedControlConfig.java - 27/06/2015 11:26:03 am
 *
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.loadshedder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Settings for a single configurable load shed switch.
 *
 * @author matt
 * @version 2.0
 */
public class LoadShedControlConfig {

	private static final Logger LOG = LoggerFactory.getLogger(LoadShedControlConfig.class);

	/** The supported time window syntax ({@code H:mm}). */
	public static final String TIME_WINDOW_PATTERN = "H:mm";

	private String controlId;
	private String name;
	private Integer priority;
	private Boolean active = Boolean.TRUE;
	private String timeWindowStart;
	private String timeWindowEnd;
	private Integer minimumLimitMinutes;

	/**
	 * Default constructor.
	 */
	public LoadShedControlConfig() {
		super();
	}

	/**
	 * Construct with a control ID.
	 *
	 * @param controlId
	 *        The control ID.
	 */
	public LoadShedControlConfig(String controlId) {
		super();
		setControlId(controlId);
	}

	/**
	 * Construct with a control ID and priority.
	 *
	 * @param controlId
	 *        The control ID.
	 * @param priority
	 *        The priority.
	 */
	public LoadShedControlConfig(String controlId, Integer priority) {
		super();
		setControlId(controlId);
		setPriority(priority);
	}

	/**
	 * Get a list of settings for configuring this object.
	 *
	 * @param prefix
	 *        A prefix to apply, e.g. for dynamic list support.
	 * @return A list of settings.
	 */
	public List<SettingSpecifier> settings(String prefix) {
		LoadShedControlConfig defaults = new LoadShedControlConfig();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();
		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", defaults.name));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "controlId", defaults.controlId));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "priority",
				(defaults.priority == null ? "" : defaults.priority.toString())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "minimumLimitMinutes",
				(defaults.minimumLimitMinutes == null ? "" : defaults.minimumLimitMinutes.toString())));
		results.add(new BasicToggleSettingSpecifier(prefix + "active", defaults.active));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "timeWindowStart",
				defaults.timeWindowStart));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "timeWindowEnd", defaults.timeWindowEnd));
		return results;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LoadShedControlConfig{");
		builder.append("controlId=");
		builder.append(controlId);
		if ( priority != null ) {
			builder.append(", priority=");
			builder.append(priority);
		}
		if ( active != null ) {
			builder.append(", active=");
			builder.append(active);
		}
		if ( minimumLimitMinutes != null ) {
			builder.append(", minimumLimitMinutes=");
			builder.append(minimumLimitMinutes);
		}
		if ( timeWindowStart != null ) {
			builder.append(", timeWindowStart=");
			builder.append(timeWindowStart);
		}
		if ( timeWindowEnd != null ) {
			builder.append(", timeWindowEnd=");
			builder.append(timeWindowEnd);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get a {@link Calendar} instance set to the current date, with the time
	 * set from the parsed time window.
	 *
	 * @param window
	 *        A time window in the pattern {@link #TIME_WINDOW_PATTERN}.
	 * @return A Calendar, or {@literal null} if the window cannot be parsed.
	 */
	private Calendar timeWindowCalendar(final long date, final String window) {
		if ( window == null ) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(TIME_WINDOW_PATTERN);
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(date);
			Calendar time = (Calendar) cal.clone();
			Date d = sdf.parse(window);
			time.setTime(d);
			cal.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
			cal.set(Calendar.MILLISECOND, 0);
			return cal;
		} catch ( ParseException e ) {
			LOG.warn("Unable to parse time window {}: {}", window, e.getMessage());
			return null;
		}
	}

	/**
	 * Test if a specific date falls within the configured time window. If both
	 * the start and end time windows are <b>not</b> configured then this method
	 * will return {@literal true}.
	 *
	 * @param date
	 *        The date to test.
	 * @return {@literal true} if the date's time component falls within the
	 *         configured time window.
	 */
	public boolean fallsWithinTimeWindow(final long date) {
		if ( timeWindowEnd == null && timeWindowStart == null ) {
			return true;
		} else if ( timeWindowEnd == null ) {
			// any time not before timeWindowStart
			Calendar start = timeWindowCalendar(date, timeWindowStart);
			return (date >= start.getTimeInMillis());
		} else if ( timeWindowStart == null ) {
			// any time not after timeWindowEnd
			Calendar end = timeWindowCalendar(date, timeWindowEnd);
			return (date <= end.getTimeInMillis());
		}
		// between start/end times
		Calendar start = timeWindowCalendar(date, timeWindowStart);
		Calendar end = timeWindowCalendar(date, timeWindowEnd);
		return (date >= start.getTimeInMillis() && date <= end.getTimeInMillis());
	}

	/**
	 * Get the control ID.
	 *
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 *
	 * @param controlId
	 *        the control ID to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the priority.
	 *
	 * @return the priority
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * Set the priority.
	 *
	 * @param priority
	 *        the priority to set
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * Get the active state.
	 *
	 * @return the state
	 */
	public Boolean getActive() {
		return active;
	}

	/**
	 * Set the active set.
	 *
	 * @param active
	 *        the state to set
	 */
	public void setActive(Boolean active) {
		this.active = active;
	}

	/**
	 * Get the time window start.
	 *
	 * @return the start
	 */
	public String getTimeWindowStart() {
		return timeWindowStart;
	}

	/**
	 * Set the time window start.
	 *
	 * @param timeWindowStart
	 *        the start to set
	 */
	public void setTimeWindowStart(String timeWindowStart) {
		this.timeWindowStart = timeWindowStart;
	}

	/**
	 * Get the time window end.
	 *
	 * @return the end
	 */
	public String getTimeWindowEnd() {
		return timeWindowEnd;
	}

	/**
	 * Set the time window end.
	 *
	 * @param timeWindowEnd
	 *        the end to set
	 */
	public void setTimeWindowEnd(String timeWindowEnd) {
		this.timeWindowEnd = timeWindowEnd;
	}

	/**
	 * Get the minimum limit minutes.
	 *
	 * @return the minimum
	 */
	public Integer getMinimumLimitMinutes() {
		return minimumLimitMinutes;
	}

	/**
	 * Set the minimum limit minutes.
	 *
	 * @param minimumLimitMinutes
	 *        the minimum
	 */
	public void setMinimumLimitMinutes(Integer minimumLimitMinutes) {
		this.minimumLimitMinutes = minimumLimitMinutes;
	}

}
