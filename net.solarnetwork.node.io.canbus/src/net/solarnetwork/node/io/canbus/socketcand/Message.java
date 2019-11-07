/* ==================================================================
 * Message.java - 20/09/2019 7:19:08 am
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

package net.solarnetwork.node.io.canbus.socketcand;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * API for a socketcand message.
 * 
 * @author matt
 * @version 1.0
 */
public interface Message {

	/**
	 * Get the type.
	 * 
	 * <p>
	 * Each message must provide <i>either</i> a non-{@literal null}
	 * {@code type} or a {@code command} value via {@link #getCommand()}.
	 * </p>
	 * 
	 * @return the type, or {@literal null} if not known
	 */
	MessageType getType();

	/**
	 * Get the command.
	 * 
	 * <p>
	 * If this message has a {@code type} then that's command value should be
	 * returned. Otherwise {@code command} will be returned.
	 * </p>
	 * 
	 * @return the command, never {@literal null}
	 */
	String getCommand();

	/**
	 * Get the message arguments.
	 * 
	 * <p>
	 * The returned list can be expected to be unmodifiable.
	 * </p>
	 * 
	 * @return the arguments, or {@literal null}
	 */
	List<String> getArguments();

	/**
	 * Write a complete message to a writer.
	 * 
	 * @param writer
	 *        the destination to write the message to
	 * @throws IOException
	 *         if an IO error occurs
	 */
	void write(Writer out) throws IOException;

	/**
	 * Cast a message to a more specific type.
	 * 
	 * @param <T>
	 *        the type to cast the message to
	 * @param m
	 *        the message to cast
	 * @param clazz
	 *        the class to cast the message to
	 * @return the cast message
	 * @throws ClassCastException
	 *         if {@code m} cannot be cast to {@code clazz}
	 */
	@SuppressWarnings("unchecked")
	static <T extends Message> T typed(Message m, Class<T> clazz) {
		return (T) m;
	}

	/**
	 * Cast this message to a more specific type.
	 * 
	 * @param <T>
	 *        the type to cast the message to
	 * @param clazz
	 *        the class to cast the message to
	 * @return this message, cast to the given type
	 * @throws ClassCastException
	 *         if this message cannot be cast to {@code clazz}
	 */
	default <T extends Message> T asType(Class<T> clazz) {
		return Message.typed(this, clazz);
	}

}
