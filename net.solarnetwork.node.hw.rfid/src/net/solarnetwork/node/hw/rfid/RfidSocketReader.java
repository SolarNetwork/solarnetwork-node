/* ==================================================================
 * RfidSocketReader.java - 20/06/2015 11:39:27 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.rfid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.SocketFactory;

/**
 * Example program to read from the {@code rfid-server} C program and print the
 * output to STDOUT.
 * 
 * @author matt
 * @version 1.0
 */
public class RfidSocketReader {

	/** The "heartbeat" message sent by the server after read timeouts. */
	public static final String HEARTBEAT_MSG = "ping";

	private int port = 9090;
	private String host = "localhost";

	/**
	 * Open a TCP/IP socket to {@code host} on {@code port} to print RFID data.
	 * This method will block and continue to read from the socket until the
	 * program is closed (or the socket closes). All lines read from the program
	 * are printed to STDOUT.
	 * 
	 * @throws UnknownHostException
	 *         if the host cannot be resolved
	 */
	public void startReading() throws UnknownHostException {
		BufferedReader in = null;
		Socket s = null;
		try {
			s = SocketFactory.getDefault().createSocket(host, port);
			s.setKeepAlive(true);
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			boolean readSomething = false;
			String line;
			while ( true ) {
				line = in.readLine();
				if ( line == null ) {
					break;
				}
				// the first line read is a status line...
				if ( readSomething && !HEARTBEAT_MSG.equalsIgnoreCase(line) ) {
					System.out.println("Got RFID line: " + line);
				} else {
					System.err.println(line);
				}
				readSomething = true;
			}
			if ( !readSomething ) {
				return;
			}
		} catch ( IOException e ) {
			System.err.println("IO error: " + e.getMessage());
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
			if ( s != null ) {
				try {
					s.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
		}
	}

	public int getPort() {
		return port;
	}

	/**
	 * Set the TCP/IP port to connect to.
	 * 
	 * @param port
	 *        The port.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	/**
	 * Set the server host name or IP address to connect to.
	 * 
	 * @param host
	 *        The host name.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Execute the reader. You can optionally pass the {@code host} and
	 * {@code port} values on the command line. Calls {@link #startReading()}.
	 * 
	 * @param args
	 *        The optional settings.
	 */
	public static void main(String[] args) {
		RfidSocketReader reader = new RfidSocketReader();
		if ( args.length > 0 ) {
			reader.setHost(args[0]);
		}
		if ( args.length > 1 ) {
			reader.setPort(Integer.valueOf(args[1]));
		}
		try {
			reader.startReading();
		} catch ( UnknownHostException e ) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

}
