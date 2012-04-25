/* ==================================================================
 * CASettingsService.java - Mar 12, 2012 1:11:29 PM
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.support.BasicFactorySettingSpecifierProvider;
import net.solarnetwork.node.support.KeyValuePair;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link SettingsService} that uses
 * {@link ConfigurationAdmin} to change settings at runtime, and
 * {@link SettingDao} to persist changes between application restarts.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>configurationAdmin</dt>
 * <dd>The {@link ConfigurationAdmin} service to use.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class CASettingsService implements SettingsService {

	/** The OSGi service property key for the setting PID. */
	public static final String OSGI_PROPERTY_KEY_SETTING_PID = "settingPid";

	private static final String OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY = CASettingsService.class
			.getName() + ".FACTORY_INSTANCE_KEY";

	private ConfigurationAdmin configurationAdmin;
	private SettingDao settingDao;

	private final Map<String, FactoryHelper> factories = new TreeMap<String, FactoryHelper>();
	// private final Map<String, SettingSpecifierProviderFactory> factories =
	// new TreeMap<String, SettingSpecifierProviderFactory>();
	// private final Map<String, List<SettingSpecifierProvider>>
	// factoryProviders = new HashMap<String, List<SettingSpecifierProvider>>();
	private final Map<String, SettingSpecifierProvider> providers = new TreeMap<String, SettingSpecifierProvider>();

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String getFactorySettingKey(String factoryPid) {
		return factoryPid + ".FACTORY";
	}

	private String getFactoryInstanceSettingKey(String factoryPid, String instanceKey) {
		return factoryPid + (instanceKey == null ? "" : "." + instanceKey);
	}

	/**
	 * Callback when a {@link SettingSpecifierProviderFactory} has been
	 * registered.
	 * 
	 * @param provider
	 *            the provider object
	 * @param properties
	 *            the service properties
	 */
	public void onBindFactory(SettingSpecifierProviderFactory provider, Map<String, ?> properties) {
		log.debug("Bind called on factory {} with props {}", provider, properties);
		final String factoryPid = provider.getFactoryUID();

		synchronized (factories) {
			factories.put(factoryPid, new FactoryHelper(provider));

			// find all configured factory instances, and publish those
			// configurations now. First we look up all registered factory
			// instances, so each returned result returns a configured instance
			// key
			List<KeyValuePair> instanceKeys = settingDao
					.getSettings(getFactorySettingKey(factoryPid));
			for ( KeyValuePair instanceKey : instanceKeys ) {
				SettingsCommand cmd = new SettingsCommand();
				cmd.setProviderKey(factoryPid);
				cmd.setInstanceKey(instanceKey.getKey());

				// now lookup all settings for the configured instance
				List<KeyValuePair> settings = settingDao.getSettings(getFactoryInstanceSettingKey(
						factoryPid, instanceKey.getKey()));
				for ( KeyValuePair setting : settings ) {
					SettingValueBean bean = new SettingValueBean();
					bean.setKey(setting.getKey());
					bean.setValue(setting.getValue());
					cmd.getValues().add(bean);
				}
				updateSettings(cmd);
			}
		}
	}

	/**
	 * Callback when a {@link SettingSpecifierProviderFactory} has been
	 * un-registered.
	 * 
	 * @param config
	 *            the configuration object
	 * @param properties
	 *            the service properties
	 */
	public void onUnbindFactory(SettingSpecifierProviderFactory provider, Map<String, ?> properties) {
		log.debug("Unbind called on factory {} with props {}", provider, properties);
		final String pid = provider.getFactoryUID();
		synchronized (factories) {
			factories.remove(pid);
		}
	}

	/**
	 * Callback when a {@link SettingSpecifierProvider} has been registered.
	 * 
	 * @param provider
	 *            the provider object
	 * @param properties
	 *            the service properties
	 */
	public void onBind(SettingSpecifierProvider provider, Map<String, ?> properties) {
		log.debug("Bind called on {} with props {}", provider, properties);
		final String pid = provider.getSettingUID();

		List<SettingSpecifierProvider> factoryList = null;
		String factoryInstanceKey = null;
		synchronized (factories) {
			FactoryHelper helper = factories.get(pid);
			if ( helper != null ) {
				// Note: SERVICE_PID not normally provided by Spring: requires
				// custom SN implementation bundle
				String instancePid = (String) properties.get(Constants.SERVICE_PID);

				Configuration conf;
				try {
					conf = configurationAdmin.getConfiguration(instancePid, null);
					@SuppressWarnings("unchecked")
					Dictionary<String, ?> props = conf.getProperties();
					if ( props != null ) {
						factoryInstanceKey = (String) props.get(OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY);
						log.debug("Got factory {} instance key {}", pid, factoryInstanceKey);

						factoryList = helper.getInstanceProviders(factoryInstanceKey);
						factoryList.add(provider);
					}
				} catch (IOException e) {
					log.error("Error getting factory instance configuration {}", instancePid, e);
				}
			}
		}

		if ( factoryList == null ) {
			synchronized (providers) {
				providers.put(pid, provider);
			}
		}

		final String settingKey = getFactoryInstanceSettingKey(pid, factoryInstanceKey);

		List<KeyValuePair> settings = settingDao.getSettings(settingKey);
		SettingsCommand cmd = new SettingsCommand();
		for (KeyValuePair pair : settings) {
			SettingValueBean bean = new SettingValueBean();
			bean.setProviderKey(provider.getSettingUID());
			bean.setInstanceKey(factoryInstanceKey);
			bean.setKey(pair.getKey());
			bean.setValue(pair.getValue());
			cmd.getValues().add(bean);
		}
		updateSettings(cmd);
	}

	/**
	 * Callback when a {@link SettingSpecifierProvider} has been un-registered.
	 * 
	 * @param config
	 *            the configuration object
	 * @param properties
	 *            the service properties
	 */
	public void onUnbind(SettingSpecifierProvider provider, Map<String, ?> properties) {
		log.debug("Unbind called on {} with props {}", provider, properties);
		final String pid = provider.getSettingUID();

		synchronized (factories) {
			FactoryHelper helper = factories.get(pid);
			if ( helper != null ) {
				helper.removeProvider(provider);
				return;
			}
		}

		synchronized (providers) {
			providers.remove(pid);
		}
	}

	@Override
	public List<SettingSpecifierProvider> getProviders() {
		synchronized (providers) {
			return new ArrayList<SettingSpecifierProvider>(providers.values());
		}
	}

	@Override
	public List<SettingSpecifierProviderFactory> getProviderFactories() {
		List<SettingSpecifierProviderFactory> results;
		synchronized (factories) {
			results = new ArrayList<SettingSpecifierProviderFactory>(factories.size());
			for ( FactoryHelper helper : factories.values() ) {
				results.add(helper.getFactory());
			}
			return results;
		}
	}

	@Override
	public SettingSpecifierProviderFactory getProviderFactory(String factoryUID) {
		synchronized (factories) {
			FactoryHelper helper = factories.get(factoryUID);
			if ( helper != null ) {
				return helper.getFactory();
			}
			return null;
		}
	}

	@Override
	public Map<String, List<FactorySettingSpecifierProvider>> getProvidersForFactory(
			String factoryUID) {
		Map<String, List<FactorySettingSpecifierProvider>> results = new LinkedHashMap<String, List<FactorySettingSpecifierProvider>>();
		synchronized (factories) {
			FactoryHelper helper = factories.get(factoryUID);
			for ( Map.Entry<String, List<SettingSpecifierProvider>> me : helper.instanceEntrySet() ) {
				String instanceUID = me.getKey();
				List<FactorySettingSpecifierProvider> list = new ArrayList<FactorySettingSpecifierProvider>(
						me.getValue().size());
				for ( SettingSpecifierProvider provider : me.getValue() ) {
					list.add(new BasicFactorySettingSpecifierProvider(instanceUID, provider));
				}
				results.put(instanceUID, list);
			}
		}
		return results;
	}

	@Override
	public Object getSettingValue(SettingSpecifierProvider provider, SettingSpecifier setting) {
		if ( setting instanceof KeyedSettingSpecifier<?> ) {
			KeyedSettingSpecifier<?> keyedSetting = (KeyedSettingSpecifier<?>) setting;
			final String providerUID = provider.getSettingUID();
			final String instanceUID = (provider instanceof FactorySettingSpecifierProvider ? ((FactorySettingSpecifierProvider) provider)
					.getFactoryInstanceUID() : null);
			try {
				Configuration conf = getConfiguration(providerUID, instanceUID);
				@SuppressWarnings("unchecked")
				Dictionary<String, ?> props = conf.getProperties();
				Object val = (props == null ? null : props.get(keyedSetting.getKey()));
				if ( val == null ) {
					val = keyedSetting.getDefaultValue();
				}
				return val;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InvalidSyntaxException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void updateSettings(SettingsCommand command) {
		try {
			Configuration conf = null;
			Dictionary<String, Object> props = null;
			if ( command.getProviderKey() != null ) {
				conf = getConfiguration(command.getProviderKey(), command.getInstanceKey());
				props = conf.getProperties();
				if ( props == null ) {
					props = new Hashtable<String, Object>();
				}
			}
			for ( SettingValueBean bean : command.getValues() ) {
				if ( command.getProviderKey() == null ) {
					conf = getConfiguration(bean.getProviderKey(), bean.getInstanceKey());
					props = conf.getProperties();
					if ( props == null ) {
						props = new Hashtable<String, Object>();
					}
				}

				String settingKey = command.getProviderKey() != null ? command.getProviderKey() : bean.getProviderKey();
				String instanceKey = command.getInstanceKey() != null ? command.getInstanceKey() : bean.getInstanceKey();
				if ( instanceKey != null ) {
					settingKey = getFactoryInstanceSettingKey(settingKey, instanceKey);
					if ( command.getInstanceKey() == null ) {
						props.put(OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY, instanceKey);
					}
				}
				props.put(bean.getKey(), bean.getValue());
				if ( command.getProviderKey() == null ) {
					conf.update(props);
				}

				settingDao.storeSetting(settingKey, bean.getKey(), bean.getValue());
			}
			if ( conf != null && props != null ) {
				if ( command.getInstanceKey() != null ) {
					props.put(OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY, command.getInstanceKey());
				}
				conf.update(props);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String addProviderFactoryInstance(String factoryUID) {
		synchronized (factories) {
			List<KeyValuePair> instanceKeys = settingDao
					.getSettings(getFactorySettingKey(factoryUID));
			int next = instanceKeys.size() + 1;
			// verify key doesn't exist
			boolean done = false;
			while ( !done ) {
				done = true;
				for ( KeyValuePair instanceKey : instanceKeys ) {
					if ( instanceKey.getKey().equals(String.valueOf(next)) ) {
						done = false;
						next++;
					}
				}
			}
			String newInstanceKey = String.valueOf(next);
			settingDao.storeSetting(getFactorySettingKey(factoryUID), newInstanceKey,
					newInstanceKey);
			try {
				Configuration conf = getConfiguration(factoryUID, newInstanceKey);
				@SuppressWarnings("unchecked")
				Dictionary<String, Object> props = conf.getProperties();
				if ( props == null ) {
					props = new Hashtable<String, Object>();
				}
				props.put(OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY, newInstanceKey);
				conf.update(props);
				return newInstanceKey;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InvalidSyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void deleteProviderFactoryInstance(String factoryUID, String instanceUID) {
		synchronized (factories) {
			// delete factory reference
			settingDao.deleteSetting(getFactorySettingKey(factoryUID), instanceUID);

			// delete instance values
			settingDao.deleteSetting(getFactoryInstanceSettingKey(factoryUID, instanceUID));

			// delete Configuration
			try {
				Configuration conf = getConfiguration(factoryUID, instanceUID);
				conf.delete();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InvalidSyntaxException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private Configuration getConfiguration(String providerUID, String factoryInstanceUID)
			throws IOException, InvalidSyntaxException {
		Configuration conf = null;
		if ( factoryInstanceUID == null ) {
			conf = configurationAdmin.getConfiguration(providerUID, null);
		} else {
			conf = findExistingConfiguration(providerUID, factoryInstanceUID);
			if ( conf == null ) {
				conf = configurationAdmin.createFactoryConfiguration(providerUID, null);
			}
		}
		return conf;
	}

	private Configuration findExistingConfiguration(String pid, String instanceKey)
			throws IOException,
			InvalidSyntaxException {
		String filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + pid + ")("
				+ OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY + "=" + instanceKey + "))";
		Configuration[] configurations = configurationAdmin.listConfigurations(filter);
		if ( configurations != null && configurations.length > 0 ) {
			return configurations[0];
		} else {
			return null;
		}
	}

	public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
	}

	public SettingDao getSettingDao() {
		return settingDao;
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

}
