package net.solarnetwork.node.weather.ibm.wc.test;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.weather.ibm.wc.WCDayDatumDataSource;
import net.solarnetwork.node.weather.ibm.wc.WCHourlyDatum;
import net.solarnetwork.node.weather.ibm.wc.WCHourlyDatumDataSource;

/**
 * Tests retrieval from the IBM API
 * 
 * @author matt frost
 */
public class WCClientTests {
	
	
	private static final String TEST_SOURCE_ID = "Test";

	private WCDayDatumDataSource dailyService;
	private WCHourlyDatumDataSource hourlyService;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Before
	public void setup() {
		dailyService = new WCDayDatumDataSource();
		hourlyService = new WCHourlyDatumDataSource();
	}

	//@Test
	public void readDailyDatum() {
		dailyService.setLocationIdentifier("NZAA");
		dailyService.setLanguage("en-US");
		dailyService.setApiKey("b20225e17d6642598225e17d668259d2");
		dailyService.setDatumPeriod("7day");
		dailyService.setObjectMapper(new ObjectMapper());
		
		log.debug("set service params");
		Collection<GeneralDayDatum> d = dailyService.readMultipleDatum();
		log.debug("Retrieved data");
		log.debug("Got datum: {}", d);

		Assert.assertNotNull("Current datum", d);

	}
	
	@Test
	public void readHourlyDatum() {
		hourlyService.setLocationIdentifier("KDEN");
		hourlyService.setLanguage("en-US");
		hourlyService.setApiKey("b20225e17d6642598225e17d668259d2");
		hourlyService.setDatumPeriod("2day");
		hourlyService.setObjectMapper(new ObjectMapper());
		
		log.debug("set service params");
		Collection<WCHourlyDatum> d = hourlyService.readMultipleDatum();
		log.debug("Retrieved data");
		log.debug("Got datum: {}", d);

		Assert.assertNotNull("Current datum", d);

	}



}
