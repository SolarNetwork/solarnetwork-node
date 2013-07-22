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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.InstructionUtils;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Make a HTTP request to test for network connectivity, and toggle a control
 * value when connectivity lost.
 * 
 * <p>
 * The idea behind this class is to test for network reachability of a
 * configured HTTP URL. If the URL cannot be reached, the configured control
 * will be set to <em>false</em>, followed by a pause, followed by setting the
 * control back to <em>true</em>. The control might cycle the power of a mobile
 * modem, for example.
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
 * <dt>sleepSeconds</dt>
 * <dd>The number of seconds to wait after toggling the control to
 * <em>false</em> before toggling the control back to <em>true</em>. Defaults to
 * <b>5</b>.</dd>
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
 * @version 1.0
 */
public class HttpRequesterJob extends AbstractJob implements StatefulJob, SettingSpecifierProvider {

	private static MessageSource MESSAGE_SOURCE;

	private String controlId;
	private int sleepSeconds = 5;
	private int connectionTimeoutSeconds = 15;
	private String url = "http://www.google.com/";

	@Resource(name = "instructionHandlerList")
	private Collection<InstructionHandler> handlers = Collections.emptyList();

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		if ( handlers == null ) {
			log.warn("No configured InstructionHandler collection");
			return;
		}
		if ( controlId == null ) {
			log.debug("No control ID configured.");
			return;
		}
		if ( ping() ) {
			log.info("Ping {} successful", url);
		} else {
			if ( toggleControl(false) == InstructionState.Completed ) {
				if ( sleepSeconds > 0 ) {
					log.info("Sleeping for {} seconds before toggling {} to true", sleepSeconds,
							controlId);
					try {
						Thread.sleep(sleepSeconds * 1000L);
					} catch ( InterruptedException e ) {
						log.warn("Interrupted while sleeping");
					}
				}
				toggleControl(true);
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
			int responseCode = connection.getResponseCode();
			return (responseCode >= 200 && responseCode < 400);
		} catch ( IOException e ) {
			log.info("Error pinging {}: {}", url, e.getMessage());
			return false;
		}
	}

	private InstructionState toggleControl(final boolean value) {
		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER,
				new Date(), Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);

		instr.addParameter(controlId, String.valueOf(value));
		InstructionState result = null;
		try {
			result = InstructionUtils.handleInstruction(handlers, instr);
		} catch ( RuntimeException e ) {
			log.error("Exception setting control parameter {} to {}", controlId, value, e);
		}
		if ( result == null ) {
			// nobody handled it!
			result = InstructionState.Declined;
			log.warn("No InstructionHandler found for control {}", controlId);
		} else if ( result == InstructionState.Completed ) {
			log.info("Set {} value to {}", controlId, value);
		} else {
			log.warn("Unable to set {} to {}; result is {}", controlId, value, result);
		}
		return result;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
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
		results.add(new BasicTextFieldSettingSpecifier("connectionTimeoutSeconds", String
				.valueOf(defaults.connectionTimeoutSeconds)));
		results.add(new BasicTextFieldSettingSpecifier("sleepSeconds", String
				.valueOf(defaults.sleepSeconds)));

		return results;
	}

	@Override
	public MessageSource getMessageSource() {
		if ( MESSAGE_SOURCE == null ) {
			ResourceBundleMessageSource source = new ResourceBundleMessageSource();
			source.setBundleClassLoader(getClass().getClassLoader());
			source.setBasename(getClass().getName());
			MESSAGE_SOURCE = source;
		}
		return MESSAGE_SOURCE;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	public void setSleepSeconds(int sleepSeconds) {
		this.sleepSeconds = sleepSeconds;
	}

	public void setHandlers(Collection<InstructionHandler> handlers) {
		this.handlers = handlers;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setConnectionTimeoutSeconds(int connectionTimeout) {
		this.connectionTimeoutSeconds = connectionTimeout;
	}

}
