/* ==================================================================
 * KcdConfigurer.java - 2/10/2019 8:00:19 pm
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

package net.solarnetwork.node.datum.canbus;

import static java.util.Arrays.asList;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.io.canbus.KcdParser;
import net.solarnetwork.node.io.canbus.kcd.BusType;
import net.solarnetwork.node.io.canbus.kcd.MessageType;
import net.solarnetwork.node.io.canbus.kcd.NetworkDefinitionType;
import net.solarnetwork.node.io.canbus.kcd.NodeRefType;
import net.solarnetwork.node.io.canbus.kcd.NodeType;
import net.solarnetwork.node.io.canbus.kcd.ProducerType;
import net.solarnetwork.node.io.canbus.kcd.SignalType;
import net.solarnetwork.node.io.canbus.kcd.ValueType;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.node.settings.support.BasicFileSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Service that can configure {@link CanbusDatumDataSource} instances via a KCD
 * resource.
 * 
 * @author matt
 * @version 1.0
 */
public class KcdConfigurer extends BaseIdentifiable
		implements SettingSpecifierProvider, SettingResourceHandler {

	/** The setting resource key for a KCD file. */
	public static final String RESOURCE_KEY_KCD_FILE = "kcdFile";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OptionalService<KcdParser> kcdParser;
	private final SettingsService settingsService;

	private String settingProviderId = CanbusDatumDataSource.SETTING_UID;
	private String settingPrefix = "";

	private Throwable lastKcdFileException = null;
	private List<String> lastKcdFileMessages = null;

	/**
	 * Constructor.
	 * 
	 * @param kcdParser
	 *        the KCD parser service to use
	 * @param settingsService
	 *        the settings service to use
	 */
	public KcdConfigurer(OptionalService<KcdParser> kcdParser, SettingsService settingsService) {
		super();
		this.kcdParser = kcdParser;
		this.settingsService = settingsService;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.canbus.KcdConfigurer";
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return "CAN Bus Datum Data Source KCD Configurer";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(3);
		results.add(new BasicTitleSettingSpecifier("lastKcdException",
				lastKcdFileException != null ? lastKcdFileException.toString() : "N/A", true));
		results.add(new BasicTitleSettingSpecifier("lastKcdMessages",
				lastKcdFileMessages != null && !lastKcdFileMessages.isEmpty()
						? StringUtils.delimitedStringFromCollection(lastKcdFileMessages, "\n")
						: "N/A",
				true));

		results.add(new BasicFileSettingSpecifier(RESOURCE_KEY_KCD_FILE, null,
				new LinkedHashSet<>(
						asList(".xml", ".kcd", ".xml.gz", ".kcd.gz", "text/xml", "text/xml+gzip")),
				false));

		return results;
	}

	@Override
	public Iterable<Resource> currentSettingResources(String settingKey) {
		return null;
	}

	@Override
	public synchronized SettingsUpdates applySettingResources(String settingKey,
			Iterable<Resource> resources) throws IOException {
		KcdParser parser = (kcdParser != null ? kcdParser.service() : null);
		if ( resources == null || parser == null ) {
			return null;
		}
		if ( RESOURCE_KEY_KCD_FILE.equals(settingKey) ) {
			for ( Resource r : resources ) {
				try {
					this.lastKcdFileException = null;
					this.lastKcdFileMessages = null;
					NetworkDefinitionType kcd = parser.parseKcd(r.getInputStream(), true);
					// only accept first file here
					return settingsCommand(kcd, Locale.getDefault());
				} catch ( Exception e ) {
					log.error("Exception parsing KCD file {}", r, e);
					this.lastKcdFileException = e;
				}
			}
		} else {
			log.warn("Ignoring setting resource key [{}]", settingKey);
		}
		return null;
	}

	private static class DatumDataSourceConfig {

		private final String networkUid;
		private final String sourceId;
		private String busName;
		private final List<CanbusMessageConfig> messageConfigs;

		private DatumDataSourceConfig(String networkUid, String sourceId) {
			super();
			this.networkUid = networkUid;
			this.sourceId = sourceId;
			this.messageConfigs = new ArrayList<>(16);
		}

	}

	private SettingsUpdates settingsCommand(NetworkDefinitionType kcd, Locale locale) {
		if ( kcd == null ) {
			return null;
		}

		// remove all factory instances so start anew
		for ( String instanceId : settingsService.getProvidersForFactory(settingProviderId).keySet() ) {
			settingsService.deleteProviderFactoryInstance(settingProviderId, instanceId);
		}

		List<NodeType> nodes = kcd.getNode();
		if ( nodes == null || nodes.isEmpty() ) {
			return null;
		}

		// build up mapping of node ID -> node and source ID -> node, eliminating any duplicate source ID mappings
		Map<String, NodeType> nodeMap = new LinkedHashMap<>(nodes.size());

		// a mapping of source ID -> data source config
		Map<String, DatumDataSourceConfig> sourceMessageConfigMap = new LinkedHashMap<>(nodeMap.size());

		List<String> parseMessages = new ArrayList<>();

		for ( NodeType node : nodes ) {
			if ( node.getSourceId() == null || node.getSourceId().isEmpty() ) {
				continue;
			}
			if ( node.getNetworkServiceName() == null || node.getNetworkServiceName().isEmpty() ) {
				continue;
			}
			if ( sourceMessageConfigMap.putIfAbsent(node.getSourceId(), new DatumDataSourceConfig(
					node.getNetworkServiceName(), node.getSourceId())) == null ) {
				nodeMap.put(node.getId(), node);
			} else {
				parseMessages.add(getMessageSource().getMessage("node.duplicateSourceId",
						new Object[] { node.getId(), node.getSourceId() }, locale));
				log.warn("Ignoring node {} with duplicate source ID {}", node.getId(),
						node.getSourceId());
			}
		}

		List<BusType> buses = kcd.getBus();
		if ( buses != null && !buses.isEmpty() ) {
			for ( BusType bus : buses ) {
				log.info("Process KCD bus [{}] for messages...", bus.getName());
				List<MessageType> messages = bus.getMessage();
				if ( messages != null && !messages.isEmpty() ) {
					for ( MessageType message : messages ) {
						if ( message.getId() == null || message.getId().isEmpty() ) {
							continue;
						}
						int address;
						try {
							if ( message.getId().startsWith("0x") ) {
								address = Integer.parseInt(message.getId().substring(2), 16);
							} else {
								address = Integer.parseInt(message.getId());
							}
						} catch ( NumberFormatException e ) {
							parseMessages.add(getMessageSource().getMessage("message.badId",
									new Object[] { bus.getName(), message.getId() }, locale));
							log.error(
									"Unable to parse Bus [{}] Message ID value [{}] as base-16 integer, skipping: {}",
									bus.getName(), message.getId(), e.toString());
							continue;
						}

						ProducerType producer = message.getProducer();
						List<NodeRefType> nodeRefs = (producer != null ? producer.getNodeRef() : null);
						NodeType node = (nodeRefs != null
								? nodeRefs.stream().map(r -> r.getId())
										.filter(s -> s != null && nodeMap.containsKey(s))
										.map(s -> nodeMap.get(s)).findFirst().orElse(null)
								: null);
						if ( node == null ) {
							parseMessages.add(getMessageSource().getMessage("message.noProducer",
									new Object[] { bus.getName(), message.getId() }, locale));
							log.warn("Bus [{}] Message [{}] has no producer node reference, skipping.",
									bus.getName(), message.getId());
							continue;
						}

						DatumDataSourceConfig dsConfig = sourceMessageConfigMap.get(node.getSourceId());
						if ( dsConfig.busName == null ) {
							// take bus name from first message for this data source
							dsConfig.busName = bus.getName();
						}
						List<CanbusMessageConfig> msgConfigs = dsConfig.messageConfigs;

						CanbusMessageConfig msgConfig = new CanbusMessageConfig();
						msgConfig.setAddress(address);
						msgConfig.setName(message.getName());
						msgConfig.setInterval(message.getInterval());

						List<SignalType> signals = message.getSignal();
						List<CanbusPropertyConfig> propConfigs = new ArrayList<>();
						for ( SignalType signal : signals ) {
							// always set this; assuming all the same but if not the last message value wins
							msgConfig.setByteOrdering("little".equalsIgnoreCase(signal.getEndianess())
									? ByteOrdering.LittleEndian
									: ByteOrdering.BigEndian);
							if ( signal.getDatumProperty() == null
									|| signal.getDatumProperty().isEmpty() ) {
								parseMessages.add(getMessageSource().getMessage("signal.noDatumProperty",
										new Object[] { bus.getName(), message.getId(),
												signal.getName() },
										locale));
								log.info(
										"Bus [{}] Message [{}] Signal [{}] has no datum property name; skipping",
										bus.getName(), message.getId(), signal.getName());
								continue;
							}
							GeneralDatumSamplesType datumPropType = GeneralDatumSamplesType.Instantaneous;
							if ( signal.getDatumPropertyClassification() != null
									&& !signal.getDatumPropertyClassification().isEmpty() ) {
								try {
									datumPropType = GeneralDatumSamplesType
											.valueOf(signal.getDatumPropertyClassification().charAt(0));
								} catch ( IllegalArgumentException e ) {
									parseMessages.add(getMessageSource().getMessage(
											"signal.badDatumPropertyClass",
											new Object[] { bus.getName(), message.getId(),
													signal.getName(),
													signal.getDatumPropertyClassification() },
											locale));
									log.info(
											"Bus [{}] Message [{}] Signal [{}] has invalid datum property classification [{}]; skipping",
											bus.getName(), message.getId(), signal.getName(),
											signal.getDatumPropertyClassification());
									continue;
								}
							}

							CanbusPropertyConfig propConfig = new CanbusPropertyConfig(
									signal.getDatumProperty(), datumPropType, signal.getOffset());
							propConfig.setBitLength(signal.getLength());

							ValueType val = signal.getValue();
							if ( val == null ) {
								parseMessages
										.add(getMessageSource()
												.getMessage(
														"signal.noValue", new Object[] { bus.getName(),
																message.getId(), signal.getName() },
														locale));
								log.info("Bus [{}] Message [{}] Signal [{}] has no Value; skipping",
										bus.getName(), message.getId(), signal.getName());
								continue;
							}

							propConfig.setSlope(bigDecimalValue(val.getSlope()));
							propConfig.setIntercept(bigDecimalValue(val.getIntercept()));
							propConfig.setUnit(val.getUnit());
							propConfig.setNormalizedUnit(val.getNormalizedUnit());
							propConfig.setDecimalScale(signal.getDecimalScale());
							if ( "signed".equalsIgnoreCase(val.getType()) ) {
								switch (propConfig.getBitLength()) {
									case 8:
										propConfig.setDataType(BitDataType.Int8);
										break;

									case 16:
										propConfig.setDataType(BitDataType.Int16);
										break;

									case 32:
										propConfig.setDataType(BitDataType.Int32);
										break;

									case 64:
										propConfig.setDataType(BitDataType.Int64);
										break;

									default:
										propConfig.setDataType(BitDataType.Integer);
										break;
								}
							} else if ( "unsigned".equalsIgnoreCase(val.getType()) ) {
								switch (propConfig.getBitLength()) {
									case 1:
										propConfig.setDataType(BitDataType.Bit);
										break;

									case 8:
										propConfig.setDataType(BitDataType.UInt8);
										break;

									case 16:
										propConfig.setDataType(BitDataType.UInt16);
										break;

									case 32:
										propConfig.setDataType(BitDataType.UInt32);
										break;

									case 64:
										propConfig.setDataType(BitDataType.UInt64);
										break;

									default:
										propConfig.setDataType(BitDataType.UnsignedInteger);
										break;
								}
							} else if ( "single".equalsIgnoreCase(val.getType()) ) {
								propConfig.setDataType(BitDataType.Float32);
							} else if ( "double".equalsIgnoreCase(val.getType()) ) {
								propConfig.setDataType(BitDataType.Float64);
							}

							KeyValuePair[] names = signal.getLocalizedName().stream()
									.map(n -> new KeyValuePair(n.getLang(), n.getValue()))
									.toArray(KeyValuePair[]::new);
							if ( names.length > 0 ) {
								propConfig.setLocalizedNames(names);
							}

							propConfigs.add(propConfig);
						}
						if ( propConfigs.isEmpty() ) {
							parseMessages.add(getMessageSource().getMessage("message.noSignals",
									new Object[] { bus.getName(), message.getId() }, locale));
							log.warn("Bus [{}] Message [{}] has no signals, skipping", bus.getName(),
									message.getId());
							continue;
						}

						msgConfig.setPropConfigs(
								propConfigs.toArray(new CanbusPropertyConfig[propConfigs.size()]));
						msgConfigs.add(msgConfig);
					}
				}
			}
		}

		this.lastKcdFileMessages = (parseMessages.isEmpty() ? null : parseMessages);

		List<SettingValueBean> settings = new ArrayList<>(32);

		for ( Map.Entry<String, DatumDataSourceConfig> me : sourceMessageConfigMap.entrySet() ) {
			final DatumDataSourceConfig dsConfig = me.getValue();
			if ( dsConfig.busName == null || dsConfig.sourceId == null || dsConfig.networkUid == null ) {
				// can happen if a Node has no associated Message/Signal elements
				continue;
			}
			final String instanceId = settingsService.addProviderFactoryInstance(settingProviderId);
			settings.add(
					setting(instanceId, "canbusNetwork.propertyFilters['UID']", dsConfig.networkUid));
			settings.add(setting(instanceId, "busName", dsConfig.busName));
			settings.add(setting(instanceId, "sourceId", dsConfig.sourceId));
			settings.add(setting(instanceId, "msgConfigsCount",
					String.valueOf(dsConfig.messageConfigs.size())));
			int idx = 0;
			for ( CanbusMessageConfig msgConfig : dsConfig.messageConfigs ) {
				settings.addAll(msgConfig.toSettingValues(settingProviderId, instanceId,
						settingPrefix + "msgConfigs[" + idx + "]."));
				idx++;
			}
		}

		SettingsCommand cmd = new SettingsCommand(settings, asList(Pattern.compile(".*")));
		return cmd;
	}

	private BigDecimal bigDecimalValue(Number n) {
		BigDecimal value = NumberUtils.bigDecimalForNumber(n);
		if ( BigDecimal.ZERO.setScale(value.scale()).equals(value) ) {
			value = BigDecimal.ZERO;
		}
		return value;
	}

	private SettingValueBean setting(String instanceId, String key, String value) {
		return new SettingValueBean(settingProviderId, instanceId, settingPrefix + key, value);
	}

	/**
	 * Get any exception generated via the last call to
	 * {@link #applySettingResources(String, Iterable)}.
	 * 
	 * @return the last exception, or {@literal null}
	 */
	public synchronized Throwable getLastKcdFileException() {
		return lastKcdFileException;
	}

	/**
	 * Get the messages generated via the last call to
	 * {@link #applySettingResources(String, Iterable)}.
	 * 
	 * @return the messages, or {@literal null}
	 */
	public synchronized List<String> getLastKcdFileMessages() {
		return lastKcdFileMessages;
	}

	/**
	 * Get the setting provider ID.
	 * 
	 * @return the provider ID; defaults to
	 *         {@link CanbusDatumDataSource#SETTING_UID}
	 */
	public String getSettingProviderId() {
		return settingProviderId;
	}

	/**
	 * Set the setting provider ID.
	 * 
	 * @param settingProviderId
	 *        the provider ID to use
	 * @throws IllegalArgumentException
	 *         if {@code settingProviderId} is {@literal null}
	 */
	public void setSettingProviderId(String settingProviderId) {
		if ( settingProviderId == null ) {
			throw new IllegalArgumentException("The factory ID must not be null.");
		}
		this.settingProviderId = settingProviderId;
	}

	/**
	 * Get the setting key prefix.
	 * 
	 * <p>
	 * This value is added as a prefix to all generated setting keys.
	 * </p>
	 * 
	 * @return the setting key prefix; defaults to an empty string
	 */
	public String getSettingPrefix() {
		return settingPrefix;
	}

	/**
	 * Set the setting key prefix.
	 * 
	 * @param settingPrefix
	 *        the prefix to use
	 */
	public void setSettingPrefix(String settingPrefix) {
		this.settingPrefix = (settingPrefix != null ? settingPrefix : "");
	}

}
