/* ==================================================================
 * Modbus4jCachedModbusConnection.java - 22/11/2022 3:00:48 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.modbus4j;

import static java.lang.String.format;
import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.serotonin.modbus4j.ModbusMaster;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.service.LockTimeoutException;

/**
 * Caching extension of {@link Modbus4jModbusConnection}, to only close the
 * connection after a length of time of no activity.
 * 
 * @author matt
 * @version 1.0
 */
public class Modbus4jCachedModbusConnection extends Modbus4jModbusConnection implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Modbus4jCachedModbusConnection.class);

	private final int keepOpenSeconds;
	private final AtomicLong keepOpenExpiry;

	private Thread keepOpenTimeoutThread;

	/**
	 * @param unitId
	 *        the unit ID
	 * @param headless
	 *        the headless flag
	 * @param controller
	 *        the controller
	 * @param describer
	 *        a function that returns a description of the connection
	 * @param keepOpenSeconds
	 *        the number of seconds to keep the connection open
	 */
	public Modbus4jCachedModbusConnection(int unitId, boolean headless, ModbusMaster controller,
			Supplier<String> describer, int keepOpenSeconds) {
		super(unitId, headless, controller, describer);
		this.keepOpenSeconds = keepOpenSeconds;
		this.keepOpenExpiry = new AtomicLong(0);
	}

	@Override
	public void open() throws IOException, LockTimeoutException {
		synchronized ( controller ) {
			super.open();
			if ( keepOpenSeconds > 0 && keepOpenTimeoutThread == null
					|| !keepOpenTimeoutThread.isAlive() ) {
				activity();
				keepOpenTimeoutThread = new Thread(this, format("Modbus Expiry %s", describer.get()));
				keepOpenTimeoutThread.setDaemon(true);
				keepOpenTimeoutThread.start();
				if ( log.isInfoEnabled() ) {
					log.info("Opened Modbus connection {}; keep for {}s", describer.get(),
							keepOpenSeconds);
				}
			}
		}
	}

	@Override
	public void close() {
		synchronized ( controller ) {
			if ( keepOpenSeconds < 1 || keepOpenExpiry.get() < System.currentTimeMillis() ) {
				doClose();
			}
		}
	}

	/**
	 * Force the cached connection to be closed.
	 */
	public void forceClose() {
		synchronized ( controller ) {
			doClose();
		}
	}

	private void doClose() {
		if ( controller.isInitialized() ) {
			controller.destroy();
			if ( keepOpenSeconds > 0 && log.isInfoEnabled() ) {
				log.info("Closed Modbus connection {}", describer.get());
			}
		}
	}

	private void activity() {
		log.trace("Extending Modbus connection {} expiry to {} seconds from now", describer.get(),
				keepOpenSeconds);
		keepOpenExpiry.set(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(keepOpenSeconds));
	}

	@Override
	public void run() {
		try {
			while ( true ) {
				long now, expire;
				synchronized ( controller ) {
					now = System.currentTimeMillis();
					expire = keepOpenExpiry.get();
					if ( expire < now ) {
						doClose();
						return;
					}
				}
				long sleep = expire - now;
				if ( log.isDebugEnabled() ) {
					log.debug("Connection {} expires in {}ms", describer.get(), sleep);
				}
				Thread.sleep(sleep);
			}
		} catch ( InterruptedException e ) {
			// end
		}
	}

	@Override
	public BitSet readDiscreetValues(int address, int count) throws IOException {
		activity();
		return super.readDiscreetValues(address, count);
	}

	@Override
	public void writeDiscreetValues(int[] addresses, BitSet bits) throws IOException {
		activity();
		super.writeDiscreetValues(addresses, bits);
	}

	@Override
	public BitSet readInputDiscreteValues(int address, int count) throws IOException {
		activity();
		return super.readInputDiscreteValues(address, count);
	}

	@Override
	public short[] readWords(ModbusReadFunction function, int address, int count) throws IOException {
		activity();
		return super.readWords(function, address, count);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, short[] values)
			throws IOException {
		activity();
		super.writeWords(function, address, values);
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, int address, int count) throws IOException {
		activity();
		return super.readBytes(function, address, count);
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, int address, byte[] values) throws IOException {
		activity();
		super.writeBytes(function, address, values);
	}

}
