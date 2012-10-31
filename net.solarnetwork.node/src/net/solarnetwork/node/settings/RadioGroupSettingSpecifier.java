/* ==================================================================
 * RadioGroupSettingSpecifier.java - Mar 12, 2012 9:47:17 AM
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
 * Similar to the {@link MultiValueSettingSpecifier} except that
 * these settings are displayed in-line.
 * 
 * <p>The {@link #getValueTitles()} is required for this setting, and 
 * provides the list of possible setting values for the user to choose from.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public interface RadioGroupSettingSpecifier extends TextFieldSettingSpecifier {

	/**
	 * Localizable text to display at the end of the group's content.
	 * 
	 * @return localizable 
	 */
	String getFooterText();
	
}
