/* ==================================================================
 * BasicPlatformPackage.java - 24/05/2019 4:16:40 pm
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

package net.solarnetwork.node.service.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.node.service.PlatformPackageService.PlatformPackage;

/**
 * Basic immutable implementation of {@link PlatformPackage}.
 *
 * @author matt
 * @version 1.1
 * @since 1.68
 */
public class BasicPlatformPackage implements PlatformPackage {

	private final String name;
	private final String version;
	private final boolean installed;

	/**
	 * Constructor.
	 *
	 * @param name
	 *        the package name
	 * @param version
	 *        the package version
	 * @param installed
	 *        {@literal true} if installed
	 */
	@JsonCreator
	public BasicPlatformPackage(@JsonProperty("name") String name,
			@JsonProperty("version") String version, @JsonProperty("installed") boolean installed) {
		super();
		this.name = name;
		this.version = version;
		this.installed = installed;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public boolean isInstalled() {
		return installed;
	}

}
