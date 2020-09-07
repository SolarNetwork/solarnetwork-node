/* ==================================================================
 * FirmwareVersion.java - 28/08/2020 6:37:28 AM
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

package net.solarnetwork.node.hw.gss.co2;

import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.FirmwareVersion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * A firmware version.
 * 
 * @author matt
 * @version 1.0
 */
public class FirmwareVersion {

	/**
	 * Date formatter for CozIR firmware date style <code>Oct 25 2016</code>.
	 */
	public static final DateTimeFormatter FIRMWARE_DATE = DateTimeFormatter.ofPattern("MMM d yyyy");

	/**
	 * Time formatter for CozIR firmware date style <code>13:24:49</code>.
	 */
	public static final DateTimeFormatter FIRMWARE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final String version;
	private final LocalDateTime date;

	/**
	 * Parse a raw firmware version message.
	 * 
	 * <p>
	 * Messages take the form: <code>Y,Oct 25 2016,13:24:49,AL22</code>.
	 * </p>
	 * 
	 * @param message
	 *        the message to parse
	 * @return the firmware version, or {@literal null} if one cannot be
	 *         determined
	 */
	public static FirmwareVersion parseMessage(String message) {
		if ( message == null || message.isEmpty() ) {
			return null;
		}
		String[] components = message.trim().split(",");
		if ( !(components.length == 4 && FirmwareVersion.getKey().equals(components[0])) ) {
			return null;
		}
		try {
			LocalDate date = LocalDate.parse(components[1], FIRMWARE_DATE);
			LocalTime time = LocalTime.parse(components[2], FIRMWARE_TIME);
			return new FirmwareVersion(components[3], date.atTime(time));
		} catch ( DateTimeParseException e ) {
			// ignore
			return null;
		}

	}

	/**
	 * Constructor.
	 * 
	 * @param version
	 *        the version
	 * @param date
	 *        the date
	 */
	public FirmwareVersion(String version, LocalDateTime date) {
		super();
		this.version = version;
		this.date = date;
	}

	/**
	 * Get the version.
	 * 
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Get the date.
	 * 
	 * @return the date
	 */
	public LocalDateTime getDate() {
		return date;
	}

}
