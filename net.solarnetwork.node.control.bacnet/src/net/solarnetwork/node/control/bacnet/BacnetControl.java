/* ==================================================================
 * BacnetControl.java - 10/11/2022 8:07:57 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.bacnet;

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.io.bacnet.BacnetConnection;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;
import net.solarnetwork.node.io.bacnet.BacnetNetwork;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Control for BACnet object properties.
 *
 * @author matt
 * @version 1.1
 */
public class BacnetControl extends BaseIdentifiable implements SettingSpecifierProvider,
		SettingsChangeObserver, NodeControlProvider, InstructionHandler {

	/** The setting UID used by this service. */
	public static final String SETTING_UID = "net.solarnetwork.node.control.bacnet";

	/** The {@code connectionCheckFrequency} property default value. */
	public static final long DEFAULT_CONNECTION_CHECK_FREQUENCY = 60_000L;

	/** The {@code reconnectDelay} property default value. */
	public static final long DEFAULT_RECONNECT_DELAY = 10_000L;

	/** The {@code sampleCacheMs} property default value. */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5_000L;

	private final OptionalService<BacnetNetwork> bacnetNetwork;
	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;
	private BacnetWritePropertyConfig[] propConfigs;
	private OptionalService<EventAdmin> eventAdmin;

	private final ConcurrentMap<String, Object> propertyValues = new ConcurrentHashMap<>(8, 0.9f, 2);

	private Map<BacnetDeviceObjectPropertyRef, BacnetWritePropertyConfig> propertyRefs;

	/**
	 * Constructor.
	 *
	 * @param bacnetNetwork
	 *        the network to use
	 */
	public BacnetControl(OptionalService<BacnetNetwork> bacnetNetwork) {
		super();
		this.bacnetNetwork = requireNonNullArgument(bacnetNetwork, "bacnetNetwork");
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		propertyRefs = null;
	}

	@Override
	public List<String> getAvailableControlIds() {
		BacnetWritePropertyConfig[] configs = getPropConfigs();
		if ( configs == null || configs.length < 1 ) {
			return Collections.emptyList();
		}
		return Arrays.stream(configs).filter(BacnetWritePropertyConfig::isValid)
				.map(BacnetWritePropertyConfig::getControlId).collect(Collectors.toList());
	}

	private BacnetWritePropertyConfig configForControlId(String controlId) {
		BacnetWritePropertyConfig[] configs = getPropConfigs();
		if ( controlId == null || configs == null || configs.length < 1 ) {
			return null;
		}
		for ( BacnetWritePropertyConfig config : configs ) {
			if ( controlId.equals(config.getControlId()) ) {
				return config;
			}
		}
		return null;
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		BacnetWritePropertyConfig config = configForControlId(controlId);
		if ( config == null || !config.isValid() ) {
			return null;
		}
		Map<BacnetDeviceObjectPropertyRef, ?> values = null;
		SimpleNodeControlInfoDatum result = null;
		try (BacnetConnection conn = connection()) {
			if ( conn != null ) {
				conn.open();
				values = conn.propertyValues(propertyRefs().keySet());
				// read the control's current status
				log.debug("Reading {} value", controlId);
				try {
					Object val = values.get(config.toRef());
					result = currentValue(config, val);
					if ( result == null ) {
						propertyValues.remove(config.getControlId());
					} else {
						propertyValues.put(config.getControlId(), val);
					}
				} catch ( Exception e ) {
					log.error("Error reading {} value: {}", controlId, e.getMessage());
				}
			}
		} catch ( IOException e ) {
			log.error("Communication problem reading BACnet values for control [{}]: {}", controlId,
					e.toString());
		}
		if ( result != null ) {
			postControlEvent(result, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
		}
		return result;
	}

	private SimpleNodeControlInfoDatum newSimpleNodeControlInfoDatum(BacnetWritePropertyConfig config,
			Object value) {
		// @formatter:off
		NodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(resolvePlaceholders(config.getControlId()))
				.withType(config.getControlPropertyType())
				.withReadonly(false)
				.withValue(value != null ? value.toString() : null)
				.build();
		// @formatter:on
		return new SimpleNodeControlInfoDatum(info, Instant.now());
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		admin.postEvent(event);
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		BacnetWritePropertyConfig[] configs = getPropConfigs();
		if ( !InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(instruction.getTopic())
				|| configs == null || configs.length < 1 ) {
			return null;
		}
		// look for a parameter name that matches a control ID
		for ( String paramName : instruction.getParameterNames() ) {
			log.trace("Got instruction parameter {}", paramName);
			BacnetWritePropertyConfig config = configForControlId(paramName);
			if ( config == null || !config.isValid() ) {
				continue;
			}
			log.debug("Inspecting instruction {} against control {}", instruction.getId(),
					config.getControlId());
			// treat parameter value as a boolean String
			String str = instruction.getParameterValue(paramName);
			Object desiredValue = controlValueForParameterValue(config, str);
			boolean success = false;
			try {
				success = setValue(config, desiredValue);
			} catch ( Exception e ) {
				log.warn("Error handling instruction {} on control {}: {}", instruction.getTopic(),
						config.getControlId(), e.getMessage());
			}
			if ( success ) {
				postControlEvent(newSimpleNodeControlInfoDatum(config, desiredValue),
						NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
				return InstructionUtils.createStatus(instruction, InstructionState.Completed);
			}
			return InstructionUtils.createStatus(instruction, InstructionState.Declined);
		}
		return null;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public String getDisplayName() {
		return "BACnet Control";
	}

	private String sampleMessage() {
		BacnetWritePropertyConfig[] configs = getPropConfigs();
		if ( configs == null || configs.length < 1 || propertyValues.isEmpty() ) {
			return "N/A";
		}
		Locale l = Locale.getDefault();
		MessageSource msgSrc = getMessageSource();
		StringBuilder buf = new StringBuilder();
		buf.append(msgSrc.getMessage("sample.markup.start", null, l));
		for ( Entry<String, Object> e : propertyValues.entrySet() ) {
			buf.append(msgSrc.getMessage("sample.markup.row", new Object[] { e.getKey(), e.getValue() },
					l));
		}
		buf.append(msgSrc.getMessage("sample.markup.end", null, l));
		return buf.toString();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);

		// get current value
		results.add(new BasicTitleSettingSpecifier("sample", sampleMessage(), true, true));

		results.add(new BasicTextFieldSettingSpecifier("uid", null));
		results.add(new BasicTextFieldSettingSpecifier("groupUid", null));
		results.add(new BasicTextFieldSettingSpecifier("bacnetNetworkUid", null, false,
				"(objectClass=net.solarnetwork.node.io.bacnet.BacnetNetwork)"));

		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));

		BacnetWritePropertyConfig[] confs = getPropConfigs();
		List<BacnetWritePropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.<BacnetWritePropertyConfig> emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingUtils.KeyedListCallback<BacnetWritePropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							BacnetWritePropertyConfig value, int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								BacnetWritePropertyConfig.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				}));

		return results;
	}

	private SimpleNodeControlInfoDatum currentValue(BacnetWritePropertyConfig config, Object propVal)
			throws IOException {
		if ( propVal == null ) {
			return null;
		}
		return newSimpleNodeControlInfoDatum(config, extractControlValue(config, propVal));
	}

	private Object extractControlValue(BacnetWritePropertyConfig config, Object propVal) {
		if ( propVal instanceof Number ) {
			if ( config.getUnitMultiplier() != null ) {
				propVal = applyUnitMultiplier((Number) propVal, config.getUnitMultiplier());
			}
			if ( config.getDecimalScale() >= 0 ) {
				propVal = applyDecimalScale((Number) propVal, config.getDecimalScale());
			}
		}
		return propVal;
	}

	private Number applyDecimalScale(Number value, int decimalScale) {
		if ( decimalScale < 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		if ( v.scale() > decimalScale ) {
			v = v.setScale(decimalScale, RoundingMode.HALF_UP);
		}
		return v;
	}

	private Number applyUnitMultiplier(Number value, BigDecimal multiplier) {
		if ( BigDecimal.ONE.compareTo(multiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.multiply(multiplier);
	}

	private Number applyReverseUnitMultiplier(Number value, BigDecimal multiplier) {
		if ( BigDecimal.ONE.compareTo(multiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.divide(multiplier);
	}

	/**
	 * Get the BACNet connection, if available.
	 *
	 * @return the connection, or {@literal null} it not available
	 */
	protected synchronized BacnetConnection connection() {
		final String networkUid = getBacnetNetworkUid();
		if ( networkUid == null ) {
			return null;
		}
		BacnetNetwork network = service(bacnetNetwork);
		if ( network == null ) {
			return null;
		}
		if ( propertyRefs == null ) {
			Map<BacnetDeviceObjectPropertyRef, BacnetWritePropertyConfig> refs = propertyRefs();
			if ( refs != null ) {
				network.setCachePolicy(refs.keySet(), sampleCacheMs);
			}
		}
		BacnetConnection conn = network.createConnection();
		if ( conn != null ) {
			log.debug("BACnet connection created for {}", networkUid);
		}
		return conn;
	}

	private synchronized Map<BacnetDeviceObjectPropertyRef, BacnetWritePropertyConfig> propertyRefs() {
		if ( propertyRefs != null ) {
			return propertyRefs;
		}
		final BacnetWritePropertyConfig[] propConfs = getPropConfigs();
		if ( propConfs == null || propConfs.length < 1 ) {
			Map<BacnetDeviceObjectPropertyRef, BacnetWritePropertyConfig> results = Collections
					.emptyMap();
			propertyRefs = results;
			return results;
		}
		Map<BacnetDeviceObjectPropertyRef, BacnetWritePropertyConfig> results = new HashMap<>();
		for ( BacnetWritePropertyConfig propConf : propConfs ) {
			if ( !propConf.isValid() ) {
				continue;
			}
			results.put(propConf.toRef(), propConf);
		}
		propertyRefs = results;
		return results;
	}

	/**
	 * Set a BACnet property.
	 *
	 * @param config
	 *        the configuration of the property to set
	 * @param desiredValue
	 *        the desired value to set, which should have been returned from
	 *        {@link #controlValueForParameterValue(BacnetWritePropertyConfig, String)}
	 * @return {@literal true} if the write succeeded
	 * @throws IOException
	 *         if an IO error occurs
	 */
	private synchronized boolean setValue(final BacnetWritePropertyConfig config,
			final Object desiredValue) throws IOException {
		log.info("Setting {} value to {}", config.getControlId(), desiredValue);
		try (BacnetConnection conn = connection()) {
			if ( conn != null ) {
				conn.updatePropertyValues(Collections.singletonMap(config.toRef(), desiredValue));
				return true;
			}
		}
		return false;
	}

	private Object controlValueForParameterValue(BacnetWritePropertyConfig config, String str) {
		Object result = null;
		switch (config.getControlPropertyType()) {
			case Boolean:
				result = StringUtils.parseBoolean(str);
				break;

			case Float:
			case Percent:
				result = new BigDecimal(str);
				break;

			case Integer:
				result = new BigInteger(str);
				break;

			case String:
				result = str;
				break;

			default:
				// nothing to do

		}

		if ( result != null ) {
			if ( result instanceof Number && config.getUnitMultiplier() != null ) {
				result = applyReverseUnitMultiplier((Number) result, config.getUnitMultiplier());
			}
			return result;
		}

		log.info("Unsupported property type {} for control {}); cannot extract value",
				config.getControlPropertyType(), config.getControlId());
		return null;
	}

	/**
	 * Get the BacnetNetwork service UID filter value.
	 *
	 * @return the BacnetNetwork UID filter value, if {@code bacnetNetwork} also
	 *         implements {@link FilterableService}
	 */
	public String getBacnetNetworkUid() {
		String uid = FilterableService.filterPropValue(bacnetNetwork, "uid");
		if ( uid != null && uid.trim().isEmpty() ) {
			uid = null;
		}
		return uid;
	}

	/**
	 * Set the BacnetNetwork service UID filter value.
	 *
	 * @param uid
	 *        the BacnetNetwork UID filter value to set, if
	 *        {@code bacnetNetwork} also implements {@link FilterableService}
	 */
	public void setBacnetNetworkUid(String uid) {
		FilterableService.setFilterProp(bacnetNetwork, "uid", uid);
	}

	/**
	 * Get the sample cache maximum age, in milliseconds.
	 *
	 * @return the cache milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 *
	 * @param sampleCacheMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the event admin service.
	 *
	 * @return the event admin
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set the event admin sevice.
	 *
	 * @param eventAdmin
	 *        the service to set
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the property configurations
	 */
	public BacnetWritePropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Get the property configurations to use.
	 *
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(BacnetWritePropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 *
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		BacnetWritePropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link BacnetWritePropertyConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count,
				BacnetWritePropertyConfig.class, null);
	}

}
