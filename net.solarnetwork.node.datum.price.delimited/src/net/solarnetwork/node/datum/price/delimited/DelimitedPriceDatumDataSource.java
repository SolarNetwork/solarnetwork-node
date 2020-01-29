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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralPriceDatum;
import net.solarnetwork.node.domain.PriceDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;
import net.solarnetwork.node.util.LimitedSizeDeque;
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
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>url</dt>
 * <dd>The URL template for accessing the delimited price data from. This will
 * be passed through {@link String#format(String, Object...)} with the current
 * date as the only parameter, allowing the URL to contain a date requeset
 * parameter if needed. For example, a value of
 * {@code http://some.place/prices?date=%1$tY-%1$tm-%1$td} would resolve to
 * something like {@code http://some.place/prices?date=2009-08-08}.</dd>
 * 
 * <dt>delimiter</dt>
 * <dd>A regular expression delimiter to split the lines of text with. Defaults
 * to {@link #DEFAULT_DELIMITER}.</dd>
 * 
 * <dt>skipLines</dt>
 * <dd>The number of lines of text to skip. This is useful for skipping a
 * "header" row with column names. Defaults to {@code 1}.</dd>
 * 
 * <dt>connectionTimeout</dt>
 * <dd>A URL connection timeout to apply when requesting the data. Defaults to
 * {@link #DEFAULT_CONNECTION_TIMEOUT}.</dd>
 * 
 * <dt>priceColumn</dt>
 * <dd>The result column index for the price. This is assumed to be parsable as
 * a double value.</dd>
 * 
 * <dt>sourceIdColumn</dt>
 * <dd>An optional column index to use for the {@link PriceDatum#getSourceId()}
 * value. If not configured, the URL used to request the data will be used.</dd>
 * 
 * <dt>dateTimeColumns</dt>
 * <dd>An array of column indices to use as the {@link PriceDatum#getCreated()}
 * value. This is provided as an array in case the date and time of the price is
 * split across multiple columns. If multiple columns are configured, they will
 * be joined with a space character before parsing the result into a Date
 * object.</dd>
 * 
 * <dt>dateFormat</dt>
 * <dd>The {@link SimpleDateFormat} format to use for parsing the price date
 * value into a Date object. Defaults to {@link #DEFAULT_DATE_FORMAT}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.3
 */
public class DelimitedPriceDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource<PriceDatum>, SettingSpecifierProvider {

	/** The default value for the {@code delimiter} property. */
	public static final String DEFAULT_DELIMITER = ",";

	/** The default value for the {@code connectionTimeout} property. */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 15000;

	/** The default value for the {@code dateFormat} property. */
	public static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";

	/** The default value for the {@code url} property. */
	public static final String DEFAULT_URL = "https://www2.electricityinfo.co.nz/download/prices?search_form%5Brun_types%5D%5B%5D=I&search_form%5Bmarket_types%5D%5B%5D=E&search_form%5Bnodes%5D%5B%5D={stationId}&search_form%5Bdate_from%5D={date}&search_form%5Btp_from%5D=1&search_form%5Bdate_to%5D={date}&search_form%5Btp_to%5D=48&search_form%5Btp_roll_back%5D=2&search_form%5Btp_roll_fwd%5D=1";

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
	public Class<? extends PriceDatum> getDatumType() {
		return GeneralPriceDatum.class;
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
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		Date created;
		try {
			created = sdf.parse(dateTimeStr);
		} catch ( ParseException e ) {
			throw new RuntimeException(e);
		}

		BigDecimal price = new BigDecimal(data[priceColumn]);

		GeneralPriceDatum datum = new GeneralPriceDatum();
		datum.setCreated(created);
		datum.setPrice(price);
		return datum;
	}

	private URL getFormattedUrl() {
		SimpleDateFormat sdf = new SimpleDateFormat(urlDateFormat);
		sdf.setTimeZone(TimeZone.getTimeZone(timeZoneId));
		String today = sdf.format(new Date());
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
	public String getSettingUID() {
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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getSkipLines() {
		return skipLines;
	}

	public void setSkipLines(int skipLines) {
		this.skipLines = skipLines;
	}

	public int[] getDateTimeColumns() {
		return dateTimeColumns;
	}

	public void setDateTimeColumns(int[] dateTimeColumns) {
		this.dateTimeColumns = dateTimeColumns;
	}

	public int getPriceColumn() {
		return priceColumn;
	}

	public void setPriceColumn(int priceColumn) {
		this.priceColumn = priceColumn;
	}

	public String getDateFormat() {
		return dateFormat;
	}

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
	 * @param timeZone
	 *        the timeZone to set
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
