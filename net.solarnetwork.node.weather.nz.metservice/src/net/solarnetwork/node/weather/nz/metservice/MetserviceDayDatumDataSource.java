/* ==================================================================
 * MetserviceDayDatumDataSource.java - Oct 18, 2011 2:48:45 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.weather.nz.metservice;

import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.weather.DayDatum;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * MetService implementation of a {@link DayDatum} {@link DatumDataSource}.
 * 
 * <p>
 * This implementation reads public data available on the MetService website to
 * collect day information (sunrise, sunset, etc.).
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>dayDateFormat</dt>
 * <dd>The {@link SimpleDateFormat} date format to use to parse the day date.
 * Defaluts to {@link #DEFAULT_DAY_DATE_FORMAT}.</dd>
 * 
 * <dt>timeDateFormat</dt>
 * The {@link SimpleDateFormat} time format to use to parse sunrise/sunset
 * times. Defaults to {@link #DEFAULT_TIME_DATE_FORMAT}.</dd>
 * 
 * <dt>riseSet</dt>
 * <dd>The name of the "riseSet" file to parse. This file is expected to contain
 * a single JSON object declaration with the sunrise, sunset, and date
 * attributes. Defaults to {@link #DEFAULT_RISE_SET}.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class MetserviceDayDatumDataSource extends MetserviceSupport<DayDatum> implements
		DatumDataSource<DayDatum>, SettingSpecifierProvider {

	/** The default value for the {@code riseSet} property. */
	public static final String DEFAULT_RISE_SET = "riseSet93434M";

	/** The default value for the {@code dayDateFormat} property. */
	public static final String DEFAULT_DAY_DATE_FORMAT = "d MMMM yyyy";

	/** The default value for the {@code timeDateFormat} property. */
	public static final String DEFAULT_TIME_DATE_FORMAT = "h:mma";

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	private String riseSet;
	private String dayDateFormat;
	private String timeDateFormat;

	/**
	 * Default constructor.
	 */
	public MetserviceDayDatumDataSource() {
		super();
		riseSet = DEFAULT_RISE_SET;
		dayDateFormat = DEFAULT_DAY_DATE_FORMAT;
		timeDateFormat = DEFAULT_TIME_DATE_FORMAT;
	}

	@Override
	public Class<? extends DayDatum> getDatumType() {
		return DayDatum.class;
	}

	@Override
	public DayDatum readCurrentDatum() {

		// first see if we have cached data
		DayDatum result = getDatumCache().get(LAST_DATUM_CACHE_KEY);
		if ( result != null ) {
			Calendar now = Calendar.getInstance();
			Calendar datumCal = Calendar.getInstance();
			datumCal.setTime(result.getDay());
			if ( now.get(Calendar.YEAR) == datumCal.get(Calendar.YEAR)
					&& now.get(Calendar.DAY_OF_YEAR) == datumCal.get(Calendar.DAY_OF_YEAR) ) {
				// cached data is for same date, so return that
				return result;
			}

			// invalid cached data, remove now
			getDatumCache().remove(LAST_DATUM_CACHE_KEY);
		}

		final String url = getBaseUrl() + '/' + riseSet;
		final SimpleDateFormat timeFormat = new SimpleDateFormat(getTimeDateFormat());
		final SimpleDateFormat dayFormat = new SimpleDateFormat(getDayDateFormat());
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			Map<String, String> data = parseSimpleJavaScriptObjectProperties(getInputStreamFromURLConnection(conn));

			Date day = parseDateAttribute("day", data, dayFormat);
			Date sunrise = parseDateAttribute("sunRise", data, timeFormat);
			Date sunset = parseDateAttribute("sunSet", data, timeFormat);

			if ( day != null && sunrise != null && sunset != null ) {
				result = new DayDatum();
				result.setDay(day);
				result.setSunrise(sunrise);
				result.setSunset(sunset);
				result.setTimeZoneId("Pacific/Auckland");

				log.debug("Obtained new DayDatum: {}", result);
				getDatumCache().put(LAST_DATUM_CACHE_KEY, result);
			}

		} catch ( IOException e ) {
			log.warn("Error reading MetService URL [{}]: {}", url, e.getMessage());
		}
		return result;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.nz.metservice.day";
	}

	@Override
	public String getDisplayName() {
		return "New Zealand Metservice day information";
	}

	@Override
	public MessageSource getMessageSource() {
		synchronized ( MONITOR ) {
			if ( MESSAGE_SOURCE == null ) {
				ResourceBundleMessageSource source = new ResourceBundleMessageSource();
				source.setBundleClassLoader(getClass().getClassLoader());
				source.setBasename(getClass().getName());
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Arrays.asList((SettingSpecifier) new BasicTextFieldSettingSpecifier("uid", null),
				(SettingSpecifier) new BasicTextFieldSettingSpecifier("groupUID", null),
				(SettingSpecifier) new BasicTextFieldSettingSpecifier("baseUrl", DEFAULT_BASE_URL),
				(SettingSpecifier) new BasicTextFieldSettingSpecifier("riseSet", DEFAULT_RISE_SET),
				(SettingSpecifier) new BasicTextFieldSettingSpecifier("dayDateFormat",
						DEFAULT_DAY_DATE_FORMAT), (SettingSpecifier) new BasicTextFieldSettingSpecifier(
						"timeDayFormat", DEFAULT_TIME_DATE_FORMAT));
	}

	public String getRiseSet() {
		return riseSet;
	}

	public void setRiseSet(String riseSet) {
		this.riseSet = riseSet;
	}

	public String getDayDateFormat() {
		return dayDateFormat;
	}

	public void setDayDateFormat(String dayDateFormat) {
		this.dayDateFormat = dayDateFormat;
	}

	public String getTimeDateFormat() {
		return timeDateFormat;
	}

	public void setTimeDateFormat(String timeDateFormat) {
		this.timeDateFormat = timeDateFormat;
	}

}
