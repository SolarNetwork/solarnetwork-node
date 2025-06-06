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
import java.util.Properties;

/**
 * Node test utilities.
 *
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public final class NodeTestUtils {

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

}
