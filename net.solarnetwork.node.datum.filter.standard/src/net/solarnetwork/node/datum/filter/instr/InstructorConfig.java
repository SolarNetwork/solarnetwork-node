/* ==================================================================
 * InstructorConfig.java - 9/07/2026 12:06:48 pm
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

package net.solarnetwork.node.datum.filter.instr;

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.support.ExpressionConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Configuration for a conditional instruction.
 *
 * @author matt
 * @version 1.0
 * @since 4.5
 */
public class InstructorConfig {

	/**
	 * A setting type pattern for a property configuration element.
	 *
	 * <p>
	 * The pattern has two capture groups: the property configuration index and
	 * the property setting name.
	 * </p>
	 */
	public static final Pattern INSTRUCTOR_SETTING_PATTERN = Pattern
			.compile("instructorConfigs\\[(\\d+)\\]\\.(.*)");

	private final ExpressionConfiguration predicate;
	private InstructionConfig @Nullable [] instructions;

	/**
	 * Constructor.
	 */
	public InstructorConfig() {
		super();
		this.predicate = new ExpressionConfiguration();
	}

	/**
	 * Create a "template" instance for settings.
	 *
	 * @return the template instance
	 */
	public static InstructorConfig template() {
		var result = new InstructorConfig();
		result.instructions = new InstructionConfig[] { InstructionConfig.template() };
		return result;
	}

	/**
	 * Test if this configuration is valid for evaluation.
	 *
	 * @param expressionServices
	 *        the available expression services
	 * @return {@code true} if a predicate expression and some valid instruction
	 *         configuration are available
	 */
	public boolean isValid(@Nullable Iterable<ExpressionService> expressionServices) {
		boolean someInstructionIsValid = false;
		final InstructionConfig[] instructionConfigs = this.instructions;
		if ( instructionConfigs != null && instructionConfigs.length > 0 ) {
			for ( InstructionConfig instructionConfig : instructionConfigs ) {
				if ( instructionConfig != null && instructionConfig.isValid() ) {
					someInstructionIsValid = true;
					break;
				}
			}
		}
		return (someInstructionIsValid && predicate.expression(expressionServices) != null);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 *
	 * @param template
	 *        {@code true} if template mode is desired
	 * @param prefix
	 *        a setting key prefix to use
	 * @param expressionServices
	 *        the available expression services
	 * @return the settings, never {@code null}
	 */
	public List<SettingSpecifier> settings(final boolean template, final String prefix,
			final Iterable<ExpressionService> expressionServices) {
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.addAll(ExpressionConfiguration.settings(InstructorConfig.class, prefix + "predicate.",
				expressionServices));

		// instructions list
		final InstructionConfig[] instrs = getInstructions();
		final List<InstructionConfig> instrsList = (template ? List.of(InstructionConfig.template())
				: (instrs != null ? List.of(instrs) : List.of()));
		result.add(SettingUtils.dynamicListSettingSpecifier(prefix + "instructions", instrsList,
				new SettingUtils.KeyedListCallback<>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							@Nullable InstructionConfig value, int index, String key) {
						return List.of(new BasicGroupSettingSpecifier(
								nonnull(value, "Instruction configuration").settings(template, key + ".",
										expressionServices)));
					}
				}));

		return result;
	}

	/**
	 * Populate a setting as a property configuration value, if possible.
	 *
	 * @param config
	 *        the filter configuration
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as a property
	 *         configuration value
	 */
	public static boolean populateFromSetting(InstructorDatumFilterServiceConfig config,
			Setting setting) {
		if ( "instructorConfigsCount".equals(setting.getType()) ) {
			// return true so treated as handled by caller
			return true;
		}
		final Matcher m = INSTRUCTOR_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		final int idx = Integer.parseInt(m.group(1));
		final String name = m.group(2);
		final List<InstructorConfig> instructorConfigs = config.getInstructorConfigs();
		if ( !(idx < instructorConfigs.size()) ) {
			instructorConfigs.add(idx, new InstructorConfig());
		}
		final InstructorConfig instructorConfig = instructorConfigs.get(idx);

		if ( InstructionConfig.populateFromSetting(instructorConfig, setting) ) {
			return true;
		}

		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "predicate.expression":
					instructorConfig.predicate.setExpression(val);
					break;
				case "predicate.expressionServiceId":
					instructorConfig.predicate.setExpressionServiceId(val);
					break;
				default:
					// ignore
			}
		}
		return true;
	}

	/**
	 * Generate a list of setting values.
	 *
	 * @param providerId
	 *        the setting provider ID
	 * @param instanceId
	 *        the factory instance ID
	 * @param instructorIdx
	 *        the instructor index
	 * @return the settings
	 */
	public List<SettingValueBean> toSettingValues(final String providerId,
			final @Nullable String instanceId, final int instructorIdx) {
		List<SettingValueBean> settings = new ArrayList<>(8);
		addSetting(settings, providerId, instanceId, instructorIdx, "predicate.expression",
				predicate.getExpression());
		addSetting(settings, providerId, instanceId, instructorIdx, "predicate.expressionServiceId",
				predicate.getExpressionServiceId());

		if ( instructions != null ) {
			int i = 0;
			for ( InstructionConfig instructionConfig : instructions ) {
				settings.addAll(
						instructionConfig.toSettingValues(providerId, instanceId, instructorIdx, i++));
			}
		}

		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId,
			@Nullable String instanceId, int i, String key, @Nullable Object val) {
		if ( val == null ) {
			return;
		}
		String fullKey = format("instructorConfigs[%d].%s", i, key);
		SettingValueBean.addSetting(settings, providerId, instanceId, fullKey, val);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InstructorConfig{");
		if ( predicate != null ) {
			builder.append("predicate=");
			builder.append(predicate);
			builder.append(", ");
		}
		if ( instructions != null ) {
			builder.append("instructions=");
			builder.append(Arrays.toString(instructions));
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the predicate configuration.
	 *
	 * @return the predicate configuration
	 */
	public final ExpressionConfiguration getPredicate() {
		return predicate;
	}

	/**
	 * Add another instruction configuration.
	 *
	 * @param config
	 *        the configuration to add
	 */
	public final void addInstruction(InstructionConfig config) {
		final int newCount = getInstructionsCount() + 1;
		setInstructionsCount(newCount);
		final InstructionConfig[] configs = nonnull(instructions, "Instruction configs");
		configs[newCount - 1] = config;
	}

	/**
	 * Get the instruction instructions.
	 *
	 * @return the instructions
	 */
	public final InstructionConfig @Nullable [] getInstructions() {
		return instructions;
	}

	/**
	 * Get the number of configured {@code instructions} elements.
	 *
	 * @return the number of {@code instructions} elements
	 */
	public final int getInstructionsCount() {
		final InstructionConfig[] confs = this.instructions;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code instructions} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link InstructionConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code instructions} elements.
	 */
	public final void setInstructionsCount(int count) {
		this.instructions = ArrayUtils.arrayWithLength(this.instructions, count, InstructionConfig.class,
				null);
	}

	/**
	 * Set the instruction instructions.
	 *
	 * @param instructions
	 *        the instructions to set
	 */
	public final void setInstructions(InstructionConfig @Nullable [] instructions) {
		this.instructions = instructions;
	}

}
