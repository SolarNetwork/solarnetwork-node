/* ==================================================================
 * DatumDaoStat.java - 17/06/2021 1:54:19 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.general;

import net.solarnetwork.util.StatCounter.Stat;

/**
 * Stats for MQTT message persistence.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public enum DatumDaoStat implements Stat {

	DatumStored(0, "datum stored"),

	DatumUploaded(1, "datum uploaded"),

	DatumDeleted(2, "datum deleted"),

	;

	private final int index;
	private final String description;

	private DatumDaoStat(int index, String description) {
		this.index = index;
		this.description = description;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
