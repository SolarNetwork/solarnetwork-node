/* ==================================================================
 * BaseDerCurveModelAccessor.java - 5/10/2026 4:58:20 pm
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

package net.solarnetwork.node.hw.sunspec.der;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.function.IntFunction;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRange;

/**
 * Base implementation of {@link DerCurveModelAccessor}.
 *
 * <p>
 * The model's curves follow its fixed block. Each curve has
 * {@link #getCurveSettingsLength()} registers of settings, followed by the
 * curve points. Each point has an x and a y register.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public abstract class BaseDerCurveModelAccessor extends BaseModelAccessor
		implements DerCurveModelAccessor {

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
	public BaseDerCurveModelAccessor(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Get the number of curve setting registers that precede the curve points.
	 *
	 * @return the number of registers
	 */
	protected abstract int getCurveSettingsLength();

	/**
	 * Get the curve settings registers.
	 *
	 * @return the registers that precede the curve points, relative to the
	 *         start of a curve
	 */
	protected abstract Collection<? extends ModbusReference> getCurveSettingsRegisters();

	/**
	 * Get the curve read-only register.
	 *
	 * @return the register, relative to the start of a curve
	 */
	protected abstract SunspecModbusReference getCurveReadOnlyRegister();

	/**
	 * Get the point x register.
	 *
	 * @return the register, relative to the start of a point
	 */
	protected abstract SunspecModbusReference getPointXRegister();

	/**
	 * Get the point x scale factor register.
	 *
	 * @return the register, relative to the model block address
	 */
	protected abstract ModbusReference getPointXScaleFactorRegister();

	/**
	 * Get the point y register.
	 *
	 * @return the register, relative to the start of a point
	 */
	protected abstract SunspecModbusReference getPointYRegister();

	/**
	 * Get the point y scale factor register.
	 *
	 * @return the register, relative to the model block address
	 */
	protected abstract ModbusReference getPointYScaleFactorRegister();

	/**
	 * Get the length of a point.
	 *
	 * @return the number of registers in each point
	 */
	private int pointLength() {
		return getPointXRegister().getWordLength() + getPointYRegister().getWordLength();
	}

	@Override
	public int getRepeatingBlockInstanceLength() {
		final Integer pointCount = getCurvePointCount();
		return (pointCount != null ? getCurveSettingsLength() + pointCount * pointLength() : 0);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The layout of the curves depends on the curve count and point count in
	 * the fixed block. Until those have been read, or if they do not match the
	 * model length, this implementation returns a single range for all the
	 * curves, so the fixed block is read before them.
	 * </p>
	 */
	@Override
	public List<IntRange> getUnsplittableAddressRanges() {
		final List<IntRange> result = new ArrayList<>(8);
		addMultiRegisterAddressRanges(result, getBlockAddress(),
				EnumSet.range(DerCurveModelRegister.Enabled, DerCurveModelRegister.ReversionCurve));
		addMultiRegisterAddressRanges(result, getBlockAddress(), getFixedBlockRegisters());
		final int curvesAddress = getBlockAddress() + getFixedBlockLength();
		final int curvesLength = getModelLength() - getFixedBlockLength();
		if ( curvesLength < 1 ) {
			return result;
		}
		final Integer curveCount = getCurveCount();
		final Integer pointCount = getCurvePointCount();
		final int curveLength = getRepeatingBlockInstanceLength();
		if ( curveCount == null || pointCount == null || curveCount < 1 || pointCount < 1
				|| curveCount * curveLength != curvesLength ) {
			result.add(new IntRange(curvesAddress, curvesAddress + curvesLength - 1));
			return result;
		}
		final List<ModbusReference> pointRegisters = List.of(getPointXRegister(), getPointYRegister());
		for ( int i = 0; i < curveCount; i++ ) {
			final int curveAddress = curvesAddress + i * curveLength;
			addMultiRegisterAddressRanges(result, curveAddress, getCurveSettingsRegisters());
			for ( int j = 0; j < pointCount; j++ ) {
				addMultiRegisterAddressRanges(result,
						curveAddress + getCurveSettingsLength() + j * pointLength(), pointRegisters);
			}
		}
		return result;
	}

	@Override
	public @Nullable Boolean isEnabled() {
		return getBooleanValue(DerCurveModelRegister.Enabled);
	}

	@Override
	public void setEnabled(ModbusConnection conn, boolean enabled) throws IOException {
		writeValue(conn, DerCurveModelRegister.Enabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable Integer getCurveCount() {
		return getIntegerValue(DerCurveModelRegister.NumberOfCurves);
	}

	@Override
	public @Nullable Integer getCurvePointCount() {
		return getIntegerValue(DerCurveModelRegister.NumberOfPoints);
	}

	@Override
	public @Nullable Integer getAdoptCurveRequest() {
		return getIntegerValue(DerCurveModelRegister.AdoptCurveRequest);
	}

	@Override
	public void adoptCurve(ModbusConnection conn, int index) throws IOException {
		final Integer count = getCurveCount();
		if ( index < 2 || (count != null && index > count) ) {
			throw new IllegalArgumentException(String.format(
					"The curve index %d is not valid: it must be between 2 and the curve count %s.",
					index, count));
		}
		writeValue(conn, DerCurveModelRegister.AdoptCurveRequest, index);
	}

	@Override
	public @Nullable DerAdoptResult getAdoptCurveResult() {
		return getCodedValue(DerCurveModelRegister.AdoptCurveResult, DerAdoptResult.class);
	}

	@Override
	public @Nullable Long getReversionTime() {
		return getLongValue(DerCurveModelRegister.ReversionTime);
	}

	@Override
	public void setReversionTime(ModbusConnection conn, long seconds) throws IOException {
		writeValue(conn, DerCurveModelRegister.ReversionTime, seconds);
	}

	@Override
	public @Nullable Long getReversionTimeRemaining() {
		return getLongValue(DerCurveModelRegister.ReversionTimeRemaining);
	}

	@Override
	public @Nullable Integer getReversionCurve() {
		return getIntegerValue(DerCurveModelRegister.ReversionCurve);
	}

	@Override
	public void setReversionCurve(ModbusConnection conn, int index) throws IOException {
		final Integer count = getCurveCount();
		if ( index < 1 || (count != null && index > count) ) {
			throw new IllegalArgumentException(String.format(
					"The curve index %d is not valid: it must be between 1 and the curve count %s.",
					index, count));
		}
		writeValue(conn, DerCurveModelRegister.ReversionCurve, index);
	}

	/**
	 * Create the list of curves.
	 *
	 * @param <T>
	 *        the curve type
	 * @param factory
	 *        a factory to create a curve for a given curve index
	 * @return the curves, never {@code null}
	 */
	protected <T extends DerCurve> List<T> curves(IntFunction<T> factory) {
		final int instanceCount = getRepeatingBlockInstanceCount();
		final Integer curveCount = getCurveCount();
		final int count = (curveCount != null ? Math.min(curveCount, instanceCount) : instanceCount);
		if ( count < 1 ) {
			return List.of();
		}
		final List<T> result = new ArrayList<>(count);
		for ( int i = 1; i <= count; i++ ) {
			result.add(factory.apply(i));
		}
		return result;
	}

	/**
	 * Base implementation of {@link DerCurve}.
	 */
	protected class BaseDerCurve implements DerCurve {

		private final int index;

		/** The address of the start of the curve. */
		protected final int curveAddress;

		/**
		 * Constructor.
		 *
		 * @param index
		 *        the curve index, starting from {@literal 1}
		 */
		protected BaseDerCurve(int index) {
			super();
			this.index = index;
			this.curveAddress = getBlockAddress() + getFixedBlockLength()
					+ (index - 1) * getRepeatingBlockInstanceLength();
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public @Nullable Boolean isReadOnly() {
			return getBooleanValue(getCurveReadOnlyRegister(), curveAddress);
		}

		/**
		 * Verify the curve can be written to.
		 *
		 * @throws UnsupportedOperationException
		 *         if the curve is read-only
		 */
		protected void requireWritable() {
			if ( Boolean.TRUE.equals(isReadOnly()) ) {
				throw new UnsupportedOperationException(
						String.format("Curve %d of model %s is read-only.", index, getModelId()));
			}
		}

		/**
		 * Verify a number of curve points is valid.
		 *
		 * @param count
		 *        the number of points
		 * @throws IllegalArgumentException
		 *         if {@code count} is outside the allowed range
		 * @throws IllegalStateException
		 *         if the model curve point count is not available
		 */
		private void requireValidPointCount(int count) {
			final Integer pointCount = getCurvePointCount();
			if ( pointCount == null ) {
				throw new IllegalStateException(
						"The curve point count is not available to validate the points with.");
			}
			if ( count < 1 || count > pointCount ) {
				throw new IllegalArgumentException(
						String.format("The point count %d is not valid: it must be between 1 and %d.",
								count, pointCount));
			}
		}

		@Override
		public @Nullable Integer getActivePointCount() {
			return getIntegerValue(DerCurveModelRegister.CurveActivePointCount, curveAddress);
		}

		@Override
		public void setActivePointCount(ModbusConnection conn, int count) throws IOException {
			requireWritable();
			requireValidPointCount(count);
			writeValue(conn, DerCurveModelRegister.CurveActivePointCount, curveAddress, count);
		}

		@Override
		public List<DerCurvePoint> getPoints() {
			final Integer activeCount = getActivePointCount();
			final Integer pointCount = getCurvePointCount();
			if ( activeCount == null || pointCount == null ) {
				return List.of();
			}
			final int count = Math.min(activeCount, pointCount);
			final List<DerCurvePoint> result = new ArrayList<>(count);
			for ( int i = 0; i < count; i++ ) {
				final int pointAddress = curveAddress + getCurveSettingsLength() + i * pointLength();
				final Float x = getScaledFloatValue(getPointXRegister(), getPointXScaleFactorRegister(),
						pointAddress, getBlockAddress());
				final Float y = getScaledFloatValue(getPointYRegister(), getPointYScaleFactorRegister(),
						pointAddress, getBlockAddress());
				result.add(new DerCurvePoint(x != null ? x : Float.NaN, y != null ? y : Float.NaN));
			}
			return result;
		}

		@Override
		public void setPoints(ModbusConnection conn, List<DerCurvePoint> points) throws IOException {
			requireWritable();
			requireValidPointCount(points.size());
			final SunspecModbusReference xRef = getPointXRegister();
			final SunspecModbusReference yRef = getPointYRegister();
			final short[] words = new short[points.size() * pointLength()];
			int offset = 0;
			for ( DerCurvePoint point : points ) {
				final short[] x = encodeScaledValue(xRef, getPointXScaleFactorRegister(),
						getBlockAddress(), point.x());
				System.arraycopy(x, 0, words, offset, x.length);
				offset += x.length;
				final short[] y = encodeScaledValue(yRef, getPointYScaleFactorRegister(),
						getBlockAddress(), point.y());
				System.arraycopy(y, 0, words, offset, y.length);
				offset += y.length;
			}
			writeWords(conn, curveAddress + getCurveSettingsLength(), words);
			writeValue(conn, DerCurveModelRegister.CurveActivePointCount, curveAddress, points.size());
		}

	}

}
