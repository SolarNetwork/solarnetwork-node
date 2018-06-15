/* ==================================================================
 * TestingMqttCallback.java - 9/06/2018 7:38:42 AM
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

import static java.util.Collections.synchronizedList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * A {@link MqttCallbackExtended} for unit testing.
 * 
 * @author matt
 * @version 1.0
 */
public class TestingMqttCallback implements MqttCallbackExtended {

	public final List<Throwable> connectionLostExceptions = synchronizedList(new ArrayList<>(8));
	public final ConcurrentMap<String, List<MqttMessage>> arrivedMessages = new ConcurrentHashMap<>(8);
	public final List<IMqttDeliveryToken> deliveryCompleteTokens = synchronizedList(new ArrayList<>(8));
	public final List<Boolean> connectCompleteReconnectFlags = synchronizedList(new ArrayList<>(8));
	public final List<String> connectCompleteUris = synchronizedList(new ArrayList<>(8));

	@Override
	public void connectionLost(Throwable cause) {
		connectionLostExceptions.add(cause);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		arrivedMessages.computeIfAbsent(topic, (k) -> synchronizedList(new ArrayList<>(8))).add(message);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		deliveryCompleteTokens.add(token);
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		connectCompleteReconnectFlags.add(reconnect);
		connectCompleteUris.add(serverURI);
	}

}
