/* ==================================================================
 * SimpleFilterSampleTransformerTests.java - 31/10/2016 2:34:10 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.samplefilter.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.datum.samplefilter.SimpleFilterSampleTransformer;
import net.solarnetwork.node.domain.GeneralNodeDatum;

/**
 * Test cases for the {@link SimpleFilterSampleTransformer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleFilterSampleTransformerTests {

	private static final String TEST_SOURCE_ID = "test.source";

	private static final String PROP_WATTS = "watts";
	private static final String PROP_WATTHOURS = "wattHours";
	private static final String PROP_FREQUENCY = "frequency";
	private static final String PROP_PHASE = "phase";

	private GeneralNodeDatum createTestGeneralNodeDatum(String sourceId) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setSourceId(sourceId);
		datum.putAccumulatingSampleValue(PROP_WATTHOURS, 1239340349L);
		datum.putInstantaneousSampleValue(PROP_FREQUENCY, 50.1);
		datum.putInstantaneousSampleValue(PROP_WATTS, 23.4);
		datum.putStatusSampleValue(PROP_PHASE, "Total");
		return datum;
	}

	@Test
	public void testInclude() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		SimpleFilterSampleTransformer xform = new SimpleFilterSampleTransformer();
		xform.setSourceId("^test");
		xform.setIncludes(new String[] { "^watt" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertEquals("Watts", datum.getSamples().getInstantaneousSampleDouble(PROP_WATTS),
				result.getInstantaneousSampleDouble(PROP_WATTS));
		assertNull("Frequency filtered", result.getInstantaneousSampleDouble(PROP_FREQUENCY));
		assertEquals("Watt hours", datum.getSamples().getAccumulatingSampleLong(PROP_WATTHOURS),
				result.getAccumulatingSampleLong(PROP_WATTHOURS));
		assertNull("Phase filtered", result.getStatusSampleString(PROP_PHASE));
	}

	@Test
	public void testIncludeMultiplePatterns() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		SimpleFilterSampleTransformer xform = new SimpleFilterSampleTransformer();
		xform.setSourceId("^test");
		xform.setIncludes(new String[] { "^watt", "^phase$" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertEquals("Watts", datum.getSamples().getInstantaneousSampleDouble(PROP_WATTS),
				result.getInstantaneousSampleDouble(PROP_WATTS));
		assertNull("Frequency filtered", result.getInstantaneousSampleDouble(PROP_FREQUENCY));
		assertEquals("Watt hours", datum.getSamples().getAccumulatingSampleLong(PROP_WATTHOURS),
				result.getAccumulatingSampleLong(PROP_WATTHOURS));
		assertEquals("Phase", datum.getSamples().getStatusSampleString(PROP_PHASE),
				result.getStatusSampleString(PROP_PHASE));
	}

	@Test
	public void testExclude() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		SimpleFilterSampleTransformer xform = new SimpleFilterSampleTransformer();
		xform.setSourceId("^test");
		xform.setExcludes(new String[] { "^watt" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertNull("Watts filtered", result.getInstantaneousSampleDouble(PROP_WATTS));
		assertEquals("Frequency", datum.getSamples().getInstantaneousSampleDouble(PROP_FREQUENCY),
				result.getInstantaneousSampleDouble(PROP_FREQUENCY));
		assertNull("Watt hours filtered", result.getAccumulatingSampleLong(PROP_WATTHOURS));
		assertEquals("Phase", datum.getSamples().getStatusSampleString(PROP_PHASE),
				result.getStatusSampleString(PROP_PHASE));
	}

	@Test
	public void testExcludeMultiplePatterns() {
		GeneralNodeDatum datum = createTestGeneralNodeDatum(TEST_SOURCE_ID);
		SimpleFilterSampleTransformer xform = new SimpleFilterSampleTransformer();
		xform.setSourceId("^test");
		xform.setExcludes(new String[] { "^watt", "^phase$" });
		xform.init();
		GeneralDatumSamples result = xform.transformSamples(datum, datum.getSamples());
		assertNotSame("New sample instance", datum.getSamples(), result);
		assertNull("Watts filtered", result.getInstantaneousSampleDouble(PROP_WATTS));
		assertEquals("Frequency", datum.getSamples().getInstantaneousSampleDouble(PROP_FREQUENCY),
				result.getInstantaneousSampleDouble(PROP_FREQUENCY));
		assertNull("Watt hours filtered", result.getAccumulatingSampleLong(PROP_WATTHOURS));
		assertNull("Phase filtered", result.getStatusSampleString(PROP_PHASE));
	}

}
