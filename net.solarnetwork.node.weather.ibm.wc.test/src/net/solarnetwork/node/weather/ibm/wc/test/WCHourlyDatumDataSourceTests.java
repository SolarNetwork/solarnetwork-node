package net.solarnetwork.node.weather.ibm.wc.test;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collection;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.weather.ibm.wc.BasicWCClient;
import net.solarnetwork.node.weather.ibm.wc.WCClient;
import net.solarnetwork.node.weather.ibm.wc.WCHourlyDatum;
import net.solarnetwork.node.weather.ibm.wc.WCHourlyDatumDataSource;

/**
 * Tests the parsing for hourly forecasts using downloaded JSON
 * 
 * @author matt frost
 *
 */
public class WCHourlyDatumDataSourceTests{
	private static final String TEST_SOURCE_ID = "src.test";
	private static final String TEST_PERIOD = "1day";
	private static final String TEST_KEY = "key.test";
	private static final String TEST_LOC = "loc.test";
	
	private WCHourlyDatumDataSource hourlyService;

	private WCClient client;
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Before
	public void additionalSetup() {

		client = EasyMock.createMock(WCClient.class);
		
		hourlyService = new WCHourlyDatumDataSource();
		hourlyService.setClient(client);
		hourlyService.setDatumPeriod(TEST_PERIOD);
		hourlyService.setApiKey(TEST_KEY);
		hourlyService.setLocationIdentifier(TEST_LOC);
		hourlyService.setObjectMapper(new ObjectMapper());

	}

	@After
	public void finish() {
		EasyMock.verify(client);
	}

	private void replayAll() {
		EasyMock.replay(client);
	}


	@Test
	public void defaultValues() {
		replayAll();
		WCHourlyDatumDataSource ds = new WCHourlyDatumDataSource();
		assertTrue("Client available", ds.getClient() instanceof BasicWCClient);
	}
	
	@Test
	public void readMultipleDatum() {

		final WCHourlyDatum datum = new WCHourlyDatum();
		datum.setCreated(new Date(System.currentTimeMillis()));
		datum.setSourceId(TEST_SOURCE_ID);
		final Collection<WCHourlyDatum> datumList = Arrays.asList(datum);

		// followed by forecast
		expect(hourlyService.readMultipleDatum()).andReturn(datumList);

		replayAll();

		Collection<WCHourlyDatum> result = hourlyService.readMultipleDatum();
		assertEquals("Forecast list", Arrays.asList(datum), result);
	}

}
