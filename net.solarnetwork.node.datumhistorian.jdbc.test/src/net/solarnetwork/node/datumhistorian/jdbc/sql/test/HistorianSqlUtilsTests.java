/* ==================================================================
 * HistorianSqlUtilsTests.java - 10 Nov 2026 11:35:17 pm
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

package net.solarnetwork.node.datumhistorian.jdbc.sql.test;

import static net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils.requireValidSchemaName;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import org.junit.Test;
import net.solarnetwork.node.datumhistorian.jdbc.sql.HistorianSqlUtils;

/**
 * Test cases for the {@link HistorianSqlUtils} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
public class HistorianSqlUtilsTests {

	@Test
	public void validSchemaNames() {
		for ( String name : new String[] { "solarhist", "solaruhist", "_x", "a1_b2", "a".repeat(63) } ) {
			// @formatter:off
			and.then(requireValidSchemaName(name))
				.as("Valid schema name [%s] returned", name)
				.isSameAs(name)
				;
			// @formatter:on
		}
	}

	@Test
	public void invalidSchemaNames() {
		for ( String name : new String[] { null, "", "1abc", "SolarHist", "solar-hist", "solar hist",
				"solarhist.dh_datum", "solarhist;", "\"solarhist\"", "a".repeat(64) } ) {
			thenIllegalArgumentException().as("Invalid schema name [%s] rejected", name)
					.isThrownBy(() -> requireValidSchemaName(name));
		}
	}

}
