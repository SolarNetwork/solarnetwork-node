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
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.procimg.InputRegister;

/**
 * Class implementing a <code>ReadInputRegistersRequest</code>. The
 * implementation directly correlates with the class 0 function <i>read multiple
 * registers (FC 4)</i>. It encapsulates the corresponding response message.
 *
 * @author Dieter Wimberger
 * @version 1.2rc2 (14/04/2014)
 */
public final class ReadInputRegistersResponse extends ModbusResponse {

	//instance attributes
	private final int m_ByteCount;
	//private int[] m_RegisterValues;
	private final InputRegister[] m_Registers;

	/**
	 * Constructor.
	 * 
	 * @param req
	 *        the request to construct the response for
	 * @param registers
	 *        the registers
	 */
	public ReadInputRegistersResponse(ReadInputRegistersRequest req, InputRegister[] registers) {
		this(registers);
		setFunctionCode(req.getFunctionCode());
		ModbusServerUtils.prepareResponse(req, this);
	}

	/**
	 * Constructs a new <code>ReadInputRegistersResponse</code> instance.
	 *
	 * @param registers
	 *        the InputRegister[] holding response input registers.
	 */
	public ReadInputRegistersResponse(InputRegister[] registers) {
		super();
		setFunctionCode(Modbus.READ_INPUT_REGISTERS);
		m_ByteCount = registers.length * 2;
		m_Registers = registers;
		//set correct data length excluding unit id and fc
		setDataLength(m_ByteCount + 1);
	}//constructor

	/**
	 * Returns the number of bytes that have been read.
	 *
	 * @return the number of bytes that have been read as <code>int</code>.
	 */
	public int getByteCount() {
		return m_ByteCount;
	}//getByteCount

	/**
	 * Returns the number of words that have been read. The returned value
	 * should be twice as much as the byte count of the response.
	 *
	 * @return the number of words that have been read as <code>int</code>.
	 */
	public int getWordCount() {
		return m_ByteCount / 2;
	}//getWordCount

	/**
	 * Returns the <code>InputRegister</code> at the given position (relative to
	 * the reference used in the request).
	 *
	 * @param index
	 *        the relative index of the <code>InputRegister</code>.
	 * @return the register as <code>InputRegister</code>.
	 * @throws IndexOutOfBoundsException
	 *         if the index is out of bounds.
	 */
	public InputRegister getRegister(int index) throws IndexOutOfBoundsException {
		if ( index >= getWordCount() ) {
			throw new IndexOutOfBoundsException();
		} else {
			return m_Registers[index];
		}
	}//getRegister

	/**
	 * Returns the value of the register at the given position (relative to the
	 * reference used in the request) interpreted as usigned short.
	 *
	 * @param index
	 *        the relative index of the register for which the value should be
	 *        retrieved.
	 * @return the value as <code>int</code>.
	 * @throws IndexOutOfBoundsException
	 *         if the index is out of bounds.
	 */
	public int getRegisterValue(int index) throws IndexOutOfBoundsException {
		if ( index >= getWordCount() ) {
			throw new IndexOutOfBoundsException();
		} else {
			return m_Registers[index].toUnsignedShort();
		}
	}//getRegisterValue

	/**
	 * Returns a reference to the array of input registers read.
	 *
	 * @return a <code>InputRegister[]</code> instance.
	 */
	public InputRegister[] getRegisters() {
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
