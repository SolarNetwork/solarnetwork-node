/* ==================================================================
 * MeteorologicalDatum.java - 10/07/2023 2:56:36 pm
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
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;

/**
 * Datum for a SunSpec compatible meteorolgical device.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class MeteorologicalDatum extends SimpleAtmosphericDatum {

	private static final long serialVersionUID = -5570238095842311281L;

	/**
	 * An instantaneous sample key for {@link #getBackOfModuleTemperature()}
	 * values.
	 */
	public static final String BOM_TEMPERATURE_KEY = "tempBOM";

	/** An instantaneous sample key for {@link #getElectricField()} values. */
	public static final String ELECTRIC_FIELD_KEY = "tempBOM";

	/**
	 * A status sample key for {@link #getPrecipitationType()} values.
	 */
	public static final String PRECIPITATION_TYPE_KEY = "precipitationType";

	/**
	 * An instantaneous sample key for {@link #getSurfaceWetness()} values.
	 */
	public static final String SURFACE_WETNESS_KEY = "surfaceWetness";

	/**
	 * An instantaneous sample key for {@link #getSoilMoisture()} values.
	 */
	public static final String SOIL_MOISTURE_KEY = "soilMoisture";

	/**
	 * An instantaneous sample key for {@link #getPlaneOfArrayIrradiance()}
	 * values.
	 */
	public static final String POA_IRRADIANCE_KEY = "irradiancePOA";

	/**
	 * An instantaneous sample key for {@link #getDiffuseIrradiance()} values.
	 */
	public static final String DF_IRRADIANCE_KEY = "irradianceDF";

	/**
	 * An instantaneous sample key for {@link #getDirectNormalIrradiance()}
	 * values.
	 */
	public static final String DN_IRRADIANCE_KEY = "irradianceDN";

	/**
	 * An instantaneous sample key for {@link #getOtherIrradiance()} values.
	 */
	public static final String OTHER_IRRADIANCE_KEY = "irradianceOT";

	/** The primary model source data. */
	private final ModelAccessor data;

	/**
	 * Construct from a {@link ModelData}.
	 * 
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 */
	public MeteorologicalDatum(ModelData data, String sourceId) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		populateMeasurements(data.findTypedModel(MeteorologicalModelAccessor.class));
		populateMeasurements(data.findTypedModel(MiniMeteorologicalModelAccessor.class));
		populateMeasurements(data.findTypedModel(IrradianceModelAccessor.class));
		populateMeasurements(data.findTypedModel(BomTemperatureModelAccessor.class));
	}

	/**
	 * Construct from a {@link MeteorologicalModelAccessor}.
	 * 
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 */
	public MeteorologicalDatum(MeteorologicalModelAccessor data, String sourceId) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		populateMeasurements(data);
	}

	/**
	 * Construct from a {@link MiniMeteorologicalModelAccessor}.
	 * 
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 */
	public MeteorologicalDatum(MiniMeteorologicalModelAccessor data, String sourceId) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		populateMeasurements(data);
	}

	/**
	 * Construct from a {@link IrradianceModelAccessor}.
	 * 
	 * @param data
	 *        the sample data
	 * @param sourceId
	 *        the source ID
	 */
	public MeteorologicalDatum(IrradianceModelAccessor data, String sourceId) {
		super(sourceId, data.getDataTimestamp(), new DatumSamples());
		this.data = data;
		populateMeasurements(data);
	}

	/**
	 * Populate datum properties from a {@link MeteorologicalModelAccessor}.
	 * 
	 * @param data
	 *        the data
	 */
	public void populateMeasurements(MeteorologicalModelAccessor data) {
		if ( data == null ) {
			return;
		}
		setTemperature(bigDecimalForNumber(data.getAmbientTemperature()));
		setHumidity(data.getRelativeHumidity());
		setAtmosphericPressure(data.getAtmosphericPressure());
		setWindSpeed(bigDecimalForNumber(data.getWindSpeed()));
		setWindDirection(data.getWindDirection());
		setRain(data.getRainAccumulation());
		setSnow(data.getSnowAccumulation());
		setPrecipitationType(data.getPrecipitationType());
		setElectricField(data.getElectricField());
		setSurfaceWetness(data.getSurfaceWetness());
		setSoilMoisture(data.getSoilMoisture());
	}

	/**
	 * Populate datum properties from a {@link MiniMeteorologicalModelAccessor}.
	 * 
	 * @param data
	 *        the data
	 */
	public void populateMeasurements(MiniMeteorologicalModelAccessor data) {
		if ( data == null ) {
			return;
		}
		setIrradiance(bigDecimalForNumber(data.getGlobalHorizontalIrradiance()));
		setTemperature(bigDecimalForNumber(data.getAmbientTemperature()));
		setBackOfModuleTemperature(bigDecimalForNumber(data.getBackOfModuleTemperature()));
		setWindSpeed(bigDecimalForNumber(data.getWindSpeed()));
	}

	/**
	 * Populate datum properties from a {@link IrradianceModelAccessor}.
	 * 
	 * @param data
	 *        the data
	 */
	public void populateMeasurements(IrradianceModelAccessor data) {
		if ( data == null ) {
			return;
		}
		setIrradiance(bigDecimalForNumber(data.getGlobalHorizontalIrradiance()));
		setPlaneOfArrayIrradiance(bigDecimalForNumber(data.getPlaneOfArrayIrradiance()));
		setDiffuseIrradiance(bigDecimalForNumber(data.getDiffuseIrradiance()));
		setDirectNormalIrradiance(bigDecimalForNumber(data.getDirectNormalIrradiance()));
		setOtherIrradiance(bigDecimalForNumber(data.getOtherIrradiance()));
	}

	/**
	 * Populate datum properties from a {@link BomTemperatureModelAccessor}.
	 * 
	 * @param data
	 *        the data
	 */
	public void populateMeasurements(BomTemperatureModelAccessor data) {
		if ( data == null ) {
			return;
		}
		List<Float> temps = data.getBackOfModuleTemperatures();
		if ( temps != null && !temps.isEmpty() ) {
			int i = 1;
			for ( Float f : temps ) {
				if ( i == 1 ) {
					setBackOfModuleTemperature(bigDecimalForNumber(f));
				} else {
					putSampleValue(Instantaneous, String.format("%s_%d", BOM_TEMPERATURE_KEY, i),
							bigDecimalForNumber(f));
				}
				i++;
			}
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
	 * Get the instantaneous temperature.
	 * 
	 * @return the temperature, in degrees Celsius
	 */
	public BigDecimal getBackOfModuleTemperature() {
		return getSampleBigDecimal(Instantaneous, BOM_TEMPERATURE_KEY);
	}

	/**
	 * Set the instantaneous back-of-module temperature.
	 * 
	 * @param value
	 *        the temperature to set, in degrees Celsius
	 */
	public void setBackOfModuleTemperature(BigDecimal value) {
		putSampleValue(Instantaneous, BOM_TEMPERATURE_KEY, value);
	}

	/**
	 * Get the instantaneous electric field.
	 * 
	 * @return the temperature, in V/m
	 */
	public Integer getElectricField() {
		return getSampleInteger(Instantaneous, ELECTRIC_FIELD_KEY);
	}

	/**
	 * Set the instantaneous electric field.
	 * 
	 * @param value
	 *        the electric field to set, in V/m
	 */
	public void setElectricField(Integer value) {
		putSampleValue(Instantaneous, ELECTRIC_FIELD_KEY, value);
	}

	/**
	 * Get the precipitation type.
	 * 
	 * @return the type
	 */
	public PrecipitationType getPrecipitationType() {
		Integer code = getSampleInteger(Status, PRECIPITATION_TYPE_KEY);
		return (code != null ? CodedValue.forCodeValue(code.intValue(), PrecipitationType.class, null)
				: null);
	}

	/**
	 * Set the precipitation type.
	 * 
	 * @param value
	 *        the precipitation type to set
	 */
	public void setPrecipitationType(PrecipitationType value) {
		putSampleValue(Status, PRECIPITATION_TYPE_KEY, value != null ? value.getCode() : null);
		setSkyConditions(value.getDescription(Locale.ENGLISH));
	}

	/**
	 * Get the instantaneous surface wetness.
	 * 
	 * @return the wetness, in Ohm
	 */
	public Integer getSurfaceWetness() {
		return getSampleInteger(Instantaneous, SURFACE_WETNESS_KEY);
	}

	/**
	 * Set the instantaneous surface wetness.
	 * 
	 * @param value
	 *        the wetness to set, in Ohm
	 */
	public void setSurfaceWetness(Integer value) {
		putSampleValue(Instantaneous, SURFACE_WETNESS_KEY, value);
	}

	/**
	 * Get the instantaneous soil moisture.
	 * 
	 * @return the wetness, as an integer percent
	 */
	public Integer getSoilMoisture() {
		return getSampleInteger(Instantaneous, SOIL_MOISTURE_KEY);
	}

	/**
	 * Set the instantaneous soil moisture.
	 * 
	 * @param value
	 *        the moisture to set, as an integer percentage
	 */
	public void setSoilMoisture(Integer value) {
		putSampleValue(Instantaneous, SOIL_MOISTURE_KEY, value);
	}

	/**
	 * Get the instantaneous plane-of-array irradiance.
	 * 
	 * @return the irradiance, in W/m2
	 */
	public BigDecimal getPlaneOfArrayIrradiance() {
		return getSampleBigDecimal(Instantaneous, POA_IRRADIANCE_KEY);
	}

	/**
	 * Set the instantaneous plane-of-array irradiance.
	 * 
	 * @param value
	 *        the irradiance to set, in W/m2
	 */
	public void setPlaneOfArrayIrradiance(BigDecimal value) {
		putSampleValue(Instantaneous, POA_IRRADIANCE_KEY, value);
	}

	/**
	 * Get the instantaneous diffuse irradiance.
	 * 
	 * @return the irradiance, in W/m2
	 */
	public BigDecimal getDiffuseIrradiance() {
		return getSampleBigDecimal(Instantaneous, DF_IRRADIANCE_KEY);
	}

	/**
	 * Set the instantaneous diffuse irradiance.
	 * 
	 * @param value
	 *        the irradiance to set, in W/m2
	 */
	public void setDiffuseIrradiance(BigDecimal value) {
		putSampleValue(Instantaneous, DF_IRRADIANCE_KEY, value);
	}

	/**
	 * Get the instantaneous direct normal irradiance.
	 * 
	 * @return the irradiance, in W/m2
	 */
	public BigDecimal getDirectNormalIrradiance() {
		return getSampleBigDecimal(Instantaneous, DN_IRRADIANCE_KEY);
	}

	/**
	 * Set the instantaneous direct normal irradiance.
	 * 
	 * @param value
	 *        the irradiance to set, in W/m2
	 */
	public void setDirectNormalIrradiance(BigDecimal value) {
		putSampleValue(Instantaneous, DN_IRRADIANCE_KEY, value);
	}

	/**
	 * Get the instantaneous other irradiance.
	 * 
	 * @return the irradiance, in W/m2
	 */
	public BigDecimal getOtherIrradiance() {
		return getSampleBigDecimal(Instantaneous, OTHER_IRRADIANCE_KEY);
	}

	/**
	 * Set the instantaneous other irradiance.
	 * 
	 * @param value
	 *        the irradiance to set, in W/m2
	 */
	public void setOtherIrradiance(BigDecimal value) {
		putSampleValue(Instantaneous, OTHER_IRRADIANCE_KEY, value);
	}

}
