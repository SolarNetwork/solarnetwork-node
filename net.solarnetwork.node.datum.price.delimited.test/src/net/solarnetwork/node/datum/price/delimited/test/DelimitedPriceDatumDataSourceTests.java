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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import net.solarnetwork.node.datum.price.delimited.DelimitedPriceDatumDataSource;
import net.solarnetwork.node.domain.PriceDatum;

/**
 * Test cases for the {@link DelimitedPriceDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DelimitedPriceDatumDataSourceTests {

	private DelimitedPriceDatumDataSource dataSource;

	@Before
	public void setup() {
		dataSource = new DelimitedPriceDatumDataSource();
	}

	@Test
	public void parseLastRow() throws IOException, ParseException {
		URL csvFile = new ClassPathResource("NZE-energy-prices-01.csv", getClass()).getURL();
		String url = new URL(csvFile, "NZE-{stationId}-01.csv").toString();
		dataSource.setUrl(url);
		dataSource.setStationId("energy-prices");
		PriceDatum datum = dataSource.readCurrentDatum();
		assertThat(datum, notNullValue());
		assertThat(datum.getPrice(), equalTo(new BigDecimal("27.89")));

		SimpleDateFormat sdf = new SimpleDateFormat(dataSource.getDateFormat());
		sdf.setTimeZone(TimeZone.getTimeZone(dataSource.getTimeZoneId()));

		assertThat(datum.getCreated(), equalTo(sdf.parse("26/09/2017 13:40:30")));
	}

}
