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

		/** Install a package. */
		Install,

		/** Remove a package. */
		Remove,

		/** Upgrade a package or all packages. */
		Upgrade;

	}

	private Action action;
	private String name;
	private String version;
	private String[] arguments;

	/**
	 * Constructor.
	 */
	public S3SetupPackageConfiguration() {
		super();
	}

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

	/**
	 * Get the action.
	 *
	 * @return the action
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * Set the action.
	 *
	 * @param action
	 *        the action to set
	 */
	public void setAction(Action action) {
		this.action = action;
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the version.
	 *
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the version.
	 *
	 * @param version
	 *        the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Get the arguments.
	 *
	 * @return the arguments
	 */
	public String[] getArguments() {
		return arguments;
	}

	/**
	 * Set the arguments.
	 *
	 * @param arguments
	 *        the arguments to set
	 */
	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

}
