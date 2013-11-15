/* ==================================================================
 * DeviceInfoDatumDataSource.java - Oct 2, 2011 8:50:13 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.power.enasolar.ws;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPathExpression;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.Setting;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.power.PowerDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.XmlServiceSupport;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Web service based support for EnaSolar inverters.
 * 
 * <p>
 * This expects to access a URL that returns XML in the following form:
 * </p>
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
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>url</dt>
 * <dd>The URL for accessing the XML data from.</dd>
 * 
 * <dt>sourceId</dt>
 * <dd>The source ID value to assign to the collected data.</dd>
 * 
 * <dt>setXpathMapping</dt>
 * <dd>A mapping of PowerDatum JavaBean property names to corresponding XPath
 * expressions for extracting the data from the XML response. This property can
 * also be configured via {@link #setXpathMap(Map)} using String values, which
 * is useful when using Spring. Defaults to a sensible value, so this should
 * only be configured in special cases.</dd>
 * 
 * <dt>settingDao</dt>
 * <dd>The {@link SettingDao} to use to keep track of "last known" values to
 * compensate for inverter power cuts. When the inverter power is cut, it might
 * return stale data in the XML service, which we want to discard.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class DeviceInfoDatumDataSource extends XmlServiceSupport implements DatumDataSource<PowerDatum>,
		SettingSpecifierProvider {

	/** The {@link SettingDao} key for a Long Wh last-known-value value. */
	public static final String SETTING_LAST_KNOWN_VALUE = "DeviceInfoDatumDataSource:globalWh";

	/** The {@link SettingDao} key for a Long counter of 0 watt readings. */
	public static final String SETTING_ZERO_WATT_COUNT = "DeviceInfoDatumDataSource:0W";

	/** The maximum number of consecutive zero-watt readings to return. */
	public static final long ZERO_WATT_THRESHOLD = 10;

	/** The default value for the {@code url} property. */
	public static final String DEFAULT_URL = "http://localhost:8082/gs/deviceinfo.xml";

	private static final Pattern DATA_VALUE_XPATH_NAME = Pattern.compile("key='(\\w+)'");

	private String url = DEFAULT_URL;
	private String sourceId;
	private Map<String, XPathExpression> xpathMapping;
	private SettingDao settingDao;

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	@Override
	public String toString() {
		return "Enasolar" + (sourceId == null ? "" : "-" + sourceId);
	}

	@Override
	public void init() {
		super.init();
		if ( xpathMapping == null ) {
			xpathMapping = getXPathExpressionMap(defaultXpathMapping());
		}
	}

	private static Map<String, String> defaultXpathMapping() {
		Map<String, String> result = new LinkedHashMap<String, String>(10);
		result.put("pvVolts", "//data[@key='pvVolts']/@value");
		result.put("pvPower", "//data[@key='pvPower']/@value");
		result.put("acOutputVolts", "//data[@key='acOutputVolts']/@value");
		result.put("acOutputAmps", "//data[@key='acPower']/@value");
		result.put("decaWattHoursTotal", "//data[@key='decaWattHoursTotal']/@value");
		return result;
	}

	@Override
	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	@Override
	public PowerDatum readCurrentDatum() {
		PowerDatum datum = new PowerDatum();
		datum.setSourceId(sourceId);
		webFormGetForBean(null, datum, url, null, xpathMapping);
		datum = validateDatum(datum);

		return datum;
	}

	private PowerDatum validateDatum(PowerDatum datum) {
		if ( settingDao == null ) {
			// nothing to compare
			return datum;
		}

		final String settingType = (sourceId == null ? "" : sourceId);
		final Long currValue = datum.getWattHourReading();
		final Long lastKnownValue;
		final Long zeroWattCount;
		final String lastKnownValueStr = settingDao.getSetting(SETTING_LAST_KNOWN_VALUE, settingType);
		lastKnownValue = (lastKnownValueStr == null ? 0L : Long.parseLong(lastKnownValueStr));
		final String zeroWattCountStr = settingDao.getSetting(SETTING_ZERO_WATT_COUNT, settingType);
		zeroWattCount = (zeroWattCountStr == null ? 0L : Long.parseLong(zeroWattCountStr));

		// we've seen values reported less than last known value after
		// a power outage (i.e. after inverter turns off, then back on)
		// on single day, so we verify that current decaWattHoursTotal is not less 
		// than last known decaWattHoursTotal value
		if ( currValue.longValue() < lastKnownValue.longValue() ) {
			log.warn("Inverter [" + sourceId + "] reported value [" + currValue
					+ "] -- less than last known value [" + lastKnownValue + "]. Discarding this datum.");
			datum = null;
		} else if ( datum.getWatts() != null && datum.getWatts() < 1 ) {
			final Long newCount = (zeroWattCount.longValue() + 1);
			if ( zeroWattCount >= ZERO_WATT_THRESHOLD ) {
				log.debug("Skipping zero-watt reading #{}", zeroWattCount);
				datum = null;
			}
			settingDao.storeSetting(new Setting(SETTING_ZERO_WATT_COUNT, settingType, newCount
					.toString(), EnumSet.of(Setting.SettingFlag.Volatile)));
		} else {
			if ( currValue.longValue() != lastKnownValue.longValue() ) {
				settingDao.storeSetting(new Setting(SETTING_LAST_KNOWN_VALUE, settingType, currValue
						.toString(), EnumSet.of(Setting.SettingFlag.Volatile)));
			}
			if ( zeroWattCount > 0 ) {
				// reset zero-watt counter
				settingDao.deleteSetting(SETTING_ZERO_WATT_COUNT, settingType);
			}
		}

		// everything checks out
		return datum;
	}

	/**
	 * Set the XPath mapping using String values.
	 * 
	 * @param xpathMap
	 *        the string XPath mapping values
	 */
	public void setXpathMap(Map<String, String> xpathMap) {
		setXpathMapping(getXPathExpressionMap(xpathMap));
	}

	// for settings
	private static String getDataMapping(Map<String, String> map) {
		StringBuilder buf = new StringBuilder();
		if ( map != null ) {
			for ( Map.Entry<String, String> me : map.entrySet() ) {
				Matcher m = DATA_VALUE_XPATH_NAME.matcher(me.getValue().toString());
				if ( m.find() ) {
					if ( buf.length() > 0 ) {
						buf.append(", ");
					}
					buf.append(me.getKey()).append('=').append(m.group(1));
				}
			}
		}
		return buf.toString();
	}

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
			map.put(kv[0], "//data[@key='" + kv[1] + "']/@value");
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
		return getDefaultSettingSpecifiers();
	}

	@Override
	public MessageSource getMessageSource() {
		synchronized ( MONITOR ) {
			if ( MESSAGE_SOURCE == null ) {
				ResourceBundleMessageSource source = new ResourceBundleMessageSource();
				source.setBundleClassLoader(getClass().getClassLoader());
				source.setBasename(getClass().getName());
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}

	public static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(10);
		result.add(new BasicTextFieldSettingSpecifier("url", "http://localhost:8082/gs/deviceinfo.xml"));
		result.add(new BasicTextFieldSettingSpecifier("sourceId", ""));
		result.add(new BasicTextFieldSettingSpecifier("dataMapping",
				getDataMapping(defaultXpathMapping())));
		return result;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Map<String, XPathExpression> getXpathMapping() {
		return xpathMapping;
	}

	public void setXpathMapping(Map<String, XPathExpression> xpathMapping) {
		this.xpathMapping = xpathMapping;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public SettingDao getSettingDao() {
		return settingDao;
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

}
