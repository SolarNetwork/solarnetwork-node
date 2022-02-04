/* ==================================================================
 * HttpRequesterJob.java - Jul 20, 2013 5:58:39 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.ping;

import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.SSLService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Make a HTTP request to test for network connectivity, and toggle a control
 * value when connectivity lost.
 * 
 * <p>
 * The idea behind this class is to test for network reachability of a
 * configured HTTP URL. If the URL cannot be reached, the configured control
 * will be set to {@code failedToggleValue}, followed by a pause, followed by
 * setting the control back to the opposite of {@code failedToggleValue}. The
 * control might cycle the power of a mobile modem, for example.
 * </p>
 * 
 * <p>
 * Alternatively, or in addition to to, toggling a control two OS-specific
 * commands can be executed if the URL cannot be reached. The
 * {@code osCommandToggleOff} command will be executed when the URL fails,
 * followed by the configured pause, followed by the {@code osCommandToggleOn}
 * command.
 * </p>
 * 
 * <p>
 * If an {@link OperationalModesService} is configured, then two operational
 * modes can be managed as well, one representing successful network
 * reachability and the other failure. Each time this job runs, the
 * {@link #getSuccessOpMode()} and {@link #getFailOpMode()} modes will be
 * toggled on/off appropriately.
 * </p>
 * 
 * @author matt
 * @version 3.1
 */
public class HttpRequesterJob extends BaseIdentifiable implements JobService, InstructionHandler {

	/**
	 * The {@literal service} instruction parameter value for WiFi
	 * configuration.
	 * 
	 * @since 2.1
	 */
	public static final String PING_SERVICE_NAME = "/setup/network/ping";

	public static final int DEFAULT_OS_COMMAND_SLEEP_SECONDS = 5;

	public static final boolean DEFAULT_FAILED_TOGGLE_VALUE = true;

	public static final int DEFAULT_SLEEP_SECONDS = 5;

	public static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 15;

	public static final String DEFAULT_URL = "http://www.google.com";

	public static final String DEFAULT_SUCCESS_OP_MODE = "net-online";

	public static final String DEFAULT_FAILURE_OP_MODE = "net-offline";

	private String controlId;
	private String osCommandToggleOff;
	private String osCommandToggleOn;
	private int osCommandSleepSeconds = DEFAULT_OS_COMMAND_SLEEP_SECONDS;
	private boolean failedToggleValue = DEFAULT_FAILED_TOGGLE_VALUE;
	private int sleepSeconds = DEFAULT_SLEEP_SECONDS;
	private int connectionTimeoutSeconds = DEFAULT_CONNECTION_TIMEOUT_SECONDS;
	private String url = DEFAULT_URL;
	private MessageSource messageSource;
	private OptionalService<InstructionExecutionService> instructionExecutionService;
	private OptionalService<SSLService> sslService;
	private OptionalService<OperationalModesService> opModesService;
	private String successOpMode = DEFAULT_SUCCESS_OP_MODE;
	private String failOpMode = DEFAULT_FAILURE_OP_MODE;

	@Override
	public void executeJobService() throws Exception {
		InstructionExecutionService instructionService = service(instructionExecutionService);
		if ( instructionService == null ) {
			log.warn("InstructionExecutionService not available, cannot execute ping.");
			return;
		}
		if ( controlId == null && osCommandToggleOff == null && osCommandToggleOn == null
				&& successOpMode == null && failOpMode == null ) {
			log.debug("No control ID or OS commands configured.");
			return;
		}
		if ( ping() ) {
			log.info("Ping {} successful", url);
			toggleOperationalModes(true);
		} else {
			toggleOperationalModes(false);
			handleOSCommand(osCommandToggleOff);
			if ( controlId != null && toggleControl(instructionService,
					failedToggleValue) == InstructionState.Completed ) {
				handleSleep();
				toggleControl(instructionService, !failedToggleValue);
			} else if ( osCommandToggleOn != null ) {
				handleSleep();
			}
			handleOSCommand(osCommandToggleOn);
		}
	}

	private void toggleOperationalModes(boolean success) {
		final String sOpMode = getSuccessOpMode();
		final String fOpMode = getFailOpMode();
		if ( (sOpMode == null || sOpMode.trim().isEmpty())
				&& (fOpMode == null || fOpMode.trim().isEmpty()) ) {
			return;
		}
		OperationalModesService s = service(opModesService);
		if ( s == null ) {
			return;
		}

		String onMode;
		String offMode;
		if ( success ) {
			onMode = sOpMode;
			offMode = fOpMode;
		} else {
			onMode = fOpMode;
			offMode = sOpMode;
		}
		if ( onMode != null && !onMode.isEmpty() ) {
			s.enableOperationalModes(Collections.singleton(onMode));
		}
		if ( offMode != null && !offMode.isEmpty() ) {
			s.disableOperationalModes(Collections.singleton(offMode));
		}
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		final String serviceName = serviceName();
		if ( instruction == null || !handlesTopic(instruction.getTopic())
				|| !serviceName.equals(instruction.getParameterValue(PARAM_SERVICE)) ) {
			return null;
		}
		Map<String, Object> resultParams = new LinkedHashMap<>(3);
		try {
			int responseCode = doPing();
			if ( isResponseCodeOk(responseCode) ) {
				resultParams.put(PARAM_MESSAGE, getMessageSource().getMessage("ping.success.msg",
						new Object[] { url }, "Connection success.", Locale.getDefault()));
			} else {
				resultParams.put(PARAM_MESSAGE,
						getMessageSource().getMessage("ping.errorStatus.msg",
								new Object[] { responseCode, url }, "Error status returned.",
								Locale.getDefault()));
				resultParams.put(PARAM_SERVICE_RESULT, responseCode);
			}
		} catch ( Exception e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			resultParams.put(PARAM_MESSAGE, getMessageSource().getMessage("ping.error.msg",
					new Object[] { url, root.toString() }, "Error connecting.", Locale.getDefault()));
			resultParams.put(PARAM_SERVICE_RESULT, -1);
		}
		return InstructionUtils.createStatus(instruction, InstructionState.Completed, Instant.now(),
				resultParams);
	}

	private String serviceName() {
		String uid = getUid();
		if ( uid != null && !uid.isEmpty() ) {
			return String.format("%s/%s", PING_SERVICE_NAME, uid);
		}
		return PING_SERVICE_NAME;
	}

	private void handleOSCommand(String command) {
		if ( command == null ) {
			return;
		}
		ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
		try {
			Process pr = pb.start();
			logInputStream(pr.getInputStream(), false);
			logInputStream(pr.getErrorStream(), true);
			pr.waitFor();
			if ( pr.exitValue() == 0 ) {
				log.debug("Command [{}] executed", command);
				handleCommandSleep();
			} else {
				log.error("Error executing [{}], exit status: {}", command, pr.exitValue());
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} catch ( InterruptedException e ) {
			throw new RuntimeException(e);
		}
	}

	private void logInputStream(final InputStream src, final boolean errorStream) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Scanner sc = new Scanner(src);
				try {
					while ( sc.hasNextLine() ) {
						if ( errorStream ) {
							log.error(sc.nextLine());
						} else {
							log.info(sc.nextLine());
						}
					}
				} finally {
					sc.close();
				}
			}
		}).start();
	}

	private void handleSleep() {
		if ( sleepSeconds > 0 ) {
			log.info("Sleeping for {} seconds before toggling {} to true", sleepSeconds, controlId);
			try {
				Thread.sleep(sleepSeconds * 1000L);
			} catch ( InterruptedException e ) {
				log.warn("Interrupted while sleeping");
			}
		}
	}

	private void handleCommandSleep() {
		if ( osCommandSleepSeconds > 0 ) {
			log.info("Sleeping for {} seconds before continuing", osCommandSleepSeconds, controlId);
			try {
				Thread.sleep(osCommandSleepSeconds * 1000L);
			} catch ( InterruptedException e ) {
				log.warn("Interrupted while sleeping");
			}
		}
	}

	private boolean ping() {
		log.debug("Attempting to ping {}", url);
		try {
			int responseCode = doPing();
			return isResponseCodeOk(responseCode);
		} catch ( IOException e ) {
			log.info("Error pinging {}: {}", url, e.getMessage());
			return false;
		}
	}

	private static boolean isResponseCodeOk(int responseCode) {
		return (responseCode >= 200 && responseCode < 400);
	}

	private int doPing() throws IOException {
		log.debug("Attempting to ping {}", url);
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setConnectTimeout(connectionTimeoutSeconds * 1000);
		connection.setReadTimeout(connectionTimeoutSeconds * 1000);
		connection.setRequestMethod("HEAD");
		connection.setInstanceFollowRedirects(false);

		if ( sslService != null && connection instanceof HttpsURLConnection ) {
			SSLService service = sslService.service();
			if ( service != null ) {
				SSLSocketFactory factory = service.getSSLSocketFactory();
				if ( factory != null ) {
					HttpsURLConnection sslConnection = (HttpsURLConnection) connection;
					sslConnection.setSSLSocketFactory(factory);
				}
			}
		}

		return connection.getResponseCode();
	}

	private InstructionState toggleControl(final InstructionExecutionService service,
			final boolean value) {
		final Instruction instr = InstructionUtils.createSetControlValueLocalInstruction(controlId,
				String.valueOf(value));
		InstructionStatus result = null;
		try {
			result = service.executeInstruction(instr);
		} catch ( RuntimeException e ) {
			log.error("Exception setting control parameter {} to {}", controlId, value, e);
		}
		if ( result == null ) {
			// nobody handled it!
			result = InstructionUtils.createStatus(instr, InstructionState.Declined);
			log.warn("No handler available to set control {} value", controlId);
		} else if ( result.getInstructionState() == InstructionState.Completed ) {
			log.info("Set {} value to {}", controlId, value);
		} else {
			log.warn("Unable to set {} to {}; result is {}", controlId, value, result);
		}
		return (result != null ? result.getInstructionState() : InstructionState.Declined);
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.ping.http";
	}

	@Override
	public String getDisplayName() {
		return "HTTP Ping";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.addAll(baseIdentifiableSettings(""));
		results.add(new BasicTextFieldSettingSpecifier("url", DEFAULT_URL));
		results.add(new BasicTextFieldSettingSpecifier("controlId", null));
		results.add(new BasicToggleSettingSpecifier("failedToggleValue",
				String.valueOf(DEFAULT_FAILED_TOGGLE_VALUE)));
		results.add(new BasicTextFieldSettingSpecifier("connectionTimeoutSeconds",
				String.valueOf(DEFAULT_CONNECTION_TIMEOUT_SECONDS)));
		results.add(new BasicTextFieldSettingSpecifier("sleepSeconds",
				String.valueOf(DEFAULT_SLEEP_SECONDS)));
		results.add(new BasicTextFieldSettingSpecifier("osCommandToggleOff", null));
		results.add(new BasicTextFieldSettingSpecifier("osCommandToggleOn", null));
		results.add(new BasicTextFieldSettingSpecifier("osCommandSleepSeconds",
				String.valueOf(DEFAULT_OS_COMMAND_SLEEP_SECONDS)));

		results.add(new BasicTextFieldSettingSpecifier("successOpMode", DEFAULT_SUCCESS_OP_MODE));
		results.add(new BasicTextFieldSettingSpecifier("failOpMode", DEFAULT_FAILURE_OP_MODE));

		return results;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Get this class as an instruction handler.
	 * 
	 * <p>
	 * This method exists to support registering this class via the dynamic
	 * service provider API.
	 * </p>
	 * 
	 * @return this instance
	 */
	public InstructionHandler getInstructionHandler() {
		return this;
	}

	/**
	 * Get the ID of the boolean control to toggle.
	 * 
	 * @return the control ID to toggle
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the ID of the boolean control to toggle.
	 * 
	 * @param value
	 *        the control ID to toggle, or {@literal null} to not toggle any
	 *        control
	 */
	public void setControlId(String value) {
		if ( value != null && value.length() < 1 ) {
			value = null;
		}
		this.controlId = value;
	}

	/**
	 * Get the number of seconds to wait after toggling the control to
	 * {@literal false} before toggling the control back to {@literal true}.
	 * 
	 * @return the sleep seconds to set; defaults to
	 *         {@link #DEFAULT_SLEEP_SECONDS}
	 */
	public int getSleepSeconds() {
		return sleepSeconds;
	}

	/**
	 * Set the number of seconds to wait after toggling the control to
	 * {@literal false} before toggling the control back to {@literal true}.
	 * 
	 * @param sleepSeconds
	 *        the sleep seconds to set
	 */
	public void setSleepSeconds(int sleepSeconds) {
		this.sleepSeconds = sleepSeconds;
	}

	/**
	 * Get the URL to attempt to reach.
	 * 
	 * @return the URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL to attempt to reach.
	 * 
	 * <p>
	 * This must be a HTTP URL that accepts {@code HEAD} requests. When this job
	 * executes, it will make a HTTP HEAD request to this URL, and will be
	 * considered successful only if the HTTP response status code is between
	 * <b>200 - 399</b>.
	 * </p>
	 * 
	 * @param url
	 *        the URL
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get the number of seconds to wait for the network connection request to
	 * return a result.
	 * 
	 * @return the connection timeout seconds; defaults to
	 *         {@link #DEFAULT_CONNECTION_TIMEOUT_SECONDS}
	 */
	public int getConnectionTimeoutSeconds() {
		return connectionTimeoutSeconds;
	}

	/**
	 * Set the number of seconds to wait for the network connection request to
	 * return a result.
	 * 
	 * @param connectionTimeout
	 *        the timeout to set
	 */
	public void setConnectionTimeoutSeconds(int connectionTimeout) {
		this.connectionTimeoutSeconds = connectionTimeout;
	}

	/**
	 * Get an OS-specific command to run after the URL cannot be reached.
	 * 
	 * @return the OS command to execute after a reachability failure
	 */
	public String getOsCommandToggleOff() {
		return osCommandToggleOff;
	}

	/**
	 * Set an OS-specific command to run after the URL cannot be reached.
	 * 
	 * @param value
	 *        the OS command to execute after a reachability failure
	 */
	public void setOsCommandToggleOff(String value) {
		if ( value != null && value.length() < 1 ) {
			value = null;
		}
		this.osCommandToggleOff = value;
	}

	/**
	 * Get an OS-specific command to run after the URL was not reached and the
	 * configured pause time has elapsed.
	 * 
	 * @return the OS command to execute after a reachability failure and pause
	 *         time
	 */
	public String getOsCommandToggleOn() {
		return osCommandToggleOn;
	}

	/**
	 * Set an OS-specific command to run after the URL was not reached and the
	 * configured pause time has elapsed.
	 * 
	 * @param value
	 *        the OS command to execute after a reachability failure and pause
	 *        time
	 */
	public void setOsCommandToggleOn(String value) {
		if ( value != null && value.length() < 1 ) {
			value = null;
		}
		this.osCommandToggleOn = value;
	}

	/**
	 * Get the failed toggle flag.
	 * 
	 * @return {@literal true} if upon a reachability failure the configured
	 *         control should be toggled on/off; defaults to
	 *         {@link #DEFAULT_FAILED_TOGGLE_VALUE}
	 */
	public boolean isFailedToggleValue() {
		return failedToggleValue;
	}

	/**
	 * Set the failed toggle flag.
	 * 
	 * @param failedToggleValue
	 *        {@literal true} if upon a reachability failure the configured
	 *        control should be toggled on/off
	 */
	public void setFailedToggleValue(boolean failedToggleValue) {
		this.failedToggleValue = failedToggleValue;
	}

	/**
	 * Get the number of seconds to sleep after successfully executing either
	 * the {@code osCommandToggleOn} or {@code osCommandToggleOff} commands.
	 * 
	 * @return the seconds; defaults to
	 *         {@link #DEFAULT_OS_COMMAND_SLEEP_SECONDS}
	 */
	public int getOsCommandSleepSeconds() {
		return osCommandSleepSeconds;
	}

	/**
	 * Set the number of seconds to sleep after successfully executing either
	 * the {@code osCommandToggleOn} or {@code osCommandToggleOff} commands.
	 * 
	 * @param osCommandSleepSeconds
	 *        the seconds
	 */
	public void setOsCommandSleepSeconds(int osCommandSleepSeconds) {
		this.osCommandSleepSeconds = osCommandSleepSeconds;
	}

	/**
	 * Get the instruction execution service.
	 * 
	 * @return the service
	 */
	public OptionalService<InstructionExecutionService> getInstructionExecutionService() {
		return instructionExecutionService;
	}

	/**
	 * Set the instruction execution service.
	 * 
	 * @param instructionExecutionService
	 *        the service to set
	 */
	public void setInstructionExecutionService(
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		this.instructionExecutionService = instructionExecutionService;
	}

	/**
	 * Get the SSL service.
	 * 
	 * @return the service
	 */
	public OptionalService<SSLService> getSslService() {
		return sslService;
	}

	/**
	 * Set the SSL service.
	 * 
	 * @param sslService
	 *        the service to set
	 */
	public void setSslService(OptionalService<SSLService> sslService) {
		this.sslService = sslService;
	}

	/**
	 * Set the operational modes service.
	 * 
	 * @return the service
	 * @since 3.1
	 */
	public OptionalService<OperationalModesService> getOpModesService() {
		return opModesService;
	}

	/**
	 * Get the operational modes service.
	 * 
	 * @param opModesService
	 *        the service to set
	 * @since 3.1
	 */
	public void setOpModesService(OptionalService<OperationalModesService> opModesService) {
		this.opModesService = opModesService;
	}

	/**
	 * Get the success operational mode to use.
	 * 
	 * @return the mode
	 * @since 3.1
	 */
	public String getSuccessOpMode() {
		return successOpMode;
	}

	/**
	 * Set the success operational mode to use.
	 * 
	 * @param successOpMode
	 *        the mode to set
	 * @since 3.1
	 */
	public void setSuccessOpMode(String successOpMode) {
		this.successOpMode = successOpMode;
	}

	/**
	 * Get the failure operational mode to use.
	 * 
	 * @return the mode
	 * @since 3.1
	 */
	public String getFailOpMode() {
		return failOpMode;
	}

	/**
	 * Set the failure operational mode to use.
	 * 
	 * @param failOpMode
	 *        the mode to set
	 * @since 3.1
	 */
	public void setFailOpMode(String failOpMode) {
		this.failOpMode = failOpMode;
	}

}
