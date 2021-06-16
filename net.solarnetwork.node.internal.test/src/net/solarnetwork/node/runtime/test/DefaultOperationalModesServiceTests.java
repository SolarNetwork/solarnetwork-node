/* ==================================================================
 * DefaultOperationalModesServiceTests.java - 10/06/2021 8:53:17 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.runtime.DefaultOperationalModesService;
import net.solarnetwork.node.support.KeyValuePair;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link DefaultOperationalModesService} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("deprecation")
public class DefaultOperationalModesServiceTests {

	private EventAdmin eventAdmin;
	private SettingDao settingDao;
	private DefaultOperationalModesService service;

	@Before
	public void setup() {
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		settingDao = EasyMock.createMock(SettingDao.class);
		service = new DefaultOperationalModesService(new StaticOptionalService<>(settingDao),
				new StaticOptionalService<>(eventAdmin));
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin, settingDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(eventAdmin, settingDao);
	}

	private void expectQueryForActiveModes(String... active) {
		List<KeyValuePair> result = new ArrayList<>();
		if ( active != null ) {
			for ( String mode : active ) {
				result.add(new KeyValuePair(DefaultOperationalModesService.SETTING_OP_MODE, mode));
			}
		}
		expect(settingDao.getSettings(DefaultOperationalModesService.SETTING_OP_MODE)).andReturn(result);
	}

	private void expectQueryForActiveMode(String mode, boolean active) {
		expect(settingDao.getSetting(DefaultOperationalModesService.SETTING_OP_MODE, mode))
				.andReturn(active ? mode : null);
	}

	private void expectQueryForModeExpiry(Map<String, Long> expiryMap) {
		if ( expiryMap == null ) {
			return;
		}
		for ( Map.Entry<String, Long> me : expiryMap.entrySet() ) {
			String r = (me.getValue() != null ? me.getValue().toString() : null);
			expect(settingDao.getSetting(DefaultOperationalModesService.SETTING_OP_MODE_EXPIRE,
					me.getKey())).andReturn(r);
		}
	}

	@Test
	public void init_nothingActive() {
		// GIVEN
		expectQueryForActiveModes();

		// WHEN
		replayAll();
		service.init();

		// THEN

	}

	@SuppressWarnings("unchecked")
	@Test
	public void init_oneActive() {
		// GIVEN
		expectQueryForActiveModes("test");
		expectQueryForModeExpiry(Collections.singletonMap("test", null));

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		service.init();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Mode change event posted", evt.getTopic(),
				is(equalTo(OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED)));
		assertThat("Mode changed event modes set available",
				evt.getProperty(OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES),
				instanceOf(Set.class));
		assertThat("Active modes provided",
				(Set<String>) evt
						.getProperty(OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES),
				containsInAnyOrder("test"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void init_multiActiveWithExpired() {
		// GIVEN
		expectQueryForActiveModes("test", "foo", "bar");
		Map<String, Long> expiryMap = new LinkedHashMap<>(3);
		expiryMap.put("test", null);
		expiryMap.put("foo", System.currentTimeMillis() - 1000L);
		expiryMap.put("bar", null);
		expectQueryForModeExpiry(expiryMap);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		service.init();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Mode change event posted", evt.getTopic(),
				is(equalTo(OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED)));
		assertThat("Mode changed event modes set available",
				evt.getProperty(OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES),
				instanceOf(Set.class));
		assertThat("Active modes provided",
				(Set<String>) evt
						.getProperty(OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES),
				containsInAnyOrder("test", "bar"));
	}

	@Test
	public void isActive_null() {
		// GIVEN

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive(null);

		// THEN
		assertThat("Null is default is always active", result, is(equalTo(true)));
	}

	@Test
	public void isActive_empty() {
		// GIVEN

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("");

		// THEN
		assertThat("Empty is default is always active", result, is(equalTo(true)));
	}

	@Test
	public void isActive_no() {
		// GIVEN
		expectQueryForActiveMode("test", false);

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("test");

		// THEN
		assertThat("Mode is not active", result, is(equalTo(false)));
	}

	@Test
	public void isActive_yes() {
		// GIVEN
		expectQueryForActiveMode("test", true);

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("test");

		// THEN
		assertThat("Mode is active", result, is(equalTo(true)));
	}

	@Test
	public void isActive_no_inverted() {
		// GIVEN
		expectQueryForActiveMode("test", false);

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("!test");

		// THEN
		assertThat("Inverted mode is active", result, is(equalTo(true)));
	}

	@Test
	public void isActive_yes_inverted() {
		// GIVEN
		expectQueryForActiveMode("test", true);

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("!test");

		// THEN
		assertThat("Inverted mode is not active", result, is(equalTo(false)));
	}

}
