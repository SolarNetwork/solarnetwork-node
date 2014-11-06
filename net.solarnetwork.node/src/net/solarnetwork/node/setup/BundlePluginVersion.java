/* ==================================================================
 * BundlePluginVersion.java - Apr 21, 2014 5:38:23 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup;

import org.osgi.framework.Version;

/**
 * PluginVersion implementation that wraps an OSGi {@link Version}.
 * 
 * @author matt
 * @version 1.0
 */
public class BundlePluginVersion implements PluginVersion {

	private final Version version;

	/**
	 * Construct with a {@link Version}.
	 * 
	 * @param version
	 *        the Version to wrap
	 */
	public BundlePluginVersion(Version version) {
		super();
		this.version = version;
	}

	@Override
	public int compareTo(PluginVersion o) {
		if ( !(o instanceof BundlePluginVersion) ) {
			throw new IllegalArgumentException("Only BundlePluginVersion supported");
		}
		return this.version.compareTo(((BundlePluginVersion) o).version);
	}

	@Override
	public int getMajor() {
		return version.getMajor();
	}

	@Override
	public int getMinor() {
		return version.getMinor();
	}

	@Override
	public int getMicro() {
		return version.getMicro();
	}

	@Override
	public String getQualifier() {
		return version.getQualifier();
	}

	@Override
	public String asNormalizedString() {
		return version.toString();
	}

	@Override
	public String toString() {
		return asNormalizedString();
	}
}
