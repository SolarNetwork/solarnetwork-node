/* ==================================================================
 * DatumHistorianSelector.java - 13 Nov 2026 2:58:41 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.support;

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.jspecify.annotations.Nullable;
import jakarta.servlet.ServletRequest;
import net.solarnetwork.node.service.FilterableDatumHistorian;
import net.solarnetwork.service.OptionalService;

/**
 * Select the datum historian to use for a request.
 *
 * <p>
 * The filtered historian is used unless the request was selected for the
 * unfiltered historian by {@link HistorianSelectorFilter}.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.4
 */
public class DatumHistorianSelector {

	private final OptionalService<FilterableDatumHistorian> filteredHistorian;
	private final OptionalService<FilterableDatumHistorian> unfilteredHistorian;

	/**
	 * Constructor.
	 *
	 * @param filteredHistorian
	 *        the filtered datum historian
	 * @param unfilteredHistorian
	 *        the unfiltered datum historian
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DatumHistorianSelector(OptionalService<FilterableDatumHistorian> filteredHistorian,
			OptionalService<FilterableDatumHistorian> unfilteredHistorian) {
		super();
		this.filteredHistorian = requireNonNullArgument(filteredHistorian, "filteredHistorian");
		this.unfilteredHistorian = requireNonNullArgument(unfilteredHistorian, "unfilteredHistorian");
	}

	/**
	 * Get the datum historian to use for a request.
	 *
	 * @param request
	 *        the request
	 * @return the historian, or {@code null} if that historian is not available
	 */
	public @Nullable FilterableDatumHistorian historian(ServletRequest request) {
		final String role = HistorianSelectorFilter.historianRole(request);
		return service(HistorianSelectorFilter.UNFILTERED_ROLE.equals(role) ? unfilteredHistorian
				: filteredHistorian);
	}

}
