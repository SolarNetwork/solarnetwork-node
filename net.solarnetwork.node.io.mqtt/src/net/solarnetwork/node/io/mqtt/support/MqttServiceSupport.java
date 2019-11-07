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

import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.support.SSLService;
import net.solarnetwork.util.OptionalService;

/**
 * Helper base class for MQTT client based services.
 * 
 * @author matt
 * @version 1.4
 */
public abstract class MqttServiceSupport
		extends net.solarnetwork.common.mqtt.support.MqttServiceSupport {

	private final ObjectMapper objectMapper;
	private MessageSource messageSource;

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
		super(taskScheduler, sslService);
		this.objectMapper = objectMapper;
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

}
