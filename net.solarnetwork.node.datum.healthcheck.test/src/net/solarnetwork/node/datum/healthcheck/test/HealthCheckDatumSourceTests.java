/* ==================================================================
 * HealthCheckDatumSourceTests.java - 16/12/2021 12:12:41 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.healthcheck.test;

import static java.lang.String.format;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.node.datum.healthcheck.HealthCheckDatumSource.PROP_MESSAGE;
import static net.solarnetwork.node.datum.healthcheck.HealthCheckDatumSource.PROP_SUCCESS;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.healthcheck.HealthCheckDatumSource;
import net.solarnetwork.node.datum.healthcheck.PublishMode;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.SystemHealthService;
import net.solarnetwork.node.service.SystemHealthService.PingTestResults;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.PingTestResultDisplay;

/**
 * Test cases for the {@link HealthCheckDatumSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class HealthCheckDatumSourceTests {

	public static final String TEST_SOURCE_ID = "health";

	private SystemHealthService systemHealthService;
	private List<PingTest> tests;
	private HealthCheckDatumSource ds;

	@Before
	public void setup() {
		systemHealthService = EasyMock.createMock(SystemHealthService.class);
		tests = new ArrayList<>();
		ds = new HealthCheckDatumSource(systemHealthService);
	}

	@After
	public void teardown() {
		EasyMock.verify(systemHealthService);
		if ( !tests.isEmpty() ) {
			EasyMock.verify(tests.toArray(new Object[tests.size()]));
		}
	}

	private PingTest newMockTest(String testId, String name, long timeout, PingTest.Result result)
			throws Exception {
		PingTest t = EasyMock.createMock(PingTest.class);
		expect(t.getPingTestMaximumExecutionMilliseconds()).andReturn(timeout).anyTimes();
		expect(t.getPingTestId()).andReturn(testId).anyTimes();
		expect(t.getPingTestName()).andReturn(name).anyTimes();
		expect(t.performPingTest()).andReturn(result).anyTimes();
		tests.add(t);
		EasyMock.replay(t);
		return t;
	}

	private void replayAll() {
		EasyMock.replay(systemHealthService);
	}

	@Test
	public void nullTestResult() {
		// GIVEN
		expect(systemHealthService.performPingTests(null)).andReturn(null);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Null returned from null test result", datum, is(nullValue()));
	}

	@Test
	public void emptyTestResult() {
		// GIVEN
		Map<String, PingTestResultDisplay> resultMap = new LinkedHashMap<>();
		PingTestResults testResults = new PingTestResults(Instant.now(), resultMap);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Null returned from null test result", datum, is(nullValue()));
	}

	@Test
	public void emptyTestResult_merged() {
		// GIVEN
		ds.setSourceId(TEST_SOURCE_ID);
		Map<String, PingTestResultDisplay> resultMap = new LinkedHashMap<>();
		PingTestResults testResults = new PingTestResults(Instant.now(), resultMap);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Null returned from null test result", datum, is(nullValue()));
	}

	@Test
	public void pubAlways_oneTestResult_ok() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String testId = "foo";

		PingTestResult testResult = new PingTestResult(true, "OK");
		PingTest test = newMockTest(testId, "Foo Test", 1000, testResult);

		ds.setPublishMode(PublishMode.Always);
		Map<String, PingTestResultDisplay> resultMap = new LinkedHashMap<>();
		resultMap.put(testId, new PingTestResultDisplay(test, testResult, now));
		PingTestResults testResults = new PingTestResults(Instant.now(), resultMap);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Datum returned for test result", datum, hasSize(1));
		NodeDatum d = datum.iterator().next();
		assertThat("Source ID derived from test ID", d.getSourceId(), is(equalTo(testId)));
		assertThat("Status property OK", d.asSampleOperations().getSampleString(DatumSamplesType.Status,
				HealthCheckDatumSource.PROP_SUCCESS), is(equalTo("true")));
		assertThat("Message property not published when test OK", d.asSampleOperations().getSampleString(
				DatumSamplesType.Status, HealthCheckDatumSource.PROP_MESSAGE), is(nullValue()));
	}

	@Test
	public void pubAlways_oneTestResult_fail() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String testId = "foo";

		PingTestResult testResult = new PingTestResult(false, "BOO");
		PingTest test = newMockTest(testId, "Foo Test", 1000, testResult);

		ds.setPublishMode(PublishMode.Always);
		Map<String, PingTestResultDisplay> resultMap = new LinkedHashMap<>();
		resultMap.put(testId, new PingTestResultDisplay(test, testResult, now));
		PingTestResults testResults = new PingTestResults(Instant.now(), resultMap);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Datum returned for test result", datum, hasSize(1));
		NodeDatum d = datum.iterator().next();
		assertThat("Source ID derived from test ID", d.getSourceId(), is(equalTo(testId)));
		assertThat("Status property FAIL", d.asSampleOperations().getSampleString(
				DatumSamplesType.Status, HealthCheckDatumSource.PROP_SUCCESS), is(equalTo("false")));
		assertThat(
				"Message property published when test FAIL", d.asSampleOperations()
						.getSampleString(DatumSamplesType.Status, HealthCheckDatumSource.PROP_MESSAGE),
				is(testResult.getMessage()));
	}

	@Test
	public void pubFailOnly_oneTestResult_ok() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String testId = "foo";

		PingTestResult testResult = new PingTestResult(true, "OK");
		PingTest test = newMockTest(testId, "Foo Test", 1000, testResult);

		ds.setPublishMode(PublishMode.OnFailure);
		Map<String, PingTestResultDisplay> resultMap = new LinkedHashMap<>();
		resultMap.put(testId, new PingTestResultDisplay(test, testResult, now));
		PingTestResults testResults = new PingTestResults(Instant.now(), resultMap);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Null returned with mode OnFailure from successful test result", datum,
				is(nullValue()));
	}

	@Test
	public void pubFailOnly_multiTestResult_mixed() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String testId1 = "foo";
		final String testId2 = "bar";

		PingTestResult testResult1 = new PingTestResult(true, "OK");
		PingTest test1 = newMockTest(testId1, "Foo Test", 1000, testResult1);

		PingTestResult testResult2 = new PingTestResult(false, "BOO");
		PingTest test2 = newMockTest(testId2, "Bar Test", 1000, testResult2);

		ds.setPublishMode(PublishMode.OnFailure);
		Map<String, PingTestResultDisplay> resultMap = new LinkedHashMap<>();
		resultMap.put(testId1, new PingTestResultDisplay(test1, testResult1, now));
		resultMap.put(testId2, new PingTestResultDisplay(test2, testResult2, now));
		PingTestResults testResults = new PingTestResults(Instant.now(), resultMap);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Datum returned for failed test result", datum, hasSize(1));
		NodeDatum d = datum.iterator().next();
		assertThat("Source ID derived from test ID", d.getSourceId(), is(equalTo(testId2)));
		assertThat("Status property OK", d.asSampleOperations().getSampleString(Status, PROP_SUCCESS),
				is(equalTo("false")));
		assertThat("Message property published when test FAIL",
				d.asSampleOperations().getSampleString(Status, PROP_MESSAGE),
				is(testResult2.getMessage()));
	}

	@Test
	public void pubAlways_multiTestResult_mixed_merged() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String testId1 = "foo";
		final String testId2 = "bar";

		PingTestResult testResult1 = new PingTestResult(true, "OK");
		PingTest test1 = newMockTest(testId1, "Foo Test", 1000, testResult1);

		PingTestResult testResult2 = new PingTestResult(false, "BOO");
		PingTest test2 = newMockTest(testId2, "Bar Test", 1000, testResult2);

		ds.setSourceId(TEST_SOURCE_ID);
		ds.setPublishMode(PublishMode.Always);
		Map<String, PingTestResultDisplay> resultMap = new LinkedHashMap<>();
		resultMap.put(testId1, new PingTestResultDisplay(test1, testResult1, now));
		resultMap.put(testId2, new PingTestResultDisplay(test2, testResult2, now));
		PingTestResults testResults = new PingTestResults(Instant.now(), resultMap);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Merged datum returned for all test result", datum, hasSize(1));
		NodeDatum d = datum.iterator().next();
		DatumSamplesOperations ops = d.asSampleOperations();
		assertThat("Source ID is fixed", d.getSourceId(), is(equalTo(TEST_SOURCE_ID)));
		assertThat("Overall status is FAIL", ops.getSampleString(Status, PROP_SUCCESS),
				is(equalTo("false")));
		assertThat("Status property 1 OK",
				ops.getSampleString(Status, format("%s_%s", testId1, PROP_SUCCESS)),
				is(equalTo("true")));
		assertThat("Message property 1 not published when OK",
				ops.getSampleString(Status, format("%s_%s", testId1, PROP_MESSAGE)), is(nullValue()));

		assertThat("Status property 2 FAIL",
				ops.getSampleString(Status, format("%s_%s", testId2, PROP_SUCCESS)),
				is(equalTo("false")));
		assertThat("Message property 2 published when FAIL",
				ops.getSampleString(Status, format("%s_%s", testId2, PROP_MESSAGE)),
				is(equalTo(testResult2.getMessage())));
	}

	@Test
	public void pubOnChange_firstTestResult_ok() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String testId = "foo";

		PingTestResult testResult = new PingTestResult(true, "OK");
		PingTest test = newMockTest(testId, "Foo Test", 1000, testResult);

		ds.setPublishMode(PublishMode.OnChange);
		Map<String, PingTestResultDisplay> resultMap = new LinkedHashMap<>();
		resultMap.put(testId, new PingTestResultDisplay(test, testResult, now));
		PingTestResults testResults = new PingTestResults(Instant.now(), resultMap);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Datum returned for first OnChange test result", datum, hasSize(1));
		NodeDatum d = datum.iterator().next();
		DatumSamplesOperations ops = d.asSampleOperations();
		assertThat("Source ID derived from test ID", d.getSourceId(), is(equalTo(testId)));
		assertThat("Status property OK",
				ops.getSampleString(DatumSamplesType.Status, HealthCheckDatumSource.PROP_SUCCESS),
				is(equalTo("true")));
		assertThat("Message property not published when OK",
				ops.getSampleString(Status, format("%s_%s", testId, PROP_MESSAGE)), is(nullValue()));
	}

	@Test
	public void pubOnChange_firstTestResult_fail() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String testId = "foo";

		PingTestResult testResult = new PingTestResult(false, "BOO");
		PingTest test = newMockTest(testId, "Foo Test", 1000, testResult);

		ds.setPublishMode(PublishMode.OnChange);
		Map<String, PingTestResultDisplay> resultMap = new LinkedHashMap<>();
		resultMap.put(testId, new PingTestResultDisplay(test, testResult, now));
		PingTestResults testResults = new PingTestResults(Instant.now(), resultMap);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum = ds.readMultipleDatum();

		// THEN
		assertThat("Datum returned for first OnChange test result", datum, hasSize(1));
		NodeDatum d = datum.iterator().next();
		assertThat("Source ID derived from test ID", d.getSourceId(), is(equalTo(testId)));
		assertThat("Status property FAIL", d.asSampleOperations().getSampleString(
				DatumSamplesType.Status, HealthCheckDatumSource.PROP_SUCCESS), is(equalTo("false")));
		assertThat(
				"Message property published when test FAIL", d.asSampleOperations()
						.getSampleString(DatumSamplesType.Status, HealthCheckDatumSource.PROP_MESSAGE),
				is(testResult.getMessage()));
	}

	@Test
	public void pubOnChange_firstTestResult_ok_ok() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String testId = "foo";
		ds.setPublishMode(PublishMode.OnChange);

		PingTestResult testResult1 = new PingTestResult(true, "OK");
		PingTest test1 = newMockTest(testId, "Foo Test", 1000, testResult1);
		Map<String, PingTestResultDisplay> resultMap1 = new LinkedHashMap<>();
		resultMap1.put(testId, new PingTestResultDisplay(test1, testResult1, now));
		PingTestResults testResults1 = new PingTestResults(Instant.now(), resultMap1);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults1);

		Thread.sleep(200);
		final Instant now2 = Instant.now();
		PingTestResult testResult2 = new PingTestResult(true, "OK");
		PingTest test2 = newMockTest(testId, "Foo Test", 1000, testResult2);
		Map<String, PingTestResultDisplay> resultMap2 = new LinkedHashMap<>();
		resultMap2.put(testId, new PingTestResultDisplay(test2, testResult2, now2));
		PingTestResults testResults2 = new PingTestResults(Instant.now(), resultMap2);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults2);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum1 = ds.readMultipleDatum();
		Collection<NodeDatum> datum2 = ds.readMultipleDatum();

		// THEN
		assertThat("Datum returned for first OnChange test result", datum1, hasSize(1));
		NodeDatum d = datum1.iterator().next();
		DatumSamplesOperations ops = d.asSampleOperations();
		assertThat("Source ID derived from test ID", d.getSourceId(), is(equalTo(testId)));
		assertThat("Status property OK",
				ops.getSampleString(DatumSamplesType.Status, HealthCheckDatumSource.PROP_SUCCESS),
				is(equalTo("true")));
		assertThat("Message property not published when OK",
				ops.getSampleString(Status, format("%s_%s", testId, PROP_MESSAGE)), is(nullValue()));

		assertThat("No Datum returned for OnChange test result that has not changed", datum2,
				is(nullValue()));
	}

	@Test
	public void pubOnChange_firstTestResult_ok_fail() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String testId = "foo";
		ds.setPublishMode(PublishMode.OnChange);

		PingTestResult testResult1 = new PingTestResult(true, "OK");
		PingTest test1 = newMockTest(testId, "Foo Test", 1000, testResult1);
		Map<String, PingTestResultDisplay> resultMap1 = new LinkedHashMap<>();
		resultMap1.put(testId, new PingTestResultDisplay(test1, testResult1, now));
		PingTestResults testResults1 = new PingTestResults(Instant.now(), resultMap1);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults1);

		Thread.sleep(200);
		final Instant now2 = Instant.now();
		PingTestResult testResult2 = new PingTestResult(false, "BOO");
		PingTest test2 = newMockTest(testId, "Foo Test", 1000, testResult2);
		Map<String, PingTestResultDisplay> resultMap2 = new LinkedHashMap<>();
		resultMap2.put(testId, new PingTestResultDisplay(test2, testResult2, now2));
		PingTestResults testResults2 = new PingTestResults(Instant.now(), resultMap2);
		expect(systemHealthService.performPingTests(null)).andReturn(testResults2);

		// WHEN
		replayAll();
		Collection<NodeDatum> datum1 = ds.readMultipleDatum();
		Collection<NodeDatum> datum2 = ds.readMultipleDatum();

		// THEN
		assertThat("Datum returned for first OnChange test result", datum1, hasSize(1));
		NodeDatum d = datum1.iterator().next();
		DatumSamplesOperations ops = d.asSampleOperations();
		assertThat("Source ID derived from test ID", d.getSourceId(), is(equalTo(testId)));
		assertThat("Status property OK",
				ops.getSampleString(DatumSamplesType.Status, HealthCheckDatumSource.PROP_SUCCESS),
				is(equalTo("true")));
		assertThat("Message property not published when OK",
				ops.getSampleString(Status, format("%s_%s", testId, PROP_MESSAGE)), is(nullValue()));

		assertThat("Datum returned for second OnChange test result because status changed", datum1,
				hasSize(1));
		d = datum2.iterator().next();
		ops = d.asSampleOperations();
		assertThat("Source ID derived from test ID", d.getSourceId(), is(equalTo(testId)));
		assertThat("Status property FAIL",
				ops.getSampleString(DatumSamplesType.Status, HealthCheckDatumSource.PROP_SUCCESS),
				is(equalTo("false")));
		assertThat("Message property published when test FAIL",
				ops.getSampleString(DatumSamplesType.Status, HealthCheckDatumSource.PROP_MESSAGE),
				is(testResult2.getMessage()));

	}

}
