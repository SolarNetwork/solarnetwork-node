/* ==================================================================
 * CannelloniCanbusFrameDecoder.java - 21/11/2019 12:47:35 pm
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

package net.solarnetwork.node.io.canbus.cannelloni;

import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.solarnetwork.node.io.canbus.CanbusFrame;
import net.solarnetwork.node.io.canbus.CanbusFrameFlag;

/**
 * Decode a Cannelloni byte stream into {@link CanbusFrame} instances.
 *
 * @author matt
 * @version 1.0
 */
public class CannelloniCanbusFrameDecoder extends MessageToMessageDecoder<ByteBuf> {

	/** Version 2. */
	public static final byte CANNELLONI_VERSION_2 = (byte) 2;

	private static final int HEADER_LENGTH = 5;

	private static final byte MSB = (byte) 0x80;

	private static enum OpCode {
		DATA,
		ACK,
		NACK
	}

	private static enum DecodeState {
		Cannelloni,
		CanFrame,
	}

	private DecodeState decodeState;
	private int remainingCanFrameCount;

	/**
	 * Constructor.
	 */
	public CannelloniCanbusFrameDecoder() {
		super();
		this.decodeState = DecodeState.Cannelloni;
		this.remainingCanFrameCount = 0;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		Object o = null;
		switch (decodeState) {
			case Cannelloni:
				if ( decodeCannelloni(ctx, in, out) ) {
					if ( remainingCanFrameCount > 0 ) {
						decodeState = DecodeState.CanFrame;
						o = decodeCanFrame(ctx, in, out);
					}
				}
				break;

			default:
				o = decodeCanFrame(ctx, in, out);
		}
		if ( decodeState == DecodeState.CanFrame && remainingCanFrameCount < 1 ) {
			decodeState = DecodeState.Cannelloni;
		}
		if ( o != null ) {
			out.add(o);
		}
	}

	private boolean decodeCannelloni(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
		if ( in.readableBytes() < HEADER_LENGTH ) {
			return false;
		}
		final byte cannelloniVersion = in.getByte(in.readerIndex());
		if ( cannelloniVersion != CANNELLONI_VERSION_2 ) {
			in.skipBytes(HEADER_LENGTH);
			return false;
		}
		final byte opCode = in.getByte(in.readerIndex() + 1);
		if ( opCode != (byte) OpCode.DATA.ordinal() ) {
			in.skipBytes(HEADER_LENGTH);
			return false;
		}

		// UNUSED: final byte seqNum = in.getByte(in.readerIndex() + 2);
		this.remainingCanFrameCount = in.getUnsignedShort(in.readerIndex() + 3);

		in.skipBytes(HEADER_LENGTH);
		return true;
	}

	private Object decodeCanFrame(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
		if ( in.readableBytes() < 4 ) {
			return null;
		}
		final int address = in.getInt(in.readerIndex());
		final byte size = in.getByte(in.readerIndex() + 4);
		byte fdFlags = 0;
		int len = 0;
		int skipLen;
		if ( (size & MSB) == MSB ) {
			// CAN FD frame
			skipLen = 6;
			fdFlags = in.getByte(in.readerIndex() + 5);
			len = (size & 0x7F);
		} else {
			skipLen = 5;
			len = size & 0xF;
		}

		// RTR frame allowed to contain DLC (len) byte; but no data actually included
		final int rtrMask = 1 << CanbusFrameFlag.RemoteTransmissionRequest.bitmaskBitOffset();
		final boolean rtr = (address & rtrMask) == rtrMask;
		if ( rtr ) {
			len = 0;
		}

		if ( in.readableBytes() < (skipLen + len) ) {
			return null;
		}

		in.skipBytes(skipLen);

		final byte[] data;
		if ( rtr ) {
			// preserve any DLC value with RTR
			data = new byte[] { size };
		} else {
			data = new byte[fdFlags > 0 ? len : len];
			in.readBytes(data);
		}
		remainingCanFrameCount--;
		return new BasicCanbusFrame(address, fdFlags, data);
	}

}
