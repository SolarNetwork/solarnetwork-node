/* ==================================================================
 * SliderSettingSpecifier.java - Mar 12, 2012 9:33:48 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.settings;

/**
 * A floating point range setting between a minimum and maximum value.
 * 
 * @author matt
 * @version $Revision$
 */
public interface SliderSettingSpecifier extends KeyedSettingSpecifier<Double> {

	/**
	 * The minimum value allowed.
	 * 
	 * <p>If <em>null</em> then <code>0.0</code> is assumed.</p>
	 * 
	 * @return the minimum value
	 */
	Double getMinimumValue();
	
	/**
	 * The maximum value allowed.
	 * 
	 * <p>If <em>null</em> then <code>1.0</code> is assumed.</p>
	 * 
	 * @return the maximum value
	 */
	Double getMaximumValue();
	
	/**
	 * Get a step value for acceptable values between the minimum and maximum.
	 * 
	 * <p>
	 * If <em>null</em> then <code>1.0</code> is assumed.
	 * </p>
	 * 
	 * @return the step value
	 */
	Double getStep();

}
