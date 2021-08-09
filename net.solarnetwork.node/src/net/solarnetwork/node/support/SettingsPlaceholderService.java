/* ==================================================================
 * SettingsPlaceholderService.java - 25/08/2020 10:48:10 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.support;

import static net.solarnetwork.util.OptionalService.service;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.PlaceholderService;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Implementation of {@link PlaceholderService} that manages parameter values
 * via a {@link SettingDao}.
 * 
 * <p>
 * The {@link StringUtils#expandTemplateString(String, Map)} method is used for
 * parameter resolution, so parameters take the form <code>{name:default}</code>
 * where <i>:default</i> is optional.
 * </p>
 * 
 * <p>
 * This service can also load "static" parameter values from a directory of
 * property files or a single property file. Note that these properties are
 * loaded lazily, the first time {@link #resolvePlaceholders(String, Map)} is
 * called, and then cached for the life of the instance.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class SettingsPlaceholderService implements PlaceholderService {

	/** The setting key to use for placeholder parameter settings. */
	public static final String SETTING_KEY = "placeholder";

	/**
	 * The default {@code cacheSeconds} property value.
	 * 
	 * @since 1.1
	 */
	public static final int DEFAULT_CACHE_SECONDS = 20;

	private final OptionalService<SettingDao> settingDao;
	private Path staticPropertiesPath;
	private int cacheSeconds = DEFAULT_CACHE_SECONDS;

	private Map<String, ?> staticProps;

	private CachedResult<Map<String, ?>> placeholdersCache;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param settingDao
	 *        the DAO to persist placeholders with
	 */
	public SettingsPlaceholderService(OptionalService<SettingDao> settingDao) {
		super();
		this.settingDao = settingDao;
	}

	@Override
	public String resolvePlaceholders(String s, Map<String, ?> parameters) {
		// don't try to resolve null/empty input
		if ( s == null || s.isEmpty() ) {
			return s;
		}

		// short-circuit check if there even are any placeholders to resolve
		if ( !StringUtils.NAMES_PATTERN.matcher(s).find() ) {
			return s;
		}

		String resolved = s;
		Map<String, ?> placeholders = allPlaceholders(parameters);
		if ( placeholders != null ) {
			resolved = StringUtils.expandTemplateString(resolved, placeholders);
		}

		return resolved;
	}

	private Map<String, ?> allPlaceholders(Map<String, ?> parameters) {
		Map<String, ?> result = null;
		if ( cacheSeconds > 0 ) {
			final CachedResult<Map<String, ?>> cached = this.placeholdersCache;
			result = (cached != null ? cached.getResult() : null);
			if ( cached == null || !cached.isValid() ) {
				try {
					synchronized ( this ) {
						result = allPlaceholdersWithoutCache();
						this.placeholdersCache = new CachedResult<Map<String, ?>>(result, cacheSeconds,
								TimeUnit.SECONDS);
					}
				} catch ( Exception e ) {
					Throwable root = e;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					log.warn(
							"Error refreshing placeholders from SettingDao; returning cached values: {}",
							root.toString());
				}
			}
		} else {
			result = allPlaceholdersWithoutCache();
		}
		return placeholdersMergedWithParameters(result, parameters);
	}

	private Map<String, ?> placeholdersMergedWithParameters(Map<String, ?> placeholders,
			Map<String, ?> parameters) {
		// try to return the minimum level of placeholders, if some levels are null
		if ( parameters == null ) {
			return placeholders;
		} else if ( placeholders == null ) {
			return parameters;
		}
		return new AbstractMap<String, Object>() {

			@Override
			public Object get(Object key) {
				// method argument takes highest precedence
				if ( parameters != null && parameters.containsKey(key) ) {
					return parameters.get(key);
				}
				// others take second precedence
				return placeholders.get(key);
			}

			@Override
			public Set<Entry<String, Object>> entrySet() {
				return null;
			}
		};
	}

	private Map<String, Object> allPlaceholdersWithoutCache() {
		List<KeyValuePair> kp = settingValues();
		Map<String, ?> props = staticPropValues();

		if ( (kp != null && !kp.isEmpty()) || (props != null && !props.isEmpty()) ) {
			Map<String, Object> mergedProps = new HashMap<>(
					(props != null ? props.size() : 1) + (kp != null ? kp.size() : 1));
			// static takes lowest precedence
			if ( props != null ) {
				mergedProps.putAll(props);
			}
			// DAO takes second precedence
			if ( kp != null ) {
				for ( KeyValuePair p : kp ) {
					mergedProps.put(p.getKey(), p.getValue());
				}
			}

			return mergedProps;
		}
		return null;
	}

	private Map<String, ?> staticPropValues() {
		Map<String, ?> props = this.staticProps;
		if ( props == null ) {
			props = loadStaticProps();
		}
		return props;
	}

	private List<KeyValuePair> settingValues() {
		SettingDao dao = service(settingDao);
		if ( dao != null ) {
			try {
				return dao.getSettingValues(SETTING_KEY);
			} catch ( Exception e ) {
				Throwable t = e;
				while ( t.getCause() != null ) {
					t = t.getCause();
				}
				log.warn("Exception loading placeholder values from SettingDao: {}", t.toString());
			}
		}
		return null;
	}

	@Override
	public void registerParameters(Map<String, ?> parameters) {
		if ( parameters == null || parameters.isEmpty() ) {
			return;
		}
		SettingDao dao = service(settingDao);
		if ( dao == null ) {
			log.warn("SettingDao not avaialble for registering parameters: {}", parameters);
			return;
		}
		for ( Map.Entry<String, ?> me : parameters.entrySet() ) {
			if ( me.getKey() == null || me.getValue() == null ) {
				continue;
			}
			dao.storeSetting(SETTING_KEY, me.getKey(), me.getValue().toString());
		}
	}

	private synchronized Map<String, ?> loadStaticProps() {
		if ( this.staticProps != null ) {
			return this.staticProps;
		}
		Map<String, Object> params = new HashMap<>(16);
		Path p = this.staticPropertiesPath;
		if ( p != null ) {
			if ( Files.isDirectory(p) ) {
				try {
					Files.list(p).forEach(e -> loadProps(e, params));
				} catch ( IOException e ) {
					log.error("Unable to list properties files in {}: {}", p, e.getMessage());
				}
			} else if ( Files.exists(p) ) {
				loadProps(p, params);
			}
		}
		this.staticProps = params;
		return params;
	}

	private void loadProps(Path p, Map<String, Object> dest) {
		if ( p.getFileName().toString().endsWith(".properties") ) {
			log.info("Loading placeholder properties from {}", p);
			Properties props = new Properties();
			try (InputStream in = new BufferedInputStream(Files.newInputStream(p))) {
				props.load(in);
				for ( Map.Entry<Object, Object> me : props.entrySet() ) {
					if ( me.getKey() != null ) {
						dest.put(me.getKey().toString(), me.getValue());
					}
				}
			} catch ( IOException e ) {
				log.error("Unable to load properties file {} (will ignore): {}", p, e.getMessage());
			}
		}
	}

	/**
	 * Get the path to the static properties directory.
	 * 
	 * @return the properties directory
	 */
	public Path getStaticPropertiesPath() {
		return staticPropertiesPath;
	}

	/**
	 * Set a path to a directory of static properties files to load as
	 * placeholders.
	 * 
	 * <p>
	 * If this path represents a properties file, that file will be loaded as
	 * placeholder parameter values. If this path represents a directory, the
	 * directory will be searched for property files. All files will be loaded
	 * and their values made available as placeholder parameters. A property
	 * file must have a {@literal .properties} file name suffix.
	 * </p>
	 * 
	 * @param staticPropertiesPath
	 *        the directory path to set
	 */
	public void setStaticPropertiesPath(Path staticPropertiesPath) {
		this.staticPropertiesPath = staticPropertiesPath;
	}

	/**
	 * Get the cache seconds value.
	 * 
	 * @return the cache seconds; defaults to {@link #DEFAULT_CACHE_SECONDS}
	 */
	public int getCacheSeconds() {
		return cacheSeconds;
	}

	/**
	 * Set the cache seconds value.
	 * 
	 * <p>
	 * If set to a value greater than {@literal 0} then placeholder values read
	 * from {@link SettingDao} will be cached for a minimum of the given number
	 * of seconds before being refreshed. This can be very helpful when
	 * placeholders are evaluated frequently. Additionally, once cached if
	 * refreshing from the DAO fails for any reason, the previously cached
	 * values will be used.
	 * </p>
	 * 
	 * @param cacheSeconds
	 *        the cache seconds to set, or {@code 0} to disable
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheSeconds = cacheSeconds;
	}

}
