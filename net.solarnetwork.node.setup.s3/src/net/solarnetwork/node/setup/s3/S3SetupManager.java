/* ==================================================================
 * S3SetupManager.java - 13/10/2017 10:29:06 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.s3;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.NodeMetadataService;
import net.solarnetwork.node.PlatformService;
import net.solarnetwork.node.PlatformService.PlatformState;
import net.solarnetwork.node.PlatformService.PlatformTask;
import net.solarnetwork.node.PlatformService.PlatformTaskStatusHandler;
import net.solarnetwork.node.RemoteServiceException;
import net.solarnetwork.node.SetupSettings;
import net.solarnetwork.node.SystemService;
import net.solarnetwork.node.backup.s3.S3BackupService;
import net.solarnetwork.node.backup.s3.S3Client;
import net.solarnetwork.node.backup.s3.S3ObjectReference;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.setup.SetupException;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Service for provisioning node resources based on versioned resource sets.
 * 
 * @author matt
 * @version 1.0
 */
public class S3SetupManager implements FeedbackInstructionHandler {

	private static final String SETTING_KEY_VERSION = "solarnode.s3.version";

	/**
	 * The instruction topic for triggering a platform update.
	 */
	public static final String TOPIC_UPDATE_PLATFORM = "UpdatePlatform";

	/**
	 * Instruction parameter for a specific version to update to.
	 * 
	 * <p>
	 * If not provided, the latest version is assumed.
	 * </p>
	 */
	public static final String INSTRUCTION_PARAM_VERSION = "Version";

	/** The default value for the {@code workDirectory} property. */
	public static final String DEFAULT_WORK_DIRECTORY = "work/s3-setup";

	/** A prefix applied to metadata objects. */
	public static final String META_OBJECT_KEY_PREFIX = "setup-meta/";

	/** A prefix applied to data objects. */
	public static final String DATA_OBJECT_KEY_PREFIX = "setup-data/";

	/**
	 * The placeholder string in the {@code syncCommand} for the source
	 * directory path.
	 */
	public static final String SOURCE_FILE_PLACEHOLDER = "__SOURCE_FILE__";

	/**
	 * The placeholder string in the {@code syncCommand} for the destination
	 * directory path.
	 */
	public static final String DESTINATION_DIRECTORY_PLACEHOLDER = "__DEST_DIR__";

	/**
	 * The default value of the {@code tarCommand} property.
	 * 
	 * <p>
	 * The tar command is expected to print the names of the files as it
	 * extracts them, which is usually done with a {@literal -v} argument.
	 * </p>
	 */
	public static final List<String> DEFAULT_TAR_COMMAND = Collections.unmodifiableList(Arrays
			.asList("tar", "xvf", SOURCE_FILE_PLACEHOLDER, "-C", DESTINATION_DIRECTORY_PLACEHOLDER));

	private static final Pattern VERSION_PAT = Pattern.compile(".*/(\\d+)");
	private static final Pattern TARBALL_PAT = Pattern.compile("\\.(tar|tgz|tbz2|txz)$");
	private static final Pattern TAR_LIST_PAT = Pattern.compile("^\\w (.*)$");

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.setSerializationInclusion(JsonInclude.Include.NON_NULL);

	private static final Pattern LEADING_ZEROS_PAT = Pattern.compile("^0+");

	/**
	 * Get the default destination path.
	 * 
	 * <p>
	 * This returns the system property {@link Constants#SYSTEM_PROP_NODE_HOME}
	 * if available, and falls back to the working directory of the process
	 * otherwise.
	 * </p>
	 * 
	 * @return the default destination path value
	 */
	public static final String defaultDestinationPath() {
		String home = System.getProperty(Constants.SYSTEM_PROP_NODE_HOME, null);
		if ( home == null ) {
			home = Paths.get(".").toAbsolutePath().normalize().toString();
		}
		return home;
	}

	private S3Client s3Client;
	private String objectKeyPrefix = S3BackupService.DEFAULT_OBJECT_KEY_PREFIX;
	private SettingDao settingDao;
	private String maxVersion = null;
	private boolean performFirstTimeUpdate = true;
	private String workDirectory = DEFAULT_WORK_DIRECTORY;
	private List<String> tarCommand = DEFAULT_TAR_COMMAND;
	private String destinationPath = defaultDestinationPath();
	private OptionalService<NodeMetadataService> nodeMetadataService;
	private OptionalService<PlatformService> platformService;
	private OptionalService<SystemService> systemService;
	private TaskExecutor taskExecutor;
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Call after all properties are configured on the class.
	 */
	public void init() {
		if ( !performFirstTimeUpdate ) {
			return;
		}
		log.info("First time update check enabled; will check if update needed");
		if ( taskExecutor != null ) {
			taskExecutor.execute(new Runnable() {

				@Override
				public void run() {
					performFirstTimeUpdateIfNeeded();
				}
			});
		} else {
			performFirstTimeUpdateIfNeeded();
		}
	}

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_UPDATE_PLATFORM.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		InstructionStatus status = processInstructionWithFeedback(instruction);
		return (status != null ? status.getInstructionState() : null);
	}

	@Override
	public InstructionStatus processInstructionWithFeedback(Instruction instruction) {
		if ( instruction == null || !TOPIC_UPDATE_PLATFORM.equals(instruction.getTopic()) ) {
			return null;
		}
		String instrVersion = instruction.getParameterValue(INSTRUCTION_PARAM_VERSION);
		String metaKey = null;
		try {
			if ( instrVersion != null ) {
				metaKey = objectKeyForPath(META_OBJECT_KEY_PREFIX + instrVersion + ".json");
			} else {
				S3ObjectReference versionObj = getConfigObjectForUpdateToHighestVersion();
				if ( versionObj != null ) {
					metaKey = versionObj.getKey();
				}
			}
			if ( metaKey == null ) {
				String msg = "Unable to setup from S3: no versions available at path "
						+ objectKeyForPath(META_OBJECT_KEY_PREFIX);
				log.warn(msg);
				return statusWithError(instruction, "S3SM003", msg);
			}
			S3SetupConfiguration config = getSetupConfiguration(metaKey);
			applySetup(config);
			return instruction.getStatus().newCopyWithState(InstructionState.Completed);
		} catch ( RemoteServiceException e ) {
			log.warn("Error accessing S3: {}", e.getMessage());
			return statusWithError(instruction, "S3SM001", e.getMessage());
		} catch ( IOException e ) {
			log.warn("Communication error apply S3 setup: {}", e.getMessage());
			return statusWithError(instruction, "S3SM002", e.getMessage());
		}
	}

	private InstructionStatus statusWithError(Instruction instruction, String code, String message) {
		Map<String, Object> resultParams = new LinkedHashMap<>();
		resultParams.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, code);
		resultParams.put(InstructionStatus.MESSAGE_RESULT_PARAM, message);
		return instruction.getStatus().newCopyWithState(InstructionState.Declined, resultParams);
	}

	private boolean isConfigured() {
		return (s3Client != null && s3Client.isConfigured());
	}

	private synchronized S3SetupTaskResult applySetup(S3SetupConfiguration config) throws IOException {
		if ( config == null || config.getObjects() == null || config.getObjects().length < 1 ) {
			return new S3SetupTaskResult(false, Collections.emptySet(), Collections.emptySet());
		}
		S3SetupManagerPlatformTask task = new S3SetupManagerPlatformTask(config);
		PlatformService pService = (platformService != null ? platformService.service() : null);
		if ( pService != null ) {
			Future<S3SetupTaskResult> result = pService
					.performTaskWithState(PlatformState.UserBlockingSystemTask, task);
			try {
				return result.get();
			} catch ( InterruptedException e ) {
				throw new RuntimeException("Interrupted applying setup version " + config.getVersion());
			} catch ( ExecutionException e ) {
				if ( e.getCause() instanceof IOException ) {
					throw (IOException) e.getCause();
				}
				throw new RuntimeException("Exception applying setup version " + config.getVersion(), e);
			}
		} else {
			try {
				return task.call();
			} catch ( IOException e ) {
				throw e;
			} catch ( Exception e ) {
				throw new RuntimeException("Exception applying setup version " + config.getVersion(), e);
			}
		}
	}

	private static enum S3SetupManagerPlatformTaskState {
		Idle,

		DownloadingAsset,

		InstallingAsset,

		SyncingPath,

		DeletingAsset,

		PostingSetupVersion,

		Complete,
	}

	private class S3SetupManagerPlatformTask implements PlatformTask<S3SetupTaskResult> {

		private final String taskId;
		private final AtomicReference<S3SetupManagerPlatformTaskState> state;
		private final AtomicReference<String> assetName;
		private final List<PlatformTaskStatusHandler> statusHandlers = new ArrayList<>(2);
		private final int stepCount;
		private final S3SetupConfiguration config;
		private int step;
		private boolean complete;

		public S3SetupManagerPlatformTask(S3SetupConfiguration config) {
			super();
			assert config != null;
			this.taskId = UUID.randomUUID().toString();
			this.config = config;
			this.stepCount = config.getTotalStepCount() + 1;
			state = new AtomicReference<S3SetupManagerPlatformTaskState>(
					S3SetupManagerPlatformTaskState.Idle);
			assetName = new AtomicReference<String>(null);
			step = 0;
			complete = false;
		}

		@Override
		public S3SetupTaskResult call() throws Exception {
			try {
				Set<Path> installedFiles = applySetupObjects(config);
				Set<Path> deletedFiles = applySetupSyncPaths(config, installedFiles);
				deletedFiles.addAll(applySetupCleanPaths(config));
				try {
					setState(S3SetupManagerPlatformTaskState.PostingSetupVersion, null);
					updateNodeMetadataForInstalledVersion(config);
				} catch ( SetupException e ) {
					// assume node not associated yet
					log.warn("Error publishing S3 setup version node metadata: {}", e.getMessage());
				}

				complete = true;
				setStateAndIncrementStep(S3SetupManagerPlatformTaskState.Complete, null);

				if ( config.isRestartRequired() ) {
					SystemService sysService = (systemService != null ? systemService.service() : null);
					if ( sysService != null ) {
						sysService.exit(true);
					} else {
						log.warn("S3 setup {} requires restart, but no SystemService available",
								config.getObjectKey());
					}
				}

				return new S3SetupTaskResult(true, installedFiles, deletedFiles);
			} finally {
				if ( !complete ) {
					complete = true;
					informStatusHandlers();
				}
			}
		}

		private synchronized void informStatusHandlers() {
			for ( PlatformTaskStatusHandler handler : statusHandlers ) {
				handler.taskStatusUpdated(this);
			}
		}

		@Override
		public synchronized void registerStatusHandler(PlatformTaskStatusHandler handler) {
			statusHandlers.add(handler);
		}

		@Override
		public String getTaskId() {
			return taskId;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public boolean isRestartRequired() {
			return config.isRestartRequired();
		}

		@Override
		public String getTitle(Locale locale) {
			MessageSource ms = messageSource;
			if ( ms == null ) {
				return null;
			}
			return messageSource.getMessage("platformTask.title", new Object[] { getConfigVersion() },
					locale);
		}

		private String getConfigVersion() {
			String version = config.getVersion();
			return (version != null ? LEADING_ZEROS_PAT.matcher(version).replaceFirst("") : "");
		}

		@Override
		public String getMessage(Locale locale) {
			MessageSource ms = messageSource;
			if ( ms == null ) {
				return null;
			}
			S3SetupManagerPlatformTaskState s = state.get();
			String asset = assetName.get();
			switch (s) {
				case Complete:
					return ms.getMessage("platformTask.message.Complete", null, locale);

				case Idle:
					return ms.getMessage("platformTask.message.Idle", null, locale);

				case DownloadingAsset:
				case InstallingAsset:
				case SyncingPath:
				case DeletingAsset:
					return ms.getMessage("platformTask.message." + s.toString(), new Object[] { asset },
							locale);

				case PostingSetupVersion:
					return ms.getMessage("platformTask.message.PostingSetupVersion",
							new Object[] { getConfigVersion() }, locale);

				default:
					return null;
			}
		}

		@Override
		public double getPercentComplete() {
			return (double) step / (double) stepCount;
		}

		private void setState(S3SetupManagerPlatformTaskState taskState, String asset) {
			state.set(taskState);
			assetName.set(asset);
			informStatusHandlers();
		}

		private void setStateAndIncrementStep(S3SetupManagerPlatformTaskState taskState, String asset) {
			state.set(taskState);
			assetName.set(asset);
			incrementStep();
		}

		private void incrementStep() {
			step++;
			informStatusHandlers();
		}

		/**
		 * Download and install all setup objects in a given configuration.
		 * 
		 * @param config
		 *        the configuration to apply
		 * @return the set of absolute paths of all installed files (or an empty
		 *         set if nothing installed)
		 * @throws IOException
		 *         if an IO error occurs
		 */
		private Set<Path> applySetupObjects(S3SetupConfiguration config) throws IOException {
			final String[] objects = config.getObjects();
			final int objCount = (objects != null ? config.getObjects().length : 0);
			if ( objCount < 1 ) {
				return Collections.emptySet();
			}
			Set<Path> installed = new LinkedHashSet<>();
			for ( int i = 0; i < objCount; i++, incrementStep() ) {
				String dataObjKey = objects[i];
				if ( !TARBALL_PAT.matcher(dataObjKey).find() ) {
					log.warn("S3 setup resource {} not a supported type; skipping");
					continue;
				}

				setState(S3SetupManagerPlatformTaskState.DownloadingAsset, dataObjKey);
				S3Object obj = s3Client.getObject(dataObjKey);
				if ( obj == null ) {
					log.warn("Data object {} not found, cannot apply setup", dataObjKey);
					continue;
				}

				File workDir = new File(workDirectory);
				if ( !workDir.exists() ) {
					if ( !workDir.mkdirs() ) {
						log.warn("Unable to create work dir {}", workDir);
					}
				}

				// download the data object to the work dir
				String dataObjFilename = DigestUtils.sha1Hex(dataObjKey);
				File dataObjFile = new File(workDir, dataObjFilename);
				try (InputStream in = obj.getObjectContent();
						OutputStream out = new BufferedOutputStream(new FileOutputStream(dataObjFile))) {
					log.info("Downloading S3 setup resource {} -> {}", dataObjKey, dataObjFile);
					FileCopyUtils.copy(obj.getObjectContent(), out);

					// extract tarball
					setStateAndIncrementStep(S3SetupManagerPlatformTaskState.InstallingAsset,
							dataObjKey);
					List<Path> extractedPaths = extractTarball(dataObjFile);
					installed.addAll(extractedPaths);
				} finally {
					if ( dataObjFile.exists() ) {
						dataObjFile.delete();
					}
				}
			}
			if ( !installed.isEmpty() ) {
				log.info("Installed files from objects {}: {}", Arrays.asList(config.getObjects()),
						installed);
			}
			return installed;
		}

		private Set<Path> applySetupSyncPaths(S3SetupConfiguration config, Set<Path> installedFiles)
				throws IOException {
			if ( config.getSyncPaths() == null || config.getSyncPaths().length < 1 ) {
				return Collections.emptySet();
			}
			Map<String, ?> sysProps = getPathTemplateVariables();
			Set<Path> deleted = new LinkedHashSet<>();
			for ( String syncPath : config.getSyncPaths() ) {
				setState(S3SetupManagerPlatformTaskState.SyncingPath, syncPath);
				syncPath = StringUtils.expandTemplateString(syncPath, sysProps);
				Path path = FileSystems.getDefault().getPath(syncPath).toAbsolutePath().normalize();
				Set<Path> result = applySetupSyncPath(path, installedFiles);
				deleted.addAll(result);
				incrementStep();
			}
			if ( !deleted.isEmpty() ) {
				log.info("Deleted files from syncPaths {}: {}", Arrays.asList(config.getSyncPaths()),
						deleted);
			}
			return deleted;
		}

		/**
		 * Apply sync rules to a specific directory.
		 * 
		 * <p>
		 * This method will delete any file found in {@code dir} that is
		 * <b>not</b> also in {@code installedFiles}.
		 * </p>
		 * 
		 * @param dir
		 *        the directory to delete files from
		 * @param installedFiles
		 *        the list of files to keep (these must be absolute paths)
		 * @return the set of deleted files, or an empty set if nothing deleted
		 * @throws IOException
		 *         if an IO error occurs
		 */
		private Set<Path> applySetupSyncPath(Path dir, Set<Path> installedFiles) throws IOException {
			if ( !Files.isDirectory(dir) ) {
				return Collections.emptySet();
			}
			Set<Path> deleted = new LinkedHashSet<>();
			Files.walk(dir).filter(p -> !Files.isDirectory(p)
					&& !installedFiles.contains(p.toAbsolutePath().normalize())).forEach(p -> {
						try {
							log.trace("Deleting syncPath {} file {}", dir, p);
							if ( Files.deleteIfExists(p) ) {
								deleted.add(p);
							}
						} catch ( IOException e ) {
							log.warn("Error deleting syncPath {} file {}: {}", dir, p, e.getMessage());
						}
					});
			return deleted;
		}

		private Set<Path> applySetupCleanPaths(S3SetupConfiguration config) throws IOException {
			if ( config.getCleanPaths() == null || config.getCleanPaths().length < 1 ) {
				return Collections.emptySet();
			}

			Map<String, ?> sysProps = getPathTemplateVariables();
			Set<Path> deleted = new LinkedHashSet<>();
			for ( String cleanPath : config.getCleanPaths() ) {
				setState(S3SetupManagerPlatformTaskState.DeletingAsset, cleanPath);
				String path = StringUtils.expandTemplateString(cleanPath, sysProps);
				if ( path.startsWith("file:") ) {
					path = path.substring(5);
				}
				File cleanFile = new File(path);
				if ( cleanFile.exists() ) {
					if ( FileSystemUtils.deleteRecursively(cleanFile) ) {
						deleted.add(cleanFile.toPath());
					}
				}
				incrementStep();
			}
			if ( !deleted.isEmpty() ) {
				log.info("Deleted files from cleanPaths {}: {}", Arrays.asList(config.getCleanPaths()),
						deleted);
			}
			return deleted;
		}

		private List<Path> extractTarball(File tarball) throws IOException {
			List<String> cmd = new ArrayList<>(tarCommand.size());
			String tarballPath = tarball.getAbsolutePath();
			for ( String param : tarCommand ) {
				param = param.replace(SOURCE_FILE_PLACEHOLDER, tarballPath);
				param = param.replace(DESTINATION_DIRECTORY_PLACEHOLDER, destinationPath);
				cmd.add(param);
			}
			if ( log.isDebugEnabled() ) {
				StringBuilder buf = new StringBuilder();
				for ( String p : cmd ) {
					if ( buf.length() > 0 ) {
						buf.append(' ');
					}
					buf.append(p);
				}
				log.debug("Tar command: {}", buf.toString());
			}
			log.info("Extracting S3 tar archive {}", tarball);
			List<Path> extractedPaths = new ArrayList<>();
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true); // OS X tar output list to STDERR; Linux GNU tar to STDOUT
			Process pr = pb.start();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
				String line = null;
				while ( (line = in.readLine()) != null ) {
					Matcher m = TAR_LIST_PAT.matcher(line);
					if ( m.matches() ) {
						line = m.group(1);
					}
					Path path = FileSystems.getDefault().getPath(line).toAbsolutePath().normalize();
					extractedPaths.add(path);
					log.trace("Installed setup resource: {}", line);
				}
			}
			try {
				pr.waitFor();
			} catch ( InterruptedException e ) {
				log.warn("Interrupted waiting for tar command to complete");
			}
			if ( pr.exitValue() != 0 ) {
				String output = extractedPaths.stream().map(p -> p.toString())
						.collect(Collectors.joining("\n")).trim();
				log.error("Tar command returned non-zero exit code {}: {}", pr.exitValue(), output);
				throw new IOException(
						"Tar command returned non-zero exit code " + pr.exitValue() + ": " + output);
			}
			return extractedPaths;
		}

	}

	private Map<String, ?> getPathTemplateVariables() {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Map<String, ?> sysProps = (Map) System.getProperties();
		return sysProps;
	}

	private void updateNodeMetadataForInstalledVersion(S3SetupConfiguration config) {
		if ( config.getVersion() == null ) {
			return;
		}

		log.info("S3 setup version {} installed", config.getVersion());
		settingDao.storeSetting(SETTING_KEY_VERSION, SetupSettings.SETUP_TYPE_KEY, config.getVersion());
		publishNodeMetadataForInstalledVersion(config.getVersion());
	}

	private void publishNodeMetadataForInstalledVersion(String version) {
		NodeMetadataService service = (nodeMetadataService != null ? nodeMetadataService.service()
				: null);
		if ( service == null ) {
			log.warn("No NodeMetadataService available to publish installed S3 version {}", version);
			return;
		}
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("setup", "s3-version", version);
		service.addNodeMetadata(meta);
	}

	private void performFirstTimeUpdateIfNeeded() {
		if ( !isConfigured() ) {
			log.info("S3 not configured, cannot perform first time update check");
			// TODO: perhaps delay and try again later?
			return;
		}
		String installedVersion = settingDao.getSetting(SETTING_KEY_VERSION,
				SetupSettings.SETUP_TYPE_KEY);
		if ( installedVersion != null ) {
			log.info("S3 setup version {} detected, not performing first time update", installedVersion);
			try {
				// publish the installed version each time we start up, to make sure pushed out when associated
				publishNodeMetadataForInstalledVersion(installedVersion);
			} catch ( SetupException e ) {
				// assume node not associated yet
				log.warn("Error publishing S3 setup version node metadata: {}", e.getMessage());
			}
			return;
		}
		performUpdateToHighestVersion();
	}

	private void performUpdateToHighestVersion() {
		try {
			S3ObjectReference versionObj = getConfigObjectForUpdateToHighestVersion();
			if ( versionObj == null ) {
				log.info("No S3 setup versions available at {}; nothing to update to",
						objectKeyForPath(META_OBJECT_KEY_PREFIX));
				return;
			}
			log.info("S3 setup {} detected, will install now", versionObj.getKey());
			S3SetupConfiguration config = getSetupConfiguration(versionObj.getKey());
			applySetup(config);
		} catch ( IOException e ) {
			log.warn("IO error performing update: {}", e.getMessage());
		} catch ( RemoteServiceException e ) {
			log.warn("Error accessing S3: {}", e.getMessage());
		}

	}

	/**
	 * Get a {@link S3SetupConfiguration} for a specific S3 object key.
	 * 
	 * <p>
	 * This method will populate the {@code objectKey} and {@code version}
	 * properties based on the passed on {@code objectKey}.
	 * </p>
	 * 
	 * @param objectKey
	 *        the S3 key of the object to load
	 * @return the parsed S3 setup metadata object
	 * @throws IOException
	 *         if an IO error occurs
	 */
	private S3SetupConfiguration getSetupConfiguration(String objectKey) throws IOException {
		String metaJson = s3Client.getObjectAsString(objectKey);
		S3SetupConfiguration config = OBJECT_MAPPER.readValue(metaJson, S3SetupConfiguration.class);
		config.setObjectKey(objectKey);
		if ( config.getVersion() == null ) {
			// apply from key
			Matcher ml = VERSION_PAT.matcher(objectKey);
			if ( ml.find() ) {
				String v = ml.group(1);
				config.setVersion(v);
			}
		}
		return config;
	}

	/**
	 * Get the S3 object for the {@link S3SetupConfiguration} to perform an
	 * update to the highest available package version.
	 * 
	 * <p>
	 * If a {@code maxVersion} is configured, this method will find the highest
	 * available package version less than or equal to {@code maxVersion}.
	 * </p>
	 * 
	 * @return the S3 object that holds the setup metadata to update to, or
	 *         {@literal null} if not available
	 */
	private S3ObjectReference getConfigObjectForUpdateToHighestVersion() throws IOException {
		final String metaDir = objectKeyForPath(META_OBJECT_KEY_PREFIX);
		Set<S3ObjectReference> objs = s3Client.listObjects(metaDir);
		S3ObjectReference versionObj = null;
		if ( maxVersion == null ) {
			// take the last (highest version), excluding the meta dir itself
			versionObj = objs.stream().filter(o -> !metaDir.equals(o.getKey())).reduce((l, r) -> r)
					.orElse(null);
		} else {
			final String max = maxVersion;
			versionObj = objs.stream().max((l, r) -> {
				String vl = null;
				String vr = null;
				Matcher ml = VERSION_PAT.matcher(l.getKey());
				Matcher mr = VERSION_PAT.matcher(r.getKey());
				if ( ml.find() && mr.find() ) {
					vl = ml.group(1);
					if ( vl.compareTo(max) > 0 ) {
						vl = null;
					}
					vr = mr.group(1);
					if ( vr.compareTo(max) > 0 ) {
						vr = null;
					}
				}
				if ( vl == null && vr == null ) {
					return 0;
				} else if ( vl == null ) {
					return -1;
				} else if ( vr == null ) {
					return 1;
				}
				return vl.compareTo(vr);
			}).orElse(null);
		}
		return versionObj;
	}

	/**
	 * Construct a full S3 object key that includes the configured
	 * {@code objectKeyPrefix} from a relative path value.
	 * 
	 * @param path
	 *        the relative object key path to get a full object key for
	 * @return the S3 object key
	 */
	private String objectKeyForPath(String path) {
		String globalPrefix = this.objectKeyPrefix;
		if ( globalPrefix == null ) {
			return path;
		}
		return globalPrefix + path;
	}

	/**
	 * Set the {@link S3Client} to use for accessing S3.
	 * 
	 * @param s3Client
	 *        the client to use
	 */
	public void setS3Client(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	/**
	 * Set the maximum version to update to.
	 * 
	 * @param maxVersion
	 *        the max version, or {@literal null} or {@literal 0} for no maximum
	 */
	public void setMaxVersion(String maxVersion) {
		this.maxVersion = maxVersion;
	}

	/**
	 * Set the {@link SettingDao} to use for maintaining persistent settings.
	 * 
	 * @param settingDao
	 *        the DAO to use for settings
	 */
	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

	/**
	 * Set a flag controlling if an update is attempted when the service starts
	 * up and no update has ever been performed before.
	 * 
	 * @param performFirstTimeUpdate
	 *        {@literal true} to perform an update after the first time starting
	 *        up
	 */
	public void setPerformFirstTimeUpdate(boolean performFirstTimeUpdate) {
		this.performFirstTimeUpdate = performFirstTimeUpdate;
	}

	/**
	 * Set a S3 object key prefix to use.
	 * 
	 * <p>
	 * This can essentially be a folder path to prefix all data with.
	 * </p>
	 * 
	 * @param objectKeyPrefix
	 *        the object key prefix to set
	 */
	public void setObjectKeyPrefix(String objectKeyPrefix) {
		this.objectKeyPrefix = objectKeyPrefix;
	}

	/**
	 * Set the path to a "work" directory for temporary files to be stored.
	 * 
	 * @param workDirectory
	 *        the work directory; defaults to {@link #DEFAULT_WORK_DIRECTORY}
	 */
	public void setWorkDirectory(String workDirectory) {
		this.workDirectory = workDirectory;
	}

	/**
	 * Set the path from which to extract setup resources.
	 * 
	 * @param destinationPath
	 *        the destination path; defaults to
	 *        {@link #defaultDestinationPath()}
	 */
	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}

	/**
	 * Set the command and arguments to use for extracting tar resources.
	 * 
	 * <p>
	 * The arguments support {@literal __SOURCE_FILE__} and
	 * {@literal __DEST_DIR__} placeholders that will be replaced by the input
	 * tar file path and the value of the {@code destinationPath} property.
	 * 
	 * @param tarCommand
	 */
	public void setTarCommand(List<String> tarCommand) {
		this.tarCommand = tarCommand;
	}

	/**
	 * Set a metadata service to publish setup status information to.
	 * 
	 * @param nodeMetadataService
	 *        the metadata service to use
	 */
	public void setNodeMetadataService(OptionalService<NodeMetadataService> nodeMetadataService) {
		this.nodeMetadataService = nodeMetadataService;
	}

	/**
	 * Set a task executor to handle background work in.
	 * 
	 * @param taskExecutor
	 *        a task executor
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Set the {@link SystemService} to use for restarting after updates are
	 * applied.
	 * 
	 * @param systemService
	 *        the system service
	 */
	public void setSystemService(OptionalService<SystemService> systemService) {
		this.systemService = systemService;
	}

	/**
	 * Set the {@link PlatformService} to use for executing setup tasks with.
	 * 
	 * @param platformService
	 *        the service to use
	 */
	public void setPlatformService(OptionalService<PlatformService> platformService) {
		this.platformService = platformService;
	}

	/**
	 * Set a {@link MessageSource} for resolving messages with.
	 * 
	 * @param messageSource
	 *        the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
