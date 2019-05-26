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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static net.solarnetwork.node.Constants.solarNodeHome;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.NodeMetadataService;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.CloseableService;
import net.solarnetwork.util.OptionalService;
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
 * {@literal user,host,port,reverse_port} values</dd>
 * 
 * <dt>{@literal showkey}</dt>
 * <dd>get the public SSH key to use</dd>
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
 * @version 1.2
 */
public class RemoteSshService
		implements FeedbackInstructionHandler, SettingSpecifierProvider, CloseableService {

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

	/**
	 * The default value for the {@code command} property.
	 * 
	 * <p>
	 * This is {@link Constants#solarNodeHome()} with {@literal /bin/solarssh}.
	 * </p>
	 */
	public static final String DEFAULT_COMMAND = solarNodeHome() + "/bin/solarssh";

	/** The node metadata info key to publish the SSH public key to. */
	public static final String METADATA_SSH_PUBLIC_KEY = "ssh-public-key";

	private String command;
	private Set<String> allowedHosts;
	private MessageSource messageSource;
	private TaskScheduler taskScheduler;
	private ScheduledFuture<?> maintenanceFuture;
	private OptionalService<NodeMetadataService> nodeMetadataService;

	private final Set<RemoteSshConfig> configs = new ConcurrentSkipListSet<RemoteSshConfig>();

	private final Logger log = LoggerFactory.getLogger(getClass());

	public RemoteSshService() {
		super();
		this.allowedHosts = unmodifiableSet(new HashSet<String>(asList("data.solarnetwork.net")));
		this.command = DEFAULT_COMMAND;
	}

	private class ConnectionMaintenanceTask implements Runnable {

		@Override
		public void run() {
			try {
				performConnectionMaintenance();
			} catch ( RuntimeException e ) {
				log.error("Error performing SSH connection maintenance", e);
			}
		}

	}

	/**
	 * Call after all properties are configured to initialize the service.
	 */
	public void init() {
		try {
			for ( RemoteSshConfig config : listActive() ) {
				configs.add(config);
			}
		} catch ( RuntimeException e ) {
			log.error("Error listing active SSH connections", e);
		}
		scheduleConnectionMaintenanceIfNeeded();
	}

	/**
	 * Call when this service is no longer needed to free internal resources.
	 */
	@Override
	public void closeService() {
		if ( maintenanceFuture != null ) {
			maintenanceFuture.cancel(false);
		}
	}

	private void scheduleConnectionMaintenanceIfNeeded() {
		if ( taskScheduler == null || maintenanceFuture != null ) {
			return;
		}
		maintenanceFuture = taskScheduler.scheduleWithFixedDelay(new ConnectionMaintenanceTask(),
				120000);
	}

	private void performConnectionMaintenance() {
		log.trace("Starting SSH connection maintenance");

		// list active from OS so our internal data consistent; removing any not returned
		Set<RemoteSshConfig> osConfigs = listActive();
		for ( Iterator<RemoteSshConfig> itr = configs.iterator(); itr.hasNext(); ) {
			RemoteSshConfig config = itr.next();
			if ( !osConfigs.contains(config) ) {
				log.info("Removing SSH connection {} that is no longer available",
						config.toDisplayInfo());
				itr.remove();
			}
		}

		// now replace all configs with OS ones, to pick up any error messages
		configs.addAll(osConfigs);
	}

	private void publishSshPublicKey() {
		String publicKey = getSshPublicKey();
		if ( publicKey == null || publicKey.length() < 1 ) {
			return;
		}
		NodeMetadataService service = (nodeMetadataService != null ? nodeMetadataService.service()
				: null);
		if ( service == null ) {
			log.debug("Cannot publish SSH public key because no NodeMetadataService available.");
			return;
		}
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue(METADATA_SSH_PUBLIC_KEY, publicKey);
		service.addNodeMetadata(meta);
	}

	@Override
	public boolean handlesTopic(String topic) {
		return (TOPIC_START_REMOTE_SSH.equalsIgnoreCase(topic)
				|| TOPIC_STOP_REMOTE_SSH.equalsIgnoreCase(topic));
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		InstructionStatus result = processInstructionWithFeedback(instruction);
		return (result != null ? result.getInstructionState() : null);
	}

	@Override
	public InstructionStatus processInstructionWithFeedback(Instruction instruction) {
		InstructionStatus result = null;
		if ( instruction != null ) {
			if ( TOPIC_START_REMOTE_SSH.equalsIgnoreCase(instruction.getTopic()) ) {
				// make sure current public key is published, in case it has changed
				publishSshPublicKey();

				result = handleStartRemoteSsh(instruction);
			} else if ( TOPIC_STOP_REMOTE_SSH.equalsIgnoreCase(instruction.getTopic()) ) {
				result = handleStopRemoteSsh(instruction);
			}
		}
		return result;
	}

	private InstructionStatus statusWithError(Instruction instruction, String code, String message) {
		Map<String, Object> resultParams = new LinkedHashMap<String, Object>();
		resultParams.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, code);
		resultParams.put(InstructionStatus.MESSAGE_RESULT_PARAM, message);
		return instruction.getStatus().newCopyWithState(InstructionState.Declined, resultParams);
	}

	private InstructionStatus handleStartRemoteSsh(Instruction instruction) {
		RemoteSshConfig config;
		try {
			config = configForInstruction(instruction);
		} catch ( IllegalArgumentException e ) {
			return statusWithError(instruction, "5001", e.getMessage());
		}

		Map<String, Object> resultParams = new LinkedHashMap<String, Object>();
		boolean started = startRemoteSsh(config, resultParams);
		if ( started ) {
			configs.add(config);
		} else {
			configs.remove(config);
		}

		final InstructionStatus startingStatus = instruction.getStatus();

		// TODO: use Executing state when connection hasn't been confirmed as connected!
		InstructionState newState = (started ? InstructionState.Completed : InstructionState.Declined);
		InstructionStatus result;
		if ( resultParams.isEmpty() ) {
			result = (startingStatus != null ? startingStatus.newCopyWithState(newState)
					: new BasicInstructionStatus(instruction.getId(), newState, new Date()));
		} else {
			result = (startingStatus != null ? startingStatus.newCopyWithState(newState, resultParams)
					: new BasicInstructionStatus(instruction.getId(), newState, new Date(), null,
							resultParams));
		}
		return result;
	}

	private InstructionStatus handleStopRemoteSsh(Instruction instruction) {
		RemoteSshConfig config;
		try {
			config = configForInstruction(instruction);
		} catch ( IllegalArgumentException e ) {
			return statusWithError(instruction, "5001", e.getMessage());
		}
		Map<String, Object> resultParams = new LinkedHashMap<String, Object>();
		boolean stopped = stopRemoteSsh(config, resultParams);
		if ( stopped ) {
			configs.remove(config);
		}
		final InstructionStatus startingStatus = instruction.getStatus();
		InstructionState newState = (stopped ? InstructionState.Completed : InstructionState.Declined);
		InstructionStatus result;
		if ( resultParams.isEmpty() ) {
			result = (startingStatus != null ? startingStatus.newCopyWithState(newState)
					: new BasicInstructionStatus(instruction.getId(), newState, new Date()));
		} else {
			result = (startingStatus != null ? startingStatus.newCopyWithState(newState, resultParams)
					: new BasicInstructionStatus(instruction.getId(), newState, new Date(), null,
							resultParams));
		}
		return result;
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

	private boolean startRemoteSsh(RemoteSshConfig config, Map<String, Object> resultParameters) {
		String[] cmd = commandForAction(config, "start");
		log.debug("Starting SSH connection {}", config);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			Process pr = pb.start();
			int status = pr.waitFor();
			if ( status != 0 ) {
				log.error("Error starting SSH connection {} (process returned {})", config, status);
				resultParameters.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, "5002");
				resultParameters.put(InstructionStatus.MESSAGE_RESULT_PARAM,
						"Error starting SSH connection; result code " + status);
				return false;
			}
			log.info("Started SSH connection {}", config);
			return true;
		} catch ( IOException e ) {
			log.warn("IOException waiting for SSH connection {} to start: {}", config, e.getMessage());
			resultParameters.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, "5003");
			resultParameters.put(InstructionStatus.MESSAGE_RESULT_PARAM,
					"Communication problem starting SSH connection: " + e.getMessage());
			return false;
		} catch ( InterruptedException e ) {
			log.warn("Interrupted waiting for SSH connection {} to start", config);
			resultParameters.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, "5004");
			resultParameters.put(InstructionStatus.MESSAGE_RESULT_PARAM,
					"Interrupted while starting SSH connection: " + e.getMessage());
			return false;
		}
	}

	private boolean stopRemoteSsh(RemoteSshConfig config, Map<String, Object> resultParameters) {
		String[] cmd = commandForAction(config, "stop");
		log.debug("Stopping SSH connection {}", config);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			Process pr = pb.start();
			int status = pr.waitFor();
			if ( status != 0 ) {
				log.error("Error stopping SSH connection {} (process returned {})", config, status);
				resultParameters.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, status);
				resultParameters.put(InstructionStatus.MESSAGE_RESULT_PARAM,
						"Error stopping SSH connection; result code " + status);
				return false;
			}
			log.info("Stopped SSH connection {}", config);
			return true;
		} catch ( IOException e ) {
			log.warn("IOException waiting for SSH connection {} to stop: {}", config, e.getMessage());
			resultParameters.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, "5003");
			resultParameters.put(InstructionStatus.MESSAGE_RESULT_PARAM,
					"Communication problem stopping SSH connection: " + e.getMessage());
			return false;
		} catch ( InterruptedException e ) {
			log.warn("Interrupted waiting for SSH connection {} to stop", config);
			resultParameters.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, "5004");
			resultParameters.put(InstructionStatus.MESSAGE_RESULT_PARAM,
					"Interrupted while stopping SSH connection: " + e.getMessage());
			return false;
		}
	}

	private String getSshPublicKey() {
		String[] cmd = new String[] { command, "showkey" };
		log.debug("Getting SSH public key with {} showkey...", command);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			Process pr = pb.start();

			String key = FileCopyUtils.copyToString(new InputStreamReader(pr.getInputStream()));
			if ( key != null ) {
				key = key.trim(); // remove trailing newline
			}
			String err = FileCopyUtils.copyToString(new InputStreamReader(pr.getErrorStream()));
			if ( err.length() > 0 ) {
				log.error("Error getting SSH public key: {}", err);
			}
			log.debug("Public SSH key: {}", key);
			return key;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
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
		Locale locale = Locale.getDefault();

		List<String> infos = new ArrayList<String>();
		for ( RemoteSshConfig config : configs ) {
			infos.add(getInfoMessage(config, locale));
		}
		if ( infos.size() > 0 ) {
			List<SettingSpecifier> groupSettings = new ArrayList<SettingSpecifier>(infos.size() + 1);
			groupSettings.add(new BasicTitleSettingSpecifier("info",
					messageSource.getMessage("info.connections", new Object[] { infos.size() }, locale),
					true));
			for ( String info : infos ) {
				groupSettings.add(new BasicTitleSettingSpecifier("connInfo", info, true));
			}
			results.add(new BasicGroupSettingSpecifier(groupSettings));
		} else {
			results.add(new BasicTitleSettingSpecifier("info",
					messageSource.getMessage("info.noconnections", null, locale), true));
		}

		results.add(new BasicTextFieldSettingSpecifier("command", defaults.command));
		results.add(new BasicTextFieldSettingSpecifier("allowedHostsValue",
				StringUtils.commaDelimitedStringFromCollection(defaults.allowedHosts)));

		return results;
	}

	private String getInfoMessage(RemoteSshConfig config, Locale locale) {
		StringBuilder buf = new StringBuilder();
		buf.append(config.getUser()).append('@').append(config.getHost()).append(':')
				.append(config.getPort()).append(" \u2190 ").append(config.getReversePort());
		if ( config.getError() ) {
			buf.append("\nERROR:");
		}
		List<String> msgs = config.getMessages();
		if ( msgs != null && !msgs.isEmpty() ) {
			for ( String s : msgs ) {
				buf.append(" ").append(s);
			}
		}
		return buf.toString();
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

	/**
	 * Set a task scheduler to help with monitoring connections.
	 * 
	 * @param taskScheduler
	 *        the taskScheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Set the optional node metadata service to use.
	 * 
	 * @param nodeMetadataService
	 *        the nodeMetadataService to set
	 */
	public void setNodeMetadataService(OptionalService<NodeMetadataService> nodeMetadataService) {
		this.nodeMetadataService = nodeMetadataService;
	}

}
