/* ==================================================================
 * DatumDataSourceController.java - 6/07/2024 1:43:32 pm
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

import static java.util.Comparator.comparing;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumSourceIdProvider;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceRegistry;
import net.solarnetwork.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderInfo;
import net.solarnetwork.settings.support.BasicSettingSpecifierProviderInfo;
import net.solarnetwork.util.SearchFilter;
import net.solarnetwork.util.SearchFilter.CompareOperator;
import net.solarnetwork.util.SearchFilter.LogicOperator;

/**
 * Web controller for datum data source support.
 *
 * @author matt
 * @version 1.0
 * @since 4.5
 */
@Controller
@RequestMapping("/a/datum-sources")
public class DatumDataSourceController {

	/**
	 * The service filter to use to look up providers of source IDs at runtime.
	 */
	public static final String SERVICE_FILTER;
	static {
		Map<String, Object> p = new LinkedHashMap<>(2);
		p.put("c1",
				new SearchFilter("objectClass", DatumDataSource.class.getName(), CompareOperator.EQUAL));
		p.put("c2", new SearchFilter("objectClass", MultiDatumDataSource.class.getName(),
				CompareOperator.EQUAL));
		p.put("c3", new SearchFilter("objectClass", DatumSourceIdProvider.class.getName(),
				CompareOperator.EQUAL));
		SERVICE_FILTER = new SearchFilter(p, LogicOperator.OR).asLDAPSearchFilterString();
	}

	private final ServiceRegistry serviceRegistry;
	private final OptionalService<SettingsService> settingsService;

	/**
	 * Constructor.
	 *
	 * @param serviceRegistry
	 *        the service registry
	 * @param settingsService
	 *        the settings service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumDataSourceController(ServiceRegistry serviceRegistry,
			@Qualifier("settingsService") OptionalService<SettingsService> settingsService) {
		super();
		this.serviceRegistry = requireNonNullArgument(serviceRegistry, "serviceRegistry");
		this.settingsService = requireNonNullArgument(settingsService, "settingsService");
	}

	/**
	 * Datum sources UI.
	 *
	 * @return the Datum Data Sources view name
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public String datumDataSourcesUi() {
		return "datum-sources";
	}

	/**
	 * Information about a datum data source.
	 */
	public static class DatumDataSourceInfo {

		private final String type;
		private final String identifier;
		private final SettingSpecifierProviderInfo info;
		private final List<String> sourceIds;

		private DatumDataSourceInfo(String type, String identifier, SettingSpecifierProviderInfo info,
				List<String> sourceIds) {
			this.type = type;
			this.identifier = identifier;
			this.info = info;
			this.sourceIds = sourceIds;
		}

		/**
		 * Get the type.
		 *
		 * @return the type
		 */
		public final String getType() {
			return type;
		}

		/**
		 * Get the identifier.
		 *
		 * @return the identifier
		 */
		public final String getIdentifier() {
			return identifier;
		}

		/**
		 * Get the datum data source info.
		 *
		 * @return the info
		 */
		public final SettingSpecifierProviderInfo getInfo() {
			return info;
		}

		/**
		 * Get the source IDs reported by the datum data source.
		 *
		 * @return the published source IDs
		 */
		public final List<String> getSourceIds() {
			return sourceIds;
		}

	}

	/**
	 * List the registered datum data sources.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the resulting list of "known" datum data sources
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<List<DatumDataSourceInfo>> listDatumDataSources(Locale locale) {
		final List<DatumDataSourceInfo> results = new ArrayList<>();

		final SettingsService service = service(settingsService);
		final Map<String, Map<String, FactorySettingSpecifierProvider>> settingFactories = new HashMap<>(
				8);

		for ( Object s : serviceRegistry.services(SERVICE_FILTER) ) {
			// all services implement DatumSourceIdProvider at a minimum, so safe to cast here
			final DatumSourceIdProvider dsp = (DatumSourceIdProvider) s;

			final SettingSpecifierProviderInfo info;
			String instanceId = null;

			if ( s instanceof SettingSpecifierProvider ) {
				info = ((SettingSpecifierProvider) s).localizedInfo(locale, dsp.getUid(),
						dsp.getGroupUid());

				if ( service != null && info.getSettingUid() != null ) {

					Map<String, FactorySettingSpecifierProvider> instanceMapping = settingFactories
							.get(info.getSettingUid());
					if ( instanceMapping == null ) {
						instanceMapping = service.getProvidersForFactory(info.getSettingUid());
						if ( instanceMapping == null ) {
							instanceMapping = Collections.emptyMap();
						}
						settingFactories.put(info.getSettingUid(), instanceMapping);
					}
					for ( Entry<String, FactorySettingSpecifierProvider> e : instanceMapping
							.entrySet() ) {
						DatumSourceIdProvider instanceDsp = e.getValue()
								.unwrap(DatumSourceIdProvider.class);
						if ( instanceDsp == dsp ) {
							instanceId = e.getKey();
							break;
						}
					}
				}

			} else {
				info = new BasicSettingSpecifierProviderInfo(null,
						dsp.getDisplayName() != null ? dsp.getDisplayName() : dsp.getClass().getName(),
						dsp.getUid(), dsp.getGroupUid());
			}
			List<String> sourceIds = new ArrayList<>(dsp.publishedSourceIds());
			sourceIds.sort(String::compareToIgnoreCase);

			String type = (s instanceof DatumDataSource ? "DatumDataSource"
					: s instanceof DatumFilterService ? "DatumFilterService" : s.getClass().getName());

			results.add(new DatumDataSourceInfo(type, instanceId, info, sourceIds));
		}
		results.sort(comparing(d -> d.getInfo().getDisplayName(), String::compareToIgnoreCase));
		return success(results);
	}

}
