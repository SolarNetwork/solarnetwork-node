/* ==================================================================
 * SimpleFilterSamplesTransformer.java - 28/10/2016 3:00:56 PM
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
 * @version 1.1
 */
public class SimpleFilterSamplesTransformer extends SamplesTransformerSupport
		implements GeneralDatumSamplesTransformer, SettingSpecifierProvider {

	private String[] includes;
	private String[] excludes;

	private Pattern[] includePatterns;
	private Pattern[] excludePatterns;

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples) {
		Pattern sourceIdPat = getSourceIdPattern();
		if ( sourceIdPat != null ) {
			if ( datum == null || datum.getSourceId() == null
					|| !sourceIdPat.matcher(datum.getSourceId()).find() ) {
				log.trace("Datum {} does not match source ID pattern {}; not filtering", datum,
						sourceIdPat);
				return samples;
			}
		}

		GeneralDatumSamples copy = null;

		// handle property inclusion rules
		Pattern[] incs = this.includePatterns;
		if ( incs != null && incs.length > 0 ) {
			Map<String, ?> map = samples.getAccumulating();
			if ( map != null ) {
				for ( String propName : map.keySet() ) {
					if ( !matchesAny(incs, propName, true) ) {
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
					if ( !matchesAny(incs, propName, true) ) {
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
					if ( !matchesAny(incs, propName, true) ) {
						if ( copy == null ) {
							copy = copy(samples);
						}
						copy.getStatus().remove(propName);
					}
				}
			}
		}

		// handle property exclusion rules
		Pattern[] excs = this.excludePatterns;
		if ( excs != null && excs.length > 0 ) {
			Map<String, ?> map = samples.getAccumulating();
			if ( map != null ) {
				for ( String propName : map.keySet() ) {
					if ( matchesAny(excs, propName, false) ) {
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
					if ( matchesAny(excs, propName, false) ) {
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
					if ( matchesAny(excs, propName, false) ) {
						if ( copy == null ) {
							copy = copy(samples);
						}
						copy.getStatus().remove(propName);
					}
				}
			}
		}

		// tidy up any empty maps we created during filtering
		if ( copy != null )

		{
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
