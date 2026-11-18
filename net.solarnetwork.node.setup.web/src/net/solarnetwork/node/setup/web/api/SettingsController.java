/* ==================================================================
 * SettingsSharedController.java - 17 Nov 2026 6:33:09 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.api;

import static java.util.Collections.sort;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.settings.ExtendedSettingSpecifierProviderFactory;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.support.SettingSpecifierProviderFactoryMessageComparator;
import net.solarnetwork.node.settings.support.SettingSpecifierProviderMessageComparator;
import net.solarnetwork.node.settings.support.SettingsUtils;
import net.solarnetwork.node.setup.web.support.GlobalExceptionRestController;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.util.SearchFilter;
import net.solarnetwork.util.StringNaturalSortComparator;

/**
 * Settings REST-only API.
 *
 * @author matt
 * @version 1.0
 * @since 5.6
 */
@GlobalExceptionRestController
@RestController("v1SettingsController")
@RequestMapping("/api/v1/sec/settings")
public class SettingsController {

	private static final SearchFilter NOT_DATUM_FILTER = nonnull(
			SearchFilter.forLDAPSearchFilterString("(!(role=datum-filter))"), "NOT_DATUM_FILTER");
	private static final SearchFilter EVERYTHING_FILTER = nonnull(
			SearchFilter.forLDAPSearchFilterString("(objectClass?*)"), "EVERYTHING_FILTER");

	private final OptionalService<SettingsService> settingsService;

	/**
	 * Constructor.
	 *
	 * @param settingsService
	 *        the settings service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SettingsController(
			@Qualifier("settingsService") OptionalService<SettingsService> settingsService) {
		super();
		this.settingsService = requireNonNullArgument(settingsService, "settingsService");
	}

	/**
	 * List factory settings that are not datum filters.
	 *
	 * @param filter
	 *        an LDAP-style service filter; if not provided
	 *        {@link #NOT_DATUM_FILTER} will be used
	 * @param locale
	 *        the locale
	 * @return the settings list view name
	 */
	@GetMapping("")
	public Result<?> factoryInfos(
			@RequestParam(name = "filter", required = false) @Nullable String filter,
			@Nullable Locale locale) {
		final SettingsService service = service(settingsService);
		if ( service == null ) {
			return success();
		}
		if ( locale == null ) {
			locale = Locale.US;
		}
		final SearchFilter searchFilter = SearchFilter.forLDAPSearchFilterString(filter);
		final List<SettingSpecifierProviderFactory> factories = service
				.getProviderFactories(searchFilter != null ? searchFilter : NOT_DATUM_FILTER);
		if ( factories != null ) {
			sort(factories, new SettingSpecifierProviderFactoryMessageComparator(locale));
		}

		return success(SettingsUtils.factoryInfos(factories, locale));
	}

	/**
	 * List provider infos that are not datum filters.
	 *
	 * @param filter
	 *        an LDAP-style service filter; if not provided
	 *        {@link #NOT_DATUM_FILTER} will be used
	 * @param locale
	 *        the locale
	 * @return the settings list view name
	 */
	@GetMapping("/services")
	public Result<?> providerInfos(
			@RequestParam(name = "filter", required = false) @Nullable String filter,
			@Nullable Locale locale) {
		final SettingsService service = service(settingsService);
		if ( service == null ) {
			return success();
		}
		if ( locale == null ) {
			locale = Locale.US;
		}
		final SearchFilter searchFilter = SearchFilter.forLDAPSearchFilterString(filter);
		final List<SettingSpecifierProvider> providers = service
				.getProviders(searchFilter != null ? searchFilter : NOT_DATUM_FILTER);
		if ( providers != null ) {
			sort(providers, new SettingSpecifierProviderMessageComparator(locale));
		}

		return success(SettingsUtils.providerInfos(providers, locale));
	}

	/**
	 * Get a list of factory instance IDs.
	 *
	 * @param factoryUid
	 *        the UID of the factory to get the instance IDs for, e.g. one
	 *        returned from {@link #notDatumFilterFactoryInfos(Locale)}
	 * @return the instance IDs available for the given factory
	 * @throws DataRetrievalFailureException
	 *         if a factory for the given UID is not available
	 * @throws UnsupportedOperationException
	 *         if the factory does not implement
	 *         {@link ExtendedSettingSpecifierProviderFactory}
	 */
	@GetMapping("/factories/instanceIds")
	public Result<Collection<String>> factoryInstanceIds(@RequestParam("uid") String factoryUid) {
		final SettingsService service = service(settingsService);
		if ( service == null ) {
			return success();
		}
		final SettingSpecifierProviderFactory f = service.getProviderFactory(factoryUid);
		if ( f == null ) {
			throw new DataRetrievalFailureException("Factory UID [" + factoryUid + "] not available.");
		}
		if ( !(f instanceof ExtendedSettingSpecifierProviderFactory ef) ) {
			throw new UnsupportedOperationException(
					"Factory [" + factoryUid + "] does not support instance ID discovery.");
		}
		final Set<String> instanceIds = ef.getSettingSpecifierProviderInstanceIds();
		return success(instanceIds);
	}

	/**
	 * Get the settings for a factory instance.
	 *
	 * @param factoryUid
	 *        the UID of the factory to get the instance settings for
	 * @param instanceId
	 *        the instance ID to get the settings for, e.g. one returned from
	 *        {@link #factoryInstanceIds(String)}
	 * @param specification
	 *        {@code true} for specification style settings, {@code false} for
	 *        active settings
	 * @param locale
	 *        the desired locale
	 * @return the instance settings
	 * @throws DataRetrievalFailureException
	 *         if a factory for the given UID is not available
	 */
	@GetMapping("/factories/instance")
	public Result<List<Map<String, Object>>> instanceSettings(@RequestParam("uid") String factoryUid,
			@RequestParam("instanceId") String instanceId,
			@RequestParam(name = "spec", required = false) boolean specification,
			@Nullable Locale locale) {
		final SettingsService service = service(settingsService);
		if ( service == null ) {
			return success();
		}
		final Map<String, FactorySettingSpecifierProvider> providers = service
				.getProvidersForFactory(factoryUid);
		final FactorySettingSpecifierProvider provider = providers.get(instanceId);
		if ( provider == null ) {
			throw new DataRetrievalFailureException(
					"Factory UID [" + factoryUid + "] instance [" + instanceId + "] not available.");
		}
		final List<Map<String, Object>> infos = SettingsUtils.factoryInstanceSettingInfos(service,
				provider, specification, locale);
		return success(infos);
	}

	/**
	 * Get a complete hierarchy of service and factory settings.
	 *
	 * <p>
	 * The returned {@code Map} will contain a {@code services} key for any
	 * setting providers and a {@code factories} key for any factory setting
	 * providers. The {@code factories} value will be another {@code Map}
	 * containing factory instance keys with associated instance settings.
	 * </p>
	 *
	 * @param specification
	 *        {@code true} for specification style settings, {@code false} for
	 *        active settings
	 * @param locale
	 *        the desired locale
	 * @return the settings
	 */
	@GetMapping("/hierarchy")
	public Result<Map<String, Object>> settingsHierarchy(
			@RequestParam(name = "spec", required = false) boolean specification,
			@Nullable Locale locale) {
		final SettingsService service = service(settingsService);
		if ( service == null ) {
			return success();
		}
		final Map<String, Object> result = new LinkedHashMap<>(2);

		// services
		final List<SettingSpecifierProvider> providers = service.getProviders(EVERYTHING_FILTER);
		if ( providers != null ) {
			sort(providers, new SettingSpecifierProviderMessageComparator(locale));
			final Map<String, Object> providerInfos = new LinkedHashMap<>(providers.size());
			for ( SettingSpecifierProvider p : providers ) {
				List<Map<String, Object>> settings = SettingsUtils.providerSettingInfos(service, p,
						specification, locale);
				providerInfos.put(p.getSettingUid(), settings);
			}
			result.put("services", providerInfos);
		}

		// factories
		final List<SettingSpecifierProviderFactory> factories = service
				.getProviderFactories(EVERYTHING_FILTER);
		if ( factories != null ) {
			sort(factories, new SettingSpecifierProviderFactoryMessageComparator(locale));
			final Map<String, Map<String, Object>> factoryInfos = new LinkedHashMap<>(factories.size());
			for ( SettingSpecifierProviderFactory f : factories ) {
				final Map<String, FactorySettingSpecifierProvider> fProviders = service
						.getProvidersForFactory(f.getFactoryUid());
				final List<String> instanceIds = fProviders.keySet().stream()
						.sorted(StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT).toList();
				for ( String instanceId : instanceIds ) {
					final FactorySettingSpecifierProvider provider = fProviders.get(instanceId);
					if ( provider != null ) {
						final List<Map<String, Object>> infos = SettingsUtils
								.factoryInstanceSettingInfos(service, provider, specification, locale);
						factoryInfos
								.computeIfAbsent(f.getFactoryUid(),
										k -> new LinkedHashMap<>(fProviders.size()))
								.put(specification ? "__SPECIFICATION__" : instanceId, infos);
						if ( specification ) {
							// need but 1 for specification
							break;
						}
					}
				}
			}
			result.put("factories", factoryInfos);
		}

		return success(result);
	}

}
