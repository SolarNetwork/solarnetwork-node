/* ===================================================================
 * DelimitedPriceDatumDataSource.java
 *
 * Created Aug 8, 2009 2:09:30 PM
 *
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.node.datum.price.delimited;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.PriceDatum;
import net.solarnetwork.node.domain.datum.SimplePriceDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.LimitedSizeDeque;
import net.solarnetwork.util.StringUtils;

/**
 * Implementation of {@link DatumDataSource} that parses a delimited text
 * resource from a URL.
 *
 * <p>
 * This class will make a URL request and parse the returned text as delimited
 * lines of data. The references to <em>columns</em> in the class properties
 * refer to zero-based column numbers created after splitting the line of data
 * into an array using the configured delimiter.
 * </p>
 *
 * @author matt
 * @version 2.2
 */
public class DelimitedPriceDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

	/** The default value for the {@code delimiter} property. */
	public static final String DEFAULT_DELIMITER = ",";

	/** The default value for the {@code connectionTimeout} property. */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 15000;

	/** The default value for the {@code dateFormat} property. */
	public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";

	/** The default value for the {@code url} property. */
	public static final String DEFAULT_URL = "https://www2.electricityinfo.co.nz/download/prices?search_form%5Brun_types%5D%5B%5D=D&search_form%5Bmarket_types%5D%5B%5D=E&search_form%5Bnodes%5D%5B%5D={stationId}&search_form%5Bdate_from%5D={date}&search_form%5Btp_from%5D=1&search_form%5Bdate_to%5D={date}&search_form%5Btp_to%5D=48&search_form%5Btp_roll_back%5D=0&search_form%5Btp_roll_fwd%5D=1";

	/** The default value for the {@code priceColumn} property. */
	public static final int DEFAULT_PRICE_COLUMN = 3;

	/** The default value for the {@code skipLines} property. */
	public static final int DEFAULT_SKIP_LINES = -1;

	/**
	 * The default value for the {@code stationId} property.
	 *
	 * @since 1.3
	 */
	public static final String DEFAULT_STATION_ID = "HAY2201";

	/**
	 * The default value for the {@link timeZoneId} property.
	 *
	 * @since 1.3
	 */
	public static final String DEFAULT_TIME_ZONE_ID = TimeZone.getDefault().getID();

	/**
	 * The default value for the {@code urlDateFormat} property.
	 *
	 * @since 1.3
	 */
	public static final String DEFAULT_URL_DATE_FORMAT = "yyyy-MM-dd";

	/** The default date time columns. */
	public static final int[] DEFAULT_DATE_TIME_COLUMNS = new int[] { 6 };

	private final Logger log = LoggerFactory.getLogger(DelimitedPriceDatumDataSource.class);

	private MessageSource messageSource;
	private String url = DEFAULT_URL;
	private String delimiter = DEFAULT_DELIMITER;
	private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
	private int skipLines = DEFAULT_SKIP_LINES;
	private int[] dateTimeColumns = DEFAULT_DATE_TIME_COLUMNS;
	private int priceColumn = DEFAULT_PRICE_COLUMN;
	private String dateFormat = DEFAULT_DATE_FORMAT;
	private String urlDateFormat = DEFAULT_URL_DATE_FORMAT;
	private String timeZoneId = DEFAULT_TIME_ZONE_ID;
	private String stationId = DEFAULT_STATION_ID;

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return PriceDatum.class;
	}

	@Override
	public String toString() {
		String host = "";
		try {
			URL theUrl = getFormattedUrl();
			host = theUrl.getHost();
		} catch ( Exception e ) {
			host = "unknown";
		}
		return "DelimitedPriceDatumDataSource{" + host + "}";
	}

	@Override
	public PriceDatum readCurrentDatum() {
		URL theUrl = getFormattedUrl();
		String dataRow = readDataRow(theUrl);
		if ( dataRow == null ) {
			return null;
		}
		String[] data = dataRow.split(this.delimiter);

		// get price date, either from single column or combination of multiple
		// which might occur if date and time are in different columns
		String dateTimeStr = null;
		if ( dateTimeColumns.length == 1 ) {
			dateTimeStr = data[dateTimeColumns[0]];
		} else {
			StringBuilder buf = new StringBuilder();
			for ( int idx : dateTimeColumns ) {
				if ( buf.length() > 0 ) {
					buf.append(' ');
				}
				buf.append(data[idx]);
			}
			dateTimeStr = buf.toString();
		}
		if ( log.isTraceEnabled() ) {
			log.trace("Parsing price date [" + dateTimeStr + ']');
		}
		Instant created;
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat)
					.withZone(ZoneId.of(timeZoneId));
			created = formatter.parse(dateTimeStr, Instant::from);
		} catch ( DateTimeParseException e ) {
			log.error("Error parsing price date from columns {} value [{}]: {}",
					Arrays.toString(dateTimeColumns), dateTimeStr, e.getMessage());
			return null;
		}

		BigDecimal price = new BigDecimal(data[priceColumn]);

		SimplePriceDatum datum = new SimplePriceDatum(null, null, created, new DatumSamples());
		datum.setPrice(price);
		return datum;
	}

	private URL getFormattedUrl() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(urlDateFormat)
				.withZone(ZoneId.of(timeZoneId));
		String today = formatter.format(Instant.now());
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("stationId", this.stationId);
		variables.put("date", today);
		String theUrl = StringUtils.expandTemplateString(this.url, variables);
		try {
			return new URL(theUrl);
		} catch ( MalformedURLException e ) {
			throw new RuntimeException(e);
		}
	}

	private String readDataRow(URL theUrl) {
		BufferedReader resp = null;
		if ( log.isDebugEnabled() ) {
			log.debug("Requesting price data from [{}]", theUrl);
		}
		try {
			URLConnection conn = theUrl.openConnection();
			conn.setConnectTimeout(this.connectionTimeout);
			conn.setReadTimeout(this.connectionTimeout);
			conn.setRequestProperty("Accept", "text/*");

			resp = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			if ( conn instanceof HttpURLConnection ) {
				HttpURLConnection hconn = (HttpURLConnection) conn;
				int status = hconn.getResponseCode();
				if ( status < 200 || status > 299 ) {
					log.warn("Non-200 response {} from [{}]; headers:\n", status, theUrl,
							conn.getHeaderFields());
					return null;
				}
			}

			String str = null;
			Deque<String> lineBuffer = null;
			int skipCount = 0;
			if ( skipLines > 0 ) {
				skipCount = this.skipLines;
			} else if ( skipLines < 0 ) {
				lineBuffer = new LimitedSizeDeque<String>(-skipLines);
			}
			while ( (str = resp.readLine()) != null ) {
				if ( skipCount > 0 ) {
					skipCount--;
					continue;
				} else if ( skipLines < 0 ) {
					lineBuffer.add(str);
				} else {
					break;
				}
			}
			if ( lineBuffer != null ) {
				str = lineBuffer.getFirst();
			}
			if ( log.isTraceEnabled() ) {
				log.trace("Found price data: {}", str);
			}
			return str;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			if ( resp != null ) {
				try {
					resp.close();
				} catch ( IOException e ) {
					// ignore this
					log.debug("Exception closing URL stream", e);
				}
			}
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.price.delimited";
	}

	@Override
	public String getDisplayName() {
		return "Delimited energy price lookup";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.addAll(getIdentifiableSettingSpecifiers());
		result.add(new BasicTextFieldSettingSpecifier("url", DEFAULT_URL));
		result.add(new BasicTextFieldSettingSpecifier("stationId", DEFAULT_STATION_ID));
		result.add(new BasicTextFieldSettingSpecifier("timeZoneId", DEFAULT_TIME_ZONE_ID));
		result.add(new BasicTextFieldSettingSpecifier("urlDateFormat", DEFAULT_URL_DATE_FORMAT));
		result.add(new BasicTextFieldSettingSpecifier("delimiter", DEFAULT_DELIMITER));
		result.add(
				new BasicTextFieldSettingSpecifier("priceColumn", String.valueOf(DEFAULT_PRICE_COLUMN)));
		result.add(new BasicTextFieldSettingSpecifier("dateTimeColumns", "1,3"));
		result.add(new BasicTextFieldSettingSpecifier("dateFormat", DEFAULT_DATE_FORMAT));
		result.add(new BasicTextFieldSettingSpecifier("skipLines", String.valueOf(DEFAULT_SKIP_LINES)));
		return result;
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
	 * with the current date and the configured {@link #getStationId()} as
	 * <code>{date}</code> and <code>{stationId</code> parameters. The date
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
	 * Get the field delimiter regular expression.
	 *
	 * @return the field delimiter; defaults to {@link #DEFAULT_DELIMITER}
	 */
	public String getDelimiter() {
		return delimiter;
	}

	/**
	 * Set the field delimiter regular expression.
	 * <p>
	 * A regular expression delimiter that will be used to split the lines of
	 * text into fields.
	 * </p>
	 *
	 * @param delimiter
	 *        the field delimiter
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
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
	 * Get the number of lines of text to skip.
	 *
	 * <p>
	 * When greater than {@literal 0} this will skip "header" rows. When
	 * {@literal 0} the first line will be used. When less than {@literal 0}
	 * then this line starting from the last available will be used, for example
	 * {@literal -1} will cause the last line to be used.
	 * </p>
	 *
	 * @return the number of lines to skip; defaults to
	 *         {@link #DEFAULT_SKIP_LINES}
	 */
	public int getSkipLines() {
		return skipLines;
	}

	/**
	 * Set the number of lines of text to skip.
	 *
	 * @param skipLines
	 *        the number of lines, or {@literal 0} to not skip any lines
	 */
	public void setSkipLines(int skipLines) {
		this.skipLines = skipLines;
	}

	/**
	 * Get an array of {@literal 0}-based column indices to use as the time
	 * stamp value for datum.
	 *
	 * <p>
	 * This is provided as an array in case the date and time of the price is
	 * split across multiple columns. If multiple columns are configured, they
	 * will be joined with a space character before parsing the result into a
	 * time stamp value
	 * </p>
	 *
	 * @return the date time columns; defaults to
	 *         {@link #DEFAULT_DATE_TIME_COLUMNS}
	 */
	public int[] getDateTimeColumns() {
		return dateTimeColumns;
	}

	/**
	 * Set an array of {@literal 0}-based column indices to use as the time
	 * stamp value for datum.
	 *
	 * @param dateTimeColumns
	 *        the column indexes to use for time stamps
	 */
	public void setDateTimeColumns(int[] dateTimeColumns) {
		this.dateTimeColumns = dateTimeColumns;
	}

	/**
	 * Get the {@literal 0}-based result column index for the price.
	 *
	 * <p>
	 * This is assumed to be parsable as a double value.
	 * </p>
	 *
	 * @return the price column index
	 */
	public int getPriceColumn() {
		return priceColumn;
	}

	/**
	 * Set the {@literal 0}-based result column index for the price.
	 *
	 * @param priceColumn
	 *        the price column index
	 */
	public void setPriceColumn(int priceColumn) {
		this.priceColumn = priceColumn;
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
	 * Get the electricity market station ID.
	 *
	 * @return the stationId; defaults to {@link #DEFAULT_STATION_ID}
	 * @since 1.3
	 */
	public String getStationId() {
		return stationId;
	}

	/**
	 * Set the electricity market station ID to download data for.
	 *
	 * @param stationId
	 *        the stationId to set
	 * @since 1.3
	 */
	public void setStationId(String stationId) {
		this.stationId = stationId;
	}

	/**
	 * Get the time zone to use for dates.
	 *
	 * @return the time zone; defaults to the system default time zone
	 * @since 1.3
	 */
	public String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Get the time zone to use for dates.
	 *
	 * @param timeZoneId
	 *        the time zone to set
	 * @since 1.3
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

}
