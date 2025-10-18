/* ==================================================================
 * SettingsService.java - Mar 12, 2012 4:58:14 PM
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

package net.solarnetwork.node.settings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.Resource;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.domain.SettingNote;
import net.solarnetwork.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.util.SearchFilter;

/**
 * Service API for settings.
 *
 * @author matt
 * @version 2.6
 * @since 2.0
 */
public interface SettingsService {

	/**
	 * The system property for the setting resource directory.
	 *
	 * @since 1.4
	 */
	String SYSTEM_PROP_SETTING_RESOURCE_DIR = "sn.rsrc";

	/**
	 * The default setting resource directory, if
	 * {@link #SYSTEM_PROP_SETTING_RESOURCE_DIR} is not defined.
	 *
	 * @since 1.4
	 */
	String DEFAULT_SETTING_RESOURCE_DIR = "conf/rsrc";

	/**
	 * A name to use for settings audit logging.
	 *
	 * @since 2.5
	 */
	String AUDIT_LOG_NAME = "net.solarnetwork.node.settings.AUDIT";

	/**
	 * Get the path to the setting resource directory.
	 *
	 * <p>
	 * This is the directory where external setting resources can be persisted.
	 * If the system property {@link #SYSTEM_PROP_SETTING_RESOURCE_DIR} is
	 * defined as an absolute path, that path is used directly. Otherwise
	 * {@link #SYSTEM_PROP_SETTING_RESOURCE_DIR} is treated as a path relative
	 * to {@link Constants#solarNodeHome()}, defaulting to
	 * {@link #DEFAULT_SETTING_RESOURCE_DIR} if not defined.
	 * </p>
	 *
	 * @return the setting resource directory, never {@literal null}
	 * @since 1.4
	 */
	static Path settingResourceDirectory() {
		Path rsrcDir = Paths
				.get(System.getProperty(SYSTEM_PROP_SETTING_RESOURCE_DIR, DEFAULT_SETTING_RESOURCE_DIR));
		if ( rsrcDir.isAbsolute() ) {
			return rsrcDir;
		}
		Path dir = Paths.get(Constants.solarNodeHome());
		return dir.resolve(rsrcDir);
	}

	/**
	 * The instruction topic for a request to update (create or change) a
	 * setting value.
	 *
	 * @since 1.3
	 */
	String TOPIC_UPDATE_SETTING = "UpdateSetting";

	/**
	 * The instruction parameter for setting key.
	 *
	 * @since 1.3
	 */
	String PARAM_UPDATE_SETTING_KEY = "key";

	/**
	 * The instruction parameter for setting type.
	 *
	 * @since 1.3
	 */
	String PARAM_UPDATE_SETTING_TYPE = "type";

	/**
	 * The instruction parameter for setting value.
	 *
	 * @since 1.3
	 */
	String PARAM_UPDATE_SETTING_VALUE = "value";

	/**
	 * The instruction parameter for setting flags.
	 *
	 * @since 1.3
	 */
	String PARAM_UPDATE_SETTING_FLAGS = "flags";

	/**
	 * Get a list of all possible non-factory setting providers.
	 *
	 * @return list of setting providers (never {@literal null})
	 */
	List<SettingSpecifierProvider> getProviders();

	/**
	 * Get a list of all possible non-factory setting providers.
	 *
	 * @param filter
	 *        the search filter; the filter is applied to the factory service
	 *        properties
	 * @return list of setting providers (never {@literal null})
	 * @since 1.6
	 */
	List<SettingSpecifierProvider> getProviders(SearchFilter filter);

	/**
	 * Get a list of all possible setting provider factories.
	 *
	 * @return list of setting provider factories (never {@literal null})
	 */
	List<SettingSpecifierProviderFactory> getProviderFactories();

	/**
	 * Get a filtered list of all possible setting provider factories.
	 *
	 * @param filter
	 *        the search filter; the filter is applied to the factory service
	 *        properties
	 * @return list of setting provider factories (never {@literal null})
	 * @since 1.6
	 */
	List<SettingSpecifierProviderFactory> getProviderFactories(SearchFilter filter);

	/**
	 * Get a specific factory for a given UID.
	 *
	 * @param factoryUid
	 *        the factory UID to get the providers for
	 *
	 * @return the factory, or {@literal null} if not available
	 */
	SettingSpecifierProviderFactory getProviderFactory(String factoryUid);

	/**
	 * Enable a provider factory instance.
	 *
	 * @param factoryUid
	 *        the factory UID to enable
	 * @param instanceUid
	 *        the instance UID to enable
	 * @since 2.1
	 */
	void enableProviderFactoryInstance(String factoryUid, String instanceUid);

	/**
	 * Disable a provider factory instance.
	 *
	 * @param factoryUid
	 *        the factory UID to disable
	 * @param instanceUid
	 *        the instance UID to disable
	 * @since 2.1
	 */
	void disableProviderFactoryInstance(String factoryUid, String instanceUid);

	/**
	 * Add a new factory instance, and return the new instance ID.
	 *
	 * @param factoryUid
	 *        the factory UID to create the new instance for
	 * @return the new instance ID
	 */
	String addProviderFactoryInstance(String factoryUid);

	/**
	 * Add a new factory instance, and return the new instance ID.
	 *
	 * <p>
	 * If {@code instanceUid} is provided and an instance already exists for
	 * that ID, this will have the same effect as if
	 * {@link #enableProviderFactoryInstance(String, String)} was called.
	 * </p>
	 *
	 * @param factoryUid
	 *        the factory UID to create the new instance for
	 * @param instanceUid
	 *        the instance UID to create the new instance for, or
	 *        {@literal null} to automatically assign one
	 * @return the new instance ID
	 * @since 2.1
	 */
	default String addProviderFactoryInstance(String factoryUid, String instanceUid) {
		return addProviderFactoryInstance(factoryUid);
	}

	/**
	 * Delete an existing factory instance.
	 *
	 * @param factoryUid
	 *        the factory UID to create the new instance for
	 * @param instanceUid
	 *        the instance UID to create the new instance for
	 */
	void deleteProviderFactoryInstance(String factoryUid, String instanceUid);

	/**
	 * Reset an existing factory instance to its default values.
	 *
	 * @param factoryUid
	 *        the factory UID to reset
	 * @param instanceUid
	 *        the instance UID to reset
	 */
	void resetProviderFactoryInstance(String factoryUid, String instanceUid);

	/**
	 * Reset and delete factory instances.
	 *
	 * @param factoryUid
	 *        the factory UID of the instances to remove
	 * @param instanceUids
	 *        the instance UID to remove
	 * @since 2.2
	 */
	default void removeProviderFactoryInstances(String factoryUid, Set<String> instanceUids) {
		for ( String instanceUid : instanceUids ) {
			resetProviderFactoryInstance(factoryUid, instanceUid);
			deleteProviderFactoryInstance(factoryUid, instanceUid);
		}
	}

	/**
	 * Get all possible setting providers for a specific factory UID, grouped by
	 * instance ID.
	 *
	 * @param factoryUid
	 *        the factory UID to get the providers for
	 *
	 * @return mapping of instance IDs to associated setting providers (never
	 *         {@literal null})
	 */
	Map<String, FactorySettingSpecifierProvider> getProvidersForFactory(String factoryUid);

	/**
	 * Get the current value of a setting.
	 *
	 * @param provider
	 *        the provider of the setting
	 * @param setting
	 *        the setting
	 * @return the current setting value
	 */
	Object getSettingValue(SettingSpecifierProvider provider, SettingSpecifier setting);

	/**
	 * Update setting values.
	 *
	 * @param command
	 *        the update command
	 */
	void updateSettings(SettingsCommand command);

	/**
	 * Get a list of all available setting resource handlers.
	 *
	 * @return the handlers, never {@literal null}
	 * @since 2.1
	 */
	List<SettingResourceHandler> getSettingResourceHandlers();

	/**
	 * Get a setting resource handler.
	 *
	 * @param handlerKey
	 *        the ID if the {@link SettingResourceHandler} to get
	 * @param instanceKey
	 *        if {@code handlerKey} is a factory, the ID of the instance to
	 *        import the resources for, otherwise {@literal null}
	 * @return the resource handler, or {@literal null} if not available
	 * @since 1.4
	 */
	SettingResourceHandler getSettingResourceHandler(String handlerKey, String instanceKey);

	/**
	 * Get current setting resources.
	 *
	 * @param handlerKey
	 *        the ID if the {@link SettingResourceHandler} to get the resources
	 *        from
	 * @param instanceKey
	 *        if {@code handlerKey} is a factory, the ID of the instance to
	 *        import the resources for, otherwise {@literal null}
	 * @param settingKey
	 *        the setting ID to get the resources for
	 * @return the resources (never {@literal null})
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.4
	 */
	Iterable<Resource> getSettingResources(String handlerKey, String instanceKey, String settingKey)
			throws IOException;

	/**
	 * Import setting resources.
	 *
	 * <p>
	 * The setting resources will be persisted by this service, and should then
	 * also be passed to the appropriate {@link SettingResourceHandler} for the
	 * given {@code handlerKey} and {@code instanceKey} values by invoking
	 * {@link SettingResourceHandler#applySettingResources(String, Iterable)}
	 * with {@code settingKey} and the persisted resources.
	 * </p>
	 *
	 * @param handlerKey
	 *        the ID if the {@link SettingResourceHandler} to import to
	 * @param instanceKey
	 *        if {@code handlerKey} is a factory, the ID of the instance to
	 *        import the resources for, otherwise {@literal null}
	 * @param settingKey
	 *        the setting ID to import the resources for
	 * @param resources
	 *        the resources to import
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.4
	 */
	void importSettingResources(String handlerKey, String instanceKey, String settingKey,
			Iterable<Resource> resources) throws IOException;

	/**
	 * Remove setting resources.
	 *
	 * <p>
	 * The persisted setting resources will be removed by this service.
	 * </p>
	 *
	 * @param handlerKey
	 *        the ID if the {@link SettingResourceHandler} to remove from
	 * @param instanceKey
	 *        if {@code handlerKey} is a factory, the ID of the instance to
	 *        remove the resources for, otherwise {@literal null}
	 * @param settingKey
	 *        the setting ID to remove the resources for
	 * @param resources
	 *        the resources to remove
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 2.3
	 */
	void removeSettingResources(String handlerKey, String instanceKey, String settingKey,
			Iterable<Resource> resources) throws IOException;

	/**
	 * Export all settings as CSV formatted text.
	 *
	 * @param out
	 *        the output stream
	 * @throws IOException
	 *         if any IO error occurs
	 */
	void exportSettingsCSV(Writer out) throws IOException;

	/**
	 * Export all settings matching a filter as CSV formatted text.
	 *
	 * @param filter
	 *        the filter, or {@literal null} for all available settings
	 * @param out
	 *        the output stream
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 2.4
	 */
	default void exportSettingsCSV(SettingsFilter filter, Writer out) throws IOException {
		exportSettingsCSV(out);
	}

	/**
	 * Get a list of all available settings for a given factory and/or instance
	 * ID.
	 *
	 * @param factoryUid
	 *        the UID of the factory to get, or {@literal null} for a
	 *        non-factory component
	 * @param instanceUid
	 *        if UID of the instance
	 * @return the available settings, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if both arguments are {@literal null}
	 * @since 2.1
	 */
	List<Setting> getSettings(String factoryUid, String instanceUid);

	/**
	 * Get a list of available setting notes for a given key.
	 *
	 * @param key
	 *        the setting key to get the notes for
	 * @return the available notes, never {@literal null}
	 * @since 2.6
	 */
	List<SettingNote> notesForKey(String key);

	/**
	 * Save a set of notes.
	 *
	 * <p>
	 * Only the note values will be persisted, all other properties are ignored.
	 * </p>
	 *
	 * @param command
	 *        the notes to save
	 */
	void saveNotes(SettingsCommand command);

	/**
	 * Import all settings from a CSV formatted text stream.
	 *
	 * @param in
	 *        the input stream
	 * @throws IOException
	 *         if any IO error occurs
	 */
	void importSettingsCSV(Reader in) throws IOException;

	/**
	 * Import all settings from a CSV formatted text stream, with options.
	 *
	 * @param in
	 *        The input stream to import.
	 * @param options
	 *        The import options.
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.2
	 */
	void importSettingsCSV(Reader in, SettingsImportOptions options) throws IOException;

	/**
	 * Create a backup of all settings, and return a backup object if the backup
	 * was performed.
	 *
	 * <p>
	 * A new backup need not be created if the settings are unchanged. In that
	 * case, or if this method does not create a backup for any reason, this
	 * method should return {@literal null}.
	 * </p>
	 *
	 * @return the backup object, or {@literal null} if no backup created
	 */
	@SuppressWarnings("deprecation")
	SettingsBackup backupSettings();

	/**
	 * Get a collection of all known settings backups.
	 *
	 * @return the backups, never {@literal null}
	 */
	@SuppressWarnings("deprecation")
	Collection<SettingsBackup> getAvailableBackups();

	/**
	 * Get a {@link Reader} to the backup data for a given SettingsBackup
	 * object.
	 *
	 * @param backup
	 *        the backup to get the Reader for
	 * @return the Reader, or {@literal null} if the backup cannot be found
	 */
	@SuppressWarnings("deprecation")
	Reader getReaderForBackup(SettingsBackup backup);
}
