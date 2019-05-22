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
import org.springframework.core.task.AsyncTaskExecutor;
import net.solarnetwork.node.PlatformPackageService;

/**
 * Base implementation of {@link PlatformPackageService}.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BasePlatformPackageService implements PlatformPackageService {

	private AsyncTaskExecutor taskExecutor;

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
	protected <T> Future<PlatformPackageExtractResult<T>> performTask(
			Callable<PlatformPackageExtractResult<T>> task, T context) {
		AsyncTaskExecutor executor = getTaskExecutor();
		if ( executor != null ) {
			return executor.submit(task);
		}

		// execute on calling thread
		CompletableFuture<PlatformPackageExtractResult<T>> future = new CompletableFuture<>();
		try {
			future.complete(task.call());
		} catch ( Throwable t ) {
			future.complete(
					new BasicPlatformPackageExtractResult<T>(false, t.getMessage(), t, null, context));
		}
		return future;
	}

	/**
	 * Get the task executor.
	 * 
	 * @return the task executor
	 */
	public AsyncTaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Set the task executor.
	 * 
	 * @param taskExecutor
	 *        the task executor
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
