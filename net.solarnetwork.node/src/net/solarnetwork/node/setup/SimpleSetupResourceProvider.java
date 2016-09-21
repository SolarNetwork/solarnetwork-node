/* ==================================================================
 * SimpleSetupResourceProvider.java - 21/09/2016 7:30:18 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Basic implementation of {@link SetupResourceProvider} for serving static
 * content.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleSetupResourceProvider implements SetupResourceProvider {

	private List<SetupResource> resources;

	@Override
	public SetupResource getSetupResource(String resourceUID) {
		if ( resources == null ) {
			return null;
		}
		for ( SetupResource rsrc : resources ) {
			if ( resourceUID.equals(rsrc.getResourceUID()) ) {
				return rsrc;
			}
		}
		return null;
	}

	@Override
	public List<SetupResource> getSetupResourcesForConsumer(String consumerType) {
		List<SetupResource> result;
		if ( resources == null ) {
			result = Collections.emptyList();
		} else {
			result = new ArrayList<SetupResource>(resources.size());
			for ( SetupResource rsrc : resources ) {
				Set<String> supported = rsrc.getSupportedConsumerTypes();
				if ( supported == null || supported.contains(consumerType) ) {
					result.add(rsrc);
				}
			}
		}
		return result;
	}

	public List<SetupResource> getResources() {
		return resources;
	}

	public void setResources(List<SetupResource> resources) {
		this.resources = resources;
	}

}
