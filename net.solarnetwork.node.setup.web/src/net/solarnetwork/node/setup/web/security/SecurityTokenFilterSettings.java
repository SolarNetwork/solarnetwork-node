/* ==================================================================
 * SecurityTokenFilterSettings.java - 4/03/2022 10:10:35 AM
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

package net.solarnetwork.node.setup.web.security;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.springframework.util.unit.DataSize;
import net.solarnetwork.web.jakarta.security.SecurityHttpServletRequestWrapper;

/**
 * Configurable settings for security token filters.
 * 
 * @author matt
 * @version 1.0
 * @since 3.3
 */
public class SecurityTokenFilterSettings {

	/** The {@code maxDateSkew} property default value. */
	public static final long DEFALUT_MAX_DATE_SKEW = 15 * 60 * 1000;

	/** The {@code maxRequestBodySize} property default value. */
	public static final DataSize DEFAULT_MAX_REQUEST_BODY_SIZE = DataSize.ofBytes(65535);

	/** The {@code minimumCompressLength} property default value. */
	public static final DataSize DEFAULT_MINIMUM_COMPRESS_LENGTH = DataSize
			.ofBytes(SecurityHttpServletRequestWrapper.DEFAULT_MINIMUM_COMPRESS_LENGTH);

	/** The {@code minimumSpoolLength} property default value. */
	public static final DataSize DEFAULT_MINIMUM_SPOOL_LENGTH = DataSize
			.ofBytes(SecurityHttpServletRequestWrapper.DEFAULT_MINIMUM_SPOOL_LENGTH);

	/** The {@code spoolDirectory} property default value. */
	public static final Path DEFAULT_SPOOL_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
			"SecurityTokenFilter");

	private long maxDateSkew = DEFALUT_MAX_DATE_SKEW;
	private DataSize maxRequestBodySize = DEFAULT_MAX_REQUEST_BODY_SIZE;

	private DataSize minimumCompressLength = DEFAULT_MINIMUM_COMPRESS_LENGTH;
	private Pattern compressibleContentTypePattern = SecurityHttpServletRequestWrapper.DEFAULT_COMPRESSIBLE_CONTENT_PATTERN;
	private DataSize minimumSpoolLength = DEFAULT_MINIMUM_SPOOL_LENGTH;
	private Path spoolDirectory = DEFAULT_SPOOL_DIRECTORY;

	/**
	 * Constructor.
	 */
	public SecurityTokenFilterSettings() {
		super();
	}

	/**
	 * Get the maximum date skew.
	 * 
	 * @return the maximum date skew, in milliseconds; defaults to
	 *         {@link #DEFALUT_MAX_DATE_SKEW}
	 */
	public long getMaxDateSkew() {
		return maxDateSkew;
	}

	/**
	 * Set the maximum date skew.
	 * 
	 * @param maxDateSkew
	 *        the maximum date skew, in milliseconds
	 */
	public void setMaxDateSkew(long maxDateSkew) {
		this.maxDateSkew = maxDateSkew;
	}

	/**
	 * Get the maximum request body size.
	 * 
	 * @return the maximum size, never {@literal null} ; defaults to
	 *         {@link #DEFAULT_MAX_REQUEST_BODY_SIZE}
	 */
	public DataSize getMaxRequestBodySize() {
		return maxRequestBodySize;
	}

	/**
	 * Set the maximum request body size.
	 * 
	 * @param maxRequestBodySize
	 *        the maximum size to set; if {@literal null} then
	 *        {@link #DEFAULT_MAX_REQUEST_BODY_SIZE} will be set
	 */
	public void setMaxRequestBodySize(DataSize maxRequestBodySize) {
		if ( maxRequestBodySize == null ) {
			maxRequestBodySize = DEFAULT_MAX_REQUEST_BODY_SIZE;
		}
		this.maxRequestBodySize = maxRequestBodySize;
	}

	/**
	 * Get the minimum content length before compression can be used.
	 * 
	 * @return the length; defaults to {@link #DEFAULT_MINIMUM_COMPRESS_LENGTH}
	 */
	public DataSize getMinimumCompressLength() {
		return minimumCompressLength;
	}

	/**
	 * Set the minimum content length before compression can be used.
	 * 
	 * @param minimumCompressLength
	 *        the length to set
	 */
	public void setMinimumCompressLength(DataSize minimumCompressLength) {
		this.minimumCompressLength = minimumCompressLength;
	}

	/**
	 * Get a pattern of compressible content types.
	 * 
	 * @return the pattern; defaults to
	 *         {@link SecurityHttpServletRequestWrapper#DEFAULT_COMPRESSIBLE_CONTENT_PATTERN}
	 */
	public Pattern getCompressibleContentTypePattern() {
		return compressibleContentTypePattern;
	}

	/**
	 * Set a pattern of compressible content types.
	 * 
	 * @param compressibleContentTypePattern
	 *        the pattern to set
	 */
	public void setCompressibleContentTypePattern(Pattern compressibleContentTypePattern) {
		this.compressibleContentTypePattern = compressibleContentTypePattern;
	}

	/**
	 * Get the minimum content length before spooling to disk is allowed.
	 * 
	 * @return the length; defaults to {@link #DEFAULT_MINIMUM_SPOOL_LENGTH}
	 */
	public DataSize getMinimumSpoolLength() {
		return minimumSpoolLength;
	}

	/**
	 * Set the minimum content length before spooling to disk is allowed.
	 * 
	 * @param minimumSpoolLength
	 *        the length to set
	 */
	public void setMinimumSpoolLength(DataSize minimumSpoolLength) {
		this.minimumSpoolLength = minimumSpoolLength;
	}

	/**
	 * Get the directory to create temporary spool files.
	 * 
	 * @return the directory; defaults to a {@link #DEFAULT_SPOOL_DIRECTORY}
	 */
	public Path getSpoolDirectory() {
		return spoolDirectory;
	}

	/**
	 * Set the directory to create temporary spool files.
	 * 
	 * @param spoolDirectory
	 *        the directory to set
	 */
	public void setSpoolDirectory(Path spoolDirectory) {
		this.spoolDirectory = spoolDirectory;
	}

}
