/* ===================================================================
 * SmaChannel.java
 * 
 * Created Sep 7, 2009 10:24:55 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.hw.sma.sunnynet;

import java.util.EnumMap;
import java.util.Map;


/**
 * SMA channel object.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public final class SmaChannel {
	
	private short index;
	private SmaChannelType type1;
	private short type2;
	private int dataFormat;
	private int accessLevel;
	private String name;
	private int dataLength;
	private Map<SmaChannelParam, Object> parameters;
	
	/**
	 * Construct from raw data.
	 * 
	 * <p>Decode a SmaChannel from a raw byte sequence, using the following
	 * format:</p>
	 * 
     * <pre># 1  index byte
     * # 1  channel type1 bytes
     * # 1  channel type2 bytes
     * # 2  data format bytes
     * # 2  access level
     * # 16 channel name bytes</pre>
     * 
	 * @param data the raw packet byte data to decode
	 * @param offset  the offset within the packet byte data to start decoding at
	 */
	public SmaChannel(byte[] data, int offset) {
		index = (short)(0xFF & data[offset]);
		type1 = SmaChannelType.forCode(0xFF & data[offset+1]);
		type2 = (short)(0xFF & data[offset+2]);
		dataFormat = (0xFF & data[offset+3]) | ((0xFF & data[offset+4]) << 8);
		accessLevel = (0xFF & data[offset+5]) | ((0xFF & data[offset+6]) << 8);
		name = SmaUtils.parseString(data, offset+7, 16);
		dataLength = 23 + decodeData(data, offset + 23);
	}
	
	/**
	 * Get an individual parameter value.
	 * 
	 * @param paramType the channel parameter to get
	 * @return the value, or <em>null</em> if not available
	 */
	public Object getParameterValue(SmaChannelParam paramType) {
		if ( parameters != null ) {
			return parameters.get(paramType);
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "SmaChannel{name=" +name
			+",type=" +type1
			+",index=" +index
			+",type2=" +type2
			+",accessLevel=" +accessLevel
			+",format=" +dataFormat
			+",dataLength=" +dataLength
			+(parameters == null ? "" : ",parameters="+parameters)
			+'}';
	}
	
	private int decodeData(byte[] data, int offset) {
		switch ( this.type1 ) {
			case Analog:
				return decodeAnalogData(data, offset);
				
			case Digital:
				return decodeDigitalData(data, offset);
				
			case Counter:
				return decodeCounterData(data, offset);
				
			case Status:
				return decodeStatusData(data, offset);
				
			default:
				return 0;
		}
	}

	/* 8 unit bytes (char)
	 * 4 gain bytes (float)
	 * 4 offset bytes (float)
	 */
	private int decodeAnalogData(byte[] data, int offset) {
		addParameter(SmaChannelParam.Unit, SmaUtils.parseString(data, offset, 8));
		addParameter(SmaChannelParam.Gain, SmaUtils.parseFloat(data, offset+8));
		addParameter(SmaChannelParam.Offset, SmaUtils.parseFloat(data, offset+12));
		return 16;
	}

	/* 16 text_low bytes (char)
	 * 16 text_low bytes (char)
	 */
	private int decodeDigitalData(byte[] data, int offset) {
		addParameter(SmaChannelParam.TextLow, SmaUtils.parseString(data, offset, 16));
		addParameter(SmaChannelParam.TextHigh, SmaUtils.parseString(data, offset+16, 16));
		return 32;
	}

	/* 8 unit bytes (char)
	 * 4 gain bytes (float)
	 */
	private int decodeCounterData(byte[] data, int offset) {
		addParameter(SmaChannelParam.Unit, SmaUtils.parseString(data, offset, 8));
		addParameter(SmaChannelParam.Gain, SmaUtils.parseFloat(data, offset+8));
		return 12;
	}

	/* 2 status size bytes (int)
	 * X status bytes (char)
	 */
	private int decodeStatusData(byte[] data, int offset) {
		int size = (0xFF & data[offset]) 
			| ((0xFF & data[offset+1]) << 8);
		addParameter(SmaChannelParam.Status, SmaUtils.parseString(data, offset+2, size));
		return 2 + size;
	}
	
	private void addParameter(SmaChannelParam type, Object value) {
		if ( parameters == null ) {
			parameters = new EnumMap<SmaChannelParam, Object>(SmaChannelParam.class);
		}
		parameters.put(type, value);
	}
	
	/**
	 * @return the index
	 */
	public short getIndex() {
		return index;
	}
	
	/**
	 * @return the type1
	 */
	public SmaChannelType getType1() {
		return type1;
	}
	
	/**
	 * @return the type2
	 */
	public short getType2() {
		return type2;
	}
	
	/**
	 * @return the dataFormat
	 */
	public int getDataFormat() {
		return dataFormat;
	}
	
	/**
	 * @return the accessLevel
	 */
	public int getAccessLevel() {
		return accessLevel;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return the dataLength
	 */
	public int getDataLength() {
		return dataLength;
	}
	
	/**
	 * @return the parameters
	 */
	public Map<SmaChannelParam, Object> getParameters() {
		return parameters;
	}
	
}