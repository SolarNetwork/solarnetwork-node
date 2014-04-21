/* ==================================================================
 * PluginController.java - Apr 21, 2014 10:32:12 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Resource;
import net.solarnetwork.node.setup.Plugin;
import net.solarnetwork.node.setup.PluginService;
import net.solarnetwork.node.setup.SimplePluginQuery;
import net.solarnetwork.util.OptionalService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller to manage the installed bundles via an OBR.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/plugins")
public class PluginController {

	@Resource(name = "pluginService")
	private OptionalService<PluginService> pluginService;

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	public List<Plugin> list(@RequestParam(value = "filter", required = false) final String filter,
			@RequestParam(value = "latestOnly", required = false) final Boolean latestOnly,
			final Locale locale) {
		PluginService service = pluginService.service();
		if ( service == null ) {
			return Collections.emptyList();
		}
		SimplePluginQuery query = new SimplePluginQuery();
		query.setSimpleQuery(filter);
		query.setLatestVersionOnly(latestOnly == null ? true : latestOnly.booleanValue());
		return service.availablePlugins(query, locale);
	}

}
