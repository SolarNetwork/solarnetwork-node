/* ==================================================================
 * DataUtils.java - 4/08/2018 9:20:59 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for parsing test data files, commonly captured from devices via
 * other tools for use in unit tests.
 * 
 * @author matt
 * @version 1.1
 */
public final class DataUtils {

	/**
	 * Parse Modbus register lines encoded in hex.
	 * 
	 * <p>
	 * This method expects the lines to contain a single hex integer value at
	 * the end (excluding whitespace) but can be proceeded by anything. For
	 * example the output of the {@literal mbpoll} command produces output like
	 * this, which can be parsed by this method:
	 * </p>
	 * 
	 * <pre>
	 * [0]:    0x4031
	 * [1]:    0x0632
	 * [2]:    0x01F0
	 * </pre>
	 * 
	 * <p>
	 * Internally this method calls
	 * {@link #parseIntLines(BufferedReader, Pattern, int)} so lines commented
	 * with a {@literal #} character are ignored.
	 * </p>
	 * 
	 * @param in
	 *        the input to read from
	 * @return the parsed values
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public static int[] parseModbusHexRegisterLines(BufferedReader in) throws IOException {
		Pattern pat = Pattern.compile(".*0x([0-9A-Fa-f]+)\\s*");
		return parseIntLines(in, pat, 16);
	}

	/**
	 * Parse hex encoded integers from lines.
	 * 
	 * <p>
	 * This method will read all lines from the given {@code Reader}, skip those
	 * starting with a {@literal #} character or not matching {@code pat}, and
	 * then parse the first matching group from {@code pat} as an integer
	 * string.
	 * </p>
	 * 
	 * @param in
	 *        the input to read lines from
	 * @param pat
	 *        the pattern to match a single integer per line, with the first
	 *        matching group matching the integer value
	 * @param radix
	 *        the radix to parse the number as
	 * @return the parsed integer values
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public static int[] parseIntLines(BufferedReader in, Pattern pat, int radix) throws IOException {
		return in.lines().filter(s -> !s.startsWith("#") && pat.matcher(s).matches()).mapToInt(s -> {
			Matcher m = pat.matcher(s);
			if ( m.matches() ) {
				return Integer.parseInt(m.group(1), radix);
			}
			return -1;
		}).toArray();
	}

	/**
	 * Parse Modbus register lines encoded in hex.
	 * 
	 * <p>
	 * This method expects the lines to contain a single decimal integer
	 * register number followed by a hexadecimal integer register value at the
	 * end (excluding whitespace), as output by the {@literal mbpoll} command.
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * [0]:    0x4031
	 * [1]:    0x0632
	 * [2]:    0x01F0
	 * </pre>
	 * 
	 * <p>
	 * Internally this method calls
	 * {@link #parseIntKeyIntValueMappingLines(BufferedReader, Pattern, int, int)}
	 * so lines commented with a {@literal #} character are ignored.
	 * </p>
	 * 
	 * @param in
	 *        the input to read from
	 * @return the parsed values
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public static Map<Integer, Integer> parseModbusHexRegisterMappingLines(BufferedReader in)
			throws IOException {
		Pattern pat = Pattern.compile("\\s*\\[(\\d+)\\].*0x([0-9A-Fa-f]+)\\s*");
		return parseIntKeyIntValueMappingLines(in, pat, 10, 16);
	}

	/**
	 * Parse integer key/value pairs from lines.
	 * 
	 * <p>
	 * This method will read all lines from the given {@code Reader}, skip those
	 * starting with a {@literal #} character or not matching {@code pat}, and
	 * then parse the first matching group from {@code pat} as an integer key
	 * and the second matching group as a integer value.
	 * </p>
	 * 
	 * @param in
	 *        the input to read lines from
	 * @param pat
	 *        the pattern to match a single hexadecimal integer per line, with
	 *        the first matching group matching the key and the second the value
	 * @param keyRadix
	 *        the radix of the key integer string
	 * @param valueRadix
	 *        the radix of the value integer string
	 * @return the parsed integer values
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public static Map<Integer, Integer> parseIntKeyIntValueMappingLines(BufferedReader in, Pattern pat,
			int keyRadix, int valueRadix) throws IOException {
		Map<Integer, Integer> result = new LinkedHashMap<>();
		in.lines().filter(s -> !s.startsWith("#") && pat.matcher(s).matches()).forEach(s -> {
			Matcher m = pat.matcher(s);
			if ( m.matches() ) {
				result.put(Integer.parseInt(m.group(1), keyRadix),
						Integer.valueOf(m.group(2), valueRadix));
			}
		});
		return result;
	}

	/**
	 * Extract an array of integer values based on a range of keys in a map.
	 * 
	 * @param data
	 *        the data map
	 * @param start
	 *        the starting address (map key)
	 * @param len
	 *        the length of the output slice
	 * @return the slice of data
	 * @since 1.1
	 */
	public static int[] mapSlice(Map<Integer, Integer> data, int start, int len) {
		int[] slice = new int[len];
		for ( int i = start, end = start + len; i < end; i++ ) {
			Integer k = i;
			Integer v = data.get(k);
			slice[i - start] = (v != null ? v.intValue() : 0);
		}
		return slice;
	}

}
