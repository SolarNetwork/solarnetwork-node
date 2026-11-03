/* ==================================================================
 * BluetoothSetupStatus.java - 29/10/2026 9:00:00 AM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.bluetooth;

import java.time.Instant;

/**
 * The Bluetooth setup radio status.
 *
 * @author elijah
 * @version 1.0
 */
public final class BluetoothSetupStatus {

	private final boolean radioActive;
	private final boolean discoverable;
	private final boolean powered;
	private final boolean installEnabled;
	private final boolean opModeActive;
	private final Long opModeExpires;
	private final boolean offline;
	private final Instant offlineSince;
	private final boolean alwaysOn;
	private final String schedule;
	private final boolean scheduleActive;
	private final String scheduleError;
	private final Long scheduleNext;
	private final String scheduleSource;
	private final String scheduleMetadataError;
	private final String timeZone;

	BluetoothSetupStatus(boolean radioActive, boolean discoverable, boolean powered,
			boolean installEnabled, boolean opModeActive, Long opModeExpires, boolean offline,
			Instant offlineSince, boolean alwaysOn, String schedule, boolean scheduleActive,
			String scheduleError, Long scheduleNext, String scheduleSource, String scheduleMetadataError,
			String timeZone) {
		super();
		this.radioActive = radioActive;
		this.discoverable = discoverable;
		this.powered = powered;
		this.installEnabled = installEnabled;
		this.opModeActive = opModeActive;
		this.opModeExpires = opModeExpires;
		this.offline = offline;
		this.offlineSince = offlineSince;
		this.alwaysOn = alwaysOn;
		this.schedule = schedule;
		this.scheduleActive = scheduleActive;
		this.scheduleError = scheduleError;
		this.scheduleNext = scheduleNext;
		this.scheduleSource = scheduleSource;
		this.scheduleMetadataError = scheduleMetadataError;
		this.timeZone = timeZone;
	}

	/**
	 * Get the radio active state.
	 *
	 * @return {@literal true} if the Bluetooth peripheral is running (and so
	 *         advertising)
	 */
	public boolean isRadioActive() {
		return radioActive;
	}

	/**
	 * Get the adapter discoverable state.
	 *
	 * @return {@literal true} if the Bluetooth adapter is discoverable
	 */
	public boolean isDiscoverable() {
		return discoverable;
	}

	/**
	 * Get the adapter powered state.
	 *
	 * @return {@literal true} if the Bluetooth adapter is powered
	 */
	public boolean isPowered() {
		return powered;
	}

	/**
	 * Get the OS install-enabled state.
	 *
	 * @return {@literal true} if the OS has the peripheral service enabled to
	 *         start at boot, which the helper sets to the last enable or
	 *         disable decision
	 */
	public boolean isInstallEnabled() {
		return installEnabled;
	}

	/**
	 * Get the operational mode active state.
	 *
	 * @return {@literal true} if the configured operational mode is active
	 */
	public boolean isOpModeActive() {
		return opModeActive;
	}

	/**
	 * Get the operational mode expiration.
	 *
	 * @return the expiration, as milliseconds since the epoch, or
	 *         {@literal null} if not active or no expiration
	 */
	public Long getOpModeExpires() {
		return opModeExpires;
	}

	/**
	 * Get the offline trigger state.
	 *
	 * @return {@literal true} if the offline trigger is currently holding the
	 *         radio on
	 */
	public boolean isOffline() {
		return offline;
	}

	/**
	 * Get the time the offline operational mode was first seen active.
	 *
	 * @return the time, or {@literal null} if the offline mode is not active
	 */
	public Instant getOfflineSince() {
		return offlineSince;
	}

	/**
	 * Get the always-on setting.
	 *
	 * @return {@literal true} if the always-on setting is enabled
	 */
	public boolean isAlwaysOn() {
		return alwaysOn;
	}

	/**
	 * Get the availability schedule.
	 *
	 * @return the schedule in canonical form, empty if none is configured
	 */
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Get the schedule active state.
	 *
	 * @return {@literal true} if the availability schedule currently allows the
	 *         radio on
	 */
	public boolean isScheduleActive() {
		return scheduleActive;
	}

	/**
	 * Get the schedule error.
	 *
	 * @return the reason the configured schedule or time zone could not be
	 *         used, or {@literal null} if it is valid
	 */
	public String getScheduleError() {
		return scheduleError;
	}

	/**
	 * Get the next schedule boundary.
	 *
	 * @return the time the schedule next starts or stops allowing the radio on,
	 *         as milliseconds since the epoch, or {@literal null}
	 */
	public Long getScheduleNext() {
		return scheduleNext;
	}

	/**
	 * Get the source of the schedule in effect.
	 *
	 * @return {@link BluetoothSetupService#SCHEDULE_SOURCE_METADATA} when the
	 *         schedule (or its error) comes from metadata, otherwise
	 *         {@link BluetoothSetupService#SCHEDULE_SOURCE_SETTING}
	 */
	public String getScheduleSource() {
		return scheduleSource;
	}

	/**
	 * Get the metadata schedule read problem.
	 *
	 * @return why the metadata schedule could not be read, such as the metadata
	 *         being unavailable or having no schedule at the path, or
	 *         {@literal null} if it was read or is not configured
	 */
	public String getScheduleMetadataError() {
		return scheduleMetadataError;
	}

	/**
	 * Get the effective time zone.
	 *
	 * @return the zone ID the schedule is evaluated in, or {@literal null} if
	 *         the configured zone is invalid
	 */
	public String getTimeZone() {
		return timeZone;
	}
}
