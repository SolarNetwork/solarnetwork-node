/* ==================================================================
 * CsvDatumDataSourceConfig.java - 31/03/2023 4:07:20 pm
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.csv;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.util.StringUtils;

/**
 * Overall configuration for a {@link CsvDatumDataSource} instance.
 *
 * @author matt
 * @version 1.0
 */
public class CsvDatumDataSourceConfig {

	/** The setting prefix for data source settings. */
	public static final String JOB_SERVICE_SETTING_PREFIX = "jobService.multiDatumDataSource.";

	/** The setting prefix for data source settings. */
	public static final String LOCATION_JOB_SERVICE_SETTING_PREFIX = "jobService.multiDatumDataSource.delegate.";

	private final boolean locationMode;
	private String key;
	private String schedule;
	private String serviceName;
	private String serviceGroup;
	private String sourceId;
	private String sourceIdColumn;
	private String locationKey;
	private String locationType;
	private String url;
	private String httpCustomizer;
	private String charsetName;
	private Integer connectionTimeout;
	private Integer skipRows;
	private Integer keepRows;
	private String dateFormat;
	private String urlDateFormat;
	private String timeZoneId;
	private String dateTimeColumn;
	private Long sampleCacheMs;

	private final List<CsvPropertyConfig> propertyConfigs = new ArrayList<>(8);

	/**
	 * Constructor.
	 *
	 * @param locationMode
	 *        {@literal true} to work with location datum configuration
	 */
	public CsvDatumDataSourceConfig(boolean locationMode) {
		super();
		this.locationMode = locationMode;
	}

	/**
	 * Generate a list of setting values from this instance.
	 *
	 * @param providerId
	 *        the setting provider key to use
	 * @return the list of setting values, never {@literal null}
	 */
	public List<SettingValueBean> toSettingValues(String providerId) {
		List<SettingValueBean> settings = new ArrayList<>(16);
		if ( schedule != null ) {
			settings.add(new SettingValueBean(providerId, key, "schedule", schedule));
		}
		addSetting(locationMode, settings, providerId, key, "uid", serviceName);
		addSetting(locationMode, settings, providerId, key, "groupUid", serviceGroup);
		if ( locationMode ) {
			addSetting(false, settings, providerId, key, "locationKey", locationKey);
			addSetting(false, settings, providerId, key, "locationType", locationType);
		} else {
			addSetting(locationMode, settings, providerId, key, "sourceId", sourceId);
			addSetting(locationMode, settings, providerId, key, "sourceIdColumn", sourceIdColumn);
		}
		addSetting(locationMode, settings, providerId, key, "url", url);
		addSetting(locationMode, settings, providerId, key, "httpRequestCustomizerUid", httpCustomizer);
		addSetting(locationMode, settings, providerId, key, "charsetName", charsetName);
		addSetting(locationMode, settings, providerId, key, "connectionTimeout", connectionTimeout);
		addSetting(locationMode, settings, providerId, key, "skipRows", skipRows);
		addSetting(locationMode, settings, providerId, key, "keepRows", keepRows);
		addSetting(locationMode, settings, providerId, key, "dateFormat", dateFormat);
		addSetting(locationMode, settings, providerId, key, "urlDateFormat", urlDateFormat);
		addSetting(locationMode, settings, providerId, key, "dateColumn", dateTimeColumn);
		addSetting(locationMode, settings, providerId, key, "timeZoneId", timeZoneId);
		addSetting(locationMode, settings, providerId, key, "dateTimeColumn", dateTimeColumn);
		addSetting(locationMode, settings, providerId, key, "sampleCacheMs", sampleCacheMs);

		int i = 0;
		for ( CsvPropertyConfig propConfig : propertyConfigs ) {
			settings.addAll(propConfig.toSettingValues(locationMode, providerId, key, i++));
		}
		return settings;
	}

	/**
	 * Populate a setting as a configuration value, if possible.
	 *
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as a configuration
	 *         value
	 */
	public boolean populateFromSetting(Setting setting) {
		if ( CsvPropertyConfig.populateFromSetting(this, setting) ) {
			return true;
		}
		final String prefix = locationMode ? LOCATION_JOB_SERVICE_SETTING_PREFIX
				: JOB_SERVICE_SETTING_PREFIX;
		String type = setting.getType();
		if ( type.startsWith(prefix) ) {
			type = type.substring(prefix.length());
		} else if ( locationMode && type.startsWith(JOB_SERVICE_SETTING_PREFIX) ) {
			type = type.substring(JOB_SERVICE_SETTING_PREFIX.length());
		}
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (type) {
				case "uid":
					setServiceName(val);
					break;
				case "groupUid":
					setServiceGroup(val);
					break;
				case "sourceId":
					setSourceId(val);
					break;
				case "sourceIdColumn":
					setSourceIdColumn(val);
					break;
				case "schedule":
					setSchedule(val);
					break;
				case "locationKey":
					setLocationKey(val);
					break;
				case "locationType":
					setLocationType(val);
					break;
				case "url":
					setUrl(val);
					break;
				case "httpCustomizer":
					setHttpCustomizer(val);
					break;
				case "charsetName":
					setCharsetName(val);
					break;
				case "connectionTimeout":
					if ( val != null ) {
						try {
							setConnectionTimeout(Integer.valueOf(val));
						} catch ( NumberFormatException e ) {
							// ignore
						}
					}
					break;
				case "skipRows":
					if ( val != null ) {
						try {
							setSkipRows(Integer.valueOf(val));
						} catch ( NumberFormatException e ) {
							// ignore
						}
					}
					break;
				case "keepRows":
					if ( val != null ) {
						try {
							setKeepRows(Integer.valueOf(val));
						} catch ( NumberFormatException e ) {
							// ignore
						}
					}
					break;
				case "dateFormat":
					setDateFormat(val);
					break;
				case "urlDateFormat":
					setUrlDateFormat(val);
					break;
				case "dateColumn":
					setDateTimeColumn(val);
					break;
				case "timeZoneId":
					setTimeZoneId(val);
					break;
				case "dateTimeColumn":
					setDateTimeColumn(val);
					break;
				case "sampleCacheMs":
					setSampleCacheMs(Long.valueOf(val));
					break;
				default:
					return false;
			}
			return true;
		}
		return false;
	}

	private static void addSetting(boolean locationPrefix, List<SettingValueBean> settings,
			String providerId, String instanceId, String key, Object val) {
		if ( val == null ) {
			return;
		}
		String fullKey = (locationPrefix ? LOCATION_JOB_SERVICE_SETTING_PREFIX
				: JOB_SERVICE_SETTING_PREFIX).concat(key);
		settings.add(new SettingValueBean(providerId, instanceId, fullKey, val.toString()));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CsvDatumDataSourceConfig{");
		if ( key != null ) {
			builder.append("key=");
			builder.append(key);
			builder.append(", ");
		}
		if ( serviceName != null ) {
			builder.append("serviceName=");
			builder.append(serviceName);
			builder.append(", ");
		}
		if ( serviceGroup != null ) {
			builder.append("serviceGroup=");
			builder.append(serviceGroup);
			builder.append(", ");
		}
		if ( schedule != null ) {
			builder.append("schedule=");
			builder.append(schedule);
			builder.append(", ");
		}
		if ( locationKey != null ) {
			builder.append("locationKey=");
			builder.append(locationKey);
			builder.append(", ");
			if ( locationType != null ) {
				builder.append("locationType=");
				builder.append(locationType);
				builder.append(", ");
			}
		} else {
			if ( sourceId != null ) {
				builder.append("sourceId=");
				builder.append(sourceId);
				builder.append(", ");
			}
			if ( sourceIdColumn != null ) {
				builder.append("sourceIdColumn=");
				builder.append(sourceIdColumn);
				builder.append(", ");
			}
			if ( sourceIdColumn != null ) {
				builder.append("sourceIdColumn=");
				builder.append(sourceIdColumn);
				builder.append(", ");
			}
		}
		if ( url != null ) {
			builder.append("url=");
			builder.append(url);
			builder.append(", ");
		}
		if ( httpCustomizer != null ) {
			builder.append("httpCustomizer=");
			builder.append(httpCustomizer);
			builder.append(", ");
		}
		if ( charsetName != null ) {
			builder.append("charsetName=");
			builder.append(charsetName);
			builder.append(", ");
		}
		if ( connectionTimeout != null ) {
			builder.append("connectionTimeout=");
			builder.append(connectionTimeout);
			builder.append(", ");
		}
		if ( skipRows != null ) {
			builder.append("skipRows=");
			builder.append(skipRows);
			builder.append(", ");
		}
		if ( keepRows != null ) {
			builder.append("keepRows=");
			builder.append(keepRows);
			builder.append(", ");
		}
		if ( dateFormat != null ) {
			builder.append("dateFormat=");
			builder.append(dateFormat);
			builder.append(", ");
		}
		if ( urlDateFormat != null ) {
			builder.append("urlDateFormat=");
			builder.append(urlDateFormat);
			builder.append(", ");
		}
		if ( timeZoneId != null ) {
			builder.append("timeZoneId=");
			builder.append(timeZoneId);
			builder.append(", ");
		}
		if ( dateTimeColumn != null ) {
			builder.append("dateTimeColumn=");
			builder.append(dateTimeColumn);
			builder.append(", ");
		}
		if ( sampleCacheMs != null ) {
			builder.append("sampleCacheMs=");
			builder.append(sampleCacheMs);
			builder.append(", ");
		}
		if ( propertyConfigs != null ) {
			builder.append("propertyConfigs=");
			builder.append(propertyConfigs);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the location mode flag.
	 *
	 * @return the location mode flag
	 */
	public boolean isLocationMode() {
		return locationMode;
	}

	/**
	 * Get the instance key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the instance ID.
	 *
	 * @param key
	 *        the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the schedule.
	 *
	 * @return the schedule
	 */
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule.
	 *
	 * @param schedule
	 *        the schedule to set
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the service name.
	 *
	 * @return the service name
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * Set the service name.
	 *
	 * @param serviceName
	 *        the service name to set
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Get the service group.
	 *
	 * @return the service group
	 */
	public String getServiceGroup() {
		return serviceGroup;
	}

	/**
	 * Set the service group.
	 *
	 * @param serviceGroup
	 *        the service group to set
	 */
	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
	}

	/**
	 * Get the source ID.
	 *
	 * @return the sourceId
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 *
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the {@literal 1}- or {@literal A}-based source ID column reference.
	 *
	 * @return the source ID column reference
	 */
	public String getSourceIdColumn() {
		return sourceIdColumn;
	}

	/**
	 * Set the {@literal 1}- or {@literal A}-based source ID column reference.
	 *
	 * @param sourceIdColumn
	 *        the source ID column reference to set
	 */
	public void setSourceIdColumn(String sourceIdColumn) {
		this.sourceIdColumn = sourceIdColumn;
	}

	/**
	 * Get the location key.
	 *
	 * @return the location key
	 */
	public String getLocationKey() {
		return locationKey;
	}

	/**
	 * Set the location key.
	 *
	 * @param locationKey
	 *        the location key to set
	 */
	public void setLocationKey(String locationKey) {
		this.locationKey = locationKey;
	}

	/**
	 * Get the location type.
	 *
	 * @return the location type
	 */
	public String getLocationType() {
		return locationType;
	}

	/**
	 * Set the location type.
	 *
	 * @param locationType
	 *        the location type to set
	 */
	public void setLocationType(String locationType) {
		this.locationType = locationType;
	}

	/**
	 * Get the URL template.
	 *
	 * @return the template
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL template to retrieve price data from.
	 *
	 * <p>
	 * This template is for accessing the delimited price data from. This will
	 * be passed through {@link StringUtils#expandTemplateString(String, Map)}
	 * with the current date as the <code>{date}</code> parameter. The date
	 * parameter will be formatted using the {@link #getUrlDateFormat()}
	 * pattern. For example, a value of
	 * <code>http://some.place/prices?date={date}</code> might resolve to
	 * something like {@code http://some.place/prices?date=2009-08-08}.
	 * </p>
	 *
	 * @param url
	 *        the URL template
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get the
	 * {@link net.solarnetwork.web.jakarta.service.HttpRequestCustomizerService}
	 * UID.
	 *
	 * @return the UID
	 */
	public String getHttpCustomizer() {
		return httpCustomizer;
	}

	/**
	 * Set the
	 * {@link net.solarnetwork.web.jakarta.service.HttpRequestCustomizerService}
	 * UID.
	 *
	 * @param httpCustomizer
	 *        the UID to set
	 */
	public void setHttpCustomizer(String httpCustomizer) {
		this.httpCustomizer = httpCustomizer;
	}

	/**
	 * Get the character set name to parse the CSV resource as.
	 *
	 * @return the character set name
	 */
	public String getCharsetName() {
		return charsetName;
	}

	/**
	 * Set the character set name to parse the CSV resource as.
	 *
	 * @param charsetName
	 *        the character set name to set
	 */
	public void setCharsetName(String charsetName) {
		this.charsetName = charsetName;
	}

	/**
	 * Get the URL connection timeout to apply when requesting the data.
	 *
	 * @return the connection timeout, in milliseconds
	 */
	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * Set the URL connection timeout to apply when requesting the data.
	 *
	 * @param connectionTimeout
	 *        the timeout, in milliseconds
	 */
	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Get the number of lines of text to skip.
	 *
	 * <p>
	 * When greater than {@literal 0} this will skip "header" rows. When
	 * {@literal 0} the first line will be used. When less than {@literal 0}
	 * then this line starting from the last available will be used, for example
	 * {@literal -1} will cause the last line to be used.
	 * </p>
	 *
	 * @return the number of lines to skip
	 */
	public Integer getSkipRows() {
		return skipRows;
	}

	/**
	 * Set the number of lines of text to skip.
	 *
	 * @param skipRows
	 *        the number of lines, or {@literal 0} to not skip any lines
	 */
	public void setSkipRows(Integer skipRows) {
		this.skipRows = skipRows;
	}

	/**
	 * Get the number of rows of CSV to keep (turn into datum).
	 *
	 * @return the number of rows to keep
	 */
	public Integer getKeepRows() {
		return keepRows;
	}

	/**
	 * Set the number of rows of CSV to keep (turn into datum).
	 *
	 * @param keepRows
	 *        the number of rows, or {@literal 0} to not keep all rows
	 */
	public void setKeepRows(Integer keepRows) {
		this.keepRows = keepRows;
	}

	/**
	 * Get the comma-delimited set of {@literal 1}- or {@literal A}-based column
	 * references to use as the time stamp value for datum.
	 *
	 * <p>
	 * This is provided as an array in case the date and time of the data is
	 * split across multiple columns. If multiple columns are configured, they
	 * will be joined with a space character before parsing the result into a
	 * time stamp value.
	 * </p>
	 *
	 * @return the date time column
	 */
	public String getDateTimeColumn() {
		return dateTimeColumn;
	}

	/**
	 * Set the comma-delimited set of {@literal 1}- or {@literal A}-based column
	 * references to use as the time stamp value for datum.
	 *
	 * @param dateTimeColumn
	 *        the column indexes to use for time stamps
	 */
	public void setDateTimeColumn(String dateTimeColumn) {
		this.dateTimeColumn = dateTimeColumn;
	}

	/**
	 * Get the {@link DateTimeFormatter} pattern to use for parsing the price
	 * date value into a time stamp.
	 *
	 * @return the date pattern
	 */
	public String getDateFormat() {
		return dateFormat;
	}

	/**
	 * Set the {@link DateTimeFormatter} pattern to use for parsing the price
	 * date value into a time stamp.
	 *
	 * @param dateFormat
	 *        the date pattern
	 */
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Get the time zone to use for dates.
	 *
	 * @return the time zone; defaults to the system default time zone
	 */
	public String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Get the time zone to use for dates.
	 *
	 * @param timeZoneId
	 *        the time zone to set
	 */
	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

	/**
	 * Get the date format to use for URL parameters.
	 *
	 * @return the date format
	 */
	public String getUrlDateFormat() {
		return urlDateFormat;
	}

	/**
	 * Set the date format to use for URL parameters.
	 *
	 * @param urlDateFormat
	 *        the urlDateFormat to set
	 */
	public void setUrlDateFormat(String urlDateFormat) {
		this.urlDateFormat = urlDateFormat;
	}

	/**
	 * Get the sample cache milliseconds.
	 *
	 * @return the sampleCacheMs
	 */
	public Long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache milliseconds.
	 *
	 * @param sampleCacheMs
	 *        the sampleCacheMs to set
	 */
	public void setSampleCacheMs(Long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the property configurations
	 */
	public List<CsvPropertyConfig> getPropertyConfigs() {
		return propertyConfigs;
	}

}
