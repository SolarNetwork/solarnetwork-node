/* ==================================================================
 * PropertyDatumFilterService.java - 28/10/2016 3:00:56 PM
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.util.StringUtils.patterns;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * {@link DatumFilterService} that can filter out sample properties based on
 * simple matching rules.
 *
 * <p>
 * If all properties of a datum are filtered out of a datum then
 * {@link #filter(Datum, DatumSamplesOperations, Map)} will return
 * {@literal null}.
 * </p>
 *
 * @author matt
 * @version 1.3
 * @since 2.0
 */
public class PropertyDatumFilterService extends DatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider, SettingsChangeObserver {

	/** The default {@code settingUid} property value. */
	public static final String DEFAULT_SETTING_UID = "net.solarnetwork.node.datum.samplefilter.simple";

	private String settingUid = DEFAULT_SETTING_UID;
	private PropertyFilterConfig[] propIncludes;
	private String[] excludes;

	private Pattern[] excludePatterns;

	/**
	 * Constructor.
	 */
	public PropertyDatumFilterService() {
		super();
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> params) {
		final long start = incrementInputStats();
		final String settingKey = settingKey();
		if ( settingKey == null ) {
			log.trace("Filter does not have a UID configured; not filtering: {}", this);
			incrementIgnoredStats(start);
			return samples;
		}

		if ( !conditionsMatch(datum, samples, params) ) {
			incrementIgnoredStats(start);
			return samples;
		}

		final PropertyFilterConfig[] incs = this.propIncludes;
		final Pattern[] excs = this.excludePatterns;

		// load all Datum "last created" settings, if we need to throttle by frequency
		final ConcurrentMap<String, Instant> lastSeenMap = transientSettings(settingKey);

		final Instant now = (datum != null && datum.getTimestamp() != null ? datum.getTimestamp()
				: Instant.now());
		log.trace(
				"Property filter [{}] examining datum {} @ {} with {} include and {} exclude configurations",
				getUid(), datum, now, (incs != null ? incs.length : 0),
				(excs != null ? excs.length : 0));

		DatumSamples copy = null;

		// handle property inclusion rules
		if ( incs != null && incs.length > 0 ) {
			for ( DatumSamplesType t : EnumSet.of(Instantaneous, Accumulating, Status) ) {
				Map<String, ?> map = samples.getSampleData(t);
				if ( map != null ) {
					for ( String propName : map.keySet() ) {
						PropertyFilterConfig match = findMatch(incs, propName, true);
						String lastSeenKey = (lastSeenMap != null && match != null
								? datum.getSourceId() + ';' + propName
								: null);
						Instant lastFilterDate = shouldLimitByFrequency(match, lastSeenKey, lastSeenMap,
								now);
						if ( match == null || lastFilterDate == null ) {
							if ( copy == null ) {
								copy = new DatumSamples(samples);
							}
							copy.putSampleValue(t, propName, null);
						} else if ( lastSeenMap != null && match != null ) {
							saveLastSeenSetting(match.getFrequency(), now, lastSeenKey, lastFilterDate,
									lastSeenMap);
						}
					}
				}
			}
		}

		// handle property exclusion rules
		if ( excs != null && excs.length > 0 ) {
			for ( DatumSamplesType t : EnumSet.of(Instantaneous, Accumulating, Status) ) {
				Map<String, ?> map = samples.getSampleData(t);
				if ( map != null ) {
					for ( String propName : map.keySet() ) {
						if ( matchesAny(excs, propName, false) ) {
							if ( copy == null ) {
								copy = new DatumSamples(samples);
							}
							copy.putSampleValue(t, propName, null);
						}
					}
				}
			}
		}

		// tidy up any empty maps we created during filtering
		if ( copy != null ) {
			if ( copy.getAccumulating() != null && copy.getAccumulating().isEmpty() ) {
				copy.setAccumulating(null);
			}
			if ( copy.getInstantaneous() != null && copy.getInstantaneous().isEmpty() ) {
				copy.setInstantaneous(null);
			}
			if ( copy.getStatus() != null && copy.getStatus().isEmpty() ) {
				copy.setStatus(null);
			}
			if ( copy.getAccumulating() == null && copy.getInstantaneous() == null
					&& copy.getStatus() == null ) {
				// all properties removed!
				return null;
			}
		}

		DatumSamplesOperations out = (copy != null ? copy : samples);
		incrementStats(start, samples, out);
		return out;
	}

	private void saveLastSeenSetting(final Integer limit, final Instant now, final String lastSeenKey,
			Instant oldLastSeenValue, ConcurrentMap<String, Instant> settings) {
		if ( limit == null || limit.intValue() < 1 ) {
			return;
		}
		super.saveLastSeenSetting(now, lastSeenKey, oldLastSeenValue, settings);
	}

	/**
	 * Call to initialize the instance after properties are configured.
	 */
	@Override
	public void init() {
		super.init();
	}

	@Override
	public void configurationChanged(Map<String, Object> props) {
		super.configurationChanged(props);
		// backwards compatibility support for the old String[] includes configuration
		if ( props != null && props.containsKey("includesCount") ) {
			final int includeCount = Integer.parseInt(props.get("includesCount").toString());
			if ( includeCount > 0 && propIncludes != null && propIncludes.length >= includeCount ) {
				for ( int i = 0; i < includeCount; i++ ) {
					String inc = (String) props.get("includes[" + i + "]");
					PropertyFilterConfig config = propIncludes[i];
					if ( config == null ) {
						config = new PropertyFilterConfig();
						propIncludes[i] = config;
					}
					if ( (config.getName() == null || config.getName().length() < 1) && inc != null
							&& inc.length() > 0 ) {
						config.setName(inc);
					}
				}
			}
		}
		this.excludePatterns = patterns(getExcludes(), Pattern.CASE_INSENSITIVE);
	}

	@Override
	public String getSettingUid() {
		return settingUid;
	}

	@Override
	public String getDisplayName() {
		return "Simple Filter Sample Transformer";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return settingSpecifiers(false);
	}

	@Override
	public List<SettingSpecifier> templateSettingSpecifiers() {
		return settingSpecifiers(true);
	}

	private List<SettingSpecifier> settingSpecifiers(final boolean template) {
		List<SettingSpecifier> results = baseIdentifiableSettings();
		populateBaseSampleTransformSupportSettings(results);
		populateStatusSettings(results);

		PropertyFilterConfig[] incs = getPropIncludes();
		List<PropertyFilterConfig> incsList = (template ? singletonList(new PropertyFilterConfig())
				: (incs != null ? asList(incs) : emptyList()));
		results.add(SettingUtils.dynamicListSettingSpecifier("propIncludes", incsList,
				new SettingUtils.KeyedListCallback<PropertyFilterConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(PropertyFilterConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								PropertyFilterConfig.settings(key + "."));
						return singletonList(configGroup);
					}
				}));

		String[] excs = getExcludes();
		List<String> listStrings = (template ? singletonList("")
				: (excs != null ? asList(excs) : emptyList()));
		results.add(SettingUtils.dynamicListSettingSpecifier("excludes", listStrings,
				new SettingUtils.KeyedListCallback<String>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(String value, int index,
							String key) {
						return singletonList(new BasicTextFieldSettingSpecifier(key, ""));
					}
				}));

		return results;
	}

	/**
	 * Get the property include configurations.
	 *
	 * @return The property include configurations.
	 */
	public PropertyFilterConfig[] getPropIncludes() {
		return this.propIncludes;
	}

	/**
	 * Set an array of property include configurations.
	 *
	 * @param propIncludes
	 *        The property include configurations.
	 */
	public void setPropIncludes(PropertyFilterConfig[] propIncludes) {
		this.propIncludes = propIncludes;
	}

	/**
	 * Get the number of configured {@code propIncludes} elements.
	 *
	 * @return The number of {@code propIncludes} elements.
	 */
	public int getPropIncludesCount() {
		PropertyFilterConfig[] incs = this.propIncludes;
		return (incs == null ? 0 : incs.length);
	}

	/**
	 * Adjust the number of configured {@code propIncludes} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link PropertyFilterConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code propIncludes} elements.
	 */
	public void setPropIncludesCount(int count) {
		this.propIncludes = ArrayUtils.arrayWithLength(this.propIncludes, count,
				PropertyFilterConfig.class, PropertyFilterConfig::new);
	}

	/**
	 * Get the array of property exclude expressions.
	 *
	 * @return The property exclude expressions.
	 */
	public String[] getExcludes() {
		return excludes;
	}

	/**
	 * Set an array of property exclude expressions.
	 *
	 * @param excludeExpressions
	 *        The property patterns to exclude.
	 */
	public void setExcludes(String[] excludeExpressions) {
		this.excludes = excludeExpressions;
	}

	/**
	 * Get the number of configured {@code excludes} elements.
	 *
	 * @return The number of {@code excludes} elements.
	 */
	public int getExcludesCount() {
		String[] pats = this.excludes;
		return (pats == null ? 0 : pats.length);
	}

	/**
	 * Adjust the number of configured {@code excludes} elements. Any newly
	 * added element values will be {@literal null}.
	 *
	 * @param count
	 *        The desired number of {@code excludes} elements.
	 */
	public void setExcludesCount(int count) {
		this.excludes = ArrayUtils.arrayWithLength(this.excludes, count, String.class, () -> null);
	}

	/**
	 * The setting UID to use.
	 *
	 * @param settingUid
	 *        the setting UID
	 */
	public void setSettingUid(String settingUid) {
		this.settingUid = settingUid;
	}

}
