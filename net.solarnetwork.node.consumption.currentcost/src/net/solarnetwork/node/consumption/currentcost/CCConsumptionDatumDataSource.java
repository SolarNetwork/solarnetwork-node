/* ==================================================================
 * CCConsumptionDatumDataSource.java - Apr 26, 2013 11:33:27 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.consumption.currentcost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.hw.currentcost.CCDatum;
import net.solarnetwork.node.hw.currentcost.CCSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.support.DataCollectorSerialPortBeanParameters;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * {@link DatumDataSource} implementation for CurrentCost watt monitors, reading
 * data via a serial port.
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
public class CCConsumptionDatumDataSource extends CCSupport implements
		DatumDataSource<ConsumptionDatum>, MultiDatumDataSource<ConsumptionDatum>,
		SettingSpecifierProvider {

	private static MessageSource MESSAGE_SOURCE;

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return ConsumptionDatum.class;
	}

	@Override
	public ConsumptionDatum readCurrentDatum() {
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

		return getConsumptionDatumInstance(messageParser.parseMessage(data), getAmpSensorIndex());
	}

	@Override
	public Class<? extends ConsumptionDatum> getMultiDatumType() {
		return ConsumptionDatum.class;
	}

	@Override
	public Collection<ConsumptionDatum> readMultipleDatum() {
		DataCollectorFactory<DataCollectorSerialPortBeanParameters> df = getDataCollectorFactory()
				.service();
		if ( df == null ) {
			return Collections.emptyList();
		}

		List<ConsumptionDatum> result = new ArrayList<ConsumptionDatum>(3);
		long endTime = isCollectAllSourceIds() && getSourceIdFilter() != null
				&& getSourceIdFilter().size() > 1 ? System.currentTimeMillis()
				+ (getCollectAllSourceIdsTimeout() * 1000) : 0;
		Set<String> sourceIdSet = new HashSet<String>(getSourceIdFilter() == null ? 0
				: getSourceIdFilter().size());
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

				if ( ccDatum == null ) {
					continue;
				}

				// add a known address for this reading
				addKnownAddress(ccDatum);

				if ( log.isDebugEnabled() ) {
					log.debug("Got CCDatum: {}", ccDatum.getStatusMessage());
				}

				for ( int ampIndex = 1; ampIndex <= 3; ampIndex++ ) {
					ConsumptionDatum datum = getConsumptionDatumInstance(ccDatum, ampIndex);
					if ( (ampIndex & getMultiAmpSensorIndexFlags()) != ampIndex ) {
						continue;
					}
					if ( datum != null ) {
						if ( !sourceIdSet.contains(datum.getSourceId()) ) {
							result.add(datum);
							sourceIdSet.add(datum.getSourceId());
						}
					}
				}
			} while ( System.currentTimeMillis() < endTime
					&& sourceIdSet.size() < (getSourceIdFilter() == null ? 0 : getSourceIdFilter()
							.size()) );
		} finally {
			if ( dc != null ) {
				dc.stopCollecting();
			}
		}

		return result;
	}

	private ConsumptionDatum getConsumptionDatumInstance(CCDatum datum, int ampIndex) {
		if ( datum == null ) {
			return null;
		}
		String addr = String.format(getSourceIdFormat(), datum.getDeviceAddress(), ampIndex);
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
		Float ampReading = (wattReading == null || wattReading.intValue() == 0 ? 0.0F : wattReading
				.floatValue() / getVoltage());

		ConsumptionDatum result = new ConsumptionDatum(addr, ampReading, getVoltage());
		result.setCreated(new Date());
		return result;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.consumption.currentcost";
	}

	@Override
	public String getDisplayName() {
		return "CurrentCost consumption meter";
	}

	@Override
	public synchronized MessageSource getMessageSource() {
		if ( MESSAGE_SOURCE == null ) {
			MessageSource parent = getDefaultSettingsMessageSource();

			ResourceBundleMessageSource source = new ResourceBundleMessageSource();
			source.setBundleClassLoader(CCConsumptionDatumDataSource.class.getClassLoader());
			source.setBasename(CCConsumptionDatumDataSource.class.getName());
			source.setParentMessageSource(parent);
			MESSAGE_SOURCE = source;
		}
		return MESSAGE_SOURCE;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return getDefaultSettingSpecifiers();
	}
}
