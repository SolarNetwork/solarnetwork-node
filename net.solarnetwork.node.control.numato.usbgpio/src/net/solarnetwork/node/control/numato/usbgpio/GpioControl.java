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
import static net.solarnetwork.node.control.numato.usbgpio.GpioPropertyConfig.ioDirectionBitSet;
import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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
import net.solarnetwork.node.domain.DataAccessor;
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
import net.solarnetwork.settings.SettingsChangeObserver;
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
public class GpioControl extends SerialDeviceSupport implements SettingSpecifierProvider,
		NodeControlProvider, InstructionHandler, SettingsChangeObserver {

	/** The {@code serialNetworkUid} property default value. */
	public static final String DEFAULT_SERIAL_NETWORK_UID = "Serial Port";

	private GpioPropertyConfig[] propConfigs;
	private Function<SerialConnection, GpioService> serviceProvider = UsbGpioService::new;
	private CachedGpioService gpioService;

	/**
	 * Constructor.
	 * 
	 * @param serialNetwork
	 *        the network to use
	 */
	public GpioControl(OptionalFilterableService<SerialNetwork> serialNetwork) {
		super();
		super.setSerialNetwork(serialNetwork);
		setDisplayName("Numato USB GPIO Control");
		setSerialNetworkUid(DEFAULT_SERIAL_NETWORK_UID);
	}

	/**
	 * Call once after properties configured.
	 */
	public void startup() {
		configurationChanged(null);
	}

	/**
	 * Call when no longer needed.
	 */
	public void shutdown() {
		clearGpioService();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		try {
			gpioService();
		} catch ( IOException e ) {
			clearGpioService();
			log.error("Communication error opening GPIO serial port [{}]: {}", getSerialNetworkUid(),
					e.getMessage());
		} catch ( Exception e ) {
			clearGpioService();
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			log.error("Error opening GPIO serial port [{}]: {}", getSerialNetworkUid(), root.toString(),
					root);
		}
	}

	private static final class CachedGpioService {

		private final String serialNetworkUid;
		private final WeakReference<GpioService> gpioService;
		private final SerialConnection conn;
		private BitSet configuredDirection;

		private CachedGpioService(String serialNetworkUid, WeakReference<GpioService> gpioService,
				SerialConnection conn) {
			super();
			if ( serialNetworkUid == null || gpioService == null || conn == null ) {
				throw new IllegalArgumentException("No argumnet may be null.");
			}
			this.serialNetworkUid = serialNetworkUid;
			this.gpioService = gpioService;
			this.conn = conn;
		}
	}

	private synchronized GpioService gpioService() throws IOException {
		String serialNetworkUid = getSerialNetworkUid();
		if ( serialNetworkUid == null ) {
			clearGpioService();
			return null;
		}
		final CachedGpioService cached = this.gpioService;
		if ( cached != null && !serialNetworkUid.equals(cached.serialNetworkUid) ) {
			// serial network UID changed, must clear existing connection
			clearGpioService();
		}
		GpioService result = (cached != null ? cached.gpioService.get() : null);
		if ( result == null ) {
			SerialNetwork serial = service(getSerialNetwork());
			if ( serial != null ) {
				SerialConnection conn = null;
				try {
					conn = serial.createConnection();
					conn.open();
					result = serviceProvider.apply(conn);
				} catch ( IOException e ) {
					// close connection in case opened
					if ( conn != null ) {
						conn.close();
					}
					throw e;
				}
				gpioService = new CachedGpioService(serialNetworkUid, new WeakReference<>(result), conn);
			}
		}
		// check if direction has been set or needs to change
		setupIoDirection(ioDirectionBitSet(propConfigs), gpioService);
		return result;
	}

	private synchronized void clearGpioService() {
		if ( gpioService != null && gpioService.conn != null ) {
			try {
				gpioService.conn.close();
			} catch ( Exception e ) {
				// ignore
			}
		}
		gpioService = null;
	}

	private synchronized void setupIoDirection(final BitSet dirs, final CachedGpioService cached) {
		if ( cached == null ) {
			return;
		}
		BitSet configuredDirection = cached.configuredDirection;
		if ( dirs == null ) {
			cached.configuredDirection = null;
			return;
		}
		if ( dirs.equals(configuredDirection) ) {
			// unchanged, nothing to do
			return;
		}
		GpioService gpio = cached.gpioService.get();
		if ( gpio == null ) {
			return;
		}
		if ( log.isInfoEnabled() ) {
			log.info("Configuring GPIO direction on [{}] as inputs: {}", getSerialNetworkUid(), dirs);
		}
		try {
			gpio.configureIoDirection(dirs);
			cached.configuredDirection = dirs;
		} catch ( IOException e ) {
			log.error("Communication error setting GPIO direction on [{}]: {}", getSerialNetworkUid(),
					e.getMessage());
		}
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

	// override this because of cached serial connection
	@Override
	protected synchronized <T> T performAction(final SerialConnectionAction<T> action)
			throws IOException {
		final CachedGpioService cached = this.gpioService;
		if ( cached == null ) {
			return super.performAction(action);
		}
		try {
			return action.doWithConnection(cached.conn);
		} catch ( IOException e ) {
			clearGpioService();
			throw e;
		}
	}

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) throws IOException {
		GpioService gpio = serviceProvider.apply(conn);
		Map<String, Object> result = new LinkedHashMap<>(2);
		String id = gpio.getId();
		if ( id != null ) {
			result.put(DataAccessor.INFO_KEY_DEVICE_NAME, id);
		}
		String v = gpio.getDeviceVersion();
		if ( v != null ) {
			result.put(DataAccessor.INFO_KEY_DEVICE_MODEL, v);
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
				.map(c -> resolvePlaceholders(c.getControlId())).collect(toList());
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
			GpioService gpio = gpioService();
			if ( gpio != null ) {
				result = currentValue(config, gpio);
			}
		} catch ( IOException e ) {
			clearGpioService();
			log.error("Communication error reading control [{}] status on [{}]: {}", controlId,
					getSerialNetworkUid(), e.getMessage());
		} catch ( Exception e ) {
			clearGpioService();
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			log.error("Error reading control [{}] status on [{}]: {}", controlId, getSerialNetworkUid(),
					root.toString(), root);
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
				Map<String, ?> statusMap = d.getSampleData(DatumSamplesType.Status);
				if ( statusMap != null && statusMap.isEmpty() ) {
					d.setSampleData(DatumSamplesType.Status, null);
				}
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
			if ( controlId.equals(resolvePlaceholders(config.getControlId())) ) {
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
