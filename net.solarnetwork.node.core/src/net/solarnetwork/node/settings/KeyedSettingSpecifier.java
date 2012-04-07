/* ==================================================================
 * KeyedSettingSpecifier.java - Mar 12, 2012 9:28:55 AM
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
 * A setting specifier that can store a value associated with a key.
 * 
 * @param <T> the type of value stored by this setting
 * @author matt
 * @version $Revision$
 */
public interface KeyedSettingSpecifier<T> extends SettingSpecifier {

	/**
	 * Get the key for this setting.
	 * 
	 * @return the key to associate with this setting
	 */
	String getKey();

	/**
	 * Get the default value for this setting.
	 * 
	 * @return the default value
	 */
	T getDefaultValue();

	/**
	 * Return a setting specifier mapped to a new path.
	 * 
	 * <p>
	 * This is to allow delegating setting specifiers to re-map the key.
	 * </p>
	 * 
	 * @param prefix
	 *            the new prefix to add to the key
	 * @return the new instance
	 */
	SettingSpecifier mappedTo(String prefix);

}
