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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.datum.egauge.ws.EGaugePowerDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.node.support.XmlServiceSupport;
import net.solarnetwork.util.ArrayUtils;

/**
 * XML implementation of the EGaugeClient. Instances of this can be shared
 * between EGaugeDatumDataSource instances.
 * 
 * The {@code propertyConfigs} configuration should match the content expected
 * to be returned by the {@code url}.
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
	private static final String DEFAULT_QUERY_URL = "/cgi-bin/egauge?inst&tot";

	private String baseUrl;
	private String queryUrl = DEFAULT_QUERY_URL;

	/** The ID that identifies the source. */
	private String sourceId;

	/** The list of property/register configurations. */
	private EGaugeDatumSamplePropertyConfig[] propertyConfigs;

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier("baseUrl", ""));
		results.add(new BasicTextFieldSettingSpecifier("queryUrl", DEFAULT_QUERY_URL));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));

		EGaugeDatumSamplePropertyConfig[] confs = getPropertyConfigs();
		List<EGaugeDatumSamplePropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<EGaugeDatumSamplePropertyConfig> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("propertyConfigs", confsList,
				new SettingsUtil.KeyedListCallback<EGaugeDatumSamplePropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							EGaugeDatumSamplePropertyConfig value, int index, String key) {

						List<SettingSpecifier> settingSpecifiers = new ArrayList<>();

						// Add the GeneralDatumSamplePropertyConfig properties
						settingSpecifiers
								.add(new BasicTextFieldSettingSpecifier(key + ".propertyKey", ""));

						BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
								key + ".propertyTypeKey", GeneralDatumSamplesType.Instantaneous.name());
						// We only support two reading types currenlty
						Map<String, String> propTypeTitles = new LinkedHashMap<>();
						propTypeTitles.put(
								Character.toString(GeneralDatumSamplesType.Instantaneous.toKey()),
								GeneralDatumSamplesType.Instantaneous.toString());
						propTypeTitles.put(
								Character.toString(GeneralDatumSamplesType.Accumulating.toKey()),
								GeneralDatumSamplesType.Accumulating.toString());
						propTypeSpec.setValueTitles(propTypeTitles);
						settingSpecifiers.add(propTypeSpec);

						// Add the EGaugePropertyConfig properties
						List<String> registerNames = getRegisterNames();
						settingSpecifiers
								.addAll(EGaugePropertyConfig.settings(key + ".config.", registerNames));

						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								settingSpecifiers);
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return results;
	}

	@Override
	public EGaugePowerDatum getCurrent() {
		if ( getBaseUrl() == null ) {
			// not configured yet
			return null;
		}

		EGaugePowerDatum datum = new EGaugePowerDatum();
		datum.setCreated(new Date());
		datum.setSourceId(getSourceId());

		try {
			populateDatum(datum);
			Map<String, ?> sampleData = datum.getSampleData();
			if ( sampleData == null || sampleData.isEmpty() ) {
				// not configured probably
				return null;
			}
		} catch ( XmlEGaugeClientException e ) {
			// An exception has been encountered and logged but we need to make sure no datum is returned
			return null;
		}

		if ( datum != null ) {
			postDatumCapturedEvent(datum);
		}
		return datum;
	}

	protected void populateDatum(EGaugePowerDatum datum) throws XmlEGaugeClientException {
		EGaugeDatumSamplePropertyConfig[] configs = getPropertyConfigs();
		if ( configs != null ) {
			Element xml = getXml(getUrl());
			if ( xml != null ) {
				for ( EGaugeDatumSamplePropertyConfig propertyConfig : configs ) {
					populateDatumProperty(datum, propertyConfig, xml);
				}
			}
		}
	}

	protected void populateDatumProperty(EGaugePowerDatum datum,
			EGaugeDatumSamplePropertyConfig propertyConfig, Element xml) {

		if ( !propertyConfig.isValid() ) {
			// no property type or key configured
			return;
		}

		GeneralDatumSamplesType propertyType = propertyConfig.getPropertyType();

		try {

			String xpathBase = "r[@n='" + propertyConfig.getConfig().getRegisterName() + "'][1]";
			String eGaugePropertyType = (String) getXPathExpression(xpathBase + "/@t").evaluate(xml,
					XPathConstants.STRING);

			switch (propertyType) {
				case Instantaneous:
					String instantenouseValue = (String) getXPathExpression(xpathBase + "/i")
							.evaluate(xml, XPathConstants.STRING);

					// Store as a BigDecimal value by default
					datum.putInstantaneousSampleValue(propertyConfig.getPropertyKey(),
							new BigDecimal(instantenouseValue));
					break;
				case Accumulating:
					String value = (String) getXPathExpression(xpathBase + "/v").evaluate(xml,
							XPathConstants.STRING);

					switch (eGaugePropertyType) {
						case "P":
							// Convert watt-seconds into watt-hours
							BigDecimal wattHours = new BigDecimal(value)
									.divide(new BigDecimal(HOUR_SECONDS), RoundingMode.DOWN);
							datum.putAccumulatingSampleValue(propertyConfig.getPropertyKey(), wattHours);
							break;
						default:
							// Store as a BigDecimal value by default
							datum.putAccumulatingSampleValue(propertyConfig.getPropertyKey(),
									new BigDecimal(value));
					}
					break;
				default:
					throw new UnsupportedOperationException("Unsuported property type: " + propertyType);
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

	/**
	 * Retrieves the eGauge file from the specified URL and returns the register
	 * names found inside.
	 * 
	 * @param queryUrl
	 *        the eGauge path to get the file from
	 * @return the register names found in the file
	 * @throws XmlEGaugeClientException
	 */
	public List<String> getRegisterNames() {
		if ( getBaseUrl() == null ) {
			// not configured yet
			return Collections.emptyList();
		}
		try {
			Element xml = getXml(getUrl());
			if ( xml != null ) {
				NodeList nodeList = (NodeList) getXPathExpression("r/@n").evaluate(xml,
						XPathConstants.NODESET);
				if ( nodeList != null ) {
					List<String> registerNames = new ArrayList<>();
					for ( int i = 0; i < nodeList.getLength(); i++ ) {
						registerNames.add(nodeList.item(i).getNodeValue());
					}
					return registerNames;
				}
			}
		} catch ( Exception e ) {
			// This is non fatal, just log a warning for reference
			log.warn("Error communicating with eGauge inverter at {}: {}", getUrl(), e.getMessage());
		}

		return null;
	}

	@Override
	public void init() {
		super.init();
		if ( getQueryUrl() == null ) {
			setQueryUrl(DEFAULT_QUERY_URL);
		}

		if ( getPropertyConfigs() == null ) {
			// Add two empty readings. Tried adding default values to them but they didn't show in the UI
			setPropertyConfigsCount(2);
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
	public EGaugeDatumSamplePropertyConfig[] getPropertyConfigs() {
		return propertyConfigs;
	}

	/**
	 * @param propertyConfigs
	 *        the propertyConfig to set
	 */
	public void setPropertyConfigs(EGaugeDatumSamplePropertyConfig[] propertyConfigs) {
		this.propertyConfigs = propertyConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropertyConfigsCount() {
		EGaugeDatumSamplePropertyConfig[] confs = this.propertyConfigs;
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
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropertyConfigsCount(int count) {
		this.propertyConfigs = ArrayUtils.arrayWithLength(this.propertyConfigs, count,
				EGaugeDatumSamplePropertyConfig.class, null);
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

	public String getUrl() {
		StringBuilder builder = new StringBuilder();
		if ( getBaseUrl() != null ) {
			builder.append(
					getBaseUrl().endsWith("/") ? getBaseUrl().substring(0, getBaseUrl().length() - 1)
							: getBaseUrl());
		}
		builder.append("/");
		if ( getQueryUrl() != null ) {
			builder.append(getQueryUrl().startsWith("/") ? getQueryUrl().substring(1) : getQueryUrl());
		}
		return builder.toString();
	}

	@Override
	public Object getSampleInfo(EGaugePowerDatum snap) {
		StringBuilder buf = new StringBuilder();
		for ( EGaugeDatumSamplePropertyConfig propertyConfig : getPropertyConfigs() ) {
			switch (propertyConfig.getPropertyType()) {
				case Instantaneous:
					buf.append(propertyConfig.getPropertyKey() + " (i) = ");
					buf.append(snap.getInstantaneousSampleInteger(propertyConfig.getPropertyKey()));
					break;
				case Accumulating:
					buf.append(propertyConfig.getPropertyKey() + " (a) = ");
					buf.append(snap.getAccumulatingSampleLong(propertyConfig.getPropertyKey()));
					break;
				default:
					// Ignore
			}
		}
		return buf.toString();
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getQueryUrl() {
		return queryUrl;
	}

	public void setQueryUrl(String queryUrl) {
		this.queryUrl = queryUrl;
	}

	@Override
	public String toString() {
		return "XmlEGaugeClient{baseUrl=" + baseUrl + ", queryUrl=" + queryUrl + ", sourceId=" + sourceId
				+ ", propertyConfigs=" + Arrays.toString(propertyConfigs) + "}";
	}

}
