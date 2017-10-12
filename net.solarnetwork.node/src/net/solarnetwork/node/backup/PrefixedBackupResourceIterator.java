/* ==================================================================
 * PrefixedBackupResourceIterator.java - 2/11/2016 11:02:04 AM
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

package net.solarnetwork.node.backup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * An iterator of {@link BackupResource} objects that adds a prefix to each
 * resource path.
 * 
 * @author matt
 * @version 1.1
 * @since 1.46
 */
public class PrefixedBackupResourceIterator implements Iterator<BackupResource> {

	private final Iterator<BackupResource> delegate;
	private final String prefix;

	/**
	 * Construct with a delegate iterator.
	 * 
	 * @param delegate
	 *        The delegate iterator.
	 * @param prefix
	 *        The prefix to add to each {@link BackupResource#getBackupPath()}.
	 */
	public PrefixedBackupResourceIterator(Iterator<BackupResource> delegate, String prefix) {
		super();
		this.delegate = delegate;
		this.prefix = prefix;
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public BackupResource next() {
		final BackupResource r = delegate.next();
		return new BackupResource() {

			@Override
			public String getBackupPath() {
				return prefix + '/' + r.getBackupPath();
			}

			@Override
			public String getProviderKey() {
				return prefix;
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return r.getInputStream();
			}

			@Override
			public long getModificationDate() {
				return r.getModificationDate();
			}

			@Override
			public String getSha256Digest() {
				return r.getSha256Digest();
			}

		};
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
