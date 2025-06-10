/* ==================================================================
 * SocketcandCanbusNetwork.java - 19/09/2019 4:13:04 pm
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

package net.solarnetwork.node.io.canbus.socketcand;

import static net.solarnetwork.util.StringUtils.expandTemplateString;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.support.AbstractCanbusNetwork;
import net.solarnetwork.node.io.canbus.support.LoggingCanbusFrameListener;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.settings.MappableSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * CAN bus network implementation using the socketcand server protocol.
 *
 * @author matt
 * @version 2.2
 * @see <a href=
 *      "https://github.com/linux-can/socketcand">linux-can/socketcand</a>
 */
public class SocketcandCanbusNetwork extends AbstractCanbusNetwork
		implements SettingSpecifierProvider, InstructionHandler {

	/** The default {@code host} value. */
	public static final String DEFAULT_HOST = "localhost";

	/** The default {@code port} value. */
	public static final int DEFAULT_PORT = 29536;

	/** The default {@code uid} value. */
	public static final String DEFAULT_UID = "Canbus Port";

	/**
	 * The {@code captureLogPath} property default value.
	 *
	 * @since 2.1
	 */
	public static final String DEFAULT_CAPTURE_LOG_PATH = "var/log/canbus-{busName}-{date}.log.gz";

	/**
	 * The {@code captureInlineDate} property default value.
	 *
	 * @since 2.1
	 */
	public static final boolean DEFAULT_CAPTURE_INLINE_DATE = true;

	/**
	 * The {@code captureLogGzip} property default value.
	 *
	 * @since 2.1
	 */
	public static final boolean DEFAULT_CAPTURE_LOG_GZIP = true;

	/**
	 * A {@literal Signal} instruction parameter for capturing all CAN bus
	 * messages to a log file.
	 *
	 * <p>
	 * The parameter value must be the CAN bus name to capture.
	 * </p>
	 *
	 * @since 2.1
	 */
	public static final String CANBUS_CAPTURE_SIGNAL_PARAM = "canbus-capture";

	/**
	 * A {@literal Signal} instruction parameter for the capture action to
	 * perform.
	 *
	 * <p>
	 * The parameter value must be a {@link CanbusCaptureAction} name
	 * (case-insensitive).
	 * </p>
	 *
	 * @since 2.1
	 */
	public static final String CANBUS_CAPTURE_ACTION_PARAM = "action";

	/**
	 * A {@literal Signal} instruction parameter for a duration after which
	 * capturing should automatically stop.
	 *
	 * <p>
	 * The parameter value must be a valid ISO-8601 duration, for example
	 * {@literal PT15M} for 15 minutes.
	 * </p>
	 *
	 * @since 2.1
	 */
	public static final String CANBUS_CAPTURE_DURATION_PARAM = "duration";

	/**
	 * The capture log file name placeholder for the CAN bus name.
	 *
	 * @since 2.1
	 */
	public static final String CANBUS_CAPTURE_BUS_NAME_PLACEHOLDER = "busName";

	/**
	 * The capture log file name for the creation date.
	 *
	 * @since 2.1
	 */
	public static final String CANBUS_CAPTURE_DATE_PLACEHOLDER = "date";

	/**
	 * The capture log file date placeholder format.
	 *
	 * @since 2.1
	 */
	public static final DateTimeFormatter CAPTURE_DATE_PLACEHOLDER_FORMATTER;
	static {
		// @formatter:off
		CAPTURE_DATE_PLACEHOLDER_FORMATTER = DateTimeFormatter
				.ofPattern("yyyy-MM-dd-HHmmss")
				.withChronology(IsoChronology.INSTANCE)
				.withZone(ZoneOffset.UTC);
		// @formatter:on
	}

	/**
	 * Enumeration of CAN bus capture actions.
	 *
	 * @author matt
	 * @since 2.1
	 */
	public static enum CanbusCaptureAction {

		/** Start capturing CAN bus frames. */
		Start,

		/** Stop capturing CAN bus frames. */
		Stop;

		/**
		 * Get an enum value for a string.
		 *
		 * @param value
		 *        the string value to get the enum value for; case insensitive
		 *        matches against enum names
		 * @return the matching enum value, or {@literal null} if not matching
		 */
		public static CanbusCaptureAction actionFor(String value) {
			if ( value == null || value.isEmpty() ) {
				return null;
			}
			switch (value.toLowerCase()) {
				case "start":
					return Start;
				case "stop":
					return Stop;
				default:
					return null;
			}
		}
	}

	private final class CaptureCanbusConnection implements Runnable {

		private final String busName;
		private final CanbusConnection connection;
		private final ScheduledFuture<?> stopFuture;

		private CaptureCanbusConnection(String busName, CanbusConnection conn, Instant stop) {
			super();
			this.busName = busName;
			this.connection = conn;
			final TaskScheduler taskScheduler = getTaskScheduler();
			if ( stop != null && taskScheduler != null ) {
				log.info("Scheduling CAN bus [{}] capture to stop at {}", this.busName, stop);
				this.stopFuture = taskScheduler.schedule(this, stop);
			} else {
				this.stopFuture = null;
			}
		}

		@Override
		public void run() {
			try {
				Map<String, String> resultParameters = new HashMap<>(2);
				boolean success = handleStopCapture(busName, resultParameters);
				if ( !success ) {
					log.warn("Failed to stop CAN bus [{}] capture: {}", busName, resultParameters);
				}
			} catch ( IOException e ) {
				log.warn("Communication error stopping CAN bus [{}] capture: {}", busName, e.toString());
			}
		}
	}

	private final ConcurrentMap<String, CaptureCanbusConnection> captureConnections = new ConcurrentHashMap<>(
			5, 0.9f, 1);

	private final CanbusSocketProvider socketProvider;
	private final Executor executor;
	private String host = DEFAULT_HOST;
	private int port = DEFAULT_PORT;

	private String captureLogPath = DEFAULT_CAPTURE_LOG_PATH;
	private boolean captureInlineDate = DEFAULT_CAPTURE_INLINE_DATE;
	private boolean captureLogGzip = DEFAULT_CAPTURE_LOG_GZIP;
	private TaskScheduler taskScheduler;

	/**
	 * Constructor.
	 *
	 * @param socketProvider
	 *        the socket provider to use
	 * @param executor
	 *        an executor to use for connection management tasks
	 * @throws IllegalArgumentException
	 *         if {@code socketProvider} is {@literal null}
	 */
	public SocketcandCanbusNetwork(CanbusSocketProvider socketProvider, Executor executor) {
		super();
		if ( socketProvider == null ) {
			throw new IllegalArgumentException("The socket provider must be provided.");
		}
		this.socketProvider = socketProvider;
		this.executor = executor;
	}

	@Override
	public String getDisplayName() {
		return "TCP CAN bus";
	}

	@Override
	protected String getNetworkDescription() {
		return host + ":" + port;
	}

	@Override
	protected CanbusConnection createConnectionInternal(String busName) {
		final String host = getHost();
		final int port = getPort();
		if ( host == null || host.trim().isEmpty() || port < 1 ) {
			log.info("CAN bus network missing host/port configuration); cannot create connection.");
			return null;
		}
		return new SocketcandCanbusConnection(socketProvider, executor, host, port, busName);
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.canbus.tcp";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(12);
		results.addAll(basicIdentifiableSettings("", "", ""));
		results.add(new BasicTextFieldSettingSpecifier("host", DEFAULT_HOST));
		results.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(DEFAULT_PORT)));

		if ( socketProvider instanceof SettingSpecifierProvider ) {
			List<SettingSpecifier> socketSettings = ((SettingSpecifierProvider) socketProvider)
					.getSettingSpecifiers();
			if ( socketSettings != null ) {
				for ( SettingSpecifier setting : socketSettings ) {
					if ( setting instanceof MappableSpecifier ) {
						results.add(((MappableSpecifier) setting).mappedTo("socketProvider."));
					} else {
						results.add(setting);
					}
				}
			}
		}

		results.add(new BasicTextFieldSettingSpecifier("captureLogPath", DEFAULT_CAPTURE_LOG_PATH));
		results.add(new BasicToggleSettingSpecifier("captureLogGzip", DEFAULT_CAPTURE_LOG_GZIP));
		results.add(new BasicToggleSettingSpecifier("captureInlineDate", DEFAULT_CAPTURE_INLINE_DATE));

		return results;
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SIGNAL.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		final String busName = instruction.getParameterValue(CANBUS_CAPTURE_SIGNAL_PARAM);
		if ( busName == null || busName.isEmpty() ) {
			return null;
		}
		final CanbusCaptureAction action = CanbusCaptureAction
				.actionFor(instruction.getParameterValue(CANBUS_CAPTURE_ACTION_PARAM));
		boolean success = false;
		Map<String, String> resultParameters = new LinkedHashMap<>(2);
		if ( action == null ) {
			success = false;
			resultParameters.put(InstructionHandler.PARAM_MESSAGE, String
					.format("Missing required instruction parameter [%s]", CANBUS_CAPTURE_ACTION_PARAM));
		} else {
			try {
				switch (action) {
					case Start:
						success = handleStartCapture(busName, resultParameters,
								instruction.getInstructionDate(),
								parseCaptureStopDate(instruction.getInstructionDate(),
										instruction.getParameterValue(CANBUS_CAPTURE_DURATION_PARAM),
										resultParameters));
						break;
					case Stop:
						success = handleStopCapture(busName, resultParameters);
						break;
					default:
						success = false;
						resultParameters.put(InstructionHandler.PARAM_MESSAGE,
								String.format("Unsupported capture action: %s", action));
				}
			} catch ( IOException e ) {
				success = false;
				resultParameters.put(InstructionHandler.PARAM_MESSAGE,
						String.format("Error configuring capture: %s", e.toString()));
			}
		}
		return InstructionUtils.createStatus(instruction,
				success ? InstructionState.Completed : InstructionState.Declined, resultParameters);
	}

	private Instant parseCaptureStopDate(Instant instructionDate, String durationValue,
			Map<String, String> resultParameters) {
		if ( durationValue == null || durationValue.isEmpty() ) {
			return null;
		}
		try {
			Duration duration = Duration.parse(durationValue);
			if ( instructionDate == null ) {
				instructionDate = Instant.now();
			}
			return instructionDate.plus(duration);
		} catch ( DateTimeParseException e ) {
			resultParameters.put(InstructionHandler.PARAM_MESSAGE,
					String.format("Ignoring parameter [%s] invalid value: %s",
							CANBUS_CAPTURE_DURATION_PARAM, e.getMessage()));
		}
		return null;
	}

	private boolean handleStartCapture(String busName, Map<String, String> resultParameters,
			Instant date, Instant stopDate) throws IOException {
		CaptureCanbusConnection captureConn = captureConnections.computeIfAbsent(busName, k -> {
			return new CaptureCanbusConnection(busName, createConnectionInternal(busName), stopDate);
		});
		CanbusConnection conn = captureConn.connection;
		try {
			synchronized ( conn ) {
				if ( !conn.isEstablished() ) {
					conn.open();
				}
				if ( conn.isMonitoring() ) {
					resultParameters.put(InstructionHandler.PARAM_MESSAGE, "Already capturing.");
					return false;
				}
				Map<String, String> nameParameters = new HashMap<>(2);
				nameParameters.put(CANBUS_CAPTURE_BUS_NAME_PLACEHOLDER, busName);
				nameParameters.put(CANBUS_CAPTURE_DATE_PLACEHOLDER,
						CAPTURE_DATE_PLACEHOLDER_FORMATTER.format(date != null ? date : Instant.now()));
				Path logPath = Paths.get(expandTemplateString(captureLogPath, nameParameters));
				LoggingCanbusFrameListener listener = new LoggingCanbusFrameListener(busName, logPath,
						captureInlineDate, false, captureLogGzip);
				conn.monitor(listener);
				return true;
			}
		} catch ( IOException e ) {
			if ( captureConn.stopFuture != null ) {
				captureConn.stopFuture.cancel(true);
			}
			throw e;
		}
	}

	private boolean handleStopCapture(String busName, Map<String, String> resultParameters)
			throws IOException {
		CaptureCanbusConnection captureConn = captureConnections.remove(busName);
		if ( captureConn == null ) {
			resultParameters.put(InstructionHandler.PARAM_MESSAGE, "Not currently capturing.");
			return false;
		}
		captureConn.connection.close();
		if ( captureConn.stopFuture != null ) {
			captureConn.stopFuture.cancel(false);
		}
		return true;
	}

	/**
	 * Get an existing connection used for capturing.
	 *
	 * @param busName
	 *        the CAN bus name to get the capturing connection for
	 * @return the connection, or {@literal null} if no capturing connection has
	 *         been established for {@code busName}
	 * @since 2.1
	 */
	public CanbusConnection capturingConnection(String busName) {
		final CaptureCanbusConnection captureConn = captureConnections.get(busName);
		return (captureConn != null ? captureConn.connection : null);
	}

	// Accessors

	/**
	 * Get the host to connect to.
	 *
	 * @return the host; defaults to {@link #DEFAULT_HOST}
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the host to connect to.
	 *
	 * @param host
	 *        the host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Get the port to connect to.
	 *
	 * @return the port; defaults to {@link #DEFAULT_PORT}
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the port to connect to.
	 *
	 * @param port
	 *        the port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Get the socket provider.
	 *
	 * @return the socket provider
	 * @since 2.1
	 */
	public CanbusSocketProvider getSocketProvider() {
		return socketProvider;
	}

	/**
	 * Get the debug mode log path.
	 *
	 * @return the debug log path; defaults to {@link #DEFAULT_CAPTURE_LOG_PATH}
	 * @since 2.1
	 */
	public String getCaptureLogPath() {
		return captureLogPath;
	}

	/**
	 * Set the debug mode log path.
	 *
	 * @param captureLogPath
	 *        the log path to set, including one string parameter for the bus
	 *        name
	 * @since 2.1
	 */
	public void setCaptureLogPath(String captureLogPath) {
		this.captureLogPath = captureLogPath;
	}

	/**
	 * Get the debug "inline date" flag.
	 *
	 * @return {@literal true} if a time stamp should be included in the debug
	 *         log output as the first field of each frame line
	 * @since 2.1
	 */
	public boolean isCaptureInlineDate() {
		return captureInlineDate;
	}

	/**
	 * Set the debug "include date" flag.
	 *
	 * @param captureInlineDate
	 *        {@literal true} if a time stamp should be included in the debug
	 *        log output as the first field of each frame line, {@literal false}
	 *        to include the date as a separate comment line
	 * @since 2.1
	 */
	public void setCaptureInlineDate(boolean captureInlineDate) {
		this.captureInlineDate = captureInlineDate;
	}

	/**
	 * Get the task scheduler.
	 *
	 * @return the task scheduler
	 * @since 2.1
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 *
	 * @param taskScheduler
	 *        the task scheduler
	 * @since 2.1
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the toggle setting for using gzip on the capture log file.
	 *
	 * @return {@literal true} if the capture log output should be compressed
	 *         with gzip
	 * @since 2.1
	 */
	public boolean isCaptureLogGzip() {
		return captureLogGzip;
	}

	/**
	 * Set the toggle setting for using gzip on the capture log file.
	 *
	 * @param captureLogGzip
	 *        {@literal true} if the capture log output should be compressed
	 *        with gzip
	 * @since 2.1
	 */
	public void setCaptureLogGzip(boolean captureLogGzip) {
		this.captureLogGzip = captureLogGzip;
	}

}
