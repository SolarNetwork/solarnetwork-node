/* ==================================================================
 * BasicAuthHttpRequestCustomizerService.java - 2/04/2023 6:52:50 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.http.req.test;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import net.solarnetwork.node.io.http.req.BasicAuthHttpRequestCustomizerService;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.util.ByteList;

/**
 * Test cases for the {@link BasicAuthHttpRequestCustomizerService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicAuthHttpRequestCustomizerServiceTests {

	private HttpRequest req;

	private Object[] mocks;

	@Before
	public void setup() {
		req = EasyMock.createMock(HttpRequest.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(req);
		if ( mocks != null ) {
			EasyMock.verify(mocks);
		}
	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(req);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
		}
	}

	@Test
	public void noCredentials() {
		final BasicAuthHttpRequestCustomizerService service = new BasicAuthHttpRequestCustomizerService();

		// WHEN
		replayAll();
		ByteList body = new ByteList();
		HttpRequest result = service.customize(req, body);

		// THEN
		assertThat("Result is input request", result, is(sameInstance(req)));
		assertThat("Body unchanged", body.size(), is(equalTo(0)));
	}

	// see https://datatracker.ietf.org/doc/html/rfc7617#section-2.1
	@Test
	public void auth_example() {
		// GIVEN
		final String username = "test";
		final String password = "123\u00A3";

		final BasicAuthHttpRequestCustomizerService service = new BasicAuthHttpRequestCustomizerService();
		service.setUsername(username);
		service.setPassword(password);
		service.configurationChanged(null);

		HttpHeaders headers = new HttpHeaders();
		expect(req.getHeaders()).andReturn(headers);

		// WHEN
		replayAll();
		ByteList body = new ByteList();
		HttpRequest result = service.customize(req, body);

		// THEN
		assertThat("Result is input request", result, is(sameInstance(req)));
		assertThat("Body unchanged", body.size(), is(equalTo(0)));
		String expectedAuth = "Basic dGVzdDoxMjPCow==";
		assertThat("Authorization header populated", headers.getFirst(HttpHeaders.AUTHORIZATION),
				is(equalTo(expectedAuth)));
	}

	@Test
	public void auth_example_withPlaceholderServiceConfigured() {
		// GIVEN
		final String username = "test";
		final String password = "123\u00A3";

		final PlaceholderService ps = EasyMock.createMock(PlaceholderService.class);

		final BasicAuthHttpRequestCustomizerService service = new BasicAuthHttpRequestCustomizerService();
		service.setPlaceholderService(new StaticOptionalService<>(ps));
		service.setUsername(username);
		service.setPassword(password);
		service.configurationChanged(null);

		HttpHeaders headers = new HttpHeaders();
		expect(req.getHeaders()).andReturn(headers);

		// WHEN
		replayAll(ps);
		ByteList body = new ByteList();
		HttpRequest result = service.customize(req, body);

		// THEN
		assertThat("Result is input request", result, is(sameInstance(req)));
		assertThat("Body unchanged", body.size(), is(equalTo(0)));
		String expectedAuth = "Basic dGVzdDoxMjPCow==";
		assertThat("Authorization header populated", headers.getFirst(HttpHeaders.AUTHORIZATION),
				is(equalTo(expectedAuth)));
	}

	@Test
	public void auth_random() {
		// GIVEN
		final String username = UUID.randomUUID().toString();
		final String password = UUID.randomUUID().toString();

		final BasicAuthHttpRequestCustomizerService service = new BasicAuthHttpRequestCustomizerService();
		service.setUsername(username);
		service.setPassword(password);
		service.configurationChanged(null);

		HttpHeaders headers = new HttpHeaders();
		expect(req.getHeaders()).andReturn(headers);

		// WHEN
		replayAll();
		ByteList body = new ByteList();
		HttpRequest result = service.customize(req, body);

		// THEN
		assertThat("Result is input request", result, is(sameInstance(req)));
		assertThat("Body unchanged", body.size(), is(equalTo(0)));
		String expectedAuth = "Basic " + Base64.getEncoder()
				.encodeToString((username + ':' + password).getBytes(StandardCharsets.UTF_8));
		assertThat("Authorization header populated", headers.getFirst(HttpHeaders.AUTHORIZATION),
				is(equalTo(expectedAuth)));
	}

	@Test
	public void auth_parameters() {
		// GIVEN
		final String username = "test";
		final String password = "123\u00A3";

		final BasicAuthHttpRequestCustomizerService service = new BasicAuthHttpRequestCustomizerService();
		service.configurationChanged(null);

		HttpHeaders headers = new HttpHeaders();
		expect(req.getHeaders()).andReturn(headers);

		// WHEN
		replayAll();
		ByteList body = new ByteList();
		Map<String, String> parameters = new HashMap<>(2);
		parameters.put(BasicAuthHttpRequestCustomizerService.USERNAME_PARAM, username);
		parameters.put(BasicAuthHttpRequestCustomizerService.PASSWORD_PARAM, password);
		HttpRequest result = service.customize(req, body, parameters);

		// THEN
		assertThat("Result is input request", result, is(sameInstance(req)));
		assertThat("Body unchanged", body.size(), is(equalTo(0)));
		String expectedAuth = "Basic dGVzdDoxMjPCow==";
		assertThat("Authorization header populated", headers.getFirst(HttpHeaders.AUTHORIZATION),
				is(equalTo(expectedAuth)));
	}

	@Test
	public void auth_parameters_override() {
		// GIVEN
		final BasicAuthHttpRequestCustomizerService service = new BasicAuthHttpRequestCustomizerService();
		service.setUsername("test");
		service.setPassword("123\u00A3");
		service.configurationChanged(null);

		HttpHeaders headers = new HttpHeaders();
		expect(req.getHeaders()).andReturn(headers);

		final String username = UUID.randomUUID().toString();
		final String password = UUID.randomUUID().toString();

		// WHEN
		replayAll();
		ByteList body = new ByteList();
		Map<String, String> parameters = new HashMap<>(2);
		parameters.put(BasicAuthHttpRequestCustomizerService.USERNAME_PARAM, username);
		parameters.put(BasicAuthHttpRequestCustomizerService.PASSWORD_PARAM, password);
		HttpRequest result = service.customize(req, body, parameters);

		// THEN
		assertThat("Result is input request", result, is(sameInstance(req)));
		assertThat("Body unchanged", body.size(), is(equalTo(0)));
		String expectedAuth = "Basic " + Base64.getEncoder()
				.encodeToString((username + ':' + password).getBytes(StandardCharsets.UTF_8));
		assertThat("Authorization header populated", headers.getFirst(HttpHeaders.AUTHORIZATION),
				is(equalTo(expectedAuth)));
	}

	@Test
	public void auth_placeholders() {
		// GIVEN
		final String username = "{fooUser}";
		final String password = "{fooPass}";

		final PlaceholderService ps = EasyMock.createMock(PlaceholderService.class);

		final BasicAuthHttpRequestCustomizerService service = new BasicAuthHttpRequestCustomizerService();
		service.setPlaceholderService(new StaticOptionalService<>(ps));
		service.setUsername(username);
		service.setPassword(password);
		service.configurationChanged(null);

		expect(ps.resolvePlaceholders(eq(username), isNull())).andReturn("test");
		expect(ps.resolvePlaceholders(eq(password), isNull())).andReturn("123\u00A3");

		HttpHeaders headers = new HttpHeaders();
		expect(req.getHeaders()).andReturn(headers);

		// WHEN
		replayAll(ps);
		ByteList body = new ByteList();
		HttpRequest result = service.customize(req, body);

		// THEN
		assertThat("Result is input request", result, is(sameInstance(req)));
		assertThat("Body unchanged", body.size(), is(equalTo(0)));
		String expectedAuth = "Basic dGVzdDoxMjPCow==";
		assertThat("Authorization header populated", headers.getFirst(HttpHeaders.AUTHORIZATION),
				is(equalTo(expectedAuth)));
	}

}
