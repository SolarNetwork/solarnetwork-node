/**
 * 
 */

package net.solarnetwork.node.io.rxtx;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import net.solarnetwork.node.DataCollector;

/**
 * Abstract support for collecting data from a serial port stream.
 * 
 * <p>
 * After constructing an instance of this class, you can configure various
 * communication settings from the class properties (e.g.
 * {@link #setStopBits(int)}). When the {@link #collectData()} method is called,
 * this class will start collecting data, until extending implementation
 * determines collection should stop. The collected data is held in memory.
 * </p>
 * 
 * <p>
 * The {@code dataBits}, {@code stopBits}, and {@code parity} class properties
 * should be initialized to values corresponding to the constants defined in the
 * {@link SerialPort} class (e.g. {@link SerialPort#DATABITS_8}, etc.).
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>toggleDtr</dt>
 * <dd>If <em>true</em> then toggle the DTR setting before closing the
 * SerialPort. Defaults to {@code true}.</dd>
 * 
 * <dt>toggleRts</dt>
 * <dd>If <em>true</em> then toggle the RTS setting before closing the
 * SerialPort. Defaults to {@code true}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractSerialPortDataCollector extends SerialPortSupport implements
		DataCollector, SerialPortEventListener {

	private final ByteArrayOutputStream buffer;

	private boolean toggleDtr = true;
	private boolean toggleRts = true;

	private boolean collectData = false;
	private InputStream in = null;
	private boolean doneCollecting = false;

	/**
	 * Construct with a port and default settings.
	 * 
	 * @param port
	 *        the port
	 */
	public AbstractSerialPortDataCollector(SerialPort port, long maxWait, int bufferSize) {
		super(port, maxWait);
		buffer = new ByteArrayOutputStream(bufferSize);
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
	public void stopCollecting() {
		closeSerialPort();
	}

	@Override
	public String getCollectedDataAsString() {
		return buffer.toString();
	}

	@Override
	public void collectData() {
		setupSerialPortParameters(this);
		try {
			synchronized ( this ) {
				this.buffer.reset();

				// open the input stream
				this.in = serialPort.getInputStream();

				this.doneCollecting = false;
				this.collectData = true;
				// sleep until we have data
				this.wait(getMaxWait());
				this.collectData = false;
			}
			if ( log.isWarnEnabled() && !doneCollecting ) {
				log.warn("Timeout collecting serial data");
				buffer.reset();
			}
		} catch ( InterruptedException e ) {
			log.warn("Interrupted, stopping data collection");
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			if ( this.in != null ) {
				if ( toggleDtr ) {
					serialPort.setDTR(!isDtr());
				}
				if ( toggleRts ) {
					serialPort.setRTS(!isRts());
				}
				try {
					this.in.close();
				} catch ( IOException e ) {
					// ignore this one
				}
			}
			serialPort.removeEventListener();
		}
	}

	@Override
	public final void serialEvent(SerialPortEvent event) {
		eventLog.trace("SerialPortEvent {}", event.getEventType());
		if ( event.getEventType() != SerialPortEvent.DATA_AVAILABLE ) {
			return;
		}

		if ( !collectData ) {
			// drain the buffer
			drainInputStream(in);
			return;
		}

		boolean done;
		try {
			done = handleSerialEventInternal(event, in, buffer);
		} catch ( Exception e ) {
			done = true;
		}
		if ( done ) {
			synchronized ( this ) {
				doneCollecting = true;
				notifyAll();
			}
			return;
		}
	}

	/**
	 * Handle a serial event.
	 * 
	 * <p>
	 * Extending classes must implement this method to process the serial event.
	 * This method will not be called unless the event is
	 * </p>
	 * 
	 * @param event
	 *        the serial event
	 * @param in
	 *        the serial InputStream
	 * @param buffer
	 *        a data buffer
	 * @return <em>true</em> if data collection is complete and should end;
	 *         <em>false</em> to keep collecting data
	 */
	protected abstract boolean handleSerialEventInternal(SerialPortEvent event, InputStream in,
			ByteArrayOutputStream buffer);

	public boolean isToggleDtr() {
		return toggleDtr;
	}

	public void setToggleDtr(boolean toggleDtr) {
		this.toggleDtr = toggleDtr;
	}

	public boolean isToggleRts() {
		return toggleRts;
	}

	public void setToggleRts(boolean toggleRts) {
		this.toggleRts = toggleRts;
	}

}
