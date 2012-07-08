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

	private final long TIMEOUT_PADDING = 800;
	
	private byte[] magic;
	private int readSize;

	private DataListener listener;
	private InputStream in;
	private OutputStream out;
	private ByteArrayOutputStream buffer;
	private boolean listening = false;
	private boolean collecting = false;

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
				timeoutStart();
				out.write(data);
				out.flush();
				this.wait(getMaxWait()+TIMEOUT_PADDING);
			}
		} catch ( InterruptedException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			this.listening = false;
		}
	}

	@Override
	public void listen() {
		try {
			synchronized (this) {
				this.buffer.reset();
				listening = true;
				collecting = false;
				timeoutStart();
				this.wait(getMaxWait()+TIMEOUT_PADDING);
			}
		} catch ( InterruptedException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setListener(DataListener listener) {
		this.listener = listener;
	}

	@Override
	public void removeListener() {
		this.listener = null;
	}

	@Override
	public void listen(DataListener listener) {
		setListener(listener);
		listen();
	}

	@Override
	public void speakAndListen(byte[] data, DataListener listener) {
		setListener(listener);
		speakAndListen(data);
	}

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
				timeoutStart();
				out.write(data);
				out.flush();
				
				this.wait(getMaxWait()+TIMEOUT_PADDING);
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
			synchronized (this) {
				timeoutClear();
				out.write(data);
				out.flush();
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("SerialPortEvent {}; listening {}; collecting {}", 
				new Object[] {event.getEventType(), listening, collecting});
		}
		if ( !listening || event.getEventType() != SerialPortEvent.DATA_AVAILABLE) {
			return;
		}
		if ( collecting ) {
			boolean done;
			try {
				done = handleSerialEvent(event, in, buffer, magic, readSize);
			} catch (Exception e) {
				done = true;
			}
			if ( done ) {
				synchronized (this) {
					notifyAll();
				}
			}
			return;
		}
		read(in, this.buffer, this.listener);
		synchronized (this) {
			notifyAll();
		}
	}

	protected final void read(InputStream in, ByteArrayOutputStream sink, DataListener listener) {
		final byte[] buf = new byte[1024];
		try {
			int len = -1;
			boolean reading = true;
			while ( reading ) {
				final int sinkSize = sink.size();
				final int readSize = (listener == null 
						? buf.length 
						: Math.min(buf.length, listener.getDesiredByteCount(this, sinkSize)));
				if ( (len = in.read(buf, 0, readSize)) > 0 ) {
					if ( listener == null ) {
						sink.write(buf, 0, len);
						reading = false;
					} else {
						if ( len > 0 && !listener.receivedData(this, buf, 0, len, sink, sink.size()) ) {
							reading = false;;
						}
					}
				} else if ( listener == null ) {
					reading = false;
				}
			}
		} catch ( IOException e ) {
			log.warn("IOException reading serial data: {}", e.getMessage());
		}
		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("Finished reading data: {}", asciiDebugValue(sink.toByteArray()));
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
