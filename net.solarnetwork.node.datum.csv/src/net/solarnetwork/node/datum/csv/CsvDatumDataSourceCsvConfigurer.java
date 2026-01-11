/* ==================================================================
 * CsvDatumDataSourceCsvConfigurer.java - 1/04/2023 3:58:10 pm
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

package net.solarnetwork.node.datum.csv;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static net.solarnetwork.io.StreamUtils.inputStreamForPossibleGzipStream;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.node.settings.support.BasicFileSettingSpecifier;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Service that can configure {@link CsvDatumDataSource} instances via CSV
 * resources.
 *
 * @author matt
 * @version 1.1
 */
public class CsvDatumDataSourceCsvConfigurer extends BasicIdentifiable
		implements SettingSpecifierProvider, SettingResourceHandler {

	/** The setting resource key for a CSV file. */
	public static final String RESOURCE_KEY_CSV_FILE = "csvFile";

	private static final Logger log = LoggerFactory.getLogger(CsvDatumDataSourceCsvConfigurer.class);

	private final boolean locationMode;
	private final SettingsService settingsService;
	private final OptionalService<IdentityService> identityService;
	private final String settingProviderId;

	private Throwable lastImportException = null;
	private List<String> lastImportMessages = null;

	/**
	 * Constructor.
	 *
	 * @param locationMode
	 *        {@literal true} to configure location-based datum data sources
	 * @param settingsService
	 *        the settings service
	 * @param identityService
	 *        the identity service
	 * @param settingProviderId
	 *        the setting provider ID to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CsvDatumDataSourceCsvConfigurer(boolean locationMode, SettingsService settingsService,
			OptionalService<IdentityService> identityService, String settingProviderId) {
		super();
		this.locationMode = locationMode;
		this.settingsService = requireNonNullArgument(settingsService, "settingsService");
		this.identityService = requireNonNullArgument(identityService, "identityService");
		this.settingProviderId = requireNonNullArgument(settingProviderId, "settingProviderId");
	}

	@Override
	public String getSettingUid() {
		return locationMode ? "net.solarnetwork.node.datum.csv.loc.csv"
				: "net.solarnetwork.node.datum.csv.csv";
	}

	@Override
	public String getDisplayName() {
		return locationMode ? "CSV Location Resource CSV Configurer" : "CSV Resource CSV Configurer";
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
	public Collection<String> supportedCurrentResourceSettingKeys() {
		return Collections.singletonList(RESOURCE_KEY_CSV_FILE);
	}

	@Override
	public Iterable<Resource> currentSettingResources(String settingKey) {
		if ( !RESOURCE_KEY_CSV_FILE.equals(settingKey) ) {
			log.warn("Ignoring setting resource key [{}]", settingKey);
			return null;
		}
		final ByteArrayOutputStream byos = new ByteArrayOutputStream(4096);
		try (CsvWriter writer = CsvWriter.builder().commentCharacter('!').build(byos)) {
			CsvDatumDataSourceConfigCsvWriter gen = new CsvDatumDataSourceConfigCsvWriter(writer,
					locationMode);

			// iterate over each instance
			for ( String instanceId : settingsService.getProvidersForFactory(settingProviderId)
					.keySet() ) {
				List<Setting> settings = settingsService.getSettings(settingProviderId, instanceId);
				gen.generateCsv(settingProviderId, instanceId, settings);
			}
		} catch ( UncheckedIOException | IOException e ) {
			log.error("Error generating {} configuration CSV: {}",
					locationMode ? "CSV Location Resource" : "CSV Resource", e.toString());
			return Collections.emptyList();
		}
		return (byos.size() > 0 ? Collections.singleton(new ByteArrayResource(byos.toByteArray(),
				locationMode ? "CSV Location Resource CSV" : "CSV Resource CSV") {

			@Override
			public String getFilename() {
				IdentityService service = OptionalService.service(identityService);
				Long nodeId = (service != null ? service.getNodeId() : null);
				return (nodeId != null
						? format(locationMode ? "csv-location-resource-config-solarnode-%d.csv"
								: "csv-resource-config-solarnode-%d.csv", nodeId)
						: locationMode ? "csv-location-resource-config.csv" : "csv-resource-config.csv");
			}

		}) : Collections.emptyList());
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
		List<CsvDatumDataSourceConfig> configs = new ArrayList<>(8);
		try {
			for ( Resource r : resources ) {
				try {
					this.lastImportException = null;
					this.lastImportMessages = null;
					List<CsvDatumDataSourceConfig> rConfigs = parseCsvResourceConfigs(r);
					if ( rConfigs != null ) {
						configs.addAll(rConfigs);
					}
				} catch ( Exception e ) {
					log.error("Exception parsing CSV file {}", r, e);
					this.lastImportException = e;
				}
			}
			if ( log.isInfoEnabled() ) {
				log.info("Parsed {} {} configurations: [\n\t{}\n]",
						locationMode ? "CSV Location Resource" : "CSV Resource", configs.size(),
						configs.stream().map(c -> c.toString()).collect(Collectors.joining("\n\t")));
			}
			return toSettingsUpdates(configs);
		} catch ( Exception e ) {
			lastImportException = e;
		}
		return null;
	}

	private List<CsvDatumDataSourceConfig> parseCsvResourceConfigs(Resource resource)
			throws IOException {
		List<CsvDatumDataSourceConfig> configs = new ArrayList<>(8);
		lastImportMessages = new ArrayList<>(8);
		CsvDatumDataSourceConfigCsvParser parser = new CsvDatumDataSourceConfigCsvParser(configs,
				getMessageSource(), lastImportMessages, locationMode);
		try (Reader in = new InputStreamReader(
				inputStreamForPossibleGzipStream(resource.getInputStream()), ByteUtils.UTF8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}
		return configs;
	}

	private SettingsUpdates toSettingsUpdates(List<CsvDatumDataSourceConfig> configs) {
		if ( configs == null || configs.isEmpty() ) {
			return null;
		}

		// remove all factory instances so start anew
		for ( String instanceId : settingsService.getProvidersForFactory(settingProviderId).keySet() ) {
			settingsService.deleteProviderFactoryInstance(settingProviderId, instanceId);
		}

		List<SettingValueBean> settings = new ArrayList<>(32);
		for ( CsvDatumDataSourceConfig config : configs ) {
			settings.addAll(config.toSettingValues(settingProviderId));
			if ( config.getKey() != null ) {
				settingsService.enableProviderFactoryInstance(settingProviderId, config.getKey());
			}
		}

		SettingsCommand cmd = new SettingsCommand(settings, asList(Pattern.compile(".*")));
		return cmd;
	}

}
