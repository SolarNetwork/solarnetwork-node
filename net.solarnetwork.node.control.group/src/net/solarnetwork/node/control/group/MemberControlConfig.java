/* ==================================================================
 * MemberControlConfig.java - 4 Nov 2026 6:28:41 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Configuration for a member of a control group.
 *
 * <p>
 * The {@code parameters} map provides a way to pass parameters to the
 * {@link ControlGroupCoordinationStrategy} at runtime.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class MemberControlConfig {

	/**
	 * A parameter name representing a rated capacity of a device.
	 *
	 * <p>
	 * The associated value will be interpreted as a number such that percentage
	 * operations can be applied against it to resolve an absolute value. For
	 * example a coordination strategy that needs to translate a percentage set
	 * point into an absolute value and vice-versa could use this value to
	 * perform those calculations.
	 * </p>
	 */
	public static final String RATED_CAPACITY_PARAM = "ratedCapacity";

	private @Nullable String controlId;
	private @Nullable Map<String, String> parameters;
	private boolean disabled;

	/**
	 * Constructor.
	 */
	public MemberControlConfig() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param controlId
	 *        the control ID to configure
	 */
	public MemberControlConfig(@Nullable String controlId) {
		super();
		this.controlId = controlId;
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@code null}
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(2);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "controlId", null));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "parametersValue", null));
		results.add(new BasicToggleSettingSpecifier(prefix + "disabled", Boolean.FALSE));

		return results;
	}

	/**
	 * Test if this configuration is valid.
	 *
	 * <p>
	 * This tests that the {@code disabled} is {@code false} and
	 * {@code controlId} is not blank.
	 * </p>
	 *
	 * @return {@code true} if this configuration is valid
	 */
	public boolean isValid() {
		final String controlId = getControlId();
		return (!disabled && controlId != null && !controlId.isBlank());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MemberControlConfig{");
		if ( controlId != null ) {
			builder.append("controlId=");
			builder.append(controlId);
			builder.append(", ");
		}
		if ( parameters != null ) {
			builder.append("parameters=");
			builder.append(parameters);
		}
		if ( disabled ) {
			builder.append("disabled");
			builder.append(", ");
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the member control ID.
	 *
	 * @return the control ID
	 */
	public final @Nullable String getControlId() {
		return controlId;
	}

	/**
	 * Set the member control ID.
	 *
	 * @param controlId
	 *        the control ID to set
	 */
	public final void setControlId(@Nullable String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the control ID, assumed non-null.
	 *
	 * <p>
	 * This method is designed to be used after {@link #isValid()} returns
	 * {@code true}.
	 * </p>
	 *
	 * @return the control ID
	 * @throws IllegalStateException
	 *         if the {@code controlId} is actually {@code null}
	 */
	public final String controlId() {
		return ObjectUtils.nonnull(controlId, "Control ID");
	}

	/**
	 * Create a copy of this configuration with a different control ID.
	 *
	 * <p>
	 * The copy shares the {@code parameters} map of this configuration.
	 * </p>
	 *
	 * @param controlId
	 *        the control ID to use
	 * @return the new configuration
	 */
	public MemberControlConfig withControlId(@Nullable String controlId) {
		final MemberControlConfig result = new MemberControlConfig(controlId);
		result.setParameters(parameters);
		result.setDisabled(disabled);
		return result;
	}

	/**
	 * Get the optional parameters.
	 *
	 * @return the parameters
	 */
	public final @Nullable Map<String, String> getParameters() {
		return parameters;
	}

	/**
	 * Set the optional parameters.
	 *
	 * @param parameters
	 *        the parameters to set
	 */
	public final void setParameters(@Nullable Map<String, String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Get the optional parameters, as a delimited string.
	 *
	 * @return the parameters as a delimited string
	 * @see StringUtils#delimitedStringFromMap(Map)
	 */
	public final @Nullable String getParametersValue() {
		return StringUtils.delimitedStringFromMap(parameters);
	}

	/**
	 * Set the optional parameters as a delimited string.
	 *
	 * @param value
	 *        the delimited string to set
	 * @see StringUtils#commaDelimitedStringToMap(String)
	 */
	public final void setParametersValue(@Nullable String value) {
		setParameters(StringUtils.commaDelimitedStringToMap(value));
	}

	/**
	 * Get the disabled flag.
	 *
	 * @return the disabled flag
	 */
	public final boolean isDisabled() {
		return disabled;
	}

	/**
	 * Set the disabled flag.
	 *
	 * @param disabled
	 *        the disabled to set
	 */
	public final void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

}
