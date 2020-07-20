/* ==================================================================
 * WMBusDatumDataSource.java - 06/07/2020 13:09:29 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.mbus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.mbus.MBusData;
import net.solarnetwork.node.io.mbus.support.WMBusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

public class WMBusDatumDataSource extends WMBusDeviceDatumDataSourceSupport
		implements DatumDataSource<GeneralNodeDatum>, SettingSpecifierProvider {

	private String sourceId;
	private MBusPropertyConfig[] propConfigs;

	public WMBusDatumDataSource() {
		super();
		sourceId = "wmbus";
	}

	/**
	 * Get the property configurations.
	 * 
	 * @return the property configurations
	 */
	public MBusPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Set the property configurations to use.
	 * 
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(MBusPropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		MBusPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link MBusPropertyConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count, MBusPropertyConfig.class,
				null);
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param soruceId
	 *        the source ID to use; defaults to {@literal wmbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	@Override
	public GeneralNodeDatum readCurrentDatum() {
		final MBusData currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setCreated(new Date(currSample.getDataTimestamp()));
		d.setSourceId(sourceId);
		populateDatumProperties(currSample, d, propConfigs);
		return d;
	}

	private void populateDatumProperties(MBusData sample, GeneralNodeDatum d,
			MBusPropertyConfig[] propConfs) {
		if ( propConfs == null ) {
			return;
		}
		for ( MBusPropertyConfig conf : propConfs ) {
			// skip configurations without a property to set
			if ( conf.getPropertyKey() == null || conf.getPropertyKey().length() < 1 ) {
				continue;
			}
			Object propVal = null;
			switch (conf.getDataType()) {
				case BCD:
				case Double:
				case Long:
					propVal = sample.getScaledValue(conf.getDataDescription());
					break;
				case Date:
					propVal = sample.getDateValue(conf.getDataDescription());
					break;
				case String:
					propVal = sample.getStringValue(conf.getDataDescription());
				case None:
					break;
				default:
					break;
			}

			if ( propVal instanceof Number ) {
				if ( conf.getUnitMultiplier() != null ) {
					propVal = applyUnitMultiplier((Number) propVal, conf.getUnitMultiplier());
				}
				if ( conf.getDecimalScale() >= 0 ) {
					propVal = applyDecimalScale((Number) propVal, conf.getDecimalScale());
				}
			}

			if ( propVal != null ) {
				switch (conf.getPropertyType()) {
					case Accumulating:
					case Instantaneous:
						if ( !(propVal instanceof Number) ) {
							log.warn(
									"Cannot set datum accumulating property {} to non-number value [{}]",
									conf.getPropertyKey(), propVal);
							continue;
						}

					default:
						// nothing
				}
				d.putSampleValue(conf.getPropertyType(), conf.getPropertyKey(), propVal);
			}
		}
	}

	private Number applyDecimalScale(Number value, int decimalScale) {
		if ( decimalScale < 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		if ( v.scale() > decimalScale ) {
			v = v.setScale(decimalScale, RoundingMode.HALF_UP);
		}
		return v;
	}

	private Number applyUnitMultiplier(Number value, BigDecimal multiplier) {
		if ( BigDecimal.ONE.compareTo(multiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.multiply(multiplier);
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.mbus";
	}

	@Override
	public String getDisplayName() {
		return "Generic Wireless M-Bus Device";
	}

	private String getSampleMessage(MBusData sample) {
		if ( sample == null || sample.getDataTimestamp() < 1 ) {
			return "N/A";
		}

		GeneralNodeDatum d = new GeneralNodeDatum();
		populateDatumProperties(sample, d, propConfigs);

		Map<String, ?> data = d.getSampleData();
		if ( data == null || data.isEmpty() ) {
			return "No data.";
		}

		StringBuilder buf = new StringBuilder();
		buf.append(StringUtils.delimitedStringFromMap(data));
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(sample.getDataTimestamp())));
		return buf.toString();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();

		results.add(0,
				new BasicTitleSettingSpecifier("sample", getSampleMessage(getCurrentSample()), true));

		results.addAll(getWMBusNetworkSettingSpecifiers());

		WMBusDatumDataSource defaults = new WMBusDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));

		MBusPropertyConfig[] confs = getPropConfigs();
		List<MBusPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<MBusPropertyConfig> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingsUtil.KeyedListCallback<MBusPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(MBusPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								MBusPropertyConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return results;
	}

	@Override
	public Class<? extends GeneralNodeDatum> getDatumType() {
		return GeneralNodeDatum.class;
	}

}
