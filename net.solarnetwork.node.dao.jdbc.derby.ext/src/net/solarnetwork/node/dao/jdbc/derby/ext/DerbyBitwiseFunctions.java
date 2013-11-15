/* ==================================================================
 * DerbyBitwiseFunctions.java - Nov 15, 2013 3:37:29 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.derby.ext;


/**
 * Derby SQL function to perform bitwise and operator.
 * 
 * <p>
 * Derby does not provide a built-in bitwise AND operator. This custom Java
 * function can be used to achieve this by making this class available to the
 * Derby class loader and registering a function, like this:
 * </p>
 * 
 * <pre>
 * CREATE FUNCTION SOLARNODE.BITWISE_AND( parm1 INTEGER, param2 INTEGER )
 *  RETURNS INTEGER
 *  LANGUAGE JAVA
 *  DETERMINISTIC
 *  PARAMETER STYLE JAVA
 *  NO SQL
 *  EXTERNAL NAME 'net.solarnetwork.node.dao.jdbc.derby.ext.DerbyBitwiseFunctions.bitwiseAnd';
 *  
 * CREATE FUNCTION SOLARNODE.BITWISE_OR( parm1 INTEGER, param2 INTEGER )
 *  RETURNS INTEGER
 *  LANGUAGE JAVA
 *  DETERMINISTIC
 *  PARAMETER STYLE JAVA
 *  NO SQL
 *  EXTERNAL NAME 'net.solarnetwork.node.dao.jdbc.derby.ext.DerbyBitwiseFunctions.bitwiseOr';
 * </pre>
 * 
 * @author matt
 * @version 1.0
 */
public final class DerbyBitwiseFunctions {

	/**
	 * Derby function to perform the bitwise AND operation.
	 * 
	 * @param param1
	 *        the first param
	 * @param param2
	 *        the second param
	 * @return the bitwise AND result
	 */
	public final static int bitwiseAnd(int param1, int param2) {
		return param1 & param2;
	}

	/**
	 * Derby function to perform the bitwise OR operation.
	 * 
	 * @param param1
	 *        the first param
	 * @param param2
	 *        the second param
	 * @return the bitwise OR result
	 */
	public final static int bitwiseOr(int param1, int param2) {
		return param1 | param2;
	}

}
