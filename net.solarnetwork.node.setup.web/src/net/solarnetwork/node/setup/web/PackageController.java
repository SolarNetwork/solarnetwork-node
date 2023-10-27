/* ==================================================================
 * PackageController.java - 27/10/2023 6:00:18 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import static net.solarnetwork.domain.Result.success;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.service.PlatformPackageService;
import net.solarnetwork.node.service.PlatformPackageService.PlatformPackage;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;

/**
 * Controller to manage the installed packages.
 * 
 * @author matt
 * @version 1.0
 */
@ServiceAwareController
@RequestMapping("/a/packages")
public class PackageController {

	/**
	 * Plugin details.
	 */
	public static class PackageDetails {

		private final Iterable<PlatformPackage> installedPackages;
		private final Iterable<PlatformPackage> availablePackages;

		/**
		 * Constructor.
		 */
		public PackageDetails() {
			this(Collections.emptyList(), Collections.emptyList());
		}

		/**
		 * Constructor.
		 * 
		 * @param installedPackages
		 *        the installed packages
		 * @param availablePackages
		 *        the available packages
		 */
		public PackageDetails(Iterable<PlatformPackage> installedPackages,
				Iterable<PlatformPackage> availablePackages) {
			super();
			this.availablePackages = availablePackages;
			this.installedPackages = installedPackages;
		}

		/**
		 * Get the installed plugins.
		 * 
		 * @return the installed plugins
		 */
		public Iterable<PlatformPackage> getInstalledPackages() {
			return installedPackages;
		}

		/**
		 * Get the available plugins.
		 * 
		 * @return the available plugins
		 */
		public Iterable<PlatformPackage> getAvailablePackages() {
			return availablePackages;
		}

	}

	@Resource(name = "platformPackageService")
	private OptionalService<PlatformPackageService> platformPackageService;

	@Autowired(required = true)
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Default constructor.
	 */
	public PackageController() {
		super();
	}

	/**
	 * List packages.
	 * 
	 * @return the packages list view name
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String home() {
		return "packages/list";
	}

	/**
	 * List plugins.
	 * 
	 * @param filter
	 *        the filter
	 * @param latestOnly
	 *        {@literal true} to show only the latest version of all plugins
	 * @param locale
	 *        the locale
	 * @return the result
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ResponseBody
	public Callable<Result<PackageDetails>> list(
			@RequestParam(value = "filter", required = false) final String filter, final Locale locale) {
		return () -> {
			PlatformPackageService service = OptionalService.service(platformPackageService);
			if ( service != null ) {
				Future<Iterable<PlatformPackage>> avail = service.listNamedPackages(filter,
						Boolean.FALSE);
				Future<Iterable<PlatformPackage>> inst = service.listNamedPackages(filter, Boolean.TRUE);
				return success(new PackageDetails(inst.get(), avail.get()));
			}
			throw new UnsupportedOperationException("PlatformPackageService not available");
		};
	}

	//private Function<T, R>

	/**
	 * Set the message source.
	 * 
	 * @param messageSource
	 *        the message source to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Set the platform package service.
	 * 
	 * @param platformPackageService
	 *        the service to set
	 */
	public void setPlatformPackageService(
			OptionalService<PlatformPackageService> platformPackageService) {
		this.platformPackageService = platformPackageService;
	}

}
