/* ==================================================================
 * CASettingsServiceTests.java - 17/09/2019 10:15:11 am
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

package net.solarnetwork.node.settings.ca.test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.dao.BasicBatchResult;
import net.solarnetwork.node.dao.BatchableDao.BatchCallback;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.ca.CASettingsService;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.util.CollectionUtils;

/**
 * Test cases for the {@link CASettingsService} class.
 *
 * @author matt
 * @version 1.2
 */
public class CASettingsServiceTests {

	private Path tmpDir;
	private ConfigurationAdmin ca;
	private SettingDao dao;
	private PlatformTransactionManager txManager;
	private CASettingsService service;

	private List<Object> mocks;

	@Before
	public void setup() {
		ca = EasyMock.createMock(ConfigurationAdmin.class);
		dao = EasyMock.createMock(SettingDao.class);
		txManager = EasyMock.createMock(PlatformTransactionManager.class);
		service = new CASettingsService();
		mocks = new ArrayList<>(8);
		mocks.add(ca);
		mocks.add(dao);
		mocks.add(txManager);

		tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		System.setProperty(Constants.SYSTEM_PROP_NODE_HOME, tmpDir.toString());

		service.setBackupDestinationPath(tmpDir.resolve("backups").toString());
		service.setConfigurationAdmin(ca);
		service.setSettingDao(dao);
		service.setTransactionTemplate(new TransactionTemplate(txManager));
	}

	@After
	public void teardown() throws IOException {
		System.clearProperty(Constants.SYSTEM_PROP_NODE_HOME);
		if ( Files.exists(tmpDir) ) {
			Files.walk(tmpDir).sorted(Comparator.reverseOrder()).map(Path::toFile)
					.peek(f -> System.out.println("Cleaning test file " + f)).forEach(File::delete);
		}
		EasyMock.verify(mocks.toArray());
	}

	private void replayAll() {
		EasyMock.replay(mocks.toArray());
	}

	@Test
	public void importResource() throws IOException {
		// GIVEN
		final String handlerKey = UUID.randomUUID().toString();
		final String settingKey = "foobar";
		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		expect(handler.getSettingUid()).andReturn(handlerKey).anyTimes();

		Capture<Iterable<Resource>> resourceCaptor = Capture.newInstance();
		expect(handler.applySettingResources(eq(settingKey), capture(resourceCaptor)))
				.andReturn(new SettingsCommand());

		// WHEN
		replayAll();
		service.onBindHandler(handler, null);

		UrlResource r = new UrlResource(getClass().getResource("test-resource-01.txt"));
		service.importSettingResources(handlerKey, null, settingKey, singleton(r));

		// THEN
		Path expectedResourcePath = tmpDir
				.resolve(Paths.get(SettingsService.DEFAULT_SETTING_RESOURCE_DIR, handlerKey, settingKey,
						"test-resource-01.txt"));
		assertThat("Resource path exists within subdirectory for handler ID",
				Files.exists(expectedResourcePath), equalTo(true));

		List<Resource> appliedResources = stream(resourceCaptor.getValue().spliterator(), false)
				.collect(toList());
		assertThat("Applied resource same as imported", appliedResources, hasSize(1));
		assertThat("Applied resource has expexcted path", appliedResources.get(0).getFile(),
				equalTo(expectedResourcePath.toFile()));
	}

	@Test
	public void removeResource() throws IOException {
		// GIVEN
		final String handlerKey = UUID.randomUUID().toString();
		final String settingKey = "foobar";
		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		expect(handler.getSettingUid()).andReturn(handlerKey).anyTimes();

		Capture<Iterable<Resource>> resourceCaptor = Capture.newInstance();
		expect(handler.applySettingResources(eq(settingKey), capture(resourceCaptor)))
				.andReturn(new SettingsCommand());

		// WHEN
		replayAll();
		service.onBindHandler(handler, null);

		UrlResource r = new UrlResource(getClass().getResource("test-resource-01.txt"));
		service.importSettingResources(handlerKey, null, settingKey, singleton(r));
		service.removeSettingResources(handlerKey, null, settingKey, singleton(r));

		// THEN
		Path expectedResourcePath = tmpDir
				.resolve(Paths.get(SettingsService.DEFAULT_SETTING_RESOURCE_DIR, handlerKey, settingKey,
						"test-resource-01.txt"));
		assertThat("Resource path no longer exists", Files.exists(expectedResourcePath), equalTo(false));

		List<Resource> appliedResources = stream(resourceCaptor.getValue().spliterator(), false)
				.collect(toList());
		assertThat("Applied resource same as imported", appliedResources, hasSize(1));
		assertThat("Applied resource has expexcted path", appliedResources.get(0).getFile(),
				equalTo(expectedResourcePath.toFile()));
	}

	@Test
	public void importResourceWithUpdatesToOtherProvider() throws IOException {
		// GIVEN
		final String handlerKey = UUID.randomUUID().toString();
		final String settingKey = "foobar";
		final String otherProviderKey = UUID.randomUUID().toString();
		final String daoSettingKey1 = handlerKey;
		final String daoSettingKey2 = otherProviderKey;

		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		expect(handler.getSettingUid()).andReturn(handlerKey).anyTimes();

		Configuration config1 = EasyMock.createMock(Configuration.class);
		mocks.add(config1);

		Hashtable<String, Object> configProps1 = new Hashtable<>();
		configProps1.put("foo", "foo");
		configProps1.put("hi", "there");

		expect(ca.getConfiguration(handlerKey, null)).andReturn(config1);
		expect(config1.getFactoryPid()).andReturn(null).anyTimes();
		expect(config1.getPid()).andReturn(handlerKey).anyTimes();
		expect(config1.getProperties()).andReturn(configProps1).anyTimes();

		// delete .* pattern matches first
		expect(dao.deleteSetting(daoSettingKey1)).andReturn(true);

		// then apply SettingValueBean updates
		dao.storeSetting(daoSettingKey1, "foo", "bar");

		// and finally update CA props
		Capture<Dictionary<String, ?>> confUpdateCaptor1 = Capture.newInstance();
		config1.update(capture(confUpdateCaptor1));

		Configuration config2 = EasyMock.createMock(Configuration.class);
		mocks.add(config2);

		Hashtable<String, Object> configProps2 = new Hashtable<>();
		configProps2.put("bim", "bim");
		configProps2.put("hi", "there");

		expect(ca.getConfiguration(otherProviderKey, null)).andReturn(config2);
		expect(config2.getFactoryPid()).andReturn(null).anyTimes();
		expect(config2.getPid()).andReturn(otherProviderKey).anyTimes();
		expect(config2.getProperties()).andReturn(configProps2).anyTimes();

		// delete .* pattern matches first
		expect(dao.deleteSetting(daoSettingKey2)).andReturn(true);

		// then apply SettingValueBean updates
		dao.storeSetting(daoSettingKey2, "bim", "bam");

		// and finally update CA props
		Capture<Dictionary<String, ?>> confUpdateCaptor2 = Capture.newInstance();
		config2.update(capture(confUpdateCaptor2));

		Capture<Iterable<Resource>> resourceCaptor = Capture.newInstance();
		SettingsCommand updates = new SettingsCommand(
				asList(new SettingValueBean("foo", "bar"),
						new SettingValueBean(otherProviderKey, null, "bim", "bam")),
				asList(Pattern.compile(".*")));
		expect(handler.applySettingResources(eq(settingKey), capture(resourceCaptor)))
				.andReturn(updates);

		// WHEN
		replayAll();
		service.onBindHandler(handler, null);

		UrlResource r = new UrlResource(getClass().getResource("test-resource-01.txt"));
		service.importSettingResources(handlerKey, null, settingKey, singleton(r));

		// THEN
		Path expectedResourcePath = tmpDir
				.resolve(Paths.get(SettingsService.DEFAULT_SETTING_RESOURCE_DIR, handlerKey, settingKey,
						"test-resource-01.txt"));
		assertThat("Resource path exists within subdirectory for handler ID",
				Files.exists(expectedResourcePath), equalTo(true));

		List<Resource> appliedResources = stream(resourceCaptor.getValue().spliterator(), false)
				.collect(toList());
		assertThat("Applied resource same as imported", appliedResources, hasSize(1));
		assertThat("Applied resource has expexcted path", appliedResources.get(0).getFile(),
				equalTo(expectedResourcePath.toFile()));

		Dictionary<String, ?> confUpdates1 = confUpdateCaptor1.getValue();
		assertThat("CA conf 1 result size", confUpdates1.size(), equalTo(1));
		assertThat("CA conf 1 result added key", confUpdates1.get("foo"), equalTo("bar"));

		Dictionary<String, ?> confUpdates2 = confUpdateCaptor2.getValue();
		assertThat("CA conf 2 result size", confUpdates2.size(), equalTo(1));
		assertThat("CA conf 2 result added key", confUpdates2.get("bim"), equalTo("bam"));
	}

	@Test
	public void importResourceForFactory() throws IOException {
		// GIVEN
		final String factoryKey = UUID.randomUUID().toString();
		final String instanceKey = String.valueOf((int) (Math.random() * 50 + 1));
		final String instancePid = UUID.randomUUID().toString();
		final String settingKey = "barfoo";

		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		SettingSpecifierProviderFactory factory = EasyMock
				.createMock(SettingSpecifierProviderFactory.class);
		mocks.add(factory);

		Configuration config = EasyMock.createMock(Configuration.class);
		mocks.add(config);

		Hashtable<String, Object> configProps = new Hashtable<>();
		configProps.put(CASettingsService.class.getName() + ".FACTORY_INSTANCE_KEY", instanceKey);

		expect(handler.getSettingUid()).andReturn(factoryKey).anyTimes();
		expect(factory.getFactoryUid()).andReturn(factoryKey).anyTimes();
		expect(dao.getSettingValues(factoryKey + ".FACTORY")).andReturn(emptyList());
		expect(ca.getConfiguration(instancePid, null)).andReturn(config);
		expect(config.getFactoryPid()).andReturn(factoryKey).anyTimes();
		expect(config.getPid()).andReturn(instancePid).anyTimes();
		expect(config.getProperties()).andReturn(configProps).anyTimes();

		Capture<Iterable<Resource>> resourceCaptor = Capture.newInstance();
		expect(handler.applySettingResources(eq(settingKey), capture(resourceCaptor)))
				.andReturn(new SettingsCommand());

		// WHEN
		replayAll();
		service.onBindFactory(factory, emptyMap());

		Hashtable<String, Object> instanceProps = new Hashtable<>(
				singletonMap(org.osgi.framework.Constants.SERVICE_PID, instancePid));
		service.onBindHandler(handler, instanceProps);

		UrlResource r = new UrlResource(getClass().getResource("test-resource-01.txt"));
		service.importSettingResources(factoryKey, instanceKey, settingKey, singleton(r));

		// THEN
		Path expectedResourcePath = tmpDir
				.resolve(Paths.get(SettingsService.DEFAULT_SETTING_RESOURCE_DIR, factoryKey, instanceKey,
						settingKey, "test-resource-01.txt"));
		assertThat("Resource path exists within subdirectory for handler ID",
				Files.exists(expectedResourcePath), equalTo(true));

		List<Resource> appliedResources = stream(resourceCaptor.getValue().spliterator(), false)
				.collect(toList());
		assertThat("Applied resource same as imported", appliedResources, hasSize(1));
		assertThat("Applied resource has expexcted path", appliedResources.get(0).getFile(),
				equalTo(expectedResourcePath.toFile()));
	}

	@Test
	public void importResourceForFactoryWithUpdates() throws IOException, InvalidSyntaxException {
		// GIVEN
		final String factoryKey = UUID.randomUUID().toString();
		final String instanceKey = String.valueOf((int) (Math.random() * 50 + 1));
		final String instancePid = UUID.randomUUID().toString();
		final String settingKey = "barfoo";
		final String daoSettingKey = String.format("%s.%s", factoryKey, instanceKey);

		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		SettingSpecifierProviderFactory factory = EasyMock
				.createMock(SettingSpecifierProviderFactory.class);
		mocks.add(factory);

		Configuration config = EasyMock.createMock(Configuration.class);
		mocks.add(config);

		Hashtable<String, Object> configProps = new Hashtable<>();
		configProps.put(CASettingsService.class.getName() + ".FACTORY_INSTANCE_KEY", instanceKey);
		configProps.put("bim", "bam");
		configProps.put("bazzar", "true");
		configProps.put("bazam", "shazam");
		configProps.put("hi", "there");

		expect(handler.getSettingUid()).andReturn(factoryKey).anyTimes();
		expect(factory.getFactoryUid()).andReturn(factoryKey).anyTimes();
		expect(dao.getSettingValues(factoryKey + ".FACTORY")).andReturn(emptyList());
		expect(ca.getConfiguration(instancePid, null)).andReturn(config);
		expect(config.getFactoryPid()).andReturn(factoryKey).anyTimes();
		expect(config.getPid()).andReturn(instancePid).anyTimes();
		expect(config.getProperties()).andReturn(configProps).anyTimes();

		Capture<Iterable<Resource>> resourceCaptor = Capture.newInstance();
		SettingsCommand updates = new SettingsCommand(
				asList(new SettingValueBean("foo", "bar"), new SettingValueBean("bim", true)),
				asList(Pattern.compile("baz.*")));
		expect(handler.applySettingResources(eq(settingKey), capture(resourceCaptor)))
				.andReturn(updates);

		// after applying resource, apply returned updates
		expect(ca.listConfigurations(
				String.format("(&(service.factoryPid=%s)(%s.FACTORY_INSTANCE_KEY=%s))", factoryKey,
						CASettingsService.class.getName(), instanceKey)))
								.andReturn(new Configuration[] { config });

		// delete baz.* pattern matches first
		expect(dao.deleteSetting(daoSettingKey, "bazzar")).andReturn(true);
		expect(dao.deleteSetting(daoSettingKey, "bazam")).andReturn(true);

		// then apply SettingValueBean updates
		dao.storeSetting(daoSettingKey, "foo", "bar");
		expect(dao.deleteSetting(daoSettingKey, "bim")).andReturn(true);

		// and finally update CA props
		Capture<Dictionary<String, ?>> confUpdateCaptor = Capture.newInstance();
		config.update(capture(confUpdateCaptor));

		// WHEN
		replayAll();
		service.onBindFactory(factory, emptyMap());

		Hashtable<String, Object> instanceProps = new Hashtable<>(
				singletonMap(org.osgi.framework.Constants.SERVICE_PID, instancePid));
		service.onBindHandler(handler, instanceProps);

		UrlResource r = new UrlResource(getClass().getResource("test-resource-01.txt"));
		service.importSettingResources(factoryKey, instanceKey, settingKey, singleton(r));

		// THEN
		Path expectedResourcePath = tmpDir
				.resolve(Paths.get(SettingsService.DEFAULT_SETTING_RESOURCE_DIR, factoryKey, instanceKey,
						settingKey, "test-resource-01.txt"));
		assertThat("Resource path exists within subdirectory for handler ID",
				Files.exists(expectedResourcePath), equalTo(true));

		List<Resource> appliedResources = stream(resourceCaptor.getValue().spliterator(), false)
				.collect(toList());
		assertThat("Applied resource same as imported", appliedResources, hasSize(1));
		assertThat("Applied resource has expexcted path", appliedResources.get(0).getFile(),
				equalTo(expectedResourcePath.toFile()));

		Dictionary<String, ?> confUpdates = confUpdateCaptor.getValue();
		assertThat("CA conf result size", confUpdates.size(), equalTo(3));
		assertThat("CA conf result instance key",
				confUpdates.get(CASettingsService.class.getName() + ".FACTORY_INSTANCE_KEY"),
				equalTo(instanceKey));
		assertThat("CA conf result preserved key", confUpdates.get("hi"), equalTo("there"));
		assertThat("CA conf result added key", confUpdates.get("foo"), equalTo("bar"));
	}

	@Test
	public void backupResourcesWithImportedResource() throws IOException {
		// GIVEN
		final String handlerKey = UUID.randomUUID().toString();
		final String settingKey = "foobar";
		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		expect(handler.getSettingUid()).andReturn(handlerKey).anyTimes();

		Capture<Iterable<Resource>> resourceCaptor = Capture.newInstance();
		expect(handler.applySettingResources(eq(settingKey), capture(resourceCaptor)))
				.andReturn(new SettingsCommand());

		Capture<BatchCallback<Setting>> batchCaptor = Capture.newInstance();
		expect(dao.batchProcess(capture(batchCaptor), EasyMock.anyObject()))
				.andReturn(new BasicBatchResult(0));

		// WHEN
		replayAll();
		service.onBindHandler(handler, null);

		UrlResource r = new UrlResource(getClass().getResource("test-resource-01.txt"));
		service.importSettingResources(handlerKey, null, settingKey, singleton(r));

		Iterable<BackupResource> backupResources = service.getBackupResources();

		// THEN
		Path expectedResourcePath = tmpDir
				.resolve(Paths.get(SettingsService.DEFAULT_SETTING_RESOURCE_DIR, handlerKey, settingKey,
						"test-resource-01.txt"));
		assertThat("Resource path exists within subdirectory for handler ID",
				Files.exists(expectedResourcePath), equalTo(true));

		assertThat("Backup resources available", backupResources, notNullValue());
		List<BackupResource> backupResourceList = StreamSupport
				.stream(backupResources.spliterator(), false).collect(Collectors.toList());
		assertThat("Backup resource list has CSV and setting resource", backupResourceList, hasSize(2));

		BackupResource backupResource = backupResourceList.get(0);
		assertThat("First backup resource is CSV settings", backupResource.getBackupPath(),
				equalTo("settings.csv"));

		backupResource = backupResourceList.get(1);
		assertThat("Second backup resource is setting resource", backupResource.getBackupPath(),
				equalTo(handlerKey + "/foobar/test-resource-01.txt"));
	}

	@Test
	public void importCsv() throws IOException {

		// wrap import in transaction
		TransactionStatus tx = EasyMock.createMock(TransactionStatus.class);
		mocks.add(tx);
		expect(txManager.getTransaction(anyObject())).andReturn(tx);

		expect(tx.isRollbackOnly()).andReturn(false);

		// import 2 settings
		Capture<Setting> settingCaptor = Capture.newInstance(CaptureType.ALL);
		dao.storeSetting(EasyMock.capture(settingCaptor));
		expectLastCall().times(2);

		// delete 1
		expect(dao.deleteSetting("del", "crash")).andReturn(true);

		txManager.commit(tx);

		// handle "bim" CA configurations update ("foo" is skipped)
		Configuration config = EasyMock.createMock(Configuration.class);
		mocks.add(config);

		Hashtable<String, Object> configProps = new Hashtable<>();
		expect(ca.getConfiguration("bim", null)).andReturn(config);
		expect(config.getProperties()).andReturn(configProps);
		Capture<Dictionary<String, ?>> configPropsUpdatesCaptor = Capture.newInstance();
		config.update(capture(configPropsUpdatesCaptor));

		// handle "del" CA configuration update
		Configuration config2 = EasyMock.createMock(Configuration.class);
		mocks.add(config2);

		Hashtable<String, Object> configProps2 = new Hashtable<>();
		configProps2.put("crash", "true");
		expect(ca.getConfiguration("del", null)).andReturn(config2);
		expect(config2.getProperties()).andReturn(configProps2);
		Capture<Dictionary<String, ?>> configPropsUpdatesCaptor2 = Capture.newInstance();
		config2.update(capture(configPropsUpdatesCaptor2));

		// WHEN
		replayAll();
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream("test-settings.csv"), "UTF-8"))) {
			service.importSettingsCSV(r);
		}

		// THEN
		assertThat("Persisted 'foo' and 'bim' setting values", settingCaptor.getValues(), hasSize(2));
		// @formatter:off
		assertThat("Persisted 'foo' setting", settingCaptor.getValues().get(0), allOf(
				hasProperty("key", equalTo("foo")),
				hasProperty("type", equalTo("")),
				hasProperty("value", equalTo("bar"))));
		assertThat("Persisted 'bim' setting", settingCaptor.getValues().get(1), allOf(
				hasProperty("key", equalTo("bim")),
				hasProperty("type", equalTo("bam")),
				hasProperty("value", equalTo("pow"))));
		// @formatter:on
	}

	@Test(expected = IllegalArgumentException.class)
	public void getSettings_nullArgs() {
		// GIVEN

		// WHEN
		replayAll();
		service.getSettings(null, null);
	}

	@Test
	public void getSettings_factoryInstance_none() {
		// GIVEN
		final String factoryId = "f";
		final String instanceId = "1";
		expect(dao.getSettingValues("f.1")).andReturn(Collections.emptyList());

		// WHEN
		replayAll();
		List<Setting> result = service.getSettings(factoryId, instanceId);

		assertThat("Empty results returned from empty DAO result", result, hasSize(0));
	}

	@Test
	public void getSettings_factoryInstance_some() {
		// GIVEN
		final String factoryId = "f";
		final String instanceId = "1";
		// @formatter:off
		final List<KeyValuePair> data = Arrays.asList(new KeyValuePair[] {
				new KeyValuePair("a", "b"),
				new KeyValuePair("c", "d"),
		});
		// @formatter:on
		expect(dao.getSettingValues("f.1")).andReturn(data);

		// WHEN
		replayAll();
		List<Setting> result = service.getSettings(factoryId, instanceId);

		assertThat("Same number of results returned as DAO results", result, hasSize(data.size()));
		for ( int i = 0; i < data.size(); i++ ) {
			KeyValuePair p = data.get(i);
			Setting s = result.get(i);
			assertThat("Setting key is factory instance value", s.getKey(), is("f.1"));
			assertThat("Setting type is data key value", s.getType(), is(p.getKey()));
			assertThat("Setting value is data value value", s.getValue(), is(p.getValue()));
		}
	}

	@Test
	public void getSettings_nonFactoryInstance_none() {
		// GIVEN
		final String instanceId = "c";
		expect(dao.getSettingValues("c")).andReturn(Collections.emptyList());

		// WHEN
		replayAll();
		List<Setting> result = service.getSettings(null, instanceId);

		assertThat("Empty results returned from empty DAO result", result, hasSize(0));
	}

	@Test
	public void getSettings_nonFactoryInstance_some() {
		// GIVEN
		final String instanceId = "c";
		// @formatter:off
		final List<KeyValuePair> data = Arrays.asList(new KeyValuePair[] {
				new KeyValuePair("a", "b"),
				new KeyValuePair("c", "d"),
		});
		// @formatter:on
		expect(dao.getSettingValues("c")).andReturn(data);

		// WHEN
		replayAll();
		List<Setting> result = service.getSettings(null, instanceId);

		assertThat("Same number of results returned as DAO results", result, hasSize(data.size()));
		for ( int i = 0; i < data.size(); i++ ) {
			KeyValuePair p = data.get(i);
			Setting s = result.get(i);
			assertThat("Setting key is instance ID value", s.getKey(), is("c"));
			assertThat("Setting type is data key value", s.getType(), is(p.getKey()));
			assertThat("Setting value is data value value", s.getValue(), is(p.getValue()));
		}
	}

	@Test
	public void processUpdateSetting() throws IOException {
		// GIVEN
		dao.storeSetting(new Setting("foo", "bar", "bam", null));

		Configuration config = EasyMock.createMock(Configuration.class);
		mocks.add(config);

		Hashtable<String, Object> configProps = new Hashtable<>();
		expect(ca.getConfiguration("foo", null)).andReturn(config);
		expect(config.getProperties()).andReturn(configProps).anyTimes();

		Capture<Dictionary<String, ?>> configPropsCaptor = Capture.newInstance();
		config.update(capture(configPropsCaptor));

		// WHEN
		replayAll();
		Map<String, String> params = new LinkedHashMap<>(2);
		params.put("key", "foo");
		params.put("type", "bar");
		params.put("value", "bam");
		Instruction instr = createLocalInstruction(SettingsService.TOPIC_UPDATE_SETTING, params);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
		Map<String, ?> props = CollectionUtils.mapForDictionary(configPropsCaptor.getValue());
		assertThat("Config updated", props, hasEntry("bar", "bam"));
		assertThat("Config updated props", props.keySet(), hasSize(1));
	}

	@Test
	public void processUpdateSetting_noValue() throws IOException {
		// GIVEN
		expect(dao.deleteSetting("foo", "bar")).andReturn(true);

		Configuration config = EasyMock.createMock(Configuration.class);
		mocks.add(config);

		Hashtable<String, Object> configProps = new Hashtable<>();
		expect(ca.getConfiguration("foo", null)).andReturn(config);
		expect(config.getProperties()).andReturn(configProps).anyTimes();

		// WHEN
		replayAll();
		Map<String, String> params = new LinkedHashMap<>(2);
		params.put("key", "foo");
		params.put("type", "bar");
		Instruction instr = createLocalInstruction(SettingsService.TOPIC_UPDATE_SETTING, params);
		InstructionStatus result = service.processInstruction(instr);

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addFactoryInstance_illegalUid() {
		// WHEN
		replayAll();
		service.addProviderFactoryInstance("foo", "! this is not allowed");
	}

	@Test
	public void addFactoryIntance() throws Exception {
		// GIVEN
		final String factoryUid = "fac.tory";
		final String instanceUid = "in/stance";

		// store factory instance setting
		dao.storeSetting(factoryUid + ".FACTORY", instanceUid, instanceUid);

		// look up configuration (not found)
		expect(ca.listConfigurations(
				String.format("(&(service.factoryPid=%s)(%s.FACTORY_INSTANCE_KEY=%s))", factoryUid,
						CASettingsService.class.getName(), instanceUid)))
								.andReturn(new Configuration[0]);

		// create new configuration
		final Configuration config = EasyMock.createMock(Configuration.class);
		mocks.add(config);
		expect(ca.createFactoryConfiguration(factoryUid, null)).andReturn(config);

		Hashtable<String, Object> configProps = new Hashtable<>();
		expect(config.getProperties()).andReturn(configProps);

		// update configuration
		Capture<Hashtable<String, Object>> configCaptor = Capture.newInstance();
		config.update(capture(configCaptor));

		// WHEN
		replayAll();
		String result = service.addProviderFactoryInstance(factoryUid, instanceUid);

		// THEN
		assertThat("Result is given instance ID", result, is(equalTo(instanceUid)));
		assertThat("Config updated", configCaptor.getValue(), is(notNullValue()));
		assertThat("Config update with one value", configCaptor.getValue().keySet(), hasSize(1));
		assertThat("Config update contains entry for instance key", configCaptor.getValue(),
				hasEntry(String.format("%s.FACTORY_INSTANCE_KEY", CASettingsService.class.getName()),
						instanceUid));
	}

}
