/* ==================================================================
 * RestTemplateOverlayCloudServiceTests.java - 7/07/2022 7:36:42 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.overlay.cloud.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;
import net.solarnetwork.node.datum.overlay.cloud.FeedData;
import net.solarnetwork.node.datum.overlay.cloud.Grid;
import net.solarnetwork.node.datum.overlay.cloud.RestTemplateOverlayCloudService;

/**
 * Test cases for the {@link RestTemplateOverlayCloudService}.
 * 
 * @author matt
 * @version 1.0
 */
public class RestTemplateOverlayCloudServiceTests {

	private Server httpServer;
	private RestTemplateOverlayCloudService service;

	@Before
	public void setup() throws Exception {
		httpServer = new Server(0);
		httpServer.start();
		service = new RestTemplateOverlayCloudService();
		service.setBaseUrl("http://localhost:" + getHttpServerPort());
	}

	private int getHttpServerPort() {
		return httpServer.getConnectors()[0].getLocalPort();
	}

	@After
	public void cleanup() throws Exception {
		httpServer.stop();
	}

	@Test
	public void getGrids() {
		// GIVEN
		AbstractTestHandler handler = new AbstractTestHandler() {

			@Override
			protected boolean handleInternal(String target, HttpServletRequest request,
					HttpServletResponse response, int dispatch) throws Exception {
				assertThat("HTTP method", request.getMethod(), is(equalTo("GET")));
				assertThat("HTTP path", target, is(equalTo("/Grid")));
				sendJsonResponse("grid-list-01.json", response);
				return true;
			}

		};
		httpServer.addHandler(handler);

		// WHEN
		List<Grid> result = service.getGrids();

		// THEN
		assertThat("Grid entities returned", result, hasSize(2));
		assertThat("Grid 1 ID", result.get(0).getId(), is(equalTo(1L)));
		assertThat("Grid 1 name", result.get(0).getName(), is(equalTo("Foobar Gym Basement")));
		assertThat("Grid 2 ID", result.get(1).getId(), is(equalTo(2L)));
		assertThat("Grid 2 name", result.get(1).getName(), is(equalTo("Someplace Else")));
	}

	@Test
	public void getFeedLatest() {
		// GIVEN
		AbstractTestHandler handler = new AbstractTestHandler() {

			@Override
			protected boolean handleInternal(String target, HttpServletRequest request,
					HttpServletResponse response, int dispatch) throws Exception {
				assertThat("HTTP method", request.getMethod(), is(equalTo("GET")));
				assertThat("HTTP path", target, is(equalTo("/Grid/1/Feed/2/Latest")));
				sendJsonResponse("feed-latest-01.json", response);
				return true;
			}

		};
		httpServer.addHandler(handler);

		// WHEN
		FeedData result = service.getFeedLatest(1L, 2L);

		// THEN
		assertThat("Feed data returned", result, is(notNullValue()));
		assertThat("Timestamp", result.getTimestamp(),
				is(equalTo(LocalDateTime.of(2022, 3, 1, 0, 10, 0).toInstant(ZoneOffset.UTC))));
		assertThat("Current A", result.getCurrentA(), is(equalTo(36.0)));
		assertThat("Current B", result.getCurrentB(), is(equalTo(8.0)));
		assertThat("Current C", result.getCurrentC(), is(equalTo(12.0)));
	}

}
