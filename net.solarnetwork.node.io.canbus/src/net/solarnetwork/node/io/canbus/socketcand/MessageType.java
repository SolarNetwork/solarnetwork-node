/* ==================================================================
 * MessageType.java - 20/09/2019 6:33:17 am
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

/**
 * Enumeration of socketcand message types.
 *
 * @author matt
 * @version 1.0
 */
public enum MessageType {

	/** The {@code hi} message. */
	Hi("hi"),

	/** The {@code open} message. */
	Open("open"),

	/** The {@code ok} message. */
	Ok("ok"),

	/** The {@code error} message. */
	Error("error"),

	/** The {@code add} message. */
	Add("add"),

	/** The {@code update} message. */
	Update("update"),

	/** The {@code delete} message. */
	Delete("delete"),

	/** The {@code send} message. */
	Send("send"),

	/** The {@code filter} message. */
	Filter("filter"),

	/** The {@code muxfilter} message. */
	Muxfilter("muxfilter"),

	/** The {@code subscribe} message. */
	Subscribe("subscribe"),

	/** The {@code unsubscribe} message. */
	Unsubscribe("unsubscribe"),

	/** The {@code echo} message. */
	Echo("echo"),

	/** The {@code rawmode} message. */
	Rawmode("rawmode"),

	/** The {@code frame} message. */
	Frame("frame"),

	/** The {@code bcmmode} message. */
	Bcmmode("bcmmode"),

	/** The {@code statistics} message. */
	Statistics("statistics"),

	/** The {@code stat} message. */
	Stat("stat");

	private final String command;

	private MessageType(String command) {
		this.command = command;
	}

	/**
	 * Get the socketcand command literal for this message.
	 *
	 * @return the command literal
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Get a type for a given command.
	 *
	 * @param command
	 *        the command to get the type for
	 * @return the type, or {@literal null} if the command is not a known type
	 */
	public static MessageType forCommand(String command) {
		if ( command == null ) {
			return null;
		}
		switch (command) {
			case "frame":
				return Frame;

			case "open":
				return Open;

			case "ok":
				return Ok;

			case "error":
				return Error;

			case "hi":
				return Hi;

			case "add":
				return Add;

			case "update":
				return Update;

			case "delete":
				return Delete;

			case "send":
				return Send;

			case "filter":
				return Filter;

			case "muxfilter":
				return Muxfilter;

			case "subscribe":
				return Subscribe;

			case "unsubscribe":
				return Unsubscribe;

			case "echo":
				return Echo;

			case "rawmode":
				return Rawmode;

			case "bcmmode":
				return Bcmmode;

			case "statistics":
				return Statistics;

			case "stat":
				return Stat;
		}

		return null;
	}

}
