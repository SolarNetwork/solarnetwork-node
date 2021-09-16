/* ==================================================================
 * AddressableCommand.java - Oct 27, 2011 12:50:11 PM
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
 */

package net.solarnetwork.node.control.jf2.lata.command;

/**
 * Implementation of {@link CommandInterface} that works with addresses.
 * 
 * @author matt
 * @version 1.0
 */
public class AddressableCommand implements CommandInterface {

	/** Regular expression to check hex value */
	private static final String COMMAND_PATTERN = "[0-9A-F]+";
	
	/** Minimum Hex value of an address in the LATA network */
	private static final Long MIN_HEX = Long.valueOf(0x00000000);
	
	/** Maximum Hex value of an address in the LATA network */
	private static final Long MAX_HEX = Long.valueOf(0x1FFFFFFF);
	
	private static final String MIN_HEX_STRING = "00000000";
	
	private static final String MAX_HEX_STRING = "1FFFFFFF";
	
	/** String representation of the switch address in the LATA network */
	private final String hexIdentifier;
	private final Command command;
	
	/**
	 * Constructor.
	 * 
	 * @param hexIdentifier the address to target
	 * @param command the command to execute
	 * @throws CommandValidationException if the address isn't valid
	 */
	public AddressableCommand(String hexIdentifier, Command command) 
	throws CommandValidationException {
		this.hexIdentifier = hexIdentifier;
		this.command = command;
		validate();
	}
	
	private void validate() {
		if (hexIdentifier == null) {
			throw new NullPointerException("Switch Identifier provided is null");
		}
		if (hexIdentifier.length() != 8) {
			throw new CommandValidationException("Hex identifier needs to be 8 characters");
		}
		if (!hexIdentifier.matches(COMMAND_PATTERN) 
				|| Long.parseLong(hexIdentifier, 16) < MIN_HEX 
				|| Long.parseLong(hexIdentifier, 16) > MAX_HEX) {
			throw new CommandValidationException("Invalid Hex identifier \"" + hexIdentifier + "\". " +
					"Passed Identifier needs to be a HEX number between \"" + 
					MIN_HEX_STRING + "\" and \"" + MAX_HEX_STRING + "\" with uppercase HEX Characters.");
		}
	}

	@Override
	public byte[] getCommandData() {
		return String.format(new String(command.getCommandData()), hexIdentifier).getBytes();
	}

	@Override
	public String getData() {
		return String.format(command.getData(), hexIdentifier);
	}

	@Override
	public boolean includesResponse() {
		return command.includesResponse();
	}

	@Override
	public String toString() {
		return "AddressableCommand{"+hexIdentifier+","+command+"}";
	}

}
