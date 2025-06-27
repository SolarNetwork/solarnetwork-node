/* ==================================================================
 * Watchdog.java - 30/08/2019 2:34:44 pm
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.stabiliti30c;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cControlAccessor;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cData;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.ModbusDeviceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.DateUtils;

/**
 * Component to integrate with the watchdog timer of the Stabiliti 30C series
 * power control system.
 *
 * @author matt
 * @version 2.1
 */
public class Watchdog extends ModbusDeviceSupport implements SettingSpecifierProvider {

	/**
	 * The default value for the {@code startupDelaySeconds} property.
	 */
	public static final int DEFAULT_STARTUP_DELAY_SECONDS = 5;

	/** The default value for the {@code timeoutSeconds} property. */
	public static final int DEFAULT_TIMEOUT_SECONDS = 900;

	/** The default value for the {@code updateFrequency} property. */
	public static final int DEFAULT_UPDATE_FREQUENCY = 60;

	private TaskScheduler taskScheduler;
	private int startupDelaySeconds = DEFAULT_STARTUP_DELAY_SECONDS;
	private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
	private long updateFrequency = DEFAULT_UPDATE_FREQUENCY;

	private boolean enabled = false;
	private Instant lastUpdateTime;
	private Exception lastUpdateError;
	private ScheduledFuture<?> task;

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return null;
	}

	/**
	 * Callback after properties have been changed.
	 *
	 * @param properties
	 *        the changed properties
	 */
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( enabled ) {
			scheduleTask();
		}
	}

	/**
	 * Startup the watchdog task.
	 *
	 * <p>
	 * Call this method after the properties of this class have been configured,
	 * to start execution of the watchdog task.
	 * </p>
	 */
	public synchronized void startup() {
		enabled = true;
		scheduleTask();
	}

	public synchronized void shutdown() {
		enabled = false;

	}

	private synchronized void unscheduleTask() {
		if ( task != null ) {
			task.cancel(true);
			task = null;
		}
	}

	private synchronized void scheduleTask() {
		unscheduleTask();
		task = taskScheduler.scheduleWithFixedDelay(new WatchdogTask(),
				Instant.ofEpochMilli(
						System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(startupDelaySeconds)),
				Duration.ofSeconds(updateFrequency));
	}

	private class WatchdogTask implements Runnable {

		@Override
		public void run() {
			final ModbusNetwork modbus = modbusNetwork();
			final int unitId = getUnitId();
			final int timeout = getTimeoutSeconds();
			if ( modbus == null ) {
				updateStatus(Instant.now(), new RuntimeException(getMessageSource()
						.getMessage("status.err.noModbusNetwork", null, Locale.getDefault())));
				log.warn("No ModbusNetwork available for watchdog task for Stabiliti {}", unitId);
				return;
			}
			log.info("Setting watchdog timeout to {}s on Stabiliti {}", timeout, modbusDeviceName());
			try {
				modbus.performAction(unitId, new ModbusConnectionAction<Void>() {

					@Override
					public Void doWithConnection(ModbusConnection conn) throws IOException {
						Stabiliti30cControlAccessor acc = new Stabiliti30cData().controlAccessor(conn);
						acc.setWatchdogTimeout(timeout);
						return null;
					}
				});
				updateStatus(Instant.now(), null);
			} catch ( Exception e ) {
				updateStatus(Instant.now(), e);
				log.error("Exception in watchdog task for Stabiliti {}: {}", modbusDeviceName(),
						e.getMessage(), e);
			}
		}

	}

	private synchronized void updateStatus(Instant time, Exception error) {
		lastUpdateTime = time;
		lastUpdateError = error;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.stabiliti30c.Watchdog";
	}

	@Override
	public String getDisplayName() {
		return "Stabiliti 30C Watchdog";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		// get last update time
		results.add(new BasicTitleSettingSpecifier("status", statusInfo(), true));

		results.add(new BasicTextFieldSettingSpecifier("uid", ""));
		results.add(new BasicTextFieldSettingSpecifier("groupUid", ""));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['uid']",
				"Modbus Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(1)));

		results.add(new BasicTextFieldSettingSpecifier("timeoutSeconds",
				String.valueOf(DEFAULT_TIMEOUT_SECONDS)));

		results.add(new BasicTextFieldSettingSpecifier("updateFrequency",
				String.valueOf(DEFAULT_UPDATE_FREQUENCY)));

		results.add(new BasicTextFieldSettingSpecifier("startupDelaySeconds",
				String.valueOf(DEFAULT_STARTUP_DELAY_SECONDS)));

		return results;
	}

	private String statusInfo() {
		final Instant luTime;
		final Exception luError;
		synchronized ( this ) {
			luTime = lastUpdateTime;
			luError = lastUpdateError;
		}
		if ( luTime == null ) {
			return "N/A";
		}

		final String ts = DateUtils.formatForLocalDisplay(luTime);
		if ( luError != null ) {
			return getMessageSource().getMessage("status.msg.err",
					new Object[] { ts, luError.getLocalizedMessage() }, Locale.getDefault());
		}
		return getMessageSource().getMessage("status.msg.ok", new Object[] { ts }, Locale.getDefault());
	}

	/**
	 * Set the task scheduler to use for the watchdog task.
	 *
	 * @param taskScheduler
	 *        the task scheduler
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the watchdog timeout value.
	 *
	 * @return the watchdog timeout value, in seconds; defaults to
	 *         {@link #DEFAULT_TIMEOUT_SECONDS}
	 */
	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	/**
	 * Set the watchdog timeout value.
	 *
	 * <p>
	 * This is the number of seconds the Stabiliti should count down from, and
	 * if not updated before it reaches zero to shut the system down. This value
	 * should be larger than the configured {@code updateFrequency}.
	 * </p>
	 *
	 * @param timeoutSeconds
	 *        the timeout value, in seconds
	 */
	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	/**
	 * Get the watchdog update frequency.
	 *
	 * @return the watchdog update frequency, in seconds; defaults to
	 *         {@link #DEFAULT_UPDATE_FREQUENCY}
	 */
	public long getUpdateFrequency() {
		return updateFrequency;
	}

	/**
	 * Set the watchdog update frequency.
	 *
	 * <p>
	 * This is the frequency at which this component should reset the watchdog
	 * timeout value on the Stabiliti to {@code timeoutSeconds}, essentially
	 * resetting the count down timer. This value should be smaller than the
	 * configured {@code timeoutSeconds}.
	 * </p>
	 *
	 * @param updateFrequency
	 *        the update frequency, in seconds
	 */
	public void setUpdateFrequency(long updateFrequency) {
		this.updateFrequency = updateFrequency;
	}

	/**
	 * Get the startup delay when scheduling the watchdog task.
	 *
	 * @return the startup delay, in seconds; defaluts to
	 *         {@link #DEFAULT_STARTUP_DELAY_SECONDS}
	 */
	public int getStartupDelaySeconds() {
		return startupDelaySeconds;
	}

	/**
	 * Get the startup delay for scheduling the watchdog task.
	 *
	 * @param startupDelaySeconds
	 *        the initial startup delay, in seconds
	 */
	public void setStartupDelaySeconds(int startupDelaySeconds) {
		this.startupDelaySeconds = startupDelaySeconds;
	}

}
