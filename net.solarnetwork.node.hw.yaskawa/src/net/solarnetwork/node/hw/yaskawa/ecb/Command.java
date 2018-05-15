/* ==================================================================
 * Command.java - 15/05/2018 4:41:52 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.yaskawa.ecb;

/**
 * A command.
 * 
 * @author matt
 * @version 1.0
 */
public interface Command {

	/**
	 * Get the command value.
	 * 
	 * @return the command value
	 */
	byte getCommand();

	/**
	 * Get the sub-command value.
	 * 
	 * @return the sub-command value
	 */
	byte getSubCommand();

	/**
	 * Get the command data length, if known.
	 * 
	 * @return the data length, or {@literal -1} if not known (i.e. variable
	 *         length)
	 */
	int getDataLength();
}
