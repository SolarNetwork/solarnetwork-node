/* ==================================================================
 * GeneralDatumSamplesTransformChain.java - 9/05/2021 3:39:09 PM
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.WeakValueConcurrentHashMap;

/**
 * A configurable chain of transformer services.
 * 
 * <p>
 * If a {@code staticService} is configured then it will be applied
 * <em>first</em>. Then the {@code transformuids} will be iterated over and the
 * first matching service found for each value in {@code transformServices} will
 * be applied.
 * </p>
 * 
 * @author matt
 * @version 1.4
 */
public class GeneralDatumSamplesTransformChain extends BaseSamplesTransformSupport
		implements DatumFilterService, SettingSpecifierProvider {

	private final String settingUid;
	private final List<DatumFilterService> transformServices;
	private final boolean configurableUid;
	private final DatumFilterService staticService;
	private String[] transformUids;
	private List<GeneralDatumSamplesTransformer> sampleTransformers;

	private final ConcurrentMap<String, DatumFilterService> serviceCache = new WeakValueConcurrentHashMap<>(
			16, 0.9f, 2);

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code configurableUid} property will be set to {@literal true}.
	 * </p>
	 * 
	 * @param settingUid
	 *        the {@link SettingSpecifierProvider#getSettingUID()} value to use
	 * @param transformServices
	 *        the list of available services
	 * @throws IllegalArgumentException
	 *         if {@code settingUid} or {@code transformServices} are
	 *         {@literal null}
	 */
	public GeneralDatumSamplesTransformChain(String settingUid,
			List<DatumFilterService> transformServices) {
		this(settingUid, transformServices, true, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param settingUid
	 *        the {@link SettingSpecifierProvider#getSettingUID()} value to use
	 * @param transformServices
	 *        the list of available services
	 * @param configurableUid
	 *        {@literal true} to support the UID and groupUID values as setting
	 *        specifiers
	 * @throws IllegalArgumentException
	 *         if {@code settingUid} or {@code transformServices} are
	 *         {@literal null}
	 */
	public GeneralDatumSamplesTransformChain(String settingUid,
			List<DatumFilterService> transformServices, boolean configurableUid) {
		this(settingUid, transformServices, configurableUid, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param settingUid
	 *        the {@link SettingSpecifierProvider#getSettingUID()} value to use
	 * @param transformServices
	 *        the list of available services
	 * @param configurableUid
	 *        {@literal true} to support the UID and groupUID values as setting
	 *        specifiers
	 * @param staticService
	 *        an optional static service
	 * @throws IllegalArgumentException
	 *         if {@code settingUid} or {@code transformServices} are
	 *         {@literal null}
	 */
	public GeneralDatumSamplesTransformChain(String settingUid,
			List<DatumFilterService> transformServices, boolean configurableUid,
			DatumFilterService staticService) {
		super();
		if ( settingUid == null || settingUid.isEmpty() ) {
			throw new IllegalArgumentException("The settingUid argument must not be null.");
		}
		this.settingUid = settingUid;
		if ( transformServices == null ) {
			throw new IllegalArgumentException("The transformServices argument must not be null.");
		}
		this.transformServices = transformServices;
		this.configurableUid = configurableUid;
		this.staticService = staticService;
	}

	@Override
	public String getSettingUID() {
		return settingUid;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);
		if ( sampleTransformers != null ) {
			result.add(new BasicTitleSettingSpecifier("availableTransformerUids",
					availableTransformerUidsStatus(), true, true));
		}

		result.add(new BasicTitleSettingSpecifier("availableUids", availableUidsStatus(), true, true));

		if ( configurableUid ) {
			result.addAll(baseIdentifiableSettings(""));
			result.add(new BasicTextFieldSettingSpecifier("requiredOperationalMode", null));
		}

		populateStatusSettings(result);

		// list of UIDs
		String[] uids = getTransformUids();
		List<String> uidsList = (uids != null ? Arrays.asList(uids) : Collections.emptyList());
		BasicGroupSettingSpecifier uidsGroup = SettingsUtil.dynamicListSettingSpecifier("transformUids",
				uidsList, new SettingsUtil.KeyedListCallback<String>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(String value, int index,
							String key) {
						return Collections.singletonList(new BasicTextFieldSettingSpecifier(key, ""));
					}
				});
		result.add(uidsGroup);

		return result;
	}

	private String availableTransformerUidsStatus() {
		List<String> names = new ArrayList<>();
		for ( GeneralDatumSamplesTransformer s : sampleTransformers ) {
			names.add(s.getDescription());
		}
		if ( names.isEmpty() ) {
			return "N/A";
		}
		Collections.sort(names, String::compareToIgnoreCase);
		StringBuilder buf = new StringBuilder("<ol>");
		for ( String uid : names ) {
			buf.append("<li>").append(uid).append("</li>");
		}
		buf.append("</ol>");
		return buf.toString();
	}

	private String availableUidsStatus() {
		List<String> uids = new ArrayList<>();
		for ( DatumFilterService s : transformServices ) {
			String uid = s.getUid();
			if ( uid != null && !uid.isEmpty() && !uid.equalsIgnoreCase(getUid()) ) {
				uids.add(uid);
			}
		}
		if ( uids.isEmpty() ) {
			return "N/A";
		}
		Collections.sort(uids, String::compareToIgnoreCase);
		StringBuilder buf = new StringBuilder("<ol>");
		for ( String uid : uids ) {
			buf.append("<li>").append(uid).append("</li>");
		}
		buf.append("</ol>");
		return buf.toString();
	}

	private DatumFilterService findService(String uid) {
		return serviceCache.compute(uid, (k, v) -> {
			// have to re-check UID, as these can change
			if ( v != null && uid.equals(v.getUid()) ) {
				return v;
			}
			for ( DatumFilterService s : transformServices ) {
				String serviceUid = s.getUid();
				if ( uid.equals(serviceUid) ) {
					return s;
				}
			}
			return null;
		});
	}

	@Override
	public GeneralDatumSamples transformSamples(final NodeDatum datum, final GeneralDatumSamples samples,
			final Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( !operationalModeMatches() ) {
			incrementIgnoredStats(start);
			return samples;
		}
		Map<String, Object> p = parameters;
		GeneralDatumSamples out = samples;
		if ( staticService != null ) {
			if ( p == null ) {
				p = new HashMap<>(8);
			}
			out = staticService.filter(datum, out, parameters);
			if ( out == null ) {
				incrementStats(start, samples, out);
				return null;
			}
		}
		final String[] uids = getTransformUids();
		if ( uids == null || uids.length < 1 ) {
			incrementStats(start, samples, out);
			return out;
		}
		for ( String uid : uids ) {
			if ( uid == null || uid.isEmpty() ) {
				continue;
			}
			DatumFilterService s = findService(uid);
			if ( s != null ) {
				if ( p == null ) {
					p = new HashMap<>(8);
				}
				out = s.filter(datum, out, p);
				if ( out == null ) {
					incrementStats(start, samples, out);
					return null;
				}
			}
		}
		incrementStats(start, samples, out);
		return out;
	}

	/**
	 * Get the transform UIDs to use.
	 * 
	 * @return the transform UIDs.
	 */
	public String[] getTransformUids() {
		return transformUids;
	}

	/**
	 * Set the transform UIDs to use.
	 * 
	 * <p>
	 * This list defines the {@link DatumFilterService}
	 * instances to apply, from the list of available services.
	 * </p>
	 * 
	 * @param transformUids
	 *        the UIDs to set
	 */
	public void setTransformUids(String[] transformUids) {
		this.transformUids = transformUids;
	}

	/**
	 * Get the transform UIDs count.
	 * 
	 * @return the number of UIDs to support
	 */
	public int getTransformUidsCount() {
		String[] uids = getTransformUids();
		return (uids != null ? uids.length : 0);
	}

	/**
	 * Set the transform UIDs count.
	 * 
	 * @param count
	 *        the number of UIDs to support
	 */
	public void setTransformUidsCount(int count) {
		this.transformUids = ArrayUtils.arrayWithLength(this.transformUids, count, String.class, null);
	}

	/**
	 * Set the sample transformers to use.
	 * 
	 * <p>
	 * These are not applied by this class. Rather, if this is set then a
	 * read-only setting will be included in {@link #getSettingSpecifiers()}
	 * that lists the configured filters.
	 * </p>
	 * 
	 * @param sampleTransformers
	 *        the transformers to use
	 */
	public void setSampleTransformers(List<GeneralDatumSamplesTransformer> sampleTransformers) {
		this.sampleTransformers = sampleTransformers;
	}

}
