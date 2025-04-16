/* ==================================================================
 * LocalStateOperations.java - 14/04/2025 3:17:35 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import java.util.Objects;

/**
 * Operations to work with {@link LocalState} entities.
 *
 * @author matt
 * @version 1.0
 * @since 3.23
 */
public interface LocalStateOperations {

	/**
	 * Get a local state value.
	 *
	 * @param key
	 *        the key of the state to get
	 * @return the state value, or {@code null}
	 */
	default Object localState(String key) {
		return localState(key, null);
	}

	/**
	 * Get a local state value.
	 *
	 * @param key
	 *        the key of the state to get
	 * @param defaultValue
	 *        the value to return if {@code key} does not exist
	 * @return the state value, or {@code defaultValue}
	 */
	Object localState(String key, Object defaultValue);

	/**
	 * Save a local state value.
	 *
	 * <p>
	 * The type will be detected based on the given value.
	 * </p>
	 *
	 * @param key
	 *        the key of the state to set
	 * @param value
	 *        the value to set
	 * @return the saved value
	 */
	default Object saveLocalState(String key, Object value) {
		return saveLocalState(key, LocalStateType.detect(value), value);
	}

	/**
	 * Save a local state value.
	 *
	 * @param key
	 *        the key of the state to set
	 * @param type
	 *        the {@link LocalStateType} key to use
	 * @param value
	 *        the value to set
	 * @return the saved value
	 */
	default Object saveLocalState(String key, String type, Object value) {
		return saveLocalState(key, LocalStateType.valueOf(type), value);
	}

	/**
	 * Save a local state value.
	 *
	 * @param key
	 *        the key of the state to set
	 * @param type
	 *        the type to use
	 * @param value
	 *        the value to set
	 * @return the saved value
	 */
	Object saveLocalState(String key, LocalStateType type, Object value);

	/**
	 * Save a local state value.
	 *
	 * <p>
	 * The type will be detected based on the given value.
	 * </p>
	 *
	 * @param key
	 *        the key of the state to set
	 * @param value
	 *        the value to set
	 * @param expectedValue
	 *        the value the state must exist as in order to update it to
	 *        {@code value}
	 * @return the final state value: the given value if it currently has the
	 *         expected value, otherwise the previously held value
	 */
	default Object saveLocalState(String key, Object value, Object expectedValue) {
		return saveLocalState(key, LocalStateType.detect(value), value, expectedValue);
	}

	/**
	 * Save a local state value.
	 *
	 * @param key
	 *        the key of the state to set
	 * @param type
	 *        the {@link LocalStateType} key to use
	 * @param value
	 *        the value to set
	 * @param expectedValue
	 *        the value the state must exist as in order to update it to
	 *        {@code value}
	 * @return the final state value: the given value if it currently has the
	 *         expected value, otherwise the previously held value
	 */
	default Object saveLocalState(String key, String type, Object value, Object expectedValue) {
		return saveLocalState(key, LocalStateType.valueOf(type), value, expectedValue);
	}

	/**
	 * Save a local state value.
	 *
	 * @param key
	 *        the key of the state to set
	 * @param type
	 *        the type to use
	 * @param value
	 *        the value to set
	 * @param expectedValue
	 *        the value the state must exist as in order to update it to
	 *        {@code value}
	 * @return the final state value: the given value if it currently has the
	 *         expected value, otherwise the previously held value
	 */
	Object saveLocalState(String key, LocalStateType type, Object value, Object expectedValue);

	/**
	 * Save a local state value, returning its previous value.
	 *
	 * <p>
	 * The type will be detected based on the given value.
	 * </p>
	 *
	 * @param key
	 *        the key of the state to set
	 * @param value
	 *        the value to set
	 * @return the previous state value
	 */
	default Object getAndSaveLocalState(String key, Object value) {
		return getAndSaveLocalState(key, LocalStateType.detect(value), value);
	}

	/**
	 * Save a local state value, returning its previous value.
	 *
	 * @param key
	 *        the key of the state to set
	 * @param type
	 *        the {@link LocalStateType} key to use
	 * @param value
	 *        the value to set
	 * @return the previous state value
	 */
	default Object getAndSaveLocalState(String key, String type, Object value) {
		return getAndSaveLocalState(key, LocalStateType.valueOf(type), value);
	}

	/**
	 * Save a local state value, returning its previous value.
	 *
	 * @param key
	 *        the key of the state to set
	 * @param type
	 *        the type to use
	 * @param value
	 *        the value to set
	 * @return the previous state value
	 */
	Object getAndSaveLocalState(String key, LocalStateType type, Object value);

	/**
	 * Save a local state value, returning {@code true} if {@code value} is
	 * different from the state's previous value.
	 *
	 * <p>
	 * The type will be detected based on the given value.
	 * </p>
	 *
	 * @param key
	 *        the key of the state to update
	 * @param value
	 *        the value to set
	 * @return {@code true} if {@code value} is <b>not</b> the same as the
	 *         previously persisted state value for key {@code key}, or
	 *         {@code false} if {@code value} is unchanged from the previous
	 *         value
	 */
	default boolean changeLocalState(String key, Object value) {
		return !Objects.equals(value, getAndSaveLocalState(key, value));
	}

	/**
	 * Save a local state value, returning {@code true} if {@code value} is
	 * different from the state's previous value.
	 *
	 * @param key
	 *        the key of the state to update
	 * @param type
	 *        the {@link LocalStateType} key to use
	 * @param value
	 *        the value to set
	 * @return {@code true} if {@code value} is <b>not</b> the same as the
	 *         previously persisted state value for key {@code key}, or
	 *         {@code false} if {@code value} is unchanged from the previous
	 *         value
	 */
	default boolean changeLocalState(String key, String type, Object value) {
		return !Objects.equals(value, getAndSaveLocalState(key, type, value));
	}

	/**
	 * Save a local state value, returning {@code true} if {@code value} is
	 * different from the state's previous value.
	 *
	 * @param key
	 *        the key of the state to update
	 * @param type
	 *        the type to use
	 * @param value
	 *        the value to set
	 * @return {@code true} if {@code value} is <b>not</b> the same as the
	 *         previously persisted state value for key {@code key}, or
	 *         {@code false} if {@code value} is unchanged from the previous
	 *         value
	 */
	default boolean changeLocalState(String key, LocalStateType type, Object value) {
		return !Objects.equals(value, getAndSaveLocalState(key, type, value));
	}

}
