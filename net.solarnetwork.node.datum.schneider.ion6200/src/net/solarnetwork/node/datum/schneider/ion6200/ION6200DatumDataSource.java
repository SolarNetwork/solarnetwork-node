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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.schneider.meter.ION6200Data;
import net.solarnetwork.node.hw.schneider.meter.ION6200DataAccessor;
import net.solarnetwork.node.hw.schneider.meter.ION6200VoltsMode;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;

/**
 * {@link DatumDataSource} for the ION6200 series meter.
 * 
 * @author matt
 * @version 1.0
 */
public class ION6200DatumDataSource extends ModbusDeviceDatumDataSourceSupport
		implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	private final ION6200Data sample;

	private long sampleCacheMs = 5000;
	private String sourceId = "ION6200";
	private boolean backwards = false;

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
						"Communication problem reading from ION6200 device " + modbusNetwork(), e);
			}
		} else {
			currSample = getSample().getSnapshot();
		}
		return currSample;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final ION6200Data currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		ION6200Datum d = new ION6200Datum(currSample, ACPhase.Total, this.backwards);
		d.setSourceId(this.sourceId);
		if ( currSample.getDataTimestamp() >= start ) {
			// we read from the device
			postDatumCapturedEvent(d);
		}
		return d;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public Collection<GeneralNodeACEnergyDatum> readMultipleDatum() {
		GeneralNodeACEnergyDatum datum = readCurrentDatum();
		// TODO: support phases
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	public ION6200Data getSample() {
		return sample;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		sample.readConfigurationData(conn);
		ION6200DataAccessor data = (ION6200DataAccessor) sample.copy();
		Map<String, Object> result = new LinkedHashMap<>(4);
		Integer type = data.getDeviceType();
		if ( type != null ) {
			Integer firmwareVersion = data.getFirmwareRevision();
			if ( firmwareVersion != null ) {
				result.put(INFO_KEY_DEVICE_MODEL,
						String.format("%d (firmware %d)", type, firmwareVersion));
			} else {
				result.put(INFO_KEY_DEVICE_MODEL, type);
			}
		}
		ION6200VoltsMode wiringMode = data.getVoltsMode();
		if ( wiringMode != null ) {
			result.put("Wiring Mode", wiringMode.getDescription());
		}
		Long l = data.getSerialNumber();
		if ( l != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, l);
		}
		return result;
	}

	/**
	 * Test if the sample data has expired.
	 * 
	 * @return {@literal true} if the sample data has expired
	 */
	protected boolean isCachedSampleExpired() {
		final long lastReadDiff = System.currentTimeMillis() - sample.getDataTimestamp();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
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

		ION6200DatumDataSource defaults = new ION6200DatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));
		results.add(new BasicToggleSettingSpecifier("megawattModel", defaults.sample.isMegawattModel()));
		results.add(new BasicToggleSettingSpecifier("backwards", defaults.backwards));

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
		if ( data.getDataTimestamp() < 1 ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(sample.getActivePower());
		buf.append(", VAR = ").append(sample.getReactivePower());
		buf.append(", Wh rec = ").append(sample.getActiveEnergyReceived());
		buf.append(", Wh del = ").append(sample.getActiveEnergyDelivered());
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(sample.getDataTimestamp())));
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
	 * @param sampleCacheSecondsMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param soruceId
	 *        the source ID to use; defaults to {@literal modbus}
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
	 * Toggle the "backwards" current direction flag.
	 * 
	 * @param backwards
	 *        {@literal true} to swap energy delivered and received values
	 */
	public void setBackwards(boolean backwards) {
		this.backwards = backwards;
	}

}
