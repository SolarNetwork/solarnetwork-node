/* ==================================================================
 * Tool.java - 1/09/2020 9:06:24 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ams.lux.tsl2591;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Testing/debugging tool.
 * 
 * @author matt
 * @version 1.0
 */
public class Tool {

	/**
	 * Command-line entry point.
	 * 
	 * @param args
	 *        the args; pass the device path to read from
	 */
	public static void main(String[] args) {
		if ( args.length < 1 ) {
			System.err.println("Pass device to read from, e.g. /dev/i2c-0");
			return;
		}
		try (Tsl2591Operations ops = Tsl2591Factory.createOperations(args[0])) {
			ops.setup(Gain.Medium, IntegrationTime.Time200ms);
			ops.enableAmbientLightSensor();
			Thread.sleep(IntegrationTime.Time200ms.getDuration());
			BigDecimal lux = ops.getLux();
			if ( lux != null ) {
				System.out.println(String.format("%s lux = %s", args[0], lux.toPlainString()));
			} else {
				System.err.println("No value returned.");
			}
		} catch ( IOException e ) {
			System.err.println("Comms problem: " + e.getMessage());
			e.printStackTrace(System.err);
		} catch ( InterruptedException e ) {
			System.err.println("Interrupted!");
		}
	}

}
