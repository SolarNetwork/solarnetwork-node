/* ==================================================================
 * DatumFilterSupport.java - 8/08/2017 3:22:32 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.std;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.dao.TransientSettingDao;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * Support class for sample transformers.
 * 
 * @author matt
 * @version 1.1
 * @since 2.0
 */
public class DatumFilterSupport extends BaseDatumFilterSupport implements SettingsChangeObserver {

	/**
	 * A setting key template, takes a single string parameter (the datum source
	 * ID).
	 */
	public static final String SETTING_KEY_TEMPLATE = "%s/valueCaptured";

	private TransientSettingDao transientSettingDao;
	private String settingKey;
	private boolean excludeBaseIdentifiableSettings;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	public DatumFilterSupport() {
		super();
		setUid(DEFAULT_UID);
		setSettingKey(String.format(SETTING_KEY_TEMPLATE, DEFAULT_UID));
	}

	/**
	 * Call to initialize the instance after properties are configured.
	 */
	public void init() {
		configurationChanged(null);
	}

	@Override
	public void configurationChanged(Map<String, Object> props) {
		setSettingKey(String.format(SETTING_KEY_TEMPLATE, getUid() != null ? getUid() : DEFAULT_UID));
	}

	/**
	 * Get settings for the configurable properties of {@code Identifiable}.
	 * 
	 * <p>
	 * Empty strings are used for the default {@code uid} and {@code groupUid}
	 * setting values. No settings are returned if
	 * {@code excludeBaseIdentifiableSettings} is {@literal true}.
	 * </p>
	 * 
	 * @return the settings
	 * @see BaseIdentifiable#baseIdentifiableSettings(String, String, String)
	 * @since 1.3
	 */
	public List<SettingSpecifier> baseIdentifiableSettings() {
		if ( excludeBaseIdentifiableSettings ) {
			return new ArrayList<>(4);
		}
		return baseIdentifiableSettings("", "", "");
	}

	/**
	 * Test if any configuration in a set matches a string value.
	 * 
	 * @param configs
	 *        the configurations to use
	 * @param value
	 *        the value to test
	 * @param emptyPatternMatches
	 *        {@literal true} if a {@literal null} regular expression is treated
	 *        as a match (thus matching any value)
	 * @return the first config that matches, or {@literal null} if none do
	 */
	public static PropertyFilterConfig findMatch(final PropertyFilterConfig[] configs,
			final String value, final boolean emptyPatternMatches) {
		if ( configs == null || configs.length < 1 || value == null ) {
			return null;
		}
		for ( PropertyFilterConfig config : configs ) {
			final Pattern pat = (config != null ? config.getNamePattern() : null);
			if ( pat == null ) {
				if ( emptyPatternMatches ) {
					return config;
				}
				continue;
			}
			if ( pat.matcher(value).find() ) {
				return config;
			}
		}
		return null;
	}

	/**
	 * Test if a property should be limited based on a configuration test.
	 * 
	 * @param config
	 *        the configuration to test
	 * @param lastSeenKey
	 *        the key to use for the last seen date
	 * @param lastSeenMap
	 *        a map of string keys to string values, where the values are; if
	 *        {@literal null} then {@literal false} will be returned hex-encoded
	 *        epoch date values (long)
	 * @param now
	 *        the date of the property
	 * @return if the property should be limited, {@literal null}; otherwise the
	 *         last saved date of that property
	 * @since 1.1
	 */
	protected Instant shouldLimitByFrequency(PropertyFilterConfig config, final String lastSeenKey,
			final ConcurrentMap<String, Instant> lastSeenMap, final Instant now) {
		return shouldLimitByFrequency((config != null ? config.getFrequency() : null), lastSeenKey,
				lastSeenMap, now);
	}

	/**
	 * Test if a property should be limited based on a configuration test.
	 * 
	 * @param frequency
	 *        the throttle frequency
	 * @param lastSeenKey
	 *        the key to use for the last seen date
	 * @param lastSeenMap
	 *        a map of string keys to string values, where the values are; if
	 *        {@literal null} then {@literal false} will be returned hex-encoded
	 *        epoch date values (long)
	 * @param now
	 *        the date of the property
	 * @return if the property should be limited, {@literal null}; otherwise the
	 *         last saved date of that property
	 * @since 1.1
	 */
	protected Instant shouldLimitByFrequency(Integer frequency, final String lastSeenKey,
			final ConcurrentMap<String, Instant> lastSeenMap, final Instant now) {
		Instant result = null;
		if ( lastSeenMap != null && lastSeenKey != null && frequency != null
				&& frequency.intValue() > 0 ) {
			Instant lastSaveTime = lastSeenMap.get(lastSeenKey);
			if ( lastSaveTime != null && lastSaveTime.plusSeconds(frequency).isAfter(now) ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Key [{}] was seen in the past {}s ({}s ago); filtering", lastSeenKey,
							frequency,
							(lastSaveTime != null ? ChronoUnit.MILLIS.between(lastSaveTime, now) : -1L));
				}
			} else {
				result = (lastSaveTime != null ? lastSaveTime : now);
			}
		}
		return result;
	}

	/**
	 * Get a setting key value for saving settings.
	 * 
	 * <p>
	 * This returns a static value. This is expected to be combined with a
	 * unique setting type key to distinguish between different datum sources
	 * and/or datum properties.
	 * </p>
	 * 
	 * @return a setting key to use, never {@literal null}
	 */
	protected String settingKey() {
		return settingKey;
	}

	/**
	 * Get transient settings for a given key.
	 * 
	 * <p>
	 * This method requires the {@link #getTransientSettingDao()} to be
	 * available.
	 * </p>
	 * 
	 * @param settingKey
	 *        the setting key to get transient settings
	 * @return the settings map, never {@literal null}
	 * @throws RuntimeException
	 *         if no {@link TransientSettingDao} is available
	 * @since 1.1
	 */
	protected <V> ConcurrentMap<String, V> transientSettings(String settingKey) {
		final TransientSettingDao dao = getTransientSettingDao();
		if ( dao == null ) {
			throw new RuntimeException("No TransientSettingDao available.");
		}
		return transientSettingDao.settings(settingKey);
	}

	/**
	 * Save a "last seen" setting.
	 * 
	 * @param newLastSeenValue
	 *        the "last seen" date to use
	 * @param lastSeenKey
	 *        the last seen key to save the date to
	 * @param oldLastSeenValue
	 *        the previous "last seen" value, or {@literal null}
	 * @param settings
	 *        the settings map to save the last seen date to
	 * @since 1.1
	 */
	protected void saveLastSeenSetting(final Instant newLastSeenValue, final String lastSeenKey,
			final Instant oldLastSeenValue, ConcurrentMap<String, Instant> settings) {
		if ( (settings.putIfAbsent(lastSeenKey, newLastSeenValue) == null) || (oldLastSeenValue != null
				&& settings.replace(lastSeenKey, oldLastSeenValue, newLastSeenValue)) ) {
			log.debug("Saved {} last seen date: {}", lastSeenKey, newLastSeenValue);
		}
	}

	/**
	 * Get this filter instance, for backwards setting compatibility.
	 * 
	 * @return this instance
	 */
	public DatumFilterSupport getSampleTransformer() {
		return this;
	}

	/**
	 * Set the setting key to use for saved settings.
	 * 
	 * @param settingKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if {@code settingKey} is {@literal null}
	 */
	public void setSettingKey(String settingKey) {
		if ( settingKey == null ) {
			throw new IllegalArgumentException("The settingKey argument must not be null.");
		}
		this.settingKey = settingKey;
	}

	/**
	 * Get the flag to indicate the base identifiable service settings should be
	 * excluded.
	 * 
	 * @return {@literal true} to exclude base identifiable service settings;
	 *         defaults to {@literal false}
	 */
	public boolean isExcludeBaseIdentifiableSettings() {
		return excludeBaseIdentifiableSettings;
	}

	/**
	 * Set the flag to indicate the base identifiable service settings should be
	 * excluded.
	 * 
	 * @param excludeBaseIdentifiableSettings
	 *        {@literal true} to exclude base identifiable service settings;
	 *        defaults to {@literal false}
	 */
	public void setExcludeBaseIdentifiableSettings(boolean excludeBaseIdentifiableSettings) {
		this.excludeBaseIdentifiableSettings = excludeBaseIdentifiableSettings;
	}

	/**
	 * Get the transient setting DAO.
	 * 
	 * @return the DAO, or {@literal null}
	 * @since 1.1
	 */
	public TransientSettingDao getTransientSettingDao() {
		return transientSettingDao;
	}

	/**
	 * Set the transient setting DAO.
	 * 
	 * @param transientSettingDao
	 *        the DAO to set
	 * @since 1.1
	 */
	public void setTransientSettingDao(TransientSettingDao transientSettingDao) {
		this.transientSettingDao = transientSettingDao;
	}

}
