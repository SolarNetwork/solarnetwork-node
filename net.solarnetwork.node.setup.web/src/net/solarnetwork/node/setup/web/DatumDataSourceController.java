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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderInfo;
import net.solarnetwork.settings.support.BasicSettingSpecifierProviderInfo;
import net.solarnetwork.util.ObjectUtils;

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

	private final OptionalServiceCollection<DatumDataSource> datumDataSources;

	/**
	 * Constructor.
	 *
	 * @param datumDataSources
	 *        the datum data sources
	 */
	public DatumDataSourceController(
			@Qualifier("datumDataSources") OptionalServiceCollection<DatumDataSource> datumDataSources) {
		super();
		this.datumDataSources = ObjectUtils.requireNonNullArgument(datumDataSources, "datumDataSources");
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

		private final SettingSpecifierProviderInfo info;
		private final List<String> sourceIds;

		private DatumDataSourceInfo(SettingSpecifierProviderInfo info, List<String> sourceIds) {
			this.info = info;
			this.sourceIds = sourceIds;
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
		List<DatumDataSourceInfo> results = new ArrayList<>();
		for ( DatumDataSource ds : datumDataSources.services() ) {
			SettingSpecifierProviderInfo info;
			if ( ds instanceof SettingSpecifierProvider ) {
				info = ((SettingSpecifierProvider) ds).localizedInfo(locale, ds.getUid(),
						ds.getGroupUid());
			} else {
				info = new BasicSettingSpecifierProviderInfo(null, ds.getDisplayName(), ds.getUid(),
						ds.getGroupUid());
			}
			List<String> sourceIds = new ArrayList<>(ds.publishedSourceIds());
			sourceIds.sort(String::compareToIgnoreCase);
			results.add(new DatumDataSourceInfo(info, sourceIds));
		}
		results.sort(comparing(d -> d.getInfo().getDisplayName(), String::compareToIgnoreCase));
		return success(results);
	}

}
