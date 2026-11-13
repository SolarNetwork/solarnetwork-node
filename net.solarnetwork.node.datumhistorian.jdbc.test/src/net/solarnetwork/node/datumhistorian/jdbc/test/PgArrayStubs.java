/* ==================================================================
 * PgArrayStubs.java - 13 Nov 2026 1:40:17 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datumhistorian.jdbc.test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicInteger;
import org.mockito.quality.Strictness;

/**
 * Stubs for mock JDBC arrays, to behave as the Postgres JDBC driver's arrays
 * do.
 *
 * @author matt
 * @version 1.0
 */
public final class PgArrayStubs {

	private PgArrayStubs() {
		// not available
	}

	/**
	 * Stub a mock array to behave as the Postgres JDBC driver's
	 * {@code NUMERIC[]} arrays do.
	 *
	 * <p>
	 * When every value is a number or {@code null}, {@link Array#getArray()}
	 * returns the values as a {@code BigDecimal[]}. Otherwise, as the driver
	 * does for an array with {@code NaN} or infinite values,
	 * {@link Array#getArray()} throws an {@link SQLException}, and
	 * {@link Array#getResultSet()} returns a result set of the index and text
	 * of each value, whose {@link ResultSet#getBigDecimal(int)} throws an
	 * {@link SQLException} for a value that is not a number.
	 * </p>
	 *
	 * @param array
	 *        the mock array to stub
	 * @param values
	 *        the text of each value, as Postgres formats a {@code NUMERIC}, or
	 *        {@code null} for a {@code NULL} value
	 * @throws SQLException
	 *         never
	 */
	public static void givenNumericArray(Array array, String... values) throws SQLException {
		final BigDecimal[] numbers = new BigDecimal[values.length];
		for ( int i = 0; i < values.length; i++ ) {
			try {
				numbers[i] = number(values[i]);
			} catch ( SQLException e ) {
				final ResultSet arrayRs = numericArrayResultSet(values);
				given(array.getArray()).willThrow(e);
				given(array.getBaseType()).willReturn(Types.NUMERIC);
				given(array.getResultSet()).willReturn(arrayRs);
				return;
			}
		}
		given(array.getArray()).willReturn(numbers);
	}

	private static BigDecimal number(String value) throws SQLException {
		if ( value == null ) {
			return null;
		}
		try {
			return new BigDecimal(value);
		} catch ( NumberFormatException e ) {
			// as the driver reports it
			throw new SQLException("Bad value for type BigDecimal : " + value);
		}
	}

	private static ResultSet numericArrayResultSet(String... values) throws SQLException {
		// lenient, as which methods are used to read a value that is not a number can vary
		final ResultSet rs = mock(ResultSet.class, withSettings().strictness(Strictness.LENIENT));
		final AtomicInteger row = new AtomicInteger();
		given(rs.next()).willAnswer(inv -> row.incrementAndGet() <= values.length);
		given(rs.getInt(1)).willAnswer(inv -> row.get());
		given(rs.getBigDecimal(2)).willAnswer(inv -> number(values[row.get() - 1]));
		given(rs.getString(2)).willAnswer(inv -> values[row.get() - 1]);
		// the driver fails to read a value of a one-dimensional array as an array
		given(rs.getArray(2)).willThrow(new SQLException("No results were returned by the query."));
		return rs;
	}

}
