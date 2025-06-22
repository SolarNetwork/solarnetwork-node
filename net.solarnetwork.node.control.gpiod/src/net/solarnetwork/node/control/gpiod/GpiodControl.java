/* ==================================================================
 * GpiodControl.java - 1/06/2023 10:35:30 am
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

package net.solarnetwork.node.control.gpiod;

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.scheduling.TaskScheduler;
import io.dvlopt.linux.gpio.GpioBuffer;
import io.dvlopt.linux.gpio.GpioDevice;
import io.dvlopt.linux.gpio.GpioEvent;
import io.dvlopt.linux.gpio.GpioEventHandle;
import io.dvlopt.linux.gpio.GpioEventRequest;
import io.dvlopt.linux.gpio.GpioEventWatcher;
import io.dvlopt.linux.gpio.GpioFlags;
import io.dvlopt.linux.gpio.GpioHandle;
import io.dvlopt.linux.gpio.GpioHandleRequest;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Datum data source for the Linux gpiod library.
 *
 * @author matt
 * @version 1.1
 */
public class GpiodControl extends BaseIdentifiable implements NodeControlProvider, InstructionHandler,
		SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver {

	/** The {@code configureDelay} property default value. */
	public static final long DEFAULT_CONFIGURE_DELAY = 15000L;

	private final OptionalService<EventAdmin> eventAdmin;
	private final TaskScheduler taskScheduler;
	private long configureDelay = DEFAULT_CONFIGURE_DELAY;
	private String gpioDevice;
	private GpiodPropertyConfig[] propConfigs;

	private final ConcurrentMap<String, SimpleNodeControlInfoDatum> controlStates = new ConcurrentHashMap<>(
			4, 0.9f, 2);
	private GpiodEventThread gpioThread;
	private ScheduledFuture<?> configureFuture;

	/**
	 * Constructor.
	 *
	 * @param eventAdmin
	 *        the event admin
	 * @param taskScheduler
	 *        the task scheduler
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public GpiodControl(OptionalService<EventAdmin> eventAdmin, TaskScheduler taskScheduler) {
		super();
		this.eventAdmin = requireNonNullArgument(eventAdmin, "eventAdmin");
		this.taskScheduler = requireNonNullArgument(taskScheduler, "taskScheduler");
	}

	@Override
	public synchronized void serviceDidStartup() {
		scheduleConfigurationTask();
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( configureFuture != null && !configureFuture.isDone() ) {
			configureFuture.cancel(false);
			configureFuture = null;
		}
		stopGpioThread();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		stopGpioThread();
		scheduleConfigurationTask();
	}

	private synchronized void stopGpioThread() {
		if ( gpioThread != null && gpioThread.isAlive() ) {
			try {
				gpioThread.close();
			} catch ( IOException e ) {
				log.debug("Communication error closing GPIO device [{}]: {}", gpioThread.deviceName,
						e.toString());
			}
			gpioThread = null;
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.gpiod";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(8);

		results.addAll(baseIdentifiableSettings(""));
		results.add(new BasicTextFieldSettingSpecifier("gpioDevice", null));

		final GpiodPropertyConfig[] confs = getPropConfigs();
		List<GpiodPropertyConfig> confsList = (confs != null ? Arrays.asList(confs)
				: Collections.emptyList());
		results.add(SettingUtils.dynamicListSettingSpecifier("propConfigs", confsList,
				new SettingUtils.KeyedListCallback<GpiodPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(GpiodPropertyConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								GpiodPropertyConfig.settings(key + ".", getMessageSource(),
										Locale.getDefault()));
						return Collections.singletonList(configGroup);
					}
				}));

		return results;
	}

	@Override
	public List<String> getAvailableControlIds() {
		GpiodPropertyConfig[] configs = getPropConfigs();
		if ( configs == null || configs.length < 1 ) {
			return Collections.emptyList();
		}
		return new ArrayList<>(Arrays.stream(configs).filter(GpiodPropertyConfig::isValid)
				.map(c -> resolvePlaceholders(c.getControlId()))
				.collect(Collectors.toCollection(LinkedHashSet::new)));
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		GpiodPropertyConfig config = configForControlId(controlId);
		if ( config == null ) {
			return null;
		}
		// read the control's current status
		log.debug("Reading control [{}] status", controlId);
		SimpleNodeControlInfoDatum result = controlStates.get(controlId);
		if ( result != null ) {
			postControlEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, result);
		} else if ( config.getGpioDirection() == GpioDirection.Output ) {
			// for output, init to 0
			result = createDatum(config, false);
		}
		return result;
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		for ( String paramName : instruction.getParameterNames() ) {
			GpiodPropertyConfig config = configForControlId(paramName);
			if ( config != null ) {
				if ( config.getGpioDirection() != GpioDirection.Output ) {
					return InstructionUtils.createStatus(instruction, InstructionState.Declined,
							InstructionUtils.createErrorResultParameters(
									"The GPIO direction is not Output; cannot change.", "GPIOD.0001"));
				}
				try {
					String paramVal = instruction.getParameterValue(paramName);
					boolean active = StringUtils.parseBoolean(paramVal);
					writeGpioValue(config, active);
					return InstructionUtils.createStatus(instruction, InstructionState.Completed);
				} catch ( IOException e ) {
					return InstructionUtils.createStatus(instruction, InstructionState.Declined,
							InstructionUtils.createErrorResultParameters(
									"Communication error changing GPIO value: " + e.getMessage(),
									"GPIOD.0001"));
				} catch ( Exception e ) {
					return InstructionUtils.createStatus(instruction, InstructionState.Declined,
							InstructionUtils.createErrorResultParameters(
									"Error changing GPIO value: " + e.getMessage(), "GPIOD.0002"));
				}
			}
		}
		return null;
	}

	private void postControlEvent(String topic, SimpleNodeControlInfoDatum info) {
		Event event = DatumEvents.datumEvent(topic, info);
		postEvent(event);
	}

	private void postEvent(Event event) {
		if ( event == null ) {
			return;
		}
		final EventAdmin ea = service(eventAdmin);
		if ( ea == null ) {
			return;
		}
		ea.postEvent(event);
	}

	private SimpleNodeControlInfoDatum createDatum(GpiodPropertyConfig config, boolean active) {
		// @formatter:off
		NodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(resolvePlaceholders(config.getControlId()))
				.withType(NodeControlPropertyType.Boolean)
				.withPropertyName(config.getPropertyKey())
				.withReadonly(config.getGpioDirection() == GpioDirection.Input)
				.withValue(String.valueOf(active))
				.build();
		// @formatter:on
		SimpleNodeControlInfoDatum d = new SimpleNodeControlInfoDatum(info, Instant.now());
		if ( config.getPropertyType() != null && config.getPropertyType() != DatumSamplesType.Status ) {
			// move the property from Status to whatever the config says, as a 0 or 1
			String propName = config.getPropertyKey() != null ? config.getPropertyKey()
					: SimpleNodeControlInfoDatum.DEFAULT_PROPERTY_NAME;
			d.putSampleValue(config.getPropertyType(), propName, active ? 1 : 0);
			d.putSampleValue(DatumSamplesType.Status, propName, null);
			Map<String, ?> statusMap = d.getSampleData(DatumSamplesType.Status);
			if ( statusMap != null && statusMap.isEmpty() ) {
				d.setSampleData(DatumSamplesType.Status, null);
			}
		}
		controlStates.put(d.getControlId(), d);
		postControlEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, d);

		return d;
	}

	private GpiodPropertyConfig configForControlId(String controlId) {
		GpiodPropertyConfig[] configs = getPropConfigs();
		if ( controlId == null || configs == null || configs.length < 1 ) {
			return null;
		}
		for ( GpiodPropertyConfig config : configs ) {
			if ( controlId.equals(resolvePlaceholders(config.getControlId())) ) {
				return config;
			}
		}
		return null;
	}

	private synchronized void scheduleConfigurationTask() {
		if ( configureFuture != null && !configureFuture.isDone() ) {
			configureFuture.cancel(false);
			configureFuture = null;
		}
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					synchronized ( GpiodControl.this ) {
						setupWatcher();
						configureFuture = null;
					}
				} catch ( Exception e ) {
					log.warn("Error configuring GPIO control {}: {}", displayName(), e.toString());
					scheduleConfigurationTask();
				}
			}
		};
		log.info("Scheduling GPIO control [{}] initialization for {}ms from now.", displayName(),
				configureDelay);
		if ( taskScheduler != null ) {
			configureFuture = taskScheduler.schedule(r,
					Instant.ofEpochMilli(System.currentTimeMillis() + configureDelay));
		} else {
			r.run();
		}
	}

	private String displayName() {
		String n = getUid();
		if ( n == null || n.isEmpty() ) {
			n = getAvailableControlIds().stream().collect(Collectors.joining(","));
			if ( n == null ) {
				n = toString();
			}
		}
		return n;
	}

	private synchronized void writeGpioValue(GpiodPropertyConfig config, boolean active)
			throws IOException {
		GpioHandleRequest request = new GpioHandleRequest().setConsumer("solarnode")
				.setFlags(new GpioFlags().setOutput());
		request.addLine(config.getAddress(), active);

		log.info("Settig control [{}] value on GPIO device [{}] address {} to {}",
				resolvePlaceholders(config.getControlId()), gpioDevice, config.getAddress(), active);

		@SuppressWarnings("resource")
		GpioDevice device = (gpioThread != null ? gpioThread.device : null);
		if ( device == null ) {
			try (GpioDevice d = new GpioDevice(gpioDevice);
					GpioHandle handle = d.requestHandle(request)) {
				// the value will have been set via the request default value
			}
		} else {
			try (GpioHandle handle = device.requestHandle(request)) {
				// the value will have been set via the request default value
			}
		}
		createDatum(config, active);
	}

	private synchronized void setupWatcher() throws IOException {
		final String deviceName = getGpioDevice();
		if ( deviceName == null || deviceName.trim().isEmpty() ) {
			log.info("GPIO device [{}] not configured.", displayName());
			return;
		}
		final GpiodPropertyConfig[] configs = validInputPropConfigs();
		if ( configs == null || configs.length < 1 ) {
			log.info("GPIO device [{}] does not have any valid controls configured.", displayName());
			return;
		}
		gpioThread = new GpiodEventThread(deviceName, configs);
		gpioThread.start();
	}

	private GpiodPropertyConfig[] validInputPropConfigs() {
		final GpiodPropertyConfig[] propConfigs = getPropConfigs();
		if ( propConfigs == null || propConfigs.length < 1 ) {
			return null;
		}
		Map<Integer, GpiodPropertyConfig> addressMap = new HashMap<>(propConfigs.length);
		Map<String, GpiodPropertyConfig> controlMap = new HashMap<>(propConfigs.length);
		GpiodPropertyConfig[] valid = Arrays.stream(propConfigs).filter(c -> {
			// check for duplicate GPIO address values
			GpiodPropertyConfig conflict = addressMap.get(c.getAddress());
			if ( conflict != null ) {
				log.error(
						"GPIO device [{}] address {} already configured on control [{}]; cannot configure control [{}] to use that address as well",
						gpioDevice, c.getAddress(), resolvePlaceholders(conflict.getControlId()),
						resolvePlaceholders(c.getControlId()));
				return false;
			}
			addressMap.put(c.getAddress(), c);

			// check for duplicate control ID values
			String controlId = c.getControlId();
			if ( controlId != null )
				conflict = controlMap.get(c.getControlId());
			if ( conflict != null ) {
				log.error("GPIO device [{}] control ID [{}] already configured on another control",
						gpioDevice, controlId);
				return false;
			}
			controlMap.put(controlId, c);

			return c.isValid() && c.getGpioDirection() == GpioDirection.Input;
		}).toArray(GpiodPropertyConfig[]::new);
		if ( valid.length < 1 ) {
			return null;
		}
		return valid;
	}

	private final class GpiodEventThread extends Thread {

		private final String deviceName;
		private final GpiodPropertyConfig[] configs;
		private final List<GpioEventHandle> handles;
		private final int timeout;
		private GpioDevice device;
		private GpioEventWatcher watcher;
		private boolean keepGoing;

		private GpiodEventThread(String deviceName, GpiodPropertyConfig[] configs) throws IOException {
			super("GPIO-Control-" + requireNonNullArgument(deviceName, "deviceName"));
			this.configs = configs;
			this.deviceName = deviceName;
			setDaemon(true);
			this.handles = new ArrayList<>(configs.length);
			this.timeout = (int) (configureDelay / 2);
			this.keepGoing = true;
		}

		private void close() throws IOException {
			this.keepGoing = false;
		}

		@Override
		public void run() {
			log.info("Starting GPIO processing for device [{}]", deviceName);
			try {
				while ( keepGoing ) {
					final Number deviceNum = StringUtils.numberValue(deviceName);
					try (GpioDevice device = (deviceNum != null ? new GpioDevice(deviceNum.intValue())
							: new GpioDevice(deviceName))) {
						this.device = device;
						GpioEventWatcher watcher = new GpioEventWatcher();
						this.watcher = watcher;
						try {
							setupWatcher(device);
							processEvents(watcher);
						} finally {
							for ( GpioEventHandle handle : handles ) {
								try {
									handle.close();
								} catch ( IOException e ) {
									log.warn("Error closing GPIO device [{}] handle [{}]: {}",
											deviceName, handle, e.toString());
								}
							}
							watcher.close();
							this.device = null;
						}
					} catch ( IOException e ) {
						log.debug("Communication error in GPIO processing for device [{}]: {}",
								deviceName, e.toString());
					}
					if ( keepGoing ) {
						// pause and try to open GPIO again
						try {
							Thread.sleep(configureDelay);
						} catch ( InterruptedException e ) {
							// ignore and continue
						}
					}
				}
			} catch ( Exception e ) {
				log.error("Error in GPIO processing for devcie [{}]: {}", deviceName, e.toString(), e);
			} catch ( NoClassDefFoundError e ) {
				log.error("GPIO native library error for devcie [{}]: {}", deviceName, e.toString(), e);
			}
			log.info("Finished GPIO processing for device [{}]", deviceName);
		}

		private void setupWatcher(final GpioDevice device) throws IOException {
			int idx = 0;
			GpioBuffer gpioBuffer = new GpioBuffer();
			for ( GpiodPropertyConfig config : configs ) {
				GpioEventHandle handle = device.requestEvent(new GpioEventRequest(
						config.getAddress().intValue(), config.getGpioEdgeMode().toEdgeDetection()));

				// get initial value
				handle.read(gpioBuffer);
				boolean active = gpioBuffer.get(handle.getLine());
				createDatum(config, active);

				handles.add(handle);

				watcher.addHandle(handle, idx++);
			}
		}

		private void processEvents(final GpioEventWatcher watcher) throws IOException {
			GpioEvent event = new GpioEvent();
			while ( keepGoing ) {
				if ( watcher.waitForEvent(event, timeout) ) {
					int idx = event.getId();
					if ( idx >= configs.length ) {
						continue;
					}
					GpiodPropertyConfig config = configs[idx];
					log.trace("GPIO event on device [{}] property [{}]: {}", deviceName,
							config.getPropertyKey(),
							event.isRising() ? "rising" : event.isFalling() ? "falling" : "other");
					boolean active = event.isRising();
					createDatum(config, active);
				}
			}
		}

	}

	/**
	 * Get configure delay.
	 *
	 * @return the delay, in milliseconds; defaults to
	 *         {@link #DEFAULT_CONFIGURE_DELAY}
	 */
	public long getConfigureDelay() {
		return configureDelay;
	}

	/**
	 * Set the configure delay.
	 *
	 * @param configureDelay
	 *        the delay to set
	 */
	public void setConfigureDelay(long configureDelay) {
		this.configureDelay = configureDelay;
	}

	/**
	 * Get the GPIO device.
	 *
	 * @return the device, either as a full system device path or a number
	 */
	public String getGpioDevice() {
		return gpioDevice;
	}

	/**
	 * Set the GPIO device.
	 *
	 * @param gpioDevice
	 *        the device to set, either as a full system device path or a number
	 */
	public void setGpioDevice(String gpioDevice) {
		this.gpioDevice = gpioDevice;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the property configurations
	 */
	public GpiodPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Get the property configurations to use.
	 *
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(GpiodPropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 *
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		GpiodPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link GpiodPropertyConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count, GpiodPropertyConfig.class,
				null);
	}
}
