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

import static net.solarnetwork.domain.Result.error;
import static net.solarnetwork.domain.Result.success;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.service.PlatformPackageService;
import net.solarnetwork.node.service.PlatformPackageService.PlatformPackage;
import net.solarnetwork.node.service.PlatformPackageService.PlatformPackageResult;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;
import net.solarnetwork.service.OptionalService;

/**
 * Controller to manage the installed packages.
 *
 * @author matt
 * @version 1.1
 * @since 3.4
 */
@ServiceAwareController
@RequestMapping("/a/packages")
public class PackageController {

	/**
	 * Package details.
	 */
	public static class PackageDetails {

		private final Iterable<PlatformPackage> installedPackages;
		private final Iterable<PlatformPackage> availablePackages;
		private final Iterable<PlatformPackage> upgradablePackages;

		/**
		 * Constructor.
		 */
		public PackageDetails() {
			this(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		}

		/**
		 * Constructor.
		 *
		 * @param installedPackages
		 *        the installed packages
		 * @param availablePackages
		 *        the available packages
		 * @param upgradablePackages
		 *        the upgradable packages
		 */
		public PackageDetails(Iterable<PlatformPackage> installedPackages,
				Iterable<PlatformPackage> availablePackages,
				Iterable<PlatformPackage> upgradablePackages) {
			super();
			this.availablePackages = availablePackages;
			this.installedPackages = installedPackages;
			this.upgradablePackages = upgradablePackages;
		}

		/**
		 * Get the installed packages.
		 *
		 * @return the installed packages
		 */
		public Iterable<PlatformPackage> getInstalledPackages() {
			return installedPackages;
		}

		/**
		 * Get the available packages.
		 *
		 * @return the available packages
		 */
		public Iterable<PlatformPackage> getAvailablePackages() {
			return availablePackages;
		}

		/**
		 * Get the upgradable packages.
		 *
		 * @return the upgradable packages
		 */
		public Iterable<PlatformPackage> getUpgradablePackages() {
			return upgradablePackages;
		}

	}

	@Autowired
	@Qualifier("platformPackageService")
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
		return "packages";
	}

	private UnsupportedOperationException serviceNotAvailable(Locale locale) {
		return new UnsupportedOperationException(messageSource.getMessage("packages.serviceNotAvailable",
				null, "PlatformPackageService not available", locale));
	}

	/**
	 * Refresh available packages.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the result
	 */
	@RequestMapping(value = "/refresh", method = RequestMethod.GET)
	@ResponseBody
	public Result<PackageDetails> refreshAvailablePackages(Locale locale) {
		PlatformPackageService service = OptionalService.service(platformPackageService);
		if ( service != null ) {
			try {
				service.refreshNamedPackages().get();
			} catch ( Exception e ) {
				Throwable t = e.getCause();
				return error("WPC.0001", messageSource.getMessage("packages.refresh.exception",
						new Object[] { t.getMessage() }, "Error refreshing packages: {0}", locale));
			}
			return list(null, locale);
		}
		throw serviceNotAvailable(locale);
	}

	/**
	 * List packages.
	 *
	 * @param filter
	 *        an optional filter to filter packages by name
	 * @param locale
	 *        the desired locale
	 * @return the result
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ResponseBody
	public Result<PackageDetails> list(
			@RequestParam(value = "filter", required = false) final String filter, Locale locale) {
		PlatformPackageService service = OptionalService.service(platformPackageService);
		if ( service != null ) {
			Future<Iterable<PlatformPackage>> avail = service.listNamedPackages(filter, Boolean.FALSE);
			Future<Iterable<PlatformPackage>> inst = service.listNamedPackages(filter, Boolean.TRUE);
			Future<Iterable<PlatformPackage>> upgr = service.listUpgradableNamedPackages();
			try {
				return success(new PackageDetails(inst.get(), avail.get(), upgr.get()));
			} catch ( Exception e ) {
				Throwable t = e.getCause();
				return error("WPC.0002", messageSource.getMessage("packages.list.exception",
						new Object[] { t.getMessage() }, "Error listing packages: {0}", locale));
			}
		}
		throw serviceNotAvailable(locale);
	}

	/**
	 * Upgrade all packages.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the result
	 */
	@RequestMapping(value = "/upgrade", method = RequestMethod.POST)
	@ResponseBody
	public Result<PlatformPackageResult<Void>> upgrade(Locale locale) {
		PlatformPackageService service = OptionalService.service(platformPackageService);
		if ( service != null ) {
			Future<PlatformPackageResult<Void>> result = service
					.upgradeNamedPackages((avoid, amountComplete) -> {
						log.info("{}% complete upgrading packages", (int) (amountComplete * 100));
					}, null);
			try {
				return success(result.get());
			} catch ( Exception e ) {
				Throwable t = e.getCause();
				return error("WPC.0003", messageSource.getMessage("packages.upgrade.exception",
						new Object[] { t.getMessage() }, "Error upgrading packages: {0}", locale));
			}
		}
		throw serviceNotAvailable(locale);
	}

	/**
	 * Install a package.
	 *
	 * @param name
	 *        the name of the package to install
	 * @param locale
	 *        the desired locale
	 * @return the result
	 */
	@RequestMapping(value = "/install", method = RequestMethod.POST)
	@ResponseBody
	public Result<PlatformPackageResult<Void>> install(@RequestParam("name") String name,
			Locale locale) {
		if ( name == null || name.trim().isEmpty() ) {
			throw new IllegalArgumentException("The name argument is required.");
		}
		PlatformPackageService service = OptionalService.service(platformPackageService);
		if ( service != null ) {
			Future<PlatformPackageResult<Void>> result = service.installNamedPackage(name, null, null,
					(avoid, amountComplete) -> {
						log.info("{}% complete installing package {}", (int) (amountComplete * 100),
								name);
					}, null);
			try {
				return success(result.get());
			} catch ( Exception e ) {
				Throwable t = e.getCause();
				return error("WPC.0004",
						messageSource.getMessage("package.install.exception",
								new Object[] { name, t.getMessage() },
								"Error installing package {0}: {1}", locale));
			}
		}
		throw serviceNotAvailable(locale);
	}

	/**
	 * Remove a package.
	 *
	 * @param name
	 *        the name of the package to remove
	 * @param locale
	 *        the desired locale
	 * @return the result
	 */
	@RequestMapping(value = "/remove", method = RequestMethod.POST)
	@ResponseBody
	public Result<PlatformPackageResult<Void>> remove(@RequestParam("name") String name, Locale locale) {
		if ( name == null || name.trim().isEmpty() ) {
			throw new IllegalArgumentException("The name argument is required.");
		}
		PlatformPackageService service = OptionalService.service(platformPackageService);
		if ( service != null ) {
			Future<PlatformPackageResult<Void>> result = service.removeNamedPackage(name,
					(avoid, amountComplete) -> {
						log.info("{}% complete removing package {}", (int) (amountComplete * 100), name);
					}, null);
			try {
				return success(result.get());
			} catch ( Exception e ) {
				Throwable t = e.getCause();
				return error("WPC.0005",
						messageSource.getMessage("package.remove.exception",
								new Object[] { name, t.getMessage() }, "Error removing package {0}: {1}",
								locale));
			}
		}
		throw serviceNotAvailable(locale);
	}

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
