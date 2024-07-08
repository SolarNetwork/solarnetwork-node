/* ==================================================================
 * ControlsDatumDataSource.java - Dec 18, 2014 7:05:05 AM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.controls;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Datum data source for {@link NodeControlProvider} instances. This exposes the
 * available {@link NodeControlProvider} services as a datum data source, so
 * they can be queried and logged like any other datum data source.
 *
 * @author matt
 * @version 2.0
 */
public class ControlsDatumDataSource extends DatumDataSourceSupport
		implements MultiDatumDataSource, SettingSpecifierProvider {

	/** The {@code unchangedIgnoreMs} property default value. */
	private static final long DEFAULT_UNCHANGED_IGNORE_MS = 1000L * 60L * 10L;

	private final Map<String, NodeDatum> cache = new LinkedHashMap<>();

	private long unchangedIgnoreMs = DEFAULT_UNCHANGED_IGNORE_MS;
	private List<NodeControlProvider> providers;

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return SimpleNodeControlInfoDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		final long now = System.currentTimeMillis();
		List<NodeDatum> results = new ArrayList<>();
		for ( NodeControlProvider provider : providers ) {
			List<String> controlIds = provider.getAvailableControlIds();
			log.debug("Requesting control infos from provider {}: {}", provider, controlIds);
			Map<String, List<NodeControlInfo>> infos = new LinkedHashMap<>(controlIds.size());
			for ( String controlId : controlIds ) {
				NodeControlInfo info;
				try {
					info = provider.getCurrentControlInfo(controlId);
				} catch ( Exception e ) {
					log.error("Error reading control {}: {}", controlId, e.getMessage());
					continue;
				}
				if ( info == null ) {
					log.debug("No info returned for control {}", controlId);
					continue;
				}
				log.trace("Read control {}: {}", controlId, info);
				List<NodeControlInfo> list = infos.get(controlId);
				if ( list == null ) {
					list = new ArrayList<>(controlIds.size());
					infos.put(controlId, list);
				}
				list.add(info);
			}
			if ( !infos.isEmpty() ) {
				for ( List<NodeControlInfo> list : infos.values() ) {
					SimpleNodeControlInfoDatum datum = new SimpleNodeControlInfoDatum(list.get(0),
							Instant.ofEpochMilli(now), list);
					NodeDatum cached = cache.get(datum.getSourceId());
					if ( unchangedIgnoreMs < 1 || cached == null
							|| (cached.getTimestamp().toEpochMilli() + unchangedIgnoreMs) < now
							|| datum.asSampleOperations().differsFrom(cached.asSampleOperations()) ) {
						results.add(datum);
						cache.put(datum.getSourceId(), datum);
					} else {
						log.debug("Control {} has not changed from cached value: {}",
								datum.getSourceId(), cached);
					}
				}
			}
		}
		log.debug("Collected changed control datum: {}", results);
		return results;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.controls";
	}

	@Override
	public String getDisplayName() {
		return "Controls Data Source";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(4);
		result.add(new BasicTextFieldSettingSpecifier("unchangedIgnoreMs",
				String.valueOf(DEFAULT_UNCHANGED_IGNORE_MS)));
		return result;
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final List<NodeControlProvider> providers = this.providers;
		if ( providers == null || providers.isEmpty() ) {
			return Collections.emptySet();
		}
		Set<String> result = new TreeSet<>();
		for ( NodeControlProvider provider : providers ) {
			List<String> controlIds = provider.getAvailableControlIds();
			if ( controlIds != null ) {
				result.addAll(controlIds);
			}
		}
		return result;
	}

	/**
	 * Set the list of providers to collect from.
	 *
	 * @param providers
	 *        the providers
	 */
	public void setProviders(List<NodeControlProvider> providers) {
		this.providers = providers;
	}

	/**
	 * Get the unchanged ignore time, in milliseconds.
	 *
	 * <p>
	 * When sampling a control, if it has not changed from the previously
	 * sampled value within this amount of time then ignore the value and do not
	 * return it from {@link #readMultipleDatum()}. Set to <code>0</code> to
	 * always return a datum for each control provider.
	 * </p>
	 *
	 * @param unchangedIgnoreMs
	 *        the ignore time in milliseconds; defaults to
	 *        {@link #DEFAULT_UNCHANGED_IGNORE_MS}
	 */
	public void setUnchangedIgnoreMs(long unchangedIgnoreMs) {
		this.unchangedIgnoreMs = unchangedIgnoreMs;
	}

}
