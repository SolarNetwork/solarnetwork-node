/* ==================================================================
 * TextFieldSettingSpecifier.java - Mar 12, 2012 9:41:06 AM
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

/**
 * A read-write string setting.
 * 
 * @author matt
 * @version 1.1
 */
public interface TextFieldSettingSpecifier extends TitleSettingSpecifier {

	/**
	 * Flag indicating the text should be hidden when editing.
	 * 
	 * @return <em>true</em> to hide the text
	 * @since 1.1
	 */
	boolean isSecureTextEntry();

}
