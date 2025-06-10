/* ==================================================================
 * VirtualMeterExpressionRootImplTests.java - 14/05/2021 3:51:19 PM
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

package net.solarnetwork.node.datum.filter.test;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.easymock.EasyMock;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.datum.filter.virt.VirtualMeterConfig;
import net.solarnetwork.node.datum.filter.virt.VirtualMeterExpressionRoot;
import net.solarnetwork.node.datum.filter.virt.VirtualMeterExpressionRootImpl;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.service.ExpressionService;

/**
 * Test cases for the {@link VirtualMeterExpressionRootImpl} class.
 *
 * @author matt
 * @version 2.2
 */
public class VirtualMeterExpressionRootImplTests {

	private SimpleDatum testDatum() {
		return SimpleDatum.nodeDatum("test", Instant.now(), new DatumSamples());
	}

	private VirtualMeterConfig testConfig() {
		VirtualMeterConfig config = new VirtualMeterConfig();
		config.setPropertyKey("test");
		config.setReadingPropertyName("testHours");
		config.setPropertyType(DatumSamplesType.Instantaneous);
		return config;
	}

	@Test
	public void timeUnits() {
		// GIVEN
		SimpleDatum d = testDatum();
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = BigDecimal.TEN;
		BigDecimal prevReading = BigDecimal.ONE;

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), null, null, null, c, prevDate, currDate, prevInput, currInput, prevReading);
		BigDecimal dt = root.getTimeUnits();

		// THEN
		assertThat("Time diff", dt, is(equalTo(new BigDecimal("0.016666666667"))));
	}

	@Test
	public void eval() {
		// GIVEN
		GeneralDatum d = testDatum();
		d.putSampleValue(DatumSamplesType.Instantaneous, "tou", new BigDecimal("11.50"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), null, null, null, c, prevDate, currDate, prevInput, currInput, prevReading);
		BigDecimal result = exprService.evaluateExpression(
				"prevReading + (timeUnits * (inputDiff / 1000) * tou)", null, root, null,
				BigDecimal.class);

		// THEN
		assertThat("Calculated result", result.setScale(4, RoundingMode.HALF_UP),
				is(equalTo(new BigDecimal("1.5750"))));
	}

	@Test
	public void eval_underscoreProperty() {
		// GIVEN
		GeneralDatum d = testDatum();
		d.putSampleValue(DatumSamplesType.Instantaneous, "my_rate", new BigDecimal("11.50"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), null, null, null, c, prevDate, currDate, prevInput, currInput, prevReading);
		BigDecimal result = exprService.evaluateExpression(
				"prevReading + (timeUnits * (inputDiff / 1000) * my_rate)", null, root, null,
				BigDecimal.class);

		// THEN
		assertThat("Calculated result", result.setScale(4, RoundingMode.HALF_UP),
				is(equalTo(new BigDecimal("1.5750"))));
	}

	@Test
	public void eval_conditional() {
		// GIVEN
		GeneralDatum d = testDatum();
		d.putSampleValue(DatumSamplesType.Instantaneous, "tou", new BigDecimal("11.50"));
		d.putSampleValue(DatumSamplesType.Instantaneous, "watts", new BigDecimal("100000"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), null, null, null, c, prevDate, currDate, prevInput, currInput, prevReading);

		BigDecimal result1 = exprService.evaluateExpression(
				"prevReading + (timeUnits * (inputDiff / 1000) * (watts > 50000 ? tou * 10 : tou))",
				null, root, null, BigDecimal.class);

		d.putSampleValue(DatumSamplesType.Instantaneous, "watts", new BigDecimal("25000"));
		BigDecimal result2 = exprService.evaluateExpression(
				"prevReading + (timeUnits * (inputDiff / 1000) * (watts > 50000 ? tou * 10 : tou))",
				null, root, null, BigDecimal.class);

		// THEN
		assertThat("Calculated result 1", result1.setScale(4, RoundingMode.HALF_UP),
				is(equalTo(new BigDecimal("6.7500"))));
		assertThat("Calculated result 2", result2.setScale(4, RoundingMode.HALF_UP),
				is(equalTo(new BigDecimal("1.5750"))));
	}

	@Test
	public void eval_conditional_missingCondition() {
		// GIVEN
		GeneralDatum d = testDatum();
		d.putSampleValue(DatumSamplesType.Instantaneous, "watts", new BigDecimal("100000"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), null, null, null, c, prevDate, currDate, prevInput, currInput, prevReading);

		BigDecimal result = exprService.evaluateExpression(
				"containsKey('tou') ? prevReading + (timeUnits * (inputDiff / 1000) * tou) : null", null,
				root, null, BigDecimal.class);

		// THEN
		assertThat("Calculated result 1", result, is(nullValue()));
	}

	@Test
	public void eval_conditional_missingCondition_alias() {
		// GIVEN
		GeneralDatum d = testDatum();
		d.putSampleValue(DatumSamplesType.Instantaneous, "watts", new BigDecimal("100000"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), null, null, null, c, prevDate, currDate, prevInput, currInput, prevReading);

		Integer result = exprService.evaluateExpression(
				"has('tou') ? prevReading + (timeUnits * (inputDiff / 1000) * tou) : -1", null, root,
				null, Integer.class);

		// THEN
		assertThat("Calculated result 1", result, is(equalTo(-1)));
	}

	@Test
	public void eval_withLatestDatum() {
		// GIVEN
		GeneralDatum d = testDatum();
		d.putSampleValue(DatumSamplesType.Instantaneous, "tou", new BigDecimal("11.50"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		SimpleDatum d2 = SimpleDatum.nodeDatum("bar");
		d2.putSampleValue(DatumSamplesType.Instantaneous, "f", 123);

		DatumService datumService = EasyMock.createMock(DatumService.class);

		expect(datumService.offset("bar", 0, NodeDatum.class)).andReturn(d2);

		// WHEN
		replay(datumService);
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), datumService, null, null, c, prevDate, currDate, prevInput, currInput,
				prevReading);
		BigDecimal result = exprService.evaluateExpression(
				"prevReading + (timeUnits * (inputDiff / 1000) * tou) + latest('bar')['f']", null, root,
				null, BigDecimal.class);

		// THEN
		assertThat("Calculated result", result.setScale(4, RoundingMode.HALF_UP),
				is(equalTo(new BigDecimal("124.5750"))));
		verify(datumService);
	}

}
