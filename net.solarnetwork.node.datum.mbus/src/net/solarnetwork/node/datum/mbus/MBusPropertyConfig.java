/* ==================================================================
 * MBusPropertyConfig.java - 09/07/2020 10:43:58 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.mbus;

import java.math.BigDecimal;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.io.mbus.MBusDataDescription;
import net.solarnetwork.node.io.mbus.MBusDataType;

/**
 * Configuration for a single datum property to be set via M-Bus.
 * 
 * <p>
 * The {@link #getConfig()} value represents the mbus address to read from.
 * </p>
 * 
 * @author alex
 * @version 1.0
 */
public class MBusPropertyConfig extends GeneralDatumSamplePropertyConfig<MBusDataDescription> {

	private MBusDataType dataType;
	private BigDecimal unitMultiplier;
	private int decimalScale;

	/**
	 * Default constructor.
	 */
	public MBusPropertyConfig() {
		super(null, GeneralDatumSamplesType.Instantaneous, MBusDataDescription.NotSupported);
		dataType = MBusDataType.None;
		unitMultiplier = BigDecimal.ONE;
		decimalScale = 0;
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param dataType
	 *        the mbus data type
	 * @param description
	 *        the mbus data description
	 */
	public MBusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType,
			MBusDataType dataType, MBusDataDescription dataDescription) {
		this(name, datumPropertyType, dataType, dataDescription, BigDecimal.ONE, 0);
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param datumPropertyType
	 *        the datum property type
	 * @param dataType
	 *        the mbus data type
	 * @param description
	 *        the mbus data description
	 * @param unitMultiplier
	 *        the unit multiplier
	 * @param decimalScale
	 *        for numbers, the maximum decimal scale to support, or
	 *        {@literal -1} for no limit
	 */
	public MBusPropertyConfig(String name, GeneralDatumSamplesType datumPropertyType,
			MBusDataType dataType, MBusDataDescription dataDescription, BigDecimal unitMultiplier,
			int decimalScale) {
		super(name, datumPropertyType, dataDescription);
		this.dataType = dataType;
		this.unitMultiplier = unitMultiplier;
		this.decimalScale = decimalScale;
	}

	/**
	 * Get the data type.
	 * 
	 * @return the type
	 */
	public MBusDataType getDataType() {
		return dataType;
	}

	/**
	 * Set the data type.
	 * 
	 * @param dataType
	 *        the type to set
	 */
	public void setDataType(MBusDataType dataType) {
		if ( dataType == null ) {
			return;
		}
		this.dataType = dataType;
	}

	/**
	 * Get the description to read data for.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}, returning
	 * {@literal MBusDataDescription.NotSupported} if that returns
	 * {@literal null}.
	 * </p>
	 * 
	 * @return the data description
	 */
	public MBusDataDescription getDataDescription() {
		MBusDataDescription desc = getConfig();
		return (desc != null ? desc : MBusDataDescription.NotSupported);
	}

	/**
	 * Set the data description to read for.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(MBusDataDescription)}.
	 * </p>
	 * 
	 * @param desc
	 *        the data description to set
	 */
	public void setAddress(MBusDataDescription desc) {
		setConfig(desc);
	}

	/**
	 * Get the unit multiplier.
	 * 
	 * @return the multiplier
	 */
	public BigDecimal getUnitMultiplier() {
		return unitMultiplier;
	}

	/**
	 * Set the unit multiplier.
	 * 
	 * <p>
	 * This value represents a multiplication factor to apply to values
	 * collected for this property so that a standardized unit is captured. For
	 * example, a power meter might report power as <i>killowatts</i>, in which
	 * case {@code multiplier} can be configured as {@literal .001} to convert
	 * the value to <i>watts</i>.
	 * </p>
	 * 
	 * @param unitMultiplier
	 *        the mutliplier to set
	 */
	public void setUnitMultiplier(BigDecimal unitMultiplier) {
		this.unitMultiplier = unitMultiplier;
	}

	/**
	 * Get the decimal scale to round decimal numbers to.
	 * 
	 * @return the decimal scale
	 */
	public int getDecimalScale() {
		return decimalScale;
	}

	/**
	 * Set the decimal scale to round decimal numbers to.
	 * 
	 * <p>
	 * This is a <i>maximum</i> scale value that decimal values should be
	 * rounded to. This is applied <i>after</i> any {@code unitMultiplier} is
	 * applied. A scale of {@literal 0} would round all decimals to integer
	 * values.
	 * </p>
	 * 
	 * @param decimalScale
	 *        the scale to set, or {@literal -1} to disable rounding completely
	 */
	public void setDecimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
	}
}
