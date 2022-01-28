/* ==================================================================
 * GpioTool.java - 19/10/2021 10:21:18 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.numato.usbgpio.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import net.solarnetwork.node.control.numato.usbgpio.GpioService;
import net.solarnetwork.node.control.numato.usbgpio.UsbGpioService;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.io.serial.pjc.PjcSerialNetwork;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;

/**
 * Command-line tool for interacting with the Numato GPIO device.
 * 
 * @author matt
 * @version 1.0
 */
public class GpioTool {

	private SerialNetwork serial;

	/**
	 * Constructor.
	 * 
	 * @param devicePath
	 *        the device path
	 */
	public GpioTool(String devicePath) {
		super();
		PjcSerialNetwork pjcSerial = new PjcSerialNetwork();
		SerialPortBeanParameters params = new SerialPortBeanParameters();
		params.setBaud(9600);
		params.setSerialPort(devicePath);
		params.setReceiveThreshold(-1);
		pjcSerial.setSerialParams(params);
		this.serial = pjcSerial;
	}

	public void go() {
		System.out.println(String.format("Opening port %s", serial.getPortName()));
		try (SerialConnection conn = serial.createConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"))) {
			conn.open();
			final GpioService service = new UsbGpioService(conn);
			System.out.println("Type ? for help.");
			while ( true ) {
				try {
					System.out.print("> ");
					String line = in.readLine().trim();
					String[] components = line.split("\\s+");
					if ( components.length < 1 ) {
						continue;
					}
					final String cmd = components[0];
					switch (cmd) {
						case "g":
						case "gpio":
							readDigital(service, components);
							break;

						case "id":
							handleId(service, components);
							break;

						case "r":
						case "read":
							readAnalog(service, components);
							break;

						case "v":
						case "ver":
							printVersion(service);
							break;

						case "?":
						case "h":
						case "help":
							printHelp();
							break;

						case "q":
						case "quit":
						case "exit":
							return;
					}
				} catch ( IOException e ) {
					System.err.println("Communication error: " + e.toString());
				}
			}
		} catch ( IOException e ) {
			System.err.println("Error opening serial port: " + e.toString());
		} finally {
			System.out.println("Goodbye.");
		}
	}

	private void readAnalog(GpioService service, String[] components) throws IOException {
		int address = -1;
		if ( components.length > 1 ) {
			try {
				address = Integer.parseInt(components[1]);
			} catch ( NumberFormatException e ) {
				// ignore
			}
		}
		if ( address < 0 ) {
			System.err.println("Pass address 0-7 to read from.");
			return;
		}
		int val = service.readAnalog(address);
		System.out.println(String.format("Analog %d is %d", address, val));
	}

	private void readDigital(GpioService service, String[] components) throws IOException {
		int address = -1;
		if ( components.length > 1 ) {
			try {
				address = Integer.parseInt(components[1]);
			} catch ( NumberFormatException e ) {
				// ignore
			}
		}
		if ( address < 0 ) {
			System.err.println("Pass address 0-7 to read from.");
			return;
		}
		boolean on = service.read(address);
		System.out.println(String.format("Digital %d is %s", address, on ? "ON" : "OFF"));
	}

	private void handleId(GpioService service, String[] components) throws IOException {
		if ( components.length < 2 ) {
			String id = service.getId();
			System.out.println(String.format("Device ID: %s", id));
		} else {
			String newId = components[1];
			if ( newId.length() != 8 ) {
				System.err.println("Invalid ID: must be 8 characters exactly.");
				return;
			}
			service.setId(newId);
			System.out.println(String.format("Device ID set: %s", newId));
		}

	}

	private void printVersion(GpioService service) throws IOException {
		String id = service.getDeviceVersion();
		System.out.println(String.format("Device ID: %s", id));
	}

	private void printHelp() {
		System.out.println("Available commands:\n");
		System.out.println("  get X      -- read digital address X; X is 0-7");
		System.out.println("  read X     -- read analog address X; X is 0-7");
		System.out.println("  id [val]   -- print device ID, or set of [ver] provided");
		System.out.println("  ver        -- print device verison");
		System.out.println("  quit       -- or exit: quit program");
	}

	/**
	 * Execute the CLI tool.
	 * 
	 * @param args
	 *        the CLI arguments; must pass the serial device path to use
	 */
	public static void main(String[] args) throws IOException {
		if ( args.length < 1 ) {
			System.out.println("Pass serial device argument, e.g. /dev/ttyUSB0.");
			System.exit(1);
		}
		GpioTool tool = new GpioTool(args[0]);
		tool.go();
	}

}
