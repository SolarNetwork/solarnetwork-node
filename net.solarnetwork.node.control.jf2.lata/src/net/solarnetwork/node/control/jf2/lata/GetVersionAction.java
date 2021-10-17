/* ==================================================================
 * GetVersionAction.java - Oct 27, 2014 2:24:31 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.jf2.lata;

import java.io.IOException;
import net.solarnetwork.node.control.jf2.lata.command.Command;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.util.ByteUtils;

/**
 * {@link SerialConnectionAction} to get the LATA version.
 * 
 * @author matt
 * @version 2.0
 */
public class GetVersionAction implements SerialConnectionAction<String> {

	private final Command command;

	/**
	 * Constructor.
	 * 
	 * @param longVersion
	 *        {@literal true} to get the long version, {@literal false} for short
	 */
	public GetVersionAction(boolean longVersion) {
		super();
		this.command = (longVersion ? Command.GetVersionLong : Command.GetVersionShort);
	}

	@Override
	public String doWithConnection(SerialConnection conn) throws IOException {
		conn.writeMessage(command.getCommandData());
		byte[] result = conn.readMarkedMessage(
				command == Command.GetVersionLong ? "v".getBytes() : "V".getBytes(), "\r".getBytes());
		if ( result == null || result.length < 2 ) {
			return null;
		}
		String ver = new String(result, ByteUtils.ASCII);
		return ver.substring(1, ver.length() - 1);
	}

}
