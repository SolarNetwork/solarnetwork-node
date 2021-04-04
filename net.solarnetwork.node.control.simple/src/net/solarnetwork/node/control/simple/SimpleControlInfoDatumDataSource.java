/* ==================================================================
 * SimpleControlInfoDatumDataSource.java - Dec 18, 2014 7:05:05 AM
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

package net.solarnetwork.node.control.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.GeneralNodeControlInfoDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Datum data source for {@link NodeControlProvider} instances. This exposes the
 * available {@link NodeControlProvider} services as a datum data source, so
 * they can be queried and logged like any other datum data source.
 * 
 * @author matt
 * @version 1.1
 */
public class SimpleControlInfoDatumDataSource
		implements MultiDatumDataSource<GeneralNodeDatum>, SettingSpecifierProvider {

	/**
	 * Controls may not change freqently, so limit unchanging values to 10
	 * minutes sample windows.
	 */
	private static final long CACHE_MAX_MS = 1000L * 60L * 10L;

	private List<NodeControlProvider> providers;
	private MessageSource messageSource;
	private String groupUID;

	private final Map<String, GeneralNodeControlInfoDatum> cache = new LinkedHashMap<String, GeneralNodeControlInfoDatum>();

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public String getUID() {
		return "net.solarnetwork.node.control.simple.SimpleControlInfoDatumDataSource";
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	@Override
	public Class<? extends GeneralNodeDatum> getMultiDatumType() {
		return GeneralNodeControlInfoDatum.class;
	}

	@Override
	public Collection<GeneralNodeDatum> readMultipleDatum() {
		final long now = System.currentTimeMillis();
		List<GeneralNodeDatum> results = new ArrayList<GeneralNodeDatum>();
		for ( NodeControlProvider provider : providers ) {
			List<String> controlIds = provider.getAvailableControlIds();
			log.debug("Requesting control info from provider {}: {}", provider, controlIds);
			Map<String, List<NodeControlInfo>> infos = new LinkedHashMap<String, List<NodeControlInfo>>(
					controlIds.size());
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
				log.trace("Read NodeControlInfo: {}", info);
				List<NodeControlInfo> list = infos.get(controlId);
				if ( list == null ) {
					list = new ArrayList<NodeControlInfo>(controlIds.size());
					infos.put(controlId, list);
				}
				list.add(info);
			}
			if ( infos.size() > 0 ) {
				for ( List<NodeControlInfo> list : infos.values() ) {
					GeneralNodeControlInfoDatum datum = new GeneralNodeControlInfoDatum(list);
					GeneralNodeControlInfoDatum cached = cache.get(datum.getSourceId());
					if ( cached == null || (cached.getCreated().getTime() + CACHE_MAX_MS) < now
							|| (cached.getSamples() != null
									&& !cached.getSamples().equals(datum.getSamples())) ) {
						results.add(datum);
						cache.put(datum.getSourceId(), datum);
					} else {
						log.debug("Control {} has not changed from cached value: {}",
								datum.getSourceId(), cached);
					}
				}
			}
		}
		log.debug("Collected changed GeneralNodeControlInfoDatum: {}", results);
		return results;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.simple";
	}

	@Override
	public String getDisplayName() {
		return "Simple Control DataSource";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		SimpleControlInfoDatumDataSource defaults = new SimpleControlInfoDatumDataSource();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(4);
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		return results;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public void setProviders(List<NodeControlProvider> providers) {
		this.providers = providers;
	}

}
