/* ==================================================================
 * DebianPlatformPackageService.java - 23/05/2019 10:03:58 am
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

package net.solarnetwork.node.setup.deb;

import java.util.regex.Pattern;
import net.solarnetwork.node.PlatformPackageService;
import net.solarnetwork.node.support.BaseSolarPkgPlatformPackageService;

/**
 * Implementation of {@link PlatformPackageService} for Debian packages.
 * 
 * @author matt
 * @version 1.0
 */
public class DebianPlatformPackageService extends BaseSolarPkgPlatformPackageService {

	private static final Pattern DEBIAN_PACKAGE_PAT = Pattern.compile("\\.deb$");

	@Override
	public boolean handlesPackage(String archiveFileName) {
		return archiveFileName != null && DEBIAN_PACKAGE_PAT.matcher(archiveFileName).find();
	}

}
