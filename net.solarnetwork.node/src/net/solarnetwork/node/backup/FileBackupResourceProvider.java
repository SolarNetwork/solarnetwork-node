/* ==================================================================
 * FileBackupResourceProvider.java - Mar 28, 2013 8:42:37 PM
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import net.solarnetwork.node.Constants;

/**
 * {@link BackupResourceProvider} for node files, such as installed application
 * bundle JARs.
 *
 * @author matt
 * @version 1.3
 */
public class FileBackupResourceProvider implements BackupResourceProvider {

	private String rootPath = System.getProperty(Constants.SYSTEM_PROP_NODE_HOME, "");
	private String[] resourceDirectories = new String[] { "app/main" };
	private String fileNamePattern = "\\.jar$";
	private boolean defaultShouldRestore = true;
	private MessageSource messageSource;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Default constructor.
	 */
	public FileBackupResourceProvider() {
		super();
	}

	@Override
	public String getKey() {
		return FileBackupResourceProvider.class.getName();
	}

	@Override
	public Iterable<BackupResource> getBackupResources() {
		if ( resourceDirectories == null || resourceDirectories.length < 1 ) {
			return Collections.emptyList();
		}
		final Pattern pat = (fileNamePattern == null ? null
				: Pattern.compile(fileNamePattern, Pattern.CASE_INSENSITIVE));
		List<BackupResource> fileList = new ArrayList<BackupResource>(20);
		for ( String path : resourceDirectories ) {
			File rootDir = (rootPath != null && rootPath.length() > 0 ? new File(rootPath, path)
					: new File(path));
			if ( !rootDir.isDirectory() ) {
				log.info("Skipping path {} because does not exist or is not a directory",
						rootDir.getAbsolutePath());
				continue;
			}
			File[] files = rootDir.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return pat.matcher(name).find();
				}
			});
			if ( files == null || files.length < 1 ) {
				continue;
			}
			for ( File f : files ) {
				// make sure backup path is relative
				final String backupPath = path + '/' + f.getName();
				String digest = null;
				InputStream in = null;
				try {
					in = new BufferedInputStream(new FileInputStream(f));
					digest = DigestUtils.sha256Hex(in);
				} catch ( IOException e ) {
					log.warn("Error calculating SHA-256 digest of {}: {}", f, e.getMessage());
				} finally {
					if ( in != null ) {
						try {
							in.close();
						} catch ( IOException e ) {
							// ignore
						}
					}
				}

				fileList.add(new ResourceBackupResource(new FileSystemResource(f), backupPath, getKey(),
						digest));
			}
		}
		return fileList;
	}

	@Override
	public boolean restoreBackupResource(BackupResource resource) {
		if ( resourceDirectories != null && resourceDirectories.length > 0 ) {
			for ( String path : resourceDirectories ) {
				File rootDir = new File(rootPath, path);
				File backupFile = new File(resource.getBackupPath());
				if ( backupFile.getAbsolutePath().startsWith(rootDir.getAbsolutePath()) ) {
					if ( !backupFile.getParentFile().isDirectory()
							&& !backupFile.getParentFile().mkdirs() ) {
						log.warn("Unable to create directory {} to restore file {} into",
								backupFile.getParent(), resource.getBackupPath());
					}
					try {
						// save to temp file first, then we'll rename
						File tempFile = new File(backupFile.getParentFile(), "." + backupFile.getName());
						log.debug("Installing resource {} => {}", resource.getBackupPath(), tempFile);
						FileCopyUtils.copy(resource.getInputStream(), new FileOutputStream(tempFile));
						tempFile.setLastModified(resource.getModificationDate());
						moveTemporaryResourceFile(tempFile, backupFile);
					} catch ( IOException e ) {
						log.error("Unable to restore backup resource {} to {}: {}",
								resource.getBackupPath(), rootDir.getPath(), e.getMessage());
					}
					return true;
				}
			}
		}
		return false;
	}

	private void moveTemporaryResourceFile(File tempFile, File outputFile) throws IOException {
		if ( outputFile.exists() ) {
			// if the file has not changed, just delete tmp file
			InputStream outputFileInputStream = null;
			InputStream tmpOutputFileInputStream = null;
			try {
				outputFileInputStream = new FileInputStream(outputFile);
				tmpOutputFileInputStream = new FileInputStream(tempFile);
				String outputFileHash = DigestUtils.sha256Hex(outputFileInputStream);
				String tmpOutputFileHash = DigestUtils.sha256Hex(tmpOutputFileInputStream);
				if ( tmpOutputFileHash.equals(outputFileHash) ) {
					// file unchanged, so just delete tmp file
					tempFile.delete();
				} else {
					log.debug("{} content updated", outputFile);
					outputFile.delete();
					tempFile.renameTo(outputFile);
				}
			} finally {
				if ( outputFileInputStream != null ) {
					try {
						outputFileInputStream.close();
					} catch ( IOException e ) {
						// ignore;
					}
				}
				if ( tmpOutputFileInputStream != null ) {
					try {
						tmpOutputFileInputStream.close();
					} catch ( IOException e ) {
						// ignore
					}
				}
			}
		} else {
			// rename temp file
			tempFile.renameTo(outputFile);
		}
	}

	@Override
	public BackupResourceProviderInfo providerInfo(Locale locale) {
		String name = "File Backup Provider";
		String desc = "Backs up system plugins.";
		MessageSource ms = messageSource;
		if ( ms != null ) {
			name = ms.getMessage("title", null, name, locale);
			desc = ms.getMessage("desc", null, desc, locale);
		}
		return new SimpleBackupResourceProviderInfo(getKey(), name, desc, defaultShouldRestore);
	}

	@Override
	public BackupResourceInfo resourceInfo(BackupResource resource, Locale locale) {
		return new SimpleBackupResourceInfo(resource.getProviderKey(), resource.getBackupPath(), null);
	}

	/**
	 * Set the {@code resourceDirectories} property as a comma-delimited string.
	 *
	 * @param list
	 *        a comma-delimited list of paths
	 */
	public void setResourceDirs(String list) {
		setResourceDirectories(StringUtils.commaDelimitedListToStringArray(list));
	}

	/**
	 * Get a comma-delimited string of the configured
	 * {@code resourceDirectories} property.
	 *
	 * @return a comma-delimited list of paths
	 */
	public String getResourceDirs() {
		return StringUtils.arrayToCommaDelimitedString(getResourceDirectories());
	}

	/**
	 * Get the root path.
	 *
	 * @return the root path
	 */
	public String getRootPath() {
		return rootPath;
	}

	/**
	 * Set the root path from which {@code resourceDirectories} are relative to.
	 *
	 * <p>
	 * If not provided, the system property
	 * {@link net.solarnetwork.node.Constants#SYSTEM_PROP_NODE_HOME} will be
	 * used, and if that isn't set then the runtime working directory will be
	 * used.
	 * </p>
	 *
	 * @param rootPath
	 *        the root path to use
	 */
	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	/**
	 * Get an array of directory paths, relative to {@code rootPath}, to look
	 * for files to include in the backup.
	 *
	 * @return the paths to use
	 */
	public String[] getResourceDirectories() {
		return resourceDirectories;
	}

	/**
	 * Set an array of directory paths, relative to {@code rootPath}, to look
	 * for files to include in the backup.
	 *
	 * @param bundlePaths
	 *        the paths to use; defaults to {@literal [app/main]}
	 */
	public void setResourceDirectories(String[] bundlePaths) {
		this.resourceDirectories = bundlePaths;
	}

	/**
	 * Get the file name patter.
	 *
	 * @return the pattern to use
	 */
	public String getFileNamePattern() {
		return fileNamePattern;
	}

	/**
	 * Set a regexp used to match against files found in the
	 * {@code resourceDirectories}.
	 *
	 * <p>
	 * Only files matching this expression are included in the backup.
	 * </p>
	 *
	 * @param fileNamePattern
	 *        the pattern to use; defaults to {@literal \.jar$}
	 */
	public void setFileNamePattern(String fileNamePattern) {
		this.fileNamePattern = fileNamePattern;
	}

	/**
	 * Set the {@link MessageSource} to resolve localized messages with.
	 *
	 * @param messageSource
	 *        The message source.
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the configured {@link MessageSource}.
	 *
	 * @return the message source
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Get the "should restore" default setting.
	 *
	 * <p>
	 * This flag indicates to the backup interface if the resources provided by
	 * this service should be enabled for restore by default.
	 * </p>
	 *
	 * @return {@literal true} if the resources provided by this service should
	 *         be selected for restore by default; defaults to {@literal true}
	 * @since 1.3
	 */
	public boolean isDefaultShouldRestore() {
		return defaultShouldRestore;
	}

	/**
	 * Get the "should restore" default setting.
	 *
	 * <p>
	 * This flag indicates to the backup interface if the resources provided by
	 * this service should be enabled for restore by default.
	 * </p>
	 *
	 * @param defaultShouldRestore
	 *        {@literal true} if the resources provided by this service should
	 *        be selected for restore by default
	 * @since 1.3
	 */
	public void setDefaultShouldRestore(boolean defaultShouldRestore) {
		this.defaultShouldRestore = defaultShouldRestore;
	}

}
