/* ==================================================================
 * DefaultLocalStateServiceTests.java - 20/10/2025 9:47:12 am
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

package net.solarnetwork.node.runtime.test;

import static java.util.UUID.randomUUID;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
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
import net.solarnetwork.node.dao.LocalStateDao;
import net.solarnetwork.node.domain.LocalState;
import net.solarnetwork.node.domain.LocalStateType;
import net.solarnetwork.node.domain.StringDateKey;
import net.solarnetwork.node.runtime.DefaultLocalStateService;
import net.solarnetwork.node.service.support.CsvConfigurableBackupServiceSupport;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DefaultLocalStateService} class.
 *
 * @author matt
 * @version 1.0
 */
public class DefaultLocalStateServiceTests {

	private LocalStateDao localStateDao;
	private PlatformTransactionManager txManager;
	private DefaultLocalStateService service;
	private Path tmpDir;

	@Before
	public void setup() throws IOException {
		localStateDao = EasyMock.createMock(LocalStateDao.class);
		txManager = EasyMock.createMock(PlatformTransactionManager.class);
		service = new DefaultLocalStateService(new StaticOptionalService<>(localStateDao));

		tmpDir = Files.createTempDirectory("DefaultLocalStateServiceTests-");
		service.getBackupService().setBackupDestinationPath(tmpDir.toString());
	}

	private void replayAll() {
		EasyMock.replay(localStateDao, txManager);
	}

	@After
	public void teardown() {
		FileSystemUtils.deleteRecursively(tmpDir.toFile());
		EasyMock.verify(localStateDao, txManager);
	}

	@Test
	public void delete() {
		// GIVEN
		final String key = UUID.randomUUID().toString();
		localStateDao.delete(new LocalState(key, null));

		// WHEN
		replayAll();
		service.deleteLocalState(key);
	}

	@Test
	public void getAvailable() {
		// GIVEN
		final List<LocalState> all = List.of(new LocalState(UUID.randomUUID().toString(), null));
		expect(localStateDao.getAll(null)).andReturn(all);

		// WHEN
		replayAll();
		Collection<LocalState> result = service.getAvailableLocalState();

		// THEN
		assertThat("DAO result returned", result, is(sameInstance(all)));
	}

	@Test
	public void forKey() {
		final String key = randomUUID().toString();
		final LocalState entity = new LocalState(UUID.randomUUID().toString(), null);
		expect(localStateDao.get(key)).andReturn(entity);

		// WHEN
		replayAll();
		LocalState result = service.localStateForKey(key);

		// THEN
		assertThat("DAO result returned", result, is(sameInstance(entity)));
	}

	@Test
	public void save() {
		final LocalState state = new LocalState(UUID.randomUUID().toString(), null);

		final String key = randomUUID().toString();
		expect(localStateDao.save(same(state))).andReturn(key);

		final LocalState entity = new LocalState(UUID.randomUUID().toString(), null);
		expect(localStateDao.get(key)).andReturn(entity);

		// WHEN
		replayAll();
		LocalState result = service.saveLocalState(state);

		// THEN
		assertThat("DAO result returned", result, is(sameInstance(entity)));
	}

	@Test
	public void exportCsv() throws IOException {
		final Capture<BatchableDao.BatchCallback<LocalState>> callbackCaptor = Capture.newInstance();
		final List<LocalState> batchData = new ArrayList<>();
		final var batchResult = new BasicBatchResult(1);
		expect(localStateDao.batchProcess(capture(callbackCaptor), anyObject())).andAnswer(() -> {
			final var callback = callbackCaptor.getValue();
			for ( int i = 0; i < 2; i++ ) {
				final var state = new LocalState(randomUUID().toString(),
						Instant.now().truncatedTo(ChronoUnit.SECONDS), LocalStateType.Int32, i);
				state.setModified(state.getCreated());
				batchData.add(state);
				callback.handle(state);
			}
			return batchResult;
		});

		// WHEN
		replayAll();
		StringWriter out = new StringWriter();
		service.exportCsvConfiguration(out);

		// THEN
		StringBuilder expectedCsv = csvHeader();
		for ( LocalState s : batchData ) {
			appendCsvRow(expectedCsv, s);
		}
		assertThat("CSV generated", out.toString(), is(equalTo(expectedCsv.toString())));
	}

	private StringBuilder csvHeader() {
		return new StringBuilder("Key,Created,Modified,Type,Value\r\n");
	}

	private void appendCsvRow(StringBuilder buf, LocalState s) {
		buf.append("%s,%s,%s,%s,%s\r\n".formatted(s.getKey(), s.getCreated(), s.getModified(),
				s.getType(), s.getValue()));
	}

	private List<StringDateKey> populateBackupFiles(final int count) throws IOException {
		List<StringDateKey> result = new ArrayList<>(count);
		Instant ts = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			String key = "%s"
					.formatted(CsvConfigurableBackupServiceSupport.BACKUP_DATE_FORMATTER.format(ts));
			StringDateKey pk = new StringDateKey(key, ts);
			FileCopyUtils.copy(String.valueOf(i), new FileWriter(tmpDir
					.resolve("%s%s.csv".formatted(DefaultLocalStateService.BACKUP_FILENAME_PREFIX, key))
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
		final List<LocalState> inputData = new ArrayList<>(count);
		final StringBuilder inputCsv = csvHeader();
		for ( int i = 0; i < count; i++ ) {
			var state = new LocalState(randomUUID().toString(),
					Instant.now().truncatedTo(ChronoUnit.SECONDS), LocalStateType.Int32, i);
			state.setModified(state.getCreated());
			inputData.add(state);
			appendCsvRow(inputCsv, state);
			expect(localStateDao.compareAndChange(state)).andReturn(state);
		}

		// WHEN
		replayAll();
		service.importCsvConfiguration(new StringReader(inputCsv.toString()), false);

		// THEN
	}

	@Test
	public void importBackup_replace() throws IOException {
		// GIVEN
		expect(localStateDao.deleteAll()).andReturn(0);

		final int count = 3;
		final List<LocalState> inputData = new ArrayList<>(count);
		final StringBuilder inputCsv = csvHeader();
		for ( int i = 0; i < count; i++ ) {
			final var state = new LocalState(randomUUID().toString(),
					Instant.now().truncatedTo(ChronoUnit.SECONDS), LocalStateType.Int32, i);
			state.setModified(state.getCreated());
			inputData.add(state);
			appendCsvRow(inputCsv, state);
			expect(localStateDao.save(state)).andReturn(state.getKey());
		}

		// WHEN
		replayAll();
		service.importCsvConfiguration(new StringReader(inputCsv.toString()), true);

		// THEN
	}

}
