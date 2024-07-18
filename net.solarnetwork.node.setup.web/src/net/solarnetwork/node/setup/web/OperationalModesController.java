/* ==================================================================
 * OperationalModesController.java - 18/03/2023 6:08:29 pm
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.util.StringUtils;

/**
 * Controller for operational modes support.
 *
 * @author matt
 * @version 1.0
 * @since 3.2
 */
@ServiceAwareController
@RequestMapping("/a/opmodes")
public class OperationalModesController extends BaseSetupWebServiceController {

	private final OperationalModesService opModesService;

	/**
	 * Constructor.
	 *
	 * @param opModesService
	 *        the operational modes service
	 */
	@Autowired
	public OperationalModesController(OperationalModesService opModesService) {
		super();
		this.opModesService = opModesService;
	}

	/**
	 * OpModes UI.
	 *
	 * @return the operational modes view name
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public String opModesUi() {
		return "opmodes";
	}

	/**
	 * Get all active modes.
	 *
	 * @return the result
	 */
	@RequestMapping(value = "/active", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Set<String>> loggers() {
		return success(opModesService.activeOperationalModes());
	}

	/**
	 * Enable one or more modes.
	 *
	 * @param modes
	 *        the modes to enable, as a comma-delimited set
	 * @param expiration
	 *        an optional expiration date; can be an ISO local date time or an
	 *        integer seconds offset from now
	 * @return the resulting overall active modes
	 */
	@RequestMapping(value = "/active", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Set<String>> enableOperationalModes(@RequestParam("modes") String modes,
			@RequestParam(name = "expires", required = false) String expiration) {
		Instant expire = null;
		if ( expiration != null && !expiration.isEmpty() ) {
			try {
				LocalDateTime ts = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(expiration,
						LocalDateTime::from);
				expire = ts.atZone(ZoneId.systemDefault()).toInstant();
			} catch ( DateTimeParseException | IndexOutOfBoundsException e ) {
				// try as seconds duration
				try {
					long secs = Long.parseLong(expiration);
					expire = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(secs);
				} catch ( NumberFormatException nfe ) {
					throw new IllegalArgumentException("Cannot parse expiration [" + expiration
							+ "] as ISO local date time or seconds offset value.");
				}
			}
		}
		Set<String> modeSet = StringUtils.commaDelimitedStringToSet(modes);
		return success(opModesService.enableOperationalModes(modeSet, expire));
	}

	/**
	 * Disable one or more modes.
	 *
	 * @param modes
	 *        a comma-delimited set of modes to disable
	 * @return the resulting overall active modes
	 */
	@RequestMapping(value = "/active", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Set<String>> disableOperationalModes(@RequestBody List<String> modes) {
		Set<String> modeSet = new LinkedHashSet<>(modes);
		return success(opModesService.disableOperationalModes(modeSet));
	}

}
