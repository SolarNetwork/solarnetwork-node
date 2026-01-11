/* ==================================================================
 * ModbusRegisterDaoCsvBackupService.java - 6/11/2025 8:49:15 am
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

package net.solarnetwork.node.io.modbus.server.impl;

import static net.solarnetwork.node.io.modbus.ModbusDataUtils.parseBytes;
import static net.solarnetwork.util.ByteUtils.encodeHexString;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.dao.BasicBatchOptions;
import net.solarnetwork.dao.BatchableDao.BatchCallback;
import net.solarnetwork.dao.BatchableDao.BatchCallbackResult;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterDao;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterEntity;
import net.solarnetwork.node.service.CsvConfigurableBackupService;
import net.solarnetwork.node.service.support.CsvConfigurableBackupServiceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.ObjectUtils;

/**
 * CSV backup service for Modbus server data.
 *
 * @author matt
 * @version 2.0
 * @since 2.1
 */
public class ModbusRegisterDaoCsvBackupService extends CsvConfigurableBackupServiceSupport
		implements CsvConfigurableBackupService {

	/** The {@code backupDestinationPath} property default value. */
	public static final String DEFAULT_BACKUP_DESTINATION_PATH = "var/modbus-server-bak";

	/** The setting UID. */
	public static final String SETTING_UID = "net.solarnetwork.node.io.modbus.server.ModbusRegisterBackup";

	private static final String BACKUP_IDENT = "modbusserver";

	/** The backup filename prefix. */
	public static final String BACKUP_FILENAME_PREFIX = BACKUP_IDENT + "_";

	private final OptionalService<ModbusRegisterDao> modbusRegisterDao;
	private OptionalService<PlatformTransactionManager> txManager;

	private static enum CsvColumns {
		ServerId,
		UnitId,
		BlockType,
		Address,
		Created,
		Modified,
		Value;
	}

	/**
	 * Constructor.
	 *
	 * @param modbusRegisterDao
	 *        the register DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ModbusRegisterDaoCsvBackupService(OptionalService<ModbusRegisterDao> modbusRegisterDao) {
		super(BACKUP_IDENT, BACKUP_FILENAME_PREFIX);
		setBackupDestinationPath(DEFAULT_BACKUP_DESTINATION_PATH);
		this.modbusRegisterDao = ObjectUtils.requireNonNullArgument(modbusRegisterDao,
				"modbusRegisterDao");
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = CsvConfigurableBackupServiceSupport.settingSpecifiers("");
		result.add(new BasicTextFieldSettingSpecifier("backupDestinationPath",
				DEFAULT_BACKUP_DESTINATION_PATH));
		return result;
	}

	@Override
	protected Instant mostRecentCsvConfigurationModificationDate() {
		final ModbusRegisterDao dao = dao();
		return dao.getMostRecentModificationDate();
	}

	@Override
	protected void importCsvConfiguration(CsvReader<CsvRecord> csvReader, boolean replace)
			throws IOException {
		final PlatformTransactionManager txMgr = OptionalService.service(getTransactionManager());
		final TransactionTemplate tt = (txMgr != null ? new TransactionTemplate(txMgr) : null);
		if ( tt != null ) {
			try {
				tt.executeWithoutResult(tx -> {
					try {
						importCsvConfigurationInternal(csvReader, replace);
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
			importCsvConfigurationInternal(csvReader, replace);
		}
	}

	private void importCsvConfigurationInternal(CsvReader<CsvRecord> csvReader, boolean replace)
			throws IOException {
		final ModbusRegisterDao dao = dao();
		if ( replace ) {
			dao.deleteAll();
		}
		csvReader.skipLines(1);
		for ( CsvRecord row : csvReader ) {
			if ( row.getFieldCount() < 5 ) {
				continue;
			}
			final String serverId = row.getField(CsvColumns.ServerId.ordinal());
			final int unitId = Integer.parseInt(row.getField(CsvColumns.UnitId.ordinal()));
			final ModbusRegisterBlockType blockType = ModbusRegisterBlockType
					.valueOf(row.getField(CsvColumns.BlockType.ordinal()));
			final int address = Integer.parseInt(row.getField(CsvColumns.Address.ordinal()));
			final Instant created = Instant.parse(row.getField(CsvColumns.Created.ordinal()));
			final Instant modified = Instant.parse(row.getField(CsvColumns.Modified.ordinal()));
			final short val = (short) (Integer.parseUnsignedInt(row.getField(CsvColumns.Value.ordinal()),
					16) & 0xFFFF);
			final ModbusRegisterEntity entity = ModbusRegisterEntity.newRegisterEntity(serverId, unitId,
					blockType, address, created, val);
			entity.setModified(modified);
			dao.save(entity);
		}
	}

	@Override
	protected void exportCsvConfiguration(CsvWriter csvWriter) throws IOException {
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
				} else if ( e instanceof UncheckedIOException uioe ) {
					throw uioe.getCause();
				}
				throw e;
			}
		} else {
			exportCsvConfigurationInternal(csvWriter);
		}
	}

	private void exportCsvConfigurationInternal(CsvWriter csvWriter) throws IOException {
		final ModbusRegisterDao dao = dao();
		final BasicBatchOptions opts = new BasicBatchOptions("export", 50, false, Map.of());
		csvWriter.writeRecord(
				Arrays.stream(CsvColumns.values()).map(e -> e.name()).toArray(String[]::new));
		dao.batchProcess(new BatchCallback<ModbusRegisterEntity>() {

			@Override
			public BatchCallbackResult handle(ModbusRegisterEntity entity) {
				String[] row = new String[CsvColumns.values().length];
				row[CsvColumns.ServerId.ordinal()] = entity.getServerId();
				row[CsvColumns.UnitId.ordinal()] = String.valueOf(entity.getUnitId());
				row[CsvColumns.BlockType.ordinal()] = entity.getBlockType().name();
				row[CsvColumns.Address.ordinal()] = String.valueOf(entity.getAddress());
				row[CsvColumns.Created.ordinal()] = entity.getCreated().toString();
				row[CsvColumns.Modified.ordinal()] = entity.getModified().toString();
				row[CsvColumns.Value.ordinal()] = encodeHexString(
						parseBytes(new short[] { entity.getValue() }, 0), 0, 2, false);
				csvWriter.writeRecord(row);
				return BatchCallbackResult.CONTINUE;
			}
		}, opts);
	}

	private ModbusRegisterDao dao() {
		ModbusRegisterDao dao = OptionalService.service(modbusRegisterDao);
		if ( dao != null ) {
			return dao;
		}
		throw new UnsupportedOperationException("ModbusRegisterDao not available.");
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
