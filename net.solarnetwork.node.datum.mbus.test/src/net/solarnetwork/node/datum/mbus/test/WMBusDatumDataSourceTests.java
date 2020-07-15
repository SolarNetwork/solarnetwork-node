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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.datum.mbus.MBusPropertyConfig;
import net.solarnetwork.node.datum.mbus.WMBusDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.mbus.MBusDataDescription;
import net.solarnetwork.node.io.mbus.MBusDataRecord;
import net.solarnetwork.node.io.mbus.MBusDataType;
import net.solarnetwork.node.io.mbus.MBusMessage;

/**
 * Test cases for the {@link WMBusDatumDataSource} class.
 * 
 * @author alex
 * @version 1.0
 */
public class WMBusDatumDataSourceTests {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_BCD_PROP_NAME = "bcd";
	private static final String TEST_DOUBLE_PROP_NAME = "double";
	private static final String TEST_LONG_PROP_NAME = "long";
	private static final String TEST_STRING_PROP_NAME = "string";
	private static final String TEST_DATE_PROP_NAME = "date";

	private WMBusDatumDataSource dataSource;

	@Before
	public void setup() {
		dataSource = new WMBusDatumDataSource();
		dataSource.setSourceId(TEST_SOURCE_ID);
	}

	@Test
	public void readDatumWithInstantaneousValues() throws IOException {
		// GIVEN
		MBusPropertyConfig[] propConfigs = new MBusPropertyConfig[] {
				new MBusPropertyConfig(TEST_BCD_PROP_NAME, GeneralDatumSamplesType.Instantaneous,
						MBusDataType.BCD, MBusDataDescription.Volume, BigDecimal.ONE, 3),
				new MBusPropertyConfig(TEST_DOUBLE_PROP_NAME, GeneralDatumSamplesType.Instantaneous,
						MBusDataType.Double, MBusDataDescription.Energy, BigDecimal.ONE, 3),
				new MBusPropertyConfig(TEST_LONG_PROP_NAME, GeneralDatumSamplesType.Instantaneous,
						MBusDataType.Long, MBusDataDescription.Current, BigDecimal.ONE, 0), };
		dataSource.setPropConfigs(propConfigs);
		final MBusMessage msg = new MBusMessage(new Date());
		msg.dataRecords.add(new MBusDataRecord(MBusDataDescription.Volume, MBusDataType.BCD, 27L, -3));
		msg.dataRecords.add(new MBusDataRecord(MBusDataDescription.Energy, 1.234, 0));
		msg.dataRecords
				.add(new MBusDataRecord(MBusDataDescription.Current, MBusDataType.Long, 874234L, 0));

		// WHEN
		dataSource.handleMessage(msg);
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("BCD value", datum.getInstantaneousSampleDouble(TEST_BCD_PROP_NAME), equalTo(0.027));
		assertThat("Double value", datum.getInstantaneousSampleDouble(TEST_DOUBLE_PROP_NAME),
				equalTo(1.234));
		assertThat("Long value", datum.getInstantaneousSampleDouble(TEST_LONG_PROP_NAME),
				equalTo(874234.0));
	}

}
