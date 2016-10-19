/* ==================================================================
 * SimpleSetupResourceService.java - 21/09/2016 6:32:01 AM
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Basic implementation of {@link SetupResourceService}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleSetupResourceService implements SetupResourceService {

	private Collection<SetupResourceProvider> setupResourceProviders;

	@Override
	public SetupResource getSetupResource(String resourceUID, Locale locale) {
		for ( SetupResourceProvider provider : getSetupResourceProviders() ) {
			SetupResource r = provider.getSetupResource(resourceUID, locale);
			if ( r != null ) {
				return r;
			}
		}
		return null;
	}

	@Override
	public Collection<SetupResource> getSetupResourcesForConsumer(String consumerType, Locale locale) {
		List<SetupResource> result = new ArrayList<SetupResource>(8);
		for ( SetupResourceProvider provider : getSetupResourceProviders() ) {
			Collection<SetupResource> list = provider.getSetupResourcesForConsumer(consumerType, locale);
			if ( list != null ) {
				result.addAll(list);
			}
		}
		return result;
	}

	public Collection<SetupResourceProvider> getSetupResourceProviders() {
		return setupResourceProviders;
	}

	public void setSetupResourceProviders(Collection<SetupResourceProvider> setupResourceProviders) {
		this.setupResourceProviders = setupResourceProviders;
	}

}
