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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.runtime.DefaultOperationalModesService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DefaultOperationalModesService} class.
 * 
 * @author matt
 * @version 1.1
 */
public class DefaultOperationalModesServiceTests {

	private EventAdmin eventAdmin;
	private SettingDao settingDao;
	private PlatformTransactionManager txManager;
	private DefaultOperationalModesService service;
	private ConcurrentMap<String, Long> activeModeCache;

	@Before
	public void setup() {
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		settingDao = EasyMock.createMock(SettingDao.class);
		txManager = EasyMock.createMock(PlatformTransactionManager.class);
		activeModeCache = new ConcurrentHashMap<>(8, 0.9f, 1);
		service = new DefaultOperationalModesService(activeModeCache,
				new StaticOptionalService<>(settingDao), new StaticOptionalService<>(eventAdmin));
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin, settingDao, txManager);
	}

	@After
	public void teardown() {
		EasyMock.verify(eventAdmin, settingDao, txManager);
	}

	private void expectQueryForActiveModes(String... active) {
		List<KeyValuePair> result = new ArrayList<>();
		if ( active != null ) {
			for ( String mode : active ) {
				result.add(new KeyValuePair(mode, mode));
			}
		}
		expect(settingDao.getSettingValues(DefaultOperationalModesService.SETTING_OP_MODE))
				.andReturn(result);
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

	@SuppressWarnings("unchecked")
	private void assertModesChangedEvent(String msg, Event evt, String... modes) {
		assertThat(format("%s event posted", msg), evt, is(notNullValue()));
		assertThat(format("%s mode change event topic", msg), evt.getTopic(),
				is(equalTo(OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED)));
		assertThat(format("%s mode changed event modes set available", msg),
				evt.getProperty(OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES),
				is(instanceOf(Set.class)));
		if ( modes == null ) {
			modes = new String[0];
		}
		assertThat(format("%s active modes provided", msg),
				(Set<String>) evt
						.getProperty(OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES),
				containsInAnyOrder(modes));
	}

	@Test
	public void init_nothingActive() {
		// GIVEN
		expectQueryForActiveModes();

		// WHEN
		replayAll();
		service.serviceDidStartup();

		// THEN
	}

	@Test
	public void init_oneActive() {
		// GIVEN
		expectQueryForActiveModes("test");
		expectQueryForModeExpiry(singletonMap("test", null));

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		service.serviceDidStartup();

		// THEN
		assertModesChangedEvent("Init active", eventCaptor.getValue(), "test");
	}

	@Test
	public void init_oneActive_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		TransactionStatus txStatus = new SimpleTransactionStatus();
		expect(txManager.getTransaction(notNull(TransactionDefinition.class))).andReturn(txStatus);

		expectQueryForActiveModes("test");
		expectQueryForModeExpiry(singletonMap("test", null));

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		txManager.commit(txStatus);

		// WHEN
		replayAll();
		service.serviceDidStartup();

		// THEN
		assertModesChangedEvent("Init TX active", eventCaptor.getValue(), "test");
	}

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
		service.serviceDidStartup();

		// THEN
		assertModesChangedEvent("Init multi w/expired active", eventCaptor.getValue(), "test", "bar");
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
	public void isActive_empty_inverted_no() {
		// GIVEN

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("!");

		// THEN
		assertThat("Invertered empty is default is active when no mode active", result,
				is(equalTo(true)));
	}

	@Test
	public void isActive_empty_inverted_yes() {
		// GIVEN
		activeModeCache.put("test", DefaultOperationalModesService.NO_EXPIRATION);

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("!");

		// THEN
		assertThat("Invertered empty is default is not active when some mode active", result,
				is(equalTo(false)));
	}

	@Test
	public void isActive_no() {
		// GIVEN

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("test");

		// THEN
		assertThat("Mode is not active", result, is(equalTo(false)));
	}

	@Test
	public void isActive_yes() {
		// GIVEN
		activeModeCache.put("test", DefaultOperationalModesService.NO_EXPIRATION);

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("test");

		// THEN
		assertThat("Mode is active", result, is(equalTo(true)));
	}

	@Test
	public void isActive_no_inverted() {
		// GIVEN

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("!test");

		// THEN
		assertThat("Inverted mode is active", result, is(equalTo(true)));
	}

	@Test
	public void isActive_yes_inverted() {
		// GIVEN
		activeModeCache.put("test", DefaultOperationalModesService.NO_EXPIRATION);

		// WHEN
		replayAll();
		boolean result = service.isOperationalModeActive("!test");

		// THEN
		assertThat("Inverted mode is not active", result, is(equalTo(false)));
	}

	@Test
	public void enableMode() {
		// GIVEN
		settingDao.storeSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test", "test");
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE_EXPIRE, "test"))
				.andReturn(false);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		Set<String> active = service.enableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode activated", active, containsInAnyOrder("test"));
		assertModesChangedEvent("Activated ", eventCaptor.getValue(), "test");
	}

	@Test
	public void enableMode_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		TransactionStatus txStatus = new SimpleTransactionStatus();
		expect(txManager.getTransaction(notNull(TransactionDefinition.class))).andReturn(txStatus);

		settingDao.storeSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test", "test");
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE_EXPIRE, "test"))
				.andReturn(false);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		txManager.commit(txStatus);

		// WHEN
		replayAll();
		Set<String> active = service.enableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode activated", active, containsInAnyOrder("test"));
		assertModesChangedEvent("Activated ", eventCaptor.getValue(), "test");
	}

	@Test
	public void enableMode_alreadyActive() {
		// GIVEN
		activeModeCache.put("test", DefaultOperationalModesService.NO_EXPIRATION);

		// WHEN
		replayAll();
		Set<String> active = service.enableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode activated", active, containsInAnyOrder("test"));
	}

	@Test
	public void enableMode_alreadyActive_tx() {
		// GIVEN

		// configure tx manager, and prove no transaction used when mode already active
		service.setTransactionManager(new StaticOptionalService<>(txManager));
		activeModeCache.put("test", DefaultOperationalModesService.NO_EXPIRATION);

		// WHEN
		replayAll();
		Set<String> active = service.enableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode activated", active, containsInAnyOrder("test"));
	}

	@Test
	public void enableMode_expiring() {
		// GIVEN
		settingDao.storeSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test", "test");

		// @formatter:off
		Capture<String> expireCaptor = new Capture<>();
		settingDao.storeSetting(
				eq(DefaultOperationalModesService.SETTING_OP_MODE_EXPIRE),
				eq("test"),
				capture(expireCaptor));
		// @formatter:on

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		Instant expire = Instant.now().plus(1, ChronoUnit.HOURS);
		Set<String> active = service.enableOperationalModes(singleton("test"), expire);

		// THEN
		assertThat("Mode activated", active, containsInAnyOrder("test"));
		assertModesChangedEvent("Activated ", eventCaptor.getValue(), "test");
		assertThat("Expire value saved", expireCaptor.getValue(),
				is(equalTo(String.valueOf(expire.toEpochMilli()))));
	}

	@Test
	public void enableMode_expiring_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		TransactionStatus txStatus = new SimpleTransactionStatus();
		expect(txManager.getTransaction(notNull(TransactionDefinition.class))).andReturn(txStatus);

		settingDao.storeSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test", "test");

		// @formatter:off
		Capture<String> expireCaptor = new Capture<>();
		settingDao.storeSetting(
				eq(DefaultOperationalModesService.SETTING_OP_MODE_EXPIRE),
				eq("test"),
				capture(expireCaptor));
		// @formatter:on

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		txManager.commit(txStatus);

		// WHEN
		replayAll();
		Instant expire = Instant.now().plus(1, ChronoUnit.HOURS);
		Set<String> active = service.enableOperationalModes(singleton("test"), expire);

		// THEN
		assertThat("Mode activated", active, containsInAnyOrder("test"));
		assertModesChangedEvent("Activated ", eventCaptor.getValue(), "test");
		assertThat("Expire value saved", expireCaptor.getValue(),
				is(equalTo(String.valueOf(expire.toEpochMilli()))));
	}

	@Test
	public void enableMode_expiring_unchanged() {
		// GIVEN
		Instant expire = Instant.now().plus(1, ChronoUnit.HOURS);
		activeModeCache.put("test", expire.toEpochMilli());

		// WHEN
		replayAll();
		Set<String> active = service.enableOperationalModes(singleton("test"), expire);

		// THEN
		assertThat("Mode activated", active, containsInAnyOrder("test"));
	}

	@Test
	public void enableMode_expiring_unchanged_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));
		Instant expire = Instant.now().plus(1, ChronoUnit.HOURS);
		activeModeCache.put("test", expire.toEpochMilli());

		// WHEN
		replayAll();
		Set<String> active = service.enableOperationalModes(singleton("test"), expire);

		// THEN
		assertThat("Mode activated", active, containsInAnyOrder("test"));
	}

	@Test
	public void disableMode() {
		// GIVEN
		activeModeCache.put("test", DefaultOperationalModesService.NO_EXPIRATION);
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test"))
				.andReturn(true);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		Set<String> active = service.disableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode deactivated", active, hasSize(0));
		assertModesChangedEvent("Deactivated ", eventCaptor.getValue());
	}

	@Test
	public void disableMode_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		TransactionStatus txStatus = new SimpleTransactionStatus();
		expect(txManager.getTransaction(notNull(TransactionDefinition.class))).andReturn(txStatus);

		activeModeCache.put("test", DefaultOperationalModesService.NO_EXPIRATION);
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test"))
				.andReturn(true);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		txManager.commit(txStatus);

		// WHEN
		replayAll();
		Set<String> active = service.disableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode deactivated", active, hasSize(0));
		assertModesChangedEvent("Deactivated ", eventCaptor.getValue());
	}

	@Test
	public void disableMode_multi() {
		// GIVEN
		activeModeCache.put("test", DefaultOperationalModesService.NO_EXPIRATION);
		activeModeCache.put("foo", DefaultOperationalModesService.NO_EXPIRATION);
		activeModeCache.put("bar", DefaultOperationalModesService.NO_EXPIRATION);
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test"))
				.andReturn(true);
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE, "bar"))
				.andReturn(true);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		Set<String> active = service.disableOperationalModes(new LinkedHashSet<>(asList("test", "bar")));

		// THEN
		assertThat("Mode deactivated", active, containsInAnyOrder("foo"));
		assertModesChangedEvent("Deactivated ", eventCaptor.getValue(), "foo");
	}

	@Test
	public void disableMode_multi_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		TransactionStatus txStatus = new SimpleTransactionStatus();
		expect(txManager.getTransaction(notNull(TransactionDefinition.class))).andReturn(txStatus);

		activeModeCache.put("test", DefaultOperationalModesService.NO_EXPIRATION);
		activeModeCache.put("foo", DefaultOperationalModesService.NO_EXPIRATION);
		activeModeCache.put("bar", DefaultOperationalModesService.NO_EXPIRATION);
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test"))
				.andReturn(true);
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE, "bar"))
				.andReturn(true);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		txManager.commit(txStatus);

		// WHEN
		replayAll();
		Set<String> active = service.disableOperationalModes(new LinkedHashSet<>(asList("test", "bar")));

		// THEN
		assertThat("Mode deactivated", active, containsInAnyOrder("foo"));
		assertModesChangedEvent("Deactivated ", eventCaptor.getValue(), "foo");
	}

	@Test
	public void disableMode_noneActive() {
		// GIVEN

		// WHEN
		replayAll();
		Set<String> active = service.disableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode deactivated", active, hasSize(0));
	}

	@Test
	public void disableMode_noneActive_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		// WHEN
		replayAll();
		Set<String> active = service.disableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode deactivated", active, hasSize(0));
	}

	@Test
	public void disableMode_notActive() {
		// GIVEN
		activeModeCache.put("foo", DefaultOperationalModesService.NO_EXPIRATION);

		// WHEN
		replayAll();
		Set<String> active = service.disableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode deactivated", active, containsInAnyOrder("foo"));
	}

	@Test
	public void disableMode_notActive_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));
		activeModeCache.put("foo", DefaultOperationalModesService.NO_EXPIRATION);

		// WHEN
		replayAll();
		Set<String> active = service.disableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode deactivated", active, containsInAnyOrder("foo"));
	}

	@Test
	public void disableMode_expired() {
		// GIVEN
		Long expire = System.currentTimeMillis() - 10_000L;
		activeModeCache.put("test", expire);

		// WHEN
		replayAll();
		Set<String> active = service.disableOperationalModes(singleton("test"));

		// THEN
		assertThat("Mode deactivated", active, hasSize(0));
	}

	@Test
	public void expire_empty() {
		// GIVEN

		// WHEN
		replayAll();
		service.expireNow();

		// THEN
	}

	@Test
	public void expire_empty_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		// WHEN
		replayAll();
		service.expireNow();

		// THEN
	}

	@Test
	public void expire_notExpired() {
		// GIVEN
		Long expire = System.currentTimeMillis() + 10_000L;
		activeModeCache.put("test", expire);

		// WHEN
		replayAll();
		service.expireNow();

		// THEN
		assertThat("Non-expired mode remains", activeModeCache, hasEntry("test", expire));
	}

	@Test
	public void expire_notExpired_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		Long expire = System.currentTimeMillis() + 10_000L;
		activeModeCache.put("test", expire);

		// WHEN
		replayAll();
		service.expireNow();

		// THEN
		assertThat("Non-expired mode remains", activeModeCache, hasEntry("test", expire));
	}

	@Test
	public void expire_expired() {
		// GIVEN
		Long expire = System.currentTimeMillis() - 10_000L;
		activeModeCache.put("test", expire);

		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test"))
				.andReturn(true);
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE_EXPIRE, "test"))
				.andReturn(true);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		service.expireNow();

		// THEN
		assertThat("Expired mode purged", activeModeCache, not(hasEntry("test", expire)));
		assertModesChangedEvent("Expired ", eventCaptor.getValue());
	}

	@Test
	public void expire_expired_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		TransactionStatus txStatus = new SimpleTransactionStatus();
		expect(txManager.getTransaction(notNull(TransactionDefinition.class))).andReturn(txStatus);

		Long expire = System.currentTimeMillis() - 10_000L;
		activeModeCache.put("test", expire);

		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE, "test"))
				.andReturn(true);
		expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE_EXPIRE, "test"))
				.andReturn(true);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		txManager.commit(txStatus);

		// WHEN
		replayAll();
		service.expireNow();

		// THEN
		assertThat("Expired mode purged", activeModeCache, not(hasEntry("test", expire)));
		assertModesChangedEvent("Expired ", eventCaptor.getValue());
	}

	@Test
	public void expire_expired_some_tx() {
		// GIVEN
		service.setTransactionManager(new StaticOptionalService<>(txManager));

		TransactionStatus txStatus = new SimpleTransactionStatus();
		expect(txManager.getTransaction(notNull(TransactionDefinition.class))).andReturn(txStatus);

		Long expire = System.currentTimeMillis() - 10_000L;
		activeModeCache.put("test", expire);
		activeModeCache.put("foo", expire);
		activeModeCache.put("bar", DefaultOperationalModesService.NO_EXPIRATION);

		for ( String mode : Arrays.asList("test", "foo") ) {
			expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE, mode))
					.andReturn(true);
			expect(settingDao.deleteSetting(DefaultOperationalModesService.SETTING_OP_MODE_EXPIRE, mode))
					.andReturn(true);
		}

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		txManager.commit(txStatus);

		// WHEN
		replayAll();
		service.expireNow();

		// THEN
		assertThat("Expired mode purged", activeModeCache, not(hasEntry("test", expire)));
		assertThat("Expired mode purged", activeModeCache, not(hasEntry("foo", expire)));
		assertThat("Non-expired mode remains", activeModeCache,
				hasEntry("bar", DefaultOperationalModesService.NO_EXPIRATION));
		assertModesChangedEvent("Expired ", eventCaptor.getValue(), "bar");
	}

}
