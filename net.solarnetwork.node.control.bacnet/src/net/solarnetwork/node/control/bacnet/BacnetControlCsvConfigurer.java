/* ==================================================================
 * BacnetControlCsvConfigurer.java - 11/11/2022 6:17:34 am
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

package net.solarnetwork.node.control.bacnet;

import static java.util.Arrays.asList;
import static net.solarnetwork.io.StreamUtils.inputStreamForPossibleGzipStream;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
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
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
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
import net.solarnetwork.util.StringNaturalSortComparator;
import net.solarnetwork.util.StringUtils;

/**
 * Service that can configure {@link BacnetControl} instances via CSV resources.
 *
 * @author matt
 * @version 1.1
 */
public class BacnetControlCsvConfigurer extends BasicIdentifiable
		implements SettingSpecifierProvider, SettingResourceHandler {

	/** The setting resource key for a CSV file. */
	public static final String RESOURCE_KEY_CSV_FILE = "csvFile";

	private static final Logger log = LoggerFactory.getLogger(BacnetControlCsvConfigurer.class);

	private final SettingsService settingsService;
	private final OptionalService<IdentityService> identityService;

	private String settingProviderId = BacnetControl.SETTING_UID;

	private Throwable lastImportException = null;
	private List<String> lastImportMessages = null;

	/**
	 * Constructor.
	 *
	 * @param settingsService
	 *        the settings service
	 * @param identityService
	 *        the identity service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BacnetControlCsvConfigurer(SettingsService settingsService,
			OptionalService<IdentityService> identityService) {
		super();
		this.settingsService = requireNonNullArgument(settingsService, "settingsService");
		this.identityService = requireNonNullArgument(identityService, "identityService");
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.bacnet.csv";
	}

	@Override
	public String getDisplayName() {
		return "BACnet Control CSV Configurer";
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
		try (Writer out = new OutputStreamWriter(byos, ByteUtils.UTF8);
				ICsvListWriter writer = new CsvListWriter(out, CsvPreference.STANDARD_PREFERENCE)) {
			BacnetControlConfigCsvWriter gen = new BacnetControlConfigCsvWriter(writer);

			// iterate over each instance in natural sort order
			List<String> instanceIds = new ArrayList<>(
					settingsService.getProvidersForFactory(settingProviderId).keySet());
			instanceIds.sort(StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT);
			for ( String instanceId : instanceIds ) {
				List<Setting> settings = settingsService.getSettings(settingProviderId, instanceId);
				gen.generateCsv(settingProviderId, instanceId, settings);
			}
		} catch ( IOException e ) {
			log.error("Error generating Bacnet Device configuration CSV: {}", e.toString());
			return Collections.emptyList();
		}
		return (byos.size() > 0
				? Collections.singleton(new ByteArrayResource(byos.toByteArray(), "Bacnet Device CSV") {

					@Override
					public String getFilename() {
						IdentityService service = OptionalService.service(identityService);
						Long nodeId = (service != null ? service.getNodeId() : null);
						return (nodeId != null
								? String.format("bacnet-control-config-solarnode-%d.csv", nodeId)
								: "bacnet-control-config.csv");
					}

				})
				: Collections.emptyList());
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
		List<BacnetControlConfig> configs = new ArrayList<>(8);
		try {
			for ( Resource r : resources ) {
				try {
					this.lastImportException = null;
					this.lastImportMessages = null;
					List<BacnetControlConfig> rConfigs = parseBacnetConfigs(r);
					if ( rConfigs != null ) {
						configs.addAll(rConfigs);
					}
				} catch ( Exception e ) {
					log.error("Exception parsing CSV file {}", r, e);
					this.lastImportException = e;
				}
			}
			if ( log.isInfoEnabled() ) {
				log.info("Parsed {} Bacnet Device configurations: [\n\t{}\n]", configs.size(),
						configs.stream().map(c -> c.toString()).collect(Collectors.joining("\n\t")));
			}
			return toSettingsUpdates(configs);
		} catch ( Exception e ) {
			lastImportException = e;
		}
		return null;
	}

	private List<BacnetControlConfig> parseBacnetConfigs(Resource resource) throws IOException {
		List<BacnetControlConfig> configs = new ArrayList<>(8);
		lastImportMessages = new ArrayList<>(8);
		BacnetControlConfigCsvParser parser = new BacnetControlConfigCsvParser(configs,
				getMessageSource(), lastImportMessages);
		try (Reader in = new InputStreamReader(
				inputStreamForPossibleGzipStream(resource.getInputStream()), ByteUtils.UTF8);
				ICsvListReader csv = new CsvListReader(in, CsvPreference.STANDARD_PREFERENCE)) {
			parser.parse(csv);
		}
		return configs;
	}

	private SettingsUpdates toSettingsUpdates(List<BacnetControlConfig> configs) {
		if ( configs == null || configs.isEmpty() ) {
			return null;
		}

		// remove all factory instances so start anew
		for ( String instanceId : settingsService.getProvidersForFactory(settingProviderId).keySet() ) {
			settingsService.deleteProviderFactoryInstance(settingProviderId, instanceId);
		}

		List<SettingValueBean> settings = new ArrayList<>(32);
		for ( BacnetControlConfig config : configs ) {
			settings.addAll(config.toSettingValues(settingProviderId));
			if ( config.getKey() != null ) {
				settingsService.enableProviderFactoryInstance(settingProviderId, config.getKey());
			}
		}

		SettingsCommand cmd = new SettingsCommand(settings, asList(Pattern.compile(".*")));
		return cmd;
	}

}
