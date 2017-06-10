/* ==================================================================
 * RemoteSshService.java - 9/06/2017 3:58:02 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.system.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Service to manage remote SSH connections with the help of a command-line
 * helper program.
 * 
 * <p>
 * The command-line helper program this has been designed for is the
 * {@literal solarssh.sh} bash script, but any program will work as long as it
 * follows this syntax:
 * </p>
 * 
 * <pre>
 * prog -u user -h host -p port -r reverse_port action
 * </pre>
 * 
 * <p>
 * The following actions are assumed:
 * </p>
 * 
 * <dl>
 * <dt>{@literal list}</dt>
 * <dd>list all known active connections, as a comma-delimited list of
 * {@literal user,host,port,report_port} values</dd>
 * 
 * <dt>{@literal status}</dt>
 * <dd>get the status of a specific connection; must return {@literal active} if
 * active</dd>
 * 
 * <dt>{@literal start}</dt>
 * <dd>start a ssh connection</dd>
 * 
 * <dt>{@literal stop}</dt>
 * <dd>stop an active ssh connection</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class RemoteSshService implements InstructionHandler, SettingSpecifierProvider {

	/**
	 * The instruction topic for starting a remote SSH connection with a reverse
	 * SSH port forwarded back to the node.
	 */
	public static final String TOPIC_START_REMOTE_SSH = "StartRemoteSsh";

	/**
	 * The instruction topic for stopping a remote SSH connection.
	 */
	public static final String TOPIC_STOP_REMOTE_SSH = "StopRemoteSsh";

	/**
	 * The instruction parameter key for the username to connect as with SSH.
	 */
	public static final String PARAM_USERNAME = "user";

	/**
	 * The instruction parameter key for the host to connect to with SSH.
	 */
	public static final String PARAM_HOST = "host";

	/**
	 * The instruction parameter key for the port to connect to with SSH.
	 */
	public static final String PARAM_PORT = "port";

	/**
	 * The instruction parameter key for the reverse port to forward back to the
	 * node on via SSH.
	 */
	public static final String PARAM_REVERSE_PORT = "rport";

	/** The default value for the {@code command} property. */
	public static final String DEFAULT_COMMAND = "solarssh";

	private String command = DEFAULT_COMMAND;
	private Set<String> allowedHosts = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList("data.solarnetwork.net")));
	private MessageSource messageSource;

	private final ConcurrentMap<RemoteSshConfig, Boolean> statusMap = new ConcurrentHashMap<RemoteSshConfig, Boolean>(
			4);

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Call after all properties are configured to initialize the service.
	 */
	public void init() {
		for ( RemoteSshConfig config : listActive() ) {
			statusMap.put(config, Boolean.TRUE);
		}
	}

	@Override
	public boolean handlesTopic(String topic) {
		return (TOPIC_START_REMOTE_SSH.equalsIgnoreCase(topic)
				|| TOPIC_STOP_REMOTE_SSH.equalsIgnoreCase(topic));
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		InstructionState result = null;
		if ( instruction != null ) {
			if ( TOPIC_START_REMOTE_SSH.equalsIgnoreCase(instruction.getTopic()) ) {
				result = handleStartRemoteSsh(instruction);
			} else if ( TOPIC_STOP_REMOTE_SSH.equalsIgnoreCase(instruction.getTopic()) ) {
				result = handleStopRemoteSsh(instruction);
			}
		}
		return result;
	}

	private InstructionState handleStartRemoteSsh(Instruction instruction) {
		RemoteSshConfig config;
		try {
			config = configForInstruction(instruction);
		} catch ( IllegalArgumentException e ) {
			return InstructionState.Declined;
		}
		boolean started = startRemoteSsh(config);
		if ( started ) {
			statusMap.put(config, Boolean.TRUE);
		} else {
			statusMap.remove(config);
		}
		return (started ? InstructionState.Completed : InstructionState.Declined);
	}

	private InstructionState handleStopRemoteSsh(Instruction instruction) {
		RemoteSshConfig config;
		try {
			config = configForInstruction(instruction);
		} catch ( IllegalArgumentException e ) {
			return InstructionState.Declined;
		}
		boolean stopped = stopRemoteSsh(config);
		if ( stopped ) {
			statusMap.remove(config);
		}
		return (stopped ? InstructionState.Completed : InstructionState.Declined);
	}

	private RemoteSshConfig configForInstruction(Instruction instruction) {
		String host = instruction.getParameterValue(PARAM_HOST);
		if ( host == null || !allowedHosts.contains(host.toLowerCase()) ) {
			log.warn("Host {} not in allowed SSH hosts: {}", host,
					StringUtils.commaDelimitedStringFromCollection(allowedHosts));
			throw new IllegalArgumentException("Host " + host + " not in allowed SSH hosts");
		}
		String user = instruction.getParameterValue(PARAM_USERNAME);
		String port = instruction.getParameterValue(PARAM_PORT);
		String reversePort = instruction.getParameterValue(PARAM_REVERSE_PORT);
		RemoteSshConfig config;
		try {
			config = new RemoteSshConfig(user, host, Integer.valueOf(port),
					Integer.valueOf(reversePort));
		} catch ( NumberFormatException e ) {
			log.warn("Bad port or reverse port value: {}", e.getMessage());
			throw new IllegalArgumentException("Bad port or reverse port value");
		}
		return config;
	}

	private String[] commandForAction(RemoteSshConfig config, String action) {
		String[] cmd = new String[] { command, "-u", config.getUser(), "-h", config.getHost(), "-p",
				config.getPort().toString(), "-r", config.getReversePort().toString(), action };
		return cmd;
	}

	private boolean startRemoteSsh(RemoteSshConfig config) {
		String[] cmd = commandForAction(config, "start");
		log.debug("Starting SSH connection {}", config);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			Process pr = pb.start();
			int status = pr.waitFor();
			if ( status != 0 ) {
				log.error("Error starting SSH connection {} (process returned {})", config, status);
				return false;
			}
			log.info("Started SSH connection {}", config);
			return true;
		} catch ( IOException e ) {
			log.warn("IOException waiting for SSH connection {} to start: {}", config, e.getMessage());
			return false;
		} catch ( InterruptedException e ) {
			log.warn("Interrupted waiting for SSH connection {} to start", config);
			return false;
		}
	}

	private boolean stopRemoteSsh(RemoteSshConfig config) {
		String[] cmd = commandForAction(config, "stop");
		log.debug("Stopping SSH connection {}", config);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			Process pr = pb.start();
			int status = pr.waitFor();
			if ( status != 0 ) {
				log.error("Error stopping SSH connection {} (process returned {})", config, status);
				return false;
			}
			log.info("Stopped SSH connection {}", config);
			return true;
		} catch ( IOException e ) {
			log.warn("IOException waiting for SSH connection {} to stop: {}", config, e.getMessage());
			return false;
		} catch ( InterruptedException e ) {
			log.warn("Interrupted waiting for SSH connection {} to stop", config);
			return false;
		}
	}

	private Set<RemoteSshConfig> listActive() {
		String[] cmd = new String[] { command, "list" };
		log.debug("Listing active SSH connections with {} list...", command);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			Process pr = pb.start();

			Set<RemoteSshConfig> result = new HashSet<RemoteSshConfig>(4);
			BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = null;
			while ( (line = in.readLine()) != null ) {
				try {
					log.trace("Read status line: {}", line);
					RemoteSshConfig config = RemoteSshConfig.parseConfigKey(line);
					result.add(config);
				} catch ( IllegalArgumentException e ) {
					log.warn("Error parsing SSH configuration [{}]: {}", line, e.getMessage());
				}
			}

			BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
			StringBuilder buf = new StringBuilder();
			while ( (line = err.readLine()) != null ) {
				if ( buf.length() > 0 ) {
					buf.append('\n');
				}
				buf.append(line);
			}
			if ( buf.length() > 0 ) {
				log.error("Error listing SSH connections: {}", buf);
			}
			log.info("Found {} active SSH connections", result.size());
			log.debug("Active SSH connections: {}",
					StringUtils.delimitedStringFromCollection(result, "; "));
			return result;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Set the helper program command to use.
	 * 
	 * @param command
	 *        the command to set
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * Set the hosts that are allowed for connecting to via SSH by this service.
	 * 
	 * @param allowedHosts
	 *        the allowedHosts to set; the set is copied
	 */
	public void setAllowedHosts(Set<String> allowedHosts) {
		Set<String> allowed;
		if ( allowedHosts == null || allowedHosts.isEmpty() ) {
			allowed = Collections.emptySet();
		} else {
			allowed = new HashSet<String>(allowedHosts.size());
			for ( String host : allowedHosts ) {
				allowed.add(host.toLowerCase());
			}
			allowed = Collections.unmodifiableSet(allowed);
		}
		this.allowedHosts = allowed;
	}

	/**
	 * Set the hosts that are allowed for connecting to via SSH by this service,
	 * as a comma-delimited list.
	 * 
	 * @param list
	 *        a comma-delimited list of hosts to allow
	 * @see RemoteSshService#setAllowedHosts(Set)
	 */
	public void setAllowedHostsValue(String list) {
		setAllowedHosts(StringUtils.commaDelimitedStringToSet(list));
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.system.ssh.RemoteSshService";
	}

	@Override
	public String getDisplayName() {
		return "Remote SSH Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);

		RemoteSshService defaults = new RemoteSshService();

		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(Locale.getDefault()), true));
		results.add(new BasicTextFieldSettingSpecifier("command", defaults.command));
		results.add(new BasicTextFieldSettingSpecifier("allowedHostsValue",
				StringUtils.commaDelimitedStringFromCollection(defaults.allowedHosts)));

		return results;
	}

	private String getInfoMessage(Locale locale) {
		StringBuilder buf = new StringBuilder();
		int count = 0;
		for ( Map.Entry<RemoteSshConfig, Boolean> me : statusMap.entrySet() ) {
			if ( !me.getValue().booleanValue() ) {
				continue;
			}
			if ( count > 0 ) {
				buf.append("; ");
			}
			RemoteSshConfig config = me.getKey();
			buf.append(config.getUser()).append('@').append(config.getHost()).append(':')
					.append(config.getPort()).append(" \u2190 ").append(config.getReversePort());
			count += 1;
		}
		if ( count > 0 ) {
			return messageSource.getMessage("info.connections", new Object[] { count, buf }, locale);
		}
		return messageSource.getMessage("info.noconnections", null, locale);
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a message source to use for i18n messages.
	 * 
	 * @param messageSource
	 *        The message source to use.
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
