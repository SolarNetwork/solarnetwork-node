/* ==================================================================
 * ResourceStorageServiceDirectoryWatcher.java - 16/10/2019 5:28:13 pm
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

package net.solarnetwork.node.upload.resource;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.service.ResourceStorageService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Service to watch a directory for file changes, and when a change is
 * encountered then copy the file to a {@link ResourceStorageService}.
 *
 * @author matt
 * @version 2.1
 */
public class ResourceStorageServiceDirectoryWatcher extends BaseIdentifiable
		implements SettingSpecifierProvider, SettingsChangeObserver, ProgressListener<Resource> {

	/** The default value for the {@code filter} property. */
	public static final Pattern DEFAULT_FILTER = Pattern.compile(".+\\..+");

	/** The default value for the {@code saveDelay} property. */
	public static final long DEFAULT_SAVE_DELAY = 10000L;

	private final ConcurrentMap<Path, ScheduledFuture<?>> delayedSaves = new ConcurrentHashMap<>(8, 0.9f,
			1);

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OptionalService<ResourceStorageService> storageService;
	private final Executor executor;
	private final AtomicIntegerArray statistics;
	private TaskScheduler taskScheduler;
	private OptionalService<DatumQueue> datumQueue;
	private String resourceStorageDatumSourceId;
	private String path;
	private boolean recursive;
	private Pattern filter;
	private long saveDelay = DEFAULT_SAVE_DELAY;

	private Throwable watchException;
	private Watcher watchThread;

	/**
	 * Constructor.
	 *
	 * @param storageService
	 *        the storage service to use
	 * @param executor
	 *        the executor to use
	 */
	public ResourceStorageServiceDirectoryWatcher(OptionalService<ResourceStorageService> storageService,
			Executor executor) {
		super();
		if ( storageService == null ) {
			throw new IllegalArgumentException("The storageService argument must not be null.");
		}
		this.storageService = storageService;
		if ( executor == null ) {
			throw new IllegalArgumentException("The executor argument must not be null.");
		}
		this.executor = executor;
		this.statistics = new AtomicIntegerArray(Statistic.values().length);
		this.filter = DEFAULT_FILTER;
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		restartWatchThread();
	}

	/**
	 * Start up the watcher thread.
	 */
	public void startup() {
		restartWatchThread();
	}

	/**
	 * Stop the watcher thread.
	 */
	public synchronized void shutdown() {
		if ( watchThread != null ) {
			watchThread.cancel();
			watchThread.interrupt();
		}
	}

	private synchronized void restartWatchThread() {
		if ( watchThread != null ) {
			watchThread.cancel();
			watchThread.interrupt();
			watchThread = null;
		}
		if ( path != null && !path.isEmpty() ) {
			try {
				watchException = null;
				watchThread = new Watcher(Paths.get(path), recursive);
				watchThread.start();
			} catch ( IOException e ) {
				log.error("Error starting watch thread: {}", e.getMessage(), e);
				watchException = e;
			}
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.upload.resource.dirwatcher";
	}

	@Override
	public String getDisplayName() {
		return "Storage Service Directory Watcher";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(4);
		result.add(new BasicTitleSettingSpecifier("status", statusValue(Locale.getDefault()), true));
		result.add(new BasicTextFieldSettingSpecifier("storageService.propertyFilters['uid']", null,
				false, "(objectClass=net.solarnetwork.service.ResourceStorageService)"));
		result.add(new BasicTextFieldSettingSpecifier("path", ""));
		result.add(new BasicTextFieldSettingSpecifier("filterValue", DEFAULT_FILTER.pattern()));
		result.add(new BasicToggleSettingSpecifier("recursive", Boolean.FALSE));
		result.add(new BasicTextFieldSettingSpecifier("saveDelay", String.valueOf(DEFAULT_SAVE_DELAY)));
		result.add(new BasicTextFieldSettingSpecifier("resourceStorageDatumSourceId", ""));
		return result;
	}

	private synchronized String statusValue(Locale locale) {
		MessageSource ms = getMessageSource();
		if ( watchException != null ) {
			return ms.getMessage("status.exception", new Object[] { watchException.getMessage() },
					locale);
		} else if ( watchThread == null || !watchThread.isAlive() ) {
			return ms.getMessage("status.notWatching", null, locale);
		}
		Object[] stats = new Object[statistics.length()];
		for ( int i = 0; i < stats.length; i++ ) {
			stats[i] = statistics.get(i);
		}
		return ms.getMessage("status.running", stats, locale);
	}

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	private void saveResource(String savePath, Path resourcePath) {
		ResourceStorageService service = storageService.service();
		if ( service == null ) {
			log.info("No ResourceStorageService available: cannot save {}", resourcePath);
			return;
		}
		log.info("Saving resource {} to {}", resourcePath, service);
		service.saveResource(savePath, new FileSystemResource(resourcePath.toFile()), true, this)
				.whenCompleteAsync((r, e) -> {
					if ( e != null ) {
						statistics.incrementAndGet(Statistic.Failed.ordinal());
						log.error("Error saving resource {} to {}: {}", resourcePath, service,
								e.getMessage(), e);
					} else {
						statistics.incrementAndGet(Statistic.Saved.ordinal());
						log.info("Resource {} saved to {}.", resourcePath, service);
					}

					// save any generated datum in a new thread so we don't block ForkJoinPool
					executor.execute(new Runnable() {

						@Override
						public void run() {
							generateDatum(service, savePath, resourcePath);
						}

					});
				});
	}

	private long effectiveSaveDelay(TaskScheduler scheduler) {
		return (scheduler != null ? getSaveDelay() : 0);
	}

	private void generateDatum(ResourceStorageService service, String savePath, Path resourcePath) {
		final String sourceId = getResourceStorageDatumSourceId();
		if ( sourceId == null || sourceId.isEmpty() ) {
			return;
		}

		if ( service == null ) {
			log.warn("No ResourceStorageService available: cannot generate datum for source {}",
					sourceId);
			return;
		}

		final DatumQueue queue = service(datumQueue);
		if ( queue == null ) {
			log.warn("No DatumQueue available: cannot generate datum for source {}", sourceId);
			return;
		}

		Instant ts = Instant.now();
		if ( Files.isReadable(resourcePath) ) {
			try {
				ts = Files.getLastModifiedTime(resourcePath).toInstant();
			} catch ( IOException e ) {
				log.warn("Unable to determine modified time of resource {}: {}", resourcePath,
						e.toString());
			}
		}

		final SimpleDatum d = SimpleDatum.nodeDatum(sourceId, ts, new DatumSamples());
		final URL resourceStorageUrl = service.resourceStorageUrl(savePath);
		if ( resourceStorageUrl != null ) {
			d.getSamples().putStatusSampleValue("url", resourceStorageUrl.toString());
		}
		if ( savePath != null ) {
			d.getSamples().putStatusSampleValue("path", savePath);
		}
		if ( Files.isReadable(resourcePath) ) {
			try {
				d.getSamples().putInstantaneousSampleValue("size", Files.size(resourcePath));
			} catch ( IOException e ) {
				log.warn("Unable to determine size of resource {}: {}", resourcePath, e.toString());
			}
		}

		if ( d.getSamples() == null || d.getSamples().isEmpty() ) {
			log.warn("No {} datum properties available for saved resource to storage service {}",
					sourceId, service.getUid());
		}

		log.info("Generated resource storage datum {}", d);

		queue.offer(d);
	}

	@Override
	public void progressChanged(Resource context, double amountComplete) {
		log.info("{}% complete saving resource {} to {}", String.format("%.1f", amountComplete * 100),
				context.getFilename(), storageService.service());
	}

	private enum Statistic {
		Created,
		Modified,
		Saved,
		Failed,
		Ignored,
	}

	private class Watcher extends Thread {

		private final Path root;
		private final Map<WatchKey, Path> keys;
		private final boolean recursive;

		private boolean keepGoing;

		private Watcher(Path dir, boolean recursive) throws IOException {
			super("StorageServiceWatcher - " + dir);
			this.root = dir;
			this.keys = new HashMap<WatchKey, Path>();
			this.recursive = recursive;
			this.keepGoing = true;

		}

		private void cancel() {
			keepGoing = false;
		}

		private void register(Path dir, WatchService watcher) throws IOException {
			WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
			log.info("Watching directory for changes: {}", dir);
			keys.put(key, dir);
		}

		private void registerAll(final Path start, final WatchService watcher) throws IOException {
			Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
						throws IOException {
					register(dir, watcher);
					return FileVisitResult.CONTINUE;
				}
			});
		}

		@Override
		public void run() {
			try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
				if ( recursive ) {
					registerAll(root, watcher);
				} else {
					register(root, watcher);
				}
				while ( keepGoing ) {
					WatchKey key;
					try {
						key = watcher.take();
					} catch ( InterruptedException x ) {
						return;
					}

					Path dir = keys.get(key);
					if ( dir == null ) {
						continue;
					}

					final TaskScheduler scheduler = getTaskScheduler();
					final Pattern filenameFilter = getFilter();

					for ( WatchEvent<?> event : key.pollEvents() ) {
						WatchEvent.Kind<?> kind = event.kind();

						if ( kind == OVERFLOW ) {
							continue;
						}

						// Context for directory entry event is the file name of entry
						WatchEvent<Path> ev = cast(event);
						Path name = ev.context();
						Path child = dir.resolve(name);

						log.debug("Watched dir event {}: {}", event.kind().name(), child);

						// if directory is created, and watching recursively, then
						// register it and its sub-directories
						try {
							if ( recursive && (kind == ENTRY_CREATE)
									&& Files.isDirectory(child, NOFOLLOW_LINKS) ) {
								registerAll(child, watcher);
							} else if ( Files.isRegularFile(child, NOFOLLOW_LINKS) ) {
								if ( kind == ENTRY_CREATE ) {
									statistics.incrementAndGet(Statistic.Created.ordinal());
								} else if ( kind == ENTRY_MODIFY ) {
									statistics.incrementAndGet(Statistic.Modified.ordinal());
								}
								if ( filenameFilter != null && !filenameFilter
										.matcher(name.getFileName().toString()).matches() ) {
									statistics.incrementAndGet(Statistic.Ignored.ordinal());
									log.debug("Ignoring watched file {} that does not match filter `{}`",
											child, filenameFilter);
									continue;
								}
								Path rel = root.relativize(child);
								long delay = effectiveSaveDelay(scheduler);
								if ( delay > 0 ) {
									delayedSaves.compute(child, (p, f) -> {
										if ( f != null ) {
											// cancel existing (coalesce events)
											f.cancel(false);
										}
										log.info("Delaying save of watched file {} for {}ms", child,
												delay);
										AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
										Runnable saveTask = new Runnable() {

											@Override
											public void run() {
												try {
													saveResource(rel.toString(), child);
												} finally {
													ScheduledFuture<?> future = futureRef.get();
													if ( future != null ) {
														delayedSaves.remove(child, future);
													}
												}
											}
										};
										ScheduledFuture<?> future = scheduler.schedule(saveTask,
												new Date(System.currentTimeMillis() + delay));
										futureRef.set(future);
										return future;
									});
								} else {
									saveResource(rel.toString(), child);
								}
							}
						} catch ( IOException e ) {
							statistics.incrementAndGet(Statistic.Failed.ordinal());
							log.error("Error handling file change event {} on {}: {}", kind, child,
									e.toString());
						}
					}

					// reset key and remove from set if directory no longer accessible
					boolean valid = key.reset();
					if ( !valid ) {
						keys.remove(key);
						if ( keys.isEmpty() ) {
							log.info("Watched dir is no longer accessible: {}", dir);
							break;
						}
					}
				}
			} catch ( Exception t ) {
				synchronized ( ResourceStorageServiceDirectoryWatcher.this ) {
					log.error("Error watching directory {}: {}", root, t.toString(), t);
					watchException = t;
				}
			}
		}

	}

	/**
	 * Get the path to watch for changes.
	 *
	 * @return the path to watch
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path to watch for changes.
	 *
	 * @param path
	 *        the path to watch
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Get the recursive watch setting.
	 *
	 * @return {@literal true} to watch all sub directories within {@code path},
	 *         {@literal false} to only watch {@code path} itself
	 */
	public boolean isRecursive() {
		return recursive;
	}

	/**
	 * Set the recursive watch setting.
	 *
	 * @param recursive
	 *        {@literal true} to watch all sub directories within {@code path},
	 *        {@literal false} to only watch {@code path} itself
	 */
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	/**
	 * Get the file name filter pattern.
	 *
	 * <p>
	 * Only file names that match this pattern will be saved.
	 * </p>
	 *
	 * @return the file name filter pattern
	 */
	public Pattern getFilter() {
		return filter;
	}

	/**
	 * Set the file name filter pattern.
	 *
	 * <p>
	 * Only file names that match this pattern will be saved.
	 * </p>
	 *
	 * @param filter
	 *        the file name filter pattern to use, or {@literal null} for all
	 *        files
	 */
	public void setFilter(Pattern filter) {
		this.filter = filter;
	}

	/**
	 * Get the file name filter pattern, as a string.
	 *
	 * <p>
	 * Only file names that match this pattern will be saved.
	 * </p>
	 *
	 * @return the file name filter pattern
	 */
	public String getFilterValue() {
		Pattern f = getFilter();
		return f != null ? f.pattern() : null;
	}

	/**
	 * Set the file name filter pattern, as a string.
	 *
	 * <p>
	 * Only file names that match this pattern will be saved.
	 * </p>
	 *
	 * @param filterValue
	 *        the file name filter pattern to use, or {@literal null} for all
	 *        files
	 */
	public synchronized void setFilterValue(String filterValue) {
		if ( watchException instanceof PatternSyntaxException ) {
			watchException = null;
		}
		try {
			setFilter(Pattern.compile(filterValue, Pattern.CASE_INSENSITIVE));
		} catch ( PatternSyntaxException e ) {
			log.error("Invalid filter pattern `{}`: {}", filterValue, e.getMessage());
			this.watchException = e;
		}
	}

	/**
	 * Get the datum queue to offer generated datum to.
	 *
	 * @return the queue
	 */
	public OptionalService<DatumQueue> getDatumQueue() {
		return datumQueue;
	}

	/**
	 * Set the datum queue to offer generated datum to.
	 *
	 * @param datumQueue
	 *        the datum queue to set
	 */
	public void setDatumQueue(OptionalService<DatumQueue> datumQueue) {
		this.datumQueue = datumQueue;
	}

	/**
	 * Get the source ID to use for datum generated in response to resource
	 * storage events.
	 *
	 * @return the resourceStorageDatumSourceId the source ID
	 */
	public String getResourceStorageDatumSourceId() {
		return resourceStorageDatumSourceId;
	}

	/**
	 * Set the source ID to use for datum generated in response to resource
	 * storage events.
	 *
	 * @param resourceStorageDatumSourceId
	 *        the source ID to set
	 */
	public void setResourceStorageDatumSourceId(String resourceStorageDatumSourceId) {
		this.resourceStorageDatumSourceId = resourceStorageDatumSourceId;
	}

	/**
	 * Get the task scheduler to handle delayed save operations with.
	 *
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler to handle delayed save operations with.
	 *
	 * <p>
	 * This scheduler is used only if {@link #getSaveDelay()} is greater than
	 * {@literal 0}, in which case the actual save operation is scheduled to run
	 * after this amount of delay.
	 * </p>
	 *
	 * @param taskScheduler
	 *        the task scheduler to use
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the save delay.
	 *
	 * @return the save delay, in milliseconds; defaults to
	 *         {@link #DEFAULT_SAVE_DELAY}
	 */
	public long getSaveDelay() {
		return saveDelay;
	}

	/**
	 * Set the save delay.
	 *
	 *
	 * <p>
	 * This delay, when greater than {@literal 0} will cause save operations
	 * triggered by file change events to be delayed by this amount of time. If
	 * additional events are monitored for a given resource before this delay
	 * has passed, the delay is restarted. In this way if a file is continually
	 * being modified it will not be saved until after the modifications stop.
	 * </p>
	 *
	 * <p>
	 * <b>Note</b> that for the delay to be used, a {@link #getTaskScheduler()}
	 * must be available.
	 * </p>
	 *
	 * @param saveDelay
	 *        the delay for save operations, in milliseconds
	 */
	public void setSaveDelay(long saveDelay) {
		this.saveDelay = saveDelay;
	}

}
