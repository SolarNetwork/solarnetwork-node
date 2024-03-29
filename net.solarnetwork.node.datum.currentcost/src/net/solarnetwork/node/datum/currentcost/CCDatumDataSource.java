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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.hw.currentcost.CCDatum;
import net.solarnetwork.node.hw.currentcost.CCSupport;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.ByteUtils;

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
 * @author matt
 * @version 4.0
 */
public class CCDatumDataSource extends CCSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	private boolean tagConsumption = true;
	private boolean tagIndoor = true;

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcEnergyDatum.class;
	}

	@Override
	public AcEnergyDatum readCurrentDatum() {
		Set<CCDatum> datumSet = allCachedDataForConfiguredAddresses();
		if ( !datumSet.isEmpty() ) {
			return getGeneralNodeACEnergyDatumInstance(datumSet.iterator().next(), getAmpSensorIndex());
		}
		CCDatum sample = null;
		try {
			sample = performAction(new SerialConnectionAction<CCDatum>() {

				@Override
				public CCDatum doWithConnection(SerialConnection conn) throws IOException {
					byte[] data = conn.readMarkedMessage(MESSAGE_START_MARKER.getBytes(ByteUtils.ASCII),
							MESSAGE_END_MARKER.getBytes(ByteUtils.ASCII));
					if ( data != null && data.length > 0 ) {
						return messageParser.parseMessage(data);
					}
					return null;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(
					"Communication problem reading from serial device " + serialNetwork(), e);
		}
		if ( sample == null ) {
			log.warn("No serial data received for CurrentCost datum");
			return null;
		}

		return getGeneralNodeACEnergyDatumInstance(sample, getAmpSensorIndex());
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AcEnergyDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		final Set<String> sourceIdSet = (new HashSet<>(
				getSourceIdFilter() == null ? 0 : getSourceIdFilter().size()));
		final List<NodeDatum> result = new ArrayList<>(4);
		Set<CCDatum> datumSet = allCachedDataForConfiguredAddresses();
		for ( CCDatum ccDatum : datumSet ) {
			processSample(result, sourceIdSet, ccDatum);
		}
		if ( !needMoreSamplesForSources(sourceIdSet) ) {
			return result;
		}
		final long endTime = (isCollectAllSourceIds() && getSourceIdFilter() != null
				&& getSourceIdFilter().size() > 1
						? System.currentTimeMillis() + (getCollectAllSourceIdsTimeout() * 1000)
						: 0);

		try {
			performAction(new SerialConnectionAction<Object>() {

				@Override
				public Object doWithConnection(SerialConnection conn) throws IOException {
					do {
						byte[] data = conn.readMarkedMessage("<msg>".getBytes("US-ASCII"),
								"</msg>".getBytes("US-ASCII"));
						if ( data == null ) {
							log.warn("Null serial data received, serial communications problem");
							return null;
						}
						CCDatum ccDatum = messageParser.parseMessage(data);

						if ( ccDatum == null || ccDatum.getDeviceAddress() == null ) {
							continue;
						}

						// add a known address for this reading
						addKnownAddress(ccDatum);

						processSample(result, sourceIdSet, ccDatum);
					} while ( System.currentTimeMillis() < endTime
							&& needMoreSamplesForSources(sourceIdSet) );
					return null;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(
					"Communication problem reading from serial device " + serialNetwork(), e);
		}

		return result;
	}

	private boolean needMoreSamplesForSources(Set<String> sourceIdSet) {
		return (sourceIdSet.isEmpty()
				|| sourceIdSet.size() < (getSourceIdFilter() == null ? 0 : getSourceIdFilter().size()));
	}

	private void processSample(List<NodeDatum> result, Set<String> sourceIdSet, CCDatum ccDatum) {
		if ( log.isDebugEnabled() ) {
			log.debug("Got CCDatum: {}", ccDatum.getStatusMessage());
		}

		for ( int ampIndex = 1; ampIndex <= 3; ampIndex++ ) {
			if ( (ampIndex & getMultiAmpSensorIndexFlags()) != ampIndex ) {
				continue;
			}
			AcEnergyDatum datum = getGeneralNodeACEnergyDatumInstance(ccDatum, ampIndex);
			if ( datum != null && !sourceIdSet.contains(datum.getSourceId()) ) {
				result.add(datum);
				sourceIdSet.add(datum.getSourceId());
			}
		}

		AcEnergyDatum datum = getGeneralNodeDatumTemperatureInstance(ccDatum);
		if ( datum != null && !sourceIdSet.contains(datum.getSourceId()) ) {
			result.add(datum);
			sourceIdSet.add(datum.getSourceId());
		}
	}

	private AcEnergyDatum getGeneralNodeACEnergyDatumInstance(CCDatum datum, int ampIndex) {
		if ( datum == null ) {
			return null;
		}
		String addr = resolvePlaceholders(addressValue(datum, ampIndex));
		if ( getAddressSourceMapping() != null && getAddressSourceMapping().containsKey(addr) ) {
			addr = getAddressSourceMapping().get(addr);
		}
		if ( getSourceIdFilter() != null && !getSourceIdFilter().contains(addr) ) {
			if ( log.isInfoEnabled() ) {
				log.info("Rejecting source [" + addr + "] not in source ID filter set");
			}
			return null;
		}

		Integer wattReading = (ampIndex == 2 ? datum.getChannel2Watts()
				: ampIndex == 3 ? datum.getChannel3Watts() : datum.getChannel1Watts());

		SimpleAcEnergyDatum result = new SimpleAcEnergyDatum(resolvePlaceholders(addr),
				datum.getCreated(), new DatumSamples());
		result.setWatts(wattReading);

		// associate consumption/generation tags with this source
		GeneralDatumMetadata sourceMeta = new GeneralDatumMetadata();
		if ( isTagConsumption() ) {
			sourceMeta.addTag(AcEnergyDatum.TAG_CONSUMPTION);
		} else {
			sourceMeta.addTag(AcEnergyDatum.TAG_GENERATION);
		}
		addSourceMetadata(addr, sourceMeta);

		return result;
	}

	private AcEnergyDatum getGeneralNodeDatumTemperatureInstance(CCDatum datum) {
		if ( datum == null ) {
			return null;
		}
		String addr = resolvePlaceholders(datum.getDeviceAddress() + ".T");
		if ( getAddressSourceMapping() != null && getAddressSourceMapping().containsKey(addr) ) {
			addr = getAddressSourceMapping().get(addr);
		}
		if ( getSourceIdFilter() != null && !getSourceIdFilter().contains(addr) ) {
			if ( log.isInfoEnabled() ) {
				log.info("Rejecting source [" + addr + "] not in source ID filter set");
			}
			return null;
		}

		SimpleAcEnergyDatum result = new SimpleAcEnergyDatum(resolvePlaceholders(addr),
				datum.getCreated(), new DatumSamples());
		result.asMutableSampleOperations().putSampleValue(DatumSamplesType.Instantaneous,
				AtmosphericDatum.TEMPERATURE_KEY, datum.getTemperature());

		// associate indoor/outdoor tags with this source
		GeneralDatumMetadata sourceMeta = new GeneralDatumMetadata();
		if ( isTagIndoor() ) {
			sourceMeta.addTag(AtmosphericDatum.TAG_ATMOSPHERE_INDOOR);
		} else {
			sourceMeta.addTag(AtmosphericDatum.TAG_ATMOSPHERE_OUTDOOR);
		}
		addSourceMetadata(addr, sourceMeta);

		return result;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.currentcost";
	}

	@Override
	public String getDisplayName() {
		return "CurrentCost amp meter";
	}

	private final Set<String> SPECS_FILTER = new HashSet<String>(
			Arrays.asList("sourceIdFormat", "voltage"));

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

		// remove some we don't want, insert addressSourceMappingValueExample
		for ( ListIterator<SettingSpecifier> itr = specs.listIterator(); itr.hasNext(); ) {
			SettingSpecifier spec = itr.next();
			if ( spec instanceof KeyedSettingSpecifier<?> ) {
				KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
				if ( SPECS_FILTER.contains(keyedSpec.getKey()) ) {
					itr.remove();
				} else if ( "addressSourceMappingValue".equals(keyedSpec.getKey()) ) {
					StringBuilder buf = new StringBuilder();
					for ( CCDatum sample : getKnownAddresses() ) {
						if ( buf.length() > 0 ) {
							buf.append("<br>\n");
						}
						String format = getSourceIdFormat();
						for ( int sensor = 1; sensor <= 3; sensor += 1 ) {
							buf.append(String.format(format, sample.getDeviceAddress(), sensor));
							buf.append(" = Phase").append(sensor).append(", ");
						}
						buf.append(sample.getDeviceAddress() + ".T = Temperature");
					}
					if ( buf.length() > 0 ) {
						itr.add(new BasicTitleSettingSpecifier("addressSourceMappingValueExample",
								buf.toString(), true));
					}
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
