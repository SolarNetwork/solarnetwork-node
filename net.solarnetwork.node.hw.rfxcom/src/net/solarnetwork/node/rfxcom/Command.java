/* ==================================================================
 * Command.java - Jul 6, 2012 5:02:16 PM
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

/**
 * A "command" to send to the RFXCOM transceiver.
 * 
 * @author matt
 * @version $Revision$
 */
public enum Command {

	/** Reset the receiver/transceiver. */
	Reset(0x0),
	
	/** Request firmware versions and configuration of the interface. */
	Status(0x2),
	
	/** Set the configuration of the interface. */
	SetMode(0x3),
	
	/** Enable all receiving modes of the receiver/transceiver. */
	EnableAllReceivingModes(0x4),
	
	/** Display undecoded messages. */
	Undecoded(0x5),
	
	/** Sve receiving modes of the receiver/transceiver in non-volatile memory. */
	SaveSettings(0x6),
	
	/** Disable receiving of X10. */
	DisableX10(0x10),
	
	/** Disable receiving of ARC. */
	DisableARC(0x11),
	
	/** Disable receiving of AC. */
	DisableAC(0x12),
	
	/** Disable receiving of HomeEasy EU. */
	DisableHomeEasyEU(0x13),
	
	/** Disable receiving of Ikea-Koppla. */
	DisableIkeaKoppla(0x14),
	
	/** Disable receiving of Oregon Scientific. */
	DisableOregonScientific(0x15),
	
	/** Disable receiving of ATI Remote Wonder. */
	DisableATIRemoteWonder(0x16),
	
	/** Disable receiving of Visonic. */
	DisableVisonic(0x17),
	
	/** Disable receiving of Mertik. */
	DisableMertik(0x18),
	
	/** Disable receiving of AD. */
	DisableAD(0x19),
	
	/** Disable receiving of Hideki. */
	DisableHideki(0x1a),
	
	/** Disable receiving of La Crosse. */
	DisableLaCrosse(0x1b),
	
	/** Disable receiving of FS20. */
	DisableFS20(0x1c),
	
	/** Select 310MHz in the 310/315 transceiver. */
	Select310(0x50),
	
	/** Select 315MHz in the 310/315 transceiver. */
	Select315(0x51),
	
	/** Select 868.00MHz ASK in the 868 transceiver. */
	Select800(0x55),
	
	/** Select 868.00MHz FSK in the 868 transceiver. */
	Select800F(0x56),
	
	/** Select 868.30MHz ASK in the 868 transceiver. */
	Select830(0x57),
	
	/** Select 868.30MHz FSK in the 868 transceiver. */
	Select830F(0x58),
	
	/** Select 868.35MHz ASK in the 868 transceiver. */
	Select835(0x59),
	
	/** Select 868.35MHz FSK in the 868 transceiver. */
	Select835F(0x5a),
	
	/** Select 868.95MHz in the 868 transceiver. */
	Select895(0x5b);
	
	private final byte value;
	
	private Command(int value) {
		this.value = (byte)value;
	}
	
	/**
	 * Get the message value of this command, suitable for using in a message packet.
	 * 
	 * @return the message value
	 */
	public byte getMessageValue() {
		return value;
	}
	
	/**
	 * Parse a byte into an enumerated value.
	 * 
	 * @param b the byte to parse
	 * @return the value
	 * @throws IllegalArgumentException if the byte is not a supported type
	 */
	public static Command valueOf(byte b) {
		switch ( b ) {
			case 0x0: return Reset;				
			case 0x2: return Status;				
			case 0x3: return SetMode;				
			case 0x4: return EnableAllReceivingModes;
			case 0x5: return Undecoded;
			case 0x6: return SaveSettings;
			case 0x10: return DisableX10;
			case 0x11: return DisableARC;
			case 0x12: return DisableAC;
			case 0x13: return DisableHomeEasyEU;
			case 0x14: return DisableIkeaKoppla;
			case 0x15: return DisableOregonScientific;
			case 0x16: return DisableATIRemoteWonder;
			case 0x17: return DisableVisonic;
			case 0x18: return DisableMertik;
			case 0x19: return DisableAD;
			case 0x1a: return DisableHideki;
			case 0x1b: return DisableLaCrosse;
			case 0x1c: return DisableFS20;
			case 0x50: return Select310;
			case 0x51: return Select315;
			case 0x55: return Select800;
			case 0x56: return Select800F;
			case 0x57: return Select830;
			case 0x58: return Select830F;
			case 0x59: return Select835;
			case 0x5a: return Select835F;
			case 0x5b: return Select895;
			default: 
				throw new IllegalArgumentException("Command 0x" 
					+String.format("%x", b) +" not supported");
		}
	}
	
	
}
