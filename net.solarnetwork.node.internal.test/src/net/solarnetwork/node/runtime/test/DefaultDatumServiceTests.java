/* ==================================================================
 * DefaultDatumServiceTests.java - 18/08/2021 10:48:38 AM
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

package net.solarnetwork.node.runtime.test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_SERVICE;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_SERVICE_ARGUMENT;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_SERVICE_RESULT;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SYSTEM_CONFIGURE;
import static net.solarnetwork.node.runtime.DefaultDatumService.SETUP_SERVICE_LATEST_DATUM;
import static net.solarnetwork.node.service.DatumEvents.datumCapturedEvent;
import static net.solarnetwork.node.service.DatumEvents.datumEvent;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.common.osgi.service.DynamicServiceTracker;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.runtime.DefaultDatumService;
import net.solarnetwork.node.runtime.DefaultDatumService.InMemoryHistorian;
import net.solarnetwork.node.service.AppendableDatumHistorian;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.DatumQueueProcessObserver.Stage;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DefaultDatumService} class.
 *
 * @author matt
 * @version 1.4
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class DefaultDatumServiceTests {

	/** The historian service class name, as configured in Blueprint. */
	private static final String HISTORIAN_CLASS_NAME = AppendableDatumHistorian.class.getName();

	/** The filtered historian service filter, as configured in Blueprint. */
	private static final String HISTORIAN_FILTER = "(!(role=unfiltered))";

	/** The unfiltered historian service filter, as configured in Blueprint. */
	private static final String UNFILTERED_HISTORIAN_FILTER = "(role=unfiltered)";

	@Mock
	private DatumMetadataService datumMetadataService;

	@Mock
	private AppendableDatumHistorian historian;

	@Mock
	private AppendableDatumHistorian unfilteredHistorian;

	@Mock
	private BundleContext bundleContext;

	@Mock
	private ServiceReference<AppendableDatumHistorian> historianRef;

	@Mock
	private ServiceReference<AppendableDatumHistorian> unfilteredHistorianRef;

	private AntPathMatcher pathMatcher;
	private InMemoryHistorian fallbackHistorian;
	private InMemoryHistorian fallbackUnfilteredHistorian;
	private DefaultDatumService service;

	private MultiValueMap<String, NodeDatum> population;

	@Before
	public void setup() {
		pathMatcher = new AntPathMatcher();
		service = new DefaultDatumService(pathMatcher, new ObjectMapper(),
				new StaticOptionalService<>(datumMetadataService));
	}

	private DefaultDatumService newService(OptionalService<AppendableDatumHistorian> history,
			OptionalService<AppendableDatumHistorian> unfiltered) {
		return new DefaultDatumService(pathMatcher, new ObjectMapper(),
				new StaticOptionalService<>(datumMetadataService), history, unfiltered);
	}

	/**
	 * Configure the service with mock historians, without any OSGi service
	 * tracking.
	 */
	private void useStaticHistorians() {
		service = newService(new StaticOptionalService<>(historian),
				new StaticOptionalService<>(unfilteredHistorian));
	}

	private DynamicServiceTracker<AppendableDatumHistorian> historianTracker(String filter,
			AppendableDatumHistorian fallback) {
		DynamicServiceTracker<AppendableDatumHistorian> tracker = new DynamicServiceTracker<>(
				bundleContext);
		tracker.setServiceClassName(HISTORIAN_CLASS_NAME);
		tracker.setServiceFilter(filter);
		tracker.setFallbackService(fallback);
		return tracker;
	}

	/**
	 * Configure the service the same way the Blueprint configuration does:
	 * dynamic historian service trackers with in-memory fallback historians.
	 */
	private void useDynamicHistorians() {
		fallbackHistorian = new InMemoryHistorian(pathMatcher);
		fallbackUnfilteredHistorian = new InMemoryHistorian(pathMatcher);
		service = newService(historianTracker(HISTORIAN_FILTER, fallbackHistorian),
				historianTracker(UNFILTERED_HISTORIAN_FILTER, fallbackUnfilteredHistorian));
	}

	private void givenHistorianRegistered() throws Exception {
		given(bundleContext.getServiceReferences(HISTORIAN_CLASS_NAME, HISTORIAN_FILTER))
				.willReturn(new ServiceReference<?>[] { historianRef });
		given(bundleContext.getService(historianRef)).willReturn(historian);
	}

	private void givenHistorianUnregistered() throws Exception {
		given(bundleContext.getServiceReferences(HISTORIAN_CLASS_NAME, HISTORIAN_FILTER))
				.willReturn(null);
	}

	private void givenUnfilteredHistorianRegistered() throws Exception {
		given(bundleContext.getServiceReferences(HISTORIAN_CLASS_NAME, UNFILTERED_HISTORIAN_FILTER))
				.willReturn(new ServiceReference<?>[] { unfilteredHistorianRef });
		given(bundleContext.getService(unfilteredHistorianRef)).willReturn(unfilteredHistorian);
	}

	private static SimpleDatum datum(String sourceId, Instant timestamp) {
		return SimpleDatum.nodeDatum(sourceId, timestamp, new DatumSamples());
	}

	private Map<String, NodeDatum> populateDatum() {
		return populateDatum(Stage.PostFilter);
	}

	private Map<String, NodeDatum> populateDatum(final Stage stage) {
		return populateDatum(5, stage);
	}

	private Map<String, NodeDatum> populateDatum(final int count) {
		return populateDatum(count, Stage.PostFilter);
	}

	private Map<String, NodeDatum> populateDatum(final int count, final Stage stage) {
		return populateDatum(count, "test.%d", stage);
	}

	private Map<String, NodeDatum> populateDatum(final int count, final String sourceIdTemplate) {
		return populateDatum(count, sourceIdTemplate, Stage.PostFilter);
	}

	private Map<String, NodeDatum> populateDatum(final int count, final String sourceIdTemplate,
			final Stage stage) {
		population = new LinkedMultiValueMap<>();
		Map<String, NodeDatum> all = new LinkedHashMap<>();
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			for ( int j = 0; j < 11; j++ ) {
				SimpleDatum datum = datum(String.format(sourceIdTemplate, j), start.plusMillis(j));
				all.put(datum.getSourceId(), datum);
				population.add(datum.getSourceId(), datum);

				service.datumQueueWillProcess(null, datum, stage, true);
			}
		}
		return all;
	}

	@Test
	public void latest_all() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		Collection<NodeDatum> result = service.latest(emptySet(), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum for all sources returned")
			.containsExactlyInAnyOrderElementsOf(all.values())
			;
		// @formatter:on
	}

	@Test
	public void latest_all_unfiltered() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(Stage.PreFilter);

		// WHEN
		Collection<NodeDatum> latest = service.latest(emptySet(), NodeDatum.class);
		Collection<NodeDatum> latestUnfiltered = service.unfiltered().latest(emptySet(),
				NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(latest)
			.as("Pre-filter datum not added to filtered history")
			.isEmpty()
			;
		and.then(latestUnfiltered)
			.as("Latest unfiltered datum for all sources returned")
			.containsExactlyInAnyOrderElementsOf(all.values())
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_oneSource() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		final String sourceId = "test.2";
		Collection<NodeDatum> result = service.latest(singleton(sourceId), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for filtered source ID")
			.containsExactly(all.get(sourceId))
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_multiSource() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		Set<String> sourceIds = Set.of("test.1", "test.3", "test.5");
		Collection<NodeDatum> result = service.latest(sourceIds, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for filtered source IDs")
			.containsExactlyInAnyOrder(all.get("test.1"), all.get("test.3"), all.get("test.5"))
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_pattern() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		Collection<NodeDatum> result = service.latest(singleton("test.1*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for source IDs matching pattern")
			.containsExactlyInAnyOrder(all.get("test.1"), all.get("test.10"))
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_pattern_2() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(5, "foo/bar/charger/%d/111111111");

		// add another that does not match pattern
		SimpleDatum outlier = datum("foo/bar/charger/1/111111111/MAX", Instant.now());
		service.datumQueueWillProcess(null, outlier, Stage.PostFilter, true);

		// WHEN
		Collection<NodeDatum> result = service.latest(singleton("**/charger/*/*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for source IDs matching pattern")
			.containsExactlyInAnyOrderElementsOf(all.values())
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_pattern_3() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(5, "/foo/bar/charger/%d");

		// add another that does not match pattern
		SimpleDatum outlier = datum("/foo/bar/other/1", Instant.now());
		service.datumQueueWillProcess(null, outlier, Stage.PostFilter, true);

		// WHEN
		Collection<NodeDatum> result = service.latest(singleton("/**/charger/*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for source IDs matching pattern")
			.containsExactlyInAnyOrderElementsOf(all.values())
			;
		// @formatter:on
	}

	@Test
	public void offset_all() {
		// GIVEN
		final int count = 8;
		populateDatum(count);

		// WHEN
		Collection<NodeDatum> result = service.offset(emptySet(), 1, NodeDatum.class);

		// THEN
		List<NodeDatum> expected = population.values().stream().map(l -> l.get(count - 2)).toList();
		// @formatter:off
		and.then(result)
			.as("Offset datum for all sources returned")
			.containsExactlyInAnyOrderElementsOf(expected)
			;
		// @formatter:on
	}

	@Test
	public void offset_notAvailable() {
		// GIVEN
		final int count = 8;
		populateDatum(count);

		// WHEN
		Collection<NodeDatum> result = service.offset(emptySet(), 8, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No offset datum available so far back")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void slice() {
		// GIVEN
		populateDatum();
		final String sourceId = "test.0";
		final List<NodeDatum> stream = population.get(sourceId);

		// WHEN
		Collection<NodeDatum> result = service.slice(sourceId, 0, 3, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Slice of latest 3 datum returned, oldest to newest")
			.containsExactlyElementsOf(stream.subList(2, 5))
			;
		// @formatter:on
	}

	@Test
	public void slice_timeOffset() {
		// GIVEN
		populateDatum();
		final String sourceId = "test.0";
		final List<NodeDatum> stream = population.get(sourceId);

		// WHEN
		Collection<NodeDatum> result = service.slice(sourceId, stream.get(2).getTimestamp(), 0, 3,
				NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Slice of 3 datum up to timestamp returned, oldest to newest")
			.containsExactlyElementsOf(stream.subList(0, 3))
			;
		// @formatter:on
	}

	@Test
	public void setHistoryRawCount() {
		// GIVEN
		service.setHistoryRawCount(2);
		populateDatum();
		final String sourceId = "test.0";
		final List<NodeDatum> stream = population.get(sourceId);

		// WHEN
		Collection<NodeDatum> result = service.slice(sourceId, 0, 5, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Only configured raw count of latest datum retained by in-memory historian")
			.containsExactlyElementsOf(stream.subList(3, 5))
			;
		// @formatter:on
	}

	@Test
	public void metaForSource_missing() {
		// GIVEN
		final String sourceId = "test.source";

		given(datumMetadataService.getSourceMetadata(sourceId)).willReturn(null);

		// WHEN
		DatumMetadataOperations result = service.datumMetadata(sourceId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("null returned when DatumMetadataService returns null")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void metaForSource_found() {
		// GIVEN
		final String sourceId = "test.source";

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		given(datumMetadataService.getSourceMetadata(sourceId)).willReturn(meta);

		// WHEN
		DatumMetadataOperations result = service.datumMetadata(sourceId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Same instance returned as from DatumMetadataService")
			.isSameAs(meta)
			;
		// @formatter:on
	}

	@Test
	public void metaForSourcePattern_noAvailableSourceIds() {
		// GIVEN
		final String sourceIdPat = "test*";
		given(datumMetadataService.availableSourceMetadataSourceIds()).willReturn(emptySet());

		// WHEN
		Collection<DatumMetadataOperations> result = service.datumMetadata(singleton(sourceIdPat));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Empty result returned when no source IDs available")
			.isEmpty()
			;
		// @formatter:on
		then(datumMetadataService).should(never()).getSourceMetadata(any());
	}

	@Test
	public void metaForSourcePattern_noMatchingSourceIds() {
		// GIVEN
		final String sourceIdPat = "test*";

		Set<String> availSourceIds = new HashSet<>(asList("foo", "bar"));
		given(datumMetadataService.availableSourceMetadataSourceIds()).willReturn(availSourceIds);

		// WHEN
		Collection<DatumMetadataOperations> result = service.datumMetadata(singleton(sourceIdPat));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Empty result returned when no source IDs match")
			.isEmpty()
			;
		// @formatter:on
		then(datumMetadataService).should(never()).getSourceMetadata(any());
	}

	@Test
	public void metaForSourcePattern_matchingSourceIds() {
		// GIVEN
		final String sourceIdPat = "**/bam/**";

		Set<String> availSourceIds = new HashSet<>(
				asList("foo/bar/bam/1/11111111", "foo/bar/bam/2/22222222", "bambi", "bar"));
		given(datumMetadataService.availableSourceMetadataSourceIds()).willReturn(availSourceIds);

		GeneralDatumMetadata meta1 = new GeneralDatumMetadata();
		given(datumMetadataService.getSourceMetadata("foo/bar/bam/1/11111111")).willReturn(meta1);
		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		given(datumMetadataService.getSourceMetadata("foo/bar/bam/2/22222222")).willReturn(meta2);

		// WHEN
		Collection<DatumMetadataOperations> result = service.datumMetadata(singleton(sourceIdPat));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Metadata for matching source IDs returned")
			.satisfiesExactlyInAnyOrder(
				m -> and.then(m).as("Source 1 metadata").isSameAs(meta1),
				m -> and.then(m).as("Source 2 metadata").isSameAs(meta2)
			)
			;
		// @formatter:on
	}

	@Test
	public void customHistorian_postFilterDatum() {
		// GIVEN
		useStaticHistorians();

		// WHEN
		final SimpleDatum datum = datum("test.1", Instant.now());
		service.datumQueueWillProcess(null, datum, Stage.PostFilter, true);

		// THEN
		then(historian).should().add(same(datum));
		then(unfilteredHistorian).shouldHaveNoInteractions();
	}

	@Test
	public void customHistorian_preFilterDatum() {
		// GIVEN
		useStaticHistorians();

		// WHEN
		final SimpleDatum datum = datum("test.1", Instant.now());
		service.datumQueueWillProcess(null, datum, Stage.PreFilter, true);

		// THEN
		then(unfilteredHistorian).should().add(same(datum));
		then(historian).shouldHaveNoInteractions();
	}

	@Test
	public void customHistorian_datumCapturedEvent() {
		// GIVEN
		useStaticHistorians();

		// WHEN
		final SimpleDatum datum = datum("test.1", Instant.now());
		service.handleEvent(datumCapturedEvent(datum));

		// THEN
		then(historian).should().add(same(datum));
		then(unfilteredHistorian).shouldHaveNoInteractions();
	}

	@Test
	public void customHistorian_otherEvent() {
		// GIVEN
		useStaticHistorians();

		// WHEN
		final SimpleDatum datum = datum("test.1", Instant.now());
		service.handleEvent(datumEvent("test/Other", datum));

		// THEN
		then(historian).shouldHaveNoInteractions();
		then(unfilteredHistorian).shouldHaveNoInteractions();
	}

	@Test
	public void customHistorian_queries() {
		// GIVEN
		useStaticHistorians();

		final String sourceId = "test.1";
		final Set<String> filter = singleton("test.*");
		final Instant ts = Instant.now();
		final NodeDatum d = datum(sourceId, ts);
		final List<NodeDatum> list = List.of(d);

		given(historian.latest(filter, NodeDatum.class)).willReturn(list);
		given(historian.latest(sourceId, NodeDatum.class)).willReturn(d);
		given(historian.offset(sourceId, 1, NodeDatum.class)).willReturn(d);
		given(historian.offset(filter, 1, NodeDatum.class)).willReturn(list);
		given(historian.offset(filter, ts, 1, NodeDatum.class)).willReturn(list);
		given(historian.offset(sourceId, ts, 1, NodeDatum.class)).willReturn(d);
		given(historian.slice(sourceId, 1, 2, NodeDatum.class)).willReturn(list);
		given(historian.slice(sourceId, ts, 1, 2, NodeDatum.class)).willReturn(list);

		// WHEN
		// @formatter:off
		Collection<NodeDatum> latestFilter          = service.latest(filter, NodeDatum.class);
		NodeDatum             latestSource          = service.latest(sourceId, NodeDatum.class);
		NodeDatum             offsetSource          = service.offset(sourceId, 1, NodeDatum.class);
		Collection<NodeDatum> offsetFilter          = service.offset(filter, 1, NodeDatum.class);
		Collection<NodeDatum> offsetFilterTimestamp = service.offset(filter, ts, 1, NodeDatum.class);
		NodeDatum             offsetSourceTimestamp = service.offset(sourceId, ts, 1, NodeDatum.class);
		Collection<NodeDatum> slice                 = service.slice(sourceId, 1, 2, NodeDatum.class);
		Collection<NodeDatum> sliceTimestamp        = service.slice(sourceId, ts, 1, 2, NodeDatum.class);

		// THEN
		and.then(latestFilter)
			.as("latest(filter) result from custom historian")
			.isSameAs(list)
			;
		and.then(latestSource)
			.as("latest(sourceId) result from custom historian")
			.isSameAs(d)
			;
		and.then(offsetSource)
			.as("offset(sourceId, offset) result from custom historian")
			.isSameAs(d)
			;
		and.then(offsetFilter)
			.as("offset(filter, offset) result from custom historian")
			.isSameAs(list)
			;
		and.then(offsetFilterTimestamp)
			.as("offset(filter, timestamp, offset) result from custom historian")
			.isSameAs(list)
			;
		and.then(offsetSourceTimestamp)
			.as("offset(sourceId, timestamp, offset) result from custom historian")
			.isSameAs(d)
			;
		and.then(slice)
			.as("slice(sourceId, offset, count) result from custom historian")
			.isSameAs(list)
			;
		and.then(sliceTimestamp)
			.as("slice(sourceId, timestamp, offset, count) result from custom historian")
			.isSameAs(list)
			;
		// @formatter:on
		then(unfilteredHistorian).shouldHaveNoInteractions();
	}

	@Test
	public void customHistorian_unfiltered() {
		// GIVEN
		useStaticHistorians();

		// WHEN
		AppendableDatumHistorian result = service.unfiltered();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Custom unfiltered historian returned")
			.isSameAs(unfilteredHistorian)
			;
		// @formatter:on
	}

	@Test
	public void customHistorian_latestDatumInstruction() {
		// GIVEN
		useStaticHistorians();

		final NodeDatum d = datum("test.1", Instant.now());
		final List<NodeDatum> list = List.of(d);
		given(historian.latest(Set.of("test.1"), NodeDatum.class)).willReturn(list);

		// WHEN
		InstructionStatus result = service
				.processInstruction(InstructionUtils.createLocalInstruction(TOPIC_SYSTEM_CONFIGURE,
						Map.of(PARAM_SERVICE, SETUP_SERVICE_LATEST_DATUM, PARAM_SERVICE_ARGUMENT,
								"[\"test.1\"]")));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Status returned")
			.isNotNull()
			.as("Instruction completed")
			.returns(InstructionState.Completed, from(InstructionStatus::getInstructionState))
			.extracting(InstructionStatus::getResultParameters, map(String.class, Object.class))
			.as("Latest datum from custom historian returned as result")
			.containsEntry(PARAM_SERVICE_RESULT, list)
			;
		// @formatter:on
	}

	@Test
	public void customHistorian_setHistoryRawCount() {
		// GIVEN
		useStaticHistorians();

		// WHEN
		service.setHistoryRawCount(2);

		// THEN
		then(historian).shouldHaveNoInteractions();
		then(unfilteredHistorian).shouldHaveNoInteractions();
	}

	@Test
	public void dynamicHistorian_noneRegistered() {
		// GIVEN
		useDynamicHistorians();

		// WHEN
		final SimpleDatum d1 = datum("test.1", Instant.now());
		final SimpleDatum d2 = datum("test.2", Instant.now());
		service.datumQueueWillProcess(null, d1, Stage.PreFilter, true);
		service.datumQueueWillProcess(null, d2, Stage.PostFilter, true);

		// THEN
		// @formatter:off
		and.then(fallbackUnfilteredHistorian.latest(emptySet(), NodeDatum.class))
			.as("Pre-filter datum added to fallback unfiltered historian")
			.containsExactly(d1)
			;
		and.then(fallbackHistorian.latest(emptySet(), NodeDatum.class))
			.as("Post-filter datum added to fallback historian")
			.containsExactly(d2)
			;
		and.then(service.latest(emptySet(), NodeDatum.class))
			.as("Latest datum queried from fallback historian")
			.containsExactly(d2)
			;
		and.then(service.unfiltered())
			.as("Fallback unfiltered historian used when no unfiltered historian service registered")
			.isSameAs(fallbackUnfilteredHistorian)
			;
		// @formatter:on
	}

	@Test
	public void dynamicHistorian_registered() throws Exception {
		// GIVEN
		useDynamicHistorians();
		givenHistorianRegistered();

		final SimpleDatum d = datum("test.1", Instant.now());
		final List<NodeDatum> latest = List.of(d);
		given(historian.latest(emptySet(), NodeDatum.class)).willReturn(latest);

		// WHEN
		service.datumQueueWillProcess(null, d, Stage.PostFilter, true);
		Collection<NodeDatum> result = service.latest(emptySet(), NodeDatum.class);

		// THEN
		then(historian).should().add(same(d));
		// @formatter:off
		and.then(result)
			.as("Latest datum queried from registered historian service")
			.isSameAs(latest)
			;
		and.then(fallbackHistorian.latest(emptySet(), NodeDatum.class))
			.as("Fallback historian not used while historian service registered")
			.isEmpty()
			;
		and.then(service.unfiltered())
			.as("Fallback unfiltered historian still used, as registered service lacks unfiltered role")
			.isSameAs(fallbackUnfilteredHistorian)
			;
		// @formatter:on
	}

	@Test
	public void dynamicHistorian_unfilteredRegistered() throws Exception {
		// GIVEN
		useDynamicHistorians();
		givenUnfilteredHistorianRegistered();

		// WHEN
		final SimpleDatum d1 = datum("test.1", Instant.now());
		final SimpleDatum d2 = datum("test.2", Instant.now());
		service.datumQueueWillProcess(null, d1, Stage.PreFilter, true);
		service.datumQueueWillProcess(null, d2, Stage.PostFilter, true);

		// THEN
		then(unfilteredHistorian).should().add(same(d1));
		then(unfilteredHistorian).shouldHaveNoMoreInteractions();
		// @formatter:off
		and.then(service.unfiltered())
			.as("Registered unfiltered historian service returned")
			.isSameAs(unfilteredHistorian)
			;
		and.then(fallbackUnfilteredHistorian.latest(emptySet(), NodeDatum.class))
			.as("Fallback unfiltered historian not used while unfiltered historian service registered")
			.isEmpty()
			;
		and.then(fallbackHistorian.latest(emptySet(), NodeDatum.class))
			.as("Post-filter datum added to fallback historian, as no filtered historian service registered")
			.containsExactly(d2)
			;
		// @formatter:on
	}

	@Test
	public void dynamicHistorian_registeredThenUnregistered() throws Exception {
		// GIVEN
		useDynamicHistorians();

		final String sourceId = "test.1";
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		final SimpleDatum d1 = datum(sourceId, start);
		final SimpleDatum d2 = datum(sourceId, start.plusSeconds(1));
		final SimpleDatum d3 = datum(sourceId, start.plusSeconds(2));

		// WHEN no historian service registered
		service.datumQueueWillProcess(null, d1, Stage.PostFilter, true);

		// THEN
		// @formatter:off
		and.then(service.latest(sourceId, NodeDatum.class))
			.as("Latest datum from fallback historian before historian service registered")
			.isSameAs(d1)
			;
		// @formatter:on

		// WHEN historian service registered
		givenHistorianRegistered();
		given(historian.latest(sourceId, NodeDatum.class)).willReturn(d2);

		service.datumQueueWillProcess(null, d2, Stage.PostFilter, true);

		// THEN
		then(historian).should().add(same(d2));
		// @formatter:off
		and.then(service.latest(sourceId, NodeDatum.class))
			.as("Latest datum from registered historian service")
			.isSameAs(d2)
			;
		and.then(fallbackHistorian.latest(sourceId, NodeDatum.class))
			.as("Fallback historian not given datum while historian service registered")
			.isSameAs(d1)
			;
		// @formatter:on

		// WHEN historian service unregistered
		givenHistorianUnregistered();

		// THEN
		// @formatter:off
		and.then(service.latest(sourceId, NodeDatum.class))
			.as("Fallback historian resumes with datum captured before historian service registered")
			.isSameAs(d1)
			;
		// @formatter:on

		// WHEN
		service.datumQueueWillProcess(null, d3, Stage.PostFilter, true);

		// THEN
		// @formatter:off
		and.then(service.latest(sourceId, NodeDatum.class))
			.as("Latest datum from fallback historian after historian service unregistered")
			.isSameAs(d3)
			;
		and.then(fallbackHistorian.slice(sourceId, 0, 5, NodeDatum.class))
			.as("Fallback historian only has datum captured while no historian service registered")
			.containsExactly(d1, d3)
			;
		// @formatter:on
		then(historian).should().latest(sourceId, NodeDatum.class);
		then(historian).shouldHaveNoMoreInteractions();
	}

	@Test
	public void dynamicHistorian_setHistoryRawCount_fallback() {
		// GIVEN
		useDynamicHistorians();

		// WHEN
		service.setHistoryRawCount(2);
		populateDatum();

		// THEN
		final String sourceId = "test.0";
		final List<NodeDatum> stream = population.get(sourceId);
		// @formatter:off
		and.then(fallbackHistorian.slice(sourceId, 0, 5, NodeDatum.class))
			.as("Raw count applied to fallback historian when no historian service registered")
			.containsExactlyElementsOf(stream.subList(3, 5))
			;
		// @formatter:on
	}

	@Test
	public void dynamicHistorian_noFallback_noneRegistered() {
		// GIVEN
		service = newService(new DynamicServiceTracker<>(bundleContext, HISTORIAN_CLASS_NAME),
				new DynamicServiceTracker<>(bundleContext, HISTORIAN_CLASS_NAME));

		final SimpleDatum d = datum("test.1", Instant.now());

		// WHEN
		// @formatter:off
		and.thenIllegalStateException()
			.as("Adding datum fails when no historian available")
			.isThrownBy(() -> service.datumQueueWillProcess(null, d, Stage.PostFilter, true))
			;
		and.thenIllegalStateException()
			.as("Querying datum fails when no historian available")
			.isThrownBy(() -> service.latest("test.1", NodeDatum.class))
			;
		and.thenIllegalStateException()
			.as("Unfiltered historian access fails when no unfiltered historian available")
			.isThrownBy(() -> service.unfiltered())
			;
		// @formatter:on
	}

}
