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
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.procimg.Register;

/**
 * Class implementing a <tt>ReadMultipleRegistersResponse</tt>. The
 * implementation directly correlates with the class 0 function <i>read multiple
 * registers (FC 3)</i>. It encapsulates the corresponding response message.
 *
 * @author Dieter Wimberger
 * @version 1.2rc2 (14/04/2014)
 */
public final class ReadMultipleRegistersResponse extends ModbusResponse {

	//instance attributes
	private final int m_ByteCount;
	private final Register[] m_Registers;

	/**
	 * Constructor.
	 * 
	 * @param req
	 *        the request to construct the response for
	 * @param regisgters
	 *        the registers
	 */
	public ReadMultipleRegistersResponse(ReadMultipleRegistersRequest req, Register[] registers) {
		this(registers);
		setFunctionCode(req.getFunctionCode());
		ModbusServerUtils.prepareResponse(req, this);
	}

	/**
	 * Constructs a new <tt>ReadInputRegistersResponse</tt> instance.
	 *
	 * @param registers
	 *        the Register[] holding response registers.
	 */
	public ReadMultipleRegistersResponse(Register[] registers) {
		super();
		m_Registers = registers;
		m_ByteCount = registers.length * 2;
		setFunctionCode(Modbus.READ_MULTIPLE_REGISTERS);
		//set correct data length excluding unit id and fc
		setDataLength(m_ByteCount + 1);
	}//constructor

	/**
	 * Returns the number of bytes that have been read.
	 * <p>
	 * 
	 * @return the number of bytes that have been read as <tt>int</tt>.
	 */
	public int getByteCount() {
		return m_ByteCount;
	}//getByteCount

	/**
	 * Returns the number of words that have been read. The returned value
	 * should be half of the the byte count of this
	 * <tt>ReadMultipleRegistersResponse</tt>.
	 * <p>
	 * 
	 * @return the number of words that have been read as <tt>int</tt>.
	 */
	public int getWordCount() {
		return m_ByteCount / 2;
	}//getWordCount

	/**
	 * Returns the value of the register at the given position (relative to the
	 * reference used in the request) interpreted as unsigned short.
	 * <p>
	 * 
	 * @param index
	 *        the relative index of the register for which the value should be
	 *        retrieved.
	 *
	 * @return the value as <tt>int</tt>.
	 *
	 * @throws IndexOutOfBoundsException
	 *         if the index is out of bounds.
	 */
	public int getRegisterValue(int index) throws IndexOutOfBoundsException {
		return m_Registers[index].toUnsignedShort();
	}//getRegisterValue

	/**
	 * Returns the <tt>Register</tt> at the given position (relative to the
	 * reference used in the request).
	 * <p>
	 * 
	 * @param index
	 *        the relative index of the <tt>Register</tt>.
	 *
	 * @return the register as <tt>Register</tt>.
	 *
	 * @throws IndexOutOfBoundsException
	 *         if the index is out of bounds.
	 */
	public Register getRegister(int index) throws IndexOutOfBoundsException {
		if ( index >= getWordCount() ) {
			throw new IndexOutOfBoundsException();
		} else {
			return m_Registers[index];
		}
	}//getRegister

	/**
	 * Returns a reference to the array of registers read.
	 *
	 * @return a <tt>Register[]</tt> instance.
	 */
	public Register[] getRegisters() {
		return m_Registers;
	}//getRegisters

	@Override
	public void writeData(DataOutput dout) throws IOException {
		dout.writeByte(m_ByteCount);
		for ( int k = 0; k < getWordCount(); k++ ) {
			dout.write(m_Registers[k].toBytes());
		}
	}//writeData

	@Override
	public void readData(DataInput din) throws IOException {
		throw new UnsupportedOperationException();
	}//readData

}
