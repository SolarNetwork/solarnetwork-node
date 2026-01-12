/* ==================================================================
 * BaseModbusServerCsvConfigurer.java - 13/01/2026 8:41:18 am
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

package net.solarnetwork.node.io.modbus.server.impl;

import static java.util.Arrays.asList;
import static net.solarnetwork.io.StreamUtils.inputStreamForPossibleGzipStream;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
import net.solarnetwork.node.io.modbus.server.domain.ModbusServerConfig;
import net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn;
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
import net.solarnetwork.util.StringNaturalSortComparator;
import net.solarnetwork.util.StringUtils;

/**
 * Base implementation of CSV configurer for Modbus servers using the
 * {@link ModbusServerCsvColumn} CSV structure with {@link ModbusServerConfig}
 * configuration.
 *
 * @author matt
 * @version 1.0
 */
public abstract class BaseModbusServerCsvConfigurer extends BasicIdentifiable
		implements SettingSpecifierProvider, SettingResourceHandler {

	/** The setting resource key for a CSV file. */
	public static final String RESOURCE_KEY_CSV_FILE = "csvFile";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final SettingsService settingsService;
	private final OptionalService<IdentityService> identityService;
	private final String serverSettingUid;
	private final String serverTypeKey;

	private Throwable lastImportException = null;
	private List<String> lastImportMessages = null;

	/**
	 * Constructor.
	 *
	 * @param settingsService
	 *        the settings service
	 * @param identityService
	 *        the identity service
	 * @param serverSettingUid
	 *        the Modbus Server setting UID
	 * @param serverTypeKey
	 *        a server implementation identifier to include in CSV export file
	 *        names, e.g. {@code tcp} or {@code rtu}
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseModbusServerCsvConfigurer(SettingsService settingsService,
			OptionalService<IdentityService> identityService, String serverSettingUid,
			String serverTypeKey) {
		super();
		this.settingsService = requireNonNullArgument(settingsService, "settingsService");
		this.identityService = requireNonNullArgument(identityService, "identityService");
		this.serverSettingUid = requireNonNullArgument(serverSettingUid, "serverSettingUid");
		this.serverTypeKey = requireNonNullArgument(serverTypeKey, "serverTypeKey");
	}

	@Override
	public final List<SettingSpecifier> getSettingSpecifiers() {
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
	public final Collection<String> supportedCurrentResourceSettingKeys() {
		return Collections.singletonList(RESOURCE_KEY_CSV_FILE);
	}

	@Override
	public final Iterable<Resource> currentSettingResources(String settingKey) {
		if ( !RESOURCE_KEY_CSV_FILE.equals(settingKey) ) {
			log.warn("Ignoring setting resource key [{}]", settingKey);
			return null;
		}
		final ByteArrayOutputStream byos = new ByteArrayOutputStream(4096);
		try (CsvWriter writer = CsvWriter.builder().commentCharacter('!').build(byos)) {
			ModbusServerConfigCsvWriter gen = new ModbusServerConfigCsvWriter(writer);

			// iterate over each instance in natural sort order
			List<String> instanceIds = new ArrayList<>(
					settingsService.getProvidersForFactory(serverSettingUid).keySet());
			instanceIds.sort(StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT);
			for ( String instanceId : instanceIds ) {
				List<Setting> settings = settingsService.getSettings(serverSettingUid, instanceId);
				gen.generateCsv(serverSettingUid, instanceId, settings);
			}
		} catch ( UncheckedIOException | IOException e ) {
			log.error("Error generating Modbus Server configuration CSV: {}", e.toString());
			return Collections.emptyList();
		}
		return (byos.size() > 0
				? Collections.singleton(new ByteArrayResource(byos.toByteArray(), "Modbus Server CSV") {

					@Override
					public String getFilename() {
						IdentityService service = OptionalService.service(identityService);
						Long nodeId = (service != null ? service.getNodeId() : null);
						return (nodeId != null
								? String.format("modbus-server-%s-config-solarnode-%d.csv",
										serverTypeKey, nodeId)
								: String.format("modbus-server-%s-config.csv", serverTypeKey));
					}

				})
				: Collections.emptyList());
	}

	@Override
	public final synchronized SettingsUpdates applySettingResources(String settingKey,
			Iterable<Resource> resources) throws IOException {
		if ( resources == null ) {
			return null;
		}
		if ( !RESOURCE_KEY_CSV_FILE.equals(settingKey) ) {
			log.warn("Ignoring setting resource key [{}]", settingKey);
			return null;
		}
		List<ModbusServerConfig> configs = new ArrayList<>(8);
		try {
			for ( Resource r : resources ) {
				try {
					this.lastImportException = null;
					this.lastImportMessages = null;
					List<ModbusServerConfig> rConfigs = parseModbusConfigs(r);
					if ( rConfigs != null ) {
						configs.addAll(rConfigs);
					}
				} catch ( Exception e ) {
					log.error("Exception parsing CSV file {}", r, e);
					this.lastImportException = e;
				}
			}
			if ( log.isInfoEnabled() ) {
				log.info("Parsed {} Modbus Server configurations: [\n\t{}\n]", configs.size(),
						configs.stream().map(c -> c.toString()).collect(Collectors.joining("\n\t")));
			}
			return toSettingsUpdates(configs);
		} catch ( Exception e ) {
			lastImportException = e;
		}
		return null;
	}

	private List<ModbusServerConfig> parseModbusConfigs(Resource resource) throws IOException {
		List<ModbusServerConfig> configs = new ArrayList<>(8);
		lastImportMessages = new ArrayList<>(8);
		ModbusServerConfigCsvParser parser = new ModbusServerConfigCsvParser(configs, getMessageSource(),
				lastImportMessages);
		try (Reader in = new InputStreamReader(
				inputStreamForPossibleGzipStream(resource.getInputStream()), StandardCharsets.UTF_8);
				CsvReader<CsvRecord> csv = CsvReader.builder().allowMissingFields(true)
						.allowExtraFields(true).skipEmptyLines(true)
						.commentStrategy(CommentStrategy.NONE)
						.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(),
								in)) {
			parser.parse(csv);
		}
		return configs;
	}

	private SettingsUpdates toSettingsUpdates(List<ModbusServerConfig> configs) {
		if ( configs == null || configs.isEmpty() ) {
			return null;
		}

		// remove all factory instances so start anew
		for ( String instanceId : settingsService.getProvidersForFactory(serverSettingUid).keySet() ) {
			settingsService.deleteProviderFactoryInstance(serverSettingUid, instanceId);
		}

		List<SettingValueBean> settings = new ArrayList<>(32);
		for ( ModbusServerConfig config : configs ) {
			settings.addAll(config.toSettingValues(serverSettingUid));
			if ( config.getKey() != null ) {
				settingsService.enableProviderFactoryInstance(serverSettingUid, config.getKey());
			}
		}

		SettingsCommand cmd = new SettingsCommand(settings, asList(Pattern.compile(".*")));
		return cmd;
	}

}
