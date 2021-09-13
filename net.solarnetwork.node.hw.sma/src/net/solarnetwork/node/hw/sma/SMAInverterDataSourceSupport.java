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
import java.util.Set;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.domain.datum.EnergyDatum;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;

/**
 * Supporting class for SMA inverter data sources.
 * 
 * @author matt
 * @version 2.0
 */
public abstract class SMAInverterDataSourceSupport extends DatumDataSourceSupport {

	/** The default value for the {@code sourceId} property. */
	public static final String DEFAULT_SOURCE_ID = "Main";

	private Set<String> channelNamesToMonitor = null;
	private SettingDao settingDao = null;
	private String sourceId = DEFAULT_SOURCE_ID;

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
	 * @return the setting value, or {@literal null} if not found
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
	 * Get the last known day value, or {@literal null} if not known.
	 * 
	 * @return the date, or {@literal null} if not known
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
	 * different from the current execution day, {@literal true} will be returned.
	 * Otherwise, {@literal false} is returned.
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
	 * Tag with {@link EnergyDatum#TAG_GENERATION}.
	 * 
	 * @param d
	 *        the datum to get the source ID from
	 * @since 1.5
	 */
	protected void addEnergyDatumSourceMetadata(Datum d) {
		// associate generation tags with this source
		GeneralDatumMetadata sourceMeta = new GeneralDatumMetadata();
		sourceMeta.addTag(EnergyDatum.TAG_GENERATION);
		addSourceMetadata(d.getSourceId(), sourceMeta);
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

	@Override
	public String getUID() {
		String uid = getUid();
		if ( uid == null ) {
			uid = getSourceId();
		}
		return uid;
	}

}
