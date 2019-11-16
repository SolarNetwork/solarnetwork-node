/* ==================================================================
 * DomainTestSupport.java - 13/11/2019 3:01:34 pm
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

package net.solarnetwork.node.io.gpsd.domain.test;

import java.io.IOException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test support for domain tests.
 * 
 * @author matt
 * @version 1.0
 */
public class DomainTestSupport {

	/**
	 * Read and parse a JSON resource into a tree.
	 * 
	 * @param resource
	 *        the classpath resource to parse
	 * @return the resource
	 * @throws RuntimeException
	 *         if any error occurs
	 */
	protected final TreeNode readJsonResource(String resource) {
		ObjectMapper m = new ObjectMapper();
		m.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		try {
			return m.readTree(getClass().getResourceAsStream(resource));
		} catch ( IOException e ) {
			throw new RuntimeException("Error reading resource [" + resource + "]", e);
		}
	}

}
