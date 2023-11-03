/* ==================================================================
 * EnaSolarXMLDatumDataSource.java - Oct 2, 2011 8:50:13 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.power.enasolar.ws;

import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPathExpression;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.node.service.support.XmlServiceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Web service based support for EnaSolar inverters.
 * 
 * This service can read from two different XML services provided by EnaSolar
 * inverters. First is the <b>deviceinfo.xml</b> form. This expects to access a
 * URL that returns XML in the following form:
 * 
 * <pre>
 * &lt;info time="16180F57"&gt;
 *  &lt;data key="vendor" value="EnaSolar" /&gt;
 *  &lt;data key="model" value="2" /&gt;
 *  &lt;data key="acOutputVolts" value="241.1" /&gt;
 *  &lt;data key="pvVolts" value="304.3" /&gt;
 *  &lt;data key="pvPower" value="0.681" /&gt;
 *  &lt;data key="acPower" value="0.628" /&gt;
 *  &lt;data key="kWattHoursToday" value="17.94" /&gt;
 *  &lt;data key="decaWattHoursTotal" value="0000167A" /&gt;
 * &lt;/info&gt;
 * </pre>
 * 
 * The second is the <b>data.xml</b> and <b>meters.xml</b> form. The first URL
 * should return XML in the following form:
 * 
 * <pre>
 * &lt;response&gt;
 *   &lt;EnergyToday&gt;0000&lt;/EnergyToday&gt;
 *   &lt;EnergyYesterday&gt;01ED&lt;/EnergyYesterday&gt;
 *   &lt;DU&gt;0000&lt;/DU&gt;
 *   &lt;EnergyLifetime&gt;000BA446&lt;/EnergyLifetime&gt;
 *   &lt;HoursExportedToday&gt;0&lt;/HoursExportedToday&gt;
 *   &lt;HoursExportedYesterday&gt;493&lt;/HoursExportedYesterday&gt;
 *   &lt;HoursExportedLifetime&gt;000A032E&lt;/HoursExportedLifetime&gt;
 *   &lt;DaysProducing&gt;03F5&lt;/DaysProducing&gt;
 * &lt;/response&gt;
 * </pre>
 * 
 * and the second URL in the following form:
 * 
 * <pre>
 * &lt;response&gt;
 *   &lt;OutputPower&gt;0&lt;/OutputPower&gt;
 *   &lt;InputVoltage&gt;0&lt;/InputVoltage&gt;
 *   &lt;OutputVoltage&gt;0&lt;/OutputVoltage&gt;
 * &lt;/response&gt;
 * </pre>
 * 
 * @author matt
 * @version 1.1
 */
public class EnaSolarXMLDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

	/** The {@link SettingDao} key for a Long counter of 0 watt readings. */
	public static final String SETTING_ZERO_WATT_COUNT = "EnaSolarXMLDatumDataSource:0W";

	/** The maximum number of consecutive zero-watt readings to return. */
	public static final long ZERO_WATT_THRESHOLD = 10;

	/** The default value for the {@code urlList} property. */
	public static final String DEFAULT_URL_LIST = "http://enasolar-gt/data.xml,http://enasolar-gt/meters.xml";

	private static final Pattern DATA_VALUE_XPATH_NAME = Pattern.compile("key='(\\w+)'");

	private final XmlServiceSupport xmlSupport = new XmlServiceSupport();
	private String[] urls;
	private String sourceId;
	private Map<String, XPathExpression> xpathMapping;
	private Map<String, String> xpathMap;
	private long sampleCacheMs = 5000;

	private EnaSolarPowerDatum sample;
	private Throwable sampleException;
	private final Map<String, Long> validationCache = new HashMap<>(4);

	private EnaSolarPowerDatum getCurrentSample() {
		EnaSolarPowerDatum datum;
		if ( isCachedSampleExpired() ) {
			datum = new EnaSolarPowerDatum(resolvePlaceholders(sourceId), Instant.now());
			sampleException = null;
			for ( String url : urls ) {
				try {
					xmlSupport.webFormGetForBean(null, datum, url, null, xpathMapping);
				} catch ( RuntimeException e ) {
					Throwable root = e;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					sampleException = root;
					if ( root instanceof IOException ) {
						// turn this into a WARN only
						log.warn("Error communicating with EnaSolar inverter at {}: {}", url,
								e.getMessage());

						// with a stacktrace in DEBUG
						log.debug("IOException communicating with EnaSolar inverter at {}", url, e);

						return null;
					} else {
						throw e;
					}
				}
			}
			datum = validateDatum(datum);
			if ( datum != null ) {
				sample = datum;
				addEnergyDatumSourceMetadata(datum);
			}
		} else {
			datum = sample;
		}
		return datum;
	}

	private boolean isCachedSampleExpired() {
		EnaSolarPowerDatum snap = sample;
		if ( snap == null || sample.getTimestamp() == null ) {
			return true;
		}
		final long lastReadDiff = sample.getTimestamp().until(Instant.now(), ChronoUnit.MILLIS);
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		if ( validateDatum(snap) == null ) {
			return true; // not valid
		}
		return false;
	}

	@Override
	public String toString() {
		return "Enasolar" + (sourceId == null ? "" : "-" + sourceId);
	}

	/**
	 * Call after properties configured to initialize the service.
	 */
	public void init() {
		if ( xpathMapping == null ) {
			setXpathMap(defaultXpathMap());
		}
		if ( urls == null ) {
			setUrlList(DEFAULT_URL_LIST);
		}
	}

	private static Map<String, String> defaultXpathMap() {
		Map<String, String> result = new LinkedHashMap<String, String>(10);
		result.put("outputPower", "//OutputPower");
		result.put("outputVoltage", "//OutputVoltage");
		result.put("inputVoltage", "//InputVoltage");
		result.put("energyLifetime", "//EnergyLifetime");
		return result;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public AcDcEnergyDatum readCurrentDatum() {
		return getCurrentSample();
	}

	private Long zeroWattCount() {
		return (validationCache.containsKey(SETTING_ZERO_WATT_COUNT)
				? validationCache.get(SETTING_ZERO_WATT_COUNT)
				: 0L);
	}

	private Long lastKnownValue() {
		EnaSolarPowerDatum snap = sample;
		Long result = null;
		if ( snap != null ) {
			result = snap.getWattHourReading();
		}
		return (result == null ? 0L : result);
	}

	private boolean isSampleOnSameDay(final Instant sampleDate) {
		final Instant lastKnownDate = (sample != null ? sample.getTimestamp() : null);
		if ( sampleDate == null || lastKnownDate == null ) {
			return false;
		}
		final LocalDate sampleDay = sampleDate.atZone(ZoneId.systemDefault()).toLocalDate();
		final LocalDate lastKnownDay = lastKnownDate.atZone(ZoneId.systemDefault()).toLocalDate();
		return (sampleDay.compareTo(lastKnownDay) == 0);
	}

	private EnaSolarPowerDatum validateDatum(EnaSolarPowerDatum datum) {
		final Long currValue = datum.getWattHourReading();
		final Long zeroWattCount = zeroWattCount();
		final Long lastKnownValue = lastKnownValue();
		final boolean dailyResettingWh = datum.isUsingDailyResettingTotal();

		// we've seen values reported less than last known value after
		// a power outage (i.e. after inverter turns off, then back on)
		// on single day, so we verify that current decaWattHoursTotal is not less 
		// than last known decaWattHoursTotal value
		if ( currValue != null && currValue.longValue() < lastKnownValue.longValue()
				&& (dailyResettingWh == false || (dailyResettingWh && zeroWattCount < 1L)) ) {
			log.warn(
					"Inverter [{}] reported value {} -- less than last known value {}. Discarding this datum.",
					sourceId, currValue, lastKnownValue);
			datum = null;
		} else if ( currValue != null && currValue.longValue() < 1L && dailyResettingWh
				&& isSampleOnSameDay(datum.getTimestamp()) == false ) {
			log.debug("Resetting last known sample for new day zero Wh");
			sample = datum;
			datum = null;
		} else if ( datum.getWatts() == null || datum.getWatts() < 1 ) {
			final Long newCount = (zeroWattCount.longValue() + 1);
			if ( zeroWattCount >= ZERO_WATT_THRESHOLD ) {
				log.debug("Skipping zero-watt reading #{}", zeroWattCount);
				datum = null;
			}
			validationCache.put(SETTING_ZERO_WATT_COUNT, newCount);
		} else if ( zeroWattCount > 0 ) {
			// reset zero-watt counter
			log.debug("Resetting zero-watt reading count from non-zero reading");
			validationCache.remove(SETTING_ZERO_WATT_COUNT);
		}

		// everything checks out
		return datum;
	}

	private void addEnergyDatumSourceMetadata(EnaSolarPowerDatum d) {
		if ( d == null ) {
			return;
		}
		// associate consumption/generation tags with this source
		GeneralDatumMetadata sourceMeta = new GeneralDatumMetadata();
		sourceMeta.addTag(AcDcEnergyDatum.TAG_GENERATION);
		addSourceMetadata(d.getSourceId(), sourceMeta);
	}

	@Override
	public String getUID() {
		return getSourceId();
	}

	/**
	 * Set the XPath mapping using String values.
	 * 
	 * @param xpathMap
	 *        the string XPath mapping values
	 */
	public void setXpathMap(Map<String, String> xpathMap) {
		this.xpathMap = xpathMap;
		setXpathMapping(xmlSupport.getXPathExpressionMap(xpathMap));
	}

	/**
	 * Get the XML data mapping as a delimited string.
	 * 
	 * @return delimited string of XML mappings
	 */
	public String getDataMapping() {
		StringBuilder buf = new StringBuilder();
		if ( xpathMap != null ) {
			for ( Map.Entry<String, String> me : xpathMap.entrySet() ) {
				if ( buf.length() > 0 ) {
					buf.append(", ");
				}
				buf.append(me.getKey()).append('=');
				Matcher m = DATA_VALUE_XPATH_NAME.matcher(me.getValue());
				if ( m.find() ) {
					buf.append(m.group(1));
				} else {
					buf.append(me.getValue().toString());
				}
			}
		}
		return buf.toString();
	}

	/**
	 * Set the XML data mapping. This supports setting simple
	 * {@code deviceinfo.xml} key names or, if the value contains a {@code /}
	 * character, direct XPath values.
	 * 
	 * 
	 * @param mapping
	 *        comma-delimited equal-delimited key value pair list
	 */
	public void setDataMapping(String mapping) {
		if ( mapping == null || mapping.length() < 1 ) {
			return;
		}
		String[] pairs = mapping.split("\\s*,\\s*");
		Map<String, String> map = new LinkedHashMap<String, String>();
		for ( String pair : pairs ) {
			String[] kv = pair.split("\\s*=\\s*");
			if ( kv == null || kv.length != 2 ) {
				continue;
			}
			if ( kv[1].contains("/") ) {
				map.put(kv[0], kv[1]);
			} else {
				map.put(kv[0], "//data[@key='" + kv[1] + "']/@value");
			}
		}
		setXpathMap(map);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.power.enasolar";
	}

	@Override
	public String getDisplayName() {
		return "EnaSolar web service data source";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		EnaSolarXMLDatumDataSource defaults = new EnaSolarXMLDatumDataSource();
		defaults.init();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTextFieldSettingSpecifier("urlList", defaults.getUrlList()));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));
		results.add(new BasicTextFieldSettingSpecifier("groupUid", null));
		results.add(new BasicTextFieldSettingSpecifier("dataMapping", defaults.getDataMapping()));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.sampleCacheMs)));
		return results;
	}

	/**
	 * Get an informational status message.
	 * 
	 * @return A status message.
	 */
	public String getInfoMessage() {
		EnaSolarPowerDatum snap = null;
		try {
			snap = getCurrentSample();
		} catch ( Exception e ) {
			// we must ignore exceptions here
		}
		StringBuilder buf = new StringBuilder();
		Throwable t = sampleException;
		if ( t != null ) {
			buf.append("Error communicating with EnaSolar inverter: ").append(t.getMessage());
		}
		if ( snap != null ) {
			if ( buf.length() > 0 ) {
				buf.append("; ");
			}
			buf.append(snap.getWatts()).append(" W; ");
			buf.append(snap.getWattHourReading()).append(" Wh; sample created ");
			buf.append(formatForLocalDisplay(snap.getTimestamp()));
		}
		return (buf.length() < 1 ? "N/A" : buf.toString());
	}

	/**
	 * Set the cached sample.
	 * 
	 * <p>
	 * This is meant to support unit tests.
	 * </p>
	 * 
	 * @param datum
	 *        the sample to set
	 */
	protected void setSample(EnaSolarPowerDatum datum) {
		this.sample = datum;
	}

	/**
	 * Get the first available URL.
	 * 
	 * @return the first URL
	 */
	public String getUrl() {
		return (urls == null || urls.length < 1 ? null : urls[0]);
	}

	/**
	 * Set the first URL.
	 * 
	 * @param url
	 *        the URL
	 */
	public void setUrl(String url) {
		if ( urls == null || urls.length < 1 ) {
			urls = new String[] { url };
		} else {
			urls[0] = url;
		}
	}

	/**
	 * Get the URLs.
	 * 
	 * @return the URLs
	 */
	public String[] getUrls() {
		return urls;
	}

	/**
	 * Set an array of URLs for accessing the XML data from (more than one in
	 * case the {@code data.xml} and {@code meters.xml} URLs are used).
	 * 
	 * @param urls
	 *        the urls to set
	 */
	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	/**
	 * Get the URL list.
	 * 
	 * @return list of URLs
	 */
	public String getUrlList() {
		return StringUtils.delimitedStringFromCollection(Arrays.asList(this.urls), ",");
	}

	/**
	 * Set the URL list as a comma-delimited string.
	 * 
	 * @param list
	 *        the comma-delimited list of URLs
	 */
	public void setUrlList(String list) {
		Set<String> set = StringUtils.delimitedStringToSet(list, ",");
		setUrls(set.toArray(new String[set.size()]));
	}

	/**
	 * Get the XPath mapping.
	 * 
	 * @return the mapping
	 */
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
	 * Get the configured source ID.
	 * 
	 * @return the source ID
	 */
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
		validationCache.clear();
	}

	/**
	 * Set the sample cache, in milliseconds.
	 * 
	 * @param sampleCacheMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

}
