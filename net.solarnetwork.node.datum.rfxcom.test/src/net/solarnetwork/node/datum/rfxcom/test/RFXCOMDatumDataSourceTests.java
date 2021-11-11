/* ==================================================================
 * RFXCOMACEnergyDatumSourceTest.java - Dec 3, 2012 6:50:26 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.rfxcom.test;

import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.datum.rfxcom.RFXCOMDatumDataSource;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.rfxcom.Message;
import net.solarnetwork.node.rfxcom.MessageFactory;
import net.solarnetwork.node.rfxcom.MessageHandler;
import net.solarnetwork.node.rfxcom.RFXCOM;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the @{link RFXCOMDatumDataSource} class.
 * 
 * @author matt
 * @version 2.0
 */
public class RFXCOMDatumDataSourceTests {

	private RFXCOMDatumDataSource dataSource;
	private RFXCOM rfxcom;
	private MessageFactory messageFactory;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Before
	public void setup() {
		rfxcom = EasyMock.createMock(RFXCOM.class);

		dataSource = new RFXCOMDatumDataSource();
		dataSource.setRfxcomTracker(new StaticOptionalService<>(rfxcom));

		messageFactory = new MessageFactory();
	}

	private Collection<NodeDatum> doReadDatum(String messageString) {
		// GIVEN
		final List<Message> messages = new ArrayList<>();
		messages.add(messageFactory.parseMessage(TestUtils.bytesFromHexString(messageString), 0));

		try {
			rfxcom.listenForMessages(assertWith(new Assertion<MessageHandler>() {

				@Override
				public void check(MessageHandler handler) throws Throwable {
					int i = 0;
					while ( i < messages.size() ) {
						if ( !handler.handleMessage(messages.get(i++)) ) {
							break;
						}
					}
				}

			}));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}

		// WHEN
		replay(rfxcom);
		Collection<NodeDatum> datum = dataSource.readMultipleDatum();

		// THEN
		verify(rfxcom);

		return datum;
	}

	private List<NodeDatum>[] doReadMultipleDatum(String[] messages) {
		@SuppressWarnings("unchecked")
		List<NodeDatum>[] results = new List[messages.length];
		int i = 0;
		for ( String messageString : messages ) {
			results[i++] = new ArrayList<>(doReadDatum(messageString));
			EasyMock.reset(rfxcom);
		}
		return results;
	}

	@Test
	public void getValidACEnergyDatumNoChange() {
		Collection<NodeDatum>[] results = doReadMultipleDatum(new String[] {
				"115A010188F200000000000000000036B079", "115A010188F200000000000000000036B079",
				"115A010188F200000000000000000036B079" });
		log.debug("Got results: {}", Arrays.asList(results));
		assertEquals(0, results[0].size());
		assertEquals(0, results[1].size());
		assertEquals(3, results[2].size());
		for ( NodeDatum datum : results[2] ) {
			assertEquals("88F2", datum.getSourceId());
			assertEquals("Use", Long.valueOf(63L), ((AcEnergyDatum) datum).getWattHourReading());
		}
	}

	@Test
	public void getBadACEnergyDatum() {
		List<NodeDatum>[] results = doReadMultipleDatum(
				new String[] { "115a011b6b120000000162000000d0666559",
						"115a011c6b120000000172000000d06bc159", "115a011d6b120000000172000000d0716159",
						"115a011f6b120000000182000000d0769359", "115a01206b120200000152006b00115a0121", // this should return null
						"115a01236b120000000152000000d080fc59", "115a01246b120000000152000000d0862059",
						"115a01256b120100000172000000d08a4459" });
		log.debug("Got results: {}", Arrays.asList(results));
		// we expect 7 messages to get through out of the 8 sent... the first 2 are buffered automatically,
		// then the "bad" message should cause it to be buffered and then flushed out
		assertEquals(0, results[0].size());
		assertEquals(0, results[1].size());
		assertEquals(3, results[2].size());
		assertEquals(1, results[3].size());
		assertEquals(0, results[4].size());
		assertEquals(0, results[5].size());
		assertEquals(0, results[6].size());
		assertEquals(3, results[7].size());
		assertEquals(61093L, ((AcEnergyDatum) results[7].get(0)).getWattHourReading().longValue());
		assertEquals(61099L, ((AcEnergyDatum) results[7].get(1)).getWattHourReading().longValue());
		assertEquals(61104L, ((AcEnergyDatum) results[7].get(2)).getWattHourReading().longValue());
	}

	@Test
	public void getBadACEnergyDatumWhileWarmingUp() {
		List<NodeDatum>[] results = doReadMultipleDatum(new String[] {
				"115a011f6b120000000182000000d0769359", "115a01206b120200000152006b00115a0121", // this should return null
				"115a01236b120000000152000000d080fc59", "115a01246b120000000152000000d0862059",
				"115a01256b120100000172000000d08a4459" });
		log.debug("Got results: {}", Arrays.asList(results));
		// We expect 3 messages to get through out of the 5 sent... the 2nd "bad" message causes
		// the 1st messag (which was automatically buffered) to get discarded, along with the bad
		// message itself. So we lost a valid datum here, but that is the expected outcome.
		assertEquals(0, results[0].size());
		assertEquals(0, results[1].size());
		assertEquals(0, results[2].size());
		assertEquals(0, results[3].size());
		assertEquals(3, results[4].size());
		assertEquals(61093L, ((AcEnergyDatum) results[4].get(0)).getWattHourReading().longValue());
		assertEquals(61099L, ((AcEnergyDatum) results[4].get(1)).getWattHourReading().longValue());
		assertEquals(61104L, ((AcEnergyDatum) results[4].get(2)).getWattHourReading().longValue());
	}

	@Test
	public void getBadACEnergyDatumTurnsOutToBeGood() {
		List<NodeDatum>[] results = doReadMultipleDatum(new String[] {
				"115a011f6b120000000182000000d0769359", "115a01206b120200000152006b00115a0121", // this is the "bad" data
				"115a01206b120200000152006b00115a0121", // this is the "bad" data
				"115a01206b120200000152006b00115a0121", // this is the "bad" data
		});
		log.debug("Got results: {}", Arrays.asList(results));
		// We expect 3 messages to get through out of the 5 sent... the 2nd "bad" message causes
		// the 1st messag (which was automatically buffered) to get discarded, along with the bad
		// message itself. So we lost a valid datum here, but that is the expected outcome.
		assertEquals(0, results[0].size());
		assertEquals(0, results[1].size());
		assertEquals(0, results[2].size());
		assertEquals(3, results[3].size());
		assertEquals(2054682597L, ((AcEnergyDatum) results[3].get(0)).getWattHourReading().longValue());
		assertEquals(2054682597L, ((AcEnergyDatum) results[3].get(1)).getWattHourReading().longValue());
		assertEquals(2054682597L, ((AcEnergyDatum) results[3].get(2)).getWattHourReading().longValue());
	}

}
