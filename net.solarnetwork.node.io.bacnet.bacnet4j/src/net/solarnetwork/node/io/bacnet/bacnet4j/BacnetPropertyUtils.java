/* ==================================================================
 * BacnetPropertyUtils.java - 7/11/2022 6:31:46 am
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

package net.solarnetwork.node.io.bacnet.bacnet4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;
import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.io.bacnet.BacnetObjectType;
import net.solarnetwork.node.io.bacnet.BacnetPropertyType;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Utility methods for BACnet properties.
 * 
 * @author matt
 * @version 1.0
 */
public final class BacnetPropertyUtils {

	private BacnetPropertyUtils() {
		// not available
	}

	/**
	 * Convert a value to a Number.
	 * 
	 * <p>
	 * This method extracts number values out of the standard BACnet data types
	 * like unsigned, signed, real, and double. Additionally it supports:
	 * </p>
	 * 
	 * <dl>
	 * <dd>boolean</dd>
	 * <dt><code>1</code> for <em>true</em> and <code>0</code> for
	 * <em>false</em></dt>
	 * <dt>enumerated</dt>
	 * <dd>the integer encoding value</dd>
	 * <dt>date</dt>
	 * <dd>if specific with year, month, and day values, then an integer in the
	 * form <code>yyyyMMdd</code></dd>
	 * <dt>time</dt>
	 * <dd>if specific with at least hour, then an integer with the number of
	 * seconds of the day; if specific with hundredths of seconds then a float
	 * with the seconds and fractional seconds of the day</dd>
	 * <dt>object-identifier</dt>
	 * <dd>a float in the form <code>type.instance</code></dd>
	 * <dt>bitstring</dt>
	 * <dd>an integer encoding of the bits</dd>
	 * </dl>
	 * 
	 * @param value
	 *        the BACnet value to convert
	 * @return the number value, or {@literal null} if not supported
	 */
	public static Number numberValue(Encodable value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof com.serotonin.bacnet4j.type.primitive.Boolean ) {
			return ((com.serotonin.bacnet4j.type.primitive.Boolean) value).booleanValue() ? 1 : 0;
		} else if ( value instanceof UnsignedInteger ) {
			return NumberUtils.narrow(((UnsignedInteger) value).bigIntegerValue(), 2);
		} else if ( value instanceof SignedInteger ) {
			return NumberUtils.narrow(((SignedInteger) value).bigIntegerValue(), 2);
		} else if ( value instanceof Real ) {
			return ((Real) value).floatValue();
		} else if ( value instanceof com.serotonin.bacnet4j.type.primitive.Double ) {
			return ((com.serotonin.bacnet4j.type.primitive.Double) value).doubleValue();
		} else if ( value instanceof Enumerated ) {
			return ((Enumerated) value).intValue();
		} else if ( value instanceof Date ) {
			Date date = (Date) value;
			if ( date.isSpecific() ) {
				return Integer.valueOf(String.format("%d%02d%02d", date.getCenturyYear(),
						date.getMonth().getId(), date.getDay()));
			}
		} else if ( value instanceof Time ) {
			Time time = (Time) value;
			if ( !time.isHourUnspecified() ) {
				int secs = (int) TimeUnit.HOURS.toSeconds(time.getHour());
				if ( !time.isMinuteUnspecified() ) {
					secs += (int) TimeUnit.MINUTES.toSeconds(time.getMinute());
				}
				if ( !time.isSecondUnspecified() ) {
					secs += time.getSecond();
				}
				if ( time.isHundredthUnspecified() ) {
					return secs;
				}
				return Float.valueOf(String.format("%d.%02d", secs, time.getHundredth()));
			}
		} else if ( value instanceof ObjectIdentifier ) {
			ObjectIdentifier ident = (ObjectIdentifier) value;
			return Float.valueOf(
					String.format("%d.%d", ident.getObjectType().intValue(), ident.getInstanceNumber()));
		} else if ( value instanceof BitString ) {
			BitString s = (BitString) value;
			boolean[] bits = s.getValue();
			if ( bits != null ) {
				StringBuilder buf = new StringBuilder(bits.length);
				for ( int i = bits.length - 1; i >= 0; i-- ) {
					buf.append(bits[i] ? '1' : '0');
				}
				return NumberUtils.narrow(new BigInteger(buf.toString(), 2), 2);
			}
		} else if ( value instanceof OctetString ) {
			OctetString s = (OctetString) value;
			byte[] bytes = s.getBytes();
			StringBuilder buf = new StringBuilder();
			for ( int i = 0, len = bytes.length; i < len; i++ ) {
				byte b = bytes[i];
				buf.append(String.format("%02x", Byte.toUnsignedInt(b)));
			}
			return NumberUtils.narrow(new BigInteger(buf.toString(), 16), 2);
		}
		return null;
	}

	/**
	 * Convert a value into a {@link BitSet}.
	 * 
	 * @param value
	 *        the value to convert; only the <em>bit-string</em> type is
	 *        supported
	 * @return the BitSet value, or {@literal null} if not supported
	 */
	public static BitSet bitSetValue(Encodable value) {
		if ( value instanceof BitString ) {
			BitString s = (BitString) value;
			boolean[] bits = s.getValue();
			if ( bits != null ) {
				BitSet result = new BitSet(bits.length);
				for ( int i = 0, len = bits.length; i < len; i++ ) {
					result.set(i, bits[i]);
				}
				return result;
			}
		}
		return null;
	}

	/**
	 * Convert a value into a {@link BitString}.
	 * 
	 * <p>
	 * The following types are supported:
	 * </p>
	 * 
	 * <dl>
	 * <dt>{@link BitSet}</dt>
	 * <dd>Converted into an equivalent {@code BitString} value.</dd>
	 * <dt>{@code Number}</dt>
	 * <dd>The value is first converted to a {@link BigInteger} via
	 * {@link NumberUtils#bigIntegerForNumber(Number)}. Then that value's set
	 * bits are set onto a {@code BitString}.</dd>
	 * </dl>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the string value, or {@literal null} if not supported
	 */
	public static BitString bitStringValue(Object value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof BitSet ) {
			BitSet set = (BitSet) value;
			BitString s = new BitString(set.length(), false);
			for ( int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1) ) {
				s.setValue(i + 1, true);
			}
			return s;
		} else if ( value instanceof Number ) {
			BigInteger big = NumberUtils.bigIntegerForNumber((Number) value);
			int len = big.bitLength();
			BitString s = new BitString(len, false);
			for ( int i = big.getLowestSetBit(); i < len; i++ ) {
				s.setValue(i + 1, big.testBit(i));
			}
			return s;
		}
		return null;
	}

	/**
	 * Convert a value into a string.
	 * 
	 * <p>
	 * The following types are supported:
	 * </p>
	 * 
	 * <dl>
	 * <dt>character-string</dt>
	 * <dd>returned as-is</dd>
	 * <dt>octet-string</dt>
	 * <dd>returned in dotted-decimal form</dt>
	 * <dt><em>number</em></dt>
	 * <dd>any type supported by {@link #numberValue(Encodable)} will be
	 * returned as a base-10 string representation</dd>
	 * </dl>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the string value, or {@literal null} if not supported
	 */
	public static String stringValue(Encodable value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof CharacterString ) {
			return ((CharacterString) value).getValue();
		} else if ( value instanceof OctetString ) {
			OctetString s = (OctetString) value;
			return s.getDescription();
		} else {
			Number n = numberValue(value);
			if ( n != null ) {
				return n.toString();
			}
		}
		return null;
	}

	/**
	 * Convert a property reference value into an {@link Encodable}.
	 * 
	 * @param ref
	 *        the property reference
	 * @param value
	 *        the property value
	 * @return the result, or {@literal null} if not supported
	 */
	public static Encodable encodeValue(BacnetDeviceObjectPropertyRef ref, Object value) {
		if ( value == null ) {
			return Null.instance;
		}

		BacnetObjectType objType = CodedValue.forCodeValue(ref.getObjectType(), BacnetObjectType.class,
				null);
		BacnetPropertyType propType = CodedValue.forCodeValue(ref.getPropertyId(),
				BacnetPropertyType.class, null);
		if ( objType == null || propType == null ) {
			return encodeValue(value);
		}
		switch (propType) {
			case PresentValue:
				switch (objType) {
					case AnalogOutput:
					case AnalogValue:
					case LightingOutput:
					case Loop:
					case PulseConverter:
					case Staging:
						return realValue(value);

					case BinaryOutput:
					case BinaryValue:
						return binaryValue(value);

					case BitstringValue:
						return bitStringValue(value);

					case DateValue:
						return dateValue(value);

					case DatetimeValue:
						return dateTimeValue(value);

					case IntegerValue:
						return signedIntegerValue(value);

					case LargeAnalogValue:
						return doubleValue(value);

					case Accumulator:
					case MultiStateOutput:
					case MultiStateValue:
					case PositiveIntegerValue:
					case Timer:
						return unsignedIntegerValue(value);

					case TimeValue:
						return timeValue(value);

					default:
						// TODO support more
						return null;

				}

			default:
				// TODO support more
				return null;
		}
	}

	/**
	 * Convert a value into a binary property value.
	 * 
	 * <p>
	 * Supports the following values:
	 * </p>
	 * 
	 * <dl>
	 * <dt>Boolean</dt>
	 * <dd>{@literal true} converts to {@code active}, {@literal false} to
	 * {@code inactive}</dd>
	 * <dt>Number</dt>
	 * <dd>{@literal 0} converts to {@code inactive}, all other values to
	 * {@code active}</dd>
	 * <dt>String</dt>
	 * <dd>value parsed as to a {@code boolean} via
	 * {@link StringUtils#parseBoolean(String)}, then converted according to the
	 * <b>Boolean</b> rules above</dd>
	 * </dl>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the binary property value, or {@literal null} if not supported or
	 *         {@code value} is {@literal null}
	 */
	public static BinaryPV binaryValue(Object value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Boolean ) {
			return ((Boolean) value).booleanValue() ? BinaryPV.active : BinaryPV.inactive;
		} else if ( value instanceof Number ) {
			return ((Number) value).intValue() == 0 ? BinaryPV.inactive : BinaryPV.active;
		} else if ( value instanceof String ) {
			boolean b = StringUtils.parseBoolean((String) value);
			return b ? BinaryPV.active : BinaryPV.inactive;
		}
		return null;
	}

	/**
	 * Convert a value into a Real.
	 * 
	 * <p>
	 * {@code Number} values will be converted to {@code float}. {@code String}
	 * values will be parsed as a {@link BigDecimal} and then converted to
	 * {@code float}.
	 * </p>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the Real, or {@literal null} if not supported or {@code value} is
	 *         {@literal null}
	 */
	public static Real realValue(Object value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Number ) {
			return new Real(((Number) value).floatValue());
		} else if ( value instanceof String ) {
			try {
				BigDecimal d = new BigDecimal((String) value);
				return new Real(d.floatValue());
			} catch ( NumberFormatException e ) {
				// ignore
			}
		}
		return null;
	}

	/**
	 * Convert a value into a Double.
	 * 
	 * <p>
	 * {@code Number} values will be converted to {@code double}. {@code String}
	 * values will be parsed as a {@link BigDecimal} and then converted to
	 * {@code double}.
	 * </p>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the Deouble, or {@literal null} if not supported or {@code value}
	 *         is {@literal null}
	 */
	public static com.serotonin.bacnet4j.type.primitive.Double doubleValue(Object value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Number ) {
			return new com.serotonin.bacnet4j.type.primitive.Double(((Number) value).doubleValue());
		} else if ( value instanceof String ) {
			try {
				BigDecimal d = new BigDecimal((String) value);
				return new com.serotonin.bacnet4j.type.primitive.Double(d.doubleValue());
			} catch ( NumberFormatException e ) {
				// ignore
			}
		}
		return null;
	}

	/**
	 * Convert a value into an UnsignedInteger.
	 * 
	 * <p>
	 * This method first converts {@code value} into a {@link BigInteger}. If
	 * {@code value} is some other {@code Number} type it is converted using
	 * {@link NumberUtils#bigIntegerForNumber(Number)}. If {@code value} is a
	 * {@code String} it is parsed into a {@code BigInteger}. Once a
	 * {@code BigInteger} is obtained, a new {@link UnsignedInteger} is returned
	 * for that.
	 * </p>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the UnsignedInteger, or {@literal null} if not supported or
	 *         {@code value} is {@literal null}
	 */
	public static UnsignedInteger unsignedIntegerValue(Object value) {
		if ( value == null ) {
			return null;
		}
		BigInteger big = null;
		if ( value instanceof BigInteger ) {
			big = (BigInteger) value;
		} else if ( value instanceof Number ) {
			big = NumberUtils.bigIntegerForNumber((Number) value);
		} else if ( value instanceof String ) {
			try {
				big = new BigInteger((String) value);
			} catch ( NumberFormatException e ) {
				// ignore
			}
		}
		return (big != null ? new UnsignedInteger(big) : null);
	}

	/**
	 * Convert a value into an UnsignedInteger.
	 * 
	 * <p>
	 * This method first converts {@code value} into a {@link BigInteger}. If
	 * {@code value} is some other {@code Number} type it is converted using
	 * {@link NumberUtils#bigIntegerForNumber(Number)}. If {@code value} is a
	 * {@code String} it is parsed into a {@code BigInteger}. Once a
	 * {@code BigInteger} is obtained, a new {@link UnsignedInteger} is returned
	 * for that.
	 * </p>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the SignedInteger, or {@literal null} if not supported or
	 *         {@code value} is {@literal null}
	 */
	public static SignedInteger signedIntegerValue(Object value) {
		if ( value == null ) {
			return null;
		}
		BigInteger big = null;
		if ( value instanceof BigInteger ) {
			big = (BigInteger) value;
		} else if ( value instanceof Number ) {
			big = NumberUtils.bigIntegerForNumber((Number) value);
		} else if ( value instanceof String ) {
			try {
				big = new BigInteger((String) value);
			} catch ( NumberFormatException e ) {
				// ignore
			}
		}
		return (big != null ? new SignedInteger(big) : null);
	}

	/**
	 * Convert a value into a {@link Date}.
	 * 
	 * <p>
	 * The following objects are supported:
	 * </p>
	 * 
	 * <ul>
	 * <li>{@link LocalDate}</li>
	 * <li>{@link LocalDateTime}</li>
	 * <li>{@link ZonedDateTime}</li>
	 * <li>{@link Instant} (converted to system time zone</li>
	 * <li>{@link java.util.Date} (converted to system time zone</li>
	 * </ul>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the Date, or {@literal null} if not supported or {@code value} is
	 *         {@literal null}
	 */
	public static Date dateValue(Object value) {
		if ( value == null ) {
			return null;
		}
		LocalDate d = null;
		if ( value instanceof LocalDate ) {
			d = (LocalDate) value;
		} else if ( value instanceof LocalDateTime ) {
			d = ((LocalDateTime) value).toLocalDate();
		} else if ( value instanceof ZonedDateTime ) {
			d = ((ZonedDateTime) value).toLocalDate();
		} else if ( value instanceof Instant ) {
			d = ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDate();
		} else if ( value instanceof java.util.Date ) {
			d = ((java.util.Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
		return (d != null ? (Date) encodeValue(d) : null);
	}

	/**
	 * Convert a value into a {@link Time}.
	 * 
	 * <p>
	 * The following objects are supported:
	 * </p>
	 * 
	 * <ul>
	 * <li>{@link LocalTime}</li>
	 * <li>{@link LocalDateTime}</li>
	 * <li>{@link ZonedDateTime}</li>
	 * <li>{@link Instant} (converted to system time zone</li>
	 * <li>{@link java.util.Date} (converted to system time zone</li>
	 * </ul>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the Time, or {@literal null} if not supported or {@code value} is
	 *         {@literal null}
	 */
	public static Time timeValue(Object value) {
		if ( value == null ) {
			return null;
		}
		LocalTime t = null;
		if ( value instanceof LocalTime ) {
			t = (LocalTime) value;
		} else if ( value instanceof LocalDateTime ) {
			t = ((LocalDateTime) value).toLocalTime();
		} else if ( value instanceof ZonedDateTime ) {
			t = ((ZonedDateTime) value).toLocalTime();
		} else if ( value instanceof Instant ) {
			t = ((Instant) value).atZone(ZoneId.systemDefault()).toLocalTime();
		} else if ( value instanceof java.util.Date ) {
			t = ((java.util.Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
		}
		return (t != null ? (Time) encodeValue(t) : null);
	}

	/**
	 * Convert a value into a {@link DateTime}.
	 * 
	 * <p>
	 * The following objects are supported:
	 * </p>
	 * 
	 * <ul>
	 * <li>{@link LocalDateTime}</li>
	 * <li>{@link ZonedDateTime}</li>
	 * <li>{@link Instant} (converted to system time zone</li>
	 * <li>{@link java.util.Date} (converted to system time zone</li>
	 * </ul>
	 * 
	 * @param value
	 *        the value to convert
	 * @return the Time, or {@literal null} if not supported or {@code value} is
	 *         {@literal null}
	 */
	public static DateTime dateTimeValue(Object value) {
		if ( value == null ) {
			return null;
		}
		LocalDateTime t = null;
		if ( value instanceof LocalDateTime ) {
			t = (LocalDateTime) value;
		} else if ( value instanceof ZonedDateTime ) {
			t = ((ZonedDateTime) value).toLocalDateTime();
		} else if ( value instanceof Instant ) {
			t = ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
		} else if ( value instanceof java.util.Date ) {
			t = ((java.util.Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		}
		return (t != null ? (DateTime) encodeValue(t) : null);
	}

	/**
	 * Convert a value into an {@link Encodable}, without any property type
	 * reference.
	 * 
	 * @param value
	 *        the property value
	 * @return the result, or {@literal null} if not supported
	 */
	public static Encodable encodeValue(Object value) {
		if ( value == null ) {
			return Null.instance;
		} else if ( value instanceof Primitive ) {
			return (Primitive) value;
		} else if ( value instanceof String ) {
			return new CharacterString((String) value);
		} else if ( value instanceof Float ) {
			return new Real(((Float) value).floatValue());
		} else if ( value instanceof Double ) {
			return new com.serotonin.bacnet4j.type.primitive.Double(((Double) value).doubleValue());
		} else if ( value instanceof Boolean ) {
			return ((Boolean) value).booleanValue() ? com.serotonin.bacnet4j.type.primitive.Boolean.TRUE
					: com.serotonin.bacnet4j.type.primitive.Boolean.FALSE;
		} else if ( value instanceof Short || value instanceof Integer ) {
			return new SignedInteger(((Number) value).intValue());
		} else if ( value instanceof Long ) {
			return new SignedInteger(((Long) value).longValue());
		} else if ( value instanceof BigDecimal ) {
			return new com.serotonin.bacnet4j.type.primitive.Double(((BigDecimal) value).doubleValue());
		} else if ( value instanceof BigInteger ) {
			return new SignedInteger((BigInteger) value);
		} else if ( value instanceof LocalDate ) {
			LocalDate d = (LocalDate) value;
			return new Date(d.getYear(), Month.valueOf(d.getMonthValue()), d.getDayOfMonth(),
					DayOfWeek.UNSPECIFIED);
		} else if ( value instanceof LocalTime ) {
			LocalTime t = (LocalTime) value;
			return new Time(t.getHour(), t.getMinute(), t.getSecond(), t.getNano() / 10000000);
		} else if ( value instanceof LocalDateTime ) {
			LocalDateTime dt = (LocalDateTime) value;
			Date d = (Date) encodeValue(dt.toLocalDate());
			Time t = (Time) encodeValue(dt.toLocalTime());
			return new DateTime(d, t);
		}
		return null;
	}

}
