//License
/***
 * Java Modbus Library (jamod)
 * Copyright (c) 2002-2004, jamod development team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ***/

package net.solarnetwork.node.io.modbus.server.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.util.BitVector;

/**
 * Class implementing a <code>ReadCoilsResponse</code>. The implementation directly
 * correlates with the class 1 function <i>read coils (FC 1)</i>. It
 * encapsulates the corresponding response message.
 * <p>
 * Coils are understood as bits that can be manipulated (i.e. set or unset).
 *
 * @author Dieter Wimberger
 * @version 1.2rc2 (14/04/2014)
 */
public final class ReadCoilsResponse extends ModbusResponse {

	//instance attributes
	private BitVector m_Coils;

	/**
	 * Constructor.
	 * 
	 * @param req
	 *        the request to construct the response for
	 */
	public ReadCoilsResponse(ReadCoilsRequest req) {
		super();
		setBitCount(req.getBitCount());
		setFunctionCode(req.getFunctionCode());
		ModbusServerUtils.prepareResponse(req, this);
	}

	/**
	 * Returns the number of bits (i.e. coils) read with the request.
	 * <p>
	 * 
	 * @return the number of bits that have been read.
	 */
	public int getBitCount() {
		if ( m_Coils == null ) {
			return 0;
		} else {
			return m_Coils.size();
		}
	}//getBitCount

	/**
	 * Sets the number of bits in this response.
	 *
	 * @param count
	 *        the number of response bits as int.
	 */
	public void setBitCount(int count) {
		m_Coils = new BitVector(count);
		//set correct length, without counting unitid and fc
		setDataLength(m_Coils.byteSize() + 1);
	}//setBitCount

	/**
	 * Returns the <code>BitVector</code> that stores the collection of bits that
	 * have been read.
	 * <p>
	 * 
	 * @return the <code>BitVector</code> holding the bits that have been read.
	 */
	public BitVector getCoils() {
		return m_Coils;
	}//getCoils

	/**
	 * Convenience method that returns the state of the bit at the given index.
	 * <p>
	 * 
	 * @param index
	 *        the index of the coil for which the status should be returned.
	 *
	 * @return true if set, false otherwise.
	 *
	 * @throws IndexOutOfBoundsException
	 *         if the index is out of bounds
	 */
	public boolean getCoilStatus(int index) throws IndexOutOfBoundsException {
		return m_Coils.getBit(index);
	}//getCoilStatus

	/**
	 * Sets the status of the given coil.
	 *
	 * @param index
	 *        the index of the coil to be set.
	 * @param b
	 *        true if to be set, false for reset.
	 */
	public void setCoilStatus(int index, boolean b) {
		m_Coils.setBit(index, b);
	}//setCoilStatus

	@Override
	public void writeData(DataOutput dout) throws IOException {
		dout.writeByte(m_Coils.byteSize());
		dout.write(m_Coils.getBytes(), 0, m_Coils.byteSize());
	}//writeData

	@Override
	public void readData(DataInput din) throws IOException {
		throw new UnsupportedOperationException();
	}//readData

}
