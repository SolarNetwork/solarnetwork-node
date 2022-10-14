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

import static net.solarnetwork.node.setup.SetupResourceUtils.localeScore;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Basic implementation of {@link SetupResourceProvider} for serving static
 * content.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleSetupResourceProvider implements SetupResourceProvider {

	private Locale defaultLocale = Locale.US;
	private List<SetupResource> resources;

	@Override
	public SetupResource getSetupResource(String resourceUID, Locale locale) {
		if ( resources == null ) {
			return null;
		}
		int bestScore = -1;
		SetupResource bestMatch = null;
		for ( SetupResource rsrc : resources ) {
			if ( resourceUID.equals(rsrc.getResourceUID()) ) {
				int score = localeScore(rsrc, locale, defaultLocale);
				if ( score == Integer.MAX_VALUE ) {
					return rsrc;
				}
				if ( bestMatch == null || score > bestScore ) {
					bestScore = score;
					bestMatch = rsrc;
				}
			}
		}
		return bestMatch;
	}

	@Override
	public Collection<SetupResource> getSetupResourcesForConsumer(String consumerType, Locale locale) {
		Collection<SetupResource> result;
		Map<String, SetupResource> bestMatches;
		if ( resources == null ) {
			result = Collections.emptyList();
		} else {
			bestMatches = new HashMap<String, SetupResource>();
			for ( SetupResource rsrc : resources ) {
				Set<String> supported = rsrc.getSupportedConsumerTypes();
				if ( supported == null || supported.contains(consumerType) ) {
					SetupResource currMatch = bestMatches.get(rsrc.getResourceUID());
					if ( localeScore(currMatch, locale, defaultLocale) < localeScore(rsrc, locale,
							defaultLocale) ) {
						bestMatches.put(rsrc.getResourceUID(), rsrc);
					}
				}
			}
			result = bestMatches.values();
		}

		return result;
	}

	/**
	 * Get the setup resources.
	 * 
	 * @return the resources
	 */
	public List<SetupResource> getResources() {
		return resources;
	}

	/**
	 * Set the list of resources to use.
	 * 
	 * @param resources
	 *        The fixed set of resources managed by this service.
	 */
	public void setResources(List<SetupResource> resources) {
		this.resources = resources;
	}

	/**
	 * Set the locale to use for resources that have no locale specified in
	 * their filename.
	 * 
	 * @param defaultLocale
	 *        The default locale.
	 */
	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

}
