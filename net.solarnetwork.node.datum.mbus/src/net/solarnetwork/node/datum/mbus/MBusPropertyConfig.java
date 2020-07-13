/* ==================================================================
 * MBusPropertyConfig.java - 09/07/2020 10:43:58 am
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

package net.solarnetwork.node.datum.mbus;

import java.math.BigDecimal;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
import net.solarnetwork.node.io.mbus.MBusDataDescription;

/**
 * Configuration for a single datum property to be set via M-Bus.
 * 
 * <p>
 * The {@link #getConfig()} value represents the mbus address to read from.
 * </p>
 * 
 * @author alex
 * @version 1.0
 */
public class MBusPropertyConfig extends GeneralDatumSamplePropertyConfig<Integer> {

	private MBusDataDescription dataType;
	private int wordLength;
	private BigDecimal unitMultiplier;
	private int decimalScale;

}
