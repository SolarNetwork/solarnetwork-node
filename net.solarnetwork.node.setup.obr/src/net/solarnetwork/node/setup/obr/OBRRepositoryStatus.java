/* ==================================================================
 * OBRRepositoryStatus.java - Apr 21, 2014 4:40:47 PM
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

package net.solarnetwork.node.setup.obr;

import java.net.URL;

/**
 * Status information about an {@link OBRRepository}.
 * 
 * @author matt
 * @version 1.0
 */
public class OBRRepositoryStatus {

	private URL repositoryURL;
	private boolean configured = false;
	private Exception exception = null;

	/**
	 * Get the configured flag.
	 * 
	 * @return {@literal true} if the repository is configured
	 */
	public boolean isConfigured() {
		return configured;
	}

	/**
	 * Set the configured flag.
	 * 
	 * @param configured
	 *        {@literal true} if the repository is configured
	 */
	public void setConfigured(boolean configured) {
		this.configured = configured;
	}

	/**
	 * Get the exception.
	 * 
	 * @return an exception
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Set the exception.
	 * 
	 * @param exception
	 *        the exception
	 */
	public void setException(Exception exception) {
		this.exception = exception;
	}

	/**
	 * Get the repository URL.
	 * 
	 * @return the repository URL
	 */
	public URL getRepositoryURL() {
		return repositoryURL;
	}

	/**
	 * Set the repository URL.
	 * 
	 * @param repositoryURL
	 *        the URL to set
	 */
	public void setRepositoryURL(URL repositoryURL) {
		this.repositoryURL = repositoryURL;
	}

}
