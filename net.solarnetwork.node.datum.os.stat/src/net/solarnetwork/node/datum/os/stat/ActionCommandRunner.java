/* ==================================================================
 * ActionCommandRunner.java - 13/08/2018 10:27:32 AM
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

package net.solarnetwork.node.datum.os.stat;

import java.util.List;
import java.util.Map;

/**
 * API for executing stat actions.
 * 
 * @author matt
 * @version 1.0
 */
public interface ActionCommandRunner {

	/**
	 * Execute the given action and return the results.
	 * 
	 * @param action
	 *        the action to perform
	 * @return the action results
	 */
	List<Map<String, String>> executeAction(StatAction action);

}
