/* ==================================================================
 * DelimitedPriceDatumDataSourceTests.java - 26/09/2017 3:19:22 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.price.delimited.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import net.solarnetwork.node.datum.price.delimited.DelimitedPriceDatumDataSource;
import net.solarnetwork.node.domain.datum.PriceDatum;

/**
 * Test cases for the {@link DelimitedPriceDatumDataSource} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DelimitedPriceDatumDataSourceTests {

	private DelimitedPriceDatumDataSource dataSource;

	@Before
	public void setup() {
		dataSource = new DelimitedPriceDatumDataSource();
	}

	@Test
	public void parseLastRow() throws IOException {
		// GIVEN
		URL csvFile = new ClassPathResource("NZE-energy-prices-01.csv", getClass()).getURL();
		String url = new URL(csvFile, "NZE-{stationId}-01.csv").toString();
		dataSource.setUrl(url);
		dataSource.setStationId("energy-prices");

		// WHEN
		PriceDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum provided", datum, notNullValue());
		assertThat("Price parsed", datum.getPrice(), equalTo(new BigDecimal("27.89")));

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));
		assertThat("Timestamp", datum.getTimestamp(),
				equalTo(formatter.parse("26/09/2017 13:40:30", Instant::from)));
	}

	@Test
	public void parseLastRow_2() throws IOException {
		// GIVEN
		URL csvFile = new ClassPathResource("NZE-energy-prices-02.csv", getClass()).getURL();
		String url = new URL(csvFile, "NZE-{stationId}-02.csv").toString();
		dataSource.setUrl(url);
		dataSource.setStationId("energy-prices");

		// WHEN
		PriceDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum provided", datum, notNullValue());
		assertThat("Price parsed", datum.getPrice(), equalTo(new BigDecimal("169.31")));

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dataSource.getDateFormat())
				.withZone(ZoneId.of(dataSource.getTimeZoneId()));
		assertThat("Timestamp", datum.getTimestamp(),
				equalTo(formatter.parse("23/03/2023 10:44:59", Instant::from)));
	}

}
