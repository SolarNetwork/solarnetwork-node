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

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.core.io.UrlResource;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.ca.CASettingsService;

/**
 * Test cases for the {@link CASettingsService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CASettingsServiceTests {

	private Path tmpDir;
	private ConfigurationAdmin ca;
	private SettingDao dao;
	private CASettingsService service;

	private List<Object> mocks;

	@Before
	public void setup() {
		ca = EasyMock.createMock(ConfigurationAdmin.class);
		dao = EasyMock.createMock(SettingDao.class);
		service = new CASettingsService();
		mocks = new ArrayList<Object>(8);
		mocks.add(ca);
		mocks.add(dao);

		tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
		System.setProperty(Constants.SYSTEM_PROP_NODE_HOME, tmpDir.toString());

		service.setBackupDestinationPath(tmpDir.resolve("backups").toString());
		service.setConfigurationAdmin(ca);
		service.setSettingDao(dao);
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
	public void importResource() {
		// GIVEN
		final String handlerKey = UUID.randomUUID().toString();
		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		expect(handler.getSettingUID()).andReturn(handlerKey).anyTimes();

		// WHEN
		replayAll();
		service.onBindHandler(handler, null);

		UrlResource r = new UrlResource(getClass().getResource("test-resource-01.txt"));
		service.importSettingResources(handlerKey, null, singleton(r));

		// THEN
		Path expectedResourcePath = tmpDir.resolve(Paths
				.get(SettingsService.DEFAULT_SETTING_RESOURCE_DIR, handlerKey, "test-resource-01.txt"));
		assertThat("Resource path exists within subdirectory for handler ID",
				Files.exists(expectedResourcePath), equalTo(true));
	}

	@Test
	public void importResourceForFactory() throws IOException {
		// GIVEN
		final String factoryKey = UUID.randomUUID().toString();
		final String instanceKey = UUID.randomUUID().toString();

		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		SettingSpecifierProviderFactory factory = EasyMock
				.createMock(SettingSpecifierProviderFactory.class);
		mocks.add(factory);

		Configuration config = EasyMock.createMock(Configuration.class);
		mocks.add(config);

		Hashtable<String, Object> configProps = new Hashtable<>();
		configProps.put(CASettingsService.class.getName() + ".FACTORY_INSTANCE_KEY", "1");

		expect(handler.getSettingUID()).andReturn(factoryKey).anyTimes();
		expect(factory.getFactoryUID()).andReturn(factoryKey).anyTimes();
		expect(dao.getSettings(factoryKey + ".FACTORY")).andReturn(emptyList());
		expect(ca.getConfiguration(instanceKey, null)).andReturn(config);
		expect(config.getFactoryPid()).andReturn(factoryKey).anyTimes();
		expect(config.getPid()).andReturn(instanceKey).anyTimes();
		expect(config.getProperties()).andReturn(configProps).anyTimes();

		// WHEN
		replayAll();
		service.onBindFactory(factory, null);

		Hashtable<String, Object> instanceProps = new Hashtable<>(
				singletonMap(org.osgi.framework.Constants.SERVICE_PID, instanceKey));
		service.onBindHandler(handler, instanceProps);

		UrlResource r = new UrlResource(getClass().getResource("test-resource-01.txt"));
		service.importSettingResources(factoryKey, instanceKey, singleton(r));

		// THEN
		Path expectedResourcePath = tmpDir
				.resolve(Paths.get(SettingsService.DEFAULT_SETTING_RESOURCE_DIR, factoryKey, instanceKey,
						"test-resource-01.txt"));
		assertThat("Resource path exists within subdirectory for handler ID",
				Files.exists(expectedResourcePath), equalTo(true));
	}

}
