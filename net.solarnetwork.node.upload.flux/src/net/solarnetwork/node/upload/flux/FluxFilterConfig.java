/* ==================================================================
 * FluxFilterConfig.java - 13/12/2019 1:57:45 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.flux;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.node.settings.support.SettingsUtil.dynamicListSettingSpecifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.util.ArrayUtils;

/**
 * Configuration for filtering options used by SolarFlux.
 * 
 * @author matt
 * @version 1.0
 * @since 1.4
 */
public class FluxFilterConfig {

	private static final Logger log = LoggerFactory.getLogger(FluxFilterConfig.class);

	private static final class NullPatternObjectFactor implements ObjectFactory<Pattern> {

		@Override
		public Pattern getObject() throws BeansException {
			return null;
		}
	}

	private Pattern sourceIdRegex;
	private Integer frequencySeconds;
	private Pattern[] propIncludes;
	private Pattern[] propExcludes;

	private static String[] patternValues(Pattern[] pats) {
		if ( pats == null ) {
			return null;
		}
		String[] res = new String[pats.length];
		for ( int i = 0, len = res.length; i < len; i++ ) {
			String r = null;
			Pattern p = pats[i];
			if ( p != null ) {
				r = p.pattern();
			}
			res[i] = r;
		}
		return res;

	}

	private static Pattern[] patterns(String[] expr) {
		if ( expr == null ) {
			return null;
		}
		Pattern[] res = new Pattern[expr.length];
		for ( int i = 0, len = res.length; i < len; i++ ) {
			String r = expr[i];
			Pattern p = null;
			if ( r != null ) {
				try {
					p = Pattern.compile(r, Pattern.CASE_INSENSITIVE);
				} catch ( PatternSyntaxException e ) {
					// ignore
				}
			}
			res[i] = p;
		}
		return res;
	}

	private static class SimpleKeyedListCallback implements SettingsUtil.KeyedListCallback<String> {

		@Override
		public Collection<SettingSpecifier> mapListSettingKey(String value, int index, String key) {
			return singletonList(new BasicTextFieldSettingSpecifier(key, ""));
		}
	}

	/**
	 * Apply the rules defined in this filter configuration to see if a datum
	 * can be published.
	 * 
	 * @param previousSourceIdPublishDate
	 *        the last known publish date for the given {@code sourceId} value,
	 *        or {@literal null} if not known
	 * @return {@literal false} if the datum should <b>not</b> be published,
	 *         {@literal true} if the datum <b>may</b> be published, although
	 *         another filter might determine it should not be
	 */
	public boolean isPublishAllowed(final Long previousSourceIdPublishDate, String sourceId,
			Map<String, Object> datum) {
		if ( datum == null ) {
			return false;
		}
		if ( sourceIdRegex != null && sourceId != null ) {
			boolean match = sourceIdRegex.matcher(sourceId).find();
			if ( !match ) {
				// this filter does not apply to the given source ID, so skip
				return true;
			}
		}

		if ( frequencySeconds != null && frequencySeconds > 0 && previousSourceIdPublishDate != null ) {
			long remainingMs = previousSourceIdPublishDate + (frequencySeconds * 1000)
					- currentTimeMillis();
			if ( remainingMs > 0 ) {
				log.trace("Filtering {} because throttled @ {}s ({}ms to go)", sourceId,
						frequencySeconds, remainingMs);
				return false;
			}
		}

		// check for special case of "exclude all" pattern, to short-circuit property checking
		Pattern[] excludes = getPropExcludes();
		if ( excludes != null && excludes.length == 1 && excludes[0] != null
				&& excludes[0].pattern().equals(".*") ) {
			log.trace("Filtering {} because of global property exclude filter", sourceId);
			return false;
		}

		Pattern[] includes = getPropIncludes();
		if ( includes != null ) {
			for ( Iterator<String> itr = datum.keySet().iterator(); itr.hasNext(); ) {
				String propName = itr.next();
				if ( propName == null ) {
					continue;
				}
				boolean found = false;
				for ( Pattern p : includes ) {
					if ( p == null ) {
						continue;
					}
					if ( p.matcher(propName).find() ) {
						found = true;
						break;
					}
				}
				if ( !found ) {
					if ( log.isTraceEnabled() ) {
						log.trace("Filtering {} property {} from prop inclusion filters {}", sourceId,
								propName, Arrays.toString(includes));
					}
					itr.remove();
				}
			}
		}

		if ( excludes != null ) {
			for ( Pattern p : excludes ) {
				if ( p == null ) {
					continue;
				}
				for ( Iterator<String> itr = datum.keySet().iterator(); itr.hasNext(); ) {
					String propName = itr.next();
					if ( propName != null && p.matcher(propName).find() ) {
						log.trace("Filtering {} property {} from prop exclusion filter {}", sourceId,
								propName, p);
						itr.remove();
					}
				}
			}
		}
		return !datum.isEmpty();

	}

	/**
	 * Get setting specifiers for this configuration.
	 * 
	 * @param prefix
	 *        a prefix to add to each setting property key
	 * @return the settings
	 */
	public List<SettingSpecifier> getSettingSpecifiers(String prefix) {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(8);

		result.add(new BasicTextFieldSettingSpecifier(prefix + "sourceIdRegexValue", ""));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "frequencySeconds", ""));

		SimpleKeyedListCallback cb = new SimpleKeyedListCallback();

		// prop includes
		String[] exprs = getPropIncludeValues();
		List<String> exprList = (exprs != null ? asList(exprs) : emptyList());
		result.add(dynamicListSettingSpecifier(prefix + "propIncludeValues", exprList, cb));

		// prop excludes
		exprs = getPropExcludeValues();
		exprList = (exprs != null ? asList(exprs) : emptyList());
		result.add(dynamicListSettingSpecifier(prefix + "propExcludeValues", exprList, cb));

		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FluxFilterConfig{");
		if ( sourceIdRegex != null ) {
			builder.append("sourceIdRegex=");
			builder.append(sourceIdRegex);
			builder.append(", ");
		}
		if ( frequencySeconds != null ) {
			builder.append("frequencySeconds=");
			builder.append(frequencySeconds);
			builder.append(", ");
		}
		if ( propIncludes != null ) {
			builder.append("propIncludes=");
			builder.append(Arrays.toString(propIncludes));
			builder.append(", ");
		}
		if ( propExcludes != null ) {
			builder.append("propExcludes=");
			builder.append(Arrays.toString(propExcludes));
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the source ID regular expression.
	 * 
	 * @return the source ID expression, or {@literal null} for including all
	 *         source IDs
	 */
	public Pattern getSourceIdRegex() {
		return sourceIdRegex;
	}

	/**
	 * Set the source ID regular expression.
	 * 
	 * @param sourceIdRegex
	 *        a pattern to match against source IDs; if defined then this filter
	 *        will only apply to datum with matching source ID values; if
	 *        {@literal null} then this filter applies to all datum
	 */
	public void setSourceIdRegex(Pattern sourceIdRegex) {
		this.sourceIdRegex = sourceIdRegex;
	}

	/**
	 * Get the source ID regular expression as a string.
	 * 
	 * @return the source ID expression string, or {@literal null} for including
	 *         all source IDs
	 */
	public String getSourceIdRegexValue() {
		Pattern p = getSourceIdRegex();
		return (p != null ? p.pattern() : null);
	}

	/**
	 * Set the source ID regular expression as a string.
	 * 
	 * <p>
	 * Errors compiling {@code sourceIdRegex} into a {@link Pattern} will be
	 * silently ignored, causing the regular expression to be set to
	 * {@literal null}.
	 * </p>
	 * 
	 * @param sourceIdRegex
	 *        a pattern to match against source IDs; if defined then this filter
	 *        will only apply to datum with matching source ID values; if
	 *        {@literal null} then this filter applies to all datum
	 */
	public void setSourceIdRegexValue(String sourceIdRegex) {
		Pattern p = null;
		if ( sourceIdRegex != null ) {
			try {
				p = Pattern.compile(sourceIdRegex, Pattern.CASE_INSENSITIVE);
			} catch ( PatternSyntaxException e ) {
				// ignore
			}
		}
		setSourceIdRegex(p);
	}

	/**
	 * Get the minimum number of seconds to limit datum to.
	 * 
	 * @return a frequency in seconds, or {@literal null}
	 */
	public Integer getFrequencySeconds() {
		return frequencySeconds;
	}

	/**
	 * Get the minimum number of seconds to limit datum to.
	 * 
	 * @param frequencySeconds
	 *        a frequency in seconds, or {@literal null} or anything less than
	 *        {@literal 1} for no limit
	 */
	public void setFrequencySeconds(Integer frequencySeconds) {
		this.frequencySeconds = frequencySeconds;
	}

	/**
	 * Get a list of property name regular expressions to limit datum to.
	 * 
	 * @return a list of patterns, or {@literal null}
	 */
	public Pattern[] getPropIncludes() {
		return propIncludes;
	}

	/**
	 * Get a list of property name regular expression values to limit data to.
	 * 
	 * @return a list of expressions, or {@literal null}
	 * @see #getPropIncludes()
	 */
	public String[] getPropIncludeValues() {
		return patternValues(this.propIncludes);
	}

	/**
	 * Set a list of property name regular expressions to limit datum to.
	 * 
	 * <p>
	 * If any property include patterns are defined, then <b>only</b> properties
	 * matching one of these patterns will be included in datum posted to
	 * SolarFlux.
	 * </p>
	 * 
	 * @param propIncludes
	 *        a list of patterns, or {@literal null}
	 */
	public void setPropIncludes(Pattern[] propIncludes) {
		this.propIncludes = propIncludes;
	}

	/**
	 * Set a list of property name regular expression values to limit datum to.
	 * 
	 * @param propIncludes
	 *        a list of expressions, or {@literal null}
	 * @see #setPropIncludes(Pattern[])
	 */
	public void setPropIncludeValues(String[] propIncludes) {
		setPropIncludes(patterns(propIncludes));
	}

	/**
	 * Get the number of configured {@code excludes} elements.
	 * 
	 * @return The number of {@code excludes} elements.
	 */
	public int getPropIncludeValuesCount() {
		Pattern[] pats = getPropIncludes();
		return (pats == null ? 0 : pats.length);
	}

	/**
	 * Adjust the number of configured {@code excludes} elements. Any newly
	 * added element values will be {@code null}.
	 * 
	 * @param count
	 *        The desired number of {@code excludes} elements.
	 */
	public void setPropIncludeValuesCount(int count) {
		this.propIncludes = ArrayUtils.arrayWithLength(this.propIncludes, count, Pattern.class,
				new NullPatternObjectFactor());
	}

	/**
	 * Get a list of property name regular expressions to exclude from datum.
	 * 
	 * @return a list of patterns, or {@literal null}
	 */
	public Pattern[] getPropExcludes() {
		return propExcludes;
	}

	/**
	 * Get a list of property name regular expression values to exclude from
	 * datum.
	 * 
	 * @return a list of patterns, or {@literal null}
	 * @see #getPropExcludes()
	 */
	public String[] getPropExcludeValues() {
		return patternValues(propExcludes);
	}

	/**
	 * Set a list of property name regular expressions to exclude from datum.
	 * 
	 * <p>
	 * Any properties matching one of these patterns will be excluded from datum
	 * posted to SolarFlux. These are applied <b>after</b> evaluating any
	 * {@link #getPropIncludes()} patterns.
	 * </p>
	 * 
	 * @param propExcludes
	 *        a lit of patterns, or {@literal null}
	 */
	public void setPropExcludes(Pattern[] propExcludes) {
		this.propExcludes = propExcludes;
	}

	/**
	 * Set a list of property name regular expression values to exclude from
	 * datum.
	 * 
	 * @param propExcludes
	 *        a lit of expressions, or {@literal null}
	 * @see #setPropExcludes(Pattern[])
	 */
	public void setPropExcludeValues(String[] propExcludes) {
		setPropExcludes(patterns(propExcludes));
	}

	/**
	 * Get the number of configured {@code excludes} elements.
	 * 
	 * @return The number of {@code excludes} elements.
	 */
	public int getPropExcludeValuesCount() {
		Pattern[] pats = getPropExcludes();
		return (pats == null ? 0 : pats.length);
	}

	/**
	 * Adjust the number of configured {@code excludes} elements. Any newly
	 * added element values will be {@code null}.
	 * 
	 * @param count
	 *        The desired number of {@code excludes} elements.
	 */
	public void setPropExcludeValuesCount(int count) {
		this.propExcludes = ArrayUtils.arrayWithLength(this.propExcludes, count, Pattern.class,
				new NullPatternObjectFactor());
	}
}
