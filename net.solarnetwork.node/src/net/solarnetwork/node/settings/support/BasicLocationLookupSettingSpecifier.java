/* ==================================================================
 * BasicLocationLookupSettingSpecifier.java - Nov 19, 2013 1:12:41 PM
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

package net.solarnetwork.node.settings.support;

import net.solarnetwork.node.domain.Location;
import net.solarnetwork.node.settings.LocationLookupSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;

/**
 * Basic implementation of {@link LocationLookupSettingSpecifier}.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicLocationLookupSettingSpecifier extends BaseKeyedSettingSpecifier<Long> implements
		LocationLookupSettingSpecifier {

	private final Location location;
	private final Class<? extends Location> locationType;

	/**
	 * Construct with a key and default value.
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 */
	public BasicLocationLookupSettingSpecifier(String key, Class<? extends Location> locationType,
			Location location) {
		super(key, (location == null ? null : location.getLocationId()));
		this.locationType = locationType;
		this.location = location;
	}

	@Override
	public SettingSpecifier mappedWithPlaceholer(String template) {
		BasicLocationLookupSettingSpecifier spec = new BasicLocationLookupSettingSpecifier(
				String.format(template, getKey()), locationType, location);
		spec.setTitle(getTitle());
		return spec;
	}

	@Override
	public SettingSpecifier mappedWithMapper(Mapper mapper) {
		BasicLocationLookupSettingSpecifier spec = new BasicLocationLookupSettingSpecifier(
				mapper.mapKey(getKey()), locationType, location);
		spec.setTitle(getTitle());
		return spec;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public Class<? extends Location> getLocationType() {
		return locationType;
	}

	@Override
	public Long getLocationId() {
		return (location == null ? null : location.getLocationId());
	}

	@Override
	public String getLocationName() {
		return (location == null ? null : location.getLocationName());
	}

	@Override
	public Long getSourceId() {
		return (location == null ? null : location.getSourceId());
	}

	@Override
	public String getSourceName() {
		return (location == null ? null : location.getSourceName());
	}

	@Override
	public String getLocationTypeKey() {
		String t = (locationType == null ? "basic" : locationType.getSimpleName());
		if ( t.endsWith("Location") ) {
			t = t.substring(0, t.length() - 8);
		}
		t = t.toLowerCase();
		return t;
	}

}
