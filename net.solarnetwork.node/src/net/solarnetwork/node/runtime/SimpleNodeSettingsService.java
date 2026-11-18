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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import org.jspecify.annotations.Nullable;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.settings.ExtendedSettingSpecifierProviderFactory;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.support.SettingsUtils;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.FactorySettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;
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
 * @version 1.1
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
	public boolean handlesTopic(@Nullable String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURATION.equals(topic);
	}

	@Override
	public @Nullable InstructionStatus processInstruction(Instruction instruction) {
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

	private @Nullable String jsonResult(Instruction instruction, Object obj, String defaultJson) {
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
		List<Map<String, Object>> result = SettingsUtils.factoryInstanceSettingInfos(service, provider,
				parseBoolean(instruction.getParameterValue(PARAM_SPECIFICATION)), null);
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
		List<Map<String, Object>> result = SettingsUtils.providerSettingInfos(service, provider,
				parseBoolean(instruction.getParameterValue(PARAM_SPECIFICATION)), null);
		return createStatus(instruction, Completed,
				singletonMap(PARAM_SERVICE_RESULT, jsonResult(instruction, result, "[]")));
	}

	private InstructionStatus generateFactoryList(Instruction instruction,
			final SettingsService service) {
		List<SettingSpecifierProviderFactory> factories = service.getProviderFactories();
		List<Map<String, Object>> results = SettingsUtils.factoryInfos(factories, null);
		return createStatus(instruction, Completed,
				singletonMap(PARAM_SERVICE_RESULT, jsonResult(instruction, results, "[]")));
	}

	private InstructionStatus generateProviderList(Instruction instruction, SettingsService service) {
		List<SettingSpecifierProvider> providers = service.getProviders();
		List<Map<String, Object>> results = SettingsUtils.providerInfos(providers, null);
		return createStatus(instruction, Completed,
				singletonMap(PARAM_SERVICE_RESULT, jsonResult(instruction, results, "[]")));
	}

}
