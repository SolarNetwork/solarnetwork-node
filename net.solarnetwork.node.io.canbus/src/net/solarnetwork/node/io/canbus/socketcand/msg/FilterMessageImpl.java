/* ==================================================================
 * FilterMessageImpl.java - 23/09/2019 11:23:25 am
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

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.socketcand.Addressed;
import net.solarnetwork.node.io.canbus.socketcand.FilterMessage;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandUtils;

/**
 * Implementation of {@link FilterMessage}.
 * 
 * @author matt
 * @version 1.0
 */
public class FilterMessageImpl extends AddressedDataMessage implements FilterMessage {

	private static final int SECONDS_OFFSET = 0;
	private static final int MICROSECONDS_OFFSET = 1;
	private static final int ADDRESS_OFFSET = 2;
	private static final int DATA_OFFSET = 4;

	private final int seconds;
	private final int microseconds;
	private final long dataFilter;

	/**
	 * Constructor.
	 * 
	 * @param arguments
	 *        the raw command arguments
	 * @throws IllegalArgumentException
	 *         if the arguments are inappropriate for a filter message
	 */
	public FilterMessageImpl(List<String> arguments) {
		super(MessageType.Filter, null, arguments, ADDRESS_OFFSET, DATA_OFFSET);
		try {
			this.seconds = Integer.parseInt(arguments.get(SECONDS_OFFSET));
			this.microseconds = Integer.parseInt(arguments.get(MICROSECONDS_OFFSET));
			this.dataFilter = SocketcandUtils.longForBytes(getData(), 0);
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException("The seconds [" + arguments.get(SECONDS_OFFSET)
					+ "] and/or microseconds [" + arguments.get(MICROSECONDS_OFFSET)
					+ "] arguments could not be parsed as numbers.", e);
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param address
	 *        the address to send the message to
	 * @param forceExtendedAddress
	 *        {@literal true} to force {@code address} to be treated as an
	 *        extended address, even it if would otherwise fit
	 * @param limitSeconds
	 *        the limit seconds
	 * @param limitMicroseconds
	 *        the limit microseconds
	 * @param dataFilter
	 *        the data filter
	 * @throws IllegalArgumentException
	 *         if the arguments are inappropriate for a filter message
	 */
	public FilterMessageImpl(int address, boolean forceExtendedAddress, int limitSeconds,
			int limitMicroseconds, long dataFilter) {
		super(MessageType.Filter, null, generateArguments(address, forceExtendedAddress, limitSeconds,
				limitMicroseconds, dataFilter), ADDRESS_OFFSET, DATA_OFFSET, forceExtendedAddress);
		if ( dataFilter == CanbusConnection.DATA_FILTER_NONE ) {
			throw new IllegalArgumentException("The data filter must be provided.");
		}
		this.seconds = limitSeconds;
		this.microseconds = limitMicroseconds;
		this.dataFilter = dataFilter;
	}

	private static List<String> generateArguments(int address, boolean forceExtendedAddress,
			int limitSeconds, int limitMicroseconds, long dataFilter) {
		List<String> args = new ArrayList<>(12);
		args.add(String.valueOf(limitSeconds));
		args.add(String.valueOf(limitMicroseconds));
		args.add(Addressed.hexAddress(address, forceExtendedAddress));

		List<String> f = SocketcandUtils.encodeAsHexStrings(dataFilter, true);
		args.add(String.valueOf(f.size()));
		args.addAll(f);

		return args;
	}

	@Override
	public int getSeconds() {
		return seconds;
	}

	@Override
	public int getMicroseconds() {
		return microseconds;
	}

	@Override
	public long getDataFilter() {
		return dataFilter;
	}

}
