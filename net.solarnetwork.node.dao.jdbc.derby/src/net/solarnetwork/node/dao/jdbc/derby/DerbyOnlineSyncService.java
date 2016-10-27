/* ==================================================================
 * DerbyOnlineSyncService.java - Mar 14, 2015 5:16:07 PM
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.derby;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.SetupService;

/**
 * Job to backup the Derby database using the
 * {@code SYSCS_UTIL.SYSCS_FREEZE_DATABASE} procedure and {@code rsync}.
 * 
 * <p>
 * This job is designed with using an OS tool like rsync to make a copy of the
 * Derby database to a backup location.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>jdbcOperations</dt>
 * <dd>The {@link JdbcOperations} to use for executing SQL statements.</dd>
 * 
 * <dt>syncCommand</dt>
 * <dd>The OS command to execute to perform the backup sync. This defaults to
 * {@code rsync -am --delete --exclude *.lck --stats __SOURCE_DIR__ __DEST_DIR__}
 * . The {@code __SOURCE_DIR__} and {@code __DEST_DIR__} values are placeholders
 * that will be replaced by the directory path for the Derby database and the
 * value of the {@code destinationPath} property.</dd>
 * 
 * <dt>destinationPath</dt>
 * <dd>The path to perform the backup sync to. Defaults to
 * {@code /var/tmp}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 */
public class DerbyOnlineSyncService implements EventHandler {

	/**
	 * The placeholder string in the {@code syncCommand} for the source
	 * directory path.
	 */
	public static final String SOURCE_DIRECTORY_PLACEHOLDER = "__SOURCE_DIR__";

	/**
	 * The placeholder string in the {@code syncCommand} for the destination
	 * directory path.
	 */
	public static final String DESTINATION_DIRECTORY_PLACEHOLDER = "__DEST_DIR__";

	/** The default value for the {@code destinationPath} property. */
	public static final String DEFAULT_DESTINATION_PATH = "/var/tmp";

	/** The default value of the {@code syncCommand} property. */
	public static final List<String> DEFAULT_SYNC_COMMAND = Collections
			.unmodifiableList(Arrays.asList("rsync", "-am", "--delete", "--exclude", "*.lck", "--stats",
					SOURCE_DIRECTORY_PLACEHOLDER, DESTINATION_DIRECTORY_PLACEHOLDER));

	private static final String FREEZE_CALL = "{CALL SYSCS_UTIL.SYSCS_FREEZE_DATABASE()}";
	private static final String UNFREEZE_CALL = "{CALL SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE()}";

	private JdbcOperations jdbcOperations;
	private List<String> syncCommand = DEFAULT_SYNC_COMMAND;
	private String destinationPath = DEFAULT_DESTINATION_PATH;
	private long syncSoonSeconds = 10;

	private ScheduledExecutorService scheduler;
	private ScheduledFuture<Boolean> syncSoonFuture;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Call to destroy this service, cleaning up any resources.
	 */
	public void destroy() {
		ScheduledExecutorService s = scheduler;
		if ( s != null ) {
			s.shutdown();
		}
	}

	/**
	 * Listen for events to automatically trigger a database sync.
	 * 
	 * The {@link SetupService#TOPIC_NETWORK_ASSOCIATION_ACCEPTED} and
	 * {@link SettingDao#EVENT_TOPIC_SETTING_CHANGED} and
	 * {@link Constants#EVENT_TOPIC_CONFIGURATION_CHANGED} event topics are
	 * handled, and will cause a sync to be scheduled in the "near future".
	 */
	@Override
	public void handleEvent(Event event) {
		if ( SetupService.TOPIC_NETWORK_ASSOCIATION_ACCEPTED.equals(event.getTopic()) ) {
			// immediately sync database!
			log.info("Scheduling database backup sync after network association acceptance event.");
			syncSoon();
		} else if ( SettingDao.EVENT_TOPIC_SETTING_CHANGED.equals(event.getTopic()) ) {
			log.info("Scheduling database backup sync after setting change event.");
			syncSoon();
		} else if ( Constants.EVENT_TOPIC_CONFIGURATION_CHANGED.equals(event.getTopic()) ) {
			log.info("Scheduling database backup sync after configuration change event.");
			syncSoon();
		}
	}

	private synchronized void syncSoon() {
		if ( scheduler == null ) {
			scheduler = Executors.newSingleThreadScheduledExecutor();
		}
		if ( syncSoonFuture != null ) {
			if ( !syncSoonFuture.isDone() && !syncSoonFuture.cancel(false) ) {
				return;
			}
		}
		syncSoonFuture = scheduler.schedule(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				log.info("Performing database backup sync.");
				sync();
				return true;
			}

		}, syncSoonSeconds, TimeUnit.SECONDS);
	}

	/**
	 * Perform an online database sync. Calling this method will call the
	 * {@code SYSCS_UTIL.SYSCS_FREEZE_DATABASE} procedure, use {@code rsync} to
	 * sync the database directory to another directory, and finally call the
	 * {@code SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE} procedure.
	 */
	public synchronized void sync() {
		log.debug("Starting Derby backup sync job");

		String dbPath = getDbPath();
		if ( dbPath == null ) {
			return;
		}

		// freeze database for backup...
		executeProcedure(FREEZE_CALL);

		try {
			// perform OK backup (rsync) now...
			performSync(dbPath);
		} finally {
			// unfreeze database
			executeProcedure(UNFREEZE_CALL);
		}
	}

	private String getDbPath() {
		String dbPath = jdbcOperations.execute(new ConnectionCallback<String>() {

			@Override
			public String doInConnection(Connection con) throws SQLException, DataAccessException {
				DatabaseMetaData meta = con.getMetaData();
				String url = meta.getURL();
				Pattern pat = Pattern.compile("^jdbc:derby:(\\w+)", Pattern.CASE_INSENSITIVE);
				Matcher m = pat.matcher(url);
				String dbName;
				if ( m.find() ) {
					dbName = m.group(1);
				} else {
					log.warn("Unable to find Derby database name in connection URL: {}", url);
					return null;
				}

				String home = System.getProperty("derby.system.home", "");
				File f = new File(home, dbName);
				return f.getPath();
			}
		});
		return dbPath;
	}

	private void performSync(String dbPath) {
		assert syncCommand != null;
		List<String> cmd = new ArrayList<String>(syncCommand.size());
		for ( String param : syncCommand ) {
			param = param.replace(SOURCE_DIRECTORY_PLACEHOLDER, dbPath);
			param = param.replace(DESTINATION_DIRECTORY_PLACEHOLDER, destinationPath);
			cmd.add(param);
		}
		if ( log.isDebugEnabled() ) {
			StringBuilder buf = new StringBuilder();
			for ( String p : cmd ) {
				if ( buf.length() > 0 ) {
					buf.append(' ');
				}
				buf.append(p);
			}
			log.debug("Derby sync command: {}", buf.toString());
		}
		ProcessBuilder pb = new ProcessBuilder(cmd);
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			Process pr = pb.start();
			pr.waitFor();
			if ( pr.exitValue() == 0 ) {
				if ( log.isDebugEnabled() ) {
					in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
					StringBuilder buf = new StringBuilder();
					String line = null;
					while ( (line = in.readLine()) != null ) {
						buf.append(line).append('\n');
					}
					log.debug("Derby sync command output:\n{}", buf.toString());
				}
				log.info("Derby backup sync complete");
			} else {
				StringBuilder buf = new StringBuilder();
				in = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
				String line = null;
				while ( (line = in.readLine()) != null ) {
					buf.append(line).append('\n');
				}
				log.error("Sync command returned non-zero exit code {}: {}", pr.exitValue(),
						buf.toString().trim());
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} catch ( InterruptedException e ) {
			throw new RuntimeException(e);
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
			if ( out != null ) {
				out.flush();
				out.close();
			}
		}
	}

	private void executeProcedure(final String procedure) {
		jdbcOperations.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				log.trace("Calling {} procedure", procedure);
				return con.prepareCall(procedure);
			}
		}, new CallableStatementCallback<Object>() {

			@Override
			public Object doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				return null;
			}
		});
	}

	public JdbcOperations getJdbcOperations() {
		return jdbcOperations;
	}

	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	public List<String> getSyncCommand() {
		return syncCommand;
	}

	public void setSyncCommand(List<String> syncCommand) {
		this.syncCommand = syncCommand;
	}

	public String getDestinationPath() {
		return destinationPath;
	}

	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}

	public long getSyncSoonSeconds() {
		return syncSoonSeconds;
	}

	/**
	 * Set the number of seconds to delay the syncing of the database due to
	 * events in {@link #handleEvent(Event)}. This delay is implemented so that
	 * when many events are triggered closely in time (say, from multiple
	 * setting changes close together) we only try to sync once for the complete
	 * set of changes.
	 * 
	 * @param syncSoonSeconds
	 *        The number of seconds to treat as the "near future".
	 */
	public void setSyncSoonSeconds(long syncSoonSeconds) {
		this.syncSoonSeconds = syncSoonSeconds;
	}

}
