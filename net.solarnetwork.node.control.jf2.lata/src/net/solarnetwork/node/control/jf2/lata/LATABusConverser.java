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

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.control.jf2.lata.command.Command;
import net.solarnetwork.node.control.jf2.lata.command.CommandInterface;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.util.ByteUtils;

/**
 * Extension of {@link SerialConnectionAction} that initializes the LATA Bus
 * prior to sending commands.
 * 
 * <p>
 * Serial parameters known to work on Linux using <code>/dev/USB</code>:
 * </p>
 * 
 * <pre>
 * baud               4800
 * data bits          8
 * stop bits          1
 * parity             0
 * flow control       -1
 * receive threshold  -1
 * receive timeout    -1
 * receive framing    -1
 * dtr                -1
 * rts                -1
 * response timeout   60000
 * </pre>
 * 
 * @author shauryab
 * @version 2.0
 */
public class LATABusConverser implements SerialConnectionAction<String> {

	private static Logger LOG = LoggerFactory.getLogger(LATABusConverser.class);

	private static final byte[] MAGIC = new byte[] { 'T' };
	private static final int READ_LENGTH = 14; // e.g. T100000BD26464

	private final CommandInterface command;

	/**
	 * Construct with a specific command.
	 * 
	 * @param command
	 *        the command
	 */
	public LATABusConverser(CommandInterface command) {
		super();
		this.command = command;
	}

	@Override
	public String doWithConnection(SerialConnection conn) throws IOException {

		// sets the Reset Mode in the LATA Bus
		speakAndWait(conn, Command.StartResetMode);

		//sets the speed in the LATA Bus
		speakAndWait(conn, Command.SetSpeed);

		//sets the Operational Mode in the LATA Bus
		speakAndWait(conn, Command.StartOperationalMode);

		// drain the input buffer... the bus sometimes has stuff waiting around
		LOG.trace("Drain the input buffer", getCommand());
		byte[] data = conn.drainInputBuffer();
		LOG.trace("Drained buffer of {} bytes", data.length);

		LOG.trace("Sending command {}: {}", getCommand(), getCommand().getData());
		conn.writeMessage(command.getCommandData());

		if ( getCommand().includesResponse() ) {
			LOG.trace("Waiting for response", getCommand(), getCommand().getData());
			data = conn.readMarkedMessage(MAGIC, READ_LENGTH);
			return (data == null ? null : new String(data, ByteUtils.ASCII));
		}

		return null;
	}

	private void speakAndWait(SerialConnection conn, CommandInterface command) throws IOException {
		LOG.trace("Sending command {}: {}", command, command.getData());
		conn.writeMessage(command.getCommandData());
		synchronized ( this ) {
			try {
				this.wait(500);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
	}

	public CommandInterface getCommand() {
		return command;
	}

}
