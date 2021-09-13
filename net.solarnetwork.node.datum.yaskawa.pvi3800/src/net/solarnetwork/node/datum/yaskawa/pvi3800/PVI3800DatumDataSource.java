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

/**
 * {@link DatumDataSource} for the Solectria PVI-3800 series inverter.
 * 
 * @author matt
 * @version 2.0
 */
public class PVI3800DatumDataSource extends SerialDeviceDatumDataSourceSupport<AcDcEnergyDatum>
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/** The {@code unitId} property default value. */
	public static final int DEFAULT_UNIT_ID = 1;

	/** The {@code sourceId} property default value. */
	public static final String DEFAULT_SOURCE_ID = "PVI-3800";

	private int unitId = DEFAULT_UNIT_ID;

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
	public PVI3800DatumDataSource(AtomicReference<AcDcEnergyDatum> sample) {
		super(sample);
		setDisplayName("Solectria PVI-3800 Meter");
		setSourceId(DEFAULT_SOURCE_ID);
	}

	private AcDcEnergyDatum getCurrentSample() {
		AcDcEnergyDatum sample = getSample();
		if ( sample == null ) {
			try {
				sample = performAction(
						new BasicDatumPopulatorAction(unitId, resolvePlaceholders(getSourceId())));
				if ( sample != null ) {
					setCachedSample(sample);
				}
				if ( log.isTraceEnabled() && sample != null ) {
					log.trace("Sample: {}", sample.asSimpleMap());
				}
				log.debug("Read PVI3800 data: {}", sample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from PVI3800 device " + serialNetwork(), e);
			}
		}
		return sample;
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
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(12);
		result.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		result.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		result.addAll(getIdentifiableSettingSpecifiers());
		result.add(new BasicTextFieldSettingSpecifier("sourceId", DEFAULT_SOURCE_ID));
		result.add(new BasicTextFieldSettingSpecifier("serialNetwork.propertyFilters['uid']",
				"Serial Port"));
		result.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(DEFAULT_UNIT_ID)));

		result.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		return result;
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

	private String getSampleMessage(AcDcEnergyDatum datum) {
		if ( datum == null ) {
			return "N/A";
		}
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

}
