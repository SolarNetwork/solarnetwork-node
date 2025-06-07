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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.TimeZone;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Base test class for non-transactional unit tests.
 *
 * @author matt
 * @version 1.3
 */
@ContextConfiguration
public abstract class AbstractNodeTest extends AbstractJUnit4SpringContextTests {

	/** A test Node ID. */
	public static final Long TEST_NODE_ID = -5555L;

	/** A test Weather Source ID. */
	public static final Long TEST_WEATHER_SOURCE_ID = -5554L;

	/** A test Location ID. */
	public static final Long TEST_LOC_ID = -5553L;

	/** A test TimeZone ID. */
	public static final String TEST_TZ = "Pacific/Auckland";

	/** A date + time format. */
	public static final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	static {
		dateTimeFormat.setTimeZone(TimeZone.getTimeZone(TEST_TZ));
	}

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** The {@literal env.properties} file from the classpath. */
	protected final Properties envProperties = new Properties();

	@Before
	public void loadEnvironmentProperties() {
		Properties p = NodeTestUtils.loadEnvironmentProperties();
		envProperties.putAll(p);
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
	protected static TestEmbeddedDatabase createEmbeddedDatabase(String environmentTypeKey) {
		TestEmbeddedDatabaseFactoryBean factory = new TestEmbeddedDatabaseFactoryBean(
				environmentTypeKey);
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

}
