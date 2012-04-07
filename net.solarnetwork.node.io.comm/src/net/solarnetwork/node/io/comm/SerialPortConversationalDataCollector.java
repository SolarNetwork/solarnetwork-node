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

package net.solarnetwork.node.io.comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;

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
public class SerialPortConversationalDataCollector<T> extends SerialPortSupport
implements ConversationalDataCollector<T>, SerialPortEventListener {

	private InputStream in;
	private OutputStream out;
	private ByteArrayOutputStream buffer;
	private boolean listening = false;
	
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

	/* (non-Javadoc)
	 * @see net.sf.solarnetwork.node.ConversationalDataCollector#collectData(net.sf.solarnetwork.node.ConversationalDataCollector.Moderator)
	 */
	public T collectData(Moderator<T> moderator) {
		setupSerialPortParameters(this);
		
		// open the input stream
		try {
			this.out = serialPort.getOutputStream();
			this.in = serialPort.getInputStream();
			
			return moderator.conductConversation(this);
			
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
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
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.solarnetwork.node.DataCollector#collectData()
	 */
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
			this.buffer.reset();
			this.listening = true;
			out.write(data);
			
			// sleep until we have data
			synchronized (this) {
				this.wait(getMaxWait());
			}
			this.listening = false;
		} catch ( InterruptedException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see net.sf.solarnetwork.node.ConversationalDataCollector#speak(byte[])
	 */
	public void speak(byte[] data) {
		try {
			out.write(data);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see javax.comm.SerialPortEventListener#serialEvent(javax.comm.SerialPortEvent)
	 */
	public void serialEvent(SerialPortEvent event) {
		if ( log.isTraceEnabled() ) {
			log.trace("SerialPortEvent: " +event);
		}
		if ( !listening || event.getEventType() != SerialPortEvent.DATA_AVAILABLE) {
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

		if ( log.isTraceEnabled() ) {
			log.trace("Finished reading data");
		}
		synchronized (this) {
			notifyAll();
			return;
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.solarnetwork.node.DataCollector#bytesRead()
	 */
	public int bytesRead() {
		return buffer.size();
	}

	/* (non-Javadoc)
	 * @see net.sf.solarnetwork.node.DataCollector#getCollectedData()
	 */
	public byte[] getCollectedData() {
		return buffer.toByteArray();
	}

	/* (non-Javadoc)
	 * @see net.sf.solarnetwork.node.DataCollector#getCollectedDataAsString()
	 */
	public String getCollectedDataAsString() {
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see net.sf.solarnetwork.node.DataCollector#stopCollecting()
	 */
	public void stopCollecting() {
		closeSerialPort();
	}

}
