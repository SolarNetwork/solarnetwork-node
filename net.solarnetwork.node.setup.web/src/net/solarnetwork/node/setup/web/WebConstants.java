/* ==================================================================
 * WebConstants.java - 13/06/2024 6:37:27 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import javax.servlet.http.HttpServletRequest;

/**
 * Web constants.
 *
 * @author matt
 * @version 1.0
 * @since 4.4
 */
public interface WebConstants {

	/** A UI model key for an error message bundle key. */
	String UI_MODEL_ERROR_MESSAGE_KEY = "errorMessageKey";

	/** A UI model key for an error message parameter without any index. */
	String UI_MODEL_ERROR_MESSAGE_PARAM = "errorMessageParam";

	/** A UI model key for an error message parameter at index 0. */
	String UI_MODEL_ERROR_MESSAGE_PARAM0 = UI_MODEL_ERROR_MESSAGE_PARAM + "0";

	/**
	 * Configure a UI error message on the current HTTP session.
	 *
	 * <p>
	 * This can be helpful when returning a HTTP redirect so the UI can display
	 * an error message on the redirected page.
	 * </p>
	 *
	 * @param request
	 *        the request to configure the session error on
	 * @param messageKey
	 *        the error message bundle key
	 * @param param
	 *        optional error message parameters
	 */
	static void setupSessionError(HttpServletRequest request, String messageKey, Object... param) {
		request.getSession(true).setAttribute(UI_MODEL_ERROR_MESSAGE_KEY, messageKey);
		if ( param != null && param.length > 0 ) {
			for ( int i = 0; i < param.length; i++ ) {
				String key = (i == 0 ? UI_MODEL_ERROR_MESSAGE_PARAM0 : UI_MODEL_ERROR_MESSAGE_PARAM + i);
				request.getSession(true).setAttribute(key, param[i]);
			}
		}
	}

}
