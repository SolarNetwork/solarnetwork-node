/* ==================================================================
 * BasicGeneralLocation.java - Oct 21, 2014 2:48:57 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralLocationSourceMetadata;

/**
 * Basic implementation of {@link GeneralLocation}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicGeneralLocation extends BasicLocation implements GeneralLocation {

	private GeneralLocationSourceMetadata sourceMetadata;

	@Override
	public GeneralDatumMetadata getMetadata() {
		return (sourceMetadata == null ? null : sourceMetadata.getMeta());
	}

	@Override
	public String getLocationName() {
		if ( sourceMetadata != null ) {
			return sourceMetadata.getMeta().getInfoString("name");
		}
		return super.getLocationName();
	}

	@Override
	public String getSourceName() {
		if ( sourceMetadata != null ) {
			return getSourceId();
		}
		return super.getSourceName();
	}

	public GeneralLocationSourceMetadata getSourceMetadata() {
		return sourceMetadata;
	}

	public void setSourceMetadata(GeneralLocationSourceMetadata sourceMetadata) {
		this.sourceMetadata = sourceMetadata;
	}

}
