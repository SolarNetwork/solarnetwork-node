/* ==================================================================
 * MBusDatumDataSource.java - 13/08/2020 12:49:12 pm
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

package net.solarnetwork.node.io.mbus.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.mbus.MBusConnection;
import net.solarnetwork.node.io.mbus.MBusData;
import net.solarnetwork.node.io.mbus.MBusNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;
import net.solarnetwork.util.OptionalService;

public abstract class MBusDeviceDatumDataSourceSupport extends DatumDataSourceSupport {

	private OptionalService<MBusNetwork> mbusNetwork;
	private int address;
	private MBusConnection connection = null;
	private long sampleCacheMs;

	// Latest complete data
	private MBusData latestData = null;
	private final Object dataLock = new Object();

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get the configured {@link MBusNetwork}.
	 * 
	 * @return the MBus network
	 */
	public OptionalService<MBusNetwork> getMBusNetwork() {
		return mbusNetwork;
	}

	/**
	 * Set the {@link MBusNetwork} to use.
	 * 
	 * @param mbusNetwork
	 *        the MBus network
	 */
	public void setMBusNetwork(OptionalService<MBusNetwork> mbusNetwork) {
		this.mbusNetwork = mbusNetwork;
	}

	/**
	 * Get the configured address.
	 * 
	 * @return the MBus primary address
	 */
	public int getAddress() {
		return address;
	}

	/**
	 * Set the primary address to use.
	 * 
	 * @param address
	 *        the MBus primary address
	 */
	public void setAddress(int address) {
		this.address = address;
		reconfigureConnection();
	}

	/**
	 * Get the {@link MBusNetwork} from the configured {@code mbusNetwork}
	 * service, or {@literal null} if not available or not configured.
	 * 
	 * @return MBusNetwork
	 */
	protected final MBusNetwork mbusNetwork() {
		return (mbusNetwork == null ? null : mbusNetwork.service());
	}

	/**
	 * Get the configured M-Bus device name.
	 * 
	 * @return the mbus device name
	 */
	public String mBusDeviceName() {
		return address + "@" + mbusNetwork();
	}

	public abstract String getSourceId();

	/**
	 * Reconfigure connection to network
	 */
	private void reconfigureConnection() {
		if ( connection != null ) {
			try {
				connection.close();
			} catch ( IOException e ) {
			}
		}
		MBusNetwork device = mbusNetwork();
		if ( device != null && address > 0 ) {
			connection = device.createConnection(address);
			try {
				connection.open();
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Perform a read from M-Bus {@link MBusNetwork}.
	 * 
	 * <p>
	 * This method attempts to obtain a {@link MBusNetwork} from the configured
	 * {@code modbusNetwork} service, calling {@link MBusNetwork#read()} if one
	 * can be obtained.
	 * </p>
	 * 
	 * @return the result of the read, or {@literal null} if the read is never
	 *         invoked
	 * @throws IOException
	 */
	protected final MBusData performRead() throws IOException {
		MBusData result = null;
		MBusNetwork device = mbusNetwork();
		if ( device != null ) {
			result = device.read(address);
		}
		return result;
	}

	protected MBusData getCurrentSample() {
		MBusData currSample = null;
		synchronized ( dataLock ) {
			// Check latest sample to see if it's current enough to use
			if ( latestData == null
					|| ((System.currentTimeMillis() - latestData.getDataTimestamp()) > sampleCacheMs) ) {
				try {
					currSample = performRead();
				} catch ( IOException e ) {
					Throwable t = e;
					while ( t.getCause() != null ) {
						t = t.getCause();
					}
					log.debug("Error reading from M-Bus device {}", mBusDeviceName(), t);
					log.warn("Communication problem reading source from Modbus device {}: {}",
							getSourceId(), mBusDeviceName(), t.getMessage());
				}
			} else {
				currSample = new MBusData(latestData);
			}
		}
		return currSample;
	}

	/**
	 * Get the sample cache maximum age, in milliseconds.
	 * 
	 * @return the cache milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 * 
	 * @param sampleCacheSecondsMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get setting specifiers for the {@literal unitId} and
	 * {@literal wMBusNetwork.propertyFilters['UID']} properties.
	 * 
	 * @return list of setting specifiers
	 * @since 1.1
	 */
	protected List<SettingSpecifier> getWMBusNetworkSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(
				new BasicTextFieldSettingSpecifier("mBusNetwork.propertyFilters['UID']", "M-Bus Port"));
		results.add(new BasicTextFieldSettingSpecifier("address", ""));
		return results;
	}

}
