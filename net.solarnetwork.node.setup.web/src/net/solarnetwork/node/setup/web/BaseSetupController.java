/* ==================================================================
 * BaseSetupController.java - Jun 1, 2010 3:08:42 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.setup.web;

import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.setup.SetupService;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for setup controllers.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>setupBiz</dt>
 *   <dd>The {@link SetupService} to use for querying/storing application
 *   state information.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Id$
 */
public class BaseSetupController {

	@Autowired private SetupService setupBiz;
	@Autowired private IdentityService identityService;

	
	public SetupService getSetupBiz() {
		return setupBiz;
	}

	public void setSetupBiz(SetupService setupBiz) {
		this.setupBiz = setupBiz;
	}

	public IdentityService getIdentityService() {
		return identityService;
	}

	public void setIdentityService(IdentityService identityService) {
		this.identityService = identityService;
	}

}
