/* ==================================================================
 * NodeControlInfoDatum.java - Oct 1, 2011 7:04:43 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.domain;

import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.Datum;

/**
 * Implementation of {@link NodeControlInfo} and {@link Datum}.
 * 
 * @author matt
 * @version $Revision$
 */
public class NodeControlInfoDatum extends BaseDatum implements NodeControlInfo {

	private NodeControlPropertyType type;
	private String value;
	private Boolean readonly;
	private String unit;
	private String propertyName;
	
	@Override
	public String getControlId() {
		return getSourceId();
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public NodeControlPropertyType getType() {
		return type;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public Boolean getReadonly() {
		return readonly;
	}

	@Override
	public String getUnit() {
		return unit;
	}
	
	@Override
	public String toString() {
		return "NodeControlInfoDatum{controlId=" +(getSourceId() == null ? "" : getSourceId()) 
				+",type=" +(type == null ? "" : type.toString())
				+",property=" +(propertyName == null ? "" : propertyName)
				+",value=" +(value == null ? "" : value)
				+'}';
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((propertyName == null) ? 0 : propertyName.hashCode());
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		NodeControlInfoDatum other = (NodeControlInfoDatum) obj;
		if (propertyName == null) {
			if (other.propertyName != null) {
				return false;
			}
		} else if (!propertyName.equals(other.propertyName)) {
			return false;
		}
		if (unit == null) {
			if (other.unit != null) {
				return false;
			}
		} else if (!unit.equals(other.unit)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

	public void setType(NodeControlPropertyType type) {
		this.type = type;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

}
