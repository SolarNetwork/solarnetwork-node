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
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import net.solarnetwork.node.hw.yaskawa.ecb.Packet;
import net.solarnetwork.node.hw.yaskawa.ecb.PacketEnvelope;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.io.serial.rxtx.SerialPortNetwork;
import net.solarnetwork.node.support.SerialPortBeanParameters;

/**
 * Interactive diagnostic app for the Yaskawa ECB protocol.
 * 
 * @author matt
 * @version 1.0
 */
public class Cmdline {

	private final SerialNetwork serial;

	/**
	 * Constructor.
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
		int addr = Integer.parseInt(components[0]);
		int cmd = Integer.parseInt(components[1]);
		int subCmd = Integer.parseInt(components[2]);
		String body = null;
		if ( components.length > 3 ) {
			body = components[3];
		}
		try {
			Packet msg = Packet.forCommand(addr, cmd, subCmd, body);
			conn.writeMessage(msg.getBytes());
			byte[] head = conn.readMarkedMessage(new byte[] { PacketEnvelope.Start.getCode() }, 4);
			System.out.println("Got head: " + Hex.encodeHexString(head));
			int dataLen = head[3];
			byte[] resp = conn.readMarkedMessage(
					new byte[] { msg.getHeader().getCommand(), msg.getHeader().getSubCommand() },
					dataLen + 5);
			System.out.println("Got resp: " + Hex.encodeHexString(resp));
			Packet respMsg = Packet.forData(head, 0, resp, 0);
			System.out.println("Got packet: " + respMsg);
		} catch ( DecoderException e ) {
			throw new IllegalArgumentException("Error decoding body hex: " + e.getMessage());
		}
	}

	private boolean handlePrompt(BufferedReader in, SerialConnection conn) throws IOException {
		System.out.print("> ");
		System.out.flush();
		String input = in.readLine();
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

	public void execute() throws IOException {
		SerialConnection conn = null;
		BufferedReader reader;
		try {
			conn = serial.createConnection();
			conn.open();
			reader = new BufferedReader(new InputStreamReader(System.in));
			handlePrompt(reader, conn);
		} finally {
			if ( conn != null ) {
				conn.close();
			}
		}
	}

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
		SerialPortNetwork serial = new SerialPortNetwork();
		SerialPortBeanParameters serialParams = serial.getSerialParams();
		serialParams.setSerialPort("/dev/ttyUSB0");
		serialParams.setBaud(9600);
		serialParams.setParity(0);
		serialParams.setStopBits(1);

		boolean oneShot = false;

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
					}
					break;

			}
		}
		try {
			Cmdline app = new Cmdline(serial);
			if ( oneShot ) {
				app.execute();
			} else {
				app.go();
			}
		} catch ( IOException e ) {
			// ignore
		}
	}
}
