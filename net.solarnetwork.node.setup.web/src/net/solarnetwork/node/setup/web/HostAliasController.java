/* ==================================================================
 * HostAliasController.java - 7/11/2023 9:41:57 am
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.service.SystemService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;

/**
 * Controller to manage host aliases.
 *
 * @author matt
 * @version 1.1
 * @since 3.5
 */
@ServiceAwareController
@RequestMapping("/a/hosts")
public class HostAliasController {

	@Autowired
	@Qualifier("systemService")
	private OptionalService<SystemService> systemService;

	@Autowired(required = true)
	private MessageSource messageSource;

	/**
	 * Constructor.
	 */
	public HostAliasController() {
		super();
	}

	/**
	 * Show host aliases home.
	 *
	 * @return the view name
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String home() {
		return "hosts";
	}

	private SystemService systemService(Locale locale) {
		SystemService service = OptionalService.service(systemService);
		if ( service != null ) {
			return service;
		}
		throw new UnsupportedOperationException(messageSource.getMessage("system.serviceNotAvailable",
				null, "SystemService not available", locale));
	}

	/**
	 * List host aliases.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the result, as a mapping of aliases to associated IP addresses,
	 *         sorted by alias
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ResponseBody
	public Result<List<KeyValuePair>> list(Locale locale) {
		SystemService service = systemService(locale);
		MultiValueMap<InetAddress, String> aliases = service.hostAliases();
		List<KeyValuePair> aliasList = new ArrayList<>(aliases.size());
		for ( Entry<InetAddress, List<String>> e : aliases.entrySet() ) {
			for ( String alias : e.getValue() ) {
				aliasList.add(new KeyValuePair(alias, e.getKey().getHostAddress()));
			}
		}
		Collections.sort(aliasList, (l, r) -> l.getKey().compareToIgnoreCase(r.getKey()));
		return success(aliasList);
	}

	/**
	 * Add a host alias.
	 *
	 * @param name
	 *        the alias to add
	 * @param address
	 *        the address
	 * @param locale
	 *        the desired locale
	 * @return the result
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST)
	@ResponseBody
	public Result<KeyValuePair> add(@RequestParam("name") String name,
			@RequestParam("address") String address, Locale locale) {
		if ( name == null || name.trim().isEmpty() ) {
			throw new IllegalArgumentException("The name argument is required.");
		}
		if ( address == null || address.trim().isEmpty() ) {
			throw new IllegalArgumentException("The address argument is required.");
		}

		InetAddress addr;
		try {
			addr = InetAddress.getByName(address);
		} catch ( UnknownHostException e ) {
			throw new IllegalArgumentException("Invalid IP address.");
		}

		SystemService service = systemService(locale);
		service.addHostAlias(name, addr);

		return success(new KeyValuePair(name, addr.getHostAddress()));
	}

	/**
	 * Remove a host alias.
	 *
	 * @param name
	 *        the name to remove
	 * @param locale
	 *        the desired locale
	 * @return the result
	 */
	@RequestMapping(value = "/remove", method = RequestMethod.POST)
	@ResponseBody
	public Result<Void> remove(@RequestParam("name") String name, Locale locale) {
		if ( name == null || name.trim().isEmpty() ) {
			throw new IllegalArgumentException("The name argument is required.");
		}
		SystemService service = systemService(locale);
		service.removeHostAlias(name);
		return success();
	}
}
