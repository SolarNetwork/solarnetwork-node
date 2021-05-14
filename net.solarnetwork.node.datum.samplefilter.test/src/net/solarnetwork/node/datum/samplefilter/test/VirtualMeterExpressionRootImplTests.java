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

package net.solarnetwork.node.datum.samplefilter.test;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.GeneralDatum;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.datum.samplefilter.virt.VirtualMeterConfig;
import net.solarnetwork.node.datum.samplefilter.virt.VirtualMeterExpressionRoot;
import net.solarnetwork.node.datum.samplefilter.virt.VirtualMeterExpressionRootImpl;
import net.solarnetwork.support.ExpressionService;

/**
 * Test cases for the {@link VirtualMeterExpressionRootImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class VirtualMeterExpressionRootImplTests {

	private GeneralDatum testDatum() {
		return new GeneralDatum("test", Instant.now());
	}

	private VirtualMeterConfig testConfig() {
		VirtualMeterConfig config = new VirtualMeterConfig();
		config.setPropertyKey("test");
		config.setReadingPropertyName("testHours");
		config.setPropertyType(GeneralDatumSamplesType.Instantaneous);
		return config;
	}

	@Test
	public void timeUnits() {
		// GIVEN
		GeneralDatum d = testDatum();
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = BigDecimal.TEN;
		BigDecimal prevReading = BigDecimal.ONE;

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), c, prevDate, currDate, prevInput, currInput, prevReading);
		BigDecimal dt = root.getTimeUnits();

		// THEN
		assertThat("Time diff", dt, is(equalTo(new BigDecimal("0.016666666667"))));
	}

	@Test
	public void eval() {
		// GIVEN
		GeneralDatum d = testDatum();
		d.putSampleValue(GeneralDatumSamplesType.Instantaneous, "tou", new BigDecimal("11.50"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), c, prevDate, currDate, prevInput, currInput, prevReading);
		BigDecimal result = exprService.evaluateExpression(
				"prevReading + (timeUnits * (inputDiff / 1000) * tou)", null, root, null,
				BigDecimal.class);

		// THEN
		assertThat("Calculated result", result.setScale(4, RoundingMode.HALF_UP),
				is(equalTo(new BigDecimal("1.5750"))));
	}

	@Test
	public void eval_conditional() {
		// GIVEN
		GeneralDatum d = testDatum();
		d.putSampleValue(GeneralDatumSamplesType.Instantaneous, "tou", new BigDecimal("11.50"));
		d.putSampleValue(GeneralDatumSamplesType.Instantaneous, "watts", new BigDecimal("100000"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), c, prevDate, currDate, prevInput, currInput, prevReading);

		BigDecimal result1 = exprService.evaluateExpression(
				"prevReading + (timeUnits * (inputDiff / 1000) * (watts > 50000 ? tou * 10 : tou))",
				null, root, null, BigDecimal.class);

		d.putSampleValue(GeneralDatumSamplesType.Instantaneous, "watts", new BigDecimal("25000"));
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
		d.putSampleValue(GeneralDatumSamplesType.Instantaneous, "watts", new BigDecimal("100000"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), c, prevDate, currDate, prevInput, currInput, prevReading);

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
		d.putSampleValue(GeneralDatumSamplesType.Instantaneous, "watts", new BigDecimal("100000"));
		VirtualMeterConfig c = testConfig();
		long currDate = d.getTimestamp().toEpochMilli();
		long prevDate = currDate - 60_000L;
		BigDecimal prevInput = BigDecimal.ZERO;
		BigDecimal currInput = new BigDecimal("3000");
		BigDecimal prevReading = BigDecimal.ONE;

		ExpressionService exprService = new SpelExpressionService();

		// WHEN
		VirtualMeterExpressionRoot root = new VirtualMeterExpressionRootImpl(d, d.getSamples(),
				emptyMap(), c, prevDate, currDate, prevInput, currInput, prevReading);

		Integer result = exprService.evaluateExpression(
				"has('tou') ? prevReading + (timeUnits * (inputDiff / 1000) * tou) : -1", null, root,
				null, Integer.class);

		// THEN
		assertThat("Calculated result 1", result, is(equalTo(-1)));
	}

}
