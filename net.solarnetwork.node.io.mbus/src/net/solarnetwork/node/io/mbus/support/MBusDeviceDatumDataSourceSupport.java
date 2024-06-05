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

import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.mbus.MBusData;
import net.solarnetwork.node.io.mbus.MBusNetwork;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Abstract base class for MBus based datum data sources.
 *
 * @author alex
 * @version 2.1
 */
public abstract class MBusDeviceDatumDataSourceSupport extends DatumDataSourceSupport {

	private static final long DEFAULT_SAMPLE_CACHE_MS = 5000;

	private OptionalService<MBusNetwork> mbusNetwork;
	private int address;
	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;

	// Latest complete data
	private MBusData latestData = null;
	private final Object dataLock = new Object();

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 */
	public MBusDeviceDatumDataSourceSupport() {
		super();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append("{");
		MBusNetwork network = service(mbusNetwork);
		if ( network != null ) {
			builder.append(network.toString());
		} else {
			builder.append(Integer.toHexString(hashCode()));
		}
		builder.append("}");
		return builder.toString();
	}

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

	/**
	 * Get the data source ID.
	 *
	 * @return the source ID
	 */
	public abstract String getSourceId();

	/**
	 * Perform a read from M-Bus {@link MBusNetwork}.
	 *
	 * <p>
	 * This method attempts to obtain a {@link MBusNetwork} from the configured
	 * service, calling {@link MBusNetwork#read(int)} if one can be obtained.
	 * </p>
	 *
	 * @return the result of the read, or {@literal null} if the read is never
	 *         invoked
	 * @throws IOException
	 *         if any communication error occurs
	 */
	protected final MBusData performRead() throws IOException {
		MBusData result = null;
		MBusNetwork device = mbusNetwork();
		if ( device != null ) {
			result = device.read(address);
		}
		return result;
	}

	/**
	 * Get the latest available sample.
	 *
	 * <p>
	 * This method will <b>not</b> refresh the data.
	 * </p>
	 *
	 * @return the latest sample, or {@literal null}
	 */
	protected final MBusData getLatestSample() {
		MBusData currSample = null;
		synchronized ( dataLock ) {
			if ( latestData != null ) {
				currSample = new MBusData(latestData);
			}
		}
		return currSample;
	}

	/**
	 * Get the current sample, reading from the device if necessary.
	 *
	 * @return the current sample, or {@literal null} if unable to read from
	 *         device
	 */
	protected final MBusData getCurrentSample() {
		MBusData currSample = null;
		synchronized ( dataLock ) {
			// Check latest sample to see if it's current enough to use
			if ( latestData == null || latestData.getDataTimestamp() == null || ((latestData
					.getDataTimestamp().until(Instant.now(), ChronoUnit.MILLIS)) > sampleCacheMs) ) {
				try {
					latestData = performRead();
					if ( latestData != null ) {
						currSample = new MBusData(latestData);
					}
				} catch ( IOException e ) {
					Throwable t = e;
					while ( t.getCause() != null ) {
						t = t.getCause();
					}
					log.debug("Error reading from M-Bus device {}", mBusDeviceName(), t);
					log.warn("Communication problem reading source {} from M-Bus device {}: {}",
							getSourceId(), mBusDeviceName(), t.getMessage());
				}
			} else {
				currSample = new MBusData(latestData);
			}
		}
		return currSample;
	}

	/**
	 * Get setting specifiers for the {@literal unitId} and
	 * {@literal mBusNetwork.propertyFilters['uid']} properties.
	 *
	 * @return list of setting specifiers
	 * @since 1.1
	 */
	protected List<SettingSpecifier> getMBusNetworkSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(
				new BasicTextFieldSettingSpecifier("mBusNetwork.propertyFilters['uid']", "M-Bus Port"));
		results.add(new BasicTextFieldSettingSpecifier("address", ""));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		return results;
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
	 * @param sampleCacheMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

}
