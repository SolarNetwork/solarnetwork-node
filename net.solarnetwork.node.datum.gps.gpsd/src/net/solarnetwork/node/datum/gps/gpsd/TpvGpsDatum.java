/* ==================================================================
 * TpvGpsDatum.java - 16/11/2019 1:09:33 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.gps.gpsd;

import java.util.Date;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.gpsd.domain.TpvReportMessage;

/**
 * Datum for a {@link TpvReportMessage}.
 * 
 * @author matt
 * @version 1.0
 */
public class TpvGpsDatum extends GeneralNodeDatum {

	/**
	 * Constructor.
	 * 
	 * @param tpv
	 *        the report data
	 */
	public TpvGpsDatum(TpvReportMessage tpv) {
		super();
		setCreated(new Date(tpv.getTimestamp() != null ? tpv.getTimestamp().toEpochMilli()
				: System.currentTimeMillis()));

		GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
		setSamples(s);

		putInstantaneousSampleValue("lat", tpv.getLatitude());
		putInstantaneousSampleValue("latError", tpv.getLatitudeError());
		putInstantaneousSampleValue("lon", tpv.getLongitude());
		putInstantaneousSampleValue("lon_ep", tpv.getLongitudeError());
		putInstantaneousSampleValue("alt", tpv.getAltitude());
		putInstantaneousSampleValue("alt_ep", tpv.getAltitudeError());
		putInstantaneousSampleValue("course", tpv.getCourse());
		putInstantaneousSampleValue("course_ep", tpv.getCourseError());
		putInstantaneousSampleValue("speed", tpv.getSpeed());
		putInstantaneousSampleValue("speed_ep", tpv.getSpeedError());
		putInstantaneousSampleValue("climbRate", tpv.getClimbRate());
		putInstantaneousSampleValue("climb_ep", tpv.getClimbRateError());
	}

}
