/* ==================================================================
 * CapturingExecutorService.java - 30/07/2016 8:26:59 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Delegate all methods to a provided {@link ExecutorService} and capture all
 * returned {@link Future} instances for later inspection.
 * 
 * <p>
 * The {@link #execute(Runnable)} method will create a {@link RunnableFuture}
 * instance and proxy the given runnable so that a future is captured for those
 * tasks as well.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class CapturingExecutorService implements ExecutorService {

	private final ExecutorService delegate;

	private final List<Future<?>> futures = new ArrayList<Future<?>>(8);

	public CapturingExecutorService(ExecutorService delegate) {
		super();
		this.delegate = delegate;
	}

	/**
	 * Get the list of all captured {@link Future} instances created. This list
	 * can be modified as needed.
	 * 
	 * @return The list of captured futures.
	 */
	public List<Future<?>> getCapturedFutures() {
		return futures;
	}

	@Override
	public void execute(Runnable command) {
		RunnableFuture<Boolean> proxy = new FutureTask<Boolean>(command, Boolean.TRUE);
		futures.add(proxy);
		delegate.execute(proxy);
	}

	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return delegate.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return delegate.awaitTermination(timeout, unit);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		Future<T> future = delegate.submit(task);
		if ( future != null ) {
			futures.add(future);
		}
		return future;
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		Future<T> future = delegate.submit(task, result);
		if ( future != null ) {
			futures.add(future);
		}
		return future;
	}

	@Override
	public Future<?> submit(Runnable task) {
		Future<?> future = delegate.submit(task);
		if ( future != null ) {
			futures.add(future);
		}
		return future;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		List<Future<T>> allFutures = delegate.invokeAll(tasks);
		if ( allFutures != null ) {
			futures.addAll(allFutures);
		}
		return allFutures;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
			TimeUnit unit) throws InterruptedException {
		List<Future<T>> allFutures = delegate.invokeAll(tasks, timeout, unit);
		if ( allFutures != null ) {
			futures.addAll(allFutures);
		}
		return allFutures;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return delegate.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return delegate.invokeAny(tasks, timeout, unit);
	}

}
