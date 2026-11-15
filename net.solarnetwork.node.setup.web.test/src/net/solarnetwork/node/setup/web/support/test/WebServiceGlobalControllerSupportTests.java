/* ==================================================================
 * WebServiceGlobalControllerSupportTests.java - 14 Nov 2026 7:05:12 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.support.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.method.annotation.ExceptionHandlerMappingInfo;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.node.setup.web.support.GlobalExceptionRestController;
import net.solarnetwork.node.setup.web.support.ObjectMapperCustomizer;
import net.solarnetwork.node.setup.web.support.WebServiceControllerSupport;
import net.solarnetwork.node.setup.web.support.WebServiceGlobalControllerSupport;

/**
 * Test cases for the {@link WebServiceGlobalControllerSupport} class.
 *
 * @author matt
 * @version 1.0
 */
public class WebServiceGlobalControllerSupportTests {

	/** The {@code Accept} header a web browser sends for a page. */
	private static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

	/** The error result of the authentication failure. */
	private static final String AUTHENTICATION_RESULT_JSON = """
			{"success":false,"message":"Bad credentials"}""";

	/** The error result of the access failure. */
	private static final String ACCESS_RESULT_JSON = """
			{"success":false,"message":"Access denied"}""";

	/**
	 * A controller that fails in the ways the advice handles.
	 */
	@GlobalExceptionRestController
	public static final class TestController {

		/**
		 * Fail authentication.
		 *
		 * @return nothing
		 */
		@GetMapping("/authentication")
		public String authentication() {
			throw new BadCredentialsException("Bad credentials");
		}

		/**
		 * Deny access.
		 *
		 * @return nothing
		 */
		@GetMapping("/access")
		public String access() {
			throw new AccessDeniedException("Access denied");
		}

		/**
		 * Commit a partial response, and then fail with an exception that
		 * {@link WebServiceControllerSupport} has a handler for.
		 *
		 * @param res
		 *        the response
		 * @throws IOException
		 *         if an IO error occurs
		 */
		@GetMapping("/committed")
		public void committed(HttpServletResponse res) throws IOException {
			res.getOutputStream().write("partial".getBytes(StandardCharsets.UTF_8));
			res.flushBuffer();
			throw new InvalidDataAccessResourceUsageException("Bad SQL");
		}

	}

	private ObjectMapperCustomizer mappers;
	private MockMvc mvc;

	@Before
	public void setup() {
		mappers = new ObjectMapperCustomizer(JsonUtils::newDatumObjectMapper);
		final WebServiceControllerSupport webServiceAdvice = new WebServiceControllerSupport();
		webServiceAdvice.setMessageSource(new StaticMessageSource());
		mvc = MockMvcBuilders.standaloneSetup(new TestController())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(mappers.getObjectMapper()),
						new MappingJackson2CborHttpMessageConverter(mappers.getCborObjectMapper()))
				.setControllerAdvice(webServiceAdvice, new WebServiceGlobalControllerSupport()).build();
	}

	/**
	 * Decode CBOR into JSON.
	 *
	 * @param cbor
	 *        the CBOR to decode
	 * @return the JSON
	 * @throws IOException
	 *         if the CBOR can not be decoded
	 */
	private String cborJson(byte[] cbor) throws IOException {
		return mappers.getObjectMapper()
				.writeValueAsString(mappers.getCborObjectMapper().readTree(cbor));
	}

	@Test
	public void authenticationException() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get("/authentication").accept(MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();

		// THEN
		then(res.getStatus()).as("Not authorized").isEqualTo(401);
		then(res.getContentAsString()).as("Error result provided").isEqualTo(AUTHENTICATION_RESULT_JSON);
	}

	@Test
	public void authenticationException_anyAccept() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(get("/authentication").accept(MediaType.ALL))
				.andReturn().getResponse();

		// THEN
		then(res.getStatus()).as("Not authorized").isEqualTo(401);
		then(res.getContentType()).as("Error result in default format")
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		then(res.getContentAsString()).as("Error result provided").isEqualTo(AUTHENTICATION_RESULT_JSON);
	}

	@Test
	public void authenticationException_browserAccept() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get("/authentication").header(HttpHeaders.ACCEPT, BROWSER_ACCEPT)).andReturn()
				.getResponse();

		// THEN
		then(res.getStatus()).as("Not authorized").isEqualTo(401);
		then(res.getContentType()).as("Error result in default format")
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		then(res.getContentAsString()).as("Error result provided").isEqualTo(AUTHENTICATION_RESULT_JSON);
	}

	@Test
	public void authenticationException_cbor() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get("/authentication").accept(MediaType.APPLICATION_CBOR)).andReturn()
				.getResponse();

		// THEN
		then(res.getStatus()).as("Not authorized").isEqualTo(401);
		then(res.getContentType()).as("Error result in requested format")
				.isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		then(cborJson(res.getContentAsByteArray())).as("Error result provided as CBOR")
				.isEqualTo(AUTHENTICATION_RESULT_JSON);
	}

	@Test
	public void authenticationException_cborPreferred() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(get("/authentication").header(HttpHeaders.ACCEPT,
				"application/cbor, application/json;q=0.5")).andReturn().getResponse();

		// THEN
		then(res.getStatus()).as("Not authorized").isEqualTo(401);
		then(res.getContentType()).as("Error result in preferred format")
				.isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		then(cborJson(res.getContentAsByteArray())).as("Error result provided as CBOR")
				.isEqualTo(AUTHENTICATION_RESULT_JSON);
	}

	@Test
	public void accessDeniedException() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get("/access").accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		then(res.getStatus()).as("Forbidden").isEqualTo(403);
		then(res.getContentAsString()).as("Error result provided").isEqualTo(ACCESS_RESULT_JSON);
	}

	@Test
	public void accessDeniedException_anyAccept() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(get("/access").accept(MediaType.ALL)).andReturn()
				.getResponse();

		// THEN
		then(res.getStatus()).as("Forbidden").isEqualTo(403);
		then(res.getContentType()).as("Error result in default format")
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		then(res.getContentAsString()).as("Error result provided").isEqualTo(ACCESS_RESULT_JSON);
	}

	@Test
	public void handlersProduceJsonOrCborOnly() {
		// GIVEN
		final ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(
				WebServiceGlobalControllerSupport.class);
		final MediaType csv = MediaType.valueOf("text/csv");

		// THEN
		then(List.<Class<? extends Throwable>> of(BadCredentialsException.class,
				AccessDeniedException.class)).allSatisfy(type -> {
					then(resolver.resolveExceptionMappingByExceptionType(type, MediaType.ALL))
							.as("Handler for %s produces JSON or CBOR", type.getSimpleName())
							.extracting(ExceptionHandlerMappingInfo::getProducibleTypes,
									InstanceOfAssertFactories.collection(MediaType.class))
							.containsExactlyInAnyOrder(MediaType.APPLICATION_JSON,
									MediaType.APPLICATION_CBOR);
					then(resolver.resolveExceptionMappingByExceptionType(type, MediaType.TEXT_HTML))
							.as("No handler for %s as HTML", type.getSimpleName()).isNull();
					then(resolver.resolveExceptionMappingByExceptionType(type, csv))
							.as("No handler for %s as CSV", type.getSimpleName()).isNull();
				});
	}

	@Test
	public void exceptionAfterResponseCommitted() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get("/committed").accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		then(res.isCommitted()).as("Response committed").isTrue();
		then(res.getContentAsString()).as("Error result not added to the committed response")
				.isEqualTo("partial");
	}

}
