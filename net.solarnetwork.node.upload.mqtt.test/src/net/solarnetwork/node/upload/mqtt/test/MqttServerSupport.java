/* ==================================================================
 * MqttServerSupport.java - 8/06/2018 2:20:24 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.mqtt.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;

/**
 * Support for MQTT client integration with an embedded MQTT server.
 * 
 * <p>
 * Unit tests can extend this class directly.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class MqttServerSupport {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected Server mqttServer;
	protected MqttClient mqttClient;
	private Properties mqttServerProperties;
	private TestingInterceptHandler testingHandler;

	protected static final int getFreePort() {
		try (ServerSocket ss = new ServerSocket(0)) {
			ss.setReuseAddress(true);
			return ss.getLocalPort();
		} catch ( IOException e ) {
			throw new RuntimeException("Unable to find unused port");
		}
	}

	protected Properties createMqttServerProperties() {
		int port = getFreePort();
		Properties p = new Properties();
		p.setProperty(BrokerConstants.PORT_PROPERTY_NAME, String.valueOf(port));
		p.setProperty(BrokerConstants.HOST_PROPERTY_NAME, "127.0.0.1");
		return p;
	}

	public void setupMqttServer(List<InterceptHandler> handlers, IAuthenticator authenticator,
			IAuthorizator authorizator) {
		testingHandler = null;
		if ( handlers == null ) {
			testingHandler = new TestingInterceptHandler();
			handlers = Collections.singletonList(testingHandler);
		}
		Server s = new Server();
		try {
			Properties p = createMqttServerProperties();
			log.debug("Starting MQTT server with props {}", p);
			s.startServer(new MemoryConfig(p), handlers, null, authenticator, authorizator);
			mqttServer = s;
			mqttServerProperties = p;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	public void setupMqttServer() {
		setupMqttServer(null, null, null);
	}

	/**
	 * Get the port the MQTT server is listing on.
	 * 
	 * @return the port
	 */
	public int getMqttServerPort() {
		if ( mqttServerProperties == null ) {
			return 1883;
		}
		String port = mqttServerProperties.getProperty(BrokerConstants.PORT_PROPERTY_NAME, "1883");
		return Integer.parseInt(port);
	}

	/**
	 * Get the default testing handler, if no specific handlers have been passed
	 * to {@link #setupMqttServer(List, IAuthenticator, IAuthorizator)}.
	 * 
	 * @return the testing handler
	 */
	public TestingInterceptHandler getTestingInterceptHandler() {
		return testingHandler;
	}

	/**
	 * Shut down the embedded MQTT server.
	 */
	public void stopMqttServer() {
		if ( mqttServer != null ) {
			mqttServer.stopServer();
			mqttServer = null;
			mqttServerProperties = null;
		}
	}

	public Server getServer() {
		return mqttServer;
	}

	public IMqttClient getClient() {
		return mqttClient;
	}

	public void setupMqttClient(String clientId, MqttCallback callback) {
		if ( mqttClient != null ) {
			try {
				mqttClient.close(true);
			} catch ( MqttException e ) {
				log.info("Exception closing MQTT client: {}", e.getMessage());
			}
		}
		int port = getMqttServerPort();
		try {
			MemoryPersistence persistence = new MemoryPersistence();
			MqttClient client = new MqttClient("tcp://127.0.0.1:" + port, clientId, persistence);
			client.setCallback(callback);
			MqttConnectOptions connOptions = new MqttConnectOptions();
			connOptions.setCleanSession(false);
			connOptions.setAutomaticReconnect(false);
			client.connect(connOptions);
			mqttClient = client;
		} catch ( MqttException e ) {
			throw new RuntimeException(e);
		}
	}

	@After
	public void teardown() {
		stopMqttServer();
	}

}
