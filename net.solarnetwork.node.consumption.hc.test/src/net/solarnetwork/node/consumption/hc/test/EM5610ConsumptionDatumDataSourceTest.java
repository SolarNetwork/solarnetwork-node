/* ==================================================================
 * EM5610ConsumptionDatumDataSourceTest.java - Jul 11, 2011 3:41:35 PM
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

package net.solarnetwork.node.consumption.hc.test;

import static org.junit.Assert.assertNotNull;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.consumption.hc.EM5610ConsumptionDatumDataSource;
import net.solarnetwork.node.support.SerialPortBeanParameters;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link EM5610ConsumptionDatumDataSource} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class EM5610ConsumptionDatumDataSourceTest extends BaseTestSupport {

	@Autowired
	private SerialPortBeanParameters serialParameters;
	
	@Test
	public void readDatum() throws Exception {
		assertNotNull(serialParameters);
		EM5610ConsumptionDatumDataSource ds = new EM5610ConsumptionDatumDataSource();
		ds.setSerialParams(serialParameters);
		ConsumptionDatum datum = ds.readCurrentDatum();
		assertNotNull(datum);
		// TODO: verify result data, as least in terms of not-null fields
	}

}
