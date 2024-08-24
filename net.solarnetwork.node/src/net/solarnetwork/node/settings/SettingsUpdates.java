/* ==================================================================
 * SettingsUpdates.java - 19/09/2019 6:41:53 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.settings;

import java.util.regex.Pattern;

/**
 * API for a set of setting updates to be applied.
 *
 * @author matt
 * @version 1.1
 * @since 1.70
 */
public interface SettingsUpdates {

	/**
	 * A single setting update to apply.
	 */
	interface Change {

		/**
		 * Get the setting provider ID this change is for.
		 *
		 * @return the setting provider key
		 */
		String getProviderKey();

		/**
		 * Get the setting instance ID of the setting provider factory this is
		 * for, or {@literal null} if the provider is not a factory
		 *
		 * @return the setting instance key, or {@literal null}
		 */
		String getInstanceKey();

		/**
		 * Get the setting key.
		 *
		 * @return the key
		 */
		String getKey();

		/**
		 * Get the setting value.
		 *
		 * @return the value
		 */
		String getValue();

		/**
		 * Get the transient flag.
		 *
		 * @return the transient flag
		 */
		boolean isTransient();

		/**
		 * Get the remove flag.
		 *
		 * @return {@literal true} if this setting should be deleted
		 */
		boolean isRemove();

		/**
		 * Get a brief note to associate with the setting.
		 *
		 * @return the note, or {@literal null}
		 * @since 1.1
		 */
		String getNote();
	}

	/**
	 * Test if there are any setting patterns to clean.
	 *
	 * @return {@literal false} only if {@link #getSettingKeyPatternsToClean()}
	 *         is known not to contain any patterns
	 */
	boolean hasSettingKeyPatternsToClean();

	/**
	 * Get a collection of setting key patterns to clean before apply any
	 * setting value updates.
	 *
	 * @return the patterns, never {@literal null}
	 */
	Iterable<Pattern> getSettingKeyPatternsToClean();

	/**
	 * Test if there are any updates to apply.
	 *
	 * @return {@literal false} only if {@link #getSettingValueUpdates()} is
	 *         known not to contain any patterns
	 */
	boolean hasSettingValueUpdates();

	/**
	 * Get a collection of setting values to apply.
	 *
	 * @return the setting values, never {@literal null}
	 */
	Iterable<? extends Change> getSettingValueUpdates();

}
