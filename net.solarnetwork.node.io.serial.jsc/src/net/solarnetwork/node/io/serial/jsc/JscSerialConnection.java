/* ==================================================================
 * JscSerialConnection.java - 31/08/2020 7:36:00 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.serial.jsc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fazecast.jSerialComm.SerialPort;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.service.LockTimeoutException;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;
import net.solarnetwork.util.ByteList;
import net.solarnetwork.util.ObjectUtils;

/**
 * PureJavaComm implementation of {@link SerialConnection}.
 * 
 * @author matt
 * @version 1.0
 */
public class JscSerialConnection implements SerialConnection {

	/** A class-level logger. */
	private static final Logger log = LoggerFactory.getLogger(JscSerialConnection.class);

	/** A class-level logger with the suffix SERIAL_EVENT. */
	private static final Logger eventLog = LoggerFactory
			.getLogger(JscSerialConnection.class.getName() + ".SERIAL_EVENT");

	private final SerialPortBeanParameters serialParams;
	private final ExecutorService executor;

	private SerialPort serialPort;
	private InputStream in;
	private OutputStream out;

	/**
	 * Constructor.
	 * 
	 * @param serialParams
	 *        the parameters to use with the SerialPort
	 * @param executor
	 *        A thread pool to use for I/O tasks with timeouts.
	 */
	public JscSerialConnection(SerialPortBeanParameters serialParams, ExecutorService executor) {
		super();
		this.serialParams = ObjectUtils.requireNonNullArgument(serialParams, "serialParams");
		this.executor = ObjectUtils.requireNonNullArgument(executor, "executor");
	}

	@Override
	public String getPortName() {
		return (serialParams != null ? serialParams.getSerialPort() : null);
	}

	@Override
	public void open() throws IOException, LockTimeoutException {
		if ( serialPort == null ) {
			try {
				serialPort = SerialPort.getCommPort(serialParams.getSerialPort());
				setupSerialPortParameters(serialPort);
				if ( !serialPort.openPort() ) {
					throw new IOException(
							"Serial port " + serialParams.getSerialPort() + " failed to open");
				}
			} catch ( RuntimeException e ) {
				try {
					close();
				} catch ( Exception e2 ) {
					// ignore this
				}
				throw new IOException(
						"Error opening serial port " + serialParams.getSerialPort() + ":" + e.toString(),
						e);
			}
		}
	}

	/**
	 * Test if the serial port has been opened.
	 * 
	 * @return boolean
	 */
	public boolean isOpen() {
		return (serialPort != null && serialPort.isOpen());
	}

	private void setupSerialPortParameters(SerialPort serialPort) {
		if ( log.isDebugEnabled() ) {
			log.debug("Setting serial port baud = {}, dataBits = {}, stopBits = {}, parity = {}",
					new Object[] { serialParams.getBaud(), serialParams.getDataBits(),
							serialParams.getStopBits(), serialParams.getParity() });
		}
		serialPort.setComPortParameters(serialParams.getBaud(), serialParams.getDataBits(),
				serialParams.getStopBits(), serialParams.getParity());

		if ( serialParams.getFlowControl() >= 0 ) {
			log.debug("Setting flow control to {}", serialParams.getFlowControl());
			serialPort.setFlowControl(serialParams.getFlowControl());
		}

		if ( serialParams.getRtsFlag() >= 0 ) {
			boolean mode = serialParams.getRtsFlag() > 0 ? true : false;
			log.debug("Setting RTS to {}", mode);
			serialPort.setRs485ModeParameters(mode, mode, 0, 0);
		}
	}

	@Override
	public void close() {
		if ( serialPort == null ) {
			return;
		}
		try {
			if ( in != null ) {
				log.debug("Closing serial port {} InputStream", this.serialPort);
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore this
					log.warn("Exception closing serial port {} input stream: {}", this.serialPort,
							e.getMessage());
				}
			}
			if ( out != null ) {
				log.debug("Closing serial port {} OutputStream", this.serialPort);
				try {
					out.close();
				} catch ( IOException e ) {
					// ignore this
					log.warn("Exception closing serial port {} output stream: {}", this.serialPort,
							e.getMessage());
				}
			}
			log.debug("Closing serial port {}", this.serialPort);
			serialPort.closePort();
			log.trace("Serial port {} closed", this.serialPort);
		} finally {
			in = null;
			out = null;
			serialPort = null;
		}
	}

	@Override
	public byte[] readMarkedMessage(final byte[] startMarker, final int length) throws IOException {
		final ByteList sink = new ByteList(startMarker.length + length);
		final byte[] buf = new byte[64];
		boolean result = false;
		if ( serialParams.getMaxWait() < 1 ) {
			do {
				result = readMarkedMessage(getInputStream(), sink, buf, startMarker, length);
			} while ( !result );
			return sink.toArrayValue();
		}
		AbortableCallable<Boolean> task = new AbortableCallable<Boolean>() {

			private boolean keepGoing = true;

			@Override
			public Boolean call() throws Exception {
				boolean found = false;
				do {
					found = readMarkedMessage(getInputStream(), sink, buf, startMarker, length);
				} while ( !found && keepGoing );
				return found;
			}

			@Override
			public void abort() {
				keepGoing = false;
			}

		};
		result = performIOTaskWithMaxWait(task);
		return (result ? sink.toArrayValue() : null);
	}

	private boolean readMarkedMessage(final InputStream in, final ByteList sink, final byte[] buf,
			final byte[] startMarker, final int length) throws IOException {
		boolean lookingForEndMarker = (sink.size() > startMarker.length);
		int max = (lookingForEndMarker ? length - sink.size() : startMarker.length);
		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("Sink contains {} bytes: {}", sink.size(),
					asciiDebugValue(sink.toArrayValue()));
		}
		int len = -1;
		eventLog.trace("Attempting to read up to {} bytes from serial port", max);
		while ( max > 0 && (len = in.read(buf, 0, max > buf.length ? buf.length : max)) > 0 ) {
			sink.add(buf, 0, len);
			if ( lookingForEndMarker == false ) {
				int foundMarkerByteCount = findMarkerBytes(sink, len, startMarker, false);
				if ( foundMarkerByteCount == startMarker.length ) {
					lookingForEndMarker = true;
				}
			}
			if ( lookingForEndMarker ) {
				if ( sink.size() == length ) {
					return true;
				}
				max = (length - sink.size());
				if ( eventLog.isDebugEnabled() ) {
					eventLog.debug("Looking for {} more message bytes, buffer: {}", max,
							asciiDebugValue(sink.toArrayValue()));
				}
			}
		}
		return false;
	}

	@Override
	public void writeMessage(final byte[] message) throws IOException {
		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("Attempting to write {} bytes to serial port: {}", message.length,
					asciiDebugValue(message));
		}
		if ( serialParams.getMaxWait() < 1 ) {
			getOutputStream().write(message);
			return;
		}
		performIOTaskWithMaxWait(new NoResultUnabortableCallable() {

			@Override
			protected void doCall() throws Exception {
				OutputStream stream = getOutputStream();
				stream.write(message);
				stream.flush();
			}
		});
	}

	@Override
	public byte[] drainInputBuffer() throws IOException {
		InputStream in = getInputStream();
		int avail = in.available();
		if ( avail < 1 ) {
			return new byte[0];
		}
		eventLog.trace("Attempting to drain {} bytes from serial port", avail);
		byte[] result = new byte[avail];
		int count = 0;
		while ( count < result.length ) {
			count += in.read(result, count, result.length - count);
		}
		eventLog.trace("Drained {} bytes from serial port", result.length);
		return result;
	}

	@Override
	public byte[] readMarkedMessage(final byte[] startMarker, final byte[] endMarker)
			throws IOException {
		final ByteList sink = new ByteList(1024);
		final byte[] buf = new byte[64];
		boolean result = false;
		if ( serialParams.getMaxWait() < 1 ) {
			do {
				result = readMarkedMessage(getInputStream(), sink, buf, startMarker, endMarker);
			} while ( !result );
			return sink.toArrayValue();
		}

		AbortableCallable<Boolean> task = new AbortableCallable<Boolean>() {

			private boolean keepGoing = true;

			@Override
			public Boolean call() throws Exception {
				boolean found = false;
				do {
					found = readMarkedMessage(getInputStream(), sink, buf, startMarker, endMarker);
				} while ( !found && keepGoing );
				return found;
			}

			@Override
			public void abort() {
				keepGoing = false;
			}

		};
		result = performIOTaskWithMaxWait(task);
		return (result ? sink.toArrayValue() : null);
	}

	private boolean readMarkedMessage(final InputStream in, final ByteList sink, final byte[] buf,
			final byte[] startMarker, final byte[] endMarker) throws IOException {
		boolean lookingForEndMarker = (sink.size() > startMarker.length);
		int max = (lookingForEndMarker ? endMarker.length : startMarker.length);
		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("Sink contains {} bytes: {}", sink.size(),
					asciiDebugValue(sink.toArrayValue()));
		}
		int len = -1;
		eventLog.trace("Attempting to read up to {} bytes from serial port", max);
		while ( max > 0 && (len = in.read(buf, 0, max > buf.length ? buf.length : max)) > 0 ) {
			sink.add(buf, 0, len);
			int foundMarkerByteCount = findMarkerBytes(sink, len,
					(lookingForEndMarker ? endMarker : startMarker), lookingForEndMarker);
			if ( lookingForEndMarker == false && foundMarkerByteCount == startMarker.length ) {
				lookingForEndMarker = true;
				// immediately look for end marker, might already be in the buffer
				foundMarkerByteCount = findMarkerBytes(sink, startMarker.length, endMarker, true);
			}
			if ( lookingForEndMarker && foundMarkerByteCount == endMarker.length ) {
				return true;
			}
		}
		if ( eventLog.isTraceEnabled() ) {
			eventLog.debug("Looking for marker {}, buffer: {}",
					(lookingForEndMarker ? asciiDebugValue(endMarker) : asciiDebugValue(startMarker)),
					asciiDebugValue(sink.toArrayValue()));
		}
		return false;
	}

	private <T> T performIOTaskWithMaxWait(AbortableCallable<T> task) throws IOException {
		T result = null;
		Future<T> future = executor.submit(task);
		final long maxMs = Math.max(1, serialParams.getMaxWait());
		eventLog.trace("Waiting at most {}ms for data", maxMs);
		try {
			result = future.get(maxMs, TimeUnit.MILLISECONDS);
		} catch ( InterruptedException e ) {
			log.debug("Interrupted communicating with serial port", e);
			throw new IOException("Interrupted communicating with serial port", e);
		} catch ( ExecutionException e ) {
			log.debug("Exception thrown communicating with serial port", e.getCause());
			throw new IOException("Exception thrown communicating with serial port", e.getCause());
		} catch ( TimeoutException e ) {
			log.warn("Timeout waiting {}ms for serial data, aborting operation", maxMs);
			future.cancel(true);
			throw new LockTimeoutException(
					"Timeout waiting " + serialParams.getMaxWait() + "ms for serial data");
		} finally {
			task.abort();
		}
		return result;
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

	private OutputStream getOutputStream() throws IOException {
		if ( out != null ) {
			return out;
		}
		if ( !isOpen() ) {
			open();
		}
		out = serialPort.getOutputStream();
		return out;
	}

	private int findMarkerBytes(final ByteList sink, final int appendedLength, final byte[] marker,
			final boolean end) {
		//final byte[] sinkBuf = sink.toArray();
		final int sinkBufLength = sink.size();
		int markerIdx = Math.max(0, sinkBufLength - appendedLength - marker.length);
		boolean foundMarker = false;
		int j = 0;

		if ( eventLog.isTraceEnabled() ) {
			eventLog.trace("Looking for {} marker bytes {} in buffer {}", new Object[] { marker.length,
					asciiDebugValue(marker), asciiDebugValue(sink.toArrayValue()) });
		}
		for ( ; markerIdx < sinkBufLength; markerIdx++ ) {
			foundMarker = true;
			for ( j = 0; j < marker.length && (j + markerIdx) < sinkBufLength; j++ ) {
				if ( sink.getValue(markerIdx + j) != marker[j] ) {
					foundMarker = false;
					break;
				}
			}
			if ( foundMarker ) {
				break;
			}
		}
		// we may have only found a partial match at the end of the buffer, so test j here
		if ( foundMarker && j == marker.length ) {
			if ( eventLog.isDebugEnabled() ) {
				eventLog.debug("Found desired {} marker bytes at index {}", asciiDebugValue(marker),
						markerIdx);
			}
			if ( end ) {
				sink.remove(markerIdx + marker.length, sink.size() - markerIdx - marker.length);
			} else {
				// shift bytes to start at marker
				sink.remove(0, markerIdx);
			}
			if ( eventLog.isDebugEnabled() ) {
				eventLog.debug("Buffer message at marker: {}", asciiDebugValue(sink.toArrayValue()));
			}
			return marker.length;
		} else if ( !end ) {
			// truncate sink to any partial match
			if ( j > 0 ) {
				sink.remove(0, markerIdx);
			} else {
				sink.clear();
			}
		}
		return j;
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

	/**
	 * Get the serial port.
	 * 
	 * @return the serial port
	 */
	public SerialPort getSerialPort() {
		return serialPort;
	}

}
