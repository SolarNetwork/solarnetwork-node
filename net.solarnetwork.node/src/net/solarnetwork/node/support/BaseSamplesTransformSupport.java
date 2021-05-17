/* ==================================================================
 * BaseSamplesTransformSupport.java - 12/05/2021 7:03:29 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.domain.Datum;

/**
 * Base class for services like
 * {@link net.solarnetwork.node.GeneralDatumSamplesTransformService} to extend.
 * 
 * @author matt
 * @version 1.0
 * @since 1.83
 */
public class BaseSamplesTransformSupport extends BaseIdentifiable {

	/**
	 * The default value for the UID property.
	 */
	public static final String DEFAULT_UID = "Default";

	private Pattern sourceId;

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
	 * Test if a given datum's source ID matches the configured source ID
	 * pattern.
	 * 
	 * @param datum
	 *        the datum whose source ID should be tested
	 * @return {@literal true} if the datum's {@code sourceId} value matches the
	 *         configured source ID pattern, or no pattern is configured
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
	 * Get a description of this service.
	 * 
	 * @return the description
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
	 * {@link net.solarnetwork.node.domain.Datum#getSourceId()} matches this
	 * pattern.
	 * 
	 * The {@code sourceIdPattern} must be a valid {@link Pattern} regular
	 * expression. The expression will be allowed to match anywhere in
	 * {@link net.solarnetwork.node.domain.Datum#getSourceId()} values, so if
	 * the pattern must match the full value only then use pattern positional
	 * expressions like {@code ^} and {@code $}.
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

}
