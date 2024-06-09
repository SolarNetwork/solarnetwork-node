/* ==================================================================
 * SettingsImportOptions.java - Feb 19, 2015 3:59:44 PM
 *
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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
 * Options for importing settings.
 *
 * @author matt
 * @version 1.1
 */
public class SettingsImportOptions {

	private boolean addOnly;

	/**
	 * Default constructor.
	 */
	public SettingsImportOptions() {
		super();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SettingsImportOptions{addOnly=");
		builder.append(addOnly);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the "add only" flag.
	 *
	 * @return the flag
	 */
	public boolean isAddOnly() {
		return addOnly;
	}

	/**
	 * Set the "add only" flag.
	 *
	 * @param addOnly
	 *        the flag value to set
	 */
	public void setAddOnly(boolean addOnly) {
		this.addOnly = addOnly;
	}

}
