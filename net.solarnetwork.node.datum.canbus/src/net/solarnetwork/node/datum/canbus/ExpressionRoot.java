/* ==================================================================
 * ExpressionRoot.java - 5/04/2021 5:08:28 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.canbus;

import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.canbus.CanbusData;
import net.solarnetwork.node.io.canbus.CanbusSignalReference;
import net.solarnetwork.node.io.canbus.support.SimpleCanbusSignalReference;
import net.solarnetwork.support.ExpressionService;

/**
 * An object to use as the "root" for {@link ExpressionService} evaluation.
 * 
 * @author matt
 * @version 1.1
 * @since 1.5
 */
public class ExpressionRoot extends net.solarnetwork.node.domain.ExpressionRoot {

	private final CanbusData sample;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 * @param sample
	 *        the current Canbus sample data
	 */
	public ExpressionRoot(GeneralNodeDatum datum, CanbusData sample) {
		super(datum);
		this.sample = sample;
	}

	/**
	 * Get the sample data.
	 * 
	 * @return the sample
	 */
	public CanbusData getSample() {
		return sample;
	}

	/**
	 * Get a Canbus frame property value.
	 * 
	 * @param address
	 *        the address
	 * @param dataType
	 *        the data type
	 * @param byteOrdering
	 *        the byte ordering
	 * @param bitOffset
	 *        the bit offset
	 * @param bitLength
	 *        the bit length
	 * @return the value, or {@literal null}
	 */
	public Number propValue(int address, BitDataType dataType, ByteOrdering byteOrdering, int bitOffset,
			int bitLength) {
		CanbusSignalReference ref = new SimpleCanbusSignalReference(address, dataType, byteOrdering,
				bitOffset, bitLength);
		return sample.getNumber(ref);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CanbusExpressionRoot{");
		if ( sample != null ) {
			builder.append("sample=");
			builder.append(sample.dataDebugString());
			builder.append(", ");
		}
		builder.append(super.toString());
		builder.append("}");
		return builder.toString();
	}

}
