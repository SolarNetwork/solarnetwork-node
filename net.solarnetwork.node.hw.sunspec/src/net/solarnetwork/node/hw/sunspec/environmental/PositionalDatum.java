/* ==================================================================
 * PositionalDatum.java - 10/07/2023 3:43:08 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.environmental;

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import net.solarnetwork.codec.BasicLocationField;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;

/**
 * Datum for a SunSpec compatible GPS or inclination device.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class PositionalDatum extends SimpleDatum {

	private static final long serialVersionUID = -8632975478821479926L;

	/** An instantaneous sample key for {@link #getInclineX()} values. */
	public static final String INCLINATION_X_KEY = "inclineX";

	/** An instantaneous sample key for {@link #getInclineY()} values. */
	public static final String INCLINATION_Y_KEY = "inclineY";

	/** An instantaneous sample key for {@link #getInclineZ()} values. */
	public static final String INCLINATION_Z_KEY = "inclineZ";

	/** The primary model source data. */
	private final ModelAccessor data;

	/**
	 * Test if a set of model IDs includes any models supported by this datum
	 * class.
	 * 
	 * @param modelIds
	 *        the set of model IDs
	 * @return {@literal true} if the set contains a supported model ID
	 */
	public static boolean includesSupportedModelId(Set<Integer> modelIds) {
		return (modelIds != null && (modelIds.contains(EnvironmentalModelId.GPS.getId())
				|| modelIds.contains(EnvironmentalModelId.Inclinometer.getId())));
	}

	/**
	 * Construct from a {@link ModelData}.
	 * 
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 */
	public PositionalDatum(ModelData data, String sourceId) {
		super(DatumId.nodeId(null, sourceId, dataTimestamp(data)), new DatumSamples());
		this.data = data;
		populateMeasurements(data.findTypedModel(GpsModelAccessor.class));
		populateMeasurements(data.findTypedModel(InclinometerModelAccessor.class));
	}

	private static Instant dataTimestamp(ModelData data) {
		GpsModelAccessor gps = data.findTypedModel(GpsModelAccessor.class);
		Instant ts = (gps != null ? gps.getGpsTimestamp() : null);
		return (ts != null ? ts : data.getDataTimestamp());
	}

	/**
	 * Construct from a {@link GpsModelAccessor}.
	 * 
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 */
	public PositionalDatum(GpsModelAccessor data, String sourceId) {
		super(DatumId.nodeId(null, sourceId, data.getGpsTimestamp()), new DatumSamples());
		this.data = data;
		populateMeasurements(data);
	}

	/**
	 * Construct from a {@link GpsModelAccessor}.
	 * 
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 */
	public PositionalDatum(InclinometerModelAccessor data, String sourceId) {
		super(DatumId.nodeId(null, sourceId, data.getDataTimestamp()), new DatumSamples());
		this.data = data;
		populateMeasurements(data);
	}

	/**
	 * Populate datum properties from a {@link GpsModelAccessor}.
	 * 
	 * @param data
	 *        the data
	 */
	public void populateMeasurements(GpsModelAccessor data) {
		setLatitude(data.getLatitude());
		setLongitude(data.getLongitude());
		setElevation(bigDecimalForNumber(data.getAltitude()));
	}

	/**
	 * Populate datum properties from a {@link InclinometerModelAccessor}.
	 * 
	 * @param data
	 *        the data
	 */
	public void populateMeasurements(InclinometerModelAccessor data) {
		List<Incline> inclines = data.getInclines();
		if ( inclines != null && !inclines.isEmpty() ) {
			Incline incline = inclines.get(0);
			setInclineX(incline.getInclineX());
			setInclineY(incline.getInclineY());
			setInclineZ(incline.getInclineZ());
		}
	}

	/**
	 * Get the raw data used to populate this datum.
	 * 
	 * @return the data
	 */
	public ModelAccessor getData() {
		return data;
	}

	/**
	 * Get the decimal latitude.
	 * 
	 * @return the latitude
	 */
	public BigDecimal getLatitude() {
		return getSampleBigDecimal(Instantaneous, BasicLocationField.Latitude.getFieldName());
	}

	/**
	 * Set the decimal latitude.
	 * 
	 * @param value
	 *        the latitude to set
	 */
	public void setLatitude(BigDecimal value) {
		putSampleValue(Instantaneous, BasicLocationField.Latitude.getFieldName(), value);
	}

	/**
	 * Get the decimal longitude.
	 * 
	 * @return the longitude
	 */
	public BigDecimal getLongitude() {
		return getSampleBigDecimal(Instantaneous, BasicLocationField.Longitude.getFieldName());
	}

	/**
	 * Set the decimal longitude.
	 * 
	 * @param value
	 *        the longitude to set
	 */
	public void setLongitude(BigDecimal value) {
		putSampleValue(Instantaneous, BasicLocationField.Longitude.getFieldName(), value);
	}

	/**
	 * Get the elevation.
	 * 
	 * @return the elevation, in meters
	 */
	public BigDecimal getElevation() {
		return getSampleBigDecimal(Instantaneous, BasicLocationField.Elevation.getFieldName());
	}

	/**
	 * Set the elevation.
	 * 
	 * @param value
	 *        the elevation to set, in meters
	 */
	public void setElevation(BigDecimal value) {
		putSampleValue(Instantaneous, BasicLocationField.Elevation.getFieldName(), value);
	}

	/**
	 * Get the X-axis inclination.
	 * 
	 * @return the X inclination, in degrees
	 */
	public Float getInclineX() {
		return getSampleFloat(Instantaneous, INCLINATION_X_KEY);
	}

	/**
	 * Set the X-axis inclination.
	 * 
	 * @param value
	 *        the X inclination to set, in degrees
	 */
	public void setInclineX(Float value) {
		putSampleValue(Instantaneous, INCLINATION_X_KEY, value);
	}

	/**
	 * Get the Y-axis inclination.
	 * 
	 * @return the Y inclination, in degrees
	 */
	public Float getInclineY() {
		return getSampleFloat(Instantaneous, INCLINATION_Y_KEY);
	}

	/**
	 * Set the Y-axis inclination.
	 * 
	 * @param value
	 *        the Y inclination to set, in degrees
	 */
	public void setInclineY(Float value) {
		putSampleValue(Instantaneous, INCLINATION_Y_KEY, value);
	}

	/**
	 * Get the Z-axis inclination.
	 * 
	 * @return the Z inclination, in degrees
	 */
	public Float getInclineZ() {
		return getSampleFloat(Instantaneous, INCLINATION_Z_KEY);
	}

	/**
	 * Set the Z-axis inclination.
	 * 
	 * @param value
	 *        the Z inclination to set, in degrees
	 */
	public void setInclineZ(Float value) {
		putSampleValue(Instantaneous, INCLINATION_Z_KEY, value);
	}

}
