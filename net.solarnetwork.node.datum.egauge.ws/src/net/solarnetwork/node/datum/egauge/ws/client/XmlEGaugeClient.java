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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import net.solarnetwork.node.datum.egauge.ws.EGaugePowerDatum;
import net.solarnetwork.node.datum.egauge.ws.EGaugePropertyConfig;
import net.solarnetwork.node.datum.egauge.ws.EGaugePropertyConfig.EGaugeReadingType;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
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

	/** The ID that identifies the source. */
	private String sourceId;

	/** The list of property/register configurations. */
	private EGaugePropertyConfig[] propertyConfigs;

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier("host", ""));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));

		EGaugePropertyConfig[] confs = getPropertyConfigs();
		List<EGaugePropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<EGaugePropertyConfig> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("propertyConfigs", confsList,
				new SettingsUtil.KeyedListCallback<EGaugePropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(EGaugePropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								EGaugePropertyConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return results;
	}

	/**
	 * 
	 * @see net.solarnetwork.node.datum.egauge.ws.client.EGaugeClient#getCurrent()
	 */
	@Override
	public EGaugePowerDatum getCurrent() {
		EGaugePowerDatum datum = new EGaugePowerDatum();
		datum.setCreated(new Date());
		datum.setSourceId(getSourceId());

		try {
			populateDatum(datum);
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
			case Instantaneous:
				return getInstantaneousQueryUrl();
			case Total:
				return getTotalQueryUrl();
			default:
				throw new UnsupportedOperationException("Unkown register type: " + type);
		}
	}

	protected void populateDatum(EGaugePowerDatum datum) throws XmlEGaugeClientException {
		Map<EGaugeReadingType, Element> documentCache = new HashMap<>();

		EGaugePropertyConfig[] configs = getPropertyConfigs();
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
				case Instantaneous:
					String instantenouseValue = (String) getXPathExpression(xpathBase + "/i")
							.evaluate(xml, XPathConstants.STRING);
					datum.addEGaugeInstantaneousPropertyReading(propertyConfig, propertyType, value,
							instantenouseValue);
					break;
				case Total:
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

		if ( getPropertyConfigs() == null ) {
			setPropertyConfigs(new EGaugePropertyConfig[] {
					// FIXME, while the two config items show up in the UI the fields show blank values, either fix or remove
					new EGaugePropertyConfig("generation", "Solar+", EGaugeReadingType.Instantaneous),
					new EGaugePropertyConfig("consumption", "Grid", EGaugeReadingType.Instantaneous) });
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.egauge.ws.client.xml";
	}

	@Override
	public String getDisplayName() {
		return "eGauge web service client";
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
	 * Get the configured source ID.
	 * 
	 * @return the source ID
	 */
	@Override
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID value to assign to the collected data.
	 * 
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * @return the propertyConfig
	 */
	public EGaugePropertyConfig[] getPropertyConfigs() {
		return propertyConfigs;
	}

	/**
	 * @param propertyConfigs
	 *        the propertyConfig to set
	 */
	public void setPropertyConfigs(EGaugePropertyConfig[] propertyConfigs) {
		this.propertyConfigs = propertyConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropertyConfigsCount() {
		EGaugePropertyConfig[] confs = this.propertyConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propertyConfigs} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link EGaugePropertyConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propIncludes} elements.
	 */
	public void setPropertyConfigsCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		EGaugePropertyConfig[] confs = this.propertyConfigs;
		int lCount = (confs == null ? 0 : confs.length);
		if ( lCount != count ) {
			EGaugePropertyConfig[] newIncs = new EGaugePropertyConfig[count];
			if ( confs != null ) {
				System.arraycopy(confs, 0, newIncs, 0, Math.min(count, confs.length));
			}
			for ( int i = 0; i < count; i++ ) {
				if ( newIncs[i] == null ) {
					newIncs[i] = new EGaugePropertyConfig();
				}
			}
			this.propertyConfigs = newIncs;
		}
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
				+ totalQueryUrl + ", host=" + host + ", sourceId=" + sourceId + ", propertyConfigs="
				+ Arrays.toString(propertyConfigs) + "]";
	}

	@Override
	public Object getSampleInfo(EGaugePowerDatum snap) {
		return snap.getSampleInfo(getPropertyConfigs());
	}

}
