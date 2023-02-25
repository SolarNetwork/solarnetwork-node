/* ==================================================================
 * LoggingService.java - 24/02/2023 1:42:40 pm
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

package net.solarnetwork.node.service;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.util.ObjectUtils;

/**
 * Service API for managing logging at runtime.
 * 
 * @author matt
 * @version 1.0
 * @since 2.10
 */
public interface LoggingService {

	/**
	 * Standard enumeration of supported logging levels, or verbosity.
	 */
	enum Level {

		/** Inherit the level of a "parent" logger. */
		INHERIT,

		/** The highest level of verbosity for tracing messages. */
		TRACE,

		/** The 2nd highest level of verbosity for debugging messages. */
		DEBUG,

		/** A "default" level of verbosity for informational messages. */
		INFO,

		/** The 2nd most urgent level of verbosity for potential problems. */
		WARN,

		/** The most urgent level of verbosity for error conditions. */
		ERROR,

		/** No logging at all. */
		OFF;

		/**
		 * Get a level for a case-insensitive level name.
		 * 
		 * @param name
		 *        the name of the level to get
		 * @return the level
		 * @throws IllegalArgumentException
		 *         if {@code name} is not valid
		 */
		public static final Level forName(String name) {
			name = ObjectUtils.requireNonNullArgument(name, "name").toUpperCase(Locale.ROOT);
			return Level.valueOf(name);
		}

	}

	/**
	 * The instruction topic for a request to set a logger level to one or more
	 * loggers.
	 * 
	 * <p>
	 * The following instruction parameters must be provided:
	 * </p>
	 * 
	 * <dl>
	 * <dt>logger</dt>
	 * <dd>The name of the logger to update the level of, for example
	 * <code>net.solarnetwork.node.io</code>, or a comma-delimited list of
	 * loggers. This parameter can also be repeated multiple times to change the
	 * level of multiple loggers at once.</dd>
	 * <dt>level</dt>
	 * <dd>The logger level, one of the {@link LoggingService.Level} enumeration
	 * values (case-insensitive).</dd>
	 * </dl>
	 */
	String TOPIC_UPDATE_LOGGER = "LoggingSetLevel";

	/** The instruction parameter for a logger name. */
	String PARAM_LOGGER_NAME = "logger";

	/** The instruction parameter for a {@link LoggingService.Level} name. */
	String PARAM_LOGGER_LEVEL = "level";

	/**
	 * Get a collection of all known loggers.
	 * 
	 * <p>
	 * This represents the list of active loggers in the system.
	 * </p>
	 * 
	 * @return the loggers, never {@literal null}
	 */
	Collection<String> loggers();

	/**
	 * Get all available logger levels.
	 * 
	 * <p>
	 * This represents the current runtime configuration of logger levels, which
	 * may be lost if the application is restarted.
	 * </p>
	 * 
	 * @return the current logger levels, never {@literal null}
	 */
	Map<String, Level> loggerLevels();

	/**
	 * Adjust logger levels.
	 * 
	 * @param levels
	 *        a mapping of logger names to desired levels
	 */
	void changeLevels(Map<String, Level> levels);
}
