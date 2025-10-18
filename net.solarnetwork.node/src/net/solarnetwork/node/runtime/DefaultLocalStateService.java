/* ==================================================================
 * DefaultLocalStateService.java - 15/04/2025 9:11:27 am
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

package net.solarnetwork.node.runtime;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvListWriter;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.BasicBatchOptions;
import net.solarnetwork.dao.BatchableDao.BatchCallback;
import net.solarnetwork.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.node.dao.LocalStateDao;
import net.solarnetwork.node.domain.LocalState;
import net.solarnetwork.node.domain.LocalStateType;
import net.solarnetwork.node.domain.StringDateKey;
import net.solarnetwork.node.service.LocalStateService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.service.support.CsvConfigurableBackupServiceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Default implementation of {@link LocalStateService}.
 *
 * @author matt
 * @version 1.1
 */
public final class DefaultLocalStateService extends BaseIdentifiable
		implements LocalStateService, SettingSpecifierProvider {

	/** The {@code backupDestinationPath} property default value. */
	public static final String DEFAULT_BACKUP_DESTINATION_PATH = "var/localstate-bak";

	/** The setting UID. */
	public static final String SETTING_UID = "net.solarnetwork.node.LocalStateService";

	private static final String BACKUP_FILENAME_PREFIX = "localstate_";
	private static final String BACKUP_SERVICE_SETTINGS_PREFIX = "backupService.";

	private final OptionalService<LocalStateDao> localStateDao;
	private final BackupService backupService;
	private OptionalService<PlatformTransactionManager> txManager;

	/**
	 * Constructor.
	 *
	 * @param localStateDao
	 *        the DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DefaultLocalStateService(OptionalService<LocalStateDao> localStateDao) {
		super();
		this.localStateDao = requireNonNullArgument(localStateDao, "localStateDao");
		this.backupService = new BackupService();
	}

	private static enum CsvColumns {
		Key,
		Created,
		Modified,
		Type,
		Value,;
	}

	private final class BackupService extends CsvConfigurableBackupServiceSupport {

		/**
		 * Constructor.
		 */
		private BackupService() {
			super(BACKUP_FILENAME_PREFIX);
			setBackupDestinationPath(DefaultLocalStateService.DEFAULT_BACKUP_DESTINATION_PATH);
		}

		private static List<SettingSpecifier> settingSpecifiers() {
			List<SettingSpecifier> result = CsvConfigurableBackupServiceSupport
					.settingSpecifiers(BACKUP_SERVICE_SETTINGS_PREFIX);
			result.add(new BasicTextFieldSettingSpecifier(
					BACKUP_SERVICE_SETTINGS_PREFIX + "backupDestinationPath",
					DefaultLocalStateService.DEFAULT_BACKUP_DESTINATION_PATH));
			return result;
		}

		@Override
		protected void importCsvConfiguration(ICsvListReader csvReader, String[] headers,
				boolean replace) throws IOException {
			final PlatformTransactionManager txMgr = OptionalService.service(getTransactionManager());
			final TransactionTemplate tt = (txMgr != null ? new TransactionTemplate(txMgr) : null);
			if ( tt != null ) {
				try {
					tt.executeWithoutResult(tx -> {
						try {
							importCsvConfigurationInternal(csvReader, headers, replace);
						} catch ( IOException e ) {
							throw new RuntimeException(e);
						}
					});
				} catch ( RuntimeException e ) {
					if ( e.getCause() instanceof IOException ioe ) {
						throw ioe;
					}
					throw e;
				}
			} else {
				importCsvConfigurationInternal(csvReader, headers, replace);
			}
		}

		private void importCsvConfigurationInternal(ICsvListReader csvReader, String[] headers,
				boolean replace) throws IOException {
			final LocalStateDao dao = dao();
			if ( replace ) {
				dao.deleteAll();
			}
			for ( List<String> row = csvReader.read(); row != null; ) {
				if ( row.size() < 5 ) {
					continue;
				}
				final String key = row.get(CsvColumns.Key.ordinal());
				if ( key == null || key.startsWith("#") ) {
					continue;
				}
				final Instant created = Instant.parse(row.get(CsvColumns.Created.ordinal()));
				final Instant modified = Instant.parse(row.get(CsvColumns.Modified.ordinal()));
				final LocalStateType type = LocalStateType.forKey(row.get(CsvColumns.Type.ordinal()));
				final String val = row.get(CsvColumns.Value.ordinal());
				final LocalState state = new LocalState(key, created, type, val);
				state.setModified(modified);
				if ( replace ) {
					dao.save(state);
				} else {
					dao.compareAndChange(state);
				}
			}
		}

		@Override
		protected void exportCsvConfiguration(ICsvListWriter csvWriter) throws IOException {
			final PlatformTransactionManager txMgr = OptionalService.service(getTransactionManager());
			final TransactionTemplate tt = (txMgr != null ? new TransactionTemplate(txMgr) : null);
			if ( tt != null ) {
				try {
					tt.executeWithoutResult(tx -> {
						try {
							exportCsvConfigurationInternal(csvWriter);
						} catch ( IOException e ) {
							throw new RuntimeException(e);
						}
					});
				} catch ( RuntimeException e ) {
					if ( e.getCause() instanceof IOException ioe ) {
						throw ioe;
					}
					throw e;
				}
			} else {
				exportCsvConfigurationInternal(csvWriter);
			}
		}

		private void exportCsvConfigurationInternal(ICsvListWriter csvWriter) throws IOException {
			final LocalStateDao dao = dao();
			final BasicBatchOptions opts = new BasicBatchOptions("export", 50, false, Map.of());
			csvWriter.writeHeader(
					Arrays.stream(CsvColumns.values()).map(e -> e.name()).toArray(String[]::new));
			dao.batchProcess(new BatchCallback<LocalState>() {

				@Override
				public BatchCallbackResult handle(LocalState state) {
					final Object val = state.getValue();
					if ( val == null ) {
						return BatchCallbackResult.CONTINUE;
					}
					String[] row = new String[CsvColumns.values().length];
					row[CsvColumns.Key.ordinal()] = state.getKey();
					row[CsvColumns.Created.ordinal()] = state.getCreated().toString();
					row[CsvColumns.Modified.ordinal()] = state.getModified().toString();
					row[CsvColumns.Type.ordinal()] = state.getType().name();
					row[CsvColumns.Value.ordinal()] = val instanceof Map<?, ?> map
							? JsonUtils.getJSONString(map)
							: val.toString();
					try {
						csvWriter.write(row);
					} catch ( IOException e ) {
						throw new RuntimeException(e);
					}
					return BatchCallbackResult.CONTINUE;
				}
			}, opts);
		}
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return BackupService.settingSpecifiers();
	}

	private LocalStateDao dao() {
		LocalStateDao dao = OptionalService.service(localStateDao);
		if ( dao != null ) {
			return dao;
		}
		throw new UnsupportedOperationException("LocalStateDao not available.");
	}

	@Override
	public Collection<LocalState> getAvailableLocalState() {
		return dao().getAll(null);
	}

	@Override
	public LocalState localStateForKey(String key) {
		return dao().get(key);
	}

	@Override
	public LocalState saveLocalState(LocalState state) {
		final LocalStateDao dao = dao();
		return dao.get(dao.save(state));
	}

	@Override
	public void deleteLocalState(String key) {
		dao().delete(new LocalState(key, null));
	}

	@Override
	public void importCsvConfiguration(Reader in, boolean replace) throws IOException {
		backupService.importCsvConfiguration(in, replace);
	}

	@Override
	public void exportCsvConfiguration(Writer out) throws IOException {
		backupService.exportCsvConfiguration(out);
	}

	@Override
	public StringDateKey backupCsvConfiguration() {
		return backupService.backupCsvConfiguration();
	}

	@Override
	public List<StringDateKey> getAvailableCsvConfigurationBackups() {
		return backupService.getAvailableCsvConfigurationBackups();
	}

	@Override
	public Reader getCsvConfigurationBackup(StringDateKey backupId) {
		return backupService.getCsvConfigurationBackup(backupId);
	}

	/**
	 * Get the backup service.
	 *
	 * @return the backup service
	 */
	public BackupService getBackupService() {
		return backupService;
	}

	/**
	 * Get the transaction manager.
	 *
	 * @return the transaction manager
	 */
	public OptionalService<PlatformTransactionManager> getTransactionManager() {
		return txManager;
	}

	/**
	 * Set the transaction manager.
	 *
	 * @param txManager
	 *        the manager to set
	 */
	public void setTransactionManager(OptionalService<PlatformTransactionManager> txManager) {
		this.txManager = txManager;
	}

}
