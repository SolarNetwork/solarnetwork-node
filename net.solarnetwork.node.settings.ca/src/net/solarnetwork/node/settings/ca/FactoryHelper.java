/* ==================================================================
 * FactoryHelper.java - Mar 23, 2012 3:13:48 PM
 * 
 * Copyright 2007 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.settings.ca;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingSpecifierProviderFactory;

/**
 * Helper class for managing factory providers.
 * 
 * @author matt
 * @version 1.1
 */
public class FactoryHelper {

	private final SettingSpecifierProviderFactory factory;
	private final Map<String, List<SettingSpecifierProvider>> instanceMap = new TreeMap<String, List<SettingSpecifierProvider>>();
	private final Map<String, List<SettingResourceHandler>> handlerMap = new TreeMap<String, List<SettingResourceHandler>>();

	public FactoryHelper(SettingSpecifierProviderFactory factory) {
		super();
		this.factory = factory;
	}

	/**
	 * Get the factory.
	 * 
	 * @return the factory
	 */
	public SettingSpecifierProviderFactory getFactory() {
		return factory;
	}

	/**
	 * Get a list of providers for a given instance ID, creating an empty list
	 * if one doesn't exist yet.
	 * 
	 * @param instanceUID
	 *        the ID of the instance to get the list of providers for
	 * @return instance list, never {@literal null}
	 */
	private List<SettingSpecifierProvider> getInstanceProviders(String instanceUID) {
		synchronized ( instanceMap ) {
			List<SettingSpecifierProvider> results = instanceMap.get(instanceUID);
			if ( results == null ) {
				results = new ArrayList<SettingSpecifierProvider>(5);
				instanceMap.put(instanceUID, results);
			}
			return results;
		}
	}

	/**
	 * Add a provider for a given instance ID.
	 * 
	 * @param instanceUID
	 *        the ID of the instance to add the provider to
	 * @param provider
	 *        the provider to add
	 * @since 1.1
	 */
	public void addProvider(String instanceUID, SettingSpecifierProvider provider) {
		synchronized ( handlerMap ) {
			List<SettingSpecifierProvider> list = getInstanceProviders(instanceUID);
			list.add(provider);
		}
	}

	/**
	 * Get a list of handlers for a given instance ID, creating an empty list if
	 * one doesn't exist yet.
	 * 
	 * @param instanceUID
	 *        the ID of the instance to get the list of handlers for
	 * @return handler list, never {@literal null}
	 * @since 1.1
	 */
	private List<SettingResourceHandler> getInstanceHandlers(String instanceUID) {
		synchronized ( handlerMap ) {
			List<SettingResourceHandler> results = handlerMap.get(instanceUID);
			if ( results == null ) {
				results = new ArrayList<SettingResourceHandler>(5);
				handlerMap.put(instanceUID, results);
			}
			return results;
		}
	}

	/**
	 * Add a handler for a given instance ID.
	 * 
	 * @param instanceUID
	 *        the ID of the instance to add the handler to
	 * @param handler
	 *        the handler to add
	 * @since 1.1
	 */
	public void addHandler(String instanceUID, SettingResourceHandler handler) {
		synchronized ( handlerMap ) {
			List<SettingResourceHandler> list = getInstanceHandlers(instanceUID);
			list.add(handler);
		}
	}

	/**
	 * Get the complete set setting providers.
	 * 
	 * @return set of instance IDs with associated providers
	 */
	public Set<Map.Entry<String, List<SettingSpecifierProvider>>> instanceEntrySet() {
		return instanceMap.entrySet();
	}

	/**
	 * Get the complete set setting resource handlers.
	 * 
	 * @return set of instance IDs with associated handlers
	 * @since 1.1
	 */
	public Set<Map.Entry<String, List<SettingResourceHandler>>> handlerEntrySet() {
		return handlerMap.entrySet();
	}

	/**
	 * Remove a provider.
	 * 
	 * @param provider
	 *        the provider
	 */
	public void removeProvider(SettingSpecifierProvider provider) {
		synchronized ( instanceMap ) {
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

	/**
	 * Remove a handler.
	 * 
	 * @param handler
	 *        the handler
	 * @since 1.1
	 */
	public void removeHandler(SettingResourceHandler handler) {
		synchronized ( handlerMap ) {
			for ( List<SettingResourceHandler> handlerList : handlerMap.values() ) {
				for ( Iterator<SettingResourceHandler> itr = handlerList.iterator(); itr.hasNext(); ) {
					SettingResourceHandler oneHandler = itr.next();
					if ( oneHandler.equals(handler) ) {
						itr.remove();
						return;
					}
				}
			}
		}
	}

}
