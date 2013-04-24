/**
 * 
 */

package net.solarnetwork.node.io.rxtx;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Read the serial port stream, looking for a "magic" byte sequence, and then
 * collecting a variable number of bytes after that until an "eof" byte sequence
 * is read.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>magic</dt>
 * <dd>A sequence of bytes that serve as the starting marker for the data to
 * collect.</dd>
 * 
 * <dt>eofMagic</dt>
 * <dd>A sequence of bytes that serve as the end marker for the data to collect.
 * </dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class SerialPortVariableDataCollector extends AbstractSerialPortDataCollector {

	private final byte[] magic;
	private final byte[] eofMagic;

	/**
	 * Construct with a port and default settings.
	 * 
	 * @param port
	 *        the port
	 */
	public SerialPortVariableDataCollector(SerialPort port) {
		this(port, 2048, new byte[] { 0 }, new byte[] { 13 }, 2000);
	}

	/**
	 * Constructor.
	 * 
	 * @param port
	 *        the port to read
	 * @param bufferSize
	 *        the maximum number of bytes to read at one time
	 * @param magic
	 *        the "magic" byte sequence to start reading after
	 * @param eofMagic
	 *        the end-of-file "magic" byte sequence to end reading with
	 * @param maxWait
	 *        the maximum number of milliseconds to wait for the data to be
	 *        collected
	 */
	public SerialPortVariableDataCollector(SerialPort port, int bufferSize, byte[] magic,
			byte[] eofMagic, long maxWait) {
		super(port, maxWait, bufferSize);
		this.magic = magic;
		this.eofMagic = eofMagic;
	}

	@Override
	protected boolean handleSerialEventInternal(SerialPortEvent event, InputStream in,
			ByteArrayOutputStream buffer) {
		boolean done;
		try {
			done = handleSerialEvent(event, in, buffer, magic, eofMagic);
		} catch ( Exception e ) {
			done = true;
		}
		return done;
	}

}
