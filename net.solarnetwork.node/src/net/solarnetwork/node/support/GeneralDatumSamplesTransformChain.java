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
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
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
 * @version 1.3
 */
public class GeneralDatumSamplesTransformChain extends BaseIdentifiable
		implements GeneralDatumSamplesTransformService, SettingSpecifierProvider {

	private final String settingUid;
	private final List<GeneralDatumSamplesTransformService> transformServices;
	private final boolean configurableUid;
	private final GeneralDatumSamplesTransformService staticService;
	private String[] transformUids;
	private List<GeneralDatumSamplesTransformer> sampleTransformers;
	private OperationalModesService opModesService;
	private String requiredOperationalMode;

	private final ConcurrentMap<String, GeneralDatumSamplesTransformService> serviceCache = new WeakValueConcurrentHashMap<>(
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
			List<GeneralDatumSamplesTransformService> transformServices) {
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
			List<GeneralDatumSamplesTransformService> transformServices, boolean configurableUid) {
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
			List<GeneralDatumSamplesTransformService> transformServices, boolean configurableUid,
			GeneralDatumSamplesTransformService staticService) {
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
		for ( GeneralDatumSamplesTransformService s : transformServices ) {
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

	private GeneralDatumSamplesTransformService findService(String uid) {
		return serviceCache.compute(uid, (k, v) -> {
			// have to re-check UID, as these can change
			if ( v != null && uid.equals(v.getUid()) ) {
				return v;
			}
			for ( GeneralDatumSamplesTransformService s : transformServices ) {
				String serviceUid = s.getUid();
				if ( uid.equals(serviceUid) ) {
					return s;
				}
			}
			return null;
		});
	}

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, Object> parameters) {
		if ( !operationalModeMatches() ) {
			return samples;
		}
		Map<String, Object> p = parameters;
		if ( staticService != null ) {
			if ( p == null ) {
				p = new HashMap<>(8);
			}
			samples = staticService.transformSamples(datum, samples, parameters);
			if ( samples == null ) {
				return null;
			}
		}
		final String[] uids = getTransformUids();
		if ( uids == null || uids.length < 1 ) {
			return samples;
		}
		for ( String uid : uids ) {
			if ( uid == null || uid.isEmpty() ) {
				continue;
			}
			GeneralDatumSamplesTransformService s = findService(uid);
			if ( s != null ) {
				if ( p == null ) {
					p = new HashMap<>(8);
				}
				samples = s.transformSamples(datum, samples, p);
				if ( samples == null ) {
					return null;
				}
			}
		}
		return samples;
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
	 * This list defines the {@link GeneralDatumSamplesTransformService}
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
	 * Theese are not applied by this class. Rather, if this is set then a
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
