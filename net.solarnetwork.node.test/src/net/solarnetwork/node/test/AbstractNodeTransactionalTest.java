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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base test class for transactional unit tests.
 * 
 * @author matt
 * @version 1.1
 */
@ContextConfiguration(locations = { "classpath:/net/solarnetwork/node/test/test-context.xml" })
@Transactional(transactionManager = "txManager")
@Rollback
public abstract class AbstractNodeTransactionalTest
		extends AbstractTransactionalJUnit4SpringContextTests {

	/** A test Node ID. */
	protected static final Long TEST_NODE_ID = -5555L;

	/** A test Weather Source ID. */
	protected static final Long TEST_WEATHER_SOURCE_ID = -5554L;

	/** A test Location ID. */
	protected static final Long TEST_LOC_ID = -5553L;

	/** A test TimeZone ID. */
	protected static final String TEST_TZ = "Pacific/Auckland";

	/** A date + time format. */
	protected final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Setup the {@link #dateTimeFormat} timezone.
	 */
	@BeforeTransaction
	public void setupDateTime() {
		dateTimeFormat.setTimeZone(TimeZone.getTimeZone(TEST_TZ));
	}

}
