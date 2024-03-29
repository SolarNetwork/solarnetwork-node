/* ==================================================================
 * DefaultController.java - Nov 28, 2012 8:33:46 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Default controller.
 * 
 * @author matt
 * @version 1.1
 */
@Controller
public class DefaultController {

	/**
	 * Default constructor.
	 */
	public DefaultController() {
		super();
	}

	/**
	 * Get the hello view.
	 * 
	 * @param principal
	 *        the actor
	 * @return the result view name
	 */
	@RequestMapping({ "/", "/hello" })
	public String hello(Principal principal) {
		return (principal == null ? "home" : "a/home");
	}

}
