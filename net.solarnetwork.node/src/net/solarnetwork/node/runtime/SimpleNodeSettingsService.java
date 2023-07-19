/* ==================================================================
 * SimpleNodeSettingsService.java - 19/07/2023 6:57:05 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.node.reactor.InstructionUtils.createErrorResultParameters;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.StringUtils.parseBoolean;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.settings.ExtendedSettingSpecifierProviderFactory;
import net.solarnetwork.node.settings.FileSettingSpecifier;
import net.solarnetwork.node.settings.LocationLookupSettingSpecifier;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.SetupResourceSettingSpecifier;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.settings.GroupSettingSpecifier;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
import net.solarnetwork.settings.SliderSettingSpecifier;
import net.solarnetwork.settings.TextAreaSettingSpecifier;
import net.solarnetwork.settings.TextFieldSettingSpecifier;
import net.solarnetwork.settings.TitleSettingSpecifier;
import net.solarnetwork.settings.ToggleSettingSpecifier;
import net.solarnetwork.util.ObjectUtils;

/**
 * Service to support node settings management.
 * 
 * <p>
 * This {@link InstructionHandler} responds to
 * {@link InstructionHandler#TOPIC_SYSTEM_CONFIGURATION
 * TOPIC_SYSTEM_CONFIGURATION} requests where the
 * {@link InstructionHandler#PARAM_SERVICE PARAM_SERVICE} value is
 * {@link #SETTINGS_SERVICE_UID}. At a minimum a {@link #PARAM_SETTING_UID}
 * value must also be provided, and optionally the {@link #PARAM_INSTANCE_ID}
 * value can be provided. Those two parameters determine the results returned,
 * as outlined below. The result is always a JSON string on the
 * {@link InstructionHandler#PARAM_SERVICE_RESULT PARAM_SERVICE_RESULT} result
 * parameter.
 * </p>
 * 
 * <table style="border: 1px solid #ccc; min-width: 50%;">
 * <caption>How the {@code uid} and {@code id} parameters control the result
 * format.</caption> <thead>
 * <tr style="background-color: #ccc;">
 * <th>uid</th>
 * <th>id</th>
 * <th>Operation</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td><kbd>*</kbd></td>
 * <td></td>
 * <td><a href="#list-providers">List providers</a></td>
 * </tr>
 * <tr>
 * <td><i>{settingUid}</i></td>
 * <td></td>
 * <td><a href="#list-settings">List settings</a></td>
 * </tr>
 * <tr>
 * <td><kbd>*</kbd></td>
 * <td><kbd>*</kbd></td>
 * <td><a href="#list-factories">List factories</a></td>
 * </tr>
 * <tr>
 * <td><i>{factoryUid}</i></td>
 * <td><kbd>*</kbd></td>
 * <td><a href="#list-factory-instances">List factory instances</a></td>
 * </tr>
 * <tr>
 * <td><i>{factoryUid}</i></td>
 * <td><i>{instanceId}</i></td>
 * <td><a href="#list-factory-instance-settings">List factory instance
 * settings</a></td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <h2 id="list-providers">List all available non-factory setting UIDs</h2>
 * 
 * <p>
 * Pass a {@code uid} parameter value of {@literal *} to generate a JSON array
 * of objects, each representing a single provider, with the following
 * properties:
 * </p>
 * 
 * <dl>
 * <dt>id</dt>
 * <dd>The provider ID ({@code settingUid}).</dd>
 * <dt>title</dt>
 * <dd>The component title.</dd>
 * </dl>
 * 
 * <h2 id="list-settings">List the settings for a non-factory provider</h2>
 * 
 * <p>
 * Pass a {@code uid} parameter value of the specific setting UID you'd like to
 * get the settings for. The result will be a JSON array of objects, each
 * representing a single setting, with the following properties:
 * </p>
 * 
 * <dl>
 * <dt>key</dt>
 * <dd>The setting key.</dd>
 * <dt>value</dt>
 * <dd>The setting value.</dd>
 * <dt>default</dt>
 * <dd>A boolean, {@literal true} indicating that the {@code value} is a
 * "default" value provided by SolarNode, not one a user has specifically
 * configured. This property might be omitted entirely instead of appearing with
 * a {@literal false} value.</dd>
 * </dl>
 * 
 * <h2 id="list-factories">List all available factory setting UIDs</h2>
 * 
 * <p>
 * Pass a {@code uid} parameter value of {@literal *} <b>and</b> a {@code id}
 * parameter value of {@literal *} to generate a JSON array of objects, each
 * representing a single factory, as shown in the <a href="#list-providers">list
 * providers</a> section.
 * </p>
 * 
 * <h2 id="list-factory-instances">List the available instance IDs for a
 * factory</h2>
 * 
 * <p>
 * To generate a JSON array of instance IDs for a specific factory UID, pass a
 * {@code uid} parameter value of the factory UID you'd like the list for, and a
 * {@code id} parameter value of {@literal *}.
 * </p>
 * 
 * <h2 id="list-factory-instance-settings">List the settings for a factory
 * instance provider</h2>
 * 
 * <p>
 * Pass a {@code uid} parameter value of the specific factory UID and a
 * {@code id} parameter value of the specific factory instance ID you'd like to
 * list the settings for. The result will be a JSON array of objects, as shown
 * in the <a href="#list-settings">list settings</a> section.
 * </p>
 * 
 * <h2>Specification mode</h2>
 * 
 * <p>
 * By passing a {@code spec} parameter value of {@literal true}, then instead of
 * returning component setting values, setting specification objects will be
 * returned instead. At a minimum, each specification object has a {@code type}
 * property that is the setting type. Other specifications provide more
 * properties, as listed below.
 * </p>
 * 
 * <h3>Group specification</h3>
 * 
 * <p>
 * A group specification has the type
 * {@literal net.solarnetwork.settings.GroupSettingSpecifier}. It will include a
 * {@code dynamic} boolean property, where {@literal true} represents a dynamic
 * list of specifications. The {@code groupSettings} property will be an array
 * of nested specification objects that make up the group.
 * </p>
 * 
 * <h3>Other specifications</h3>
 * 
 * <p>
 * Most specifications provide a {@code key} property representing the setting
 * key for that specification.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.3
 */
public class SimpleNodeSettingsService extends BaseIdentifiable implements InstructionHandler {

	/** The default UID for this service. */
	public static final String SETTINGS_SERVICE_UID = "net.solarnetwork.node.settings";

	/**
	 * An optional instruction parameter containing a specific setting UID (or
	 * factory UID) to get the settings for.
	 */
	public static final String PARAM_SETTING_UID = "uid";

	/**
	 * An optional instruction parameter containing a specific setting factory
	 * instance ID to get the settings for.
	 * 
	 * <p>
	 * If this is provided, then the {@link #PARAM_SETTING_UID} parameter must
	 * also be provided, and that will be interpreted as the factory UID.
	 * </p>
	 */
	public static final String PARAM_INSTANCE_ID = "id";

	/**
	 * An optional instruction parameter containing a boolean flag that, when
	 * {@literal true}, means setting specifications should be returned instead
	 * of setting values.
	 */
	public static final String PARAM_SPECIFICATION = "spec";

	/**
	 * An optional instruction parameter containing a boolean flag that, when
	 * {@literal true}, means the result JSON should be gzip-compressed and
	 * encoded into a Base64 string representation.
	 * 
	 * <p>
	 * Note that only successful results will be compressed. If the compressed
	 * result is larger than the uncompressed JSON, the uncompressed JSON will
	 * be returned instead.
	 * </p>
	 */
	public static final String PARAM_COMPRESSED = "compress";

	/**
	 * A special {@link #PARAM_SETTING_UID} or {@link #PARAM_INSTANCE_ID} value
	 * that signals to return the list of available IDs.
	 */
	public static final String ID_LIST = "*";

	/** A redacted setting value. */
	public static final String REDACTED_VALUE = "*****";

	private static Pattern SENSITVE_PATTERN = Pattern.compile("(apikey|password|secret)",
			Pattern.CASE_INSENSITIVE);

	private final OptionalService<SettingsService> settingsService;

	/**
	 * Constructor.
	 * 
	 * @param settingsService
	 *        the settings service
	 * @throws IllegalArgumentException
	 *         if any argument is null
	 */
	public SimpleNodeSettingsService(OptionalService<SettingsService> settingsService) {
		super();
		this.settingsService = ObjectUtils.requireNonNullArgument(settingsService, "settingsService");
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURATION.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		final String uid = getUid() != null ? getUid() : SETTINGS_SERVICE_UID;
		final String serviceId = instruction.getParameterValue(PARAM_SERVICE);
		if ( !uid.equals(serviceId) ) {
			return null;
		}

		final SettingsService service = service(settingsService);
		if ( service == null ) {
			return createStatus(instruction, Declined,
					createErrorResultParameters("No SettingsService available.", "SNS.00001"));
		}

		final String sid = instruction.getParameterValue(PARAM_SETTING_UID);
		final String iid = instruction.getParameterValue(PARAM_INSTANCE_ID);

		if ( iid != null && sid != null ) {
			if ( ID_LIST.equals(sid) ) {
				// return list of available factories
				return generateFactoryList(instruction, service);
			}
			// factory instance
			SettingSpecifierProviderFactory f = service.getProviderFactory(sid);
			if ( f == null ) {
				return createStatus(instruction, Declined,
						createErrorResultParameters("Factory not available.", "SNS.00002"));
			}
			if ( ID_LIST.equals(iid) ) {
				// want list of factory instance IDs
				return generateFactoryInstanceList(instruction, f);
			} else {
				return generateFactoryInstanceSettingsList(instruction, service, sid, iid);
			}
		} else if ( sid != null ) {
			// provider
			if ( ID_LIST.equals(sid) ) {
				return generateProviderList(instruction, service);
			}
			return generateProviderSettingsList(instruction, service, sid);
		}

		return createStatus(instruction, Declined,
				createErrorResultParameters("Required parameter [uid] missing.", "SNS.00000"));
	}

	private String jsonResult(Instruction instruction, Object obj, String defaultJson) {
		String json = JsonUtils.getJSONString(obj, defaultJson);
		if ( json == null || (defaultJson != null && defaultJson.equals(json)) ) {
			return json;
		}
		boolean compress = parseBoolean(instruction.getParameterValue(PARAM_COMPRESSED));
		if ( !compress ) {
			return json;
		}

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(baos);
				OutputStreamWriter out = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
			FileCopyUtils.copy(json, out);
			String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
			return b64.length() > json.length() ? json : b64;
		} catch ( IOException e ) {
			// ignore and return uncompressed
			return json;
		}
	}

	private InstructionStatus generateFactoryInstanceList(Instruction instruction,
			SettingSpecifierProviderFactory f) {
		if ( f instanceof ExtendedSettingSpecifierProviderFactory ) {
			ExtendedSettingSpecifierProviderFactory ef = (ExtendedSettingSpecifierProviderFactory) f;
			Set<String> instanceIds = ef.getSettingSpecifierProviderInstanceIds();
			return createStatus(instruction, Completed,
					singletonMap(PARAM_SERVICE_RESULT, jsonResult(instruction, instanceIds, "[]")));
		} else {
			return createStatus(instruction, Declined,
					createErrorResultParameters("Factory instances not discoverable.", "SNS.00003"));
		}
	}

	private InstructionStatus generateFactoryInstanceSettingsList(Instruction instruction,
			final SettingsService service, final String factoryId, final String instanceId) {
		Map<String, FactorySettingSpecifierProvider> providers = service
				.getProvidersForFactory(factoryId);
		FactorySettingSpecifierProvider provider = providers.get(instanceId);
		if ( provider == null ) {
			return createStatus(instruction, Declined, createErrorResultParameters(
					"Factory instance provider not available.", "SNS.00004"));
		}
		List<Map<String, Object>> result = parseBoolean(
				instruction.getParameterValue(PARAM_SPECIFICATION))
						? resultSpecification(service, provider)
						: resultSettings(service, provider);
		return createStatus(instruction, Completed,
				singletonMap(PARAM_SERVICE_RESULT, jsonResult(instruction, result, "[]")));
	}

	private InstructionStatus generateProviderSettingsList(Instruction instruction,
			final SettingsService service, final String settingsUid) {
		SettingSpecifierProvider provider = service.getProviders().stream()
				.filter(p -> settingsUid.equals(p.getSettingUid())).findAny().orElse(null);
		if ( provider == null ) {
			return createStatus(instruction, Declined,
					createErrorResultParameters("Provider not available.", "SNS.00005"));
		}
		List<Map<String, Object>> result = parseBoolean(
				instruction.getParameterValue(PARAM_SPECIFICATION))
						? resultSpecification(service, provider)
						: resultSettings(service, provider);
		return createStatus(instruction, Completed,
				singletonMap(PARAM_SERVICE_RESULT, jsonResult(instruction, result, "[]")));
	}

	private InstructionStatus generateFactoryList(Instruction instruction,
			final SettingsService service) {
		List<SettingSpecifierProviderFactory> factories = service.getProviderFactories();
		List<Map<String, Object>> results = new ArrayList<>(factories.size());
		for ( SettingSpecifierProviderFactory f : factories ) {
			Map<String, Object> props = new LinkedHashMap<>(2);
			props.put("id", f.getFactoryUid());
			String title = f.getDisplayName();
			if ( f.getMessageSource() != null ) {
				title = f.getMessageSource().getMessage("title", null, title, Locale.getDefault());
			}
			props.put("title", title);
			results.add(props);
		}
		return createStatus(instruction, Completed,
				singletonMap(PARAM_SERVICE_RESULT, jsonResult(instruction, results, "[]")));
	}

	private InstructionStatus generateProviderList(Instruction instruction, SettingsService service) {
		List<SettingSpecifierProvider> providers = service.getProviders();
		List<Map<String, Object>> results = new ArrayList<>(providers.size());
		for ( SettingSpecifierProvider p : providers ) {
			Map<String, Object> props = new LinkedHashMap<>(2);
			props.put("id", p.getSettingUid());
			String title = p.getDisplayName();
			if ( p.getMessageSource() != null ) {
				title = p.getMessageSource().getMessage("title", null, title, Locale.getDefault());
			}
			props.put("title", title);
			results.add(props);
		}
		return createStatus(instruction, Completed,
				singletonMap(PARAM_SERVICE_RESULT, jsonResult(instruction, results, "[]")));
	}

	private List<Map<String, Object>> resultSettings(SettingsService service,
			SettingSpecifierProvider provider) {
		String instanceId = provider instanceof FactorySettingSpecifierProvider
				? ((FactorySettingSpecifierProvider) provider).getFactoryInstanceUID()
				: null;
		List<SettingSpecifier> specs = provider.getSettingSpecifiers();
		List<Setting> settings = service.getSettings(
				instanceId != null ? provider.getSettingUid() : null,
				instanceId != null ? instanceId : provider.getSettingUid());
		Map<String, Setting> settingsMap;
		if ( settings != null && !settings.isEmpty() ) {
			settingsMap = new HashMap<>(settings.size());
			for ( Setting s : settings ) {
				// in a Setting, the key is the UID and type is the setting key
				settingsMap.put(s.getType(), s);
			}
		} else {
			settingsMap = Collections.emptyMap();
		}
		List<Map<String, Object>> resultSettings = new ArrayList<>(specs.size());
		for ( SettingSpecifier spec : specs ) {
			generateResultSetting(resultSettings, settingsMap, spec);
		}
		return resultSettings;
	}

	private void generateResultSetting(final List<Map<String, Object>> resultSettings,
			final Map<String, Setting> settingsMap, final SettingSpecifier spec) {
		if ( spec instanceof KeyedSettingSpecifier<?> ) {
			KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
			if ( keyedSpec.isTransient() ) {
				return;
			}
			Map<String, Object> props = new LinkedHashMap<>(4);
			props.put("key", keyedSpec.getKey());
			Setting setting = settingsMap.get(keyedSpec.getKey());
			props.put("value", setting != null ? settingValue(setting) : keyedSpec.getDefaultValue());
			if ( setting == null ) {
				props.put("default", true);
			}
			resultSettings.add(props);
		} else if ( spec instanceof GroupSettingSpecifier ) {
			GroupSettingSpecifier group = (GroupSettingSpecifier) spec;
			for ( SettingSpecifier groupSpec : group.getGroupSettings() ) {
				generateResultSetting(resultSettings, settingsMap, groupSpec);
			}
		}
	}

	private static String settingValue(Setting setting) {
		String key = setting.getType();
		String value = setting.getValue();
		if ( value != null && key != null && SENSITVE_PATTERN.matcher(key).find() ) {
			value = REDACTED_VALUE;
		}
		return value;
	}

	private List<Map<String, Object>> resultSpecification(SettingsService service,
			SettingSpecifierProvider provider) {
		List<SettingSpecifier> specs = provider.templateSettingSpecifiers();
		List<Map<String, Object>> resultSettings = new ArrayList<>(specs.size());
		for ( SettingSpecifier spec : specs ) {
			generateResultSpecification(resultSettings, spec);
		}
		return resultSettings;
	}

	private void generateResultSpecification(List<Map<String, Object>> resultSettings,
			SettingSpecifier spec) {
		if ( spec instanceof GroupSettingSpecifier ) {
			GroupSettingSpecifier group = (GroupSettingSpecifier) spec;
			Map<String, Object> props = new LinkedHashMap<>(2);
			props.put("type", group.getType());
			props.put("dynamic", group.isDynamic());
			List<Map<String, Object>> nested = new ArrayList<>(8);
			for ( SettingSpecifier groupSpec : group.getGroupSettings() ) {
				generateResultSpecification(nested, groupSpec);
			}
			props.put("groupSettings", nested);
			resultSettings.add(props);
		} else if ( spec instanceof KeyedSettingSpecifier<?> ) {
			KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
			if ( keyedSpec.isTransient() ) {
				return;
			}
			Map<String, Object> props = new LinkedHashMap<>(8);
			props.put("key", keyedSpec.getKey());
			props.put("type", spec.getType());

			Object defaultValue = keyedSpec.getDefaultValue();
			if ( defaultValue != null ) {
				if ( !((defaultValue instanceof String) && ((String) defaultValue).isEmpty()) ) {
					props.put("defaultValue", keyedSpec.getDefaultValue());
				}
			}
			if ( spec instanceof FileSettingSpecifier ) {
				FileSettingSpecifier file = (FileSettingSpecifier) spec;
				props.put("acceptableFileTypeSpecifiers", file.getAcceptableFileTypeSpecifiers());
				if ( file.isMultiple() ) {
					props.put("multiple", file.isMultiple());
				}
			} else if ( spec instanceof LocationLookupSettingSpecifier ) {
				LocationLookupSettingSpecifier look = (LocationLookupSettingSpecifier) spec;
				props.put("locationTypeKey", look.getLocationTypeKey());
			} else if ( spec instanceof SliderSettingSpecifier ) {
				SliderSettingSpecifier slider = (SliderSettingSpecifier) spec;
				props.put("minimumValue", slider.getMinimumValue());
				props.put("maximumValue", slider.getMaximumValue());
				props.put("step", slider.getStep());
			} else if ( spec instanceof TextAreaSettingSpecifier ) {
				TextAreaSettingSpecifier area = (TextAreaSettingSpecifier) spec;
				if ( area.isDirect() ) {
					props.put("direct", area.isDirect());
				}
			} else if ( spec instanceof TitleSettingSpecifier ) {
				TitleSettingSpecifier title = (TitleSettingSpecifier) spec;
				if ( title.isMarkup() ) {
					props.put("markup", title.isMarkup());
				}
				if ( title.getValueTitles() != null ) {
					props.put("valueTitles", title.getValueTitles());
				}
				if ( spec instanceof TextFieldSettingSpecifier ) {
					TextFieldSettingSpecifier text = (TextFieldSettingSpecifier) spec;
					if ( text.isSecureTextEntry() ) {
						props.put("secureTextEntry", text.isSecureTextEntry());
					}
				}
			} else if ( spec instanceof ToggleSettingSpecifier ) {
				ToggleSettingSpecifier toggle = (ToggleSettingSpecifier) spec;
				props.put("falseValue", toggle.getFalseValue());
				props.put("trueValue", toggle.getTrueValue());
			}
			resultSettings.add(props);
		} else if ( spec instanceof SetupResourceSettingSpecifier ) {
			SetupResourceSettingSpecifier setup = (SetupResourceSettingSpecifier) spec;
			Map<String, Object> props = new LinkedHashMap<>(2);
			props.put("type", setup.getType());
			props.put("setupResourceProperties", setup.getSetupResourceProperties());
			resultSettings.add(props);
		}
	}

}
