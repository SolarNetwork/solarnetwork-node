/* ==================================================================
 * SetModeMessage.java - Jul 8, 2012 1:32:11 PM
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

import java.util.Arrays;

/**
 * A message for setting the mode of the RFXCOM transceiver.
 * 
 * @author matt
 * @version $Revision$
 */
public class SetModeMessage extends StatusMessage {

	private byte[] mutableData;
	
	/**
	 * Constructor.
	 * 
	 * @param sequenceNumber the sequence number
	 */
	public SetModeMessage(short sequenceNumber, TransceiverType type) {
		super(sequenceNumber, Command.SetMode);
		mutableData = new byte[10];
		Arrays.fill(mutableData, (byte)0);
		mutableData[1] = type.getMessageValue();
	}
	
	/**
	 * Create a SetMode message using a StatusMessage as the starting point.
	 * 
	 * <p>This is useful for querying the current status, then making changes
	 * only to specific settings as needed.</p>
	 * 
	 * @param sequenceNumber the sequence number
	 * @param status the status message to copy
	 */
	public SetModeMessage(short sequenceNumber, StatusMessage status) {
		super(sequenceNumber, Command.SetMode);
		mutableData = new byte[10];
		System.arraycopy(status.getData(), 0, mutableData, 0, mutableData.length);
	}

	@Override
	public byte[] getData() {
		return mutableData;
	}
	
	private void setBitFlag(int index, byte value, boolean enabled) {
		if ( enabled ) {
			mutableData[index] |= value;
		} else {
			mutableData[index] &= ~value;
		}
	}
	
	public void setUndecodedMode(boolean value) {
		setBitFlag(3, (byte)0x80, value);
	}
	
	public void setProGuardEnabled(boolean value) {
		setBitFlag(4, (byte)0x20, value);
	}
	
	public void setFS20Enabled(boolean value) {
		setBitFlag(4, (byte)0x10, value);
	}
	
	public void setLaCrosseEnabled(boolean value) {
		setBitFlag(4, (byte)0x8, value);
	}
	
	public void setHidekiEnabled(boolean value) {
		setBitFlag(4, (byte)0x4, value);
	}
	
	public void setADEnabled(boolean value) {
		setBitFlag(4, (byte)0x2, value);
	}
	
	public void setMertikEnabled(boolean value) {
		setBitFlag(4, (byte)0x1, value);
	}
	
	public void setVisonicEnabled(boolean value) {
		setBitFlag(5, (byte)0x80, value);
	}
	
	public void setATIEnabled(boolean value) {
		setBitFlag(5, (byte)0x40, value);
	}
	
	public void setOregonEnabled(boolean value) {
		setBitFlag(5, (byte)0x20, value);
	}
	
	public void setIkeaKopplaEnabled(boolean value) {
		setBitFlag(5, (byte)0x10, value);
	}
	
	public void setHomeEasyEUEnabled(boolean value) {
		setBitFlag(5, (byte)0x8, value);
	}
	
	public void setACEnabled(boolean value) {
		setBitFlag(4, (byte)0x4, value);
	}
	
	public void setARCEnabled(boolean value) {
		setBitFlag(5, (byte)0x2, value);
	}
	
	public void setX10Enabled(boolean value) {
		setBitFlag(5, (byte)0x1, value);
	}
	
}
