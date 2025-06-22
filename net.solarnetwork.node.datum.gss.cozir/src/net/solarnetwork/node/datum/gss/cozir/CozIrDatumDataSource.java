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

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.hw.gss.co2.CozIrData;
import net.solarnetwork.node.hw.gss.co2.CozIrHelper;
import net.solarnetwork.node.hw.gss.co2.CozIrUtils;
import net.solarnetwork.node.hw.gss.co2.FirmwareVersion;
import net.solarnetwork.node.hw.gss.co2.MeasurementType;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.support.SerialDeviceDatumDataSourceSupport;
import net.solarnetwork.node.job.JobUtils;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicCronExpressionSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * Data source for CozIR series CO2 sensors.
 *
 * @author matt
 * @version 2.1
 */
public class CozIrDatumDataSource extends SerialDeviceDatumDataSourceSupport<AtmosphericDatum>
		implements DatumDataSource, SettingSpecifierProvider, SettingsChangeObserver,
		ServiceLifecycleObserver, SerialConnectionAction<AtmosphericDatum>, Runnable {

	public static final String DEFAULT_SERIAL_PORT = "Serial Port";
	public static final String DEFAULT_SOURCE_ID = "CozIR";
	public static final String DEFAULT_CO2_CALIBRATION_SCHEDULE = "0 0 5 ? * MON";
	public static final String DEFAULT_ALTITUDE = "{altitude:10}";

	/**
	 * The name used to schedule the calibration jobs as.
	 */
	public static final String CALIBRATION_JOB_NAME = "CO2Calibration";

	/**
	 * The group name used to schedule the calibration jobs as.
	 */
	public static final String COZIR_JOB_GROUP = "CozIR";

	private String co2CalibrationSchedule = DEFAULT_CO2_CALIBRATION_SCHEDULE;
	private String altitude = DEFAULT_ALTITUDE;
	private OptionalService<TaskScheduler> scheduler;

	private ScheduledFuture<?> altitudeCalibrationFuture;
	private ScheduledFuture<?> scheduledCalibrationTrigger;
	private final int resolvedAltitude = -1;

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
	public CozIrDatumDataSource(AtomicReference<AtmosphericDatum> sample) {
		super(sample);
		setSourceId(DEFAULT_SOURCE_ID);
	}

	@Override
	public void serviceDidStartup() {
		rescheduleCalibrationJob();
		updateAltitudeCompensation();
	}

	@Override
	public void serviceDidShutdown() {
		unscheduleCalibrationJob();
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		int configuredAltitude = resolveAltitude();
		if ( configuredAltitude != resolvedAltitude ) {
			updateAltitudeCompensation();
		}
		rescheduleCalibrationJob();
	}

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) throws IOException {
		CozIrHelper helper = new CozIrHelper(conn);
		String serialNum = helper.getSerialNumber();
		Map<String, Object> info = new LinkedHashMap<>(3);
		if ( serialNum != null ) {
			info.put(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, serialNum);
		}
		FirmwareVersion fwVers = helper.getFirmwareVersion();
		if ( fwVers != null ) {
			info.put(DataAccessor.INFO_KEY_DEVICE_MANUFACTURE_DATE, fwVers.getDate());
			info.put(DataAccessor.INFO_KEY_DEVICE_MODEL, fwVers.getVersion());
		}
		return info.isEmpty() ? null : info;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.gss.cozir";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("sourceId", DEFAULT_SOURCE_ID));

		results.add(new BasicTextFieldSettingSpecifier("serialNetwork.propertyFilters['uid']",
				DEFAULT_SERIAL_PORT));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));

		results.add(new BasicTextFieldSettingSpecifier("altitude", DEFAULT_ALTITUDE));
		results.add(new BasicCronExpressionSettingSpecifier("co2CalibrationSchedule",
				DEFAULT_CO2_CALIBRATION_SCHEDULE));

		return results;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AtmosphericDatum.class;
	}

	@Override
	public AtmosphericDatum readCurrentDatum() {
		return getCurrentSample();
	}

	private AtmosphericDatum getCurrentSample() {
		AtmosphericDatum sample = getSample();
		if ( sample == null ) {
			try {
				sample = performAction(this);
				if ( sample != null ) {
					setCachedSample(sample);
				}
				if ( log.isTraceEnabled() && sample != null ) {
					log.trace("Sample: {}", sample.asSimpleMap());
				}
				log.debug("Read CozIR data: {}", sample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from CozIR device " + serialNetwork(), e);
			}
		}
		return sample;
	}

	@Override
	public AtmosphericDatum doWithConnection(SerialConnection conn) throws IOException {
		CozIrHelper helper = new CozIrHelper(conn);
		helper.setMeasurementOutput(EnumSet.of(MeasurementType.Co2Filtered, MeasurementType.Humidity,
				MeasurementType.Temperature));
		CozIrData data = helper.getMeasurements();
		SimpleAtmosphericDatum datum = null;
		if ( data != null ) {
			datum = new SimpleAtmosphericDatum(resolvePlaceholders(getSourceId()), Instant.now(),
					new DatumSamples());
			datum.getSamples().putInstantaneousSampleValue("co2", data.getCo2());
			datum.getSamples().putInstantaneousSampleValue(AtmosphericDatum.HUMIDITY_KEY,
					data.getHumidity());
			datum.setTemperature(data.getTemperature());
		}
		return datum;
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

	private String getSampleMessage(AtmosphericDatum datum) {
		if ( datum == null ) {
			return "N/A";
		}
		DatumSamplesOperations ops = datum.asSampleOperations();
		StringBuilder buf = new StringBuilder();
		buf.append("CO2 = ").append(ops.getSampleBigDecimal(Instantaneous, "co2"));
		buf.append(", T = ").append(datum.getTemperature());
		buf.append(", H = ")
				.append(ops.getSampleBigDecimal(Instantaneous, AtmosphericDatum.HUMIDITY_KEY));
		buf.append("; sampled at ").append(formatForLocalDisplay(datum.getTimestamp()));
		return buf.toString();
	}

	private synchronized void rescheduleCalibrationJob() {
		unscheduleCalibrationJob();

		final TaskScheduler s = service(scheduler);
		if ( s == null ) {
			return;
		}

		final String schedule = getCo2CalibrationSchedule();
		final Trigger trigger = JobUtils.triggerForExpression(schedule, TimeUnit.SECONDS, true);
		if ( trigger != null ) {
			this.scheduledCalibrationTrigger = s.schedule(this, trigger);
		}
	}

	private synchronized void unscheduleCalibrationJob() {
		if ( scheduledCalibrationTrigger == null ) {
			return;
		}

		try {
			scheduledCalibrationTrigger.cancel(true);
		} catch ( Exception e ) {
			// ignore
		}
		scheduledCalibrationTrigger = null;

	}

	private int resolveAltitude() {
		int resolvedAltitude;
		try {
			resolvedAltitude = Integer.parseInt(resolvePlaceholders(altitude));
		} catch ( NumberFormatException e ) {
			log.warn(
					"Configured altitude [{}] does not resolve to an integer: falling back to default of 0m",
					altitude);
			resolvedAltitude = 0;
		}
		return resolvedAltitude;
	}

	private class CalibrateAltitudeTask implements Runnable, SerialConnectionAction<Void> {

		@Override
		public void run() {
			try {
				performAction(this);
			} catch ( IOException e ) {
				log.error("Communication error calibrating altitude on CozIR {}: {}", getSourceId(),
						e.toString());
			}
		}

		@Override
		public Void doWithConnection(SerialConnection conn) throws IOException {
			final int resolvedAltitude = resolveAltitude();
			log.info("Calibrating CozIR altitude for {}m", resolvedAltitude);
			CozIrHelper helper = new CozIrHelper(conn);
			helper.setAltitudeCompensation(
					CozIrUtils.altitudeCompensationValueForAltitudeInMeters(resolvedAltitude));
			;
			return null;
		}

	}

	private synchronized void updateAltitudeCompensation() {
		TaskScheduler taskScheduler = getTaskScheduler();
		Runnable task = new CalibrateAltitudeTask();
		if ( altitudeCalibrationFuture != null ) {
			altitudeCalibrationFuture.cancel(true);
			altitudeCalibrationFuture = null;
		}
		if ( taskScheduler != null ) {
			log.info("Scheduling altitude calibration for 15s from now.");
			altitudeCalibrationFuture = taskScheduler.schedule(task,
					Instant.ofEpochMilli(System.currentTimeMillis() + 15000L));
		} else {
			task.run();
		}
	}

	@Override
	public void run() {
		try {
			calibrateAsCo2FreshAirLevel();
		} catch ( Exception e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			log.error("Error calibrating {} Co2 fresh air level: {}", getSourceId(), root.toString());
		}
	}

	/**
	 * Calibrate the CO2 level to "fresh air" level.
	 *
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public void calibrateAsCo2FreshAirLevel() throws IOException {
		log.info("Calibrating CozIR {} CO2 sensor to fresh-air level", getSourceId());
		performAction(new SerialConnectionAction<Void>() {

			@Override
			public Void doWithConnection(SerialConnection conn) throws IOException {
				CozIrHelper helper = new CozIrHelper(conn);
				helper.calibrateAsCo2FreshAirLevel();
				return null;
			}
		});
	}

	/**
	 * Get the CO2 calibration schedule to use.
	 *
	 * @return the cron schedule
	 */
	public String getCo2CalibrationSchedule() {
		return co2CalibrationSchedule;
	}

	/**
	 * Set the CO2 calibration schedule to use.
	 *
	 * @param co2CalibrationSchedule
	 *        the cron schedule to set
	 */
	public void setCo2CalibrationSchedule(String co2CalibrationSchedule) {
		this.co2CalibrationSchedule = co2CalibrationSchedule;
	}

	/**
	 * Set the altitude to use for configuring the CO2 compensation value of the
	 * sensor.
	 *
	 * <p>
	 * This is configured as a string to allow for placeholders via the
	 * configured {@link PlaceholderService}. After resolving any placeholders
	 * an integer value is expected representing the altitude of the sensor, in
	 * meters.
	 * </p>
	 *
	 * @param altitude
	 *        the altitude to set, as a decimal number; placeholders are
	 *        supported
	 */
	public void setAltitude(String altitude) {
		this.altitude = altitude;
	}

	/**
	 * Get the scheduler.
	 *
	 * @return the scheduler
	 */
	public OptionalService<TaskScheduler> getScheduler() {
		return scheduler;
	}

	/**
	 * Set the scheduler.
	 *
	 * @param scheduler
	 *        the scheduler
	 */
	public void setScheduler(OptionalService<TaskScheduler> scheduler) {
		this.scheduler = scheduler;
	}

}
