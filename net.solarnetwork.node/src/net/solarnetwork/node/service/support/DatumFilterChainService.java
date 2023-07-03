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

package net.solarnetwork.node.service.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.WeakValueConcurrentHashMap;

/**
 * A configurable chain of transformer services.
 * 
 * <p>
 * If a {@code staticService} is configured then it will be applied
 * <em>first</em>. Then the {@code transformUids} will be iterated over and the
 * first matching service found for each value in {@code transformServices} will
 * be applied.
 * </p>
 * 
 * @author matt
 * @version 1.3
 * @since 2.0
 */
public class DatumFilterChainService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	private final String settingUid;
	private final List<DatumFilterService> transformServices;
	private final boolean configurableUid;
	private final DatumFilterService staticService;
	private String[] transformUids;
	private List<DatumFilterService> alternateDatumFilterServices;
	private boolean ignoreTransformUids;

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
	 *        the {@link SettingSpecifierProvider#getSettingUid()} value to use
	 * @param transformServices
	 *        the list of available services
	 * @throws IllegalArgumentException
	 *         if {@code settingUid} or {@code transformServices} are
	 *         {@literal null}
	 */
	public DatumFilterChainService(String settingUid, List<DatumFilterService> transformServices) {
		this(settingUid, transformServices, true, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param settingUid
	 *        the {@link SettingSpecifierProvider#getSettingUid()} value to use
	 * @param transformServices
	 *        the list of available services
	 * @param configurableUid
	 *        {@literal true} to support the UID and groupUid values as setting
	 *        specifiers
	 * @throws IllegalArgumentException
	 *         if {@code settingUid} or {@code transformServices} are
	 *         {@literal null}
	 */
	public DatumFilterChainService(String settingUid, List<DatumFilterService> transformServices,
			boolean configurableUid) {
		this(settingUid, transformServices, configurableUid, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param settingUid
	 *        the {@link SettingSpecifierProvider#getSettingUid()} value to use
	 * @param transformServices
	 *        the list of available services
	 * @param configurableUid
	 *        {@literal true} to support the UID and groupUid values as setting
	 *        specifiers
	 * @param staticService
	 *        an optional static service
	 * @throws IllegalArgumentException
	 *         if {@code settingUid} or {@code transformServices} are
	 *         {@literal null}
	 */
	public DatumFilterChainService(String settingUid, List<DatumFilterService> transformServices,
			boolean configurableUid, DatumFilterService staticService) {
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
		this.ignoreTransformUids = false;
	}

	@Override
	public String getSettingUid() {
		return settingUid;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);
		if ( alternateDatumFilterServices != null ) {
			result.add(new BasicTitleSettingSpecifier("availableAlternateDatumFilterUids",
					availableAlternateDatumFilterUidsStatus(), true, true));
		}

		result.add(new BasicTitleSettingSpecifier("availableUids", availableUidsStatus(), true, true));

		if ( configurableUid ) {
			result.addAll(baseIdentifiableSettings(""));
			result.add(new BasicTextFieldSettingSpecifier("requiredOperationalMode", null));
			result.add(new BasicTextFieldSettingSpecifier("requiredTag", null));
		}

		populateStatusSettings(result);

		// list of UIDs
		String[] uids = getTransformUids();
		List<String> uidsList = (uids != null ? Arrays.asList(uids) : Collections.emptyList());
		BasicGroupSettingSpecifier uidsGroup = SettingUtils.dynamicListSettingSpecifier("transformUids",
				uidsList, new SettingUtils.KeyedListCallback<String>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(String value, int index,
							String key) {
						return Collections.singletonList(new BasicTextFieldSettingSpecifier(key, ""));
					}
				});
		result.add(uidsGroup);

		return result;
	}

	private String availableAlternateDatumFilterUidsStatus() {
		List<String> names = new ArrayList<>();
		for ( DatumFilterService s : alternateDatumFilterServices ) {
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
			try {
				if ( v != null && uid.equals(v.getUid()) ) {
					return v;
				}
				for ( DatumFilterService s : transformServices ) {
					String serviceUid = s.getUid();
					if ( uid.equals(serviceUid) ) {
						return s;
					}
				}
			} catch ( Exception e ) {
				log.warn("Discarding cached service [{}] because of exception: {}", uid, e.toString());
			}
			return null;
		});
	}

	@Override
	public DatumSamplesOperations filter(final Datum datum, final DatumSamplesOperations samples,
			final Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		Map<String, Object> p = parameters;
		DatumSamplesOperations out = samples;
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
		if ( ignoreTransformUids ) {
			if ( transformServices != null ) {
				for ( DatumFilterService s : transformServices ) {
					out = s.filter(datum, out, parameters);
					if ( out == null ) {
						incrementStats(start, samples, out);
						return null;
					}
				}
			}
		} else {
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
	 * This list defines the {@link DatumFilterService} instances to apply, from
	 * the list of available services.
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
	 * @param alternateDatumFilterServices
	 *        the transformers to use
	 */
	public void setAlternateDatumFilterServices(List<DatumFilterService> alternateDatumFilterServices) {
		this.alternateDatumFilterServices = alternateDatumFilterServices;
	}

	/**
	 * Get the ignore {@code transformUids} flag.
	 * 
	 * @return {@literal true} to always apply all available filters in the
	 *         {@code transformServices} property; defaults to {@literal false}
	 * @since 1.2
	 */
	public boolean isIgnoreTransformUids() {
		return ignoreTransformUids;
	}

	/**
	 * Set the ignore {@code transformUids} flag.
	 * 
	 * @param ignoreTransformUids
	 *        {@literal true} to always apply all available filters in the
	 *        {@code transformServices} property
	 * @since 1.2
	 */
	public void setIgnoreTransformUids(boolean ignoreTransformUids) {
		this.ignoreTransformUids = ignoreTransformUids;
	}

}
