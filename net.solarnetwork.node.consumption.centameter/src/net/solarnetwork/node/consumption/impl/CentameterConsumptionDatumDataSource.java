/* ===================================================================
 * CentameterConsumptionDatumDataSource.java
 * 
 * Created Jul 23, 2009 9:18:41 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.consumption.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.centameter.CentameterSupport;
import net.solarnetwork.node.centameter.CentameterUtils;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.util.DataUtils;

/**
 * {@link ConsumptionDataSource} implementation for Cent-a-meter monitors, 
 * reading data via a serial port.
 * 
 * <p>This implementation relies on a device that can listen to the radio
 * signal broadcast by a Cent-a-meter monitor and write that data to a
 * local serial port. This class will read the Cent-a-meter data from the
 * serial port to generate consumption data.</p>
 * 
 * <p>It assumes the {@link DataCollector} implementation blocks until 
 * appropriate data is available when the {@link DataCollector#collectData()}
 * method is called.</p>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class CentameterConsumptionDatumDataSource extends CentameterSupport
implements DatumDataSource<ConsumptionDatum>, MultiDatumDataSource<ConsumptionDatum> {
	
	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return ConsumptionDatum.class;
	}

	@Override
	public ConsumptionDatum readCurrentDatum() {
		DataCollector dataCollector = null;
		byte[] data = null;
		try {
			dataCollector = getDataCollectorFactory().getObject();
			dataCollector.collectData();
			data = dataCollector.getCollectedData();
		} finally {
			if ( dataCollector != null ) {
				dataCollector.stopCollecting();
			}
		}
		
		if ( data == null ) {
			log.warn("Null serial data received, serial communications problem");
			return null;
		}
		
		return getConsumptionDatumInstance(
				DataUtils.getUnsignedValues(data), getAmpSensorIndex());
	}
	
	@Override
	public Class<? extends ConsumptionDatum> getMultiDatumType() {
		return ConsumptionDatum.class;
	}

	@Override
	public Collection<ConsumptionDatum> readMultipleDatum() {
		DataCollector dataCollector = null;

		List<ConsumptionDatum> result = new ArrayList<ConsumptionDatum>(3);
		long endTime = isCollectAllSourceIds() && getSourceIdFilter() != null 
				&& getSourceIdFilter().size() > 1
				? System.currentTimeMillis() + (getCollectAllSourceIdsTimeout() * 1000)
				: 0;
		Set<String> sourceIdSet = new HashSet<String>(
				getSourceIdFilter() == null ? 0 : getSourceIdFilter().size());
		try {
			dataCollector = getDataCollectorFactory().getObject();
			do {
				dataCollector.collectData();
				byte[] data = dataCollector.getCollectedData();
				if ( data == null ) {
					log.warn("Null serial data received, serial communications problem");
					return null;
				}
				short[] unsigned = DataUtils.getUnsignedValues(data);
				for ( int ampIndex = 1; ampIndex <= 3; ampIndex++ ) {
					if ( (ampIndex & getMultiAmpSensorIndexFlags()) != ampIndex ) {
						continue;
					}
					ConsumptionDatum datum = getConsumptionDatumInstance(unsigned, ampIndex);
					if ( datum != null ) {
						if ( !sourceIdSet.contains(datum.getSourceId()) ) {
							result.add(datum);
							sourceIdSet.add(datum.getSourceId());
						}
					}
				}
			} while ( System.currentTimeMillis() < endTime && sourceIdSet.size() < 
					(getSourceIdFilter() == null ? 0 : getSourceIdFilter().size()) );
		} finally {
			if ( dataCollector != null ) {
				dataCollector.stopCollecting();
			}
		}
		
		return result.size() < 1 ? null : result;
	}
	
	private ConsumptionDatum getConsumptionDatumInstance(short[] unsigned, int ampIndex) {
		// report the Centameter address as upper-case hex value
		String addr = String.format(getSourceIdFormat(), unsigned[CENTAMETER_ADDRESS_IDX], ampIndex);
		float amps = (float)CentameterUtils.getAmpReading(unsigned, ampIndex);
		
		if ( log.isDebugEnabled() ) {
			log.debug(String.format(
				"Centameter address %s, count %d, amp1 %.1f, amp2 %.1f, amp3 %.1f", 
				addr, 
				( unsigned[2] & 0xF ), 
				CentameterUtils.getAmpReading(unsigned, 1),
				CentameterUtils.getAmpReading(unsigned, 2),
				CentameterUtils.getAmpReading(unsigned, 3)
				));
		}
		
		if ( getAddressSourceMapping() != null && getAddressSourceMapping().containsKey(addr)) {
			addr = getAddressSourceMapping().get(addr);
		}
		if ( getSourceIdFilter() != null && !getSourceIdFilter().contains(addr) ) {
			if ( log.isInfoEnabled() ) {
				log.info("Rejecting source [" +addr +"] not in source ID filter set");
			}
			return null;
		}

		ConsumptionDatum datum = new ConsumptionDatum(addr, amps, getVoltage());
		datum.setCreated(new Date());
		return datum;
	}
		
}
