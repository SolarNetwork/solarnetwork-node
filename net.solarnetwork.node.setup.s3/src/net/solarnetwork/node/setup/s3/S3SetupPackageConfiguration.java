/* ==================================================================
 * S3SetupPackageConfiguration.java - 24/05/2019 2:19:58 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.s3;

/**
 * Configuration for a single setup package.
 * 
 * @author matt
 * @version 1.1
 * @since 1.1
 */
public class S3SetupPackageConfiguration {

	/**
	 * The package setup actions.
	 */
	public static enum Action {

		Install,

		Remove,

		Upgrade;

	}

	private Action action;
	private String name;
	private String version;
	private String[] arguments;

	/**
	 * Get a description of this configuration.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		if ( version != null ) {
			return name + " " + version;
		}
		return name;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String[] getArguments() {
		return arguments;
	}

	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

}
