/* ==================================================================
 * LocalStateDao.java - 14/04/2025 7:23:15 am
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

package net.solarnetwork.node.dao;

import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.node.domain.LocalState;

/**
 * DAO API for {@link LocalState} entities.
 *
 * @author matt
 * @version 1.0
 * @since 3.23
 */
public interface LocalStateDao extends GenericDao<LocalState, String> {

	/**
	 * Persist an entity, creating if does not exist or updating if the current
	 * value equals {@code expectedValue}.
	 *
	 * @param entity
	 *        the domain object so store
	 * @param expectedValue
	 *        if a record exists, then only update its value if it currently
	 *        holds this expected value; otherwise leave unchanged
	 * @return the final stored object
	 */
	LocalState compareAndSave(LocalState entity, Object expectedValue);

	/**
	 * Persist an entity if it does not exist or its value differs from the
	 * currently persisted value.
	 *
	 * @param entity
	 *        the entity to save if changed
	 * @return the final stored object
	 */
	LocalState compareAndChange(LocalState entity);

	/**
	 * Persist an entity, creating if does not exist or updating if it does,
	 * returning the previously stored value.
	 *
	 * @param entity
	 *        the domain object so store
	 * @return the previously stored object, or {@code null} if inserted
	 */
	LocalState getAndSave(LocalState entity);

}
