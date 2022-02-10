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

package net.solarnetwork.node.service.support;

import static net.solarnetwork.service.OptionalService.service;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.CachedResult;
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
 * @version 2.3
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

	/**
	 * The default {@code daoRetryCount} property value.
	 * 
	 * @since 2.2
	 */
	public static final int DEFAULT_DAO_RETRY_COUNT = 3;

	private final OptionalService<SettingDao> settingDao;
	private Path staticPropertiesPath;
	private int cacheSeconds = DEFAULT_CACHE_SECONDS;
	private AsyncTaskExecutor taskExecutor;
	private int daoRetryCount = DEFAULT_DAO_RETRY_COUNT;

	private Map<String, ?> staticProps;

	private volatile CachedResult<Map<String, ?>> placeholdersCache;
	private volatile Future<Map<String, ?>> refreshCacheTask;

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
		String result = StringUtils.expandTemplateString(resolved, placeholders);
		if ( log.isTraceEnabled() && !result.equals(s) ) {
			log.trace("Placeholders in [{}] resolved to [{}]", s, result);
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> void mapPlaceholders(Map<String, T> destination,
			Function<Stream<Entry<String, ?>>, Stream<Entry<String, T>>> filter) {
		Map<String, ?> placeholders = allPlaceholders(null);
		if ( placeholders == null || placeholders.isEmpty() ) {
			return;
		}
		if ( filter == null ) {
			destination.putAll((Map<String, T>) placeholders);
		} else {
			@SuppressWarnings("rawtypes")
			Stream<Entry<String, ?>> input = (Stream) placeholders.entrySet().stream();
			Stream<Entry<String, T>> output = filter.apply(input);
			output.forEach(e -> {
				destination.put(e.getKey(), e.getValue());
			});
		}
	}

	@Override
	public <T> void copyPlaceholders(Map<String, T> destination, Predicate<Entry<String, T>> filter) {
		@SuppressWarnings("unchecked")
		Map<String, T> placeholders = (Map<String, T>) allPlaceholders(null);
		if ( placeholders == null || placeholders.isEmpty() ) {
			return;
		}
		if ( filter == null ) {
			destination.putAll(placeholders);
		} else {
			placeholders.entrySet().stream().filter(filter).forEach(e -> {
				destination.put(e.getKey(), e.getValue());
			});
		}
	}

	private Map<String, ?> allPlaceholders(Map<String, ?> parameters) {
		Map<String, ?> result = null;
		if ( cacheSeconds > 0 && settingDao.service() != null ) {
			final CachedResult<Map<String, ?>> cached = this.placeholdersCache;
			result = (cached != null ? cached.getResult() : null);
			if ( result == null || cached == null || !cached.isValid() ) {
				final AsyncTaskExecutor executor = this.taskExecutor;
				synchronized ( this ) {
					if ( refreshCacheTask == null || refreshCacheTask.isDone() ) {
						final Callable<Map<String, ?>> task = new CacheRefreshTask();
						try {
							if ( executor != null && result != null ) {
								// refresh cache asynchronously and return expired data
								refreshCacheTask = executor.submit(task);
							} else {
								log.debug("Loading initial setting placeholder values");
								result = task.call();
							}
						} catch ( Exception e ) {
							log.error("Error loading placeholders: {}", e);
						}
					}
				}
			}
		}
		if ( result == null ) {
			result = allPlaceholdersWithoutCache();
		}
		return placeholdersMergedWithParameters(result, parameters);
	}

	private final class CacheRefreshTask implements Callable<Map<String, ?>> {

		@Override
		public Map<String, ?> call() throws Exception {
			final CachedResult<Map<String, ?>> cached;
			synchronized ( SettingsPlaceholderService.this ) {
				cached = placeholdersCache;
				if ( cached != null && cached.isValid() ) {
					return cached.getResult();
				}
			}
			log.debug("Refreshing placeholder cache in background");
			try {
				Map<String, ?> placeholders = allPlaceholdersWithoutCache();
				synchronized ( SettingsPlaceholderService.this ) {
					placeholdersCache = new CachedResult<Map<String, ?>>(placeholders, cacheSeconds,
							TimeUnit.SECONDS);
				}
				log.info("Cached {} placeholder values", placeholders.size());
				return placeholders;
			} catch ( Exception e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				log.warn("Error refreshing placeholders from SettingDao; returning cached values: {}",
						root.toString());
			}
			return (cached != null ? cached.getResult() : null);
		}

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
			return settingValues(dao, daoRetryCount);
		} else {
			log.warn("SettingDao not available for resolving placeholders.");
		}
		return null;
	}

	private List<KeyValuePair> settingValues(SettingDao dao, int i) {
		try {
			return dao.getSettingValues(SETTING_KEY);
		} catch ( TransientDataAccessException e ) {
			// try this again
			if ( i > 0 ) {
				log.warn(
						"Transient exception loading placeholder values from SettingDao, will retry up to {} more times: {}",
						i, e);
				return settingValues(dao, i - 1);
			}
		} catch ( Exception e ) {
			Throwable t = e;
			while ( t.getCause() != null ) {
				t = t.getCause();
			}
			log.warn("Exception loading placeholder values from SettingDao: {}", t.toString());
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
	 * @since 1.1
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
	 * @since 1.1
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheSeconds = cacheSeconds;
	}

	/**
	 * Get the task executor.
	 * 
	 * @return the task executor
	 * @since 1.1
	 */
	public AsyncTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	/**
	 * An executor to handle cache refreshing with.
	 * 
	 * <p>
	 * If configured then cache refresh operations will occur asynchronously
	 * after expiring.
	 * </p>
	 * 
	 * @param taskExecutor
	 *        a task executor
	 * @since 1.1
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Get the maximum number of time to retry transient DAO exceptions when
	 * loading placeholder values from {@link SettingDao}.
	 * 
	 * @return the maximum number of retry times
	 * @since 2.2
	 */
	public int getDaoRetryCount() {
		return daoRetryCount;
	}

	/**
	 * Set the number of time to retry transient DAO exceptions when loading
	 * placeholder values from {@link SettingDao}.
	 * 
	 * @param daoRetryCount
	 *        the maximum number of retry times, or {@literal 0} to disable
	 * @since 2.2
	 */
	public void setDaoRetryCount(int daoRetryCount) {
		this.daoRetryCount = daoRetryCount;
	}

}
