/* ==================================================================
 * SystemHealthService.java - 14/12/2021 11:16:12 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.PingTestResultDisplay;

/**
 * API for monitoring SolarNode system health.
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public interface SystemHealthService {

	/**
	 * A collection of ping test results.
	 */
	@JsonPropertyOrder({ "allGood", "date", "results" })
	public static class PingTestResults {

		private final Instant date;
		private final Map<String, PingTestResultDisplay> results;
		private final boolean allGood;

		/**
		 * Construct with values.
		 * 
		 * @param date
		 *        The date the tests were executed at.
		 * @param results
		 *        The test results (or {@literal null} if none available).
		 */
		public PingTestResults(Instant date, Map<String, PingTestResultDisplay> results) {
			super();
			this.date = date;
			boolean allOK = true;
			if ( results == null ) {
				this.results = Collections.emptyMap();
				allOK = false;
			} else {
				this.results = results;
				for ( PingTestResultDisplay r : results.values() ) {
					if ( !r.isSuccess() ) {
						allOK = false;
						break;
					}
				}
			}
			allGood = allOK;
		}

		/**
		 * Get a map of test ID to test results.
		 * 
		 * @return All test results.
		 */
		public Map<String, PingTestResultDisplay> getResults() {
			return results;
		}

		/**
		 * Get the date the tests were executed.
		 * 
		 * @return The date.
		 */
		public Instant getDate() {
			return date;
		}

		/**
		 * Return {@literal true} if there are test results available and all
		 * the results return {@literal true} for
		 * {@link PingTestResult#isSuccess()}.
		 * 
		 * @return {@literal true} if test results are available and all tests
		 *         passed
		 */
		public boolean isAllGood() {
			return allGood;
		}

	}

	/**
	 * Perform a set of ping tests and return their results.
	 * 
	 * @param pingTestIds
	 *        an optional set of ID regular expressions to limit the tests to,
	 *        or {@literal null} or empty to perform all available tests
	 * @return the results, never {@literal null}
	 */
	PingTestResults performPingTests(Set<String> pingTestIds);

}
