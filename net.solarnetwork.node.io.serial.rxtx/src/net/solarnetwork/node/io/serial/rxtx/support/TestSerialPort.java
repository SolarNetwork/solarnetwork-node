/* ==================================================================
 * TestSerialPort.java - Oct 27, 2014 8:07:48 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.serial.rxtx.support;

import gnu.io.SerialPort;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

/**
 * Implementation of {@link SerialPort} for testing purposes;
 * 
 * @author matt
 * @version 1.0
 */
public class TestSerialPort extends SerialPort {

	private int baud = 1200;
	private int dataBits = 8;
	private int stopBits = 1;
	private int parity = 0;
	private int flowControl = 0;
	private boolean dtr;
	private boolean rts;
	private boolean cts;
	private boolean dsr;
	private boolean carrierDetect;
	private boolean ringIndicator;
	private SerialPortEventListener listener;
	private boolean notifyOnDataAvailable;
	private boolean notifyOnOutputEmpty;
	private boolean notifyOnCTS;
	private boolean notifyOnCarrierDetect;
	private boolean notifyOnDSR;
	private boolean notifyOnRingIndicator;
	private boolean notifyOnOverrunError;
	private boolean notifyOnParityError;
	private boolean notifyOnFramingError;
	private boolean notifyOnBreakInterrupt;
	private byte parityErrorChar;
	private byte endOfInputChar;
	private String uartType;
	private int baudBase;
	private int divisor;
	private boolean lowLatency;
	private boolean callOutHangup;
	private int receiveFramingByte;
	private int receiveTimeout;
	private int receiveThreshold = 1;
	private int inputBufferSize;
	private int outputBufferSize;

	@Override
	public void setSerialPortParams(int b, int d, int s, int p) throws UnsupportedCommOperationException {
		baud = b;
		dataBits = d;
		stopBits = s;
		parity = p;
	}

	@Override
	public int getBaudRate() {
		return baud;
	}

	@Override
	public int getDataBits() {
		return dataBits;
	}

	@Override
	public int getStopBits() {
		return stopBits;
	}

	@Override
	public int getParity() {
		return parity;
	}

	@Override
	public void setFlowControlMode(int flowcontrol) throws UnsupportedCommOperationException {
		flowControl = flowcontrol;
	}

	@Override
	public int getFlowControlMode() {
		return flowControl;
	}

	@Override
	public boolean isDTR() {
		return dtr;
	}

	@Override
	public void setDTR(boolean state) {
		dtr = state;
	}

	@Override
	public void setRTS(boolean state) {
		rts = state;
	}

	@Override
	public boolean isCTS() {
		return cts;
	}

	@Override
	public boolean isDSR() {
		return dsr;
	}

	@Override
	public boolean isCD() {
		return carrierDetect;
	}

	@Override
	public boolean isRI() {
		return ringIndicator;
	}

	@Override
	public boolean isRTS() {
		return rts;
	}

	@Override
	public void sendBreak(int duration) {
		// no-op
	}

	@Override
	public void addEventListener(SerialPortEventListener lsnr) throws TooManyListenersException {
		if ( listener != null ) {
			throw new TooManyListenersException();
		}
		listener = lsnr;
	}

	@Override
	public void removeEventListener() {
		listener = null;
	}

	@Override
	public void notifyOnDataAvailable(boolean enable) {
		notifyOnDataAvailable = enable;
	}

	@Override
	public void notifyOnOutputEmpty(boolean enable) {
		notifyOnOutputEmpty = enable;
	}

	@Override
	public void notifyOnCTS(boolean enable) {
		notifyOnCTS = enable;
	}

	@Override
	public void notifyOnDSR(boolean enable) {
		notifyOnDSR = enable;
	}

	@Override
	public void notifyOnRingIndicator(boolean enable) {
		notifyOnRingIndicator = enable;
	}

	@Override
	public void notifyOnCarrierDetect(boolean enable) {
		notifyOnCarrierDetect = enable;
	}

	@Override
	public void notifyOnOverrunError(boolean enable) {
		notifyOnOverrunError = enable;
	}

	@Override
	public void notifyOnParityError(boolean enable) {
		notifyOnParityError = enable;
	}

	@Override
	public void notifyOnFramingError(boolean enable) {
		notifyOnFramingError = enable;
	}

	@Override
	public void notifyOnBreakInterrupt(boolean enable) {
		notifyOnBreakInterrupt = enable;
	}

	@Override
	public byte getParityErrorChar() throws UnsupportedCommOperationException {
		return parityErrorChar;
	}

	@Override
	public boolean setParityErrorChar(byte b) throws UnsupportedCommOperationException {
		parityErrorChar = b;
		return true;
	}

	@Override
	public byte getEndOfInputChar() throws UnsupportedCommOperationException {
		return endOfInputChar;
	}

	@Override
	public boolean setEndOfInputChar(byte b) throws UnsupportedCommOperationException {
		endOfInputChar = b;
		return true;
	}

	@Override
	public boolean setUARTType(String type, boolean test) throws UnsupportedCommOperationException {
		uartType = type;
		return true;
	}

	@Override
	public String getUARTType() throws UnsupportedCommOperationException {
		return uartType;
	}

	@Override
	public boolean setBaudBase(int BaudBase) throws UnsupportedCommOperationException, IOException {
		baudBase = BaudBase;
		return true;
	}

	@Override
	public int getBaudBase() throws UnsupportedCommOperationException, IOException {
		return baudBase;
	}

	@Override
	public boolean setDivisor(int Divisor) throws UnsupportedCommOperationException, IOException {
		divisor = Divisor;
		return true;
	}

	@Override
	public int getDivisor() throws UnsupportedCommOperationException, IOException {
		return divisor;
	}

	@Override
	public boolean setLowLatency() throws UnsupportedCommOperationException {
		lowLatency = true;
		return true;
	}

	@Override
	public boolean getLowLatency() throws UnsupportedCommOperationException {
		return lowLatency;
	}

	@Override
	public boolean setCallOutHangup(boolean NoHup) throws UnsupportedCommOperationException {
		callOutHangup = NoHup;
		return true;
	}

	@Override
	public boolean getCallOutHangup() throws UnsupportedCommOperationException {
		return callOutHangup;
	}

	@Override
	public void enableReceiveFraming(int f) throws UnsupportedCommOperationException {
		receiveFramingByte = f;
	}

	@Override
	public void disableReceiveFraming() {
		receiveFramingByte = 0;
	}

	@Override
	public boolean isReceiveFramingEnabled() {
		return receiveFramingByte != 0;
	}

	@Override
	public int getReceiveFramingByte() {
		return receiveFramingByte;
	}

	@Override
	public void disableReceiveTimeout() {
		receiveTimeout = 0;
	}

	@Override
	public void enableReceiveTimeout(int time) throws UnsupportedCommOperationException {
		receiveTimeout = time;
	}

	@Override
	public boolean isReceiveTimeoutEnabled() {
		return receiveTimeout > 0;
	}

	@Override
	public int getReceiveTimeout() {
		return receiveTimeout;
	}

	@Override
	public void enableReceiveThreshold(int thresh) throws UnsupportedCommOperationException {
		receiveThreshold = thresh;
	}

	@Override
	public void disableReceiveThreshold() {
		receiveThreshold = 1;
	}

	@Override
	public int getReceiveThreshold() {
		return receiveThreshold;
	}

	@Override
	public boolean isReceiveThresholdEnabled() {
		return receiveThreshold > 1;
	}

	@Override
	public void setInputBufferSize(int size) {
		inputBufferSize = size;
	}

	@Override
	public int getInputBufferSize() {
		return inputBufferSize;
	}

	@Override
	public void setOutputBufferSize(int size) {
		outputBufferSize = size;
	}

	@Override
	public int getOutputBufferSize() {
		return outputBufferSize;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	public boolean isNotifyOnDataAvailable() {
		return notifyOnDataAvailable;
	}

	public boolean isNotifyOnOutputEmpty() {
		return notifyOnOutputEmpty;
	}

	public boolean isNotifyOnCTS() {
		return notifyOnCTS;
	}

	public boolean isNotifyOnCarrierDetect() {
		return notifyOnCarrierDetect;
	}

	public boolean isNotifyOnDSR() {
		return notifyOnDSR;
	}

	public boolean isNotifyOnOverrunError() {
		return notifyOnOverrunError;
	}

	public boolean isNotifyOnFramingError() {
		return notifyOnFramingError;
	}

	public boolean isNotifyOnBreakInterrupt() {
		return notifyOnBreakInterrupt;
	}

	public boolean isNotifyOnRingIndicator() {
		return notifyOnRingIndicator;
	}

	public boolean isNotifyOnParityError() {
		return notifyOnParityError;
	}

}
