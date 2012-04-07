/* ==================================================================
 * UrlDataCollector.java - Dec 9, 2009 9:46:41 AM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.io.url;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.solarnetwork.node.DataCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

/**
 * Implementation of {@link DataCollector} that reads lines of characters from
 * a URL.
 * 
 * <p>This class expects the configured URL to return a character data stream
 * with newline characters separating records of data.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>url</dt>
 *   <dd>The URL to access the character data from. Either this or 
 *   {@code urlFactory} must be configured.</dd>
 *   
 *   <dt>urlFactory</dt>
 *   <dd>A factory for creating URL values dynamically. This allows dynamic
 *   URLs to be used, such as those with the current date in the path. Either
 *   this or {@code url} must be configured. If this is configured it will
 *   take precedence over any configured {@code url} value.</dd>
 *   
 *   <dt>connectionTimeout</dt>
 *   <dd>A timeout to use for connecting to the configured {@code url}.
 *   Defaults to </dd>
 *   
 *   <dt>matchExpression</dt>
 *   <dd>A regular expression to match against lines of data read from the URL.
 *   The collector will ignore all lines of data until this expression matches
 *   a line read from the configured URL. Once a match is found, it will stop
 *   reading any further data. Defaults to 
 *   {@link #DEFAULT_MATCH_EXPRESSION}.</dd>
 *   
 *   <dt>encoding</dt>
 *   <dd>The character encoding to use for reading the URL data. If configured
 *   as <em>null</em> then this class will attempt to use the encoding
 *   specified by the URL connection itself. If the URL connection does not
 *   provide an encoding, {@link #DEFAULT_ENCODING} will be used. Defaults to
 *   <em>null</em>.</dd>
 *   
 *   <dt>skipToLastLine</dt>
 *   <dd>If <em>true</em> then read all available data from the URL and 
 *   return the last line found, as long as it also matches the configured
 *   {@code matchExpression} property. This mode means that the entire URL
 *   data stream must be read each time {@link #collectData()} is called.
 *   Defaults to <em>false</em>.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Id$
 */
public class UrlDataCollector implements DataCollector {
	
	/**
	 * The default {@code encoding} to use if {@code encoding} is not
	 * configured and the URL connection does not specify an encoding. */
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	/** The default value for the {@code connectionTimeout} property. */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 15000;

	/** The default value for the {@code matchExpression} property. */
	public static final String DEFAULT_MATCH_EXPRESSION = "^A";
	
	private String url = null;
	private ObjectFactory<String> urlFactory = null;
	private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
	private String matchExpression = DEFAULT_MATCH_EXPRESSION;
	private String encoding = null;
	private boolean skipToLastLine = false;

	private StringBuilder buffer = null;
	
	private final Logger log = LoggerFactory.getLogger(UrlDataCollector.class);
	
	@Override
	public int bytesRead() {
		String enc = getEncodingToUse();
		try {
			return buffer == null 
					? 0 
					: buffer.toString().getBytes(enc).length;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void collectData() {
		String resolvedUrl = url;
		if ( urlFactory != null ) {
			resolvedUrl = urlFactory.getObject();
		}
		URL dataUrl = null;
		try {
			dataUrl = new URL(resolvedUrl);
		} catch ( MalformedURLException e ) {
			throw new RuntimeException("Bad url configured: " +resolvedUrl);
		}
		if ( log.isDebugEnabled() )  {
			log.debug("Connecting to URL [" +resolvedUrl +']');
		}
		BufferedReader reader = null;
		String data = null;
		String enc = null;
		Pattern pat = Pattern.compile(matchExpression);
		try {
			URLConnection conn = dataUrl.openConnection();
			conn.setConnectTimeout(connectionTimeout);
			conn.setReadTimeout(connectionTimeout);
			conn.setUseCaches(false);
			InputStream in = conn.getInputStream();
			if ( this.encoding == null ) {
				enc = conn.getContentEncoding();
				if ( enc != null ) {
					if ( log.isTraceEnabled() ) {
						log.trace("Using connection encoding [" +enc +']');
					}
					this.encoding = enc;
				}
			}
			if ( enc == null ) {
				enc = getEncodingToUse();
			}
			reader = new BufferedReader(new InputStreamReader(in, enc));
			String lastLine = null;
			boolean keepGoing = true;
			while ( keepGoing ) {
				String line = reader.readLine();
				if ( line == null ) {
					keepGoing = false;
					if ( skipToLastLine ) {
						line = lastLine;
					}
				}
				Matcher m = pat.matcher(line);
				if ( m.find() ) {
					if ( log.isDebugEnabled() ) {
						log.debug("Found matching data line [" +line +']');
					}
					data = line;
					keepGoing = false;
				} else {
					lastLine = line;
				}
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			if ( reader != null ) {
				try {
					reader.close();
				} catch ( IOException e ) {
					if ( log.isWarnEnabled() ) {
						log.warn("IOException closing input stream: " +e);
					}
				}
			}
		}
		if ( data == null ) {
			log.info("Input stream finished without finding expected data");
		} else {
			if ( this.buffer == null ) {
				this.buffer = new StringBuilder(data);
			} else {
				this.buffer.append(data);
			}
		}
	}

	@Override
	public byte[] getCollectedData() {
		String enc = getEncodingToUse();
		try {
			return buffer == null 
					? new byte[0]
					: buffer.toString().getBytes(enc);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private String getEncodingToUse() {
		return this.encoding == null ? DEFAULT_ENCODING : this.encoding;
	}

	@Override
	public String getCollectedDataAsString() {
		return buffer == null ? null : buffer.toString();
	}

	@Override
	public void stopCollecting() {
		if ( buffer == null ) {
			return;
		}
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the connectionTimeout
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * @param connectionTimeout the connectionTimeout to set
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * @return the matchExpression
	 */
	public String getMatchExpression() {
		return matchExpression;
	}

	/**
	 * @param matchExpression the matchExpression to set
	 */
	public void setMatchExpression(String matchExpression) {
		this.matchExpression = matchExpression;
	}

	/**
	 * @return the encoding
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * @param encoding the encoding to set
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * @return the skipToLastLine
	 */
	public boolean isSkipToLastLine() {
		return skipToLastLine;
	}

	/**
	 * @param skipToLastLine the skipToLastLine to set
	 */
	public void setSkipToLastLine(boolean skipToLastLine) {
		this.skipToLastLine = skipToLastLine;
	}

	/**
	 * @return the urlFactory
	 */
	public ObjectFactory<String> getUrlFactory() {
		return urlFactory;
	}

	/**
	 * @param urlFactory the urlFactory to set
	 */
	public void setUrlFactory(ObjectFactory<String> urlFactory) {
		this.urlFactory = urlFactory;
	}

}
