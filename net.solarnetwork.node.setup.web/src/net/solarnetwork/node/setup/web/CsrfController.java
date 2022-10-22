/* ==================================================================
 * CsrfController.java - 22/10/2016 9:10:10 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to serve up CSRF tokens, to support non-JSP based access.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
public class CsrfController {

	/**
	 * Get the CSRF token.
	 * 
	 * @param token
	 *        the current token
	 * @return the token
	 */
	@RequestMapping("/csrf")
	public CsrfToken csrf(CsrfToken token) {
		return token;
	}

}
