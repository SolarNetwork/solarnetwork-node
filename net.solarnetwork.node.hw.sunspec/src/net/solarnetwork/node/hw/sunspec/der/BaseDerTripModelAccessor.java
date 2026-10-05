/* ==================================================================
 * BaseDerTripModelAccessor.java - 5/10/2026 6:24:51 pm
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
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Base implementation of {@link DerTripModelAccessor}.
 *
 * <p>
 * The model's curve sets follow its fixed block. Each curve set has a read-only
 * register followed by the must trip, may trip, and momentary cessation curves.
 * Each curve has an active point count register followed by the curve points,
 * and each point has an x and a time register.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public abstract class BaseDerTripModelAccessor extends BaseModelAccessor
		implements DerTripModelAccessor {

	/** The DER trip model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 7;

	private final SunspecModbusReference pointXRegister;
	private final ModbusReference pointXScaleFactorRegister;
	private final SunspecModbusReference pointTimeRegister;

	/**
	 * Constructor.
	 *
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 * @param pointXRegister
	 *        the point x register, relative to the start of a point
	 * @param pointXScaleFactorRegister
	 *        the point x scale factor register
	 * @param pointTimeRegister
	 *        the point time register, relative to the start of a point
	 */
	public BaseDerTripModelAccessor(ModelData data, int baseAddress, ModelId modelId,
			SunspecModbusReference pointXRegister, ModbusReference pointXScaleFactorRegister,
			SunspecModbusReference pointTimeRegister) {
		super(data, baseAddress, modelId);
		this.pointXRegister = pointXRegister;
		this.pointXScaleFactorRegister = pointXScaleFactorRegister;
		this.pointTimeRegister = pointTimeRegister;
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	private int pointLength() {
		return pointXRegister.getWordLength() + pointTimeRegister.getWordLength();
	}

	private int curveLength() {
		final Integer pointCount = getCurvePointCount();
		return 1 + (pointCount != null ? pointCount * pointLength() : 0);
	}

	@Override
	public int getRepeatingBlockInstanceLength() {
		return (getCurvePointCount() != null ? 1 + 3 * curveLength() : 0);
	}

	@Override
	public @Nullable Boolean isEnabled() {
		return getBooleanValue(DerTripModelRegister.Enabled);
	}

	@Override
	public void setEnabled(ModbusConnection conn, boolean enabled) throws IOException {
		writeValue(conn, DerTripModelRegister.Enabled, enabled ? 1 : 0);
	}

	@Override
	public @Nullable Integer getCurveSetCount() {
		return getIntegerValue(DerTripModelRegister.NumberOfCurveSets);
	}

	@Override
	public @Nullable Integer getCurvePointCount() {
		return getIntegerValue(DerTripModelRegister.NumberOfPoints);
	}

	@Override
	public @Nullable Integer getAdoptCurveRequest() {
		return getIntegerValue(DerTripModelRegister.AdoptCurveRequest);
	}

	@Override
	public void adoptCurveSet(ModbusConnection conn, int index) throws IOException {
		final Integer count = getCurveSetCount();
		if ( index < 2 || (count != null && index > count) ) {
			throw new IllegalArgumentException(String.format(
					"The curve set index %d is not valid: it must be between 2 and the curve set count %s.",
					index, count));
		}
		writeValue(conn, DerTripModelRegister.AdoptCurveRequest, index);
	}

	@Override
	public @Nullable DerAdoptCurveResult getAdoptCurveResult() {
		return getCodedValue(DerTripModelRegister.AdoptCurveResult, DerAdoptCurveResult.class);
	}

	@Override
	public List<DerTripCurveSet> getCurveSets() {
		final int instanceCount = getRepeatingBlockInstanceCount();
		final Integer setCount = getCurveSetCount();
		final int count = (setCount != null ? Math.min(setCount, instanceCount) : instanceCount);
		if ( count < 1 ) {
			return List.of();
		}
		final List<DerTripCurveSet> result = new ArrayList<>(count);
		for ( int i = 1; i <= count; i++ ) {
			result.add(new TripCurveSet(i));
		}
		return result;
	}

	private final class TripCurveSet implements DerTripCurveSet {

		private final int index;
		private final int setAddress;

		private TripCurveSet(int index) {
			super();
			this.index = index;
			this.setAddress = getBlockAddress() + FIXED_BLOCK_LENGTH
					+ (index - 1) * getRepeatingBlockInstanceLength();
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public @Nullable Boolean isReadOnly() {
			return getBooleanValue(DerTripModelRegister.CurveSetReadOnly, setAddress);
		}

		@Override
		public DerCurve getMustTripCurve() {
			return new TripCurve(this, 0);
		}

		@Override
		public DerCurve getMayTripCurve() {
			return new TripCurve(this, 1);
		}

		@Override
		public DerCurve getMomentaryCessationCurve() {
			return new TripCurve(this, 2);
		}

	}

	private final class TripCurve implements DerCurve {

		private final TripCurveSet set;
		private final int curveAddress;

		private TripCurve(TripCurveSet set, int position) {
			super();
			this.set = set;
			this.curveAddress = set.setAddress + 1 + position * curveLength();
		}

		@Override
		public int getIndex() {
			return set.getIndex();
		}

		@Override
		public @Nullable Boolean isReadOnly() {
			return set.isReadOnly();
		}

		private void requireWritable() {
			if ( Boolean.TRUE.equals(isReadOnly()) ) {
				throw new UnsupportedOperationException(String
						.format("Curve set %d of model %s is read-only.", set.getIndex(), getModelId()));
			}
		}

		private void requireValidPointCount(int count) {
			final Integer pointCount = getCurvePointCount();
			if ( pointCount == null ) {
				throw new IllegalStateException(
						"The curve point count is not available to validate the points with.");
			}
			if ( count < 0 || count > pointCount ) {
				throw new IllegalArgumentException(
						String.format("The point count %d is not valid: it must be between 0 and %d.",
								count, pointCount));
			}
		}

		@Override
		public @Nullable Integer getActivePointCount() {
			return getIntegerValue(DerTripModelRegister.CurveActivePointCount, curveAddress);
		}

		@Override
		public void setActivePointCount(ModbusConnection conn, int count) throws IOException {
			requireWritable();
			requireValidPointCount(count);
			writeValue(conn, DerTripModelRegister.CurveActivePointCount, curveAddress, count);
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
				final int pointAddress = curveAddress + 1 + i * pointLength();
				final Float x = getScaledFloatValue(pointXRegister, pointXScaleFactorRegister,
						pointAddress, getBlockAddress());
				final Float time = getScaledFloatValue(pointTimeRegister,
						DerTripModelRegister.ScaleFactorTime, pointAddress, getBlockAddress());
				result.add(
						new DerCurvePoint(x != null ? x : Float.NaN, time != null ? time : Float.NaN));
			}
			return result;
		}

		@Override
		public void setPoints(ModbusConnection conn, List<DerCurvePoint> points) throws IOException {
			requireWritable();
			requireValidPointCount(points.size());
			final short[] words = new short[points.size() * pointLength()];
			int offset = 0;
			for ( DerCurvePoint point : points ) {
				final short[] x = encodeScaledValue(pointXRegister, pointXScaleFactorRegister,
						getBlockAddress(), point.x());
				System.arraycopy(x, 0, words, offset, x.length);
				offset += x.length;
				final short[] time = encodeScaledValue(pointTimeRegister,
						DerTripModelRegister.ScaleFactorTime, getBlockAddress(), point.y());
				System.arraycopy(time, 0, words, offset, time.length);
				offset += time.length;
			}
			if ( words.length > 0 ) {
				writeWords(conn, curveAddress + 1, words);
			}
			writeValue(conn, DerTripModelRegister.CurveActivePointCount, curveAddress, points.size());
		}

	}

}
