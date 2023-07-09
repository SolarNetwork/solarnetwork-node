/* ==================================================================
 * MeteorologicalModelAccessorImpl.java - 10/07/2023 8:43:47 am
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

import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;

/**
 * Implementation of {@link MeteorologicalModelAccessor}.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class MeteorologicalModelAccessorImpl extends BaseModelAccessor
		implements MeteorologicalModelAccessor {

	/** The model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 11;

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public MeteorologicalModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
		super(data, baseAddress, modelId);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@link EnvironmentalModelId} class will be used as the
	 * {@code ModelId} instance.
	 * </p>
	 * 
	 * @param data
	 *        the overall data object
	 * @param baseAddress
	 *        the base address for this model's data
	 * @param modelId
	 *        the model ID
	 */
	public MeteorologicalModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, EnvironmentalModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public Float getAmbientTemperature() {
		Number n = getValue(MeteorologicalModelRegister.TemperatureAmbient);
		return (n != null ? n.floatValue() / 10f : null);
	}

	@Override
	public Integer getRelativeHumidity() {
		return getIntegerValue(MeteorologicalModelRegister.RelativeHumidity);
	}

	@Override
	public Integer getAtmosphericPressure() {
		Number n = getValue(MeteorologicalModelRegister.BarometricPressure);
		return (n != null ? n.intValue() * 100 : null);
	}

	@Override
	public Integer getWindSpeed() {
		return getIntegerValue(MeteorologicalModelRegister.WindSpeed);
	}

	@Override
	public Integer getWindDirection() {
		return getIntegerValue(MeteorologicalModelRegister.WindDirection);
	}

	@Override
	public Integer getRainAccumulation() {
		return getIntegerValue(MeteorologicalModelRegister.Rain);
	}

	@Override
	public Integer getSnowAccumulation() {
		return getIntegerValue(MeteorologicalModelRegister.Snow);
	}

	@Override
	public PrecipitationType getPrecipitationType() {
		Number n = getValue(MeteorologicalModelRegister.PrecipitationType);
		return (n != null ? CodedValue.forCodeValue(n.intValue(), PrecipitationType.class, null) : null);

	}

	@Override
	public Integer getElectricField() {
		return getIntegerValue(MeteorologicalModelRegister.ElectricField);
	}

	@Override
	public Integer getSurfaceWetness() {
		Number n = getValue(MeteorologicalModelRegister.SurfaceWetness);
		return (n != null ? n.intValue() * 1000 : null);
	}

	@Override
	public Integer getSoilMoisture() {
		return getIntegerValue(MeteorologicalModelRegister.SoilMoisture);
	}

}
