/* ==================================================================
 * XmlYrClientTests.java - 19/05/2017 4:11:04 PM
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

package net.solarnetwork.node.weather.yr.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.weather.yr.XmlYrClient;

/**
 * Test cases for the {@link XmlYrClient} class.
 * 
 * @author matt
 * @version 1.0
 */
public class XmlYrClientTests extends AbstractHttpClientTests {

	private static final String TEST_YR_LOC_IDENTIFIER = "/test/identifier";

	private XmlYrClient client;

	@Before
	public void setupClient() {
		client = new XmlYrClient();
		client.setBaseUrl(getHttpServerBaseUrl());
		client.init();
	}

	@Test
	public void getHourlyForecast() throws Exception {
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				assertEquals("GET", request.getMethod());
				assertEquals("Request path", "/place/test/identifier/forecast_hour_by_hour.xml",
						request.getPathInfo());
				respondWithXmlResource(response, "forecast_hour_by_hour-01.xml");
				response.flushBuffer();
				return true;
			}

		};
		getHttpServer().addHandler(handler);

		Collection<AtmosphericDatum> results = client.getHourlyForecast(TEST_YR_LOC_IDENTIFIER);
		assertThat("Request handled", handler.isHandled(), equalTo(true));
		assertThat("Result count", results, hasSize(48));
	}

}
