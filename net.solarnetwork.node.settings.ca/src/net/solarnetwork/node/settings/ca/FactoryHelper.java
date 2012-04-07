/* ==================================================================
 * FactoryHelper.java - Mar 23, 2012 3:13:48 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.settings.ca;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingSpecifierProviderFactory;

/**
 * Helper class for managing factory providers.
 * 
 * @author matt
 * @version $Revision$
 */
public class FactoryHelper {

	private final SettingSpecifierProviderFactory factory;
	private final Map<String, List<SettingSpecifierProvider>> instanceMap = new TreeMap<String, List<SettingSpecifierProvider>>();

	public FactoryHelper(SettingSpecifierProviderFactory factory) {
		super();
		this.factory = factory;
	}

	public SettingSpecifierProviderFactory getFactory() {
		return factory;
	}

	/**
	 * Get a list of providers for a given instance ID, creating an empty list
	 * if one doesn't exist yet.
	 * 
	 * @param instanceUID
	 *            the ID of the instance to get the list of providers for
	 * @return instance list, never <em>null</em>
	 */
	public List<SettingSpecifierProvider> getInstanceProviders(String instanceUID) {
		synchronized (instanceMap) {
			List<SettingSpecifierProvider> results = instanceMap.get(instanceUID);
			if ( results == null ) {
				results = new ArrayList<SettingSpecifierProvider>(5);
				instanceMap.put(instanceUID, results);
			}
			return results;
		}
	}

	public Set<Map.Entry<String, List<SettingSpecifierProvider>>> instanceEntrySet() {
		return instanceMap.entrySet();
	}

	public void removeProvider(SettingSpecifierProvider provider) {
		synchronized (instanceMap) {
			for ( List<SettingSpecifierProvider> providerList : instanceMap.values() ) {
				for ( Iterator<SettingSpecifierProvider> itr = providerList.iterator(); itr
						.hasNext(); ) {
					SettingSpecifierProvider oneProvider = itr.next();
					if ( oneProvider.equals(provider) ) {
						itr.remove();
						return;
					}
				}
			}
		}
	}

}
