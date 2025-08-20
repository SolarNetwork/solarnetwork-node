/* ==================================================================
 * BaseSOEHandler.java - 8/08/2025 8:05:41 am
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

package net.solarnetwork.node.io.dnp3.impl;

import com.automatak.dnp3.AnalogInput;
import com.automatak.dnp3.AnalogOutputStatus;
import com.automatak.dnp3.BinaryInput;
import com.automatak.dnp3.BinaryOutputStatus;
import com.automatak.dnp3.Counter;
import com.automatak.dnp3.DNPTime;
import com.automatak.dnp3.DoubleBitBinaryInput;
import com.automatak.dnp3.FrozenCounter;
import com.automatak.dnp3.HeaderInfo;
import com.automatak.dnp3.IndexedValue;
import com.automatak.dnp3.ResponseInfo;
import com.automatak.dnp3.SOEHandler;

/**
 * Base implementation of {@link SOEHandler}, to process measurements from an
 * outstation.
 *
 * @author matt
 * @version 1.0
 */
public class BaseSOEHandler implements SOEHandler {

	/**
	 * Constructor.
	 */
	public BaseSOEHandler() {
		super();
	}

	@Override
	public void beginFragment(ResponseInfo info) {
		// no-op
	}

	@Override
	public void endFragment(ResponseInfo info) {
		// no-op
	}

	@Override
	public void processBI(HeaderInfo info, Iterable<IndexedValue<BinaryInput>> values) {
		// no-op
	}

	@Override
	public void processDBI(HeaderInfo info, Iterable<IndexedValue<DoubleBitBinaryInput>> values) {
		// no-op
	}

	@Override
	public void processAI(HeaderInfo info, Iterable<IndexedValue<AnalogInput>> values) {
		// no-op
	}

	@Override
	public void processC(HeaderInfo info, Iterable<IndexedValue<Counter>> values) {
		// no-op
	}

	@Override
	public void processFC(HeaderInfo info, Iterable<IndexedValue<FrozenCounter>> values) {
		// no-op
	}

	@Override
	public void processBOS(HeaderInfo info, Iterable<IndexedValue<BinaryOutputStatus>> values) {
		// no-op
	}

	@Override
	public void processAOS(HeaderInfo info, Iterable<IndexedValue<AnalogOutputStatus>> values) {
		// no-op
	}

	@Override
	public void processDNPTime(HeaderInfo info, Iterable<DNPTime> values) {
		// no-op
	}

}
