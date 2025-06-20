/* ==================================================================
 * StatusMessageHandlerInterceptor.java - 21/06/2025 9:15:57 am
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

package net.solarnetwork.node.setup.web.support;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Clean up session-based status messages after a redirect view has been
 * handled.
 *
 * @author matt
 * @version 1.0
 * @since 5.0
 */
public class StatusMessageHandlerInterceptor implements HandlerInterceptor {

	/** The status message key session attribute name. */
	public static final String MODEL_KEY_STATUS_MSG = "statusMessageKey";

	/** The status message key parameter session attribute name. */
	public static final String MODEL_KEY_STATUS_MSG_PARAM0 = "statusMessageParam0";

	/** The error message key session attribute name. */
	public static final String MODEL_KEY_ERROR_MSG = "errorMessageKey";

	/** The error message key parameter session attribute name. */
	public static final String MODEL_KEY_ERROR_MSG_PARAM0 = "errorMessageParam0";

	/**
	 * Constructor.
	 */
	public StatusMessageHandlerInterceptor() {
		super();
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) throws Exception {
		if ( ex != null || HttpStatus.valueOf(response.getStatus()).is3xxRedirection() ) {
			return;
		}
		HttpSession session = request.getSession(false);
		if ( session == null ) {
			return;
		}
		session.removeAttribute(MODEL_KEY_STATUS_MSG);
		session.removeAttribute(MODEL_KEY_STATUS_MSG_PARAM0);
		session.removeAttribute(MODEL_KEY_ERROR_MSG);
		session.removeAttribute(MODEL_KEY_ERROR_MSG_PARAM0);
	}

}
