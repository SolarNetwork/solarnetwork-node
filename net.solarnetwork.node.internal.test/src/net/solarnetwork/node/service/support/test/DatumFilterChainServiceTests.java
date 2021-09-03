/* ==================================================================
 * GeneralDatumSamplesTransformChainTests.java - 29/07/2021 6:31:13 AM
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

package net.solarnetwork.node.service.support.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.node.service.support.DatumFilterChainService;
import net.solarnetwork.service.DatumFilterService;

/**
 * Test cases for the {@link DatumFilterChainService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumFilterChainServiceTests {

	private static final String TEST_UID = "test";
	private static final String TEST_UID2 = "test2";

	private OperationalModesService opModesService;

	private List<DatumFilterService> xforms;
	private DatumFilterChainService chain;

	@Before
	public void setup() {
		opModesService = EasyMock.createMock(OperationalModesService.class);

		xforms = new ArrayList<>();
		chain = new DatumFilterChainService(TEST_UID, xforms);
		chain.setOpModesService(opModesService);
	}

	@After
	public void teardown() {
		EasyMock.verify(opModesService);
	}

	private void replayAll() {
		EasyMock.replay(opModesService);
	}

	private SimpleDatum createTestDatum() {
		SimpleDatum d = SimpleDatum.nodeDatum("test.source", Instant.now(), new DatumSamples());
		d.getSamples().putInstantaneousSampleValue("foo", 1);
		return d;
	}

	@Test
	public void opMode_disabled() {
		// GIVEN
		chain.setRequiredOperationalMode("foo");

		expect(opModesService.isOperationalModeActive("foo")).andReturn(false);

		SimpleDatum d = createTestDatum();

		// WHEN
		replayAll();
		DatumSamples s = new DatumSamples(d.getSamples());
		DatumSamplesOperations result = chain.filter(d, s, null);

		// THEN
		assertThat("Input instance returned because op mode disabled", result, is(sameInstance(s)));
		assertThat("Samples unchanged", result, is(equalTo(d.getSamples())));
	}

	private static class InvocationCountingTransform extends BaseDatumFilterSupport
			implements DatumFilterService {

		private int count = 0;
		private final List<DatumSamplesOperations> input = new ArrayList<>();
		private final List<DatumSamplesOperations> output = new ArrayList<>();
		private final List<Map<String, Object>> params = new ArrayList<>();

		public InvocationCountingTransform(String uid) {
			super();
			setUid(uid);
		}

		@Override
		public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
				Map<String, Object> parameters) {
			count++;
			input.add(samples);
			DatumSamples out = new DatumSamples(samples);
			output.add(out);
			params.add(parameters);
			return out;
		}

	}

	@Test
	public void opMode_enabled() {
		// GIVEN
		chain.setRequiredOperationalMode("foo");
		InvocationCountingTransform xform = new InvocationCountingTransform(TEST_UID);
		xforms.add(xform);
		chain.setTransformUids(new String[] { TEST_UID });

		expect(opModesService.isOperationalModeActive("foo")).andReturn(true);

		SimpleDatum d = createTestDatum();

		// WHEN
		replayAll();
		DatumSamples s = new DatumSamples(d.getSamples());
		DatumSamplesOperations result = chain.filter(d, s, null);

		// THEN
		assertThat("Different instance returned because op mode enabled", result,
				is(not(sameInstance(s))));
		assertThat("Transform invoked", xform.count, is(equalTo(1)));
		assertThat("Input instance provided to xform", s, is(sameInstance(xform.input.get(0))));
		assertThat("Transformed instance returned", result, is(sameInstance(xform.output.get(0))));
		assertThat("Parameter map provided", xform.params.get(0), is(notNullValue()));
	}

	@Test
	public void invoke_one() {
		// GIVEN
		InvocationCountingTransform xform = new InvocationCountingTransform(TEST_UID);
		xforms.add(xform);
		chain.setTransformUids(new String[] { TEST_UID });

		SimpleDatum d = createTestDatum();

		// WHEN
		replayAll();
		DatumSamples s = new DatumSamples(d.getSamples());
		DatumSamplesOperations result = chain.filter(d, s, null);

		// THEN
		assertThat("Different instance returned because op mode enabled", result,
				is(not(sameInstance(s))));
		assertThat("Transform invoked", xform.count, is(equalTo(1)));
		assertThat("Input instance provided to xform", s, is(sameInstance(xform.input.get(0))));
		assertThat("Transformed instance returned", result, is(sameInstance(xform.output.get(0))));
		assertThat("Parameter map provided", xform.params.get(0), is(notNullValue()));
	}

	@Test
	public void invoke_two() {
		// GIVEN
		InvocationCountingTransform xform = new InvocationCountingTransform(TEST_UID);
		xforms.add(xform);
		InvocationCountingTransform xform2 = new InvocationCountingTransform(TEST_UID2);
		xforms.add(xform2);
		chain.setTransformUids(new String[] { TEST_UID, TEST_UID2 });

		SimpleDatum d = createTestDatum();

		// WHEN
		replayAll();
		DatumSamples s = new DatumSamples(d.getSamples());
		DatumSamplesOperations result = chain.filter(d, s, null);

		// THEN
		assertThat("Different instance returned because op mode enabled", result,
				is(not(sameInstance(s))));
		assertThat("Transform 1 invoked", xform.count, is(equalTo(1)));
		assertThat("Input 1 instance provided to xform", s, is(sameInstance(xform.input.get(0))));
		assertThat("Parameter map provided", xform.params.get(0), is(notNullValue()));

		assertThat("Transform 2 invoked", xform2.count, is(equalTo(1)));
		assertThat("Input 2 instance provided from xform 1 output", xform2.input.get(0),
				is(sameInstance(xform.output.get(0))));
		assertThat("Parameter map provided, same as first xform", xform2.params.get(0),
				is(sameInstance(xform.params.get(0))));

		assertThat("Transformed instance 2 returned", result, is(sameInstance(xform2.output.get(0))));
	}

}
