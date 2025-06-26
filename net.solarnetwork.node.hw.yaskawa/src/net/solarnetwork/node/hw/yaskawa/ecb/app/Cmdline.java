/* ==================================================================
 * Cmdline.java - 17/05/2018 6:43:53 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.yaskawa.ecb.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.yaskawa.ecb.Packet;
import net.solarnetwork.node.hw.yaskawa.ecb.PacketUtils;
import net.solarnetwork.node.io.serial.ConfigurableSerialNetwork;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;

/**
 * Interactive diagnostic app for the Yaskawa ECB protocol.
 *
 * @author matt
 * @version 2.1
 */
public class Cmdline {

	private final SerialNetwork serial;

	private static final Logger log = LoggerFactory.getLogger(Cmdline.class);

	/**
	 * Constructor.
	 *
	 * @param serial
	 *        the serial network to use
	 */
	public Cmdline(SerialNetwork serial) {
		super();
		this.serial = serial;
	}

	private static enum Action {
		Unknown,
		Quit,
		Send;
	}

	private static class ActionLine {

		private final Action action;
		private final String arguments;

		private ActionLine(Action action, String arguments) {
			super();
			this.action = action;
			this.arguments = arguments;
		}

		private static ActionLine forLine(String line) {
			line = line.trim();
			int idx = line.indexOf(' ');
			if ( idx < 0 ) {
				return new ActionLine(Action.Unknown, line);
			}
			String a = line.substring(0, idx).toLowerCase();
			String r = null;
			if ( line.length() > idx ) {
				r = line.substring(idx + 1);
			}
			switch (a) {
				case "q":
				case "quit":
					return new ActionLine(Action.Quit, null);

				case "s":
				case "send":
					return new ActionLine(Action.Send, r);

				default:
					return new ActionLine(Action.Unknown, line);
			}
		}
	}

	/**
	 * Send a command.
	 *
	 * <p>
	 * Arguments must be in form: <code>ADDR CMD SUBCMD HEXBODY</code>.
	 * </p>
	 *
	 * @param conn
	 *        the serial connection to use
	 * @param arguments
	 *        the arguments
	 * @return the response packet
	 */
	private void handleSend(SerialConnection conn, String arguments) throws IOException {
		String[] components = arguments.split("\\s+", 4);
		if ( components.length < 3 ) {
			throw new IllegalArgumentException("send must provide ADDR CMD SUBCMD HEXBODY arguments");
		}
		log.trace("Got cmd components: {}", Arrays.toString(components));
		int addr = Integer.parseInt(components[0]);
		int cmd = Integer.parseInt(components[1]);
		int subCmd = Integer.parseInt(components[2]);
		String body = null;
		if ( components.length > 3 ) {
			body = components[3];
		}
		try {
			Packet msg = Packet.forCommand(addr, cmd, subCmd, body);
			Packet respMsg = PacketUtils.sendPacket(conn, msg);
			System.out.println(">>> " + respMsg.toDebugString());
		} catch ( DecoderException e ) {
			throw new IllegalArgumentException("Error decoding body hex: " + e.getMessage());
		}
	}

	private boolean handleCommand(String input, SerialConnection conn) throws IOException {
		ActionLine act = ActionLine.forLine(input);
		try {
			switch (act.action) {
				case Quit:
					return false;

				case Send:
					handleSend(conn, act.arguments);
					break;

				default:
					System.err.println("Unknown action: " + input);
			}
		} catch ( RuntimeException e ) {
			System.err.println(e.getMessage());
		}
		return true;
	}

	private boolean handlePrompt(BufferedReader in, SerialConnection conn) throws IOException {
		System.out.print("> ");
		System.out.flush();
		String input = in.readLine();
		return handleCommand(input, conn);
	}

	/**
	 * Execute the command.
	 *
	 * @param cmd
	 *        the command to execute
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public void execute(String cmd) throws IOException {
		SerialConnection conn = null;
		BufferedReader reader;
		try {
			conn = serial.createConnection();
			conn.open();
			if ( cmd != null && cmd.length() > 0 ) {
				handleCommand(cmd, conn);
			} else {
				reader = new BufferedReader(new InputStreamReader(System.in));
				handlePrompt(reader, conn);
			}
		} finally {
			if ( conn != null ) {
				conn.close();
			}
		}
	}

	/**
	 * Start the app event loop.
	 *
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public void go() throws IOException {
		SerialConnection conn = null;
		BufferedReader reader;
		try {
			conn = serial.createConnection();
			conn.open();
			reader = new BufferedReader(new InputStreamReader(System.in));
			boolean keepGoing = true;
			while ( keepGoing ) {
				keepGoing = handlePrompt(reader, conn);
				if ( keepGoing ) {
					System.out.println("\n");
				}
			}
		} finally {
			if ( conn != null ) {
				conn.close();
			}
		}
	}

	private static String getArg(String[] args, int index) {
		if ( index >= args.length ) {
			return null;
		}
		return args[index];
	}

	/**
	 * Command-line entry point.
	 *
	 * @param args
	 *        the arguments
	 */
	public static void main(String[] args) {
		ConfigurableSerialNetwork serial;
		try {
			serial = (ConfigurableSerialNetwork) Cmdline.class.getClassLoader()
					.loadClass("net.solarnetwork.node.io.serial.rxtx.SerialPortNetwork")
					.getDeclaredConstructor().newInstance();
		} catch ( InstantiationException | IllegalAccessException | ClassNotFoundException
				| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e1 ) {
			System.err.println("Error loading SerialPortNetwork class: " + e1);
			System.exit(1);
			return;
		}

		SerialPortBeanParameters serialParams = serial.getSerialParams();
		serialParams.setSerialPort("/dev/ttyUSB0");
		serialParams.setBaud(9600);
		serialParams.setParity(0);
		serialParams.setStopBits(1);
		serialParams.setReceiveThreshold(-1);
		serialParams.setRts(false);
		serialParams.setDtr(false);

		boolean oneShot = false;
		List<String> nonSwitchArguments = new ArrayList<>(8);

		for ( int i = 0; i < args.length; i++ ) {
			String arg = args[i];
			String nextArg = null;
			switch (arg) {
				case "-1":
					oneShot = true;
					break;

				case "-p":
					nextArg = getArg(args, i + 1);
					if ( nextArg != null ) {
						serialParams.setSerialPort(nextArg);
						i++;
					}
					break;

				default:
					nonSwitchArguments.add(arg);
					break;
			}
		}

		try {
			Cmdline app = new Cmdline(serial);
			if ( oneShot ) {
				app.execute(nonSwitchArguments.stream().collect(Collectors.joining(" ")));
			} else {
				app.go();
			}
		} catch ( IOException e ) {
			// ignore
		}
		System.exit(0);
	}
}
