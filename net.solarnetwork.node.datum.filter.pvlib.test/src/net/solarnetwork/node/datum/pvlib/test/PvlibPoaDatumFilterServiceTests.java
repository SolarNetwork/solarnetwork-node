/* ==================================================================
 * PvlibPoaDatumFilterServiceTests.java - 27/10/2026 10:15:42 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.pvlib.test;

import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.node.datum.pvlib.PvlibPoaDatumFilterService.DEFAULT_GHI_PROPERTY_NAME;
import static net.solarnetwork.node.datum.pvlib.PvlibPoaDatumFilterService.DEFAULT_POA_PROPERTY_NAME;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.datum.pvlib.PvlibPoaDatumFilterService;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link PvlibPoaDatumFilterService} class.
 *
 * @author elijah
 * @version 1.0
 */
@SuppressWarnings("static-access")
public class PvlibPoaDatumFilterServiceTests {

	private static final String SOURCE_ID = "test.source";
	private static final String META_PATH = "/pm/pv-characteristics";
	private static final String META_KEY = "pv-characteristics";
	private static final Instant DATE = Instant.parse("2026-01-15T01:00:00Z");
	private static final int GHI = 800;

	private static final String BIFACIAL_OPTION = "--bifacial";

	private static final String[] BIFACIAL_VALUE_OPTIONS = new String[] { "--bifaciality", "--albedo",
			"--height", "--pitch", "--shade-factor", "--transmission-factor" };

	/**
	 * Test filter that captures the command instead of running it.
	 */
	private static class TestPvlibPoaDatumFilterService extends PvlibPoaDatumFilterService {

		private final List<List<String>> commands = new ArrayList<>(2);
		private Map<String, ?> result = emptyMap();

		private TestPvlibPoaDatumFilterService(InstantSource clock, ObjectMapper objectMapper,
				OptionalService<DatumMetadataService> datumMetadataService,
				OptionalFilterableService<MetadataService> characteristicsMetadataService) {
			super(clock, objectMapper, datumMetadataService, characteristicsMetadataService);
		}

		@Override
		protected Map<String, ?> executeCommand(List<String> cmd) {
			commands.add(cmd);
			return result;
		}

	}

	private DatumMetadataService datumMetadataService;
	private MetadataService metadataService;
	private TestPvlibPoaDatumFilterService service;

	@Before
	public void setup() {
		datumMetadataService = mock(DatumMetadataService.class);
		metadataService = mock(MetadataService.class);
		service = new TestPvlibPoaDatumFilterService(InstantSource.fixed(DATE), new ObjectMapper(),
				new StaticOptionalService<>(datumMetadataService),
				new StaticOptionalService<>(metadataService));
		service.setLat(new BigDecimal("-36.8485"));
		service.setLon(new BigDecimal("174.7633"));
		service.setTimeZoneId("Pacific/Auckland");
	}

	@After
	public void teardown() {
		then(datumMetadataService).shouldHaveNoMoreInteractions();
		then(metadataService).shouldHaveNoMoreInteractions();
	}

	private static SimpleDatum createTestDatum() {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue(DEFAULT_GHI_PROPERTY_NAME, GHI);
		return SimpleDatum.nodeDatum(SOURCE_ID, DATE, s);
	}

	private static Map<String, Object> pvCharacteristics(Object... keyValuePairs) {
		Map<String, Object> result = new LinkedHashMap<>(keyValuePairs.length / 2);
		for ( int i = 0; i < keyValuePairs.length; i += 2 ) {
			result.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
		}
		return result;
	}

	private static GeneralDatumMetadata pvCharacteristicsMetadata(Map<String, Object> props) {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		props.forEach((k, v) -> meta.putInfoValue(META_KEY, k, v));
		return meta;
	}

	/**
	 * Get the arguments of the one command executed by the service.
	 *
	 * @return the command arguments, as a map of option keys to values
	 */
	private Map<String, String> executedCommandArguments() {
		// @formatter:off
		and.then(service.commands)
			.as("Command executed once")
			.hasSize(1)
			;
		// @formatter:on
		final List<String> cmd = service.commands.get(0);
		// @formatter:off
		and.then(cmd)
			.as("Command is configured command")
			.first()
			.isEqualTo(service.getCommand())
			;
		and.then(cmd.size() - 1)
			.as("Command arguments are option/value pairs")
			.isEven()
			;
		// @formatter:on
		Map<String, String> args = new LinkedHashMap<>(cmd.size() / 2);
		for ( int i = 1; i < cmd.size(); i += 2 ) {
			args.put(cmd.get(i), cmd.get(i + 1));
		}
		return args;
	}

	@Test
	public void bifacialOff_noBifacialOptions() {
		// GIVEN
		service.setTilt(new BigDecimal("20"));
		service.setAzimuth(new BigDecimal("0"));

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Standard options provided")
			.contains(
					entry("--latitude", "-36.8485"),
					entry("--longitude", "174.7633"),
					entry("--zone", "Pacific/Auckland"),
					entry("--array-tilt", "20"),
					entry("--array-azimuth", "0"),
					entry("--transpose", "haydavies"),
					entry("--irradiance", "800"),
					entry("--date", "2026-01-15T14:00"))
			.as("Bifacial mode not provided when not configured")
			.doesNotContainKey(BIFACIAL_OPTION)
			.as("Bifacial options not provided when not configured")
			.doesNotContainKeys(BIFACIAL_VALUE_OPTIONS)
			;
		// @formatter:on
	}

	@Test
	public void bifacialOff_albedoProvided() {
		// GIVEN
		service.setAlbedo(new BigDecimal("0.3"));

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Bifacial mode not provided when not configured")
			.doesNotContainKey(BIFACIAL_OPTION)
			.as("Albedo provided without bifacial mode")
			.containsEntry("--albedo", "0.3")
			;
		// @formatter:on
	}

	@Test
	public void bifacialSettings_passThrough() {
		// GIVEN
		service.setGcr(new BigDecimal("0.4"));
		service.setBifacial(true);
		service.setBifaciality(new BigDecimal("0.7"));
		service.setAlbedo(new BigDecimal("0.3"));
		service.setRowHeight(new BigDecimal("2.1"));
		service.setRowPitch(new BigDecimal("6.5"));
		service.setShadeFactor(new BigDecimal("-0.05"));
		service.setTransmissionFactor(new BigDecimal("-0.01"));

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Bifacial options provided from settings")
			.contains(
					entry("--gcr", "0.4"),
					entry("--bifacial", "true"),
					entry("--bifaciality", "0.7"),
					entry("--albedo", "0.3"),
					entry("--height", "2.1"),
					entry("--pitch", "6.5"),
					entry("--shade-factor", "-0.05"),
					entry("--transmission-factor", "-0.01"))
			;
		// @formatter:on
	}

	@Test
	public void bifacialSettings_rangeBoundaries() {
		// GIVEN
		service.setGcr(BigDecimal.ONE);
		service.setBifacial(true);
		service.setBifaciality(BigDecimal.ONE);
		service.setAlbedo(BigDecimal.ZERO);
		service.setRowHeight(new BigDecimal("100"));
		service.setRowPitch(new BigDecimal("1000"));
		service.setShadeFactor(new BigDecimal("-1"));
		service.setTransmissionFactor(BigDecimal.ZERO);

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Bifacial options at range boundaries provided")
			.contains(
					entry("--gcr", "1"),
					entry("--bifaciality", "1"),
					entry("--albedo", "0"),
					entry("--height", "100"),
					entry("--pitch", "1000"),
					entry("--shade-factor", "-1"),
					entry("--transmission-factor", "0"))
			;
		// @formatter:on
	}

	@Test
	public void bifacialSettings_outOfRange() {
		// GIVEN
		service.setGcr(BigDecimal.ZERO);
		service.setBifacial(true);
		service.setBifaciality(new BigDecimal("1.5"));
		service.setAlbedo(new BigDecimal("-0.1"));
		service.setRowHeight(BigDecimal.ZERO);
		service.setRowPitch(new BigDecimal("1000.1"));
		service.setShadeFactor(new BigDecimal("0.1"));
		service.setTransmissionFactor(new BigDecimal("-1.5"));

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Bifacial mode provided")
			.containsEntry(BIFACIAL_OPTION, "true")
			.as("Out of range bifacial settings not provided")
			.doesNotContainKeys(BIFACIAL_VALUE_OPTIONS)
			.as("Out of range GCR not provided")
			.doesNotContainKey("--gcr")
			;
		// @formatter:on
	}

	@Test
	public void bifacialSettings_rowGeometryOtherBoundsOutOfRange() {
		// GIVEN
		service.setBifacial(true);
		service.setRowHeight(new BigDecimal("100.1"));
		service.setRowPitch(BigDecimal.ZERO);

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Bifacial mode provided")
			.containsEntry(BIFACIAL_OPTION, "true")
			.as("Row height above maximum not provided")
			.doesNotContainKey("--height")
			.as("Row pitch at exclusive minimum not provided")
			.doesNotContainKey("--pitch")
			;
		// @formatter:on
	}

	@Test
	public void datumMetadata_overridesSettings() {
		// GIVEN
		service.setMetadataPath(META_PATH);
		service.setGcr(new BigDecimal("0.4"));
		service.setBifaciality(new BigDecimal("0.8"));
		service.setRowHeight(new BigDecimal("1.5"));
		service.setRowPitch(new BigDecimal("5"));

		// @formatter:off
		final GeneralDatumMetadata meta = pvCharacteristicsMetadata(pvCharacteristics(
				"gcr", 0.35,
				"bifacial", true,
				"bifaciality", 0.75,
				"albedo", 0.3,
				"pvRowHeight", 2.5,
				"pvRowPitch", 7,
				"shadeFactor", -0.03,
				"transmissionFactor", "-0.01"));
		// @formatter:on
		given(datumMetadataService.getSourceMetadata(SOURCE_ID)).willReturn(meta);

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		then(datumMetadataService).should().getSourceMetadata(SOURCE_ID);
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Bifacial options provided from datum metadata")
			.contains(
					entry("--gcr", "0.35"),
					entry("--bifacial", "true"),
					entry("--bifaciality", "0.75"),
					entry("--albedo", "0.3"),
					entry("--height", "2.5"),
					entry("--pitch", "7"),
					entry("--shade-factor", "-0.03"),
					entry("--transmission-factor", "-0.01"))
			;
		// @formatter:on
	}

	@Test
	public void datumMetadata_overridesCharacteristicsMetadata() {
		// GIVEN
		service.setMetadataPath(META_PATH);
		service.setCharacteristicsMetadataServiceUid("pv");

		// @formatter:off
		final GeneralDatumMetadata pvMeta = pvCharacteristicsMetadata(pvCharacteristics(
				"bifacial", "true",
				"pvRowHeight", 2,
				"pvRowPitch", 6));
		// @formatter:on
		given(metadataService.getAllMetadata()).willReturn(pvMeta);

		final GeneralDatumMetadata meta = pvCharacteristicsMetadata(pvCharacteristics("pvRowHeight", 3));
		given(datumMetadataService.getSourceMetadata(SOURCE_ID)).willReturn(meta);

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		then(metadataService).should().getAllMetadata();
		then(datumMetadataService).should().getSourceMetadata(SOURCE_ID);
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Bifacial options merged from metadata sources")
			.contains(
					entry("--bifacial", "true"),
					entry("--height", "3"),
					entry("--pitch", "6"))
			;
		// @formatter:on
	}

	@Test
	public void datumMetadata_bifacialOff_overridesSetting() {
		// GIVEN
		service.setMetadataPath(META_PATH);
		service.setBifacial(true);

		final GeneralDatumMetadata meta = pvCharacteristicsMetadata(
				pvCharacteristics("bifacial", false));
		given(datumMetadataService.getSourceMetadata(SOURCE_ID)).willReturn(meta);

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		then(datumMetadataService).should().getSourceMetadata(SOURCE_ID);
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Bifacial mode disabled from datum metadata")
			.containsEntry(BIFACIAL_OPTION, "false")
			;
		// @formatter:on
	}

	@Test
	public void datumMetadata_invalid_keepsSettings() {
		// GIVEN
		service.setMetadataPath(META_PATH);
		service.setGcr(new BigDecimal("0.4"));
		service.setBifacial(true);
		service.setBifaciality(new BigDecimal("0.7"));
		service.setAlbedo(new BigDecimal("0.2"));
		service.setRowHeight(new BigDecimal("2"));
		service.setRowPitch(new BigDecimal("6"));
		service.setShadeFactor(new BigDecimal("-0.05"));
		service.setTransmissionFactor(new BigDecimal("-0.02"));

		// @formatter:off
		final GeneralDatumMetadata meta = pvCharacteristicsMetadata(pvCharacteristics(
				"gcr", 1.1,
				"bifacial", "yes",
				"bifaciality", 1.5,
				"albedo", "not a number",
				"pvRowHeight", 0,
				"pvRowPitch", 0,
				"shadeFactor", 0.1,
				"transmissionFactor", -1.5));
		// @formatter:on
		given(datumMetadataService.getSourceMetadata(SOURCE_ID)).willReturn(meta);

		final SimpleDatum datum = createTestDatum();

		// WHEN
		service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		then(datumMetadataService).should().getSourceMetadata(SOURCE_ID);
		// @formatter:off
		and.then(executedCommandArguments())
			.as("Invalid metadata values ignored, keeping setting values")
			.contains(
					entry("--gcr", "0.4"),
					entry("--bifacial", "true"),
					entry("--bifaciality", "0.7"),
					entry("--albedo", "0.2"),
					entry("--height", "2"),
					entry("--pitch", "6"),
					entry("--shade-factor", "-0.05"),
					entry("--transmission-factor", "-0.02"))
			;
		// @formatter:on
	}

	@Test
	public void poaGlobalResult() {
		// GIVEN
		service.setBifacial(true);

		final Map<String, Object> cmdResult = new LinkedHashMap<>(4);
		cmdResult.put("poa_global", 1012.5);
		cmdResult.put("poa_front", 934.1);
		cmdResult.put("poa_back", 98.0);
		service.result = cmdResult;

		final SimpleDatum datum = createTestDatum();

		// WHEN
		DatumSamplesOperations result = service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		// @formatter:off
		and.then(service.commands)
			.as("Command executed once")
			.hasSize(1)
			;
		and.then(result)
			.as("Result changed")
			.isNotSameAs(datum.getSamples())
			.as("POA global result extracted")
			.returns(new BigDecimal("1012.5"),
					from(s -> s.getSampleBigDecimal(Instantaneous, DEFAULT_POA_PROPERTY_NAME)))
			.as("GHI property preserved")
			.returns(GHI,
					from(s -> s.getSampleInteger(Instantaneous, DEFAULT_GHI_PROPERTY_NAME)))
			.as("Other result values not added without expressions")
			.returns(null, from(s -> s.getSampleValue(Instantaneous, "poa_front")))
			;
		// @formatter:on
	}

	@Test
	public void poaFrontResult() {
		// GIVEN
		service.setBifacial(true);
		service.setPoaResultKey("poa_front");

		final Map<String, Object> cmdResult = new LinkedHashMap<>(4);
		cmdResult.put("poa_global", 1012.5);
		cmdResult.put("poa_front", 934.1);
		service.result = cmdResult;

		final SimpleDatum datum = createTestDatum();

		// WHEN
		DatumSamplesOperations result = service.filter(datum, datum.getSamples(), emptyMap());

		// THEN
		// @formatter:off
		and.then(result)
			.as("POA front result extracted")
			.returns(new BigDecimal("934.1"),
					from(s -> s.getSampleBigDecimal(Instantaneous, DEFAULT_POA_PROPERTY_NAME)))
			;
		// @formatter:on
	}

}
