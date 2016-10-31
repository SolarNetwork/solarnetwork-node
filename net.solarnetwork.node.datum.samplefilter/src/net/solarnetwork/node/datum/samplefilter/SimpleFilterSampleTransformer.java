/* ==================================================================
 * SimpleFilterSampleTransformer.java - 28/10/2016 3:00:56 PM
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

package net.solarnetwork.node.datum.samplefilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;

/**
 * {@link GeneralDatumSamplesTransformer} that can filter out sample properties
 * based on simple matching rules.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleFilterSampleTransformer
		implements GeneralDatumSamplesTransformer, SettingSpecifierProvider {

	private Pattern sourceId;
	private String[] includes;
	private String[] excludes;
	private MessageSource messageSource;

	private Pattern[] includePatterns;
	private Pattern[] excludePatterns;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples) {
		Pattern sourceIdPat = sourceId;
		if ( sourceIdPat != null ) {
			if ( datum == null || datum.getSourceId() == null
					|| !sourceIdPat.matcher(datum.getSourceId()).find() ) {
				log.trace("Datum {} does not match source ID pattern {}; not filtering", datum,
						sourceId);
				return samples;
			}
		}

		GeneralDatumSamples copy = null;

		// handle property inclusion rules
		Pattern[] incs = this.includePatterns;
		if ( incs != null && incs.length > 0 ) {
			for ( Pattern pat : incs ) {
				if ( pat == null ) {
					continue;
				}
				Map<String, ?> map = samples.getAccumulating();
				if ( map != null ) {
					for ( String propName : map.keySet() ) {
						if ( propName != null && !pat.matcher(propName).find() ) {
							if ( copy == null ) {
								copy = copy(samples);
							}
							copy.getAccumulating().remove(propName);
						}
					}
				}
				map = samples.getInstantaneous();
				if ( map != null ) {
					for ( String propName : map.keySet() ) {
						if ( propName != null && !pat.matcher(propName).find() ) {
							if ( copy == null ) {
								copy = copy(samples);
							}
							copy.getInstantaneous().remove(propName);
						}
					}
				}
				map = samples.getStatus();
				if ( map != null ) {
					for ( String propName : map.keySet() ) {
						if ( propName != null && !pat.matcher(propName).find() ) {
							if ( copy == null ) {
								copy = copy(samples);
							}
							copy.getStatus().remove(propName);
						}
					}
				}
			}
		}

		// handle property exclusion rules
		Pattern[] excs = this.excludePatterns;
		if ( excs != null && excs.length > 0 ) {
			for ( Pattern pat : excs ) {
				if ( pat == null ) {
					continue;
				}
				Map<String, ?> map = samples.getAccumulating();
				if ( map != null ) {
					for ( String propName : map.keySet() ) {
						if ( propName != null && pat.matcher(propName).find() ) {
							if ( copy == null ) {
								copy = copy(samples);
							}
							copy.getAccumulating().remove(propName);
						}
					}
				}
				map = samples.getInstantaneous();
				if ( map != null ) {
					for ( String propName : map.keySet() ) {
						if ( propName != null && pat.matcher(propName).find() ) {
							if ( copy == null ) {
								copy = copy(samples);
							}
							copy.getInstantaneous().remove(propName);
						}
					}
				}
				map = samples.getStatus();
				if ( map != null ) {
					for ( String propName : map.keySet() ) {
						if ( propName != null && pat.matcher(propName).find() ) {
							if ( copy == null ) {
								copy = copy(samples);
							}
							copy.getStatus().remove(propName);
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
		}

		return (copy != null ? copy : samples);
	}

	private static GeneralDatumSamples copy(GeneralDatumSamples samples) {
		GeneralDatumSamples copy = new GeneralDatumSamples(
				samples.getInstantaneous() != null
						? new LinkedHashMap<String, Number>(samples.getInstantaneous()) : null,
				samples.getAccumulating() != null
						? new LinkedHashMap<String, Number>(samples.getAccumulating()) : null,
				samples.getStatus() != null ? new LinkedHashMap<String, Object>(samples.getStatus())
						: null);
		copy.setTags(samples.getTags() != null ? new LinkedHashSet<String>(samples.getTags()) : null);
		return copy;
	}

	/**
	 * Call to initialize the instance after properties are configured.
	 */
	public void init() {
		configurationChanged(null);
	}

	/**
	 * Call after any of the include/exclude values are modified.
	 */
	public void configurationChanged(Map<String, ?> props) {
		this.includePatterns = patterns(getIncludes());
		this.excludePatterns = patterns(getExcludes());
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.samplefilter.simple";
	}

	@Override
	public String getDisplayName() {
		return "Simple Filter Sample Transformer";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);

		results.add(new BasicTextFieldSettingSpecifier("sourceId", ""));

		String[] incs = getIncludes();
		Collection<String> listStrings = (incs != null ? Arrays.asList(incs)
				: Collections.<String> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("includes", listStrings,
				new SettingsUtil.KeyedListCallback<String>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(String value, int index,
							String key) {
						return Collections.<SettingSpecifier> singletonList(
								new BasicTextFieldSettingSpecifier(key, ""));
					}
				}));

		String[] excs = getExcludes();
		listStrings = (excs != null ? Arrays.asList(excs) : Collections.<String> emptyList());
		results.add(SettingsUtil.dynamicListSettingSpecifier("excludes", listStrings,
				new SettingsUtil.KeyedListCallback<String>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(String value, int index,
							String key) {
						return Collections.<SettingSpecifier> singletonList(
								new BasicTextFieldSettingSpecifier(key, ""));
					}
				}));

		return results;
	}

	private Pattern[] patterns(String[] expressions) {
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
					? Pattern.compile(sourceIdPattern, Pattern.CASE_INSENSITIVE) : null);
		} catch ( PatternSyntaxException e ) {
			log.warn("Error compiling regex [{}]", sourceIdPattern, e);
			this.sourceId = null;
		}
	}

	/**
	 * Get the property include expressions.
	 * 
	 * @return The property include expressions.
	 */
	public String[] getIncludes() {
		return this.includes;
	}

	/**
	 * Set an array of property include expressions.
	 * 
	 * @param includeExpressions
	 *        The property include expressions.
	 */
	public void setIncludes(String[] includeExpressions) {
		this.includes = includeExpressions;
	}

	/**
	 * Get the number of configured {@code includes} elements.
	 * 
	 * @return The number of {@code includes} elements.
	 */
	public int getIncludesCount() {
		String[] pats = this.includes;
		return (pats == null ? 0 : pats.length);
	}

	/**
	 * Adjust the number of configured {@code includes} elements. Any newly
	 * added element values will be {@code null}.
	 * 
	 * @param count
	 *        The desired number of {@code includes} elements.
	 */
	public void setIncludesCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		String[] pats = this.includes;
		int lCount = (pats == null ? 0 : pats.length);
		if ( lCount != count ) {
			String[] newPats = new String[count];
			if ( pats != null ) {
				System.arraycopy(pats, 0, newPats, 0, Math.min(count, pats.length));
			}
			this.includes = newPats;
		}
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a {@link MessageSource} to use for settings.
	 * 
	 * @param messageSource
	 *        The message source.
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
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
	 * added element values will be {@code null}.
	 * 
	 * @param count
	 *        The desired number of {@code excludes} elements.
	 */
	public void setExcludesCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		String[] pats = this.excludes;
		int lCount = (pats == null ? 0 : pats.length);
		if ( lCount != count ) {
			String[] newPats = new String[count];
			if ( pats != null ) {
				System.arraycopy(pats, 0, newPats, 0, Math.min(count, pats.length));
			}
			this.excludes = newPats;
		}
	}

}
