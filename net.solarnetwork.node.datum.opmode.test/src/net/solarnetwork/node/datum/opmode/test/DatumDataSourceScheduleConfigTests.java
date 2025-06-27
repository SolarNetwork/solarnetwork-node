/* ==================================================================
 * DatumDataSourceScheduleConfigTests.java - 20/12/2018 2:53:26 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.opmode.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.datum.opmode.DatumDataSourceOpModeInvoker;
import net.solarnetwork.node.datum.opmode.DatumDataSourceScheduleConfig;
import net.solarnetwork.node.domain.datum.DcEnergyDatum;
import net.solarnetwork.node.domain.datum.EnergyDatum;
import net.solarnetwork.node.service.DatumDataSource;

/**
 * Test cases for the {@link DatumDataSourceOpModeInvoker}.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DatumDataSourceScheduleConfigTests {

	private DatumDataSource datumDataSource;

	@Before
	public void setup() {
		datumDataSource = EasyMock.createMock(DatumDataSource.class);
	}

	@Test
	public void datumTypeInvalidClass() {
		expect(datumDataSource.getDatumType()).andReturn((Class) DcEnergyDatum.class);

		DatumDataSourceScheduleConfig config = new DatumDataSourceScheduleConfig();
		config.setDatumType("nah");

		replay(datumDataSource);

		assertThat("datumType not", config.matches(datumDataSource), equalTo(false));

		verify(datumDataSource);
	}

	@Test
	public void datumTypeClassMismatch() {
		expect(datumDataSource.getDatumType()).andReturn((Class) DcEnergyDatum.class);

		DatumDataSourceScheduleConfig config = new DatumDataSourceScheduleConfig();
		config.setDatumType(String.class.getName());

		replay(datumDataSource);

		assertThat("datumType not", config.matches(datumDataSource), equalTo(false));

		verify(datumDataSource);
	}

	@Test
	public void datumTypeMatchExact() {
		expect(datumDataSource.getDatumType()).andReturn((Class) DcEnergyDatum.class);

		DatumDataSourceScheduleConfig config = new DatumDataSourceScheduleConfig();
		config.setDatumType(DcEnergyDatum.class.getName());

		replay(datumDataSource);

		assertThat("datumType match", config.matches(datumDataSource), equalTo(true));

		verify(datumDataSource);
	}

	@Test
	public void datumTypeMatchDirectInterface() {
		expect(datumDataSource.getDatumType()).andReturn((Class) DcEnergyDatum.class);

		DatumDataSourceScheduleConfig config = new DatumDataSourceScheduleConfig();
		config.setDatumType(DcEnergyDatum.class.getName());

		replay(datumDataSource);

		assertThat("datumType match", config.matches(datumDataSource), equalTo(true));

		verify(datumDataSource);
	}

	@Test
	public void datumTypeMatchInheritedInterface() {
		expect(datumDataSource.getDatumType()).andReturn((Class) DcEnergyDatum.class);

		DatumDataSourceScheduleConfig config = new DatumDataSourceScheduleConfig();
		config.setDatumType(EnergyDatum.class.getName());

		replay(datumDataSource);

		assertThat("datumType match", config.matches(datumDataSource), equalTo(true));

		verify(datumDataSource);
	}
}
