/* ==================================================================
 * SimpleControlInfoDatumDao.java - Oct 1, 2011 7:23:36 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DatumDao} implementation for {@link NodeControlInfoDatum} objects.
 * 
 * <p>
 * This service uses registered {@link NodeControlProvider} instances to manage
 * all node control providers in the system. It keeps track of the state
 * returned by each provider, and only returns changed data when the
 * {@link #getDatumNotUploaded()} method is called.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class SimpleControlInfoDatumDao implements DatumDao<NodeControlInfoDatum> {

	private List<NodeControlProvider> providers;

	private final Map<String, Map<String, NodeControlInfoDatum>> cache = new LinkedHashMap<String, Map<String, NodeControlInfoDatum>>();

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public Class<? extends NodeControlInfoDatum> getDatumType() {
		return NodeControlInfoDatum.class;
	}

	@Override
	public List<NodeControlInfoDatum> getDatumNotUploaded(String destination) {
		List<NodeControlInfoDatum> results = new ArrayList<NodeControlInfoDatum>();
		if ( providers != null ) {
			for ( NodeControlProvider provider : providers ) {
				List<String> controlIds = provider.getAvailableControlIds();
				log.debug("Requesting control info from provider {}: {}", provider, controlIds);
				for ( String controlId : provider.getAvailableControlIds() ) {
					NodeControlInfo info = provider.getCurrentControlInfo(controlId);
					if ( info == null ) {
						log.debug("No info returned for control {}", controlId);
						continue;
					}
					log.trace("Read NodeControlInfo: {}", info);
					NodeControlInfoDatum datum = new NodeControlInfoDatum();
					datum.setSourceId(controlId);
					datum.setPropertyName(info.getPropertyName());
					datum.setReadonly(info.getReadonly());
					datum.setType(info.getType());
					datum.setUnit(info.getUnit());
					datum.setValue(info.getValue());

					Map<String, NodeControlInfoDatum> destCache = cache.get(destination);
					if ( destCache == null ) {
						destCache = new LinkedHashMap<String, NodeControlInfoDatum>();
						cache.put(destination, destCache);
					}
					NodeControlInfoDatum cached = destCache.get(controlId);
					if ( cached == null || !cached.equals(datum) ) {
						results.add(datum);
						destCache.put(controlId, datum);
					} else {
						log.debug("Control {} has not changed from cached value: {}", controlId, cached);
					}
				}
			}
			log.debug("Collected changed NodeControlInfoDatum: {}", results);
		}
		return results;
	}

	@Override
	public void storeDatum(NodeControlInfoDatum datum) {
		// nothing to do here
	}

	@Override
	public int deleteUploadedDataOlderThan(int hours) {
		return 0;
	}

	@Override
	public void setDatumUploaded(NodeControlInfoDatum datum, Date date, String destination,
			String trackingId) {
		// nothing to do here
	}

	public List<NodeControlProvider> getProviders() {
		return providers;
	}

	public void setProviders(List<NodeControlProvider> providers) {
		this.providers = providers;
	}

}
