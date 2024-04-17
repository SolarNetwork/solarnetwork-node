/* ==================================================================
 * S3SetupTaskResult.java - 21/11/2017 2:19:47 PM
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.s3;

import java.nio.file.Path;
import java.util.Set;

/**
 * Result object for the S3 setup task.
 *
 * @author matt
 * @version 1.0
 */
public class S3SetupTaskResult {

	private final boolean success;
	private final Set<Path> installedFiles;
	private final Set<Path> deletedFiles;

	/**
	 * Constructor.
	 *
	 * @param success
	 *        {@literal true} if the result is a success
	 * @param installedFiles
	 *        a list of installed files
	 * @param deletedFiles
	 *        a list of deleted files
	 */
	public S3SetupTaskResult(boolean success, Set<Path> installedFiles, Set<Path> deletedFiles) {
		super();
		this.success = success;
		this.installedFiles = installedFiles;
		this.deletedFiles = deletedFiles;
	}

	/**
	 * Get the success flag.
	 *
	 * @return {@literal true} if the task completed successfully
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Get the set of installed files.
	 *
	 * @return the installed files
	 */
	public Set<Path> getInstalledFiles() {
		return installedFiles;
	}

	/**
	 * Get the set of deleted files.
	 *
	 * @return the deleted files
	 */
	public Set<Path> getDeletedFiles() {
		return deletedFiles;
	}

}
