/* ==================================================================
 * LocalStateController.java - 15/04/2025 9:08:33 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.Collections;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.domain.LocalState;
import net.solarnetwork.node.service.LocalStateService;
import net.solarnetwork.node.setup.web.support.LocalStateInfo;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;

/**
 * Controller to manage local state.
 *
 * @author matt
 * @version 1.0
 * @since 4.11
 */
@ServiceAwareController
@RequestMapping("/a/local-state")
public class LocalStateController extends BaseSetupWebServiceController {

	private final LocalStateService localStateService;

	/**
	 * Constructor.
	 *
	 * @param localStateService
	 *        the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LocalStateController(LocalStateService localStateService) {
		super();
		this.localStateService = requireNonNullArgument(localStateService, "localStateService");
	}

	/**
	 * Local State UI.
	 *
	 * @return the Local State view name
	 */
	@GetMapping(value = { "", "/" }, produces = MediaType.TEXT_HTML_VALUE)
	public ModelAndView localStateUi() {
		return new ModelAndView("locstate",
				Collections.singletonMap("entities", localStateService.getAvailableLocalState()));
	}

	/**
	 * Local State UI.
	 *
	 * @return the Local State view name
	 */
	@GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<Collection<LocalState>> listLocalState() {
		return success(localStateService.getAvailableLocalState());
	}

	/**
	 * Save a local state entity.
	 *
	 * @param info
	 *        the local state info
	 * @return the persisted state entity
	 */
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<LocalState> saveLocalState(LocalStateInfo info) {
		LocalState input = null;
		if ( info != null ) {
			input = info.toLocalState();
		}
		if ( input == null ) {
			return success();
		}
		return success(localStateService.saveLocalState(input));
	}

	/**
	 * Delete a local state entity.
	 *
	 * @param key
	 *        the ID of the entity to delete
	 * @return the result
	 */
	@DeleteMapping
	@ResponseBody
	public Result<Void> deleteSecurityToken(@RequestParam("key") String key) {
		localStateService.deleteLocalState(key);
		return success();
	}

}
