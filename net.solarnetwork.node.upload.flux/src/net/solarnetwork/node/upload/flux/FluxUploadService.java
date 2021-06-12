/* ==================================================================
 * FluxUploadService.java - 16/12/2018 9:18:57 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.flux;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.node.OperationalModesService.hasActiveOperationalMode;
import static net.solarnetwork.node.settings.support.SettingsUtil.dynamicListSettingSpecifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.codec.binary.Hex;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.common.mqtt.BaseMqttConnectionService;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttConnectionObserver;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.common.mqtt.MqttVersion;
import net.solarnetwork.common.mqtt.ReconfigurableMqttConnection;
import net.solarnetwork.common.mqtt.dao.BasicMqttMessageEntity;
import net.solarnetwork.common.mqtt.dao.MqttMessageDao;
import net.solarnetwork.common.mqtt.dao.MqttMessageEntity;
import net.solarnetwork.dao.BasicBatchOptions;
import net.solarnetwork.dao.BatchableDao;
import net.solarnetwork.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.Identifiable;
import net.solarnetwork.io.ObjectEncoder;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatumSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * Service to listen to datum events and upload datum to SolarFlux.
 * 
 * @author matt
 * @version 1.10
 */
public class FluxUploadService extends BaseMqttConnectionService implements EventHandler,
		SettingSpecifierProvider, SettingsChangeObserver, MqttConnectionObserver {

	/** The MQTT topic template for node data publication. */
	public static final String NODE_DATUM_TOPIC_TEMPLATE = "node/%d/datum/0/%s";

	/** The default value for the {@code mqttHost} property. */
	public static final String DEFAULT_MQTT_HOST = "mqtts://influx.solarnetwork.net:8884";

	/** The default value for the {@code mqttUsername} property. */
	public static final String DEFAULT_MQTT_USERNAME = "solarnode";

	/**
	 * The default value for the {@code excludePropertyNamesPattern} property.
	 */
	public static final Pattern DEFAULT_EXCLUDE_PROPERTY_NAMES_PATTERN = Pattern.compile("_.*");

	/** A tag to indicate that CBOR encoding v2 is in use. */
	public static final String TAG_VERSION = "_v";

	/** The default value for the {@code includeVersionTag} property. */
	public static final boolean DEFAULT_INCLUDE_VERSION_TAG = true;

	/**
	 * The default MQTT version to use.
	 * 
	 * @since 1.8
	 */
	public static final MqttVersion DEFAULT_MQTT_VERSION = MqttVersion.Mqtt5;

	private final ConcurrentMap<String, Long> SOURCE_CAPTURE_TIMES = new ConcurrentHashMap<>(16, 0.9f,
			2);

	private final ObjectMapper objectMapper;
	private final IdentityService identityService;
	private String requiredOperationalMode;
	private Pattern excludePropertyNamesPattern = DEFAULT_EXCLUDE_PROPERTY_NAMES_PATTERN;
	private OperationalModesService opModesService;
	private Executor executor;
	private FluxFilterConfig[] filters;
	private boolean includeVersionTag = DEFAULT_INCLUDE_VERSION_TAG;
	private OptionalServiceCollection<ObjectEncoder> datumEncoders;
	private OptionalServiceCollection<GeneralDatumSamplesTransformService> transformServices;
	private OptionalService<MqttMessageDao> mqttMessageDao;

	/**
	 * Constructor.
	 * 
	 * @param connectionFactory
	 *        the factory to use for {@link MqttConnection} instances
	 * @param objectMapper
	 *        the object mapper to use
	 * @param identityService
	 *        the identity service
	 */
	public FluxUploadService(MqttConnectionFactory connectionFactory, ObjectMapper objectMapper,
			IdentityService identityService) {
		super(connectionFactory, new MqttStats(100));
		this.objectMapper = objectMapper;
		this.identityService = identityService;
		setPublishQos(MqttQos.AtMostOnce);
		getMqttConfig().setUsername(DEFAULT_MQTT_USERNAME);
		getMqttConfig().setServerUriValue(DEFAULT_MQTT_HOST);
		getMqttConfig().setVersion(DEFAULT_MQTT_VERSION);
	}

	@Override
	public void init() {
		if ( requiredOperationalMode != null && !requiredOperationalMode.isEmpty() ) {
			if ( opModesService == null
					|| !opModesService.isOperationalModeActive(requiredOperationalMode) ) {
				// don't connect to MQTT because required operational state not active
				log.info(
						"Not connecting to SolarFlux because required operational state [{}] not active",
						requiredOperationalMode);
				return;
			}
		}
		super.init();
	}

	@Override
	public synchronized Future<?> startup() {
		if ( getMqttConfig().getClientId() == null ) {
			getMqttConfig().setUid("SolarFluxUpload-" + getMqttConfig().getServerUriValue());
			getMqttConfig().setClientId(getMqttClientId());
		}
		return super.startup();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		getMqttConfig().setUid("SolarFluxUpload-" + getMqttConfig().getServerUriValue());
		MqttConnection conn = connection();
		if ( conn instanceof ReconfigurableMqttConnection ) {
			((ReconfigurableMqttConnection) conn).reconfigure();
		}
		FluxFilterConfig[] filters = getFilters();
		if ( filters != null ) {
			for ( FluxFilterConfig f : filters ) {
				f.configurationChanged(properties);
			}
		}
	}

	@Override
	public void onMqttServerConnectionLost(MqttConnection connection, boolean willReconnect,
			Throwable cause) {
		// nothing special here
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		// if we have persistence, upload any cached messages now
		MqttMessageDao dao = OptionalService.service(mqttMessageDao);
		String dest = getMqttConfig().getServerUriValue();
		int timeoutSecs = getMqttConfig().getConnectTimeoutSeconds();
		if ( dao != null ) {
			Runnable task = new Runnable() {

				@Override
				public void run() {
					final BatchableDao.BatchResult result = dao
							.batchProcess(new BatchableDao.BatchCallback<MqttMessageEntity>() {

								@Override
								public BatchCallbackResult handle(MqttMessageEntity entity) {
									if ( !connection.isEstablished() ) {
										return BatchCallbackResult.STOP;
									}
									BatchCallbackResult action;
									try {
										connection.publish(entity).get(timeoutSecs, TimeUnit.SECONDS);
										action = BatchCallbackResult.DELETE;
										log.debug("Published cached MQTT message {} to topic {}",
												entity.getId(), entity.getTopic());
									} catch ( Exception e ) {
										log.debug(
												"Error publishing cached MQTT message {} to topic {}: {}",
												entity.getId(), entity.getTopic(), e.toString());
										action = BatchCallbackResult.CONTINUE;
									}
									return action;
								}

							}, new BasicBatchOptions("Process cached MQTT messages",
									BasicBatchOptions.DEFAULT_BATCH_SIZE, true,
									singletonMap(MqttMessageDao.BATCH_OPTION_DESTINATION, dest)));
					if ( result.numProcessed() > 0 ) {
						log.info("Uploaded {} locally cached MQTT messages to {}", result.numProcessed(),
								dest);
					}
				}
			};
			Executor e = this.executor;
			if ( e != null ) {
				e.execute(task);
			} else {
				task.run();
			}
		}
	}

	private String getMqttClientId() {
		final Long nodeId = identityService.getNodeId();
		return (nodeId != null ? nodeId.toString() : null);
	}

	@Override
	public void handleEvent(Event event) {
		final String topic = event.getTopic();
		if ( !(OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED.equals(topic)
				|| DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED.equals(topic)) ) {
			return;
		}
		Runnable task = new Runnable() {

			@Override
			public void run() {
				if ( OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED.equals(topic) ) {
					if ( requiredOperationalMode == null || requiredOperationalMode.isEmpty() ) {
						return;
					}
					log.trace("Operational modes changed; required = [{}]; active = {}",
							requiredOperationalMode, event.getProperty(
									OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES));
					if ( hasActiveOperationalMode(event, requiredOperationalMode) ) {
						// operational mode is active, bring up MQTT connection
						Future<?> f = startup();
						try {
							f.get(getMqttConfig().getConnectTimeoutSeconds(), TimeUnit.SECONDS);
						} catch ( Exception e ) {
							Throwable root = e;
							while ( root.getCause() != null ) {
								root = root.getCause();
							}
							String msg = (root instanceof TimeoutException ? "timeout"
									: root.getMessage());
							log.error("Error starting up connection to SolarFlux at {}: {}",
									getMqttConfig().getServerUri(), msg);
						}
					} else {
						// operational mode is no longer active, shut down MQTT connection
						shutdown();
					}
				} else {
					// EVENT_TOPIC_DATUM_CAPTURED
					if ( requiredOperationalMode != null && !requiredOperationalMode.isEmpty()
							&& (opModesService == null || !opModesService
									.isOperationalModeActive(requiredOperationalMode)) ) {
						log.trace("Not posting to SolarFlux because operational mode [{}] not active",
								requiredOperationalMode);
						return;
					}
					String sourceId = sourceIdForEvent(event);
					if ( sourceId == null || sourceId.isEmpty() ) {
						return;
					}

					final FluxFilterConfig[] activeFilters = activeFilters(getFilters(), sourceId);

					Map<String, Object> data = mapForEvent(activeFilters, sourceId, event);
					if ( data == null || data.isEmpty() ) {
						return;
					}
					publishDatum(activeFilters, sourceId, data);
				}
			}

		};
		Executor e = this.executor;
		if ( e != null ) {
			e.execute(task);
		} else {
			task.run();
		}
	}

	private FluxFilterConfig[] activeFilters(FluxFilterConfig[] filters, String sourceId) {
		if ( filters == null || filters.length < 1 ) {
			return null;
		}
		final OperationalModesService opModesService = this.opModesService;
		return Arrays.stream(filters).filter(f -> {
			if ( f.getRequiredOperationalMode() != null && !f.getRequiredOperationalMode().isEmpty() ) {
				if ( opModesService == null ) {
					// op mode required, but no service available
					return false;
				}
				if ( !opModesService.isOperationalModeActive(f.getRequiredOperationalMode()) ) {
					return false;
				}
			}
			return f.isSourceIdMatch(sourceId);
		}).toArray(FluxFilterConfig[]::new);
	}

	private boolean shouldPublishDatum(FluxFilterConfig[] activeFilters, String sourceId,
			Map<String, Object> data) {
		Long ts = System.currentTimeMillis();
		Long prevTs = SOURCE_CAPTURE_TIMES.get(sourceId);
		if ( activeFilters != null ) {
			for ( FluxFilterConfig filter : activeFilters ) {
				if ( filter == null ) {
					continue;
				}
				if ( !filter.isPublishAllowed(prevTs, sourceId, data) ) {
					return false;
				}
			}
		}
		SOURCE_CAPTURE_TIMES.compute(sourceId, (k, v) -> {
			return ((v == null && prevTs == null) || (v != null && v.equals(prevTs)) ? ts : v);
		});
		return true;
	}

	private void publishDatum(FluxFilterConfig[] activeFilters, String sourceId,
			Map<String, Object> data) {
		final Long nodeId = identityService.getNodeId();
		if ( nodeId == null ) {
			return;
		}
		if ( !shouldPublishDatum(activeFilters, sourceId, data) ) {
			return;
		}
		if ( sourceId.startsWith("/") ) {
			sourceId = sourceId.substring(1);
		}
		if ( sourceId.isEmpty() ) {
			return;
		}
		final MqttMessageDao dao = OptionalService.service(mqttMessageDao);
		MqttMessage msgToPersist = null;
		MqttConnection conn = connection();
		if ( dao != null || (conn != null && conn.isEstablished()) ) {
			String topic = String.format(NODE_DATUM_TOPIC_TEMPLATE, nodeId, sourceId);
			MqttMessage msg = null;
			try {
				byte[] payload;
				ObjectEncoder encoder = encoderForSourceId(activeFilters, sourceId);
				if ( encoder != null ) {
					payload = encoder.encodeAsBytes(data, null);
				} else {
					if ( includeVersionTag ) {
						data.put(TAG_VERSION, 2);
					}
					JsonNode jsonData = objectMapper.valueToTree(data);
					payload = objectMapper.writeValueAsBytes(jsonData);
					log.trace("Publishing to MQTT topic {} JSON:\n{}", topic, jsonData);
				}
				msg = new BasicMqttMessage(topic, true, getPublishQos(), payload);
				if ( conn != null && conn.isEstablished() ) {
					if ( log.isTraceEnabled() ) {
						log.trace("Publishing to MQTT topic {}\n{}", topic,
								Hex.encodeHexString(payload));
					}
					conn.publish(msg).get(getMqttConfig().getConnectTimeoutSeconds(), TimeUnit.SECONDS);
					log.debug("Published to MQTT topic {}: {}", topic, data);
				} else if ( dao != null ) {
					msgToPersist = msg;
				}
			} catch ( Exception e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				String message = (root instanceof TimeoutException ? "timeout" : root.getMessage());
				log.warn("Error publishing to MQTT topic {} datum {} @ {}: {}", topic, data,
						getMqttConfig().getServerUri(), message);
				if ( dao != null ) {
					msgToPersist = msg;
				}
			}
		}
		if ( msgToPersist != null ) {
			String dest = getMqttConfig().getServerUriValue();
			if ( dest != null ) {
				log.debug("Locally persisting MQTT message to {} on topic {}", dest,
						msgToPersist.getTopic());
				BasicMqttMessageEntity entity = new BasicMqttMessageEntity(null, Instant.now(), dest,
						msgToPersist);
				dao.save(entity);
			}
		}
	}

	private ObjectEncoder encoderForSourceId(FluxFilterConfig[] activeFilters, String sourceId) {
		return serviceForSourceId(activeFilters, sourceId, getDatumEncoders(),
				FluxFilterConfig::getDatumEncoderUid);
	}

	private GeneralDatumSamplesTransformService transformServiceForSourceId(
			FluxFilterConfig[] activeFilters, String sourceId) {
		return serviceForSourceId(activeFilters, sourceId, getTransformServices(),
				FluxFilterConfig::getTransformServiceUid);
	}

	private <T extends Identifiable> T serviceForSourceId(FluxFilterConfig[] activeFilters,
			String sourceId, OptionalServiceCollection<T> services,
			Function<FluxFilterConfig, String> uidProvider) {
		if ( services == null ) {
			return null;
		}
		if ( activeFilters == null || activeFilters.length < 1 ) {
			return null;
		}
		Iterable<T> serviceItr = null;
		for ( FluxFilterConfig cfg : activeFilters ) {
			String uid = (cfg != null ? uidProvider.apply(cfg) : null);
			if ( uid == null || uid.isEmpty() ) {
				continue;
			}
			Pattern filterSourceId = cfg.getSourceIdRegex();
			if ( filterSourceId == null || filterSourceId.matcher(sourceId).find() ) {
				if ( serviceItr == null ) {
					serviceItr = services.services();
				}
				for ( T service : serviceItr ) {
					if ( uid.equals(service.getUid()) ) {
						return service;
					}
				}
				// if no service available, don't keep checking more filters for another
				break;
			}
		}
		return null;
	}

	private String sourceIdForEvent(Event event) {
		if ( event == null ) {
			return null;
		}
		Object val = event.getProperty(Datum.SOURCE_ID);
		if ( val != null ) {
			return val.toString().trim();
		}
		val = event.getProperty(Datum.DATUM_PROPERTY);
		if ( val instanceof Datum ) {
			return ((Datum) val).getSourceId();
		}
		return null;
	}

	private Map<String, Object> mapForEvent(FluxFilterConfig[] activeFilters, String sourceId,
			Event event) {
		if ( event == null ) {
			return null;
		}
		String[] propNames = event.getPropertyNames();
		if ( propNames == null || propNames.length < 1 ) {
			return null;
		}
		Map<String, Object> map = new LinkedHashMap<>(propNames.length);
		Pattern exPattern = this.excludePropertyNamesPattern;
		for ( String propName : propNames ) {
			Object propVal = event.getProperty(propName);
			if ( Datum.DATUM_PROPERTY.equals(propName) && (propVal instanceof Datum) ) {
				Datum d = (Datum) propVal;
				if ( d instanceof GeneralDatumSupport ) {
					GeneralDatumSupport gds = (GeneralDatumSupport) d;
					GeneralDatumSamplesTransformService xform = transformServiceForSourceId(
							activeFilters, sourceId);
					if ( xform != null ) {
						GeneralDatumSamples samples = xform.transformSamples(d, gds.getSamples(),
								new HashMap<>(4));
						if ( samples == null ) {
							return null;
						}
						if ( samples != gds.getSamples() ) {
							GeneralDatumSupport newGds = gds.clone();
							newGds.setSamples(samples);
							d = newGds;
						}
					}
				}
				Map<String, ?> datumProps = d.asSimpleMap();
				if ( datumProps != null ) {
					for ( Map.Entry<String, ?> me : datumProps.entrySet() ) {
						String datumPropName = me.getKey();
						if ( exPattern != null && exPattern.matcher(datumPropName).matches() ) {
							// exclude this property
							continue;
						}
						map.put(datumPropName, me.getValue());
					}
				}
				continue;
			} else if ( exPattern != null && exPattern.matcher(propName).matches() ) {
				// exclude this property
				continue;
			} else if ( EventConstants.EVENT_TOPIC.equals(propName) ) {
				// exclude event topic
				continue;
			}
			map.put(propName, propVal);
		}
		return map;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.upload.flux";
	}

	@Override
	public String getDisplayName() {
		return "SolarFlux Upload Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(4);
		results.add(new BasicTitleSettingSpecifier("status", getStatusMessage(), true, true));

		results.add(new BasicTextFieldSettingSpecifier("mqttHost", DEFAULT_MQTT_HOST));
		results.add(new BasicTextFieldSettingSpecifier("mqttUsername", DEFAULT_MQTT_USERNAME));
		results.add(new BasicTextFieldSettingSpecifier("mqttPassword", "", true));
		results.add(new BasicTextFieldSettingSpecifier("excludePropertyNamesRegex",
				DEFAULT_EXCLUDE_PROPERTY_NAMES_PATTERN.pattern()));
		results.add(new BasicTextFieldSettingSpecifier("requiredOperationalMode", ""));

		// drop-down menu for version
		BasicMultiValueSettingSpecifier versionSpec = new BasicMultiValueSettingSpecifier("mqttVersion",
				DEFAULT_MQTT_VERSION.name());
		Map<String, String> versionTitles = new LinkedHashMap<String, String>(4);
		versionTitles.put(MqttVersion.Mqtt311.name(), "3.1.1");
		versionTitles.put(MqttVersion.Mqtt5.name(), "5");
		versionSpec.setValueTitles(versionTitles);
		results.add(versionSpec);

		results.add(new BasicToggleSettingSpecifier("wireLogging", false));

		// filter list
		FluxFilterConfig[] confs = getFilters();
		List<FluxFilterConfig> confsList = (confs != null ? asList(confs) : emptyList());
		results.add(dynamicListSettingSpecifier("filters", confsList,
				new SettingsUtil.KeyedListCallback<FluxFilterConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(FluxFilterConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								value.getSettingSpecifiers(key + "."));
						return singletonList(configGroup);
					}
				}));

		return results;
	}

	private String getStatusMessage() {
		final MqttConnection conn = connection();
		final boolean connected = (conn != null ? conn.isEstablished() : false);
		String connMsg = getMessageSource().getMessage(
				format("status.%s", connected ? "connected" : "disconnected"), null,
				Locale.getDefault());
		final MqttStats s = getMqttStats();
		// @formatter:off
		return getMessageSource().getMessage("status.msg",
				new Object[] { 
						connMsg, 
						s.get(MqttStats.BasicCounts.MessagesDelivered),
						s.get(MqttStats.BasicCounts.PayloadBytesDelivered) },
				Locale.getDefault());
		// @formatter:on
	}

	@Override
	public String getPingTestName() {
		return getDisplayName();
	}

	/**
	 * Set the MQTT host to use.
	 * 
	 * <p>
	 * This accepts a URI syntax, e.g. {@literal mqtts://host:port}.
	 * </p>
	 * 
	 * @param mqttHost
	 *        the MQTT host to use
	 */
	public void setMqttHost(String mqttHost) {
		getMqttConfig().setServerUriValue(mqttHost);
	}

	/**
	 * Set the MQTT username to use.
	 * 
	 * @param mqttUsername
	 *        the username
	 */
	public void setMqttUsername(String mqttUsername) {
		getMqttConfig().setUsername(mqttUsername);
	}

	/**
	 * Set the MQTT password to use.
	 * 
	 * @param mqttPassword
	 *        the password, or {@literal null} for no password
	 */
	public void setMqttPassword(String mqttPassword) {
		getMqttConfig().setPassword(mqttPassword);
	}

	/**
	 * Set the MQTT version to use.
	 * 
	 * @param mqttVersion
	 *        the version, or {@literal null} for a default version
	 * @since 1.8
	 */
	public void setMqttVersion(MqttVersion mqttVersion) {
		getMqttConfig().setVersion(mqttVersion != null ? mqttVersion : DEFAULT_MQTT_VERSION);
	}

	/**
	 * Set an operational mode that must be active for a connection to SolarFlux
	 * to be established.
	 * 
	 * @param requiredOperationalMode
	 *        the mode to require, or {@literal null} to enable by default
	 */
	public void setRequiredOperationalMode(String requiredOperationalMode) {
		if ( requiredOperationalMode == this.requiredOperationalMode
				|| (this.requiredOperationalMode != null
						&& this.requiredOperationalMode.equals(requiredOperationalMode)) ) {
			return;
		}
		this.requiredOperationalMode = requiredOperationalMode;
		Runnable task = new Runnable() {

			@Override
			public void run() {
				MqttConnection client = connection();
				if ( requiredOperationalMode == null || requiredOperationalMode.isEmpty() ) {
					if ( client == null ) {
						// start up client now
						startup();
					}
				} else if ( client != null ) {
					if ( opModesService == null
							|| !opModesService.isOperationalModeActive(requiredOperationalMode) ) {
						// shut down client, op mode no longer active
						shutdown();
					}
				}
			}
		};
		Executor e = this.executor;
		if ( e != null ) {
			e.execute(task);
		} else {
			task.run();
		}
	}

	/**
	 * Set an executor to use for internal tasks.
	 * 
	 * @param executor
	 *        the executor
	 * @since 1.3
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Set the operational modes service to use.
	 * 
	 * @param opModesService
	 *        the service to use
	 * @since 1.1
	 */
	public void setOpModesService(OperationalModesService opModesService) {
		this.opModesService = opModesService;
	}

	/**
	 * Set a regular expression of pattern names to exclude from posting to
	 * SolarFlux.
	 * 
	 * <p>
	 * You can use this to exclude internal properties with a regular expression
	 * like <code>_.*</code>. The pattern will automatically use
	 * case-insensitive matching.
	 * </p>
	 * 
	 * @param regex
	 *        the regular expression or {@literal null} for no filter; pattern
	 *        syntax errors are ignored and result in no regular expression
	 *        being used
	 * @since 1.1
	 */
	public void setExcludePropertyNamesRegex(String regex) {
		Pattern p = null;
		try {
			if ( regex != null && !regex.isEmpty() ) {
				p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			}
		} catch ( PatternSyntaxException e ) {
			// ignore
		}
		this.excludePropertyNamesPattern = p;
	}

	/**
	 * Get a list of filter configurations to apply to datum.
	 * 
	 * @return the filters to apply, or {@literal null}
	 * @since 1.4
	 */
	public FluxFilterConfig[] getFilters() {
		return filters;
	}

	/**
	 * Set a list of filter configurations to apply to datum.
	 * 
	 * <p>
	 * These filters are applied in array order.
	 * </p>
	 * 
	 * @param filters
	 *        the filters to apply, or {@literal null}
	 * @since 1.4
	 */
	public void setFilters(FluxFilterConfig[] filters) {
		this.filters = filters;
	}

	/**
	 * Get the number of configured {@code filters} elements.
	 * 
	 * @return The number of {@code filters} elements.
	 * @since 1.4
	 */
	public int getFiltersCount() {
		FluxFilterConfig[] list = getFilters();
		return (list == null ? 0 : list.length);
	}

	/**
	 * Adjust the number of configured {@code filters} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link FluxFilterConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code filters} elements.
	 * @since 1.4
	 */
	public void setFiltersCount(int count) {
		this.filters = ArrayUtils.arrayWithLength(this.filters, count, FluxFilterConfig.class, null);
	}

	/**
	 * Get the "include version tag" toggle.
	 * 
	 * @return {@literal true} to include the {@literal _v} version tag with
	 *         each datum; defaults to {@link #DEFAULT_INCLUDE_VERSION_TAG}
	 * @since 1.5
	 */
	public boolean isIncludeVersionTag() {
		return includeVersionTag;
	}

	/**
	 * Set the "inclue version tag" toggle.
	 * 
	 * @param includeVersionTag
	 *        {@literal true} to include the {@link #TAG_VERSION} property with
	 *        each datum; only disable if you can be sure that all receivers of
	 *        SolarFlux messages interpret the data in the same way
	 * @since 1.5
	 */
	public void setIncludeVersionTag(boolean includeVersionTag) {
		this.includeVersionTag = includeVersionTag;
	}

	/**
	 * Get the encoder services to use for MQTT messages.
	 * 
	 * @return the encoder services
	 * @since 1.7
	 */
	public OptionalServiceCollection<ObjectEncoder> getDatumEncoders() {
		return datumEncoders;
	}

	/**
	 * Set the available encoder services to use for MQTT messages.
	 * 
	 * @param datumEncoders
	 *        the encoders to set
	 * @since 1.7
	 */
	public void setDatumEncoders(OptionalServiceCollection<ObjectEncoder> datumEncoders) {
		this.datumEncoders = datumEncoders;
	}

	/**
	 * Toggle the wire logging mode.
	 * 
	 * @param wireLogging
	 *        {@literal true} to enable wire logging
	 * @since 1.8
	 */
	public void setWireLogging(boolean wireLogging) {
		getMqttConfig().setWireLoggingEnabled(wireLogging);
	}

	/**
	 * Get the transform services to use for MQTT messages.
	 * 
	 * @return the transform services
	 * @since 1.9
	 */
	public OptionalServiceCollection<GeneralDatumSamplesTransformService> getTransformServices() {
		return transformServices;
	}

	/**
	 * Set the available transform services to use for MQTT messages.
	 * 
	 * @param transformServices
	 *        the services to set
	 * @since 1.9
	 */
	public void setTransformServices(
			OptionalServiceCollection<GeneralDatumSamplesTransformService> transformServices) {
		this.transformServices = transformServices;
	}

	/**
	 * Get the DAO to use for offline MQTT message persistence.
	 * 
	 * @return the DAO to use for offline MQTT messages
	 * @since 1.10
	 */
	public OptionalService<MqttMessageDao> getMqttMessageDao() {
		return mqttMessageDao;
	}

	/**
	 * Set the DAO to use for offline MQTT message persistence.
	 * 
	 * @param mqttMessageDao
	 *        the DAO to use for offline MQTT messages
	 * @since 1.10
	 */
	public void setMqttMessageDao(OptionalService<MqttMessageDao> mqttMessageDao) {
		this.mqttMessageDao = mqttMessageDao;
	}

}
