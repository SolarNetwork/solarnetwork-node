/* ==================================================================
 * VirtualMeterExpressionRoot.java - 9/05/2021 11:31:31 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.samplefilter;

import java.math.BigDecimal;
import java.util.Map;
import net.solarnetwork.node.domain.DatumExpressionRoot;

/**
 * API for a virtual meter expression root object.
 * 
 * @author matt
 * @version 1.0
 * @since 1.6
 */
public interface VirtualMeterExpressionRoot extends DatumExpressionRoot {

	/**
	 * Get the virtual meter configuration.
	 * 
	 * @return the configuration
	 */
	VirtualMeterConfig getConfig();

	/**
	 * Get the metadata.
	 * 
	 * @return the metadata
	 */
	Map<String, ?> getMetadata();

	/**
	 * Get the previous date.
	 * 
	 * @return the date, as an epoch
	 */
	long getPrevDate();

	/**
	 * Get the current date.
	 * 
	 * @return the date, as an epoch
	 */
	long getCurrDate();

	/**
	 * Get the previous input property value.
	 * 
	 * @return the previous property value
	 */
	BigDecimal getPrevVal();

	/**
	 * Get the current input property value.
	 * 
	 * @return the current property value
	 */
	BigDecimal getCurrVal();

}
