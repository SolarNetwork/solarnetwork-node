/* ==================================================================
 * BasicPlatformPackageResult.java - 22/05/2019 4:20:30 pm
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

package net.solarnetwork.node.support;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.PlatformPackageService.PlatformPackageResult;

/**
 * A basic, immutable implementation of
 * {@Link PlatformPackageService.PlatformPackageResult}.
 * 
 * @param <T>
 *        the context type
 * @author matt
 * @version 1.0
 * @since 1.68
 */
public class BasicPlatformPackageResult<T> implements PlatformPackageResult<T> {

	private final boolean success;
	private final String message;
	private final Throwable exception;
	private final List<Path> extractedPaths;
	private final T context;

	/**
	 * Constructor.
	 * 
	 * @param success
	 *        the success flag
	 * @param message
	 *        the message
	 * @param exception
	 *        the exception
	 * @param extractedPaths
	 *        the extracted paths
	 * @param context
	 *        the context
	 */
	public BasicPlatformPackageResult(boolean success, String message, Throwable exception,
			List<Path> extractedPaths, T context) {
		super();
		this.success = success;
		this.message = message;
		this.exception = exception;
		this.extractedPaths = extractedPaths != null ? Collections.unmodifiableList(extractedPaths)
				: null;
		this.context = context;
	}

	@Override
	public boolean isSuccess() {
		return success;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public Throwable getException() {
		return exception;
	}

	@Override
	public List<Path> getExtractedPaths() {
		return extractedPaths;
	}

	@Override
	public T getContext() {
		return context;
	}

}
