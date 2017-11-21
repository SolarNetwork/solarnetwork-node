/* ==================================================================
 * DefaultPlatformService.java - 21/11/2017 10:51:28 AM
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

package net.solarnetwork.node.runtime;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import net.solarnetwork.node.PlatformService;
import net.solarnetwork.util.OptionalService;

/**
 * Default implementation of {@link PlatformService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultPlatformService implements PlatformService {

	private final AtomicInteger activeState = new AtomicInteger(PlatformState.Normal.ordinal());
	private final AtomicReference<PlatformTask<?>> activeSingletonTask = new AtomicReference<PlatformTask<?>>();
	private ExecutorService singletonExecutorService = defaultSingletonExecutorService();
	private OptionalService<EventAdmin> eventAdmin;

	private static ExecutorService defaultSingletonExecutorService() {
		// we want at most one task happening at a time for this service;
		// size the blocking queue to a reasonable size for a typical node
		return new ThreadPoolExecutor(0, 1, 5, TimeUnit.MINUTES,
				new ArrayBlockingQueue<Runnable>(10, true),
				new CustomizableThreadFactory("PlatformService-Main-"));
	}

	@Override
	public PlatformState activePlatformState() {
		return PlatformState.values()[activeState.get()];
	}

	private void updatePlatformState(PlatformState state) {
		PlatformState oldState = PlatformState.values()[activeState.getAndSet(state.ordinal())];
		if ( oldState == state ) {
			return;
		}
		OptionalService<EventAdmin> optEa = eventAdmin;
		EventAdmin ea = (optEa != null ? optEa.service() : null);
		if ( ea == null ) {
			return;
		}
		Map<String, Object> props = new HashMap<String, Object>(2);
		props.put(PLATFORM_STATE_PROPERTY, state.toString());
		props.put(OLD_PLATFORM_STATE_PROPERTY, oldState.toString());
		Event event = new Event(EVENT_TOPIC_PLATFORM_STATE_CHANGED, props);
		ea.postEvent(event);
	}

	@Override
	public PlatformTaskStatus activePlatformTaskStatus() {
		return activeSingletonTask.get();
	}

	@Override
	public PlatformTaskInfo activePlatformTaskInfo(Locale locale) {
		PlatformTaskStatus status = activePlatformTaskStatus();
		if ( status == null ) {
			return null;
		}
		return new SimplePlatformTaskInfo(status, locale);
	}

	@Override
	public <T> Future<T> performTaskWithState(final PlatformState state, final PlatformTask<T> task) {
		return singletonExecutorService.submit(new Callable<T>() {

			@Override
			public T call() throws Exception {
				try {
					if ( !activeSingletonTask.compareAndSet(null, task) ) {
						throw new IllegalStateException(
								"Another task is active; cannot start task " + task);
					}
					updatePlatformState(state);
					return task.call();
				} finally {
					activeSingletonTask.compareAndSet(task, null);
					updatePlatformState(PlatformState.Normal);
				}
			}

		});
	}

	/**
	 * Configure the {@link ExecutorService} to use for the singleton task
	 * queue.
	 * 
	 * <p>
	 * This service is expected to perform just one task at a time.
	 * </p>
	 * 
	 * @param singletonExecutorService
	 *        the service to use; defaults to a service using an array-based
	 *        blocking queue with a single thread
	 */
	public void setSingletonExecutorService(ExecutorService singletonExecutorService) {
		this.singletonExecutorService = singletonExecutorService;
	}

	/**
	 * Set the {@link EventAdmin} to use.
	 * 
	 * @param eventAdmin
	 *        the service to use
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
