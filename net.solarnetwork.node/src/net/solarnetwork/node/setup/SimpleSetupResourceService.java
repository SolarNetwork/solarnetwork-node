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
import org.jspecify.annotations.Nullable;

/**
 * Basic implementation of {@link SetupResourceService}.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleSetupResourceService implements SetupResourceService {

	private @Nullable Collection<SetupResourceProvider> setupResourceProviders;

	/**
	 * Default constructor.
	 */
	public SimpleSetupResourceService() {
		super();
	}

	@Override
	public @Nullable SetupResource getSetupResource(String resourceUID, @Nullable Locale locale) {
		final Collection<SetupResourceProvider> setupResourceProviders = getSetupResourceProviders();
		if ( setupResourceProviders != null ) {
			for ( SetupResourceProvider provider : setupResourceProviders ) {
				SetupResource r = provider.getSetupResource(resourceUID, locale);
				if ( r != null ) {
					return r;
				}
			}
		}
		return null;
	}

	@Override
	public Collection<SetupResource> getSetupResourcesForConsumer(String consumerType,
			@Nullable Locale locale) {
		List<SetupResource> result = new ArrayList<>(8);
		final Collection<SetupResourceProvider> setupResourceProviders = getSetupResourceProviders();
		if ( setupResourceProviders != null ) {
			for ( SetupResourceProvider provider : setupResourceProviders ) {
				Collection<SetupResource> list = provider.getSetupResourcesForConsumer(consumerType,
						locale);
				if ( list != null ) {
					result.addAll(list);
				}
			}
		}
		return result;
	}

	/**
	 * Get the setup resource providers.
	 *
	 * @return the providers
	 */
	public @Nullable Collection<SetupResourceProvider> getSetupResourceProviders() {
		return setupResourceProviders;
	}

	/**
	 * Set the setup resource providers.
	 *
	 * @param setupResourceProviders
	 *        the providers to set
	 */
	public void setSetupResourceProviders(
			@Nullable Collection<SetupResourceProvider> setupResourceProviders) {
		this.setupResourceProviders = setupResourceProviders;
	}

}
