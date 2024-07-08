/* ==================================================================
 * ION6200DatumDataSource.java - 15/05/2018 7:02:47 AM
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

package net.solarnetwork.node.datum.schneider.ion6200;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.schneider.meter.ION6200Data;
import net.solarnetwork.node.hw.schneider.meter.ION6200DataAccessor;
import net.solarnetwork.node.hw.schneider.meter.ION6200VoltsMode;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.support.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * {@link DatumDataSource} for the ION6200 series meter.
 *
 * @author matt
 * @version 1.7
 */
public class ION6200DatumDataSource extends ModbusDeviceDatumDataSourceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/**
	 * The {@code sampleCacheMs} property default value.
	 *
	 * @since 1.6
	 */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5000;

	private final ION6200Data sample;

	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;
	private String sourceId;
	private boolean backwards;
	private boolean includePhaseMeasurements;

	/**
	 * Default constructor.
	 */
	public ION6200DatumDataSource() {
		this(new ION6200Data());
	}

	/**
	 * Construct with a specific sample data instance.
	 *
	 * @param sample
	 *        the sample data to use
	 */
	public ION6200DatumDataSource(ION6200Data sample) {
		super();
		this.sample = sample;
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	private ION6200Data getCurrentSample() {
		ION6200Data currSample = null;
		if ( isCachedSampleExpired() ) {
			try {
				currSample = performAction(new ModbusConnectionAction<ION6200Data>() {

					@Override
					public ION6200Data doWithConnection(ModbusConnection connection) throws IOException {
						getSample().readMeterData(connection);
						return getSample().getSnapshot();
					}

				});
				if ( log.isTraceEnabled() && currSample != null ) {
					log.trace(currSample.dataDebugString());
				}
				log.debug("Read ION6200 data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from ION6200 device " + modbusDeviceName(), e);
			}
		} else {
			currSample = getSample().getSnapshot();
		}
		return currSample;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcEnergyDatum.class;
	}

	@Override
	public AcEnergyDatum readCurrentDatum() {
		final ION6200Data currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		ION6200Datum d = new ION6200Datum(currSample, resolvePlaceholders(sourceId), AcPhase.Total,
				this.backwards);
		if ( this.includePhaseMeasurements ) {
			d.populatePhaseMeasurementProperties(currSample);
		}
		return d;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AcEnergyDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		AcEnergyDatum datum = readCurrentDatum();
		// TODO: support phases
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	/**
	 * Get the sample.
	 *
	 * @return the sample
	 */
	public ION6200Data getSample() {
		return sample;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) throws IOException {
		sample.readConfigurationData(conn);
		ION6200DataAccessor data = (ION6200DataAccessor) sample.copy();
		Map<String, Object> result = new LinkedHashMap<>(4);
		Integer type = data.getDeviceType();
		if ( type != null ) {
			Integer firmwareVersion = data.getFirmwareRevision();
			if ( firmwareVersion != null ) {
				result.put(DataAccessor.INFO_KEY_DEVICE_MODEL,
						String.format("%d (firmware %d)", type, firmwareVersion));
			} else {
				result.put(DataAccessor.INFO_KEY_DEVICE_MODEL, type);
			}
		}
		ION6200VoltsMode wiringMode = data.getVoltsMode();
		if ( wiringMode != null ) {
			result.put("Wiring Mode", wiringMode.getDescription());
		}
		Long l = data.getSerialNumber();
		if ( l != null ) {
			result.put(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, l);
		}
		return result;
	}

	/**
	 * Test if the sample data has expired.
	 *
	 * @return {@literal true} if the sample data has expired
	 */
	protected boolean isCachedSampleExpired() {
		final Instant ts = sample.getDataTimestamp();
		if ( ts == null ) {
			return true;
		}
		final long lastReadDiff = sample.getDataTimestamp().until(Instant.now(), ChronoUnit.MILLIS);
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.schneider.ion6200";
	}

	@Override
	public String getDisplayName() {
		return "Schneider ION6200 Meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(sample), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicToggleSettingSpecifier("megawattModel", Boolean.FALSE));
		results.add(new BasicToggleSettingSpecifier("backwards", Boolean.FALSE));
		results.add(new BasicToggleSettingSpecifier("includePhaseMeasurements", Boolean.FALSE));

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

	private String getSampleMessage(ION6200Data data) {
		if ( data.getDataTimestamp() == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(sample.getActivePower());
		buf.append(", VAR = ").append(sample.getReactivePower());
		buf.append(", Wh rec = ").append(sample.getActiveEnergyReceived());
		buf.append(", Wh del = ").append(sample.getActiveEnergyDelivered());
		buf.append("; sampled at ").append(sample.getDataTimestamp());
		return buf.toString();
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
	 * Get the source ID to use for returned datum.
	 *
	 * @return the source ID
	 * @since 1.6
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to use for returned datum.
	 *
	 * @param sourceId
	 *        the source ID to use
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Toggle the "Megawatt" model mode.
	 *
	 * @param megawattModel
	 *        {@literal true} to interpret the data for a Megawatt model device,
	 *        {@literal false} for other models
	 */
	public void setMegawattModel(boolean megawattModel) {
		this.sample.setMegawattModel(megawattModel);
	}

	/**
	 * Get the "backwards" current direction flag.
	 *
	 * @return {@literal true} to swap energy delivered and received values
	 * @since 1.6
	 */
	public boolean isBackwards() {
		return backwards;
	}

	/**
	 * Toggle the "backwards" current direction flag.
	 *
	 * @param backwards
	 *        {@literal true} to swap energy delivered and received values
	 */
	public void setBackwards(boolean backwards) {
		this.backwards = backwards;
	}

	/**
	 * Get the inclusion toggle of phase measurement properties in collected
	 * datum.
	 *
	 * @return {@literal true} to collect phase measurements
	 * @since 1.6
	 */
	public boolean isIncludePhaseMeasurements() {
		return includePhaseMeasurements;
	}

	/**
	 * Toggle the inclusion of phase measurement properties in collected datum.
	 *
	 * @param includePhaseMeasurements
	 *        {@literal true} to collect phase measurements
	 * @since 1.1
	 */
	public void setIncludePhaseMeasurements(boolean includePhaseMeasurements) {
		this.includePhaseMeasurements = includePhaseMeasurements;
	}
}
