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

import static net.solarnetwork.service.OptionalService.service;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.PlatformPackageService;
import net.solarnetwork.node.service.SystemService;
import net.solarnetwork.node.setup.PluginService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;

/**
 * Add global services to all MVC controllers.
 *
 * @author matt
 * @version 2.3
 * @since 1.23
 */
@ControllerAdvice(annotations = { ServiceAwareController.class })
public class ControllerServiceSupport {

	/** The model attribute name for the {@code SystemService}. */
	public static final String SYSTEM_SERVICE_ATTRIBUTE = "systemService";

	/** The model attribute name for the {@code IdentityService}. */
	public static final String IDENTITY_SERVICE_ATTRIBUTE = "identityService";

	/**
	 * The model attribute name for the {@code PlatformPackageService}.
	 *
	 * @since 2.1
	 */
	public static final String PLATFORM_PACKAGE_SERVICE_ATTRIBUTE = "platformPackageService";

	/**
	 * The model attribute name for the {@code PluginService}.
	 *
	 * @since 2.2
	 */
	public static final String PLUGIN_SERVICE_ATTRIBUTE = "pluginService";

	/**
	 * The model attribute name for the {@code PluginService}.
	 *
	 * @since 2.3
	 */
	public static final String METRIC_DAO_ATTRIBUTE = "metricDao";

	@Resource(name = "systemService")
	private OptionalService<SystemService> systemService;

	@Resource(name = "platformPackageService")
	private OptionalService<PlatformPackageService> platformPackageService;

	@Resource(name = "pluginService")
	private OptionalService<PluginService> pluginService;

	@Resource(name = "metricDao")
	private OptionalService<MetricDao> metricDao;

	@Autowired
	private IdentityService identityService;

	/**
	 * Default constructor.
	 */
	public ControllerServiceSupport() {
		super();
	}

	/**
	 * Get the system service.
	 *
	 * @return the service
	 */
	@ModelAttribute(value = SYSTEM_SERVICE_ATTRIBUTE)
	public SystemService systemService() {
		return service(systemService);
	}

	/**
	 * The {@link IdentityService}.
	 *
	 * @return the service
	 * @since 1.1
	 */
	@ModelAttribute(value = IDENTITY_SERVICE_ATTRIBUTE)
	public IdentityService identityService() {
		return identityService;
	}

	/**
	 * The {@link PlatformPackageService}.
	 *
	 * @return the service
	 * @since 2.1
	 */
	@ModelAttribute(value = PLATFORM_PACKAGE_SERVICE_ATTRIBUTE)
	public PlatformPackageService platformPackageService() {
		return service(platformPackageService);
	}

	/**
	 * The {@link PlatformPackageService}.
	 *
	 * @return the service
	 * @since 2.2
	 */
	@ModelAttribute(value = PLUGIN_SERVICE_ATTRIBUTE)
	public PluginService pluginService() {
		return service(pluginService);
	}

	/**
	 * The {@link MetricDao}.
	 *
	 * @return the service
	 * @since 2.3
	 */
	@ModelAttribute(value = METRIC_DAO_ATTRIBUTE)
	public MetricDao metricDao() {
		return service(metricDao);
	}

}
