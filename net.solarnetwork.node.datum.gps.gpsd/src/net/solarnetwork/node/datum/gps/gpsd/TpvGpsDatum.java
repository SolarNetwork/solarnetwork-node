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

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import java.time.Instant;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.io.gpsd.domain.TpvReportMessage;

/**
 * Datum for a {@link TpvReportMessage}.
 * 
 * @author matt
 * @version 2.0
 */
public class TpvGpsDatum extends SimpleDatum {

	private static final long serialVersionUID = 5942243017470296625L;

	/**
	 * Constructor.
	 * 
	 * @param tpv
	 *        the report data
	 * @param sourceId
	 *        the source ID
	 */
	public TpvGpsDatum(TpvReportMessage tpv, String sourceId) {
		super(DatumId.nodeId(null, sourceId,
				tpv.getTimestamp() != null ? tpv.getTimestamp() : Instant.now()), new DatumSamples());
		MutableDatumSamplesOperations ops = asMutableSampleOperations();
		ops.putSampleValue(Instantaneous, "lat", tpv.getLatitude());
		ops.putSampleValue(Instantaneous, "lat_ep", tpv.getLatitudeError());
		ops.putSampleValue(Instantaneous, "lon", tpv.getLongitude());
		ops.putSampleValue(Instantaneous, "lon_ep", tpv.getLongitudeError());
		ops.putSampleValue(Instantaneous, "alt", tpv.getAltitude());
		ops.putSampleValue(Instantaneous, "alt_ep", tpv.getAltitudeError());
		ops.putSampleValue(Instantaneous, "course", tpv.getCourse());
		ops.putSampleValue(Instantaneous, "course_ep", tpv.getCourseError());
		ops.putSampleValue(Instantaneous, "speed", tpv.getSpeed());
		ops.putSampleValue(Instantaneous, "speed_ep", tpv.getSpeedError());
		ops.putSampleValue(Instantaneous, "climbRate", tpv.getClimbRate());
		ops.putSampleValue(Instantaneous, "climbRate_ep", tpv.getClimbRateError());
	}

}
