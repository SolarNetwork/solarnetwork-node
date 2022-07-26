/* ==================================================================
 * SerialParametersBean.java - Jul 12, 2013 9:52:18 AM
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

package net.solarnetwork.node.io.modbus.j2mod;

import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * A JavaBean wrapper around SerialParameters to make working with dependency
 * inject easier.
 * 
 * <p>
 * The {@link SerialParameters} class defines many JavaBean-esque accessor
 * methods, but violates the JavaBeans contract in some cases where different
 * method types are used on the same bean property. For example, the
 * <i>parity</i> property has {#link {@link SerialParameters#getParity()} that
 * returns an <i>int</i> and {@link SerialParameters#setParity(String)} as well
 * as {@link SerialParameters#setParity(int)}. This breaks JavaBean access in
 * some JVMs.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class SerialParametersBean extends SerialParameters {

	/**
	 * Set the parity as a String value.
	 * 
	 * @param parity
	 *        the parity string value to set
	 */
	public void setParityString(String parity) {
		setParity(parity);
	}

}
