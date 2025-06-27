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
 */

package net.solarnetwork.node.setup.web;

import static net.solarnetwork.service.OptionalService.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.SystemService;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.service.OptionalService;

/**
 * Base class for setup controllers.
 *
 * @author matt
 * @version 2.2
 */
public class BaseSetupController {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private SetupService setupBiz;

	@Autowired
	private IdentityService identityService;

	@Autowired
	@Qualifier("systemService")
	private OptionalService<SystemService> systemService;

	/**
	 * Default constructor.
	 */
	public BaseSetupController() {
		super();
	}

	/**
	 * Shutdown SolarNode in the near future.
	 *
	 * This can be used during the setup process, when restoring backups for
	 * example. By shutting down we assume some external watchdog process will
	 * bring SolarNode back up, such as {@code systemd} or Monit.
	 *
	 * @since 1.1
	 */
	protected void shutdownSoon() {
		final SystemService ss = service(systemService);
		if ( ss != null ) {
			ss.exit(true);
		}
	}

	/**
	 * Get the {@link SetupService} to use for querying/storing application
	 * state information.
	 *
	 * @return the service
	 */
	public SetupService getSetupBiz() {
		return setupBiz;
	}

	/**
	 * Set the {@link SetupService} to use for querying/storing application
	 * state information.
	 *
	 * @param setupBiz
	 *        the service to set
	 */
	public void setSetupBiz(SetupService setupBiz) {
		this.setupBiz = setupBiz;
	}

	/**
	 * Get the {@link IdentityService} to use for querying identity information.
	 *
	 * @return the service
	 */
	public IdentityService getIdentityService() {
		return identityService;
	}

	/**
	 * Set the {@link IdentityService} to use for querying identity information.
	 *
	 * @param identityService
	 *        the service to set
	 */
	public void setIdentityService(IdentityService identityService) {
		this.identityService = identityService;
	}

	/**
	 * Get the system service.
	 *
	 * @return the system service
	 * @since 2.1
	 */
	public final OptionalService<SystemService> getSystemService() {
		return systemService;
	}

	/**
	 * Set the system service.
	 *
	 * @param systemService
	 *        the system service to set
	 * @since 2.1
	 */
	public final void setSystemService(OptionalService<SystemService> systemService) {
		this.systemService = systemService;
	}

}
