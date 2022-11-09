/* ==================================================================
 * BacnetObjectType.java - 4/11/2022 3:07:41 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.bacnet;

import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.util.ObjectUtils;

/**
 * Enumeration of BACnet object types.
 * 
 * @author matt
 * @version 1.0
 */
public enum BacnetObjectType implements CodedValue {

	/** Analog input. */
	AnalogInput(0),

	/** Analog output. */
	AnalogOutput(1),

	/** Analog value. */
	AnalogValue(2),

	/** Binary input. */
	BinaryInput(3),

	/** Binary output. */
	BinaryOutput(4),

	/** Binary value. */
	BinaryValue(5),

	/** Calendar. */
	Calendar(6),

	/** Command. */
	Command(7),

	/** Device. */
	Device(8),

	/** Event enrollment. */
	EventEnrollment(9),

	/** File. */
	File(10),

	/** Group. */
	Group(11),

	/** Loop. */
	Loop(12),

	/** Multi-state input. */
	MultiStateInput(13),

	/** Multi-state output. */
	MultiStateOutput(14),

	/** Notification class. */
	NotificationClass(15),

	/** Program. */
	Program(16),

	/** Schedule. */
	Schedule(17),

	/** Averaging. */
	Averaging(18),

	/** Multi-state value. */
	MultiStateValue(19),

	/** Trend log. */
	TrendLog(20),

	/** Life safety point. */
	LifeSafetyPoint(21),

	/** Life safety zone. */
	LifeSafetyZone(22),

	/** Accumulator. */
	Accumulator(23),

	/** Pulse converter. */
	PulseConverter(24),

	/** Event log. */
	EventLog(25),

	/** Global group. */
	GlobalGroup(26),

	/** Trend log multiple. */
	TrendLogMultiple(27),

	/** Load control. */
	LoadControl(28),

	/** Structured view. */
	StructuredView(29),

	/** Access door. */
	AccessDoor(30),

	/** Timer. */
	Timer(31),

	/** Access credential. */
	AccessCredential(32),

	/** Access point. */
	AccessPoint(33),

	/** Access rights. */
	AccessRights(34),

	/** Access user. */
	AccessUser(35),

	/** Access zone. */
	AccessZone(36),

	/** Credential data input. */
	CredentialDataInput(37),

	/** Network security. */
	NetworkSecurity(38),

	/** Bitstring value. */
	BitstringValue(39),

	/** Character string value. */
	CharacterstringValue(40),

	/** Date pattern value. */
	DatePatternValue(41),

	/** Date value. */
	DateValue(42),

	/** Datetime pattern value. */
	DatetimePatternValue(43),

	/** Datetime value. */
	DatetimeValue(44),

	/** Integer value. */
	IntegerValue(45),

	/** Large analog value. */
	LargeAnalogValue(46),

	/** Octetstring value. */
	OctetstringValue(47),

	/** Positive integer value. */
	PositiveIntegerValue(48),

	/** Time pattern value. */
	TimePatternValue(49),

	/** Time value. */
	TimeValue(50),

	/** Notification forwarder. */
	NotificationForwarder(51),

	/** Alert enrolment. */
	AlertEnrollment(52),

	/** Channel. */
	Channel(53),

	/** Lighting output. */
	LightingOutput(54),

	/** Binary lighting output. */
	BinaryLightingOutput(55),

	/** Network port. */
	NetworkPort(56),

	/** Elevator group. */
	ElevatorGroup(57),

	/** Escalator. */
	Escalator(58),

	/** Lift. */
	Lift(59),

	/** Staging. */
	Staging(60),

	/** Audit log. */
	AuditLog(61),

	/** Audit reporter. */
	AuditReporter(62),

	;

	private int id;

	private BacnetObjectType(int id) {
		this.id = id;
	}

	@Override
	public int getCode() {
		return id;
	}

	/**
	 * Get the object ID.
	 * 
	 * <p>
	 * This is an alias for {@link #getCode()}.
	 * </p>
	 * 
	 * @return the ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get an enumeration value for a string key.
	 * 
	 * @param value
	 *        the value to parse into an enumeration value; can be either an
	 *        integer {@code code} or an enumeration name
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if the value cannot be parsed into an enumeration value
	 * @see CodedValue#forCodeValue(int, Class, Enum)
	 */
	public static BacnetObjectType forKey(String value) {
		ObjectUtils.requireNonNullArgument(value, "value");
		try {
			int code = Integer.parseInt(value);
			BacnetObjectType result = CodedValue.forCodeValue(code, BacnetObjectType.class, null);
			if ( result != null ) {
				return result;
			}
			throw new IllegalArgumentException(
					String.format("Unsupported BacnetObjectType value [%s]", value));
		} catch ( NumberFormatException e ) {
			// ignore and try by name
			try {
				return BacnetObjectType.valueOf(value);
			} catch ( IllegalArgumentException e2 ) {
				try {
					// ignore and try by name with train-case
					return BacnetObjectType.valueOf(BacnetUtils.kebabToCamelCase(value));
				} catch ( IllegalArgumentException e3 ) {
					throw new IllegalArgumentException(
							String.format("Unsupported BacnetObjectType value [%s]", value));
				}
			}
		}
	}

}
