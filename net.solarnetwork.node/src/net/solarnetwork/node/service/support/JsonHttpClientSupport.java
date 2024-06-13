/* ==================================================================
 * JsonHttpClientSupport.java - Oct 6, 2014 12:35:03 PM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.service.RemoteServiceException;

/**
 * An abstract class to support HTTP based services that use JSON.
 *
 * <p>
 * The configurable properties of this class are:
 * </p>
 *
 * <dl class="class-properties">
 * <dt>compress</dt>
 * <dd>Flag to compress the HTTP body content, defaults to
 * {@literal false}.</dd>
 *
 * <dt>objectMapper</dt>
 * <dd>The {@link ObjectMapper} to marshall/unmarshall objects to/from JSON
 * with.</dd>
 * </dl>
 *
 * @author matt
 * @version 1.4
 */
public abstract class JsonHttpClientSupport extends HttpClientSupport {

	/** The JSON MIME type. */
	public static final String JSON_MIME_TYPE = "application/json";

	private ObjectMapper objectMapper;
	private boolean compress = false;

	/**
	 * Default constructor.
	 */
	public JsonHttpClientSupport() {
		super();
	}

	/**
	 * Perform a JSON HTTP request.
	 *
	 * @param url
	 *        the URL to make the request to
	 * @param method
	 *        the HTTP method, e.g. {@link HttpClientSupport#HTTP_METHOD_GET}
	 * @param data
	 *        the optional data to marshall to JSON and upload as the request
	 *        content
	 * @return the InputStream for the HTTP response
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected final InputStream doJson(String url, String method, Object data) throws IOException {
		return doJson(url, method, data, null);
	}

	/**
	 * Perform a JSON HTTP request.
	 *
	 * @param url
	 *        the URL to make the request to
	 * @param method
	 *        the HTTP method, e.g. {@link HttpClientSupport#HTTP_METHOD_GET}
	 * @param data
	 *        the optional data to marshall to JSON and upload as the request
	 *        content
	 * @param connectionCustomizer
	 *        an optional consumer to customize the created URL connection,
	 *        before it is opened
	 * @return the InputStream for the HTTP response
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.3
	 */
	protected final InputStream doJson(String url, String method, Object data,
			Consumer<URLConnection> connectionCustomizer) throws IOException {
		URLConnection conn = getURLConnection(url, method, JSON_MIME_TYPE, connectionCustomizer);
		if ( data != null ) {
			conn.setRequestProperty("Content-Type", JSON_MIME_TYPE + ";charset=UTF-8");
			if ( compress ) {
				conn.setRequestProperty("Content-Encoding", "gzip");
			}
			OutputStream out = conn.getOutputStream();
			if ( compress ) {
				out = new GZIPOutputStream(out);
			}

			if ( log.isDebugEnabled() ) {
				log.debug("Posting JSON data: {}",
						objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
			}
			objectMapper.writeValue(out, data);
			out.flush();
			out.close();
		}

		return getInputStreamFromURLConnection(conn);
	}

	/**
	 * Perform a JSON GET HTTP request.
	 *
	 * @param url
	 *        the URL to GET
	 * @return the HTTP response InputStream
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected final InputStream jsonGET(String url) throws IOException {
		return doJson(url, HTTP_METHOD_GET, null, null);
	}

	/**
	 * Perform a JSON GET HTTP request.
	 *
	 * @param url
	 *        the URL to GET
	 * @param connectionCustomizer
	 *        an optional consumer to customize the created URL connection,
	 *        before it is opened
	 * @return the HTTP response InputStream
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.3
	 */
	protected final InputStream jsonGET(String url, Consumer<URLConnection> connectionCustomizer)
			throws IOException {
		return doJson(url, HTTP_METHOD_GET, null, connectionCustomizer);
	}

	/**
	 * Perform a JSON POST HTTP request.
	 *
	 * @param url
	 *        the URL to POST
	 * @param data
	 *        the object to marshall into JSON
	 * @return the HTTP response InputStream
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected final InputStream jsonPOST(String url, Object data) throws IOException {
		return doJson(url, HTTP_METHOD_POST, data);
	}

	/**
	 * Perform a JSON POST HTTP request.
	 *
	 * @param url
	 *        the URL to POST
	 * @param data
	 *        the object to marshall into JSON
	 * @param connectionCustomizer
	 *        an optional consumer to customize the created URL connection,
	 *        before it is opened
	 * @return the HTTP response InputStream
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.3
	 */
	protected final InputStream jsonPOST(String url, Object data,
			Consumer<URLConnection> connectionCustomizer) throws IOException {
		return doJson(url, HTTP_METHOD_POST, data, connectionCustomizer);
	}

	/**
	 * Parse a standard {@code Response} HTTP response and return the
	 * {@code data} object as the provided type.
	 *
	 * @param <T>
	 *        the element type
	 * @param in
	 *        the InputStream to read, which will be closed before returning
	 *        from this method
	 * @param dataType
	 *        the type of object to extract from the response
	 * @return the extracted object, or {@literal null}
	 * @throws RemoteServiceException
	 *         if the response does not include the success flag
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected <T> T extractResponseData(InputStream in, Class<T> dataType)
			throws RemoteServiceException, IOException {
		final ObjectMapper mapper = getObjectMapper();
		if ( mapper == null ) {
			throw new RuntimeException(
					"No ObjectMapper configured for extracting JSON response data with.");
		}
		try {
			JsonNode root = mapper.readTree(in);
			if ( root.isObject() ) {
				JsonNode child = root.get("success");
				if ( child != null && child.asBoolean() ) {
					child = root.get("data");
					if ( child != null ) {
						return objectMapper.treeToValue(child, dataType);
					}
					log.debug("Server returned no data for request.");
					return null;
				}
			}
			throw new RemoteServiceException("Server response not successful: "
					+ (root.get("message") == null ? "(no message)" : root.get("message").asText()));
		} finally {
			if ( in != null ) {
				in.close();
			}
		}
	}

	/**
	 * Parse a standard {@code Response} HTTP response and return the
	 * {@code data} array as a collection of objects of the provided type.
	 *
	 * @param <T>
	 *        the collection element type
	 * @param in
	 *        the InputStream to read, which will be closed before returning
	 *        from this method
	 * @param dataType
	 *        the type of object to extract from the response
	 * @return the extracted object, or {@literal null}
	 * @throws RemoteServiceException
	 *         if the response does not include the success flag
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	protected <T> Collection<T> extractCollectionResponseData(InputStream in, Class<T> dataType)
			throws RemoteServiceException, IOException {
		try {
			JsonNode root = getObjectMapper().readTree(in);
			if ( root.isObject() ) {
				JsonNode child = root.get("success");
				if ( child != null && child.asBoolean() ) {
					child = root.get("data");
					if ( child != null && child.isArray() ) {
						Iterator<JsonNode> children = child.iterator();
						List<T> result = new ArrayList<T>();
						while ( children.hasNext() ) {
							child = children.next();
							result.add(objectMapper.treeToValue(child, dataType));
						}
						return result;
					}
					log.debug("Server returned no data for request.");
					return null;
				}
			}
			throw new RemoteServiceException(
					"Server response not successful: " + root.get("message") == null ? "(no message)"
							: root.get("message").asText());
		} finally {
			if ( in != null ) {
				in.close();
			}
		}
	}

	/**
	 * Parse a standard {@code Response} HTTP response and return the
	 * {@code data.results} array as objects of the provided type.
	 *
	 * @param <T>
	 *        the collection element type
	 * @param in
	 *        the InputStream to read, which will be closed before returning
	 *        from this method
	 * @param dataType
	 *        the type of object to extract from the response
	 * @return the extracted objects, or {@literal null}
	 * @throws RemoteServiceException
	 *         if the response does not include the success flag
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.2
	 */
	protected <T> Collection<T> extractFilterResultsCollectionResponseData(InputStream in,
			Class<T> dataType) throws RemoteServiceException, IOException {
		try {
			JsonNode root = getObjectMapper().readTree(in);
			if ( root.isObject() ) {
				JsonNode child = root.get("success");
				if ( child != null && child.asBoolean() ) {
					child = root.get("data");
					if ( child != null ) {
						child = child.get("results");
					}
					if ( child != null && child.isArray() ) {
						Iterator<JsonNode> children = child.iterator();
						List<T> result = new ArrayList<T>();
						while ( children.hasNext() ) {
							child = children.next();
							result.add(objectMapper.treeToValue(child, dataType));
						}
						return result;
					}
					log.debug("Server returned no data for request.");
					return null;
				}
			}
			throw new RemoteServiceException(
					"Server response not successful: " + root.get("message") == null ? "(no message)"
							: root.get("message").asText());
		} finally {
			if ( in != null ) {
				in.close();
			}
		}
	}

	/**
	 * Test for a successful standard {@code Response} HTTP response.
	 *
	 * @param in
	 *        the InputStream to read, which will be closed before returning
	 *        from this method
	 * @throws RemoteServiceException
	 *         if the response does not include the success flag
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected void verifyResponseSuccess(InputStream in) throws RemoteServiceException, IOException {
		try {
			JsonNode root = getObjectMapper().readTree(in);
			if ( root.isObject() ) {
				JsonNode child = root.get("success");
				if ( child != null && child.asBoolean() ) {
					return;
				}
			}
			throw new RemoteServiceException("Server response not successful: "
					+ (root.get("message") == null ? "(no message)" : root.get("message").asText()));
		} finally {
			if ( in != null ) {
				in.close();
			}
		}
	}

	/**
	 * Get the JSON mapper.
	 *
	 * @return the mapper
	 */
	public final ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * Set the JSON mapper.
	 *
	 * @param objectMapper
	 *        the mapper
	 */
	public final void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Get the compress flag.
	 *
	 * @return {@literal true} to compress HTTP body content with gzip
	 */
	public final boolean isCompress() {
		return compress;
	}

	/**
	 * Set the compress flag.
	 *
	 * @param compress
	 *        {@literal true} to compress HTTP body content with gzip
	 */
	public final void setCompress(boolean compress) {
		this.compress = compress;
	}

}
