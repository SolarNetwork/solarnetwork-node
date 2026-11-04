/* ==================================================================
 * BluetoothSetupUtils.java - 29/10/2026 9:00:00 AM
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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.StringUtils.escapeHtml;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.node.service.support.TariffScheduleUtils;

/**
 * Utilities for Bluetooth setup.
 *
 * @author elijah
 * @version 1.0
 */
public final class BluetoothSetupUtils {

	private BluetoothSetupUtils() {
		// not available
	}

	/**
	 * Parse an instruction duration parameter value.
	 *
	 * @param value
	 *        the value to parse, in seconds
	 * @return the duration, or {@literal 0} if {@code value} is {@literal null}
	 *         or empty
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a number
	 */
	public static long parseDuration(@Nullable String value) {
		if ( value == null || value.isEmpty() ) {
			return 0L;
		}
		try {
			return Long.parseLong(value.trim());
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException("Invalid duration [" + value + "]: not a number.");
		}
	}

	/**
	 * Generate a localized status message describing the radio state and why it
	 * is on.
	 *
	 * @param messageSource
	 *        the message source, or {@literal null}
	 * @param status
	 *        the status, or {@literal null} if unknown
	 * @param opMode
	 *        the operational mode that enables the radio
	 * @param offlineOpMode
	 *        the operational mode that signals the network is offline
	 * @param offlineThresholdMinutes
	 *        the minutes the offline mode must be active before the radio turns
	 *        on
	 * @param metadataPath
	 *        the metadata schedule path, or {@literal null} if no metadata
	 *        schedule is configured
	 * @return the message, or an empty string if {@code messageSource} is
	 *         {@literal null}
	 */
	public static String statusMessage(@Nullable MessageSource messageSource,
			@Nullable BluetoothSetupStatus status, String opMode, String offlineOpMode,
			int offlineThresholdMinutes, @Nullable String metadataPath) {
		if ( messageSource == null ) {
			return "";
		}
		if ( status == null ) {
			return nonnull(
					messageSource.getMessage("status.unknown", null, "Unknown", Locale.getDefault()),
					"Status message");
		}
		final List<String> reasons = new ArrayList<>(3);
		if ( status.isAlwaysOn() ) {
			reasons.add(
					messageSource.getMessage("status.alwaysOn", null, "always on", Locale.getDefault()));
		}
		if ( status.isOpModeActive() ) {
			final Long opModeExpires = status.getOpModeExpires();
			Object expires = (opModeExpires != null ? Instant.ofEpochMilli(opModeExpires)
					: messageSource.getMessage("status.noExpiry", null, "no expiry",
							Locale.getDefault()));
			reasons.add(messageSource.getMessage("status.opMode", new Object[] { opMode, expires },
					"operational mode active", Locale.getDefault()));
		}
		final Instant offlineSince = status.getOfflineSince();
		if ( status.isOffline() ) {
			reasons.add(messageSource.getMessage("status.offline",
					new Object[] { offlineOpMode, offlineSince }, "offline mode active",
					Locale.getDefault()));
		} else if ( offlineSince != null ) {
			reasons.add(messageSource.getMessage("status.offlinePending",
					new Object[] { offlineOpMode, offlineSince,
							offlineSince.plus(Duration.ofMinutes(offlineThresholdMinutes)) },
					"offline mode active", Locale.getDefault()));
		}
		if ( metadataPath != null ) {
			final String readError = status.getScheduleMetadataError();
			if ( !BluetoothSetupService.SCHEDULE_SOURCE_METADATA.equals(status.getScheduleSource()) ) {
				reasons.add(messageSource.getMessage("status.scheduleMetadataNotLoaded",
						new Object[] { metadataPath, readError != null ? readError : "not read yet" },
						"metadata schedule not loaded", Locale.getDefault()));
			} else if ( readError != null ) {
				reasons.add(messageSource.getMessage("status.scheduleMetadataStale",
						new Object[] { metadataPath, readError }, "metadata unavailable",
						Locale.getDefault()));
			} else {
				reasons.add(messageSource.getMessage("status.scheduleMetadata",
						new Object[] { metadataPath }, "schedule from metadata", Locale.getDefault()));
			}
		}
		if ( status.getScheduleError() != null ) {
			reasons.add(messageSource.getMessage("status.scheduleInvalid",
					new Object[] { status.getScheduleError() }, "schedule invalid",
					Locale.getDefault()));
		} else if ( !status.getSchedule().isEmpty() ) {
			// the rules are shown in the schedule table, not inline
			final Long scheduleNext = status.getScheduleNext();
			if ( scheduleNext == null && !status.isScheduleActive() ) {
				reasons.add(messageSource.getMessage("status.scheduleNextNone",
						new Object[] { status.getTimeZone() },
						"schedule has no window within the next year", Locale.getDefault()));
			} else {
				Object next = (scheduleNext != null ? Instant.ofEpochMilli(scheduleNext)
						: messageSource.getMessage("status.noExpiry", null, "no expiry",
								Locale.getDefault()));
				reasons.add(messageSource.getMessage(
						status.isScheduleActive() ? "status.schedule" : "status.scheduleNext",
						new Object[] { status.getTimeZone(), next },
						status.isScheduleActive() ? "schedule active" : "schedule inactive",
						Locale.getDefault()));
			}
		}
		final String state = nonnull(
				messageSource.getMessage(status.isRadioActive() ? "status.on" : "status.off", null,
						status.isRadioActive() ? "On" : "Off", Locale.getDefault()),
				"Status message");
		if ( reasons.isEmpty() ) {
			return state;
		}
		return state + " (" + String.join("; ", reasons) + ")";
	}

	/**
	 * Render an availability schedule as HTML: a table of its rules and the
	 * rule that matches now.
	 *
	 * <p>
	 * The table lists the rules actually parsed, so it can be compared with the
	 * expected rules: a schedule without a header row is missing its first
	 * rule, which was taken as the header.
	 * </p>
	 *
	 * @param messageSource
	 *        the message source, or {@literal null}
	 * @param schedule
	 *        the schedule
	 * @param now
	 *        the date and time to find the matching rule at, in the schedule's
	 *        time zone, or {@literal null} to not show the matching rule (for
	 *        example when the time zone is invalid)
	 * @param locale
	 *        the locale
	 * @return the HTML, or an empty string if {@code messageSource} is
	 *         {@literal null}
	 */
	public static String scheduleTable(@Nullable MessageSource messageSource,
			AvailabilitySchedule schedule, @Nullable LocalDateTime now, Locale locale) {
		if ( messageSource == null ) {
			return "";
		}
		final StringBuilder buf = new StringBuilder();
		if ( schedule.isEmpty() ) {
			buf.append("<p>").append(escapeHtml(messageSource.getMessage("status.scheduleRulesEmpty",
					null, "The schedule has no rules.", locale))).append("</p>");
			return buf.toString();
		}
		final Map<Integer, Tariff> active = TariffScheduleUtils.renderTariffScheduleTable(messageSource,
				schedule.getSchedule(), (now != null ? now : LocalDateTime.MIN),
				AvailabilitySchedule.EVALUATOR, true, locale, buf);
		if ( now != null ) {
			final String when = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
					.withLocale(locale).format(now);
			buf.append("<p>");
			if ( active.isEmpty() ) {
				buf.append(escapeHtml(messageSource.getMessage("status.scheduleRuleNone",
						new Object[] { when }, "No rule matches now.", locale)));
			} else {
				buf.append(escapeHtml(messageSource.getMessage("status.scheduleRuleActive",
						new Object[] { active.keySet().iterator().next() + 1, when },
						"A rule matches now.", locale)));
			}
			buf.append("</p>");
		}
		return buf.toString();
	}

}
