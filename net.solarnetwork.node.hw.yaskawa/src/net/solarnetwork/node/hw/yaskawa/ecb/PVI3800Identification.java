/* ==================================================================
 * PVI3800Identification.java - 18/05/2018 5:12:32 PM
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

package net.solarnetwork.node.hw.yaskawa.ecb;

import static net.solarnetwork.node.hw.yaskawa.ecb.PVI3800Command.InfoReadIdentification;
import java.io.UnsupportedEncodingException;

/**
 * PVI-3800 system identification information.
 * 
 * @author matt
 * @version 1.0
 */
public class PVI3800Identification {

	private final int countryCode;
	private final int variant;
	private final String description;

	/**
	 * Construct from a packet.
	 * 
	 * @param p
	 *        the packet to decode
	 * @throws IllegalArgumentException
	 *         if the packet is not appropriate
	 */
	public PVI3800Identification(Packet p) {
		super();
		if ( p == null || p.getCommand() != InfoReadIdentification.getCommand()
				|| p.getSubCommand() != InfoReadIdentification.getSubCommand() ) {
			throw new IllegalArgumentException("Wrong packet command");
		}
		byte[] body = p.getBody();
		if ( body == null || body.length < 2 ) {
			throw new IllegalArgumentException("Not enough data.");
		}
		this.countryCode = body[0] & 0xFF;
		this.variant = body[1] & 0xFF;
		byte[] stringData = new byte[0];
		for ( int i = 2; i < body.length; i++ ) {
			if ( body[i] == 0x00 ) {
				stringData = new byte[i - 2];
				System.arraycopy(body, 2, stringData, 0, stringData.length);
				break;
			}
		}
		try {
			this.description = new String(stringData, "US-ASCII");
		} catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the country code.
	 * 
	 * @return the country code
	 */
	public int getCountryCode() {
		return countryCode;
	}

	/**
	 * Get the variant.
	 * 
	 * @return the variant
	 */
	public int getVariant() {
		return variant;
	}

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
