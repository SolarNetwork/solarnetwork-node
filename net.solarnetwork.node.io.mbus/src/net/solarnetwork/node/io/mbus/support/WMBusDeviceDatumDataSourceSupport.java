/* ==================================================================
 * WMBusDatumDataSource.java - 06/07/2020 15:17:51 pm
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

import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import net.solarnetwork.node.io.mbus.MBusData;
import net.solarnetwork.node.io.mbus.MBusMessage;
import net.solarnetwork.node.io.mbus.MBusMessageHandler;
import net.solarnetwork.node.io.mbus.MBusSecondaryAddress;
import net.solarnetwork.node.io.mbus.WMBusConnection;
import net.solarnetwork.node.io.mbus.WMBusNetwork;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

public abstract class WMBusDeviceDatumDataSourceSupport extends DatumDataSourceSupport
		implements MBusMessageHandler {

	private OptionalService<WMBusNetwork> wmbusNetwork;
	private MBusSecondaryAddress address;
	private byte[] key;
	private WMBusConnection connection = null;

	// Partial message, awaiting more messages
	private MBusData partialData = null;
	// Latest complete data
	private MBusData latestData = null;
	private final Object dataLock = new Object();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append("{");
		WMBusNetwork network = service(wmbusNetwork);
		if ( network != null ) {
			builder.append(network.toString());
		} else {
			builder.append(Integer.toHexString(hashCode()));
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the configured {@link WMBusNetwork}.
	 * 
	 * @return the WMBus network
	 */
	public OptionalService<WMBusNetwork> getWMBusNetwork() {
		return wmbusNetwork;
	}

	/**
	 * Set the {@link WMBusNetwork} to use.
	 * 
	 * @param wmbusNetwork
	 *        the WMBus network
	 */
	public void setWMBusNetwork(OptionalService<WMBusNetwork> wmbusNetwork) {
		this.wmbusNetwork = wmbusNetwork;
	}

	/**
	 * Get the configured {@link MBusSecondaryAddress}.
	 * 
	 * @return the MBus secondary address
	 */
	public MBusSecondaryAddress getSecondaryAddress() {
		return address;
	}

	/**
	 * Set the {@link MBusSecondaryAddress} to use.
	 * 
	 * @param address
	 *        the MBus secondary address
	 */
	public void setSecondaryAddress(MBusSecondaryAddress address) {
		this.address = address;
		reconfigureConnection();
	}

	/**
	 * Get the configured key.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return Hex.encodeHexString(key);
	}

	/**
	 * Set the {@link MBusSecondaryAddress} to use.
	 * 
	 * @param key
	 *        the encryption key, encoded in hex
	 */
	public void setKey(String key) {
		try {
			this.key = Hex.decodeHex(key);
			reconfigureConnection();
		} catch ( DecoderException e ) {
			// ignore
		}
	}

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
		WMBusNetwork device = (wmbusNetwork == null ? null : wmbusNetwork.service());
		if ( device != null && address != null && key != null ) {
			connection = device.createConnection(address, key);
			try {
				connection.open(this);
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleMessage(MBusMessage message) {
		synchronized ( dataLock ) {
			if ( message.moreRecordsFollow ) {
				if ( partialData == null ) {
					partialData = new MBusData(message);
				} else {
					partialData.addRecordsFrom(message);
				}
			} else {
				if ( partialData == null ) {
					latestData = new MBusData(message);
				} else {
					latestData = partialData;
					latestData.addRecordsFrom(message);
					partialData = null;
				}
			}
		}
	}

	protected MBusData getCurrentSample() {
		synchronized ( dataLock ) {
			if ( latestData == null ) {
				return null;
			}
			return new MBusData(latestData);
		}
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
		results.add(new BasicTextFieldSettingSpecifier("wMBusNetwork.propertyFilters['UID']",
				"M-Bus (Wireless) Port"));
		results.add(new BasicTextFieldSettingSpecifier("secondaryAddress", ""));
		results.add(new BasicTextFieldSettingSpecifier("key", "", true));
		return results;
	}

}
