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
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileCopyUtils;

/**
 * Base test class for transactional unit tests.
 *
 * @author matt
 * @version 2.0
 */
@ContextConfiguration(locations = "test-context.xml")
@TestExecutionListeners(listeners = { DirtiesContextBeforeModesTestExecutionListener.class,
		ApplicationEventsTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class,
		SqlScriptsTestExecutionListener.class, EventPublishingTestExecutionListener.class },
		inheritListeners = false)
@Transactional
@Rollback
public abstract class AbstractNodeTransactionalTest extends AbstractNodeTest {

	@Autowired
	protected TestEmbeddedDatabase testDatabase;

	@Autowired
	protected DataSource dataSource;

	@Autowired
	protected PlatformTransactionManager txManager;

	protected JdbcTemplate jdbcTemplate;
	protected TransactionTemplate txTemplate;

	@Before
	public void setupNodeTransactionalTest() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		txTemplate = new TransactionTemplate(txManager);
		/*-if ( TestEmbeddedDatabase.POSTGRES_TYPE.equals(testDatabase.getDatabaseType()) ) {
			TestTransaction.start();
			TestTransaction.flagForRollback();
		}*/
	}

	@After
	public void teardownNodeTransactionalTest() {
		/*-if ( testDatabase != null && TestEmbeddedDatabase.POSTGRES_TYPE.equals(testDatabase.getDatabaseType()) ) {
			if ( TestTransaction.isFlaggedForRollback() ) {
				TestTransaction.end();
			}
		}*/
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
