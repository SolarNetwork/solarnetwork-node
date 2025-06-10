/* ==================================================================
 * CsvDatumDataSource.java - 31/03/2023 3:10:09 pm
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

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.NumberUtils.narrow;
import static net.solarnetwork.util.StringUtils.numberValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.io.UrlUtils;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.ByteList;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.LimitedSizeDeque;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.web.jakarta.service.HttpRequestCustomizerService;

/**
 * Read data from a CSV-formatted resource and generate one or more datum.
 *
 * @author matt
 * @version 2.0
 */
public class CsvDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider,
		SettingsChangeObserver, ServiceLifecycleObserver {

	/** The default value for the {@code settingUid} property. */
	public static final String DEFAULT_SETTING_UID = "net.solarnetwork.node.datum.csv";

	/** The default value for the {@code connectionTimeout} property. */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 15000;

	/** The default value for the {@code dateFormat} property. */
	public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";

	/** The default value for the {@code skipRows} property. */
	public static final int DEFAULT_SKIP_ROWS = -1;

	/** The default value for the {@code keepRows} property. */
	public static final int DEFAULT_KEEP_ROWS = 1;

	/** The default value for the {@link timeZoneId} property. */
	public static final String DEFAULT_TIME_ZONE_ID = TimeZone.getDefault().getID();

	/** The default value for the {@code urlDateFormat} property. */
	public static final String DEFAULT_URL_DATE_FORMAT = "yyyy-MM-dd";

	/** The {@code sampleCacheMs} property default value. */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 55_000L;

	private final String settingUid;

	private String sourceId;
	private String sourceIdColumn;
	private boolean includeSourceIdSetting;
	private String url;
	private Charset charset;
	private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
	private int skipRows = DEFAULT_SKIP_ROWS;
	private int keepRows = DEFAULT_KEEP_ROWS;
	private String dateFormat = DEFAULT_DATE_FORMAT;
	private String urlDateFormat = DEFAULT_URL_DATE_FORMAT;
	private String timeZoneId = DEFAULT_TIME_ZONE_ID;
	private String dateTimeColumn;
	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;
	private CsvPropertyConfig[] propConfigs;
	private OptionalService<ClientHttpRequestFactory> httpRequestFactory;
	private OptionalFilterableService<HttpRequestCustomizerService> httpRequestCustomizer;

	private DateTimeFormatter dateFormatter;
	private DateTimeFormatter urlDateFormatter;
	private int[] sourceIdColumnIndexes;
	private int[] dateTimeColumnIndexes;

	private final AtomicReference<CachedResult<List<NodeDatum>>> cache = new AtomicReference<>();

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link #DEFAULT_SETTING_UID} will be used.
	 * </p>
	 */
	public CsvDatumDataSource() {
		this(DEFAULT_SETTING_UID);
	}

	/**
	 * Constructor.
	 *
	 * @param settingUid
	 *        the setting UID to use
	 */
	public CsvDatumDataSource(String settingUid) {
		super();
		this.settingUid = ObjectUtils.requireNonNullArgument(settingUid, "settingUid");
		setDisplayName("CSV");
		setCharset(null); // apply default
		this.includeSourceIdSetting = true;
	}

	@Override
	public void serviceDidStartup() {
		configurationChanged(null);
	}

	@Override
	public void serviceDidShutdown() {
		// nothing
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		try {
			dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
			if ( timeZoneId != null ) {
				dateFormatter = dateFormatter.withZone(ZoneId.of(timeZoneId));
			}
		} catch ( Exception e ) {
			this.dateFormatter = null;
		}
		try {
			urlDateFormatter = DateTimeFormatter.ofPattern(urlDateFormat);
			if ( timeZoneId != null ) {
				urlDateFormatter = urlDateFormatter.withZone(ZoneId.of(timeZoneId));
			}
		} catch ( Exception e ) {
			this.urlDateFormatter = null;
		}
		this.sourceIdColumnIndexes = CsvDatumDataSourceUtils.columnIndexes(sourceIdColumn);
		this.dateTimeColumnIndexes = CsvDatumDataSourceUtils.columnIndexes(dateTimeColumn);
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final int[] cols = this.sourceIdColumnIndexes;
		if ( cols == null || cols.length < 1 ) {
			final String sourceId = resolvePlaceholders(getSourceId());
			return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
					: Collections.singleton(sourceId));
		}

		// have to fetch data to resolve source IDs
		final Collection<NodeDatum> datum = readMultipleDatum();
		if ( datum == null || datum.isEmpty() ) {
			return Collections.emptySet();
		}
		final Set<String> result = new TreeSet<>();
		for ( NodeDatum d : datum ) {
			result.add(d.getSourceId());
		}
		return result;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		Collection<NodeDatum> result = readMultipleDatum();
		return (result != null ? result.stream().findFirst().orElse(null) : null);
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return NodeDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		// check for cached result
		final CachedResult<List<NodeDatum>> c = cache.get();
		if ( c != null && c.isValid() ) {
			return c.getResult();
		}

		if ( !isConfigurationValid() ) {
			return Collections.emptyList();
		}

		final int[] dateTimeCols = this.dateTimeColumnIndexes;
		if ( dateTimeCols == null || dateTimeCols.length < 1 ) {
			return Collections.emptyList();
		}

		final DateTimeFormatter tsFormat = DateTimeFormatter.ofPattern(dateFormat)
				.withZone(ZoneId.of(timeZoneId));

		final List<NodeDatum> result = new ArrayList<>(4);

		final Collection<List<String>> rows = readDataRows(url(getUrl()), getCharset(), getSkipRows(),
				getKeepRows());
		final CsvPropertyConfig[] propConfigs = getPropConfigs();
		final StringBuilder buf = new StringBuilder(32);
		try {
			for ( List<String> row : rows ) {
				DatumSamples s = new DatumSamples();
				for ( CsvPropertyConfig propConfig : propConfigs ) {
					final int[] cols = propConfig.getColumnIndexes();
					if ( cols == null ) {
						continue;
					}
					buf.setLength(0);
					for ( int col : cols ) {
						if ( buf.length() > 0 ) {
							buf.append(' ');
						}
						buf.append(row.get(col));
					}
					String val = buf.toString();
					switch (propConfig.getPropertyType()) {
						case Accumulating:
						case Instantaneous:
							try {
								s.putSampleValue(propConfig.getPropertyType(),
										propConfig.getPropertyKey(), narrow(numberValue(val), 2));
							} catch ( NumberFormatException e ) {
								log.warn("Unable to parse CSV column {} value [{}] as a number: {}",
										propConfig.getColumn(), val, e.getMessage());
							}
							break;

						case Status:
							s.putStatusSampleValue(propConfig.getPropertyKey(), val);
							break;

						case Tag:
							s.addTag(val);
							break;
					}

				}
				if ( !s.isEmpty() ) {
					// extract datum timestamp from row
					buf.setLength(0);
					for ( int i = 0, len = dateTimeCols.length; i < len; i++ ) {
						if ( buf.length() > 0 ) {
							buf.append(' ');
						}
						buf.append(row.get(dateTimeCols[i]));
					}
					Instant ts = null;
					try {
						ts = tsFormat.parse(buf.toString(), Instant::from);
					} catch ( DateTimeParseException e ) {
						log.warn(
								"Error parsing CSV columns [{}] value [{}] as datum timestamp using pattern [{}]: {}",
								dateTimeColumn, buf, dateFormat);
						return Collections.emptyList();
					}

					final String sourceId = sourceId(row, buf);
					if ( sourceId == null || sourceId.trim().isEmpty() ) {
						continue;
					}

					SimpleDatum d = SimpleDatum.nodeDatum(sourceId, ts, s);
					result.add(d);
				}
			}
		} catch ( IndexOutOfBoundsException e ) {
			log.warn("Invalid CSV configuration: column index out of bounds: {}", e.getMessage());
		}

		final long cacheMs = getSampleCacheMs();
		if ( cacheMs > 0 ) {
			cache.compareAndSet(c,
					new CachedResult<List<NodeDatum>>(result, cacheMs, TimeUnit.MILLISECONDS));
		}
		return result;
	}

	private String url(String template) {
		final DateTimeFormatter fmt = this.urlDateFormatter;
		Map<String, Object> params = Collections.emptyMap();
		if ( fmt != null ) {
			params = Collections.singletonMap("date", fmt.format(Instant.now()));
		}
		return StringUtils.expandTemplateString(template, params);
	}

	private String sourceId(List<String> row, StringBuilder buf) {
		final int[] cols = sourceIdColumnIndexes;
		String sourceId = null;
		if ( cols != null ) {
			final int len = cols.length;
			if ( len == 1 ) {
				return row.get(cols[0]);
			}
			buf.setLength(0);
			for ( int i = 0; i < len; i++ ) {
				if ( buf.length() > 0 ) {
					buf.append(' ');
				}
				buf.append(row.get(cols[i]));
			}
			sourceId = buf.toString();
		} else {
			sourceId = getSourceId();
		}
		return resolvePlaceholders(sourceId);
	}

	/**
	 * Test if this instance has a valid configuration.
	 *
	 * @return {@literal true} if this instance has a valid configuration
	 */
	public boolean isConfigurationValid() {
		final String url = getUrl();
		if ( url == null || url.trim().isEmpty() ) {
			// no URL configured
			return false;

		}
		final String sourceId = getSourceId();
		final String sourceIdColumn = getSourceIdColumn();
		if ( (sourceId == null || sourceId.trim().isEmpty())
				&& (sourceIdColumn == null || sourceIdColumn.trim().isEmpty()) ) {
			// no source ID configured
			return false;
		}

		final CsvPropertyConfig[] configs = getPropConfigs();
		if ( configs == null || configs.length < 1 ) {
			// no properties configured
			return false;
		}

		if ( dateTimeColumnIndexes == null || dateTimeColumnIndexes.length < 1 ) {
			// no date column specified
			return false;
		}

		for ( CsvPropertyConfig config : configs ) {
			if ( config.isValid() ) {
				// we have at least 1 valid property, we're good
				return true;
			}
		}

		return false;
	}

	private InputStream fetchCsvResource(final String theUrl) throws IOException {
		ClientHttpRequestFactory reqFactory = service(httpRequestFactory);
		if ( reqFactory == null || !theUrl.startsWith("http") ) {
			return UrlUtils.getInputStreamFromURLConnection(
					UrlUtils.getURLConnection(theUrl, "GET", "text/*", connectionTimeout, null));
		}
		ClientHttpRequest req = reqFactory.createRequest(URI.create(theUrl), HttpMethod.GET);
		HttpRequestCustomizerService cust = service(httpRequestCustomizer);
		if ( cust != null ) {
			PlaceholderService phs = service(getPlaceholderService());
			Map<String, Object> parameters;
			if ( phs != null ) {
				parameters = new HashMap<>();
				phs.copyPlaceholders(parameters);
			} else {
				parameters = Collections.emptyMap();
			}
			ByteList body = new ByteList();
			req = cust.apply(reqFactory, req, body, parameters);
		}
		return req.execute().getBody();
	}

	private Collection<List<String>> readDataRows(final String theUrl, final Charset charset,
			final int skipRows, final int keepRows) {
		if ( log.isDebugEnabled() ) {
			log.debug("Requesting CSV data from [{}]", theUrl);
		}

		Collection<List<String>> rowBuffer = null;

		try (Reader in = new InputStreamReader(fetchCsvResource(theUrl), charset);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {

			int skipCount = skipRows;
			int rowCount = keepRows;
			if ( skipRows < 0 ) {
				skipCount = -skipRows;
				rowCount = -skipRows;
			}
			rowBuffer = (rowCount > 0 ? new LimitedSizeDeque<>(rowCount) : new ArrayList<>(16));

			List<String> row = null;
			while ( (row = csv.read()) != null ) {
				if ( skipRows > 0 && skipCount > 0 ) {
					--skipCount;
					continue;
				} else if ( skipRows < 0 ) {
					rowBuffer.add(row);
				} else {
					rowBuffer.add(row);
					if ( keepRows > 0 && --rowCount < 1 ) {
						break;
					}
				}
			}

			// with skip count negative, might have more than desired keep count in buffer
			if ( keepRows > 0 && skipCount > keepRows ) {
				List<List<String>> tmp = new ArrayList<>(keepRows);
				int count = keepRows;
				for ( List<String> tmpRow : rowBuffer ) {
					tmp.add(tmpRow);
					if ( --count < 1 ) {
						break;
					}
				}
				rowBuffer = tmp;
			}

			if ( log.isTraceEnabled() ) {
				log.trace("Read CSV data: {}", rowBuffer);
			}

			return rowBuffer;
		} catch ( IOException e ) {
			throw new RemoteServiceException(
					String.format("Error reading CSV resource [%s]: %s", theUrl, e.getMessage()), e);
		}
	}

	@Override
	public String getSettingUid() {
		return settingUid;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.addAll(getIdentifiableSettingSpecifiers());
		if ( includeSourceIdSetting ) {
			result.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		}
		result.add(new BasicTextFieldSettingSpecifier("url", null));
		result.add(new BasicTextFieldSettingSpecifier("httpRequestCustomizerUid", null, false,
				"(objectClass=net.solarnetwork.web.service.HttpRequestCustomizerService)"));
		result.add(new BasicTextFieldSettingSpecifier("charsetName", StandardCharsets.UTF_8.name()));
		result.add(new BasicTextFieldSettingSpecifier("connectionTimeout",
				String.valueOf(DEFAULT_CONNECTION_TIMEOUT)));
		result.add(new BasicTextFieldSettingSpecifier("skipRows", String.valueOf(DEFAULT_SKIP_ROWS)));
		result.add(new BasicTextFieldSettingSpecifier("keepRows", String.valueOf(DEFAULT_KEEP_ROWS)));
		if ( includeSourceIdSetting ) {
			result.add(new BasicTextFieldSettingSpecifier("sourceIdColumn", null));
		}
		result.add(new BasicTextFieldSettingSpecifier("urlDateFormat", DEFAULT_URL_DATE_FORMAT));
		result.add(new BasicTextFieldSettingSpecifier("dateTimeColumn", null));
		result.add(new BasicTextFieldSettingSpecifier("dateFormat", DEFAULT_DATE_FORMAT));
		result.add(new BasicTextFieldSettingSpecifier("timeZoneId", DEFAULT_TIME_ZONE_ID));

		result.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));

		CsvPropertyConfig[] confs = getPropConfigs();
		List<CsvPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<CsvPropertyConfig> emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingUtils.KeyedListCallback<CsvPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(CsvPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								CsvPropertyConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Get the source ID used for datum.
	 *
	 * <p>
	 * If {@link #getSourceIdColumn()} is configured, that will be used in
	 * preference to this value.
	 * </p>
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to use for returned datum.
	 *
	 * <p>
	 * If {@link #getPlaceholderService()} is configured then placeholder values
	 * will be resolved in the configured {@code sourceId}.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID to use
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
	 * Get the flag to include a source ID setting.
	 *
	 * @return {@literal true} to include a source ID setting in
	 *         {@link #getSettingSpecifiers()}
	 */
	public boolean isIncludeSourceIdSetting() {
		return includeSourceIdSetting;
	}

	/**
	 * Set the flag to include a source ID setting.
	 * <p>
	 * This can be disabled if this data source is proxied behind a
	 * {@link net.solarnetwork.node.service.support.LocationDatumDataSource},
	 * for example.
	 * </p>
	 *
	 * @param includeSourceIdSetting
	 *        {@literal true} to include a source ID setting in
	 *        {@link #getSettingSpecifiers()}
	 */
	public void setIncludeSourceIdSetting(boolean includeSourceIdSetting) {
		this.includeSourceIdSetting = includeSourceIdSetting;
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
	 * Get the character set the CSV resource should be parsed as.
	 *
	 * @return the character set, never {@literal null}
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Set the character set the CSV resource should be parsed as.
	 *
	 * @param charset
	 *        the character set to set; if {@literal null} then
	 *        {@link StandardCharsets#UTF_8} will be used
	 */
	public void setCharset(Charset charset) {
		this.charset = charset != null ? charset : StandardCharsets.UTF_8;
	}

	/**
	 * Get the character set name.
	 *
	 * @return the character set name, never {@literal null}
	 */
	public String getCharsetName() {
		return getCharset().name();
	}

	/**
	 * Set the character set the CSV resource should be parsed as, as a string
	 * name.
	 *
	 * @param name
	 *        the character set name to set; if invalid then
	 *        {@link StandardCharsets#UTF_8} will be used
	 */
	public void setCharsetName(String name) {
		Charset cs = null;
		try {
			cs = Charset.forName(name);
		} catch ( Exception e ) {
			// ignore and use default
		}
		setCharset(cs);
	}

	/**
	 * Get the URL connection timeout to apply when requesting the data.
	 *
	 * @return the connection timeout, in milliseconds; defaults to
	 *         {@link #DEFAULT_CONNECTION_TIMEOUT}
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * Set the URL connection timeout to apply when requesting the data.
	 *
	 * @param connectionTimeout
	 *        the timeout, in milliseconds
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Get the number of rows of CSV to skip.
	 *
	 * <p>
	 * When greater than {@literal 0} this will skip "header" rows. When
	 * {@literal 0} the first row will be used. When less than {@literal 0} then
	 * this line starting from the last available will be used, for example
	 * {@literal -1} will cause the last row to be used.
	 * </p>
	 *
	 * @return the number of rows to skip; defaults to
	 *         {@link #DEFAULT_SKIP_ROWS}
	 */
	public int getSkipRows() {
		return skipRows;
	}

	/**
	 * Set the number of rows of CSV to skip.
	 *
	 * @param skipRows
	 *        the number of rows, or {@literal 0} to not skip any rows
	 */
	public void setSkipRows(int skipRows) {
		this.skipRows = skipRows;
	}

	/**
	 * Get the number of rows of CSV to keep (turn into datum).
	 *
	 * @return the number of rows to keep; defaults to
	 *         {@link #DEFAULT_KEEP_ROWS}
	 */
	public int getKeepRows() {
		return keepRows;
	}

	/**
	 * Set the number of rows of CSV to keep (turn into datum).
	 *
	 * @param keepRows
	 *        the number of rows, or {@literal 0} to keep all rows
	 */
	public void setKeepRows(int keepRows) {
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
	 * @return the date time columns
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
	 * @return the date pattern; defaults to {@link #DEFAULT_DATE_FORMAT}.
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
	 * @return the date format; defaults to {@link #DEFAULT_URL_DATE_FORMAT}
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
	 * Get the sample cache maximum age, in milliseconds.
	 *
	 * @return the cache milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 *
	 * @param sampleCacheMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the property configurations
	 */
	public CsvPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Get the property configurations to use.
	 *
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(CsvPropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 *
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		CsvPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link CsvPropertyConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count, CsvPropertyConfig.class,
				null);
	}

	/**
	 * Get the optional HTTP request factory.
	 *
	 * @return the factory
	 */
	public OptionalService<ClientHttpRequestFactory> getHttpRequestFactory() {
		return httpRequestFactory;
	}

	/**
	 * Set the optional HTTP request factory.
	 *
	 * @param httpRequestFactory
	 *        the factory to set
	 */
	public void setHttpRequestFactory(OptionalService<ClientHttpRequestFactory> httpRequestFactory) {
		this.httpRequestFactory = httpRequestFactory;
	}

	/**
	 * An optional HTTP request customizer service.
	 *
	 * @return the service
	 */
	public OptionalFilterableService<HttpRequestCustomizerService> getHttpRequestCustomizer() {
		return httpRequestCustomizer;
	}

	/**
	 * An optional HTTP request customizer service.
	 *
	 * <p>
	 * If a {@link #getPlaceholderService()} is configured, all placeholder
	 * values will be provided to the customizer as parameters to the
	 * {@link HttpRequestCustomizerService#customize(org.springframework.http.HttpRequest, net.solarnetwork.util.ByteList, Map)}
	 * method.
	 * </p>
	 *
	 * @param httpRequestCustomizer
	 *        the service to set
	 */
	public void setHttpRequestCustomizer(
			OptionalFilterableService<HttpRequestCustomizerService> httpRequestCustomizer) {
		this.httpRequestCustomizer = httpRequestCustomizer;
	}

	/**
	 * Get the UID of the {@code HttpRequestCustomizerService} service to use.
	 *
	 * @return the service UID
	 */
	public String getHttpRequestCustomizerUid() {
		final OptionalFilterableService<HttpRequestCustomizerService> s = getHttpRequestCustomizer();
		return (s != null ? s.getPropertyValue(UID_PROPERTY) : null);
	}

	/**
	 * Set the UID of the {@code HttpRequestCustomizerService} service to use.
	 *
	 * @param uid
	 *        the service UID to set
	 */
	public void setHttpRequestCustomizerUid(String uid) {
		final OptionalFilterableService<HttpRequestCustomizerService> s = getHttpRequestCustomizer();
		if ( s != null ) {
			s.setPropertyFilter(UID_PROPERTY, uid);
		}
	}

}
