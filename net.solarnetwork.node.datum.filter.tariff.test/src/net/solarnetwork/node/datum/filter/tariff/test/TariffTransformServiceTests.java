/* ==================================================================
 * TariffTransformServiceTests.java - 13/05/2021 11:57:35 AM
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

package net.solarnetwork.node.datum.filter.tariff.test;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.tariff.SimpleTemporalRangesTariffEvaluator;
import net.solarnetwork.node.MetadataService;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.datum.filter.tariff.TariffTransformService;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link TariffTransformService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class TariffTransformServiceTests {

	private static final String META_PATH = "/pm/tariffs/foo";

	private MetadataService metadataService;
	private OperationalModesService opModesService;
	private TariffTransformService service;

	@Before
	public void setup() {
		metadataService = EasyMock.createMock(MetadataService.class);
		opModesService = EasyMock.createMock(OperationalModesService.class);
		service = new TariffTransformService(new StaticOptionalService<>(metadataService),
				new StaticOptionalService<>(SimpleTemporalRangesTariffEvaluator.DEFAULT_EVALUATOR));
		service.setTariffMetadataPath(META_PATH);
		service.setFirstMatchOnly(true);
		service.setOpModesService(opModesService);
	}

	@After
	public void teardown() {
		EasyMock.verify(metadataService, opModesService);
	}

	private void replayAll() {
		EasyMock.replay(metadataService, opModesService);
	}

	private String stringResource(String resource) {
		try {
			return FileCopyUtils.copyToString(
					new InputStreamReader(getClass().getResourceAsStream(resource), "UTF-8"));
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void matchFirst() {
		// GIVEN
		String csv = stringResource("test-tariffs-01.csv");
		expect(metadataService.metadataAtPath(META_PATH)).andReturn(csv);

		LocalDateTime datumDate = LocalDateTime.of(2021, 5, 13, 12, 15);
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSamples(new GeneralDatumSamples());
		d.setCreated(new Date(datumDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));

		// WHEN
		replayAll();
		GeneralDatumSamples result = service.transformSamples(d, d.getSamples(), emptyMap());

		// THEN
		assertThat("Result changed", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Single rate added", result.getSampleData().keySet(), contains("rate"));
		assertThat("Rate applied", result.getInstantaneousSampleBigDecimal("rate"),
				is(equalTo(new BigDecimal("11.00"))));
	}

	@Test
	public void matchMulti() {
		// GIVEN
		service.setFirstMatchOnly(false);
		String csv = stringResource("test-tariffs-02.csv");
		expect(metadataService.metadataAtPath(META_PATH)).andReturn(csv);

		LocalDateTime datumDate = LocalDateTime.of(2021, 5, 15, 2, 0);
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSamples(new GeneralDatumSamples());
		d.setCreated(new Date(datumDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));

		// WHEN
		replayAll();
		GeneralDatumSamples result = service.transformSamples(d, d.getSamples(), emptyMap());

		// THEN
		assertThat("Result changed", result, is(not(sameInstance(d.getSamples()))));
		assertThat("Two rates added", result.getSampleData().keySet(), contains("tou", "line"));
		assertThat("TOU rate applied", result.getInstantaneousSampleBigDecimal("tou"),
				is(equalTo(new BigDecimal("9.19"))));
		assertThat("Line rate applied", result.getInstantaneousSampleBigDecimal("line"),
				is(equalTo(new BigDecimal("9.99"))));
	}

	@Test
	public void operationalMode_noMatch() {
		// GIVEN
		service.setRequiredOperationalMode("foo");
		expect(opModesService.isOperationalModeActive("foo")).andReturn(false);

		LocalDateTime datumDate = LocalDateTime.of(2021, 5, 13, 12, 15);
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSamples(new GeneralDatumSamples());
		d.setCreated(new Date(datumDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));

		// WHEN
		replayAll();
		GeneralDatumSamples result = service.transformSamples(d, d.getSamples(), emptyMap());

		// THEN
		assertThat("Result unchanged because required operational mode not active", result,
				is(sameInstance(d.getSamples())));
	}

	@Test
	public void operationalMode_match() {
		// GIVEN
		service.setRequiredOperationalMode("foo");
		expect(opModesService.isOperationalModeActive("foo")).andReturn(true);

		String csv = stringResource("test-tariffs-01.csv");
		expect(metadataService.metadataAtPath(META_PATH)).andReturn(csv);

		LocalDateTime datumDate = LocalDateTime.of(2021, 5, 13, 12, 15);
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setSamples(new GeneralDatumSamples());
		d.setCreated(new Date(datumDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));

		// WHEN
		replayAll();
		GeneralDatumSamples result = service.transformSamples(d, d.getSamples(), emptyMap());

		// THEN
		assertThat("Result changed because required operational mode active", result,
				is(not(sameInstance(d.getSamples()))));
		assertThat("Single rate added", result.getSampleData().keySet(), contains("rate"));
		assertThat("Rate applied", result.getInstantaneousSampleBigDecimal("rate"),
				is(equalTo(new BigDecimal("11.00"))));
	}

}
