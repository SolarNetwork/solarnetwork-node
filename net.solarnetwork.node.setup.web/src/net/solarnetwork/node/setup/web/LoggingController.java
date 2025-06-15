/* ==================================================================
 * LoggingController.java - 25/02/2023 9:30:03 am
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

package net.solarnetwork.node.setup.web;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.service.LoggingService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.ObjectUtils;

/**
 * Controller to manage logging.
 *
 * @author matt
 * @version 1.1
 * @since 2.7
 */
@ServiceAwareController
@RequestMapping("/a/logging")
public class LoggingController {

	private final OptionalService<LoggingService> loggingService;

	/**
	 * Constructor.
	 *
	 * @param loggingService
	 *        the logging service
	 */
	public LoggingController(
			@Qualifier("loggingService") OptionalService<LoggingService> loggingService) {
		super();
		this.loggingService = ObjectUtils.requireNonNullArgument(loggingService, "loggingService");
	}

	/**
	 * Logging UI.
	 *
	 * @return the logging view name
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public String filterSettingsList() {
		return "logging";
	}

	/**
	 * Get all active loggers.
	 *
	 * @return the result
	 */
	@RequestMapping(value = "/loggers", method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Collection<String>> loggers() {
		final LoggingService service = OptionalService.service(loggingService);
		if ( service == null ) {
			return Result.error("SRV.0001", "LoggingService not avaialble.");
		}
		return Result.success(service.loggers());
	}

	/**
	 * Get all configured logging levels.
	 *
	 * @return the result
	 */
	@RequestMapping(value = "/levels", method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Map<String, LoggingService.Level>> loggingLevels() {
		final LoggingService service = OptionalService.service(loggingService);
		if ( service == null ) {
			return Result.error("SRV.0001", "LoggingService not avaialble.");
		}
		return Result.success(service.loggerLevels());
	}

	/**
	 * Set the logging level for a logger.
	 *
	 * @param logger
	 *        the name of the logger to update
	 * @param level
	 *        the level to set
	 * @return the result
	 */
	@RequestMapping(value = "/levels", method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Void> setLoggingLevels(@RequestParam("logger") String logger,
			@RequestParam("level") String level) {
		final LoggingService service = OptionalService.service(loggingService);
		if ( service == null ) {
			return Result.error("SRV.0001", "LoggingService not avaialble.");
		}
		service.changeLevels(Collections.singletonMap(logger, LoggingService.Level.forName(level)));
		return Result.success();
	}

}
