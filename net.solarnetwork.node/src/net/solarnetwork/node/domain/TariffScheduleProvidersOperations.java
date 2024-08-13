/* ==================================================================
 * TariffScheduleProvidersOperations.java - 6/08/2024 8:56:33 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import static java.util.Collections.emptyList;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import java.util.ArrayList;
import java.util.Collection;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.node.service.TariffScheduleProvider;
import net.solarnetwork.service.OptionalServiceCollection;

/**
 * Operations on a collection of {@link TariffScheduleProvider} services.
 *
 * @author matt
 * @version 1.0
 * @since 3.17
 */
public interface TariffScheduleProvidersOperations {

	/**
	 * Get a collection of available tariff schedule providers.
	 *
	 * @return the providers collection, possibly {@literal null}
	 */
	OptionalServiceCollection<TariffScheduleProvider> getTariffScheduleProviders();

	/**
	 * Resolve a tariff schedule for a given UID.
	 *
	 * @param uid
	 *        the UID of the schedule to resolve
	 * @return the schedule from the first available provider whose
	 *         {@link TariffScheduleProvider#getUid()} matches {@code uid} in a
	 *         case-insensitive manner, or {@literal null} if no match is
	 *         available or the matching provider returns a {@code null}
	 *         schedule
	 */
	default TariffSchedule tariffSchedule(String uid) {
		if ( uid == null || uid.isEmpty() ) {
			return null;
		}
		Iterable<TariffScheduleProvider> providers = services(getTariffScheduleProviders());
		if ( providers == null ) {
			return null;
		}
		for ( TariffScheduleProvider p : providers ) {
			if ( uid.equalsIgnoreCase(p.getUid()) ) {
				return p.tariffSchedule();
			}
		}
		return null;
	}

	/**
	 * Resolve a collection of tariff schedules for a given group UID.
	 *
	 * @param groupUid
	 *        the group UID of the schedules to resolve
	 * @return the schedules from every provider whose
	 *         {@link TariffScheduleProvider#getGroupUid()} matches
	 *         {@code groupUid} in a case-insensitive manner, of an empty
	 *         collection if none available
	 */
	default Collection<TariffSchedule> tariffScheduleGroup(String groupUid) {
		if ( groupUid == null || groupUid.isEmpty() ) {
			return emptyList();
		}
		Iterable<TariffScheduleProvider> providers = services(getTariffScheduleProviders());
		if ( providers == null ) {
			return null;
		}
		Collection<TariffSchedule> result = null;
		for ( TariffScheduleProvider p : providers ) {
			if ( groupUid.equalsIgnoreCase(p.getGroupUid()) ) {
				TariffSchedule s = p.tariffSchedule();
				if ( s != null ) {
					if ( result == null ) {
						result = new ArrayList<>(4);
					}
					result.add(s);
				}
			}
		}
		return (result != null ? result : emptyList());
	}

}
