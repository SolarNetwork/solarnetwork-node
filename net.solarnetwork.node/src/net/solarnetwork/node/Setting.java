/* ==================================================================
 * Setting.java - Nov 5, 2012 11:05:11 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * An application setting object.
 * 
 * <p>
 * A setting is based on a {@code key} and {@code type}, the combination of
 * which forms a unique key for its associated value. No values are allowed to
 * be <em>null</em>, but empty strings are allowed.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class Setting {

	public enum SettingFlag {
		IgnoreModificationDate(0),

		Volatile(1);

		private final int key;

		private SettingFlag(int key) {
			this.key = key;
		}

		/**
		 * Get the key value.
		 * 
		 * @return the key
		 */
		public int getKey() {
			return key;
		}

		/**
		 * Get a SettingFlag for a given key.
		 * 
		 * @param key
		 *        the key
		 * @return the flag
		 * @throws IllegalArgumentException
		 *         if the key is not valid
		 */
		public static SettingFlag forKey(int key) {
			switch (key) {
				case 0:
					return IgnoreModificationDate;

				case 1:
					return Volatile;

				default:
					throw new IllegalArgumentException(key + " is not a valid key");
			}
		}

		/**
		 * Create a set of SettingFlag from a mask.
		 * 
		 * @param mask
		 *        the mask
		 * @return the set (never <em>null</em>)
		 */
		public static Set<SettingFlag> setForMask(int mask) {
			Set<SettingFlag> maskSet = new HashSet<Setting.SettingFlag>();
			for ( SettingFlag flag : EnumSet.allOf(SettingFlag.class) ) {
				if ( ((mask >> flag.getKey()) & 1) == 1 ) {
					maskSet.add(flag);
				}
			}
			if ( maskSet.size() < 1 ) {
				return Collections.emptySet();
			}
			return EnumSet.copyOf(maskSet);
		}

		/**
		 * Get a mask from a set of SettingFlag.
		 * 
		 * @param set
		 *        the set
		 * @return the mask
		 */
		public static int maskForSet(Set<SettingFlag> set) {
			int mask = 0;
			if ( set != null ) {
				for ( SettingFlag flag : set ) {
					mask |= (1 << flag.getKey());
				}
			}
			return mask;
		}
	}

	private String key;
	private String type;
	private String value;
	private Date modified;
	private Set<SettingFlag> flags;

	/**
	 * Default constructor.
	 */
	public Setting() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param key
	 *        the key
	 * @param type
	 *        the type
	 * @param value
	 *        the value
	 * @param flags
	 *        the falgs
	 */
	public Setting(String key, String type, String value, Set<SettingFlag> flags) {
		super();
		this.key = key;
		this.type = type;
		this.value = value;
		this.flags = flags;
	}

	@Override
	public String toString() {
		return "Setting{key=" + key + ",type=" + type + ",flags=" + flags + '}';
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		Setting other = (Setting) obj;
		if ( key == null ) {
			if ( other.key != null )
				return false;
		} else if ( !key.equals(other.key) )
			return false;
		if ( type == null ) {
			if ( other.type != null )
				return false;
		} else if ( !type.equals(other.type) )
			return false;
		return true;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Set<SettingFlag> getFlags() {
		return flags;
	}

	public void setFlags(Set<SettingFlag> flags) {
		this.flags = flags;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

}
