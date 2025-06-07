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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Service to perform online backups of H2 databases using the {@code BACkUP TO}
 * command.
 *
 * <p>
 * For robustness the backup is performed first to a temporary location, and
 * then the output file is renamed to the actual destination.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public class H2OnlineBackupService extends BasicIdentifiable implements EventHandler, JobService {

	/** The {@code backupDelaySecs} property default value. */
	public static final int DEFAULT_BACKUP_DELAY_SECS = 10;

	/** A special "all databases" URL. */
	private static final String ALL_DATASOURCE_URL = "db://all";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Collection<DataSource> dataSources;
	private TaskScheduler taskScheduler;
	private Path destinationPath;
	private Path temporaryDestinationPath;
	private int backupDelaySecs;

	// a mapping of data source URL -> scheduled future
	private final ConcurrentMap<String, ScheduledFuture<?>> backupFutures;

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
		this.temporaryDestinationPath = Paths.get("var", "work");
		this.backupDelaySecs = DEFAULT_BACKUP_DELAY_SECS;
		this.backupFutures = new ConcurrentHashMap<>(4, 0.9f, 2);
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
		result.add(new BasicTextFieldSettingSpecifier("temporaryDestination",
				temporaryDestinationPath.toString()));
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
		} else if ( event.getTopic().startsWith("net/solarnetwork/dao/") ) {
			String[] components = event.getTopic().split("/");
			Object id = event.getProperty("id");
			String message;
			if ( components.length > 4 ) {
				message = String.format("%s entity [%s] %s", components[3], id, components[4]);
			} else {
				message = "entity changed";
			}
			Object dataSourceUrl = event.getProperty(BaseJdbcGenericDao.DATASOURCE_URL_PROP);
			if ( dataSourceUrl != null ) {
				backupSoon(message, dataSourceUrl.toString());
			} else {
				backupSoon(message);
			}
		}
	}

	private void backupSoon(String reasonMessage) {
		backupSoon(reasonMessage, ALL_DATASOURCE_URL);
	}

	private synchronized void backupSoon(String reasonMessage, String dataSourceUrl) {
		if ( taskScheduler == null ) {
			taskScheduler = new ConcurrentTaskScheduler();
		}

		final ScheduledFuture<?> backupFuture = backupFutures.get(dataSourceUrl);
		final ScheduledFuture<?> allBackupFuture = backupFutures.get(ALL_DATASOURCE_URL);

		if ( backupFuture != null ) {
			if ( !backupFuture.isDone() && !backupFuture.cancel(false) ) {
				return;
			}
		} else if ( allBackupFuture != null ) {
			// all backup previously scheduled; re-schedule all
			if ( !allBackupFuture.isDone() && !allBackupFuture.cancel(false) ) {
				return;
			}
		} else {
			log.info("Scheduling database backup sync after {} event.", reasonMessage);
		}

		final String backupDataSourceUrl = (allBackupFuture != null ? ALL_DATASOURCE_URL
				: dataSourceUrl);

		final Runnable task = () -> {
			backup(backupDataSourceUrl);
			backupFutures.remove(backupDataSourceUrl);
		};

		final Instant ts = Instant
				.ofEpochMilli(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(backupDelaySecs));

		if ( ALL_DATASOURCE_URL.equals(backupDataSourceUrl) ) {
			// remove any specific databases, as we're backing up ALL
			for ( Map.Entry<String, ScheduledFuture<?>> e : backupFutures.entrySet() ) {
				e.getValue().cancel(false);
			}
			backupFutures.clear();
			ScheduledFuture<?> f = taskScheduler.schedule(task, ts);
			backupFutures.compute(backupDataSourceUrl, (k, v) -> {
				if ( v != null && v != f ) {
					v.cancel(false);
				}
				return f;
			});
		} else {
			ScheduledFuture<?> f = taskScheduler.schedule(task, ts);
			backupFutures.compute(backupDataSourceUrl, (k, v) -> {
				if ( v != null && v != f ) {
					v.cancel(false);
				}
				return f;
			});
		}
	}

	/**
	 * Execute the backup.
	 *
	 * <p>
	 * The backup is first performed to the
	 * {@link #getTemporaryDestinationPath()} directory, and assuming no error
	 * occurs is then moved to {@link #getDestinationPath()}.
	 * </p>
	 */
	public void backup() {
		backup(ALL_DATASOURCE_URL);
	}

	private void backup(String dataSourceUrl) {
		final boolean all = ALL_DATASOURCE_URL.equals(dataSourceUrl);
		final Set<String> completedUrls = new LinkedHashSet<>();
		for ( DataSource dataSource : dataSources ) {
			JdbcOperations jdbcOps = new JdbcTemplate(dataSource);
			jdbcOps.execute(new ConnectionCallback<Void>() {

				@Override
				public Void doInConnection(Connection con) throws SQLException, DataAccessException {
					con.setAutoCommit(true);
					String url = con.getMetaData().getURL();
					if ( !all && !dataSourceUrl.equals(url) ) {
						return null;
					}
					if ( completedUrls.contains(url) ) {
						return null;
					}
					String dbName = H2Utils.h2DatabaseName(url);
					if ( dbName == null ) {
						log.debug("Ignoring DataSource {}", url);
						return null;
					}
					log.info("Performing {} database backup...", dbName);
					String archiveName = String.format("%s.zip", dbName);
					Path tmpDestPath = temporaryDestinationPath.resolve(archiveName);
					log.debug("Backing up [{}] database to [{}]", dbName, tmpDestPath);
					try (PreparedStatement stmt = con.prepareStatement("BACKUP TO ?")) {
						stmt.setString(1, tmpDestPath.toString());
						stmt.execute();
					}
					Path destPath = destinationPath.resolve(archiveName);
					try {
						if ( Files.exists(tmpDestPath) && Files.size(tmpDestPath) > 0 ) {
							log.debug("Moving database [{}] temporary backup [{}] to [{}]", dbName,
									tmpDestPath, destPath);
							if ( !Files.isDirectory(destinationPath) ) {
								Files.createDirectories(destinationPath);
							}
							Files.move(tmpDestPath, destPath, StandardCopyOption.ATOMIC_MOVE,
									StandardCopyOption.REPLACE_EXISTING);
							log.info("Backed up [{}] database to [{}]", dbName, destPath);
						}
					} catch ( IOException e ) {
						log.error("Error backup up database [{}] to [{}]: {}", dbName, destPath,
								e.toString());
					}
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
	 * Get the temporary destination path (directory) for the backup archives.
	 *
	 * @return the temporary destination path for the backup archives, never
	 *         {@literal null}
	 */
	public Path getTemporaryDestinationPath() {
		return destinationPath;
	}

	/**
	 * Set the temporary destination path (directory) for the backup archives.
	 *
	 * @param destinationPath
	 *        the path to use; {@literal null} value will be ignored
	 */
	public void setTemporaryDestinationPath(Path destinationPath) {
		if ( destinationPath == null ) {
			return;
		}
		this.destinationPath = destinationPath;
	}

	/**
	 * Get the temporary destination path as a string.
	 *
	 * @return the path, never {@literal null}
	 */
	public String getTemporaryDestination() {
		return destinationPath.toString();
	}

	/**
	 * Set the temporary destination path as a string.
	 *
	 * @param destination
	 *        the path to set
	 */
	public void setTemporaryDestination(String destination) {
		if ( destination == null || destination.isEmpty() ) {
			return;
		}
		setTemporaryDestinationPath(Paths.get(destination));
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
