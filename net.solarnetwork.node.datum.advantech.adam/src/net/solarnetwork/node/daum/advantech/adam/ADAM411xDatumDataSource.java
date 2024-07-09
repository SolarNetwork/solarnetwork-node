/* ==================================================================
 * ADAM411xDatumDataSource.java - 22/11/2018 12:49:23 PM
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

package net.solarnetwork.node.daum.advantech.adam;

import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.node.domain.datum.MutableNodeDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.hw.advantech.adam.ADAM411xData;
import net.solarnetwork.node.hw.advantech.adam.ADAM411xDataAccessor;
import net.solarnetwork.node.hw.advantech.adam.InputRangeType;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Datum data source for ADAM 411x series devices.
 *
 * @author matt
 * @version 2.1
 */
public class ADAM411xDatumDataSource extends ModbusDataDatumDataSourceSupport<ADAM411xData>
		implements DatumDataSource, SettingSpecifierProvider {

	private static final int CHANNEL_COUNT = 8;

	private String sourceId;
	private ChannelPropertyConfig[] propConfigs;

	/**
	 * Default constructor.
	 */
	public ADAM411xDatumDataSource() {
		this(new ADAM411xData());
	}

	/**
	 * Construct with a sample instance.
	 *
	 * @param sample
	 *        the sample to use
	 */
	public ADAM411xDatumDataSource(ADAM411xData sample) {
		super(sample);
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, ADAM411xData sample)
			throws IOException {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, ADAM411xData sample)
			throws IOException {
		sample.readDeviceData(connection);
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		if ( sourceId == null ) {
			return null;
		}
		try {
			final ADAM411xData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			SimpleDatum d = SimpleDatum.nodeDatum(resolvePlaceholders(sourceId),
					currSample.getDataTimestamp());
			if ( !populateDatumProperties(currSample, d, getPropConfigs()) ) {
				return null;
			}

			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from ADAM 411x device {}: {}", sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	private boolean populateDatumProperties(ADAM411xData sample, MutableNodeDatum d,
			ChannelPropertyConfig[] propConfs) {
		if ( propConfs == null || propConfs.length < 1 ) {
			return false;
		}
		MutableDatumSamplesOperations ops = d.asMutableSampleOperations();
		boolean result = false;
		for ( ChannelPropertyConfig conf : propConfs ) {
			// skip configurations without a property to set
			if ( conf.getPropertyKey() == null || conf.getPropertyKey().length() < 1 ) {
				continue;
			}
			Number propVal = sample.getChannelValue(conf.getChannel());
			propVal = conf.applyTransformations(propVal);

			if ( propVal != null ) {
				ops.putSampleValue(conf.getPropertyType(), conf.getPropertyKey(), propVal);
				result = true;
			}
		}
		return result;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.daum.advantech.adam411x";
	}

	@Override
	public String getDisplayName() {
		return "ADAM 411x";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		ADAM411xDatumDataSource defaults = new ADAM411xDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));

		ChannelPropertyConfig[] confs = getPropConfigs();
		List<ChannelPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<ChannelPropertyConfig> emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingUtils.KeyedListCallback<ChannelPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(ChannelPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								ChannelPropertyConfig.settings(key + ".", CHANNEL_COUNT));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));
		return results;
	}

	private String getInfoMessage() {
		String msg = null;
		try {
			msg = getDeviceInfoMessage();
		} catch ( RuntimeException e ) {
			log.debug("Error reading info: {}", e.getMessage());
		}
		return (msg == null ? "N/A" : msg);
	}

	private String getSampleMessage(ADAM411xDataAccessor data) {
		if ( data.getDataTimestamp() == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		Set<Integer> enabledChannelSet = data.getEnabledChannelNumbers();
		if ( enabledChannelSet == null || enabledChannelSet.isEmpty() ) {
			buf.append("No channels enabled");
		} else {
			for ( Integer channel : enabledChannelSet ) {
				InputRangeType type = data.getChannelType(channel);
				if ( buf.length() > 0 ) {
					buf.append("; ");
				}
				buf.append("Channel ").append(channel).append(": ");
				BigDecimal val = data.getChannelValue(channel);
				if ( val != null ) {
					val = val.setScale(5, RoundingMode.HALF_UP);
					buf.append(val);
				} else {
					buf.append("?");
				}
				buf.append(" ").append(type.getUnit().getKey());
			}

		}
		buf.append("; sampled at ").append(formatForLocalDisplay(data.getDataTimestamp()));
		return buf.toString();
	}

	/**
	 * Get the source ID to use for returned datum.
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to use for returned datum.
	 *
	 * @param sourceId
	 *        the source ID to use
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the channel property configurations.
	 *
	 * @return the property configurations
	 */
	public ChannelPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Set the channel property configurations.
	 *
	 * @param propConfigs
	 *        the configurations to set
	 */
	public void setPropConfigs(ChannelPropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 *
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		ChannelPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ChannelPropertyConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count,
				ChannelPropertyConfig.class, null);
	}
}
