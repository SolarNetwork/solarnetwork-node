/* ==================================================================
 * UbsGpioControl.java - 24/09/2021 1:18:27 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.numato.usbgpio;

import static java.util.stream.Collectors.toList;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.osgi.service.event.Event;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.io.serial.support.SerialDeviceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * {@link NodeControlProvider} for the Numato USB GPIO module.
 * 
 * @author matt
 * @version 1.0
 */
public class GpioControl extends SerialDeviceSupport
		implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler {

	/** The {@code serialNetworkUid} property default value. */
	public static final String DEFAULT_SERIAL_NETWORK_UID = "Serial Port";

	private GpioPropertyConfig[] propConfigs;
	private Function<SerialConnection, GpioService> serviceProvider = UsbGpioService::new;

	/**
	 * Constructor.
	 */
	public GpioControl(OptionalFilterableService<SerialNetwork> serialNetwork) {
		super();
		super.setSerialNetwork(serialNetwork);
		setDisplayName("Numato USB GPIO Control");
		setSerialNetworkUid(DEFAULT_SERIAL_NETWORK_UID);
	}

	@Override
	public void setSerialNetwork(OptionalFilterableService<SerialNetwork> serialDevice) {
		// disallow changing from constructed value
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		// TODO
		return null;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) throws IOException {
		GpioService gpio = serviceProvider.apply(conn);
		Map<String, Object> result = new LinkedHashMap<>(2);
		String id = gpio.getId();
		if ( id != null ) {
			result.put(INFO_KEY_DEVICE_NAME, id);
		}
		String v = gpio.getDeviceVersion();
		if ( v != null ) {
			result.put(INFO_KEY_DEVICE_MODEL, v);
		}
		return result;
	}

	@Override
	public List<String> getAvailableControlIds() {
		GpioPropertyConfig[] configs = getPropConfigs();
		if ( configs == null || configs.length < 1 ) {
			return Collections.emptyList();
		}
		return Arrays.stream(configs).filter(GpioPropertyConfig::isValid)
				.map(GpioPropertyConfig::getControlId).collect(toList());
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		GpioPropertyConfig config = configForControlId(controlId);
		if ( config == null ) {
			return null;
		}
		// read the control's current status
		log.debug("Reading control [{}] status", controlId);
		SimpleNodeControlInfoDatum result = null;
		try {
			result = performAction(new SerialConnectionAction<SimpleNodeControlInfoDatum>() {

				@Override
				public SimpleNodeControlInfoDatum doWithConnection(SerialConnection conn)
						throws IOException {
					GpioService gpio = serviceProvider.apply(conn);
					return currentValue(config, gpio);
				}
			});
		} catch ( IOException e ) {
			log.error("Communication error reading control [{}] status: {}", controlId, e.getMessage());

		} catch ( Exception e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			log.error("Error reading control [{}] status: {}", controlId, root.toString(), root);
		}
		if ( result != null ) {
			postControlEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, result);
		}
		return result;
	}

	private void postControlEvent(String topic, SimpleNodeControlInfoDatum info) {
		Event event = DatumEvents.datumEvent(topic, info);
		postEvent(event);
	}

	private SimpleNodeControlInfoDatum currentValue(GpioPropertyConfig config, GpioService gpio)
			throws IOException {
		Integer addr = config.getAddress();
		if ( addr == null || addr.intValue() < 0 ) {
			return null;
		}
		Object value = null;
		if ( config.getGpioType() == GpioType.Analog ) {
			int v = gpio.readAnalog(addr);
			value = config.applyTransformations(v);
		} else {
			value = gpio.read(addr);
		}
		return createDatum(config, value);
	}

	private SimpleNodeControlInfoDatum createDatum(GpioPropertyConfig config, Object value) {
		// @formatter:off
		NodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(resolvePlaceholders(config.getControlId()))
				.withType(config.getGpioType() == GpioType.Analog 
						? NodeControlPropertyType.Float
						: NodeControlPropertyType.Boolean)
				.withPropertyName(config.getPropertyKey())
				.withReadonly(true) // FIXME: add "writable" boolean to config
				.withValue(value != null ? value.toString() : null)
				.build();
		// @formatter:on
		SimpleNodeControlInfoDatum d = new SimpleNodeControlInfoDatum(info, Instant.now());
		if ( config.getPropertyType() != null && config.getPropertyType() != DatumSamplesType.Status ) {
			// move the property from Status to whatever the config says, as long as the value is a number
			String propName = config.getPropertyKey() != null ? config.getPropertyKey()
					: SimpleNodeControlInfoDatum.DEFAULT_PROPERTY_NAME;
			Object o = d.getSampleValue(DatumSamplesType.Status, propName);
			if ( o instanceof Number ) {
				d.putSampleValue(config.getPropertyType(), propName, o);
				d.putSampleValue(DatumSamplesType.Status, propName, null);
			}
		}
		return d;
	}

	private GpioPropertyConfig configForControlId(String controlId) {
		GpioPropertyConfig[] configs = getPropConfigs();
		if ( controlId == null || configs == null || configs.length < 1 ) {
			return null;
		}
		for ( GpioPropertyConfig config : configs ) {
			if ( controlId.equals(config.getControlId()) ) {
				return config;
			}
		}
		return null;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.numato.usbgpio";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTitleSettingSpecifier("info", getDeviceInfoMessage(), true));
		results.addAll(baseIdentifiableSettings(""));
		results.addAll(serialNetworkSettings("", DEFAULT_SERIAL_NETWORK_UID));

		final GpioPropertyConfig[] confs = getPropConfigs();
		List<GpioPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingUtils.KeyedListCallback<GpioPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(GpioPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								GpioPropertyConfig.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return results;
	}

	/**
	 * Get the property configurations.
	 * 
	 * @return the property configurations
	 */
	public GpioPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Get the property configurations to use.
	 * 
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(GpioPropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		GpioPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link GpioPropertyConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count, GpioPropertyConfig.class,
				null);
	}

	/**
	 * Get the {@link GpioService} provider.
	 * 
	 * @return the provider; defaults to {@code UsbGpioService::new}
	 */
	public Function<SerialConnection, GpioService> getServiceProvider() {
		return serviceProvider;
	}

	/**
	 * Set a {@link GpioService} provider.
	 * 
	 * @param serviceProvider
	 *        the provider to set; if {@literal null} then
	 *        {@code UsbGpioService::new} will be used
	 */
	public void setServiceProvider(Function<SerialConnection, GpioService> serviceProvider) {
		if ( serviceProvider == null ) {
			serviceProvider = UsbGpioService::new;
		}
		this.serviceProvider = serviceProvider;
	}

}
