/* ==================================================================
 * CanbusDatumDataSourceTests.java - 8/10/2019 10:38:59 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.canbus.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.external.indriya.IndriyaMeasurementServiceProvider;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.datum.canbus.CanbusDatumDataSource;
import net.solarnetwork.node.datum.canbus.CanbusMessageConfig;
import net.solarnetwork.node.datum.canbus.CanbusPropertyConfig;
import net.solarnetwork.node.io.canbus.support.MeasurementHelper;
import net.solarnetwork.util.StaticOptionalService;
import net.solarnetwork.util.StaticOptionalServiceCollection;
import systems.uom.ucum.internal.UCUMServiceProvider;

/**
 * Test cases for the {@link CanbusDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CanbusDatumDataSourceTests {

	private static final String TEST_SOURCE = "/test/source";

	private DatumMetadataService datumMetadataService;
	private CanbusDatumDataSource dataSource;

	@Before
	public void setup() {
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);

		dataSource = new CanbusDatumDataSource();
		dataSource.setDatumMetadataService(new StaticOptionalService<>(datumMetadataService));
		dataSource.setMeasurementHelper(new MeasurementHelper(new StaticOptionalServiceCollection<>(
				Arrays.asList(new IndriyaMeasurementServiceProvider(new UCUMServiceProvider())))));
	}

	@After
	public void teardown() {
		EasyMock.verify(datumMetadataService);
	}

	private void replayAll() {
		EasyMock.replay(datumMetadataService);
	}

	@Test
	public void generateMetadata() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig();
		CanbusPropertyConfig prop1 = new CanbusPropertyConfig("watts",
				GeneralDatumSamplesType.Instantaneous, 0);
		prop1.setLocalizedNames(new KeyValuePair[] { new KeyValuePair("en", "Foo Bar"),
				new KeyValuePair("zh-Hans", "Foo酒吧") });
		prop1.setUnit("kW");
		message.setPropConfigs(new CanbusPropertyConfig[] { prop1 });

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>();
		datumMetadataService.addSourceMetadata(eq(TEST_SOURCE), capture(metaCaptor));

		// WHEN
		replayAll();
		dataSource.configurationChanged(null);

		// THEN
		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertThat("Metadata added", meta, notNullValue());
		assertThat("Property metadata keys", meta.getPm().keySet(), containsInAnyOrder("watts"));

		assertThat("watts property metadata values", meta.getPm().get("watts").keySet(),
				containsInAnyOrder("name", "unit"));
		assertThat("watts names", meta.getPm().get("watts"),
				hasEntry("name", prop1.getLocalizedNamesMap()));
		assertThat("watts unit", meta.getPm().get("watts"), hasEntry("unit", "kW"));
	}

}
