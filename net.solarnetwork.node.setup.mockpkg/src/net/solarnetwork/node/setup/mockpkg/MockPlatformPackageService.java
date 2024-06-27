/* ==================================================================
 * MockPlatformPackageService.java - 27/10/2023 6:48:13 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.mockpkg;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.util.StringUtils.parseBoolean;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import net.solarnetwork.node.service.PlatformPackageService;
import net.solarnetwork.node.service.support.BasePlatformPackageService;
import net.solarnetwork.node.service.support.BasicPlatformPackage;
import net.solarnetwork.node.service.support.BasicPlatformPackageResult;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Mock implementation of
 * {@link net.solarnetwork.node.service.PlatformPackageService}.
 * 
 * @author matt
 * @version 1.1
 */
public class MockPlatformPackageService extends BasePlatformPackageService
		implements ServiceLifecycleObserver {

	private final ConcurrentNavigableMap<String, PlatformPackage> available = new ConcurrentSkipListMap<>();
	private final ConcurrentNavigableMap<String, PlatformPackage> installed = new ConcurrentSkipListMap<>();
	private final ConcurrentNavigableMap<String, PlatformPackage> upgradable = new ConcurrentSkipListMap<>();

	/**
	 * Constructor.
	 */
	public MockPlatformPackageService() {
		super();
	}

	@Override
	public void serviceDidStartup() {
		performTask(() -> {
			loadPackages();
			return null;
		});
	}

	private synchronized void loadPackages() {
		available.clear();
		installed.clear();
		upgradable.clear();
		parsePackages("solarpkg-list-available-11.csv", available);
		parsePackages("solarpkg-list-installed-11.csv", installed);
		parsePackages("solarpkg-list-upgradable-11.csv", upgradable);
		log.info("Initialized {} installed and {} available and {} upgradable packages",
				installed.size(), available.size(), upgradable.size());

	}

	private void parsePackages(String resource, Map<String, PlatformPackage> dest) {
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream(resource), UTF_8))) {
			String line;
			while ( (line = r.readLine()) != null ) {
				parsePackage(line, dest);
			}
		} catch ( IOException e ) {
			log.error("I/O error parsing resource [" + resource + "]: " + e.toString());
		}
	}

	private static PlatformPackage parsePackage(String line, Map<String, PlatformPackage> dest) {
		String[] cols = StringUtils.commaDelimitedListToStringArray(line);
		if ( cols != null && cols.length > 2 ) {
			PlatformPackage pkg = new BasicPlatformPackage(cols[0], cols[1], parseBoolean(cols[2]));
			if ( pkg != null ) {
				dest.put(pkg.getName(), pkg);
			}
		}
		return null;
	}

	@Override
	public void serviceDidShutdown() {
		// nothing to do
	}

	@Override
	public boolean handlesPackage(String archiveFileName) {
		return archiveFileName != null && archiveFileName.endsWith(".deb");
	}

	// regex with name, version, arch groups
	private static final Pattern DEB_FILENAME_PATTERN = Pattern.compile("^(.*)_(.*)_(.*)\\.deb$");

	@Override
	public <T> Future<PlatformPackageResult<T>> installPackage(Path archive, Path baseDirectory,
			ProgressListener<T> progressListener, T context) {
		Callable<PlatformPackageResult<T>> task = new Callable<PlatformPackageService.PlatformPackageResult<T>>() {

			@Override
			public PlatformPackageResult<T> call() throws Exception {
				log.info("Installing package archive {}", archive);
				Matcher m = DEB_FILENAME_PATTERN.matcher(archive.getFileName().toString());
				if ( m.matches() ) {
					String name = m.group(1);
					String version = m.group(2);
					PlatformPackage pkg = new BasicPlatformPackage(name, version, true);
					installed.put(name, pkg);
					available.remove(name);
				}
				progressListener.progressChanged(context, 1);
				return new BasicPlatformPackageResult<T>(true, null, null, Collections.emptyList(),
						context);
			}
		};
		return performPackageResultTask(task, context);
	}

	@Override
	public Future<Iterable<PlatformPackage>> listNamedPackages(String nameFilter,
			Boolean installedFilter) {
		final Pattern filter = globPattern(nameFilter);
		return performTask(() -> {
			if ( Boolean.TRUE.equals(installedFilter) ) {
				if ( filter == null ) {
					return installed.values();
				}
				return installed.values().stream().filter(pkg -> filter.matcher(pkg.getName()).matches())
						.collect(toList());
			} else if ( Boolean.FALSE.equals(installedFilter) ) {
				if ( filter == null ) {
					return available.values();
				}
				return available.values().stream().filter(pkg -> filter.matcher(pkg.getName()).matches())
						.collect(toList());
			}
			List<PlatformPackage> result = new ArrayList<>();
			for ( PlatformPackage pkg : installed.values() ) {
				if ( filter == null || filter.matcher(pkg.getName()).matches() ) {
					result.add(pkg);
				}
			}
			for ( PlatformPackage pkg : available.values() ) {
				if ( filter == null || filter.matcher(pkg.getName()).matches() ) {
					result.add(pkg);
				}
			}
			Collections.sort(result, (l, r) -> {
				String nl = l.getName();
				String nr = r.getName();
				return nl.compareToIgnoreCase(nr);
			});
			return result;

		});
	}

	@Override
	public Future<Iterable<PlatformPackage>> listUpgradableNamedPackages() {
		return performTask(() -> {
			return upgradable.values();
		});
	}

	private Pattern globPattern(String nameFilter) {
		if ( nameFilter == null || nameFilter.trim().isEmpty() ) {
			return null;
		}
		String[] parts = nameFilter.split("\\*");
		if ( parts.length == 1 ) {
			return Pattern.compile(Pattern.quote(nameFilter), Pattern.CASE_INSENSITIVE);
		}
		StringBuilder buf = new StringBuilder();
		for ( String part : parts ) {
			buf.append(Pattern.quote(part));
			buf.append(".*");
		}
		return Pattern.compile(buf.toString(), Pattern.CASE_INSENSITIVE);
	}

	@Override
	public Future<Boolean> refreshNamedPackages() {
		return performTask(() -> {
			loadPackages();
			return true;
		});
	}

	@Override
	public Future<Boolean> cleanup() {
		return CompletableFuture.completedFuture(Boolean.TRUE);
	}

	@Override
	public <T> Future<PlatformPackageResult<T>> installNamedPackage(String name, String version,
			Path baseDirectory, ProgressListener<T> progressListener, T context) {
		Callable<PlatformPackageResult<T>> task = new Callable<PlatformPackageService.PlatformPackageResult<T>>() {

			@Override
			public PlatformPackageResult<T> call() throws Exception {
				log.info("Installing named package {}", name);
				if ( available.containsKey(name) ) {
					installed.put(name, available.remove(name));
				} else if ( upgradable.containsKey(name) ) {
					installed.put(name, upgradable.remove(name));
				}
				progressListener.progressChanged(context, 1);
				return new BasicPlatformPackageResult<T>(true, null, null, Collections.emptyList(),
						context);
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
				log.info("Installing named package {}", name);
				if ( upgradable.containsKey(name) ) {
					available.put(name, upgradable.remove(name));
					installed.remove(name);
				} else if ( installed.containsKey(name) ) {
					available.put(name, installed.remove(name));
				}
				progressListener.progressChanged(context, 1);
				return new BasicPlatformPackageResult<T>(true, null, null, Collections.emptyList(),
						context);
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
				log.info("Upgrading named packages");
				final int count = upgradable.size();
				int i = 0;
				for ( Entry<String, PlatformPackage> e : upgradable.entrySet() ) {
					installed.put(e.getKey(), e.getValue());
					progressListener.progressChanged(context, (double) (++i) / (double) count);
				}
				upgradable.clear();
				return new BasicPlatformPackageResult<T>(true, null, null, Collections.emptyList(),
						context);
			}
		};
		return performPackageResultTask(task, context);
	}

}
