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

import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.springframework.expression.ExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.service.support.XmlServiceSupport;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * XML implementation of the EGaugeClient. Instances of this can be shared
 * between EGaugeDatumDataSource instances.
 *
 * The {@code propertyConfigs} configuration should match the content expected
 * to be returned by the {@code url}.
 *
 * @author maxieduncan
 * @version 2.1
 */
public class XmlEGaugeClient extends XmlServiceSupport implements EGaugeClient {

	/** The number of seconds in an hour, used for conversion. */
	private static final int HOUR_SECONDS = 3600;

	/**
	 * The default query URL that returns the XML data we will apply the XPath
	 * mappings to.
	 */
	private static final String DEFAULT_QUERY_URL = "/cgi-bin/egauge?inst";

	private String baseUrl;
	private String queryUrl = DEFAULT_QUERY_URL;

	/** The ID that identifies the source. */
	private String sourceId;

	/** The list of property/register configurations. */
	private EGaugeDatumSamplePropertyConfig[] propertyConfigs;

	/**
	 * Constructor.
	 */
	public XmlEGaugeClient() {
		super();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier("baseUrl", ""));
		results.add(new BasicTextFieldSettingSpecifier("queryUrl", DEFAULT_QUERY_URL));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));

		EGaugeDatumSamplePropertyConfig[] confs = getPropertyConfigs();
		List<EGaugeDatumSamplePropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<EGaugeDatumSamplePropertyConfig> emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("propertyConfigs", confsList,
				new SettingUtils.KeyedListCallback<EGaugeDatumSamplePropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							EGaugeDatumSamplePropertyConfig value, int index, String key) {

						List<SettingSpecifier> settingSpecifiers = new ArrayList<>();

						// Add the GeneralDatumSamplePropertyConfig properties
						settingSpecifiers
								.add(new BasicTextFieldSettingSpecifier(key + ".propertyKey", ""));

						BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
								key + ".propertyTypeKey", Instantaneous.name());
						// We only support two reading types currently
						Map<String, String> propTypeTitles = new LinkedHashMap<>();
						propTypeTitles.put(Character.toString(Instantaneous.toKey()),
								Instantaneous.toString());
						propTypeTitles.put(Character.toString(Accumulating.toKey()),
								Accumulating.toString());
						propTypeSpec.setValueTitles(propTypeTitles);
						settingSpecifiers.add(propTypeSpec);

						// Add the EGaugePropertyConfig properties
						List<String> registerNames = getRegisterNames();
						settingSpecifiers.addAll(EGaugePropertyConfig.settings(key + ".config.",
								registerNames, services(getExpressionServices())));

						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								settingSpecifiers);
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return results;
	}

	@Override
	public AcDcEnergyDatum getCurrent() {
		if ( getBaseUrl() == null ) {
			// not configured yet
			return null;
		}

		SimpleAcDcEnergyDatum datum = new SimpleAcDcEnergyDatum(resolvePlaceholders(getSourceId()),
				Instant.now(), new DatumSamples());

		try {
			populateDatum(datum);
			if ( datum.getSamples() == null || datum.getSamples().isEmpty() ) {
				// not configured probably
				return null;
			}
		} catch ( XmlEGaugeClientException e ) {
			// An exception has been encountered and logged but we need to make sure no datum is returned
			return null;
		}

		return datum;
	}

	/**
	 * Populate a datum.
	 *
	 * @param datum
	 *        the datum to populate
	 * @throws XmlEGaugeClientException
	 *         if a client error occurs
	 */
	protected void populateDatum(AcDcEnergyDatum datum) throws XmlEGaugeClientException {
		EGaugeDatumSamplePropertyConfig[] configs = getPropertyConfigs();
		if ( configs != null ) {
			Element xml = getXml(getUrl());
			if ( xml != null ) {
				List<DataRegister> registers = dataRegisters(xml);
				for ( EGaugeDatumSamplePropertyConfig propertyConfig : configs ) {
					populateDatumProperty(datum, propertyConfig, registers);
				}
			}
		}
	}

	private List<DataRegister> dataRegisters(Element xml) {
		NodeList els;
		try {
			els = (NodeList) getXPathExpression("//r").evaluate(xml, XPathConstants.NODESET);
		} catch ( XPathExpressionException e ) {
			throw new RuntimeException(e);
		}
		final int len = (els != null ? els.getLength() : 0);
		if ( len < 1 ) {
			return Collections.emptyList();
		}
		List<DataRegister> regs = new ArrayList<>(len);
		for ( int i = 0; i < len; i++ ) {
			Element el = (Element) els.item(i);
			String name = el.getAttribute("n");
			String type = el.getAttribute("t");
			String rType = el.getAttribute("rt");
			BigInteger value = null;
			BigDecimal inst = null;
			NodeList childEls = el.getChildNodes();
			final int childLen = (childEls != null ? childEls.getLength() : 0);
			for ( int j = 0; j < childLen; j++ ) {
				Element child = (Element) childEls.item(j);
				if ( "v".equalsIgnoreCase(child.getLocalName()) ) {
					value = new BigInteger(child.getTextContent());
				} else if ( "i".equalsIgnoreCase(child.getLocalName()) ) {
					inst = new BigDecimal(child.getTextContent());
				}
			}
			regs.add(new DataRegister(name, type, rType, value, inst));
		}
		return regs;
	}

	/**
	 * Populate a datum property.
	 *
	 * @param datum
	 *        the datum to populate the property on
	 * @param propertyConfig
	 *        the property configuration
	 * @param registers
	 *        the available registers
	 */
	protected void populateDatumProperty(AcDcEnergyDatum datum,
			EGaugeDatumSamplePropertyConfig propertyConfig, List<DataRegister> registers) {

		if ( !propertyConfig.isValid() ) {
			// no property type or key configured
			return;
		}

		final DatumSamplesType propertyType = propertyConfig.getPropertyType();
		final ExpressionServiceExpression expr;
		try {
			expr = propertyConfig.getExpression(services(getExpressionServices()));
		} catch ( ExpressionException e ) {
			log.warn("Error parsing property [{}] expression `{}`: {}", propertyConfig.getPropertyKey(),
					propertyConfig.getConfig().getExpression(), e.getMessage());
			return;
		}

		Number propValue = null;

		if ( expr != null ) {
			ExpressionRoot root = new ExpressionRoot(registers);
			try {
				propValue = expr.getService().evaluateExpression(expr.getExpression(), null, root, null,
						BigDecimal.class);
			} catch ( ExpressionException e ) {
				log.warn("Error evaluating property [{}] expression `{}`: {}",
						propertyConfig.getPropertyKey(), propertyConfig.getConfig().getExpression(),
						e.getMessage());
			}
		} else {
			final String regName = propertyConfig.getConfig().getRegisterName();
			DataRegister reg = registers.stream().filter(r -> regName.equalsIgnoreCase(r.getName()))
					.findFirst().orElse(null);
			if ( reg != null ) {
				switch (propertyType) {
					case Instantaneous:
						propValue = reg.getInstant();
						break;

					case Accumulating:
						propValue = reg.getValue();
						if ( "P".equalsIgnoreCase(reg.getType()) ) {
							// Convert watt-seconds into watt-hours
							propValue = new BigDecimal((BigInteger) propValue)
									.divide(new BigDecimal(HOUR_SECONDS), RoundingMode.DOWN);
						}

						break;

					default:
						// ignore
				}
			}
		}
		if ( propValue != null ) {
			switch (propertyType) {
				case Instantaneous:
					datum.asMutableSampleOperations().putSampleValue(Instantaneous,
							propertyConfig.getPropertyKey(), propValue);
					break;

				case Accumulating:
					datum.asMutableSampleOperations().putSampleValue(Accumulating,
							propertyConfig.getPropertyKey(), propValue);
					break;

				default:
					// ignore
			}
		}
	}

	private XPathExpression getXPathExpression(String xpath) throws XPathExpressionException {
		XPath xp = getXpathFactory().newXPath();
		if ( getNsContext() != null ) {
			xp.setNamespaceContext(getNsContext());
		}
		return xp.compile(xpath);
	}

	private Element getXml(String url) throws XmlEGaugeClientException {
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
	 * @return the register names found in the file
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
	public String getSettingUid() {
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
	 * Get the property configurations.
	 *
	 * @return the configurations
	 */
	public EGaugeDatumSamplePropertyConfig[] getPropertyConfigs() {
		return propertyConfigs;
	}

	/**
	 * Set the property configurations.
	 *
	 * @param propertyConfigs
	 *        the configurations to set
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

	/**
	 * Get the URL.
	 *
	 * @return the URL
	 */
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
	public String getSampleInfo(NodeDatum snap) {
		StringBuilder buf = new StringBuilder();
		DatumSamplesOperations ops = snap.asSampleOperations();
		for ( EGaugeDatumSamplePropertyConfig propertyConfig : getPropertyConfigs() ) {
			if ( !propertyConfig.isValid() ) {
				continue;
			}
			if ( buf.length() > 0 ) {
				buf.append("; ");
			}
			buf.append(propertyConfig.getPropertyKey()).append(" (")
					.append(propertyConfig.getPropertyTypeKey()).append(") = ");
			Object v = ops.getSampleValue(propertyConfig.getPropertyType(),
					propertyConfig.getPropertyKey());
			if ( v != null ) {
				buf.append(v);
			}
		}
		return buf.toString();
	}

	@Override
	public String toString() {
		return "XmlEGaugeClient{baseUrl=" + baseUrl + ", queryUrl=" + queryUrl + ", sourceId=" + sourceId
				+ ", propertyConfigs=" + Arrays.toString(propertyConfigs) + "}";
	}

	/**
	 * Get the base URL.
	 *
	 * @return the base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Set the base URL.
	 *
	 * @param baseUrl
	 *        the base URL to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Get the query URL.
	 *
	 * @return the query URL
	 */
	public String getQueryUrl() {
		return queryUrl;
	}

	/**
	 * Set the query URL.
	 *
	 * @param queryUrl
	 *        the query URL to set
	 */
	public void setQueryUrl(String queryUrl) {
		this.queryUrl = queryUrl;
	}

}
