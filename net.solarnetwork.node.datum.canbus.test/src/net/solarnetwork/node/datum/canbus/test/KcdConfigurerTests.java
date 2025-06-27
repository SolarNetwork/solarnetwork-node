/* ==================================================================
 * KcdConfigurerTests.java - 6/10/2019 5:48:30 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.canbus.test;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.node.datum.canbus.CanbusDatumDataSource.SETTING_UID;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.easymock.EasyMock;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import net.solarnetwork.node.datum.canbus.CanbusDatumDataSource;
import net.solarnetwork.node.datum.canbus.KcdConfigurer;
import net.solarnetwork.node.io.canbus.KcdParser;
import net.solarnetwork.node.io.canbus.support.JaxbSnKcdParser;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.node.settings.SettingsUpdates.Change;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link KcdConfigurer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class KcdConfigurerTests {

	private SettingsService settingsService;
	private KcdConfigurer configurer;

	@Before
	public void setup() {
		settingsService = EasyMock.createMock(SettingsService.class);

		configurer = new KcdConfigurer(new StaticOptionalService<KcdParser>(new JaxbSnKcdParser(true)),
				settingsService);
	}

	@After
	public void teardown() {
		EasyMock.verify(settingsService);
	}

	private void replayAll() {
		EasyMock.replay(settingsService);
	}

	@Test
	public void parseKcd_noExistingProviders() throws IOException {
		// GIVEN

		// look for existing instances to delete, but find none
		expect(settingsService.getProvidersForFactory(SETTING_UID)).andReturn(Collections.emptyMap());

		// create new datum data source instances for both <Node> elements
		final String instanceId = "1";
		expect(settingsService.addProviderFactoryInstance(SETTING_UID)).andReturn(instanceId);

		// WHEN
		replayAll();
		ClassPathResource r = new ClassPathResource("kcd-test-01.xml", getClass());
		SettingsUpdates updates = configurer.applySettingResources(KcdConfigurer.RESOURCE_KEY_KCD_FILE,
				singleton(r));

		// THEN
		assertThat("Updates generated", updates, notNullValue());
		assertThat("Patterns to clean available", updates.getSettingKeyPatternsToClean(),
				notNullValue());
		assertThat("Pattern to clean",
				stream(updates.getSettingKeyPatternsToClean().spliterator(), false).map(p -> p.pattern())
						.collect(toList()),
				Matchers.contains(".*"));
		List<Change> changes = stream(updates.getSettingValueUpdates().spliterator(), false)
				.collect(toList());
		assertThat("Setting change count", changes, hasSize(25));
		int i = 0;
		for ( Change change : changes ) {
			String msg = "Change " + i;
			switch (i) {
				case 0:
					assertChangeEquals(msg, change, instanceId, "canbusNetwork.propertyFilters['uid']",
							"Canbus Port");
					break;

				case 1:
					assertChangeEquals(msg, change, instanceId, "busName", "CANB");
					break;

				case 2:
					assertChangeEquals(msg, change, instanceId, "sourceId", "/BUS1/BAT1");
					break;

				case 3:
					assertChangeEquals(msg, change, instanceId, "msgConfigsCount", "1");
					break;

				case 4:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].address",
							String.valueOf(0x0C17A709));
					break;

				case 5:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].name",
							"Battery Energy Output");
					break;

				case 6:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].interval", "6000");
					break;

				case 7:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].byteOrderingCode", "l");
					break;

				case 8:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigsCount", "1");
					break;

				case 9:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyKey", "wattHourReading");
					break;

				case 10:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyTypeKey", "a");
					break;

				case 11:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].dataTypeKey", "u32");
					break;

				case 12:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].unit",
							"kW.h");
					break;

				case 13:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitOffset",
							"32");
					break;

				case 14:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitLength",
							"32");
					break;

				case 15:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].slope",
							"0.01");
					break;

				case 16:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].intercept",
							"0");
					break;

				case 17:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].decimalScale", "-1");
					break;

				case 18:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabelsCount", "0");
					break;

				case 19:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].localizedNamesCount", "2");
					break;

				case 20:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].localizedNames[0].key", "en");
					break;

				case 21:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].localizedNames[0].value",
							"Battery Output Energy");
					break;

				case 22:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].localizedNames[1].key", "fr");
					break;

				case 23:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].localizedNames[1].value",
							"Énergie de sortie de la batterie");
					break;

				case 24:
					assertChangeEquals(msg, change, instanceId, "expressionConfigsCount", "0");
					break;

				default:
					fail("Unexpected setting change: " + change);
			}
			i++;
		}
	}

	private void assertChangeEquals(String msg, Change change, String instanceId, String key,
			String value) {
		assertThat(msg + " provider", change.getProviderKey(),
				equalTo(CanbusDatumDataSource.SETTING_UID));
		assertThat(msg + " instance", change.getInstanceKey(), equalTo(instanceId));
		assertThat(msg + " key", change.getKey(), equalTo(key));
		assertThat(msg + " value", change.getValue(), equalTo(value));
	}

	@Test
	public void parseKcd_normalizedUnit() throws Exception {
		// GIVEN

		// look for existing instances to delete, but find none
		expect(settingsService.getProvidersForFactory(SETTING_UID)).andReturn(Collections.emptyMap());

		// create new datum data source instances for both <Node> elements
		final String instanceId = "1";
		expect(settingsService.addProviderFactoryInstance(SETTING_UID)).andReturn(instanceId);

		// WHEN
		replayAll();
		ClassPathResource r = new ClassPathResource("kcd-test-02.xml", getClass());
		SettingsUpdates updates = configurer.applySettingResources(KcdConfigurer.RESOURCE_KEY_KCD_FILE,
				singleton(r));

		// THEN
		assertThat("Updates generated", updates, notNullValue());
		assertThat("Patterns to clean available", updates.getSettingKeyPatternsToClean(),
				notNullValue());
		assertThat("Pattern to clean",
				stream(updates.getSettingKeyPatternsToClean().spliterator(), false).map(p -> p.pattern())
						.collect(toList()),
				Matchers.contains(".*"));
		List<Change> changes = stream(updates.getSettingValueUpdates().spliterator(), false)
				.collect(toList());
		assertThat("Setting change count", changes, hasSize(22));
		int i = 0;
		for ( Change change : changes ) {
			String msg = "Change " + i;
			switch (i) {
				case 0:
					assertChangeEquals(msg, change, instanceId, "canbusNetwork.propertyFilters['uid']",
							"Canbus Port");
					break;

				case 1:
					assertChangeEquals(msg, change, instanceId, "busName", "CANB");
					break;

				case 2:
					assertChangeEquals(msg, change, instanceId, "sourceId", "/BUS1/VEH1");
					break;

				case 3:
					assertChangeEquals(msg, change, instanceId, "msgConfigsCount", "1");
					break;

				case 4:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].address",
							String.valueOf(0x0C17A708));
					break;

				case 5:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].name", "Stats");
					break;

				case 6:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].interval", "6000");
					break;

				case 7:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].byteOrderingCode", "l");
					break;

				case 8:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigsCount", "1");
					break;

				case 9:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyKey", "distance");
					break;

				case 10:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyTypeKey", "a");
					break;

				case 11:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].dataTypeKey", "u32");
					break;

				case 12:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].unit",
							"km");
					break;

				case 13:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].normalizedUnit", "m");
					break;

				case 14:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitOffset",
							"32");
					break;

				case 15:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitLength",
							"32");
					break;

				case 16:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].slope",
							"0.01");
					break;

				case 17:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].intercept",
							"0");
					break;

				case 18:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].decimalScale", "-1");
					break;

				case 19:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabelsCount", "0");
					break;

				case 20:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].localizedNamesCount", "0");
					break;

				case 21:
					assertChangeEquals(msg, change, instanceId, "expressionConfigsCount", "0");
					break;

				default:
					fail("Unexpected setting change: " + change);
			}
			i++;
		}
	}

	@Test
	public void parseKcd_valueLabels() throws Exception {
		// GIVEN

		// look for existing instances to delete, but find none
		expect(settingsService.getProvidersForFactory(SETTING_UID)).andReturn(Collections.emptyMap());

		// create new datum data source instances for both <Node> elements
		final String instanceId = "1";
		expect(settingsService.addProviderFactoryInstance(SETTING_UID)).andReturn(instanceId);

		// WHEN
		replayAll();
		ClassPathResource r = new ClassPathResource("kcd-test-03.xml", getClass());
		SettingsUpdates updates = configurer.applySettingResources(KcdConfigurer.RESOURCE_KEY_KCD_FILE,
				singleton(r));

		// THEN
		assertThat("Updates generated", updates, notNullValue());
		assertThat("Patterns to clean available", updates.getSettingKeyPatternsToClean(),
				notNullValue());
		assertThat("Pattern to clean",
				stream(updates.getSettingKeyPatternsToClean().spliterator(), false).map(p -> p.pattern())
						.collect(toList()),
				Matchers.contains(".*"));
		List<Change> changes = stream(updates.getSettingValueUpdates().spliterator(), false)
				.collect(toList());
		assertThat("Setting change count", changes, hasSize(31));
		int i = 0;
		for ( Change change : changes ) {
			String msg = "Change " + i;
			switch (i) {
				case 0:
					assertChangeEquals(msg, change, instanceId, "canbusNetwork.propertyFilters['uid']",
							"Canbus Port");
					break;

				case 1:
					assertChangeEquals(msg, change, instanceId, "busName", "CANB");
					break;

				case 2:
					assertChangeEquals(msg, change, instanceId, "sourceId", "/BUS1/VEH1");
					break;

				case 3:
					assertChangeEquals(msg, change, instanceId, "msgConfigsCount", "1");
					break;

				case 4:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].address",
							String.valueOf(0x0C17A708));
					break;

				case 5:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].name", "Stats");
					break;

				case 6:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].interval", "6000");
					break;

				case 7:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].byteOrderingCode", "l");
					break;

				case 8:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigsCount", "1");
					break;

				case 9:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyKey", "status");
					break;

				case 10:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyTypeKey", "s");
					break;

				case 11:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].dataTypeKey", "u16");
					break;

				case 12:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].unit",
							"1");
					break;

				case 13:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitOffset",
							"0");
					break;

				case 14:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitLength",
							"16");
					break;

				case 15:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].slope",
							"1.0");
					break;

				case 16:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].intercept",
							"0");
					break;

				case 17:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].decimalScale", "-1");
					break;

				case 18:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabelsCount", "5");
					break;

				case 19:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[0].key", "0");
					break;

				case 20:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[0].value", "Normal");
					break;

				case 21:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[1].key", "1");
					break;

				case 22:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[1].value", "Broken");
					break;

				case 23:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[2].key", "2");
					break;

				case 24:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[2].value", "Get Out");
					break;

				case 25:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[3].key", "3");
					break;

				case 26:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[3].value", "Get Out");
					break;

				case 27:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[4].key", "4");
					break;

				case 28:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabels[4].value", "Get Out");
					break;

				case 29:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].localizedNamesCount", "0");
					break;

				case 30:
					assertChangeEquals(msg, change, instanceId, "expressionConfigsCount", "0");
					break;

				default:
					fail("Unexpected setting change: " + change);
			}
			i++;
		}
	}

	@Test
	public void parseKcd_multiNodeRefs() throws Exception {
		// GIVEN

		// look for existing instances to delete, but find none
		expect(settingsService.getProvidersForFactory(SETTING_UID)).andReturn(Collections.emptyMap());

		// create new datum data source instances for both <Node> elements
		final String instanceId1 = "1";
		final String instanceId2 = "2";
		expect(settingsService.addProviderFactoryInstance(SETTING_UID)).andReturn(instanceId1)
				.andReturn(instanceId2);

		// WHEN
		replayAll();
		ClassPathResource r = new ClassPathResource("kcd-test-04.xml", getClass());
		SettingsUpdates updates = configurer.applySettingResources(KcdConfigurer.RESOURCE_KEY_KCD_FILE,
				singleton(r));

		// THEN
		assertThat("Updates generated", updates, notNullValue());
		assertThat("Patterns to clean available", updates.getSettingKeyPatternsToClean(),
				notNullValue());
		assertThat("Pattern to clean",
				stream(updates.getSettingKeyPatternsToClean().spliterator(), false).map(p -> p.pattern())
						.collect(toList()),
				Matchers.contains(".*"));
		List<Change> changes = stream(updates.getSettingValueUpdates().spliterator(), false)
				.collect(toList());
		assertThat("Setting change count", changes, hasSize(42));
		int i = 0;
		for ( Change change : changes ) {
			String msg = "Change " + i;
			switch (i) {
				case 0:
					assertChangeEquals(msg, change, instanceId1, "canbusNetwork.propertyFilters['uid']",
							"Canbus Port");
					break;

				case 1:
					assertChangeEquals(msg, change, instanceId1, "busName", "CANB");
					break;

				case 2:
					assertChangeEquals(msg, change, instanceId1, "sourceId", "/BUS1/VEH1");
					break;

				case 3:
					assertChangeEquals(msg, change, instanceId1, "msgConfigsCount", "1");
					break;

				case 4:
					assertChangeEquals(msg, change, instanceId1, "msgConfigs[0].address",
							String.valueOf(0x0C17A708));
					break;

				case 5:
					assertChangeEquals(msg, change, instanceId1, "msgConfigs[0].name", "Stats");
					break;

				case 6:
					assertChangeEquals(msg, change, instanceId1, "msgConfigs[0].interval", "6000");
					break;

				case 7:
					assertChangeEquals(msg, change, instanceId1, "msgConfigs[0].byteOrderingCode", "l");
					break;

				case 8:
					assertChangeEquals(msg, change, instanceId1, "msgConfigs[0].propConfigsCount", "1");
					break;

				case 9:
					assertChangeEquals(msg, change, instanceId1,
							"msgConfigs[0].propConfigs[0].propertyKey", "status");
					break;

				case 10:
					assertChangeEquals(msg, change, instanceId1,
							"msgConfigs[0].propConfigs[0].propertyTypeKey", "s");
					break;

				case 11:
					assertChangeEquals(msg, change, instanceId1,
							"msgConfigs[0].propConfigs[0].dataTypeKey", "u16");
					break;

				case 12:
					assertChangeEquals(msg, change, instanceId1, "msgConfigs[0].propConfigs[0].unit",
							"1");
					break;

				case 13:
					assertChangeEquals(msg, change, instanceId1,
							"msgConfigs[0].propConfigs[0].bitOffset", "0");
					break;

				case 14:
					assertChangeEquals(msg, change, instanceId1,
							"msgConfigs[0].propConfigs[0].bitLength", "16");
					break;

				case 15:
					assertChangeEquals(msg, change, instanceId1, "msgConfigs[0].propConfigs[0].slope",
							"1.0");
					break;

				case 16:
					assertChangeEquals(msg, change, instanceId1,
							"msgConfigs[0].propConfigs[0].intercept", "0");
					break;

				case 17:
					assertChangeEquals(msg, change, instanceId1,
							"msgConfigs[0].propConfigs[0].decimalScale", "-1");
					break;

				case 18:
					assertChangeEquals(msg, change, instanceId1,
							"msgConfigs[0].propConfigs[0].valueLabelsCount", "0");
					break;

				case 19:
					assertChangeEquals(msg, change, instanceId1,
							"msgConfigs[0].propConfigs[0].localizedNamesCount", "0");
					break;

				case 20:
					assertChangeEquals(msg, change, instanceId1, "expressionConfigsCount", "0");
					break;

				case 21:
					assertChangeEquals(msg, change, instanceId2, "canbusNetwork.propertyFilters['uid']",
							"Canbus Port");
					break;

				case 22:
					assertChangeEquals(msg, change, instanceId2, "busName", "CANB");
					break;

				case 23:
					assertChangeEquals(msg, change, instanceId2, "sourceId", "/BUS1/VEH2");
					break;

				case 24:
					assertChangeEquals(msg, change, instanceId2, "msgConfigsCount", "1");
					break;

				case 25:
					assertChangeEquals(msg, change, instanceId2, "msgConfigs[0].address",
							String.valueOf(0x0C17A708));
					break;

				case 26:
					assertChangeEquals(msg, change, instanceId2, "msgConfigs[0].name", "Stats");
					break;

				case 27:
					assertChangeEquals(msg, change, instanceId2, "msgConfigs[0].interval", "6000");
					break;

				case 28:
					assertChangeEquals(msg, change, instanceId2, "msgConfigs[0].byteOrderingCode", "l");
					break;

				case 29:
					assertChangeEquals(msg, change, instanceId2, "msgConfigs[0].propConfigsCount", "1");
					break;

				case 30:
					assertChangeEquals(msg, change, instanceId2,
							"msgConfigs[0].propConfigs[0].propertyKey", "status");
					break;

				case 31:
					assertChangeEquals(msg, change, instanceId2,
							"msgConfigs[0].propConfigs[0].propertyTypeKey", "s");
					break;

				case 32:
					assertChangeEquals(msg, change, instanceId2,
							"msgConfigs[0].propConfigs[0].dataTypeKey", "u16");
					break;

				case 33:
					assertChangeEquals(msg, change, instanceId2, "msgConfigs[0].propConfigs[0].unit",
							"1");
					break;

				case 34:
					assertChangeEquals(msg, change, instanceId2,
							"msgConfigs[0].propConfigs[0].bitOffset", "0");
					break;

				case 35:
					assertChangeEquals(msg, change, instanceId2,
							"msgConfigs[0].propConfigs[0].bitLength", "16");
					break;

				case 36:
					assertChangeEquals(msg, change, instanceId2, "msgConfigs[0].propConfigs[0].slope",
							"1.0");
					break;

				case 37:
					assertChangeEquals(msg, change, instanceId2,
							"msgConfigs[0].propConfigs[0].intercept", "0");
					break;

				case 38:
					assertChangeEquals(msg, change, instanceId2,
							"msgConfigs[0].propConfigs[0].decimalScale", "-1");
					break;

				case 39:
					assertChangeEquals(msg, change, instanceId2,
							"msgConfigs[0].propConfigs[0].valueLabelsCount", "0");
					break;

				case 40:
					assertChangeEquals(msg, change, instanceId2,
							"msgConfigs[0].propConfigs[0].localizedNamesCount", "0");
					break;

				case 41:
					assertChangeEquals(msg, change, instanceId2, "expressionConfigsCount", "0");
					break;

				default:
					fail("Unexpected setting change: " + change);
			}
			i++;
		}
	}

	@Test
	public void parseKcd_expression() throws Exception {
		// GIVEN

		// look for existing instances to delete, but find none
		expect(settingsService.getProvidersForFactory(SETTING_UID)).andReturn(Collections.emptyMap());

		// create new datum data source instances for both <Node> elements
		final String instanceId = "1";
		expect(settingsService.addProviderFactoryInstance(SETTING_UID)).andReturn(instanceId);

		// WHEN
		replayAll();
		ClassPathResource r = new ClassPathResource("kcd-test-05.xml", getClass());
		SettingsUpdates updates = configurer.applySettingResources(KcdConfigurer.RESOURCE_KEY_KCD_FILE,
				singleton(r));

		// THEN
		assertThat("Updates generated", updates, notNullValue());
		assertThat("Patterns to clean available", updates.getSettingKeyPatternsToClean(),
				notNullValue());
		assertThat("Pattern to clean",
				stream(updates.getSettingKeyPatternsToClean().spliterator(), false).map(p -> p.pattern())
						.collect(toList()),
				Matchers.contains(".*"));
		List<Change> changes = stream(updates.getSettingValueUpdates().spliterator(), false)
				.collect(toList());
		assertThat("Setting change count", changes, hasSize(25));
		int i = 0;
		for ( Change change : changes ) {
			String msg = "Change " + i;
			switch (i) {
				case 0:
					assertChangeEquals(msg, change, instanceId, "canbusNetwork.propertyFilters['uid']",
							"Canbus Port");
					break;

				case 1:
					assertChangeEquals(msg, change, instanceId, "busName", "CANB");
					break;

				case 2:
					assertChangeEquals(msg, change, instanceId, "sourceId", "/BUS1/VEH1");
					break;

				case 3:
					assertChangeEquals(msg, change, instanceId, "msgConfigsCount", "1");
					break;

				case 4:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].address",
							String.valueOf(0x0C17A708));
					break;

				case 5:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].name", "Stats");
					break;

				case 6:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].interval", "6000");
					break;

				case 7:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].byteOrderingCode", "l");
					break;

				case 8:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigsCount", "1");
					break;

				case 9:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyKey", "distance");
					break;

				case 10:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyTypeKey", "a");
					break;

				case 11:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].dataTypeKey", "u32");
					break;

				case 12:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].unit",
							"1");
					break;

				case 13:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitOffset",
							"32");
					break;

				case 14:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitLength",
							"32");
					break;

				case 15:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].slope",
							"1.0");
					break;

				case 16:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].intercept",
							"0");
					break;

				case 17:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].decimalScale", "-1");
					break;

				case 18:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabelsCount", "0");
					break;

				case 19:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].localizedNamesCount", "0");
					break;

				case 20:
					assertChangeEquals(msg, change, instanceId, "expressionConfigsCount", "1");
					break;

				case 21:
					assertChangeEquals(msg, change, instanceId, "expressionConfigs[0].name", "area");
					break;

				case 22:
					assertChangeEquals(msg, change, instanceId,
							"expressionConfigs[0].datumPropertyTypeKey", "i");
					break;

				case 23:
					assertChangeEquals(msg, change, instanceId,
							"expressionConfigs[0].expressionServiceId",
							"net.solarnetwork.common.expr.spel.SpelExpressionService");
					break;

				case 24:
					assertChangeEquals(msg, change, instanceId, "expressionConfigs[0].expression",
							"props('distance') * props('distance')");
					break;

				default:
					fail("Unexpected setting change: " + change);
			}
			i++;
		}
	}

	@Test
	public void parseKcd_metadata_setting() throws Exception {
		// GIVEN

		// look for existing instances to delete, but find none
		expect(settingsService.getProvidersForFactory(SETTING_UID)).andReturn(Collections.emptyMap());

		// create new datum data source instances for both <Node> elements
		final String instanceId = "1";
		expect(settingsService.addProviderFactoryInstance(SETTING_UID)).andReturn(instanceId);

		// WHEN
		replayAll();
		ClassPathResource r = new ClassPathResource("kcd-test-06.xml", getClass());
		SettingsUpdates updates = configurer.applySettingResources(KcdConfigurer.RESOURCE_KEY_KCD_FILE,
				singleton(r));

		// THEN
		assertThat("Updates generated", updates, notNullValue());
		assertThat("Patterns to clean available", updates.getSettingKeyPatternsToClean(),
				notNullValue());
		assertThat("Pattern to clean",
				stream(updates.getSettingKeyPatternsToClean().spliterator(), false).map(p -> p.pattern())
						.collect(toList()),
				Matchers.contains(".*"));
		List<Change> changes = stream(updates.getSettingValueUpdates().spliterator(), false)
				.collect(toList());
		assertThat("Setting change count", changes, hasSize(22));
		int i = 0;
		for ( Change change : changes ) {
			String msg = "Change " + i;
			switch (i) {
				case 0:
					assertChangeEquals(msg, change, instanceId, "canbusNetwork.propertyFilters['uid']",
							"Canbus Port");
					break;

				case 1:
					assertChangeEquals(msg, change, instanceId, "busName", "CANB");
					break;

				case 2:
					assertChangeEquals(msg, change, instanceId, "sourceId", "/BUS1/VEH1");
					break;

				case 3:
					assertChangeEquals(msg, change, instanceId,
							"samplesTransformService.propertyFilters['uid']", "Vehicle Virtual Meter");
					break;

				case 4:
					assertChangeEquals(msg, change, instanceId, "msgConfigsCount", "1");
					break;

				case 5:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].address",
							String.valueOf(0x0C17A708));
					break;

				case 6:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].name", "Stats");
					break;

				case 7:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].interval", "6000");
					break;

				case 8:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].byteOrderingCode", "l");
					break;

				case 9:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigsCount", "1");
					break;

				case 10:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyKey", "distance");
					break;

				case 11:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].propertyTypeKey", "a");
					break;

				case 12:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].dataTypeKey", "u32");
					break;

				case 13:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].unit",
							"1");
					break;

				case 14:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitOffset",
							"32");
					break;

				case 15:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].bitLength",
							"32");
					break;

				case 16:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].slope",
							"1.0");
					break;

				case 17:
					assertChangeEquals(msg, change, instanceId, "msgConfigs[0].propConfigs[0].intercept",
							"0");
					break;

				case 18:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].decimalScale", "-1");
					break;

				case 19:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].valueLabelsCount", "0");
					break;

				case 20:
					assertChangeEquals(msg, change, instanceId,
							"msgConfigs[0].propConfigs[0].localizedNamesCount", "0");
					break;

				case 21:
					assertChangeEquals(msg, change, instanceId, "expressionConfigsCount", "0");
					break;

				default:
					fail("Unexpected setting change: " + change);
			}
			i++;
		}
	}

}
