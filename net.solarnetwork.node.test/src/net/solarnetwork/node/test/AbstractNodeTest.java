/* ==================================================================
 * AbstractNodeTest.java - Jul 21, 2013 8:28:35 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
import java.util.Properties;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Base test class for non-transactional unit tests.
 * 
 * @author matt
 * @version 1.1
 */
@ContextConfiguration
public abstract class AbstractNodeTest extends AbstractJUnit4SpringContextTests {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final Properties envProperties = new Properties();

	@Before
	public void loadEnvironmentProperties() {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("env.properties")) {
			envProperties.load(in);
		} catch ( IOException e ) {
			// we'll ignore this
		}
	}

	/**
	 * Create a new embedded database builder.
	 * 
	 * @param environmentTypeKey
	 *        the environment property key for the database type; if not found
	 *        {@literal DERBY} will be assumed
	 * @return the database builder
	 * @since 1.1
	 */
	protected EmbeddedDatabaseBuilder createEmbeddedDatabase(String environmentTypeKey) {
		EmbeddedDatabaseBuilder db = new EmbeddedDatabaseBuilder().generateUniqueName(true);
		EmbeddedDatabaseType dbType = EmbeddedDatabaseType.DERBY;
		if ( envProperties.containsKey(environmentTypeKey) ) {
			String prefix = envProperties.getProperty(environmentTypeKey);
			if ( "h2".equals(prefix) ) {
				dbType = EmbeddedDatabaseType.H2;
			}
		}
		return db.setType(dbType);
	}

}
