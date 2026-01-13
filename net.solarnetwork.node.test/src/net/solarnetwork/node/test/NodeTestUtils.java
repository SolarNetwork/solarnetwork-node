/* ==================================================================
 * NodeTestUtils.java - 5/06/2025 11:57:45 am
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

package net.solarnetwork.node.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.UUID;
import org.springframework.util.FileCopyUtils;

/**
 * Node test utilities.
 *
 * @author matt
 * @version 1.1
 * @since 2.0
 */
public final class NodeTestUtils {

	/** A random number generator. */
	public static final SecureRandom RNG = new SecureRandom();

	/**
	 * Load test environment properties.
	 *
	 * @return the properties
	 */
	public static Properties loadEnvironmentProperties() {
		Properties props = new Properties();
		try (InputStream in = NodeTestUtils.class.getClassLoader()
				.getResourceAsStream("env.properties")) {
			props.load(in);
		} catch ( IOException e ) {
			// we'll ignore this
		}
		return props;
	}

	/**
	 * Load a UTF-8 string classpath resource.
	 *
	 * @param resource
	 *        the resource to load
	 * @param clazz
	 *        the class from which to load the resource
	 * @return the resource
	 * @throws UncheckedIOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	public static String utf8StringResource(String resource, Class<?> clazz) {
		try {
			return FileCopyUtils.copyToString(
					new InputStreamReader(clazz.getResourceAsStream(resource), StandardCharsets.UTF_8));
		} catch ( IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Get a random decimal number.
	 *
	 * @return the random decimal number
	 * @since 1.1
	 */
	public static BigDecimal randomDecimal() {
		return new BigDecimal(RNG.nextDouble(-1000.0, 1000.0)).setScale(4, RoundingMode.HALF_UP);
	}

	/**
	 * Get a random string value.
	 *
	 * @return the string
	 * @since 1.1
	 */
	public static String randomString() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 14);
	}

	/**
	 * Get a random string value of an arbitrary length.
	 *
	 * @return the string
	 * @since 1.1
	 */
	public static String randomString(int len) {
		StringBuilder buf = new StringBuilder();
		while ( buf.length() < len ) {
			buf.append(UUID.randomUUID().toString().replace("-", ""));
		}
		buf.setLength(len);
		return buf.toString();
	}

	/**
	 * Get a random positive integer value.
	 *
	 * @return the integer
	 * @since 1.1
	 */
	public static Integer randomInt() {
		return RNG.nextInt(1, Integer.MAX_VALUE);
	}

	/**
	 * Get a random positive long value.
	 *
	 * @return the long
	 * @since 1.1
	 */
	public static Long randomLong() {
		return RNG.nextLong(1, Long.MAX_VALUE);
	}

	/**
	 * Get a random boolean value.
	 *
	 * @return the boolean
	 * @since 1.1
	 */
	public static boolean randomBoolean() {
		return RNG.nextBoolean();
	}

	/**
	 * Get 16 random bytes.
	 *
	 * @return the random bytes
	 * @since 1.1
	 */
	public static byte[] randomBytes() {
		return randomBytes(16);
	}

	/**
	 * Get random bytes.
	 *
	 * @param len
	 *        the desired number of bytes
	 * @return random bytes, of length {@code len}
	 * @since 1.1
	 */
	public static byte[] randomBytes(int len) {
		byte[] bytes = new byte[len];
		RNG.nextBytes(bytes);
		return bytes;
	}

}
