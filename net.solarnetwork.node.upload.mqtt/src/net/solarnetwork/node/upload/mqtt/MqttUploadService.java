/* ==================================================================
 * MqttUploadService.java - 7/06/2018 7:34:41 AM
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

package net.solarnetwork.node.upload.mqtt;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.SSLService;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.util.OptionalService;

/**
 * {@link UploadService} using MQTT.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttUploadService implements UploadService, MqttCallbackExtended {

	/** The JSON MIME type. */
	public static final String JSON_MIME_TYPE = "application/json";

	/** The MQTT topic template for node instruction subscription. */
	public static final String NODE_INSTRUCTION_TOPIC_TEMPLATE = "node/%s/instr";

	/** The MQTT topic template for node data publication. */
	public static final String NODE_DATUM_TOPIC_TEMPLATE = "node/%s/data";

	private final ObjectMapper objectMapper;
	private final IdentityService identityService;
	private final OptionalService<SSLService> sslServiceOpt;
	private final OptionalService<ReactorService> reactorServiceOpt;
	private final AtomicReference<IMqttClient> clientRef;

	private String persistencePath = "var/mqtt";

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param objectMapper
	 *        the object mapper to use
	 * @param identityService
	 *        the identity service
	 * @param sslService
	 *        the optional SSL service
	 * @param reactorService
	 *        the optional reactor service
	 */
	public MqttUploadService(ObjectMapper objectMapper, IdentityService identityService,
			OptionalService<SSLService> sslService, OptionalService<ReactorService> reactorService) {
		super();
		this.objectMapper = objectMapper;
		this.identityService = identityService;
		this.sslServiceOpt = sslService;
		this.reactorServiceOpt = reactorService;
		this.clientRef = new AtomicReference<IMqttClient>();
	}

	@Override
	public String getKey() {
		return "MqttUploadService:" + identityService.getSolarInMqttUrl();
	}

	@Override
	public String uploadDatum(Datum data) {
		final Long nodeId = identityService.getNodeId();
		if ( nodeId != null ) {
			IMqttClient client = client();
			if ( client != null ) {
				String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, identityService.getNodeId());
				try {
					client.publish(topic, objectMapper.writeValueAsBytes(data), 1, false);
					return DigestUtils.md5DigestAsHex(
							String.format("%tQ;%s", data.getCreated(), data.getSourceId()).getBytes());
				} catch ( MqttException | IOException e ) {
					log.warn("Error posting datum {} via MQTT @ {}: {}", data, client.getServerURI(),
							e.getMessage());
				}
			}
		}
		return null;
	}

	private IMqttClient client() {
		IMqttClient client = clientRef.get();
		if ( client != null ) {
			return client;
		}

		final Long nodeId = identityService.getNodeId();
		if ( nodeId == null ) {
			return null;
		}

		URI uri;
		try {
			uri = new URI(identityService.getSolarInMqttUrl());
		} catch ( URISyntaxException e1 ) {
			log.error("Invalid MQTT URL: " + identityService.getSolarInMqttUrl());
			return null;
		}

		int port = uri.getPort();
		String scheme = uri.getScheme();
		boolean useSsl = (port == 8883 || "mqtts".equalsIgnoreCase(scheme)
				|| "ssl".equalsIgnoreCase(scheme));

		final String serverUri = (useSsl ? "ssl" : "tcp") + "://" + uri.getHost()
				+ (port > 0 ? ":" + uri.getPort() : "");

		MqttConnectOptions connOptions = new MqttConnectOptions();
		connOptions.setCleanSession(false);
		connOptions.setAutomaticReconnect(true);

		final SSLService sslService = (sslServiceOpt != null ? sslServiceOpt.service() : null);
		if ( useSsl && sslService != null ) {
			connOptions.setSocketFactory(sslService.getSolarInSocketFactory());
		}

		final String instructionTopic = String.format(NODE_INSTRUCTION_TOPIC_TEMPLATE, nodeId);

		MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(persistencePath);
		MqttClient c = null;
		try {
			c = new MqttClient(serverUri, nodeId.toString(), persistence);
			c.setCallback(this);
			if ( clientRef.compareAndSet(null, c) ) {
				c.connect(connOptions);
				c.subscribe(instructionTopic);
				return c;
			}
		} catch ( MqttException e ) {
			log.warn("Error configuring MQTT client: {}", e.getMessage());
			if ( c != null ) {
				clientRef.compareAndSet(c, null);
			}
		}
		return null;
	}

	@Override
	public void connectionLost(Throwable cause) {
		IMqttClient client = clientRef.get();
		log.info("Connection to MQTT server @ {} lost: {}",
				(client != null ? client.getServerURI() : "N/A"), cause.getMessage());
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// look for and process instructions from message body, as JSON array
		ReactorService reactor = (reactorServiceOpt != null ? reactorServiceOpt.service() : null);
		if ( reactor != null ) {
			JsonNode root = objectMapper.readTree(message.getPayload());
			JsonNode instrArray = root.path("instructions");
			if ( instrArray != null && instrArray.isArray() ) {
				List<InstructionStatus> status = reactor.processInstruction(
						identityService.getSolarInBaseUrl(), instrArray, JSON_MIME_TYPE, null);
				log.debug("Instructions processed: {}", status);
			}
		}

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// nothing to do

	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		log.info("{} to MQTT server @ {}", (reconnect ? "Reconnected" : "Connected"), serverURI);
	}

	/**
	 * Set the path to store persisted MQTT data.
	 * 
	 * <p>
	 * This directory will be created if it does not already exist.
	 * </p>
	 * 
	 * @param persistencePath
	 *        the path to set; defaults to {@literal var/mqtt}
	 */
	public void setPersistencePath(String persistencePath) {
		this.persistencePath = persistencePath;
	}

}
