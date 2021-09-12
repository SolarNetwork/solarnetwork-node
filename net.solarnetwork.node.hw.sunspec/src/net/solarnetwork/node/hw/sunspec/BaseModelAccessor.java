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

import static net.solarnetwork.util.NumberUtils.maximumDecimalScale;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Base class for {@link ModelAccessor} implementations.
 * 
 * @author matt
 * @version 2.0
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
		if ( modelId == null ) {
			throw new IllegalArgumentException("ModelId value is required");
		}
		this.baseAddress = baseAddress;
		this.blockAddress = baseAddress + 2;
		this.data = data;
		this.modelId = modelId;
	}

	@Override
	public String toString() {
		return modelId.getDescription();
	}

	@Override
	public Instant getDataTimestamp() {
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
		return getScaleFactor(ref, blockAddress);
	}

	/**
	 * Get a decimal value suitable for multiplication against a data property
	 * for a scale factor.
	 * 
	 * @param ref
	 *        the block address relative reference to the scale factor register,
	 *        which is expected to contain a signed integer from -10..10
	 * @param offset
	 *        the address offset to add to {@link ModbusReference#getAddress()}
	 * @return the decimal multiplier to use, never {@literal null}
	 * @since 1.2
	 */
	protected BigDecimal getScaleFactor(ModbusReference ref, int offset) {
		Number n = data.getNumber(ref, offset);
		if ( n == null ) {
			return BigDecimal.ONE;
		}
		int factor = n.intValue();
		if ( factor == 0 || factor == ModelData.NAN_SUNSSF16 ) {
			return BigDecimal.ONE;
		}
		return new BigDecimal(BigInteger.ONE, -factor);
	}

	/**
	 * Get a bitfield register value.
	 * 
	 * @param ref
	 *        the block address relative reference to the bitfield register(s)
	 * @return the value, never {@literal null}
	 * @since 1.1
	 */
	protected Number getBitfield(ModbusReference ref) {
		return getBitfield(ref, blockAddress);
	}

	/**
	 * Get a bitfield register value.
	 * 
	 * @param ref
	 *        the block address relative reference to the bitfield register(s)
	 * @param offset
	 *        the address offset to add to {@link ModbusReference#getAddress()}
	 * @return the value, never {@literal null}
	 * @since 1.2
	 */
	protected Number getBitfield(ModbusReference ref, int offset) {
		Number v = data.getNumber(ref, offset);
		if ( v == null ) {
			return 0;
		}

		if ( ref instanceof SunspecModbusReference ) {
			DataClassification classification = ((SunspecModbusReference) ref).getClassification();
			if ( DataClassification.Bitfield == classification ) {
				// for bit fields, if the most significant bit is set, it is NaN
				if ( ref.getWordLength() == 1
						&& (v.intValue() & ModelData.NAN_BITFIELD16) == ModelData.NAN_BITFIELD16 ) {
					return 0;
				} else if ( ref.getWordLength() == 2
						&& (v.intValue() & ModelData.NAN_BITFIELD32) == ModelData.NAN_BITFIELD32 ) {
					return 0;
				}
			}
		}

		return v;
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
		return getScaledValue(dataRef, scaleRef, blockAddress, blockAddress);
	}

	/**
	 * Get a scaled data property value.
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param scaleRef
	 *        the block address relative reference to the scale factor
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @param scaleOffset
	 *        the scale address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @return the scaled value, or {@literal null} if not available
	 */
	public BigDecimal getScaledValue(ModbusReference dataRef, ModbusReference scaleRef, int dataOffset,
			int scaleOffset) {
		Number v = getValue(dataRef, dataOffset);
		if ( v == null ) {
			return null;
		}

		BigDecimal sf = getScaleFactor(scaleRef, scaleOffset);
		BigDecimal d = new BigDecimal(v.toString());
		if ( sf.equals(BigDecimal.ONE) || d.compareTo(BigDecimal.ZERO) == 0 ) {
			return d;
		}
		return d.multiply(sf);
	}

	/**
	 * Get an non-scaled data property value.
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @return the value, or {@literal null} if not available
	 * @since 1.2
	 */
	public Number getValue(ModbusReference dataRef) {
		return getValue(dataRef, blockAddress);
	}

	/**
	 * Get a non-scaled data property value.
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @return the value, or {@literal null} if not available
	 * @since 1.2
	 */
	public Number getValue(ModbusReference dataRef, int dataOffset) {
		Number v = data.getNumber(dataRef, dataOffset);
		if ( v == null ) {
			return null;
		}

		DataClassification classification = null;
		if ( dataRef instanceof SunspecModbusReference ) {
			classification = ((SunspecModbusReference) dataRef).getClassification();
		}

		// check for NaN
		if ( DataClassification.Accumulator == classification && v.intValue() == 0 ) {
			return null;
		} else if ( DataClassification.Bitfield == classification ) {
			// for bit fields, if the most significant bit is set, it is NaN
			if ( dataRef.getWordLength() == 1
					&& (v.intValue() & ModelData.NAN_BITFIELD16) == ModelData.NAN_BITFIELD16 ) {
				return null;
			} else if ( dataRef.getWordLength() == 2
					&& (v.intValue() & ModelData.NAN_BITFIELD32) == ModelData.NAN_BITFIELD32 ) {
				return null;
			}
		}

		switch (dataRef.getDataType()) {
			case Int16:
				if ( (v.intValue() & 0xFFFF) == ModelData.NAN_INT16 ) {
					return null;
				}
				break;

			case Int32:
				if ( (v.intValue() & 0xFFFFFFFF) == ModelData.NAN_INT32 ) {
					return null;
				}
				break;

			case Int64:
				if ( (v.longValue() & 0xFFFFFFFFFFFFFFFFL) == ModelData.NAN_INT64 ) {
					return null;
				}
				break;

			case UInt16:
				if ( v.intValue() == ModelData.NAN_UINT16 ) {
					return null;
				}
				break;

			case UInt32:
				if ( v.longValue() == ModelData.NAN_UINT32 ) {
					return null;
				}
				break;

			case Float32:
				if ( v.floatValue() == ModelData.NAN_FLOAT32 ) {
					return null;
				}
				break;

			default:
				// continue
		}
		return v;
	}

	/**
	 * Get a float data property value.
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @return the value, or {@literal null} if not available
	 * @since 1.2
	 */
	public Float getFloatValue(ModbusReference dataRef) {
		return getFloatValue(dataRef, blockAddress);
	}

	/**
	 * Get a float data property value.
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @return the value, or {@literal null} if not available
	 * @since 1.2
	 */
	public Float getFloatValue(ModbusReference dataRef, int dataOffset) {
		Number n = getValue(dataRef, dataOffset);
		return (n != null ? n.floatValue() : null);
	}

	/**
	 * Get an integer data property value.
	 * 
	 * <p>
	 * The value will be rounded, if necessary.
	 * </p>
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @return the value, or {@literal null} if not available
	 * @since 1.2
	 */
	public Integer getIntegerValue(ModbusReference dataRef) {
		return getIntegerValue(dataRef, blockAddress);
	}

	/**
	 * Get an integer data property value.
	 * 
	 * <p>
	 * The value will be rounded, if necessary.
	 * </p>
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @return the value, or {@literal null} if not available
	 * @since 1.2
	 */
	public Integer getIntegerValue(ModbusReference dataRef, int dataOffset) {
		Number n = maximumDecimalScale(getValue(dataRef, dataOffset), 0);
		return (n != null ? n.intValue() : null);
	}

	/**
	 * Get a long data property value.
	 * 
	 * <p>
	 * The value will be rounded, if necessary.
	 * </p>
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @return the value, or {@literal null} if not available
	 * @since 1.2
	 */
	public Long getLongValue(ModbusReference dataRef) {
		return getLongValue(dataRef, blockAddress);
	}

	/**
	 * Get a long data property value.
	 * 
	 * <p>
	 * The value will be rounded, if necessary.
	 * </p>
	 * 
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @return the value, or {@literal null} if not available
	 * @since 1.2
	 */
	public Long getLongValue(ModbusReference dataRef, int dataOffset) {
		Number n = maximumDecimalScale(getValue(dataRef, dataOffset), 0);
		return (n != null ? n.longValue() : null);
	}

}
