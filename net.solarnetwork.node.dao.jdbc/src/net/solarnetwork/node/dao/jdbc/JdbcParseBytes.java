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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

/**
 * Parse hex-encoded string values into a byte array.
 * 
 * @author matt
 * @version 1.0
 * @since 2.3
 */
public class JdbcParseBytes extends CellProcessorAdaptor implements StringCellProcessor {

	/**
	 * Default constructor.
	 */
	public JdbcParseBytes() {
		super();
	}

	/**
	 * Construct with a chained processor.
	 * 
	 * @param next
	 *        the next processor
	 */
	public JdbcParseBytes(final StringCellProcessor next) {
		super(next);
	}

	@Override
	public <T> T execute(final Object value, final CsvContext context) {
		validateInputNotNull(value, context);

		byte[] result;
		try {
			result = Hex.decodeHex(value.toString());
		} catch ( DecoderException e ) {
			throw new SuperCsvCellProcessorException("Error decoding hex value.", context, this, e);
		}

		return next.execute(result, context);
	}

}
