/* ==================================================================
 * JsonUtils.java - 15/11/2019 11:16:06 am
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

package net.solarnetwork.node.hw.gpsd.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import com.fasterxml.jackson.core.JsonGenerator;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.util.NumberUtils;

/**
 * Utility methods for dealing with GPSd JSON.
 * 
 * @author matt
 * @version 1.0
 */
public final class JsonUtils {

	/**
	 * Write a number field value using the smallest possible number type.
	 * 
	 * <p>
	 * If {@code value} is {@literal null} then <b>nothing</b> will be
	 * generated.
	 * </p>
	 * 
	 * @param gen
	 *        the JSON generator
	 * @param fieldName
	 *        the field name
	 * @param value
	 *        the number value
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public static void writeNumberField(JsonGenerator gen, String fieldName, Number value)
			throws IOException {
		if ( value == null ) {
			return;
		}
		if ( value instanceof Double ) {
			gen.writeNumberField(fieldName, (Double) value);
		} else if ( value instanceof Float ) {
			gen.writeNumberField(fieldName, (Float) value);
		} else if ( value instanceof Long ) {
			gen.writeNumberField(fieldName, (Long) value);
		} else if ( value instanceof Integer ) {
			gen.writeNumberField(fieldName, (Integer) value);
		} else if ( value instanceof Short ) {
			gen.writeFieldName(fieldName);
			gen.writeNumber((Short) value);
		} else if ( value instanceof BigInteger ) {
			gen.writeFieldName(fieldName);
			gen.writeNumber((BigInteger) value);
		} else {
			BigDecimal d = NumberUtils.bigDecimalForNumber(value);
			if ( d != null ) {
				gen.writeNumberField(fieldName, d);
			}
		}
	}

	/**
	 * Write a timestamp field value in ISO 8601 form.
	 * 
	 * <p>
	 * If {@code value} is {@literal null} then <b>nothing</b> will be
	 * generated.
	 * </p>
	 * 
	 * @param gen
	 *        the JSON generator
	 * @param fieldName
	 *        the field name
	 * @param value
	 *        the instant value
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public static void writeIso8601Timestamp(JsonGenerator gen, String fieldName, Instant value)
			throws IOException {
		if ( value == null ) {
			return;
		}
		gen.writeStringField(fieldName, DateTimeFormatter.ISO_INSTANT.format(value));
	}

	/**
	 * Write a bitmask set as a field number value.
	 * 
	 * <p>
	 * If {@code value} is {@literal null} or empty then <b>nothing</b> will be
	 * generated.
	 * </p>
	 * 
	 * @param gen
	 *        the JSON generator
	 * @param fieldName
	 *        the field name
	 * @param value
	 *        the instant value
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public static void writeBitmaskValue(JsonGenerator gen, String fieldName,
			Set<? extends Bitmaskable> value) throws IOException {
		int v = Bitmaskable.bitmaskValue(value);
		if ( v > 0 ) {
			gen.writeNumberField(fieldName, v);
		}
	}

	/**
	 * Parse an ISO 8601 timestamp value into an {@link Instant}.
	 * 
	 * @param timestamp
	 *        the timestamp value
	 * @return the instant, or {@literal null} if {@code timestamp} is
	 *         {@literal null}, empty, or cannot be parsed
	 */
	public static Instant iso8610Timestamp(String timestamp) {
		Instant ts = null;
		if ( timestamp != null && !timestamp.isEmpty() ) {
			try {
				ts = Instant.parse(timestamp);
			} catch ( DateTimeParseException e ) {
				// ignore
			}
		}
		return ts;
	}

}
