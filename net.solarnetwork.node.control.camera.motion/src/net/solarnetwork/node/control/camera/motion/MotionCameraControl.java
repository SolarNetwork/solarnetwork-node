/* ==================================================================
 * MotionCameraControl.java - 18/10/2019 3:49:15 pm
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

package net.solarnetwork.node.control.camera.motion;

import static net.solarnetwork.node.setup.SetupResource.WEB_CONSUMER_TYPES;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.io.ResultStatusException;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicSetupResourceSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.setup.ResourceSetupResource;
import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.UrlUtils;

/**
 * Integrate with the <a href="https://motion-project.github.io/">motion</a>
 * image change detection program.
 * 
 * @author matt
 * @version 1.0
 */
public class MotionCameraControl extends BaseIdentifiable implements SettingSpecifierProvider,
		NodeControlProvider, InstructionHandler, SetupResourceProvider {

	/** The default value of the {@code path} property. */
	public static final String DEFAULT_PATH = "/var/lib/motion";

	/** The default value of the {@code latestSnapshotFilename} property. */
	public static final Pattern DEFAULT_PATH_SNAPSHOT_FILTER = Pattern.compile(".+-snapshot\\.jpg");

	/** The default value for the {@code pathFilter} property. */
	public static final Pattern DEFAULT_PATH_FILTER = Pattern.compile(".+\\.jpg");

	/** The default value for the {@code resourceCacheSecs} property. */
	public static final int DEFAULT_RESOURCE_CACHE_SECS = 15;

	/** The default value for the {@code motionBaseUrl} property. */
	public static final String DEFAULT_MOTION_BASE_URL = "http://localhost:8180";

	/** The instruction signal name for initiating a snapshot. */
	public static final String SIGNAL_SNAPSHOT = "snapshot";

	/** An instruction parameter for the motion camera ID to operate on. */
	public static final String CAMERA_ID_PARAM = "cameraId";

	/** The default value for the {@code connectionTimeout} property. */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 15000;

	private static final String MEDIA_RESOURCE_LAST_MODIFIED = "media-resource-last-modified";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String controlId;
	private String path = DEFAULT_PATH;
	private Pattern pathFilter = DEFAULT_PATH_FILTER;
	private Pattern pathSnapshotFilter = DEFAULT_PATH_SNAPSHOT_FILTER;
	private SetupResourceProvider mediaResourceProvider;
	private int resourceCacheSecs = DEFAULT_RESOURCE_CACHE_SECS;
	private String motionBaseUrl = DEFAULT_MOTION_BASE_URL;
	private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

	// NodeControlProvider

	@Override
	public List<String> getAvailableControlIds() {
		final String controlId = getControlId();
		return (controlId == null || controlId.isEmpty() ? Collections.emptyList()
				: Collections.singletonList(controlId));
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		// TODO Auto-generated method stub
		return null;
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SIGNAL.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		final String topic = (instruction != null ? instruction.getTopic() : null);
		if ( !InstructionHandler.TOPIC_SIGNAL.equals(topic) ) {
			return null;
		}
		final String controlId = getControlId();
		if ( controlId == null || controlId.isEmpty() ) {
			return null;
		}
		final String signal = instruction.getParameterValue(controlId);
		if ( signal == null ) {
			return null;
		}
		int cameraId = 1;
		if ( instruction.isParameterAvailable(CAMERA_ID_PARAM) ) {
			try {
				cameraId = Integer.parseInt(instruction.getParameterValue(CAMERA_ID_PARAM));
			} catch ( NumberFormatException e ) {
				log.error("Instruction {} cameraId parameter invalid: {}",
						instruction.getRemoteInstructionId(),
						instruction.getParameterValue(CAMERA_ID_PARAM));
				return InstructionState.Declined;
			}
		}
		if ( cameraId > 0 ) {
			try {
				if ( SIGNAL_SNAPSHOT.equalsIgnoreCase(signal) ) {
					if ( takeSnapshot(cameraId) ) {
						return InstructionState.Completed;
					}
				}
			} catch ( IOException e ) {
				log.error("Communication error with motion camera {} at {}: {}", cameraId, motionBaseUrl,
						e.toString());
			} catch ( ResultStatusException e ) {
				log.error("Error response code received from motion camera {} request {}: {}", cameraId,
						e.getUrl(), e.getStatusCode());
			}
		} else {
			log.error("Instruction {} cameraId parameter invalid: {}",
					instruction.getRemoteInstructionId(), cameraId);
		}
		return InstructionState.Declined;
	}

	private boolean takeSnapshot(final int cameraId) throws IOException {
		final String baseUrl = getMotionBaseUrl();
		if ( baseUrl == null ) {
			log.error("No motion URL configured to take camera {} snapshot with.", cameraId);
			return false;
		}
		String result = UrlUtils.getURLForString(
				MotionWebApi.ActionSnapshot.absoluteUrl(baseUrl, cameraId, null), UrlUtils.ACCEPT_TEXT,
				null, connectionTimeout, null);
		log.info("Snapshot successful for motion camera {} at {}: {}", cameraId, baseUrl, result);
		return true;
	}

	// SetupResourceProvider

	private String snapshotResourceId() {
		String controlId = getControlId();
		if ( controlId == null ) {
			return null;
		}
		byte[] name = (controlId + "-snapshot").getBytes();
		return DigestUtils.md5DigestAsHex(name) + ".jpg";
	}

	private String latestResourceId() {
		String controlId = getControlId();
		if ( controlId == null ) {
			return null;
		}
		byte[] name = (controlId + "-latest").getBytes();
		return DigestUtils.md5DigestAsHex(name) + ".jpg";
	}

	private Resource snapshotResource() {
		String dir = getPath();
		if ( dir == null || dir.isEmpty() ) {
			return null;
		}
		Path snapPath = null;
		// search based on modification date
		try {
			snapPath = Files.list(Paths.get(dir)).filter(p -> {
				BasicFileAttributes attrs;
				try {
					attrs = Files.readAttributes(p, BasicFileAttributes.class,
							LinkOption.NOFOLLOW_LINKS);
					return attrs.isRegularFile() && !attrs.isSymbolicLink()
							&& matchesSnapshotResource(p);
				} catch ( IOException e ) {
					return false;
				}
			}).max(Comparator.comparingLong(p -> {
				try {
					return Files.getLastModifiedTime(p).toMillis();
				} catch ( IOException e ) {
					return 0;
				}
			})).orElse(null);
		} catch ( InvalidPathException e ) {
			log.error(
					"Cannot determine latest snapshot resource file because of invalid path value [{}]: {}",
					dir, e.getMessage());
		} catch ( IOException e ) {
			log.warn("Error searching for latest snapshot image file: {}", e.toString());
		}
		if ( snapPath != null && Files.isReadable(snapPath) ) {
			return new FileSystemResource(snapPath.toFile());
		}
		return null;
	}

	private boolean matchesNonSnapshotResource(Path p) {
		Pattern pat = getPathFilter();
		String name = p.getFileName().toString();
		boolean match = (pat != null ? pat.matcher(name).matches() : false);
		if ( match ) {
			// verify not a "snapshot" name
			Pattern snapPat = getPathSnapshotFilter();
			match = (snapPat != null ? !snapPat.matcher(name).matches() : true);
		}
		return match;
	}

	private boolean matchesSnapshotResource(Path p) {
		Pattern pat = getPathSnapshotFilter();
		String name = p.getFileName().toString();
		return (pat != null ? pat.matcher(name).matches() : false);
	}

	private Resource latestNonSnapshotResource() {
		String dir = getPath();
		if ( dir == null || dir.isEmpty() ) {
			return null;
		}
		Path snapPath = null;
		// search based on modification date
		try {
			snapPath = Files.list(Paths.get(dir)).filter(p -> {
				BasicFileAttributes attrs;
				try {
					attrs = Files.readAttributes(p, BasicFileAttributes.class,
							LinkOption.NOFOLLOW_LINKS);
					return attrs.isRegularFile() && !attrs.isSymbolicLink()
							&& matchesNonSnapshotResource(p);
				} catch ( IOException e ) {
					return false;
				}
			}).max(Comparator.comparingLong(p -> {
				try {
					return Files.getLastModifiedTime(p).toMillis();
				} catch ( IOException e ) {
					return 0;
				}
			})).orElse(null);
		} catch ( InvalidPathException e ) {
			log.error("Cannot determine latest resource file because of invalid path value [{}]: {}",
					dir, e.getMessage());
		} catch ( IOException e ) {
			log.warn("Error searching for latest snapshot image file: {}", e.toString());
		}
		if ( snapPath != null && Files.isReadable(snapPath) ) {
			return new FileSystemResource(snapPath.toFile());
		}
		return null;
	}

	@Override
	public SetupResource getSetupResource(String resourceUID, Locale locale) {
		Resource resource = null;
		final String latestResourceId = latestResourceId();
		if ( latestResourceId != null && latestResourceId.equals(resourceUID) ) {
			resource = latestNonSnapshotResource();
		} else {
			final String snapResourceId = snapshotResourceId();
			if ( snapResourceId != null && snapResourceId.equals(resourceUID) ) {
				resource = snapshotResource();
			}
		}
		if ( resource != null ) {
			String filename = resource.getFilename();
			MediaResourceMimeType type = MediaResourceMimeType.forFilename(filename);
			String mimeType = (type != null ? type.getMimeType() : "application/octet-stream");
			try {
				return new ResourceSetupResource(resource, resourceUID, mimeType, resourceCacheSecs,
						WEB_CONSUMER_TYPES, null);
			} catch ( IOException e ) {
				log.warn("Error getting latest snapshot image file: {}", e.toString());
			}
		}
		return null;
	}

	@Override
	public Collection<SetupResource> getSetupResourcesForConsumer(String consumerType, Locale locale) {
		return Collections.emptyList();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.camera.motion";
	}

	@Override
	public String getDisplayName() {
		return "Motion Camera Control";
	}

	private Map<String, Object> mediaResourceProperties(Resource resource, String resourceId) {
		if ( resource == null ) {
			return Collections.emptyMap();
		}
		Map<String, Object> m = new HashMap<>(4);
		m.put("media-resource-id", resourceId);

		try {
			long modified = resource.lastModified();
			if ( modified > 0 ) {
				m.put(MEDIA_RESOURCE_LAST_MODIFIED, modified);
			}
		} catch ( IOException e ) {
			log.warn("Last modified date not available for resource {}: {}", resource, e.getMessage());
		}

		return m;
	}

	private String mediaResourceFormattedDateProperty(Map<String, Object> props, String propName) {
		Object p = props.get(propName);
		ZonedDateTime d = null;
		if ( p instanceof Long ) {
			d = Instant.ofEpochMilli((Long) p).atZone(ZoneId.systemDefault());
		}
		if ( d != null ) {
			DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM,
					FormatStyle.MEDIUM);
			return d.format(fmt);
		}
		return "N/A";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(4);

		SetupResourceProvider snapProvider = getMediaResourceProvider();
		if ( snapProvider != null ) {
			Resource latestResource = latestNonSnapshotResource();
			if ( latestResource != null ) {
				String latestResourceId = latestResourceId();
				if ( latestResourceId != null ) {
					Map<String, Object> props = mediaResourceProperties(latestResource,
							latestResourceId);
					results.add(
							new BasicTitleSettingSpecifier("latestImage",
									getMessageSource().getMessage("latestImage.info",
											new Object[] { mediaResourceFormattedDateProperty(props,
													MEDIA_RESOURCE_LAST_MODIFIED) },
											Locale.getDefault())));
					results.add(new BasicSetupResourceSettingSpecifier(mediaResourceProvider, props));
				}
			}

			String snapResourceId = snapshotResourceId();
			Resource snapResource = snapshotResource();
			if ( snapResource != null ) {
				if ( snapResourceId != null ) {
					Map<String, Object> props = mediaResourceProperties(snapResource, snapResourceId);
					results.add(
							new BasicTitleSettingSpecifier("snapImage",
									getMessageSource().getMessage("snapImage.info",
											new Object[] { mediaResourceFormattedDateProperty(props,
													MEDIA_RESOURCE_LAST_MODIFIED) },
											Locale.getDefault())));
					results.add(new BasicSetupResourceSettingSpecifier(mediaResourceProvider, props));
				}
			}
		}

		results.add(new BasicTextFieldSettingSpecifier("controlId", ""));
		results.add(new BasicTextFieldSettingSpecifier("motionBaseUrl", DEFAULT_MOTION_BASE_URL));
		results.add(new BasicTextFieldSettingSpecifier("connectionTimeout",
				String.valueOf(DEFAULT_CONNECTION_TIMEOUT)));
		results.add(new BasicTextFieldSettingSpecifier("path", DEFAULT_PATH));
		results.add(
				new BasicTextFieldSettingSpecifier("pathFilterValue", DEFAULT_PATH_FILTER.pattern()));
		results.add(new BasicTextFieldSettingSpecifier("pathSnapshotFilterValue",
				DEFAULT_PATH_SNAPSHOT_FILTER.pattern()));

		return results;
	}

	// Accessors

	/**
	 * Get the file name path filter pattern.
	 * 
	 * @return the file name path filter pattern
	 */
	public Pattern getPathFilter() {
		return pathFilter;
	}

	/**
	 * Set the file name path filter pattern.
	 * 
	 * <p>
	 * Only file names that match this pattern will be considered a media
	 * resource.
	 * </p>
	 * 
	 * @param filterm
	 *        the file name path filter pattern to use, or {@literal null} for
	 *        all files
	 */
	public void setPathFilter(Pattern filter) {
		this.pathFilter = filter;
	}

	/**
	 * Get the file name path filter pattern, as a string.
	 * 
	 * @return the file name path filter pattern
	 */
	public String getPathFilterValue() {
		Pattern f = getPathFilter();
		return f != null ? f.pattern() : null;
	}

	/**
	 * Set the file name path filter pattern, as a string.
	 * 
	 * <p>
	 * Only file names that match this pattern will be considered a media
	 * resource.
	 * </p>
	 * 
	 * @param filterm
	 *        the file name path filter pattern to use, or {@literal null} for
	 *        all files
	 */
	public void setPathFilterValue(String filterValue) {
		try {
			setPathFilter(Pattern.compile(filterValue, Pattern.CASE_INSENSITIVE));
		} catch ( PatternSyntaxException e ) {
			log.error("Invalid pathFilter pattern `{}`: {}", filterValue, e.getMessage());
		}
	}

	/**
	 * Get the control ID.
	 * 
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 * 
	 * @param controlId
	 *        the control ID
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the file system path to the motion image directory.
	 * 
	 * @return the path to the motion image directory
	 */
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Get a setup resource provider to support viewing media images.
	 * 
	 * @return the setup resource provider
	 */
	public SetupResourceProvider getMediaResourceProvider() {
		return mediaResourceProvider;
	}

	/**
	 * Set a setup resource provider to support viewing media images.
	 * 
	 * @param mediaResourceProvider
	 *        the setup resource provider
	 */
	public void setMediaResourceProvider(SetupResourceProvider mediaResourceProvider) {
		this.mediaResourceProvider = mediaResourceProvider;
	}

	/**
	 * Get the number of seconds to allow for caching media setup resources.
	 * 
	 * @return the number of seconds to cache
	 */
	public int getResourceCacheSecs() {
		return resourceCacheSecs;
	}

	/**
	 * Set the number of seconds to allow for caching media setup resources.
	 * 
	 * @param resourceCacheSecs
	 *        the number of seconds to cache
	 */
	public void setResourceCacheSecs(int resourceCacheSecs) {
		this.resourceCacheSecs = resourceCacheSecs;
	}

	/**
	 * Get the snapshot file name path filter pattern.
	 * 
	 * @return the file name path filter pattern
	 */
	public Pattern getPathSnapshotFilter() {
		return pathSnapshotFilter;
	}

	/**
	 * Set the snapshot file name path filter pattern.
	 * 
	 * <p>
	 * Only file names that match this pattern will be considered as a snapshot
	 * media resource.
	 * </p>
	 * 
	 * @param filterm
	 *        the file name path filter pattern to use, or {@literal null} for
	 *        all files
	 */
	public void setPathSnapshotFilter(Pattern filter) {
		this.pathSnapshotFilter = filter;
	}

	/**
	 * Get the snapshot file name path filter pattern, as a string.
	 * 
	 * @return the file name path filter pattern
	 */
	public String getPathSnapshotFilterValue() {
		Pattern f = getPathSnapshotFilter();
		return f != null ? f.pattern() : null;
	}

	/**
	 * Set the snapshot file name path filter pattern, as a string.
	 * 
	 * <p>
	 * Only file names that match this pattern will be considered as a snapshot
	 * media resource.
	 * </p>
	 * 
	 * @param filterm
	 *        the file name path filter pattern to use, or {@literal null} for
	 *        all files
	 */
	public void setPathSnapshotFilterValue(String filterValue) {
		try {
			setPathSnapshotFilter(Pattern.compile(filterValue, Pattern.CASE_INSENSITIVE));
		} catch ( PatternSyntaxException e ) {
			log.error("Invalid pathSnapshotFilter pattern `{}`: {}", filterValue, e.getMessage());
		}
	}

	/**
	 * Get the base URL to the motion web server.
	 * 
	 * @return the base URL; defaults to {@link #DEFAULT_MOTION_BASE_URL}
	 */
	public String getMotionBaseUrl() {
		return motionBaseUrl;
	}

	/**
	 * Set the base URL to the motion web server.
	 * 
	 * @param motionBaseUrl
	 *        the base URL to set
	 */
	public void setMotionBaseUrl(String motionBaseUrl) {
		this.motionBaseUrl = motionBaseUrl;
	}

	/**
	 * Get the motion web server connection timeout.
	 * 
	 * @return the connection timeout, in milliseconds; defaults to
	 *         {@link #DEFAULT_CONNECTION_TIMEOUT}
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * Set the motion web server connection timeout.
	 * 
	 * @param connectionTimeout
	 *        the timeout to set, in milliseconds
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

}
