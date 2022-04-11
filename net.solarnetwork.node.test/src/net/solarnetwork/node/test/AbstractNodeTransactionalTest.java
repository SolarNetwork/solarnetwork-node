/* ==================================================================
 * AbstractNodeTransactionalTest.java - Jan 11, 2010 9:59:13 AM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.test;

import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.Before;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileCopyUtils;

/**
 * Base test class for transactional unit tests.
 * 
 * @author matt
 * @version 1.2
 */
public abstract class AbstractNodeTransactionalTest extends AbstractNodeTest {

	protected TestEmbeddedDatabase dataSource;
	protected JdbcTemplate jdbcTemplate;
	protected PlatformTransactionManager txManager;
	protected TransactionTemplate txTemplate;

	@Before
	public void setupNodeTransactionalTest() {
		dataSource = createEmbeddedDatabase("db.type");
		jdbcTemplate = new JdbcTemplate(dataSource);
		txManager = new DataSourceTransactionManager(dataSource);
		txTemplate = new TransactionTemplate(txManager);
	}

	@After
	public void teardownNodeTransactionalTest() {
		dataSource.shutdown();
	}

	/**
	 * Execute the given SQL script.
	 * <p>
	 * Use with caution outside of a transaction!
	 * <p>
	 * The script will normally be loaded by classpath.
	 * <p>
	 * <b>Do not use this method to execute DDL if you expect rollback.</b>
	 * 
	 * @param sqlResourcePath
	 *        the Spring resource path for the SQL script
	 * @param continueOnError
	 *        whether or not to continue without throwing an exception in the
	 *        event of an error
	 * @throws DataAccessException
	 *         if there is an error executing a statement
	 * @see ResourceDatabasePopulator
	 */
	protected void executeSqlScript(String sqlResourcePath, boolean continueOnError)
			throws DataAccessException {
		Resource resource;
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(sqlResourcePath)) {
			resource = new ByteArrayResource(FileCopyUtils.copyToByteArray(in));
		} catch ( IOException e ) {
			throw new RuntimeException(
					String.format("Error loading SQL from [%s]: %s", sqlResourcePath, e.toString()), e);
		}
		new ResourceDatabasePopulator(continueOnError, false, null, resource).execute(dataSource);
	}

}
