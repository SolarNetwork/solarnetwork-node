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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.SSLService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
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
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>controlId</dt>
 * <dd>The ID of the boolean control to toggle.</dd>
 * 
 * <dt>failedToggleValue</dt>
 * <dd>The value to set the configured control to if the ping fails. The
 * opposite value will then be used to toggle the control back again.</dd>
 * 
 * <dt>osCommandToggleOff</dt>
 * <dd>If configured, an OS-specific command to run after the URL cannot be
 * reached.</dd>
 * 
 * <dt>osCommandToggleOn</dt>
 * <dd>If configured, an OS-specific command to run after the URL was not
 * reached and the configured pause time has elapsed.</dd>
 * 
 * <dt>osCommandSleepSeconds</dt>
 * <dd>The number of seconds to sleep after successfully executing either the
 * {@code osCommandToggleOn} or {@code osCommandToggleOff} commands. Defaults to
 * <b>5</b></dd>
 * 
 * <dt>sleepSeconds</dt>
 * <dd>The number of seconds to wait after toggling the control to
 * {@literal false} before toggling the control back to {@literal true}.
 * Defaults to <b>5</b>.</dd>
 * 
 * <dt>connectionTimeoutSeconds</dt>
 * <dd>The number of seconds to wait for the network connection request to
 * return a result. Defaults to <b>15</b>.</dd>
 * 
 * <dt>url</dt>
 * <dd>The URL to "ping". This must be a HTTP URL that accepts {@code HEAD}
 * requests. When this job executes, it will make a HTTP HEAD request to this
 * URL, and will be considered successful only if the HTTP response status code
 * is between <b>200 - 399</b>.</dd>
 * </dl>
 * 
 * @author matt
 * @version 3.0
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class HttpRequesterJob extends AbstractJob implements SettingSpecifierProvider {

	private String controlId;
	private String osCommandToggleOff;
	private String osCommandToggleOn;
	private int osCommandSleepSeconds = 5;
	private boolean failedToggleValue = true;
	private int sleepSeconds = 5;
	private int connectionTimeoutSeconds = 15;
	private String url = "http://www.google.com/";
	private MessageSource messageSource;
	private OptionalService<InstructionExecutionService> instructionExecutionService;
	private OptionalService<SSLService> sslService;

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		InstructionExecutionService instructionService = service(instructionExecutionService);
		if ( instructionService == null ) {
			log.warn("InstructionExecutionService not available, cannot execute ping.");
			return;
		}
		if ( controlId == null && osCommandToggleOff == null && osCommandToggleOn == null ) {
			log.debug("No control ID or OS commands configured.");
			return;
		}
		if ( ping() ) {
			log.info("Ping {} successful", url);
		} else {
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

			int responseCode = connection.getResponseCode();
			return (responseCode >= 200 && responseCode < 400);
		} catch ( IOException e ) {
			log.info("Error pinging {}: {}", url, e.getMessage());
			return false;
		}
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
		HttpRequesterJob defaults = new HttpRequesterJob();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(4);
		results.add(new BasicTextFieldSettingSpecifier("url", defaults.url));
		results.add(new BasicTextFieldSettingSpecifier("controlId", defaults.controlId));
		results.add(new BasicToggleSettingSpecifier("failedToggleValue", defaults.failedToggleValue));
		results.add(new BasicTextFieldSettingSpecifier("connectionTimeoutSeconds",
				String.valueOf(defaults.connectionTimeoutSeconds)));
		results.add(new BasicTextFieldSettingSpecifier("sleepSeconds",
				String.valueOf(defaults.sleepSeconds)));
		results.add(
				new BasicTextFieldSettingSpecifier("osCommandToggleOff", defaults.osCommandToggleOff));
		results.add(new BasicTextFieldSettingSpecifier("osCommandToggleOn", defaults.osCommandToggleOn));
		results.add(new BasicTextFieldSettingSpecifier("osCommandSleepSeconds",
				String.valueOf(defaults.osCommandSleepSeconds)));

		return results;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setControlId(String value) {
		if ( value != null && value.length() < 1 ) {
			value = null;
		}
		this.controlId = value;
	}

	public void setSleepSeconds(int sleepSeconds) {
		this.sleepSeconds = sleepSeconds;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setConnectionTimeoutSeconds(int connectionTimeout) {
		this.connectionTimeoutSeconds = connectionTimeout;
	}

	public void setSslService(OptionalService<SSLService> sslService) {
		this.sslService = sslService;
	}

	public void setOsCommandToggleOff(String value) {
		if ( value != null && value.length() < 1 ) {
			value = null;
		}
		this.osCommandToggleOff = value;
	}

	public void setOsCommandToggleOn(String value) {
		if ( value != null && value.length() < 1 ) {
			value = null;
		}
		this.osCommandToggleOn = value;
	}

	public void setFailedToggleValue(boolean failedToggleValue) {
		this.failedToggleValue = failedToggleValue;
	}

	public void setOsCommandSleepSeconds(int osCommandSleepSeconds) {
		this.osCommandSleepSeconds = osCommandSleepSeconds;
	}

	public void setInstructionExecutionService(
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		this.instructionExecutionService = instructionExecutionService;
	}

}
