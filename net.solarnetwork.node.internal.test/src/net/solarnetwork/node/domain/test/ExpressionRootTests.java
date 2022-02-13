/* ==================================================================
 * ExpressionRootTests.java - 2/11/2021 9:27:47 AM
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

package net.solarnetwork.node.domain.test;

import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.util.HashMap;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.ExpressionService;

/**
 * Test cases for the {@link ExpressionRoot} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ExpressionRootTests {

	private DatumService datumService;
	private OperationalModesService opModesService;
	private ExpressionService expressionService = new SpelExpressionService();

	@Before
	public void setup() {
		datumService = EasyMock.createMock(DatumService.class);
		opModesService = EasyMock.createMock(OperationalModesService.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumService, opModesService);
	}

	private void replayAll() {
		EasyMock.replay(datumService, opModesService);
	}

	private ExpressionRoot createTestRoot() {
		return createTestRoot(datumService, opModesService);
	}

	private ExpressionRoot createTestRoot(DatumService datumService,
			OperationalModesService opModesService) {
		SimpleDatum d = SimpleDatum.nodeDatum("foo");
		d.putSampleValue(Instantaneous, "a", 3);
		d.putSampleValue(Instantaneous, "b", 5);
		d.putSampleValue(Accumulating, "c", 7);
		d.putSampleValue(Accumulating, "d", 9);

		DatumSamples s = new DatumSamples();
		d.putSampleValue(Instantaneous, "b", 21);
		d.putSampleValue(Instantaneous, "c", 23);
		d.putSampleValue(Accumulating, "e", 25);
		d.putSampleValue(Accumulating, "f", 25);

		Map<String, Object> p = new HashMap<>();
		p.put("d", 31);
		p.put("c", 33);
		p.put("f", 35);
		p.put("g", 35);

		return new ExpressionRoot(d, s, p, datumService, opModesService);
	}

	@Test
	public void hasLatest_yes() {
		// GIVEN
		SimpleDatum other = SimpleDatum.nodeDatum("bar");
		other.putSampleValue(Instantaneous, "aa", 100);

		expect(datumService.offset("bar", 0, NodeDatum.class)).andReturn(other);

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		Boolean result = expressionService.evaluateExpression("hasLatest('bar')", null, root, null,
				Boolean.class);

		// THEN
		assertThat("Expression resolves true when datum returned from DatumService", result, is(true));
	}

	@Test
	public void hasLatest_no() {
		// GIVEN
		expect(datumService.offset("bar", 0, NodeDatum.class)).andReturn(null);

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		Boolean result = expressionService.evaluateExpression("hasLatest('bar')", null, root, null,
				Boolean.class);

		// THEN
		assertThat("Expression resolves false when no datum returned from DatumService", result,
				is(false));
	}

	@Test
	public void latest() {
		// GIVEN
		SimpleDatum other = SimpleDatum.nodeDatum("bar");
		other.putSampleValue(Instantaneous, "aa", 100);

		expect(datumService.offset("bar", 0, NodeDatum.class)).andReturn(other);

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		Integer result = expressionService.evaluateExpression("a + latest('bar')['aa']", null, root,
				null, Integer.class);

		// THEN
		assertThat("Expression resolves latest datum", result, is(103));
	}

	@Test
	public void latest_conditionally() {
		// GIVEN
		SimpleDatum other = SimpleDatum.nodeDatum("bar");
		other.putSampleValue(Instantaneous, "aa", 100);
		other.putSampleValue(Accumulating, "bb", 200);

		expect(datumService.offset("bar", 0, NodeDatum.class)).andReturn(other).times(3);

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		Integer result = expressionService.evaluateExpression(
				"a + (hasLatest('bar') ? latest('bar')['aa'] + latest('bar')['bb'] : 0)", null, root,
				null, Integer.class);

		// THEN
		assertThat("Expression resolves latest datum conditionally", result, is(3 + 100 + 200));
	}

	@Test
	public void isOpMode_noService() {
		// GIVEN

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot(datumService, null);
		Boolean result = expressionService.evaluateExpression("isOpMode('foo')", null, root, null,
				Boolean.class);

		// THEN
		assertThat("isOpMode() returns false when no service available", result, is(false));
	}

	@Test
	public void isOpMode() {
		// GIVEN
		expect(opModesService.isOperationalModeActive("foo")).andReturn(true);
		expect(opModesService.isOperationalModeActive("bar")).andReturn(false);

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		Boolean result1 = expressionService.evaluateExpression("isOpMode('foo')", null, root, null,
				Boolean.class);
		Boolean result2 = expressionService.evaluateExpression("isOpMode('bar')", null, root, null,
				Boolean.class);

		// THEN
		assertThat("isOpMode('foo') returns result of isOperationalModeActive()", result1, is(true));
		assertThat("isOpMode('bar') returns result of isOperationalModeActive()", result2, is(false));
	}

}
