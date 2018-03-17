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
 */

package net.solarnetwork.node.settings.ca;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.supercsv.cellprocessor.ConvertNullTo;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;
import net.solarnetwork.node.Setting;
import net.solarnetwork.node.Setting.SettingFlag;
import net.solarnetwork.node.SetupSettings;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceInfo;
import net.solarnetwork.node.backup.BackupResourceProvider;
import net.solarnetwork.node.backup.BackupResourceProviderInfo;
import net.solarnetwork.node.backup.ResourceBackupResource;
import net.solarnetwork.node.backup.SimpleBackupResourceInfo;
import net.solarnetwork.node.backup.SimpleBackupResourceProviderInfo;
import net.solarnetwork.node.dao.BasicBatchOptions;
import net.solarnetwork.node.dao.BatchableDao.BatchCallback;
import net.solarnetwork.node.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.SettingsBackup;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsImportOptions;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.support.BasicFactorySettingSpecifierProvider;
import net.solarnetwork.node.support.KeyValuePair;

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
 * @version 1.4
 */
public class CASettingsService implements SettingsService, BackupResourceProvider {

	/** The OSGi service property key for the setting PID. */
	public static final String OSGI_PROPERTY_KEY_SETTING_PID = "settingPid";

	private static final String OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY = CASettingsService.class
			.getName() + ".FACTORY_INSTANCE_KEY";
	private static final String SETTING_LAST_BACKUP_DATE = "solarnode.settings.lastBackupDate";
	private static final String BACKUP_DATE_FORMAT = "yyyy-MM-dd-HHmmss";
	private static final String BACKUP_FILENAME_PREFIX = "settings_";
	private static final String BACKUP_FILENAME_EXT = "txt";
	private static final Pattern BACKUP_FILENAME_PATTERN = Pattern.compile('^' + BACKUP_FILENAME_PREFIX
			+ "(\\d{4}-\\d{2}-\\d{2}-\\d{6})\\." + BACKUP_FILENAME_EXT + "$");
	private static final int DEFAULT_BACKUP_MAX_COUNT = 5;
	private static final String FACTORY_SETTING_KEY_SUFFIX = ".FACTORY";

	// a CA PID pattern so that only these are attempted to be restored
	private static final Pattern CA_PID_PATTERN = Pattern.compile("^[a-zA-Z0-9.]+$");

	private ConfigurationAdmin configurationAdmin;
	private SettingDao settingDao;
	private TransactionTemplate transactionTemplate;
	private String backupDestinationPath;
	private int backupMaxCount = DEFAULT_BACKUP_MAX_COUNT;
	private MessageSource messageSource;

	private final Map<String, FactoryHelper> factories = new TreeMap<String, FactoryHelper>();
	// private final Map<String, SettingSpecifierProviderFactory> factories =
	// new TreeMap<String, SettingSpecifierProviderFactory>();
	// private final Map<String, List<SettingSpecifierProvider>>
	// factoryProviders = new HashMap<String, List<SettingSpecifierProvider>>();
	private final Map<String, SettingSpecifierProvider> providers = new TreeMap<String, SettingSpecifierProvider>();

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String getFactorySettingKey(String factoryPid) {
		return factoryPid + FACTORY_SETTING_KEY_SUFFIX;
	}

	private String getFactoryInstanceSettingKey(String factoryPid, String instanceKey) {
		return factoryPid + (instanceKey == null ? "" : "." + instanceKey);
	}

	/**
	 * Callback when a {@link SettingSpecifierProviderFactory} has been
	 * registered.
	 * 
	 * @param provider
	 *        the provider object
	 * @param properties
	 *        the service properties
	 */
	public void onBindFactory(SettingSpecifierProviderFactory provider, Map<String, ?> properties) {
		log.debug("Bind called on factory {} with props {}", provider, properties);
		final String factoryPid = provider.getFactoryUID();

		synchronized ( factories ) {
			factories.put(factoryPid, new FactoryHelper(provider));

			// find all configured factory instances, and publish those
			// configurations now. First we look up all registered factory
			// instances, so each returned result returns a configured instance
			// key
			List<KeyValuePair> instanceKeys = settingDao.getSettings(getFactorySettingKey(factoryPid));
			for ( KeyValuePair instanceKey : instanceKeys ) {
				SettingsCommand cmd = new SettingsCommand();
				cmd.setProviderKey(factoryPid);
				cmd.setInstanceKey(instanceKey.getKey());

				// now lookup all settings for the configured instance
				List<KeyValuePair> settings = settingDao
						.getSettings(getFactoryInstanceSettingKey(factoryPid, instanceKey.getKey()));
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
	 *        the configuration object
	 * @param properties
	 *        the service properties
	 */
	public void onUnbindFactory(SettingSpecifierProviderFactory provider, Map<String, ?> properties) {
		if ( provider == null ) {
			// gemini blueprint calls this when availability="optional" and there are no services
			return;
		}
		log.debug("Unbind called on factory {} with props {}", provider, properties);
		final String pid = provider.getFactoryUID();
		synchronized ( factories ) {
			factories.remove(pid);
		}
	}

	/**
	 * Callback when a {@link SettingSpecifierProvider} has been registered.
	 * 
	 * @param provider
	 *        the provider object
	 * @param properties
	 *        the service properties
	 */
	public void onBind(SettingSpecifierProvider provider, Map<String, ?> properties) {
		log.debug("Bind called on {} with props {}", provider, properties);
		final String pid = provider.getSettingUID();

		List<SettingSpecifierProvider> factoryList = null;
		String factoryInstanceKey = null;
		synchronized ( factories ) {
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
				} catch ( IOException e ) {
					log.error("Error getting factory instance configuration {}", instancePid, e);
				}
			}
		}

		if ( factoryList == null ) {
			synchronized ( providers ) {
				providers.put(pid, provider);
			}
		}

		final String settingKey = getFactoryInstanceSettingKey(pid, factoryInstanceKey);

		List<KeyValuePair> settings = settingDao.getSettings(settingKey);
		if ( settings.size() < 1 ) {
			return;
		}
		SettingsCommand cmd = new SettingsCommand();
		for ( KeyValuePair pair : settings ) {
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
	 *        the configuration object
	 * @param properties
	 *        the service properties
	 */
	public void onUnbind(SettingSpecifierProvider provider, Map<String, ?> properties) {
		if ( provider == null ) {
			// gemini blueprint calls this when availability="optional" and there are no services
			return;
		}
		log.debug("Unbind called on {} with props {}", provider, properties);
		final String pid = provider.getSettingUID();

		synchronized ( factories ) {
			FactoryHelper helper = factories.get(pid);
			if ( helper != null ) {
				helper.removeProvider(provider);
				return;
			}
		}

		synchronized ( providers ) {
			providers.remove(pid);
		}
	}

	@Override
	public List<SettingSpecifierProvider> getProviders() {
		synchronized ( providers ) {
			return new ArrayList<SettingSpecifierProvider>(providers.values());
		}
	}

	@Override
	public List<SettingSpecifierProviderFactory> getProviderFactories() {
		List<SettingSpecifierProviderFactory> results;
		synchronized ( factories ) {
			results = new ArrayList<SettingSpecifierProviderFactory>(factories.size());
			for ( FactoryHelper helper : factories.values() ) {
				results.add(helper.getFactory());
			}
			return results;
		}
	}

	@Override
	public SettingSpecifierProviderFactory getProviderFactory(String factoryUID) {
		synchronized ( factories ) {
			FactoryHelper helper = factories.get(factoryUID);
			if ( helper != null ) {
				return helper.getFactory();
			}
			return null;
		}
	}

	@Override
	public Map<String, List<FactorySettingSpecifierProvider>> getProvidersForFactory(String factoryUID) {
		Map<String, List<FactorySettingSpecifierProvider>> results = new LinkedHashMap<String, List<FactorySettingSpecifierProvider>>();
		synchronized ( factories ) {
			FactoryHelper helper = factories.get(factoryUID);
			if ( helper != null ) {
				for ( Map.Entry<String, List<SettingSpecifierProvider>> me : helper
						.instanceEntrySet() ) {
					String instanceUID = me.getKey();
					List<FactorySettingSpecifierProvider> list = new ArrayList<FactorySettingSpecifierProvider>(
							me.getValue().size());
					for ( SettingSpecifierProvider provider : me.getValue() ) {
						list.add(new BasicFactorySettingSpecifierProvider(instanceUID, provider));
					}
					results.put(instanceUID, list);
				}
			}
		}
		return results;
	}

	@Override
	public Object getSettingValue(SettingSpecifierProvider provider, SettingSpecifier setting) {
		if ( setting instanceof KeyedSettingSpecifier<?> ) {
			KeyedSettingSpecifier<?> keyedSetting = (KeyedSettingSpecifier<?>) setting;
			if ( keyedSetting.isTransient() ) {
				return keyedSetting.getDefaultValue();
			}
			final String providerUID = provider.getSettingUID();
			final String instanceUID = (provider instanceof FactorySettingSpecifierProvider
					? ((FactorySettingSpecifierProvider) provider).getFactoryInstanceUID() : null);
			try {
				Configuration conf = getConfiguration(providerUID, instanceUID);
				@SuppressWarnings("unchecked")
				Dictionary<String, ?> props = conf.getProperties();
				Object val = (props == null ? null : props.get(keyedSetting.getKey()));
				if ( val == null ) {
					val = keyedSetting.getDefaultValue();
				}
				return val;
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			} catch ( InvalidSyntaxException e ) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private static final Pattern INDEXED_PROP_PATTERN = Pattern.compile("\\[\\d+\\]");

	@SuppressWarnings("unchecked")
	@Override
	public void updateSettings(SettingsCommand command) {
		// group all updates by provider+instance, to reduce the number of CA updates
		// when multiple settings are changed
		if ( command.getProviderKey() == null ) {
			Map<String, SettingsCommand> groups = new LinkedHashMap<String, SettingsCommand>(8);
			Map<String, SettingsCommand> indexedGroups = null;
			for ( SettingValueBean bean : command.getValues() ) {
				String groupKey = bean.getProviderKey()
						+ (bean.getInstanceKey() == null ? "" : bean.getInstanceKey());
				final boolean indexed = INDEXED_PROP_PATTERN.matcher(bean.getKey()).find();
				SettingsCommand cmd = null;
				if ( indexed ) {
					// indexed property, add in indexed groups
					if ( indexedGroups == null ) {
						indexedGroups = new LinkedHashMap<String, SettingsCommand>(8);
					}
					cmd = indexedGroups.get(groupKey);
				} else {
					cmd = groups.get(groupKey);
				}

				if ( cmd == null ) {
					cmd = new SettingsCommand();
					cmd.setProviderKey(bean.getProviderKey());
					cmd.setInstanceKey(bean.getInstanceKey());
					if ( indexed ) {
						indexedGroups.put(groupKey, cmd);
					} else {
						groups.put(groupKey, cmd);
					}
				}
				cmd.getValues().add(bean);
			}
			for ( SettingsCommand cmd : groups.values() ) {
				updateSettings(cmd);
			}
			if ( indexedGroups != null ) {
				for ( SettingsCommand cmd : indexedGroups.values() ) {
					updateSettings(cmd);
				}
			}
			return;
		}

		try {
			Configuration conf = getConfiguration(command.getProviderKey(), command.getInstanceKey());
			Dictionary<String, Object> props = conf.getProperties();
			if ( props == null ) {
				props = new Hashtable<String, Object>();
			}
			for ( SettingValueBean bean : command.getValues() ) {
				String settingKey = command.getProviderKey();
				String instanceKey = command.getInstanceKey();
				if ( instanceKey != null ) {
					settingKey = getFactoryInstanceSettingKey(settingKey, instanceKey);
				}
				if ( bean.isRemove() ) {
					props.remove(bean.getKey());
				} else {
					props.put(bean.getKey(), bean.getValue());
				}

				if ( !bean.isTransient() ) {
					if ( bean.isRemove() ) {
						settingDao.deleteSetting(settingKey, bean.getKey());
					} else {
						settingDao.storeSetting(settingKey, bean.getKey(), bean.getValue());
					}
				}
			}
			if ( conf != null && props != null ) {
				if ( command.getInstanceKey() != null ) {
					props.put(OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY, command.getInstanceKey());
				}
				conf.update(props);
			}

		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} catch ( InvalidSyntaxException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String addProviderFactoryInstance(String factoryUID) {
		synchronized ( factories ) {
			List<KeyValuePair> instanceKeys = settingDao.getSettings(getFactorySettingKey(factoryUID));
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
			return addProviderFactoryInstance(factoryUID, newInstanceKey);
		}
	}

	@Override
	public void deleteProviderFactoryInstance(String factoryUID, String instanceUID) {
		synchronized ( factories ) {
			// delete factory reference
			settingDao.deleteSetting(getFactorySettingKey(factoryUID), instanceUID);

			// delete instance values
			settingDao.deleteSetting(getFactoryInstanceSettingKey(factoryUID, instanceUID));

			// delete Configuration
			try {
				Configuration conf = getConfiguration(factoryUID, instanceUID);
				conf.delete();
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			} catch ( InvalidSyntaxException e ) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public void resetProviderFactoryInstance(String factoryUID, String instanceUID) {
		deleteProviderFactoryInstance(factoryUID, instanceUID);
		synchronized ( factories ) {
			addProviderFactoryInstance(factoryUID, instanceUID);
		}
	}

	private String addProviderFactoryInstance(String factoryUID, String instanceUID) {
		settingDao.storeSetting(getFactorySettingKey(factoryUID), instanceUID, instanceUID);
		try {
			Configuration conf = getConfiguration(factoryUID, instanceUID);
			@SuppressWarnings("unchecked")
			Dictionary<String, Object> props = conf.getProperties();
			if ( props == null ) {
				props = new Hashtable<String, Object>();
			}
			props.put(OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY, instanceUID);
			conf.update(props);
			return instanceUID;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} catch ( InvalidSyntaxException e ) {
			throw new RuntimeException(e);
		}
	}
	
	private static final String[] CSV_HEADERS = new String[] { "key", "type", "value", "flags",
			"modified" };
	private static final String SETTING_MODIFIED_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	@Override
	public void exportSettingsCSV(Writer out) throws IOException {
		final ICsvBeanWriter writer = new CsvBeanWriter(out, CsvPreference.STANDARD_PREFERENCE);
		final List<IOException> errors = new ArrayList<IOException>(1);
		final CellProcessor[] processors = new CellProcessor[] {
				new org.supercsv.cellprocessor.Optional(), new org.supercsv.cellprocessor.Optional(),
				new org.supercsv.cellprocessor.Optional(), new CellProcessor() {

					@Override
					public Object execute(Object value, CsvContext ctx) {
						@SuppressWarnings("unchecked")
						Set<net.solarnetwork.node.Setting.SettingFlag> set = (Set<net.solarnetwork.node.Setting.SettingFlag>) value;
						if ( set != null ) {
							return net.solarnetwork.node.Setting.SettingFlag.maskForSet(set);
						}
						return 0;
					}
				}, new org.supercsv.cellprocessor.FmtDate(SETTING_MODIFIED_DATE_FORMAT) };
		try {
			writer.writeHeader(CSV_HEADERS);
			settingDao.batchProcess(new BatchCallback<Setting>() {

				@Override
				public BatchCallbackResult handle(Setting domainObject) {
					try {
						writer.write(domainObject, CSV_HEADERS, processors);
					} catch ( IOException e ) {
						errors.add(e);
						return BatchCallbackResult.STOP;
					}
					return BatchCallbackResult.CONTINUE;
				}
			}, new BasicBatchOptions("Export Settings"));
			if ( errors.size() > 0 ) {
				throw errors.get(0);
			}
		} finally {
			if ( writer != null ) {
				try {
					writer.flush();
					writer.close();
				} catch ( IOException e ) {
					// ignore these
				}
			}
		}
	}

	/**
	 * A callback API for allowing the settings import process to decide which
	 * settings should be imported.
	 */
	private interface ImportCallback {

		/**
		 * Test if a specific should be imported at all.
		 * 
		 * @param key
		 *        the setting key
		 * @param type
		 *        the setting value
		 * @param value
		 *        the setting value
		 * @return <em>true</em> to allow the setting to be imported,
		 *         <em>false</em> to skip
		 */
		boolean shouldImportSetting(Setting setting);

	}

	@Override
	public void importSettingsCSV(Reader in) throws IOException {
		importSettingsCSV(in, new SettingsImportOptions());
	}

	@Override
	public void importSettingsCSV(final Reader in, final SettingsImportOptions options)
			throws IOException {
		// TODO: need a better way to organize settings into "do not restore" category
		synchronized ( factories ) {
			final Pattern allowed = Pattern.compile("^(?!solarnode).*", Pattern.CASE_INSENSITIVE);
			importSettingsCSV(in, new ImportCallback() {

				@Override
				public boolean shouldImportSetting(Setting s) {
					if ( allowed.matcher(s.getKey()).matches() == false ) {
						return false;
					}
					if ( options.isAddOnly() ) {
						// check if setting exists already, and if so do not import it
						if ( settingDao.getSetting(s.getKey(), s.getType()) != null ) {
							log.debug("Not updating existing setting {}", s.getKey());
							return false;
						}
					}
					return true;
				}
			});
		}
	}

	private void importSettingsCSV(Reader in, final ImportCallback callback) throws IOException {
		final ICsvBeanReader reader = new CsvBeanReader(in, CsvPreference.STANDARD_PREFERENCE);
		final CellProcessor[] processors = new CellProcessor[] { null, new ConvertNullTo(""), null,
				new CellProcessor() {

					@Override
					public Object execute(Object arg, CsvContext ctx) {
						Set<net.solarnetwork.node.Setting.SettingFlag> set = null;
						if ( arg != null ) {
							int mask = Integer.parseInt(arg.toString());
							set = net.solarnetwork.node.Setting.SettingFlag.setForMask(mask);
						}
						return set;
					}
				}, new org.supercsv.cellprocessor.ParseDate(SETTING_MODIFIED_DATE_FORMAT) };
		reader.getHeader(true);
		final List<Setting> importedSettings = new ArrayList<Setting>();
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(final TransactionStatus status) {
				Setting s;
				try {
					while ( (s = reader.read(Setting.class, CSV_HEADERS, processors)) != null ) {
						if ( !callback.shouldImportSetting(s) ) {
							continue;
						}
						if ( s.getKey() == null ) {
							continue;
						}
						if ( s.getValue() == null ) {
							settingDao.deleteSetting(s.getKey(), s.getType());
						} else {
							settingDao.storeSetting(s);
							importedSettings.add(s);
						}
					}
				} catch ( IOException e ) {
					log.error("Unable to import settings: {}", e.getMessage());
					status.setRollbackOnly();
				} finally {
					try {
						reader.close();
					} catch ( IOException e ) {
						// ingore
					}
					if ( status.isRollbackOnly() ) {
						importedSettings.clear();
					}
				}
			}
		});

		// now that settings have been imported into DAO layer, we need to apply them to the existing runtime

		// first, determine what factories we have... these have keys like <factoryPID>.FACTORY
		final Map<String, Setting> factorySettings = new HashMap<String, Setting>();
		for ( Setting s : importedSettings ) {
			if ( s.getKey() == null || !s.getKey().endsWith(FACTORY_SETTING_KEY_SUFFIX) ) {
				continue;
			}
			String factoryPID = s.getKey().substring(0,
					s.getKey().length() - FACTORY_SETTING_KEY_SUFFIX.length());
			log.debug("Discovered imported factory setting {}", factoryPID);
			factorySettings.put(factoryPID, s);

			// Now create the CA configuration for all defined factories, to handle situation where we don't actually
			// configure any custom settings on the factory. In that case we don't have any settings, but we need
			// to instantiate the factory so we create a default instance.
			try {
				int instanceCount = Integer.valueOf(s.getValue());
				for ( int i = 1; i <= instanceCount; i++ ) {
					String instanceKey = String.valueOf(i);
					Configuration conf = getConfiguration(factoryPID, instanceKey);
					@SuppressWarnings("unchecked")
					Dictionary<String, Object> props = conf.getProperties();
					if ( props == null ) {
						props = new Hashtable<String, Object>();
						props.put(OSGI_PROPERTY_KEY_FACTORY_INSTANCE_KEY, instanceKey);
						conf.update(props);
					}
				}
			} catch ( NumberFormatException e ) {
				log.warn("Factory {} setting does not have instance count value: {}", factoryPID,
						e.getMessage());
			} catch ( InvalidSyntaxException e ) {
				log.warn("Factory {} setting has invalid syntax: {}", factoryPID, e.getMessage());
			}
		}

		// now convert imported settings into a SettingsCommand, so values are applied to Configuration Admin
		SettingsCommand cmd = new SettingsCommand();

		for ( Setting s : importedSettings ) {
			if ( s.getKey() == null ) {
				continue;
			}

			// skip factory instance definitions
			if ( s.getKey().endsWith(FACTORY_SETTING_KEY_SUFFIX) ) {
				continue;
			}

			// skip things that don't look like CA settings
			if ( !CA_PID_PATTERN.matcher(s.getKey()).matches() || s.getType() == null
					|| SetupSettings.SETUP_TYPE_KEY.equals(s.getType()) || s.getType().length() < 1 ) {
				continue;
			}

			SettingValueBean bean = new SettingValueBean();

			// find out if this is a factory
			for ( String factoryPID : factorySettings.keySet() ) {
				if ( s.getKey().startsWith(factoryPID + ".")
						&& s.getKey().length() > (factoryPID.length() + 1) ) {
					bean.setProviderKey(factoryPID);
					bean.setInstanceKey(s.getKey().substring(factoryPID.length() + 1));
					break;
				}
			}

			if ( bean.getProviderKey() == null ) {
				// not a factory setting
				bean.setProviderKey(s.getKey());
			}
			bean.setKey(s.getType());
			bean.setValue(s.getValue());
			bean.setTransient(s.getFlags() != null && s.getFlags().contains(SettingFlag.Volatile));
			cmd.getValues().add(bean);
		}
		if ( cmd.getValues().size() > 0 ) {
			updateSettings(cmd);
		}
	}

	@Override
	public SettingsBackup backupSettings() {
		final Date mrd = settingDao.getMostRecentModificationDate();
		final SimpleDateFormat sdf = new SimpleDateFormat(BACKUP_DATE_FORMAT);
		final String lastBackupDateStr = settingDao.getSetting(SETTING_LAST_BACKUP_DATE);
		final Date lastBackupDate;
		try {
			lastBackupDate = (lastBackupDateStr == null ? null : sdf.parse(lastBackupDateStr));
		} catch ( ParseException e ) {
			throw new RuntimeException("Unable to parse backup last date: " + e.getMessage());
		}
		if ( mrd == null || (lastBackupDate != null && lastBackupDate.after(mrd)) ) {
			log.debug("Settings unchanged since last backup on {}", lastBackupDateStr);
			return null;
		}
		final Date backupDate = new Date();
		final String backupDateKey = sdf.format(backupDate);
		final File dir = new File(backupDestinationPath);
		if ( !dir.exists() ) {
			dir.mkdirs();
		}
		final File f = new File(dir, BACKUP_FILENAME_PREFIX + backupDateKey + '.' + BACKUP_FILENAME_EXT);
		log.info("Backing up settings to {}", f.getPath());
		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(f));
			exportSettingsCSV(writer);
			settingDao.storeSetting(new Setting(SETTING_LAST_BACKUP_DATE, null, backupDateKey,
					EnumSet.of(SettingFlag.IgnoreModificationDate)));
		} catch ( IOException e ) {
			log.error("Unable to create settings backup {}: {}", f.getPath(), e.getMessage());
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch ( IOException e ) {
				// ignore
			}
		}

		// clean out older backups
		File[] files = dir.listFiles(new RegexFileFilter(BACKUP_FILENAME_PATTERN));
		if ( files != null && files.length > backupMaxCount ) {
			// sort array 
			Arrays.sort(files, new FilenameReverseComparator());
			for ( int i = backupMaxCount; i < files.length; i++ ) {
				if ( !files[i].delete() ) {
					log.warn("Unable to delete old settings backup file {}", files[i]);
				}
			}
		}
		return new SettingsBackup(backupDateKey, backupDate);
	}

	@Override
	public Collection<SettingsBackup> getAvailableBackups() {
		final File dir = new File(backupDestinationPath);
		File[] files = dir.listFiles(new RegexFileFilter(BACKUP_FILENAME_PATTERN));
		if ( files == null || files.length == 0 ) {
			return Collections.emptyList();
		}
		Arrays.sort(files, new FilenameReverseComparator());
		List<SettingsBackup> list = new ArrayList<SettingsBackup>(files.length);
		SimpleDateFormat sdf = new SimpleDateFormat(BACKUP_DATE_FORMAT);
		for ( File f : files ) {
			Matcher m = BACKUP_FILENAME_PATTERN.matcher(f.getName());
			if ( m.matches() ) {
				String dateStr = m.group(1);
				try {
					list.add(new SettingsBackup(dateStr, sdf.parse(dateStr)));
				} catch ( ParseException e ) {
					log.warn("Unable to parse backup file date from filename {}: {}", f.getName(),
							e.getMessage());
				}
			}
		}
		return list;
	}

	@Override
	public Reader getReaderForBackup(SettingsBackup backup) {
		final File dir = new File(backupDestinationPath);
		try {
			final String fname = BACKUP_FILENAME_PREFIX + backup.getBackupKey() + '.'
					+ BACKUP_FILENAME_EXT;
			final File f = new File(dir, fname);
			if ( f.canRead() ) {
				return new BufferedReader(new FileReader(f));
			}
		} catch ( FileNotFoundException e ) {
			return null;
		}
		return null;
	}

	@Override
	public String getKey() {
		return CASettingsService.class.getName();
	}

	private static final String BACKUP_RESOURCE_SETTINGS_CSV = "settings.csv";

	@Override
	public Iterable<BackupResource> getBackupResources() {
		// create resource from our settings CSV data
		ByteArrayOutputStream byos = new ByteArrayOutputStream();
		try {
			OutputStreamWriter writer = new OutputStreamWriter(byos, "UTF-8");
			exportSettingsCSV(writer);
		} catch ( IOException e ) {
			log.error("Unable to create settings backup resource", e);
		}
		List<BackupResource> resources = new ArrayList<BackupResource>(1);
		resources.add(new ResourceBackupResource(new ByteArrayResource(byos.toByteArray()),
				BACKUP_RESOURCE_SETTINGS_CSV, getKey()));
		return resources;
	}

	@Override
	public boolean restoreBackupResource(BackupResource resource) {
		if ( BACKUP_RESOURCE_SETTINGS_CSV.equalsIgnoreCase(resource.getBackupPath()) ) {
			try {
				// TODO: need a better way to organize settings into "do not restore" category
				final Pattern notAllowed = Pattern.compile("^solarnode.*", Pattern.CASE_INSENSITIVE);
				InputStreamReader reader = new InputStreamReader(resource.getInputStream(), "UTF-8");
				importSettingsCSV(reader, new ImportCallback() {

					@Override
					public boolean shouldImportSetting(Setting s) {
						if ( notAllowed.matcher(s.getKey()).matches() ) {
							// only allow restoring solarnode keys if their type is NOT empty
							return (s.getType() != null && s.getType().length() > 0);
						}
						return true;
					}
				});
				return true;
			} catch ( IOException e ) {
				log.error("Unable to restore settings backup resource", e);
			}
		}
		return false;
	}

	@Override
	public BackupResourceProviderInfo providerInfo(Locale locale) {
		String name = "Settings Backup Provider";
		String desc = "Backs up system settings.";
		MessageSource ms = messageSource;
		if ( ms != null ) {
			name = ms.getMessage("title", null, name, locale);
			desc = ms.getMessage("desc", null, desc, locale);
		}
		return new SimpleBackupResourceProviderInfo(getKey(), name, desc);
	}

	@Override
	public BackupResourceInfo resourceInfo(BackupResource resource, Locale locale) {
		return new SimpleBackupResourceInfo(resource.getProviderKey(), resource.getBackupPath(), null);
	}

	private static class FilenameReverseComparator implements Comparator<File> {

		@Override
		public int compare(File o1, File o2) {
			// order in reverse, and then we can delete all but maxBackupCount
			return o2.getName().compareTo(o1.getName());
		}
	}

	private static class RegexFileFilter implements FileFilter {

		final Pattern p;

		private RegexFileFilter(Pattern p) {
			super();
			this.p = p;
		}

		@Override
		public boolean accept(File pathname) {
			Matcher m = p.matcher(pathname.getName());
			return m.matches();
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
			throws IOException, InvalidSyntaxException {
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

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	public void setBackupDestinationPath(String backupDestinationPath) {
		this.backupDestinationPath = backupDestinationPath;
	}

	public void setBackupMaxCount(int backupMaxCount) {
		this.backupMaxCount = backupMaxCount;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
