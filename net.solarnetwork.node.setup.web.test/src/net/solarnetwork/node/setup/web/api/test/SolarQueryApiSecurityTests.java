/* ==================================================================
 * SolarQueryApiSecurityTests.java - 13 Nov 2026 5:58:42 pm
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

package net.solarnetwork.node.setup.web.api.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletMapping;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import jakarta.servlet.Filter;
import jakarta.servlet.http.MappingMatch;
import net.solarnetwork.node.domain.SecurityToken;
import net.solarnetwork.node.service.FilterableDatumHistorian;
import net.solarnetwork.node.service.SecurityTokenService;
import net.solarnetwork.node.setup.web.api.NodesController;
import net.solarnetwork.node.setup.web.security.SecurityTokenAuthenticationFilter;
import net.solarnetwork.node.setup.web.security.SecurityTokenServiceUserDetailsService;
import net.solarnetwork.node.setup.web.support.HistorianSelectorFilter;
import net.solarnetwork.node.setup.web.support.WebServiceControllerSupport;
import net.solarnetwork.node.setup.web.support.WebServiceGlobalControllerSupport;
import net.solarnetwork.security.AuthorizationUtils;
import net.solarnetwork.security.Snws2AuthorizationBuilder;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.web.jakarta.security.SecurityTokenAuthenticationEntryPoint;

/**
 * Test the SolarQuery compatible API security, with the filters in the same
 * order as the web application.
 *
 * <p>
 * The security filter chains are like the {@code /api/v1/sec/**} and
 * {@code /solarquery/api/v1/sec/**} ones of the web application, with the CORS
 * configuration of its MVC context, and a catch-all chain that allows any
 * request stands in for the web application's other chains, which allow
 * requests to paths they do not protect. The authentication entry point
 * resolves errors with the exception handlers, as in the web application.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class SolarQueryApiSecurityTests extends SolarQueryApiTestSupport {

	private static final String TOKEN_ID = "abcdefghij0123456789";
	private static final String TOKEN_SECRET = "secret";
	private static final String SOURCES_PATH = "/solarquery/api/v1/sec/nodes/sources";
	private static final String API_SOURCES_PATH = "/api/v1/sec/nodes/sources";
	private static final String UNFILTERED_SOURCES_PATH = "/unfiltered" + SOURCES_PATH;
	private static final String OTHER_ORIGIN = "https://other.example.com";

	/** The error result of a request without authentication. */
	private static final String UNAUTHENTICATED_RESULT_JSON = """
			{"success":false,"message":"Full authentication is required to access this resource"}""";

	/** The MVC context of the web application, relative to this project. */
	private static final String APP_MVC_CONTEXT = "../net.solarnetwork.node.setup.web/web/WEB-INF/webmvc-context.xml";

	/** The Spring MVC XML namespace. */
	private static final String MVC_NS = "http://www.springframework.org/schema/mvc";

	/**
	 * The bean name of the CORS configuration defined with {@code <mvc:cors>}.
	 */
	private static final String MVC_CORS_BEAN_NAME = "mvcCorsConfigurations";

	@Mock
	private SecurityTokenService securityTokenService;

	private GenericWebApplicationContext ctx;
	private MockMvc mvc;

	@Before
	public void setup() throws Exception {
		setupSupport();

		ctx = new GenericWebApplicationContext(new MockServletContext());
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ctx);
		ctx.registerBean(DelegatingWebMvcConfiguration.class);
		ctx.registerBean(WebMvcConfigurer.class, () -> new WebMvcConfigurer() {

			@Override
			public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
				converters.addAll(messageConverters());
			}

		});
		ctx.registerBean(WebServiceControllerSupport.class, SolarQueryApiTestSupport::controllerAdvice);
		ctx.registerBean(WebServiceGlobalControllerSupport.class);

		// the separate resolver the web application gives the authentication entry point
		ctx.registerBean("exceptionHandlerExceptionResolver", ExceptionHandlerExceptionResolver.class,
				() -> {
					final ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
					resolver.setMessageConverters(messageConverters());
					return resolver;
				});
		ctx.registerBean(NodesController.class,
				() -> new NodesController(identityService, historianSelector));
		ctx.refresh();

		final SecurityTokenAuthenticationEntryPoint entryPoint = new SecurityTokenAuthenticationEntryPoint();
		entryPoint.setHandlerExceptionResolver(
				ctx.getBean("exceptionHandlerExceptionResolver", HandlerExceptionResolver.class));

		final SecurityTokenAuthenticationFilter tokenFilter = new SecurityTokenAuthenticationFilter();
		tokenFilter
				.setUserDetailsService(new SecurityTokenServiceUserDetailsService(securityTokenService));
		tokenFilter.setAuthenticationEntryPoint(entryPoint);
		tokenFilter.afterPropertiesSet();

		final ExceptionTranslationFilter exceptionFilter = new ExceptionTranslationFilter(entryPoint);
		exceptionFilter.setAccessDeniedHandler(entryPoint);

		final UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
		corsSource.setCorsConfigurations(appCorsConfigurations());

		final List<Filter> apiFilters = List.of(
				new SecurityContextHolderFilter(new RequestAttributeSecurityContextRepository()),
				new CorsFilter(corsSource), tokenFilter, new AnonymousAuthenticationFilter("anonymous"),
				exceptionFilter, new AuthorizationFilter(AuthorityAuthorizationManager.hasRole("USER")));
		final HandlerMappingIntrospector introspector = ctx.getBean(HandlerMappingIntrospector.class);
		final FilterChainProxy securityFilter = new FilterChainProxy(List.of(
				new DefaultSecurityFilterChain(new MvcRequestMatcher(introspector, "/api/v1/sec/**"),
						apiFilters),
				new DefaultSecurityFilterChain(
						new MvcRequestMatcher(introspector, "/solarquery/api/v1/sec/**"), apiFilters),
				new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE)));
		securityFilter.afterPropertiesSet();

		mvc = MockMvcBuilders.webAppContextSetup(ctx)
				.addFilters(new HistorianSelectorFilter(), securityFilter).build();
	}

	/**
	 * Get the CORS configuration of the web application, from the
	 * {@code <mvc:cors>} element of its MVC context.
	 *
	 * <p>
	 * The rest of the MVC context needs the OSGi runtime, so every other
	 * element is removed before the context is loaded.
	 * </p>
	 *
	 * @return the CORS configuration, mapped by path pattern
	 * @throws Exception
	 *         if the MVC context can not be loaded
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, CorsConfiguration> appCorsConfigurations() throws Exception {
		final FileSystemResource resource = new FileSystemResource(APP_MVC_CONTEXT);
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		final Document doc;
		try (InputStream in = resource.getInputStream()) {
			doc = factory.newDocumentBuilder().parse(in);
		}
		final Element root = doc.getDocumentElement();
		Node next;
		for ( Node node = root.getFirstChild(); node != null; node = next ) {
			next = node.getNextSibling();
			if ( !(MVC_NS.equals(node.getNamespaceURI()) && "cors".equals(node.getLocalName())) ) {
				root.removeChild(node);
			}
		}
		try (GenericApplicationContext corsCtx = new GenericApplicationContext()) {
			new XmlBeanDefinitionReader(corsCtx).registerBeanDefinitions(doc, resource);
			corsCtx.refresh();
			return (Map<String, CorsConfiguration>) corsCtx.getBean(MVC_CORS_BEAN_NAME);
		}
	}

	@After
	public void teardown() {
		SecurityContextHolder.clearContext();
		ctx.close();
	}

	/**
	 * Set up a request as the container does for the servlet mapped to
	 * {@code /*}.
	 */
	private static RequestPostProcessor servletMapping() {
		return request -> {
			request.setHttpServletMapping(new MockHttpServletMapping(
					request.getRequestURI().substring(1), "/*", "setup", MappingMatch.PATH));
			return request;
		};
	}

	private static MockHttpServletRequestBuilder unsignedGet(String path) {
		return get(path).accept(MediaType.APPLICATION_JSON).with(servletMapping());
	}

	/**
	 * Create a GET request signed with the SNWS2 scheme, covering the same
	 * headers as the Grafana data source does.
	 */
	private static MockHttpServletRequestBuilder signedGet(String path, String signedPath, String secret,
			Map<String, String> params) {
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(TOKEN_ID).useSnDate(true)
				.date(Instant.now()).host("localhost").method("GET").path(signedPath)
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).queryParams(params);
		final MockHttpServletRequestBuilder req = unsignedGet(path)
				.header(HttpHeaders.AUTHORIZATION, auth.build(secret))
				.header(AuthorizationUtils.SN_DATE_HEADER,
						auth.headerValue(AuthorizationUtils.SN_DATE_HEADER));
		params.forEach((name, value) -> req.param(name, value));
		return req;
	}

	private void givenToken() {
		given(securityTokenService.tokenForId(TOKEN_ID))
				.willReturn(new SecurityToken(TOKEN_ID, TOKEN_SECRET));
	}

	private void givenSource(OptionalService<FilterableDatumHistorian> service,
			FilterableDatumHistorian historian, String sourceId) {
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(service.service()).willReturn(historian);
		given(historian.findDatumStreamMetadata(any()))
				.willReturn(List.of(nodeMeta(NODE_ID, sourceId, new String[] { "a" }, null, null)));
	}

	@Test
	public void signed() throws Exception {
		// GIVEN
		givenToken();
		givenSource(filteredService, filtered, "f");

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(signedGet(SOURCES_PATH, SOURCES_PATH, TOKEN_SECRET, Map.of())).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("Signed request allowed").isEqualTo(200);
		and.then(res.getContentAsString()).as("Sources of filtered historian provided").isEqualTo("""
				{"success":true,"data":[{"nodeId":123,"sourceId":"f"}]}""");
	}

	@Test
	public void unsigned() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(unsignedGet(SOURCES_PATH)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("Unsigned request not authorized").isEqualTo(401);
		and.then(res.getHeader(HttpHeaders.WWW_AUTHENTICATE)).as("Authentication scheme provided")
				.isEqualTo("SNWS2");
		and.then(res.getContentType()).as("Error result in requested format")
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getContentAsString()).as("Error result provided by exception handler")
				.isEqualTo(UNAUTHENTICATED_RESULT_JSON);
		then(identityService).shouldHaveNoInteractions();
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void unsigned_cbor() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(SOURCES_PATH).accept(MediaType.APPLICATION_CBOR).with(servletMapping()))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Unsigned request not authorized").isEqualTo(401);
		and.then(res.getContentType()).as("Error result in requested format")
				.isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray())).as("Error result provided as CBOR")
				.isEqualTo(UNAUTHENTICATED_RESULT_JSON);
		then(identityService).shouldHaveNoInteractions();
	}

	@Test
	public void unsigned_noAccept() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(SOURCES_PATH).with(servletMapping()))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Unsigned request not authorized").isEqualTo(401);
		and.then(res.getContentType()).as("Error result in default format")
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getContentAsString()).as("Error result provided by exception handler")
				.isEqualTo(UNAUTHENTICATED_RESULT_JSON);
		then(identityService).shouldHaveNoInteractions();
	}

	@Test
	public void unsigned_unsupportedAccept() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(SOURCES_PATH).accept(MediaType.TEXT_HTML).with(servletMapping()))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Unsigned request not authorized").isEqualTo(401);
		and.then(res.getContentType()).as("Error result in default format")
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getContentAsString()).as("Error result provided by entry point")
				.startsWith("{\"success\":false,\"code\":\"401\"");
	}

	@Test
	public void signed_badSecret() throws Exception {
		// GIVEN
		givenToken();

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(signedGet(SOURCES_PATH, SOURCES_PATH, "not the secret", Map.of())).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("Request signed with wrong secret not authorized").isEqualTo(401);
		and.then(res.getContentAsString()).as("Error result provided by exception handler").isEqualTo("""
				{"success":false,"message":"Bad credentials"}""");
		then(identityService).shouldHaveNoInteractions();
		then(filteredService).shouldHaveNoInteractions();
	}

	/**
	 * Create a CORS preflight request for a GET request signed by a browser
	 * client.
	 */
	private static MockHttpServletRequestBuilder preflight(String path) {
		return options(path).header(HttpHeaders.ORIGIN, OTHER_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,x-sn-date")
				.with(servletMapping());
	}

	private static void thenPreflightAllowedWithoutCredentials(MockHttpServletResponse res) {
		and.then(res.getStatus()).as("Preflight request allowed").isEqualTo(200);
		and.then(res.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).as("Any origin allowed")
				.isEqualTo("*");
		and.then(res.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).as("Signature headers allowed")
				.isEqualTo("authorization, x-sn-date");
		and.then(res.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
				.as("Credentials not allowed").isNull();
	}

	@Test
	public void cors_preflight() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(preflight(SOURCES_PATH)).andReturn()
				.getResponse();

		// THEN
		thenPreflightAllowedWithoutCredentials(res);
	}

	@Test
	public void cors_preflight_apiPath() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(preflight(API_SOURCES_PATH)).andReturn()
				.getResponse();

		// THEN
		thenPreflightAllowedWithoutCredentials(res);
	}

	@Test
	public void cors_preflight_unfiltered() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(preflight(UNFILTERED_SOURCES_PATH)).andReturn()
				.getResponse();

		// THEN
		thenPreflightAllowedWithoutCredentials(res);
	}

	@Test
	public void cors_signed() throws Exception {
		// GIVEN
		givenToken();
		givenSource(filteredService, filtered, "f");

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(signedGet(SOURCES_PATH, SOURCES_PATH, TOKEN_SECRET, Map.of())
						.header(HttpHeaders.ORIGIN, OTHER_ORIGIN))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Signed request allowed").isEqualTo(200);
		and.then(res.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).as("Any origin allowed")
				.isEqualTo("*");
		and.then(res.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
				.as("Credentials not allowed").isNull();
	}

	@Test
	public void cors_unsigned() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(unsignedGet(SOURCES_PATH).header(HttpHeaders.ORIGIN, OTHER_ORIGIN)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("Unsigned request not authorized").isEqualTo(401);
		and.then(res.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.as("Any origin allowed to read the error").isEqualTo("*");
		and.then(res.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
				.as("Credentials not allowed").isNull();
	}

	@Test
	public void unfiltered_signedWithoutPrefix() throws Exception {
		// GIVEN
		givenToken();
		givenSource(unfilteredService, unfiltered, "u");

		// WHEN
		final MockHttpServletResponse res = mvc.perform(signedGet(UNFILTERED_SOURCES_PATH, SOURCES_PATH,
				TOKEN_SECRET, Map.of("nodeIds", NODE_ID.toString()))).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Request signed without the prefix allowed").isEqualTo(200);
		and.then(res.getContentAsString()).as("Sources of unfiltered historian provided").isEqualTo("""
				{"success":true,"data":[{"nodeId":123,"sourceId":"u"}]}""");
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void unfiltered_unsigned() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(unsignedGet(UNFILTERED_SOURCES_PATH)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("Unsigned request with the prefix not authorized").isEqualTo(401);
		and.then(res.getHeader(HttpHeaders.WWW_AUTHENTICATE)).as("Authentication scheme provided")
				.isEqualTo("SNWS2");
		then(identityService).shouldHaveNoInteractions();
		then(unfilteredService).shouldHaveNoInteractions();
	}

	@Test
	public void unfiltered_signedWithPrefix() throws Exception {
		// GIVEN
		givenToken();

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				signedGet(UNFILTERED_SOURCES_PATH, UNFILTERED_SOURCES_PATH, TOKEN_SECRET, Map.of()))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Request signed with the prefix not authorized").isEqualTo(401);
		then(identityService).shouldHaveNoInteractions();
		then(unfilteredService).shouldHaveNoInteractions();
	}

}
