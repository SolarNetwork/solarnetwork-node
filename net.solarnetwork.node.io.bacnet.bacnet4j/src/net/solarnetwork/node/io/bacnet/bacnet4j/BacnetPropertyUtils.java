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

import java.math.BigInteger;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import net.solarnetwork.util.NumberUtils;

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

}
