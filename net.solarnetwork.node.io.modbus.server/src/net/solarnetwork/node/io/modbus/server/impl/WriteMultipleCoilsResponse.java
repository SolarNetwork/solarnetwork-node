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
import net.wimpi.modbus.msg.WriteMultipleCoilsRequest;

/**
 * Class implementing a <tt>WriteMultipleCoilsResponse</tt>. The implementation
 * directly correlates with the class 1 function <i>read coils (FC 15)</i>. It
 * encapsulates the corresponding response message.
 * <p>
 * Coils are understood as bits that can be manipulated (i.e. set or unset).
 *
 * @author Dieter Wimberger
 * @version 1.2rc2 (14/04/2014)
 */
public final class WriteMultipleCoilsResponse extends ModbusResponse {

	//instance attributes
	private int m_Reference;
	private int m_BitCount;

	/**
	 * Constructor.
	 * 
	 * @param req
	 *        the request to construct the response for
	 */
	public WriteMultipleCoilsResponse(WriteMultipleCoilsRequest req) {
		super();
		setFunctionCode(req.getFunctionCode());
		setDataLength(4);
		ModbusServerUtils.prepareResponse(req, this);
		m_Reference = req.getReference();
		m_BitCount = req.getBitCount();
	}

	/**
	 * Returns the reference of the register to to start reading from with this
	 * <tt>WriteMultipleCoilsRequest</tt>.
	 * <p>
	 * 
	 * @return the reference of the register to start reading from as
	 *         <tt>int</tt>.
	 */
	public int getReference() {
		return m_Reference;
	}//getReference

	/**
	 * Returns the number of bits (i.e. coils) read with the request.
	 * <p>
	 * 
	 * @return the number of bits that have been read.
	 */
	public int getBitCount() {
		return m_BitCount;
	}//getBitCount

	/**
	 * Sets the number of bits (i.e. coils) that will be in a response.
	 *
	 * @param count
	 *        the number of bits in the response.
	 */
	public void setBitCount(int count) {
		m_BitCount = count;
	}//setBitCount

	@Override
	public void writeData(DataOutput dout) throws IOException {

		dout.writeShort(m_Reference);
		dout.writeShort(m_BitCount);
	}//writeData

	@Override
	public void readData(DataInput din) throws IOException {

		m_Reference = din.readUnsignedShort();
		m_BitCount = din.readUnsignedShort();
	}//readData

}//class ReadCoilsResponse
