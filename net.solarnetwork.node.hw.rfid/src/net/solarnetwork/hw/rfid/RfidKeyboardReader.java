/* ==================================================================
 * RfidKeyboardReader.java - 19/06/2015 4:51:34 pm
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

package net.solarnetwork.hw.rfid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Example program to read from the {@code rfid-reader} C program and print the
 * output to STDOUT.
 * 
 * @author matt
 * @version 1.0
 */
public class RfidKeyboardReader {

	private String prog = "/usr/local/bin/rfid-reader";
	private String device = "/dev/rfid-reader";

	/**
	 * Start reading from {@code prog} program for RFID data. This method will
	 * block and continue to read from STDIN until it is closed. All lines read
	 * from the program are printed to STDOUT.
	 */
	public void startReading() {
		ProcessBuilder pb = new ProcessBuilder(prog, device);
		BufferedReader in = null;
		try {
			Process pr = pb.start();
			in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			boolean readSomething = false;
			String line;
			while ( true ) {
				line = in.readLine();
				if ( line == null ) {
					break;
				}
				// the first line read is a status line...
				if ( readSomething ) {
					System.out.println("Got RFID line: " + line);
				} else {
					System.err.println(line);
				}
				readSomething = true;
			}
			if ( !readSomething ) {
				BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
				while ( (line = err.readLine()) != null ) {
					System.err.println(line);
				}
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
		}

	}

	public String getProg() {
		return prog;
	}

	/**
	 * Set the full path to the {@code net.solarnetwork.hw.rfid-reader} program
	 * to read the RFID data from STDIN.
	 * 
	 * @param prog
	 *        The program, e.g.
	 *        {@code /usr/local/bin/net.solarnetwork.hw.rfid-reader}.
	 */
	public void setProg(String prog) {
		this.prog = prog;
	}

	public String getDevice() {
		return device;
	}

	/**
	 * Set the device to pass to the {@code net.solarnetwork.hw.rfid-reader}
	 * program to read RFID data from.
	 * 
	 * @param device
	 *        The device, e.g. {@code /dev/net.solarnetwork.hw.rfid-reader}.
	 */
	public void setDevice(String device) {
		this.device = device;
	}

	/**
	 * Execute the reader. You can optionally pass the {@code prog} and
	 * {@code device} values on the command line. Calls {@link #startReading()}.
	 * 
	 * @param args
	 *        The optional settings.
	 */
	public static void main(String[] args) {
		RfidKeyboardReader reader = new RfidKeyboardReader();
		if ( args.length > 0 ) {
			reader.setProg(args[0]);
		}
		if ( args.length > 1 ) {
			reader.setDevice(args[1]);
		}
		reader.startReading();
	}

}
