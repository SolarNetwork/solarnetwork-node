/* ==================================================================
 * JsonDatumMetadataServiceTests.java - 24/08/2018 10:46:19 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.metadata.json.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import net.solarnetwork.codec.ObjectMapperFactoryBean;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.metadata.json.JsonDatumMetadataService;
import net.solarnetwork.node.metadata.json.JsonDatumMetadataService.CachedMetadata;
import net.solarnetwork.node.metadata.json.NodeMetadataInstructions;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;
import net.solarnetwork.util.CachedResult;

/**
 * Test cases for the {@link JsonDatumMetadataService} class.
 *
 * @author matt
 * @version 2.1
 */
public class JsonDatumMetadataServiceTests extends AbstractHttpServerTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final String TEST_SOUCE_ID = "test.source";

	private IdentityService identityService;
	private SettingsService settingsService;
	private SettingDao settingDao;
	private TaskScheduler taskScheduler;
	private ConcurrentMap<String, CachedMetadata> sourceMetadata;
	private ConcurrentMap<ObjectDatumStreamMetadataId, CachedResult<ObjectDatumStreamMetadata>> datumStreamMetadata;
	private JsonDatumMetadataService service;

	@Before
	public void setupClient() throws Exception {
		identityService = EasyMock.createMock(IdentityService.class);
		settingsService = EasyMock.createMock(SettingsService.class);
		settingDao = EasyMock.createMock(SettingDao.class);
		taskScheduler = EasyMock.createMock(TaskScheduler.class);
		sourceMetadata = new ConcurrentHashMap<>();
		datumStreamMetadata = new ConcurrentHashMap<>();
		service = new JsonDatumMetadataService(settingsService, taskScheduler, sourceMetadata,
				datumStreamMetadata);
		service.setIdentityService(identityService);
		service.setSettingDao(settingDao);
		service.setUpdateThrottleSeconds(0);
		service.setUpdatePersistDelaySeconds(0);

		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setFeaturesToDisable(Arrays.asList(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		factory.setDeserializers(Arrays
				.asList(net.solarnetwork.codec.BasicObjectDatumStreamMetadataDeserializer.INSTANCE));
		service.setObjectMapper(factory.getObject());
	}

	private void replayAll() {
		EasyMock.replay(identityService, settingsService, settingDao, taskScheduler);
	}

	@Override
	@After
	public void teardown() {
		EasyMock.verify(identityService, settingsService, settingDao, taskScheduler);
	}

	@Test
	public void requestMetadata_notCached() throws Exception {
		// GIVEN
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// no cached metadata available
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.emptyList());

		// also fall back to legacy data
		expect(settingDao.getSetting(JsonDatumMetadataService.SETTING_KEY_SOURCE_META, TEST_SOUCE_ID))
				.andReturn(null);

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("Source ID", queryParams.getValue("sourceId"), equalTo(TEST_SOUCE_ID));
				respondWithJsonResource(request, response, "meta-01.json");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = service.getSourceMetadata(TEST_SOUCE_ID);

		// THEN
		assertThat("Metadata returned", meta, notNullValue());
		assertThat("Info metadata", meta.getInfo(), nullValue());
		assertThat("Property metadata", meta.getPropertyInfo(), notNullValue());
		assertThat("Property metadata size", meta.getPropertyInfo().keySet(),
				contains("irradianceHours"));
		assertThat("irradianceHours -> date", meta.getInfoLong("irradianceHours", "vm-date"),
				equalTo(1535065709013L));
		assertThat("irradianceHours -> value", meta.getInfoString("irradianceHours", "vm-value"),
				equalTo("61"));
		assertThat("irradianceHours -> reading", meta.getInfoString("irradianceHours", "vm-reading"),
				equalTo("558.857455"));
	}

	@Test
	public void requestMetadata_cached() throws Exception {
		// GIVEN
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// cached metadata available
		ByteArrayResource jsonResource = new ByteArrayResource(
				"{\"m\":{\"foo\":\"bar\"}}".getBytes(Charset.forName("UTF-8")));
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.singleton(jsonResource));

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = service.getSourceMetadata(TEST_SOUCE_ID);

		// THEN
		assertThat("Metadata returned", meta, notNullValue());
		assertThat("Info metadata", meta.getInfo().keySet(), hasSize(1));
		assertThat("Info metadata", meta.getInfo(), hasEntry("foo", "bar"));
		assertThat("Property metadata", meta.getPropertyInfo(), nullValue());
	}

	@Test
	public void requestMetadata_cached_2ndTime() throws Exception {
		// GIVEN
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// cached metadata available
		ByteArrayResource jsonResource = new ByteArrayResource(
				"{\"m\":{\"foo\":\"bar\"}}".getBytes(Charset.forName("UTF-8")));
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.singleton(jsonResource));

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = service.getSourceMetadata(TEST_SOUCE_ID);

		// can call again, but cached in memory now so no more loading from metadata
		GeneralDatumMetadata meta2 = service.getSourceMetadata(TEST_SOUCE_ID);

		// THEN
		assertThat("Metadata returned", meta, notNullValue());
		assertThat("Info metadata", meta.getInfo().keySet(), hasSize(1));
		assertThat("Info metadata", meta.getInfo(), hasEntry("foo", "bar"));
		assertThat("Property metadata", meta.getPropertyInfo(), nullValue());
		assertThat("Metadata content unchanged", meta2, equalTo(meta));
	}

	@Test
	public void postMetadata_notCached() throws Exception {
		// GIVEN
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// no cached metadata available
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.emptyList());

		// also fall back to legacy data
		expect(settingDao.getSetting(JsonDatumMetadataService.SETTING_KEY_SOURCE_META, TEST_SOUCE_ID))
				.andReturn(null);

		// not found locally, so fetch from SolarNetwork; then post to SolarNetwork
		TestHttpHandler handler = new TestHttpHandler() {

			private int count = 0;

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				count++;
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("Source ID", queryParams.getValue("sourceId"), equalTo(TEST_SOUCE_ID));
				if ( count == 1 ) {
					// initial GET metadata request; none available
					assertThat("Request method", request.getMethod(), equalTo("GET"));
				} else {
					// second POST metadata request
					assertThat("Request method", request.getMethod(), equalTo("POST"));
					String body = getRequestBody(request);
					assertThat("JSON body", body, equalTo("{\"m\":{\"foo\":\"bar\"}}"));

				}
				respondWithJson(request, response, "{\"success\":true}");
				return true;
			}

		};
		addHandler(handler);

		// then persist copy locally as settings resource
		Capture<Iterable<Resource>> resourcesCaptor = Capture.newInstance();
		settingsService.importSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey),
				capture(resourcesCaptor));

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// THEN
		Iterable<Resource> savedResources = resourcesCaptor.getValue();
		List<Resource> rsrcs = StreamSupport.stream(savedResources.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Saved single resource", rsrcs, hasSize(1));
		String json = FileCopyUtils.copyToString(
				new InputStreamReader(rsrcs.get(0).getInputStream(), Charset.forName("UTF-8")));
		assertThat("Cached metadata json", json, equalTo("{\"m\":{\"foo\":\"bar\"}}"));
	}

	@Test
	public void postMetadata_cachedLegacy() throws Exception {
		// GIVEN
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));

		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// no cached metadata available
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.emptyList());

		// legacy cached metadata *is* available (first to look up, second before saving)
		expect(settingDao.getSetting(JsonDatumMetadataService.SETTING_KEY_SOURCE_META, TEST_SOUCE_ID))
				.andReturn("{\"m\":{\"foo\":\"bar\"}}");

		// then store as settings resource
		Capture<Iterable<Resource>> resourcesCaptor = Capture.newInstance();
		settingsService.importSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey),
				capture(resourcesCaptor));

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("POST"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("Source ID", queryParams.getValue("sourceId"), equalTo(TEST_SOUCE_ID));

				String body = getRequestBody(request);
				assertThat("JSON body", body, equalTo("{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\"}}"));

				respondWithJson(request, response, "{\"success\":true}");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("bim", "bam");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// THEN
		Iterable<Resource> savedResources = resourcesCaptor.getValue();
		List<Resource> rsrcs = StreamSupport.stream(savedResources.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Saved single resource", rsrcs, hasSize(1));
		String json = FileCopyUtils.copyToString(
				new InputStreamReader(rsrcs.get(0).getInputStream(), Charset.forName("UTF-8")));
		assertThat("Cached metadata json", json, equalTo("{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\"}}"));
	}

	@Test
	public void postMetadata_cached() throws Exception {
		// GIVEN
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));

		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// cached metadata available
		ByteArrayResource jsonResource = new ByteArrayResource(
				"{\"m\":{\"foo\":\"bar\"}}".getBytes(Charset.forName("UTF-8")));
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.singleton(jsonResource));

		// then store as settings resource
		Capture<Iterable<Resource>> resourcesCaptor = Capture.newInstance();
		settingsService.importSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey),
				capture(resourcesCaptor));

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("POST"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("Source ID", queryParams.getValue("sourceId"), equalTo(TEST_SOUCE_ID));

				String body = getRequestBody(request);
				assertThat("JSON body", body, equalTo("{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\"}}"));

				respondWithJson(request, response, "{\"success\":true}");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("bim", "bam");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// THEN
		Iterable<Resource> savedResources = resourcesCaptor.getValue();
		List<Resource> rsrcs = StreamSupport.stream(savedResources.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Saved single resource", rsrcs, hasSize(1));
		String json = FileCopyUtils.copyToString(
				new InputStreamReader(rsrcs.get(0).getInputStream(), Charset.forName("UTF-8")));
		assertThat("Cached metadata json", json, equalTo("{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\"}}"));
	}

	@Test
	public void postMetadata_cached_2ndTime() throws Exception {
		// GIVEN
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));

		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// cached metadata available
		ByteArrayResource jsonResource = new ByteArrayResource(
				"{\"m\":{\"foo\":\"bar\"}}".getBytes(Charset.forName("UTF-8")));
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.singleton(jsonResource));

		// then store as settings resource
		Capture<Iterable<Resource>> resourcesCaptor = Capture.newInstance(CaptureType.ALL);
		settingsService.importSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey),
				capture(resourcesCaptor));
		expectLastCall().times(2);

		AtomicInteger count = new AtomicInteger(0);
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				final int i = count.incrementAndGet();
				assertThat("Request method", request.getMethod(), equalTo("POST"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("Source ID", queryParams.getValue("sourceId"), equalTo(TEST_SOUCE_ID));

				String body = getRequestBody(request);
				if ( i == 1 ) {
					assertThat("JSON body", body, equalTo("{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\"}}"));
				} else {
					assertThat("JSON body", body,
							equalTo("{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\",\"bee\":\"bop\"}}"));

				}

				respondWithJson(request, response, "{\"success\":true}");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("bim", "bam");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// call again with new value; 2nd time does not re-load from local cache because already in memory
		meta.putInfoValue("bee", "bop");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// THEN
		List<Iterable<Resource>> savedResourcesList = resourcesCaptor.getValues();
		assertThat("Persisted metadata locally twice", savedResourcesList, hasSize(2));
		for ( int i = 0; i < 2; i++ ) {
			List<Resource> rsrcs = StreamSupport.stream(savedResourcesList.get(i).spliterator(), false)
					.collect(Collectors.toList());
			assertThat("Saved single resource", rsrcs, hasSize(1));
			String json = FileCopyUtils.copyToString(
					new InputStreamReader(rsrcs.get(0).getInputStream(), Charset.forName("UTF-8")));
			assertThat("Cached metadata json " + i, json,
					equalTo(i == 0 ? "{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\"}}"
							: "{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\",\"bee\":\"bop\"}}"));
		}
	}

	@Test
	public void postMetadata_cached_2ndTime_coalesced() throws Exception {
		// GIVEN
		service.setUpdateThrottleSeconds(2);
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));

		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// cached metadata available
		ByteArrayResource jsonResource = new ByteArrayResource(
				"{\"m\":{\"foo\":\"bar\"}}".getBytes(Charset.forName("UTF-8")));
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.singleton(jsonResource));

		// then store as settings resource
		Capture<Iterable<Resource>> resourcesCaptor = Capture.newInstance(CaptureType.ALL);
		settingsService.importSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey),
				capture(resourcesCaptor));
		expectLastCall().times(2); // persisted locally each time

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("POST"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("Source ID", queryParams.getValue("sourceId"), equalTo(TEST_SOUCE_ID));

				String body = getRequestBody(request);
				assertThat("JSON body", body, equalTo("{\"m\":{\"foo\":\"bar\",\"bim\":\"bop\"}}"));

				respondWithJson(request, response, "{\"success\":true}");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("bim", "bam");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// because we have coalescing enabled, the next call won't actually persist anywhere
		meta.putInfoValue("bim", "bop");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// sleep for longer than coalesce time
		Thread.sleep(3);

		// manually call persist task
		service.run();

		// THEN
		List<Iterable<Resource>> savedResourcesList = resourcesCaptor.getValues();
		assertThat("Two sets of saved resources", savedResourcesList, hasSize(2));
		for ( int i = 0; i < 2; i++ ) {
			Iterable<Resource> savedResources = savedResourcesList.get(i);
			List<Resource> rsrcs = StreamSupport.stream(savedResources.spliterator(), false)
					.collect(Collectors.toList());
			assertThat("Saved single resource", rsrcs, hasSize(1));
			String json = FileCopyUtils.copyToString(
					new InputStreamReader(rsrcs.get(0).getInputStream(), Charset.forName("UTF-8")));
			String expectedJson;
			if ( i == 0 ) {
				expectedJson = "{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\"}}";
			} else {
				expectedJson = "{\"m\":{\"foo\":\"bar\",\"bim\":\"bop\"}}";
			}
			assertThat("Cached metadata json " + i, json, equalTo(expectedJson));
		}
	}

	@Test
	public void postMetadata_cached_noChange() throws Exception {
		// GIVEN
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));

		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// cached metadata available
		ByteArrayResource jsonResource = new ByteArrayResource(
				"{\"m\":{\"foo\":\"bar\"}}".getBytes(Charset.forName("UTF-8")));
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.singleton(jsonResource));

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// THEN
	}

	private static class TestScheduledFuture extends CompletableFuture<Object>
			implements ScheduledFuture<Object> {

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed o) {
			return 0;
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void addMetadata_cached_coalesced_delayPersist() throws Exception {
		// GIVEN
		service.setUpdatePersistDelaySeconds(60);
		service.setUpdateThrottleSeconds(2);
		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(Charset.forName("UTF-8")));

		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// cached metadata available
		ByteArrayResource jsonResource = new ByteArrayResource(
				"{\"m\":{\"foo\":\"bar\"}}".getBytes(Charset.forName("UTF-8")));
		expect(settingsService.getSettingResources(service.getSettingUid(), null, settingKey))
				.andReturn(Collections.singleton(jsonResource));

		// create delayed persist task
		Capture<Runnable> persistRunCaptor = Capture.newInstance(CaptureType.ALL);
		Capture<Instant> persistDateCaptor = Capture.newInstance(CaptureType.ALL);
		TestScheduledFuture f1 = new TestScheduledFuture();
		TestScheduledFuture f2 = new TestScheduledFuture();
		expect(taskScheduler.schedule(capture(persistRunCaptor), capture(persistDateCaptor)))
				.andReturn((ScheduledFuture) f1).andAnswer(new IAnswer<ScheduledFuture<?>>() {

					@Override
					public ScheduledFuture<?> answer() throws Throwable {
						persistRunCaptor.getValues().get(1).run();
						f2.complete(Boolean.TRUE);
						return f2;
					}
				});

		// then store as settings resource
		Capture<Iterable<Resource>> resourcesCaptor = Capture.newInstance();
		settingsService.importSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey),
				capture(resourcesCaptor));

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("bim", "bam");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// call again with new value; 2nd time does not re-load from local cache because already in memory
		meta.putInfoValue("bee", "bop");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// THEN
		Iterable<Resource> savedResources = resourcesCaptor.getValue();
		List<Resource> rsrcs = StreamSupport.stream(savedResources.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Saved single resource", rsrcs, hasSize(1));
		String json = FileCopyUtils.copyToString(
				new InputStreamReader(rsrcs.get(0).getInputStream(), Charset.forName("UTF-8")));
		assertThat("Cached metadata json", json,
				is(equalTo("{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\",\"bee\":\"bop\"}}")));

		assertThat("Persist future 1 cancelled from 2nd update", f1.isCancelled(), is(equalTo(true)));
		assertThat("Persist future 2 completed after 2nd update timeout",
				f2.isDone() && !f2.isCompletedExceptionally() && !f2.isCancelled(), is(equalTo(true)));
	}

	@Test
	public void requestNodeStreamMetadata_notCached() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID + "/stream"));

				Fields queryParams = Request.extractQueryParameters(request);

				assertThat("Source ID", queryParams.getValue("sourceId"), equalTo(TEST_SOUCE_ID));
				assertThat("Kind", queryParams.getValue("kind"), equalTo(ObjectDatumKind.Node.name()));
				respondWithJsonResource(request, response, "node-stream-meta-01.json");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		ObjectDatumStreamMetadata meta = service.getDatumStreamMetadata(ObjectDatumKind.Node, null,
				TEST_SOUCE_ID);

		// THEN
		assertThat("Metadata returned", meta, notNullValue());
		assertThat("Metadata stream ID", meta.getStreamId(),
				is(equalTo(UUID.fromString("a66e3344-3791-4113-afff-22b44eb3c833"))));
		assertThat("Metadata object kind", meta.getKind(), is(equalTo(ObjectDatumKind.Node)));
		assertThat("Metadata object ID", meta.getObjectId(), is(equalTo(TEST_NODE_ID)));
		assertThat("Metadata source ID", meta.getSourceId(), is(equalTo(TEST_SOUCE_ID)));
	}

	@Test
	public void requestNodeStreamMetadata_cached() throws Exception {
		// GIVEN

		// put metadata into cache from start
		BasicObjectDatumStreamMetadata m = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"Pacific/Auckland", ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOUCE_ID, null,
				new String[] { "foo" }, null, null, null);
		CachedResult<ObjectDatumStreamMetadata> cachedMeta = new CachedResult<ObjectDatumStreamMetadata>(
				m, 1, TimeUnit.HOURS);
		datumStreamMetadata.put(
				new ObjectDatumStreamMetadataId(m.getKind(), m.getObjectId(), m.getSourceId()),
				cachedMeta);

		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();

		// WHEN
		replayAll();

		ObjectDatumStreamMetadata meta = service.getDatumStreamMetadata(ObjectDatumKind.Node, null,
				TEST_SOUCE_ID);

		// THEN
		assertThat("Metadata returned", meta, notNullValue());
		assertThat("Metadata stream ID", meta.getStreamId(), is(equalTo(m.getStreamId())));
		assertThat("Metadata object kind", meta.getKind(), is(equalTo(ObjectDatumKind.Node)));
		assertThat("Metadata object ID", meta.getObjectId(), is(equalTo(TEST_NODE_ID)));
		assertThat("Metadata source ID", meta.getSourceId(), is(equalTo(TEST_SOUCE_ID)));
	}

	@Test
	public void availableSourceIds_noneLoaded() {
		// GIVEN

		// WHEN
		replayAll();
		Set<String> result = service.availableSourceMetadataSourceIds();

		// THEN
		assertThat("Empty set returned when no sources loaded", result,
				is(emptyCollectionOf(String.class)));
	}

	@Test
	public void availableSourceIds_someLoaded() {
		// GIVEN
		// put metadata into cache from start
		sourceMetadata.put(TEST_SOUCE_ID,
				service.createCachedMetadata(TEST_SOUCE_ID, new GeneralDatumMetadata()));

		final String sourceId2 = "test.source.2";
		sourceMetadata.put(sourceId2,
				service.createCachedMetadata(sourceId2, new GeneralDatumMetadata()));

		// WHEN
		replayAll();
		Set<String> result = service.availableSourceMetadataSourceIds();

		// THEN
		assertThat("Set contains loaded source IDs", result,
				containsInAnyOrder(TEST_SOUCE_ID, sourceId2));
	}

	@Test
	public void clearSourceCache_oneSource() throws IOException {
		// GIVEN
		Instruction instr = InstructionUtils.createLocalInstruction(
				JsonDatumMetadataService.DATUM_SOURCE_METADATA_CACHE_CLEAR_TOPIC,
				JsonDatumMetadataService.SOURCE_IDS_PARAM, TEST_SOUCE_ID);

		final String settingKey = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(StandardCharsets.UTF_8));
		Capture<Iterable<Resource>> resourcesCaptor = Capture.newInstance();
		settingsService.removeSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey),
				capture(resourcesCaptor));

		// WHEN
		replayAll();
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Instruction status returned", result, is(notNullValue()));
		assertThat("Instruction handled", result.getInstructionState(),
				is(equalTo(InstructionState.Completed)));

		Iterable<Resource> savedResources = resourcesCaptor.getValue();
		List<Resource> rsrcs = StreamSupport.stream(savedResources.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Removed single resource", rsrcs, hasSize(1));
	}

	@Test
	public void clearSourceCache_multiSource() throws IOException {
		// GIVEN
		final String sourceId2 = UUID.randomUUID().toString();
		Instruction instr = InstructionUtils.createLocalInstruction(
				JsonDatumMetadataService.DATUM_SOURCE_METADATA_CACHE_CLEAR_TOPIC,
				JsonDatumMetadataService.SOURCE_IDS_PARAM, TEST_SOUCE_ID + "," + sourceId2);

		final String settingKey1 = DigestUtils
				.md5DigestAsHex(TEST_SOUCE_ID.getBytes(StandardCharsets.UTF_8));
		Capture<Iterable<Resource>> resourcesCaptor1 = Capture.newInstance();
		settingsService.removeSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey1),
				capture(resourcesCaptor1));

		final String settingKey2 = DigestUtils.md5DigestAsHex(sourceId2.getBytes(UTF_8));
		Capture<Iterable<Resource>> resourcesCaptor2 = Capture.newInstance();
		settingsService.removeSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey2),
				capture(resourcesCaptor2));

		// WHEN
		replayAll();
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Instruction status returned", result, is(notNullValue()));
		assertThat("Instruction handled", result.getInstructionState(),
				is(equalTo(InstructionState.Completed)));

		List<Resource> rsrcs = StreamSupport.stream(resourcesCaptor1.getValue().spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Removed single resource 1", rsrcs, hasSize(1));

		rsrcs = StreamSupport.stream(resourcesCaptor2.getValue().spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Removed single resource 2", rsrcs, hasSize(1));
	}

	@Test
	public void clearSourceCache_signal() throws IOException {
		// GIVEN
		// put metadata into cache from start
		sourceMetadata.put(TEST_SOUCE_ID,
				service.createCachedMetadata(TEST_SOUCE_ID, new GeneralDatumMetadata()));

		final String sourceId2 = "test.source.2";
		sourceMetadata.put(sourceId2,
				service.createCachedMetadata(sourceId2, new GeneralDatumMetadata()));

		final String settingKey = DigestUtils.md5DigestAsHex(TEST_SOUCE_ID.getBytes(UTF_8));
		Capture<Iterable<Resource>> resourcesCaptor = Capture.newInstance();
		settingsService.removeSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey),
				capture(resourcesCaptor));

		final String settingKey2 = DigestUtils.md5DigestAsHex(sourceId2.getBytes(UTF_8));
		Capture<Iterable<Resource>> resourcesCaptor2 = Capture.newInstance();
		settingsService.removeSettingResources(eq(service.getSettingUid()), isNull(), eq(settingKey2),
				capture(resourcesCaptor2));

		// WHEN
		replayAll();

		Instruction instr = InstructionUtils.createLocalInstruction(InstructionHandler.TOPIC_SIGNAL,
				JsonDatumMetadataService.SETTING_UID, NodeMetadataInstructions.CLEAR_CACHE_SIGNAL);

		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Instruction status returned", result, is(notNullValue()));
		assertThat("Instruction handled", result.getInstructionState(),
				is(equalTo(InstructionState.Completed)));

		Iterable<Resource> savedResources = resourcesCaptor.getValue();
		List<Resource> rsrcs = StreamSupport.stream(savedResources.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Removed single resource for first source", rsrcs, hasSize(1));

		Iterable<Resource> savedResources2 = resourcesCaptor2.getValue();
		List<Resource> rsrcs2 = StreamSupport.stream(savedResources2.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Removed single resource for second source", rsrcs2, hasSize(1));
	}

}
