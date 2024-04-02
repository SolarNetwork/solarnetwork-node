/* ==================================================================
 * BasicInstructionTests.java - 3/04/2024 6:45:24 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.reactor.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.node.reactor.BasicInstruction;

/**
 * Test cases for the {@link BasicInstruction} class.
 *
 * @author matt
 * @version 1.0
 */
public class BasicInstructionTests {

	@Test
	public void params_none() {
		// GIVEN
		BasicInstruction instr = new BasicInstruction(UUID.randomUUID().toString(), Instant.now(),
				UUID.randomUUID().toString(), null);

		// WHEN
		Map<String, String> params = instr.params();

		// THEN
		assertThat("Map not null", params, is(notNullValue()));
		assertThat("Map is empty", params.keySet(), is(empty()));
	}

	@Test
	public void params_singleValues() {
		// GIVEN
		BasicInstruction instr = new BasicInstruction(UUID.randomUUID().toString(), Instant.now(),
				UUID.randomUUID().toString(), null);
		instr.addParameter("foo", "bar");
		instr.addParameter("bim", "bam");

		// WHEN
		Map<String, String> params = instr.params();

		// THEN
		assertThat("Map not null", params, is(notNullValue()));
		assertThat("Single param value", params, hasEntry("foo", "bar"));
		assertThat("Single param value", params, hasEntry("bim", "bam"));
		assertThat("Param count", params.keySet(), hasSize(2));
	}

	@Test
	public void params_mergedValues() {
		// GIVEN
		BasicInstruction instr = new BasicInstruction(UUID.randomUUID().toString(), Instant.now(),
				UUID.randomUUID().toString(), null);
		instr.addParameter("foo", "bar");
		instr.addParameter("bim", "bam");
		instr.addParameter("foo", "hop");

		// WHEN
		Map<String, String> params = instr.params();

		// THEN
		assertThat("Map not null", params, is(notNullValue()));
		assertThat("Multi param value merged", params, hasEntry("foo", "barhop"));
		assertThat("Single param value", params, hasEntry("bim", "bam"));
		assertThat("Param count", params.keySet(), hasSize(2));
	}

}
