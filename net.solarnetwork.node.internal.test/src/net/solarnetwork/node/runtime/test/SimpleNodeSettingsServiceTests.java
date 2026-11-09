/* ==================================================================
 * SimpleNodeSettingsServiceTests.java - 09/11/2026 3:53:39 pm
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

package net.solarnetwork.node.runtime.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_SERVICE;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_SERVICE_RESULT;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SYSTEM_CONFIGURATION;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.node.runtime.SimpleNodeSettingsService.PARAM_SETTING_UID;
import static net.solarnetwork.node.runtime.SimpleNodeSettingsService.SETTINGS_SERVICE_UID;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.runtime.SimpleNodeSettingsService;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Test cases for the {@link SimpleNodeSettingsService} class.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleNodeSettingsServiceTests {

	private static final String TEST_SETTING_UID = "test.provider";

	private SettingsService settingsService;
	private SettingSpecifierProvider provider;
	private SimpleNodeSettingsService service;

	@Before
	public void setup() {
		settingsService = EasyMock.createMock(SettingsService.class);
		provider = EasyMock.createMock(SettingSpecifierProvider.class);
		service = new SimpleNodeSettingsService(new StaticOptionalService<>(settingsService));
	}

	private void replayAll() {
		EasyMock.replay(settingsService, provider);
	}

	@After
	public void teardown() {
		EasyMock.verify(settingsService, provider);
	}

	private void expectProviderSettings(List<SettingSpecifier> specs, Setting... settings) {
		expect(settingsService.getProviders()).andReturn(singletonList(provider));
		expect(provider.getSettingUid()).andReturn(TEST_SETTING_UID).anyTimes();
		expect(provider.getSettingSpecifiers()).andReturn(specs);
		expect(settingsService.getSettings(null, TEST_SETTING_UID)).andReturn(asList(settings));
	}

	private Object listProviderSettings() {
		Map<String, String> params = new HashMap<>(2);
		params.put(PARAM_SERVICE, SETTINGS_SERVICE_UID);
		params.put(PARAM_SETTING_UID, TEST_SETTING_UID);
		InstructionStatus result = service
				.processInstruction(createLocalInstruction(TOPIC_SYSTEM_CONFIGURATION, params));
		assertThat("Status result returned", result, is(notNullValue()));
		assertThat("State is Completed", result.getInstructionState(), is(equalTo(Completed)));
		return result.getResultParameters().get(PARAM_SERVICE_RESULT);
	}

	@Test
	public void providerSettings_sensitiveKey() {
		// GIVEN
		expectProviderSettings(
				asList(new BasicTextFieldSettingSpecifier("host", ""),
						new BasicTextFieldSettingSpecifier("apiKey", "")),
				new Setting(TEST_SETTING_UID, "host", "example.com", null),
				new Setting(TEST_SETTING_UID, "apiKey", "abc123", null));

		// WHEN
		replayAll();
		Object json = listProviderSettings();

		// THEN
		assertThat("Value with sensitive key redacted", json, is(equalTo(
				"[{\"key\":\"host\",\"value\":\"example.com\"},{\"key\":\"apiKey\",\"value\":\"*****\"}]")));
	}

	@Test
	public void providerSettings_secureTextEntry() {
		// GIVEN
		expectProviderSettings(
				asList(new BasicTextFieldSettingSpecifier("address", ""),
						new BasicTextFieldSettingSpecifier("key", "", true)),
				new Setting(TEST_SETTING_UID, "address", "12345678", null),
				new Setting(TEST_SETTING_UID, "key", "0123456789ABCDEF", null));

		// WHEN
		replayAll();
		Object json = listProviderSettings();

		// THEN
		assertThat("Secure text entry value redacted", json, is(equalTo(
				"[{\"key\":\"address\",\"value\":\"12345678\"},{\"key\":\"key\",\"value\":\"*****\"}]")));
	}

}
