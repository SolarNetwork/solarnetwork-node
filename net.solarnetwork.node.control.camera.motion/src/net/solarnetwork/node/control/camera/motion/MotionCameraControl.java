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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicSetupResourceSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.setup.ResourceSetupResource;
import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.node.support.BaseIdentifiable;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class MotionCameraControl extends BaseIdentifiable implements SettingSpecifierProvider,
		NodeControlProvider, InstructionHandler, SetupResourceProvider {

	/** The default value of the {@code path} property. */
	public static final String DEFAULT_PATH = "/var/lib/motion";

	/** The default value of the {@code latestSnapshotFilename} property. */
	public static final String DEFAULT_LATEST_SNAPSHOT_FILENAME = "lastsnap.jpg";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String controlId;
	private String path = DEFAULT_PATH;
	private String latestSnapshotFilename = DEFAULT_LATEST_SNAPSHOT_FILENAME;
	private SetupResourceProvider snapshotResourceProvider;

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
		return false;
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		return null;
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

	private Resource snapshotResource() {
		String path = getPath();
		if ( path == null || path.isEmpty() ) {
			return null;
		}
		Path snapPath = null;

		String latestName = getLatestSnapshotFilename();
		if ( latestName != null && !latestName.isEmpty() ) {
			snapPath = Paths.get(path, latestName);
		} else {
			// search based on modification date
			try {
				snapPath = Files.list(Paths.get(path))
						.filter(p -> !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
						.max(Comparator.comparingLong(p -> {
							try {
								return Files.getLastModifiedTime(p).toMillis();
							} catch ( IOException e ) {
								return 0;
							}
						})).orElse(null);
			} catch ( IOException e ) {
				log.warn("Error searching for latest snapshot image file: {}", e.toString());
				return null;
			}
		}
		if ( snapPath != null && Files.isReadable(snapPath) ) {
			return new FileSystemResource(snapPath.toFile());
		}
		return null;
	}

	@Override
	public SetupResource getSetupResource(String resourceUID, Locale locale) {
		String snapResourceId = snapshotResourceId();
		if ( snapResourceId != null && snapResourceId.equals(resourceUID) ) {
			Resource snapshotResource = snapshotResource();
			if ( snapshotResource != null ) {
				try {
					return new ResourceSetupResource(snapshotResource, snapResourceId, "image/jpeg", 15,
							WEB_CONSUMER_TYPES, null);
				} catch ( IOException e ) {
					log.warn("Error getting latest snapshot image file: {}", e.toString());
				}
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

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(4);

		SetupResourceProvider snapProvider = getSnapshotResourceProvider();
		if ( snapProvider != null ) {
			String resourceId = snapshotResourceId();
			if ( resourceId != null ) {
				results.add(new BasicSetupResourceSettingSpecifier(snapshotResourceProvider,
						Collections.singletonMap("snap-id", resourceId)));
			}
		}

		results.add(new BasicTextFieldSettingSpecifier("controlId", ""));
		results.add(new BasicTextFieldSettingSpecifier("path", DEFAULT_PATH));
		results.add(new BasicTextFieldSettingSpecifier("latestSnapshotFilename",
				DEFAULT_LATEST_SNAPSHOT_FILENAME));

		return results;
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
	 * Get the name of the "latest snapshot" file within {@code path}.
	 * 
	 * @return the filename
	 */
	public String getLatestSnapshotFilename() {
		return latestSnapshotFilename;
	}

	/**
	 * Set the name of the "latest snapshot" file within {@code path}.
	 * 
	 * @param latestSnapshotFilename
	 *        the filename to set, or {@literal null} if file modification dates
	 *        must be used
	 */
	public void setLatestSnapshotFilename(String latestSnapshotFilename) {
		this.latestSnapshotFilename = latestSnapshotFilename;
	}

	/**
	 * Get a setup resource provider to support viewing snapshot images.
	 * 
	 * @return the setup resource provider
	 */
	public SetupResourceProvider getSnapshotResourceProvider() {
		return snapshotResourceProvider;
	}

	/**
	 * Set a setup resource provider to support viewing snapshot images.
	 * 
	 * @param snapshotResourceProvider
	 *        the setup resource provider
	 */
	public void setSnapshotResourceProvider(SetupResourceProvider snapshotResourceProvider) {
		this.snapshotResourceProvider = snapshotResourceProvider;
	}

}
