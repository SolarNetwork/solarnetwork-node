/* ==================================================================
 * TitleSettingSpecifier.java - Mar 12, 2012 9:36:14 AM
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

import java.util.Map;

/**
 * A read-only string setting.
 * 
 * @author matt
 * @version $Revision$
 */
public interface TitleSettingSpecifier extends KeyedSettingSpecifier<String> {

	/**
	 * An optional mapping of possible values for this setting to associated titles.
	 * 
	 * <p>This can be used to display user-friendly titles for setting
	 * values if the setting value itself is cryptic.</p>
	 * 
	 * @return
	 */
	Map<String, String> getValueTitles();
	
}
