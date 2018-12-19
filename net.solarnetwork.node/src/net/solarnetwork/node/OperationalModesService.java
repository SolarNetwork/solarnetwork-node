/* ==================================================================
 * OperationalModesService.java - 20/12/2018 10:14:50 AM
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

package net.solarnetwork.node;

import java.util.Set;

/**
 * API for operational mode management.
 * 
 * @author matt
 * @version 1.0
 * @since 1.62
 */
public interface OperationalModesService {

	/**
	 * The instruction topic for activating a set of operational modes.
	 * 
	 * <p>
	 * The instruction is expected to provide one or more
	 * {@link #INSTRUCTION_PARAM_OPERATIONAL_MODE} parameters with the modes to
	 * be enabled.
	 * </p>
	 */
	String TOPIC_ENABLE_OPERATIONAL_MODES = "EnableOperationalModes";

	/**
	 * The instruction topic for deactivating a set of operational modes.
	 * 
	 * <p>
	 * The instruction is expected to provide one or more
	 * {@link #INSTRUCTION_PARAM_OPERATIONAL_MODE} parameters with the modes to
	 * be disabled.
	 * </p>
	 */
	String TOPIC_DISABLE_OPERATIONAL_MODES = "DisableOperationalModes";

	/**
	 * Instruction parameter for an operational mode name.
	 */
	String INSTRUCTION_PARAM_OPERATIONAL_MODE = "OpMode";

	/**
	 * An {@link org.osgi.service.event.Event} topic for when a the active
	 * operational modes have changed.
	 * 
	 * <p>
	 * This event must be sent <b>after</b> the a change to the operational
	 * modes has been persisted. The event is expected to provide a
	 * {@link #EVENT_PARAM_ACTIVE_OPERATIONAL_MODES} parameter with the modes
	 * that are effectively active after the change.
	 * </p>
	 */
	String EVENT_TOPIC_OPERATIONAL_MODES_CHANGED = "net/solarnetwork/node/OperationalModesService/MODES_CHANGED";

	/**
	 * Event parameter with a {@link Set} of active operational modes.
	 * 
	 * <p>
	 * If no user-defined modes are active, a "default" mode is assumed and an
	 * empty {@link Set} must be provided.
	 * </p>
	 */
	String EVENT_PARAM_ACTIVE_OPERATIONAL_MODES = "ActiveOpModes";

	/**
	 * Get the set of active operational modes.
	 * 
	 * @return the active operational modes, never {@literal null}
	 */
	Set<String> activeOperationalModes();

	/**
	 * Enable a set of operational modes.
	 * 
	 * @param modes
	 *        the modes to enable
	 * @return the active operational modes, after activating {@code modes},
	 *         never {@literal null}
	 */
	Set<String> enableOperationalModes(Set<String> modes);

	/**
	 * Disable a set of operational modes.
	 * 
	 * @param modes
	 *        the modes to disable
	 * @return the active operational modes, after deactivating {@code modes},
	 *         never {@literal null}
	 */
	Set<String> disableOperationalModes(Set<String> modes);

}
