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
import net.wimpi.modbus.msg.WriteSingleRegisterRequest;

/**
 * Class implementing a <code>WriteSingleRegisterResponse</code>. The implementation
 * directly correlates with the class 0 function <i>write single register (FC
 * 6)</i>. It encapsulates the corresponding response message.
 *
 * @author Dieter Wimberger
 * @version 1.2rc2 (14/04/2014)
 */
public final class WriteSingleRegisterResponse extends ModbusResponse {

	//instance attributes
	private int m_Reference;
	private int m_RegisterValue;

	/**
	 * Constructor.
	 * 
	 * @param req
	 *        the request to construct the response for
	 */
	public WriteSingleRegisterResponse(WriteSingleRegisterRequest req) {
		super();
		setFunctionCode(req.getFunctionCode());
		setDataLength(4);
		ModbusServerUtils.prepareResponse(req, this);
		m_Reference = req.getReference();
		m_RegisterValue = req.getRegister().getValue();
	}

	/**
	 * Constructs a new <code>WriteSingleRegisterResponse</code> instance.
	 */
	public WriteSingleRegisterResponse() {
		super();
		setDataLength(4);
	}//constructor

	/**
	 * Constructs a new <code>WriteSingleRegisterResponse</code> instance.
	 *
	 * @param reference
	 *        the offset of the register written.
	 * @param value
	 *        the value of the register.
	 */
	public WriteSingleRegisterResponse(int reference, int value) {
		super();
		setReference(reference);
		setRegisterValue(value);
		setDataLength(4);
	}//constructor

	/**
	 * Returns the value that has been returned in this
	 * <code>WriteSingleRegisterResponse</code>.
	 * 
	 * @return the value of the register.
	 */
	public int getRegisterValue() {
		return m_RegisterValue;
	}//getValue

	/**
	 * Sets the value that has been returned in the response message.
	 * 
	 * @param value
	 *        the returned register value.
	 */
	private void setRegisterValue(int value) {
		m_RegisterValue = value;
	}//setRegisterValue

	/**
	 * Returns the reference of the register that has been written to.
	 * 
	 * @return the reference of the written register.
	 */
	public int getReference() {
		return m_Reference;
	}//getReference

	/**
	 * Sets the reference of the register that has been written to.
	 * 
	 * @param ref
	 *        the reference of the written register.
	 */
	private void setReference(int ref) {
		m_Reference = ref;
		//setChanged(true);
	}//setReference

	@Override
	public void writeData(DataOutput dout) throws IOException {
		dout.writeShort(getReference());
		dout.writeShort(getRegisterValue());
	}//writeData

	@Override
	public void readData(DataInput din) throws IOException {
		setReference(din.readUnsignedShort());
		setRegisterValue(din.readUnsignedShort());
		//update data length
		setDataLength(4);
	}//readData

}//class WriteSingleRegisterResponse
