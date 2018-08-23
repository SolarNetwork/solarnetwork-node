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

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.metadata.json.JsonDatumMetadataService;
import net.solarnetwork.util.ObjectMapperFactoryBean;

/**
 * Test cases for the {@link JsonDatumMetadataService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JsonDatumMetadataServiceTests extends AbstractHttpClientTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final String TEST_SOUCE_ID = "test.source";

	private IdentityService identityService;
	private SettingDao settingDao;
	private JsonDatumMetadataService service;

	@Before
	public void setupClient() throws Exception {
		identityService = EasyMock.createMock(IdentityService.class);
		settingDao = EasyMock.createMock(SettingDao.class);
		service = new JsonDatumMetadataService();
		service.setIdentityService(identityService);
		service.setSettingDao(settingDao);

		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setFeaturesToDisable(Arrays.asList(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		service.setObjectMapper(factory.getObject());
	}

	private void replayAll() {
		EasyMock.replay(identityService, settingDao);
	}

	@Override
	@After
	public void teardown() {
		EasyMock.verify(identityService, settingDao);
	}

	@Test
	public void requestMetadata() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID));
				assertThat("Source ID", request.getParameter("sourceId"), equalTo(TEST_SOUCE_ID));
				respondWithJsonResource(response, "meta-01.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

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
	public void postMetadataNotCached() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// no cached metadata available (first to look up, second before saving)
		expect(settingDao.getSetting(JsonDatumMetadataService.SETTING_KEY_SOURCE_META, TEST_SOUCE_ID))
				.andReturn(null).times(2);

		settingDao.storeSetting(JsonDatumMetadataService.SETTING_KEY_SOURCE_META, TEST_SOUCE_ID,
				"{\"m\":{\"foo\":\"bar\"}}");

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("POST"));
				assertThat("Request path", request.getPathInfo(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID));
				assertThat("Source ID", request.getParameter("sourceId"), equalTo(TEST_SOUCE_ID));

				String body = FileCopyUtils.copyToString(request.getReader());
				assertThat("JSON body", body, equalTo("{\"m\":{\"foo\":\"bar\"}}"));

				respondWithJson(response, "{\"success\":true}");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// THEN
	}

	@Test
	public void postMetadataAlreadyCached() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// no cached metadata available (first to look up, second before saving)
		expect(settingDao.getSetting(JsonDatumMetadataService.SETTING_KEY_SOURCE_META, TEST_SOUCE_ID))
				.andReturn("{\"m\":{\"foo\":\"bar\"}}").times(2);

		settingDao.storeSetting(JsonDatumMetadataService.SETTING_KEY_SOURCE_META, TEST_SOUCE_ID,
				"{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\"}}");

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("POST"));
				assertThat("Request path", request.getPathInfo(),
						equalTo("/api/v1/sec/datum/meta/" + TEST_NODE_ID));
				assertThat("Source ID", request.getParameter("sourceId"), equalTo(TEST_SOUCE_ID));

				String body = FileCopyUtils.copyToString(request.getReader());
				assertThat("JSON body", body, equalTo("{\"m\":{\"foo\":\"bar\",\"bim\":\"bam\"}}"));

				respondWithJson(response, "{\"success\":true}");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("bim", "bam");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// THEN
	}

	@Test
	public void postMetadataAlreadyCachedNoChange() throws Exception {
		// GIVEN
		expect(identityService.getNodeId()).andReturn(TEST_NODE_ID).anyTimes();
		expect(identityService.getSolarInBaseUrl()).andReturn(getHttpServerBaseUrl()).anyTimes();

		// no cached metadata available (first to look up, second before saving)
		expect(settingDao.getSetting(JsonDatumMetadataService.SETTING_KEY_SOURCE_META, TEST_SOUCE_ID))
				.andReturn("{\"m\":{\"foo\":\"bar\"}}").times(1);

		// WHEN
		replayAll();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		service.addSourceMetadata(TEST_SOUCE_ID, meta);

		// THEN
	}

}
