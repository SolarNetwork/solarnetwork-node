/* ==================================================================
 * PlatformService.java - 21/11/2017 10:33:13 AM
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

package net.solarnetwork.node;

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * API for SolarNode platform-wide support.
 * 
 * @author matt
 * @version 1.0
 * @since 1.56
 */
public interface PlatformService {

	/**
	 * An {@link org.osgi.service.event.Event} topic for when a the platform
	 * state has changed.
	 * 
	 * <p>
	 * The properties of the event shall be any of the JavaBean properties of
	 * the Datum supported by events (i.e. any simple Java property such as
	 * numbers and strings). In addition, the
	 * {@link #EVENT_DATUM_CAPTURED_DATUM_TYPE} property shall be populated with
	 * the name of the <em>core</em> class name of the datum type.
	 * </p>
	 */
	String EVENT_TOPIC_PLATFORM_STATE_CHANGED = "net/solarnetwork/node/PlatformService/STATE_CHANGED";

	/**
	 * A {@link org.osgi.service.event.Event} property name for a
	 * {@link PlatformState} string value that represents the "active" state.
	 */
	String PLATFORM_STATE_PROPERTY = "platformState";

	/**
	 * A {@link org.osgi.service.event.Event} property name for a
	 * {@link PlatformState} string value that represents an "old" state.
	 */
	String OLD_PLATFORM_STATE_PROPERTY = "oldPlatformState";

	/**
	 * An enumeration of possible platform states.
	 */
	enum PlatformState {
		/** A normal state, without any specific restrictions in place. */
		Normal,

		/**
		 * A task is being performed that restricts user interactions until the
		 * task completes.
		 */
		UserBlockingSystemTask,

	}

	/**
	 * Information about a task.
	 */
	interface PlatformTaskInfo {

		/**
		 * Get a title of this task.
		 * 
		 * @return the title
		 */
		String getTitle();

		/**
		 * Get a message describing what the task is currently doing.
		 * 
		 * @return the message
		 */
		String getMessage();

		/**
		 * Get the amount of work that has been completed, as a fractional
		 * percentage between {@literal 0} and {@literal 1}.
		 * 
		 * @return the amount of work completed, or anything < 0 if not known
		 */
		double getPercentComplete();

		/**
		 * Get a flag that indicates if the task is complete.
		 * 
		 * @return the complete flag
		 */
		boolean isComplete();

		/**
		 * Get a flag that indicates if a system restart is required after the
		 * task completes.
		 * 
		 * @return a restart required flag
		 */
		boolean isRestartRequired();

	}

	/**
	 * Status information about a task.
	 */
	interface PlatformTaskStatus {

		/**
		 * Get a title of this task.
		 * 
		 * @param locale
		 *        the desired locale of the title
		 * @return the title
		 */
		String getTitle(Locale locale);

		/**
		 * Get a message describing what the task is currently doing.
		 * 
		 * @param locale
		 *        the desired locale of the message
		 * @return the message
		 */
		String getMessage(Locale locale);

		/**
		 * Get the amount of work that has been completed, as a fractional
		 * percentage between {@literal 0} and {@literal 1}.
		 * 
		 * @return the amount of work completed, or anything < 0 if not known
		 */
		double getPercentComplete();

		/**
		 * Get a flag that indicates if the task is complete.
		 * 
		 * @return the complete flag
		 */
		boolean isComplete();

		/**
		 * Get a flag that indicates if a system restart is required after the
		 * task completes.
		 * 
		 * @return a restart required flag
		 */
		boolean isRestartRequired();
	}

	/**
	 * A platform task.
	 *
	 * @param <T>
	 *        the task result
	 */
	interface PlatformTask<T> extends Callable<T>, PlatformTaskStatus {

	}

	/**
	 * Get the current active platform state.
	 * 
	 * @return the active platform state
	 */
	PlatformState activePlatformState();

	/**
	 * Get the current active platform task status.
	 * 
	 * @return the task info, or {@literal null} if no task is active
	 */
	PlatformTaskStatus activePlatformTaskStatus();

	/**
	 * Get the current active platform task status, localized into task info.
	 * 
	 * @param locale
	 *        the desired locale of the info, or {@literal null} for the system
	 *        default
	 * @return the localized platform task info
	 */
	PlatformTaskInfo activePlatformTaskInfo(Locale locale);

	/**
	 * Perform a platform state-altering task.
	 * 
	 * <p>
	 * This method will schedule a task such that while that task is running the
	 * provided state is applied to the platform. While {@code task} is
	 * executing the {@link #activePlatformState()} method will return the given
	 * {@code state}.
	 * </p>
	 * 
	 * @param state
	 *        the state the task must run with
	 * @param task
	 *        the task to execute
	 * @return a future with the task results
	 * @param <T>
	 *        the task result type
	 */
	<T> Future<T> performTaskWithState(PlatformState state, PlatformTask<T> task);

}
