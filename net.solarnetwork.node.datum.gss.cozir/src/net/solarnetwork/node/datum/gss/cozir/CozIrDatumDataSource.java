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

import static net.solarnetwork.util.OptionalService.service;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.PlaceholderService;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.hw.gss.co2.CozIrData;
import net.solarnetwork.node.hw.gss.co2.CozIrHelper;
import net.solarnetwork.node.hw.gss.co2.CozIrUtils;
import net.solarnetwork.node.hw.gss.co2.FirmwareVersion;
import net.solarnetwork.node.hw.gss.co2.MeasurementType;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.support.SerialDeviceDatumDataSourceSupport;
import net.solarnetwork.node.job.JobUtils;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicCronExpressionSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.support.ServiceLifecycleObserver;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.OptionalService;

/**
 * Data source for CozIR series CO2 sensors.
 * 
 * @author matt
 * @version 1.0
 */
public class CozIrDatumDataSource extends SerialDeviceDatumDataSourceSupport
		implements DatumDataSource<GeneralNodeDatum>, SettingSpecifierProvider, SettingsChangeObserver,
		ServiceLifecycleObserver, CozIrService {

	public static final String DEFAULT_SERIAL_PORT = "Serial Port";
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5000L;
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

	/**
	 * The key used for the calibration job.
	 */
	public static final JobKey CALIBRATION_JOB_KEY = new JobKey(CALIBRATION_JOB_NAME, COZIR_JOB_GROUP);

	private final AtomicReference<CachedResult<GeneralNodeDatum>> sample;

	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;
	private String sourceId = DEFAULT_SOURCE_ID;
	private String co2CalibrationSchedule = DEFAULT_CO2_CALIBRATION_SCHEDULE;
	private String altitude = DEFAULT_ALTITUDE;
	private OptionalService<Scheduler> scheduler;

	private ScheduledFuture<?> altitudeCalibrationFuture;
	private Trigger scheduledCalibrationTrigger;
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
	public CozIrDatumDataSource(AtomicReference<CachedResult<GeneralNodeDatum>> sample) {
		super();
		this.sample = sample;
	}

	@Override
	public void serviceDidStartup() {
		rescheduleCalibrationJob();
		updateAltitudeCompensation();
	}

	@Override
	public void serviceDidShutdown() {
		unscheduleCalibrationJob(scheduledCalibrationTrigger);
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

		results.add(new BasicTextFieldSettingSpecifier("serialNetwork.propertyFilters['UID']",
				DEFAULT_SERIAL_PORT));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));

		results.add(new BasicTextFieldSettingSpecifier("sourceId", DEFAULT_SOURCE_ID));
		results.add(new BasicTextFieldSettingSpecifier("altitude", DEFAULT_ALTITUDE));
		results.add(new BasicCronExpressionSettingSpecifier("co2CalibrationSchedule",
				DEFAULT_CO2_CALIBRATION_SCHEDULE));

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
							datum.setCreated(new Date());
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
		buf.append("CO2 = ").append(data.getInstantaneousSampleBigDecimal("co2"));
		buf.append(", T = ")
				.append(data.getInstantaneousSampleBigDecimal(AtmosphericDatum.TEMPERATURE_KEY));
		buf.append(", H = ")
				.append(data.getInstantaneousSampleBigDecimal(AtmosphericDatum.HUMIDITY_KEY));
		buf.append("; sampled at ")
				.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
						.format(data.getCreated().toInstant().atZone(ZoneId.systemDefault())));
		return buf.toString();
	}

	private synchronized void rescheduleCalibrationJob() {
		unscheduleCalibrationJob(scheduledCalibrationTrigger);
		scheduledCalibrationTrigger = null;

		final Scheduler s = service(scheduler);
		if ( s == null ) {
			return;
		}

		final String schedule = getCo2CalibrationSchedule();
		final String jobDesc = calibrationJobDescription(sourceId);
		final TriggerKey triggerKey = triggerKey(sourceId);
		final JobDataMap props = new JobDataMap();
		props.put("service", this);
		Trigger trigger = JobUtils.scheduleJob(s, CozIrCo2CalibrationJob.class, CALIBRATION_JOB_KEY,
				jobDesc, schedule, triggerKey, props);
		this.scheduledCalibrationTrigger = trigger;
	}

	private void unscheduleCalibrationJob(Trigger trigger) {
		if ( trigger == null ) {
			return;
		}

		Scheduler s = service(scheduler);
		if ( s == null ) {
			return;
		}
		try {
			JobUtils.unscheduleJob(s, trigger.getDescription(), trigger.getKey());
		} catch ( Exception e ) {
			// ignore
		}
	}

	private String calibrationJobDescription(String sourceId) {
		return String.format("CozIR calibration [%s]", sourceId);
	}

	private TriggerKey triggerKey(String sourceId) {
		return new TriggerKey(sourceId, COZIR_JOB_GROUP);
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
				log.error("Communication error calibrating altitude on CozIR {}: {}", sourceId,
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
					new Date(System.currentTimeMillis() + 15000L));
		} else {
			task.run();
		}
	}

	@Override
	public void calibrateAsCo2FreshAirLevel() throws IOException {
		log.info("Calibrating CozIR {} CO2 sensor to fresh-air level", sourceId);
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
	public OptionalService<Scheduler> getScheduler() {
		return scheduler;
	}

	/**
	 * Set the scheduler.
	 * 
	 * @param scheduler
	 *        the scheduler
	 */
	public void setScheduler(OptionalService<Scheduler> scheduler) {
		this.scheduler = scheduler;
	}

}
