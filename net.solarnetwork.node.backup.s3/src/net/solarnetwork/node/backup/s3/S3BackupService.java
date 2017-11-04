/* ==================================================================
 * S3BackupService.java - 3/10/2017 12:03:47 PM
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

package net.solarnetwork.node.backup.s3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.MessageSource;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.ObjectMetadata;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.RemoteServiceException;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceIterable;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.backup.BackupServiceInfo;
import net.solarnetwork.node.backup.BackupServiceSupport;
import net.solarnetwork.node.backup.BackupStatus;
import net.solarnetwork.node.backup.CollectionBackupResourceIterable;
import net.solarnetwork.node.backup.SimpleBackup;
import net.solarnetwork.node.backup.SimpleBackupServiceInfo;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.OptionalService;

/**
 * {@link BackupService} implementation using Amazon S3 for storage in the
 * cloud.
 * 
 * <p>
 * <b>Note</b> that backups are stored using a <b>shared</b> object structure in
 * S3, where individual backup resources are named after the SHA256 digest of
 * their <i>content</i> with a {@code backup-data/} prefix added. Metadata about
 * each backup (including a listing of the resources included in the backup) is
 * then stored as an object named with the node ID and backup timestamp with a
 * `{@code backup-meta/} prefix added.
 * </p>
 * 
 * <p>
 * This object sharing system means that backup resources are only stored once
 * in S3, across any number of backups. It also means that multiple nodes can
 * backup to the same S3 location and the same resources present on different
 * nodes will only be stored once as well. Keep in mind the security
 * implications of this approach when using multiple nodes: all nodes
 * technically have access to all backups.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class S3BackupService extends BackupServiceSupport implements SettingSpecifierProvider {

	/** The value returned by {@link #getKey()}. */
	public static final String SERVICE_KEY = S3BackupService.class.getName();

	/** The default value for the {@code regionName} property. */
	public static final String DEFAULT_REGION_NAME = Regions.US_WEST_2.getName();

	/** The default value for the {@code objectKeyPrefix} property. */
	public static final String DEFAULT_OBJECT_KEY_PREFIX = "solarnode-backups/";

	/** The default value for the {@code additionalBackupCount} property. */
	public static final int DEFAULT_ADDITIONAL_BACKUP_COUNT = 10;

	private static final String META_NAME_FORMAT = "node-%2$d-backup-%1$tY%1$tm%1$tdT%1$tH%1$tM%1$tS";
	private static final String META_OBJECT_KEY_PREFIX = "backup-meta/";
	private static final String DATA_OBJECT_KEY_PREFIX = "backup-data/";

	private String accessToken;
	private String accessSecret;
	private String regionName;
	private String bucketName;
	private String objectKeyPrefix;
	private MessageSource messageSource;
	private OptionalService<IdentityService> identityService;
	private int cacheSeconds;
	private int additionalBackupCount;

	private SdkS3Client s3Client = new SdkS3Client();

	private final AtomicReference<BackupStatus> status = new AtomicReference<>(
			BackupStatus.Unconfigured);

	private final AtomicReference<S3BackupMetadata> inProgressBackup = new AtomicReference<>();
	private final AtomicReference<CachedResult<List<Backup>>> cachedBackupList = new AtomicReference<>();

	private static final ConcurrentMap<String, CachedResult<S3BackupMetadata>> CACHED_BACKUPS = new ConcurrentHashMap<>(
			8);

	/**
	 * Default constructor.
	 */
	public S3BackupService() {
		super();
		setRegionName(DEFAULT_REGION_NAME);
		setObjectKeyPrefix(DEFAULT_OBJECT_KEY_PREFIX);
		setCacheSeconds((int) TimeUnit.HOURS.toSeconds(1));
		setAdditionalBackupCount(DEFAULT_ADDITIONAL_BACKUP_COUNT);
	}

	@Override
	public String getKey() {
		return SERVICE_KEY;
	}

	@Override
	public BackupServiceInfo getInfo() {
		Collection<Backup> availableBackups = getAvailableBackups();
		Date mostRecentDate = null;
		if ( availableBackups != null && !availableBackups.isEmpty() ) {
			mostRecentDate = availableBackups.iterator().next().getDate();
		}
		return new SimpleBackupServiceInfo(mostRecentDate, status.get());
	}

	@Override
	public Backup performBackup(Iterable<BackupResource> resources) {
		final Calendar now = new GregorianCalendar();
		now.set(Calendar.MILLISECOND, 0);
		return performBackupInternal(resources, now, null);
	}

	private String calculateContentDigest(BackupResource rsrc, MessageDigest digest, byte[] buf,
			ObjectMetadata objectMetadata) throws IOException {
		// S3 client buffers to RAM unless content length set; so since we have to calculate the 
		// SHA256 digest of the content anyway, also calculate the content length at the same time
		long contentLength = 0;
		digest.reset();
		int len = 0;
		try (InputStream rsrcIn = rsrc.getInputStream()) {
			while ( (len = rsrcIn.read(buf)) >= 0 ) {
				digest.update(buf, 0, len);
				contentLength += len;
			}
		}
		objectMetadata.setContentLength(contentLength);
		return new String(Hex.encodeHex(digest.digest()));
	}

	private void setupClient() {
		SdkS3Client c = s3Client;
		if ( c != null ) {
			c.setBucketName(bucketName);
			c.setRegionName(regionName);
			if ( accessToken != null && accessToken.length() > 0 && accessSecret != null
					&& accessSecret.length() > 0 ) {
				c.setCredentialsProvider(new AWSStaticCredentialsProvider(
						new BasicAWSCredentials(accessToken, accessSecret)));
			}
			if ( c.isConfigured() ) {
				status.set(BackupStatus.Configured);
			}
		}
	}

	private Backup performBackupInternal(final Iterable<BackupResource> resources, final Calendar now,
			Map<String, String> props) {
		if ( resources == null ) {
			return null;
		}
		final Iterator<BackupResource> itr = resources.iterator();
		if ( !itr.hasNext() ) {
			log.debug("No resources provided, nothing to backup");
			return null;
		}
		S3Client client = this.s3Client;
		if ( !status.compareAndSet(BackupStatus.Configured, BackupStatus.RunningBackup) ) {
			// try to reset from error
			if ( !status.compareAndSet(BackupStatus.Error, BackupStatus.RunningBackup) ) {
				return null;
			}
		}
		S3BackupMetadata result = null;
		try {
			final Long nodeId = nodeId(props);
			final String metaName = String.format(META_NAME_FORMAT, now, nodeId);
			final String metaObjectKey = objectKeyForPath(META_OBJECT_KEY_PREFIX + metaName);
			log.info("Starting backup to archive {}", metaObjectKey);

			final Set<S3ObjectReference> allDataObjects = client
					.listObjects(objectKeyForPath(DATA_OBJECT_KEY_PREFIX));

			S3BackupMetadata meta = new S3BackupMetadata();
			meta.setNodeId(nodeId);
			MessageDigest digest = DigestUtils.getSha256Digest();
			byte[] buf = new byte[4096];
			for ( BackupResource rsrc : resources ) {
				ObjectMetadata objectMetadata = new ObjectMetadata();
				if ( rsrc.getModificationDate() > 0 ) {
					objectMetadata.setLastModified(new Date(rsrc.getModificationDate()));
				}
				String sha = calculateContentDigest(rsrc, digest, buf, objectMetadata);
				String objectKey = objectKeyForPath(DATA_OBJECT_KEY_PREFIX + sha);

				// see if already exists
				if ( !allDataObjects.contains(new S3ObjectReference(objectKey)) ) {
					log.info("Saving resource to S3: {}", rsrc.getBackupPath());
					client.putObject(objectKey, rsrc.getInputStream(), objectMetadata);
				} else {
					log.info("Backup resource already saved to S3: {}", rsrc.getBackupPath());
				}
				meta.addBackupResource(rsrc, objectKey, sha);
			}

			// now save metadata
			meta.setComplete(true);
			meta.setDate(now.getTime());
			meta.setKey(metaName);
			byte[] metaJsonBytes = OBJECT_MAPPER.writeValueAsBytes(meta);
			try (ByteArrayInputStream in = new ByteArrayInputStream(metaJsonBytes)) {
				ObjectMetadata metaObjectMetadata = new ObjectMetadata();
				metaObjectMetadata.setContentType("application/json;charset=UTF-8");
				metaObjectMetadata.setContentLength(metaJsonBytes.length);
				metaObjectMetadata.setLastModified(meta.getDate());
				S3ObjectReference metaRef = client.putObject(metaObjectKey, in, metaObjectMetadata);
				result = new S3BackupMetadata(metaRef);
			}

			if ( additionalBackupCount < 1 ) {
				// add this backup to the cached data
				CachedResult<List<Backup>> cached = cachedBackupList.get();
				if ( cached != null ) {
					List<Backup> list = cached.getResult();
					List<Backup> newList = new ArrayList<>(list);
					newList.add(0, result);
					updateCachedBackupList(newList);
				}
			} else {
				// clean out older backups
				List<Backup> knownBackups = getAvailableBackupsInternal();
				List<String> backupsForNode = knownBackups.stream()
						.filter(b -> nodeId.equals(b.getNodeId())).map(b -> b.getKey())
						.collect(Collectors.toList());
				if ( backupsForNode.size() > additionalBackupCount + 1 ) {
					Set<String> keysToDelete = backupsForNode.stream()
							.limit(backupsForNode.size() - additionalBackupCount - 1)
							.collect(Collectors.toSet());
					log.info("Deleting {} expired backups for node {}: {}", keysToDelete.size(), nodeId,
							keysToDelete);
					client.deleteObjects(keysToDelete);

					// update cache
					knownBackups = knownBackups.stream().filter(b -> !keysToDelete.contains(b.getKey()))
							.collect(Collectors.toList());
					updateCachedBackupList(knownBackups);
				}
			}
		} catch ( IOException e ) {
			log.error("IO error performing backup", e);
		} finally {
			status.compareAndSet(BackupStatus.RunningBackup, BackupStatus.Configured);
		}
		return result;
	}

	private void updateCachedBackupList(List<Backup> newList) {
		CachedResult<List<Backup>> cached = cachedBackupList.get();
		if ( cached != null ) {
			cachedBackupList.compareAndSet(cached, new CachedResult<List<Backup>>(newList,
					cached.getExpires() - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
		}
	}

	private Long nodeId(Map<String, String> props) {
		Long nodeId = backupNodeIdFromProps(null, props);
		if ( nodeId == 0L ) {
			nodeId = nodeId();
		}
		return nodeId;
	}

	private Long nodeId() {
		IdentityService service = (identityService != null ? identityService.service() : null);
		final Long nodeId = (service != null ? service.getNodeId() : null);
		return (nodeId != null ? nodeId : 0L);
	}

	@Override
	public Backup backupForKey(String key) {
		return backupForKeyInternal(key);
	}

	private S3BackupMetadata backupForKeyInternal(String key) {
		String backupKey = canonicalObjectKeyForBackupKey(key);
		CachedResult<S3BackupMetadata> cachedBackup = CACHED_BACKUPS.get(backupKey);
		if ( cachedBackup != null && cachedBackup.isValid() ) {
			return cachedBackup.getResult();
		}
		S3BackupMetadata inProgress = inProgressBackup.get();
		if ( inProgress != null && backupKey.equals(inProgress.getKey()) ) {
			return inProgress;
		}
		S3Client client = this.s3Client;
		if ( EnumSet.of(BackupStatus.Unconfigured, BackupStatus.Error).contains(status.get()) ) {
			return null;
		}
		S3BackupMetadata backup = null;
		try {
			Set<S3ObjectReference> objs = client.listObjects(backupKey);
			if ( !objs.isEmpty() ) {
				S3ObjectReference backupMetaObj = objs.iterator().next();
				backup = new S3BackupMetadata(backupMetaObj);
				CachedResult<S3BackupMetadata> newCachedBackup = new CachedResult<>(backup, cacheSeconds,
						TimeUnit.SECONDS);
				if ( cachedBackup == null ) {
					CACHED_BACKUPS.putIfAbsent(backupKey, newCachedBackup);
				} else {
					CACHED_BACKUPS.replace(backupKey, cachedBackup, newCachedBackup);
				}
			}
		} catch ( RemoteServiceException e ) {
			log.warn("Error accessing S3: {}", e.getMessage());
		}
		return backup;
	}

	private String objectKeyForPath(String path) {
		String globalPrefix = this.objectKeyPrefix;
		if ( globalPrefix == null ) {
			return path;
		}
		return globalPrefix + path;
	}

	private String pathWithoutPrefix(String objectKey, String prefix) {
		if ( objectKey.startsWith(prefix) ) {
			return objectKey.substring(prefix.length());
		}
		return objectKey;
	}

	@Override
	public Collection<Backup> getAvailableBackups() {
		return getAvailableBackupsInternal();
	}

	private List<Backup> getAvailableBackupsInternal() {
		CachedResult<List<Backup>> cached = cachedBackupList.get();
		if ( cached != null && cached.isValid() ) {
			return cached.getResult();
		}
		S3Client client = this.s3Client;
		if ( EnumSet.of(BackupStatus.Unconfigured, BackupStatus.Error).contains(status.get()) ) {
			return Collections.emptyList();
		}
		final String objectKeyPrefix = objectKeyForPath(META_OBJECT_KEY_PREFIX);
		try {
			Set<S3ObjectReference> objs = client.listObjects(objectKeyPrefix);
			List<Backup> result = objs.stream()
					.map(o -> new SimpleBackup(
							identityFromBackupKey(pathWithoutPrefix(o.getKey(), objectKeyPrefix)), null,
							true))
					.collect(Collectors.toList());
			cachedBackupList.compareAndSet(cached,
					new CachedResult<List<Backup>>(result, cacheSeconds, TimeUnit.SECONDS));
			return result;
		} catch ( RemoteServiceException e ) {
			log.warn("Error accessing S3: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Get the canonical key for a backup.
	 * 
	 * <p>
	 * This method may be called even if {@code key} is already in canonical
	 * form.
	 * </p>
	 * 
	 * @param key
	 *        the backup key to get in canonical form
	 * @return the key, in canonical form
	 */
	private String canonicalObjectKeyForBackupKey(String key) {
		String prefix = objectKeyForPath(META_OBJECT_KEY_PREFIX);
		if ( key.startsWith(prefix) ) {
			return key;
		}
		return prefix + key;
	}

	private String objectKeyForBackup(Backup backup) {
		// support both relative and full paths
		return canonicalObjectKeyForBackupKey(backup.getKey());
	}

	@Override
	public BackupResourceIterable getBackupResources(Backup backup) {
		S3Client client = this.s3Client;
		if ( EnumSet.of(BackupStatus.Unconfigured, BackupStatus.Error).contains(status.get()) ) {
			return new CollectionBackupResourceIterable(Collections.emptyList());
		}

		// try to return a cached value if we can
		String backupKey = canonicalObjectKeyForBackupKey(backup.getKey());
		S3BackupMetadata meta = backupForKeyInternal(backupKey);
		List<S3BackupResourceMetadata> resourceMetaList = meta.getResourceMetadata();
		if ( resourceMetaList == null ) {
			try {
				String metaKey = objectKeyForBackup(backup);
				String metaJson = client.getObjectAsString(metaKey);
				S3BackupMetadata remoteMeta = OBJECT_MAPPER.readValue(metaJson, S3BackupMetadata.class);
				resourceMetaList = remoteMeta.getResourceMetadata();

				CachedResult<S3BackupMetadata> cachedResult = new CachedResult<S3BackupMetadata>(
						remoteMeta, cacheSeconds, TimeUnit.SECONDS);
				CACHED_BACKUPS.put(backupKey, cachedResult);

			} catch ( IOException e ) {
				log.warn("Communciation error accessing S3: {}", e.getMessage());
			} catch ( RemoteServiceException e ) {
				log.warn("Error accessing S3: {}", e.getMessage());
			}
		}
		if ( resourceMetaList == null || resourceMetaList.isEmpty() ) {
			return new CollectionBackupResourceIterable(Collections.emptyList());
		}
		List<BackupResource> resources = resourceMetaList.stream()
				.map(m -> new S3BackupResource(client, m)).collect(Collectors.toList());
		return new CollectionBackupResourceIterable(resources);
	}

	@Override
	public Backup importBackup(Date date, BackupResourceIterable resources, Map<String, String> props) {
		final Date backupDate = backupDateFromProps(date, props);
		final Calendar cal = new GregorianCalendar();
		cal.setTime(backupDate);
		cal.set(Calendar.MILLISECOND, 0);
		return performBackupInternal(resources, cal, props);
	}

	@Override
	public SettingSpecifierProvider getSettingSpecifierProvider() {
		return this;
	}

	@Override
	public SettingSpecifierProvider getSettingSpecifierProviderForRestore() {
		return this;
	}

	@Override
	public String getSettingUID() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return "S3 Backup Service";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);
		result.add(new BasicTextFieldSettingSpecifier("accessToken", ""));
		result.add(new BasicTextFieldSettingSpecifier("accessSecret", "", true));
		result.add(new BasicTextFieldSettingSpecifier("regionName", DEFAULT_REGION_NAME));
		result.add(new BasicTextFieldSettingSpecifier("bucketName", ""));
		result.add(new BasicTextFieldSettingSpecifier("objectKeyPrefix", DEFAULT_OBJECT_KEY_PREFIX));
		return result;
	}

	/**
	 * Set the {@link MessageSource} to use with settings.
	 * 
	 * @param messageSource
	 *        the {@code MessageSource} to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Set the AWS access token to use.
	 * 
	 * @param accessToken
	 *        the access token to set
	 */
	public void setAccessToken(String accessToken) {
		if ( accessToken != null && !accessToken.equals(this.accessToken) ) {
			this.accessToken = accessToken;
			setupClient();
		}
	}

	/**
	 * Set the AWS access token secret to use.
	 * 
	 * @param accessSecret
	 *        the access secret to set
	 */
	public void setAccessSecret(String accessSecret) {
		if ( accessSecret != null && !accessSecret.equals(this.accessSecret) ) {
			this.accessSecret = accessSecret;
			setupClient();
		}
	}

	/**
	 * Set the AWS region to connect to.
	 * 
	 * @param regionName
	 *        the region name to set
	 */
	public void setRegionName(String regionName) {
		if ( regionName != null && !regionName.equals(this.regionName) ) {
			this.regionName = regionName;
			setupClient();
		}
	}

	/**
	 * Set the S3 bucket name to use.
	 * 
	 * @param bucketName
	 *        the name to set
	 */
	public void setBucketName(String bucketName) {
		if ( bucketName != null && !bucketName.equals(this.bucketName) ) {
			this.bucketName = bucketName;
			setupClient();
		}
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
	 * Set the {@link IdentityService} to use.
	 * 
	 * @param identityService
	 *        the service to set
	 */
	public void setIdentityService(OptionalService<IdentityService> identityService) {
		this.identityService = identityService;
	}

	/**
	 * Set the number of seconds to cache S3 results.
	 * 
	 * @param cacheSeconds
	 *        the seconds to set
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheSeconds = cacheSeconds;
	}

	/**
	 * Set the S3 client.
	 * 
	 * @param s3Client
	 *        the client
	 */
	public void setS3Client(SdkS3Client s3Client) {
		this.s3Client = s3Client;
		setupClient();
	}

	/**
	 * Set the number of additional backups to maintain.
	 * 
	 * <p>
	 * If greater than zero, then this service will maintain this many copies of
	 * past backups <em>for the same node ID</em> in addition to the most recent
	 * one. Backups will be purged by their creation date, so that any backups
	 * other the most recent plus the next {@code additionalBackupCount} older
	 * backups are deleted.
	 * </p>
	 * 
	 * @param additionalBackupCount
	 *        the count; defaults to 10
	 */
	public void setAdditionalBackupCount(int additionalBackupCount) {
		this.additionalBackupCount = additionalBackupCount;
	}
}
