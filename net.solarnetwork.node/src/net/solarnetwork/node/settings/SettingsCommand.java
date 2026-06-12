/* ==================================================================
 * SettingsCommand.java - Mar 18, 2012 3:37:02 PM
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

package net.solarnetwork.node.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Edit settings command object.
 *
 * @author matt
 * @version 1.3
 */
public class SettingsCommand implements SettingsUpdates, SettingsFilter {

	private @Nullable String providerKey;
	private @Nullable String instanceKey;
	private List<SettingValueBean> values;
	private boolean force;

	private final Iterable<Pattern> settingKeyPatternsToClean;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@code values} property will be created automatically.
	 * </p>
	 */
	public SettingsCommand() {
		this(null);
	}

	/**
	 * Construct with a "forced" setting.
	 *
	 * <p>
	 * This can be useful when configuring an instance from a factory, where the
	 * instance only has default settings (and thus no instance settings).
	 * </p>
	 *
	 * @param force
	 *        {@literal true} to force updating the setting
	 */
	public SettingsCommand(boolean force) {
		this(null);
		this.force = force;
	}

	/**
	 * Constructor.
	 *
	 * @param values
	 *        the values, or {@code null} to have a list created automatically
	 */
	public SettingsCommand(@Nullable List<SettingValueBean> values) {
		this(values, null);
	}

	/**
	 * Constructor.
	 *
	 * @param values
	 *        the values, or {@code null} to have a list created automatically
	 * @param settingKeyPatternsToClean
	 *        the key patterns to clean, or {@code null}
	 */
	public SettingsCommand(@Nullable List<SettingValueBean> values,
			@Nullable Iterable<Pattern> settingKeyPatternsToClean) {
		super();
		this.values = (values != null ? values : new ArrayList<>(8));
		this.settingKeyPatternsToClean = (settingKeyPatternsToClean != null ? settingKeyPatternsToClean
				: List.of());
	}

	@Override
	public boolean hasSettingKeyPatternsToClean() {
		if ( settingKeyPatternsToClean instanceof Collection<?> ) {
			return !((Collection<?>) settingKeyPatternsToClean).isEmpty();
		} else if ( settingKeyPatternsToClean != null ) {
			Iterator<Pattern> itr = settingKeyPatternsToClean.iterator();
			return itr.hasNext();
		}
		return false;
	}

	@Override
	public boolean hasSettingValueUpdates() {
		return (force || !values.isEmpty());
	}

	@Override
	public Iterable<Pattern> getSettingKeyPatternsToClean() {
		return settingKeyPatternsToClean;
	}

	@Override
	public Iterable<? extends Change> getSettingValueUpdates() {
		return getValues();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SettingsCommand{");
		if ( providerKey != null ) {
			builder.append("providerKey=");
			builder.append(providerKey);
			builder.append(", ");
		}
		if ( instanceKey != null ) {
			builder.append("instanceKey=");
			builder.append(instanceKey);
			builder.append(", ");
		}
		builder.append("force=");
		builder.append(force);
		builder.append(", hasSettingKeyPatternsToClean=");
		builder.append(hasSettingKeyPatternsToClean());
		builder.append(", hasSettingValueUpdates=");
		builder.append(hasSettingValueUpdates());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the values.
	 *
	 * @return the values
	 */
	public final List<SettingValueBean> getValues() {
		return values;
	}

	/**
	 * Set the values.
	 *
	 * @param values
	 *        the values to set
	 */
	public final void setValues(List<SettingValueBean> values) {
		this.values = (values != null ? values : List.of());
	}

	@Override
	public final @Nullable String getProviderKey() {
		return providerKey;
	}

	/**
	 * Set the provider key.
	 *
	 * @param providerKey
	 *        the provider key
	 */
	public final void setProviderKey(@Nullable String providerKey) {
		this.providerKey = providerKey;
	}

	@Override
	public final @Nullable String getInstanceKey() {
		return instanceKey;
	}

	/**
	 * Set the instance key.
	 *
	 * @param instanceKey
	 *        the instance key to set
	 */
	public final void setInstanceKey(@Nullable String instanceKey) {
		this.instanceKey = instanceKey;
	}

	/**
	 * Get the flag indicating if the update command should be forced, even if
	 * there are no setting values.
	 *
	 * @return {@literal true} if the update should be forced
	 * @since 1.1
	 */
	public final boolean isForce() {
		return force;
	}

	/**
	 * Set a flag indicating if the update command should be forced, even if
	 * there are no setting values.
	 *
	 * @param force
	 *        {@literal true} if the update should be forced
	 * @since 1.1
	 */
	public final void setForce(boolean force) {
		this.force = force;
	}

}
