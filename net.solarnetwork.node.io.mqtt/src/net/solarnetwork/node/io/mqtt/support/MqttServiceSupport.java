/* ==================================================================
 * MqttServiceSupport.java - 16/12/2018 11:16:57 AM
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

package net.solarnetwork.node.io.mqtt.support;

import java.net.URI;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.SSLService;
import net.solarnetwork.util.OptionalService;

/**
 * Helper base class for MQTT client based services.
 * 
 * @author matt
 * @version 1.1
 */
public abstract class MqttServiceSupport implements MqttCallbackExtended {

	/** The default value for the {@code persistencePath} property. */
	public static final String DEFAULT_PERSISTENCE_PATH = "var/mqtt";

	private static final long MAX_CONNECT_DELAY_MS = 120000L;

	private int keepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
	private MessageSource messageSource;

	private final ObjectMapper objectMapper;
	private final TaskScheduler taskScheduler;
	private final OptionalService<SSLService> sslServiceOpt;
	private final AtomicReference<IMqttClient> clientRef;
	private Runnable initTask;
	private MqttConnectOptions connectOptions;

	private String persistencePath = DEFAULT_PERSISTENCE_PATH;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

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
	 * @param reactorService
	 *        the optional reactor service
	 * @param instructionExecutionService
	 *        the instruction execution service
	 */
	public MqttServiceSupport(ObjectMapper objectMapper, TaskScheduler taskScheduler,
			OptionalService<SSLService> sslService) {
		super();
		this.objectMapper = objectMapper;
		this.taskScheduler = taskScheduler;
		this.sslServiceOpt = sslService;
		this.clientRef = new AtomicReference<IMqttClient>();
	}

	/**
	 * Initialize the service after fully configured.
	 * 
	 * <p>
	 * It is safe to call this method multiple times over the life of this
	 * service, e.g. after calling {@link #close()} this method can be called
	 * again to re-connect to the MQTT broker.
	 * </p>
	 */
	public synchronized void init() {
		if ( taskScheduler != null ) {
			if ( initTask != null ) {
				return;
			}
			final AtomicLong sleep = new AtomicLong(2000);
			initTask = new Runnable() {

				@Override
				public void run() {
					try {
						synchronized ( MqttServiceSupport.this ) {
							IMqttClient client = client();
							if ( client != null ) {
								initTask = null;
								return;
							}
						}
					} catch ( RuntimeException e ) {
						// ignore
					}
					long delay = sleep.accumulateAndGet(sleep.get() / 2000, (c, s) -> {
						long d = (s * 2) * 2000;
						if ( d > MAX_CONNECT_DELAY_MS ) {
							d = MAX_CONNECT_DELAY_MS;
						}
						return d;
					});
					log.info("Failed to connect to MQTT server {}, will try again in {}s", getMqttUri(),
							delay / 1000);
					taskScheduler.schedule(this, new Date(System.currentTimeMillis() + delay));
				}
			};
			taskScheduler.schedule(initTask, new Date(System.currentTimeMillis() + sleep.get()));

		} else {
			client();
		}
	}

	/**
	 * Close down the service.
	 * 
	 * <p>
	 * The {@link #init()} method can be called later to re-connect to the MQTT
	 * service.
	 * </p>
	 */
	public synchronized void close() {
		IMqttClient client = clientRef.get();
		if ( client != null ) {
			try {
				if ( this.connectOptions != null ) {
					this.connectOptions.setAutomaticReconnect(false);
				}
				client.disconnect();
			} catch ( MqttException e ) {
				log.warn("Error closing MQTT connection to {}: {}", client.getServerURI(), e.toString());
				try {
					client.disconnectForcibly();
				} catch ( MqttException e2 ) {
					// ignore
				}
			} finally {
				try {
					client.close();
				} catch ( MqttException e ) {
					// ignore
				} finally {
					clientRef.set(null);
				}
			}
		}
	}

	/**
	 * Get the MQTT client ID to connect with.
	 * 
	 * @return the MQTT client ID to use
	 */
	protected abstract String getMqttClientId();

	/**
	 * Get the MQTT broker URI to connect to.
	 * 
	 * <p>
	 * The URI should take the form {@literal mqtt://server:port} or
	 * {@literal mqtts://server:port}.
	 * </p>
	 * 
	 * @return the MQTT borker URI
	 */
	protected abstract URI getMqttUri();

	/**
	 * Test if a MQTT broker URI should use a SSL connection.
	 * 
	 * @param uri
	 *        the URI to test
	 * @return {@literal true} if SSL should be used
	 */
	protected boolean shouldUseSsl(URI uri) {
		int port = uri.getPort();
		String scheme = uri.getScheme();
		return (port == 8883 || "mqtts".equalsIgnoreCase(scheme) || "ssl".equalsIgnoreCase(scheme));
	}

	/**
	 * Create a MQTT connection options instance.
	 * 
	 * <p>
	 * This method sets the following options:
	 * </p>
	 * 
	 * <ul>
	 * <li>{@code cleanSession} to {@literal true}</li>
	 * <li>{@code automaticReconnect} to {@literal true}</li>
	 * <li>{@code socketFactory} to {@link SSLService#getSolarInSocketFactory()}
	 * if the MQTT {@code uri} uses SSL and a {@link SSLService} is
	 * configured</li>
	 * </ul>
	 * 
	 * <p>
	 * Extending classes can override these settings as necessary.
	 * </p>
	 * 
	 * @param uri
	 *        the MQTT broker URI connecting to
	 * @return the MQTT connection options to use
	 */
	protected MqttConnectOptions createMqttConnectOptions(URI uri) {
		MqttConnectOptions connOptions = new MqttConnectOptions();
		connOptions.setCleanSession(true);
		connOptions.setAutomaticReconnect(true);
		connOptions.setKeepAliveInterval(keepAliveInterval);

		final SSLService sslService = (sslServiceOpt != null ? sslServiceOpt.service() : null);
		if ( sslService != null ) {
			boolean useSsl = shouldUseSsl(uri);
			if ( useSsl ) {
				connOptions.setSocketFactory(sslService.getSolarInSocketFactory());
			}
		}

		return connOptions;
	}

	/**
	 * Create the MQTT client persistence settings to use.
	 * 
	 * <p>
	 * This method returns a {@link MqttDefaultFilePersistence} configured with
	 * {@code persistencePath}.
	 * </p>
	 * 
	 * @return the persistence to use
	 */
	protected MqttClientPersistence createMqttClientPersistence() {
		return new MqttDefaultFilePersistence(persistencePath);
	}

	/**
	 * Get the MQTT client, creating it if not already created.
	 * 
	 * <p>
	 * This method will create the client and connect if not already done,
	 * otherwise the existing connected client will be returned.
	 * </p>
	 * 
	 * @return the MQTT client
	 */
	protected synchronized final IMqttClient client() {
		IMqttClient client = clientRef.get();
		if ( client != null ) {
			return client;
		}

		URI uri = getMqttUri();
		if ( uri == null ) {
			log.info("No MQTT URL available");
			return null;
		}

		int port = uri.getPort();
		boolean useSsl = shouldUseSsl(uri);

		final String serverUri = (useSsl ? "ssl" : "tcp") + "://" + uri.getHost()
				+ (port > 0 ? ":" + uri.getPort() : "");

		this.connectOptions = null;
		MqttConnectOptions connOptions = createMqttConnectOptions(uri);

		MqttClientPersistence persistence = createMqttClientPersistence();
		MqttClient c = null;
		try {
			c = new MqttClient(serverUri, getMqttClientId(), persistence);
			c.setCallback(this);
			if ( clientRef.compareAndSet(null, c) ) {
				c.connect(connOptions);
				this.connectOptions = connOptions;
				return c;
			}
		} catch ( MqttException e ) {
			log.warn("Error configuring MQTT client: {}", e.toString());
			if ( c != null ) {
				clientRef.compareAndSet(c, null);
			}
		}
		return null;
	}

	/**
	 * Get the MQTT client.
	 * 
	 * @return the client, or {@literal null} if it has not been created
	 */
	protected final synchronized IMqttClient getClient() {
		return clientRef.get();
	}

	@Override
	public void connectionLost(Throwable cause) {
		IMqttClient client = clientRef.get();
		log.info("Connection to MQTT server @ {} lost: {}",
				(client != null ? client.getServerURI() : "N/A"), cause.getMessage());
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		log.info("{} to MQTT server @ {}", (reconnect ? "Reconnected" : "Connected"), serverURI);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// extending classes can override
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// extending classes can override
	}

	/**
	 * Get the configured {@link ObjectMapper}.
	 * 
	 * @return the mapper
	 */
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * Get the configured task scheduler.
	 * 
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Get the configured optional SSL service.
	 * 
	 * @return the service
	 */
	public OptionalService<SSLService> getSslServiceOpt() {
		return sslServiceOpt;
	}

	/**
	 * Set the path to store persisted MQTT data.
	 * 
	 * <p>
	 * This directory will be created if it does not already exist.
	 * </p>
	 * 
	 * @param persistencePath
	 *        the path to set; defaults to {@link #DEFAULT_PERSISTENCE_PATH}
	 */
	public void setPersistencePath(String persistencePath) {
		this.persistencePath = persistencePath;
	}

	/**
	 * Get the configured {@link MessageSource}.
	 * 
	 * @return the message source, or {@literal null}
	 * @since 1.1
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a {@link MessageSource} to use for resolving localized messages.
	 * 
	 * @param messageSource
	 *        the message source to use
	 * @since 1.1
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the MQTT keep-alive interval, in seconds.
	 * 
	 * @return the keep-alive interval
	 * @since 1.1
	 */
	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	/**
	 * Set a MQTT keep-alive interval, in seconds.
	 * 
	 * <p>
	 * This interval is used by the MQTT client to issue small PING packets.
	 * </p>
	 * 
	 * @param keepAliveInterval
	 *        the interval, in seconds, or {@literal 0} to disable; defaults to
	 *        {@link MqttConnectOptions#KEEP_ALIVE_INTERVAL_DEFAULT}
	 * @since 1.1
	 */
	public void setKeepAliveInterval(int keepAliveInterval) {
		if ( keepAliveInterval < 0 ) {
			return;
		}
		this.keepAliveInterval = keepAliveInterval;
	}

}
