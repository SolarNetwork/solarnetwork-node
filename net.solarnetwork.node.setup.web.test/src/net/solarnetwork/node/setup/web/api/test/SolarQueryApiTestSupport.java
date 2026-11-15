/* ==================================================================
 * SolarQueryApiTestSupport.java - 13 Nov 2026 4:36:44 pm
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

package net.solarnetwork.node.setup.web.api.test;

import static net.solarnetwork.dao.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.codec.jackson.CborUtils;
import net.solarnetwork.dao.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.datum.AggregateStreamDatum;
import net.solarnetwork.domain.datum.BasicAggregateStreamDatum;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.BasicReadingStreamDatum;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ReadingStreamDatum;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.node.service.FilterableDatumHistorian;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.setup.web.support.DatumHistorianSelector;
import net.solarnetwork.node.setup.web.support.HistorianSelectorFilter;
import net.solarnetwork.node.setup.web.support.ObjectMapperCustomizer;
import net.solarnetwork.node.setup.web.support.WebServiceControllerSupport;
import net.solarnetwork.node.setup.web.support.WebServiceGlobalControllerSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.web.jakarta.support.SimpleCsvHttpMessageConverter;

/**
 * Support for SolarQuery compatible API tests.
 *
 * <p>
 * The datum fixtures are those of the Commons stream processor tests, so the
 * expected outputs are the same.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public abstract class SolarQueryApiTestSupport {

	/** The node ID. */
	protected static final Long NODE_ID = 123L;

	/** The fixture stream time zone. */
	protected static final String TZ = "Pacific/Auckland";

	@Mock
	protected IdentityService identityService;

	@Mock
	protected OptionalService<FilterableDatumHistorian> filteredService;

	@Mock
	protected OptionalService<FilterableDatumHistorian> unfilteredService;

	@Mock
	protected FilterableDatumHistorian filtered;

	@Mock
	protected FilterableDatumHistorian unfiltered;

	/** The historian selector, using the historian service mocks. */
	protected DatumHistorianSelector historianSelector;

	/** The web object mappers. */
	protected ObjectMapperCustomizer mappers;

	/**
	 * Set up the support objects; call from a {@code @Before} method.
	 */
	protected void setupSupport() {
		historianSelector = new DatumHistorianSelector(filteredService, unfilteredService);
		mappers = new ObjectMapperCustomizer(JsonUtils::newDatumObjectMapper);
	}

	/**
	 * Get the message converters configured like the web application.
	 *
	 * @return the converters
	 */
	protected List<HttpMessageConverter<?>> messageConverters() {
		final MappingJackson2HttpMessageConverter json = new MappingJackson2HttpMessageConverter(
				mappers.getObjectMapper());
		json.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
		return List.of(json, new MappingJackson2CborHttpMessageConverter(mappers.getCborObjectMapper()),
				new SimpleCsvHttpMessageConverter(), new AnyXmlHttpMessageConverter());
	}

	/**
	 * Get the web service exception handling advice.
	 *
	 * @return the advice
	 */
	protected static WebServiceControllerSupport controllerAdvice() {
		final WebServiceControllerSupport advice = new WebServiceControllerSupport();
		advice.setMessageSource(new StaticMessageSource());
		return advice;
	}

	/**
	 * Create a {@link MockMvc} for a controller, set up like the web
	 * application.
	 *
	 * @param controller
	 *        the controller
	 * @return the mock MVC
	 */
	protected MockMvc mockMvc(Object controller) {
		return MockMvcBuilders.standaloneSetup(controller)
				.setMessageConverters(messageConverters().toArray(HttpMessageConverter<?>[]::new))
				.setControllerAdvice(controllerAdvice(), new WebServiceGlobalControllerSupport())
				.addFilters(new HistorianSelectorFilter()).build();
	}

	/**
	 * Decode CBOR into JSON.
	 *
	 * @param cbor
	 *        the CBOR to decode
	 * @return the JSON
	 */
	protected static String cborJson(byte[] cbor) {
		return net.solarnetwork.codec.jackson.JsonUtils
				.getJSONString(CborUtils.CBOR_OBJECT_MAPPER.readTree(cbor));
	}

	/**
	 * Create node stream metadata.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param i
	 *        the instantaneous property names
	 * @param a
	 *        the accumulating property names
	 * @param s
	 *        the status property names
	 * @return the metadata
	 */
	protected static ObjectDatumStreamMetadata nodeMeta(Long nodeId, String sourceId, String[] i,
			String[] a, String[] s) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TZ, ObjectDatumKind.Node, nodeId,
				sourceId, i, a, s);
	}

	/**
	 * Create the fixture node stream metadata.
	 *
	 * @return the metadata
	 */
	protected static ObjectDatumStreamMetadata fixtureMeta() {
		return nodeMeta(NODE_ID, "test/source", new String[] { "a", "b" }, new String[] { "c" },
				new String[] { "d" });
	}

	/**
	 * Create the fixture datum of a stream.
	 *
	 * @param meta
	 *        the stream metadata
	 * @return the datum
	 */
	protected static List<StreamDatum> fixtureDatum(ObjectDatumStreamMetadata meta) {
		final Instant start = LocalDateTime.of(2022, 4, 29, 13, 52).atZone(ZoneId.of(TZ)).toInstant();

		final DatumProperties p1 = new DatumProperties();
		p1.setInstantaneous(decimalArray("1.23", "2.34"));
		p1.setAccumulating(decimalArray("3.45"));
		p1.setStatus(new String[] { "foo" });
		p1.setTags(new String[] { "a" });

		final DatumProperties p2 = new DatumProperties();
		p2.setInstantaneous(decimalArray("3.21", "4.32"));
		p2.setAccumulating(decimalArray("5.43"));
		p2.setStatus(new String[] { "bar" });

		return List.of(new BasicStreamDatum(meta.getStreamId(), start, p1),
				new BasicStreamDatum(meta.getStreamId(), start.plusSeconds(1), p2));
	}

	/**
	 * Get the expected JSON stream result of the fixture datum.
	 *
	 * @param meta
	 *        the fixture stream metadata
	 * @return the JSON
	 */
	protected static String fixtureStreamJson(ObjectDatumStreamMetadata meta) {
		return """
				{"success":true,"meta":[\
				{"streamId":"%s","zone":"Pacific/Auckland","kind":"n","objectId":123,\
				"sourceId":"test/source","i":["a","b"],"a":["c"],"s":["d"]}\
				],"data":[\
				[0,1651197120000,1.23,2.34,3.45,"foo","a"],\
				[0,1651197121000,3.21,4.32,5.43,"bar"]\
				]}""".formatted(meta.getStreamId());
	}

	/**
	 * Get the expected CSV stream result of the fixture datum.
	 *
	 * @param meta
	 *        the fixture stream metadata
	 * @return the CSV
	 */
	protected static String fixtureStreamCsv(ObjectDatumStreamMetadata meta) {
		return """
				ts,streamId,objectId,sourceId,a,b,c,d,tags\r
				2022-04-29T01:52:00Z,%1$s,123,test/source,1.23,2.34,3.45,foo,a\r
				2022-04-29T01:52:01Z,%1$s,123,test/source,3.21,4.32,5.43,bar,\r
				""".formatted(meta.getStreamId());
	}

	/**
	 * Create the fixture aggregate datum of a stream.
	 *
	 * <p>
	 * This is the datum of the Commons stream processor
	 * {@code oneStream_aggregate} tests.
	 * </p>
	 *
	 * @param meta
	 *        the stream metadata
	 * @param ts
	 *        the aggregate timestamp
	 * @return the datum
	 */
	protected static AggregateStreamDatum fixtureAggregateDatum(ObjectDatumStreamMetadata meta,
			Instant ts) {
		// @formatter:off
		return new BasicAggregateStreamDatum(meta.getStreamId(), ts,
				propertiesOf(
						decimalArray("1.23", "2.34"),
						decimalArray("3"),
						new String[] { "foo" },
						new String[] { "wham", "bam" }),
				null,
				statisticsOf(new BigDecimal[][] {
						decimalArray("10", "1.0", "2.0"),
						decimalArray("10", "2.0", "3.0")
					}, new BigDecimal[][] {
						decimalArray("30", "100", "130")
					}));
		// @formatter:on
	}

	/**
	 * Create the fixture reading datum of a stream.
	 *
	 * <p>
	 * This is the datum of the Commons stream processor
	 * {@code oneStream_reading} tests.
	 * </p>
	 *
	 * @param meta
	 *        the stream metadata
	 * @param ts
	 *        the reading timestamp
	 * @param endTs
	 *        the reading end timestamp
	 * @return the datum
	 */
	protected static ReadingStreamDatum fixtureReadingDatum(ObjectDatumStreamMetadata meta, Instant ts,
			Instant endTs) {
		// @formatter:off
		return new BasicReadingStreamDatum(meta.getStreamId(), ts,
				propertiesOf(
						decimalArray("1.23", "2.34"),
						decimalArray("3"), null, null),
				endTs,
				statisticsOf(new BigDecimal[][] {
						decimalArray("10", "1.0", "2.0"),
						decimalArray("10", "2.0", "3.0")
					}, new BigDecimal[][] {
						decimalArray("30", "100", "130")
					}));
		// @formatter:on
	}

	/**
	 * Get the timestamp of the JSON and CBOR aggregate and reading fixtures.
	 *
	 * @return the timestamp
	 */
	protected static Instant fixtureAggregateJsonTimestamp() {
		return LocalDateTime.of(2022, 4, 29, 13, 51).atZone(ZoneId.of(TZ)).toInstant();
	}

	/**
	 * Get the timestamp of the CSV aggregate and reading fixtures.
	 *
	 * @return the timestamp
	 */
	protected static Instant fixtureAggregateCsvTimestamp() {
		return LocalDateTime.of(2022, 4, 29, 13, 0).atZone(ZoneId.of(TZ)).toInstant();
	}

	/**
	 * Get the expected JSON stream result of the fixture aggregate datum.
	 *
	 * <p>
	 * This is the expected output of the Commons stream processor
	 * {@code oneStream_aggregate} test, for JSON and CBOR.
	 * </p>
	 *
	 * @param meta
	 *        the fixture stream metadata
	 * @return the JSON
	 */
	protected static String fixtureAggregateStreamJson(ObjectDatumStreamMetadata meta) {
		return """
				{"success":true,"meta":[\
				{"streamId":"%s","zone":"Pacific/Auckland","kind":"n","objectId":123,\
				"sourceId":"test/source","i":["a","b"],"a":["c"],"s":["d"]}\
				],"data":[\
				[0,[1651197060000,null],[1.23,10,1.0,2.0],[2.34,10,2.0,3.0],[3,100,130],\
				"foo","wham","bam"]\
				]}""".formatted(meta.getStreamId());
	}

	/**
	 * Get the expected CSV stream result of the fixture aggregate datum.
	 *
	 * <p>
	 * This is the expected output of the Commons CSV stream processor
	 * {@code oneStream_aggregate} test.
	 * </p>
	 *
	 * @param meta
	 *        the fixture stream metadata
	 * @return the CSV
	 */
	protected static String fixtureAggregateStreamCsv(ObjectDatumStreamMetadata meta) {
		return """
				ts_start,ts_end,streamId,objectId,sourceId,a,a_count,a_min,a_max,b,b_count,b_min,b_max,\
				c,c_start,c_end,d,tags\r
				2022-04-29T01:00:00Z,,%1$s,123,test/source,1.23,10,1.0,2.0,2.34,10,2.0,3.0,\
				3,100,130,foo,"wham,bam"\r
				""".formatted(meta.getStreamId());
	}

	/**
	 * Get the expected JSON stream result of the fixture reading datum.
	 *
	 * <p>
	 * This is the expected output of the Commons stream processor
	 * {@code oneStream_reading} test, for JSON and CBOR.
	 * </p>
	 *
	 * @param meta
	 *        the fixture stream metadata
	 * @return the JSON
	 */
	protected static String fixtureReadingStreamJson(ObjectDatumStreamMetadata meta) {
		return """
				{"success":true,"meta":[\
				{"streamId":"%s","zone":"Pacific/Auckland","kind":"n","objectId":123,\
				"sourceId":"test/source","i":["a","b"],"a":["c"],"s":["d"]}\
				],"data":[\
				[0,[1651197060000,1651200660000],[1.23,10,1.0,2.0],[2.34,10,2.0,3.0],[30,100,130],\
				null]\
				]}""".formatted(meta.getStreamId());
	}

	/**
	 * Get the expected CSV stream result of the fixture reading datum.
	 *
	 * <p>
	 * This is the expected output of the Commons CSV stream processor
	 * {@code oneStream_reading} test.
	 * </p>
	 *
	 * @param meta
	 *        the fixture stream metadata
	 * @return the CSV
	 */
	protected static String fixtureReadingStreamCsv(ObjectDatumStreamMetadata meta) {
		return """
				ts_start,ts_end,streamId,objectId,sourceId,a,a_count,a_min,a_max,b,b_count,b_min,b_max,\
				c,c_start,c_end,d,tags\r
				2022-04-29T01:00:00Z,2022-04-29T02:00:00Z,%1$s,123,test/source,1.23,10,1.0,2.0,\
				2.34,10,2.0,3.0,30,100,130,,\r
				""".formatted(meta.getStreamId());
	}

	/**
	 * Create an answer for
	 * {@link FilterableDatumHistorian#findFilteredStream(net.solarnetwork.node.dao.NodeDatumFilter, StreamDatumFilteredResultsProcessor, List, Long, Integer)}
	 * or
	 * {@link FilterableDatumHistorian#findFilteredStreamReadings(net.solarnetwork.node.dao.NodeDatumFilter, StreamDatumFilteredResultsProcessor, List, Long, Integer)}
	 * that gives datum to the processor.
	 *
	 * @param metas
	 *        the stream metadata
	 * @param datum
	 *        the datum
	 * @return the answer
	 */
	protected static Answer<Void> streamAnswer(List<ObjectDatumStreamMetadata> metas,
			List<StreamDatum> datum) {
		return invocation -> {
			final StreamDatumFilteredResultsProcessor processor = invocation.getArgument(1);
			processor.start(null, null, null, Map.of(METADATA_PROVIDER_ATTR, staticProvider(metas)));
			for ( StreamDatum d : datum ) {
				processor.handleResultItem(d);
			}
			return null;
		};
	}

	/**
	 * Create a decimal array.
	 *
	 * @param nums
	 *        the decimal strings
	 * @return the decimals
	 */
	protected static BigDecimal[] decimalArray(String... nums) {
		return Arrays.stream(nums).map(BigDecimal::new).toArray(BigDecimal[]::new);
	}

	/**
	 * A converter that writes anything as XML, to show that XML is excluded by
	 * the {@code produces} lists rather than by the lack of a converter.
	 */
	private static final class AnyXmlHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

		private AnyXmlHttpMessageConverter() {
			super(MediaType.APPLICATION_XML);
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void writeInternal(Object t, HttpOutputMessage outputMessage) throws IOException {
			outputMessage.getBody().write("<xml/>".getBytes(StandardCharsets.UTF_8));
		}

	}

}
