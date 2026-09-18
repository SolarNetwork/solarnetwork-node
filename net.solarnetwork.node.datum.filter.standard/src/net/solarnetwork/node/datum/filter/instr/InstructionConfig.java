/* ==================================================================
 * InstructionConfig.java - 11/07/2026 1:51:45 pm
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
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * A single instruction configuration.
 *
 * @author matt
 * @version 1.0
 * @since 4.5
 */
public class InstructionConfig {

	/**
	 * A setting type pattern for an instruction block configuration element.
	 *
	 * <p>
	 * The pattern has two capture groups: the instruction configuration index
	 * and the property setting name.
	 * </p>
	 */
	public static final Pattern INSTRUCTION_SETTING_PATTERN = Pattern
			.compile(".+".concat(Pattern.quote(".instructions[")).concat("(\\d+)\\]\\.(.*)"));

	/**
	 * A setting type pattern for an instruction parameter block configuration
	 * element.
	 *
	 * <p>
	 * The pattern has two capture groups: the instruction parameter
	 * configuration index and the property setting name.
	 * </p>
	 */
	public static final Pattern PARAMETER_SETTING_PATTERN = Pattern
			.compile(".+".concat(Pattern.quote(".parameters[")).concat("(\\d+)\\]\\.(.*)"));

	/**
	 * A setting type pattern for an instruction parameter block configuration
	 * element.
	 *
	 * <p>
	 * The pattern has two capture groups: the instruction parameter
	 * configuration index and the property setting name.
	 * </p>
	 */
	public static final Pattern RESPONSE_SETTING_PATTERN = Pattern
			.compile(".+".concat(Pattern.quote(".responses[")).concat("(\\d+)\\]\\.(.*)"));

	private @Nullable String topic;
	private ExpressionConfig @Nullable [] parameters;
	private ExpressionConfig @Nullable [] responses;

	/**
	 * Constructor.
	 */
	public InstructionConfig() {
		super();
	}

	/**
	 * Create a "template" instance for settings.
	 *
	 * @return the template instance
	 */
	public static InstructionConfig template() {
		var result = new InstructionConfig();
		result.parameters = new ExpressionConfig[] { new ExpressionConfig() };
		result.responses = new ExpressionConfig[] { new ExpressionConfig() };
		return result;
	}

	/**
	 * Test if this configuration is valid for evaluation.
	 *
	 * @return {@code true} if an instruction topic is available
	 */
	public boolean isValid() {
		return (topic != null && !topic.isEmpty());
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

		result.add(new BasicTextFieldSettingSpecifier(prefix + "topic", null));

		// parameters list
		final ExpressionConfig[] params = getParameters();
		final List<ExpressionConfig> paramsList = (template ? List.of(new ExpressionConfig())
				: (params != null ? List.of(params) : List.of()));
		result.add(SettingUtils.dynamicListSettingSpecifier(prefix + "parameters", paramsList,
				new SettingUtils.KeyedListCallback<>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							@Nullable ExpressionConfig value, int index, String key) {
						// remove the Property Type setting from the expression config, which does not apply for
						// these instruction parameter expressions
						List<SettingSpecifier> exprSettings = ExpressionConfig
								.settings(InstructorConfig.class, key + ".", expressionServices).stream()
								.filter(s -> !(s instanceof KeyedSettingSpecifier<?> ks
										&& ks.getKey().endsWith(".datumPropertyTypeKey")))
								.toList();
						return List.of(new BasicGroupSettingSpecifier(exprSettings));
					}
				}));

		// responses list
		final ExpressionConfig[] resps = getResponses();
		final List<ExpressionConfig> respsList = (template ? List.of(new ExpressionConfig())
				: (resps != null ? List.of(resps) : List.of()));
		result.add(SettingUtils.dynamicListSettingSpecifier(prefix + "responses", respsList,
				new SettingUtils.KeyedListCallback<>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							@Nullable ExpressionConfig value, int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(ExpressionConfig
								.settings(InstructorConfig.class, key + ".", expressionServices));
						return List.of(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Populate a setting as a configuration value, if possible.
	 *
	 * @param config
	 *        the overall configuration
	 * @param setting
	 *        the setting to try to handle
	 * @return {@code true} if the setting was handled as a property
	 *         configuration value
	 */
	public static boolean populateFromSetting(InstructorConfig config, Setting setting) {
		Matcher m = INSTRUCTION_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		if ( idx >= config.getInstructionsCount() ) {
			config.setInstructionsCount(idx + 1);
		}

		final InstructionConfig instructionConfig = nonnull(config.getInstructions(),
				"Instruction configs")[idx];

		if ( populateParameterFromSetting(instructionConfig, setting) ) {
			return true;
		} else if ( populateResponseFromSetting(instructionConfig, setting) ) {
			return true;
		}

		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "topic":
					instructionConfig.setTopic(val);
					break;
				default:
					// ignore
			}
		}
		return true;
	}

	private static boolean populateParameterFromSetting(InstructionConfig config, Setting setting) {
		Matcher m = PARAMETER_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		if ( idx >= config.getParametersCount() ) {
			config.setParametersCount(idx + 1);
		}

		final ExpressionConfig parameterConfig = nonnull(config.getParameters(),
				"Parameter configs")[idx];
		parameterConfig.populateSetting(name, setting);
		return true;
	}

	private static boolean populateResponseFromSetting(InstructionConfig config, Setting setting) {
		Matcher m = RESPONSE_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		if ( idx >= config.getResponsesCount() ) {
			config.setResponsesCount(idx + 1);
		}

		final ExpressionConfig responseConfig = nonnull(config.getResponses(), "Response configs")[idx];
		responseConfig.populateSetting(name, setting);
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
	 *        the unit configuration index
	 * @param instructionIdx
	 *        the block configuration index
	 * @return the settings
	 */
	public List<SettingValueBean> toSettingValues(final String providerId,
			final @Nullable String instanceId, final int instructorIdx, final int instructionIdx) {
		List<SettingValueBean> settings = new ArrayList<>(2);
		addSetting(settings, providerId, instanceId, instructorIdx, instructionIdx, "topic", getTopic());
		if ( parameters != null ) {
			int i = 0;
			for ( ExpressionConfig paramConfig : parameters ) {
				String prefix = format("instructorConfigs[%d].instructions[%d].parameters[%d].",
						instructorIdx, instructionIdx, i++);
				// remove the Property Type setting from the expression config, which does not apply for
				// these instruction parameter expressions
				List<SettingValueBean> paramSettings = paramConfig
						.toSettingValues(providerId, instanceId, prefix).stream()
						.filter(s -> s.getKey() != null && !s.getKey().endsWith(".datumPropertyTypeKey"))
						.toList();
				settings.addAll(paramSettings);
			}
		}
		if ( responses != null ) {
			int i = 0;
			for ( ExpressionConfig responseConfig : responses ) {
				String prefix = format("instructorConfigs[%d].instructions[%d].responses[%d].",
						instructorIdx, instructionIdx, i++);
				List<SettingValueBean> responseSettings = responseConfig.toSettingValues(providerId,
						instanceId, prefix);
				settings.addAll(responseSettings);
			}
		}
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId,
			@Nullable String instanceId, int instructorIdx, int instructionIdx, String key,
			@Nullable Object val) {
		if ( val == null ) {
			return;
		}
		String fullKey = format("instructorConfigs[%d].instructions[%d].%s", instructorIdx,
				instructionIdx, key);
		SettingValueBean.addSetting(settings, providerId, instanceId, fullKey, val);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InstructionConfig{");
		if ( topic != null ) {
			builder.append("topic=");
			builder.append(topic);
			builder.append(", ");
		}
		if ( parameters != null ) {
			builder.append("parameters=");
			builder.append(Arrays.toString(parameters));
			builder.append(", ");
		}
		if ( responses != null ) {
			builder.append("responses=");
			builder.append(Arrays.toString(responses));
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the instruction topic.
	 *
	 * @return the topic
	 */
	public final @Nullable String getTopic() {
		return topic;
	}

	/**
	 * Set the instruction topic.
	 *
	 * @param topic
	 *        the topic to set
	 */
	public final void setTopic(@Nullable String topic) {
		this.topic = topic;
	}

	/**
	 * Add another parameter configuration.
	 *
	 * @param config
	 *        the configuration to add
	 */
	public final void addParameter(ExpressionConfig config) {
		final int newCount = getParametersCount() + 1;
		setParametersCount(newCount);
		final ExpressionConfig[] configs = nonnull(parameters, "Parameter configs");
		configs[newCount - 1] = config;
	}

	/**
	 * Get the instruction parameters.
	 *
	 * @return the parameters
	 */
	public final ExpressionConfig @Nullable [] getParameters() {
		return parameters;
	}

	/**
	 * Get the number of configured {@code parameters} elements.
	 *
	 * @return the number of {@code parameters} elements
	 */
	public final int getParametersCount() {
		final ExpressionConfig[] confs = this.parameters;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code parameters} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ExpressionConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code parameters} elements.
	 */
	public final void setParametersCount(int count) {
		this.parameters = ArrayUtils.arrayWithLength(this.parameters, count, ExpressionConfig.class,
				null);
	}

	/**
	 * Set the instruction parameters.
	 *
	 * @param parameters
	 *        the parameters to set
	 */
	public final void setParameters(ExpressionConfig @Nullable [] parameters) {
		this.parameters = parameters;
	}

	/**
	 * Add another response configuration.
	 *
	 * @param config
	 *        the configuration to add
	 */
	public final void addResponse(ExpressionConfig config) {
		final int newCount = getResponsesCount() + 1;
		setResponsesCount(newCount);
		final ExpressionConfig[] configs = nonnull(responses, "Response configs");
		configs[newCount - 1] = config;
	}

	/**
	 * Get the response expression configurations.
	 *
	 * @return the responses
	 */
	public final ExpressionConfig @Nullable [] getResponses() {
		return responses;
	}

	/**
	 * Set the response expression configurations.
	 *
	 * @param responses
	 *        the responses to set
	 */
	public final void setResponses(ExpressionConfig @Nullable [] responses) {
		this.responses = responses;
	}

	/**
	 * Get the number of configured {@code responses} elements.
	 *
	 * @return the number of {@code responses} elements
	 */
	public final int getResponsesCount() {
		final ExpressionConfig[] confs = this.responses;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code responses} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ExpressionConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code responses} elements.
	 */
	public final void setResponsesCount(int count) {
		this.responses = ArrayUtils.arrayWithLength(this.responses, count, ExpressionConfig.class, null);
	}

}
