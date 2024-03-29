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

package net.solarnetwork.node.service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.osgi.service.event.Event;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.util.ObjectUtils;

/**
 * API for operational mode management.
 * 
 * @author matt
 * @version 2.1
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
	 * Instruction parameter for an expiration time.
	 * 
	 * @since 1.1
	 */
	String INSTRUCTION_PARAM_EXPIRATION = "Expiration";

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
	 * If no user-defined modes are active, a <i>default</i> mode is assumed and
	 * an empty {@link Set} must be provided.
	 * </p>
	 */
	String EVENT_PARAM_ACTIVE_OPERATIONAL_MODES = "ActiveOpModes";

	/**
	 * Test if a specific mode is active.
	 * 
	 * <p>
	 * Note that a {@literal null} or empty {@code mode} argument will be
	 * treated as testing if the <i>default</i> mode is active, which is always
	 * {@literal true}.
	 * </p>
	 * 
	 * @param mode
	 *        the mode to test; the mode can be prefixed with {@literal !} to
	 *        test if the given mode is <b>not</b> active
	 * @return {@literal true} if {@code mode} is active, or if {@code mode}
	 *         starts with {@literal !} then {@literal true} if {@code mode} is
	 *         <b>not</b> active
	 */
	boolean isOperationalModeActive(String mode);

	/**
	 * Get the set of active operational modes.
	 * 
	 * @return the active operational modes, never {@literal null}
	 */
	Set<String> activeOperationalModes();

	/**
	 * Get the set of active operational modes with expirations.
	 * 
	 * <p>
	 * This method returns only the operational modes that are both active and
	 * have an expiration configured (that is still in the future).
	 * </p>
	 * 
	 * @return the active operational modes that have expiration dates, never
	 *         {@literal null}
	 * @since 2.1
	 */
	Map<String, Long> activeOperationalModesWithExpirations();

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
	 * Enable a set of operational modes.
	 * 
	 * @param modes
	 *        the modes to enable
	 * @param expire
	 *        a date after which {@code modes} should be automatically disabled,
	 *        or {@literal null} for no expiration
	 * @return the active operational modes, after activating {@code modes},
	 *         never {@literal null}
	 * @since 2.0
	 */
	Set<String> enableOperationalModes(Set<String> modes, Instant expire);

	/**
	 * Disable a set of operational modes.
	 * 
	 * @param modes
	 *        the modes to disable
	 * @return the active operational modes, after deactivating {@code modes},
	 *         never {@literal null}
	 */
	Set<String> disableOperationalModes(Set<String> modes);

	/**
	 * Test if an event has an active mode parameter value.
	 * 
	 * <p>
	 * This method will look for a {@link #EVENT_PARAM_ACTIVE_OPERATIONAL_MODES}
	 * event parameter in the provided {@code event}. If the parameter is a
	 * {@link Set}, and it contains {@code mode} or {@code mode} is
	 * {@literal null} or empty, then {@literal true} will be returned.
	 * </p>
	 * 
	 * @param event
	 *        the event to inspect
	 * @param mode
	 *        the mode to test for
	 * @return {@literal true} if {@code mode} is included in the active modes
	 *         included in the event
	 */
	static boolean hasActiveOperationalMode(Event event, String mode) {
		Object v = (event != null ? event.getProperty(EVENT_PARAM_ACTIVE_OPERATIONAL_MODES) : null);
		if ( !(v instanceof Set<?>) ) {
			return false;
		}
		if ( mode == null || mode.isEmpty() ) {
			// default mode is always active
			return true;
		}
		mode = mode.toLowerCase();
		return ((Set<?>) v).contains(mode);
	}

	/**
	 * Parse an expiration value from an instruction parameter.
	 * 
	 * <p>
	 * The {@link #INSTRUCTION_PARAM_EXPIRATION} parameter is inspected for an
	 * epoch String value, which will be parsed and returned as a date instance
	 * if available.
	 * </p>
	 * 
	 * @param instruction
	 *        the instruction to parse the expiration parameter from
	 * @return the expiration date, or {@literal null} if none available
	 * @since 2.0
	 */
	static Instant expirationDate(Instruction instruction) {
		String s = (instruction != null ? instruction.getParameterValue(INSTRUCTION_PARAM_EXPIRATION)
				: null);
		if ( s == null ) {
			return null;
		}
		try {
			return Instant.ofEpochMilli(Long.parseLong(s));
		} catch ( NumberFormatException e ) {
			return null;
		}
	}

	/**
	 * Registered operational mode information.
	 * 
	 * @version 1.0
	 * @since 2.1
	 */
	final class OperationalModeInfo {

		private final String name;
		private final Set<String> tags;

		/**
		 * Constructor.
		 * 
		 * @param name
		 *        the name
		 * @throws IllegalArgumentException
		 *         if {@code name} is {@literal null}
		 */
		public OperationalModeInfo(String name) {
			this(name, null);
		}

		/**
		 * Constructor.
		 * 
		 * @param name
		 *        the name
		 * @param tags
		 *        the tags
		 * @throws IllegalArgumentException
		 *         if {@code name} is {@literal null}
		 */
		public OperationalModeInfo(String name, Set<String> tags) {
			super();
			this.name = ObjectUtils.requireNonNullArgument(name, "name");
			this.tags = (tags != null ? tags : Collections.emptySet());
		}

		/**
		 * Get the operational mode name.
		 * 
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Get the optional operational mode tags.
		 * 
		 * @return the tags
		 */
		public Set<String> getTags() {
			return tags;
		}

	}

	/**
	 * Register operational mode information for "well known" modes.
	 * 
	 * @param info
	 *        the information to register
	 * @return a unique registration ID
	 * @since 2.1
	 */
	UUID registerOperationalModeInfo(OperationalModeInfo info);

	/**
	 * Get all registered operational mode information.
	 * 
	 * @return all registered operational mode information
	 * @since 2.1
	 */
	Stream<OperationalModeInfo> registeredOperationalModes();

	/**
	 * Unregister previously registered operational mode information.
	 * 
	 * @param registrationId
	 *        the registration ID, previously returned from
	 *        {@link #registerOperationalModeInfo(OperationalModeInfo)}
	 * @return {@literal true} if the registration ID was found,
	 *         {@literal false} otherwise
	 * @since 2.1
	 */
	boolean unregisterOperationalModeInfo(UUID registrationId);

	/**
	 * An operational mode information predicate to match on a name prefix.
	 * 
	 * @param prefix
	 *        the prefix to match
	 * @return the predicate
	 * @since 2.1
	 */
	static Predicate<OperationalModeInfo> withPrefix(String prefix) {
		return (e) -> e.name.startsWith(prefix);
	}

	/**
	 * An operational mode information predicate to match on a tag.
	 * 
	 * @param tag
	 *        the tag to match
	 * @return the predicate
	 * @since 2.1
	 */
	static Predicate<OperationalModeInfo> withTag(String tag) {
		return (e) -> e.tags != null && e.tags.contains(tag);
	}

	/**
	 * An operational mode information predicate to match on a tag.
	 * 
	 * @param prefix
	 *        the prefix to match
	 * @param tag
	 *        the tag to match
	 * @return the predicate
	 * @since 2.1
	 */
	static Predicate<OperationalModeInfo> withPrefixAndTag(String prefix, String tag) {
		return (e) -> e.name.startsWith(prefix) && e.tags != null && e.tags.contains(tag);
	}

}
