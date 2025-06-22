/* ==================================================================
 * ZipStreamBackupResource.java - 2/11/2016 11:02:04 AM
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import org.apache.commons.io.input.TeeInputStream;
import net.solarnetwork.node.Constants;

/**
 * A zip input stream backup resource.
 *
 * @author matt
 * @version 1.3
 * @since 1.46
 */
public class ZipStreamBackupResource implements BackupResource {

	private final InputStream stream;
	private final ZipEntry entry;
	private final String providerKey;
	private final String path;

	private File tempFile;

	/**
	 * Construct with values.
	 *
	 * @param stream
	 *        the zip archive stream
	 * @param entry
	 *        the entry previously obtained from the zip archive
	 * @param providerKey
	 *        the provider key
	 * @param path
	 *        the path to use
	 */
	public ZipStreamBackupResource(InputStream stream, ZipEntry entry, String providerKey, String path) {
		super();
		this.stream = stream;
		this.entry = entry;
		this.providerKey = providerKey;
		this.path = path;
	}

	private static final class TempFileCleaner implements Runnable {

		private final File file;

		private TempFileCleaner(File file) {
			super();
			this.file = file;
		}

		@Override
		public void run() {
			file.delete();
		}

	}

	@Override
	public String getProviderKey() {
		return providerKey;
	}

	@Override
	public String getBackupPath() {
		return path;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		// to support calling getInputStream() more than once, tee the input to a temp file
		// the first time, and subsequent times
		if ( tempFile != null ) {
			return new BufferedInputStream(new FileInputStream(tempFile));
		}
		tempFile = File.createTempFile(entry.getName(), ".tmp");
		Constants.cleaner().register(this, new TempFileCleaner(tempFile));
		final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));
		return new TeeInputStream(new FilterInputStream(stream) {

			@Override
			public void close() throws IOException {
				out.flush();
				out.close();
			}
		}, out, false);
	}

	@Override
	public long getModificationDate() {
		return entry.getTime();
	}

	@Override
	public String getSha256Digest() {
		return null;
	}

}
