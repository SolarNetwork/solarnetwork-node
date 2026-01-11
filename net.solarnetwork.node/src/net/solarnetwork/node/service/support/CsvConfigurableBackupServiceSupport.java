/* ==================================================================
 * CsvBackupServiceSupport.java - 19/10/2025 9:44:40 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support;

import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.node.domain.StringDateKey;
import net.solarnetwork.node.service.CsvConfigurableBackupService;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Helper base class for {@link CsvConfigurableBackupService} implementations.
 *
 * @author matt
 * @version 2.0
 * @since 4.1
 */
public abstract class CsvConfigurableBackupServiceSupport extends BasicIdentifiable
		implements CsvConfigurableBackupService {

	/** The {@code backupDestinationPath} property default value. */
	public static final String DEFAULT_BACKUP_DESTINATION_PATH = "var";

	/** The {@code backupMaxCount} property default value. */
	public static final int DEFAULT_BACKUP_MAX_COUNT = 5;

	private static final String BACKUP_DATE_FORMAT = "yyyy-MM-dd-HHmmss";

	/** A date formatter for backup file names. */
	public static final DateTimeFormatter BACKUP_DATE_FORMATTER = DateTimeFormatter
			.ofPattern(BACKUP_DATE_FORMAT).withZone(UTC);

	private static final String CSV_FILENAME_EXT = ".csv";
	private static final String GZIP_FILENAME_EXT = ".gz";

	private final String identifier;
	private final String filenamePrefix;
	private final Pattern filenamePattern;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private String backupDestinationPath = DEFAULT_BACKUP_DESTINATION_PATH;
	private int backupMaxCount = DEFAULT_BACKUP_MAX_COUNT;
	private boolean compress = true;

	/**
	 * Constructor.
	 *
	 * @param identifier
	 *        the identifier
	 * @param filenamePrefix
	 *        a prefix to use in CSV file names, before the date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CsvConfigurableBackupServiceSupport(String identifier, String filenamePrefix) {
		super();
		this.identifier = requireNonNullArgument(identifier, "identifier");
		this.filenamePrefix = requireNonNullArgument(filenamePrefix, "filenamePrefix");

		// support both compressed and not compressed
		this.filenamePattern = Pattern.compile('^' + filenamePrefix + "(\\d{4}-\\d{2}-\\d{2}-\\d{6})\\"
				+ CSV_FILENAME_EXT + "(?:\\" + GZIP_FILENAME_EXT + ")?$");
	}

	/**
	 * Get core setting specifiers for configuring this class.
	 *
	 * @param prefix
	 *        the prefix to add
	 * @return the settings
	 */
	public static List<SettingSpecifier> settingSpecifiers(String prefix) {
		prefix = (prefix != null ? prefix : "");

		List<SettingSpecifier> result = new ArrayList<>(4);
		result.add(new BasicTextFieldSettingSpecifier(prefix + "backupMaxCount",
				String.valueOf(DEFAULT_BACKUP_MAX_COUNT)));

		return result;
	}

	@Override
	public String getSettingUid() {
		return null;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return List.of();
	}

	@Override
	public String getCsvConfigurationIdentifier() {
		return identifier;
	}

	@Override
	public final void importCsvConfiguration(Reader in, boolean replace) throws IOException {
		try (CsvReader<CsvRecord> csvReader = CsvReader.builder().allowMissingFields(true)
				.allowExtraFields(true).commentStrategy(CommentStrategy.SKIP)
				.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(), in)) {
			importCsvConfiguration(csvReader, replace);
		}
	}

	/**
	 * Called by {@link #importCsvConfiguration(Reader, boolean)} to import CSV
	 * configuration.
	 *
	 * @param csvReader
	 *        the CSV reader
	 * @param replace
	 *        {@code true} to replace all existing configuration
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 2.0
	 */
	protected abstract void importCsvConfiguration(CsvReader<CsvRecord> csvReader, boolean replace)
			throws IOException;

	@Override
	public final void exportCsvConfiguration(Writer out) throws IOException {
		try (CsvWriter csvWriter = CsvWriter.builder().build(out)) {
			exportCsvConfiguration(csvWriter);
		}
	}

	/**
	 * Called by {@link #exportCsvConfiguration(Writer)} to export CSV
	 * configuration.
	 *
	 * @param csvWriter
	 *        the CSV writer
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 2.0
	 */
	protected abstract void exportCsvConfiguration(CsvWriter csvWriter) throws IOException;

	/**
	 * Get the most recent CSV configuration modification date.
	 *
	 * <p>
	 * This method returns the current time. Extending classes can override to
	 * provide an actual modification date, if known.
	 * </p>
	 *
	 * @return the most recent modification date of the configuration that is
	 *         exported as CSV
	 */
	protected Instant mostRecentCsvConfigurationModificationDate() {
		return Instant.now();
	}

	/**
	 * Get the most recent CSV configuration backup date.
	 *
	 * @return the most recent backup date, or the epoch if no backup is
	 *         available
	 */
	protected Instant mostRecentCsvConfigurationBackupDate() {
		List<StringDateKey> backups = getAvailableCsvConfigurationBackups();
		return (backups == null || backups.isEmpty() ? Instant.ofEpochMilli(0)
				: backups.get(backups.size() - 1).getTimestamp());
	}

	@Override
	public StringDateKey backupCsvConfiguration() {
		final Instant mrd = mostRecentCsvConfigurationModificationDate();
		final Instant lastBackupDate = mostRecentCsvConfigurationBackupDate();
		if ( mrd == null || (lastBackupDate != null && lastBackupDate.isAfter(mrd)) ) {
			log.debug("Settings unchanged since last backup on {}", lastBackupDate);
			return null;
		}
		final Instant backupDate = Instant.now();
		final String backupDateKey = BACKUP_DATE_FORMATTER.format(backupDate);
		final Path dir = Paths.get(backupDestinationPath);
		if ( !Files.isDirectory(dir) ) {
			try {
				Files.createDirectories(dir);
			} catch ( IOException e ) {
				log.warn("Error creating CSV backup directory [{}], unable to create backup: {}", dir,
						e.getMessage());
				return null;
			}
		}

		final Path f = dir.resolve(
				filenamePrefix + backupDateKey + CSV_FILENAME_EXT + (compress ? GZIP_FILENAME_EXT : ""));
		log.info("Backing up CSV to {}", f);
		try (OutputStream out = Files.newOutputStream(f);
				Writer writer = new OutputStreamWriter(compress ? new GZIPOutputStream(out) : out,
						StandardCharsets.UTF_8)) {
			exportCsvConfiguration(writer);
		} catch ( IOException e ) {
			log.error("Unable to create CSV backup {}: {}", f, e.getMessage());
			return null;
		}

		// clean out older backups
		Path[] files;
		try {
			files = Files.find(dir, 1, (path, attr) -> {
				return Files.isRegularFile(path)
						&& filenamePattern.matcher(path.getFileName().toString()).matches();
			}).toArray(Path[]::new);
			if ( files != null && files.length > backupMaxCount ) {
				Arrays.sort(files, Comparator.reverseOrder());
				for ( int i = backupMaxCount; i < files.length; i++ ) {
					try {
						Files.delete(files[i]);
					} catch ( IOException e ) {
						log.warn("Unable to delete old CSV backup file {}: {}", files[i],
								e.getMessage());
					}
				}
			}
		} catch ( IOException e ) {
			log.warn("Error cleaning up older CSV backup files: {}", e.toString());
		}

		return new StringDateKey(backupDateKey, backupDate);
	}

	@Override
	public List<StringDateKey> getAvailableCsvConfigurationBackups() {
		final Path dir = Paths.get(backupDestinationPath);
		if ( !Files.isDirectory(dir) ) {
			return List.of();
		}
		try {
			return Files.find(dir, 1, (path, attr) -> {
				return Files.isRegularFile(path)
						&& filenamePattern.matcher(path.getFileName().toString()).matches();
			}).map(path -> {
				Matcher m = filenamePattern.matcher(path.getFileName().toString());
				String key = null;
				Instant ts = null;
				if ( m.matches() ) {
					key = m.group(1);
					try {
						ts = BACKUP_DATE_FORMATTER.parse(key, Instant::from);
					} catch ( DateTimeParseException e ) {
						log.warn("Unable to parse backup file date from filename {}: {}",
								path.getFileName(), e.getMessage());
					}
				}
				return new StringDateKey(key, ts);
			}).sorted().toList();
		} catch ( IOException e ) {
			log.warn("Unable to list backup files: {}", e.getMessage());
			return List.of();
		}
	}

	@Override
	public Reader getCsvConfigurationBackup(final String backupKey) {
		final String uncompressedFileName = filenamePrefix + backupKey + CSV_FILENAME_EXT;
		final String compressedFileName = uncompressedFileName + GZIP_FILENAME_EXT;
		final boolean compress = isCompress();
		final String backupDestinationPath = getBackupDestinationPath();
		final List<Path> possibleFiles = List.of(
				Paths.get(backupDestinationPath, compress ? compressedFileName : uncompressedFileName),
				Paths.get(backupDestinationPath, compress ? uncompressedFileName : compressedFileName));
		for ( Path file : possibleFiles ) {
			if ( Files.isReadable(file) ) {
				try {
					InputStream in = Files.newInputStream(file);
					return new InputStreamReader(
							file.getFileName().toString().endsWith(GZIP_FILENAME_EXT)
									? new GZIPInputStream(in)
									: in,
							StandardCharsets.UTF_8);
				} catch ( IOException e ) {
					log.warn("Error reading backup file [{}]: {}", file, e.toString());
				}
			}
		}
		return null;
	}

	/**
	 * Get the backup destination path.
	 *
	 * @return the path
	 */
	public String getBackupDestinationPath() {
		return backupDestinationPath;
	}

	/**
	 * Set the backup destination path.
	 *
	 * @param backupDestinationPath
	 *        the path to set
	 */
	public void setBackupDestinationPath(String backupDestinationPath) {
		this.backupDestinationPath = backupDestinationPath;
	}

	/**
	 * Get the backup maximum count.
	 *
	 * @return the count
	 */
	public int getBackupMaxCount() {
		return backupMaxCount;
	}

	/**
	 * Set the backup maximum count.
	 *
	 * @param backupMaxCount
	 *        the count to set
	 */
	public void setBackupMaxCount(int backupMaxCount) {
		this.backupMaxCount = backupMaxCount;
	}

	/**
	 * Get the compress mode.
	 *
	 * @return the compress mode
	 */
	public boolean isCompress() {
		return compress;
	}

	/**
	 * Set the compress mode.
	 *
	 * @param compress
	 *        {@code true} to compress CSV files
	 */
	public void setCompress(boolean compress) {
		this.compress = compress;
	}

}
