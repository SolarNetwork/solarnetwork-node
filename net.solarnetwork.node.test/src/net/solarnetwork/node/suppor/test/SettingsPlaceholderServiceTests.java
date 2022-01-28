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

package net.solarnetwork.node.suppor.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.dao.TransientDataAccessResourceException;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.service.support.SettingsPlaceholderService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link SettingsPlaceholderService} class.
 * 
 * <p>
 * <b>Note</b> this test is not in the
 * {@code net.solarnetwork.node.internal.test} project because of the file
 * system access required during testing.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class SettingsPlaceholderServiceTests {

	private ExecutorService executor;
	private SettingDao settingDao;
	private SettingsPlaceholderService service;

	private final Logger log = LoggerFactory.getLogger(SettingsPlaceholderServiceTests.class);

	@Before
	public void setup() {
		executor = Executors.newCachedThreadPool();
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
		executor.shutdownNow();
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
	public void resolveWithSettings_retryAfterTransientFailure() {
		// GIVEN

		// first try throw exception
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY))
				.andThrow(new TransientDataAccessResourceException("Try again!"));

		// second try succeed
		List<KeyValuePair> data = Arrays.asList(new KeyValuePair("foo", "bar"));
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(data);

		// WHEN
		replayAll();
		String result = service.resolvePlaceholders("{a} != {foo}", null);

		// THEN
		assertThat("Resolved static placeholders with default after DAO retry", result,
				equalTo("one != bar"));
	}

	@Test
	public void resolveWithSettings_giveUpAfterRetryAfterTransientFailures() {
		// GIVEN
		service.setDaoRetryCount(2);

		// first try + 2 retry throw exception
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY))
				.andThrow(new TransientDataAccessResourceException("Try again!")).times(3);

		// WHEN
		replayAll();
		String result = service.resolvePlaceholders("{a} != {foo}", null);

		// THEN
		assertThat("Resolved static placeholders with default only after DAO retries giveup", result,
				equalTo("one != "));
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
	public void resolveWithParametersArgument_cached() {
		// GIVEN
		service.setCacheSeconds(Integer.MAX_VALUE);
		List<KeyValuePair> data = Arrays.asList(new KeyValuePair("foo", "bar"),
				new KeyValuePair("f", "b"));
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(data);

		// WHEN
		replayAll();
		Map<String, Object> params = new HashMap<>(4);
		params.put("a", "eh");
		params.put("foo", "boo");
		String result = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		params.put("foo", "BOO");
		String result2 = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		// THEN
		assertThat("Resolved static placeholders with DAO and parameters", result,
				equalTo("eh != boo != two != b"));
		assertThat("Resolved static placeholders with DAO (cached) and parameters", result2,
				equalTo("eh != BOO != two != b"));
	}

	@Test
	public void resolveWithParametersArgument_cached_async() throws Exception {
		// GIVEN
		service.setCacheSeconds(Integer.MAX_VALUE);
		service.setTaskExecutor(new TaskExecutorAdapter(executor));
		List<KeyValuePair> data = Arrays.asList(new KeyValuePair("foo", "bar"),
				new KeyValuePair("f", "b"));
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(data);

		// WHEN
		replayAll();
		Map<String, Object> params = new HashMap<>(4);
		params.put("a", "eh");
		params.put("foo", "boo");
		String result = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		params.put("foo", "BOO");
		String result2 = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);

		// THEN
		assertThat("Resolved static placeholders with DAO and parameters", result,
				equalTo("eh != boo != two != b"));
		assertThat("Resolved static placeholders with DAO (cached) and parameters", result2,
				equalTo("eh != BOO != two != b"));
	}

	@Test
	public void resolveWithParametersArgument_cached_expired() throws InterruptedException {
		// GIVEN
		service.setCacheSeconds(1);
		List<KeyValuePair> data1 = Arrays.asList(new KeyValuePair("foo", "bar"),
				new KeyValuePair("f", "b"));
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(data1);

		List<KeyValuePair> data2 = Arrays.asList(new KeyValuePair("foo", "BAR"),
				new KeyValuePair("f", "B"));
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(data2);

		// WHEN
		replayAll();
		Map<String, Object> params = new HashMap<>(4);
		params.put("a", "eh");
		params.put("foo", "boo");
		String result = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		params.put("foo", "BOO");
		String result2 = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		// let cache expire
		Thread.sleep(1050);

		params.put("foo", "BOO!");
		String result3 = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		params.put("foo", "BOO!!");
		String result4 = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		// THEN
		assertThat("Resolved static placeholders with DAO and parameters", result,
				equalTo("eh != boo != two != b"));
		assertThat("Resolved static placeholders with DAO (cached) and parameters", result2,
				equalTo("eh != BOO != two != b"));
		assertThat("Resolved static placeholders with DAO (cache refreshed) and parameters", result3,
				equalTo("eh != BOO! != two != B"));
		assertThat("Resolved static placeholders with DAO (cache refreshed cached again) and parameters",
				result4, equalTo("eh != BOO!! != two != B"));
	}

	@Test
	public void resolveWithParametersArgument_cached_expired_async() throws InterruptedException {
		// GIVEN
		service.setTaskExecutor(new TaskExecutorAdapter(executor));
		service.setCacheSeconds(2);
		List<KeyValuePair> data1 = Arrays.asList(new KeyValuePair("foo", "bar"),
				new KeyValuePair("f", "b"));
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(data1);

		List<KeyValuePair> data2 = Arrays.asList(new KeyValuePair("foo", "BAR"),
				new KeyValuePair("f", "B"));
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY)).andReturn(data2);

		// WHEN
		replayAll();
		Map<String, Object> params = new HashMap<>(4);
		params.put("a", "eh");
		params.put("foo", "boo");
		String result = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		params.put("foo", "BOO");
		String result2 = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		// let cache expire
		Thread.sleep(2050);

		// invoke now, but expired data should be returned while cache refreshed
		params.put("foo", "BOO!");
		String result3 = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		// give async refresh time to complete
		Thread.sleep(1000);

		params.put("foo", "BOO!!");
		String result4 = service.resolvePlaceholders("{a} != {foo} != {b} != {f}", params);

		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);

		// THEN
		assertThat("Resolved static placeholders with DAO and parameters", result,
				equalTo("eh != boo != two != b"));
		assertThat("Resolved static placeholders with DAO (cached) and parameters", result2,
				equalTo("eh != BOO != two != b"));
		assertThat("Resolved static placeholders with DAO (cache refreshing async) and parameters",
				result3, equalTo("eh != BOO! != two != b"));
		assertThat("Resolved static placeholders with DAO (cache refreshed cached again) and parameters",
				result4, equalTo("eh != BOO!! != two != B"));
	}

	@Test
	public void resolveWithParametersArgument_cached_expired_async_slowDao() throws Exception {
		// GIVEN
		service.setTaskExecutor(new TaskExecutorAdapter(executor));
		service.setCacheSeconds(1);

		final AtomicInteger count = new AtomicInteger();
		expect(settingDao.getSettingValues(SettingsPlaceholderService.SETTING_KEY))
				.andAnswer(new IAnswer<List<KeyValuePair>>() {

					@Override
					public List<KeyValuePair> answer() throws Throwable {
						Thread.sleep(200);
						int i = count.incrementAndGet();
						log.info("Returning setting placeholders version {}", i);
				// @formatter:off
						return Arrays.asList(
								new KeyValuePair("a", String.format("a-%d", i)),
								new KeyValuePair("f", String.format("f-%d", i)));
						// @formatter:on
					}
				}).anyTimes();

		// WHEN
		replayAll();

		final int numThreads = 8;
		final long runMs = TimeUnit.SECONDS.toMillis(10);
		final long endDate = System.currentTimeMillis() + runMs;
		final AtomicInteger threadCount = new AtomicInteger();
		final Map<String, Object> params = new HashMap<>(4);

		ExecutorService tp = Executors.newWorkStealingPool();
		List<Future<?>> taskFutures = new ArrayList<>();
		for ( int i = 0; i < numThreads; i++ ) {
			taskFutures.add(tp.submit(new Runnable() {

				private final int idx = threadCount.incrementAndGet();

				@Override
				public void run() {
					log.info("Thread {} starting...", idx);
					while ( System.currentTimeMillis() < endDate ) {
						String result = service.resolvePlaceholders("{a}", params);
						assertThat("{a} should resolve to DAO value", result, startsWith("a-"));
					}
					log.info("Thread {} finished.", idx);
				}
			}));
		}

		// let threads run
		Thread.sleep(runMs);

		tp.shutdown();
		tp.awaitTermination(2, TimeUnit.SECONDS);

		for ( Future<?> f : taskFutures ) {
			f.get();
		}
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
