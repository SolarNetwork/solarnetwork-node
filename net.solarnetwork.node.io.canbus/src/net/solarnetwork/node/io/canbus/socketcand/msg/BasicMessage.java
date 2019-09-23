/* ==================================================================
 * BasicMessage.java - 20/09/2019 7:10:29 am
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

package net.solarnetwork.node.io.canbus.socketcand.msg;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import net.solarnetwork.node.io.canbus.socketcand.Message;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;

/**
 * A socketcand message.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicMessage implements Message {

	private final String command;
	private final MessageType type;
	private final List<String> arguments;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the type
	 */
	public BasicMessage(MessageType type) {
		this(type, type.getCommand(), null);
	}

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the message type, or {@literal null} if not known
	 * @param command
	 *        the raw command, if {@code type} is {@literal null}
	 * @param arguments
	 *        the raw command arguments
	 * @throws IllegalArgumentException
	 *         if both {@code type} and {@code command} are {@literal null}
	 */
	public BasicMessage(MessageType type, String command, List<String> arguments) {
		super();
		if ( type == null && command == null ) {
			throw new IllegalArgumentException(
					"Either a MessageType or command value must be provided.");
		}
		this.command = command;
		this.type = type;
		this.arguments = arguments;
	}

	@Override
	public MessageType getType() {
		return type;
	}

	@Override
	public String getCommand() {
		if ( type != null ) {
			return type.getCommand();
		}
		return command;
	}

	@Override
	public List<String> getArguments() {
		return arguments;
	}

	@Override
	public int hashCode() {
		return Objects.hash(arguments, command, type);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof BasicMessage) ) {
			return false;
		}
		BasicMessage other = (BasicMessage) obj;
		return Objects.equals(arguments, other.arguments) && Objects.equals(command, other.command)
				&& type == other.type;
	}

	@Override
	public void write(Writer out) throws IOException {
		out.write('<');
		out.write(' ');
		out.write(getCommand());

		if ( arguments != null && !arguments.isEmpty() ) {
			for ( String arg : arguments ) {
				out.write(' ');
				out.write(arg);
			}
		}

		out.write(' ');
		out.write('>');
	}

}
