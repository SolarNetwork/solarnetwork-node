/* ==================================================================
 * JdbcEnergyCapacityDatumDaoTest.java - Oct 10, 2011 3:11:28 PM
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

package net.solarnetwork.node.dao.jdbc.energy.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.solarnetwork.node.dao.jdbc.energy.JdbcEnergyCapacityDatumDao;
import net.solarnetwork.node.energy.EnergyCapacityDatum;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test case for the {@link JdbcEnergyCapacityDatumDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
@ContextConfiguration
public class JdbcEnergyCapacityDatumDaoTest extends AbstractNodeTransactionalTest {

	private static final String TEST_SOURCE_ID = "Test Source";
	private static final Float TEST_VOLTS = 2.2F;
	private static final Double TEST_AMP_HOURS = 2.1D;
	private static final Double TEST_WATT_HOURS = 2.3D;
	
	private static final String SQL_GET_BY_ID = 
		"SELECT id, created, source_id, voltage, amp_hour, watt_hour "
		+"FROM solarnode.sn_energy_capacity_datum WHERE id = ?";

	@Autowired private JdbcEnergyCapacityDatumDao dao;
	@Autowired private JdbcOperations jdbcOps;

	@Test
	public void storeNew() {
		EnergyCapacityDatum datum = new EnergyCapacityDatum();
		datum.setAmpHours(TEST_AMP_HOURS);
		datum.setSourceId(TEST_SOURCE_ID);
		datum.setVolts(TEST_VOLTS);
		datum.setWattHours(TEST_WATT_HOURS);
		
		final Long id = dao.storeDatum(datum);
		assertNotNull(id);
		
		jdbcOps.query(SQL_GET_BY_ID, new Object[] {id}, new ResultSetExtractor<Object>() {
			@Override
			public Object extractData(ResultSet rs) throws SQLException,
					DataAccessException {
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
				
				Float f = rs.getFloat(col++);
				assertFalse(rs.wasNull());
				assertEquals(TEST_VOLTS.floatValue(), f.floatValue(), 0.001);
				
				Double d = rs.getDouble(col++);
				assertFalse(rs.wasNull());
				assertEquals(TEST_AMP_HOURS.doubleValue(), d.doubleValue(), 0.001);
				
				d = rs.getDouble(col++);
				assertFalse(rs.wasNull());
				assertEquals(TEST_WATT_HOURS.doubleValue(), d.doubleValue(), 0.001);
				
				assertFalse("Must not have more than one result", rs.next());
				return null;
			}
			
		});
	}
	
}
