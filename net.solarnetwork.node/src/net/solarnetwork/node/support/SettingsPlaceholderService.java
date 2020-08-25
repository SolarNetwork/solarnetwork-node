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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.PlaceholderService;
import net.solarnetwork.node.dao.SettingDao;
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
 * @version 1.0
 */
public class SettingsPlaceholderService implements PlaceholderService {

	/** The setting key to use for placeholder parameter settings. */
	public static final String SETTING_KEY = "placeholder";

	private final OptionalService<SettingDao> settingDao;
	private Path staticPropertiesPath;

	private Map<String, ?> staticProps;

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
		String resolved = s;

		List<KeyValuePair> kp = settingValues();
		Map<String, ?> props = staticPropValues();

		if ( (kp != null && !kp.isEmpty()) || (props != null && !props.isEmpty()) ) {
			Map<String, ?> mergedProps = new AbstractMap<String, Object>() {

				@Override
				public Object get(Object key) {
					// method argument takes highest precedence
					if ( parameters != null && parameters.containsKey(key) ) {
						return parameters.get(key);
					}
					// DAO takes second precedence
					if ( kp != null ) {
						for ( KeyValuePair p : kp ) {
							if ( key.equals(p.getKey()) ) {
								return p.getValue();
							}
						}
					}
					// static takes lowest precedence
					return (props != null ? props.get(key) : null);
				}

				@Override
				public Set<Entry<String, Object>> entrySet() {
					return null;
				}
			};
			resolved = StringUtils.expandTemplateString(resolved, mergedProps);
		}

		return resolved;
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
			return dao.getSettingValues(SETTING_KEY);
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

	private synchronized Map<String, Object> loadStaticProps() {
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

}
