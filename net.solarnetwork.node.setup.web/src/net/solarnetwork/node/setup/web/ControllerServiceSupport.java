/* ==================================================================
 * ControllerServiceSupport.java - 13/02/2017 10:25:07 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.SystemService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;

/**
 * Add global services to all MVC controllers.
 * 
 * @author matt
 * @version 2.0
 * @since 1.23
 */
@ControllerAdvice(annotations = { ServiceAwareController.class })
public class ControllerServiceSupport {

	/** The model attribute name for the {@code SystemService}. */
	public static final String SYSTEM_SERVICE_ATTRIBUTE = "systemService";

	/** The model attribute name for the {@code IdentityService}. */
	public static final String IDENTITY_SERVICE_ATTRIBUTE = "identityService";

	@Resource(name = "systemService")
	private OptionalService<SystemService> systemService;

	@Autowired
	private IdentityService identityService;

	@ModelAttribute(value = SYSTEM_SERVICE_ATTRIBUTE)
	public SystemService systemService() {
		final SystemService sysService = (systemService != null ? systemService.service() : null);
		return sysService;
	}

	/**
	 * The {@link IdentityService}.
	 * 
	 * @return the identity service
	 * @since 1.1
	 */
	@ModelAttribute(value = IDENTITY_SERVICE_ATTRIBUTE)
	public IdentityService identityService() {
		return identityService;
	}

}
