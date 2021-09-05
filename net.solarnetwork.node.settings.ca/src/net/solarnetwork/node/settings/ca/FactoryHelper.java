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
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.util.MapPathMatcher;
import net.solarnetwork.util.SearchFilter;

/**
 * Helper class for managing factory providers.
 * 
 * @author matt
 * @version 2.0
 */
public final class FactoryHelper {

	private static final Logger log = LoggerFactory.getLogger(FactoryHelper.class);

	private final SettingSpecifierProviderFactory factory;
	private final Map<String, ?> properties;
	private final Map<String, SettingSpecifierProvider> instanceMap;
	private final Map<String, SettingResourceHandler> handlerMap;

	/**
	 * Constructor.
	 * 
	 * @param factory
	 *        the factory to provide help to
	 * @param properties
	 *        the properties
	 */
	public FactoryHelper(SettingSpecifierProviderFactory factory, Map<String, ?> properties) {
		this(factory, properties, new TreeMap<>(), new TreeMap<>());
	}

	/**
	 * Constructor.
	 * 
	 * @param factory
	 *        the factory to provide help to
	 * @param properties
	 *        the properties
	 * @param instanceMap
	 *        a map to use for tracking instance providers
	 * @param handlerMap
	 *        a map to use for tracking instance handlers
	 */
	public FactoryHelper(SettingSpecifierProviderFactory factory, Map<String, ?> properties,
			Map<String, SettingSpecifierProvider> instanceMap,
			Map<String, SettingResourceHandler> handlerMap) {
		super();
		this.factory = factory;
		this.properties = properties;
		this.instanceMap = instanceMap;
		this.handlerMap = handlerMap;
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
	 * Get the properties.
	 * 
	 * @return the properties
	 * @since 1.2
	 */
	public Map<String, ?> getProperties() {
		return properties;
	}

	/**
	 * Test if a filter matches the provider service properties.
	 * 
	 * @param filter
	 *        the filter
	 * @return {@literal true} if the filter matches
	 * @since 1.2
	 */
	public boolean matches(SearchFilter filter) {
		if ( properties == null ) {
			return false;
		}
		return MapPathMatcher.matches(properties, filter);
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
		synchronized ( instanceMap ) {
			SettingSpecifierProvider existing = instanceMap.putIfAbsent(instanceUID, provider);
			if ( existing != null ) {
				log.warn(
						"Duplicate setting provider instance {} for provider {} ignored; already configured as {}",
						instanceUID, provider, existing);
			}
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
			SettingResourceHandler existing = handlerMap.putIfAbsent(instanceUID, handler);
			if ( existing != null ) {
				log.warn(
						"Duplicate setting resource handler instance {} for provider {} ignored; already configured as {}",
						instanceUID, handler, existing);
			}
		}
	}

	/**
	 * Get the complete set setting providers.
	 * 
	 * @return set of instance IDs with associated providers
	 */
	public Iterable<Map.Entry<String, SettingSpecifierProvider>> instanceEntrySet() {
		synchronized ( instanceMap ) {
			return new ArrayList<>(instanceMap.entrySet());
		}
	}

	/**
	 * Get the complete set setting resource handlers.
	 * 
	 * @param instanceUID
	 *        the handler key to get
	 * @return the handler, or {@literal null} if none available
	 * @since 1.1
	 */
	public SettingResourceHandler getHandler(String instanceUID) {
		synchronized ( handlerMap ) {
			return handlerMap.get(instanceUID);
		}
	}

	/**
	 * Remove a provider.
	 * 
	 * @param provider
	 *        the provider
	 */
	public void removeProvider(SettingSpecifierProvider provider) {
		synchronized ( instanceMap ) {
			for ( Iterator<SettingSpecifierProvider> itr = instanceMap.values().iterator(); itr
					.hasNext(); ) {
				SettingSpecifierProvider oneProvider = itr.next();
				if ( oneProvider.equals(provider) ) {
					itr.remove();
					return;
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
			for ( Iterator<SettingResourceHandler> itr = handlerMap.values().iterator(); itr
					.hasNext(); ) {
				SettingResourceHandler oneHandler = itr.next();
				if ( oneHandler.equals(handler) ) {
					itr.remove();
					return;
				}
			}
		}
	}

}
