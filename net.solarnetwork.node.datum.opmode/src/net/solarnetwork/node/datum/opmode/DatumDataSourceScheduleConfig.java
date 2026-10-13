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
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Configuration for a {@link DatumDataSource} schedule filter.
 *
 * @author matt
 * @version 2.0
 */
public class DatumDataSourceScheduleConfig {

	/** Default value for the {@code schedule} property. */
	public static final String DEFAULT_SCHEDULE = "0/15 * * * * ?";

	private @Nullable String uid;
	private @Nullable String groupUid;
	private @Nullable String datumType;
	private @Nullable String schedule = DEFAULT_SCHEDULE;
	private boolean persist;

	/**
	 * Constructor.
	 */
	public DatumDataSourceScheduleConfig() {
		super();
	}

	/**
	 * Get a list of setting specifiers suitable for configuring instances of
	 * this class.
	 *
	 * @param prefix
	 *        a prefix to use for all setting keys
	 * @return the list of settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier(prefix + "uid", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "groupUid", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "datumType", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "schedule", DEFAULT_SCHEDULE));
		results.add(new BasicToggleSettingSpecifier(prefix + "persist", Boolean.FALSE));

		return results;
	}

	private boolean matchesIdentifiable(Identifiable identifiable) {
		if ( uid != null && !uid.isEmpty() && !uid.equalsIgnoreCase(identifiable.getUid()) ) {
			return false;
		}
		if ( groupUid != null && !groupUid.isEmpty()
				&& !groupUid.equalsIgnoreCase(identifiable.getGroupUid()) ) {
			return false;
		}
		return true;
	}

	private boolean matchesDatumType(@Nullable Class<?> dataSourceDatumType) {
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
	public boolean matches(@Nullable DatumDataSource dataSource) {
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
	public boolean matches(@Nullable MultiDatumDataSource dataSource) {
		if ( dataSource == null ) {
			return false;
		}
		if ( !matchesIdentifiable(dataSource) ) {
			return false;
		}
		return matchesDatumType(dataSource.getMultiDatumType());
	}

	/**
	 * Get the UID.
	 *
	 * @return the UID
	 */
	public final @Nullable String getUid() {
		return uid;
	}

	/**
	 * Set the UID.
	 *
	 * @param uid
	 *        the UID to set
	 */
	public final void setUid(@Nullable String uid) {
		this.uid = uid;
	}

	/**
	 * Get the group UID.
	 *
	 * @return the group UID
	 */
	public final @Nullable String getGroupUid() {
		return groupUid;
	}

	/**
	 * Set the group UID.
	 *
	 * @param groupUid
	 *        the group UID to set
	 */
	public final void setGroupUid(@Nullable String groupUid) {
		this.groupUid = groupUid;
	}

	/**
	 * Get the datum type.
	 *
	 * @return the datum type
	 */
	public final @Nullable String getDatumType() {
		return datumType;
	}

	/**
	 * Set the datum type.
	 *
	 * @param datumType
	 *        the datum type to set
	 */
	public final void setDatumType(@Nullable String datumType) {
		this.datumType = datumType;
	}

	/**
	 * Get the schedule.
	 *
	 * @return the schedule
	 */
	public final @Nullable String getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule.
	 *
	 * @param schedule
	 *        the schedule to set
	 */
	public final void setSchedule(@Nullable String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the persist mode.
	 *
	 * @return {@literal true} if polled datum should be persisted
	 * @since 2.0
	 */
	public final boolean isPersist() {
		return persist;
	}

	/**
	 * Set the persist mode.
	 *
	 * @param persist
	 *        {@literal true} if polled datum should be persisted
	 * @since 2.0
	 */
	public final void setPersist(boolean persist) {
		this.persist = persist;
	}

}
