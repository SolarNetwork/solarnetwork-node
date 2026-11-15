/* ==================================================================
 * DatumHistorianSelectorTests.java - 13 Nov 2026 4:29:20 pm
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

package net.solarnetwork.node.setup.web.support.test;

import static net.solarnetwork.node.setup.web.support.HistorianSelectorFilter.HISTORIAN_ROLE_ATTR;
import static net.solarnetwork.node.setup.web.support.HistorianSelectorFilter.UNFILTERED_ROLE;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import net.solarnetwork.node.service.FilterableDatumHistorian;
import net.solarnetwork.node.setup.web.support.DatumHistorianSelector;
import net.solarnetwork.service.OptionalService;

/**
 * Test cases for the {@link DatumHistorianSelector} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class DatumHistorianSelectorTests {

	@Mock
	private OptionalService<FilterableDatumHistorian> filteredService;

	@Mock
	private OptionalService<FilterableDatumHistorian> unfilteredService;

	@Mock
	private FilterableDatumHistorian filtered;

	@Mock
	private FilterableDatumHistorian unfiltered;

	private DatumHistorianSelector selector;

	@Before
	public void setup() {
		selector = new DatumHistorianSelector(filteredService, unfilteredService);
	}

	@Test
	public void filteredByDefault() {
		// GIVEN
		given(filteredService.service()).willReturn(filtered);

		// WHEN
		final FilterableDatumHistorian result = selector.historian(new MockHttpServletRequest());

		// THEN
		and.then(result).as("Filtered historian used by default").isSameAs(filtered);
		then(unfilteredService).shouldHaveNoInteractions();
	}

	@Test
	public void unfilteredRole() {
		// GIVEN
		given(unfilteredService.service()).willReturn(unfiltered);

		final MockHttpServletRequest req = new MockHttpServletRequest();
		req.setAttribute(HISTORIAN_ROLE_ATTR, UNFILTERED_ROLE);

		// WHEN
		final FilterableDatumHistorian result = selector.historian(req);

		// THEN
		and.then(result).as("Unfiltered historian used for unfiltered role").isSameAs(unfiltered);
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void unavailable() {
		// GIVEN
		given(filteredService.service()).willReturn(null);

		// WHEN
		final FilterableDatumHistorian result = selector.historian(new MockHttpServletRequest());

		// THEN
		and.then(result).as("No historian when service not available").isNull();
	}

}
