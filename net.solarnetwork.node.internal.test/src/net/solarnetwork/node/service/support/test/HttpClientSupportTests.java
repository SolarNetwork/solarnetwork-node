/* ==================================================================
 * HttpClientSupportTests.java - 11/07/2023 8:04:23 am
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

package net.solarnetwork.node.service.support.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import net.solarnetwork.node.service.support.HttpClientSupport;
import net.solarnetwork.node.test.internal.AbstractHttpClientTests;
import net.solarnetwork.node.test.internal.TestHttpHandler;

/**
 * Test cases for the {@link HttpClientSupport} class.
 * 
 * @author matt
 * @version 1.0
 */
public class HttpClientSupportTests extends AbstractHttpClientTests {

	private final class TestClient extends HttpClientSupport {

		private InputStream getJson(String url) throws IOException {
			return getInputStreamFromURLConnection(getURLConnection(url, "GET", "application/json"));
		}
	}

	@Test
	public void gzipResponseStream() throws IOException {
		// GIVEN
		final String json = FileCopyUtils.copyToString(new InputStreamReader(
				getClass().getResourceAsStream("meta-01.json"), StandardCharsets.UTF_8));

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Req method", request.getMethod(), is(equalTo("GET")));
				assertThat("Path", request.getPathInfo(), is(equalTo("/meta-01.json")));
				response.setHeader("Content-Encoding", "gzip");
				respondWithJson(response, json);
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		final TestClient client = new TestClient();

		// WHEN
		final String url = getHttpServerBaseUrl() + "/meta-01.json";
		try (InputStream in = client.getJson(url)) {
			assertThat("InputStream is GZIP", in, is(instanceOf(GZIPInputStream.class)));
			String result = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
			assertThat("JSON gunzipped", result, is(equalTo(json)));
		}
	}

	@Test
	public void deflateResponseStream() throws IOException {
		// GIVEN
		final String json = FileCopyUtils.copyToString(new InputStreamReader(
				getClass().getResourceAsStream("meta-01.json"), StandardCharsets.UTF_8));

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Req method", request.getMethod(), is(equalTo("GET")));
				assertThat("Path", request.getPathInfo(), is(equalTo("/meta-01.json")));
				response.setHeader("Content-Encoding", "deflate");
				respondWithJson(response, json);
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		final TestClient client = new TestClient();

		// WHEN
		final String url = getHttpServerBaseUrl() + "/meta-01.json";
		try (InputStream in = client.getJson(url)) {
			assertThat("InputStream is GZIP", in, is(instanceOf(InflaterInputStream.class)));
			String result = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
			assertThat("JSON gunzipped", result, is(equalTo(json)));
		}
	}

}
