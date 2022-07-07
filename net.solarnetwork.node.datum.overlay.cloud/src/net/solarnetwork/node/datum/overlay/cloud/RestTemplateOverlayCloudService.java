/* ==================================================================
 * RestTemplateOverlayCloudService.java - 5/07/2022 7:41:30 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.overlay.cloud;

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Implementation of {@link OverlayCloudService} using {@link RestTemplate}.
 * 
 * @author matt
 * @version 1.0
 */
public class RestTemplateOverlayCloudService
		implements ConfigurableOverlayCloudService, ClientHttpRequestInterceptor {

	/** The {@code baseUrl} property default value. */
	public static final String DEFAULT_BASE_URL = "http://14.1.52.251";

	private final RestOperations restOps;
	private String baseUrl = DEFAULT_BASE_URL;
	private String username;
	private String password;

	/**
	 * Constructor.
	 */
	public RestTemplateOverlayCloudService() {
		this(new RestTemplate(), JsonUtils.newObjectMapper());
	}

	/**
	 * Constructor.
	 * 
	 * @param restTemplate
	 *        the REST template to use
	 * @param mapper
	 *        the JSON mapper to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public RestTemplateOverlayCloudService(RestTemplate restTemplate, ObjectMapper mapper) {
		super();
		this.restOps = requireNonNullArgument(restTemplate, "restTemplate");
		setupRestTemplateInterceptors(restTemplate, requireNonNullArgument(mapper, "mapper"));
	}

	private void setupRestTemplateInterceptors(RestTemplate restTemplate, ObjectMapper mapper) {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		if ( restTemplate.getInterceptors() != null ) {
			interceptors.addAll(restTemplate.getInterceptors());
		}
		for ( HttpMessageConverter<?> converter : restTemplate.getMessageConverters() ) {
			if ( converter instanceof MappingJackson2HttpMessageConverter ) {
				MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
				jsonConverter.setObjectMapper(mapper);
				break;
			}
		}
		if ( !interceptors.contains(this) ) {
			interceptors.add(this);
		}
		restTemplate.setInterceptors(interceptors);
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		final String username = this.username;
		final String password = this.password;
		if ( username != null && password != null && !username.isEmpty() && !password.isEmpty() ) {
			HttpHeaders headers = request.getHeaders();
			if ( !headers.containsKey(HttpHeaders.AUTHORIZATION) ) {
				String token = Base64Utils.encodeToString(
						(this.username + ":" + this.password).getBytes(StandardCharsets.ISO_8859_1));
				headers.add("Authorization", "Basic " + token);
			}
		}
		return execution.execute(request, body);
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers(String prefix) {
		prefix = (prefix != null ? prefix : "");
		List<SettingSpecifier> results = new ArrayList<>(3);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "baseUrl", DEFAULT_BASE_URL));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "username", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "password", null, true));
		return results;
	}

	@Override
	public List<Grid> getGrids() {
		// @formatter:off
	    final URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
	        .path("/Grid")
	        .build().toUri();
	    // @formatter:on

		try {
			Grid[] grids = restOps.getForObject(uri, Grid[].class);
			return Arrays.asList(grids);
		} catch ( HttpClientErrorException e ) {
			throw new RemoteServiceException(
					format("Non-success response from Overlay request for feed latest [%s]", uri), e);
		} catch ( RuntimeException e ) {
			throw new RemoteServiceException(
					format("Error making Overlay request for feed latest [%s]", uri), e);
		}
	}

	@Override
	public FeedData getFeedLatest(Long gridId, Long feedId) {
		Map<String, Object> pathParams = new HashMap<>(2);
		pathParams.put("gridId", requireNonNullArgument(gridId, "gridId"));
		pathParams.put("feedId", requireNonNullArgument(feedId, "feedId"));
		// @formatter:off
	    final URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
	        .path("/Grid/{gridId}/Feed/{feedId}/Latest")
	        .buildAndExpand(pathParams).toUri();
	    // @formatter:on

		try {
			return restOps.getForObject(uri, FeedData.class);
		} catch ( HttpClientErrorException e ) {
			throw new RemoteServiceException(
					format("Non-success response from Overlay request for feed latest [%s]", uri), e);
		} catch ( RuntimeException e ) {
			throw new RemoteServiceException(
					format("Error making Overlay request for feed latest [%s]", uri), e);
		}
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

}
