/* ==================================================================
 * CCDatumDataSource.java - Aug 26, 2014 10:19:02 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.currentcost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.AtmosphereDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.hw.currentcost.CCDatum;
import net.solarnetwork.node.hw.currentcost.CCSupport;
import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.support.DataCollectorSerialPortBeanParameters;

/**
 * {@link MultiDatumDataSource} implementation for CurrentCost watt monitors,
 * reading data via a serial port.
 * 
 * <p>
 * This implementation relies on a device that can listen to the radio signal
 * broadcast by a CurrentCost watt meters and write that data to a local serial
 * port. This class will read the device data from the serial port to generate
 * consumption data.
 * </p>
 * 
 * <p>
 * It assumes the {@link DataCollector} implementation blocks until appropriate
 * data is available when the {@link DataCollector#collectData()} method is
 * called.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class CCDatumDataSource extends CCSupport implements DatumDataSource<GeneralNodeDatum>,
		MultiDatumDataSource<GeneralNodeDatum>, SettingSpecifierProvider {

	private boolean tagConsumption = true;
	private boolean tagIndoor = true;

	@Override
	public Class<? extends GeneralNodeDatum> getDatumType() {
		return GeneralNodeDatum.class;
	}

	@Override
	public GeneralNodeDatum readCurrentDatum() {
		Set<CCDatum> datumSet = allCachedDataForConfiguredAddresses();
		if ( !datumSet.isEmpty() ) {
			return getGeneralNodeACEnergyDatumInstance(datumSet.iterator().next(), getAmpSensorIndex());
		}

		DataCollectorFactory<DataCollectorSerialPortBeanParameters> df = getDataCollectorFactory()
				.service();
		if ( df == null ) {
			log.debug("No DataCollectorFactory available");
			return null;
		}

		byte[] data = null;
		DataCollector dc = df.getDataCollectorInstance(getSerialParams());
		try {
			dc.collectData();
			data = dc.getCollectedData();
		} finally {
			if ( dc != null ) {
				dc.stopCollecting();
			}
		}

		if ( data == null || data.length == 0 ) {
			log.warn("No serial data received, serial communications problem");
			return null;
		}

		return getGeneralNodeACEnergyDatumInstance(messageParser.parseMessage(data), getAmpSensorIndex());
	}

	@Override
	public Class<? extends GeneralNodeDatum> getMultiDatumType() {
		return GeneralNodeDatum.class;
	}

	@Override
	public Collection<GeneralNodeDatum> readMultipleDatum() {
		Set<String> sourceIdSet = new HashSet<String>(getSourceIdFilter() == null ? 0
				: getSourceIdFilter().size());
		List<GeneralNodeDatum> result = new ArrayList<GeneralNodeDatum>(4);
		Set<CCDatum> datumSet = allCachedDataForConfiguredAddresses();
		for ( CCDatum ccDatum : datumSet ) {
			processSample(result, sourceIdSet, ccDatum);
		}
		if ( !needMoreSamplesForSources(sourceIdSet) ) {
			return result;
		}
		DataCollectorFactory<DataCollectorSerialPortBeanParameters> df = getDataCollectorFactory()
				.service();
		if ( df == null ) {
			return Collections.emptyList();
		}

		long endTime = isCollectAllSourceIds() && getSourceIdFilter() != null
				&& getSourceIdFilter().size() > 1 ? System.currentTimeMillis()
				+ (getCollectAllSourceIdsTimeout() * 1000) : 0;
		DataCollector dc = null;
		try {
			dc = df.getDataCollectorInstance(getSerialParams());
			do {
				dc.collectData();
				byte[] data = dc.getCollectedData();
				if ( data == null ) {
					log.warn("Null serial data received, serial communications problem");
					return Collections.emptyList();
				}
				CCDatum ccDatum = messageParser.parseMessage(data);

				if ( ccDatum == null || ccDatum.getDeviceAddress() == null ) {
					continue;
				}

				// add a known address for this reading
				addKnownAddress(ccDatum);

				processSample(result, sourceIdSet, ccDatum);
			} while ( System.currentTimeMillis() < endTime && needMoreSamplesForSources(sourceIdSet) );
		} finally {
			if ( dc != null ) {
				dc.stopCollecting();
			}
		}

		return result;
	}

	private boolean needMoreSamplesForSources(Set<String> sourceIdSet) {
		return (sourceIdSet.isEmpty() || sourceIdSet.size() < (getSourceIdFilter() == null ? 0
				: getSourceIdFilter().size()));
	}

	private void processSample(List<GeneralNodeDatum> result, Set<String> sourceIdSet, CCDatum ccDatum) {
		if ( log.isDebugEnabled() ) {
			log.debug("Got CCDatum: {}", ccDatum.getStatusMessage());
		}

		for ( int ampIndex = 1; ampIndex <= 3; ampIndex++ ) {
			if ( (ampIndex & getMultiAmpSensorIndexFlags()) != ampIndex ) {
				continue;
			}
			GeneralNodeDatum datum = getGeneralNodeACEnergyDatumInstance(ccDatum, ampIndex);
			if ( datum != null && !sourceIdSet.contains(datum.getSourceId()) ) {
				result.add(datum);
				sourceIdSet.add(datum.getSourceId());
			}
		}

		GeneralNodeDatum datum = getGeneralNodeDatumTemperatureInstance(ccDatum);
		if ( datum != null && !sourceIdSet.contains(datum.getSourceId()) ) {
			result.add(datum);
			sourceIdSet.add(datum.getSourceId());
		}
	}

	private GeneralNodeACEnergyDatum getGeneralNodeACEnergyDatumInstance(CCDatum datum, int ampIndex) {
		if ( datum == null ) {
			return null;
		}
		String addr = addressValue(datum, ampIndex);
		if ( getAddressSourceMapping() != null && getAddressSourceMapping().containsKey(addr) ) {
			addr = getAddressSourceMapping().get(addr);
		}
		if ( getSourceIdFilter() != null && !getSourceIdFilter().contains(addr) ) {
			if ( log.isInfoEnabled() ) {
				log.info("Rejecting source [" + addr + "] not in source ID filter set");
			}
			return null;
		}

		Integer wattReading = (ampIndex == 2 ? datum.getChannel2Watts() : ampIndex == 3 ? datum
				.getChannel3Watts() : datum.getChannel1Watts());

		GeneralNodeACEnergyDatum result = new GeneralNodeACEnergyDatum();
		result.setCreated(new Date(datum.getCreated()));
		result.setSourceId(addr);
		result.setWatts(wattReading);
		if ( isTagConsumption() ) {
			result.tagAsConsumption();
		} else {
			result.tagAsGeneration();
		}
		return result;
	}

	private GeneralNodeDatum getGeneralNodeDatumTemperatureInstance(CCDatum datum) {
		if ( datum == null ) {
			return null;
		}
		String addr = datum.getDeviceAddress() + ".T";
		if ( getAddressSourceMapping() != null && getAddressSourceMapping().containsKey(addr) ) {
			addr = getAddressSourceMapping().get(addr);
		}
		if ( getSourceIdFilter() != null && !getSourceIdFilter().contains(addr) ) {
			if ( log.isInfoEnabled() ) {
				log.info("Rejecting source [" + addr + "] not in source ID filter set");
			}
			return null;
		}

		GeneralNodeDatum result = new GeneralNodeDatum();
		result.setCreated(new Date(datum.getCreated()));
		result.setSourceId(addr);
		result.putInstantaneousSampleValue(AtmosphereDatum.TEMPERATURE_KEY, datum.getTemperature());
		if ( isTagIndoor() ) {
			result.addTag(AtmosphereDatum.TAG_ATMOSPHERE_INDOOR);
		} else {
			result.addTag(AtmosphereDatum.TAG_ATMOSPHERE_OUTDOOR);
		}
		return result;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.currentcost";
	}

	@Override
	public String getDisplayName() {
		return "CurrentCost amp meter";
	}

	private final Set<String> SPECS_FILTER = new HashSet<String>(Arrays.asList("sourceIdFormat",
			"voltage"));

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		CCDatumDataSource defaults = new CCDatumDataSource();
		List<SettingSpecifier> specs = getDefaultSettingSpecifiers();
		SettingSpecifier energyTag = new BasicToggleSettingSpecifier("tagConsumption",
				Boolean.valueOf(defaults.isTagConsumption()));
		SettingSpecifier atmosTag = new BasicToggleSettingSpecifier("tagIndoor",
				Boolean.valueOf(defaults.isTagIndoor()));
		if ( specs.size() > 4 ) {
			specs.add(4, atmosTag);
			specs.add(4, energyTag);
		} else {
			specs.add(energyTag);
			specs.add(atmosTag);
		}
		// remove some we don't want
		for ( Iterator<SettingSpecifier> itr = specs.iterator(); itr.hasNext(); ) {
			SettingSpecifier spec = itr.next();
			if ( spec instanceof KeyedSettingSpecifier<?> ) {
				KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
				if ( SPECS_FILTER.contains(keyedSpec.getKey()) ) {
					itr.remove();
				}
			}
		}
		return specs;
	}

	public boolean isTagConsumption() {
		return tagConsumption;
	}

	public void setTagConsumption(boolean tagConsumption) {
		this.tagConsumption = tagConsumption;
	}

	public boolean isTagIndoor() {
		return tagIndoor;
	}

	public void setTagIndoor(boolean tagIndoor) {
		this.tagIndoor = tagIndoor;
	}

}
