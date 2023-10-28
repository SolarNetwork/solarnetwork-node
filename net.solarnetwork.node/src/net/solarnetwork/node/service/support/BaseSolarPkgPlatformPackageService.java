/* ==================================================================
 * BaseSolarPkgPlatformPackageService.java - 24/05/2019 2:07:43 pm
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

package net.solarnetwork.node.service.support;

import static net.solarnetwork.util.StringUtils.delimitedStringFromCollection;
import static net.solarnetwork.util.StringUtils.parseBoolean;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.util.StringUtils;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.service.PlatformPackageService;
import net.solarnetwork.service.ProgressListener;

/**
 * Base implementation of {@link PlatformPackageService}, with support for using
 * an OS-level helper program that adheres to a <i>solarpkg</i> API.
 * 
 * <p>
 * The <i>solarpkg</i> program API follows this simple pattern:
 * </p>
 * 
 * <pre>
 * <code>solarpkg &lt;action&gt; [arguments...]</code>
 * </pre>
 * 
 * <p>
 * All invocations are expected to return a status code <code>0</code> on
 * success. All "return" values are printed to STDOUT. The <code>action</code>
 * values include:
 * </p>
 * 
 * <dl>
 * <dt><code>clean</code></dt>
 * <dd>Remove any cached download packages or temporary files. Remove any
 * packages no longer required by other packages.</dd>
 * <dt><code>install</code> <i>name</i> [<i>version</i>]</dt>
 * <dd>Install package <i>name</i>. If <i>name</i> appears to be the path to a
 * package file, then install the specific package file. Otherwise, download and
 * install <i>name</i> from some OS-configured package repository; if
 * <i>version</i> specified then install the specific version.</dd>
 * <dt><code>is-installed</code> <i>name</i></dt>
 * <dd>Test if a particular package is installed. Returns {@literal true} or
 * {@literal false}.</dd>
 * <dt><code>list</code> [<i>name</i>]</dt>
 * <dd>List packages. If <i>name</i> is provided, only packages matching this
 * name (including wildcards) will be listed. The output is a CSV table of
 * columns: <code>name, version, installed</code>. The <code>installed</code>
 * column contains ({@literal true} if the package is currently installed, or
 * {@literal false} otherwise.</dd>
 * <dt><code>list-available</code> [<i>name</i>]</dt>
 * <dd>List packages available to be installesd. If <i>name</i> is provided,
 * only packages matching this name (including wildcards) will be listed. The
 * output is a CSV table of columns: <code>name, version, installed</code>. The
 * <code>installed</code> column contains ({@literal false} in all cases.</dd>
 * <dt><code>list-installed</code> [<i>name</i>]</dt>
 * <dd>List installed packages. If <i>name</i> is provided, only packages
 * matching this name (including wildcards) will be listed. The output is a CSV
 * table of columns: <code>name, version, installed</code>. The
 * <code>installed</code> column contains ({@literal true} in all cases.</dd>
 * <dt><code>list-upgradable</code></dt>
 * <dd>List upgradable packages. The output is a CSV table of columns:
 * <code>name, version, installed</code>. The <code>installed</code> column
 * contains ({@literal false} in all cases.</dd>
 * <dt><code>refresh</code></dt>
 * <dd>Refresh the available packages from remote repositories.</dd>
 * <dt><code>remove</code> <i>name</i></dt>
 * <dd>Remove the installed package <i>name</i>.</dd>
 * <dt><code>upgrade</code> [<i>major</i>]</dt>
 * <dd>Upgrade all packages. If <i>major</i> defined, then perform a "major"
 * upgrade, allowing for more aggressive upgrading.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 * @since 2.0
 */
public abstract class BaseSolarPkgPlatformPackageService extends BasePlatformPackageService {

	/** The default value for the {@code command} property. */
	public static final String DEFAULT_COMMAND = Constants.solarNodeHome() + "/bin/solarpkg";

	/** The default package timeout value. */
	public static final long DEFAULT_PACKAGE_ACTION_TIMEOUT_SECS = TimeUnit.MINUTES.toSeconds(5);

	private long packageActionTimeoutSecs = DEFAULT_PACKAGE_ACTION_TIMEOUT_SECS;

	/**
	 * The "solarpkg" actions.
	 */
	public static enum Action {

		/** Clean up cached resources. */
		Clean("clean"),

		/** Install a package. */
		Install("install"),

		/** Test if a package is installed. */
		IsInstalled("is-installed"),

		/** List all packages. */
		List("list"),

		/** List available packages. */
		ListAvailable("list-available"),

		/** List installed packages. */
		ListInstalled("list-installed"),

		/** List upgradable packages. */
		ListUpgradable("list-upgradable"),

		/** Refresh local package cache. */
		Refresh("refresh"),

		/** Remove a package. */
		Remove("remove"),

		/** Upgrade a package. */
		Upgrade("upgrade");

		private final String command;

		private Action(String cmd) {
			this.command = cmd;
		}

		/**
		 * Get the action command value.
		 * 
		 * @return the command value
		 */
		public String getCommand() {
			return command;
		}

		/**
		 * Get an enum from a command value.
		 * 
		 * @param cmd
		 *        the command value
		 * @return the associated enum
		 * @throws IllegalArgumentException
		 *         if {@code cmd} is not supported
		 */
		public static Action forCommand(String cmd) {
			for ( Action a : Action.values() ) {
				if ( a.command.equalsIgnoreCase(cmd) ) {
					return a;
				}
			}
			throw new IllegalArgumentException("Command [" + cmd + "] not supported");
		}

	}

	private String command = DEFAULT_COMMAND;

	/**
	 * Default constructor.
	 */
	public BaseSolarPkgPlatformPackageService() {
		super();
	}

	/**
	 * Get an OS command to run, based on the configured {@code command}
	 * program, an action verb, and optional arguments.
	 * 
	 * @param action
	 *        the action verb
	 * @param args
	 *        the arguments
	 * @return the command as a list
	 */
	protected List<String> pkgCommand(Action action, String... args) {
		List<String> result = new ArrayList<>(2 + (args != null ? args.length : 0));
		result.add(getCommand());
		result.add(action.getCommand());
		if ( args != null ) {
			for ( String arg : args ) {
				if ( arg != null ) {
					result.add(arg);
				}
			}
		}
		return result;
	}

	/**
	 * Execute an OS command that prints out file paths.
	 * 
	 * @param <T>
	 *        the context type
	 * @param cmd
	 *        the command to execute
	 * @param context
	 *        the context
	 * @return the result
	 * @throws Exception
	 *         if an error occurs
	 */
	protected <T> PlatformPackageResult<T> executePackageCommand(List<String> cmd, T context)
			throws Exception {
		if ( log.isDebugEnabled() ) {
			log.debug("Package command: {}", delimitedStringFromCollection(cmd, " "));
		}
		List<String> installed = executeCommand(cmd);
		List<Path> extractedPaths = new ArrayList<>();
		for ( String p : installed ) {
			Path path = FileSystems.getDefault().getPath(p).toAbsolutePath().normalize();
			extractedPaths.add(path);
		}
		return new BasicPlatformPackageResult<T>(true, null, null, extractedPaths, context);
	}

	/**
	 * Execute an OS command.
	 * 
	 * @param cmd
	 *        the command to execute
	 * @return the STDOUT result
	 * @throws Exception
	 *         if an error occurs
	 */
	protected List<String> executeCommand(List<String> cmd) throws Exception {
		if ( log.isDebugEnabled() ) {
			log.debug("Package command: {}", delimitedStringFromCollection(cmd, " "));
		}
		List<String> result = new ArrayList<>();
		ProcessBuilder pb = new ProcessBuilder(cmd);
		Path devNull = Paths.get("/dev/null");
		if ( Files.exists(devNull) ) {
			pb.redirectInput(devNull.toFile());
		}
		Process pr = pb.start();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()))) {
			String line = null;
			while ( (line = in.readLine()) != null ) {
				result.add(line);
				log.trace("Package command output: {}", line);
			}
			if ( !pr.waitFor(packageActionTimeoutSecs, TimeUnit.SECONDS) ) {
				throw new IOException("Timeout waiting for package command to complete.");
			}
			StringBuilder errMsg = new StringBuilder();
			while ( (line = err.readLine()) != null ) {
				if ( errMsg.length() > 0 ) {
					errMsg.append("\n");
				}
				errMsg.append(line);
			}
			if ( errMsg.length() > 0 ) {
				log.warn("Package command issued warnings: {}", errMsg);
			}
		}
		if ( pr.exitValue() != 0 ) {
			String output = delimitedStringFromCollection(result, "\n");
			log.error("Package command returned non-zero exit code {}: {}", pr.exitValue(), output);
			throw new IOException(
					"Package command returned non-zero exit code " + pr.exitValue() + ": " + output);
		}

		return result;
	}

	@Override
	public <T> Future<PlatformPackageResult<T>> installPackage(Path archive, Path baseDirectory,
			ProgressListener<T> progressListener, T context) {
		Callable<PlatformPackageResult<T>> task = new Callable<PlatformPackageService.PlatformPackageResult<T>>() {

			@Override
			public PlatformPackageResult<T> call() throws Exception {
				log.info("Installing package archive {}", archive);
				List<String> cmd = pkgCommand(Action.Install, archive.toAbsolutePath().toString());
				return executePackageCommand(cmd, context);
			}
		};
		return performPackageResultTask(task, context);
	}

	@Override
	public Future<Iterable<PlatformPackage>> listNamedPackages(String nameFilter,
			Boolean installedFilter) {
		Callable<Iterable<PlatformPackage>> task = new Callable<Iterable<PlatformPackage>>() {

			@Override
			public Iterable<PlatformPackage> call() throws Exception {
				Action action = installedFilter == null ? Action.List
						: installedFilter == true ? Action.ListInstalled : Action.ListAvailable;
				List<String> cmd = pkgCommand(action, nameFilter);
				List<String> csv = executeCommand(cmd);
				if ( csv == null || csv.isEmpty() ) {
					return Collections.emptyList();
				}
				List<PlatformPackage> results = new ArrayList<>(csv.size());
				for ( String row : csv ) {
					String[] cols = StringUtils.commaDelimitedListToStringArray(row);
					if ( cols != null && cols.length > 2 ) {
						results.add(new BasicPlatformPackage(cols[0], cols[1], parseBoolean(cols[2])));
					}
				}
				return results;
			}
		};
		return performTask(task);
	}

	@Override
	public Future<Iterable<PlatformPackage>> listUpgradableNamedPackages() {
		Callable<Iterable<PlatformPackage>> task = new Callable<Iterable<PlatformPackage>>() {

			@Override
			public Iterable<PlatformPackage> call() throws Exception {
				List<String> cmd = pkgCommand(Action.ListUpgradable);
				List<String> csv = executeCommand(cmd);
				if ( csv == null || csv.isEmpty() ) {
					return Collections.emptyList();
				}
				List<PlatformPackage> results = new ArrayList<>(csv.size());
				for ( String row : csv ) {
					String[] cols = StringUtils.commaDelimitedListToStringArray(row);
					if ( cols != null && cols.length > 2 ) {
						results.add(new BasicPlatformPackage(cols[0], cols[1], parseBoolean(cols[2])));
					}
				}
				return results;
			}
		};
		return performTask(task);
	}

	@Override
	public <T> Future<PlatformPackageResult<T>> installNamedPackage(String name, String version,
			Path baseDirectory, ProgressListener<T> progressListener, T context) {
		Callable<PlatformPackageResult<T>> task = new Callable<PlatformPackageService.PlatformPackageResult<T>>() {

			@Override
			public PlatformPackageResult<T> call() throws Exception {
				log.info("Installing package {} version {}", name, version);
				List<String> cmd = pkgCommand(Action.Install, name, version);
				return executePackageCommand(cmd, context);
			}
		};
		return performPackageResultTask(task, context);
	}

	@Override
	public <T> Future<PlatformPackageResult<T>> removeNamedPackage(String name,
			ProgressListener<T> progressListener, T context) {
		Callable<PlatformPackageResult<T>> task = new Callable<PlatformPackageService.PlatformPackageResult<T>>() {

			@Override
			public PlatformPackageResult<T> call() throws Exception {
				log.info("Removing package {} version {}", name);
				List<String> cmd = pkgCommand(Action.Remove, name);
				return executePackageCommand(cmd, context);
			}
		};
		return performPackageResultTask(task, context);
	}

	@Override
	public <T> Future<PlatformPackageResult<T>> upgradeNamedPackages(
			ProgressListener<T> progressListener, T context) {
		Callable<PlatformPackageResult<T>> task = new Callable<PlatformPackageService.PlatformPackageResult<T>>() {

			@Override
			public PlatformPackageResult<T> call() throws Exception {
				log.info("Upgrading packages");
				List<String> cmd = pkgCommand(Action.Upgrade);
				return executePackageCommand(cmd, context);
			}
		};
		return performPackageResultTask(task, context);
	}

	@Override
	public Future<Boolean> refreshNamedPackages() {
		Callable<Boolean> task = new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				List<String> cmd = pkgCommand(Action.Refresh);
				executeCommand(cmd);
				return true;
			}
		};
		return performTask(task);
	}

	@Override
	public Future<Boolean> cleanup() {
		Callable<Boolean> task = new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				List<String> cmd = pkgCommand(Action.Clean);
				executeCommand(cmd);
				return true;
			}
		};
		return performTask(task);
	}

	/**
	 * Get the OS command for the package helper program to use.
	 * 
	 * @return the OS command; defaults to {@link #DEFAULT_COMMAND}
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Set the OS command for the package helper program to use.
	 * 
	 * @param command
	 *        the OS command
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * Get the timeout, in seconds, to use for package actions.
	 * 
	 * @return the seconds; defaults to
	 *         {@link #DEFAULT_PACKAGE_ACTION_TIMEOUT_SECS}
	 */
	public long getPackageActionTimeoutSecs() {
		return packageActionTimeoutSecs;
	}

	/**
	 * Set a timeout, in seconds, to use for package actions.
	 * 
	 * @param packageActionTimeoutSecs
	 *        the timeout to use
	 */
	public void setPackageActionTimeoutSecs(long packageActionTimeoutSecs) {
		this.packageActionTimeoutSecs = packageActionTimeoutSecs;
	}
}
