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

package net.solarnetwork.node.domain;

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

	@Override
	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	@Override
	public String getLocationName() {
		if ( sourceMetadata != null ) {
			return sourceMetadata.getMeta().getInfoString("name");
		}
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

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

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	@Override
	public GeneralDatumMetadata getMetadata() {
		return (sourceMetadata == null ? null : sourceMetadata.getMeta());
	}

	public GeneralLocationSourceMetadata getSourceMetadata() {
		return sourceMetadata;
	}

	public void setSourceMetadata(GeneralLocationSourceMetadata sourceMetadata) {
		this.sourceMetadata = sourceMetadata;
	}
}
