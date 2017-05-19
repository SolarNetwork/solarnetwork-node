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
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.support.XmlServiceSupport;

/**
 * Implementation of {@link YrClient} that queries XML weather resources.
 * 
 * @author matt
 * @version 1.0
 */
public class XmlYrClient extends XmlServiceSupport implements YrClient {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_API_BASE_URL = "http://www.yr.no/";

	private String baseUrl;

	private Map<String, XPathExpression> locationXPathMapping = Collections.emptyMap();
	private Map<String, XPathExpression> timeNodeXPathMapping = Collections.emptyMap();
	private XPathExpression forecastDataXPath;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public XmlYrClient() {
		super();
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

			final DateFormat parseDateFormat = xmlTimestampParseDateFormat(loc.getTimeZoneId());
			final DateFormat dateFormat = iso8601TimestampDateFormat();

			NodeList timeNodes = (NodeList) forecastDataXPath.evaluate(root, XPathConstants.NODESET);
			for ( int i = 0, len = timeNodes.getLength(); i < len; i++ ) {
				Node n = timeNodes.item(i);
				YrAtmosphericDatum datum = new YrAtmosphericDatum(loc, parseDateFormat, dateFormat);
				extractBeanDataFromXml(datum, n, timeNodeXPathMapping);
				if ( datum.getCreated() != null && datum.getSamples().getInstantaneous() != null ) {
					results.add(datum);
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

	private DateFormat xmlTimestampParseDateFormat(String timeZoneId) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone(timeZoneId));
		return sdf;
	}

	private DateFormat iso8601TimestampDateFormat() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf;
	}

	private String urlForPlacePath(String identifier, String resource) {
		return (baseUrl + "/place" + identifier + '/' + resource);
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
