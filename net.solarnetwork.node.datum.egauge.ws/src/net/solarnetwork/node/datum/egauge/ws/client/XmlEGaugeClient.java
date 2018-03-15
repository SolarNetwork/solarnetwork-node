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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
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
	private static final String DEAFAULT_INST_QUERY_URL = "/cgi-bin/egauge?inst";

	/**
	 * The URL that should be used to retrieve the instantanseous eGauge XML.
	 */
	private String instQueryUrl = DEAFAULT_INST_QUERY_URL;

	private String host;

	/** The ID that identifies the source. */
	private String sourceId;

	/** The list of property/register configurations. */
	private GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>[] propertyConfigs;

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier("host", ""));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));

		GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>[] confs = getPropertyConfigs();
		List<GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>> confsList = (confs != null
				? Arrays.asList(confs)
				: Collections.<GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("propertyConfigs", confsList,
				new SettingsUtil.KeyedListCallback<GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							GeneralDatumSamplePropertyConfig<EGaugePropertyConfig> value, int index,
							String key) {

						List<SettingSpecifier> settingSpecifiers = new ArrayList<>();

						// Add the GeneralDatumSamplePropertyConfig properties
						settingSpecifiers
								.add(new BasicTextFieldSettingSpecifier(key + ".propertyKey", ""));

						BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
								key + ".propertyType", GeneralDatumSamplesType.Instantaneous.name());
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
						settingSpecifiers.addAll(EGaugePropertyConfig.settings(key + ".config."));

						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								settingSpecifiers);
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
	 * @param queryUrl
	 *        The eGauge query URL to access on {@link #getHost()}.
	 * @return the URL that should be used to retrieve the eGauge data.
	 */
	protected String constructEGaugeUrl(String queryUrl) {
		return (host == null ? "" : getHost()) + queryUrl;
	}

	protected void populateDatum(EGaugePowerDatum datum) throws XmlEGaugeClientException {

		GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>[] configs = getPropertyConfigs();
		if ( configs != null ) {
			for ( GeneralDatumSamplePropertyConfig<EGaugePropertyConfig> propertyConfig : configs ) {
				Element xml = getXml(constructEGaugeUrl(getInstQueryUrl()));
				if ( xml != null ) {
					populateDatumProperty(datum, propertyConfig, xml);
				}
			}
		}

	}

	protected void populateDatumProperty(EGaugePowerDatum datum,
			GeneralDatumSamplePropertyConfig<EGaugePropertyConfig> propertyConfig, Element xml) {

		GeneralDatumSamplesType propertyType = propertyConfig.getPropertyType();

		try {

			String xpathBase = "r[@n='" + propertyConfig.getConfig().getRegisterName() + "'][1]";
			String eGaugePropertyType = (String) getXPathExpression(xpathBase + "/@t").evaluate(xml,
					XPathConstants.STRING);

			switch (propertyType) {
				case Instantaneous:
					String instantenouseValue = (String) getXPathExpression(xpathBase + "/i")
							.evaluate(xml, XPathConstants.STRING);
					// Assume an int value
					datum.putInstantaneousSampleValue(propertyConfig.getPropertyKey(),
							Integer.valueOf(instantenouseValue));
					break;
				case Accumulating:
					String value = (String) getXPathExpression(xpathBase + "/v").evaluate(xml,
							XPathConstants.STRING);

					switch (eGaugePropertyType) {
						case "P":
							// Convert watt-seconds into watt-hours
							Long wattHours = Long.valueOf(value) / HOUR_SECONDS;// TODO review rounding
							datum.putAccumulatingSampleValue(propertyConfig.getPropertyKey(), wattHours);
							break;
						default:
							// Assume a Long value by default
							datum.putAccumulatingSampleValue(propertyConfig.getPropertyKey(),
									Long.valueOf(value));
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

	@SuppressWarnings("unchecked")
	@Override
	public void init() {
		super.init();
		if ( getInstQueryUrl() == null ) {
			setInstQueryUrl(DEAFAULT_INST_QUERY_URL);
		}

		if ( getPropertyConfigs() == null ) {
			// FIXME, while the two config items show up in the UI the fields show blank values, either fix or remove
			@SuppressWarnings("rawtypes")
			GeneralDatumSamplePropertyConfig[] defaultConfigs = new GeneralDatumSamplePropertyConfig[] {
					new GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>("consumptionWatts",
							GeneralDatumSamplesType.Instantaneous, new EGaugePropertyConfig("Grid")),
					new GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>(
							"consumptionWattHourReading", GeneralDatumSamplesType.Accumulating,
							new EGaugePropertyConfig("Grid")),
					new GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>("generationWatts",
							GeneralDatumSamplesType.Instantaneous, new EGaugePropertyConfig("Solar+")),
					new GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>(
							"generationWattHourReading", GeneralDatumSamplesType.Accumulating,
							new EGaugePropertyConfig("Solar+")) };
			setPropertyConfigs(defaultConfigs);
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
	public String getInstQueryUrl() {
		return instQueryUrl;
	}

	/**
	 * @param instantaneousQueryUrl
	 *        the instantaneousQueryUrl to set
	 */
	public void setInstQueryUrl(String instantaneousQueryUrl) {
		this.instQueryUrl = instantaneousQueryUrl;
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
	public GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>[] getPropertyConfigs() {
		return propertyConfigs;
	}

	/**
	 * @param propertyConfigs
	 *        the propertyConfig to set
	 */
	public void setPropertyConfigs(
			GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>[] propertyConfigs) {
		this.propertyConfigs = propertyConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropertyConfigsCount() {
		GeneralDatumSamplePropertyConfig<EGaugePropertyConfig>[] confs = this.propertyConfigs;
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
	@SuppressWarnings("unchecked")
	public void setPropertyConfigsCount(int count) {
		this.propertyConfigs = ArrayUtils.arrayWithLength(this.propertyConfigs, count,
				GeneralDatumSamplePropertyConfig.class, null);
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
		return "XmlEGaugeClient [instantaneousQueryUrl=" + instQueryUrl + ", host=" + host
				+ ", sourceId=" + sourceId + ", propertyConfigs=" + Arrays.toString(propertyConfigs)
				+ "]";
	}

	@Override
	public Object getSampleInfo(EGaugePowerDatum snap) {
		StringBuilder buf = new StringBuilder();
		for ( GeneralDatumSamplePropertyConfig<EGaugePropertyConfig> propertyConfig : getPropertyConfigs() ) {
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

}
