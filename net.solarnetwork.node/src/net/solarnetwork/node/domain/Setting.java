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
 */

package net.solarnetwork.node.domain;

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
 * be {@literal null}, but empty strings are allowed.
 * </p>
 *
 * @author matt
 * @version 2.1
 */
public class Setting implements SettingNote {

	/**
	 * An enumeration of setting flags.
	 */
	public enum SettingFlag {

		/** Ignore the modification date. */
		IgnoreModificationDate(0),

		/** The setting is "volatile" and should not be persisted. */
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
		 * @return the set (never {@literal null})
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
	private String note;
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
		this(key, type, value, null, flags);
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
	 * @param note
	 *        the note
	 * @param flags
	 *        the falgs
	 * @since 2.1
	 */
	public Setting(String key, String type, String value, String note, Set<SettingFlag> flags) {
		super();
		this.key = key;
		this.type = type;
		this.value = value;
		this.note = note;
		this.flags = flags;
	}

	/**
	 * Create a {@link SettingNote} instance.
	 *
	 * @param key
	 *        the key
	 * @param type
	 *        the type
	 * @param note
	 *        the note
	 * @return the note instance
	 * @since 2.1
	 */
	public static SettingNote note(String key, String type, String note) {
		return new Setting(key, type, null, note, null);
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

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	@Override
	public String getKey() {
		return key;
	}

	/**
	 * Set the key.
	 *
	 * @param key
	 *        the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the type.
	 *
	 * @return the type
	 */
	@Override
	public String getType() {
		return type;
	}

	/**
	 * Set the type.
	 *
	 * @param type
	 *        the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the value.
	 *
	 * @param value
	 *        the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Get the note.
	 *
	 * @return the note
	 * @since 2.1
	 */
	@Override
	public final String getNote() {
		return note;
	}

	/**
	 * Set the note.
	 *
	 * @param note
	 *        the note to set
	 * @since 2.1
	 */
	public final void setNote(String note) {
		this.note = note;
	}

	/**
	 * Get the flags.
	 *
	 * @return the flags
	 */
	public Set<SettingFlag> getFlags() {
		return flags;
	}

	/**
	 * Set the flags.
	 *
	 * @param flags
	 *        the flags to set
	 */
	public void setFlags(Set<SettingFlag> flags) {
		this.flags = flags;
	}

	/**
	 * Get the modification date.
	 *
	 * @return the date
	 */
	public Date getModified() {
		return modified;
	}

	/**
	 * Set the modification date.
	 *
	 * @param modified
	 *        the date to set
	 */
	public void setModified(Date modified) {
		this.modified = modified;
	}

}
