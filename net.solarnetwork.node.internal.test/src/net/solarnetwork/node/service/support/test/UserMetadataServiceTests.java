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

package net.solarnetwork.node.service.support.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.util.UUID;
import org.easymock.EasyMock;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.DeserializationFeature;
import net.solarnetwork.codec.ObjectMapperFactoryBean;
import net.solarnetwork.domain.NetworkIdentity;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.NodeAppConfiguration;
import net.solarnetwork.node.service.support.UserMetadataService;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.security.Snws2AuthorizationBuilder;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;

/**
 * Test cases for the {@link UserMetadataService} class.
 *
 * @author matt
 * @version 2.0
 */
public class UserMetadataServiceTests extends AbstractHttpServerTests {

	private SetupService setupService;
	private NodeAppConfiguration appConfig;
	private UserMetadataService service;

	@Override
	@Before
	public void setup() {
		super.setup();
		setupService = EasyMock.createMock(SetupService.class);

		appConfig = new NodeAppConfiguration(singletonMap(NetworkIdentity.SOLARQUERY_NETWORK_SERVICE_KEY,
				getHttpServerBaseUrl() + "/solarquery"));

		service = new UserMetadataService(new StaticOptionalService<>(setupService));

		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setFeaturesToDisable(asList(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		try {
			service.setObjectMapper(factory.getObject());
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@After
	public void teardown() {
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
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(),
						equalTo("/solarquery/api/v1/sec/users/meta"));
				respondWithJsonResource(request, response, "meta-01.json");
				return true;
			}

		};
		addHandler(handler);

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

			private final String p = "/solarquery/api/v1/sec/users/meta";

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getHttpURI().getPath(), equalTo(p));

				long reqDate = request.getHeaders().getDateField("Date");
				String auth = new Snws2AuthorizationBuilder(token).saveSigningKey(secret)
						.host("localhost:" + getHttpServerPort()).path(p)
						.date(Instant.ofEpochMilli(reqDate)).build();
				assertThat("Request auth", request.getHeaders().get("Authorization"), equalTo(auth));

				respondWithJsonResource(request, response, "meta-01.json");
				return true;
			}

		};
		addHandler(handler);

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
