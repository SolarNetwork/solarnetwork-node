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

import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import static net.solarnetwork.util.NumberUtils.maximumDecimalScale;
import static net.solarnetwork.util.NumberUtils.unsignedNumber;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;

/**
 * Base class for {@link ModelAccessor} implementations.
 *
 * @author matt
 * @version 2.1
 */
public abstract class BaseModelAccessor implements ModelAccessor {

	/** Cached "not implemented" value for a SunSpec "uint64" data type. */
	private static final Number NAN_UINT64 = nonnull(unsignedNumber(ModelData.NAN_UINT64), "NAN_UINT64");

	/** The largest valid SunSpec "uint64" value. */
	private static final BigInteger UINT64_MAX = new BigInteger("FFFFFFFFFFFFFFFE", 16);

	/** The smallest valid SunSpec "sunssf" value. */
	private static final int SCALE_FACTOR_MIN = -10;

	/** The largest valid SunSpec "sunssf" value. */
	private static final int SCALE_FACTOR_MAX = 10;

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
		this.data = requireNonNullArgument(data, "data");
		this.modelId = requireNonNullArgument(modelId, "modelId");
	}

	@Override
	public String toString() {
		return modelId.getDescription();
	}

	@Override
	public @Nullable Instant getDataTimestamp() {
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
		Number n = data.getNumber(ModelRegister.ModelLength, baseAddress);
		return (n != null ? n.intValue() : 0);
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
	 * @return the decimal multiplier to use, or {@code null} if the scale
	 *         factor is not available or not implemented
	 * @see #getScaleFactor(ModbusReference, int)
	 */
	protected @Nullable BigDecimal getScaleFactor(ModbusReference ref) {
		return getScaleFactor(ref, blockAddress);
	}

	/**
	 * Get a decimal value suitable for multiplication against a data property
	 * for a scale factor.
	 *
	 * <p>
	 * SunSpec scale factors range from -10 to 10. Any other value, including
	 * the SunSpec "not implemented" value {@code 0x8000}, means the scale
	 * factor is not implemented.
	 * </p>
	 *
	 * @param ref
	 *        the block address relative reference to the scale factor register,
	 *        which is expected to contain a signed integer from -10..10
	 * @param offset
	 *        the address offset to add to {@link ModbusReference#getAddress()}
	 * @return the decimal multiplier to use, or {@code null} if the scale
	 *         factor is not available or not implemented
	 * @since 1.2
	 */
	protected @Nullable BigDecimal getScaleFactor(ModbusReference ref, int offset) {
		final Integer factor = scaleFactorExponent(ref, offset);
		if ( factor == null ) {
			return null;
		}
		if ( factor == 0 ) {
			return BigDecimal.ONE;
		}
		return new BigDecimal(BigInteger.ONE, -factor);
	}

	/**
	 * Get a scale factor's power of ten exponent.
	 *
	 * @param ref
	 *        the block address relative reference to the scale factor register
	 * @param offset
	 *        the address offset to add to {@link ModbusReference#getAddress()}
	 * @return the exponent, or {@code null} if the scale factor is not
	 *         available or not implemented
	 */
	private @Nullable Integer scaleFactorExponent(ModbusReference ref, int offset) {
		final Number n = data.getNumber(ref, offset);
		if ( n == null ) {
			return null;
		}
		final int factor = n.intValue();
		return (factor < SCALE_FACTOR_MIN || factor > SCALE_FACTOR_MAX ? null : factor);
	}

	/**
	 * Get a bitfield register value.
	 *
	 * @param ref
	 *        the block address relative reference to the bitfield register(s)
	 * @return the value, never {@code null}
	 * @since 1.1
	 */
	protected @Nullable Number getBitfield(ModbusReference ref) {
		return getBitfield(ref, blockAddress);
	}

	/**
	 * Get a bitfield register value.
	 *
	 * @param ref
	 *        the block address relative reference to the bitfield register(s)
	 * @param offset
	 *        the address offset to add to {@link ModbusReference#getAddress()}
	 * @return the value, never {@code null}
	 * @since 1.2
	 */
	protected @Nullable Number getBitfield(ModbusReference ref, int offset) {
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
	 * @return the scaled value, or {@code null} if not available
	 */
	public @Nullable BigDecimal getScaledValue(ModbusReference dataRef, ModbusReference scaleRef) {
		return getScaledValue(dataRef, scaleRef, blockAddress, blockAddress);
	}

	/**
	 * Get a scaled data property value.
	 *
	 * <p>
	 * The value is not available if the scale factor is not implemented, as
	 * described in {@link #getScaleFactor(ModbusReference, int)}.
	 * </p>
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
	 * @return the scaled value, or {@code null} if not available
	 */
	public @Nullable BigDecimal getScaledValue(ModbusReference dataRef, ModbusReference scaleRef,
			int dataOffset, int scaleOffset) {
		Number v = getValue(dataRef, dataOffset);
		if ( v == null ) {
			return null;
		}

		BigDecimal sf = getScaleFactor(scaleRef, scaleOffset);
		if ( sf == null ) {
			return null;
		}
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
	 * @return the value, or {@code null} if not available
	 * @since 1.2
	 */
	public @Nullable Number getValue(ModbusReference dataRef) {
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
	 * @return the value, or {@code null} if not available
	 * @since 1.2
	 */
	public @Nullable Number getValue(ModbusReference dataRef, int dataOffset) {
		Number v = data.getNumber(dataRef, dataOffset);
		if ( v == null ) {
			return null;
		}

		DataClassification classification = null;
		if ( dataRef instanceof SunspecModbusReference ) {
			classification = ((SunspecModbusReference) dataRef).getClassification();
		}

		// check for NaN
		if ( DataClassification.Accumulator == classification ) {
			// only zero means "not accumulated"; all other values are valid
			return (v.longValue() == 0 ? null : v);
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

			case UInt64:
				if ( NAN_UINT64.equals(v) ) {
					return null;
				}
				break;

			case Float32:
				if ( Float.isNaN(v.floatValue()) ) {
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
	 * @return the value, or {@code null} if not available
	 * @since 1.2
	 */
	public @Nullable Float getFloatValue(ModbusReference dataRef) {
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
	 * @return the value, or {@code null} if not available
	 * @since 1.2
	 */
	public @Nullable Float getFloatValue(ModbusReference dataRef, int dataOffset) {
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
	 * @return the value, or {@code null} if not available
	 * @since 1.2
	 */
	public @Nullable Integer getIntegerValue(ModbusReference dataRef) {
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
	 * @return the value, or {@code null} if not available
	 * @since 1.2
	 */
	public @Nullable Integer getIntegerValue(ModbusReference dataRef, int dataOffset) {
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
	 * @return the value, or {@code null} if not available
	 * @since 1.2
	 */
	public @Nullable Long getLongValue(ModbusReference dataRef) {
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
	 * @return the value, or {@code null} if not available
	 * @since 1.2
	 */
	public @Nullable Long getLongValue(ModbusReference dataRef, int dataOffset) {
		Number n = maximumDecimalScale(getValue(dataRef, dataOffset), 0);
		return (n != null ? n.longValue() : null);
	}

	/**
	 * Get a scaled data property value as a float.
	 *
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param scaleRef
	 *        the block address relative reference to the scale factor
	 * @return the value, or {@code null} if not available
	 * @see #getScaledValue(ModbusReference, ModbusReference)
	 * @since 2.1
	 */
	public @Nullable Float getScaledFloatValue(ModbusReference dataRef, ModbusReference scaleRef) {
		return getScaledFloatValue(dataRef, scaleRef, blockAddress, blockAddress);
	}

	/**
	 * Get a scaled data property value as a float.
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
	 * @return the value, or {@code null} if not available
	 * @see #getScaledValue(ModbusReference, ModbusReference, int, int)
	 * @since 2.1
	 */
	public @Nullable Float getScaledFloatValue(ModbusReference dataRef, ModbusReference scaleRef,
			int dataOffset, int scaleOffset) {
		Number n = getScaledValue(dataRef, scaleRef, dataOffset, scaleOffset);
		return (n != null ? n.floatValue() : null);
	}

	/**
	 * Get a scaled data property value as an integer.
	 *
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param scaleRef
	 *        the block address relative reference to the scale factor
	 * @return the value, or {@code null} if not available
	 * @see #getScaledValue(ModbusReference, ModbusReference)
	 * @since 2.1
	 */
	public @Nullable Integer getScaledIntegerValue(ModbusReference dataRef, ModbusReference scaleRef) {
		return getScaledIntegerValue(dataRef, scaleRef, blockAddress, blockAddress);
	}

	/**
	 * Get a scaled data property value as an integer.
	 *
	 * <p>
	 * Any fractional part of the scaled value is discarded. The value is not
	 * available if the result does not fit in an integer.
	 * </p>
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
	 * @return the value, or {@code null} if not available
	 * @see #getScaledValue(ModbusReference, ModbusReference, int, int)
	 * @since 2.1
	 */
	public @Nullable Integer getScaledIntegerValue(ModbusReference dataRef, ModbusReference scaleRef,
			int dataOffset, int scaleOffset) {
		BigInteger n = scaledWholeValue(dataRef, scaleRef, dataOffset, scaleOffset);
		return (n != null && n.bitLength() < Integer.SIZE ? n.intValue() : null);
	}

	/**
	 * Get a scaled data property value as a long.
	 *
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param scaleRef
	 *        the block address relative reference to the scale factor
	 * @return the value, or {@code null} if not available
	 * @see #getScaledValue(ModbusReference, ModbusReference)
	 * @since 2.1
	 */
	public @Nullable Long getScaledLongValue(ModbusReference dataRef, ModbusReference scaleRef) {
		return getScaledLongValue(dataRef, scaleRef, blockAddress, blockAddress);
	}

	/**
	 * Get a scaled data property value as a long.
	 *
	 * <p>
	 * Any fractional part of the scaled value is discarded. The value is not
	 * available if the result does not fit in a long, which is possible for
	 * {@code uint64} points.
	 * </p>
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
	 * @return the value, or {@code null} if not available
	 * @see #getScaledValue(ModbusReference, ModbusReference, int, int)
	 * @since 2.1
	 */
	public @Nullable Long getScaledLongValue(ModbusReference dataRef, ModbusReference scaleRef,
			int dataOffset, int scaleOffset) {
		BigInteger n = scaledWholeValue(dataRef, scaleRef, dataOffset, scaleOffset);
		return (n != null && n.bitLength() < Long.SIZE ? n.longValue() : null);
	}

	/**
	 * Get a scaled data property value with any fractional part discarded.
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
	 * @return the value, or {@code null} if not available
	 */
	private @Nullable BigInteger scaledWholeValue(ModbusReference dataRef, ModbusReference scaleRef,
			int dataOffset, int scaleOffset) {
		BigDecimal d = getScaledValue(dataRef, scaleRef, dataOffset, scaleOffset);
		return (d != null ? d.toBigInteger() : null);
	}

	/**
	 * Get a string data property value.
	 *
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @return the value, or {@code null} if not available
	 * @see ModelData#getStringValue(ModbusReference, int)
	 * @since 2.1
	 */
	public @Nullable String getStringValue(ModbusReference dataRef) {
		return getStringValue(dataRef, blockAddress);
	}

	/**
	 * Get a string data property value.
	 *
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @return the value, or {@code null} if not available
	 * @see ModelData#getStringValue(ModbusReference, int)
	 * @since 2.1
	 */
	public @Nullable String getStringValue(ModbusReference dataRef, int dataOffset) {
		return data.getStringValue(dataRef, dataOffset);
	}

	/**
	 * Get a boolean data property value.
	 *
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @return the value, or {@code null} if not available
	 * @see #getBooleanValue(ModbusReference, int)
	 * @since 2.1
	 */
	public @Nullable Boolean getBooleanValue(ModbusReference dataRef) {
		return getBooleanValue(dataRef, blockAddress);
	}

	/**
	 * Get a boolean data property value.
	 *
	 * <p>
	 * This is for SunSpec enumerations with two values, such as
	 * {@code DISABLED (0)} and {@code ENABLED (1)}: {@code 0} is returned as
	 * {@code false}, {@code 1} as {@code true}, and any other value, including
	 * the SunSpec "not implemented" value, as {@code null}.
	 * </p>
	 *
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @return the value, or {@code null} if not available
	 * @since 2.1
	 */
	public @Nullable Boolean getBooleanValue(ModbusReference dataRef, int dataOffset) {
		Number n = getValue(dataRef, dataOffset);
		if ( n == null ) {
			return null;
		}
		switch (n.intValue()) {
			case 0:
				return Boolean.FALSE;

			case 1:
				return Boolean.TRUE;

			default:
				return null;
		}
	}

	/**
	 * Get an enumerated data property value.
	 *
	 * @param <T>
	 *        the enumeration type
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param type
	 *        the enumeration type
	 * @return the value, or {@code null} if not available
	 * @see #getCodedValue(ModbusReference, int, Class)
	 * @since 2.1
	 */
	public <T extends Enum<T> & CodedValue> @Nullable T getCodedValue(ModbusReference dataRef,
			Class<T> type) {
		return getCodedValue(dataRef, blockAddress, type);
	}

	/**
	 * Get an enumerated data property value.
	 *
	 * <p>
	 * A value that is not one of the codes of the enumeration, including the
	 * SunSpec "not implemented" value, is returned as {@code null}.
	 * </p>
	 *
	 * @param <T>
	 *        the enumeration type
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @param type
	 *        the enumeration type
	 * @return the value, or {@code null} if not available
	 * @since 2.1
	 */
	public <T extends Enum<T> & CodedValue> @Nullable T getCodedValue(ModbusReference dataRef,
			int dataOffset, Class<T> type) {
		Number n = getValue(dataRef, dataOffset);
		return (n != null ? CodedValue.forCodeValue(n.intValue(), type, null) : null);
	}

	/**
	 * Get a bitfield data property value as a set of enumeration values.
	 *
	 * @param <T>
	 *        the enumeration type
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param type
	 *        the enumeration type
	 * @return the values, never {@code null}
	 * @see #getBitmaskableValues(ModbusReference, int, Class)
	 * @since 2.1
	 */
	public <T extends Enum<T> & Bitmaskable> Set<T> getBitmaskableValues(ModbusReference dataRef,
			Class<T> type) {
		return getBitmaskableValues(dataRef, blockAddress, type);
	}

	/**
	 * Get a bitfield data property value as a set of enumeration values.
	 *
	 * <p>
	 * SunSpec bitfields never have their most significant bit set, so a value
	 * with that bit set, including the SunSpec "not implemented" value, is
	 * returned as an empty set. Bits without a corresponding enumeration value
	 * are ignored.
	 * </p>
	 *
	 * @param <T>
	 *        the enumeration type
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @param type
	 *        the enumeration type
	 * @return the values, never {@code null}
	 * @since 2.1
	 */
	public <T extends Enum<T> & Bitmaskable> Set<T> getBitmaskableValues(ModbusReference dataRef,
			int dataOffset, Class<T> type) {
		Number n = data.getNumber(dataRef, dataOffset);
		if ( n == null ) {
			return Collections.emptySet();
		}
		final long v = n.longValue();
		if ( (v & (1L << (dataRef.getWordLength() * 16 - 1))) != 0 ) {
			return Collections.emptySet();
		}
		return Bitmaskable.setForBitmask((int) v, type);
	}

	/**
	 * Get the indexes of the bits set in a bitfield data property.
	 *
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @return the bit indexes, in ascending order, never {@code null}
	 * @see #getBitfieldIndexes(ModbusReference, int)
	 * @since 2.1
	 */
	public Set<Integer> getBitfieldIndexes(ModbusReference dataRef) {
		return getBitfieldIndexes(dataRef, blockAddress);
	}

	/**
	 * Get the indexes of the bits set in a bitfield data property.
	 *
	 * <p>
	 * This is for bitfields whose bits are numbered things, such as ports or
	 * contactors, rather than flags. SunSpec bitfields never have their most
	 * significant bit set, so a bitfield with that bit set, including the
	 * SunSpec "not implemented" value, results in an empty set.
	 * </p>
	 *
	 * @param dataRef
	 *        the block address relative reference to the data property
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @return the bit indexes, in ascending order, never {@code null}
	 * @since 2.1
	 */
	public Set<Integer> getBitfieldIndexes(ModbusReference dataRef, int dataOffset) {
		Number n = data.getNumber(dataRef, dataOffset);
		if ( n == null ) {
			return Collections.emptySet();
		}
		final long v = n.longValue();
		final int bitCount = dataRef.getWordLength() * 16;
		if ( v == 0 || (v & (1L << (bitCount - 1))) != 0 ) {
			return Collections.emptySet();
		}
		final Set<Integer> result = new TreeSet<>();
		for ( int i = 0; i < bitCount - 1; i++ ) {
			if ( ((v >> i) & 1L) == 1L ) {
				result.add(i);
			}
		}
		return result;
	}

	/**
	 * Get the bits set in a sequence of bitfield data properties, as a single
	 * bit set.
	 *
	 * @param dataRefs
	 *        the block address relative references to the data properties
	 * @return the bit set, never {@code null}
	 * @see #getBitfieldBits(int, ModbusReference...)
	 * @since 2.1
	 */
	public BitSet getBitfieldBits(ModbusReference... dataRefs) {
		return getBitfieldBits(blockAddress, dataRefs);
	}

	/**
	 * Get the bits set in a sequence of bitfield data properties, as a single
	 * bit set.
	 *
	 * <p>
	 * The bits of each bitfield follow the bits of the bitfields before it, so
	 * for a sequence of 32-bit bitfields, such as the SunSpec vendor event
	 * fields, the first bit of the second bitfield is index {@literal 32}. Each
	 * bitfield is read as with
	 * {@link #getBitfieldIndexes(ModbusReference, int)}, so a bitfield with its
	 * most significant bit set contributes no bits.
	 * </p>
	 *
	 * @param dataOffset
	 *        the data address offset to add to each
	 *        {@link ModbusReference#getAddress()}
	 * @param dataRefs
	 *        the block address relative references to the data properties
	 * @return the bit set, never {@code null}
	 * @since 2.1
	 */
	public BitSet getBitfieldBits(int dataOffset, ModbusReference... dataRefs) {
		final BitSet result = new BitSet();
		int start = 0;
		for ( ModbusReference ref : dataRefs ) {
			for ( Integer i : getBitfieldIndexes(ref, dataOffset) ) {
				result.set(start + i);
			}
			start += ref.getWordLength() * 16;
		}
		return result;
	}

	/**
	 * Write a point value to a device.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param dataRef
	 *        the block address relative reference to the point to write
	 * @param value
	 *        the value to write
	 * @throws IllegalArgumentException
	 *         if {@code dataRef} is not writable or {@code value} is not valid
	 *         for the point
	 * @throws IOException
	 *         if any communication error occurs
	 * @see #writeValue(ModbusConnection, SunspecModbusReference, int, Number)
	 * @since 2.1
	 */
	public void writeValue(ModbusConnection conn, SunspecModbusReference dataRef, Number value)
			throws IOException {
		writeValue(conn, dataRef, blockAddress, value);
	}

	/**
	 * Write a point value to a device.
	 *
	 * <p>
	 * The value is encoded with
	 * {@link #encodeValue(SunspecModbusReference, Number)} and written with
	 * {@link #writeWords(ModbusConnection, int, short[])}.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param dataRef
	 *        the block address relative reference to the point to write
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @param value
	 *        the value to write
	 * @throws IllegalArgumentException
	 *         if {@code dataRef} is not writable or {@code value} is not valid
	 *         for the point
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	public void writeValue(ModbusConnection conn, SunspecModbusReference dataRef, int dataOffset,
			Number value) throws IOException {
		requireWritable(dataRef);
		writeWords(conn, dataRef.getAddress() + dataOffset, encodeValue(dataRef, value));
	}

	/**
	 * Write a scaled point value to a device.
	 *
	 * @param conn
	 *        the connection to write to
	 * @param dataRef
	 *        the block address relative reference to the point to write
	 * @param scaleRef
	 *        the block address relative reference to the scale factor
	 * @param value
	 *        the value to write
	 * @throws IllegalArgumentException
	 *         if {@code dataRef} is not writable or {@code value} is not valid
	 *         for the point
	 * @throws IllegalStateException
	 *         if the scale factor has not been read or is not implemented
	 * @throws IOException
	 *         if any communication error occurs
	 * @see #writeScaledValue(ModbusConnection, SunspecModbusReference,
	 *      ModbusReference, int, int, Number)
	 * @since 2.1
	 */
	public void writeScaledValue(ModbusConnection conn, SunspecModbusReference dataRef,
			ModbusReference scaleRef, Number value) throws IOException {
		writeScaledValue(conn, dataRef, scaleRef, blockAddress, blockAddress, value);
	}

	/**
	 * Write a scaled point value to a device.
	 *
	 * <p>
	 * The value is encoded with
	 * {@link #encodeScaledValue(SunspecModbusReference, ModbusReference, int, Number)}
	 * and written with {@link #writeWords(ModbusConnection, int, short[])}.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param dataRef
	 *        the block address relative reference to the point to write
	 * @param scaleRef
	 *        the block address relative reference to the scale factor
	 * @param dataOffset
	 *        the data address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @param scaleOffset
	 *        the scale address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @param value
	 *        the value to write
	 * @throws IllegalArgumentException
	 *         if {@code dataRef} is not writable or {@code value} is not valid
	 *         for the point
	 * @throws IllegalStateException
	 *         if the scale factor has not been read or is not implemented
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	public void writeScaledValue(ModbusConnection conn, SunspecModbusReference dataRef,
			ModbusReference scaleRef, int dataOffset, int scaleOffset, Number value) throws IOException {
		requireWritable(dataRef);
		writeWords(conn, dataRef.getAddress() + dataOffset,
				encodeScaledValue(dataRef, scaleRef, scaleOffset, value));
	}

	/**
	 * Encode a point value as Modbus register values.
	 *
	 * <p>
	 * Values for integer data types are rounded to an integer, using the
	 * {@link RoundingMode#HALF_UP} mode, and must be within the SunSpec range
	 * of the data type, which excludes the "not implemented" value.
	 * </p>
	 *
	 * @param ref
	 *        the reference to the point to encode the value for
	 * @param value
	 *        the value to encode
	 * @return the encoded register values
	 * @throws IllegalArgumentException
	 *         if {@code value} is not valid for the point, or the point data
	 *         type is not supported
	 * @since 2.1
	 */
	protected short[] encodeValue(SunspecModbusReference ref, Number value) {
		final ModbusDataType type = ref.getDataType();
		final Number n;
		if ( type == ModbusDataType.Float32 ) {
			if ( !Float.isFinite(value.floatValue()) ) {
				throw new IllegalArgumentException(
						String.format("The value %s is not valid for the %s point.", value, ref));
			}
			n = value;
		} else {
			final BigInteger i = decimalValue(value, ref).setScale(0, RoundingMode.HALF_UP)
					.toBigInteger();
			final BigInteger min = minimumIntegerValue(ref);
			final BigInteger max = maximumIntegerValue(ref);
			if ( i.compareTo(min) < 0 || i.compareTo(max) > 0 ) {
				throw new IllegalArgumentException(String.format(
						"The value %s is outside the %s point range %d - %d.", value, ref, min, max));
			}
			n = i;
		}
		return nonnull(ModbusDataUtils.encodeNumber(type, n), "Encoded value");
	}

	/**
	 * Encode a scaled point value as Modbus register values.
	 *
	 * <p>
	 * The value is divided by the scale factor and then encoded with
	 * {@link #encodeValue(SunspecModbusReference, Number)}. The scale factor
	 * must have been read from the device, and be implemented as described in
	 * {@link #getScaleFactor(ModbusReference, int)}.
	 * </p>
	 *
	 * @param ref
	 *        the reference to the point to encode the value for
	 * @param scaleRef
	 *        the block address relative reference to the scale factor
	 * @param scaleOffset
	 *        the scale address offset to add to
	 *        {@link ModbusReference#getAddress()}
	 * @param value
	 *        the value to encode
	 * @return the encoded register values
	 * @throws IllegalArgumentException
	 *         if {@code value} is not valid for the point, or the point data
	 *         type is not supported
	 * @throws IllegalStateException
	 *         if the scale factor has not been read or is not implemented
	 * @since 2.1
	 */
	protected short[] encodeScaledValue(SunspecModbusReference ref, ModbusReference scaleRef,
			int scaleOffset, Number value) {
		if ( !data.dataRegisters().containsKey(scaleRef.getAddress() + scaleOffset) ) {
			throw new IllegalStateException(String
					.format("The %s scale factor for the %s point has not been read.", scaleRef, ref));
		}
		final Integer sf = scaleFactorExponent(scaleRef, scaleOffset);
		if ( sf == null ) {
			throw new IllegalStateException(String
					.format("The %s scale factor for the %s point is not implemented.", scaleRef, ref));
		}
		return encodeValue(ref, decimalValue(value, ref).movePointLeft(sf));
	}

	/**
	 * Write Modbus register values to a device.
	 *
	 * <p>
	 * The values are written with a single "write multiple holding registers"
	 * request, so the registers of a SunSpec synchronization group can be
	 * written atomically. After a successful write the values are also saved to
	 * the model data, as SunSpec requires a subsequent read of the registers to
	 * return the written values.
	 * </p>
	 *
	 * @param conn
	 *        the connection to write to
	 * @param address
	 *        the Modbus address of the first register to write
	 * @param words
	 *        the register values to write
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 2.1
	 */
	protected void writeWords(ModbusConnection conn, int address, short[] words) throws IOException {
		conn.writeWords(ModbusWriteFunction.WriteMultipleHoldingRegisters, address, words);
		data.performUpdates(m -> {
			m.saveDataArray(words, address);
			return true;
		});
	}

	private static void requireWritable(SunspecModbusReference ref) {
		if ( ref.getAccess() != PointAccess.ReadWrite ) {
			throw new IllegalArgumentException(String.format("The %s point is not writable.", ref));
		}
	}

	private static BigDecimal decimalValue(Number value, SunspecModbusReference ref) {
		if ( !Double.isFinite(value.doubleValue()) ) {
			throw new IllegalArgumentException(
					String.format("The value %s is not valid for the %s point.", value, ref));
		}
		return nonnull(bigDecimalForNumber(value), "Decimal value");
	}

	private static BigInteger minimumIntegerValue(SunspecModbusReference ref) {
		switch (ref.getDataType()) {
			case Int16:
				return BigInteger.valueOf(-0x7FFF);

			case Int32:
				return BigInteger.valueOf(-0x7FFFFFFFL);

			case Int64:
				return BigInteger.valueOf(-Long.MAX_VALUE);

			case UInt16:
			case UInt32:
			case UInt64:
				return BigInteger.ZERO;

			default:
				throw new IllegalArgumentException(String
						.format("The %s point data type %s is not supported.", ref, ref.getDataType()));
		}
	}

	private static BigInteger maximumIntegerValue(SunspecModbusReference ref) {
		final DataClassification classification = ref.getClassification();
		switch (ref.getDataType()) {
			case Int16:
				return BigInteger.valueOf(0x7FFF);

			case Int32:
				return BigInteger.valueOf(0x7FFFFFFFL);

			case Int64:
				return BigInteger.valueOf(Long.MAX_VALUE);

			case UInt16:
				return BigInteger.valueOf(DataClassification.Accumulator == classification ? 0xFFFF
						: DataClassification.Bitfield == classification ? 0x7FFF : 0xFFFE);

			case UInt32:
				return BigInteger.valueOf(DataClassification.Accumulator == classification ? 0xFFFFFFFFL
						: DataClassification.Bitfield == classification ? 0x7FFFFFFFL : 0xFFFFFFFEL);

			case UInt64:
				return (DataClassification.Accumulator == classification
						? BigInteger.valueOf(Long.MAX_VALUE)
						: UINT64_MAX);

			default:
				throw new IllegalArgumentException(String
						.format("The %s point data type %s is not supported.", ref, ref.getDataType()));
		}
	}

}
