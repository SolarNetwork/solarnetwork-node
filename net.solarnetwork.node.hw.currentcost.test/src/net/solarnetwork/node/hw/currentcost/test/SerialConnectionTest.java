/* ==================================================================
 * SerialConnectionTest.java - Apr 23, 2013 3:30:30 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.currentcost.test;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import java.io.UnsupportedEncodingException;
import net.solarnetwork.node.io.rxtx.AbstractSerialPortSupportFactory;
import net.solarnetwork.node.io.rxtx.SerialPortVariableDataCollector;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class SerialConnectionTest {

	final Logger log = LoggerFactory.getLogger(getClass());

	private static class SerialFactory extends AbstractSerialPortSupportFactory {

		static final Logger LOG = LoggerFactory.getLogger(SerialFactory.class);

		private CommPortIdentifier portId = null;

		private SerialFactory() {
			super();
			setBaud(9600);
			setDataBits(8);
			setStopBits(1);
			setReceiveThreshold(-1);
			setSerialPort("/dev/ttyUSB0");
		}

		public SerialPortVariableDataCollector getDataCollectorInstance() {
			LOG.debug("Opening serial port");
			if ( portId == null ) {
				portId = getCommPortIdentifier();
			}
			SerialPort port;
			try {
				port = (SerialPort) portId.open("UnitTest", 2000);
			} catch ( PortInUseException e ) {
				throw new RuntimeException(e);
			}
			SerialPortVariableDataCollector dc;
			try {
				dc = new SerialPortVariableDataCollector(port, 2048, "<msg>".getBytes("US-ASCII"),
						"</msg>".getBytes("US-ASCII"), 8000);
			} catch ( UnsupportedEncodingException e ) {
				throw new RuntimeException(e);
			}
			setupSerialPortSupport(dc);
			return dc;
		}

	}

	@Test
	public void readFromSerialConnection() throws InterruptedException, UnsupportedEncodingException {
		SerialFactory factory = new SerialFactory();
		SerialPortVariableDataCollector dc = null;
		try {
			dc = factory.getDataCollectorInstance();
			dc.collectData();
		} finally {
			if ( dc != null ) {
				dc.stopCollecting();
			}
		}
		log.debug("Collected " + dc.getCollectedData().length + " bytes:\n"
				+ dc.getCollectedDataAsString());
	}
}
