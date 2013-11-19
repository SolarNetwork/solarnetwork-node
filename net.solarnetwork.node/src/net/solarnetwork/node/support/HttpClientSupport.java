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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.SSLService;
import net.solarnetwork.util.OptionalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supporting methods for HTTP client operations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class HttpClientSupport {

	/** A HTTP Accept header value for any text type. */
	public static final String ACCEPT_TEXT = "text/*";

	/** A HTTP Accept header value for a JSON type. */
	public static final String ACCEPT_JSON = "application/json,text/json";

	/** The default value for the {@code connectionTimeout} property. */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 15000;

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
	 * The {@link #getInputSourceFromURLConnection(URLConnection)} automatically
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
		URL connUrl = new URL(url);
		URLConnection conn = connUrl.openConnection();
		if ( conn instanceof HttpURLConnection ) {
			HttpURLConnection hConn = (HttpURLConnection) conn;
			hConn.setRequestMethod(httpMethod);
		}
		if ( sslService != null && conn instanceof HttpsURLConnection ) {
			SSLService service = sslService.service();
			if ( service != null ) {
				SSLSocketFactory factory = service.getSolarInSocketFactory();
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
		return conn;
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

}
