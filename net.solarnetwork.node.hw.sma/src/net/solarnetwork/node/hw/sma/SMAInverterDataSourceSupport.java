/* ==================================================================
 * SMAInverterDataSourceSupport.java - Mar 7, 2013 12:02:55 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.Setting;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.OptionalService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supporting class for SMA inverter data sources.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>settingDao</dt>
 * <dd>The {@link SettingDao} to persist settings with.</dd>
 * 
 * <dt>sourceId</dt>
 * <dd>A source ID value to use for captured datums. Defaults to
 * {@link #DEFAULT_SOURCE_ID}.</dd>
 * 
 * <dt>eventAdmin</dt>
 * <dd>An optional {@link EventAdmin} service to use for posting events.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.3
 */
public abstract class SMAInverterDataSourceSupport {

	/** The default value for the {@code sourceId} property. */
	public static final String DEFAULT_SOURCE_ID = "Main";

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private Set<String> channelNamesToMonitor = null;
	private SettingDao settingDao = null;
	private String sourceId = DEFAULT_SOURCE_ID;
	private String groupUID = null;
	private OptionalService<EventAdmin> eventAdmin;

	protected final String getSettingPrefixDayStartValue() {
		return getClass().getSimpleName() + "." + sourceId + ".start:";
	}

	protected final String getSettingPrefixLastKnownValue() {
		return getClass().getSimpleName() + "." + sourceId + ".value:";
	}

	protected final String getSettingKeyLastKnownDay() {
		return getClass().getSimpleName() + "." + sourceId + ".knownDay";
	}

	/**
	 * Get a "volatile" setting, that is one that does not trigger an automatic
	 * settings backup.
	 * 
	 * @param key
	 *        the setting key
	 * @return the setting value, or <em>null</em> if not found
	 */
	protected final String getVolatileSetting(String key) {
		return (settingDao == null ? null : settingDao.getSetting(key));
	}

	/**
	 * Save a "volatile" setting.
	 * 
	 * @param key
	 *        the setting key
	 * @param value
	 *        the setting value
	 * @see #getVolatileSetting(String)
	 */
	protected final void saveVolatileSetting(String key, String value) {
		if ( settingDao == null ) {
			return;
		}
		settingDao.storeSetting(new Setting(key, null, value, EnumSet.of(Setting.SettingFlag.Volatile)));
	}

	/**
	 * Get the current day of the year as a String value.
	 * 
	 * @return the day of the year
	 */
	protected String getDayOfYearValue() {
		Calendar now = Calendar.getInstance();
		return String.valueOf(now.get(Calendar.YEAR)) + "."
				+ String.valueOf(now.get(Calendar.DAY_OF_YEAR));
	}

	/**
	 * Save today as the last known day.
	 * 
	 * @see #getDayOfYearValue()
	 */
	protected final void storeLastKnownDay() {
		String dayOfYear = getDayOfYearValue();
		if ( log.isDebugEnabled() ) {
			log.debug("Saving last known day as [" + dayOfYear + ']');
		}
		saveVolatileSetting(getSettingKeyLastKnownDay(), dayOfYear);
	}

	/**
	 * Get the last known day value, or <em>null</em> if not known.
	 * 
	 * @return the date, or <em>null</em> if not known
	 */
	protected final Calendar getLastKnownDay() {
		String lastKnownDayOfYear = getLastKnownDayOfYearValue();
		Calendar day = null;
		if ( lastKnownDayOfYear != null ) {
			int dot = lastKnownDayOfYear.indexOf('.');
			day = Calendar.getInstance();
			day.set(Calendar.YEAR, Integer.parseInt(lastKnownDayOfYear.substring(0, dot)));
			day.set(Calendar.DAY_OF_YEAR, Integer.parseInt(lastKnownDayOfYear.substring(dot + 1)));
		}
		return day;
	}

	protected final String getLastKnownDayOfYearValue() {
		return getVolatileSetting(getSettingKeyLastKnownDay());
	}

	/**
	 * Test if today is a different day from the last known day.
	 * 
	 * <p>
	 * If the {@code settingDao} to be configured, this method will use that to
	 * load a "last known day" value. If that value is not found, or is
	 * different from the current execution day, <em>true</em> will be returned.
	 * Otherwise, <em>false</em> is returned.
	 * </p>
	 * 
	 * @return boolean
	 */
	protected final boolean isNewDay() {
		if ( this.settingDao == null ) {
			return false;
		}
		Calendar now = Calendar.getInstance();
		Calendar then = getLastKnownDay();
		if ( then == null || (now.get(Calendar.YEAR) != then.get(Calendar.YEAR))
				|| (now.get(Calendar.DAY_OF_YEAR) != then.get(Calendar.DAY_OF_YEAR)) ) {
			return true;
		}
		return false;
	}

	/**
	 * Parse a String into a Number of a specific type.
	 * 
	 * @param numberType
	 *        the type of Number to return
	 * @param numberString
	 *        the String to parse
	 * @return the new Number instance
	 */
	protected final Number parseNumber(Class<? extends Number> numberType, String numberString) {
		if ( Integer.class.isAssignableFrom(numberType) ) {
			return Integer.valueOf(numberString);
		} else if ( Float.class.isAssignableFrom(numberType) ) {
			return Float.valueOf(numberString);
		} else if ( Long.class.isAssignableFrom(numberType) ) {
			return Long.valueOf(numberString);
		}
		return Double.valueOf(numberString);
	}

	/**
	 * Divide two {@link Number} instances using a specific implementation of
	 * Number.
	 * 
	 * <p>
	 * Really the {@code numberType} argument should be considered a
	 * {@code Class<? extends Number>} but to simplify calling this method any
	 * Class is allowed.
	 * </p>
	 * 
	 * @param numberType
	 *        the type of Number to treat the dividend and divisor as
	 * @param dividend
	 *        the dividend value
	 * @param divisor
	 *        the divisor value
	 * @return a Number instance of type {@code numberType}
	 */
	protected final Number divide(Class<?> numberType, Number dividend, Number divisor) {
		if ( Integer.class.isAssignableFrom(numberType) ) {
			return dividend.intValue() / divisor.intValue();
		} else if ( Float.class.isAssignableFrom(numberType) ) {
			return dividend.floatValue() / divisor.floatValue();
		} else if ( Long.class.isAssignableFrom(numberType) ) {
			return dividend.longValue() / divisor.longValue();
		}
		return dividend.doubleValue() / divisor.doubleValue();
	}

	/**
	 * Subtract two Number instances.
	 * 
	 * <p>
	 * The returned Number will be an instance of the {@code start} class.
	 * </p>
	 * 
	 * @param start
	 *        the starting number to subtract from
	 * @param offset
	 *        the amount to subtract
	 * @return a Number instance of the same type as {@code start}
	 */
	protected final Number diff(Number start, Number offset) {
		if ( start instanceof Integer ) {
			return Integer.valueOf(start.intValue() - offset.intValue());
		} else if ( start instanceof Float ) {
			return Float.valueOf(start.floatValue() - offset.floatValue());
		} else if ( start instanceof Long ) {
			return Long.valueOf(start.longValue() - offset.longValue());
		}
		return Double.valueOf(start.doubleValue() - offset.doubleValue());
	}

	/**
	 * Multiply two Number instances.
	 * 
	 * <p>
	 * The returned Number will be an instance of the {@code a} class.
	 * </p>
	 * 
	 * @param a
	 *        first number
	 * @param b
	 *        second number
	 * @return a Number instance of the same type as {@code a}
	 */
	protected final Number mult(Number a, Number b) {
		if ( a instanceof Integer ) {
			return Integer.valueOf(a.intValue() * b.intValue());
		} else if ( a instanceof Float ) {
			return Float.valueOf(a.floatValue() * b.floatValue());
		} else if ( a instanceof Long ) {
			return Long.valueOf(a.longValue() * b.longValue());
		}
		return Double.valueOf(a.doubleValue() * b.doubleValue());
	}

	/**
	 * Post a {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED} {@link Event}.
	 * 
	 * <p>
	 * This method calls {@link #createDatumCapturedEvent(Datum, Class)} to
	 * create the actual Event, which may be overridden by extending classes.
	 * </p>
	 * 
	 * @param datum
	 *        the {@link Datum} to post the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @since 1.3
	 */
	protected final void postDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || datum == null ) {
			return;
		}
		Event event = createDatumCapturedEvent(datum, eventDatumType);
		ea.postEvent(event);
	}

	/**
	 * Create a new {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
	 * {@link Event} object out of a {@link Datum}.
	 * 
	 * <p>
	 * This method will populate all simple properties of the given
	 * {@link Datum} into the event properties, along with the
	 * {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE}.
	 * 
	 * @param datum
	 *        the datum to create the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @return the new Event instance
	 * @since 1.3
	 */
	protected Event createDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		Map<String, Object> props = ClassUtils.getSimpleBeanProperties(datum, null);
		props.put(DatumDataSource.EVENT_DATUM_CAPTURED_DATUM_TYPE, eventDatumType.getName());
		log.debug("Created {} event with props {}", DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
		return new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
	}

	public Set<String> getChannelNamesToMonitor() {
		return channelNamesToMonitor;
	}

	public void setChannelNamesToMonitor(Set<String> channelNamesToMonitor) {
		this.channelNamesToMonitor = channelNamesToMonitor;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

	public String getUID() {
		return getSourceId();
	}

	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
