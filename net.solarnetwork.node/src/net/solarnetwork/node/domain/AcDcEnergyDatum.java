/* ==================================================================
 * AcDcEnergyDatum.java - 3/09/2021 4:53:08 PM
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

package net.solarnetwork.node.domain;

/**
 * Standardized API for inverter type devices that deal with both alternating
 * and direct current related energy datum to implement.
 * 
 * <p>
 * Also, likes to rock.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface AcDcEnergyDatum
		extends AcEnergyDatum, DcEnergyDatum, net.solarnetwork.domain.datum.AcDcEnergyDatum {

}
