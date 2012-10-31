/* ==================================================================
 * SettingSpecifier.java - Mar 12, 2012 9:13:02 AM
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
 * Base API for a setting specifier.
 * 
 * <p>
 * This API is generally not used by application code. Rather, the various
 * interfaces that extend this interface are used.
 * </p>
 * 
 * @author matt
 * @version $Revision$
 */
public interface SettingSpecifier {

	/**
	 * Localizable text to display with the setting's content.
	 * 
	 * @return the title
	 */
	String getTitle();
	
}
