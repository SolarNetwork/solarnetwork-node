/* ==================================================================
 * XmlEGaugeClient.java - 9/03/2018 12:45:52 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.egauge.ws.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathExpression;
import net.solarnetwork.node.datum.egauge.ws.EGaugePowerDatum;
import net.solarnetwork.node.support.XmlServiceSupport;

/**
 * XML implementation of the EGaugeClient. Instances of this can be shared
 * between EGaugeDatumDataSource instances.
 * 
 * The {@code xpathMap} configuration should match the content expected to be
 * returned by the {@code queryUrl}.
 * 
 * @author maxieduncan
 * @version 1.0
 */
public class XmlEGaugeClient extends XmlServiceSupport implements EGaugeClient {

	/** The number of seconds in an hour, used for conversion. */
	private static final int HOUR_SECONDS = 3600;

	/**
	 * The default query URL that returns the XML data we will apply the XPath
	 * mappings to.
	 */
	private static final String DEAFAULT_QUERY_URL = "/cgi-bin/egauge?inst";

	/** The URL that should be used to retrieve the eGauge XML. */
	private String queryUrl = DEAFAULT_QUERY_URL;

	/** The XPath mappings that will be used to read the file. */
	private Map<String, XPathExpression> xpathMapping;
	/**
	 * Used to set the {@link #getXpathMapping()} values. Not kept in sync if
	 * {@link #getXpathMapping()} is edited directly.
	 */
	private Map<String, String> xpathMap;

	/**
	 * 
	 * @see net.solarnetwork.node.datum.egauge.ws.client.EGaugeClient#getCurrent()
	 */
	@Override
	public EGaugePowerDatum getCurrent(String host, String sourceId) {
		String url = getUrl(host);
		EGaugePowerDatum datum = new EGaugePowerDatum();
		datum.setCreated(new Date());
		datum.setSourceId(sourceId);

		try {
			webFormGetForBean(null, datum, url, null, xpathMapping);
		} catch ( RuntimeException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}

			/*
			 * TODO review, this behaviour mimics what was done in
			 * EnaSolarXMLDatumDataSource except the IOException is swallowed.
			 * It may be that we just want to throw this exception here?
			 */
			if ( root instanceof IOException ) {
				// turn this into a WARN only
				log.warn("Error communicating with eGauge inverter at {}: {}", url, e.getMessage());

				// with a stacktrace in DEBUG
				log.debug("IOException communicating with eGauge inverter at {}", url, e);

				// Note that this means that the exception is swallowed and EGaugePowerDatum won't be able to store a reference in sampleException
				return null;
			}

			throw e;
		}

		if ( datum != null ) {
			convertEGaugeValues(datum);
			//			addEnergyDatumSourceMetadata(datum);
			postDatumCapturedEvent(datum);
		}
		return datum;
	}

	/**
	 * eGauge store the power readings in Watt-seconds while we store them in
	 * Watt-hours so the units need to be converted.
	 * 
	 * @param datum
	 *        The instance containing the eGauge readings to be updated.
	 */
	protected void convertEGaugeValues(EGaugePowerDatum datum) {
		datum.setSolarPlusWattHourReading(datum.getSolarPlusWattHourReading() / HOUR_SECONDS); //  TODO review rounding
		datum.setGridWattHourReading(datum.getGridWattHourReading() / HOUR_SECONDS);
	}

	/**
	 * Constructs the URL that should be used to retrieve the eGauge data.
	 * 
	 * @param host
	 *        The host to get the data from. Used in conjunction with
	 *        {@link #getQueryUrl()}.
	 * @return the URL that should be used to retrieve the eGauge data.
	 */
	protected String getUrl(String host) {
		return (host == null ? "" : host) + getQueryUrl();
	}

	//	private void addEnergyDatumSourceMetadata(EGaugePowerDatum d) {
	//		if ( d == null ) {
	//			return;
	//		}
	//		// associate consumption/generation tags with this source
	//		GeneralDatumMetadata sourceMeta = new GeneralDatumMetadata();
	//		// TODO review I think we have both generation and consumption data in this datum so is this the correct tag?
	//		sourceMeta.addTag(net.solarnetwork.node.domain.EnergyDatum.TAG_GENERATION);
	//		addSourceMetadata(d.getSourceId(), sourceMeta);
	//	}

	@Override
	public void init() {
		super.init();
		if ( xpathMapping == null ) {
			setXpathMap(defaultXpathMap());
		}
		if ( getQueryUrl() == null ) {
			setQueryUrl(DEAFAULT_QUERY_URL);
		}
	}

	/**
	 * The default eGauge XPATH mappings to use when none are provided.
	 * 
	 * @return tThe default eGauge XPATH mappings.
	 */
	private static Map<String, String> defaultXpathMap() {
		Map<String, String> result = new LinkedHashMap<String, String>(10);
		result.put("solarPlusWatts", "r[@n='Solar+'][1]/i");
		result.put("solarPlusWattHourReading", "r[@n='Solar+'][1]/v");
		result.put("gridWatts", "r[@n='Grid'][1]/i");
		result.put("gridWattHourReading", "r[@n='Grid'][1]/v");
		return result;
	}

	/**
	 * Provides access to the internal XPath map.
	 * 
	 * @return the XPath map if set.
	 */
	protected Map<String, String> getXpathMap() {
		return xpathMap;
	}

	/**
	 * Set the XPath mapping using String values.
	 * 
	 * @param xpathMap
	 *        the string XPath mapping values
	 */
	public void setXpathMap(Map<String, String> xpathMap) {
		this.xpathMap = xpathMap;
		setXpathMapping(getXPathExpressionMap(xpathMap));
	}

	/**
	 * Gets the XML data mapping as a String from the {@link #getXpathMapping()}
	 * configuration.
	 * 
	 * @return the data mapping.
	 */
	public String getDataMapping() {
		return getXpathMap().entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining(","));
	}

	/**
	 * Set the XML data mapping. The string should be a comma separated list of
	 * property names to XPath paths.
	 * 
	 * 
	 * @param mapping
	 *        comma-delimited equal-delimited key value pair list
	 */
	public void setDataMapping(String mapping) {
		if ( mapping == null || mapping.length() < 1 ) {
			return;
		}
		// Split the string by "comma" then the first "equals" to get key/value pairs
		Map<String, String> map = Arrays.stream(mapping.split("\\s*,\\s*"))
				.map(s -> s.split("\\s*=\\s*", 2))
				.collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
		setXpathMap(map);
	}

	public Map<String, XPathExpression> getXpathMapping() {
		return xpathMapping;
	}

	/**
	 * Set a mapping of PowerDatum JavaBean property names to corresponding
	 * XPath expressions for extracting the data from the XML response.
	 * 
	 * <p>
	 * This property can also be configured via {@link #setXpathMap(Map)} using
	 * String values, which is useful when using Spring. Defaults to a sensible
	 * value, so this should only be configured in special cases.
	 * </p>
	 * 
	 * @param xpathMapping
	 *        the mapping to set
	 */
	public void setXpathMapping(Map<String, XPathExpression> xpathMapping) {
		this.xpathMapping = xpathMapping;
	}

	/**
	 * The URL used to query the eGauge meter.
	 * 
	 * @return the eGauge query URL.
	 */
	public String getQueryUrl() {
		return queryUrl;
	}

	/**
	 * Sets the URL used to query the eGauge meter.
	 * 
	 * @param url
	 *        the eGauge query URL.
	 */
	public void setQueryUrl(String url) {
		this.queryUrl = url;
	}

}
