/* ==================================================================
 * DatumDataSourceSupportTests.java - 8/03/2022 9:05:31 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support.test;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.eq;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link DatumDataSourceSupport} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumDataSourceSupportTests {

	private DatumMetadataService metaService;
	private TestDatumDataSourceSupport service;

	@Before
	public void setup() {
		metaService = EasyMock.createMock(DatumMetadataService.class);
		service = new TestDatumDataSourceSupport();
		service.setDatumMetadataService(new StaticOptionalService<>(metaService));
	}

	@After
	public void teardown() {
		EasyMock.verify(metaService);
	}

	private void replayAll() {
		EasyMock.replay(metaService);
	}

	private KeyValuePair[] testMetadata() {
		// @formatter:off
		return new KeyValuePair[] {
				null,
				new KeyValuePair(),
				new KeyValuePair("a", null),
				new KeyValuePair("b", "bee"),
				new KeyValuePair("int", Integer.toString(123)),
		};
		// @formatter:on
	}

	private KeyValuePair[] testKeyPathMetadata() {
		// @formatter:off
		return new KeyValuePair[] {
				new KeyValuePair("a", "eh"),
				new KeyValuePair("/m/m", "mmm"),
				new KeyValuePair("/pm/c/u", "icu"),
				new KeyValuePair("/pm/d/a", Integer.toString(123)),
				new KeyValuePair("/pm/d/b", "dee"),
				new KeyValuePair("/pm/d/e/f", "eff"),
		};
		// @formatter:on
	}

	private static class TestDatumDataSourceSupport extends DatumDataSourceSupport {

		@Override
		public void saveMetadata(String sourceId) {
			super.saveMetadata(sourceId);
		}

	}

	@Test
	public void saveMetadata_simple() {
		// GIVEN
		service.setMetadata(testMetadata());

		metaService.addSourceMetadata(eq("foo"), assertWith(new Assertion<GeneralDatumMetadata>() {

			@Override
			public void check(GeneralDatumMetadata meta) throws Throwable {
				Map<String, Object> info = meta.getInfo();
				assertThat("Info populated", info.keySet(), hasSize(2));
				assertThat("Info values populated", info,
						allOf(hasEntry("b", (Object) "bee"), hasEntry("int", 123)));
			}

		}));

		// WHEN
		replayAll();
		service.saveMetadata("foo");
	}

	@Test
	public void saveMetadata_keyPaths() {
		// GIVEN
		service.setMetadata(testKeyPathMetadata());

		metaService.addSourceMetadata(eq("foo"), assertWith(new Assertion<GeneralDatumMetadata>() {

			@Override
			public void check(GeneralDatumMetadata meta) throws Throwable {
				Map<String, Object> info = meta.getInfo();
				assertThat("Info populated", info.keySet(), hasSize(2));
				assertThat("Info values populated", info,
						allOf(hasEntry("a", (Object) "eh"), hasEntry("m", "mmm")));

				Map<String, Map<String, Object>> propInfo = meta.getPropertyInfo();
				assertThat("Property info populated", propInfo.keySet(), containsInAnyOrder("c", "d"));

				Map<String, Object> c = propInfo.get("c");
				assertThat("c map values", c, hasEntry("u", "icu"));
				assertThat("c map populated", c.keySet(), hasSize(1));

				Map<String, Object> d = propInfo.get("d");
				// @formatter:off
				assertThat("c map values", d, allOf(
						hasEntry("a", (Object) 123),
						hasEntry("b", (Object) "dee"),
						hasEntry("e", (Object) singletonMap("f", "eff")
				)));
				// @formatter:on

				assertThat("c map populated", d.keySet(), hasSize(3));
			}

		}));

		// WHEN
		replayAll();
		service.saveMetadata("foo");
	}

}
