/* ==================================================================
 * FactoryHelperTests.java - 17/09/2019 3:16:54 pm
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.ca.FactoryHelper;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;

/**
 * Test cases for the {@link FactoryHelper} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FactoryHelperTests {

	private SettingSpecifierProviderFactory factory;
	private Map<String, Object> factoryProperties;
	private Map<String, SettingSpecifierProvider> providers;
	private Map<String, SettingResourceHandler> handlers;
	private FactoryHelper helper;

	private List<Object> mocks;

	@Before
	public void setup() {
		mocks = new ArrayList<>(8);
		factory = EasyMock.createMock(SettingSpecifierProviderFactory.class);
		mocks.add(factory);
		factoryProperties = new LinkedHashMap<>();
		providers = new TreeMap<>();
		handlers = new TreeMap<>();
		helper = new FactoryHelper(factory, factoryProperties, providers, handlers);
	}

	@After
	public void teardown() {
		EasyMock.verify(mocks.toArray());
	}

	private void replayAll() {
		EasyMock.replay(mocks.toArray());
	}

	@Test
	public void addProvider() {
		// GIVEN
		SettingSpecifierProvider provider = EasyMock.createMock(SettingSpecifierProvider.class);
		mocks.add(provider);

		// WHEN
		replayAll();
		final String instanceKey = UUID.randomUUID().toString();
		helper.addProvider(instanceKey, provider);

		// THEN
		assertThat("Provider populated in instance map", providers, hasEntry(instanceKey, provider));
	}

	@Test
	public void removeProvider() {
		// GIVEN
		SettingSpecifierProvider provider = EasyMock.createMock(SettingSpecifierProvider.class);
		mocks.add(provider);

		final String instanceKey = UUID.randomUUID().toString();
		providers.put(instanceKey, provider);

		// WHEN
		replayAll();
		helper.removeProvider(provider);

		// THEN
		assertThat("Provider removed from instance map", providers.keySet(), hasSize(0));
	}

	@Test
	public void allProviders() {
		// GIVEN
		final String instanceKey1 = "a";
		SettingSpecifierProvider provider1 = EasyMock.createMock(SettingSpecifierProvider.class);
		mocks.add(provider1);
		providers.put(instanceKey1, provider1);

		final String instanceKey2 = "b";
		SettingSpecifierProvider provider2 = EasyMock.createMock(SettingSpecifierProvider.class);
		mocks.add(provider2);
		providers.put(instanceKey2, provider2);

		// WHEN
		replayAll();
		Iterable<Map.Entry<String, SettingSpecifierProvider>> providers = helper.instanceEntrySet();

		// THEN
		int i = 0;
		for ( Map.Entry<String, SettingSpecifierProvider> me : providers ) {
			String instanceKey = me.getKey();
			SettingSpecifierProvider provider = me.getValue();
			switch (i) {
				case 0:
					assertThat("Instance key", instanceKey, equalTo(instanceKey1));
					assertThat("Provider", provider, sameInstance(provider1));
					break;

				case 1:
					assertThat("Instance key", instanceKey, equalTo(instanceKey2));
					assertThat("Provider", provider, sameInstance(provider2));
					break;

				default:
					Assert.fail("Unexpected provider.");
			}
			i++;
		}
	}

	@Test
	public void addHandler() {
		// GIVEN
		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		// WHEN
		replayAll();
		final String instanceKey = UUID.randomUUID().toString();
		helper.addHandler(instanceKey, handler);

		// THEN
		assertThat("Handler populated in instance map", handlers, hasEntry(instanceKey, handler));
	}

	@Test
	public void removeHandler() {
		// GIVEN
		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		final String instanceKey = UUID.randomUUID().toString();
		handlers.put(instanceKey, handler);

		// WHEN
		replayAll();
		helper.removeHandler(handler);

		// THEN
		assertThat("Handler removed from instance map", handlers.keySet(), hasSize(0));
	}

	@Test
	public void getHandler() {
		// GIVEN
		SettingResourceHandler handler = EasyMock.createMock(SettingResourceHandler.class);
		mocks.add(handler);

		final String instanceKey = UUID.randomUUID().toString();
		handlers.put(instanceKey, handler);

		// WHEN
		replayAll();
		SettingResourceHandler result = helper.getHandler(instanceKey);

		// THEN
		assertThat("Handler found from instance map", result, sameInstance(handler));
	}
}
