/* ==================================================================
 * CozIrDatumDataSource.java - 28/08/2020 11:03:50 AM
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

package net.solarnetwork.node.datum.gss.cozir;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.hw.gss.co2.CozIrData;
import net.solarnetwork.node.hw.gss.co2.CozIrHelper;
import net.solarnetwork.node.hw.gss.co2.FirmwareVersion;
import net.solarnetwork.node.hw.gss.co2.MeasurementType;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.support.SerialDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.CachedResult;

/**
 * Data source for CozIR series CO2 sensors.
 * 
 * @author matt
 * @version 1.0
 */
public class CozIrDatumDataSource extends SerialDeviceDatumDataSourceSupport
		implements DatumDataSource<GeneralNodeDatum>, SettingSpecifierProvider {

	private final AtomicReference<CachedResult<GeneralNodeDatum>> sample;

	private long sampleCacheMs = 5000;
	private String sourceId = "CozIR";

	/**
	 * Default constructor.
	 */
	public CozIrDatumDataSource() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public CozIrDatumDataSource(AtomicReference<CachedResult<GeneralNodeDatum>> sample) {
		super();
		this.sample = sample;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) throws IOException {
		CozIrHelper helper = new CozIrHelper(conn);
		String serialNum = helper.getSerialNumber();
		Map<String, Object> info = new LinkedHashMap<>(3);
		if ( serialNum != null ) {
			info.put(INFO_KEY_DEVICE_SERIAL_NUMBER, serialNum);
		}
		FirmwareVersion fwVers = helper.getFirmwareVersion();
		if ( fwVers != null ) {
			info.put(INFO_KEY_DEVICE_MANUFACTURE_DATE, fwVers.getDate());
			info.put(INFO_KEY_DEVICE_MODEL, fwVers.getVersion());
		}
		return info.isEmpty() ? null : info;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.gss.cozir";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());

		CozIrDatumDataSource defaults = new CozIrDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("serialNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));

		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));

		return results;
	}

	@Override
	public Class<? extends GeneralNodeDatum> getDatumType() {
		return GeneralNodeDatum.class;
	}

	@Override
	public GeneralNodeDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final GeneralNodeDatum currSample = getCurrentSample();
		if ( currSample == null ) {
			return null;
		}
		if ( currSample.getCreated().getTime() >= start ) {
			// we read from the device
			postDatumCapturedEvent(currSample);
		}
		return currSample;
	}

	public CachedResult<GeneralNodeDatum> getSample() {
		return sample.get();
	}

	private GeneralNodeDatum getCurrentSample() {
		CachedResult<GeneralNodeDatum> cachedResult = sample.get();
		GeneralNodeDatum currSample = null;
		if ( cachedResult == null || !cachedResult.isValid() ) {
			try {
				currSample = performAction(new SerialConnectionAction<GeneralNodeDatum>() {

					@Override
					public GeneralNodeDatum doWithConnection(SerialConnection conn) throws IOException {
						CozIrHelper helper = new CozIrHelper(conn);
						helper.setMeasurementOutput(EnumSet.of(MeasurementType.Co2Filtered,
								MeasurementType.Humidity, MeasurementType.Temperature));
						CozIrData data = helper.getMeasurements();
						GeneralNodeDatum datum = null;
						if ( data != null ) {
							datum = new GeneralNodeDatum();
							datum.putInstantaneousSampleValue("co2", data.getCo2());
							datum.putInstantaneousSampleValue(AtmosphericDatum.HUMIDITY_KEY,
									data.getHumidity());
							datum.putInstantaneousSampleValue(AtmosphericDatum.TEMPERATURE_KEY,
									data.getTemperature());
						}
						return datum;
					}
				});
				if ( currSample != null ) {
					currSample.setSourceId(resolvePlaceholders(sourceId));
					sample.set(new CachedResult<GeneralNodeDatum>(currSample,
							currSample.getCreated().getTime(), sampleCacheMs, TimeUnit.MILLISECONDS));
				}
				if ( log.isTraceEnabled() && currSample != null ) {
					log.trace("Sample: {}", currSample.asSimpleMap());
				}
				log.debug("Read CozIR data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from CozIR device " + serialNetwork(), e);
			}
		} else {
			currSample = cachedResult.getResult();
		}
		return currSample;
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

	private String getSampleMessage(CachedResult<GeneralNodeDatum> sample) {
		if ( sample == null ) {
			return "N/A";
		}
		GeneralNodeDatum data = sample.getResult();
		StringBuilder buf = new StringBuilder();
		buf.append("co2 = ").append(data.getInstantaneousSampleBigDecimal("co2"));
		buf.append(", T = ")
				.append(data.getInstantaneousSampleBigDecimal(AtmosphericDatum.TEMPERATURE_KEY));
		buf.append(", H = ")
				.append(data.getInstantaneousSampleBigDecimal(AtmosphericDatum.HUMIDITY_KEY));
		buf.append("; sampled at ")
				.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
						.format(data.getCreated().toInstant().atZone(ZoneId.systemDefault())));
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
	 * Set the source ID to use for returned datum.
	 * 
	 * @param sourceId
	 *        the source ID to use; defaults to {@literal CozIR}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
