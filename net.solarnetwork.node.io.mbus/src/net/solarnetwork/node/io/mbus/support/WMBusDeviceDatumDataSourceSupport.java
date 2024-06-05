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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
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
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Supporting abstract class for WMBus datum data sources.
 *
 * @author alex
 * @version 1.3
 */
public abstract class WMBusDeviceDatumDataSourceSupport extends DatumDataSourceSupport
		implements MBusMessageHandler, ServiceLifecycleObserver {

	private OptionalService<WMBusNetwork> wmbusNetwork;
	private MBusSecondaryAddress address;
	private byte[] key;
	private WMBusConnection connection = null;

	// Partial message, awaiting more messages
	private MBusData partialData = null;
	// Latest complete data
	private MBusData latestData = null;
	private final Object dataLock = new Object();

	private boolean active;
	private ScheduledFuture<?> connectFuture;

	/**
	 * Constructor.
	 */
	public WMBusDeviceDatumDataSourceSupport() {
		super();
	}

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
		builder.append('@');
		builder.append(address);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public void serviceDidStartup() {
		active = true;
	}

	@Override
	public synchronized void serviceDidShutdown() {
		active = false;
		if ( connectFuture != null ) {
			try {
				connectFuture.cancel(true);
			} catch ( Exception e ) {
				// ignore
			}
		}
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
	public MBusSecondaryAddress getMBusSecondaryAddress() {
		return address;
	}

	/**
	 * Set the {@link MBusSecondaryAddress} to use.
	 *
	 * @param address
	 *        the MBus secondary address
	 */
	public void setMBusSecondaryAddress(MBusSecondaryAddress address) {
		this.address = address;
		reconfigureConnection();
	}

	/**
	 * Get the configured {@link MBusSecondaryAddress} as a hex-encoded string.
	 *
	 * @return the MBus secondary address as a hex-encoded string
	 * @since 1.1
	 */
	public String getSecondaryAddress() {
		MBusSecondaryAddress addr = getMBusSecondaryAddress();
		return (addr != null ? addr.toString() : null);
	}

	/**
	 * Set the hex-encoded {@link MBusSecondaryAddress} to use.
	 *
	 * @param address
	 *        the MBus secondary address as a hex-encoded string
	 * @since 1.1
	 */
	public void setSecondaryAddress(String address) {
		setMBusSecondaryAddress(
				address == null || address.isEmpty() ? null : new MBusSecondaryAddress(address));
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
	 * Reconfigure connection to network.
	 */
	private synchronized void reconfigureConnection() {
		if ( connectFuture != null && !connectFuture.isDone() ) {
			return;
		}
		if ( !active || address == null || key == null ) {
			return;
		}
		ConnectTask task = new ConnectTask();
		if ( getTaskScheduler() != null ) {
			connectFuture = getTaskScheduler().schedule(task, Date.from(Instant.now().plusSeconds(1)));
		} else {
			task.run();
		}
	}

	private synchronized void reconnect() {
		if ( connection != null ) {
			log.info("Closing wireless M-Bus connection {} for {}", connection, address);
			try {
				connection.close();
				connection = null;
			} catch ( IOException e ) {
				// ignore
			}
		}
		if ( !active ) {
			return;
		}
		WMBusNetwork device = service(wmbusNetwork);
		if ( device != null && address != null && key != null ) {
			WMBusConnection conn = device.createConnection(address, key);
			try {
				log.info("Opening wireless M-Bus connection {} for {}", conn, address);
				conn.open(this);
				connection = conn;
			} catch ( IOException e ) {
				log.error("Error opening wireless M-Bus connection {} for {}: {}", conn, address,
						e.toString());
			}
		} else if ( device == null && address != null && key != null ) {
			log.warn("No wireless M-Bus network available for {}", address);
		}
	}

	private final class ConnectTask implements Runnable {

		@Override
		public void run() {
			try {
				reconnect();
			} finally {
				synchronized ( WMBusDeviceDatumDataSourceSupport.this ) {
					if ( connection == null ) {
						// try again
						if ( active && getTaskScheduler() != null ) {
							log.info("Will try opening wireless M-Bus connection for {} in 10s",
									address);
							connectFuture = getTaskScheduler().schedule(this,
									Date.from(Instant.now().plusSeconds(10)));
						}
					} else {
						connectFuture = null;
					}
				}
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

	/**
	 * Get the current sample.
	 *
	 * @return the current sample, or {@literal null} if no data is available
	 */
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
	 * {@literal wMBusNetwork.propertyFilters['uid']} properties.
	 *
	 * @return list of setting specifiers
	 * @since 1.1
	 */
	protected List<SettingSpecifier> getWMBusNetworkSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(new BasicTextFieldSettingSpecifier("wMBusNetwork.propertyFilters['uid']",
				"M-Bus (Wireless) Port"));
		results.add(new BasicTextFieldSettingSpecifier("secondaryAddress", ""));
		results.add(new BasicTextFieldSettingSpecifier("key", "", true));
		return results;
	}

}
