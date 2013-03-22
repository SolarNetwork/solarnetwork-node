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

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.TooManyListenersException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.solarnetwork.node.support.SerialPortBean;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class with properties to support {@link SerialPort} communication.
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public abstract class SerialPortSupport extends SerialPortBean {

	/** The SerialPort. */
	protected SerialPort serialPort;

	private final long maxWait;
	private final ExecutorService executor;
	private long timeout = 0;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** A class-level logger with the suffix SERIAL_EVENT. */
	protected final Logger eventLog = LoggerFactory.getLogger(getClass().getName() + ".SERIAL_EVENT");

	/**
	 * Constructor.
	 * 
	 * @param serialPort
	 *        the SerialPort to use
	 * @param maxWait
	 *        the maximum number of milliseconds to wait when waiting to read
	 *        data
	 */
	public SerialPortSupport(SerialPort serialPort, long maxWait) {
		this.serialPort = serialPort;
		this.maxWait = maxWait;
		if ( maxWait > 0 ) {
			executor = Executors.newFixedThreadPool(1);
		} else {
			executor = null;
		}
	}

	/**
	 * Set a "timeout" flag, so that all subsequent calls to
	 * {@link #handleSerialEvent(SerialPortEvent, InputStream, ByteArrayOutputStream, byte[], int)}
	 * use this as the reference point for calculating the maximum time to wait
	 * for serial data.
	 * 
	 * <p>
	 * When called, the {@code handleSerialEvent} method will treat the time
	 * offset from the call to this method as the reference amount of time that
	 * has passed before the {@code maxWait} value triggers a timeout.
	 * </p>
	 */
	protected void timeoutStart() {
		if ( maxWait > 0 ) {
			timeout = System.currentTimeMillis();
		}
	}

	/**
	 * Clear the timeout flag, so no timeout used.
	 * 
	 * @see #timeoutStart()
	 */
	protected void timeoutClear() {
		timeout = 0;
	}

	/**
	 * Close the connected serial port.
	 */
	protected void closeSerialPort() {
		if ( this.serialPort != null ) {
			log.debug("Closing serial port {}", this.serialPort);
			this.serialPort.close();
			log.trace("Serial port closed");
			if ( executor != null ) {
				executor.shutdownNow();
			}
		}
	}

	/**
	 * Set up the SerialPort for use, configuring with class properties.
	 * 
	 * <p>
	 * This method can be called once when wanting to start using the serial
	 * port.
	 * </p>
	 * 
	 * @param listener
	 *        a listener to pass to
	 *        {@link SerialPort#addEventListener(SerialPortEventListener)}
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
					log.warn("Receive framing configured as {} but not supported by driver.",
							getReceiveFraming());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive framing set to {}", getReceiveFraming());
				}
			} else {
				serialPort.disableReceiveFraming();
			}

			if ( getReceiveTimeout() >= 0 ) {
				serialPort.enableReceiveTimeout(getReceiveTimeout());
				if ( !serialPort.isReceiveTimeoutEnabled() ) {
					log.warn("Receive timeout configured as {} but not supported by driver.",
							getReceiveTimeout());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive timeout set to {}", getReceiveTimeout());
				}
			} else {
				serialPort.disableReceiveTimeout();
			}
			if ( getReceiveThreshold() >= 0 ) {
				serialPort.enableReceiveThreshold(getReceiveThreshold());
				if ( !serialPort.isReceiveThresholdEnabled() ) {
					log.warn("Receive threshold configured as [{}] but not supported by driver.",
							getReceiveThreshold());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive threshold set to {}", getReceiveThreshold());
				}
			} else {
				serialPort.disableReceiveThreshold();
			}

			if ( log.isDebugEnabled() ) {
				log.debug("Setting serial port baud = {}, dataBits = {}, stopBits = {}, parity = {}",
						new Object[] { getBaud(), getDataBits(), getStopBits(), getParity() });
			}
			serialPort.setSerialPortParams(getBaud(), getDataBits(), getStopBits(), getParity());

			if ( getFlowControl() >= 0 ) {
				log.debug("Setting flow control to {}", getFlowControl());
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
	 * <p>
	 * <b>Note</b> that the <em>magic</em> bytes are <em>not</em> returned by
	 * this method, they are stripped from the output buffer.
	 * </p>
	 * 
	 * @param event
	 *        the event
	 * @param in
	 *        the InputStream to read data from
	 * @param sink
	 *        the output buffer to store the collected bytes
	 * @param magicBytes
	 *        the "magic" bytes to look for in the event stream
	 * @param readLength
	 *        the number of bytes, excluding the magic bytes, to read from the
	 *        stream
	 * @return <em>true</em> if the data has been found
	 * @throws TimeoutException
	 *         if {@code maxWait} is configured and that amount of time passes
	 *         before the requested serial data is read
	 */
	protected boolean handleSerialEvent(final SerialPortEvent event, final InputStream in,
			final ByteArrayOutputStream sink, final byte[] magicBytes, final int readLength)
			throws TimeoutException, InterruptedException, ExecutionException {
		if ( timeout < 1 ) {
			return handleSerialEventWithoutTimeout(event, in, sink, magicBytes, readLength);
		}

		Callable<Boolean> task = new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return handleSerialEventWithoutTimeout(event, in, sink, magicBytes, readLength);
			}
		};
		Future<Boolean> future = executor.submit(task);
		boolean result = false;
		final long maxMs = Math.max(1, this.maxWait - System.currentTimeMillis() + timeout);
		eventLog.trace("Waiting at most {}ms for data", maxMs);
		try {
			result = future.get(maxMs, TimeUnit.MILLISECONDS);
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for serial data");
			throw e;
		} catch ( ExecutionException e ) {
			// log stack trace in DEBUG
			log.debug("Exception thrown reading from serial port", e.getCause());
			throw e;
		} catch ( TimeoutException e ) {
			log.warn("Timeout waiting {}ms for serial data, aborting read", maxMs);
			future.cancel(true);
			throw e;
		}
		return result;
	}

	private boolean handleSerialEventWithoutTimeout(SerialPortEvent event, InputStream in,
			ByteArrayOutputStream sink, byte[] magicBytes, int readLength) {
		int sinkSize = sink.size();
		boolean append = sinkSize > 0;
		byte[] buf = new byte[Math.min(readLength, 1024)];
		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("Sink contains {} bytes: {}", sinkSize, asciiDebugValue(sink.toByteArray()));
		}
		try {
			int len = -1;
			final int max = Math.min(in.available(), buf.length);
			eventLog.trace("Attempting to read {} bytes from serial port", max);
			while ( max > 0 && (len = in.read(buf, 0, max)) > 0 ) {
				sink.write(buf, 0, len);
				sinkSize += len;

				if ( append ) {
					// if we've collected at least desiredSize bytes, we're done
					if ( sinkSize >= readLength ) {
						if ( eventLog.isDebugEnabled() ) {
							eventLog.debug("Got desired {}  bytes of data: {}", readLength,
									asciiDebugValue(sink.toByteArray()));
						}
						return true;
					}
					eventLog.debug("Looking for {} more bytes of data", (readLength - sinkSize));
					return false;
				} else {
					eventLog.trace("Looking for {} magic bytes 0x{}", magicBytes.length,
							Hex.encodeHexString(magicBytes));
				}

				// look for magic in the buffer
				int magicIdx = 0;
				byte[] sinkBuf = sink.toByteArray();
				boolean found = false;
				for ( ; magicIdx < (sinkBuf.length - magicBytes.length); magicIdx++ ) {
					found = true;
					for ( int j = 0; j < magicBytes.length; j++ ) {
						if ( sinkBuf[magicIdx + j] != magicBytes[j] ) {
							found = false;
							break;
						}
					}
					if ( found ) {
						break;
					}
				}

				sink.reset();
				sinkSize = 0;

				if ( found ) {
					// magic found!
					if ( eventLog.isTraceEnabled() ) {
						eventLog.trace("Found magic bytes " + asciiDebugValue(magicBytes)
								+ " at buffer index " + magicIdx);
					}

					// skip over magic bytes
					magicIdx += magicBytes.length;

					int count = readLength;
					count = Math.min(readLength, sinkBuf.length - magicIdx);
					sink.write(sinkBuf, magicIdx, count);
					sinkSize += count;
					if ( eventLog.isTraceEnabled() ) {
						eventLog.trace("Sink contains {} bytes: {}", sinkSize,
								asciiDebugValue(sink.toByteArray()));
					}
					if ( sinkSize >= readLength ) {
						// we got all the data here... we're done
						return true;
					}
					eventLog.trace("Need {} more bytes of data", (readLength - sinkSize));
					append = true;
				}
			}
		} catch ( IOException e ) {
			log.error("Error reading from serial port: {}", e.getMessage());
			throw new RuntimeException(e);
		}

		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("Need {} more bytes of data, buffer: {}", (readLength - sinkSize),
					asciiDebugValue(sink.toByteArray()));
		}
		return false;
	}

	protected final void readAvailable(InputStream in, ByteArrayOutputStream sink) {
		byte[] buf = new byte[1024];
		try {
			int len = -1;
			while ( in.available() > 0 && (len = in.read(buf, 0, buf.length)) > 0 ) {
				sink.write(buf, 0, len);
			}
		} catch ( IOException e ) {
			log.warn("IOException reading serial data: {}", e.getMessage());
		}
		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("Finished reading data: {}", asciiDebugValue(sink.toByteArray()));
		}
	}

	protected final String asciiDebugValue(byte[] data) {
		if ( data == null || data.length < 1 ) {
			return "";
		}
		StringBuilder buf = new StringBuilder();
		buf.append(Hex.encodeHex(data)).append(" (");
		for ( byte b : data ) {
			if ( b >= 32 && b < 126 ) {
				buf.append(Character.valueOf((char) b));
			} else {
				buf.append('~');
			}
		}
		buf.append(")");
		return buf.toString();
	}

	public SerialPort getSerialPort() {
		return serialPort;
	}

	public long getMaxWait() {
		return maxWait;
	}

}
