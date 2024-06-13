/* ===================================================================
 * DatabaseSetup.java
 *
 * Created Dec 1, 2009 1:21:24 PM
 *
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.node.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * Class to initialize a database for first-time use by a Solar Node.
 *
 * <p>
 * The {@link DatabaseSetup#init()} method should be called once during
 * application startup, before any other JDBC-based DAOs attempt to initialize
 * or access the database.
 * </p>
 *
 * <p>
 * The configurable properties of this class are:
 * </p>
 *
 * <dl class="class-properties">
 * <dt>dataSource</dt>
 * <dd>The DataSource to use for accessing the database with.</dd>
 *
 * <dt>initSqlResource</dt>
 * <dd>A Resource to a SQL script that will initialize the database for the
 * first time, when it is not found to exist already. Defaults to a
 * classpath-relative resource named {@link #DEFAULT_INIT_SQL_RESOURCE}.</dd>
 * </dl>
 *
 * @author matt
 * @version 2.0
 */
public class DatabaseSetup {

	/**
	 * The default classpath resource for the {@code initSqlResource} property.
	 */
	public static final String DEFAULT_INIT_SQL_RESOURCE = "derby-init.sql";

	/** The default value for the {@code sqlGetTablesVersion} property. */
	public static final String DEFAULT_SQL_GET_TABLES_VERSION = "SELECT svalue FROM "
			+ JdbcDaoConstants.SCHEMA_NAME + "." + JdbcDaoConstants.TABLE_SETTINGS + " WHERE skey = "
			+ "'solarnode.sn_settings.version'";

	private static final int TABLES_VERSION = 6;

	private DataSource dataSource = null;
	private Resource initSqlResource = new ClassPathResource(DEFAULT_INIT_SQL_RESOURCE,
			DatabaseSetup.class);

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 */
	public DatabaseSetup() {
		super();
	}

	/**
	 * Check for the existence of the database, and if not found create and
	 * initialize it.
	 */
	public void init() {
		JdbcDao dao = new JdbcDao();
		try {
			dao.init();
		} catch ( RuntimeException e ) {
			log.error("Error initializing database", e);
		}
	}

	/**
	 * Helper implementation of AbstractJdbcDao so we can make use of some of
	 * its functionality in setting up the database.
	 */
	private class JdbcDao extends AbstractJdbcDao<NodeDatum> {

		private JdbcDao() {
			setDataSource(DatabaseSetup.this.dataSource);
			setInitSqlResource(DatabaseSetup.this.initSqlResource);
			setSchemaName(JdbcDaoConstants.SCHEMA_NAME);
			setTableName(JdbcDaoConstants.TABLE_SETTINGS);
			setTablesVersion(TABLES_VERSION);
			setSqlGetTablesVersion(DEFAULT_SQL_GET_TABLES_VERSION);
			setSqlResourcePrefix("derby-init");
		}

		@Override
		public MessageSource getMessageSource() {
			return null;
		}

		private static final String CREATE_SCHEMA_SQL_TEMPLATE = "CREATE SCHEMA %s";

		@Override
		protected void verifyDatabaseExists(String schema, String table, Resource initSql) {
			// first verify our schema exists
			getJdbcTemplate().execute(new ConnectionCallback<Object>() {

				@Override
				public Object doInConnection(Connection con) throws SQLException, DataAccessException {
					if ( !schemaExists(con, schema) ) {
						PreparedStatement stmt = null;
						try {
							stmt = con
									.prepareStatement(String.format(CREATE_SCHEMA_SQL_TEMPLATE, schema));
							log.info("Initializing database schema [{}]", schema);
							stmt.executeUpdate();
						} finally {
							if ( stmt != null ) {
								stmt.close();
							}
						}
					}
					return null;
				}
			});
			super.verifyDatabaseExists(schema, table, initSql);
		}

	}

	/**
	 * Get the data source.
	 *
	 * @return the dataSource
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Set the data source.
	 *
	 * @param dataSource
	 *        the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Get the initializing SQL resource.
	 *
	 * @return the initSqlResource
	 */
	public Resource getInitSqlResource() {
		return initSqlResource;
	}

	/**
	 * Set the initializing SQL resource.
	 *
	 * @param initSqlResource
	 *        the initSqlResource to set
	 */
	public void setInitSqlResource(Resource initSqlResource) {
		this.initSqlResource = initSqlResource;
	}

}
