/* ==================================================================
 * Em6Co2DatumDataSource.java - 10/03/2023 11:50:54 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.co2.nz.em6;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * Collect carbon intensity datum for NZ from the em6 API.
 * 
 * @author matt
 * @version 1.0
 */
public class Em6Co2DatumDataSource extends DatumDataSourceSupport
		implements SettingSpecifierProvider, DatumDataSource, MultiDatumDataSource {

	private final Em6ApiClient client;

	/**
	 * Constructor.
	 * 
	 * @param client
	 *        the client to use
	 */
	public Em6Co2DatumDataSource(Em6ApiClient client) {
		super();
		this.client = requireNonNullArgument(client, "client");
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		Collection<NodeDatum> current = readMultipleDatum();
		return (current != null && !current.isEmpty() ? current.iterator().next() : null);
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return NodeDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		return client.currentCarbonIntensity();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.co2.nz.em6";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.addAll(baseIdentifiableSettings(null));
		return results;
	}

}
