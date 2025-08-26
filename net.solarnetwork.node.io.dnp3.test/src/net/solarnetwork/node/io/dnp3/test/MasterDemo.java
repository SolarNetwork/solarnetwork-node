/* ==================================================================
 * MasterDemo.java - 21/02/2019 3:51:09 pm
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import com.automatak.dnp3.Channel;
import com.automatak.dnp3.ChannelRetry;
import com.automatak.dnp3.CommandTaskResult;
import com.automatak.dnp3.ControlRelayOutputBlock;
import com.automatak.dnp3.DNP3Manager;
import com.automatak.dnp3.Header;
import com.automatak.dnp3.IPEndpoint;
import com.automatak.dnp3.LogMasks;
import com.automatak.dnp3.Master;
import com.automatak.dnp3.MasterStackConfig;
import com.automatak.dnp3.enums.CommandStatus;
import com.automatak.dnp3.enums.OperationType;
import com.automatak.dnp3.enums.TripCloseCode;
import com.automatak.dnp3.impl.DNP3ManagerFactory;
import com.automatak.dnp3.mock.DefaultMasterApplication;
import com.automatak.dnp3.mock.PrintingSOEHandler;
import net.solarnetwork.dnp3.util.Slf4jChannelListener;
import net.solarnetwork.dnp3.util.Slf4jLogHandler;

/**
 * Example master than can be run against the example outstation.
 */
public class MasterDemo {

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

	static void run(DNP3Manager manager) throws Exception {
		// Create a tcp channel class that will connect to the loopback
		Channel channel = manager.addTCPClient("client", LogMasks.NORMAL | LogMasks.APP_COMMS,
				ChannelRetry.getDefault(), List.of(new IPEndpoint("127.0.0.1", 20000)), "0.0.0.0",
				new Slf4jChannelListener());

		// You can modify the defaults to change the way the master behaves
		MasterStackConfig config = new MasterStackConfig();

		// Create a master instance, pass in a simple singleton to print received values to the console
		Master master = channel.addMaster("master", PrintingSOEHandler.getInstance(),
				DefaultMasterApplication.getInstance(), config);

		// do an integrity scan every 2 seconds
		//master.addPeriodicScan(Duration.ofSeconds(2), Header.getIntegrity());

		master.enable();

		// all this cruft just to read a line of text in Java. Oh the humanity.
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);

		while ( true ) {
			System.out.println("Enter something to issue a command or type <quit> to exit");
			String line = in.readLine();
			switch (line) {
				case ("quit"):
					return;
				case ("crob"):
					ControlRelayOutputBlock crob = new ControlRelayOutputBlock(OperationType.LATCH_ON,
							TripCloseCode.CLOSE, false, (short) 1, 100, 100, CommandStatus.SUCCESS);
					master.selectAndOperateCROB(crob, 0).thenAccept(
							//asynchronously print the result of the command operation
							(CommandTaskResult result) -> System.out.println(result));
					break;
				case ("scan"):
					master.scan(Header.getEventClasses(), PrintingSOEHandler.getInstance());
					break;
				default:
					System.out.println("Unknown command: " + line);
					break;
			}
		}
	}

}
