/* ==================================================================
 * UserMetadataServiceTests.java - 7/05/2021 3:20:50 PM
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

package net.solarnetwork.node.support.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Date;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.DeserializationFeature;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.NetworkIdentity;
import net.solarnetwork.node.domain.NodeAppConfiguration;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.node.support.UserMetadataService;
import net.solarnetwork.node.test.internal.AbstractHttpClientTests;
import net.solarnetwork.node.test.internal.TestHttpHandler;
import net.solarnetwork.util.ObjectMapperFactoryBean;
import net.solarnetwork.util.StaticOptionalService;
import net.solarnetwork.web.security.AuthorizationV2Builder;

/**
 * Test cases for the {@link UserMetadataService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserMetadataServiceTests extends AbstractHttpClientTests {

	private SetupService setupService;
	private NodeAppConfiguration appConfig;
	private UserMetadataService service;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		setupService = EasyMock.createMock(SetupService.class);

		appConfig = new NodeAppConfiguration(singletonMap(NetworkIdentity.SOLARQUERY_NETWORK_SERVICE_KEY,
				getHttpServerBaseUrl() + "/solarquery"));

		service = new UserMetadataService(new StaticOptionalService<>(setupService));

		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setFeaturesToDisable(asList(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		service.setObjectMapper(factory.getObject());
	}

	@Override
	@After
	public void teardown() throws Exception {
		super.teardown();
		EasyMock.verify(setupService);
	}

	private void replayAll() {
		EasyMock.replay(setupService);
	}

	@Test
	public void getMetadata_withoutToken() {
		// GIVEN
		expect(setupService.getAppConfiguration()).andReturn(appConfig);

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(),
						equalTo("/solarquery/api/v1/sec/users/meta"));
				respondWithJsonResource(response, "meta-01.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		replayAll();
		GeneralDatumMetadata meta = service.getAllMetadata();

		// THEN
		assertThat("Metadata returned", meta, notNullValue());
		assertThat("Info metadata", meta.getInfo(), notNullValue());
		assertThat("Info metadata keys", meta.getInfo().keySet(), contains("foo"));
		assertThat("Info metadata foo value", meta.getInfo(), hasEntry("foo", "bar"));
		assertThat("Property metadata", meta.getPropertyInfo(), notNullValue());
		assertThat("Property metadata keys", meta.getPropertyInfo().keySet(),
				contains("irradianceHours"));
		assertThat("irradianceHours -> date", meta.getInfoLong("irradianceHours", "vm-date"),
				equalTo(1535065709013L));
		assertThat("irradianceHours -> value", meta.getInfoString("irradianceHours", "vm-value"),
				equalTo("61"));
		assertThat("irradianceHours -> reading", meta.getInfoString("irradianceHours", "vm-reading"),
				equalTo("558.857455"));
	}

	@Test
	public void getMetadata_withToken() {
		final String token = UUID.randomUUID().toString();
		final String secret = UUID.randomUUID().toString();
		service.setToken(token);
		service.setTokenSecret(secret);

		// GIVEN
		expect(setupService.getAppConfiguration()).andReturn(appConfig);

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(),
						equalTo("/solarquery/api/v1/sec/users/meta"));

				long reqDate = request.getDateHeader("Date");
				String auth = new AuthorizationV2Builder(token).saveSigningKey(secret)
						.host("localhost:" + getHttpServerPort()).date(new Date(reqDate)).build();
				assertThat("Request auth", request.getHeader("Authorization"), equalTo(auth));

				respondWithJsonResource(response, "meta-01.json");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// WHEN
		replayAll();
		GeneralDatumMetadata meta = service.getAllMetadata();

		// THEN
		assertThat("Metadata returned", meta, notNullValue());
		assertThat("Info metadata", meta.getInfo(), notNullValue());
		assertThat("Info metadata keys", meta.getInfo().keySet(), contains("foo"));
		assertThat("Info metadata foo value", meta.getInfo(), hasEntry("foo", "bar"));
		assertThat("Property metadata", meta.getPropertyInfo(), notNullValue());
		assertThat("Property metadata keys", meta.getPropertyInfo().keySet(),
				contains("irradianceHours"));
		assertThat("irradianceHours -> date", meta.getInfoLong("irradianceHours", "vm-date"),
				equalTo(1535065709013L));
		assertThat("irradianceHours -> value", meta.getInfoString("irradianceHours", "vm-value"),
				equalTo("61"));
		assertThat("irradianceHours -> reading", meta.getInfoString("irradianceHours", "vm-reading"),
				equalTo("558.857455"));
	}

}
