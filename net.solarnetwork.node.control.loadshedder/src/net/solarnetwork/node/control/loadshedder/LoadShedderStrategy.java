/* ==================================================================
 * LoadShedderStrategy.java - 29/06/2015 7:02:05 am
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

package net.solarnetwork.node.control.loadshedder;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.node.domain.datum.EnergyDatum;
import net.solarnetwork.service.Identifiable;

/**
 * API for a load shedding strategy.
 * 
 * @author matt
 * @version 2.0
 */
public interface LoadShedderStrategy extends Identifiable {

	/**
	 * Evaluate a set of rules for a specific date and power conditions.
	 * 
	 * @param rules
	 *        The rules to evaluate.
	 * @param limitStatuses
	 *        A map of control IDs to associated status objects.
	 * @param date
	 *        The date with which to evaluate the rules at. Generally this will
	 *        be the current date.
	 * @param powerSamples
	 *        A set of power samples, ordered from most recent to oldest.
	 * @return The action to execute, or {@literal null} if no action is needed.
	 */
	Collection<LoadShedAction> evaulateRules(List<LoadShedControlConfig> rules,
			Map<String, LoadShedControlInfo> limitStatuses, long date,
			Collection<EnergyDatum> powerSamples);

	/**
	 * Get a status message for a specific control.
	 * 
	 * @param info
	 *        The control info to get a status message for.
	 * @param locale
	 *        The locale of the message.
	 * @return The message, or {@literal null} if nothing to report.
	 */
	String getStatusMessage(LoadShedControlInfo info, Locale locale);

}
