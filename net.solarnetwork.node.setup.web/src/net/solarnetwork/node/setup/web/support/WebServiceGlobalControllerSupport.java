/* ==================================================================
 * WebServiceGlobalControllerSupport.java - 14 Nov 2026 6:52:48 am
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

package net.solarnetwork.node.setup.web.support;

import static net.solarnetwork.node.setup.web.support.WebServiceControllerSupport.requestDescription;
import static net.solarnetwork.node.setup.web.support.WebServiceControllerSupport.userPrincipalName;
import static org.springframework.http.MediaType.APPLICATION_CBOR;
import static org.springframework.http.MediaType.APPLICATION_CBOR_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Web service exception handling for all controllers, and for exceptions
 * resolved without a controller.
 *
 * <p>
 * Unlike {@link WebServiceControllerSupport}, this advice has no controller
 * selectors, so it also applies when an exception is resolved without a
 * controller. The
 * {@link net.solarnetwork.web.jakarta.security.SecurityTokenAuthenticationEntryPoint}
 * resolves authentication and access errors that way, so their error results
 * are written as JSON or CBOR, as the client asks, with JSON preferred. The
 * handlers do not apply to requests that accept neither, so the entry point
 * handles those errors itself. The advice has a low precedence, so the
 * exception handlers of controllers and of other advice take precedence over
 * its handlers.
 * </p>
 *
 * <p>
 * The advice also skips writing the response body of any exception handler once
 * the response has been committed, as the error can not be passed to the client
 * by then.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.4
 */
@RestControllerAdvice
@Order(1000)
public final class WebServiceGlobalControllerSupport implements ResponseBodyAdvice<Object> {

	private static final Logger log = LoggerFactory.getLogger(WebServiceGlobalControllerSupport.class);

	/**
	 * The media types the exception handlers produce, in order of preference.
	 */
	private static final Set<MediaType> PRODUCIBLE_TYPES = Collections
			.unmodifiableSet(new LinkedHashSet<>(List.of(APPLICATION_JSON, APPLICATION_CBOR)));

	/**
	 * Constructor.
	 */
	public WebServiceGlobalControllerSupport() {
		super();
	}

	/**
	 * Handle an {@link AuthenticationException}.
	 *
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 */
	@ExceptionHandler(value = AuthenticationException.class,
			produces = { APPLICATION_JSON_VALUE, APPLICATION_CBOR_VALUE })
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Response<?> handleAuthenticationException(AuthenticationException e, WebRequest request) {
		log.debug("AuthenticationException in request {}: {}", requestDescription(request),
				e.getMessage());
		preferJson(request);
		return new Response<Object>(Boolean.FALSE, null, e.getMessage(), null);
	}

	/**
	 * Handle an {@link AccessDeniedException}.
	 *
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @return an error response object
	 */
	@ExceptionHandler(value = AccessDeniedException.class,
			produces = { APPLICATION_JSON_VALUE, APPLICATION_CBOR_VALUE })
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public Response<?> handleAccessDeniedException(AccessDeniedException e, WebRequest request) {
		log.debug("AccessDeniedException in request {}; user [{}]: {}", requestDescription(request),
				userPrincipalName(request), e.getMessage());
		preferJson(request);
		return new Response<Object>(Boolean.FALSE, null, e.getMessage(), null);
	}

	/**
	 * Prefer JSON to CBOR when the client accepts both equally.
	 *
	 * <p>
	 * Spring passes the {@link ExceptionHandler#produces()} media types on to
	 * the message converters in an unordered set, in which CBOR comes first, so
	 * a client that accepts any media type, or sends no {@code Accept} header,
	 * would be sent CBOR. Replacing them with an ordered set makes JSON the
	 * default, as it is for the other API results.
	 * </p>
	 *
	 * @param request
	 *        the request
	 */
	private static void preferJson(WebRequest request) {
		request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, PRODUCIBLE_TYPES,
				RequestAttributes.SCOPE_REQUEST);
	}

	/**
	 * Apply to {@link ExceptionHandler} methods only.
	 */
	@Override
	public boolean supports(MethodParameter returnType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		return returnType.hasMethodAnnotation(ExceptionHandler.class);
	}

	/**
	 * Skip writing an {@link ExceptionHandler} response body if the response
	 * has already been committed.
	 *
	 * <p>
	 * The response is committed when an exception is thrown after a controller
	 * has started writing its response. The error can not be passed to the
	 * client by then, and the error result would only be added to the end of
	 * the partial response.
	 * </p>
	 */
	@Override
	public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType,
			MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		if ( response instanceof ServletServerHttpResponse servletResponse
				&& servletResponse.getServletResponse().isCommitted() ) {
			log.debug("Response committed so error can not be passed to client in request {}: {}",
					request.getURI(), body);
			return null;
		}
		return body;
	}

}
