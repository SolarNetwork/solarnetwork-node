/* ==================================================================
 * ModbusCsvConfigurer.java - 9/03/2022 11:48:43 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.modbus;

import static java.util.Arrays.asList;
import static net.solarnetwork.io.StreamUtils.inputStreamForPossibleGzipStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.node.settings.support.BasicFileSettingSpecifier;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Service that can configure {@link ModbusDatumDataSource} instances via CSV
 * resources.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusCsvConfigurer extends BasicIdentifiable
		implements SettingSpecifierProvider, SettingResourceHandler {

	/** The setting resource key for a CSV file. */
	public static final String RESOURCE_KEY_CSV_FILE = "csvFile";

	private static final Logger log = LoggerFactory.getLogger(ModbusCsvConfigurer.class);

	private final SettingsService settingsService;

	private String settingProviderId = ModbusDatumDataSource.SETTING_UID;

	private Throwable lastImportException = null;
	private List<String> lastImportMessages = null;

	/**
	 * Constructor.
	 * 
	 * @param settingsService
	 *        the settings service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusCsvConfigurer(SettingsService settingsService) {
		super();
		this.settingsService = ObjectUtils.requireNonNullArgument(settingsService, "settingsService");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.modbus.csv";
	}

	@Override
	public String getDisplayName() {
		return "Modbus Datum Data Source CSV Configurer";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(3);
		results.add(new BasicTitleSettingSpecifier("lastImportException",
				lastImportException != null ? lastImportException.toString() : "N/A", true));
		results.add(new BasicTitleSettingSpecifier("lastImportMessages",
				lastImportMessages != null && !lastImportMessages.isEmpty()
						? StringUtils.delimitedStringFromCollection(lastImportMessages, "\n")
						: "N/A",
				true));

		results.add(new BasicFileSettingSpecifier(RESOURCE_KEY_CSV_FILE, null,
				new LinkedHashSet<>(asList(".csv", ".csv.gz", "text/csv", "text/csv+gzip")), true));

		return results;
	}

	@Override
	public Iterable<Resource> currentSettingResources(String settingKey) {
		// TODO could translate current settings back into CSV
		return null;
	}

	@Override
	public synchronized SettingsUpdates applySettingResources(String settingKey,
			Iterable<Resource> resources) throws IOException {
		if ( resources == null ) {
			return null;
		}
		if ( !RESOURCE_KEY_CSV_FILE.equals(settingKey) ) {
			log.warn("Ignoring setting resource key [{}]", settingKey);
			return null;
		}
		List<ModbusDatumDataSourceConfig> configs = new ArrayList<>(8);
		try {
			for ( Resource r : resources ) {
				try {
					this.lastImportException = null;
					this.lastImportMessages = null;
					List<ModbusDatumDataSourceConfig> rConfigs = parseModbusConfigs(r);
					if ( rConfigs != null ) {
						configs.addAll(rConfigs);
					}
				} catch ( Exception e ) {
					log.error("Exception parsing CSV file {}", r, e);
					this.lastImportException = e;
				}
			}
			if ( log.isInfoEnabled() ) {
				log.info("Parsed {} Modbus Device configurations: [\n\t{}\n]", configs.size(),
						configs.stream().map(c -> c.toString()).collect(Collectors.joining("\n\t")));
			}
			return toSettingsUpdates(configs);
		} catch ( Exception e ) {
			lastImportException = e;
		}
		return null;
	}

	private List<ModbusDatumDataSourceConfig> parseModbusConfigs(Resource resource) throws IOException {
		List<ModbusDatumDataSourceConfig> configs = new ArrayList<>(8);
		lastImportMessages = new ArrayList<>(8);
		ModbusDatumDataSourceConfigCsvParser parser = new ModbusDatumDataSourceConfigCsvParser(configs,
				getMessageSource(), lastImportMessages);
		try (Reader in = new InputStreamReader(
				inputStreamForPossibleGzipStream(resource.getInputStream()), ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}
		return configs;
	}

	private SettingsUpdates toSettingsUpdates(List<ModbusDatumDataSourceConfig> configs) {
		if ( configs == null || configs.isEmpty() ) {
			return null;
		}

		// remove all factory instances so start anew
		for ( String instanceId : settingsService.getProvidersForFactory(settingProviderId).keySet() ) {
			settingsService.deleteProviderFactoryInstance(settingProviderId, instanceId);
		}

		List<SettingValueBean> settings = new ArrayList<>(32);
		for ( ModbusDatumDataSourceConfig config : configs ) {
			settings.addAll(config.toSettingValues(settingProviderId));
			if ( config.getKey() != null ) {
				settingsService.enableProviderFactoryInstance(settingProviderId, config.getKey());
			}
		}

		SettingsCommand cmd = new SettingsCommand(settings, asList(Pattern.compile(".*")));
		return cmd;
	}

}
