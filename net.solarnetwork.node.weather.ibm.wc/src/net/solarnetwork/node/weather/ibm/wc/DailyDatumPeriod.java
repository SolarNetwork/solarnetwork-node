/* ===================================================================
 * Copyright 2018 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.node.weather.ibm.wc;

/**
 * The hourly datum period enum represents the well defined IBM Weather Channel
 * Daily endpoints.
 * 
 * @author matt frost
 *
 */
public enum DailyDatumPeriod {
	THREEDAY("3day"),
	FIVEDAY("5day"),
	SEVENDAY("7day"),
	TENDAY("10day"),
	FIFTEENDAY("15day");

	private final String period;

	DailyDatumPeriod(String period) {
		this.period = period;
	}

	public String getPeriod() {
		return this.period;
	}

	@Override
	public String toString() {
		return this.getPeriod();
	}

	public static DailyDatumPeriod forPeriod(String period) {
		for ( DailyDatumPeriod e : DailyDatumPeriod.values() ) {
			if ( e.getPeriod().equalsIgnoreCase(period) ) {
				return e;
			}
		}
		return null;// not found
	}

}
