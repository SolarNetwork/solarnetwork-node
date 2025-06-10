/* ==================================================================
 * WebProxyServletTests.java - 26/03/2019 2:55:40 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.proxy.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.zip.GZIPOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.node.setup.web.proxy.SimpleWebProxyConfiguration;
import net.solarnetwork.node.setup.web.proxy.WebProxyServlet;

/**
 * Test cases for the {@link WebProxyServlet} class.
 * 
 * @author matt
 * @version 2.0
 */
public class WebProxyServletTests extends AbstractHttpServerTests {

	private static final String PROXY_PATH_PREFIX = "/foo/webproxy";

	private static final String PROXY_NAME = "bar";

	private static final String PROXIED_PATH = "/path/to/thing";

	private static final String REQUEST_PREFIX = PROXY_PATH_PREFIX + "/" + PROXY_NAME;

	private static final String REQUEST_PATH = REQUEST_PREFIX + PROXIED_PATH;

	private SimpleWebProxyConfiguration configuration;
	private WebProxyServlet servlet;

	@Before
	@Override
	public void setup() throws Exception {
		super.setup();
		configuration = new SimpleWebProxyConfiguration();
		configuration.setProxyPath("bar");
		configuration.setProxyTargetUri(getHttpServerBaseUrl());
		servlet = new WebProxyServlet(configuration, PROXY_PATH_PREFIX);
		servlet.init();
	}

	@Test
	public void notRewritableContentType() throws Exception {
		// given
		final String json = "{\"foo\":1}";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "application/json; charset=utf-8", json.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		assertThat("Response content unchanged", res.getContentAsString(), equalTo(json));
	}

	@Test
	public void rewritableNoLinks() throws Exception {
		// given
		final String html = "<html><h1>Hi</h1></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "text/html; charset=utf-8", html.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		assertThat("Response content unchanged", res.getContentAsString(), equalTo(html));
	}

	@Test
	public void rewritableAlmostALink() throws Exception {
		// given
		final String html = "<html><a not-quite-href=\"/me\">Hi</a></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "text/html; charset=utf-8", html.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		assertThat("Response content unchanged", res.getContentAsString(), equalTo(html));
	}

	@Test
	public void rewritableAlmostALink2() throws Exception {
		// given
		final String html = "<html><a ahref=\"/me\">Hi</a></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "text/html; charset=utf-8", html.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		assertThat("Response content unchanged", res.getContentAsString(), equalTo(html));
	}

	@Test
	public void rewritableWithRewritableLink() throws Exception {
		// given
		final String html = "<html><a href=\"/rewrite/me\">Hi</a></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "text/html; charset=utf-8", html.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		final String expectedHtml = String.format("<html><a href=\"%s/rewrite/me\">Hi</a></html>",
				REQUEST_PREFIX);
		assertThat("Response link rewritten", res.getContentAsString(), equalTo(expectedHtml));
	}

	@Test
	public void rewritableWithRewritableLinkAndGzip() throws Exception {
		// given
		final String html = "<html><a href=\"/rewrite/me\">Hi</a></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				ByteArrayOutputStream byos = new ByteArrayOutputStream();
				try (GZIPOutputStream out = new GZIPOutputStream(byos)) {
					FileCopyUtils.copy(new ByteArrayInputStream(html.getBytes("UTF-8")), out);
				}
				response.setHeader("Content-Encoding", "gzip");
				respondWithContent(response, "text/html; charset=utf-8", byos.toByteArray());
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		final String expectedHtml = String.format("<html><a href=\"%s/rewrite/me\">Hi</a></html>",
				REQUEST_PREFIX);
		assertThat("Response link rewritten", res.getContentAsString(), equalTo(expectedHtml));
	}

	@Test
	public void rewritableWithRewritableAction() throws Exception {
		// given
		final String html = "<html><form action=\"/rewrite/me\">Hi</form></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "text/html; charset=utf-8", html.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		final String expectedHtml = String
				.format("<html><form action=\"%s/rewrite/me\">Hi</form></html>", REQUEST_PREFIX);
		assertThat("Response link rewritten", res.getContentAsString(), equalTo(expectedHtml));
	}

	@Test
	public void rewritableWithRewritableLinkLotsOfWhitespace() throws Exception {
		// given
		final String html = "<html><a\nhref\n=\n\"/rewrite/me\">Hi</a></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "text/html; charset=utf-8", html.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		final String expectedHtml = String.format("<html><a\nhref\n=\n\"%s/rewrite/me\">Hi</a></html>",
				REQUEST_PREFIX);
		assertThat("Response link rewritten", res.getContentAsString(), equalTo(expectedHtml));
	}

	@Test
	public void rewritableAbsoluteLink() throws Exception {
		// given
		final String html = "<html><a href=\"http://localhost/me\">Hi</a></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "text/html; charset=utf-8", html.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		assertThat("Response content unchanged", res.getContentAsString(), equalTo(html));
	}

	@Test
	public void rewritableRelativeLink() throws Exception {
		// given
		final String html = "<html><a href=\"me\">Hi</a></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "text/html; charset=utf-8", html.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		assertThat("Response content unchanged", res.getContentAsString(), equalTo(html));
	}

	@Test
	public void rewritableMixAndMatch() throws Exception {
		// given
		final String html = "<html><link href=\"/c/1\"><link href=\"../that\"><a href=\"me\">Hi</a><form action = \"/c/2\"></form></html>";
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithContent(response, "text/html; charset=utf-8", html.getBytes("UTF-8"));
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		final String expectedHtml = String.format(
				"<html><link href=\"%1$s/c/1\"><link href=\"../that\"><a href=\"me\">Hi</a><form action = \"%1$s/c/2\"></form></html>",
				REQUEST_PREFIX);
		assertThat("Response content changed", res.getContentAsString(), equalTo(expectedHtml));
	}

	@Test
	public void rewritableFile01() throws Exception {
		// given
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertThat("Request method", request.getMethod(), equalTo("GET"));
				assertThat("Request path", request.getPathInfo(), equalTo(PROXIED_PATH));
				respondWithResource(response, "test-html-01.html", "text/html; charset=utf-8");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		// when
		MockHttpServletRequest req = new MockHttpServletRequest("GET", REQUEST_PATH);
		req.setPathInfo(req.getRequestURI());
		MockHttpServletResponse res = new MockHttpServletResponse();
		servlet.service(req, res);

		// then
		final String expectedHtml = FileCopyUtils.copyToString(
				new InputStreamReader(getClass().getResourceAsStream("test-html-01.exp.html"), "UTF-8"));
		File f = File.createTempFile("test-html-01.res.", ".html");
		FileCopyUtils.copy(res.getContentAsByteArray(), f);
		log.info("Saved output to {}", f);
		assertThat("Response content changed", res.getContentAsString(), equalTo(expectedHtml));
	}

}
