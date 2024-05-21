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
import static net.solarnetwork.settings.support.SettingUtils.dynamicListSettingSpecifier;
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
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Configuration for filtering options used by SolarFlux.
 *
 * @author matt
 * @version 2.0
 * @since 1.4
 */
public class FluxFilterConfig implements SettingsChangeObserver {

	private static final Logger log = LoggerFactory.getLogger(FluxFilterConfig.class);

	private static final class NullStringObjectFactory implements ObjectFactory<String> {

		@Override
		public String getObject() throws BeansException {
			return null;
		}
	}

	private Pattern sourceIdRegex;
	private String datumEncoderUid;
	private String transformServiceUid;
	private String requiredOperationalMode;
	private Integer frequencySeconds;
	private String[] propIncludeValues;
	private String[] propExcludeValues;

	private Pattern[] propIncludes;
	private Pattern[] propExcludes;

	/**
	 * Constructor.
	 */
	public FluxFilterConfig() {
		super();
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

	private static class SimpleKeyedListCallback implements SettingUtils.KeyedListCallback<String> {

		@Override
		public Collection<SettingSpecifier> mapListSettingKey(String value, int index, String key) {
			return singletonList(new BasicTextFieldSettingSpecifier(key, ""));
		}
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		this.propIncludes = patterns(propIncludeValues);
		this.propExcludes = patterns(propExcludeValues);
	}

	/**
	 * Test if a source ID matches the source ID pattern configured on this
	 * instance.
	 *
	 * @param sourceId
	 *        the source ID to test
	 * @return {@literal true} if {@code sourceId} matches the configured source
	 *         ID pattern, or no pattern is defined
	 * @since 1.3
	 */
	public boolean isSourceIdMatch(String sourceId) {
		if ( sourceIdRegex != null && sourceId != null ) {
			return sourceIdRegex.matcher(sourceId).find();
		}
		return true;
	}

	/**
	 * Apply the rules defined in this filter configuration to see if a datum
	 * can be published.
	 *
	 * @param previousSourceIdPublishDate
	 *        the last known publish date for the given {@code sourceId} value,
	 *        or {@literal null} if not known
	 * @param sourceId
	 *        the source ID to test, or {@literal null} if none
	 * @param datum
	 *        the datum data
	 * @return {@literal false} if the datum should <b>not</b> be published,
	 *         {@literal true} if the datum <b>may</b> be published, although
	 *         another filter might determine it should not be
	 */
	public boolean isPublishAllowed(final Long previousSourceIdPublishDate, String sourceId,
			Map<String, Object> datum) {
		if ( datum == null ) {
			return false;
		}
		if ( !isSourceIdMatch(sourceId) ) {
			return true;
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
		final Pattern[] excludes = this.propExcludes;
		if ( excludes != null && excludes.length == 1 && excludes[0] != null
				&& excludes[0].pattern().equals(".*") ) {
			log.trace("Filtering {} because of global property exclude filter", sourceId);
			return false;
		}

		final Pattern[] includes = this.propIncludes;
		if ( includes != null && includes.length > 0 ) {
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
		result.add(new BasicTextFieldSettingSpecifier(prefix + "transformServiceUid", ""));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "requiredOperationalMode", ""));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "datumEncoderUid", ""));
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
		if ( datumEncoderUid != null ) {
			builder.append("datumEncoderUid=");
			builder.append(datumEncoderUid);
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
	 * Get a list of property name regular expression values to limit data to.
	 *
	 * @return a list of expressions, or {@literal null}
	 * @see #getPropIncludeValues()
	 */
	public String[] getPropIncludeValues() {
		return propIncludeValues;
	}

	/**
	 * Set a list of property name regular expression values to limit datum to.
	 *
	 * @param propIncludes
	 *        a list of expressions, or {@literal null}
	 * @see #setPropIncludeValues(String[])
	 */
	public void setPropIncludeValues(String[] propIncludes) {
		this.propIncludeValues = propIncludes;
	}

	/**
	 * Get the number of configured {@code excludes} elements.
	 *
	 * @return The number of {@code excludes} elements.
	 */
	public int getPropIncludeValuesCount() {
		String[] pats = getPropIncludeValues();
		return (pats == null ? 0 : pats.length);
	}

	/**
	 * Adjust the number of configured {@code excludes} elements. Any newly
	 * added element values will be {@literal null}.
	 *
	 * @param count
	 *        The desired number of {@code excludes} elements.
	 */
	public void setPropIncludeValuesCount(int count) {
		this.propIncludeValues = ArrayUtils.arrayWithLength(this.propIncludeValues, count, String.class,
				new NullStringObjectFactory());
	}

	/**
	 * Get a list of property name regular expression values to exclude from
	 * datum.
	 *
	 * @return a list of patterns, or {@literal null}
	 * @see #getPropExcludeValues()
	 */
	public String[] getPropExcludeValues() {
		return propExcludeValues;
	}

	/**
	 * Set a list of property name regular expression values to exclude from
	 * datum.
	 *
	 * @param propExcludes
	 *        a lit of expressions, or {@literal null}
	 * @see #setPropExcludeValues(String[])
	 */
	public void setPropExcludeValues(String[] propExcludes) {
		this.propExcludeValues = propExcludes;
	}

	/**
	 * Get the number of configured {@code excludes} elements.
	 *
	 * @return The number of {@code excludes} elements.
	 */
	public int getPropExcludeValuesCount() {
		String[] pats = getPropExcludeValues();
		return (pats == null ? 0 : pats.length);
	}

	/**
	 * Adjust the number of configured {@code excludes} elements. Any newly
	 * added element values will be {@literal null}.
	 *
	 * @param count
	 *        The desired number of {@code excludes} elements.
	 */
	public void setPropExcludeValuesCount(int count) {
		this.propExcludeValues = ArrayUtils.arrayWithLength(this.propExcludeValues, count, String.class,
				new NullStringObjectFactory());
	}

	/**
	 * Get the UID of a {@code ObjectEncoder} service to encode the message
	 * with.
	 *
	 * @return the datumEncoderUid the UID of the encoder service to use
	 * @since 1.1
	 */
	public String getDatumEncoderUid() {
		return datumEncoderUid;
	}

	/**
	 * Set the UID of a {@code ObjectEncoder} service to encode the message
	 * with.
	 *
	 * @param datumEncoderUid
	 *        the datumEncoderUid to set
	 * @since 1.1
	 */
	public void setDatumEncoderUid(String datumEncoderUid) {
		this.datumEncoderUid = datumEncoderUid;
	}

	/**
	 * Get the UID of a {code GeneralDatumSamplesTransformService} to transform
	 * the datum with.
	 *
	 * @return the UID, or {@literal null}
	 * @since 1.2
	 */
	public String getTransformServiceUid() {
		return transformServiceUid;
	}

	/**
	 * Set the UID of a {code GeneralDatumSamplesTransformService} to transform
	 * the datum with.
	 *
	 * @param transformServiceUid
	 *        the UID to set
	 * @since 1.2
	 */
	public void setTransformServiceUid(String transformServiceUid) {
		this.transformServiceUid = transformServiceUid;
	}

	/**
	 * Get the required operational mode.
	 *
	 * @return the operational mode that must be active for this configuration
	 *         to be applicable
	 * @since 1.3
	 */
	public String getRequiredOperationalMode() {
		return requiredOperationalMode;
	}

	/**
	 * Set the required operational mode.
	 *
	 * @param requiredOperationalMode
	 *        the operational mode that must be active for this configuration to
	 *        be applicable, or {@literal null} for no requirement
	 * @since 1.3
	 */
	public void setRequiredOperationalMode(String requiredOperationalMode) {
		this.requiredOperationalMode = requiredOperationalMode;
	}

}
