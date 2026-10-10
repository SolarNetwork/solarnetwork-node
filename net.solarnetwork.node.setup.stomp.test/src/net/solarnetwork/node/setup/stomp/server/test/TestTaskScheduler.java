/* ==================================================================
 * TestTaskScheduler.java - 10/10/2026 1:42:00 PM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.stomp.server.test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

/**
 * A {@link TaskScheduler} that records tasks so tests can run them manually.
 *
 * @author elijah
 * @version 1.0
 */
public class TestTaskScheduler implements TaskScheduler {

	/** A recorded task. */
	public static final class Task implements ScheduledFuture<Object> {

		private final Runnable runnable;
		private final Instant start;
		private final Duration period;
		private boolean cancelled;
		private boolean done;

		private Task(Runnable runnable, Instant start, Duration period) {
			this.runnable = runnable;
			this.start = start;
			this.period = period;
		}

		/**
		 * Test if this is a periodic task.
		 *
		 * @return {@literal true} if periodic
		 */
		public boolean isPeriodic() {
			return period != null;
		}

		/**
		 * Get the start date.
		 *
		 * @return the start date
		 */
		public Instant getStart() {
			return start;
		}

		/**
		 * Get the period.
		 *
		 * @return the period, or {@literal null} for one-shot tasks
		 */
		public Duration getPeriod() {
			return period;
		}

		/**
		 * Run the task, if not cancelled. One-shot tasks become done.
		 */
		public void run() {
			if ( cancelled || done ) {
				return;
			}
			if ( period == null ) {
				done = true;
			}
			runnable.run();
		}

		/**
		 * Run the task even if it was cancelled, to simulate a task that had
		 * already started running when it was cancelled.
		 */
		public void forceRun() {
			runnable.run();
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed o) {
			return 0;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if ( done || cancelled ) {
				return false;
			}
			cancelled = true;
			return true;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return done || cancelled;
		}

		@Override
		public Object get() {
			return null;
		}

		@Override
		public Object get(long timeout, TimeUnit unit) {
			return null;
		}

	}

	private final List<Task> tasks = new ArrayList<>();

	private synchronized Task add(Runnable r, Instant start, Duration period) {
		Task t = new Task(r, start, period);
		tasks.add(t);
		return t;
	}

	/**
	 * Get all recorded tasks.
	 *
	 * @return the tasks
	 */
	public synchronized List<Task> getTasks() {
		return new ArrayList<>(tasks);
	}

	/**
	 * Get the one-shot tasks that are neither done nor cancelled.
	 *
	 * @return the pending tasks
	 */
	public synchronized List<Task> pendingOneShots() {
		return tasks.stream().filter(t -> !t.isPeriodic() && !t.isDone()).collect(Collectors.toList());
	}

	/**
	 * Get the periodic tasks that are not cancelled.
	 *
	 * @return the active periodic tasks
	 */
	public synchronized List<Task> activePeriodic() {
		return tasks.stream().filter(t -> t.isPeriodic() && !t.isCancelled())
				.collect(Collectors.toList());
	}

	/**
	 * Run all pending one-shot tasks scheduled to start at or before a date,
	 * including any scheduled while running.
	 *
	 * @param upTo
	 *        the date
	 * @return the number of tasks run
	 */
	public int runOneShots(Instant upTo) {
		int count = 0;
		while ( true ) {
			List<Task> ready = pendingOneShots().stream().filter(t -> !t.getStart().isAfter(upTo))
					.collect(Collectors.toList());
			if ( ready.isEmpty() ) {
				return count;
			}
			for ( Task t : ready ) {
				t.run();
				count++;
			}
		}
	}

	/**
	 * Run every active periodic task once.
	 *
	 * @return the number of tasks run
	 */
	public int runPeriodic() {
		List<Task> active = activePeriodic();
		for ( Task t : active ) {
			t.run();
		}
		return active.size();
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		return add(task, startTime, null);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		return add(task, startTime, period);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		return add(task, Instant.EPOCH, period);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		return add(task, startTime, delay);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		return add(task, Instant.EPOCH, delay);
	}

}
