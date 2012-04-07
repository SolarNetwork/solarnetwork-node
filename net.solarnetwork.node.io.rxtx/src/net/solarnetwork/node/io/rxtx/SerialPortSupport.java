/* ===================================================================
 * SerialPortSupport.java
 * 
 * Created Aug 19, 2009 11:25:27 AM
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.TooManyListenersException;

import net.solarnetwork.node.support.SerialPortBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * A base class with properties to support {@link SerialPort} communication.
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public abstract class SerialPortSupport extends SerialPortBean {

	/** The SerialPort. */
	protected SerialPort serialPort;
	
	private long maxWait;
	
	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Constructor.
	 * 
	 * @param serialPort the SerialPort to use
	 * @param maxWait the maximum number of milliseconds to wait when waiting
	 * to read data
	 */
	public SerialPortSupport(SerialPort serialPort, long maxWait) {
		this.serialPort = serialPort;
		this.maxWait = maxWait;
	}
	
	/**
	 * Close the connected serial port.
	 */
	protected void closeSerialPort() {
		if ( this.serialPort != null ) {
			this.serialPort.close();
		}
	}

	/**
	 * Set up the SerialPort for use, configuring with class properties.
	 * 
	 * <p>This method can be called once when wanting to start using the 
	 * serial port.</p>
	 * 
	 * @param listener a listener to pass to {@link SerialPort#addEventListener(SerialPortEventListener)}
	 */
	protected void setupSerialPortParameters(SerialPortEventListener listener) {
		if ( listener != null ) {
			try {
				serialPort.addEventListener(listener);
			} catch ( TooManyListenersException e ) {
				throw new RuntimeException(e);
			}
		}

		serialPort.notifyOnDataAvailable(true);

		try {

			if ( getReceiveFraming() >= 0 ) {
				serialPort.enableReceiveFraming(getReceiveFraming());
				if ( !serialPort.isReceiveFramingEnabled() ) {
					log.warn("Receive framing configured as {} but not supported by driver."
							, getReceiveFraming());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive framing set to {}", getReceiveFraming());
				}
			} else {
				serialPort.disableReceiveFraming();
			}
			
			if ( getReceiveTimeout() >= 0 ) {
				serialPort.enableReceiveTimeout(getReceiveTimeout());
				if ( !serialPort.isReceiveTimeoutEnabled() ) {
					log.warn("Receive timeout configured as {} but not supported by driver."
							, getReceiveTimeout());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive timeout set to {}", getReceiveTimeout());
				}
			} else {
				serialPort.disableReceiveTimeout();
			}
			if ( getReceiveThreshold() >= 0 ) {
				serialPort.enableReceiveThreshold(getReceiveThreshold());
				if ( !serialPort.isReceiveThresholdEnabled() ) {
					log.warn("Receive threshold configured as [{}] but not supported by driver."
							, getReceiveThreshold());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive threshold set to {}", getReceiveThreshold());
				}
			} else {
				serialPort.disableReceiveThreshold();
			}

			serialPort.setSerialPortParams(getBaud(), getDataBits(),
					getStopBits(), getParity());

			if ( getFlowControl() >= 0 ) {
				serialPort.setFlowControlMode(getFlowControl());
			}

			if ( getDtrFlag() >= 0 ) {
				boolean mode = getDtrFlag() > 0 ? true : false;
				log.debug("Setting DTR to {}", mode);
				serialPort.setDTR(mode);
			}
			if ( getRtsFlag() >= 0 ) {
				boolean mode = getRtsFlag() > 0 ? true : false;
				log.debug("Setting RTS to {}", mode);
				serialPort.setRTS(mode);
			}
			
		} catch ( UnsupportedCommOperationException e ) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Handle a SerialEvent, looking for "magic" data.
	 * 
	 * @param event the event
	 * @param in the InputStream to read data from
	 * @param sink the output buffer to store the collected bytes
	 * @param magicBytes the "magic" bytes to look for in the event stream
	 * @param readLength the number of bytes, excluding the magic bytes, to
	 * read from the stream
	 * @return <em>true</em> if the data has been found
	 */
	protected boolean handleSerialEvent(SerialPortEvent event, InputStream in,
			ByteArrayOutputStream sink, byte[] magicBytes, int readLength) {
		int sinkSize = sink.size();
		boolean append = sinkSize > 0;
		byte[] buf = new byte[1024];
		try {
			int len = -1;
			while ( (len = in.read(buf, 0, buf.length)) > 0 ) {
				sink.write(buf, 0, len);
				sinkSize += len;

				if ( append ) {
					// if we've collected at least desiredSize bytes, we're done
					if ( sinkSize >= readLength ) {
						if ( log.isDebugEnabled() ) {
							log.debug("Got desired " +readLength +" bytes of data");
						}
						synchronized (this) {
							notifyAll();
							return true;
						}
					}
					if ( log.isDebugEnabled() ) {
						log.debug("Looking for " +(readLength - sinkSize) +" more bytes of data");
					}
					return false;
				}
				
				// look for magic in the buffer
				int magicIdx = 0;
				byte[] sinkBuf = sink.toByteArray();
				boolean found = false;
				for ( ; magicIdx < (sinkBuf.length - magicBytes.length); magicIdx++ ) {
					found = true;
					for ( int j = 0; j < magicBytes.length; j++ ) {
						if ( sinkBuf[magicIdx+j] != magicBytes[j] ) {
							found = false;
							break;
						}
					}
					if ( found ) {
						break;
					}
				}

				sink.reset();

				if ( found ) {
					// magic found!
					if ( log.isTraceEnabled() ) {
						log.trace("Found magic bytes " +Arrays.toString(magicBytes)
								+" at buffer index " +magicIdx);
					}
					
					// skip over magic bytes
					magicIdx += magicBytes.length;
					
					int count = readLength;
					count = Math.min(readLength, sinkBuf.length - magicIdx);
					sink.write(sinkBuf, magicIdx, count);
					sinkSize += count;
					if ( sinkSize >= readLength ) {
						// we got all the data here... we're done
						synchronized (this) {
							notifyAll();
						}
						return true;
					}
				}
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("Buffer: " +sink.toString());
		}
		return false;
	}

	public SerialPort getSerialPort() {
		return serialPort;
	}
	public long getMaxWait() {
		return maxWait;
	}
	
}
