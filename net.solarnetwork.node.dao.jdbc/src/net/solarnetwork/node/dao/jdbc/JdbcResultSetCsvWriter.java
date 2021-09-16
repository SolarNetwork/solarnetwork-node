/* ==================================================================
 * JdbcResultSetCsvWriter.java - 6/10/2016 8:41:58 AM
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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.ICsvWriter;

/**
 * Write JDBC {@link ResultSet} instances as CSV data.
 * 
 * CSV column headers are generated from column names provided by the
 * {@code ResultSet} itself, and their order matches the order of columns in the
 * {@code ResultSet}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.17
 */
public interface JdbcResultSetCsvWriter extends ICsvWriter {

	/**
	 * Export a {@link ResultSet} as CSV data.
	 * 
	 * @param resultSet
	 *        The {@code ResultSet} to write.
	 * @throws SQLException
	 *         If any SQL error occurs.
	 * @throws IOException
	 *         If any IO error occurs.
	 */
	void write(ResultSet resultSet) throws SQLException, IOException;

	/**
	 * Export a {@link ResultSet} as CSV data, using cell processors.
	 * 
	 * @param resultSet
	 *        The {@code ResultSet} to write.
	 * @param cellProcessors
	 *        An array of cell processors to handle each exported column. The
	 *        length of the array should match the number and order of columns
	 *        in the {@code ResultSet}. {@literal null} values are permitted and
	 *        indicate no processing should be performed on that column.
	 * @throws SQLException
	 *         If any SQL error occurs.
	 * @throws IOException
	 *         If any IO error occurs.
	 */
	void write(ResultSet resultSet, CellProcessor[] cellProcessors) throws SQLException, IOException;

}
