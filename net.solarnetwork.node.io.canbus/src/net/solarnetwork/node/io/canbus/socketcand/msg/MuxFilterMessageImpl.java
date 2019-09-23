/* ==================================================================
 * MuxFilterMessageImpl.java - 23/09/2019 2:25:40 pm
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
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.MuxFilterMessage;
import net.solarnetwork.node.io.canbus.socketcand.SocketcandUtils;

/**
 * Implementation of {@link MuxFilterMessage}.
 * 
 * @author matt
 * @version 1.0
 */
public class MuxFilterMessageImpl extends AddressedDataMessage implements MuxFilterMessage {

	private static final int SECONDS_OFFSET = 0;
	private static final int MICROSECONDS_OFFSET = 1;
	private static final int ADDRESS_OFFSET = 2;
	private static final int DATA_OFFSET = 4;

	private final int seconds;
	private final int microseconds;
	private final long multiplexIdentifierBitmask;
	private final List<Long> multiplexDataFilters;

	/**
	 * Constructor.
	 * 
	 * @param arguments
	 *        the raw command arguments
	 * @throws IllegalArgumentException
	 *         if the arguments are inappropriate for a muxfilter message
	 */
	public MuxFilterMessageImpl(List<String> arguments) {
		super(MessageType.Muxfilter, null, arguments, ADDRESS_OFFSET, DATA_OFFSET);
		try {
			this.seconds = Integer.parseInt(arguments.get(SECONDS_OFFSET));
			this.microseconds = Integer.parseInt(arguments.get(MICROSECONDS_OFFSET));
			byte[] data = getData();
			this.multiplexIdentifierBitmask = SocketcandUtils.longForBytes(data, 0);
			this.multiplexDataFilters = new ArrayList<>(
					Math.max(0, (data.length - Long.BYTES) / Long.BYTES));
			for ( int i = Long.BYTES; i < data.length; i += Long.BYTES ) {
				this.multiplexDataFilters.add(SocketcandUtils.longForBytes(data, i));
			}
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
	 * @param multiplexIdentifierBitmask
	 *        the multiplex identifier bitmask
	 * @param multiplexDataFilters
	 *        the data filters
	 * @throws IllegalArgumentException
	 *         if the arguments are inappropriate for a muxfilter message
	 */
	public MuxFilterMessageImpl(int address, boolean forceExtendedAddress, int limitSeconds,
			int limitMicroseconds, long multiplexIdentifierBitmask, List<Long> multiplexDataFilters) {
		super(MessageType.Muxfilter, null,
				generateArguments(address, forceExtendedAddress, limitSeconds, limitMicroseconds,
						multiplexIdentifierBitmask, multiplexDataFilters),
				ADDRESS_OFFSET, DATA_OFFSET, forceExtendedAddress);
		if ( multiplexIdentifierBitmask == CanbusConnection.DATA_FILTER_NONE ) {
			throw new IllegalArgumentException("The multiplex identifier bitmask must be provided.");
		}
		if ( multiplexDataFilters == null || multiplexDataFilters.isEmpty() ) {
			throw new IllegalArgumentException("At least one multiplex data filter must be provided.");
		}
		this.seconds = limitSeconds;
		this.microseconds = limitMicroseconds;
		this.multiplexIdentifierBitmask = multiplexIdentifierBitmask;
		this.multiplexDataFilters = multiplexDataFilters;
	}

	private static List<String> generateArguments(int address, boolean forceExtendedAddress,
			int limitSeconds, int limitMicroseconds, long multiplexIdentifierBitmask,
			List<Long> multiplexDataFilters) {
		List<String> args = new ArrayList<>(5 + multiplexDataFilters.size() * 8);
		args.add(String.valueOf(limitSeconds));
		args.add(String.valueOf(limitMicroseconds));
		args.add(Addressed.hexAddress(address, forceExtendedAddress));
		args.add(String.valueOf(1 + multiplexDataFilters.size()));
		args.addAll(SocketcandUtils.encodeAsHexStrings(multiplexIdentifierBitmask, false));
		for ( Long f : multiplexDataFilters ) {
			args.addAll(SocketcandUtils.encodeAsHexStrings(f, false));
		}
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
	public long getMultiplexIdentifierBitmask() {
		return multiplexIdentifierBitmask;
	}

	@Override
	public Iterable<Long> getMultiplexDataFilters() {
		return multiplexDataFilters;
	}

}
