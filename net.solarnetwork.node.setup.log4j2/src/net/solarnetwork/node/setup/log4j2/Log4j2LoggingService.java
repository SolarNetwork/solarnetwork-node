/* ==================================================================
 * Log4j2LoggingService.java - 24/02/2023 1:41:25 pm
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

package net.solarnetwork.node.setup.log4j2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.LoggingService;
import net.solarnetwork.util.StringUtils;

/**
 * Logging service implementation for log4j2.
 * 
 * @author matt
 * @version 1.0
 */
public class Log4j2LoggingService implements LoggingService, InstructionHandler {

	private static final Logger log = LoggerFactory.getLogger(Log4j2LoggingService.class);

	@Override
	public synchronized Collection<String> loggers() {
		final org.apache.logging.log4j.spi.LoggerContext ctxSpi = LogManager.getContext(true);
		if ( !(ctxSpi instanceof org.apache.logging.log4j.core.LoggerContext) ) {
			return Collections.emptyList();
		}
		@SuppressWarnings("resource")
		final LoggerContext ctx = (LoggerContext) ctxSpi;
		final List<String> result = new ArrayList<>(32);
		Collection<org.apache.logging.log4j.core.Logger> loggers = ctx.getLoggers();
		for ( org.apache.logging.log4j.core.Logger logger : loggers ) {
			result.add(logger.getName());
		}
		Collections.sort(result);
		return result;
	}

	@Override
	public synchronized Map<String, Level> loggerLevels() {
		final org.apache.logging.log4j.spi.LoggerContext ctxSpi = LogManager.getContext(true);
		if ( !(ctxSpi instanceof org.apache.logging.log4j.core.LoggerContext) ) {
			return Collections.emptyMap();
		}
		@SuppressWarnings("resource")
		final LoggerContext ctx = (LoggerContext) ctxSpi;
		final Configuration config = ctx.getConfiguration();

		final Map<String, Level> result = new LinkedHashMap<>(32);

		for ( Entry<String, LoggerConfig> e : config.getLoggers().entrySet() ) {
			result.put(e.getKey(), level(e.getValue().getLevel()));
		}

		return result;
	}

	@Override
	public synchronized void changeLevels(Map<String, Level> levels) {
		if ( levels == null || levels.isEmpty() ) {
			return;
		}
		final org.apache.logging.log4j.spi.LoggerContext ctxSpi = LogManager.getContext(true);
		if ( !(ctxSpi instanceof org.apache.logging.log4j.core.LoggerContext) ) {
			return;
		}

		@SuppressWarnings("resource")
		final LoggerContext ctx = (LoggerContext) ctxSpi;
		final Configuration config = ctx.getConfiguration();

		for ( Entry<String, Level> e : levels.entrySet() ) {
			setLogLevel(e.getKey(), e.getValue(), config);
		}
		ctx.updateLoggers();
	}

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_UPDATE_LOGGER.equalsIgnoreCase(topic);
	}

	@Override
	public synchronized InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		String[] loggerNames = instruction.getAllParameterValues(PARAM_LOGGER_NAME);
		if ( loggerNames == null || loggerNames.length < 1 ) {
			return InstructionUtils.createStatus(instruction, InstructionState.Declined,
					Collections.singletonMap(PARAM_MESSAGE, "No logger provided."));
		}
		LoggingService.Level level;
		try {
			level = LoggingService.Level.forName(instruction.getParameterValue(PARAM_LOGGER_LEVEL));
		} catch ( IllegalArgumentException e ) {
			return InstructionUtils.createStatus(instruction, InstructionState.Declined,
					Collections.singletonMap(PARAM_MESSAGE, String.format("Invalid logger level [%s].",
							instruction.getParameterValue(PARAM_LOGGER_LEVEL))));
		}

		final org.apache.logging.log4j.spi.LoggerContext ctxSpi = LogManager.getContext(true);
		if ( !(ctxSpi instanceof org.apache.logging.log4j.core.LoggerContext) ) {
			return InstructionUtils.createStatus(instruction, InstructionState.Declined,
					Collections.singletonMap(PARAM_MESSAGE,
							String.format("Unsupported LoggerContext detected [%s].",
									ctxSpi != null ? ctxSpi.getClass() : null)));
		}

		@SuppressWarnings("resource")
		final LoggerContext ctx = (LoggerContext) ctxSpi;
		final Configuration config = ctx.getConfiguration();

		for ( String loggerNameList : loggerNames ) {
			for ( String loggerName : StringUtils.commaDelimitedStringToSet(loggerNameList) ) {
				setLogLevel(loggerName, level, config);
			}
		}

		ctx.updateLoggers();

		return InstructionUtils.createStatus(instruction, InstructionState.Completed);
	}

	private void setLogLevel(String loggerName, LoggingService.Level level, final Configuration config) {
		if ( level == LoggingService.Level.INHERIT ) {
			// remove logger
			log.info("Configuring logger [{}] to inherit level", loggerName);
			config.removeLogger(loggerName);
		} else {
			// set logger level
			final org.apache.logging.log4j.Level log4jLevel = levelValue(level);
			log.info("Adjusting logger [{}] to level [{}]", loggerName, log4jLevel);
			LoggerConfig logConfig = config.getLoggerConfig(loggerName);
			if ( logConfig.getName().equals(loggerName) ) {
				logConfig.setLevel(log4jLevel);
			} else {
				// specific logger configuration not found, so create now
				logConfig = new LoggerConfig(loggerName, log4jLevel, true);
				config.addLogger(loggerName, logConfig);
			}
		}
	}

	private static final org.apache.logging.log4j.Level levelValue(Level level) {
		switch (level) {
			case INHERIT:
				return org.apache.logging.log4j.Level.ALL;

			case TRACE:
				return org.apache.logging.log4j.Level.TRACE;

			case DEBUG:
				return org.apache.logging.log4j.Level.DEBUG;

			case INFO:
				return org.apache.logging.log4j.Level.INFO;

			case WARN:
				return org.apache.logging.log4j.Level.WARN;

			case ERROR:
				return org.apache.logging.log4j.Level.ERROR;

			default:
				return org.apache.logging.log4j.Level.OFF;
		}
	}

	private static final Level level(org.apache.logging.log4j.Level level) {
		if ( level.isLessSpecificThan(org.apache.logging.log4j.Level.TRACE) ) {
			return Level.TRACE;
		} else if ( level.isLessSpecificThan(org.apache.logging.log4j.Level.DEBUG) ) {
			return Level.DEBUG;
		} else if ( level.isLessSpecificThan(org.apache.logging.log4j.Level.INFO) ) {
			return Level.INFO;
		} else if ( level.isLessSpecificThan(org.apache.logging.log4j.Level.WARN) ) {
			return Level.WARN;
		} else if ( level.isLessSpecificThan(org.apache.logging.log4j.Level.ALL) ) {
			return Level.ERROR;
		} else {
			return Level.OFF;
		}
	}

}
