/* ==================================================================
 * GpsModelAccessorImpl.java - 8/07/2023 9:38:58 am
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import net.solarnetwork.node.hw.sunspec.BaseModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelId;
import net.solarnetwork.util.NumberUtils;

/**
 * Implementation of {@link GpsModelAccessor}.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class GpsModelAccessorImpl extends BaseModelAccessor implements GpsModelAccessor {

	/** The GPS model fixed block length. */
	public static final int FIXED_BLOCK_LENGTH = 36;

	private static final int COORDINATES_SCALE = -7;

	/** The GPS date + time pattern. */
	public static final DateTimeFormatter GPS_TIMESTAMP_FORMATTER = DateTimeFormatter
			.ofPattern("yyyyMMddHHmmss.SSS'Z'").withZone(ZoneOffset.UTC)
			.withChronology(IsoChronology.INSTANCE);

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
	public GpsModelAccessorImpl(ModelData data, int baseAddress, ModelId modelId) {
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
	public GpsModelAccessorImpl(ModelData data, int baseAddress, int modelId) {
		this(data, baseAddress, EnvironmentalModelId.forId(modelId));
	}

	@Override
	public int getFixedBlockLength() {
		return FIXED_BLOCK_LENGTH;
	}

	@Override
	public Instant getGpsTimestamp() {
		String time = getData().getLatin1String(GpsModelRegister.Time, getBlockAddress(), true);
		String date = getData().getLatin1String(GpsModelRegister.Date, getBlockAddress(), true);
		try {
			return GPS_TIMESTAMP_FORMATTER.parse(date + time, Instant::from);
		} catch ( DateTimeParseException e ) {
			// ignore
		}
		return null;
	}

	@Override
	public String getLocationName() {
		return getData().getLatin1String(GpsModelRegister.Location, getBlockAddress(), true);
	}

	@Override
	public BigDecimal getLatitude() {
		return NumberUtils.scaled(getIntegerValue(GpsModelRegister.Latitude, getBlockAddress()),
				COORDINATES_SCALE);
	}

	@Override
	public BigDecimal getLongitude() {
		return NumberUtils.scaled(getIntegerValue(GpsModelRegister.Longitude, getBlockAddress()),
				COORDINATES_SCALE);
	}

	@Override
	public Integer getAltitude() {
		return getIntegerValue(GpsModelRegister.Altitude, getBlockAddress());
	}

}
