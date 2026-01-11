/* ==================================================================
 * BackupServiceSupport.java - 4/10/2017 7:03:53 AM
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

package net.solarnetwork.node.backup;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.Constants;

/**
 * Abstract support class for {@link BackupService} implementations.
 *
 * @author matt
 * @version 1.4
 * @since 1.54
 */
public abstract class BackupServiceSupport implements BackupService {

	/** A date and time format to use with backup keys. */
	public static final String BACKUP_KEY_DATE_FORMAT = "yyyyMMdd'T'HHmmss";

	/**
	 * A date time formatter for backup keys.
	 *
	 * @since 1.4
	 */
	public static final DateTimeFormatter BACKUP_KEY_DATE_FORMATTER = DateTimeFormatter
			.ofPattern(BACKUP_KEY_DATE_FORMAT, Locale.ENGLISH).withZone(ZoneOffset.UTC);

	/**
	 * A pattern to match {@literal node-N-backup-D-Q} where {@literal N} is a
	 * node ID and {@literal D} is a date formatted using
	 * {@link #BACKUP_KEY_DATE_FORMAT} and {@literal Q} is an optional
	 * qualifier.
	 *
	 * <p>
	 * Note that the qualifier and the leading dash is optional, so its
	 * {@link Matcher} group is {@literal 4} (not 3).
	 * </p>
	 */
	public static final Pattern NODE_AND_DATE_BACKUP_KEY_PATTERN = Pattern
			.compile("node-(\\d+)-backup-(\\d{8}T\\d{6})(-(\\w+))?");

	/** The object mapper to use. */
	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private static final String MARKED_BACKUP_PROP_KEY = "key";
	private static final String MARKED_BACKUP_PROP_PROPS = "props";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Default constructor.
	 */
	public BackupServiceSupport() {
		super();
	}

	/**
	 * Get a directory to use for local backup data.
	 *
	 * @return a directory to use for local backup data
	 */
	protected File defaultBackuprDir() {
		String path = System.getProperty(Constants.SYSTEM_PROP_NODE_HOME, null);
		if ( path == null ) {
			path = System.getProperty("java.io.tmpdir");
		} else {
			if ( !path.endsWith("/") ) {
				path += "/";
			}
			path += "var/backups";
		}
		return new File(path);
	}

	/**
	 * Get a file to use for "marked backup" metadata.
	 *
	 * @return the file for marked backup metadata
	 */
	protected File markedBackupForRestoreFile() {
		return new File(defaultBackuprDir(), getKey() + ".RESTORE_ON_BOOT");
	}

	/**
	 * Shortcut for {@link #backupDateFromProps(Date, Map, Pattern, String)}
	 * using default values.
	 *
	 * <p>
	 * The {@link #NODE_AND_DATE_BACKUP_KEY_PATTERN} pattern and
	 * {@link #BACKUP_KEY_DATE_FORMAT} format are used.
	 * </p>
	 *
	 * @param date
	 *        if not {@literal null} then return this value
	 * @param props
	 *        a mapping of properties, which may include a
	 *        {@link BackupManager#BACKUP_KEY} value
	 * @return the date, never {@literal null}
	 * @see #backupDateFromProps(Date, Map, Pattern, String)
	 */
	protected Date backupDateFromProps(Date date, Map<String, String> props) {
		return backupDateFromProps(date, props, NODE_AND_DATE_BACKUP_KEY_PATTERN,
				BACKUP_KEY_DATE_FORMAT);
	}

	/**
	 * Parse a date from a backup key.
	 *
	 * <p>
	 * If no date can be extracted from the given arguments, the current system
	 * time will be returned.
	 * </p>
	 *
	 * @param date
	 *        if not {@literal null} then return this value
	 * @param props
	 *        a mapping of properties, which may include a
	 *        {@link BackupManager#BACKUP_KEY} value
	 * @param nodeIdAndDatePattern
	 *        a regular expression to match a node ID and date from a backup key
	 * @param dateFormat
	 *        the format of the date extracted from the
	 *        {@code nodeIdAndDatePattern}
	 * @return the date, never {@literal null}
	 */
	protected Date backupDateFromProps(Date date, Map<String, String> props,
			Pattern nodeIdAndDatePattern, String dateFormat) {
		if ( date != null ) {
			return date;
		}
		final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		sdf.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
		String backupKey = (props == null ? null : props.get(BackupManager.BACKUP_KEY));
		if ( backupKey != null ) {
			Matcher m = nodeIdAndDatePattern.matcher(backupKey);
			if ( m.find() ) {
				try {
					return sdf.parse(m.group(2));
				} catch ( ParseException e ) {
					log.warn("Unable to parse backup date from key [{}]", backupKey);
				}
			}
		}
		return new Date();
	}

	/**
	 * Shortcut for {@link #backupNodeIdFromProps(Long, Map, Pattern)} using
	 * default values.
	 *
	 * <p>
	 * The {@link #NODE_AND_DATE_BACKUP_KEY_PATTERN} pattern is used.
	 * </p>
	 *
	 * @param nodeId
	 *        if not {@literal null} then return this value
	 * @param props
	 *        a mapping of properties, which may include a
	 *        {@link BackupManager#BACKUP_KEY} value
	 * @return the node ID, or {@literal 0} if not available
	 * @see #backupNodeIdFromProps(Long, Map, Pattern)
	 */
	protected Long backupNodeIdFromProps(Long nodeId, Map<String, String> props) {
		return backupNodeIdFromProps(nodeId, props, NODE_AND_DATE_BACKUP_KEY_PATTERN);
	}

	/**
	 * Parse a node ID from a backup key.
	 *
	 * @param nodeId
	 *        if not {@literal null} then return this value
	 * @param props
	 *        a mapping of properties, which may include a
	 *        {@link BackupManager#BACKUP_KEY} value
	 * @param nodeIdAndDatePattern
	 *        a regular expression to match a node ID and date from a backup key
	 * @return the node ID, or {@literal 0} if not available
	 */
	protected Long backupNodeIdFromProps(Long nodeId, Map<String, String> props,
			Pattern nodeIdAndDatePattern) {
		if ( nodeId != null ) {
			return nodeId;
		}
		Long result = 0L;
		String backupKey = (props == null ? null : props.get(BackupManager.BACKUP_KEY));
		if ( backupKey != null ) {
			Matcher m = nodeIdAndDatePattern.matcher(backupKey);
			if ( m.find() ) {

				try {
					result = Long.valueOf(m.group(1));
				} catch ( NumberFormatException e ) {
					log.warn("Unable to parse node ID from key [{}]", backupKey);
				}
			}
		}
		return result;
	}

	/**
	 * Extract backup identity information from a backup key.
	 *
	 * <p>
	 * This method calls
	 * {@link #identityFromBackupKey(Pattern, String, String)}, passing
	 * {@link #NODE_AND_DATE_BACKUP_KEY_PATTERN} and
	 * {@link #BACKUP_KEY_DATE_FORMAT} for arguments.
	 * </p>
	 *
	 * @param key
	 *        the key to extract the details from
	 * @return the extracted details, or {@literal null} if none found
	 * @since 1.1
	 */
	public static final BackupIdentity identityFromBackupKey(String key) {
		return identityFromBackupKey(NODE_AND_DATE_BACKUP_KEY_PATTERN, BACKUP_KEY_DATE_FORMAT, key);
	}

	/**
	 * Extract backup identity information from a backup key.
	 *
	 * @param nodeIdAndDatePattern
	 *        a pattern that contains groups for a node ID, date, and an
	 *        optional qualifier
	 * @param dateFormat
	 *        the date format to parse the date with
	 * @param key
	 *        the key to extract the details from
	 * @return the extracted details, or {@literal null} if none found
	 * @since 1.1
	 */
	public static final BackupIdentity identityFromBackupKey(Pattern nodeIdAndDatePattern,
			String dateFormat, String key) {
		BackupIdentity result = null;
		if ( key != null ) {
			Matcher m = nodeIdAndDatePattern.matcher(key);
			if ( m.find() ) {
				final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
				sdf.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
				try {
					Long nodeId = Long.valueOf(m.group(1));
					Date date = sdf.parse(m.group(2));
					String qualifier = null;
					if ( m.groupCount() > 2 ) {
						qualifier = m.group(m.groupCount());
					}
					result = new SimpleBackupIdentity(key, date, nodeId, qualifier);
				} catch ( NumberFormatException e ) {
					// ignore
				} catch ( ParseException e ) {
					// ignore
				}
			}
		}
		return result;
	}

	@Override
	public synchronized boolean markBackupForRestore(Backup backup, Map<String, String> props) {
		File markFile = markedBackupForRestoreFile();
		if ( backup == null ) {
			if ( markFile.exists() ) {
				log.info("Clearing marked backup.");
				return markFile.delete();
			}
			return true;
		} else if ( markFile.exists() ) {
			log.warn("Marked backup exists, will not mark again");
			return false;
		} else {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put(MARKED_BACKUP_PROP_KEY, backup.getKey());
			if ( props != null && !props.isEmpty() ) {
				data.put(MARKED_BACKUP_PROP_PROPS, props);
			}
			File parentDir = markFile.getParentFile();
			if ( parentDir != null && !parentDir.exists() ) {
				if ( !parentDir.mkdirs() ) {
					log.warn("Failed to create directory {} for backup restore mark file {}", parentDir,
							markFile.getName());
				}
			}
			try {
				OBJECT_MAPPER.writeValue(markFile, data);
				return true;
			} catch ( IOException e ) {
				log.warn("Failed to create restore mark file {}", markFile, e);
			}
			return false;
		}
	}

	@Override
	public synchronized Backup markedBackupForRestore(Map<String, String> props) {
		File markFile = markedBackupForRestoreFile();
		if ( markFile.exists() ) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> data = OBJECT_MAPPER.readValue(markFile, Map.class);
				if ( data == null || !data.containsKey(MARKED_BACKUP_PROP_KEY) ) {
					return null;
				}
				String key = (String) data.get(MARKED_BACKUP_PROP_KEY);
				if ( props != null && data.get(MARKED_BACKUP_PROP_PROPS) instanceof Map ) {
					@SuppressWarnings("unchecked")
					Map<String, String> dataProps = (Map<String, String>) data
							.get(MARKED_BACKUP_PROP_PROPS);
					props.putAll(dataProps);
				}
				return backupForKey(key);
			} catch ( IOException e ) {
				log.warn("Failed to read restore mark file {}", markFile, e);
			}
		}
		return null;
	}

}
