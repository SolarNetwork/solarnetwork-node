/* ==================================================================
 * KcdParser.java - 2/10/2019 3:57:10 pm
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

package net.solarnetwork.node.io.canbus;

import java.io.IOException;
import java.io.InputStream;
import net.solarnetwork.node.io.canbus.kcd.NetworkDefinitionType;

/**
 * API for a service that can parse a KCD XML document into a
 * {@link NetworkDefinitionType} instance.
 * 
 * @author matt
 * @version 1.0
 */
public interface KcdParser {

	/**
	 * Parse an XML input stream into a {@link NetworkDefinitionType} instance.
	 * 
	 * @param in
	 *        the input stream to read
	 * @param validate
	 *        {@literal true} to validate the XML adheres to the KCD schema
	 * @return the network definition
	 * @throws IOException
	 *         if any IO error occurs
	 * @throws IllegalArgumentException
	 *         if {@code validate} is {@literal true} and the XML is not valid
	 */
	NetworkDefinitionType parseKcd(InputStream in, boolean validate) throws IOException;

}
