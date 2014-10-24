/* ==================================================================
 * SerialPortConnection.java - Oct 23, 2014 2:21:31 PM
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

package net.solarnetwork.node.io.serial.rxtx;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.solarnetwork.node.LockTimeoutException;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RXTX implentation of {@link SerialConnection}.
 * 
 * @author matt
 * @version 1.0
 */
public class SerialPortConnection implements SerialConnection, SerialPortEventListener {

	/** A class-level logger. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	/** A class-level logger with the suffix SERIAL_EVENT. */
	private final Logger eventLog = LoggerFactory.getLogger(getClass().getName() + ".SERIAL_EVENT");

	private final SerialPortBeanParameters serialParams;
	private final long maxWait;
	private final ExecutorService executor;
	private long timeout = 0;

	private SerialPort serialPort;
	private InputStream in;
	private OutputStream out;
	private ByteArrayOutputStream buffer;
	private final boolean listening = false;
	private final boolean collecting = false;

	/**
	 * Constructor.
	 * 
	 * @param serialPort
	 *        the SerialPort to use
	 * @param serialParams
	 *        the parameters to use with the SerialPort
	 * @param maxWait
	 *        the maximum number of milliseconds to wait when waiting to read
	 *        data
	 */
	public SerialPortConnection(SerialPortBeanParameters params, long maxWait) {
		this.serialParams = params;
		this.maxWait = maxWait;
		if ( maxWait > 0 ) {
			executor = Executors.newFixedThreadPool(1);
		} else {
			executor = null;
		}
	}

	@Override
	public void open() throws IOException, LockTimeoutException {
		CommPortIdentifier portId = getCommPortIdentifier(serialParams.getSerialPort());
		try {
			serialPort = (SerialPort) portId.open(serialParams.getCommPortAppName(), 2000);
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
			setupSerialPortParameters(this);
		} catch ( PortInUseException e ) {
			throw new IOException("Serial port " + serialParams.getSerialPort() + " in use", e);
		} catch ( TooManyListenersException e ) {
			try {
				close();
			} catch ( Exception e2 ) {
				// ignore this
			}
			throw new IOException("Serial port " + serialParams.getSerialPort()
					+ " has too many listeners", e);
		}
	}

	/**
	 * Test if the serial port has been opened.
	 * 
	 * @return boolean
	 */
	public boolean isOpen() {
		return (serialPort != null);
	}

	@Override
	public void close() {
		if ( serialPort == null ) {
			return;
		}
		try {
			log.debug("Closing serial port {}", this.serialPort);
			serialPort.close();
			log.trace("Serial port closed");
		} finally {
			if ( executor != null ) {
				try {
					// FIXME: this means it can't be started up again... should that be allowed?
					executor.shutdownNow();
				} catch ( Exception e ) {
					log.debug("Exception shutting down Executor", e);
				}
			}
			serialPort = null;
		}
	}

	@Override
	public byte[] readMarkedMessage(final byte[] startMarker, final byte[] endMarker) throws IOException {
		final InputStream in = getInputStream();
		final ByteArrayOutputStream sink = new ByteArrayOutputStream(1024);
		boolean result = false;
		if ( maxWait < 1 ) {
			do {
				result = readMarkedMessage(in, sink, startMarker, endMarker);
			} while ( !result );
			return sink.toByteArray();
		}

		Callable<Boolean> task = new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				boolean found = false;
				do {
					found = readMarkedMessage(in, sink, startMarker, endMarker);
				} while ( !found );
				return found;
			}
		};
		timeoutStart();
		Future<Boolean> future = executor.submit(task);
		final long maxMs = Math.max(1, this.maxWait - System.currentTimeMillis() + timeout);
		eventLog.trace("Waiting at most {}ms for data", maxMs);
		try {
			result = future.get(maxMs, TimeUnit.MILLISECONDS);
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for serial data");
			throw new IOException("Interrupted waiting for serial data", e);
		} catch ( ExecutionException e ) {
			// log stack trace in DEBUG
			log.debug("Exception thrown reading from serial port", e.getCause());
			throw new IOException("Exception thrown reading from serial port", e.getCause());
		} catch ( TimeoutException e ) {
			log.warn("Timeout waiting {}ms for serial data, aborting read", maxMs);
			future.cancel(true);
			throw new IOException("Timeout waiting {}ms for serial data", e.getCause());
		}
		return (result ? sink.toByteArray() : null);
	}

	private boolean readMarkedMessage(final InputStream in, final ByteArrayOutputStream sink,
			final byte[] startMarker, final byte[] endMarker) {
		int sinkSize = sink.size();
		boolean append = sinkSize > 0;
		byte[] buf = new byte[1024];
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
					// look for eofBytes, starting where we last appended
					if ( findEndMarkerBytes(sink, len, endMarker) ) {
						if ( eventLog.isDebugEnabled() ) {
							eventLog.debug("Found desired end marker {}", asciiDebugValue(endMarker));
						}
						return true;
					}
					eventLog.debug("Looking for end marker {}", asciiDebugValue(endMarker));
					return false;
				} else {
					eventLog.trace("Looking for {} start marker {} in buffer {}",
							new Object[] { startMarker.length, asciiDebugValue(startMarker),
									asciiDebugValue(sink.toByteArray()) });
				}

				// look for magic in the buffer
				int magicIdx = 0;
				byte[] sinkBuf = sink.toByteArray();
				boolean found = false;
				for ( ; magicIdx < (sinkBuf.length - startMarker.length + 1); magicIdx++ ) {
					found = true;
					for ( int j = 0; j < startMarker.length; j++ ) {
						if ( sinkBuf[magicIdx + j] != startMarker[j] ) {
							found = false;
							break;
						}
					}
					if ( found ) {
						break;
					}
				}

				if ( found ) {
					// magic found!
					if ( eventLog.isTraceEnabled() ) {
						eventLog.trace("Found start marker " + asciiDebugValue(startMarker)
								+ " at buffer index " + magicIdx);
					}

					int count = buf.length;
					count = Math.min(buf.length, sinkBuf.length - magicIdx);
					sink.reset();
					sink.write(sinkBuf, magicIdx, count);
					sinkSize = count;
					if ( eventLog.isTraceEnabled() ) {
						eventLog.trace("Sink contains {} bytes: {}", sinkSize,
								asciiDebugValue(sink.toByteArray()));
					}
					if ( findEndMarkerBytes(sink, len, endMarker) ) {
						// we got all the data here... we're done
						return true;
					}
					append = true;
				} else if ( sinkBuf.length > startMarker.length ) {
					// haven't found the magic yet, and the sink is larger than magic size, so 
					// trim sink down to just magic size
					sink.reset();
					sink.write(sinkBuf, sinkBuf.length - startMarker.length, startMarker.length);
					sinkSize = startMarker.length;
				}
			}
		} catch ( IOException e ) {
			log.error("Error reading from serial port: {}", e.getMessage());
			throw new RuntimeException(e);
		}

		if ( eventLog.isTraceEnabled() ) {
			eventLog.debug("Looking for marker {}, buffer: {}", (append ? asciiDebugValue(endMarker)
					: asciiDebugValue(startMarker)), asciiDebugValue(sink.toByteArray()));
		}
		return false;
	}

	private InputStream getInputStream() throws IOException {
		if ( in != null ) {
			return in;
		}
		if ( !isOpen() ) {
			open();
		}
		in = serialPort.getInputStream();
		return in;
	}

	@SuppressWarnings("unchecked")
	private CommPortIdentifier getCommPortIdentifier(final String portId) throws IOException {
		// first try directly
		CommPortIdentifier commPortId = null;
		try {
			commPortId = CommPortIdentifier.getPortIdentifier(portId);
			if ( commPortId != null ) {
				log.debug("Found port identifier: {}", portId);
				return commPortId;
			}
		} catch ( NoSuchPortException e ) {
			log.debug("Port {} not found, inspecting available ports...", portId);
		}
		Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier.getPortIdentifiers();
		List<String> foundNames = new ArrayList<String>(5);
		while ( portIdentifiers.hasMoreElements() ) {
			commPortId = portIdentifiers.nextElement();
			log.trace("Inspecting available port identifier: {}", commPortId.getName());
			foundNames.add(commPortId.getName());
			if ( commPortId.getPortType() == CommPortIdentifier.PORT_SERIAL
					&& portId.equals(commPortId.getName()) ) {
				log.debug("Found port identifier: {}", portId);
				break;
			}
		}
		if ( commPortId == null ) {
			throw new IOException("Couldn't find port identifier for [" + portId
					+ "]; available ports: " + foundNames);
		}
		return commPortId;
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("SerialPortEvent {}; listening {}; collecting {}",
					new Object[] { event.getEventType(), listening, collecting });
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
	private void setupSerialPortParameters(SerialPortEventListener listener) {
		if ( listener != null ) {
			try {
				serialPort.addEventListener(listener);
			} catch ( TooManyListenersException e ) {
				throw new RuntimeException(e);
			}
		}

		serialPort.notifyOnDataAvailable(true);

		try {

			if ( serialParams.getReceiveFraming() >= 0 ) {
				serialPort.enableReceiveFraming(serialParams.getReceiveFraming());
				if ( !serialPort.isReceiveFramingEnabled() ) {
					log.warn("Receive framing configured as {} but not supported by driver.",
							serialParams.getReceiveFraming());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive framing set to {}", serialParams.getReceiveFraming());
				}
			} else {
				serialPort.disableReceiveFraming();
			}

			if ( serialParams.getReceiveTimeout() >= 0 ) {
				serialPort.enableReceiveTimeout(serialParams.getReceiveTimeout());
				if ( !serialPort.isReceiveTimeoutEnabled() ) {
					log.warn("Receive timeout configured as {} but not supported by driver.",
							serialParams.getReceiveTimeout());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive timeout set to {}", serialParams.getReceiveTimeout());
				}
			} else {
				serialPort.disableReceiveTimeout();
			}
			if ( serialParams.getReceiveThreshold() >= 0 ) {
				serialPort.enableReceiveThreshold(serialParams.getReceiveThreshold());
				if ( !serialPort.isReceiveThresholdEnabled() ) {
					log.warn("Receive threshold configured as [{}] but not supported by driver.",
							serialParams.getReceiveThreshold());
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive threshold set to {}", serialParams.getReceiveThreshold());
				}
			} else {
				serialPort.disableReceiveThreshold();
			}

			if ( log.isDebugEnabled() ) {
				log.debug(
						"Setting serial port baud = {}, dataBits = {}, stopBits = {}, parity = {}",
						new Object[] { serialParams.getBaud(), serialParams.getDataBits(),
								serialParams.getStopBits(), serialParams.getParity() });
			}
			serialPort.setSerialPortParams(serialParams.getBaud(), serialParams.getDataBits(),
					serialParams.getStopBits(), serialParams.getParity());

			if ( serialParams.getFlowControl() >= 0 ) {
				log.debug("Setting flow control to {}", serialParams.getFlowControl());
				serialPort.setFlowControlMode(serialParams.getFlowControl());
			}

			if ( serialParams.getDtrFlag() >= 0 ) {
				boolean mode = serialParams.getDtrFlag() > 0 ? true : false;
				log.debug("Setting DTR to {}", mode);
				serialPort.setDTR(mode);
			}
			if ( serialParams.getRtsFlag() >= 0 ) {
				boolean mode = serialParams.getRtsFlag() > 0 ? true : false;
				log.debug("Setting RTS to {}", mode);
				serialPort.setRTS(mode);
			}

		} catch ( UnsupportedCommOperationException e ) {
			throw new RuntimeException(e);
		}
	}

	private boolean findEndMarkerBytes(final ByteArrayOutputStream sink, final int appendedLength,
			final byte[] endMarker) {
		byte[] sinkBuf = sink.toByteArray();
		int eofIdx = Math.max(0, sinkBuf.length - appendedLength - endMarker.length);
		boolean foundEOF = false;
		for ( ; eofIdx < (sinkBuf.length - endMarker.length); eofIdx++ ) {
			foundEOF = true;
			for ( int j = 0; j < endMarker.length; j++ ) {
				if ( sinkBuf[eofIdx + j] != endMarker[j] ) {
					foundEOF = false;
					break;
				}
			}
			if ( foundEOF ) {
				break;
			}
		}
		if ( foundEOF ) {
			if ( eventLog.isDebugEnabled() ) {
				eventLog.debug("Found desired {} end marker bytes at index {}",
						asciiDebugValue(endMarker), eofIdx);
			}
			sink.reset();
			sink.write(sinkBuf, 0, eofIdx + endMarker.length);
			if ( eventLog.isDebugEnabled() ) {
				eventLog.debug("Buffer message at end marker: {}", asciiDebugValue(sink.toByteArray()));
			}
			return true;
		}
		eventLog.debug("Looking for end marker bytes {}", asciiDebugValue(endMarker));
		return false;
	}

	/**
	 * Read from the InputStream until it is empty.
	 * 
	 * @param in
	 */
	private void drainInputStream(InputStream in) {
		byte[] buf = new byte[1024];
		int len = -1;
		int total = 0;
		try {
			final int max = Math.min(in.available(), buf.length);
			eventLog.trace("Attempting to drain {} bytes from serial port", max);
			while ( max > 0 && (len = in.read(buf, 0, max)) > 0 ) {
				// keep draining
				total += len;
			}
		} catch ( IOException e ) {
			// ignore this
		}
		eventLog.trace("Drained {} bytes from serial port", total);
	}

	private String asciiDebugValue(byte[] data) {
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
