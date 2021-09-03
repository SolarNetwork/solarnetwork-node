/* ==================================================================
 * HttpClientSupport.java - Nov 19, 2013 3:46:57 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.support;

import static net.solarnetwork.web.security.AuthorizationV2Builder.httpDate;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.support.SSLService;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.web.security.AuthorizationV2Builder;

/**
 * Supporting methods for HTTP client operations.
 * 
 * @author matt
 * @version 1.5
 */
public abstract class HttpClientSupport extends BaseIdentifiable {

	/** A HTTP Accept header value for any text type. */
	public static final String ACCEPT_TEXT = "text/*";

	/** A HTTP Accept header value for a JSON type. */
	public static final String ACCEPT_JSON = "application/json,text/json";

	/** The default value for the {@code connectionTimeout} property. */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 55000;

	/** The HTTP method GET. */
	public static final String HTTP_METHOD_GET = "GET";

	/** The HTTP method POST. */
	public static final String HTTP_METHOD_POST = "POST";

	private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
	private IdentityService identityService = null;
	private OptionalService<SSLService> sslService = null;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get an InputStream from a URLConnection response, handling compression.
	 * 
	 * <p>
	 * This method handles decompressing the response if the encoding is set to
	 * {@code gzip} or {@code deflate}.
	 * </p>
	 * 
	 * @param conn
	 *        the URLConnection
	 * @return the InputStream
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected InputStream getInputStreamFromURLConnection(URLConnection conn) throws IOException {
		String enc = conn.getContentEncoding();
		String type = conn.getContentType();

		if ( conn instanceof HttpURLConnection ) {
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			if ( httpConn.getResponseCode() < 200 || httpConn.getResponseCode() > 299 ) {
				log.info("Non-200 HTTP response from {}: {}", conn.getURL(), httpConn.getResponseCode());
			}
		}

		log.trace("Got content type [{}] encoded as [{}]", type, enc);

		InputStream is = conn.getInputStream();
		if ( "gzip".equalsIgnoreCase(enc) ) {
			is = new GZIPInputStream(is);
		} else if ( "deflate".equalsIgnoreCase("enc") ) {
			is = new DeflaterInputStream(is);
		}
		return is;
	}

	/**
	 * Get a Reader for a Unicode encoded URL connection response.
	 * 
	 * <p>
	 * This calls {@link #getInputStreamFromURLConnection(URLConnection)} so
	 * compressed responses are handled appropriately.
	 * </p>
	 * 
	 * @param conn
	 *        the URLConnection
	 * @return the Reader
	 * @throws IOException
	 *         if an IO error occurs
	 */
	protected Reader getUnicodeReaderFromURLConnection(URLConnection conn) throws IOException {
		return new BufferedReader(new UnicodeReader(getInputStreamFromURLConnection(conn), null));
	}

	/**
	 * Get a URLConnection for a specific URL and HTTP method.
	 * 
	 * <p>
	 * This defaults to the {@link #ACCEPT_TEXT} accept value.
	 * </p>
	 * 
	 * @param url
	 *        the URL to connect to
	 * @param httpMethod
	 *        the HTTP method
	 * @return the URLConnection
	 * @throws IOException
	 *         if any IO error occurs
	 * @see #getURLConnection(String, String, String)
	 */
	protected URLConnection getURLConnection(String url, String httpMethod) throws IOException {
		return getURLConnection(url, httpMethod, "text/*");
	}

	/**
	 * Get a URLConnection for a specific URL and HTTP method.
	 * 
	 * <p>
	 * If the httpMethod equals {@code POST} then the connection's
	 * {@code doOutput} property will be set to <em>true</em>, otherwise it will
	 * be set to <em>false</em>. The {@code doInput} property is always set to
	 * <em>true</em>.
	 * </p>
	 * 
	 * <p>
	 * This method also sets up the request property
	 * {@code Accept-Encoding: gzip,deflate} so the response can be compressed.
	 * The {@link #getInputStreamFromURLConnection(URLConnection)} automatically
	 * handles compressed responses.
	 * </p>
	 * 
	 * <p>
	 * If the {@link #getSslService()} property is configured and the URL
	 * represents an HTTPS connection, then that factory will be used to for the
	 * connection.
	 * </p>
	 * 
	 * @param url
	 *        the URL to connect to
	 * @param httpMethod
	 *        the HTTP method
	 * @param accept
	 *        the HTTP Accept header value
	 * @return the URLConnection
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected URLConnection getURLConnection(String url, String httpMethod, String accept)
			throws IOException {
		return getURLConnection(url, httpMethod, accept, null);
	}

	/**
	 * Get a URLConnection for a specific URL and HTTP method.
	 * 
	 * <p>
	 * If the httpMethod equals {@code POST} then the connection's
	 * {@code doOutput} property will be set to {@literal true}, otherwise it
	 * will be set to {@literal false}. The {@code doInput} property is always
	 * set to {@literal true}.
	 * </p>
	 * 
	 * <p>
	 * This method also sets up the request property
	 * {@code Accept-Encoding: gzip,deflate} so the response can be compressed.
	 * The {@link #getInputStreamFromURLConnection(URLConnection)} automatically
	 * handles compressed responses.
	 * </p>
	 * 
	 * <p>
	 * If the {@link #getSslService()} property is configured and the URL
	 * represents an HTTPS connection, then that factory will be used to for the
	 * connection.
	 * </p>
	 * 
	 * @param url
	 *        the URL to connect to
	 * @param httpMethod
	 *        the HTTP method
	 * @param accept
	 *        the HTTP Accept header value
	 * @param connectionCustomizer
	 *        an optional consumer to customize the connection before it is
	 *        opened
	 * @return the URLConnection
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.5
	 */
	protected URLConnection getURLConnection(String url, String httpMethod, String accept,
			Consumer<URLConnection> connectionCustomizer) throws IOException {
		URL connUrl = new URL(url);
		URLConnection conn = connUrl.openConnection();
		if ( conn instanceof HttpURLConnection ) {
			HttpURLConnection hConn = (HttpURLConnection) conn;
			hConn.setRequestMethod(httpMethod);
		}
		if ( sslService != null && conn instanceof HttpsURLConnection ) {
			SSLService service = sslService.service();
			if ( service != null ) {
				SSLSocketFactory factory = service.getSSLSocketFactory();
				if ( factory != null ) {
					HttpsURLConnection hConn = (HttpsURLConnection) conn;
					hConn.setSSLSocketFactory(factory);
				}
			}
		}
		conn.setRequestProperty("Accept", accept);
		conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
		conn.setDoInput(true);
		conn.setDoOutput(HTTP_METHOD_POST.equalsIgnoreCase(httpMethod));
		conn.setConnectTimeout(this.connectionTimeout);
		conn.setReadTimeout(connectionTimeout);
		if ( connectionCustomizer != null ) {
			connectionCustomizer.accept(conn);
		}
		return conn;
	}

	/**
	 * Populate SolarNetwork token authorization headers on a
	 * {@code URLConnection}.
	 * 
	 * @param conn
	 *        the connection to populate
	 * @param builder
	 *        the authorization builder
	 * @param requestDate
	 *        the request date
	 * @param headers
	 *        any additional headers to populate on the request
	 * @since 1.5
	 */
	public void setupTokenAuthorization(URLConnection conn, AuthorizationV2Builder builder,
			Date requestDate, Map<String, List<String>> headers) {
		if ( headers != null ) {
			for ( Map.Entry<String, List<String>> me : headers.entrySet() ) {
				boolean first = true;
				for ( String v : me.getValue() ) {
					if ( first ) {
						conn.setRequestProperty(me.getKey(), v);
					} else {
						conn.addRequestProperty(me.getKey(), v);
					}
				}
			}
		}
		builder.date(requestDate);

		URL url = conn.getURL();
		String host = url.getHost();
		int port = url.getPort();
		if ( port != 80 && port != -1 ) {
			host += ":" + port;
		}
		builder.host(host).path(url.getPath());

		if ( log.isTraceEnabled() ) {
			log.trace("Canonical request data: {}", builder.buildCanonicalRequestData());
		}

		conn.setRequestProperty("Date", httpDate(requestDate));
		conn.setRequestProperty("Authorization", builder.build());
	}

	/**
	 * Append a URL-escaped key/value pair to a string buffer.
	 * 
	 * @param buf
	 *        the buffer to append to
	 * @param key
	 *        the parameter key
	 * @param value
	 *        the parameter value
	 */
	protected void appendXWWWFormURLEncodedValue(StringBuilder buf, String key, Object value) {
		if ( value == null ) {
			return;
		}
		if ( buf.length() > 0 ) {
			buf.append('&');
		}
		try {
			buf.append(URLEncoder.encode(key, "UTF-8")).append('=')
					.append(URLEncoder.encode(value.toString(), "UTF-8"));
		} catch ( UnsupportedEncodingException e ) {
			// should not get here ever
			throw new RuntimeException(e);
		}
	}

	/**
	 * Encode a map of data into a string suitable for posting to a web server
	 * as the content type {@code application/x-www-form-urlencoded}. Arrays and
	 * Collections of values are supported as well.
	 * 
	 * @param data
	 *        the map of data to encode
	 * @return the encoded data, or an empty string if nothing to encode
	 */
	protected String xWWWFormURLEncoded(Map<String, ?> data) {
		if ( data == null || data.size() < 0 ) {
			return "";
		}
		StringBuilder buf = new StringBuilder();
		for ( Map.Entry<String, ?> me : data.entrySet() ) {
			String key;
			try {
				key = URLEncoder.encode(me.getKey(), "UTF-8");
			} catch ( UnsupportedEncodingException e ) {
				// should not get here ever
				throw new RuntimeException(e);
			}
			Object val = me.getValue();
			if ( val instanceof Collection<?> ) {
				for ( Object colVal : (Collection<?>) val ) {
					appendXWWWFormURLEncodedValue(buf, key, colVal);
				}
			} else if ( val.getClass().isArray() ) {
				for ( Object arrayVal : (Object[]) val ) {
					appendXWWWFormURLEncodedValue(buf, key, arrayVal);
				}
			} else {
				appendXWWWFormURLEncodedValue(buf, key, val);
			}
		}
		return buf.toString();
	}

	/**
	 * HTTP POST data as {@code application/x-www-form-urlencoded} (e.g. a web
	 * form) to a URL.
	 * 
	 * @param url
	 *        the URL to post to
	 * @param accept
	 *        the value to use for the Accept HTTP header
	 * @param data
	 *        the data to encode and send as the body of the HTTP POST
	 * @return the URLConnection after the post data has been sent
	 * @throws IOException
	 *         if any IO error occurs
	 * @throws RuntimeException
	 *         if the HTTP response code is not within the 200 - 299 range
	 */
	protected URLConnection postXWWWFormURLEncodedData(String url, String accept, Map<String, ?> data)
			throws IOException {
		URLConnection conn = getURLConnection(url, HTTP_METHOD_POST, accept);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		String body = xWWWFormURLEncoded(data);
		log.trace("Encoded HTTP POST data {} for {} as {}", data, url, body);
		OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
		FileCopyUtils.copy(new StringReader(body), out);
		if ( conn instanceof HttpURLConnection ) {
			HttpURLConnection http = (HttpURLConnection) conn;
			int status = http.getResponseCode();
			if ( status < 200 || status > 299 ) {
				throw new RuntimeException("HTTP result status not in the 200-299 range: "
						+ http.getResponseCode() + " " + http.getResponseMessage());
			}
		}
		return conn;
	}

	/**
	 * HTTP POST data as {@code application/x-www-form-urlencoded} (e.g. a web
	 * form) to a URL and return the response body as a string.
	 * 
	 * @param url
	 *        the URL to post to
	 * @param data
	 *        the data to encode and send as the body of the HTTP POST
	 * @return the response body as a String
	 * @throws IOException
	 *         if any IO error occurs
	 * @throws RuntimeException
	 *         if the HTTP response code is not within the 200 - 299 range
	 * @see #postXWWWFormURLEncodedData(String, String, Map)
	 */
	protected String postXWWWFormURLEncodedDataForString(String url, Map<String, ?> data)
			throws IOException {
		URLConnection conn = postXWWWFormURLEncodedData(url, "text/*, application/json", data);
		return FileCopyUtils.copyToString(getUnicodeReaderFromURLConnection(conn));
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public IdentityService getIdentityService() {
		return identityService;
	}

	public void setIdentityService(IdentityService identityService) {
		this.identityService = identityService;
	}

	public OptionalService<SSLService> getSslService() {
		return sslService;
	}

	public void setSslService(OptionalService<SSLService> sslService) {
		this.sslService = sslService;
	}

	@Override
	public String getUID() {
		return getUid();
	}

	@Override
	public String getUid() {
		return super.getUid();
	}

	@Override
	public void setUid(String uid) {
		super.setUid(uid);
	}

	@Override
	public String getGroupUID() {
		return super.getGroupUID();
	}

	@Override
	public void setGroupUID(String groupUID) {
		super.setGroupUID(groupUID);
	}

}
