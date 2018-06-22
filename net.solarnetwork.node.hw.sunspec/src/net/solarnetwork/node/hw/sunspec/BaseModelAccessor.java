/* ==================================================================
 * BaseModelAccessor.java - 22/05/2018 10:39:06 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec;

import java.math.BigDecimal;
import java.math.BigInteger;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Base class for {@link ModelAccessor} implementations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseModelAccessor implements ModelAccessor {

	private final ModelData data;
	private final int baseAddress;
	private final int blockAddress;
	private final ModelId modelId;

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public BaseModelAccessor(ModelData data, int baseAddress, ModelId modelId) {
		super();
		this.baseAddress = baseAddress;
		this.blockAddress = baseAddress + 2;
		this.data = data;
		this.modelId = modelId;
	}

	@Override
	public long getDataTimestamp() {
		return data.getDataTimestamp();
	}

	@Override
	public int getBaseAddress() {
		return baseAddress;
	}

	@Override
	public int getBlockAddress() {
		return blockAddress;
	}

	@Override
	public ModelId getModelId() {
		return modelId;
	}

	@Override
	public int getModelLength() {
		return data.getNumber(ModelRegister.ModelLength, baseAddress).intValue();
	}

	/**
	 * Get the data.
	 * 
	 * @return the data
	 */
	protected ModelData getData() {
		return data;
	}

	/**
	 * Get a decimal value suitable for multiplication against a data property
	 * for a scale factor.
	 * 
	 * @param ref
	 *        the block address relative reference to the scale factor register,
	 *        which is expected to contain a signed integer from -10..10
	 * @return the decimal multiplier to use, never {@literal null}
	 */
	protected BigDecimal getScaleFactor(ModbusReference ref) {
		Number n = data.getNumber(ref, blockAddress);
		if ( n == null ) {
			return BigDecimal.ONE;
		}
		int factor = n.intValue();
		if ( factor == 0 ) {
			return BigDecimal.ONE;
		}
		return new BigDecimal(BigInteger.ONE, -factor);
	}

	/**
	 * Get a scaled data property value.
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param scaleRef
	 *        the block address relative reference to the scale factor
	 * @return the scaled value, or {@literal null} if not available
	 */
	public BigDecimal getScaledValue(ModbusReference dataRef, ModbusReference scaleRef) {
		Number v = data.getNumber(dataRef, blockAddress);
		if ( v == null ) {
			return null;
		}
		BigDecimal sf = getScaleFactor(scaleRef);
		BigDecimal d = new BigDecimal(v.toString());
		if ( sf.equals(BigDecimal.ONE) || d.compareTo(BigDecimal.ZERO) == 0 ) {
			return d;
		}
		return d.multiply(sf);
	}
}
