/* ==================================================================
 * JdbcFmtDate.java - 7/10/2016 7:06:46 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc;

import java.time.format.DateTimeFormatter;
import org.apache.commons.codec.binary.Hex;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

/**
 * Format dates using a Joda {@link DateTimeFormatter}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.3
 */
public class JdbcFmtBytes extends CellProcessorAdaptor implements CellProcessor {

	public JdbcFmtBytes() {
		super();
	}

	public JdbcFmtBytes(StringCellProcessor next) {
		super(next);
	}

	@Override
	public <T> T execute(final Object value, final CsvContext context) {
		validateInputNotNull(value, context);

		String result;

		if ( value instanceof byte[] ) {
			result = Hex.encodeHexString((byte[]) value);
		} else {
			throw new SuperCsvCellProcessorException(byte[].class, value, context, this);
		}

		return next.execute(result, context);
	}

}
