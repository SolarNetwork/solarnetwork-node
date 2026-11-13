/* ==================================================================
 * AbstractPostgresHistorianTests.java - 10 Nov 2026 10:31:12 pm
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

package net.solarnetwork.node.datumhistorian.jdbc.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.junit.Assume.assumeTrue;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import net.solarnetwork.node.datumhistorian.jdbc.JdbcDatumHistorian;
import net.solarnetwork.node.service.support.DatumStreamIdUtils;
import net.solarnetwork.node.test.NodeTestUtils;

/**
 * Base class for tests that use the Postgres test database.
 *
 * <p>
 * The database is configured by the {@code postgres.url},
 * {@code postgres.username}, and {@code postgres.password} test environment
 * properties, and must have the historian tables created in both the
 * {@link JdbcDatumHistorian#DEFAULT_SCHEMA_NAME} and
 * {@link JdbcDatumHistorian#UNFILTERED_SCHEMA_NAME} schemas. Tests are skipped
 * when the database is not available.
 * </p>
 *
 * <p>
 * Each test runs in a transaction that is rolled back after the test, after
 * which all the historian tables are verified to be empty.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
public abstract class AbstractPostgresHistorianTests {

	/** The schemas with the historian tables. */
	public static final List<String> SCHEMAS = List.of(JdbcDatumHistorian.DEFAULT_SCHEMA_NAME,
			JdbcDatumHistorian.UNFILTERED_SCHEMA_NAME);

	/** The historian tables. */
	public static final List<String> TABLES = List.of("dh_datum", "dh_datum_meta", "dh_datum_type");

	private static DataSource dataSource;
	private static String unavailableReason;

	/** The JDBC template, which joins the test transaction. */
	protected JdbcTemplate jdbcTemplate;

	private DataSourceTransactionManager txManager;
	private TransactionStatus tx;

	@BeforeClass
	public static void setupDataSource() {
		if ( dataSource != null || unavailableReason != null ) {
			return;
		}
		final Properties env = NodeTestUtils.loadEnvironmentProperties();
		final String url = env.getProperty("postgres.url");
		if ( url == null || url.isBlank() ) {
			unavailableReason = "The postgres.url test environment property is not configured.";
			return;
		}
		final PGSimpleDataSource ds = new PGSimpleDataSource();
		ds.setURL(url);
		ds.setUser(env.getProperty("postgres.username"));
		ds.setPassword(env.getProperty("postgres.password"));
		try {
			final JdbcTemplate jdbc = new JdbcTemplate(ds);
			for ( String schema : SCHEMAS ) {
				for ( String table : TABLES ) {
					String name = jdbc.queryForObject("SELECT to_regclass(?)::TEXT", String.class,
							schema + '.' + table);
					if ( name == null ) {
						unavailableReason = "The %s.%s table does not exist.".formatted(schema, table);
						return;
					}
				}
			}
		} catch ( RuntimeException e ) {
			unavailableReason = "The Postgres test database [%s] is not available: %s".formatted(url,
					e.getMessage());
			return;
		}
		dataSource = ds;
	}

	@Before
	public void setupTransaction() {
		assumeTrue(unavailableReason, dataSource != null);
		jdbcTemplate = new JdbcTemplate(dataSource);
		txManager = new DataSourceTransactionManager(dataSource);
		tx = txManager.getTransaction(new DefaultTransactionDefinition());
	}

	@After
	public void rollbackTransaction() {
		if ( tx == null ) {
			return;
		}
		txManager.rollback(tx);
		tx = null;

		// verify everything was rolled back
		final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		for ( String schema : SCHEMAS ) {
			for ( String table : TABLES ) {
				Long count = jdbc.queryForObject("SELECT count(*) FROM %s.%s".formatted(schema, table),
						Long.class);
				// @formatter:off
				and.then(count)
					.as("Table %s.%s is empty after test", schema, table)
					.isZero()
					;
				// @formatter:on
			}
		}
	}

	/**
	 * Get the rows of a table.
	 *
	 * <p>
	 * Array columns are returned as {@link java.sql.Array} instances, so use
	 * {@code array_to_json()} in {@code columns} to compare them more easily.
	 * </p>
	 *
	 * @param schema
	 *        the schema name
	 * @param table
	 *        the table name
	 * @param columns
	 *        the columns to select
	 * @param order
	 *        the column(s) to order by
	 * @return the rows
	 */
	protected List<Map<String, Object>> rows(String schema, String table, String columns,
			String order) {
		return jdbcTemplate.queryForList(
				"SELECT %s FROM %s.%s ORDER BY %s".formatted(columns, schema, table, order));
	}

	/**
	 * Get the UUID stream ID a historian gives a stream.
	 *
	 * @param schema
	 *        the schema name
	 * @param domain
	 *        the stream ID domain of the historian
	 * @param nodeId
	 *        the node ID of the stream
	 * @param locationId
	 *        the location ID of the stream, or {@code null} for a node stream
	 * @param sourceId
	 *        the source ID of the stream
	 * @return the stream ID
	 */
	protected UUID streamId(String schema, int domain, Long nodeId, Long locationId, String sourceId) {
		final Integer localStreamId = jdbcTemplate.queryForObject("""
				SELECT stream_id FROM %s.dh_datum_meta
				WHERE node_id = ? AND source_id = ? AND loc_id IS NOT DISTINCT FROM ?::BIGINT
				""".formatted(schema), Integer.class, nodeId, sourceId, locationId);
		return DatumStreamIdUtils.encodeLocalStreamId(nodeId, domain, localStreamId);
	}

	/**
	 * Get the data source.
	 *
	 * @return the data source
	 */
	protected static DataSource dataSource() {
		return dataSource;
	}

}
