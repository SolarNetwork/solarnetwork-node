/* ==================================================================
 * BasePlatformPackageService.java - 22/05/2019 6:23:58 pm
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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import net.solarnetwork.node.PlatformPackageService;
import net.solarnetwork.util.OptionalService;

/**
 * Base implementation of {@link PlatformPackageService}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.68
 */
public abstract class BasePlatformPackageService implements PlatformPackageService {

	private OptionalService<AsyncTaskExecutor> taskExecutor;

	/** A class-level logger. */
	protected Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Perform the extraction task, using the configured
	 * {@link AsyncTaskExecutor} if available.
	 * 
	 * <p>
	 * If {@link #getTaskExecutor()} is {@literal null}, the task will be
	 * executed on the calling thread.
	 * </p>
	 * 
	 * @param <T>
	 *        the context type
	 * @param task
	 *        the task
	 * @param context
	 *        the context
	 * @return the task future
	 */
	protected <T> Future<PlatformPackageResult<T>> performPackageResultTask(
			Callable<PlatformPackageResult<T>> task, T context) {
		AsyncTaskExecutor executor = taskExecutor();
		if ( executor != null ) {
			return executor.submit(task);
		}

		// execute on calling thread
		CompletableFuture<PlatformPackageResult<T>> future = new CompletableFuture<>();
		try {
			future.complete(task.call());
		} catch ( Throwable t ) {
			future.complete(new BasicPlatformPackageResult<T>(false, t.getMessage(), t, null, context));
		}
		return future;
	}

	/**
	 * Perform the extraction task, using the configured
	 * {@link AsyncTaskExecutor} if available.
	 * 
	 * <p>
	 * If {@link #getTaskExecutor()} is {@literal null}, the task will be
	 * executed on the calling thread.
	 * </p>
	 * 
	 * @param <T>
	 *        the context type
	 * @param task
	 *        the task
	 * @return the task future
	 */
	protected <T> Future<T> performTask(Callable<T> task) {
		AsyncTaskExecutor executor = taskExecutor();
		if ( executor != null ) {
			return executor.submit(task);
		}

		// execute on calling thread
		CompletableFuture<T> future = new CompletableFuture<>();
		try {
			future.complete(task.call());
		} catch ( Throwable t ) {
			future.completeExceptionally(t);
		}
		return future;
	}

	/**
	 * Get the configured task executor.
	 * 
	 * @return the task executor, or {@literal null}.
	 */
	protected AsyncTaskExecutor taskExecutor() {
		OptionalService<AsyncTaskExecutor> os = getTaskExecutor();
		return (os != null ? os.service() : null);
	}

	/**
	 * Get the task executor.
	 * 
	 * @return the task executor
	 */
	public OptionalService<AsyncTaskExecutor> getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Set the task executor.
	 * 
	 * @param taskExecutor
	 *        the task executor
	 */
	public void setTaskExecutor(OptionalService<AsyncTaskExecutor> taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
