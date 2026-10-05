/* ==================================================================
 * ModelAddressRangeTests.java - 6/10/2026 2:41:08 pm
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

package net.solarnetwork.node.hw.sunspec.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.GenericModelId;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelDataFactory;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.util.IntRange;

/**
 * Test cases for reading model data in address ranges that do not split
 * multi-register values.
 *
 * @author matt
 * @version 1.0
 */
public class ModelAddressRangeTests {

	/**
	 * The multi-register point ranges of the test captures, generated from the
	 * SunSpec model definitions.
	 */
	private static final String MULTI_REGISTER_POINTS = "multi-register-points.txt";

	/**
	 * Maximum read lengths to test, all at least as long as the longest point
	 * in the test captures (32 registers).
	 */
	private static final int[] MAX_READ_LENGTHS = new int[] { 32, 33, 40, 50, 64, 99, 100, 125 };

	/** The multi-register points of a model in a test capture. */
	private record ModelPoints(String resource, int modelId, int baseAddress, List<IntRange> points) {

	}

	private static Map<String, List<ModelPoints>> multiRegisterPoints() throws IOException {
		final Map<String, List<ModelPoints>> result = new LinkedHashMap<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				ModelAddressRangeTests.class.getResourceAsStream(MULTI_REGISTER_POINTS),
				StandardCharsets.UTF_8))) {
			String line;
			while ( (line = r.readLine()) != null ) {
				if ( line.isBlank() || line.startsWith("#") ) {
					continue;
				}
				final String[] fields = line.trim().split(" ");
				final List<IntRange> points = new ArrayList<>();
				if ( fields.length > 3 ) {
					for ( String range : fields[3].split(",") ) {
						final String[] limits = range.split("-");
						points.add(
								new IntRange(Integer.parseInt(limits[0]), Integer.parseInt(limits[1])));
					}
				}
				result.computeIfAbsent(fields[0], k -> new ArrayList<>()).add(new ModelPoints(fields[0],
						Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), points));
			}
		}
		return result;
	}

	private static List<IntRange> sorted(Collection<IntRange> ranges) {
		final List<IntRange> result = new ArrayList<>(ranges);
		result.sort(Comparator.comparingInt(IntRange::getMin).thenComparingInt(IntRange::getMax));
		return result;
	}

	/**
	 * Get the distinct ranges within a model, in ascending order.
	 *
	 * @param model
	 *        the model
	 * @return the ranges that are entirely within the model
	 */
	private static List<IntRange> modelRanges(ModelAccessor model) {
		final int end = model.getBlockAddress() + model.getModelLength();
		return sorted(model.getUnsplittableAddressRanges().stream()
				.filter(r -> r.getMin() >= model.getBlockAddress() && r.getMax() < end).distinct()
				.toList());
	}

	private static ModelAccessor findModel(ModelData data, ModelPoints model) {
		if ( data.getBaseAddress() == model.baseAddress() ) {
			return data;
		}
		for ( ModelAccessor m : data.getModels() ) {
			if ( m.getBaseAddress() == model.baseAddress() ) {
				return m;
			}
		}
		throw new AssertionError(String.format("Model %d not found at %d in %s", model.modelId(),
				model.baseAddress(), model.resource()));
	}

	private static ModelAccessor testAccessor(int blockAddress, int length, IntRange... unsplittable) {
		return new ModelAccessor() {

			@Override
			public @Nullable Instant getDataTimestamp() {
				return null;
			}

			@Override
			public int getBaseAddress() {
				return blockAddress - 2;
			}

			@Override
			public int getBlockAddress() {
				return blockAddress;
			}

			@Override
			public ModelId getModelId() {
				return new GenericModelId(64000);
			}

			@Override
			public int getFixedBlockLength() {
				return length;
			}

			@Override
			public int getModelLength() {
				return length;
			}

			@Override
			public List<IntRange> getUnsplittableAddressRanges() {
				return List.of(unsplittable);
			}

		};
	}

	@Test
	public void addressRanges_fits() {
		ModelAccessor model = testAccessor(2, 10, new IntRange(5, 6));
		assertThat("Model that fits read in one range", List.of(model.getAddressRanges(10)),
				contains(new IntRange(2, 11)));
		assertThat("Unlimited read length", List.of(model.getAddressRanges(Integer.MAX_VALUE)),
				contains(new IntRange(2, 11)));
	}

	@Test
	public void addressRanges_noUnsplittableRanges() {
		ModelAccessor model = testAccessor(2, 25);
		assertThat("Model split into maximum length ranges", List.of(model.getAddressRanges(10)),
				contains(new IntRange(2, 11), new IntRange(12, 21), new IntRange(22, 26)));
	}

	@Test
	public void addressRanges_endBeforeUnsplittableRange() {
		ModelAccessor model = testAccessor(0, 30, new IntRange(9, 10));
		assertThat("Range ends before a multi-register value", List.of(model.getAddressRanges(10)),
				contains(new IntRange(0, 8), new IntRange(9, 18), new IntRange(19, 28),
						new IntRange(29, 29)));
	}

	@Test
	public void addressRanges_adjacentUnsplittableRanges() {
		ModelAccessor model = testAccessor(0, 20, new IntRange(8, 9), new IntRange(10, 11));
		assertThat("Range ends between adjacent multi-register values",
				List.of(model.getAddressRanges(10)), contains(new IntRange(0, 9), new IntRange(10, 19)));
	}

	@Test
	public void addressRanges_overlappingUnsplittableRanges() {
		// overlapping ranges are treated as one, so the range does not end inside either
		ModelAccessor model = testAccessor(0, 30, new IntRange(10, 15), new IntRange(5, 11));
		assertThat("Range ends before overlapping multi-register values",
				List.of(model.getAddressRanges(12)), contains(new IntRange(0, 4), new IntRange(5, 16),
						new IntRange(17, 28), new IntRange(29, 29)));
	}

	@Test
	public void addressRanges_unsplittableRangeLongerThanMaximum() {
		ModelAccessor model = testAccessor(0, 25, new IntRange(0, 24));
		assertThat("Range longer than the maximum is split", List.of(model.getAddressRanges(10)),
				contains(new IntRange(0, 9), new IntRange(10, 19), new IntRange(20, 24)));
	}

	@Test
	public void addressRange_fromAddress() {
		ModelAccessor model = testAccessor(0, 30, new IntRange(19, 20));
		assertThat("Range from an address", model.getAddressRange(10, 10),
				is(equalTo(new IntRange(10, 18))));
		assertThat("Range to the end of the model", model.getAddressRange(25, 10),
				is(equalTo(new IntRange(25, 29))));
	}

	@Test
	public void addressRange_invalidArguments() {
		ModelAccessor model = testAccessor(2, 10);
		for ( int[] args : new int[][] { { 1, 10 }, { 12, 10 }, { 2, 0 } } ) {
			try {
				model.getAddressRange(args[0], args[1]);
				fail(String.format("Address %d with maximum length %d should be rejected.", args[0],
						args[1]));
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
	}

	@Test
	public void unsplittableAddressRanges_matchSunSpecModels() throws IOException {
		final List<String> errors = new ArrayList<>();
		int count = 0;
		for ( Map.Entry<String, List<ModelPoints>> e : multiRegisterPoints().entrySet() ) {
			final ModelData data = ModelDataUtils.getModelDataInstance(getClass(), e.getKey());
			for ( ModelPoints model : e.getValue() ) {
				final List<IntRange> actual = modelRanges(findModel(data, model));
				if ( !actual.equals(model.points()) ) {
					errors.add(String.format("%s model %d: expected %s but was %s", model.resource(),
							model.modelId(), model.points(), actual));
				}
				count++;
			}
		}
		assertThat("Models compared", count, is(greaterThan(80)));
		assertThat("Model multi-register ranges match the SunSpec definitions", errors,
				is(equalTo(List.of())));
	}

	@Test
	public void readModelData_doesNotSplitPoints() throws IOException {
		final List<String> errors = new ArrayList<>();
		for ( Map.Entry<String, List<ModelPoints>> e : multiRegisterPoints().entrySet() ) {
			final List<IntRange> points = new ArrayList<>();
			for ( ModelPoints model : e.getValue() ) {
				points.addAll(model.points());
			}
			for ( int max : MAX_READ_LENGTHS ) {
				final RecordingModbusConnection conn = ModelDataUtils
						.getWritableModbusConnection(getClass(), e.getKey());
				final ModelData data = ModelDataFactory.getInstance().getModelData(conn, max);
				final BitSet read = new BitSet();
				for ( List<Integer> req : conn.getReads() ) {
					final int first = req.get(0);
					final int last = first + req.get(1) - 1;
					read.set(first, last + 1);
					if ( req.get(1) > max ) {
						errors.add(String.format("%s max %d: read %d-%d is too long", e.getKey(), max,
								first, last));
					}
					for ( IntRange p : points ) {
						if ( (p.getMin() < first && first <= p.getMax())
								|| (p.getMin() <= last && last < p.getMax()) ) {
							errors.add(String.format("%s max %d: read %d-%d splits point %s", e.getKey(),
									max, first, last, p));
						}
					}
				}
				final List<ModelAccessor> models = new ArrayList<>(data.getModels());
				models.add(data);
				for ( ModelAccessor m : models ) {
					final int end = m.getBlockAddress() + m.getModelLength();
					if ( read.nextClearBit(m.getBlockAddress()) < end ) {
						errors.add(String.format("%s max %d: model %d not fully read", e.getKey(), max,
								m.getModelId().getId()));
					}
				}
			}
		}
		assertThat("Model data read without splitting multi-register points", errors,
				is(equalTo(List.of())));
	}

	@Test
	public void readModelData_shortMaximumReadLength() throws IOException {
		// GIVEN
		// shorter than the 16 register common model strings
		final RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				"/net/solarnetwork/node/hw/sunspec/der/test/test-data-der-01.txt");

		// WHEN
		final ModelData data = ModelDataFactory.getInstance().getModelData(conn, 10);

		// THEN
		assertThat("Model data read", data.getManufacturer(), is(equalTo("OutBack Power")));
		for ( List<Integer> req : conn.getReads() ) {
			assertThat("Read no longer than maximum", req.get(1) <= 10, is(equalTo(true)));
		}
	}

	@Test
	public void readModelData_curveLayoutDiscovered() throws IOException {
		// GIVEN
		final RecordingModbusConnection conn = ModelDataUtils.getWritableModbusConnection(getClass(),
				"/net/solarnetwork/node/hw/sunspec/der/test/test-data-der-01.txt");
		// the 707 model, with 7 fixed block registers then 134 registers of curve sets
		final int blockAddress = 583;
		final int end = blockAddress + 141;

		// WHEN
		final ModelData data = ModelDataFactory.getInstance().getModelData(conn, 100);

		// THEN
		final List<List<Integer>> firstReads = modelReads(conn.getReads(), blockAddress, end);
		assertThat("Fixed block read first, as the curve set layout is not known yet", firstReads.get(0),
				is(equalTo(List.of(blockAddress, 7))));
		assertThat("Curve sets read after the fixed block", firstReads, hasSize(3));

		// WHEN
		conn.getReads().clear();
		data.readModelData(conn);

		// THEN
		final List<List<Integer>> nextReads = modelReads(conn.getReads(), blockAddress, end);
		assertThat("Curve set layout known from the earlier read", nextReads, hasSize(2));
		assertThat("First read longer than the fixed block", nextReads.get(0).get(1),
				is(greaterThan(7)));
	}

	private static List<List<Integer>> modelReads(List<List<Integer>> reads, int blockAddress, int end) {
		final List<List<Integer>> result = new ArrayList<>();
		for ( List<Integer> req : reads ) {
			if ( req.get(0) >= blockAddress && req.get(0) < end ) {
				result.add(req);
			}
		}
		return result;
	}

}
