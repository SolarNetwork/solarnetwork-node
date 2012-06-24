/* ==================================================================
 * Converser.java - Jun 27, 2011 1:06:01 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id: Converser.java 1703 2011-07-12 01:43:35Z shauryab $
 * ==================================================================
 */

package net.solarnetwork.node.control.jf2.lata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.node.ConversationalDataCollector;
import net.solarnetwork.node.control.jf2.lata.command.Command;
import net.solarnetwork.node.control.jf2.lata.command.CommandInterface;

/**
 * Extension of {@link Converser} that initializes the LATA Bus prior to 
 * sending commands.
 * 
 * <p>Serial parameters known to work on Linux using <code>/dev/USB</code>:<p>
 * 
 * <pre>baud               4800
 * data bits          8
 * stop bits          1
 * parity             0
 * flow control       -1
 * receive threshold  -1
 * receive timeout    30000
 * receive framing    -1
 * dtr                -1
 * rts                -1
 * response timeout   60000</pre>
 * 
 * @author shauryab
 */
public class LATABusConverser extends Converser {

	private static Logger LOG = LoggerFactory.getLogger(LATABusConverser.class);
	
	private static final byte[] MAGIC = new byte[] {'T'};
	private static final int READ_LENGTH = 13; // e.g. 100000BD26464
	
	/**
	 * Construct with a specific command.
	 * 
	 * @param command the command
	 */
	public LATABusConverser(CommandInterface command) {
		super(command);
	}
	
	@Override
	public String conductConversation(ConversationalDataCollector dataCollector) {
		//sets the Operational Mode in the LATA Bus
		speakAndWait(dataCollector, Command.StartOperationalMode, 500);
		
		//sets the speed in the LATA Bus
		speakAndWait(dataCollector, Command.SetSpeed, 500);
		
		//send the actual command
		
		// TODO: what was this for, when we call this again immediately below?
		// speakAndWait(dataCollector, getCommand(), 500);
		
		if ( getCommand().includesResponse() ) {
			LOG.trace("Sending command {} ({}) and waiting for response", 
					getCommand(), getCommand().getData());
			dataCollector.speakAndCollect(getCommand().getCommandData(), MAGIC, READ_LENGTH);
			return dataCollector.getCollectedDataAsString();
		}
		
		dataCollector.speak(getCommand().getCommandData());
		return null;
	}
	
	private void speakAndWait(ConversationalDataCollector dataCollector,
			CommandInterface command, long waitMillis) {
		LOG.trace("Sending command {}: {}", command, command.getData());
		dataCollector.speak(command.getCommandData());
		synchronized (this) {
			try {
				this.wait(waitMillis);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	
}
