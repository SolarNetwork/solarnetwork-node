/* ==================================================================
 * Snws2AuthHttpRequestCustomizerServiceTests.java - 5/08/2024 5:26:06 pm
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

package net.solarnetwork.node.io.http.req.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import net.solarnetwork.node.io.http.req.Snws2AuthHttpRequestCustomizerService;
import net.solarnetwork.util.ByteList;

/**
 * Test cases for the {@link Snws2AuthHttpRequestCustomizerService} class.
 *
 * @author matt
 * @version 1.0
 */
public class Snws2AuthHttpRequestCustomizerServiceTests {

	private static final ZonedDateTime TEST_TS = LocalDateTime.of(2024, 8, 5, 17, 30)
			.atZone(ZoneOffset.UTC);
	private static final String TEST_HOST = "localhost";
	private static final String TEST_BASE_URL = "http://" + TEST_HOST;

	private HttpRequest req;

	private Clock clock;
	private Object[] mocks;

	@Before
	public void setup() {
		clock = Clock.fixed(TEST_TS.toInstant(), TEST_TS.getZone());
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
		final Snws2AuthHttpRequestCustomizerService service = new Snws2AuthHttpRequestCustomizerService(
				clock);

		// WHEN
		replayAll();
		ByteList body = new ByteList();
		HttpRequest result = service.customize(req, body);

		// THEN
		assertThat("Result is input request", result, is(sameInstance(req)));
		assertThat("Body unchanged", body.size(), is(equalTo(0)));
	}

	@Test
	public void auth_example() {
		// GIVEN
		final URI uri = URI.create(TEST_BASE_URL + "/foo/bar");

		final String token = "test";
		final String secret = "secret";

		final Snws2AuthHttpRequestCustomizerService service = new Snws2AuthHttpRequestCustomizerService(
				clock);
		service.setToken(token);
		service.setTokenSecret(secret);

		expect(req.getURI()).andReturn(uri);
		expect(req.getMethod()).andReturn(HttpMethod.GET);

		HttpHeaders headers = new HttpHeaders();
		expect(req.getHeaders()).andReturn(headers).anyTimes();

		// WHEN
		replayAll();
		ByteList body = new ByteList();
		HttpRequest result = service.customize(req, body);

		// THEN
		assertThat("Result is input request", result, is(sameInstance(req)));
		assertThat("Body unchanged", body.size(), is(equalTo(0)));

		assertThat("Authorization header populated", headers.getFirst(HttpHeaders.AUTHORIZATION),
				is(startsWith("SNWS2 Credential=test,SignedHeaders=date;host,Signature=")));
	}

}
