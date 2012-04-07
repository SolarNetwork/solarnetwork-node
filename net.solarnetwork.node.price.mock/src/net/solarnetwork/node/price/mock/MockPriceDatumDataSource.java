/* ===================================================================
 * MockPriceDatumDataSource.java
 * 
 * Created Dec 3, 2009 3:49:12 PM
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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.price.mock;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.price.PriceDatum;

/**
 * Mock implementation of {@link DatumDataSource} for {@link PriceDatum}
 * objects.
 * 
 * <p>This simple implementation returns an object with random data.</p>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class MockPriceDatumDataSource implements DatumDataSource<PriceDatum> {

	@Override
	public Class<? extends PriceDatum> getDatumType() {
		return PriceDatum.class;
	}

	@Override
	public PriceDatum readCurrentDatum() {
		return new PriceDatum("mock", Math.random() * 10, -1L);
	}

}
