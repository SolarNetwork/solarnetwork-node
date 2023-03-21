/* ==================================================================
 * SimpleDatumLocation.java - Nov 17, 2013 7:37:13 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.datum;

import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.GeneralLocationSourceMetadata;

/**
 * Basic implementation of {@link DatumLocation}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleDatumLocation implements DatumLocation {

	private Long locationId;
	private String locationName;
	private String sourceId;
	private String sourceName;
	private GeneralLocationSourceMetadata sourceMetadata;

	/**
	 * Default constructor.
	 */
	public SimpleDatumLocation() {
		super();
	}

	@Override
	public Long getLocationId() {
		return locationId;
	}

	/**
	 * Set the location ID.
	 * 
	 * @param locationId
	 *        the ID to set
	 */
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	@Override
	public String getLocationName() {
		if ( sourceMetadata != null ) {
			GeneralDatumMetadata meta = sourceMetadata.getMeta();
			if ( meta != null ) {
				return meta.getInfoString("name");
			}
		}
		return locationName;
	}

	/**
	 * Set the location name.
	 * 
	 * @param locationName
	 *        the location name to set
	 */
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 * 
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	@Override
	public String getSourceName() {
		if ( sourceMetadata != null ) {
			return getSourceId();
		}
		return sourceName;
	}

	/**
	 * Set the source name.
	 * 
	 * @param sourceName
	 *        the name to set
	 */
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	@Override
	public GeneralDatumMetadata getMetadata() {
		return (sourceMetadata == null ? null : sourceMetadata.getMeta());
	}

	/**
	 * Get the source metadata.
	 * 
	 * @return the metadata
	 */
	public GeneralLocationSourceMetadata getSourceMetadata() {
		return sourceMetadata;
	}

	/**
	 * Set the source metadata.
	 * 
	 * @param sourceMetadata
	 *        the metadata to set
	 */
	public void setSourceMetadata(GeneralLocationSourceMetadata sourceMetadata) {
		this.sourceMetadata = sourceMetadata;
	}
}
