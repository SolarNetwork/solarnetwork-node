/* ==================================================================
 * JdbcTableBackupResourceProviderTests.java - 11/01/2026 11:30:42 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.ResourceBackupResource;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.JdbcTableBackupResourceProvider;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

/**
 * Test cases for the {@link JdbcTableBackupResourceProvider} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcTableBackupResourceProviderTests extends AbstractNodeTransactionalTest {

	@BeforeTransaction
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		executeSqlScript("net/solarnetwork/node/dao/jdbc/test/init-csv-data.sql", false);
	}

	private void importData(final String tableName) {
		final JdbcTableBackupResourceProvider provider = new JdbcTableBackupResourceProvider("test",
				jdbcTemplate, txTemplate,
				new ConcurrentTaskExecutor(Executors.newSingleThreadExecutor()));

		final BackupResource rsrc = new ResourceBackupResource(
				new ClassPathResource("csv-data-01.csv", getClass()), tableName + ".csv",
				provider.getKey());

		provider.restoreBackupResource(rsrc);
	}

	@Test
	public void importTable() throws Exception {
		final String tableName = "SOLARNODE.TEST_CSV_IO";
		importData(tableName);
		final AtomicInteger row = new AtomicInteger(0);
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		final Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		jdbcTemplate.query(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				return con.prepareStatement(
						"select PK,STR,INUM,DNUM,TS from solarnode.test_csv_io order by pk");
			}
		}, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				row.incrementAndGet();
				final int i = row.intValue();
				assertEquals("PK " + i, i, rs.getLong(1));
				if ( i == 2 ) {
					assertNull("STR " + i, rs.getString(2));
				} else {
					assertEquals("STR " + i, "s0" + i, rs.getString(2));
				}
				if ( i == 3 ) {
					assertNull("INUM " + i, rs.getObject(3));
				} else {
					assertEquals("INUM " + i, i, rs.getInt(3));
				}
				if ( i == 4 ) {
					assertNull("DNUM " + i, rs.getObject(4));
				} else {
					assertEquals("DNUM " + i, i, rs.getDouble(4), 0.01);
				}
				if ( i == 5 ) {
					assertNull("TS " + i, rs.getObject(5));
				} else {
					Timestamp ts = rs.getTimestamp(5, utcCalendar);
					try {
						assertEquals("TS " + i, sdf.parse("2016-10-0" + i + "T12:01:02.345Z"), ts);
					} catch ( ParseException e ) {
						// should not get here
					}
				}
			}
		});

		// @formatter:off
		then(row.intValue())
			.as("Imported all rows")
			.isEqualTo(5)
			;
		// @formatter:on
	}

	@Test
	public void updateTable() throws Exception {
		final String tableName = "SOLARNODE.TEST_CSV_IO";
		importData(tableName);

		txTemplate.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// verify the savepoint logic works to ignore inserts on data that already exists
				importData(tableName);
			}
		});
	}

	@Test
	public void exportTable() throws Exception {
		// GIVEN
		final String tableName = "SOLARNODE.TEST_CSV_IO";
		importData(tableName);

		// because export uses a pipe, need to commit transaction to see the data
		TestTransaction.flagForCommit();
		TestTransaction.end();

		final JdbcTableBackupResourceProvider provider = new JdbcTableBackupResourceProvider("test",
				jdbcTemplate, txTemplate,
				new ConcurrentTaskExecutor(Executors.newSingleThreadExecutor()));
		provider.setTableNames(new String[] { "SOLARNODE.TEST_CSV_IO" });

		// WHEN
		Iterable<BackupResource> rsrcs = provider.getBackupResources();

		// THEN
		// @formatter:off
		then(rsrcs)
			.as("One table exported")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				then(r.getInputStream())
					.hasSameContentAs(getClass().getResourceAsStream("csv-data-01.csv"))
					;
			})
			;
		// @formatter:on
	}
}
