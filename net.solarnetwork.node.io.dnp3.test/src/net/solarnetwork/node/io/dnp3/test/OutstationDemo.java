/* ==================================================================
 * OutstationDemo.java - 21/02/2019 3:34:12 pm
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.dnp3.test;

import static java.lang.System.currentTimeMillis;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.automatak.dnp3.AnalogInput;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.Counter;
import com.automatak.dnp3.DNP3Manager;
import com.automatak.dnp3.DNPTime;
import com.automatak.dnp3.DatabaseConfig;
import com.automatak.dnp3.EventBufferConfig;
import com.automatak.dnp3.Flags;
import com.automatak.dnp3.IPEndpoint;
import com.automatak.dnp3.LogMasks;
import com.automatak.dnp3.Outstation;
import com.automatak.dnp3.OutstationChangeSet;
import com.automatak.dnp3.OutstationStackConfig;
import com.automatak.dnp3.enums.ServerAcceptMode;
import com.automatak.dnp3.impl.DNP3ManagerFactory;
import com.automatak.dnp3.mock.DefaultOutstationApplication;
import com.automatak.dnp3.mock.SuccessCommandHandler;
import net.solarnetwork.dnp3.util.Slf4jChannelListener;
import net.solarnetwork.dnp3.util.Slf4jLogHandler;

/**
 * Example master than can be run against the example outstation.
 */
public class OutstationDemo {

	public static void main(String[] args) throws Exception {

		// create the root class with a thread pool size of 1
		DNP3Manager manager = DNP3ManagerFactory.createManager(1, new Slf4jLogHandler());

		try {
			run(manager);
		} catch ( Exception ex ) {
			System.out.println("Exception: " + ex.getMessage());
		} finally {
			// This call is needed b/c the thread-pool will stop the application from exiting
			// and the finalizer isn't guaranteed to run b/c the GC might not be collected during main() exit
			manager.shutdown();
		}
	}

	public static void run(DNP3Manager manager) throws Exception {

		// Create a tcp channel class that will connect to the loopback
		Channel channel = manager.addTCPServer("client", LogMasks.NORMAL | LogMasks.APP_COMMS,
				ServerAcceptMode.CloseNew, new IPEndpoint("127.0.0.1", 20000),
				new Slf4jChannelListener());

		// Create the default outstation configuration
		OutstationStackConfig config = new OutstationStackConfig(DatabaseConfig.allValues(5),
				EventBufferConfig.allTypes(50));

		// Create an Outstation instance, pass in a simple a command handler that responds successfully to everything
		Outstation outstation = channel.addOutstation("outstation", SuccessCommandHandler.getInstance(),
				DefaultOutstationApplication.getInstance(), config);

		outstation.enable();

		// all this stuff just to read a line of text in Java. Oh the humanity.
		String line = "";
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);

		int i = 0;
		while ( true ) {
			System.out.println("Enter something to update a counter or type <quit> to exit");
			line = in.readLine();
			if ( line.equals("quit") ) {
				break;
			} else if ( line.equals("meas") ) {
				OutstationChangeSet set = new OutstationChangeSet();
				set.update(new AnalogInput(Math.random(), new Flags((byte) 0x01),
						new DNPTime(currentTimeMillis())), 0);
				outstation.apply(set);
			} else {
				OutstationChangeSet set = new OutstationChangeSet();
				set.update(new Counter(i, new Flags((byte) 0x01), new DNPTime(currentTimeMillis())), 1);
				outstation.apply(set);
				++i;
			}
		}
	}
}
