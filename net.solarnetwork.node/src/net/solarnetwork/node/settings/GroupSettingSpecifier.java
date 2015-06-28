/* ==================================================================
 * GroupSettingSpecifier.java - Mar 12, 2012 9:15:16 AM
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
 */

package net.solarnetwork.node.settings;

import java.util.List;

/**
 * A grouping of other settings.
 * 
 * @author matt
 * @version 1.1
 */
public interface GroupSettingSpecifier extends SettingSpecifier, MappableSpecifier {

	/**
	 * Get the key for this setting.
	 * 
	 * @return the key to associate with this setting
	 * @since 1.1
	 */
	String getKey();

	/**
	 * Localizable text to display at the end of the group's content.
	 * 
	 * @return localizable
	 */
	String getFooterText();

	/**
	 * Get the settings in this group.
	 * 
	 * @return the list of group settings
	 */
	List<SettingSpecifier> getGroupSettings();

	/**
	 * Get dynamic flag. A dynamic group is one that a user can manage any
	 * number of copies of the group settings, adding and removing as necessary.
	 * 
	 * @return The dynamic flag.
	 * @since 1.1
	 */
	boolean isDynamic();

}
