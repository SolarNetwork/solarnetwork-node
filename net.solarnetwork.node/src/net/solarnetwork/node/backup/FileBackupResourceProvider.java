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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
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
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>rootPath</dt>
 * <dd>The root path from which {@code resourceDirectories} are relative to. If
 * not provided, the runtime working directory will be used.</dd>
 * <dt>resourceDirectories</dt>
 * <dd>An array of directory paths, relative to {@code rootPath}, to look for
 * files to include in the backup. Defaults to
 * {@code [app/base, app/main]}.</dd>
 * 
 * <dt>fileNamePattern</dt>
 * <dd>A regexp used to match against files found in the
 * {@code resourceDirectories}. Only files matching this expression are included
 * in the backup. Defaults to {@code \.jar$}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 */
public class FileBackupResourceProvider implements BackupResourceProvider {

	private String rootPath = System.getProperty(Constants.SYSTEM_PROP_NODE_HOME, "");
	private String[] resourceDirectories = new String[] { "app/base", "app/main" };
	private String fileNamePattern = "\\.jar$";
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

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
				fileList.add(
						new ResourceBackupResource(new FileSystemResource(f), backupPath, getKey()));
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
						FileCopyUtils.copy(resource.getInputStream(), new FileOutputStream(backupFile));
						backupFile.setLastModified(resource.getModificationDate());
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

	@Override
	public BackupResourceProviderInfo providerInfo(Locale locale) {
		String name = "File Backup Provider";
		String desc = "Backs up system plugins.";
		MessageSource ms = messageSource;
		if ( ms != null ) {
			name = ms.getMessage("title", null, name, locale);
			desc = ms.getMessage("desc", null, desc, locale);
		}
		return new SimpleBackupResourceProviderInfo(getKey(), name, desc);
	}

	@Override
	public BackupResourceInfo resourceInfo(BackupResource resource, Locale locale) {
		return new SimpleBackupResourceInfo(resource.getProviderKey(), resource.getBackupPath(), null);
	}

	/**
	 * Set the {@code resourceDirectories} property as a comma-delimieted
	 * string.
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

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public void setResourceDirectories(String[] bundlePaths) {
		this.resourceDirectories = bundlePaths;
	}

	public void setFileNamePattern(String fileNamePattern) {
		this.fileNamePattern = fileNamePattern;
	}

	public String getRootPath() {
		return rootPath;
	}

	public String[] getResourceDirectories() {
		return resourceDirectories;
	}

	public String getFileNamePattern() {
		return fileNamePattern;
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

}
