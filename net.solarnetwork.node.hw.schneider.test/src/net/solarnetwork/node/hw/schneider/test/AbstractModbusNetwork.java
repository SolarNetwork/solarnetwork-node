/* ==================================================================
 * AbstractModbusNetwork.java - 28/02/2014 3:13:38 PM
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

package net.solarnetwork.node.hw.schneider.test;

import java.io.IOException;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;

/**
 * Abstract implementation of {@link ModbusNetwork} to simplify testing.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractModbusNetwork implements ModbusNetwork {

	@Override
	public String getUID() {
		return null;
	}

	@Override
	public String getGroupUID() {
		return null;
	}

	@Override
	public <T> T performAction(ModbusConnectionAction<T> action, int unitId) throws IOException {
		return null;
	}

	@Override
	public ModbusConnection createConnection(int unitId) {
		return null;
	}
}
