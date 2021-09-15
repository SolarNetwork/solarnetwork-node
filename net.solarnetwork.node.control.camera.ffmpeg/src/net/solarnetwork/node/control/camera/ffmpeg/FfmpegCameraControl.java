/* ==================================================================
 * FfmpegCameraControl.java - 31/08/2021 3:28:39 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.camera.ffmpeg;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.node.setup.SetupResource.WEB_CONSUMER_TYPES;
import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.io.ResultStatusException;
import net.solarnetwork.node.job.JobUtils;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.settings.support.BasicSetupResourceSettingSpecifier;
import net.solarnetwork.node.setup.ResourceSetupResource;
import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Integrate with the <a href="https://www.ffmpeg.org/">FFmpeg</a> media tool to
 * capture images from camera video streams.
 * 
 * @author matt
 * @version 2.0
 */
public class FfmpegCameraControl extends BaseIdentifiable
		implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler,
		SetupResourceProvider, SettingsChangeObserver, FfmpegService {

	/** The instruction signal name for initiating a snapshot. */
	public static final String SIGNAL_SNAPSHOT = "snapshot";

	/** The "last modified" media resource key. */
	public static final String MEDIA_RESOURCE_LAST_MODIFIED = "media-resource-last-modified";

	/** The default value of the {@code path} property. */
	public static final String DEFAULT_PATH = "var/ffmpeg-images";

	/** The default value of the {@code latestSnapshotFilename} property. */
	public static final Pattern DEFAULT_PATH_SNAPSHOT_FILTER = Pattern.compile(".+-snapshot\\.jpg");

	/** The default value for the {@code schedule} property. */
	public static final String DEFAULT_SCHEDULE = "0 0/10 * * * ?";

	/** The default value for the {@code resourceCacheSecs} property. */
	public static final int DEFAULT_RESOURCE_CACHE_SECS = 15;

	/** The default value for the {@code ffmpegPath} property. */
	public static final String DEFAULT_FFMPEG_PATH = "/usr/bin/ffmpeg";

	/** The default value for the {@code outputFileTemplate} property. */
	public static final String DEFAULT_OUTPUT_FILE_TEMPLATE = "ffmpeg-{ts}-snapshot.jpg";

	/** The group name used to schedule the invoker jobs as. */
	public static final String SNAPSHOT_JOB_NAME = "FFmpegSnapshot";

	/** The group name used to schedule the invoker jobs as. */
	public static final String SNAPSHOT_JOB_GROUP = "FFmpegCameraControl";

	/** The key used for the snapshot job. */
	public static final JobKey SNAPSHOT_JOB_KEY = new JobKey(SNAPSHOT_JOB_NAME, SNAPSHOT_JOB_GROUP);

	private static final DateTimeFormatter OUTPUT_FILE_DATE_FORMAT = DateTimeFormatter
			.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

	private final OptionalService<Scheduler> scheduler;
	private String controlId;
	private String path = DEFAULT_PATH;
	private String outputFileTemplate = DEFAULT_OUTPUT_FILE_TEMPLATE;
	private Pattern pathSnapshotFilter = DEFAULT_PATH_SNAPSHOT_FILTER;
	private SetupResourceProvider mediaResourceProvider;
	private int resourceCacheSecs = DEFAULT_RESOURCE_CACHE_SECS;
	private String schedule = DEFAULT_SCHEDULE;
	private String ffmpegPath = DEFAULT_FFMPEG_PATH;
	private String ffmpegSnapshotOptions;

	private Trigger snapshotTrigger;

	/**
	 * Constructor.
	 * 
	 * @param scheduler
	 *        the scheduler
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public FfmpegCameraControl(OptionalService<Scheduler> scheduler) {
		super();
		if ( scheduler == null ) {
			throw new IllegalArgumentException("The scheduler argument must not be null.");
		}
		this.scheduler = scheduler;
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		rescheduleSnapshotJob();
	}

	/**
	 * Call when this service has been configured.
	 */
	public synchronized void startup() {
		rescheduleSnapshotJob();
	}

	/**
	 * Call when this service is no longer needed to clean up resources.
	 */
	public synchronized void shutdown() {
		unscheduleSnapshotJob();
	}

	private synchronized void rescheduleSnapshotJob() {
		unscheduleSnapshotJob();

		if ( !isSnapshotConfigurationValid() ) {
			return;
		}

		final Scheduler s = service(scheduler);
		if ( s == null ) {
			return;
		}

		final String schedule = getSchedule();
		final String jobDesc = snapshotJobDescription();
		final TriggerKey triggerKey = triggerKey();
		final JobDataMap props = new JobDataMap();
		props.put("service", this);
		Trigger trigger = JobUtils.scheduleJob(s, FfmpegSnapshotJob.class, SNAPSHOT_JOB_KEY, jobDesc,
				schedule, triggerKey, props);
		snapshotTrigger = trigger;
	}

	private boolean isSnapshotConfigurationValid() {
		return ffmpegPath != null && !ffmpegPath.isEmpty() && ffmpegSnapshotOptions != null
				&& !ffmpegSnapshotOptions.isEmpty() && outputFileTemplate != null
				&& !outputFileTemplate.isEmpty();
	}

	private synchronized void unscheduleSnapshotJob() {
		if ( snapshotTrigger == null ) {
			return;
		}
		Scheduler s = service(scheduler);
		if ( s == null ) {
			return;
		}
		try {
			JobUtils.unscheduleJob(s, snapshotJobDescription(), snapshotTrigger.getKey());
		} catch ( Exception e ) {
			// ignore
		}
		snapshotTrigger = null;
	}

	private String snapshotJobDescription() {
		return String.format("FFmpeg auto-snapshot [%s]", controlId);
	}

	private TriggerKey triggerKey() {
		String controlId = getControlId();
		if ( controlId == null ) {
			controlId = "";
		}
		return new TriggerKey(String.format("Snapshot-%s", controlId), SNAPSHOT_JOB_GROUP);
	}

	@Override
	public boolean takeSnapshot() throws IOException {
		final String ffmpeg = getFfmpegPath();
		if ( ffmpeg == null || ffmpeg.isEmpty() ) {
			log.error("No FFmpeg path configured to take {} snapshot with.", controlId);
			return false;
		}
		final String options = getFfmpegSnapshotOptions();
		if ( options == null || options.isEmpty() ) {
			log.error("No FFmpeg options configured to take {} snapshot with.", controlId);
			return false;
		}
		String result = executeFfmpeg(ffmpeg, options);
		log.info("Snapshot successful for ffmpeg snapshot {}: {}", controlId, result);
		return result != null;
	}

	private String executeFfmpeg(final String ffmpegPath, final String ffmpegOptions)
			throws IOException {
		final Instant now = Instant.now();
		final String ts = OUTPUT_FILE_DATE_FORMAT.format(now);
		final String outFileName = StringUtils.expandTemplateString(outputFileTemplate,
				singletonMap("ts", ts));
		final Path outFilePath = Paths.get(path, outFileName);

		if ( !Files.isDirectory(outFilePath.getParent()) ) {
			Files.createDirectories(outFilePath.getParent());
		}

		String args = StringUtils.expandTemplateString(ffmpegOptions,
				singletonMap("outputFile", outFilePath.toString()));
		if ( !args.contains(outFilePath.toString()) ) {
			args += " " + outFilePath.toString();
		}

		final List<String> command = new ArrayList<>(Arrays.asList(args.split("\\s+")));
		command.add(0, ffmpegPath);
		log.debug("Executing ffmpeg {} {}", ffmpegPath, args);
		ProcessBuilder pb = new ProcessBuilder(command);
		try {
			Process pr = pb.start();
			int status = pr.waitFor();
			if ( status != 0 ) {
				log.error("Error executing {} {}: {}", ffmpegPath, args, status);
				return null;
			}
			return outFilePath.toString();
		} catch ( IOException e ) {
			throw new IOException("Error executing [" + ffmpegPath + " " + args + ": " + e.toString(),
					e);
		} catch ( InterruptedException e ) {
			log.warn("Interrupted executing {} {}", ffmpegPath, args);
			return null;
		}
	}

	// NodeControlProvider

	@Override
	public List<String> getAvailableControlIds() {
		final String controlId = getControlId();
		return (controlId == null || controlId.isEmpty() ? Collections.emptyList()
				: Collections.singletonList(controlId));
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		return null;
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SIGNAL.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
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
		try {
			if ( SIGNAL_SNAPSHOT.equalsIgnoreCase(signal) ) {
				if ( takeSnapshot() ) {
					return InstructionUtils.createStatus(instruction, InstructionState.Completed);
				}
			}
		} catch ( IOException e ) {
			log.error("Communication error with ffmpeg {} at {}: {}", ffmpegPath, e.toString());
		} catch ( ResultStatusException e ) {
			log.error("Error response code received from ffmpeg {} request {}: {}", ffmpegPath,
					e.getUrl(), e.getStatusCode());
		}
		return InstructionUtils.createStatus(instruction, InstructionState.Declined);
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

	private boolean matchesSnapshotResource(Path p) {
		Pattern pat = getPathSnapshotFilter();
		String name = p.getFileName().toString();
		return (pat != null ? pat.matcher(name).matches() : false);
	}

	@Override
	public SetupResource getSetupResource(String resourceUID, Locale locale) {
		Resource resource = null;
		final String snapResourceId = snapshotResourceId();
		if ( snapResourceId != null && snapResourceId.equals(resourceUID) ) {
			resource = snapshotResource();
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
	public String getSettingUid() {
		return "net.solarnetwork.node.control.camera.ffmpeg";
	}

	@Override
	public String getDisplayName() {
		return "FFmpeg Camera Control " + getControlId();
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
		List<SettingSpecifier> results = new ArrayList<>(12);

		SetupResourceProvider snapProvider = getMediaResourceProvider();
		if ( snapProvider != null ) {
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
		results.add(new BasicTextFieldSettingSpecifier("schedule", DEFAULT_SCHEDULE));
		results.add(new BasicTextFieldSettingSpecifier("ffmpegPath", DEFAULT_FFMPEG_PATH));
		results.add(new BasicTextFieldSettingSpecifier("ffmpegSnapshotOptions", null));
		results.add(new BasicTextFieldSettingSpecifier("path", DEFAULT_PATH));
		results.add(
				new BasicTextFieldSettingSpecifier("outputFileTemplate", DEFAULT_OUTPUT_FILE_TEMPLATE));
		results.add(new BasicTextFieldSettingSpecifier("pathSnapshotFilterValue",
				DEFAULT_PATH_SNAPSHOT_FILTER.pattern()));

		return results;
	}

	// Accessors

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
	 * Get the file system path to the output image directory.
	 * 
	 * @return the path to the output image directory; defaults to
	 *         {@link #DEFAULT_PATH}
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the file system path to the output image directory.
	 * 
	 * @param path
	 *        the path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Get the output file template.
	 * 
	 * @return the output file template; defaults to
	 *         {@link #DEFAULT_OUTPUT_FILE_TEMPLATE}
	 */
	public String getOutputFileTemplate() {
		return outputFileTemplate;
	}

	/**
	 * Set the output file template.
	 * 
	 * @param outputFileTemplate
	 *        the template to set
	 */
	public void setOutputFileExtension(String outputFileTemplate) {
		this.outputFileTemplate = outputFileTemplate;
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
	 * @return the number of seconds to cache; defaults to
	 *         {@link #DEFAULT_RESOURCE_CACHE_SECS}
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
	 * @param filter
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
	 * @param filterValue
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
	 * Get the auto-snapshot schedule.
	 * 
	 * @return the schedule
	 */
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Set the auto-snapshot schedule.
	 * 
	 * <p>
	 * This schedule defines when to manually request snapshot images from
	 * ffmpeg. If just a number, then the frequency in seconds at which to
	 * create snapshots. Otherwise a Quartz-compatible cron expression
	 * representing the schedule at which to create snapshots.
	 * </p>
	 * 
	 * @param schedule
	 *        the schedule
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the system path to the {@literal ffmpeg} program.
	 * 
	 * @return the path; defaults to {@link #DEFAULT_FFMPEG_PATH}
	 */
	public String getFfmpegPath() {
		return ffmpegPath;
	}

	/**
	 * Set the system path to the {@literal ffmpeg} program.
	 * 
	 * @param ffmpegPath
	 *        the path to set
	 */
	public void setFfmpegPath(String ffmpegPath) {
		this.ffmpegPath = ffmpegPath;
	}

	/**
	 * Get the FFmpeg snapshot options.
	 * 
	 * @return the options to use
	 */
	public String getFfmpegSnapshotOptions() {
		return ffmpegSnapshotOptions;
	}

	/**
	 * Set the FFmpeg snapshot options.
	 * 
	 * @param ffmpegSnapshotOptions
	 *        the options to set
	 */
	public void setFfmpegSnapshotOptions(String ffmpegSnapshotOptions) {
		this.ffmpegSnapshotOptions = ffmpegSnapshotOptions;
	}

}
