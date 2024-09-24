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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.tariff.SimpleTariffRate;
import net.solarnetwork.domain.tariff.SimpleTemporalTariffSchedule;
import net.solarnetwork.domain.tariff.TemporalRangeSetsTariff;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumHistorian;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.TariffScheduleProvider;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link ExpressionRoot} class.
 *
 * @author matt
 * @version 1.2
 */
public class ExpressionRootTests {

	private DatumService datumService;
	private DatumHistorian unfilteredDatumHistorian;
	private OperationalModesService opModesService;
	private MetadataService metadataService;
	private TariffScheduleProvider tariffScheduleProvider;
	private ExpressionService expressionService = new SpelExpressionService();

	@Before
	public void setup() {
		datumService = EasyMock.createMock(DatumService.class);
		unfilteredDatumHistorian = EasyMock.createMock(DatumHistorian.class);
		opModesService = EasyMock.createMock(OperationalModesService.class);
		metadataService = EasyMock.createMock(MetadataService.class);
		tariffScheduleProvider = EasyMock.createMock(TariffScheduleProvider.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumService, unfilteredDatumHistorian, opModesService, metadataService,
				tariffScheduleProvider);
	}

	private void replayAll() {
		EasyMock.replay(datumService, unfilteredDatumHistorian, opModesService, metadataService,
				tariffScheduleProvider);
	}

	private ExpressionRoot createTestRoot() {
		return createTestRoot(datumService, opModesService, metadataService, tariffScheduleProvider);
	}

	private static ExpressionRoot createTestRoot(DatumService datumService,
			OperationalModesService opModesService, MetadataService metadataService,
			TariffScheduleProvider tariffScheduleProvider) {
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

		ExpressionRoot r = new ExpressionRoot(d, s, p, datumService, opModesService, metadataService);
		r.setTariffScheduleProviders(
				new StaticOptionalServiceCollection<>(Collections.singleton(tariffScheduleProvider)));
		return r;
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
	public void hasUnfilteredLatest_yes() {
		// GIVEN
		SimpleDatum other = SimpleDatum.nodeDatum("bar");
		other.putSampleValue(Instantaneous, "aa", 100);

		expect(datumService.unfiltered()).andReturn(unfilteredDatumHistorian);
		expect(unfilteredDatumHistorian.offset("bar", 0, NodeDatum.class)).andReturn(other);

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		Boolean result = expressionService.evaluateExpression("hasUnfilteredLatest('bar')", null, root,
				null, Boolean.class);

		// THEN
		assertThat("Expression resolves true when datum returned from unfiltered DatumService", result,
				is(true));
	}

	@Test
	public void hasUnfilteredLatest_no() {
		// GIVEN
		expect(datumService.unfiltered()).andReturn(unfilteredDatumHistorian);
		expect(unfilteredDatumHistorian.offset("bar", 0, NodeDatum.class)).andReturn(null);

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		Boolean result = expressionService.evaluateExpression("hasUnfilteredLatest('bar')", null, root,
				null, Boolean.class);

		// THEN
		assertThat("Expression resolves false when no datum returned from unfiltered DatumService",
				result, is(false));
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
	public void unfilteredLatest() {
		// GIVEN
		SimpleDatum other = SimpleDatum.nodeDatum("bar");
		other.putSampleValue(Instantaneous, "aa", 100);

		expect(datumService.unfiltered()).andReturn(unfilteredDatumHistorian);
		expect(unfilteredDatumHistorian.offset("bar", 0, NodeDatum.class)).andReturn(other);

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();
		Integer result = expressionService.evaluateExpression("a + unfilteredLatest('bar')['aa']", null,
				root, null, Integer.class);

		// THEN
		assertThat("Expression resolves latest unfiltered datum", result, is(103));
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
	public void unfilteredLatestMatching() {
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
		expect(datumService.unfiltered()).andReturn(unfilteredDatumHistorian).anyTimes();
		expect(unfilteredDatumHistorian.offset(singleton("foo/*"), root.getTimestamp(), 0,
				NodeDatum.class)).andReturn(matches).anyTimes();

		// WHEN
		replayAll();
		BigDecimal result = expressionService.evaluateExpression(
				"sum(unfilteredLatestMatching('foo/*').?[aa < 111].![aa])", null, root, null,
				BigDecimal.class);
		BigDecimal result2 = expressionService.evaluateExpression(
				"sum(unfilteredLatestMatching('foo/*').?[aa < 111].![aa * bb])", null, root, null,
				BigDecimal.class);

		// THEN
		assertThat("Expression resolves matching unfiltered datum and evaluates projection", result,
				is(new BigDecimal("210")));
		assertThat("Expression resolves matching unfiltered datum and evaluates projection", result2,
				is(new BigDecimal("44200")));
	}

	@Test
	public void isOpMode_noService() {
		// GIVEN

		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot(datumService, null, null, null);
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

	@Test
	public void sort_strings() {
		// WHEN
		replayAll();
		ExpressionRoot root = createTestRoot();

		Collection<?> result = expressionService.evaluateExpression("sort({'b','a','c'})", null, root,
				null, Collection.class);

		// THEN
		assertThat("Sorted collection returned", result, contains("a", "b", "c"));
	}

	@Test
	public void sort_datumByProperty() {
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

		Collection<?> result = expressionService.evaluateExpression(
				"sort(latestMatching('foo/*'), true, 'aa').![aa]", null, root, null, Collection.class);

		// THEN
		assertThat("Sorted collection of extracted 'aa' prop values returned", result,
				contains(d3.getSampleInteger(Instantaneous, "aa"),
						d2.getSampleInteger(Instantaneous, "aa"),
						d1.getSampleInteger(Instantaneous, "aa")));
	}

	@Test
	public void resolveTariffRate() {
		// GIVEN
		final TemporalRangeSetsTariff t1 = new TemporalRangeSetsTariff("Jan-Jun", null, null, null,
				Collections.singletonList(
						new SimpleTariffRate("price", "Price", new BigDecimal("1.23"))),
				Locale.US);
		final TemporalRangeSetsTariff t2 = new TemporalRangeSetsTariff("Jul-Dec", null, null, null,
				Collections.singletonList(
						new SimpleTariffRate("price", "Price", new BigDecimal("12.34"))),
				Locale.US);
		final SimpleTemporalTariffSchedule schedule = new SimpleTemporalTariffSchedule(
				Arrays.asList(t1, t2));

		final String scheduleId = "my-schedule";
		expect(tariffScheduleProvider.getUid()).andReturn(scheduleId).anyTimes();
		expect(tariffScheduleProvider.tariffSchedule()).andReturn(schedule).anyTimes();

		final ExpressionRoot root = createTestRoot();

		// WHEN
		replayAll();

		LocalDateTime now = LocalDateTime.now();
		BigDecimal price = expressionService.evaluateExpression(
				"tariffSchedule('my-schedule')?.resolveTariff(datePlus(now(), 1, 'months'), null)?.rates['price']?.amount",
				null, root, null, BigDecimal.class);

		// THEN
		BigDecimal expectedPrice = schedule.resolveTariff(now.plusMonths(1), null).getRates()
				.get("price").getAmount();
		assertThat("Tariff rate resolved", price, is(equalTo(expectedPrice)));
	}

	@Test
	public void resolveDate_startOfHour() {
		// GIVEN
		final ExpressionRoot root = createTestRoot();

		// WHEN
		replayAll();
		Temporal result = expressionService.evaluateExpression(
				"dateTruncate(date(2024,8,13,12,30), \"hours\")", null, root, null, Temporal.class);

		// THEN
		assertThat("Result is LocalDateTime for start of hour", result,
				is(equalTo(LocalDateTime.of(2024, 8, 13, 12, 0))));
	}

	@Test
	public void resolveDate_startOfMonth() {
		// GIVEN
		final ExpressionRoot root = createTestRoot();

		// WHEN
		replayAll();
		Temporal result = expressionService.evaluateExpression(
				"dateTruncate(date(2024,8,13), \"months\")", null, root, null, Temporal.class);

		// THEN
		assertThat("Result is LocalDate for 1st of month", result,
				is(equalTo(LocalDate.of(2024, 8, 1))));
	}

	@Test
	public void resolveDate_startOfNextWeek() {
		// GIVEN
		final ExpressionRoot root = createTestRoot();

		// WHEN
		replayAll();
		Temporal result = expressionService.evaluateExpression(
				"datePlus(dateTruncate(date(2024,8,13), \"weeks\"), 1, \"weeks\")", null, root, null,
				Temporal.class);

		// THEN
		assertThat("Result is LocalDate for following Monday", result,
				is(equalTo(LocalDate.of(2024, 8, 19))));
	}

}
