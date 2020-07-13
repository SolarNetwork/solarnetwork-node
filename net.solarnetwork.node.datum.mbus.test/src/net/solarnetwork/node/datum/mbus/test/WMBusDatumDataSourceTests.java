/* ==================================================================
 * WMBusDatumDataSourceTests.java - 09/07/2020 09:53:55 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.mbus.test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.datum.mbus.MBusPropertyConfig;
import net.solarnetwork.node.datum.mbus.WMBusDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.mbus.WMBusConnection;
import net.solarnetwork.node.io.mbus.WMBusNetwork;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link WMBusDatumDataSource} class.
 * 
 * @author alex
 * @version 1.0
 */
public class WMBusDatumDataSourceTests {

	private WMBusNetwork mbusNetwork;
	private WMBusConnection mbusConnection;
	private DatumMetadataService datumMetadataService;

	private WMBusDatumDataSource dataSource;

	@Before
	public void setup() {
		mbusNetwork = EasyMock.createMock(WMBusNetwork.class);
		mbusConnection = EasyMock.createMock(WMBusConnection.class);
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);

		dataSource = new WMBusDatumDataSource();
		dataSource.setWMBusNetwork(new StaticOptionalService<WMBusNetwork>(mbusNetwork));
		dataSource.setDatumMetadataService(
				new StaticOptionalService<DatumMetadataService>(datumMetadataService));
	}

	private void replayAll() {
		EasyMock.replay(mbusNetwork, mbusConnection, datumMetadataService);
	}

	@After
	public void teardown() {
		EasyMock.verify(mbusNetwork, mbusConnection, datumMetadataService);
	}

	@Test
	public void readDatumWithInstantaneousValues() throws IOException {
		// GIVEN
		MBusPropertyConfig[] propConfigs = new MBusPropertyConfig[] { new MBusPropertyConfig(), };
		dataSource.setPropConfigs(propConfigs);

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
	}

}
