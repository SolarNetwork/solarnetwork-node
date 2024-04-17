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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.ExpressionService;

/**
 * Test cases for the {@link ExpressionRoot} class.
 *
 * @author matt
 * @version 1.1
 */
public class ExpressionRootTests {

	private DatumService datumService;
	private OperationalModesService opModesService;
	private MetadataService metadataService;
	private ExpressionService expressionService = new SpelExpressionService();

	@Before
	public void setup() {
		datumService = EasyMock.createMock(DatumService.class);
		opModesService = EasyMock.createMock(OperationalModesService.class);
		metadataService = EasyMock.createMock(MetadataService.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumService, opModesService, metadataService);
	}

	private void replayAll() {
		EasyMock.replay(datumService, opModesService, metadataService);
	}

	private ExpressionRoot createTestRoot() {
		return createTestRoot(datumService, opModesService, metadataService);
	}

	private static ExpressionRoot createTestRoot(DatumService datumService,
			OperationalModesService opModesService, MetadataService metadataService) {
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

		return new ExpressionRoot(d, s, p, datumService, opModesService, metadataService);
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
	public void latestMatching() {
		// GIVEN
		ExpressionRoot root = createTestRoot();

		SimpleDatum d1 = SimpleDatum.nodeDatum("foo/1");
		d1.putSampleValue(Instantaneous, "aa", 100);
		d1.putSampleValue(Accumulating, "bb", 200);

		SimpleDatum d2 = SimpleDatum.nodeDatum("foo/2");
		d2.putSampleValue(Instantaneous, "aa", 110);
		d2.putSampleValue(Accumulating, "bb", 220);

		SimpleDatum d3 = SimpleDatum.nodeDatum("foo/3");
		d3.putSampleValue(Instantaneous, "aa", 111);
		d3.putSampleValue(Accumulating, "bb", 222);

		List<NodeDatum> matches = Arrays.asList(new NodeDatum[] { d1, d2, d3 });
		expect(datumService.offset(singleton("foo/*"), root.getTimestamp(), 0, NodeDatum.class))
				.andReturn(matches).anyTimes();

		// WHEN
		replayAll();
		BigDecimal result = expressionService.evaluateExpression(
				"sum(latestMatching('foo/*').?[aa < 111].![aa])", null, root, null, BigDecimal.class);
		BigDecimal result2 = expressionService.evaluateExpression(
				"sum(latestMatching('foo/*').?[aa < 111].![aa * bb])", null, root, null,
				BigDecimal.class);

		// THEN
		assertThat("Expression resolves matching datum and evaluates projection", result,
				is(new BigDecimal("210")));
		assertThat("Expression resolves matching datum and evaluates projection", result2,
				is(new BigDecimal("44200")));
	}

	@Test
	public void isOpMode_noService() {
		// GIVEN

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot(datumService, null, null);
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

	@Test
	public void datumMeta() {
		// GIVEN
		GeneralDatumMetadata meta1 = new GeneralDatumMetadata();
		meta1.putInfoValue("a", 1);
		meta1.putInfoValue("b", "two");
		meta1.putInfoValue("deviceInfo", "Version", "1.23.4");
		meta1.putInfoValue("deviceInfo", "Name", "Thingy");
		meta1.putInfoValue("deviceInfo", "Capacity", 3000);

		expect(datumService.datumMetadata("foo")).andReturn(meta1).anyTimes();

		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		meta2.putInfoValue("a", 2);
		meta2.putInfoValue("deviceInfo", "Capacity", 1000);

		expect(datumService.datumMetadata(singleton("foo/*"))).andReturn(asList(meta1, meta2))
				.anyTimes();

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		String result1 = expressionService.evaluateExpression("meta('foo')?.info?.b", null, root, null,
				String.class);
		Integer result2 = expressionService.evaluateExpression("sum(metaMatching('foo/*').![info?.a])",
				null, root, null, Integer.class);
		Integer result3 = expressionService.evaluateExpression(
				"meta('foo')?.getInfoNumber('deviceInfo', 'Capacity')", null, root, null, Integer.class);
		Integer result4 = expressionService.evaluateExpression(
				"sum(metaMatching('foo/*').![getInfoNumber('deviceInfo', 'Capacity')])", null, root,
				null, Integer.class);

		// THEN
		assertThat("Metadata info traversal", result1, is("two"));
		assertThat("Metadata match info direct traversal", result2, is(3));
		assertThat("Metadata property info traversal", result3, is(3000));
		assertThat("Metadata match property info traversal", result4, is(4000));
	}

	@Test
	public void nodeMeta() {
		// GIVEN
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("a", 1);
		meta.putInfoValue("b", "two");
		meta.putInfoValue("deviceInfo", "Version", "1.23.4");
		meta.putInfoValue("deviceInfo", "Name", "Thingy");
		meta.putInfoValue("deviceInfo", "Capacity", 3000);

		expect(metadataService.getAllMetadata()).andReturn(meta).anyTimes();

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		String result1 = expressionService.evaluateExpression("metadata()?.info?.b", null, root, null,
				String.class);
		Integer result2 = expressionService.evaluateExpression("getInfoNumber('a')", null, root, null,
				Integer.class);
		Integer result3 = expressionService.evaluateExpression("getInfoNumber('deviceInfo', 'Capacity')",
				null, root, null, Integer.class);
		String result4 = expressionService.evaluateExpression("getInfoString('deviceInfo', 'Name')",
				null, root, null, String.class);
		String result5 = expressionService.evaluateExpression(
				"metadata()?.propertyInfo?.deviceInfo?.Version", null, root, null, String.class);

		// THEN
		assertThat("Metadata info traversal", result1, is("two"));
		assertThat("Metadata info number accessor", result2, is(1));
		assertThat("Metadata property info number accessor", result3, is(3000));
		assertThat("Metadata property info string accessor", result4, is("Thingy"));
		assertThat("Metadata property info string direct traversal", result5, is("1.23.4"));
	}

}
