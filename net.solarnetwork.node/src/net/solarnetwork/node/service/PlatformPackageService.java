/* ==================================================================
 * PlatformPackageService.java - 22/05/2019 3:43:59 pm
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

package net.solarnetwork.node.service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import net.solarnetwork.service.ProgressListener;

/**
 * API for a service that deals with platform packages.
 * 
 * <p>
 * Platform packages could be as simple as <code>tar</code> archives or some
 * sort of native package like a <code>.deb</code> package on Debian based
 * systems.
 * </p>
 * 
 * @author matt
 * @version 2.1
 * @since 1.68
 */
public interface PlatformPackageService {

	/**
	 * A platform package.
	 */
	interface PlatformPackage {

		/**
		 * Get the package name.
		 * 
		 * @return the name
		 */
		String getName();

		/**
		 * Get the package version.
		 * 
		 * @return the version
		 */
		String getVersion();

		/**
		 * Flag if this package is installed.
		 * 
		 * @return {@literal true} if the package is installed
		 */
		boolean isInstalled();
	}

	/**
	 * The results of performing an action on a package.
	 * 
	 * @param <T>
	 *        the context object type
	 */
	interface PlatformPackageResult<T> {

		/**
		 * Get the success flag.
		 * 
		 * @return {@literal true} if the package was extracted successfully
		 */
		boolean isSuccess();

		/**
		 * Get a result message.
		 * 
		 * @return a result message (or {@literal null})
		 */
		String getMessage();

		/**
		 * Get a result exception.
		 * 
		 * @return an exception, or {@literal null} if no exception occurred
		 */
		Throwable getException();

		/**
		 * Get a complete list of files installed from the package.
		 * 
		 * @return the extracted paths, or {@literal null} if nothing installed
		 */
		List<Path> getExtractedPaths();

		/**
		 * Get the context object.
		 * 
		 * @return the context object
		 */
		T getContext();

	}

	/**
	 * Test if this service handles packages with a given name.
	 * 
	 * @param archiveFileName
	 *        the name of the package archive file; a file extension might be
	 *        used to determine the type of package the name represents
	 * @return {@literal true} if the archive file name is supported by this
	 *         service
	 */
	boolean handlesPackage(String archiveFileName);

	/**
	 * Install a package from an archive file.
	 * 
	 * @param <T>
	 *        the context object type
	 * @param archive
	 *        the package to extract and install
	 * @param baseDirectory
	 *        a "base" directory to resolve relative file paths against
	 * @param progressListener
	 *        an optional listener of the progress of extracting the package
	 * @param context
	 *        a context object to pass to {@code progressListener} and provide
	 *        in the result; may be {@literal null}
	 * @return a future for the package installation results
	 * @throws IllegalArgumentException
	 *         if {@code archive} is not a supported type (that is, the
	 *         {@link #handlesPackage(String)} does not return {@literal true}
	 *         for the archives name)
	 */
	<T> Future<PlatformPackageResult<T>> installPackage(Path archive, Path baseDirectory,
			ProgressListener<T> progressListener, T context);

	/**
	 * List named packages, optionally filtering by name and installed status.
	 * 
	 * @param nameFilter
	 *        a regular expression to filter packages by, or {@literal null} to
	 *        include everything
	 * @param installedFilter
	 *        {@literal true} to include only installed packages,
	 *        {@literal false} to exclude installed packages, or {@literal null}
	 *        to include everything
	 * @return the matching packages, never {@literal null}
	 */
	Future<Iterable<PlatformPackage>> listNamedPackages(String nameFilter, Boolean installedFilter);

	/**
	 * List upgradable named packages.
	 * 
	 * @return the upgradable packages, never {@literal null}
	 * @since 2.1
	 */
	Future<Iterable<PlatformPackage>> listUpgradableNamedPackages();

	/**
	 * Refresh named packages.
	 * 
	 * @return the task completion status
	 */
	Future<Boolean> refreshNamedPackages();

	/**
	 * Cleanup any temporary or cached data.
	 * 
	 * @return the task completion status
	 */
	Future<Boolean> cleanup();

	/**
	 * Install a package.
	 * 
	 * <p>
	 * This method is used to install named packages that are "available" to be
	 * installed, i.e. one that is returned from the
	 * {@link #listNamedPackages(String, Boolean)} method when {@literal false}
	 * is passed. The package might have to be downloaded from a remote package
	 * repository and then installed.
	 * </p>
	 * 
	 * @param <T>
	 *        the context object type
	 * @param name
	 *        the package name to install
	 * @param version
	 *        the package version to install, or {@literal null} for the highest
	 *        available version
	 * @param baseDirectory
	 *        a "base" directory to resolve relative file paths against
	 * @param progressListener
	 *        an optional listener of the progress of extracting the package
	 * @param context
	 *        a context object to pass to {@code progressListener} and provide
	 *        in the result; may be {@literal null}
	 * @return a future for the package installation results
	 */
	<T> Future<PlatformPackageResult<T>> installNamedPackage(String name, String version,
			Path baseDirectory, ProgressListener<T> progressListener, T context);

	/**
	 * Remove a named package.
	 * 
	 * <p>
	 * This method is used to remove named packages that are currently
	 * installed.
	 * </p>
	 * 
	 * @param <T>
	 *        the context object type
	 * @param name
	 *        the package name to install
	 * @param progressListener
	 *        an optional listener of the progress of extracting the package
	 * @param context
	 *        a context object to pass to {@code progressListener} and provide
	 *        in the result; may be {@literal null}
	 * @return a future for the package installation results
	 */
	<T> Future<PlatformPackageResult<T>> removeNamedPackage(String name,
			ProgressListener<T> progressListener, T context);

	/**
	 * Upgrade all installed packages.
	 * 
	 * <p>
	 * This method is used to upgrade named packages to the highest available
	 * versions.
	 * </p>
	 * 
	 * @param <T>
	 *        the context object type
	 * @param progressListener
	 *        an optional listener of the progress of extracting the package
	 * @param context
	 *        a context object to pass to {@code progressListener} and provide
	 *        in the result; may be {@literal null}
	 * @return a future for the package installation results
	 */
	<T> Future<PlatformPackageResult<T>> upgradeNamedPackages(ProgressListener<T> progressListener,
			T context);

}
