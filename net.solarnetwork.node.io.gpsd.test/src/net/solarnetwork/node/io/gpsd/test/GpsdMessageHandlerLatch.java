/* ==================================================================
 * GpsdMessageHandlerLatch.java - 13/11/2019 3:55:39 pm
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

package net.solarnetwork.node.io.gpsd.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.io.gpsd.service.GpsdMessageHandler;

/**
 * {@link GpsdMessageHandler} combined with a count-down latch.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdMessageHandlerLatch implements GpsdMessageHandler {

	private final CountDownLatch latch;
	private final List<GpsdMessage> messages;

	public GpsdMessageHandlerLatch(CountDownLatch latch) {
		super();
		this.latch = latch;
		this.messages = new ArrayList<>(8);
	}

	@Override
	public void handleGpsdMessage(GpsdMessage message) {
		messages.add(message);
		latch.countDown();
	}

	/**
	 * Wait for the latch to reach zero.
	 * 
	 * @param timeout
	 *        maximum time to wait
	 * @param unit
	 *        the time unit
	 * @return {@literal true} if the latch reached zero
	 */
	public boolean await(long timeout, TimeUnit unit) {
		try {
			return latch.await(timeout, unit);
		} catch ( InterruptedException e ) {
			// ignore
		}
		return false;
	}

	/**
	 * Get the handled messages.
	 * 
	 * @return the messages
	 */
	public List<GpsdMessage> getMessages() {
		return messages;
	}

}
