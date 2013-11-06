/* ==================================================================
 * JdbcPowerDatumDaoTest.java - Dec 21, 2011 8:50:28 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.dao.jdbc.power.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.solarnetwork.node.dao.jdbc.power.JdbcPowerDatumDao;
import net.solarnetwork.node.power.PowerDatum;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test case for the {@link JdbcPowerDatumDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
@ContextConfiguration
public class JdbcPowerDatumDaoTest extends AbstractNodeTransactionalTest {

	private static final String TEST_SOURCE_ID = "Test Source";
	private static final Integer TEST_WATTS = 22;
	private static final Long TEST_WATT_HOURS = 2300L;

	private static final String SQL_GET_BY_ID = "SELECT id, created, source_id, watts, watt_hours "
			+ "FROM solarnode.sn_power_datum WHERE id = ?";

	@Autowired
	private JdbcPowerDatumDao dao;
	@Autowired
	private JdbcOperations jdbcOps;

	@Test
	public void storeNew() {
		PowerDatum datum = new PowerDatum();
		datum.setWatts(TEST_WATTS);
		datum.setSourceId(TEST_SOURCE_ID);
		datum.setWattHourReading(TEST_WATT_HOURS);

		final Long id = dao.storeDatum(datum);
		assertNotNull(id);

		jdbcOps.query(SQL_GET_BY_ID, new Object[] { id }, new ResultSetExtractor<Object>() {

			@Override
			public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
				assertTrue("Must have one result", rs.next());

				int col = 1;

				Long l = rs.getLong(col++);
				assertFalse(rs.wasNull());
				assertEquals(id, l);

				rs.getTimestamp(col++);
				assertFalse(rs.wasNull());

				String s = rs.getString(col++);
				assertFalse(rs.wasNull());
				assertEquals(TEST_SOURCE_ID, s);

				int w = rs.getInt(col++);
				assertFalse(rs.wasNull());
				assertEquals(TEST_WATTS.intValue(), w);

				long wh = rs.getLong(col++);
				assertFalse(rs.wasNull());
				assertEquals(TEST_WATT_HOURS.longValue(), wh);

				assertFalse("Must not have more than one result", rs.next());
				return null;
			}

		});
	}

}
