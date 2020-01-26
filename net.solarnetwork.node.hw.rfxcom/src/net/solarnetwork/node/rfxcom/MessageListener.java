/* ==================================================================
 * MessageListener.java - Jul 9, 2012 4:03:50 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.rfxcom;

import static net.solarnetwork.util.NumberUtils.unsigned;
import java.io.IOException;
import java.io.OutputStream;
import net.solarnetwork.node.ConversationalDataCollector;

/**
 * A {@link ConversationalDataCollector.DataListener} suitable for parsing
 * RFXCOM messages.
 * 
 * @author matt
 * @version $Revision$
 */
public class MessageListener implements ConversationalDataCollector.DataListener {

	private int packetSize = -1;

	/**
	 * Reset so another packet can be read.
	 */
	@Override
	public void reset() {
		packetSize = -1;
	}

	@Override
	public int getDesiredByteCount(ConversationalDataCollector dataCollector, int sinkSize) {
		return (packetSize < 1 ? 1 : packetSize - sinkSize + 1);
	}

	@Override
	public boolean receivedData(ConversationalDataCollector dataCollector, byte[] data, int offset,
			int length, OutputStream sink, int sinkSize) throws IOException {
		if ( packetSize < 1 ) {
			packetSize = unsigned(data[offset]);
		}
		sink.write(data, offset, length);
		return (packetSize + 1 - sinkSize - length) > 0;
	}

}
