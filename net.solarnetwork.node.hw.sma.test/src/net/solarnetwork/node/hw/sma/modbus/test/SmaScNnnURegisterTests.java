/* ==================================================================
 * SmaScNnnURegisterTests.java - 14/09/2020 3:23:36 PM
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

package net.solarnetwork.node.hw.sma.modbus.test;

import static net.solarnetwork.util.IntRange.rangeOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sma.modbus.SmaScNnnURegister;
import net.solarnetwork.util.CollectionUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for the {@link SmaScNnnURegister} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaScNnnURegisterTests {

	private static final Logger log = LoggerFactory.getLogger(SmaScNnnURegisterTests.class);

	@Test
	public void infoRegisterSet() {
		IntRangeSet orig = SmaScNnnURegister.INFO_REGISTER_ADDRESS_SET;
		List<IntRange> covering = CollectionUtils.coveringIntRanges(orig, 64);
		log.debug("Info range set: {}", covering);
		assertThat("Info covered ranges", covering, contains(rangeOf(30057, 30058)));
	}

	@Test
	public void dataRegisterSet() {
		IntRangeSet orig = SmaScNnnURegister.DATA_REGISTER_ADDRESS_SET;
		List<IntRange> covering = CollectionUtils.coveringIntRanges(orig, 64);
		log.debug("Data range set: {}", covering);
		assertThat("Data covered ranges", covering,
				contains(rangeOf(30193, 30248), rangeOf(30257, 30266), rangeOf(30513, 30528),
						rangeOf(30769, 30814), rangeOf(30835, 30842), rangeOf(34101, 34126)));
	}

}
