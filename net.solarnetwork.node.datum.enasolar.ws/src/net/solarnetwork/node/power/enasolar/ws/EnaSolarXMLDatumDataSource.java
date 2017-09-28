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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPathExpression;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.XmlServiceSupport;
import net.solarnetwork.util.StringUtils;

/**
 * Web service based support for EnaSolar inverters.
 * 
 * This service can read from two different XML services provided by EnaSolar
 * inverters. First is the {@bold deviceinfo.xml} form. This expects to access a
 * URL that returns XML in the following form:
 * 
 * <pre>
 * &lt;info time="16180F57">
 *  &lt;data key="vendor" value="EnaSolar" />
 *  &lt;data key="model" value="2" />
 *  &lt;data key="acOutputVolts" value="241.1" />
 *  &lt;data key="pvVolts" value="304.3" />
 *  &lt;data key="pvPower" value="0.681" />
 *  &lt;data key="acPower" value="0.628" />
 *  &lt;data key="kWattHoursToday" value="17.94" />
 *  &lt;data key="decaWattHoursTotal" value="0000167A" />
 * &lt;/info>
 * </pre>
 * 
 * The second is the {@bold data.xml} and {@bold meters.xml} form. The first URL
 * should return XML in the following form:
 * 
 * <pre>
 * &lt;response>
 *   &lt;EnergyToday>0000&lt;/EnergyToday>
 *   &lt;EnergyYesterday>01ED&lt;/EnergyYesterday>
 *   &lt;DU>0000&lt;/DU>
 *   &lt;EnergyLifetime>000BA446&lt;/EnergyLifetime>
 *   &lt;HoursExportedToday>0&lt;/HoursExportedToday>
 *   &lt;HoursExportedYesterday>493&lt;/HoursExportedYesterday>
 *   &lt;HoursExportedLifetime>000A032E&lt;/HoursExportedLifetime>
 *   &lt;DaysProducing>03F5&lt;/DaysProducing>
 * &lt;/response>
 * </pre>
 * 
 * and the second URL in the following form:
 * 
 * <pre>
 * &lt;response>
 *   &lt;OutputPower>0&lt;/OutputPower>
 *   &lt;InputVoltage>0&lt;/InputVoltage>
 *   &lt;OutputVoltage>0&lt;/OutputVoltage>
 * &lt;/response>
 * </pre>
 * 
 * @author matt
 * @version 1.0
 */
public class EnaSolarXMLDatumDataSource extends XmlServiceSupport
		implements DatumDataSource<GeneralNodePVEnergyDatum>, SettingSpecifierProvider {

	/** The {@link SettingDao} key for a Long counter of 0 watt readings. */
	public static final String SETTING_ZERO_WATT_COUNT = "EnaSolarXMLDatumDataSource:0W";

	/** The maximum number of consecutive zero-watt readings to return. */
	public static final long ZERO_WATT_THRESHOLD = 10;

	/** The default value for the {@code urlList} property. */
	public static final String DEFAULT_URL_LIST = "http://enasolar-gt/data.xml,http://enasolar-gt/meters.xml";

	private static final Pattern DATA_VALUE_XPATH_NAME = Pattern.compile("key='(\\w+)'");

	private String[] urls;
	private String sourceId;
	private Map<String, XPathExpression> xpathMapping;
	private Map<String, String> xpathMap;
	private long sampleCacheMs = 5000;

	private EnaSolarPowerDatum sample;
	private Throwable sampleException;
	private final Map<String, Long> validationCache = new HashMap<String, Long>(4);

	private EnaSolarPowerDatum getCurrentSample() {
		EnaSolarPowerDatum datum;
		if ( isCachedSampleExpired() ) {
			datum = new EnaSolarPowerDatum();
			datum.setCreated(new Date());
			datum.setSourceId(sourceId);
			sampleException = null;
			for ( String url : urls ) {
				try {
					webFormGetForBean(null, datum, url, null, xpathMapping);
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
				postDatumCapturedEvent(datum);
			}
		} else {
			datum = sample;
		}
		return datum;
	}

	private boolean isCachedSampleExpired() {
		EnaSolarPowerDatum snap = sample;
		if ( snap == null || sample.getCreated() == null ) {
			return true;
		}
		final long lastReadDiff = System.currentTimeMillis() - sample.getCreated().getTime();
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

	@Override
	public void init() {
		super.init();
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
		result.put("voltage", "//OutputVoltage");
		result.put("DCVoltage", "//InputVoltage");
		result.put("energyLifetime", "//EnergyLifetime");
		return result;
	}

	@Override
	public Class<? extends GeneralNodePVEnergyDatum> getDatumType() {
		return EnaSolarPowerDatum.class;
	}

	@Override
	public GeneralNodePVEnergyDatum readCurrentDatum() {
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

	private boolean isSampleOnSameDay(final Date sampleDate) {
		final Date lastKnownDate = (sample != null ? sample.getCreated() : null);
		if ( sampleDate == null || lastKnownDate == null ) {
			return false;
		}
		final Calendar sampleCal = Calendar.getInstance();
		sampleCal.setTime(sampleDate);
		final Calendar lastKnownCal = Calendar.getInstance();
		lastKnownCal.setTime(lastKnownDate);
		return (sampleCal.get(Calendar.DAY_OF_YEAR) == lastKnownCal.get(Calendar.DAY_OF_YEAR)
				&& sampleCal.get(Calendar.YEAR) == lastKnownCal.get(Calendar.YEAR));
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
				&& isSampleOnSameDay(datum.getCreated()) == false ) {
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
		sourceMeta.addTag(net.solarnetwork.node.domain.EnergyDatum.TAG_GENERATION);
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
		setXpathMapping(getXPathExpressionMap(xpathMap));
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
	public String getSettingUID() {
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
		results.add(new BasicTextFieldSettingSpecifier("groupUID", null));
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
			buf.append(String.format("%tc", snap.getCreated()));
		}
		return (buf.length() < 1 ? "N/A" : buf.toString());
	}

	public String getUrl() {
		return (urls == null || urls.length < 1 ? null : urls[0]);
	}

	public void setUrl(String url) {
		if ( urls == null || urls.length < 1 ) {
			urls = new String[] { url };
		} else {
			urls[0] = url;
		}
	}

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

	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

}
