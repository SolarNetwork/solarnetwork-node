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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.metadata.json.JsonNodeMetadataService;
import net.solarnetwork.node.service.IdentityService;

/**
 * Test cases for the {@link JsonNodeMetadataService} class.
 *
 * @author matt
 * @version 1.0
 */
public class JsonNodeMetadataServiceTests extends AbstractHttpClientTests {

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
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo("/api/v1/sec/nodes/meta"));
				respondWithJsonResource(response, "node-meta-01.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

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
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Handled only once", handled, is(false));
				handled = true;
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo("/api/v1/sec/nodes/meta"));
				respondWithJsonResource(response, "node-meta-01.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

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
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo("/api/v1/sec/nodes/meta"));
				respondWithJsonResource(response, String.format("node-meta-0%d.json", ++count));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

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
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request path", request.getPathInfo(), equalTo("/api/v1/sec/nodes/meta"));
				int cnt = ++count;
				if ( cnt == 1 ) {
					assertThat("Request method", request.getMethod(), equalTo("GET"));
					respondWithJsonResource(response, "node-meta-01.json");
				} else if ( cnt == 2 ) {
					assertThat("Request method", request.getMethod(), equalTo("POST"));
					String body = FileCopyUtils.copyToString(
							new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
					assertThat("Body is updated metadata", body, is(equalTo(
							"{\"m\":{\"bim\":\"bam\"},\"pm\":{\"wham\":{\"bam\":\"thankyoumam\"}}}")));
					respondWithJson(response, "{\"success\":true}");
				} else {
					response.sendError(422);
				}
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

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

}
