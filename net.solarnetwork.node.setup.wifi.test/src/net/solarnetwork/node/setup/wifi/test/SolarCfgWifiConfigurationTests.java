/* ==================================================================
 * SolarCfgWifiConfigurationTests.java - 23/06/2020 7:24:29 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.wifi.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.setup.wifi.SolarCfgWifiConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.TextFieldSettingSpecifier;

/**
 * Test cases for the {@link SolarCfgWifiConfiguration} class.
 * 
 * @author matt
 * @version 2.0
 */
public class SolarCfgWifiConfigurationTests {

	private SolarCfgWifiConfiguration service;

	private File tmpFile;

	@Before
	public void setup() throws IOException {
		service = new SolarCfgWifiConfiguration();
		// could make script name configurable... i.e. support other OSes?
		// copy script to file so test can run via JAR (i.e. Ant)
		tmpFile = File.createTempFile("solarcfg-", ".sh");
		tmpFile.setExecutable(true, true);
		FileCopyUtils.copy(getClass().getResourceAsStream("solarcfg.sh"), new FileOutputStream(tmpFile));
		service.setCommand(tmpFile.getAbsolutePath());

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SolarCfgWifiConfiguration.class.getName());
		service.setMessageSource(msgSource);
	}

	@After
	public void teardown() {
		if ( tmpFile != null ) {
			tmpFile.delete();
		}
	}

	private TextFieldSettingSpecifier textSetting(String key, List<SettingSpecifier> settings) {
		TextFieldSettingSpecifier setting = (TextFieldSettingSpecifier) settings.stream()
				.filter(s -> (s instanceof TextFieldSettingSpecifier)
						&& key.equals(((TextFieldSettingSpecifier) s).getKey()))
				.findAny().orElseThrow(IllegalArgumentException::new);
		return setting;
	}

	@Test
	public void getSettings() {
		// WHEN
		List<SettingSpecifier> settings = service.getSettingSpecifiers();

		// THEN
		assertThat("Settings returned", settings, hasSize(4));

		TextFieldSettingSpecifier setting = textSetting("country", settings);
		assertThat("Country current value", setting.getDefaultValue(), equalTo("NZ"));

		setting = textSetting("ssid", settings);
		assertThat("SSID current value", setting.getDefaultValue(), equalTo("Test SSID"));
	}

	@Test
	public void updateSettings() {
		// GIVEN
		service.setCountry("ZZ");
		service.setSsid("SSSSIIDD");
		service.setPassword("1234567890");

		// WHEN
		List<String> result = service.updateConfiguration();

		// THEN
		assertThat("Result returned", result, hasSize(3));
		assertThat("Results echoed country, SSID, and password length", result,
				hasItems("ZZ", "SSSSIIDD", "10"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void systemConfigure() {
		// GIVEN
		final String country = "nz";
		final String ssid = "foo-ssid";
		final String password = "secret-password";

		// WHEN
		Map<String, String> instrParams = new LinkedHashMap<>(4);
		instrParams.put(InstructionHandler.PARAM_SERVICE, SolarCfgWifiConfiguration.WIFI_SERVICE_NAME);
		instrParams.put(SolarCfgWifiConfiguration.PARAM_COUNTRY, country);
		instrParams.put(SolarCfgWifiConfiguration.PARAM_SSID, ssid);
		instrParams.put(SolarCfgWifiConfiguration.PARAM_PASSWORD, password);
		Instruction instr = InstructionUtils
				.createLocalInstruction(InstructionHandler.TOPIC_SYSTEM_CONFIGURE, instrParams);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Instruction returned result", result, is(notNullValue()));
		assertThat("Compelted OK", result.getInstructionState(), is(InstructionState.Completed));
		assertThat("Result parameters provided", result.getResultParameters(), is(notNullValue()));
		assertThat("Result parameter result provided",
				result.getResultParameters().get(InstructionHandler.PARAM_SERVICE_RESULT),
				is(instanceOf(List.class)));
		assertThat("Result parameter result list has country, ssid, and password length values",
				(List<String>) result.getResultParameters().get(InstructionHandler.PARAM_SERVICE_RESULT),
				contains(country, ssid, String.valueOf(password.length())));
	}

	@Test
	public void systemConfigure_status() {
		// GIVEN
		service.setCountry("ZZ");
		service.setSsid("SSSSIIDD");
		service.setPassword("1234567890");

		// WHEN
		Map<String, String> instrParams = new LinkedHashMap<>(4);
		instrParams.put(InstructionHandler.PARAM_SERVICE, SolarCfgWifiConfiguration.WIFI_SERVICE_NAME);
		Instruction instr = InstructionUtils
				.createLocalInstruction(InstructionHandler.TOPIC_SYSTEM_CONFIGURE, instrParams);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Instruction returned result", result, is(notNullValue()));
		assertThat("Compelted OK", result.getInstructionState(), is(InstructionState.Completed));
		assertThat("Result parameters provided", result.getResultParameters(), is(notNullValue()));
		assertThat("Result parameter result provided",
				result.getResultParameters().get(InstructionHandler.PARAM_SERVICE_RESULT),
				is(instanceOf(SolarCfgWifiConfiguration.Status.class)));
		SolarCfgWifiConfiguration.Status resultStatus = (SolarCfgWifiConfiguration.Status) result
				.getResultParameters().get(InstructionHandler.PARAM_SERVICE_RESULT);
		assertThat("Result status is active", resultStatus.isActive(), is(equalTo(true)));
		assertThat("Result status address list", resultStatus.getAddresses(),
				contains("127.0.1.1", "2406:e006:3093:b301:65b1:4726:2af:d721"));
	}

}
