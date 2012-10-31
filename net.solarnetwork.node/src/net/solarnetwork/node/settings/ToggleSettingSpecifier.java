/* ==================================================================
 * ToggleSettingSpecifier.java - Mar 12, 2012 9:25:52 AM
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
 * A logically boolean toggle setting.
 * 
 * @author matt
 * @version $Revision$
 */
public interface ToggleSettingSpecifier extends KeyedSettingSpecifier<Object> {

	/**
	 * Get the "true" value for this setting.
	 * 
	 * <p>If this returns <em>null</em> then {@link Boolean#TRUE} is assumed.</p>
	 * 
	 * @return the "true" value
	 */
	Object getTrueValue();
	
	/**
	 * Get the "false" value for this setting
	 * 
	 * <p>If this returns <em>null</em> then {@link Boolean#FALSE} is assumed.</p>
	 * 
	 * @return the "false" value
	 */
	Object getFalseValue();
	
}
