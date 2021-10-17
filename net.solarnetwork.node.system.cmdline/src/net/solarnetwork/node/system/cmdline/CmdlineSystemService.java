/* ==================================================================
 * CmdlineSystemService.java - 10/02/2017 3:16:47 PM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.system.cmdline;

import static net.solarnetwork.util.StringUtils.delimitedStringFromCollection;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.SystemService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * SystemService implementation using OS command line actions to perform
 * functions.
 * 
 * @author matt
 * @version 2.0
 */
public class CmdlineSystemService
		implements SystemService, SettingSpecifierProvider, InstructionHandler {

	/** The default value for the {@code exitCommand} property. */
	public static final String DEFAULT_EXIT_COMMAND = "sudo systemctl restart solarnode";

	/** The default value for the {@code rebootCommand} property. */
	public static final String DEFAULT_REBOOT_COMMAND = "sudo reboot";

	/** The default value for the {@code resetCommand} property. */
	public static final String DEFAULT_RESET_COMMAND = "sudo systemctl start sn-reset";

	/** The default value for the {@code resetAooCommand} property. */
	public static final String DEFAULT_RESET_APP_COMMAND = "sudo systemctl start sn-reset-app";

	private String exitCommand = DEFAULT_EXIT_COMMAND;
	private String rebootCommand = DEFAULT_REBOOT_COMMAND;
	private String resetCommand = DEFAULT_RESET_COMMAND;
	private String resetAppCommand = DEFAULT_RESET_APP_COMMAND;

	private final BundleContext bundleContext;
	private MessageSource messageSource;

	private Thread shutdownThread;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * <p>
	 * No {@link BundleContext} will be available.
	 * </p>
	 */
	public CmdlineSystemService() {
		this(null);
	}

	/**
	 * Constructor.
	 * 
	 * @param bundleContext
	 *        the bundle context
	 */
	public CmdlineSystemService(BundleContext bundleContext) {
		super();
		this.bundleContext = bundleContext;
	}

	@Override
	public synchronized void exit(final boolean syncState) {
		if ( shutdownThread != null ) {
			return;
		}
		log.warn("Restart requested");
		final BundleContext ctx = bundleContext;
		shutdownThread = new Thread(new Runnable() {

			@Override
			public void run() {
				log.warn("Restart sequence initiated");

				// pause slightly at start to give time for original calling thread time to complete
				try {
					Thread.sleep(1000);
				} catch ( Exception e ) {
					// ignore
				}

				final long start = System.currentTimeMillis();

				// start another thread to monitor the shutdown process, in case OSGi takes too long or gets hung up
				Thread shutdownMonitorThread = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							shutdownThread.join(8000);
							final long end = (System.currentTimeMillis() - start);
							if ( end < 900 ) {
								Thread.sleep(2000);
							}
						} catch ( Exception e ) {
							// ignore
						} finally {
							if ( syncState ) {
								System.err.println("Exiting via command: " + exitCommand);
								handleOSCommand(exitCommand);
							} else {
								System.err.println("Exiting from shutdown request.");
								System.exit(0);
							}
						}
					}
				}, "System Service Shutdown Monitor");
				shutdownMonitorThread.setDaemon(true);
				shutdownMonitorThread.start();

				if ( ctx != null ) {
					try {
						log.warn("Stopping OSGi from shutdown request...");
						ctx.getBundle(0).stop(org.osgi.framework.Bundle.STOP_TRANSIENT);
					} catch ( Exception e ) {
						System.err.println("Exception shutting down OSGi: " + e);
					}
				}
			}
		}, "System Service Shutdown");
		shutdownThread.start();
	}

	@Override
	public void reboot() {
		if ( shutdownThread != null ) {
			return;
		}
		log.warn("Reboot requested");
		shutdownThread = new Thread(new Runnable() {

			@Override
			public void run() {
				log.warn("Reboot sequence initiated");

				// pause slightly at start to give time for original calling thread time to complete
				try {
					Thread.sleep(1000);
				} catch ( Exception e ) {
					// ignore
				}

				handleOSCommand(rebootCommand);
			}
		}, "System Service Reboot");
		shutdownThread.start();
	}

	@Override
	public void reset(boolean applicationOnly) {
		if ( shutdownThread != null ) {
			return;
		}
		log.warn("Reboot requested");
		shutdownThread = new Thread(new Runnable() {

			@Override
			public void run() {
				if ( applicationOnly ) {
					log.warn("Reset application sequence initiated");
				} else {
					log.warn("Reset sequence initiated");
				}

				// pause slightly at start to give time for original calling thread time to complete
				try {
					Thread.sleep(1000);
				} catch ( Exception e ) {
					// ignore
				}

				handleOSCommand(applicationOnly ? resetAppCommand : resetCommand);
			}
		}, "System Service Reset");
		shutdownThread.start();
	}

	private void handleOSCommand(String command) {
		if ( command == null ) {
			return;
		}
		List<String> arguments = Arrays.asList(command.split("\\s+"));
		handleOSCommand(arguments);
	}

	private void handleOSCommand(List<String> arguments) {
		ProcessBuilder pb = new ProcessBuilder(arguments);
		try {
			Process pr = pb.start();
			logInputStream(pr.getInputStream(), false);
			logInputStream(pr.getErrorStream(), true);
			pr.waitFor();
			if ( pr.exitValue() == 0 ) {
				log.debug("Command [{}] executed", delimitedStringFromCollection(arguments, " "));
			} else {
				log.error("Error executing [{}], exit status: {}",
						delimitedStringFromCollection(arguments, " "), pr.exitValue());
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
						handleInputStreamLine(errorStream, sc.nextLine());
					}
				} finally {
					sc.close();
				}
			}
		}).start();
	}

	/**
	 * Handle a line of input from the OS command process.
	 * 
	 * @param errorStream
	 *        {@literal true} if the line is from STDERR, {@literal false} from
	 *        STDOUT
	 * @param line
	 *        the line
	 * @since 1.2
	 */
	protected void handleInputStreamLine(boolean errorStream, String line) {
		if ( errorStream ) {
			log.error(line);
		} else {
			log.info(line);
		}
	}

	// FeedbackInstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return (TOPIC_REBOOT.equals(topic) || TOPIC_RESTART.equals(topic));
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		final String topic = (instruction != null ? instruction.getTopic() : null);
		if ( TOPIC_REBOOT.equals(topic) ) {
			reboot();
		} else if ( TOPIC_RESTART.equals(topic) ) {
			exit(true);
		} else if ( TOPIC_RESET.equals(topic) ) {
			String appOnly = (instruction != null ? instruction.getParameterValue("applicationOnly")
					: null);
			reset(StringUtils.parseBoolean(appOnly));
		}
		return InstructionUtils.createStatus(instruction, InstructionState.Completed);
	}

	// Settings

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.system.cmdline.CmdlineSystemService";
	}

	@Override
	public String getDisplayName() {
		return "Command Line System Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);

		results.add(new BasicTextFieldSettingSpecifier("exitCommand", DEFAULT_EXIT_COMMAND));
		results.add(new BasicTextFieldSettingSpecifier("rebootCommand", DEFAULT_REBOOT_COMMAND));

		return results;
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
	 * Set the OS command to use to exit and sync the application state. The
	 * command will be split on whitespace to turn into command-line arguments.
	 * 
	 * @param exitCommand
	 *        The command and arguments to use when {@link #exit(boolean)} is
	 *        called with a {@code true} argument.
	 */
	public void setExitCommand(String exitCommand) {
		this.exitCommand = exitCommand;
	}

	/**
	 * Set the OS command to use to reboot the device the application is running
	 * on. The command will be split on whitespace to turn into command-line
	 * arguments.
	 * 
	 * @param rebootCommand
	 *        The command and arguments to use when {@link #reboot()} is called.
	 */
	public void setRebootCommand(String rebootCommand) {
		this.rebootCommand = rebootCommand;
	}

	/**
	 * Set the OS command to use to reset the whole device.
	 * 
	 * @param resetCommand
	 *        the command to set
	 * @since 1.2
	 */
	public void setResetCommand(String resetCommand) {
		this.resetCommand = resetCommand;
	}

	/**
	 * Set the OS command to use to reset the application.
	 * 
	 * @param resetAppCommand
	 *        the command to set
	 * @since 1.2
	 */
	public void setResetAppCommand(String resetAppCommand) {
		this.resetAppCommand = resetAppCommand;
	}

}
