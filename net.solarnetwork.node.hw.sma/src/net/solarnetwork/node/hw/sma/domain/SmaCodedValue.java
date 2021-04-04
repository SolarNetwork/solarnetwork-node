/* ==================================================================
 * SmaCodedValue.java - 14/09/2020 8:22:29 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.domain;

import net.solarnetwork.domain.CodedValue;

/**
 * Extension of {@link CodedValue} to include a description.
 * 
 * <p>
 * SMA has defined many enumeration constants as "common status code" values,
 * and other enumeration values exist but may or may not be included in
 * {@link SmaCommonStatusCode}. Using this API allows other enums to be added in
 * the future, if it makes sense to define them.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface SmaCodedValue extends CodedValue {

	/**
	 * Get a friendly description.
	 * 
	 * @return the description
	 */
	String getDescription();

}
