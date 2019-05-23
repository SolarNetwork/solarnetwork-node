/* ==================================================================
 * DebianPlatformPackageService.java - 23/05/2019 10:03:58 am
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

package net.solarnetwork.node.setup.deb;

import static net.solarnetwork.util.StringUtils.delimitedStringFromCollection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.PlatformPackageService;
import net.solarnetwork.node.support.BasePlatformPackageService;
import net.solarnetwork.node.support.BasicPlatformPackageExtractResult;
import net.solarnetwork.util.ProgressListener;

/**
 * Implementation of {@link PlatformPackageService} for Debian packages.
 * 
 * @author matt
 * @version 1.0
 */
public class DebianPlatformPackageService extends BasePlatformPackageService
		implements PlatformPackageService {

	private static final Pattern DEBIAN_PACKAGE_PAT = Pattern.compile("\\.deb$");

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public boolean handlesPackage(String archiveFileName) {
		return archiveFileName != null && DEBIAN_PACKAGE_PAT.matcher(archiveFileName).find();
	}

	@Override
	public <T> Future<PlatformPackageExtractResult<T>> extractPackage(Path archive, Path baseDirectory,
			ProgressListener<T> progressListener, T context) {
		return performTask(createTask(archive, baseDirectory, progressListener, context), context);
	}

	protected <T> Callable<PlatformPackageExtractResult<T>> createTask(Path archive, Path baseDirectory,
			ProgressListener<T> progressListener, T context) {
		return new Callable<PlatformPackageService.PlatformPackageExtractResult<T>>() {

			@Override
			public PlatformPackageExtractResult<T> call() throws Exception {
				List<String> cmd = pkgCommand("install", archive.toAbsolutePath().toString());
				if ( log.isDebugEnabled() ) {
					log.debug("Package command: {}", delimitedStringFromCollection(cmd, " "));
				}
				log.info("Extracting Debian archive {}", archive);
				List<Path> extractedPaths = new ArrayList<>();
				ProcessBuilder pb = new ProcessBuilder(cmd);
				Path devNull = Paths.get("/dev/null");
				if ( Files.exists(devNull) ) {
					pb.redirectInput(devNull.toFile());
				}
				Process pr = pb.start();
				try (BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
						BufferedReader err = new BufferedReader(
								new InputStreamReader(pr.getErrorStream()))) {
					String line = null;
					while ( (line = in.readLine()) != null ) {
						Path path = FileSystems.getDefault().getPath(line).toAbsolutePath().normalize();
						extractedPaths.add(path);
						log.trace("Installed package resource: {}", line);
					}
					if ( !pr.waitFor(5, TimeUnit.MINUTES) ) {
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
					String output = delimitedStringFromCollection(extractedPaths, "\n");
					log.error("Package command returned non-zero exit code {}: {}", pr.exitValue(),
							output);
					throw new IOException("Package command returned non-zero exit code " + pr.exitValue()
							+ ": " + output);
				}

				return new BasicPlatformPackageExtractResult<T>(true, null, null, extractedPaths,
						context);
			}
		};
	}

}
