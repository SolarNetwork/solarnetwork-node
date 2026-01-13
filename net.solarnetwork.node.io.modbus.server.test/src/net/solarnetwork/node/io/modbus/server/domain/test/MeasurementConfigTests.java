/* ==================================================================
 * MeasurementConfigTests.java - 13/01/2026 2:51:22 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.domain.test;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.server.domain.MeasurementConfig;

/**
 * Test cases for the {@link MeasurementConfig} class.
 *
 * @author matt
 * @version 1.0
 */
public class MeasurementConfigTests {

	@Test
	public void controlId() {
		// GIVEN
		final MeasurementConfig config = new MeasurementConfig();
		config.setSourceId("foo/bar");
		config.setControlId("bim/bam");

		// THEN
		// @formatter:off
		then(config.controlId())
			.as("Effective control ID is given control ID")
			.isEqualTo(config.getControlId())
			;
		// @formatter:on
	}

	@Test
	public void controlId_sourceId() {
		// GIVEN
		final MeasurementConfig config = new MeasurementConfig();
		config.setSourceId("foo/bar");
		config.setControlId(MeasurementConfig.CONTROL_ID_AS_SOURCE_ID);

		// THEN
		// @formatter:off
		then(config.controlId())
			.as("Effective control ID is source ID")
			.isEqualTo(config.getSourceId())
			;
		// @formatter:on
	}

	@Test
	public void controlId_sourceId_null() {
		// GIVEN
		final MeasurementConfig config = new MeasurementConfig();
		config.setControlId(MeasurementConfig.CONTROL_ID_AS_SOURCE_ID);

		// THEN
		// @formatter:off
		then(config.controlId())
			.as("Effective control ID is source ID")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void controlId_sourceIdAndPropertyName() {
		// GIVEN
		final MeasurementConfig config = new MeasurementConfig();
		config.setSourceId("foo/bar");
		config.setPropertyName("pow");
		config.setControlId(MeasurementConfig.CONTROL_ID_AS_SOURCE_ID_AND_PROPERTY_NAME);

		// THEN
		// @formatter:off
		then(config.controlId())
			.as("Effective control ID is source ID and property name")
			.isEqualTo("%s/%s".formatted(config.getSourceId(), config.getPropertyName()))
			;
		// @formatter:on
	}

	@Test
	public void controlId_sourceIdAndPropertyName_nulls() {
		// GIVEN
		final MeasurementConfig config = new MeasurementConfig();
		config.setControlId(MeasurementConfig.CONTROL_ID_AS_SOURCE_ID_AND_PROPERTY_NAME);

		// THEN
		// @formatter:off
		then(config.controlId())
			.as("Effective control ID is source ID and property name")
			.isEqualTo("null/null")
			;
		// @formatter:on
	}

}
