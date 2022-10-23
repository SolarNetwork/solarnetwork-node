/* ==================================================================
 * CollectionBackupResourceIterable.java - Mar 27, 2013 4:55:33 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.backup;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Simple implementation of {@link BackupResourceIterable} that uses a
 * collection.
 * 
 * @author matt
 * @version 1.0
 */
public class CollectionBackupResourceIterable implements BackupResourceIterable {

	private final Collection<BackupResource> collection;

	/**
	 * Constructor.
	 * 
	 * @param collection
	 *        the collection to wrrap
	 */
	public CollectionBackupResourceIterable(Collection<BackupResource> collection) {
		super();
		this.collection = collection;
	}

	@Override
	public Iterator<BackupResource> iterator() {
		return collection.iterator();
	}

	@Override
	public void close() throws IOException {
		// nothing to do here
	}

}
