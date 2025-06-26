/* ==================================================================
 * SunSpecDeviceDatumDataSourceSupportTests.java - 12/09/2019 2:22:17 pm
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

package net.solarnetwork.node.hw.sunspec.support.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.combiner.test.StringCombinerAdvancedModelAccessorImpl_402_01Tests;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.test.IntegerInverterModelAccessor_101_01Tests;
import net.solarnetwork.node.hw.sunspec.support.SunSpecDeviceDatumDataSourceSupport;
import net.solarnetwork.node.hw.sunspec.test.ModelDataUtils;

/**
 * Test cases for the {@link SunSpecDeviceDatumDataSourceSupport} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SunSpecDeviceDatumDataSourceSupportTests {

	private class TestDatumDataSource extends SunSpecDeviceDatumDataSourceSupport {

		private final Class<? extends ModelAccessor> primaryType;

		private TestDatumDataSource(Class<? extends ModelAccessor> primaryType, ModelData modelData) {
			super(new AtomicReference<>(modelData));
			this.primaryType = primaryType;
		}

		@Override
		protected Class<? extends ModelAccessor> getPrimaryModelAccessorType() {
			return primaryType;
		}

		@Override
		protected SunSpecDeviceDatumDataSourceSupport getSettingsDefaultInstance() {
			return new TestDatumDataSource(this.primaryType, getSample());
		}

		public String getSecondaryTypesMessage() {
			return super.getSecondaryTypesMessage(getSample());
		}

	}

	@Test
	public void populateSecondaryModels_NoneAvailable() {
		// GIVEN
		final ModelData data = ModelDataUtils.getModelDataInstance(
				IntegerInverterModelAccessor_101_01Tests.class, "test-data-103-01.txt");
		TestDatumDataSource ds = new TestDatumDataSource(InverterModelAccessor.class, data);

		// WHEN
		String msg = ds.getSecondaryTypesMessage();

		// THEN
		assertThat("Message does not contain secondary models", msg, equalTo("N/A"));
	}

	@Test
	public void populateSecondaryModels_Lots() {
		// GIVEN
		final ModelData data = ModelDataUtils.getModelDataInstance(
				IntegerInverterModelAccessor_101_01Tests.class, "test-data-101-01.txt");
		TestDatumDataSource ds = new TestDatumDataSource(InverterModelAccessor.class, data);

		// WHEN
		String msg = ds.getSecondaryTypesMessage();

		// THEN
		assertThat("Message does not contain secondary models", msg, equalTo(
				"120 (Inverter controls nameplate ratings), 121 (Inverter controls basic settings), 122, 123, 126, 131, 132"));
	}

	@Test
	public void populateSecondaryModels_Combiner() {
		// GIVEN
		final ModelData data = ModelDataUtils.getModelDataInstance(
				StringCombinerAdvancedModelAccessorImpl_402_01Tests.class, "test-data-402-01.txt");
		TestDatumDataSource ds = new TestDatumDataSource(InverterModelAccessor.class, data);

		// WHEN
		String msg = ds.getSecondaryTypesMessage();

		// THEN
		assertThat("Message contains secondary models", msg, equalTo("402 (Advanced string combiner)"));
	}

}
