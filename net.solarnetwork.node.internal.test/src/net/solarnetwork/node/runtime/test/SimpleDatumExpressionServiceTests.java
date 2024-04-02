/* ==================================================================
 * SimpleDatumExpressionServiceTests.java - 2/04/2024 5:57:37 pm
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

package net.solarnetwork.node.runtime.test;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleEnergyDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.runtime.DefaultDatumService;
import net.solarnetwork.node.runtime.SimpleDatumExpressionService;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link SimpleDatumExpressionService} class.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleDatumExpressionServiceTests {

	private SpelExpressionService spel;
	private DefaultDatumService datumService;
	private SimpleDatumExpressionService service;

	@Before
	public void setup() {
		AntPathMatcher pathMatcher = new AntPathMatcher();
		pathMatcher.setCaseSensitive(false);
		pathMatcher.setCachePatterns(false);

		ObjectMapper mapper = JsonUtils.newDatumObjectMapper();

		datumService = new DefaultDatumService(pathMatcher, mapper,
				new StaticOptionalService<DatumMetadataService>(null));

		spel = new SpelExpressionService();

		service = new SimpleDatumExpressionService(datumService);
		service.setExpressionServices(
				new StaticOptionalServiceCollection<ExpressionService>(singleton(spel)));
	}

	@Test
	public void executeExpression() {
		// GIVEN
		final Integer watts = 1234;

		final SimpleEnergyDatum d = new SimpleEnergyDatum("/power/1", Instant.now(), new DatumSamples());
		d.putSampleValue(DatumSamplesType.Instantaneous, "watts", watts);
		datumService.accept(d);

		final Map<String, String> instrParams = new LinkedHashMap<>(4);
		instrParams.put(SimpleDatumExpressionService.PARAM_EXPRESSION_LANGUAGE, spel.getUid());
		instrParams.put(SimpleDatumExpressionService.PARAM_EXPRESSION, "latest('/power/1').watts");
		final Instruction instr = InstructionUtils.createLocalInstruction(
				SimpleDatumExpressionService.TOPIC_DATUM_EXPRESSION, instrParams);

		// WHEN
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result is completed", result.getInstructionState(),
				is(equalTo(InstructionState.Completed)));

		Map<String, ?> resultParams = result.getResultParameters();
		assertThat("Expression result provided as JSON array on 'result' result parameter", resultParams,
				hasEntry("result", Arrays.asList(1234)));
	}

	@Test
	public void executeExpression_defaultServiceId() {
		// GIVEN
		final Integer watts = 1234;

		final SimpleEnergyDatum d = new SimpleEnergyDatum("/power/1", Instant.now(), new DatumSamples());
		d.putSampleValue(DatumSamplesType.Instantaneous, "watts", watts);
		datumService.accept(d);

		final Map<String, String> instrParams = new LinkedHashMap<>(4);
		instrParams.put(SimpleDatumExpressionService.PARAM_EXPRESSION, "latest('/power/1').watts");
		final Instruction instr = InstructionUtils.createLocalInstruction(
				SimpleDatumExpressionService.TOPIC_DATUM_EXPRESSION, instrParams);

		// WHEN
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Result returned, using default expression service", result, is(notNullValue()));
		assertThat("Result is completed", result.getInstructionState(),
				is(equalTo(InstructionState.Completed)));

		Map<String, ?> resultParams = result.getResultParameters();
		assertThat("Expression result provided as JSON array on 'result' result parameter", resultParams,
				hasEntry("result", Arrays.asList(1234)));
	}

}
