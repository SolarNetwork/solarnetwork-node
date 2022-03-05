/* ==================================================================
 * ParameterDatumFilterServiceTests.java - 5/03/2022 1:35:36 PM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.param.test;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.filter.param.ParameterDatumFilterService;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * Test cases for the {@link ParameterDatumFilterService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ParameterDatumFilterServiceTests {

	private static final String SOURCE_ID_1 = "foo";
	private static final String PROP_1 = "voltage";
	private static final String PROP_2 = "amps";
	private static final String PARAM_1 = "p1";
	private static final String PARAM_2 = "p2";

	private ExpressionService exprService;
	private ParameterDatumFilterService xform;

	@Before
	public void setup() {
		xform = new ParameterDatumFilterService();
		exprService = new SpelExpressionService();
		xform.setExpressionServices(new StaticOptionalServiceCollection<>(singleton(exprService)));
	}

	private SimpleDatum createTestSimpleDatum(String sourceId, String prop, Number val) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId);
		datum.getSamples().putInstantaneousSampleValue(prop, val);
		return datum;
	}

	@Test
	public void simpleExpressions() throws Exception {
		// GIVEN
		ExpressionConfig config1 = new ExpressionConfig();
		config1.setName(PARAM_1);
		config1.setExpressionServiceId(exprService.getUid());
		config1.setExpression("voltage * 2");

		ExpressionConfig config2 = new ExpressionConfig();
		config2.setName(PARAM_2);
		config2.setExpressionServiceId(exprService.getUid());
		config2.setExpression("voltage * amps");
		xform.setExpressionConfigs(new ExpressionConfig[] { config1, config2 });

		// WHEN
		SimpleDatum d = createTestSimpleDatum(SOURCE_ID_1, PROP_1, 123);
		d.putSampleValue(DatumSamplesType.Instantaneous, PROP_2, 345);
		Map<String, Object> parameters = new LinkedHashMap<>();
		DatumSamplesOperations result = xform.filter(d, d.getSamples(), parameters);

		// THEN
		assertThat("Result unchanged", result, is(sameInstance(d.getSamples())));
		assertThat("Output parameters generated", parameters.keySet(), hasSize(2));
		assertThat("Parameters generated via expressions", parameters,
				allOf(hasEntry(PARAM_1, 246), hasEntry(PARAM_2, 42435)));
	}

}
