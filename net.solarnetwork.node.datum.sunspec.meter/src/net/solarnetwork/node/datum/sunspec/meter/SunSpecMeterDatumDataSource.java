/* ==================================================================
 * SunSpecMeterDatumDataSource.java - 23/05/2018 6:45:54 AM
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

package net.solarnetwork.node.datum.sunspec.meter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelDataFactory;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;

/**
 * {@link DatumDataSource} for a SunSpec compatible power meter.
 * 
 * @author matt
 * @version 1.1
 */
public class SunSpecMeterDatumDataSource extends ModbusDeviceDatumDataSourceSupport
		implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	private final AtomicReference<ModelData> sample;

	private long sampleCacheMs = 5000;
	private String sourceId = "SunSpec-Meter";
	private boolean backwards = false;

	/**
	 * Default constructor.
	 */
	public SunSpecMeterDatumDataSource() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public SunSpecMeterDatumDataSource(AtomicReference<ModelData> sample) {
		super();
		this.sample = sample;
	}

	private ModelData getCurrentSample() {
		ModelData currSample = getSample();
		if ( isCachedSampleExpired(currSample) ) {
			try {
				final ModelData data = currSample;
				currSample = performAction(new ModbusConnectionAction<ModelData>() {

					@Override
					public ModelData doWithConnection(ModbusConnection connection) throws IOException {
						if ( data == null ) {
							ModelData result = ModelDataFactory.getInstance().getModelData(connection);
							if ( result != null ) {
								sample.set(result);
							}
							return result;
						}
						data.readModelData(connection);
						return data;
					}

				});
				if ( log.isTraceEnabled() && currSample != null ) {
					log.trace(currSample.dataDebugString());
				}
				log.debug("Read SunSpec meter data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from SunSpec meter device " + modbusNetwork(), e);
			}
		}
		return (currSample != null ? currSample.getSnapshot() : null);
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final ModelData currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		MeterModelAccessor data = currSample.findTypedModel(MeterModelAccessor.class);
		SunSpecMeterDatum d = new SunSpecMeterDatum(data, ACPhase.Total, this.backwards);
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

	public ModelData getSample() {
		return sample.get();
	}

	public ModelData getSampleSnapshot() {
		ModelData data = getSample();
		return (data != null ? data.getSnapshot() : null);
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection connection) {
		CommonModelAccessor data = ModelDataFactory.getInstance().getModelData(connection);
		if ( data == null ) {
			return null;
		}
		Map<String, Object> result = new LinkedHashMap<>(4);
		String manufacturer = data.getManufacturer();
		if ( manufacturer != null ) {
			result.put(INFO_KEY_DEVICE_MANUFACTURER, manufacturer);
		}
		String model = data.getModelName();
		if ( model != null ) {
			String version = data.getVersion();
			if ( version != null ) {
				result.put(INFO_KEY_DEVICE_MODEL, String.format("%s (version %s)", model, version));
			} else {
				result.put(INFO_KEY_DEVICE_MODEL, model.toString());
			}
		}
		String sn = data.getSerialNumber();
		if ( sn != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, sn);
		}
		return result;
	}

	/**
	 * Test if the sample data has expired.
	 * 
	 * @return {@literal true} if the sample data has expired
	 */
	protected boolean isCachedSampleExpired(ModelData data) {
		if ( data == null ) {
			return true;
		}
		final long lastReadDiff = System.currentTimeMillis() - data.getDataTimestamp();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.sunspec.meter";
	}

	@Override
	public String getDisplayName() {
		return "SunSpec Power Meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		SunSpecMeterDatumDataSource defaults = new SunSpecMeterDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));
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

	private String getSampleMessage() {
		return getSampleMessage(getSampleSnapshot());
	}

	private String getSampleMessage(ModelData sample) {
		if ( sample == null || sample.getDataTimestamp() < 1 ) {
			return "N/A";
		}
		MeterModelAccessor data;
		try {
			data = sample.getTypedModel();
			if ( data == null ) {
				return "N/A";
			}
		} catch ( RuntimeException e ) {
			return "Unexpected model: " + sample.getModel();
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(data.getActivePower());
		buf.append(", freq = ").append(data.getFrequency());
		buf.append(", Wh imp = ").append(data.getActiveEnergyImported());
		buf.append(", Wh exp = ").append(data.getActiveEnergyExported());
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(data.getDataTimestamp())));
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
	 * Toggle the "backwards" current direction flag.
	 * 
	 * @param backwards
	 *        {@literal true} to swap energy delivered and received values
	 */
	public void setBackwards(boolean backwards) {
		this.backwards = backwards;
	}

}
