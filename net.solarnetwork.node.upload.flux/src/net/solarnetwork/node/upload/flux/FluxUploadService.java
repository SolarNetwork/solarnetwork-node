/* ==================================================================
 * FluxUploadService.java - 16/12/2018 9:18:57 AM
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

package net.solarnetwork.node.upload.flux;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.SSLService;
import net.solarnetwork.node.io.mqtt.support.MqttServiceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.OptionalService;

/**
 * Service to listen to datum events and upload datum to SolarFlux.
 * 
 * @author matt
 * @version 1.0
 */
public class FluxUploadService extends MqttServiceSupport
		implements EventHandler, SettingSpecifierProvider {

	/** The MQTT topic template for node data publication. */
	public static final String NODE_DATUM_TOPIC_TEMPLATE = "node/%d/datum/0/%s";

	/** The default value for the {@code persistencePath} property. */
	public static final String DEFAULT_PERSISTENCE_PATH = "var/flux";

	/** The default value for the {@code mqttHost} property. */
	public static final String DEFAULT_MQTT_HOST = "mqtts://influx.solarnetwork.net:8884";

	/** The default value for the {@code mqttUsername} property. */
	public static final String DEFAULT_MQTT_USERNAME = "solarnode";

	private final IdentityService identityService;
	private MessageSource messageSource;
	private String mqttHost = DEFAULT_MQTT_HOST;
	private String mqttUsername = DEFAULT_MQTT_USERNAME;
	private String mqttPassword = null;
	private TaskExecutor taskExecutor;

	/**
	 * Constructor.
	 * 
	 * @param objectMapper
	 *        the object mapper to use
	 * @param taskScheduler
	 *        an optional task scheduler to auto-connect with, or
	 *        {@literal null} for no auto-connect support
	 * @param sslService
	 *        the optional SSL service
	 * @param identityService
	 *        the identity service
	 */
	public FluxUploadService(ObjectMapper objectMapper, TaskScheduler taskScheduler,
			OptionalService<SSLService> sslService, IdentityService identityService) {
		super(objectMapper, taskScheduler, sslService);
		setPersistencePath(DEFAULT_PERSISTENCE_PATH);
		this.identityService = identityService;
	}

	@Override
	protected String getMqttClientId() {
		final Long nodeId = identityService.getNodeId();
		return (nodeId != null ? nodeId.toString() : null);
	}

	@Override
	protected MqttConnectOptions createMqttConnectOptions(URI uri) {
		MqttConnectOptions opts = super.createMqttConnectOptions(uri);
		opts.setUserName(mqttUsername);
		if ( mqttPassword != null ) {
			opts.setPassword(mqttPassword.toCharArray());
		}
		return opts;
	}

	@Override
	protected URI getMqttUri() {
		try {
			return new URI(mqttHost);
		} catch ( NullPointerException | URISyntaxException e1 ) {
			log.error("Invalid MQTT URL: " + mqttHost);
			return null;
		}
	}

	@Override
	public void handleEvent(Event event) {
		Map<String, Object> data = mapForEvent(event);
		if ( data == null ) {
			return;
		}
		TaskExecutor executor = this.taskExecutor;
		if ( executor != null ) {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					publishDatum(data);
				}
			});
		} else {
			publishDatum(data);
		}
	}

	private void publishDatum(Map<String, Object> data) {
		final Long nodeId = identityService.getNodeId();
		if ( nodeId == null ) {
			return;
		}
		final Object sourceIdObj = data.get("sourceId");
		if ( sourceIdObj == null ) {
			return;
		}
		String sourceId = sourceIdObj.toString().trim();
		if ( sourceId.startsWith("/") ) {
			sourceId = sourceId.substring(1);
		}
		if ( sourceId.isEmpty() ) {
			return;
		}
		IMqttClient client = getClient();
		ObjectMapper objectMapper = getObjectMapper();
		if ( client != null ) {
			String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, nodeId, sourceId);
			try {
				JsonNode jsonData = objectMapper.valueToTree(data);
				byte[] payload = objectMapper.writeValueAsBytes(jsonData);
				if ( log.isTraceEnabled() ) {
					log.trace("Publishing to MQTT topic {} JSON:\n{}", topic, jsonData);
					log.trace("Publishing to MQTT topic {}\n{}", topic, Hex.encodeHexString(payload));
				}
				client.publish(topic, payload, 0, true);
				log.debug("Published to MQTT topic {}: {}", topic, data);
			} catch ( MqttException | IOException e ) {
				log.warn("Error publishing to MQTT topic {} datum {} @ {}: {}", topic, data,
						client.getServerURI(), e.getMessage());
			}
		}
	}

	private Map<String, Object> mapForEvent(Event event) {
		if ( event == null ) {
			return null;
		}
		String[] propNames = event.getPropertyNames();
		if ( propNames == null || propNames.length < 1 ) {
			return null;
		}
		Map<String, Object> map = new LinkedHashMap<>(propNames.length);
		for ( String propName : propNames ) {
			map.put(propName, event.getProperty(propName));
		}
		return map;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.upload.flux";
	}

	@Override
	public String getDisplayName() {
		return "SolarFlux Upload Service";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(4);
		results.add(new BasicTextFieldSettingSpecifier("mqttHost", DEFAULT_MQTT_HOST));
		results.add(new BasicTextFieldSettingSpecifier("mqttUsername", DEFAULT_MQTT_USERNAME));
		results.add(new BasicTextFieldSettingSpecifier("mqttPassword", "", true));
		return results;
	}

	/**
	 * Set the MQTT host to use.
	 * 
	 * <p>
	 * This accepts a URI syntax, e.g. {@literal mqtts://host:port}.
	 * </p>
	 * 
	 * @param mqttHost
	 *        the MQTT host to use
	 */
	public void setMqttHost(String mqttHost) {
		this.mqttHost = mqttHost;
	}

	/**
	 * Set the MQTT username to use.
	 * 
	 * @param mqttUsername
	 *        the username
	 */
	public void setMqttUsername(String mqttUsername) {
		this.mqttUsername = mqttUsername;
	}

	/**
	 * Set the MQTT password to use.
	 * 
	 * @param mqttPassword
	 *        the password, or {@literal null} for no password
	 */
	public void setMqttPassword(String mqttPassword) {
		this.mqttPassword = mqttPassword;
	}

	/**
	 * Set the message source to use for settings.
	 * 
	 * @param messageSource
	 *        the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Configure an executor for handling datum captured events with.
	 * 
	 * <p>
	 * If this is configured, then the {@link #handleEvent(Event)} will create a
	 * task for each event and pass them to this executor. This helps prevent
	 * blacklisting by the event publisher, and is recommended in production
	 * deployments.
	 * </p>
	 * 
	 * @param taskExecutor
	 *        the task executor to use
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
