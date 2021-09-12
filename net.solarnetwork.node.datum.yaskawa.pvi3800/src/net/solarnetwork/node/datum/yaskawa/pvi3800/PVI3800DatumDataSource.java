/* ==================================================================
 * PVI3800DatumDataSource.java - 18/05/2018 12:23:34 PM
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

package net.solarnetwork.node.datum.yaskawa.pvi3800;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.yaskawa.ecb.BasicDatumPopulatorAction;
import net.solarnetwork.node.hw.yaskawa.ecb.PVI3800Command;
import net.solarnetwork.node.hw.yaskawa.ecb.PVI3800Identification;
import net.solarnetwork.node.hw.yaskawa.ecb.Packet;
import net.solarnetwork.node.hw.yaskawa.ecb.PacketUtils;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.support.SerialDeviceDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.CachedResult;

/**
 * {@link DatumDataSource} for the Solectria PVI-3800 series inverter.
 * 
 * @author matt
 * @version 2.0
 */
public class PVI3800DatumDataSource extends SerialDeviceDatumDataSourceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	private final AtomicReference<CachedResult<AcDcEnergyDatum>> sample;

	private int unitId = 1;
	private long sampleCacheMs = 5000;
	private String sourceId = "PVI-3800";

	/**
	 * Default constructor.
	 */
	public PVI3800DatumDataSource() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public PVI3800DatumDataSource(AtomicReference<CachedResult<AcDcEnergyDatum>> sample) {
		super();
		this.sample = sample;
	}

	private AcDcEnergyDatum getCurrentSample() {
		CachedResult<AcDcEnergyDatum> cachedResult = sample.get();
		AcDcEnergyDatum currSample = null;
		if ( cachedResult == null || !cachedResult.isValid() ) {
			try {
				currSample = performAction(
						new BasicDatumPopulatorAction(unitId, resolvePlaceholders(sourceId)));
				if ( currSample != null ) {
					sample.set(new CachedResult<>(currSample, currSample.getTimestamp().toEpochMilli(),
							sampleCacheMs, TimeUnit.MILLISECONDS));
				}
				if ( log.isTraceEnabled() && currSample != null ) {
					log.trace("Sample: {}", currSample.asSimpleMap());
				}
				log.debug("Read PVI3800 data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from PVI3800 device " + serialNetwork(), e);
			}
		} else {
			currSample = cachedResult.getResult();
		}
		return currSample;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public AcDcEnergyDatum readCurrentDatum() {
		final AcDcEnergyDatum currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		return currSample;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		AcDcEnergyDatum datum = readCurrentDatum();
		// TODO: support phases
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	public CachedResult<AcDcEnergyDatum> getSample() {
		return sample.get();
	}

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) {
		Map<String, Object> result = new LinkedHashMap<>(4);
		try {
			try {
				PVI3800Identification ident = new PVI3800Identification(PacketUtils.sendPacket(conn,
						PVI3800Command.InfoReadIdentification.request(unitId)));
				result.put(INFO_KEY_DEVICE_MODEL, ident.getDescription());
			} catch ( IllegalArgumentException e ) {
				log.warn("Error reading system information from unit {}: {}", unitId, e.getMessage());
			}

			Packet serialNumber = PacketUtils.sendPacket(conn,
					PVI3800Command.InfoReadSerialNumber.request(unitId));
			if ( serialNumber != null ) {
				try {
					result.put(INFO_KEY_DEVICE_SERIAL_NUMBER,
							new String(serialNumber.getBody(), "US-ASCII"));
				} catch ( UnsupportedEncodingException e ) {
					// ignore this
				}
			}

		} catch ( IOException e ) {
			log.warn("Communication error requesting device info for unit {}: {}", unitId,
					e.getMessage());
		}
		return result;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.yaskawa.pvi3800";
	}

	@Override
	public String getDisplayName() {
		return "Solectria PVI-3800 Meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());

		PVI3800DatumDataSource defaults = new PVI3800DatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("serialNetwork.propertyFilters['uid']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));

		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.unitId)));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));

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

	private String getSampleMessage(CachedResult<AcDcEnergyDatum> sample) {
		if ( sample == null ) {
			return "N/A";
		}
		AcDcEnergyDatum datum = sample.getResult();
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(datum.getWatts());
		buf.append(", Wh = ").append(datum.getWattHourReading());
		buf.append("; sampled at ").append(datum.getTimestamp());
		return buf.toString();
	}

	/**
	 * Set the unit ID of the inverter to connect to.
	 * 
	 * <p>
	 * Values less than {@literal 1} or greater than {@code 255} are ignored.
	 * </p>
	 * 
	 * @param unitId
	 *        the unit ID (address), from {@literal 1} to {@literal 255}
	 */
	public void setUnitId(int unitId) {
		if ( unitId < 1 || unitId > 255 ) {
			return;
		}
		this.unitId = unitId;
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

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param sourceId
	 *        the source ID to use; defaults to {@literal PVI-3800}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
