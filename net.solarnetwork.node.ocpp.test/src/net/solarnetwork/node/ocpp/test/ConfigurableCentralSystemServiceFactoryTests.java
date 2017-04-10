/* ==================================================================
 * ConfigurableCentralSystemServiceFactoryTests.java - 4/04/2017 11:04:25 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.test;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.ws.BindingProvider;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.ocpp.impl.ConfigurableCentralSystemServiceFactory;
import net.solarnetwork.support.SSLService;
import net.solarnetwork.util.StaticOptionalService;
import ocpp.v15.cs.CentralSystemService;

/**
 * Test cases for the {@link ConfigurableCentralSystemServiceFactory} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ConfigurableCentralSystemServiceFactoryTests {

	private SSLService sslService;

	private ConfigurableCentralSystemServiceFactory factory;

	@Before
	public void setup() {
		sslService = EasyMock.createMock(SSLService.class);
		factory = new ConfigurableCentralSystemServiceFactory();
		factory.setSslService(new StaticOptionalService<SSLService>(sslService));
	}

	private void replayAll() {
		EasyMock.replay(sslService);
	}

	@After
	public void verifyAll() {
		EasyMock.verify(sslService);
	}

	@Test
	public void setupWithSSLService() {
		SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		EasyMock.expect(sslService.getSSLSocketFactory()).andReturn(socketFactory);

		replayAll();

		CentralSystemService client = factory.service();
		Assert.assertNotNull("CentralSystemService created", client);

		Object o = ((BindingProvider) client).getRequestContext().get(
				ConfigurableCentralSystemServiceFactory.DEFAULT_SSL_SOCKET_FACTORY_REQUEST_CONTEXT_KEY);
		Assert.assertSame("Configured SSLSocketFactory", socketFactory, o);
	}

}
