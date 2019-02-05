/* ==================================================================
 * ExpressionRoot.java - 5/02/2019 4:29:31 pm
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

package net.solarnetwork.node.datum.egauge.ws.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.support.ExpressionService;

/**
 * An object to use as the "root" for {@link ExpressionService} evaluation.
 * 
 * @author matt
 * @version 1.0
 */
public class ExpressionRoot {

	private final List<DataRegister> data;
	private final Map<String, DataRegister> reg;

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the data
	 */
	public ExpressionRoot(List<DataRegister> data) {
		super();
		this.data = data == null ? Collections.emptyList() : data;

		if ( data != null ) {
			Map<String, DataRegister> m = new LinkedHashMap<>(data.size());
			for ( DataRegister r : data ) {
				if ( r.getName() != null ) {
					m.put(r.getName(), r);
				}
			}
			this.reg = m;
		} else {
			this.reg = Collections.emptyMap();
		}

	}

	/**
	 * Get the list of registers.
	 * 
	 * @return the registers (never {@literal null})
	 */
	public List<DataRegister> getData() {
		return data;
	}

	/**
	 * Get a mapping of register names to associated register data values.
	 * 
	 * @return the register mapping, never {@literal null}
	 */
	public Map<String, DataRegister> getRegisters() {
		return this.reg;
	}

}
