/* ==================================================================
 * StatusResponse.java - Jul 7, 2012 4:00:34 PM
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
 * A status message command message
 * 
 * @author matt
 * @version $Revision$
 */
public class StatusMessage extends CommandMessage {

	public StatusMessage(short sequenceNumber, byte[] data) {
		super(sequenceNumber, data);
	}
	
	protected StatusMessage(short sequenceNumber, Command command) {
		super(command, sequenceNumber);
	}
	
	private byte nullSafeGetDataIndex(int index) {
		byte[] d = getData();
		if ( d == null || index < 0 || index >= d.length ) {
			return -1;
		}
		return d[index];
	}
	
	public TransceiverType getTransceiverType() {
		return TransceiverType.valueOf(nullSafeGetDataIndex(1));
	}
	
	public int getFirmwareVersion() {
		return nullSafeGetDataIndex(2);
	}
	
	public int getHardwareVersion() {
		return 1; // TODO where is this coming from?
	}

	public boolean isUndecodedMode() {
		return ((nullSafeGetDataIndex(3) & (byte)0x80) == (byte)0x80);
	}
	
	public boolean isProGuardEnabled() {
		return ((nullSafeGetDataIndex(4) & (byte)0x20) == (byte)0x20);
	}
	
	public boolean isFS20Enabled() {
		return ((nullSafeGetDataIndex(4) & (byte)0x10) == (byte)0x10);
	}
	
	public boolean isLaCrosseEnabled() {
		return ((nullSafeGetDataIndex(4) & (byte)0x8) == (byte)0x8);
	}
	
	public boolean isHidekiEnabled() {
		return ((nullSafeGetDataIndex(4) & (byte)0x4) == (byte)0x4);
	}
	
	public boolean isADEnabled() {
		return ((nullSafeGetDataIndex(4) & (byte)0x2) == (byte)0x2);
	}
	
	public boolean isMertikEnabled() {
		return ((nullSafeGetDataIndex(4) & (byte)0x1) == (byte)0x1);
	}
	
	public boolean isVisonicEnabled() {
		return ((nullSafeGetDataIndex(5) & (byte)0x80) == (byte)0x80);
	}
	
	public boolean isATIEnabled() {
		return ((nullSafeGetDataIndex(5) & (byte)0x40) == (byte)0x40);
	}
	
	public boolean isOregonEnabled() {
		return ((nullSafeGetDataIndex(5) & (byte)0x20) == (byte)0x20);
	}
	
	public boolean isIkeaKopplaEnabled() {
		return ((nullSafeGetDataIndex(5) & (byte)0x10) == (byte)0x10);
	}
	
	public boolean isHomeEasyEUEnabled() {
		return ((nullSafeGetDataIndex(5) & (byte)0x8) == (byte)0x8);
	}
	
	public boolean isACEnabled() {
		return ((nullSafeGetDataIndex(5) & (byte)0x4) == (byte)0x4);
	}
	
	public boolean isARCEnabled() {
		return ((nullSafeGetDataIndex(5) & (byte)0x2) == (byte)0x2);
	}
	
	public boolean isX10Enabled() {
		return ((nullSafeGetDataIndex(5) & (byte)0x1) == (byte)0x1);
	}
	
}
