/* ==================================================================
 * SamplesTransformerSupport.java - 8/08/2017 3:22:32 PM
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

package net.solarnetwork.node.datum.samplefilter;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.Setting;
import net.solarnetwork.node.Setting.SettingFlag;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.support.BaseIdentifiable;

/**
 * Support class for sample transformers.
 * 
 * @author matt
 * @version 1.2
 */
public class SamplesTransformerSupport extends BaseIdentifiable {

	/** The default value for the {@code settingCacheSecs} property. */
	public static final int DEFAULT_SETTING_CACHE_SECS = 15;

	/**
	 * A setting key template, takes a single string parameter (the datum source
	 * ID).
	 */
	public static final String SETTING_KEY_TEMPLATE = "%s/valueCaptured";

	/**
	 * The default value for the UID property.
	 */
	public static final String DEFAULT_UID = "Default";

	/**
	 * A global cache for helping with transformers that require persistence.
	 */
	protected static final ConcurrentMap<String, ConcurrentMap<String, String>> SETTING_CACHE = new ConcurrentHashMap<String, ConcurrentMap<String, String>>(
			4, 0.9f, 1);

	private Pattern sourceId;
	private SettingDao settingDao;
	private int settingCacheSecs;
	private String settingKey;

	private final AtomicLong settingCacheExpiry = new AtomicLong(0);

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Clear the internal setting cache.
	 */
	public static void clearSettingCache() {
		SETTING_CACHE.clear();
	}

	public SamplesTransformerSupport() {
		super();
		setUid(DEFAULT_UID);
		setSettingCacheSecs(DEFAULT_SETTING_CACHE_SECS);
		setSettingKey(String.format(SETTING_KEY_TEMPLATE, DEFAULT_UID));
	}

	/**
	 * Test if a given datum's source ID matches the configured source ID
	 * pattern.
	 * 
	 * @param datum
	 *        the datum whose source ID should be tested
	 * @return {@literal true} if the datum's {@code sourceId} value matches the
	 *         configured source ID pattern, or no pattern is configured
	 * @since 1.2
	 */
	protected boolean sourceIdMatches(Datum datum) {
		Pattern sourceIdPat = getSourceIdPattern();
		if ( sourceIdPat != null ) {
			if ( datum == null || datum.getSourceId() == null
					|| !sourceIdPat.matcher(datum.getSourceId()).find() ) {
				log.trace("Datum {} does not match source ID pattern {}; not filtering", datum,
						sourceIdPat);
				return false;
			}
		}
		return true;
	}

	/**
	 * Get the source ID regex.
	 * 
	 * @return the regex
	 */
	protected Pattern getSourceIdPattern() {
		return sourceId;
	}

	/**
	 * Copy a samples object.
	 * 
	 * <p>
	 * This method copies the {@code samples} instance and the
	 * {@code instantaneous}, {@code accumulating}, {@code status}, and
	 * {@code tags} collection instances.
	 * </p>
	 * 
	 * @param samples
	 *        the samples to copy
	 * @return the copied samples instance
	 */
	public static GeneralDatumSamples copy(GeneralDatumSamples samples) {
		GeneralDatumSamples copy = new GeneralDatumSamples(
				samples.getInstantaneous() != null
						? new LinkedHashMap<String, Number>(samples.getInstantaneous())
						: null,
				samples.getAccumulating() != null
						? new LinkedHashMap<String, Number>(samples.getAccumulating())
						: null,
				samples.getStatus() != null ? new LinkedHashMap<String, Object>(samples.getStatus())
						: null);
		copy.setTags(samples.getTags() != null ? new LinkedHashSet<String>(samples.getTags()) : null);
		return copy;
	}

	/**
	 * Test if any regular expression in a set matches a string value.
	 * 
	 * @param pats
	 *        the regular expressions to use
	 * @param value
	 *        the value to test
	 * @param emptyPatternMatches
	 *        {@literal true} if a {@literal null} regular expression is treated
	 *        as a match (thus matching any value)
	 * @return {@literal true} if at least one regular expression matches
	 *         {@code value}
	 */
	public static boolean matchesAny(final Pattern[] pats, final String value,
			final boolean emptyPatternMatches) {
		if ( pats == null || pats.length < 1 || value == null ) {
			return true;
		}
		for ( Pattern pat : pats ) {
			if ( pat == null ) {
				if ( emptyPatternMatches ) {
					return true;
				}
				continue;
			}
			if ( pat.matcher(value).find() ) {
				return true;
			}
		}
		return false;
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
	 *        a map of string keys to string values, where the values are
	 *        hex-encoded epoch date values (long)
	 * @param now
	 *        the date of the property
	 * @return {@literal true} if the property should be limited
	 */
	protected boolean shouldLimitByFrequency(PropertyFilterConfig config, final String lastSeenKey,
			final ConcurrentMap<String, String> lastSeenMap, final long now) {
		boolean limit = false;
		if ( config.getFrequency() != null && config.getFrequency().intValue() > 0 ) {
			final long offset = config.getFrequency() * 1000L;
			String lastSaveSetting = lastSeenMap.get(lastSeenKey);
			long lastSaveTime = (lastSaveSetting != null ? Long.valueOf(lastSaveSetting, 16) : 0);
			if ( lastSaveTime > 0 && lastSaveTime + offset > now ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Property {} was seen in the past {}s ({}s ago); filtering", lastSeenKey,
							offset, (now - lastSaveTime) / 1000.0);
				}
				limit = true;
			}
		}
		return limit;
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
	 * Load all available settings for a given key.
	 * 
	 * <p>
	 * This method caches the results for at most {@code settingCacheSecs}
	 * seconds.
	 * </p>
	 * 
	 * @param key
	 *        the key to load
	 * @return the settings, mapped into a {@code ConcurrentMap} using the
	 *         setting {@code type} for keys and the setting {@code value} for
	 *         the associated values
	 */
	@SuppressWarnings("deprecation")
	protected ConcurrentMap<String, String> loadSettings(String key) {
		ConcurrentMap<String, String> result = SETTING_CACHE.get(key);
		if ( result == null ) {
			SETTING_CACHE.putIfAbsent(key, new ConcurrentHashMap<String, String>(16, 0.9f, 1));
			result = SETTING_CACHE.get(key);
		}
		final long expiry = settingCacheExpiry.get();
		if ( expiry > System.currentTimeMillis() ) {
			return result;
		}
		SettingDao dao = getSettingDao();
		if ( dao != null ) {
			List<net.solarnetwork.node.support.KeyValuePair> pairs = dao.getSettings(key);
			if ( pairs != null && !pairs.isEmpty() ) {
				for ( net.solarnetwork.node.support.KeyValuePair pair : pairs ) {
					result.put(pair.getKey(), pair.getValue());
				}
			}
		}
		settingCacheExpiry.compareAndSet(expiry, System.currentTimeMillis() + settingCacheSecs * 1000L);
		return result;
	}

	/**
	 * Compile a set of regular expression strings.
	 * 
	 * <p>
	 * The returned array will be the same length as {@code expressions} and the
	 * compiled {@link Pattern} objects in the same order. If any expression
	 * fails to compile, a warning log will be emitted and a {@literal null}
	 * value will be returned for that expression.
	 * </p>
	 * 
	 * @param expressions
	 *        the expressions to compile
	 * @return the regular expressions
	 */
	protected Pattern[] patterns(String[] expressions) {
		Pattern[] pats = null;
		if ( expressions != null ) {
			final int len = expressions.length;
			pats = new Pattern[len];
			for ( int i = 0; i < len; i++ ) {
				if ( expressions[i] == null || expressions[i].length() < 1 ) {
					continue;
				}
				try {
					pats[i] = Pattern.compile(expressions[i], Pattern.CASE_INSENSITIVE);
				} catch ( PatternSyntaxException e ) {
					log.warn("Error compiling includePatterns regex [{}]", expressions[i], e);
				}
			}
		}
		return pats;
	}

	/**
	 * Save a "last seen" setting.
	 * 
	 * @param seenDate
	 *        the "last seen" date to use
	 * @param settingKey
	 *        the setting key to save the date to
	 * @param lastSeenKey
	 *        the setting type key to save the date to
	 * @param oldLastSeenValue
	 *        the previous "last seen" value, or {@literal null}
	 * @param lastSeenMap
	 *        a map to save the last seen date to, as a hex-encoded string
	 */
	protected void saveLastSeenSetting(final long seenDate, final String settingKey,
			final String lastSeenKey, final String oldLastSeenValue,
			ConcurrentMap<String, String> lastSeenMap) {
		// save the new setting date
		final String newLastSeenValue = Long.toString(seenDate, 16);
		if ( (oldLastSeenValue == null && lastSeenMap.putIfAbsent(lastSeenKey, newLastSeenValue) == null)
				|| (oldLastSeenValue != null
						&& lastSeenMap.replace(lastSeenKey, oldLastSeenValue, newLastSeenValue)) ) {
			log.debug("Saving {} last seen date: {}", lastSeenKey, seenDate);
			Setting s = new Setting(settingKey, lastSeenKey, Long.toString(seenDate, 16),
					EnumSet.of(SettingFlag.Volatile, SettingFlag.IgnoreModificationDate));
			getSettingDao().storeSetting(s);
		}
	}

	/**
	 * Get a description of this service.
	 * 
	 * @return the description
	 * @since 1.2
	 */
	public String getDescription() {
		String uid = getUid();
		MessageSource msg = getMessageSource();
		String title = msg.getMessage("title", null, getClass().getSimpleName(), Locale.getDefault());
		if ( uid != null && !DEFAULT_UID.equals(uid) ) {
			return String.format("%s (%s)", uid, title);
		}
		return title;
	}

	/**
	 * Get the source ID pattern.
	 * 
	 * @return The pattern.
	 */
	public String getSourceId() {
		return (sourceId != null ? sourceId.pattern() : null);
	}

	/**
	 * Set a source ID pattern to match samples against.
	 * 
	 * Samples will only be considered for filtering if
	 * {@link Datum#getSourceId()} matches this pattern.
	 * 
	 * The {@code sourceIdPattern} must be a valid {@link Pattern} regular
	 * expression. The expression will be allowed to match anywhere in
	 * {@link Datum#getSourceId()} values, so if the pattern must match the full
	 * value only then use pattern positional expressions like {@code ^} and
	 * {@code $}.
	 * 
	 * @param sourceIdPattern
	 *        The source ID regex to match. Syntax errors in the pattern will be
	 *        ignored and a {@code null} value will be set instead.
	 */
	public void setSourceId(String sourceIdPattern) {
		try {
			this.sourceId = (sourceIdPattern != null
					? Pattern.compile(sourceIdPattern, Pattern.CASE_INSENSITIVE)
					: null);
		} catch ( PatternSyntaxException e ) {
			log.warn("Error compiling regex [{}]", sourceIdPattern, e);
			this.sourceId = null;
		}
	}

	/**
	 * Get the SettingDao to use.
	 * 
	 * @return the DAO
	 */
	public SettingDao getSettingDao() {
		return this.settingDao;
	}

	/**
	 * Set the {@link SettingDao} to use to persist "last seen" time stamps
	 * with.
	 * 
	 * @param settingDao
	 *        the DAO to set
	 */
	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

	/**
	 * The maximum number of seconds to use cached {@link SettingDao} data when
	 * filtering datum.
	 * 
	 * <p>
	 * An internal cache is used so that when iterating over sets of datum the
	 * settings don't need to be loaded from the database each time over a very
	 * short amount of time.
	 * </p>
	 * 
	 * @param settingCacheSecs
	 *        the settingCacheSecs to set
	 */
	public void setSettingCacheSecs(int settingCacheSecs) {
		this.settingCacheSecs = settingCacheSecs;
	}

	/**
	 * Set the setting key to use for saved settings.
	 * 
	 * @param settingKey
	 *        the key to set
	 * @throws IllegalArgumentException
	 *         if {@code settingKey} is {@literal null}
	 * @since 1.2
	 */
	public void setSettingKey(String settingKey) {
		if ( settingKey == null ) {
			throw new IllegalArgumentException("The settingKey argument must not be null.");
		}
		this.settingKey = settingKey;
	}

}
