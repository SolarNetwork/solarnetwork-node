/* ==================================================================
 * JsonNodeMetadataServiceTests.java - 6/04/2024 3:42:51 pm
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

package net.solarnetwork.node.metadata.json.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.EasyMock;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.metadata.json.JsonNodeMetadataService;
import net.solarnetwork.node.metadata.json.NodeMetadataInstructions;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;

/**
 * Test cases for the {@link JsonNodeMetadataService} class.
 *
 * @author matt
 * @version 2.1
 */
public class JsonNodeMetadataServiceTests extends AbstractHttpServerTests {

	private static final Long TEST_NODE_ID = UUID.randomUUID().getMostSignificantBits();

	private IdentityService identityService;
	private JsonNodeMetadataService service;

	@Before
	public void setupClient() throws Exception {
		identityService = EasyMock.createMock(IdentityService.class);
		service = new JsonNodeMetadataService();
		service.setIdentityService(identityService);
		service.setObjectMapper(JsonUtils.newDatumObjectMapper());
	}

	private void replayAll() {
		EasyMock.replay(identityService);
	}

	@Override
	@After
	public void teardown() {
		EasyMock.verify(identityService);
	}

	@Test
	public void requestMetadata_notCached() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/nodes/meta"));
				respondWithJsonResource(request, response, "node-meta-01.json");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = service.getNodeMetadata();

		// THEN
		assertThat("Metadata returned", meta, is(notNullValue()));
		assertThat("Info metadata", meta.getInfo(), is(notNullValue()));
		assertThat("Info metadata size", meta.getInfo().keySet(), hasSize(1));
		assertThat("Info metadata a", meta.getInfo(), hasEntry("foo", "bar"));

		assertThat("Property metadata", meta.getPropertyInfo(), is(notNullValue()));
		assertThat("Property metadata size", meta.getPropertyInfo().keySet(), hasSize(1));
		assertThat("Property meta stuff -> a", meta.getInfoInteger("stuff", "a"), is(equalTo(1)));
		assertThat("Property meta stuff -> b", meta.getInfoString("stuff", "b"), is(equalTo("two")));
		assertThat("Property meta stuff -> c", meta.getInfoInteger("stuff", "c"), is(equalTo(42)));
	}

	@Test
	public void requestMetadata_cached() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		TestHttpHandler handler = new TestHttpHandler() {

			private boolean handled = false;

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Handled only once", handled, is(false));
				handled = true;
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/nodes/meta"));
				respondWithJsonResource(request, response, "node-meta-01.json");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = service.getNodeMetadata();
		GeneralDatumMetadata meta2 = service.getNodeMetadata();

		// THEN
		assertThat("Metadata returned", meta, is(notNullValue()));
		assertThat("Cached metadata returned", meta2, is(sameInstance(meta)));
	}

	@Test
	public void requestMetadata_cachedExpired() throws Exception {
		// GIVEN
		service.setCacheSeconds(1);
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		TestHttpHandler handler = new TestHttpHandler() {

			private int count = 0;

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/nodes/meta"));
				respondWithJsonResource(request, response, String.format("node-meta-0%d.json", ++count));
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = service.getNodeMetadata();
		Thread.sleep(1200L);
		GeneralDatumMetadata meta2 = service.getNodeMetadata();

		// THEN
		assertThat("Metadata returned", meta, is(notNullValue()));
		assertThat("Info metadata", meta.getInfo(), is(notNullValue()));
		assertThat("Info metadata size", meta.getInfo().keySet(), hasSize(1));
		assertThat("Info metadata a", meta.getInfo(), hasEntry("foo", "bar"));

		assertThat("Property metadata", meta.getPropertyInfo(), is(notNullValue()));
		assertThat("Property metadata size", meta.getInfo().keySet(), hasSize(1));
		assertThat("Property meta stuff -> a", meta.getInfoInteger("stuff", "a"), equalTo(1));
		assertThat("Property meta stuff -> b", meta.getInfoString("stuff", "b"), equalTo("two"));
		assertThat("Property meta stuff -> c", meta.getInfoInteger("stuff", "c"), equalTo(42));

		assertThat("Metadata 2 returned", meta2, is(notNullValue()));
		assertThat("Metadata 2 not cached instance", meta2, is(not(sameInstance(meta))));
		assertThat("Info 2 metadata", meta2.getInfo(), is(nullValue()));
		assertThat("Property 2 metadata", meta2.getPropertyInfo(), is(notNullValue()));
		assertThat("Property 2 metadata size", meta2.getPropertyInfo().keySet(), hasSize(1));
		assertThat("Property 2 meta stuff -> a", meta2.getInfoInteger("stuff", "d"), is(equalTo(1)));
	}

	@Test
	public void requestMetadata_cached_merge() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		TestHttpHandler handler = new TestHttpHandler() {

			private int count = 0;

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/nodes/meta"));
				int cnt = ++count;
				if ( cnt == 1 ) {
					assertThat("Request method", request.getMethod(), equalTo("GET"));
					respondWithJsonResource(request, response, "node-meta-01.json");
				} else if ( cnt == 2 ) {
					assertThat("Request method", request.getMethod(), equalTo("POST"));
					String body = getRequestBody(request);
					assertThat("Body is updated metadata", body, is(equalTo(
							"{\"m\":{\"bim\":\"bam\"},\"pm\":{\"wham\":{\"bam\":\"thankyoumam\"}}}")));
					respondWithJson(request, response, "{\"success\":true}");
				} else {
					response.setStatus(422);
				}
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = service.getNodeMetadata();

		GeneralDatumMetadata newMeta = new GeneralDatumMetadata();
		newMeta.putInfoValue("bim", "bam");
		newMeta.putInfoValue("wham", "bam", "thankyoumam");
		service.addNodeMetadata(newMeta);

		GeneralDatumMetadata meta2 = service.getNodeMetadata();

		// THEN
		assertThat("Metadata returned", meta, is(notNullValue()));
		assertThat("Metadata 2 is cached instance", meta2, is(sameInstance(meta)));

		assertThat("Info metadata has merged props", meta.getInfo().keySet(), hasSize(2));
		assertThat("Info metadata foo", meta.getInfoString("foo"), is(equalTo("bar")));
		assertThat("Info metadata bim (merged)", meta.getInfoString("bim"), is(equalTo("bam")));

		assertThat("Property metadata", meta.getPropertyInfo(), is(notNullValue()));
		assertThat("Property metadata has merged props", meta.getInfo().keySet(), hasSize(2));
		assertThat("Property meta stuff -> a", meta.getInfoInteger("stuff", "a"), equalTo(1));
		assertThat("Property meta stuff -> b", meta.getInfoString("stuff", "b"), equalTo("two"));
		assertThat("Property meta stuff -> c", meta.getInfoInteger("stuff", "c"), equalTo(42));
		assertThat("Property meta wham -> bam", meta.getInfoString("wham", "bam"),
				equalTo("thankyoumam"));
	}

	@Test
	public void addMetadata_notCached_noneExists() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		AtomicInteger counter = new AtomicInteger();

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/nodes/meta"));
				final int reqNum = counter.getAndIncrement();
				assertThat("No more than 2 requets made", reqNum, is(lessThan(3)));
				if ( reqNum == 0 ) {
					assertThat("Request 1 method", request.getMethod(), equalTo("GET"));
					respondWithJsonResource(request, response, "node-meta-03.json");
				} else if ( reqNum == 1 ) {
					assertThat("Request 2 method", request.getMethod(), equalTo("POST"));
					String body = getRequestBody(request);
					assertThat("Body is updated metadata", body,
							is(equalTo("{\"m\":{\"foo\":\"bar\"}}")));
					respondWithJson(request, response, "{\"success\":true}");
				}
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();
		GeneralDatumMetadata newMeta = new GeneralDatumMetadata();
		newMeta.putInfoValue("foo", "bar");
		service.addNodeMetadata(newMeta);

		// THEN
		assertThat("API called", counter.get(), is(equalTo(2)));
	}

	@Test
	public void clearCache() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		final AtomicInteger reqCount = new AtomicInteger();

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				reqCount.incrementAndGet();
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/api/v1/sec/nodes/meta"));
				respondWithJsonResource(request, response, "node-meta-01.json");
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		replayAll();

		// request 1 (populate in cache)
		GeneralDatumMetadata meta = service.getNodeMetadata();

		// clear cache
		Instruction instr = InstructionUtils.createLocalInstruction(InstructionHandler.TOPIC_SIGNAL,
				JsonNodeMetadataService.SETTING_UID, NodeMetadataInstructions.CLEAR_CACHE_SIGNAL);
		InstructionStatus result = service.processInstruction(instr);

		// request 2 (repopulate cache)
		GeneralDatumMetadata meta2 = service.getNodeMetadata();

		// THEN
		assertThat("Metadata returned on 1st request", meta, is(notNullValue()));

		assertThat("Signal instruction handled", result, is(notNullValue()));
		assertThat("Signal instruction completed", result.getInstructionState(),
				is(equalTo(InstructionState.Completed)));

		assertThat("Two requests executed", reqCount.get(), is(equalTo(2)));
		assertThat("Non-cached metadata returned on 2nd request", meta2, is(not(sameInstance(meta))));

	}

}
