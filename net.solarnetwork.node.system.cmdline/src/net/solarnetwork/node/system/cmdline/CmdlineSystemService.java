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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.SystemService;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * SystemService implementation using OS command line actions to perform
 * functions.
 * 
 * @author matt
 * @version 1.0
 */
public class CmdlineSystemService implements SystemService, SettingSpecifierProvider {

	/** The default value for the {@code exitCommand} property. */
	public static final String DEFAULT_EXIT_COMMAND = "sudo systemctl restart solarnode";

	/** The default value for the {@code rebootCommand} property. */
	public static final String DEFAULT_REBOOT_COMMAND = "sudo reboot";

	private String exitCommand = DEFAULT_EXIT_COMMAND;
	private String rebootCommand = DEFAULT_REBOOT_COMMAND;

	private BundleContext bundleContext;
	private MessageSource messageSource;

	private Thread shutdownThread;

	private final Logger log = LoggerFactory.getLogger(getClass());

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

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.system.cmdline.CmdlineSystemService";
	}

	@Override
	public String getDisplayName() {
		return "Command Line System Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);

		CmdlineSystemService defaults = new CmdlineSystemService();

		results.add(new BasicTextFieldSettingSpecifier("exitCommand", defaults.exitCommand));
		results.add(new BasicTextFieldSettingSpecifier("rebootCommand", defaults.rebootCommand));

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
	 * Set a bundle context to use.
	 * 
	 * @param bundleContext
	 *        The bundle context.
	 */
	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
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
	 * @param exitCommand
	 *        The command and arguments to use when {@link #reboot()} is called.
	 */
	public void setRebootCommand(String rebootCommand) {
		this.rebootCommand = rebootCommand;
	}

}
