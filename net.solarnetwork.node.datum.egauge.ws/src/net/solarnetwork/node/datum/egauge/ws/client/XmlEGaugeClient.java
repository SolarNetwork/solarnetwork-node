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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import net.solarnetwork.node.datum.egauge.ws.EGaugeDatumDataSource;
import net.solarnetwork.node.datum.egauge.ws.EGaugePowerDatum;
import net.solarnetwork.node.datum.egauge.ws.EGaugePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.EGaugePropertyConfig.EGaugeReadingType;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
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

	/**
	 * The default query URL that returns the XML data we will apply the XPath
	 * mappings to.
	 */
	private static final String DEAFAULT_INST_QUERY_URL = "/cgi-bin/egauge?inst";
	private static final String DEAFAULT_TOT_QUERY_URL = "/cgi-bin/egauge?tot";

	/**
	 * The URL that should be used to retrieve the instantanseous eGauge XML.
	 */
	private String instantaneousQueryUrl = DEAFAULT_INST_QUERY_URL;
	/**
	 * The URL that should be used to retrieve the instantanseous eGauge XML.
	 */
	private String totalQueryUrl = DEAFAULT_TOT_QUERY_URL;

	private String host;

	@Override
	public List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "host", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "instantaneousQueryUrl",
				DEAFAULT_INST_QUERY_URL));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "totalQueryUrl", DEAFAULT_TOT_QUERY_URL));

		return results;
	}

	/**
	 * 
	 * @see net.solarnetwork.node.datum.egauge.ws.client.EGaugeClient#getCurrent()
	 */
	@Override
	public EGaugePowerDatum getCurrent(EGaugeDatumDataSource source) {
		EGaugePowerDatum datum = new EGaugePowerDatum();
		datum.setCreated(new Date());
		datum.setSourceId(source.getSourceId());

		try {
			populateDatum(datum, source);
		} catch ( XmlEGaugeClientException e ) {
			// An exception has been encountered and logged but we need to make sure no datum is returned
			return null;
		}

		if ( datum != null ) {
			postDatumCapturedEvent(datum);
		}
		return datum;
	}

	/**
	 * Constructs the URL that should be used to retrieve the eGauge data.
	 * 
	 * @param host
	 *        The host to get the data from. Used in conjunction with
	 *        {@link #getQueryUrl()}.
	 * @return the URL that should be used to retrieve the eGauge data.
	 */
	protected String getUrl(String host, EGaugeReadingType type) {
		return (host == null ? "" : host) + getQueryUrl(type);
	}

	protected String getQueryUrl(EGaugeReadingType type) {
		switch (type) {
			case INSTANTANEOUS:
				return getInstantaneousQueryUrl();
			case TOTAL:
				return getTotalQueryUrl();
			default:
				throw new UnsupportedOperationException("Unkown register type: " + type);
		}
	}

	protected void populateDatum(EGaugePowerDatum datum, EGaugeDatumDataSource source)
			throws XmlEGaugeClientException {
		Map<EGaugeReadingType, Element> documentCache = new HashMap<>();

		EGaugePropertyConfig[] configs = source.getPropertyConfigs();
		if ( configs != null ) {
			for ( EGaugePropertyConfig propertyConfig : configs ) {
				Element xml = getXml(propertyConfig.getReadingType(), documentCache);
				if ( xml != null ) {
					populateDatumProperty(datum, propertyConfig, xml);
				}
			}
		}

	}

	protected void populateDatumProperty(EGaugePowerDatum datum, EGaugePropertyConfig propertyConfig,
			Element xml) {

		String xpathBase = "r[@n='" + propertyConfig.getRegisterName() + "'][1]";

		try {
			String propertyType = (String) getXPathExpression(xpathBase + "/@t").evaluate(xml,
					XPathConstants.STRING);
			String value = (String) getXPathExpression(xpathBase + "/v").evaluate(xml,
					XPathConstants.STRING);

			switch (propertyConfig.getReadingType()) {
				case INSTANTANEOUS:
					String instantenouseValue = (String) getXPathExpression(xpathBase + "/i")
							.evaluate(xml, XPathConstants.STRING);
					datum.addEGaugeInstantaneousPropertyReading(propertyConfig, propertyType, value,
							instantenouseValue);
					break;
				case TOTAL:
					datum.addEGaugeAccumulatingPropertyReading(propertyConfig, propertyType, value);
					break;
				default:
					throw new UnsupportedOperationException(
							"Unkown register type: " + propertyConfig.getReadingType());
			}
		} catch ( XPathExpressionException e ) {
			throw new RuntimeException(e);
		}
	}

	protected XPathExpression getXPathExpression(String xpath) throws XPathExpressionException {
		XPath xp = getXpathFactory().newXPath();
		if ( getNsContext() != null ) {
			xp.setNamespaceContext(getNsContext());
		}
		return xp.compile(xpath);
	}

	protected Element getXml(EGaugeReadingType type, Map<EGaugeReadingType, Element> cache)
			throws XmlEGaugeClientException {
		if ( cache != null && cache.containsKey(type) ) {
			return cache.get(type);
		} else {
			String url = getUrl(getHost(), type);
			Element xml = getXml(url);
			cache.put(type, xml);
			return xml;
		}
	}

	protected Element getXml(String url) throws XmlEGaugeClientException {
		Document doc;
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			InputSource is = getInputSourceFromURLConnection(conn);
			doc = getDocBuilderFactory().newDocumentBuilder().parse(is);
		} catch ( SAXException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			// turn this into a WARN only
			log.warn("Error communicating with eGauge inverter at {}: {}", url, e.getMessage());

			// with a stacktrace in DEBUG
			log.debug("IOException communicating with eGauge inverter at {}", url, e);

			// Note that this means that the exception is swallowed and EGaugePowerDatum won't be able to store a reference in sampleException
			throw new XmlEGaugeClientException("Error communicating with eGauge inverter at " + url, e);
		} catch ( ParserConfigurationException e ) {
			throw new RuntimeException(e);
		}

		return doc.getDocumentElement();
	}

	@Override
	public void init() {
		super.init();
		if ( getInstantaneousQueryUrl() == null ) {
			setInstantaneousQueryUrl(DEAFAULT_INST_QUERY_URL);
		}
		if ( getTotalQueryUrl() == null ) {
			setTotalQueryUrl(DEAFAULT_TOT_QUERY_URL);
		}
	}

	/**
	 * @return the instantaneousQueryUrl
	 */
	public String getInstantaneousQueryUrl() {
		return instantaneousQueryUrl;
	}

	/**
	 * @param instantaneousQueryUrl
	 *        the instantaneousQueryUrl to set
	 */
	public void setInstantaneousQueryUrl(String instantaneousQueryUrl) {
		this.instantaneousQueryUrl = instantaneousQueryUrl;
	}

	/**
	 * @return the totalQueryUrl
	 */
	public String getTotalQueryUrl() {
		return totalQueryUrl;
	}

	/**
	 * @param totalQueryUrl
	 *        the totalQueryUrl to set
	 */
	public void setTotalQueryUrl(String totalQueryUrl) {
		this.totalQueryUrl = totalQueryUrl;
	}

	/**
	 * 
	 * Indicates that an exception has been encountered and handled but that the
	 * datum should not be processed further.
	 * 
	 * @author maxieduncan
	 * @version 1.0
	 */
	private class XmlEGaugeClientException extends Exception {

		private static final long serialVersionUID = -4997752985931615440L;

		public XmlEGaugeClientException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String toString() {
		return "XmlEGaugeClient [instantaneousQueryUrl=" + instantaneousQueryUrl + ", totalQueryUrl="
				+ totalQueryUrl + ", host=" + host + "]";
	}

}
