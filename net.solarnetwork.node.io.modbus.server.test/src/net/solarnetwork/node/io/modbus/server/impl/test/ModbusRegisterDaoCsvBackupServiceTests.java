/* ==================================================================
 * ModbusRegisterDaoCsvBackupServiceTests.java - 6/11/2025 10:47:28 am
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

package net.solarnetwork.node.io.modbus.server.impl.test;

import static net.solarnetwork.node.io.modbus.ModbusDataUtils.parseBytes;
import static net.solarnetwork.util.ByteUtils.encodeHexString;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import net.solarnetwork.dao.BasicBatchResult;
import net.solarnetwork.dao.BatchableDao;
import net.solarnetwork.node.domain.StringDateKey;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterDao;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterEntity;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterKey;
import net.solarnetwork.node.io.modbus.server.impl.ModbusRegisterDaoCsvBackupService;
import net.solarnetwork.node.service.support.CsvConfigurableBackupServiceSupport;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link ModbusRegisterDaoCsvBackupService} class.
 *
 * @author matt
 * @version 1.0
 */
public class ModbusRegisterDaoCsvBackupServiceTests {

	private ModbusRegisterDao dao;
	private PlatformTransactionManager txManager;
	private ModbusRegisterDaoCsvBackupService service;
	private Path tmpDir;

	@Before
	public void setup() throws IOException {
		dao = EasyMock.createMock(ModbusRegisterDao.class);
		txManager = EasyMock.createMock(PlatformTransactionManager.class);
		service = new ModbusRegisterDaoCsvBackupService(new StaticOptionalService<>(dao));

		tmpDir = Files.createTempDirectory("ModbusRegisterDaoCsvBackupServiceTests-");
		service.setBackupDestinationPath(tmpDir.toString());
	}

	private void replayAll() {
		EasyMock.replay(dao, txManager);
	}

	@After
	public void teardown() {
		FileSystemUtils.deleteRecursively(tmpDir.toFile());
		EasyMock.verify(dao, txManager);
	}

	private ModbusRegisterEntity createTestModbusRegisterEntity(String serverId, int unitId,
			ModbusRegisterBlockType blockType, int address, short value) {
		ModbusRegisterEntity result = new ModbusRegisterEntity(
				new ModbusRegisterKey(serverId, unitId, blockType, address),
				Instant.now().truncatedTo(ChronoUnit.MILLIS));
		result.setModified(result.getCreated());
		result.setValue(value);
		return result;
	}

	@Test
	public void exportCsv() throws IOException {
		final Capture<BatchableDao.BatchCallback<ModbusRegisterEntity>> callbackCaptor = Capture
				.newInstance();
		final List<ModbusRegisterEntity> batchData = new ArrayList<>();
		final var batchResult = new BasicBatchResult(1);
		expect(dao.batchProcess(capture(callbackCaptor), anyObject())).andAnswer(() -> {
			final var callback = callbackCaptor.getValue();
			for ( int i = 0; i < 2; i++ ) {
				final var entity = createTestModbusRegisterEntity("test", 1,
						ModbusRegisterBlockType.Holding, i, (short) 0xFF);
				batchData.add(entity);
				callback.handle(entity);
			}
			return batchResult;
		});

		// WHEN
		replayAll();
		StringWriter out = new StringWriter();
		service.exportCsvConfiguration(out);

		// THEN
		StringBuilder expectedCsv = csvHeader();
		for ( ModbusRegisterEntity s : batchData ) {
			appendCsvRow(expectedCsv, s);
		}
		assertThat("CSV generated", out.toString(), is(equalTo(expectedCsv.toString())));
	}

	private StringBuilder csvHeader() {
		return new StringBuilder("ServerId,UnitId,BlockType,Address,Created,Modified,Value\r\n");
	}

	private void appendCsvRow(StringBuilder buf, ModbusRegisterEntity s) {
		buf.append("%s,%s,%s,%s,%s,%s,%s\r\n".formatted(s.getServerId(), s.getUnitId(),
				s.getBlockType().name(), s.getAddress(), s.getCreated(), s.getModified(),
				encodeHexString(parseBytes(new short[] { s.getValue() }, 0), 0, 2, false)));
	}

	private List<StringDateKey> populateBackupFiles(final int count) throws IOException {
		List<StringDateKey> result = new ArrayList<>(count);
		Instant ts = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			String key = "%s"
					.formatted(CsvConfigurableBackupServiceSupport.BACKUP_DATE_FORMATTER.format(ts));
			StringDateKey pk = new StringDateKey(key, ts);
			FileCopyUtils
					.copy(String.valueOf(i),
							new FileWriter(tmpDir.resolve("%s%s.csv".formatted(
									ModbusRegisterDaoCsvBackupService.BACKUP_FILENAME_PREFIX, key))
									.toFile()));
			result.add(pk);
			ts = ts.minus(1L, ChronoUnit.HOURS);
		}
		return result;
	}

	@Test
	public void exportBackup() throws IOException {
		final List<StringDateKey> backupFiles = populateBackupFiles(3);
		final int backupIdx = new SecureRandom().nextInt(3);
		final StringDateKey backup = backupFiles.get(backupIdx);

		// WHEN
		replayAll();
		Reader r = service.getCsvConfigurationBackup(backup.getKey());

		// THEN
		String data = FileCopyUtils.copyToString(r);
		assertThat("Reader to backup file returned", data, is(equalTo(String.valueOf(backupIdx))));
	}

	@Test
	public void importBackup() throws IOException {
		// GIVEN
		final int count = 3;
		final List<ModbusRegisterEntity> inputData = new ArrayList<>(count);
		final StringBuilder inputCsv = csvHeader();
		for ( int i = 0; i < count; i++ ) {
			final var entity = createTestModbusRegisterEntity("test", 1, ModbusRegisterBlockType.Holding,
					i, (short) 0xFF);
			inputData.add(entity);
			appendCsvRow(inputCsv, entity);
			expect(dao.save(entity)).andReturn(entity.getId());
		}

		// WHEN
		replayAll();
		service.importCsvConfiguration(new StringReader(inputCsv.toString()), false);

		// THEN
	}

	@Test
	public void importBackup_replace() throws IOException {
		// GIVEN
		expect(dao.deleteAll()).andReturn(0);

		final int count = 3;
		final StringBuilder inputCsv = csvHeader();
		for ( int i = 0; i < count; i++ ) {
			final var entity = createTestModbusRegisterEntity("test", 1, ModbusRegisterBlockType.Holding,
					i, (short) 0xFF);
			appendCsvRow(inputCsv, entity);
			expect(dao.save(entity)).andReturn(entity.getId());
		}

		// WHEN
		replayAll();
		service.importCsvConfiguration(new StringReader(inputCsv.toString()), true);

		// THEN
	}

}
