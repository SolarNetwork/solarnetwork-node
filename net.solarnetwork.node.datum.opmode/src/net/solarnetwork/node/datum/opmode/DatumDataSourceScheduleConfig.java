/* ==================================================================
 * DatumDataSourceScheduleConfig.java - 20/12/2018 1:35:45 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.opmode;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.Identifiable;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicCronExpressionSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a {@link DatumDataSource} schedule filter.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumDataSourceScheduleConfig {

	/** Default value for the {@code schedule} property. */
	public static final String DEFAULT_SCHEDULE = "0/15 * * * * ?";

	private String uid;
	private String groupUID;
	private String datumType;
	private String schedule = DEFAULT_SCHEDULE;

	/**
	 * Get a list of setting specifiers suitable for configuring instances of
	 * this class.
	 * 
	 * @param prefix
	 *        a prefix to use for all setting keys
	 * @return the list of settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "uid", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "groupUID", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "datumType", ""));
		results.add(new BasicCronExpressionSettingSpecifier(prefix + "schedule", DEFAULT_SCHEDULE));

		return results;
	}

	private boolean matchesIdentifiable(Identifiable identifiable) {
		if ( uid != null && !uid.isEmpty() && !uid.equalsIgnoreCase(identifiable.getUID()) ) {
			return false;
		}
		if ( groupUID != null && !groupUID.isEmpty()
				&& !groupUID.equalsIgnoreCase(identifiable.getGroupUID()) ) {
			return false;
		}
		return true;
	}

	private boolean matchesDatumType(Class<?> dataSourceDatumType) {
		if ( datumType != null && !datumType.isEmpty() ) {
			if ( dataSourceDatumType == null ) {
				return false;
			}
			// first check if an exact class name match
			if ( !datumType.equalsIgnoreCase(dataSourceDatumType.getName()) ) {
				// see if perhaps instanceof can work
				try {
					Class<?> datumTypeClass = dataSourceDatumType.getClassLoader().loadClass(datumType);
					if ( !datumTypeClass.isAssignableFrom(dataSourceDatumType) ) {
						return false;
					}
				} catch ( ClassNotFoundException e ) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Test if a {@link DatumDataSource} matches this configuration.
	 * 
	 * @param dataSource
	 *        the DatumDataSource to test
	 * @return {@literal true} if the configuration in this instance matches
	 *         {@code dataSource}
	 */
	public boolean matches(DatumDataSource<? extends Datum> dataSource) {
		if ( dataSource == null ) {
			return false;
		}
		if ( !matchesIdentifiable(dataSource) ) {
			return false;
		}
		return matchesDatumType(dataSource.getDatumType());
	}

	/**
	 * Test if a {@link MultiDatumDataSource} matches this configuration.
	 * 
	 * @param dataSource
	 *        the MultiDatumDataSource to test
	 * @return {@literal true} if the configuration in this instance matches
	 *         {@code dataSource}
	 */
	public boolean matches(MultiDatumDataSource<? extends Datum> dataSource) {
		if ( dataSource == null ) {
			return false;
		}
		if ( !matchesIdentifiable(dataSource) ) {
			return false;
		}
		return matchesDatumType(dataSource.getMultiDatumType());
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public String getDatumType() {
		return datumType;
	}

	public void setDatumType(String datumType) {
		this.datumType = datumType;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

}
