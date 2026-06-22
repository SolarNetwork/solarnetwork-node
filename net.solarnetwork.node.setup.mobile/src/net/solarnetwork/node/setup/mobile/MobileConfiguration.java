/* ==================================================================
 * MobileConfiguration.java - 6/06/2026 9:00:00 AM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.mobile;

import static net.solarnetwork.node.Constants.solarNodeHome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * Settings provider and instruction handler for mobile (cellular/4G) network
 * configuration.
 *
 * <p>
 * This service handles the {@link InstructionHandler#TOPIC_SYSTEM_CONFIGURE}
 * instruction topic when the {@link InstructionHandler#PARAM_SERVICE} parameter
 * is {@link #MOBILE_SERVICE_NAME}. The {@link #PARAM_ACTION} parameter selects
 * the operation to perform:
 * </p>
 *
 * <ul>
 * <li>{@code status} (or no action) - return the current mobile connection
 * status</li>
 * <li>{@code reset} - reset the mobile connection</li>
 * <li>{@code restart} - restart the mobile networking service</li>
 * </ul>
 *
 * <p>
 * All operations are delegated to the OS-specific {@code solarcfg} helper
 * script, invoked as {@code solarcfg mobile <action>}. The actual work is
 * implemented by the {@code mobile} service script (for example
 * {@code /usr/share/solarnode/cfg.d/mobile.sh}) provided by an OS support
 * package.
 * </p>
 *
 * @author elijah
 * @version 1.0
 */
public class MobileConfiguration extends BaseIdentifiable
		implements SettingSpecifierProvider, SettingsChangeObserver, InstructionHandler {

	/** The {@code solarcfg} service name for mobile networking. */
	public static final String CONFIG_SERVICE = "mobile";

	/** The default value for the {@code command} property. */
	public static final String DEFAULT_COMMAND = solarNodeHome() + "/bin/solarcfg";

	/**
	 * The {@literal service} instruction parameter value for mobile network
	 * configuration.
	 */
	public static final String MOBILE_SERVICE_NAME = "/setup/network/mobile";

	/** The {@literal action} instruction parameter name. */
	public static final String PARAM_ACTION = "action";

	/** The {@literal action} parameter value to return the current status. */
	public static final String ACTION_STATUS = "status";

	/** The {@literal action} parameter value to reset the connection. */
	public static final String ACTION_RESET = "reset";

	/** The {@literal action} parameter value to restart the service. */
	public static final String ACTION_RESTART = "restart";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String command = DEFAULT_COMMAND;
	private boolean reset = false;

	/**
	 * Constructor.
	 */
	public MobileConfiguration() {
		super();
		setUid("net.solarnetwork.node.setup.mobile.MobileConfiguration");
		setDisplayName("Mobile Network");
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		// the "reset" toggle is transient; when toggled on, perform a reset and
		// then clear the flag so the UI returns to "off" on reload
		if ( reset ) {
			reset = false;
			log.info("Mobile network reset requested via settings");
			try {
				executeAction(ACTION_RESET);
			} catch ( Exception e ) {
				log.warn("Error resetting mobile network: {}", e.getMessage());
			}
		}
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic);
	}

	@Override
	public synchronized InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic())
				|| !MOBILE_SERVICE_NAME.equals(instruction.getParameterValue(PARAM_SERVICE)) ) {
			return null;
		}
		String action = instruction.getParameterValue(PARAM_ACTION);
		if ( action == null || action.isEmpty() ) {
			action = ACTION_STATUS;
		}
		Map<String, Object> resultParams = new LinkedHashMap<>(2);
		InstructionState resultState = InstructionState.Completed;
		try {
			switch (action.toLowerCase(Locale.ENGLISH)) {
				case ACTION_STATUS:
					resultParams.put(PARAM_SERVICE_RESULT, currentStatus());
					break;
				case ACTION_RESET:
				case ACTION_RESTART:
					List<String> result = executeAction(action.toLowerCase(Locale.ENGLISH));
					resultParams.put(PARAM_SERVICE_RESULT, result);
					break;
				default:
					resultParams.put(PARAM_MESSAGE,
							getMessageSource().getMessage("error.unsupportedAction",
									new Object[] { action }, "Unsupported action.",
									Locale.getDefault()));
					resultState = InstructionState.Declined;
			}
		} catch ( Exception e ) {
			resultParams.put(PARAM_MESSAGE, e.toString());
			resultState = InstructionState.Declined;
		}
		return InstructionUtils.createStatus(instruction, resultState, Instant.now(), resultParams);
	}

	@Override
	public String getSettingUid() {
		return getUid();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		final Status status = currentStatus();
		final List<SettingSpecifier> result = new ArrayList<>(2);
		result.add(new BasicTitleSettingSpecifier("status", statusMessage(status)));

		// Only offer the reset action when a modem is actually present, so nodes
		// without a mobile modem do not show a confusing toggle.
		if ( status.present ) {
			result.add(new BasicToggleSettingSpecifier("reset", Boolean.FALSE, true));
		}
		return result;
	}

	private String statusMessage(Status status) {
		MessageSource messageSource = getMessageSource();
		if ( messageSource == null ) {
			return "";
		}
		if ( !status.present ) {
			return messageSource.getMessage("notSupported.label", null, "No mobile modem available",
					Locale.getDefault());
		}
		StringBuilder buf = new StringBuilder();
		if ( status.active ) {
			buf.append(messageSource.getMessage("active.label", null, "Active", Locale.getDefault()));
		} else {
			buf.append(
					messageSource.getMessage("inactive.label", null, "Inactive", Locale.getDefault()));
		}
		if ( status.info != null && !status.info.isEmpty() ) {
			buf.append("; ");
			buf.append(StringUtils.delimitedStringFromCollection(status.info, ", "));
		}
		return buf.toString();
	}

	/**
	 * A mobile connection status.
	 */
	public static final class Status {

		private final boolean present;
		private final boolean active;
		private final List<String> info;

		private Status(boolean present, boolean active, List<String> info) {
			super();
			this.present = present;
			this.active = active;
			this.info = info;
		}

		/**
		 * Get the modem presence status.
		 *
		 * <p>
		 * This indicates whether a mobile modem is available on the node at all,
		 * and thus whether a reset can be performed. A client (such as the mobile
		 * app) can use this to decide whether to offer a reset action, rather than
		 * attempting a reset that has nothing to act on.
		 * </p>
		 *
		 * @return {@literal true} if a mobile modem is present
		 */
		public boolean isPresent() {
			return present;
		}

		/**
		 * Get the active status.
		 *
		 * @return {@literal true} if the mobile connection is currently active
		 */
		public boolean isActive() {
			return active;
		}

		/**
		 * Get additional status detail lines (such as operator, access
		 * technology, or signal), as emitted by the helper script.
		 *
		 * @return the status detail lines
		 */
		public List<String> getInfo() {
			return info;
		}
	}

	private Status currentStatus() {
		boolean present = false;
		boolean active = false;
		List<String> info = new ArrayList<>(4);
		try {
			List<String> result = executeAction(ACTION_STATUS);
			if ( result != null ) {
				for ( String line : result ) {
					int idx = line.indexOf(':');
					if ( idx < 0 ) {
						continue;
					}
					String key = line.substring(0, idx).trim().toLowerCase(Locale.ENGLISH);
					String value = line.substring(idx + 1).trim();
					if ( "present".equals(key) ) {
						present = "true".equalsIgnoreCase(value);
					} else if ( "active".equals(key) ) {
						active = "true".equalsIgnoreCase(value);
					} else if ( !value.isEmpty() ) {
						info.add(line.trim());
					}
				}
			}
		} catch ( Throwable t ) {
			log.warn("Error getting current mobile network status: {}", t.getMessage());
		}
		return new Status(present, active, info);
	}

	private synchronized List<String> executeAction(final String action, String... args) {
		log.debug("Executing mobile action {}", action);
		List<String> cmd = new ArrayList<>(8);
		cmd.add(command);
		cmd.add(CONFIG_SERVICE);
		cmd.add(action);
		if ( args != null && args.length > 0 ) {
			for ( String arg : args ) {
				cmd.add(arg);
			}
		}
		List<String> result = new ArrayList<>(8);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			Process pr = pb.start();
			BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = null;
			while ( (line = in.readLine()) != null ) {
				result.add(line);
			}

			BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
			StringBuilder buf = new StringBuilder();
			line = null;
			while ( (line = err.readLine()) != null ) {
				if ( buf.length() > 0 ) {
					buf.append('\n');
				}
				buf.append(line);
			}
			if ( buf.length() > 0 ) {
				log.error("Error executing mobile action {}: {}", action, buf);
			}
			return result;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Set the command to use.
	 *
	 * @param command
	 *        the command to set; defaults to {@link #DEFAULT_COMMAND}
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * Set the reset toggle.
	 *
	 * <p>
	 * This is a transient setting: when set to {@literal true} a mobile network
	 * reset is performed and the value is reset to {@literal false}.
	 * </p>
	 *
	 * @param reset
	 *        {@literal true} to trigger a reset
	 */
	public void setReset(boolean reset) {
		this.reset = reset;
	}
}
