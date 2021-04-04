/* ==================================================================
 * SettingsPlaceholderServiceTests.java - 25/08/2020 11:23:08 AM
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

package net.solarnetwork.node.support.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.support.SettingsPlaceholderService;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link SettingsPlaceholderService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SettingsPlaceholderServiceTests {

	private SettingDao settingDao;
	private SettingsPlaceholderService service;

	@Before
	public void setup() {
		settingDao = EasyMock.createMock(SettingDao.class);
		service = new SettingsPlaceholderService(new StaticOptionalService<SettingDao>(settingDao));
		service.setStaticPropertiesPath(Paths.get("environment/test/placeholders"));
	}

	private void replayAll() {
		EasyMock.replay(settingDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(settingDao);
	}

	@Test
	public void nullInput() {
		// WHEN
		replayAll();
		String result = service.resolvePlaceholders(null, null);

		// THEN
		assertThat("Resolved null output on null input", result, nullValue());
	}

	@Test
	public void emptyInput() {
		// WHEN
		replayAll();
		String result = service.resolvePlaceholders("", null);

		// THEN
		assertThat("Resolved empty output on empty input", result, equalTo(""));
	}

	@Test
	public void noPlaceholdersInput() {
		// WHEN
		replayAll();
		String input = "this is not the string you are looking for";
		String result = service.resolvePlaceholders(input, null);

		// THEN
		assertThat("Resolved output matches input because no placeholders", result, equalTo(input));
	}

	@Test
	public void resolveStaticOnly() {
		// GIVEN
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(null);

		// WHEN
		replayAll();
		String result = service.resolvePlaceholders("{a} + {b} = {c}", null);

		// THEN
		assertThat("Resolved static placeholders", result, equalTo("one + two = three"));
	}

	@Test
	public void resolveStaticOnly_withDefault() {
		// GIVEN
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(null);

		// WHEN
		replayAll();
		String result = service.resolvePlaceholders("{a} != {e:Foo}", null);

		// THEN
		assertThat("Resolved static placeholders with default", result, equalTo("one != Foo"));
	}

	@Test
	public void resolveWithSettings() {
		// GIVEN
		List<KeyValuePair> data = Arrays.asList(new KeyValuePair("foo", "bar"));
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(data);

		// WHEN
		replayAll();
		String result = service.resolvePlaceholders("{a} != {foo}", null);

		// THEN
		assertThat("Resolved static placeholders with default", result, equalTo("one != bar"));
	}

	@Test
	public void resolveWithSettingsOnly() {
		// GIVEN
		service.setStaticPropertiesPath(null);
		List<KeyValuePair> data = Arrays.asList(new KeyValuePair("foo", "bar"));
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(data);

		// WHEN
		replayAll();
		String result = service.resolvePlaceholders("{a} != {foo}", null);

		// THEN
		assertThat("Resolved static placeholders with default", result, equalTo(" != bar"));
	}

	@Test
	public void resolveWithParametersArgument() {
		// GIVEN
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(null);

		// WHEN
		replayAll();
		Map<String, Object> params = new HashMap<>(4);
		params.put("a", "eh");
		params.put("foo", "boo");
		String result = service.resolvePlaceholders("{a} != {foo} != {b}", params);

		// THEN
		assertThat("Resolved static placeholders with default", result, equalTo("eh != boo != two"));
	}

	@Test
	public void register() {
		// GIVEN
		settingDao.storeSetting(SettingsPlaceholderService.SETTING_KEY, "foo", "bar");
		settingDao.storeSetting(SettingsPlaceholderService.SETTING_KEY, "bim", "bam");

		// WHEN
		replayAll();
		Map<String, String> params = new LinkedHashMap<>(4);
		params.put("foo", "bar");
		params.put("bim", "bam");
		service.registerParameters(params);
	}

}
