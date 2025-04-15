/* ==================================================================
 * LocalStateType.java - 14/04/2025 7:35:05 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * A local state data type.
 *
 * @author matt
 * @version 1.0
 * @since 3.23
 */
public enum LocalStateType {

	/** A boolean. */
	Boolean('b', 1),

	/** A 32-bit signed integer. */
	Int32('i', java.lang.Integer.BYTES),

	/** A 64-bit signed integer. */
	Int64('l', Long.BYTES),

	/** An arbitrary precision integer. */
	Integer('I', 0),

	/** A 32-bit floating point. */
	Float32('f', Float.BYTES),

	/** A 64-bit floating point. */
	Float64('d', Double.BYTES),

	/** An arbitrary precision floating point. */
	Decimal('D', 0),

	/** A UTF-8 encoded string. */
	String('s', 0),

	/** A map with string keys. */
	Mapping('m', 0),

	;

	private static final ObjectMapper MAPPER = JsonUtils.newObjectMapper();

	private static final Logger log = LoggerFactory.getLogger(LocalStateType.class);

	private final char key;
	private final int size;

	private LocalStateType(char key, int size) {
		this.key = key;
		this.size = size;
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public char getKey() {
		return key;
	}

	/**
	 * Encode a value for this type.
	 *
	 * @param value
	 *        the value to encode
	 * @return the encoded value, or {@code null} if {@code value} is
	 *         {@code null} or cannot be encoded for any reason
	 * @see #decode(Object)
	 */
	public byte[] encode(Object value) {
		if ( value == null ) {
			return null;
		}
		try {
			if ( this == LocalStateType.String ) {
				return value.toString().getBytes(UTF_8);
			} else if ( this == LocalStateType.Mapping ) {
				Map<String, Object> map = JsonUtils.getStringMapFromObject(value);
				return MAPPER.writeValueAsBytes(map);
			} else if ( this == LocalStateType.Integer ) {
				BigInteger d = null;
				if ( value instanceof Number ) {
					d = NumberUtils.bigIntegerForNumber((Number) value);
				} else {
					d = new BigInteger(value.toString());
				}
				return d.toByteArray();
			} else if ( this == LocalStateType.Decimal ) {
				BigDecimal d = null;
				if ( value instanceof Number ) {
					d = NumberUtils.bigDecimalForNumber((Number) value);
				} else {
					d = new BigDecimal(value.toString());
				}
				return d.toString().getBytes(UTF_8);
			} else if ( this == LocalStateType.Boolean ) {
				// @formatter:off
				boolean b = (value instanceof Boolean
						? ((Boolean)value).booleanValue()
						: value instanceof Number
						? ((Number)value).intValue() != 0
						: StringUtils.parseBoolean(value.toString()));
				// @formatter:on
				return new byte[] { b ? (byte) 1 : (byte) 0 };
			} else {
				// number types
				BigDecimal d = null;
				if ( value instanceof Number ) {
					d = NumberUtils.bigDecimalForNumber((Number) value);
				} else {
					d = new BigDecimal(value.toString());
				}
				ByteBuffer buf = ByteBuffer.allocate(size);
				switch (this) {
					case Int32:
						buf.putInt(d.intValue());
						break;

					case Int64:
						buf.putLong(d.longValue());
						break;

					case Float32:
						buf.putFloat(d.floatValue());
						break;

					case Float64:
						buf.putDouble(d.doubleValue());
						break;

					default:
						// should not be here
						return null;
				}
				buf.rewind();
				return buf.array();
			}
		} catch ( Exception e ) {
			log.warn("Exception encoding state type {} value [{}]: {}", this, value, e.toString());
		}
		return null;
	}

	/**
	 * Decode data previously encoded for this type.
	 *
	 * @param data
	 *        the data to decode
	 * @return the decoded value, or {@code null} if {@code data} is
	 *         {@code null} or cannot be decoded for any reason
	 * @see #encode(Object)
	 */
	public Object decode(byte[] data) {
		if ( data == null || data.length == 0 ) {
			return null;
		}
		try {
			if ( this == LocalStateType.String ) {
				return new String(data, UTF_8);
			} else if ( this == LocalStateType.Mapping ) {
				try {
					return MAPPER.readValue(data, JsonUtils.STRING_MAP_TYPE);
				} catch ( IOException e ) {
					return null;
				}
			} else if ( this == LocalStateType.Integer ) {
				return new BigInteger(data);
			} else if ( this == LocalStateType.Decimal ) {
				return new BigDecimal(new String(data, UTF_8));
			} else if ( this == LocalStateType.Boolean ) {
				return data[0] != 0;
			} else {
				// number types
				ByteBuffer buf = ByteBuffer.wrap(data);
				switch (this) {
					case Int32:
						return buf.getInt();

					case Int64:
						return buf.getLong();

					case Float32:
						return buf.getFloat();

					case Float64:
						return buf.getDouble();

					default:
						// should not be here
				}
			}
		} catch ( Exception e ) {
			log.warn("Exception decoding state type {} data [{}]: {}", this,
					ByteUtils.encodeHexString(data, 0, data.length, false), e.toString());
		}
		return null;
	}

	/**
	 * Get an enumeration value for a key or name value.
	 *
	 * @param val
	 *        can be a {@link Character} or {@link Integer} {@code key} value,
	 *        or a case-insensitive enumeration name string
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if {@code val} is not supported
	 * @throws NullPointerException
	 *         if {@code val} is {@code null}
	 */
	public static LocalStateType forKey(Object val) {
		assert val != null;
		Character key = null;
		String s = null;
		if ( val instanceof Character ) {
			key = (Character) val;
		} else if ( val instanceof Integer ) {
			key = (char) ((Integer) val).intValue();
		} else {
			s = val.toString();
			if ( s.length() == 1 ) {
				key = s.charAt(0);
			}
		}
		for ( LocalStateType e : LocalStateType.values() ) {
			if ( key != null && key.charValue() == e.key ) {
				return e;
			}
			if ( s.equalsIgnoreCase(e.name()) ) {
				return e;
			}

		}
		throw new IllegalArgumentException("Unsupported LocalStateType value [" + val + "]");
	}

	/**
	 * Detect the best-fit type to use for a given object value.
	 *
	 * @param value
	 *        the value to test
	 * @return the best-fit type, never {@code null}
	 */
	public static LocalStateType detect(Object value) {
		if ( value == null ) {
			return LocalStateType.String;
		}
		if ( value instanceof Short || value instanceof Integer ) {
			return LocalStateType.Int32;
		} else if ( value instanceof Long ) {
			return LocalStateType.Int64;
		} else if ( value instanceof Float ) {
			return LocalStateType.Float32;
		} else if ( value instanceof Double ) {
			return LocalStateType.Float64;
		} else if ( value instanceof BigInteger ) {
			return LocalStateType.Integer;
		} else if ( value instanceof Number ) {
			return LocalStateType.Decimal;
		} else if ( value instanceof String ) {
			return LocalStateType.String;
		}
		return LocalStateType.Mapping;
	}

}
