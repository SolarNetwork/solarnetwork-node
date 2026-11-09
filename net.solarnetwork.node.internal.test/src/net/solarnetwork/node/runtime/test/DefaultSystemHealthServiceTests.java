/* ==================================================================
 * DefaultSystemHealthServiceTests.java - 09/11/2026 4:12:08 pm
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

package net.solarnetwork.node.runtime.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.runtime.DefaultSystemHealthService;
import net.solarnetwork.node.service.SystemHealthService.PingTestResults;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.PingTestResultDisplay;

/**
 * Test cases for the {@link DefaultSystemHealthService} class.
 *
 * @author matt
 * @version 1.0
 */
public class DefaultSystemHealthServiceTests {

	private PingTest test1;
	private PingTest test2;
	private DefaultSystemHealthService service;

	@Before
	public void setup() {
		test1 = pingTest("test.one");
		test2 = pingTest("test.two");
		service = new DefaultSystemHealthService(asList(test1, test2));
	}

	private PingTest pingTest(String id) {
		PingTest t = EasyMock.createMock(PingTest.class);
		expect(t.getPingTestId()).andReturn(id).anyTimes();
		expect(t.getPingTestName()).andReturn(id).anyTimes();
		expect(t.getPingTestMaximumExecutionMilliseconds()).andReturn(1000L).anyTimes();
		return t;
	}

	private void replayAll() {
		EasyMock.replay(test1, test2);
	}

	@After
	public void teardown() {
		EasyMock.verify(test1, test2);
	}

	@Test
	public void ping_filtered() throws Exception {
		// GIVEN
		expect(test2.performPingTest()).andReturn(new PingTestResult(true, "OK"));

		// WHEN
		replayAll();
		PingTestResults results = service.performPingTests(singleton("TWO"));

		// THEN
		assertThat("Results returned", results, is(notNullValue()));
		assertThat("Only matching test performed", results.getResults().keySet(), contains("test.two"));
		assertThat("Matching test passed", results.isAllGood(), is(equalTo(true)));
	}

	@Test
	public void ping_invalidFilter() {
		// GIVEN

		// WHEN
		replayAll();
		PingTestResults results = service.performPingTests(singleton("["));

		// THEN
		assertThat("Results returned", results, is(notNullValue()));
		assertThat("No tests performed, only error result returned", results.getResults().keySet(),
				contains("__error.idPattern"));
		PingTestResultDisplay error = results.getResults().get("__error.idPattern");
		assertThat("Error result is not for a real test", error.getPingTestId(),
				is(equalTo("__error.idPattern")));
		assertThat("Error result failed", error.isSuccess(), is(equalTo(false)));
		assertThat("Error message includes the invalid pattern", error.getMessage(),
				startsWith("Invalid test ID pattern [[]"));
	}

}
