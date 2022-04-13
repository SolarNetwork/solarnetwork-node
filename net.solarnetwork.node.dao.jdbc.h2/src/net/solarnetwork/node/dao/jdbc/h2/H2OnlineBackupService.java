/* ==================================================================
 * H2OnlineBackupService.java - 11/04/2022 5:28:57 PM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.h2;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Service to perform online backups of H2 databases using the {@code BACkUP TO}
 * command.
 * 
 * @author matt
 * @version 1.0
 */
public class H2OnlineBackupService extends BasicIdentifiable implements EventHandler, JobService {

	/** The {@code backupDelaySecs} property default value. */
	public static final int DEFAULT_BACKUP_DELAY_SECS = 10;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Collection<DataSource> dataSources;
	private TaskScheduler taskScheduler;
	private Path destinationPath;
	private int backupDelaySecs;

	private ScheduledFuture<?> backupFuture;

	/**
	 * Constructor.
	 * 
	 * @param dataSources
	 *        the list of data sources to back up
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public H2OnlineBackupService(Collection<DataSource> dataSources) {
		super();
		this.dataSources = requireNonNullArgument(dataSources, "dataSources");
		this.destinationPath = Paths.get("var", "db-bak");
		this.backupDelaySecs = DEFAULT_BACKUP_DELAY_SECS;
		setUid(getSettingUid());
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.dao.jdbc.h2.backup";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(4);
		result.add(new BasicTextFieldSettingSpecifier("destination", destinationPath.toString()));
		return result;
	}

	@Override
	public void executeJobService() throws Exception {
		try {
			backup();
		} catch ( Exception e ) {
			log.error("Error backing up H2 database(s): {}", e.toString(), e);
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
			backupSoon("network association acceptance");
		} else if ( SettingDao.EVENT_TOPIC_SETTING_CHANGED.equals(event.getTopic()) ) {
			backupSoon("setting change");
		} else if ( Constants.EVENT_TOPIC_CONFIGURATION_CHANGED.equals(event.getTopic()) ) {
			backupSoon("configuration change");
		}
	}

	private void backupSoon(String reasonMessage) {
		if ( taskScheduler == null ) {
			taskScheduler = new ConcurrentTaskScheduler();
		}
		if ( backupFuture != null ) {
			if ( !backupFuture.isDone() && !backupFuture.cancel(false) ) {
				return;
			}
		} else {
			log.info("Scheduling database backup sync after {} event.", reasonMessage);
		}
		backupFuture = taskScheduler.schedule(new Runnable() {

			@Override
			public void run() {
				log.info("Performing database backup...");
				backup();
				backupFuture = null;
				log.info("Database backup complete.");
			}

		}, new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(backupDelaySecs)));
	}

	/**
	 * Execute the backup.
	 */
	public void backup() {
		final Set<String> completedUrls = new LinkedHashSet<>();
		for ( DataSource dataSource : dataSources ) {
			JdbcOperations jdbcOps = new JdbcTemplate(dataSource);
			jdbcOps.execute(new ConnectionCallback<Void>() {

				@Override
				public Void doInConnection(Connection con) throws SQLException, DataAccessException {
					String url = con.getMetaData().getURL();
					if ( completedUrls.contains(url) ) {
						return null;
					}
					if ( !url.startsWith("jdbc:h2:") ) {
						log.debug("Ignoring DataSource {}", url);
						return null;
					}
					String[] components = url.split(":");
					String dbName = components[components.length - 1];
					int optIdx = dbName.indexOf(';');
					if ( optIdx > 0 ) {
						dbName = dbName.substring(0, optIdx);
					}
					optIdx = dbName.indexOf('/');
					if ( optIdx >= 0 ) {
						components = dbName.split("/");
						dbName = components[components.length - 1];
					}
					Path destPath = destinationPath.resolve(String.format("%s.zip", dbName));
					log.debug("Backing up [{}] database to [{}]", dbName, destPath);
					try (PreparedStatement stmt = con.prepareStatement("BACKUP TO ?")) {
						stmt.setString(1, destPath.toString());
						stmt.execute();
					}
					log.info("Backed up [{}] database to [{}]", dbName, destPath);
					completedUrls.add(url);
					return null;
				}
			});
		}
	}

	/**
	 * Get the destination path (directory) for the backup archives.
	 * 
	 * @return the destination path for the backup archives, never
	 *         {@literal null}
	 */
	public Path getDestinationPath() {
		return destinationPath;
	}

	/**
	 * Set the destination path (directory) for the backup archives.
	 * 
	 * @param destinationPath
	 *        the path to use; {@literal null} value will be ignored
	 */
	public void setDestinationPath(Path destinationPath) {
		if ( destinationPath == null ) {
			return;
		}
		this.destinationPath = destinationPath;
	}

	/**
	 * Get the destination path as a string.
	 * 
	 * @return the path, never {@literal null}
	 */
	public String getDestination() {
		return destinationPath.toString();
	}

	/**
	 * Set the destination path as a string.
	 * 
	 * @param destination
	 *        the path to set
	 */
	public void setDestination(String destination) {
		if ( destination == null || destination.isEmpty() ) {
			return;
		}
		setDestinationPath(Paths.get(destination));
	}

	/**
	 * Get the backup delay seconds.
	 * 
	 * @return the backup delay seconds; defaults to
	 *         {@link #DEFAULT_BACKUP_DELAY_SECS}
	 */
	public int getBackupDelaySecs() {
		return backupDelaySecs;
	}

	/**
	 * Set the backup delay seconds.
	 * 
	 * @param backupDelaySecs
	 *        the backup delay seconds to use
	 */
	public void setBackupDelaySecs(int backupDelaySecs) {
		this.backupDelaySecs = backupDelaySecs;
	}

}
