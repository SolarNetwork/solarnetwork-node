/* ==================================================================
 * VariableLengthCommand.java - 15/05/2018 4:43:40 PM
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
 * A variable length command configured with a known length.
 * 
 * @author matt
 * @version 1.0
 */
public class VariableLengthCommand implements Command {

	private final Command delegate;
	private final int bodyLength;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the command to execute
	 * @param bodyLength
	 *        the body length
	 */
	public VariableLengthCommand(Command delegate, int bodyLength) {
		super();
		this.delegate = delegate;
		this.bodyLength = bodyLength;
	}

	@Override
	public byte getCommand() {
		return delegate.getCommand();
	}

	@Override
	public byte getSubCommand() {
		return delegate.getSubCommand();
	}

	@Override
	public int getBodyLength() {
		return this.bodyLength;
	}

}
