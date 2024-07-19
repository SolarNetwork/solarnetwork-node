/* ==================================================================
 * CliConsoleController.java - 20/03/2023 4:35:06 pm
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

import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.node.service.OperationalModesService.withPrefix;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Controller for CLI Commands UI.
 *
 * @author matt
 * @version 1.0
 * @since 3.2
 */
@ServiceAwareController
@RequestMapping("/a/cli-console")
public class CliConsoleController {

	/** The operational mode prefix for CLI Command logging mdoes. */
	public static final String CLI_COMMANDS_OP_MODE_PREFIX = "cli-commands/";

	private final OperationalModesService opModesService;

	/**
	 * Constructor.
	 *
	 * @param opModesService
	 *        the operational modes service
	 */
	public CliConsoleController(OperationalModesService opModesService) {
		super();
		this.opModesService = ObjectUtils.requireNonNullArgument(opModesService, "opModesService");
	}

	/**
	 * CLI Console UI.
	 *
	 * @return the CLI Console view name
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public String cliConsoleUi() {
		return "cli-console";
	}

	private static final Set<String> typesForModes(Set<String> modes) {
		// @formatter:off
		return modes.stream()
				.filter(t -> t.startsWith(CLI_COMMANDS_OP_MODE_PREFIX))
				.map(t -> t.substring(CLI_COMMANDS_OP_MODE_PREFIX.length()))
				.collect(Collectors.toSet());
		// @formatter:on
	}

	private static final Set<String> typesForRegisteredModes(
			Stream<OperationalModesService.OperationalModeInfo> infos) {
		// @formatter:off
		return infos.map(e -> e.getName().substring(CLI_COMMANDS_OP_MODE_PREFIX.length()))
				.collect(Collectors.toSet());
		// @formatter:on
	}

	/**
	 * List the registered CLI Command logging types.
	 *
	 * @return the resulting set of "known" CLI Command types
	 */
	@RequestMapping(value = "/types", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Set<String>> listRegisteredCliCommandTypes() {
		return success(typesForRegisteredModes(opModesService.registeredOperationalModes()
				.filter(withPrefix(CLI_COMMANDS_OP_MODE_PREFIX))));
	}

	/**
	 * List the active CLI Command logging types.
	 *
	 * @return the resulting set of active CLI Command types
	 */
	@RequestMapping(value = "/logging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Set<String>> listActiveCliCommandLoggingTypes() {
		return success(typesForModes(opModesService.activeOperationalModes()));
	}

	/**
	 * Toggle CLI Command logging for a set of command types.
	 *
	 * @param enabled
	 *        {@literal true} to enable logging, {@literal false} to disable
	 * @param types
	 *        the types to toggle
	 * @return the resulting set of active CLI Command types
	 */
	@RequestMapping(value = "/logging", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Set<String>> toggleCliCommandLoggingEnabled(@RequestParam("enabled") boolean enabled,
			@RequestParam("types") String types) {
		Set<String> modes = StringUtils.commaDelimitedStringToSet(types).stream()
				.map(t -> CLI_COMMANDS_OP_MODE_PREFIX.concat(t)).collect(Collectors.toSet());

		Set<String> result;
		if ( enabled ) {
			result = opModesService.enableOperationalModes(modes);
		} else {
			result = opModesService.disableOperationalModes(modes);
		}
		return success(typesForModes(result));
	}

}
