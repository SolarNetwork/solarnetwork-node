/* ==================================================================
 * PlatformController.java - 21/11/2017 11:38:23 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import net.solarnetwork.node.service.PlatformService;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Web controller for platform service support.
 * 
 * @author matt
 * @version 2.0
 */
@RestController
public class PlatformController extends BaseSetupWebServiceController
		implements ApplicationListener<SessionSubscribeEvent> {

	private final PlatformService platformService;

	/**
	 * Constructor.
	 * 
	 * @param platformService
	 *        the platform service
	 */
	@Autowired
	public PlatformController(PlatformService platformService) {
		super();
		this.platformService = platformService;
	}

	/**
	 * Get the platform state.
	 * 
	 * @return the result
	 */
	@RequestMapping(value = "/pub/platform/state", method = RequestMethod.GET)
	public Response<PlatformService.PlatformState> activePlatformState() {
		PlatformService.PlatformState state = platformService.activePlatformState();
		return Response.response(state);
	}

	/**
	 * Get the platform task.
	 * 
	 * @param locale
	 *        the locale
	 * @return the result
	 */
	@RequestMapping(value = "/pub/platform/task", method = RequestMethod.GET)
	public Response<PlatformService.PlatformTaskInfo> activePlatformTaskInfo(Locale locale) {
		PlatformService.PlatformTaskInfo info = platformService.activePlatformTaskInfo(locale);
		return Response.response(info);
	}

	@Override
	public void onApplicationEvent(SessionSubscribeEvent event) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(event.getMessage(),
				StompHeaderAccessor.class);
		String topic = accessor.getDestination();
		if ( topic.endsWith("/platform/task") ) {
			String langHeaderValue = accessor.getFirstNativeHeader(HttpHeaders.ACCEPT_LANGUAGE);
			Locale locale = parseFirstLocaleFromAcceptHeader(langHeaderValue);
			platformService.subscribeToActivePlatformTaskInfo(locale);
		}
	}

	private Locale parseFirstLocaleFromAcceptHeader(String headerValue) {
		Locale result = null;
		if ( headerValue != null ) {
			String[] langTags = headerValue.split(",", 2);
			if ( langTags.length > 0 ) {
				String[] langComponents = langTags[0].trim().replace('-', '_').split(";", 2)[0]
						.split("_");
				switch (langComponents.length) {
					case 2:
						result = new Locale(langComponents[0], langComponents[1]);
						break;
					case 3:
						result = new Locale(langComponents[0], langComponents[1], langComponents[2]);
						break;
					default:
						result = new Locale(langComponents[0]);
						break;
				}
			}
		}
		if ( result == null ) {
			result = Locale.getDefault();
		}
		return result;
	}

}
