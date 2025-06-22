/* ==================================================================
 * XmlYrClient.java - 19/05/2017 3:27:03 PM
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.yr;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLConnection;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.service.support.XmlServiceSupport;

/**
 * Implementation of {@link YrClient} that queries XML weather resources.
 *
 * @author matt
 * @version 2.1
 */
public class XmlYrClient extends XmlServiceSupport implements YrClient {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_API_BASE_URL = "http://www.yr.no";

	private String baseUrl;

	private Map<String, XPathExpression> locationXPathMapping = Collections.emptyMap();
	private Map<String, XPathExpression> timeNodeXPathMapping = Collections.emptyMap();
	private XPathExpression forecastDataXPath;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 */
	public XmlYrClient() {
		super();
		setBaseUrl(DEFAULT_API_BASE_URL);
	}

	/**
	 * Post-configuration initialization method.
	 *
	 * <p>
	 * This method must be called once after all properties have been
	 * configured, before any other method is called.
	 * </p>
	 *
	 */
	@Override
	public void init() {
		super.init();
		try {
			setupLocationXPathMapping();
			setupTimeNodeXPathMapping();
			forecastDataXPath = getXpathFactory().newXPath().compile("forecast/tabular/time");
		} catch ( XPathExpressionException e ) {
			throw new RuntimeException("Error initializing XML parser", e);
		}
	}

	private void setupLocationXPathMapping() {
		/*-
		<location>
		<name>Glenorchy</name>
		<type>Locality</type>
		<country>New Zealand</country>
		<timezone id="Pacific/Auckland" utcoffsetMinutes="720"/>
		<location altitude="310" latitude="-44.84836" longitude="168.37007" geobase="geonames" geobaseid="6204577"/>
		</location>
		 */

		Map<String, String> xpaths = new LinkedHashMap<String, String>(8);
		xpaths.put("name", "location/name");
		xpaths.put("country", "location/country");
		xpaths.put("timeZoneId", "location/timezone/@id");
		xpaths.put("latitude", "location/location/@latitude");
		xpaths.put("longitude", "location/location/@longitude");
		locationXPathMapping = getXPathExpressionMap(xpaths);
	}

	private void setupTimeNodeXPathMapping() {
		/*-
		<time from="2017-05-10T11:00:00" to="2017-05-10T12:00:00">
		<!-- Valid from 2017-05-10T11:00:00 to 2017-05-10T12:00:00 -->
		<symbol number="3" numberEx="3" name="Partly cloudy" var="03d"/>
		<precipitation value="0"/>
		<!-- Valid at 2017-05-10T11:00:00 -->
		<windDirection deg="151.0" code="SSE" name="South-southeast"/>
		<windSpeed mps="0.6" name="Light air"/>
		<temperature unit="celsius" value="8"/>
		<pressure unit="hPa" value="1026.6"/>
		</time>
		 */
		Map<String, String> xpaths = new LinkedHashMap<String, String>(8);
		xpaths.put("createdTimestamp", "@from");
		xpaths.put("validToTimestamp", "@to");
		xpaths.put("skyConditions", "symbol/@name");
		xpaths.put("symbolVariable", "symbol/@var");
		xpaths.put("rainDecimal", "precipitation/@value");
		xpaths.put("windDirectionDecimal", "windDirection/@deg");
		xpaths.put("windSpeed", "windSpeed/@mps");
		xpaths.put("temperature", "temperature/@value");
		xpaths.put("atmosphericPressureHectopascal", "pressure/@value");
		timeNodeXPathMapping = getXPathExpressionMap(xpaths);
	}

	@Override
	public List<AtmosphericDatum> getHourlyForecast(String identifier) {
		if ( identifier == null ) {
			return null;
		}
		final String url = urlForPlacePath(identifier, "forecast_hour_by_hour.xml");
		List<AtmosphericDatum> results = getAtmosphericDatumList(identifier, url);
		for ( AtmosphericDatum datum : results ) {
			YrAtmosphericDatum d = (YrAtmosphericDatum) datum;
			d.addTag(AtmosphericDatum.TAG_FORECAST);
		}
		return results;
	}

	@Override
	public List<DayDatum> getTenDayForecast(String identifier) {
		if ( identifier == null ) {
			return null;
		}
		final String url = urlForPlacePath(identifier, "forecast.xml");
		List<AtmosphericDatum> forecast = getAtmosphericDatumList(identifier, url);

		// construct 2d array of atmos; grouped by day, to average out values for single DayDatum
		List<List<YrAtmosphericDatum>> dayRangeForecasts = new ArrayList<>(forecast.size() / 4 + 1);
		List<YrAtmosphericDatum> currDays = null;

		final ZoneId zone = (!forecast.isEmpty()
				? ZoneId.of(((YrAtmosphericDatum) forecast.get(0)).getLocation().getTimeZoneId())
				: ZoneOffset.UTC);
		final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);

		for ( AtmosphericDatum ad : forecast ) {
			YrAtmosphericDatum atmo = (YrAtmosphericDatum) ad;
			String hourMinute = timeFormat.format(atmo.getTimestamp());
			if ( "00:00".equals(hourMinute) ) {
				if ( currDays != null ) {
					dayRangeForecasts.add(currDays);
				}
				// start new day
				currDays = new ArrayList<YrAtmosphericDatum>(4);
			}
			if ( currDays != null ) {
				currDays.add(atmo);
			}
		}
		if ( currDays != null ) {
			dayRangeForecasts.add(currDays);
		}

		// average out into days now
		List<DayDatum> result = new ArrayList<DayDatum>(dayRangeForecasts.size());
		for ( List<YrAtmosphericDatum> dayRange : dayRangeForecasts ) {
			Map<String, DayAggregateValue> dayValues = new HashMap<String, DayAggregateValue>(8);

			for ( YrAtmosphericDatum atmo : dayRange ) {
				if ( atmo.getSamples().getInstantaneous() != null ) {
					for ( Map.Entry<String, Number> me : atmo.getSamples().getInstantaneous()
							.entrySet() ) {
						DayAggregateValue dayValue = dayValues.get(me.getKey());
						if ( dayValue == null ) {
							dayValue = new DayAggregateValue(me.getKey());
							dayValues.put(me.getKey(), dayValue);
						}
						dayValue.addValue(me.getValue());
					}
				}
				if ( atmo.getSamples().getAccumulating() != null ) {
					for ( Map.Entry<String, Number> me : atmo.getSamples().getAccumulating()
							.entrySet() ) {
						DayAggregateValue dayValue = dayValues.get(me.getKey());
						if ( dayValue == null ) {
							dayValue = new DayAggregateValue(me.getKey());
							dayValues.put(me.getKey(), dayValue);
						}
						dayValue.addValue(me.getValue());
					}
				}
				if ( atmo.getSamples().getStatus() != null ) {
					for ( Map.Entry<String, Object> me : atmo.getSamples().getStatus().entrySet() ) {
						DayAggregateValue dayValue = dayValues.get(me.getKey());
						if ( dayValue == null ) {
							dayValue = new DayAggregateValue(me.getKey());
							dayValues.put(me.getKey(), dayValue);
						}
						dayValue.addStatus(me.getValue().toString());
					}
				}
			}

			YrAtmosphericDatum first = dayRange.get(0);
			YrDayDatum dayDatum = new YrDayDatum(first.getTimestamp(), first.getLocation());
			dayDatum.addTag(DayDatum.TAG_FORECAST);

			if ( dayValues.containsKey(AtmosphericDatum.RAIN_KEY) ) {
				// total rain accumulation for the day
				dayDatum.setRain(dayValues.get(AtmosphericDatum.RAIN_KEY).tot.intValue());
			}
			if ( dayValues.containsKey(AtmosphericDatum.TEMPERATURE_KEY) ) {
				DayAggregateValue dv = dayValues.get(AtmosphericDatum.TEMPERATURE_KEY);
				dayDatum.setTemperatureMinimum(dv.min);
				dayDatum.setTemperatureMaximum(dv.max);
			}
			if ( dayValues.containsKey(AtmosphericDatum.WIND_DIRECTION_KEY) ) {
				dayDatum.setWindDirection(dayValues.get(AtmosphericDatum.WIND_DIRECTION_KEY).average()
						.setScale(0, RoundingMode.HALF_UP).intValue());
			}
			if ( dayValues.containsKey(AtmosphericDatum.WIND_SPEED_KEY) ) {
				dayDatum.setWindSpeed(dayValues.get(AtmosphericDatum.WIND_SPEED_KEY).max);
			}
			if ( dayValues.containsKey(AtmosphericDatum.SKY_CONDITIONS_KEY) ) {
				dayDatum.setSkyConditions(
						dayValues.get(AtmosphericDatum.SKY_CONDITIONS_KEY).mostFrequentStatus());
			}
			if ( dayValues.containsKey(YrAtmosphericDatum.SYMBOL_VAR_KEY) ) {
				dayDatum.getSamples().putStatusSampleValue(YrDayDatum.SYMBOL_VAR_KEY,
						dayValues.get(YrAtmosphericDatum.SYMBOL_VAR_KEY).mostFrequentStatus());
			}
			result.add(dayDatum);
		}

		return result;
	}

	private List<AtmosphericDatum> getAtmosphericDatumList(String identifier, final String url) {
		List<AtmosphericDatum> results = new ArrayList<AtmosphericDatum>();
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			InputSource is = getInputSourceFromURLConnection(conn);
			Document doc = getDocBuilderFactory().newDocumentBuilder().parse(is);
			Element root = doc.getDocumentElement();

			// get location info
			BasicYrLocation loc = new BasicYrLocation();
			loc.setIdentifier(identifier);
			extractBeanDataFromXml(loc, root, locationXPathMapping);

			final DateTimeFormatter parseDateFormat = xmlTimestampParseDateFormat(loc.getTimeZoneId());

			NodeList timeNodes = (NodeList) forecastDataXPath.evaluate(root, XPathConstants.NODESET);
			for ( int i = 0, len = timeNodes.getLength(); i < len; i++ ) {
				Node n = timeNodes.item(i);
				YrAtmosphericDatum datum = new YrAtmosphericDatum(loc, parseDateFormat);
				extractBeanDataFromXml(datum, n, timeNodeXPathMapping);
				if ( datum.getFromTimestamp() != null && !datum.isEmpty() ) {
					results.add(datum.copyUsingFromTimestamp());
				} else {
					String s = getXmlAsString(new DOMSource(n), false);
					log.warn("Ignoring hourly datum that did not include any data: {}", s);
				}
			}
		} catch ( IOException e ) {
			log.warn("Error reading Yr URL [{}]: {}", url, e.getMessage());
		} catch ( SAXException e ) {
			throw new RuntimeException("Error parsing Yr XML returned from URL [" + url + "]", e);
		} catch ( XPathExpressionException e ) {
			throw new RuntimeException("Error evaluating Yr XML returned from URL [" + url + "]", e);
		} catch ( ParserConfigurationException e ) {
			throw new RuntimeException("Error initializing XML parser", e);
		}
		return results;
	}

	private static final BigDecimal getBigDecimal(Number n) {
		if ( n instanceof BigDecimal ) {
			return (BigDecimal) n;
		}
		try {
			return new BigDecimal(n.toString());
		} catch ( NumberFormatException e ) {
			return null;
		}
	}

	private DateTimeFormatter xmlTimestampParseDateFormat(String timeZoneId) {
		return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of(timeZoneId));
	}

	private String urlForPlacePath(String identifier, String resource) {
		return (baseUrl + "/place" + identifier + '/' + resource);
	}

	/**
	 * Helper class for aggregating forecast values over a day.
	 */
	private static class DayAggregateValue {

		@SuppressWarnings("unused")
		private final String key;

		private BigDecimal min = null;
		private BigDecimal max = null;
		private BigDecimal tot = null;
		private int count = 0;

		private BigDecimal prev = null;
		private BigDecimal acc = new BigDecimal(0);

		private final Map<String, Integer> statusValues = new LinkedHashMap<String, Integer>();

		private DayAggregateValue(String key) {
			super();
			this.key = key;
		}

		private void addValue(Number n) {
			count += 1;
			BigDecimal bd = getBigDecimal(n);
			if ( min == null ) {
				min = bd;
			} else if ( min.compareTo(bd) > 0 ) {
				min = bd;
			}
			if ( max == null ) {
				max = bd;
			} else if ( max.compareTo(bd) < 0 ) {
				max = bd;
			}
			if ( tot == null ) {
				tot = bd;
			} else {
				tot = tot.add(bd);
			}
			if ( prev == null ) {
				prev = bd;
			} else {
				acc = acc.add(bd.subtract(prev));
			}
			prev = bd;
		}

		private void addStatus(String s) {
			Integer count = statusValues.get(s);
			if ( count == null ) {
				count = Integer.valueOf(1);
			} else {
				count = count.intValue() + 1;
			}
			statusValues.put(s, count);
		}

		private BigDecimal average() {
			return tot.divide(new BigDecimal(count));
		}

		private String mostFrequentStatus() {
			String result = null;
			int max = 0;
			for ( Map.Entry<String, Integer> me : statusValues.entrySet() ) {
				if ( me.getValue() > max ) {
					max = me.getValue();
					result = me.getKey();
				}
			}
			return result;
		}
	}

	/**
	 * Set the base URL for the Yr service.
	 *
	 * @param baseUrl
	 *        the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

}
