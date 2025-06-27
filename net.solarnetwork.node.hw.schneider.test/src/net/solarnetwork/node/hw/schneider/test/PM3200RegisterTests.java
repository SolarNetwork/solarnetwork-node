/* ==================================================================
 * PM3200RegisterTests.java - 21/01/2020 3:24:48 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.schneider.test;

import static net.solarnetwork.util.CollectionUtils.coveringIntRanges;
import static net.solarnetwork.util.IntRange.rangeOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.schneider.meter.PM3200Register;
import net.solarnetwork.util.IntRange;

/**
 * Test cases for the {@link PM3200Register} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PM3200RegisterTests {

	private static final Logger log = LoggerFactory.getLogger(PM3200RegisterTests.class);

	@Test
	public void reducedIntRangeSet() {
		// [0-12,102-144,4000-4014]
		List<IntRange> ranges = coveringIntRanges(PM3200Register.getRegisterAddressSet(), 64);
		if ( log.isDebugEnabled() ) {
			StringBuilder buf = new StringBuilder();
			for ( IntRange r : ranges ) {
				buf.append(String.format("-r %d -c %d\n", r.getMin(), r.length()));
			}
			log.debug("mbpoll listing:\n{}", buf);
		}
		assertThat("Reduced ranges", ranges,
				contains(rangeOf(29, 88), rangeOf(129, 134), rangeOf(1636, 1636), rangeOf(2013, 2015),
						rangeOf(2999, 3062), rangeOf(3063, 3110), rangeOf(3131, 3132),
						rangeOf(3203, 3242), rangeOf(3517, 3552)));
	}

}
