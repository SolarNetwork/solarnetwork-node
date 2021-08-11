/* ==================================================================
 * StompSetupServerHandler.java - 4/08/2021 11:28:02 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.stomp;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import io.netty.util.ReferenceCountUtil;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.InstructionUtils;

/**
 * Handle the STOMP protocol setup integration.
 * 
 * @author matt
 * @version 1.0
 */
public class StompSetupServerHandler extends ChannelInboundHandlerAdapter {

	/** The server version. */
	public static final String SERVER_VERSION = "1.0.0";

	private static final Set<String> STOMP_HEADER_NAMES = createStompHeaderNames();

	private final List<FeedbackInstructionHandler> instructionHandlers;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ConcurrentMap<UUID, SetupSession> sessions = new ConcurrentHashMap<>(4, 0.9f, 1);

	// TODO: need cleanup timer to clean expired sessions

	/**
	 * Constructor.
	 * 
	 * @param instructionHandlers
	 *        the handlers
	 */
	public StompSetupServerHandler(List<FeedbackInstructionHandler> instructionHandlers) {
		super();
		if ( instructionHandlers == null ) {
			throw new IllegalArgumentException("The instructionHandlers argument must not be null.");
		}
		this.instructionHandlers = instructionHandlers;
	}

	private static Set<String> createStompHeaderNames() {
		Set<String> result = new LinkedHashSet<>(20);
		result.add(StompHeaders.ACCEPT_VERSION.toString());
		result.add(StompHeaders.HOST.toString());
		result.add(StompHeaders.LOGIN.toString());
		result.add(StompHeaders.PASSCODE.toString());
		result.add(StompHeaders.HEART_BEAT.toString());
		result.add(StompHeaders.VERSION.toString());
		result.add(StompHeaders.SESSION.toString());
		result.add(StompHeaders.SERVER.toString());
		result.add(StompHeaders.DESTINATION.toString());
		result.add(StompHeaders.ID.toString());
		result.add(StompHeaders.ACK.toString());
		result.add(StompHeaders.TRANSACTION.toString());
		result.add(StompHeaders.RECEIPT.toString());
		result.add(StompHeaders.MESSAGE_ID.toString());
		result.add(StompHeaders.SUBSCRIPTION.toString());
		result.add(StompHeaders.RECEIPT_ID.toString());
		result.add(StompHeaders.MESSAGE.toString());
		result.add(StompHeaders.CONTENT_LENGTH.toString());
		result.add(StompHeaders.CONTENT_TYPE.toString());
		return result;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		try {
			StompFrame frame = (msg instanceof StompFrame ? (StompFrame) msg : null);
			if ( frame == null ) {
				return;
			}
			log.debug("Got stomp message: {}", frame);
			final SetupSession session = sessionForChannel(ctx.channel());
			if ( session == null ) {
				setupSession(ctx, frame);
				return;
			}

			switch (frame.command()) {
				case STOMP:
				case CONNECT:
					sendError(ctx, "Already connected.");
					break;

				case SEND:
					executeInstruction(ctx, frame);
					break;

				default:
					sendError(ctx, "Unsupported STOMP command");
					break;

			}
			// TODO: something useful
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	private SetupSession sessionForChannel(Channel channel) {
		return sessions.values().stream().filter(s -> s.getChannel() == channel).findAny().orElse(null);
	}

	private void setupSession(ChannelHandlerContext ctx, StompFrame connectFrame) {
		final Channel channel = ctx.channel();
		final SetupSession s = new SetupSession(channel);
		final UUID sessionId = s.getSessionId();
		sessions.put(s.getSessionId(), s);
		channel.closeFuture().addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) {
				sessions.remove(sessionId);
			}
		});
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.CONNECTED);
		f.headers().set(StompHeaders.VERSION, "1.2");
		f.headers().set(StompHeaders.SERVER, "SolarNode-Setup/" + SERVER_VERSION);
		f.headers().set(StompHeaders.SESSION, s.getSessionId().toString());
		f.headers().set(StompHeaders.MESSAGE, "Please authenticate.");
		ctx.writeAndFlush(f);
	}

	private void executeInstruction(ChannelHandlerContext ctx, StompFrame frame) {
		String topic = frame.headers().getAsString(StompHeaders.DESTINATION);
		if ( !InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic) ) {
			// unsupported topic: ignore
			return;
		}
		BasicInstruction instr = new BasicInstruction(topic, new Date(),
				Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
		StompHeaders headers = frame.headers();
		for ( Iterator<Entry<String, String>> itr = headers.iteratorAsString(); itr.hasNext(); ) {
			Entry<String, String> entry = itr.next();
			if ( STOMP_HEADER_NAMES.contains(entry.getKey()) ) {
				continue;
			}
			instr.addParameter(entry.getKey(), entry.getValue());
		}
		InstructionStatus status = InstructionUtils.handleInstructionWithFeedback(instructionHandlers,
				instr);
		if ( status == null ) {
			sendError(ctx, "Unsupported topic");
		}
	}

	private void sendError(ChannelHandlerContext ctx, String message) {
		DefaultStompFrame f = new DefaultStompFrame(StompCommand.ERROR);
		f.headers().set(StompHeaders.MESSAGE, message);
		ctx.writeAndFlush(f);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

	/**
	 * Get the configured instruction handlers.
	 * 
	 * @return the handlers
	 */
	public List<FeedbackInstructionHandler> getInstructionHandlers() {
		return instructionHandlers;
	}

}
