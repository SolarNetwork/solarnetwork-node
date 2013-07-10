/* ==================================================================
 * InstructorController.java - Jul 10, 2013 4:00:40 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import net.solarnetwork.node.NodeControlProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller to act as a local Instructor to the local node.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/controls")
public class InstructorController {

	private static final String KEY_PROVIDER_IDS = "providerIds";

	@Resource(name = "nodeControlProviderList")
	private final Collection<NodeControlProvider> providers = Collections.emptyList();

	@RequestMapping(value = "", method = RequestMethod.GET)
	public String settingsList(ModelMap model) {
		List<String> providerIds = new ArrayList<String>();
		for ( NodeControlProvider provider : providers ) {
			providerIds.addAll(provider.getAvailableControlIds());
		}
		model.put(KEY_PROVIDER_IDS, providerIds);
		return "control-provider-list";
	}

}
