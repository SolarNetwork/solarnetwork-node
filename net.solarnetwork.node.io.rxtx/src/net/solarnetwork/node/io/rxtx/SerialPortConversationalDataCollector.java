/* ===================================================================
 * SerialPortConversationalDataCollector.java
 * 
 * Created Aug 19, 2009 11:49:24 AM
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

package net.solarnetwork.node.io.rxtx;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.node.ConversationalDataCollector;
import net.solarnetwork.node.DataCollector;

/**
 * Serial port conversation-based {@link DataCollector} implementation.
 * 
 * <p>Note this class is not thread-safe, in that
 * {@link #collectData(net.solarnetwork.node.ConversationalDataCollector.Moderator)}
 * should not be called by more than one thread at a time.</p>
 * 
 * @author matt
 * @version $Revision$ $Date$
 * @param <T> the datum type
 */
public class SerialPortConversationalDataCollector extends SerialPortSupport implements
		ConversationalDataCollector, SerialPortEventListener {

	private byte[] magic;
	private int readSize;

	private InputStream in;
	private OutputStream out;
	private ByteArrayOutputStream buffer;
	private boolean listening = false;
	private boolean collecting = false;
	private boolean doneCollecting = false;

	private Logger eventLog = LoggerFactory.getLogger(getClass().getName()+".SERIAL_EVENT");
	
	/**
	 * Constructor.
	 * 
	 * @param serialPort the SerialPort to use
	 * @param maxWait the maximum number of milliseconds to wait when waiting
	 * to read data
	 */
	public SerialPortConversationalDataCollector(SerialPort serialPort, long maxWait) {
		super(serialPort, maxWait);
		this.buffer = new ByteArrayOutputStream();
	}

	@Override
	public <T> T collectData(Moderator<T> moderator) {
		setupSerialPortParameters(this);
		
		// open the input stream
		try {
			this.out = serialPort.getOutputStream();
			this.in = serialPort.getInputStream();
			
			return moderator.conductConversation(this);
			
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			log.trace("Cleaning up port {}...", serialPort);
			if ( this.in != null ) {
				try {
					this.in.close();
				} catch ( IOException e ) {
					// ignore this one
				}
			}
			if ( this.out != null ) {
				try {
					this.out.close();
				} catch ( IOException e ) {
					// ignore this one
				}
			}
			serialPort.removeEventListener();
			log.trace("Clean up port {} complete.", serialPort);
		}
	}

	@Override
	public void collectData() {
		throw new UnsupportedOperationException("Use the collectData(Moderator) method.");
	}
	
	/**
	 * Speak and then listen for a response.
	 * 
	 * <p>The {@code data} will be written to the serial port's
	 * {@link OutputStream} and then this method will block until all
	 * available data has been read from the serial port's
	 * {@link InputStream}. Each invocation of this method will first
	 * clear the internal data buffer, and all received response data
	 * will be stored on the internal data buffer. Calling code can
	 * access this buffer by calling {@link #getCollectedData()}.</p>
	 * 
	 * @param data the data to write to the serial port
	 */
	public void speakAndListen(byte[] data) {
		try {
			// sleep until we have data
			synchronized (this) {
				this.buffer.reset();
				listening = true;
				collecting = false;
				doneCollecting = false;
				out.write(data);
				this.wait(getMaxWait());
			}
			if ( log.isWarnEnabled() && !doneCollecting ) {
				log.warn("Timeout collecting serial data");
			}
		} catch ( InterruptedException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			this.listening = false;
		}
	}

	/**
	 * Speak and then collect data from the response.
	 * 
	 * <p>The {@code data} will be written to the serial port's
	 * {@link OutputStream} and then this method will block until the
	 * {@link #getMagic()} bytes are read, followed by {@link #getReadSize()}
	 * more bytes. Each invocation of this method will first
	 * clear the internal data buffer, and all received response data
	 * will be stored on the internal data buffer. Calling code can
	 * access this buffer by calling {@link #getCollectedData()}.</p>
	 * 
	 * @param data the data to write to the serial port
	 * @param magicBytes the magic bytes to look for in the response
	 * @param readLength the number of bytes to read, including the magic
	 */
	@Override
	public void speakAndCollect(byte[] data, byte[] magicBytes, int readLength) {
		try {
			// sleep until we have data
			synchronized (this) {
				buffer.reset();
				magic = magicBytes;
				readSize = readLength;
				listening = true;
				collecting = true;
				this.doneCollecting = false;
				out.write(data);
				
				this.wait(getMaxWait());
			}
			if ( log.isWarnEnabled() && !doneCollecting ) {
				log.warn("Timeout collecting serial data");
			}
		} catch ( InterruptedException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			this.listening = false;
			this.collecting = false;
		}
	}

	@Override
	public void speak(byte[] data) {
		try {
			out.write(data);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		eventLog.trace("SerialPortEvent {}", event.getEventType());
		if ( !listening || event.getEventType() != SerialPortEvent.DATA_AVAILABLE) {
			return;
		}
		if ( collecting ) {
			boolean done = handleSerialEvent(event, in, buffer, magic, readSize);
			if ( done ) {
				synchronized (this) {
					doneCollecting = true;
					notifyAll();
				}
			}
			return;
		}
		byte[] buf = new byte[1024];
		try {
			int len = -1;
			while ( (len = in.read(buf, 0, buf.length)) > 0 ) {
				this.buffer.write(buf, 0, len);
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}

		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("Finished reading data: {}", Arrays.toString(buffer.toByteArray()));
		}
		synchronized (this) {
			doneCollecting = true;
			notifyAll();
		}
	}

	@Override
	public int bytesRead() {
		return buffer.size();
	}

	@Override
	public byte[] getCollectedData() {
		return buffer.toByteArray();
	}

	@Override
	public String getCollectedDataAsString() {
		return buffer.toString();
	}

	@Override
	public void stopCollecting() {
		closeSerialPort();
	}

}
