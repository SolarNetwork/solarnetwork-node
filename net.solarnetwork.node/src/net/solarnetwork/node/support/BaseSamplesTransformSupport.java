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
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Base class for services like
 * {@link net.solarnetwork.node.GeneralDatumSamplesTransformService} to extend.
 * 
 * @author matt
 * @version 1.1
 * @since 1.83
 */
public class BaseSamplesTransformSupport extends BaseIdentifiable {

	/**
	 * The default value for the UID property.
	 */
	public static final String DEFAULT_UID = "Default";

	private Pattern sourceId;
	private OperationalModesService opModesService;
	private String requiredOperationalMode;

	/**
	 * Populate settings for the {@code BaseSamplesTransformSupport} class.
	 * 
	 * <p>
	 * This will use {@literal null} for all default values.
	 * </p>
	 * 
	 * @param settings
	 *        the settings to populate
	 * @since 1.1
	 * @see BaseSamplesTransformSupport#populateBaseSampleTransformSupportSettings(List,
	 *      String, String)
	 */
	public static void populateBaseSampleTransformSupportSettings(List<SettingSpecifier> settings) {
		populateBaseSampleTransformSupportSettings(settings, null, null);
	}

	/**
	 * Populate settings for the {@code BaseSamplesTransformSupport} class.
	 * 
	 * <p>
	 * This will add settings for the {@code sourceId} and
	 * {@code requiredOperationalMode} settings.
	 * </p>
	 * 
	 * @param settings
	 *        the list to add settings to
	 * @param sourceIdDefault
	 *        the default {@code sourceId} value
	 * @param requiredOperationalModeDefault
	 *        the default {@code requiredOperationalMode} value
	 * @since 1.1
	 */
	public static void populateBaseSampleTransformSupportSettings(List<SettingSpecifier> settings,
			String sourceIdDefault, String requiredOperationalModeDefault) {
		settings.add(new BasicTextFieldSettingSpecifier("sourceId", sourceIdDefault));
		settings.add(new BasicTextFieldSettingSpecifier("requiredOperationalMode",
				requiredOperationalModeDefault));

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
	 * Test if the configured required operational mode is active.
	 * 
	 * <p>
	 * If {@link #getRequiredOperationalMode()} is configured but
	 * {@code #getOpModesService()} is not, this method will always return
	 * {@literal false}.
	 * </p>
	 * 
	 * @return {@literal true} if an operational mode is required and that mode
	 *         is currently active
	 * @since 1.1
	 */
	protected boolean operationalModeMatches() {
		final String mode = getRequiredOperationalMode();
		if ( mode == null ) {
			// no mode required, so automatically matches
			return true;
		}
		final OperationalModesService service = getOpModesService();
		if ( service == null ) {
			// service not available, so automatically does not match
			return false;
		}
		return service.isOperationalModeActive(mode);
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

	/**
	 * Get the operational modes service to use.
	 * 
	 * @return the service, or {@literal null}
	 */
	public OperationalModesService getOpModesService() {
		return opModesService;
	}

	/**
	 * Set the operational modes service to use.
	 * 
	 * @param opModesService
	 *        the service to use
	 * @since 1.1
	 */
	public void setOpModesService(OperationalModesService opModesService) {
		this.opModesService = opModesService;
	}

	/**
	 * Get an operational mode that is required by this service.
	 * 
	 * @return the required operational mode, or {@literal null} for none
	 * @since 1.1
	 */
	public String getRequiredOperationalMode() {
		return requiredOperationalMode;
	}

	/**
	 * Set an operational mode that is required by this service.
	 * 
	 * @param requiredOperationalMode
	 *        the required operational mode, or {@literal null} or an empty
	 *        string that will be treated as {@literal null}
	 * @since 1.1
	 */
	public void setRequiredOperationalMode(String requiredOperationalMode) {
		if ( requiredOperationalMode != null && requiredOperationalMode.trim().isEmpty() ) {
			requiredOperationalMode = null;
		}
		this.requiredOperationalMode = requiredOperationalMode;
	}

}
