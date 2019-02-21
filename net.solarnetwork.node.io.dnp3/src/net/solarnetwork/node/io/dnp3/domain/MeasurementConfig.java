/* ==================================================================
 * MeasurementConfig.java - 21/02/2019 4:39:11 pm
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

package net.solarnetwork.node.io.dnp3.domain;

import net.solarnetwork.node.DatumDataSource;

/**
 * A configuration for a DNP3 measurement integration with a
 * {@link net.solarnetwork.node.DatumDataSource} property.
 * 
 * <p>
 * This configuration maps a datum property to a DNP3 measurement.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class MeasurementConfig {

	private String dataSourceUid;
	private String sourceId;
	private String propertyName;
	private MeasurementType type;

	/**
	 * Default constructor.
	 */
	public MeasurementConfig() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param dataSourceUid
	 *        the {@link DatumDataSource#getUID()} to collect from
	 * @param sourceId
	 *        the source ID a
	 *        {@link net.solarnetwork.node.domain.Datum#getSourceId()} to
	 *        collect from
	 * @param propertyName
	 *        the datum property name to collect
	 * @param type
	 *        the DNP3 measurement type
	 */
	public MeasurementConfig(String dataSourceUid, String sourceId, String propertyName,
			MeasurementType type) {
		super();
		this.dataSourceUid = dataSourceUid;
		this.sourceId = sourceId;
		this.propertyName = propertyName;
		this.type = type;
	}

	public String getDataSourceUid() {
		return dataSourceUid;
	}

	public void setDataSourceUid(String dataSourceUid) {
		this.dataSourceUid = dataSourceUid;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public MeasurementType getType() {
		return type;
	}

	public void setType(MeasurementType type) {
		this.type = type;
	}

}
