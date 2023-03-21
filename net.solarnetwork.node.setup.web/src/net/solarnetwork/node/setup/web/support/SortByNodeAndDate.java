/* ==================================================================
 * SortByNodeAndDate.java - 23/07/2019 11:04:49 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.support;

import java.util.Comparator;
import net.solarnetwork.node.backup.Backup;

/**
 * Comparator to sort backups by node ID, then date.
 * 
 * @author matt
 * @version 1.0
 * @since 1.39.2
 */
public class SortByNodeAndDate implements Comparator<Backup> {

	/** A default instance of this comparator. */
	public static final SortByNodeAndDate DEFAULT = new SortByNodeAndDate();

	/**
	 * Default constructor.
	 */
	public SortByNodeAndDate() {
		super();
	}

	/**
	 * Compare two backups.
	 * 
	 * <p>
	 * The backup node IDs are ordered in ascending order, followed by their
	 * dates in reverse chronological order.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public int compare(Backup o1, Backup o2) {
		// sort first by node ID
		Long n1 = o1.getNodeId();
		Long n2 = o2.getNodeId();

		int r = n1.compareTo(n2);
		if ( r != 0 ) {
			return r;
		}

		// sort in reverse chronological order (newest to oldest)
		return o2.getDate().compareTo(o1.getDate());
	}

}
