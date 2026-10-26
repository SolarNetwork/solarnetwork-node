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
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
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
public class PvlibPoaDatumFilterServiceTests {

	private static final String SOURCE_ID = "test.source";
	private static final String META_PATH = "/pm/pv-characteristics";
	private static final Instant DATE = Instant.parse("2026-01-15T01:00:00Z");

	private static final String[] BIFACIAL_OPTIONS = new String[] { "--bifacial", "--bifaciality",
			"--albedo", "--height", "--pitch", "--shade-factor", "--transmission-factor" };

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
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);
		metadataService = EasyMock.createMock(MetadataService.class);
		service = new TestPvlibPoaDatumFilterService(InstantSource.fixed(DATE), new ObjectMapper(),
				new StaticOptionalService<>(datumMetadataService),
				new StaticOptionalService<>(metadataService));
		service.setLat(new BigDecimal("-36.8485"));
		service.setLon(new BigDecimal("174.7633"));
		service.setTimeZoneId("Pacific/Auckland");
	}

	@After
	public void teardown() {
		EasyMock.verify(datumMetadataService, metadataService);
	}

	private void replayAll() {
		EasyMock.replay(datumMetadataService, metadataService);
	}

	private SimpleDatum createTestDatum() {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue(PvlibPoaDatumFilterService.DEFAULT_GHI_PROPERTY_NAME, 800);
		return SimpleDatum.nodeDatum(SOURCE_ID, DATE, s);
	}

	private Map<String, String> commandArguments(List<String> cmd) {
		assertThat("Command arguments are option/value pairs", (cmd.size() - 1) % 2, is(equalTo(0)));
		Map<String, String> args = new LinkedHashMap<>(cmd.size() / 2);
		for ( int i = 1; i < cmd.size(); i += 2 ) {
			args.put(cmd.get(i), cmd.get(i + 1));
		}
		return args;
	}

	private Map<String, String> filterAndCaptureArguments() {
		SimpleDatum d = createTestDatum();
		service.filter(d, d.getSamples(), emptyMap());
		assertThat("Command executed once", service.commands, hasSize(1));
		List<String> cmd = service.commands.get(0);
		assertThat("Command is configured command", cmd.get(0), is(equalTo(service.getCommand())));
		return commandArguments(cmd);
	}

	@Test
	public void bifacialOff_noBifacialOptions() {
		// GIVEN
		service.setTilt(new BigDecimal("20"));
		service.setAzimuth(new BigDecimal("0"));

		// WHEN
		replayAll();
		Map<String, String> args = filterAndCaptureArguments();

		// THEN
		assertThat("Standard options provided", args,
				allOf(hasEntry("--latitude", "-36.8485"), hasEntry("--longitude", "174.7633"),
						hasEntry("--zone", "Pacific/Auckland"), hasEntry("--array-tilt", "20"),
						hasEntry("--array-azimuth", "0"), hasEntry("--transpose", "haydavies"),
						hasEntry("--irradiance", "800"), hasEntry("--date", "2026-01-15T14:00")));
		for ( String opt : BIFACIAL_OPTIONS ) {
			assertThat("Bifacial option not provided when not configured", args, not(hasKey(opt)));
		}
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

		// WHEN
		replayAll();
		Map<String, String> args = filterAndCaptureArguments();

		// THEN
		assertThat("Bifacial options provided from settings", args, allOf(hasEntry("--gcr", "0.4"),
				hasEntry("--bifacial", "true"), hasEntry("--bifaciality", "0.7"),
				hasEntry("--albedo", "0.3"), hasEntry("--height", "2.1"), hasEntry("--pitch", "6.5"),
				hasEntry("--shade-factor", "-0.05"), hasEntry("--transmission-factor", "-0.01")));
	}

	@Test
	public void bifacialSettings_rangeBoundaries() {
		// GIVEN
		service.setBifacial(true);
		service.setBifaciality(BigDecimal.ONE);
		service.setAlbedo(BigDecimal.ZERO);
		service.setRowHeight(new BigDecimal("100"));
		service.setRowPitch(new BigDecimal("1000"));
		service.setShadeFactor(new BigDecimal("-1"));
		service.setTransmissionFactor(BigDecimal.ZERO);

		// WHEN
		replayAll();
		Map<String, String> args = filterAndCaptureArguments();

		// THEN
		assertThat("Bifacial options at range boundaries provided", args,
				allOf(hasEntry("--bifaciality", "1"), hasEntry("--albedo", "0"),
						hasEntry("--height", "100"), hasEntry("--pitch", "1000"),
						hasEntry("--shade-factor", "-1"), hasEntry("--transmission-factor", "0")));
	}

	@Test
	public void bifacialSettings_outOfRange() {
		// GIVEN
		service.setBifacial(true);
		service.setBifaciality(new BigDecimal("1.5"));
		service.setAlbedo(new BigDecimal("-0.1"));
		service.setRowHeight(BigDecimal.ZERO);
		service.setRowPitch(new BigDecimal("1000.1"));
		service.setShadeFactor(new BigDecimal("0.1"));
		service.setTransmissionFactor(new BigDecimal("-1.5"));

		// WHEN
		replayAll();
		Map<String, String> args = filterAndCaptureArguments();

		// THEN
		assertThat("Bifacial mode provided", args, hasEntry("--bifacial", "true"));
		for ( String opt : BIFACIAL_OPTIONS ) {
			if ( "--bifacial".equals(opt) ) {
				continue;
			}
			assertThat("Out of range bifacial setting not provided", args, not(hasKey(opt)));
		}
	}

	@Test
	public void datumMetadata_overridesSettings() {
		// GIVEN
		service.setMetadataPath(META_PATH);
		service.setBifaciality(new BigDecimal("0.8"));
		service.setRowHeight(new BigDecimal("1.5"));
		service.setRowPitch(new BigDecimal("5"));

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("pv-characteristics", "bifacial", true);
		meta.putInfoValue("pv-characteristics", "bifaciality", 0.75);
		meta.putInfoValue("pv-characteristics", "albedo", 0.3);
		meta.putInfoValue("pv-characteristics", "pvRowHeight", 2.5);
		meta.putInfoValue("pv-characteristics", "pvRowPitch", 7);
		meta.putInfoValue("pv-characteristics", "shadeFactor", -0.03);
		meta.putInfoValue("pv-characteristics", "transmissionFactor", "-0.01");
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(meta);

		// WHEN
		replayAll();
		Map<String, String> args = filterAndCaptureArguments();

		// THEN
		assertThat("Bifacial options provided from datum metadata", args,
				allOf(hasEntry("--bifacial", "true"), hasEntry("--bifaciality", "0.75"),
						hasEntry("--albedo", "0.3"), hasEntry("--height", "2.5"),
						hasEntry("--pitch", "7"), hasEntry("--shade-factor", "-0.03"),
						hasEntry("--transmission-factor", "-0.01")));
	}

	@Test
	public void datumMetadata_overridesCharacteristicsMetadata() {
		// GIVEN
		service.setMetadataPath(META_PATH);
		service.setCharacteristicsMetadataServiceUid("pv");

		GeneralDatumMetadata pvMeta = new GeneralDatumMetadata();
		pvMeta.putInfoValue("pv-characteristics", "bifacial", "true");
		pvMeta.putInfoValue("pv-characteristics", "pvRowHeight", 2);
		pvMeta.putInfoValue("pv-characteristics", "pvRowPitch", 6);
		expect(metadataService.getAllMetadata()).andReturn(pvMeta);

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("pv-characteristics", "pvRowHeight", 3);
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(meta);

		// WHEN
		replayAll();
		Map<String, String> args = filterAndCaptureArguments();

		// THEN
		assertThat("Bifacial options merged from metadata sources", args, allOf(
				hasEntry("--bifacial", "true"), hasEntry("--height", "3"), hasEntry("--pitch", "6")));
	}

	@Test
	public void datumMetadata_bifacialOff_overridesSetting() {
		// GIVEN
		service.setMetadataPath(META_PATH);
		service.setBifacial(true);

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("pv-characteristics", "bifacial", false);
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(meta);

		// WHEN
		replayAll();
		Map<String, String> args = filterAndCaptureArguments();

		// THEN
		assertThat("Bifacial mode disabled from datum metadata", args, hasEntry("--bifacial", "false"));
	}

	@Test
	public void datumMetadata_invalid_keepsSettings() {
		// GIVEN
		service.setMetadataPath(META_PATH);
		service.setBifacial(true);
		service.setBifaciality(new BigDecimal("0.7"));
		service.setAlbedo(new BigDecimal("0.2"));
		service.setRowHeight(new BigDecimal("2"));
		service.setRowPitch(new BigDecimal("6"));
		service.setShadeFactor(new BigDecimal("-0.05"));
		service.setTransmissionFactor(new BigDecimal("-0.02"));

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("pv-characteristics", "bifacial", "yes");
		meta.putInfoValue("pv-characteristics", "bifaciality", 1.5);
		meta.putInfoValue("pv-characteristics", "albedo", "not a number");
		meta.putInfoValue("pv-characteristics", "pvRowHeight", 0);
		meta.putInfoValue("pv-characteristics", "pvRowPitch", -5);
		meta.putInfoValue("pv-characteristics", "shadeFactor", 0.1);
		meta.putInfoValue("pv-characteristics", "transmissionFactor", -1.5);
		expect(datumMetadataService.getSourceMetadata(SOURCE_ID)).andReturn(meta);

		// WHEN
		replayAll();
		Map<String, String> args = filterAndCaptureArguments();

		// THEN
		assertThat("Invalid metadata values ignored, keeping setting values", args,
				allOf(hasEntry("--bifacial", "true"), hasEntry("--bifaciality", "0.7"),
						hasEntry("--albedo", "0.2"), hasEntry("--height", "2"), hasEntry("--pitch", "6"),
						hasEntry("--shade-factor", "-0.05"),
						hasEntry("--transmission-factor", "-0.02")));
	}

	@Test
	public void poaGlobalResult() {
		// GIVEN
		service.setBifacial(true);

		Map<String, Object> cmdResult = new LinkedHashMap<>(4);
		cmdResult.put("poa_global", 1012.5);
		cmdResult.put("poa_front", 934.1);
		cmdResult.put("poa_back", 98.0);
		service.result = cmdResult;

		// WHEN
		replayAll();
		SimpleDatum d = createTestDatum();
		DatumSamplesOperations result = service.filter(d, d.getSamples(), emptyMap());

		// THEN
		assertThat("Command executed once", service.commands, hasSize(1));
		assertThat("Result changed", result, is(not(sameInstance(d.getSamples()))));
		assertThat("POA global result extracted",
				result.getSampleBigDecimal(DatumSamplesType.Instantaneous,
						PvlibPoaDatumFilterService.DEFAULT_POA_PROPERTY_NAME),
				is(equalTo(new BigDecimal("1012.5"))));
		assertThat("GHI property preserved", result.getSampleInteger(DatumSamplesType.Instantaneous,
				PvlibPoaDatumFilterService.DEFAULT_GHI_PROPERTY_NAME), is(equalTo(800)));
		assertThat("Other result values not added without expressions",
				result.getSampleValue(DatumSamplesType.Instantaneous, "poa_front"), is(nullValue()));
	}

	@Test
	public void poaFrontResult() {
		// GIVEN
		service.setBifacial(true);
		service.setPoaResultKey("poa_front");

		Map<String, Object> cmdResult = new LinkedHashMap<>(4);
		cmdResult.put("poa_global", 1012.5);
		cmdResult.put("poa_front", 934.1);
		service.result = cmdResult;

		// WHEN
		replayAll();
		SimpleDatum d = createTestDatum();
		DatumSamplesOperations result = service.filter(d, d.getSamples(), emptyMap());

		// THEN
		assertThat("POA front result extracted",
				result.getSampleBigDecimal(DatumSamplesType.Instantaneous,
						PvlibPoaDatumFilterService.DEFAULT_POA_PROPERTY_NAME),
				is(equalTo(new BigDecimal("934.1"))));
	}

}
