/* ==================================================================
 * CsvDatumDataSourceUtils.java - 1/04/2023 8:32:28 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.solarnetwork.codec.CsvUtils;
import net.solarnetwork.util.IntRangeSet;
import net.solarnetwork.util.StringUtils;

/**
 * Helper utilities for the CSV datum data source.
 * 
 * @author matt
 * @version 1.0
 */
public final class CsvDatumDataSourceUtils {

	private CsvDatumDataSourceUtils() {
		// not available
	}

	/**
	 * Compute an array of 0-based column indexes from a comma-delimited list of
	 * 1-based column reference ranges.
	 * 
	 * @param columnRefs
	 *        the list of 1-based column references
	 * @return the array of 0-based column indexes, or {@literal null} if the
	 *         reference cannot be parsed
	 */
	public static int[] columnIndexes(String columnRefs) {
		int[] result = null;
		if ( columnRefs != null && !columnRefs.trim().isEmpty() ) {
			try {
				List<IntRangeSet> dateCols = StringUtils.commaDelimitedStringToSet(columnRefs).stream()
						.map(CsvUtils::parseColumnsReference).collect(Collectors.toList());
				if ( dateCols != null ) {
					List<Integer> indexes = new ArrayList<>(1);
					for ( IntRangeSet r : dateCols ) {
						r.forEachOrdered(col -> {
							indexes.add(col - 1);
						});
					}
					final int len = indexes.size();
					if ( len > 0 ) {
						result = new int[indexes.size()];
						for ( int i = 0; i < len; i++ ) {
							result[i] = indexes.get(i);
						}
					}
				}
			} catch ( IllegalArgumentException e ) {
				result = null;
			}
		}
		return result;
	}

}
