/* ==================================================================
 * DefaultLocalStateService.java - 15/04/2025 9:11:27 am
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

package net.solarnetwork.node.runtime;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import net.solarnetwork.node.dao.LocalStateDao;
import net.solarnetwork.node.domain.LocalState;
import net.solarnetwork.node.service.LocalStateService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;

/**
 * Default implementation of {@link LocalStateService}.
 *
 * @author matt
 * @version 1.0
 */
public final class DefaultLocalStateService extends BaseIdentifiable implements LocalStateService {

	private final OptionalService<LocalStateDao> localStateDao;

	/**
	 * Constructor.
	 *
	 * @param localStateDao
	 *        the DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DefaultLocalStateService(OptionalService<LocalStateDao> localStateDao) {
		super();
		this.localStateDao = requireNonNullArgument(localStateDao, "localStateDao");
	}

	private LocalStateDao dao() {
		LocalStateDao dao = OptionalService.service(localStateDao);
		if ( dao != null ) {
			return dao;
		}
		throw new UnsupportedOperationException("LocalStateDao not available.");
	}

	@Override
	public Collection<LocalState> getAvailableLocalState() {
		return dao().getAll(null);
	}

	@Override
	public LocalState localStateForKey(String key) {
		return dao().get(key);
	}

	@Override
	public LocalState saveLocalState(LocalState state) {
		final LocalStateDao dao = dao();
		return dao.get(dao.save(state));
	}

	@Override
	public void deleteLocalState(String key) {
		dao().delete(new LocalState(key, null));
	}

}
