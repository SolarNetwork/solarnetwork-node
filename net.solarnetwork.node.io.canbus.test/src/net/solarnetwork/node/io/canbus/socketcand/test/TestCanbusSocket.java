/* ==================================================================
 * TestCanbusSocket.java - 23/09/2019 9:07:15 pm
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

package net.solarnetwork.node.io.canbus.socketcand.test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.solarnetwork.node.io.canbus.socketcand.CanbusSocket;
import net.solarnetwork.node.io.canbus.socketcand.Message;
import net.solarnetwork.node.io.canbus.socketcand.MessageType;
import net.solarnetwork.node.io.canbus.socketcand.msg.BasicMessage;

/**
 * Implementation of {@link CanbusSocket} to help with unit tests.
 * 
 * @author matt
 * @version 1.0
 */
public class TestCanbusSocket implements CanbusSocket {

	private String host = null;
	private int port = -1;
	private boolean confirmed = false;
	private boolean closed = false;

	private final List<Message> written = new ArrayList<Message>(8);
	private final List<Message> responded = new ArrayList<Message>(8);
	private final Queue<Message> responseBuffer = new LinkedList<>();
	private final Lock lock = new ReentrantLock();
	private final Condition haveResponse = lock.newCondition();

	public TestCanbusSocket() {
		super();
	}

	@Override
	public void close() throws IOException {
		if ( closed ) {
			return;
		}
		closed = true;
		lock.lock();
		try {
			haveResponse.signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void open(String host, int port) throws IOException {
		if ( closed ) {
			throw new IOException("Already closed.");
		}
		if ( this.host != null ) {
			throw new IOException("Already opened to " + this.host + ":" + this.port);
		}
		this.host = host;
		this.port = port;

		// always respond with Hi first thing
		respondMessage(new BasicMessage(MessageType.Hi));
	}

	@Override
	public void connectionConfirmed() throws IOException {
		if ( this.confirmed ) {
			throw new IOException("Already confirmed.");
		}
		this.confirmed = true;
	}

	@Override
	public boolean isEstablished() {
		return confirmed;
	}

	@Override
	public Message nextMessage(long timeout, TimeUnit unit) throws IOException {
		lock.lock();
		try {
			while ( true ) {
				try {
					Message m = responseBuffer.poll();
					if ( m != null ) {
						return m;
					}
					if ( !haveResponse.await(timeout, unit) ) {
						throw new SocketTimeoutException("Timeout waiting at most " + timeout + " "
								+ unit.toString().toLowerCase() + " for next CAN bus message from "
								+ host + ":" + port);
					}
				} catch ( InterruptedException e ) {
					return null;
				}
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void writeMessage(Message message) throws IOException {
		written.add(message);
		if ( !closed && message.getType() == MessageType.Open ) {
			// auto respond with Ok
			respondMessage(new BasicMessage(MessageType.Ok));
		}
	}

	/**
	 * Enqueue a response message.
	 * 
	 * <p>
	 * The message will be added to the queue of messages used by
	 * {@link #nextMessage()}.
	 * </p>
	 * 
	 * @param message
	 *        the message to enqueue
	 */
	public void respondMessage(Message message) throws IOException {
		lock.lock();
		try {
			responded.add(message);
			responseBuffer.add(message);
			haveResponse.signal();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Get the list of messages that have been passed to
	 * {@link #writeMessage(Message)}.
	 * 
	 * @return the list of written messages
	 */
	public List<Message> getWrittenMessages() {
		return written;
	}

	/**
	 * Get the list of messages that have been passed to
	 * {@link #respondMessage(Message)}.
	 * 
	 * @return the list of responded messages
	 */
	public List<Message> getRespondedMessages() {
		return responded;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

}
