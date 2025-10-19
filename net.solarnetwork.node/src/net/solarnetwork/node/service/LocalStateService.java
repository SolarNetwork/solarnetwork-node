/* ==================================================================
 * LocalStateService.java - 15/04/2025 9:10:31 am
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

package net.solarnetwork.node.service;

import java.util.Collection;
import net.solarnetwork.node.domain.LocalState;

/**
 * Service API for {@link LocalState} management.
 *
 * @author matt
 * @version 1.1
 */
public interface LocalStateService extends CsvConfigurableBackupService {

	/**
	 * Get all available local state.
	 *
	 * @return the state entities, never {@code null}
	 */
	Collection<LocalState> getAvailableLocalState();

	/**
	 * Get local state for a given key.
	 *
	 * @param key
	 *        the key of the local state to get
	 * @return the state entity, or {@code null} if not found
	 */
	LocalState localStateForKey(String key);

	/**
	 * Save a local state entity.
	 *
	 * @param state
	 *        the state to save
	 * @return the persisted state
	 */
	LocalState saveLocalState(LocalState state);

	/**
	 * Delete a local state entity.
	 *
	 * @param key
	 *        the identifier of the state to delete
	 */
	void deleteLocalState(String key);

}
