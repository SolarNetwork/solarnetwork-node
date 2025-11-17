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

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.MessageSource;
import org.springframework.util.MimeType;
import net.solarnetwork.common.s3.S3Client;
import net.solarnetwork.common.s3.S3ObjectMeta;
import net.solarnetwork.common.s3.S3ObjectMetadata;
import net.solarnetwork.common.s3.S3ObjectRef;
import net.solarnetwork.common.s3.S3ObjectReference;
import net.solarnetwork.common.s3.sdk.SdkS3Client;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupFilter;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceIterable;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.backup.BackupServiceInfo;
import net.solarnetwork.node.backup.BackupServiceSupport;
import net.solarnetwork.node.backup.BackupStatus;
import net.solarnetwork.node.backup.CollectionBackupResourceIterable;
import net.solarnetwork.node.backup.SimpleBackup;
import net.solarnetwork.node.backup.SimpleBackupServiceInfo;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicSliderSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.CachedResult;

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
 * @version 2.3
 */
public class S3BackupService extends BackupServiceSupport
		implements SettingSpecifierProvider, SettingsChangeObserver {

	private static final String CONTENT_SHA256_KEY = "Content-SHA256";

	/** The value returned by {@link #getKey()}. */
	public static final String SERVICE_KEY = S3BackupService.class.getName();

	/** The default value for the {@code regionName} property. */
	public static final String DEFAULT_REGION_NAME = "us-west-2";

	/** The default value for the {@code objectKeyPrefix} property. */
	public static final String DEFAULT_OBJECT_KEY_PREFIX = "solarnode-backups/";

	/** The default value for the {@code additionalBackupCount} property. */
	public static final int DEFAULT_ADDITIONAL_BACKUP_COUNT = 10;

	/**
	 * The default value for the {@code storageClass} property.
	 *
	 * @since 1.2
	 */
	public static final String DEFAULT_STORAGE_CLASS = "STANDARD";

	/**
	 * The {@code cacheSeconds} property default value.
	 *
	 * @since 2.1
	 */
	public static final int DEFAULT_CACHE_SECONDS = 3600;

	private static final String META_NAME_FORMAT = "node-%2$d-backup-%1$tY%1$tm%1$tdT%1$tH%1$tM%1$tS";
	private static final String NODE_PREFIX_FORMAT = "node-%d-backup-";
	private static final String META_OBJECT_KEY_PREFIX = "backup-meta/";
	private static final String DATA_OBJECT_KEY_PREFIX = "backup-data/";

	private String objectKeyPrefix;
	private MessageSource messageSource;
	private OptionalService<IdentityService> identityService;
	private int cacheSeconds;
	private int additionalBackupCount;
	private String storageClass;

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
		setCacheSeconds(DEFAULT_CACHE_SECONDS);
		setAdditionalBackupCount(DEFAULT_ADDITIONAL_BACKUP_COUNT);
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		s3Client.configurationChanged(properties);
		setupClient();
		cachedBackupList.set(null);
		CACHED_BACKUPS.clear();
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
		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		return performBackupInternal(resources, now, null);
	}

	private S3ObjectMeta setupMetadata(BackupResource rsrc, MessageDigest digest, byte[] buf)
			throws IOException {
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
		Date modified = null;
		if ( rsrc.getModificationDate() > 0 ) {
			modified = new Date(rsrc.getModificationDate());
		} else {
			modified = new Date();
		}
		String sha256 = new String(Hex.encodeHex(digest.digest()));

		return new S3ObjectMeta(contentLength, modified, storageClass,
				S3ObjectMetadata.DEFAULT_CONTENT_TYPE, singletonMap(CONTENT_SHA256_KEY, sha256));
	}

	private void setupClient() {
		SdkS3Client c = s3Client;
		if ( c != null && c.isConfigured() ) {
			status.set(BackupStatus.Configured);
		}
	}

	private Backup performBackupInternal(final Iterable<BackupResource> resources, final Instant now,
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
		Backup listBackupItem = null;
		try {
			final Long nodeId = nodeId(props);
			final String metaName = String.format(META_NAME_FORMAT, now.atOffset(UTC), nodeId);
			final String metaObjectKey = objectKeyForPath(META_OBJECT_KEY_PREFIX + metaName);
			log.info("Starting backup to archive {}", metaObjectKey);

			final Set<S3ObjectReference> allDataObjects = client
					.listObjects(objectKeyForPath(DATA_OBJECT_KEY_PREFIX));

			S3BackupMetadata meta = new S3BackupMetadata();
			meta.setNodeId(nodeId);
			MessageDigest digest = DigestUtils.getSha256Digest();
			byte[] buf = new byte[4096];
			for ( BackupResource rsrc : resources ) {
				S3ObjectMeta objMeta = setupMetadata(rsrc, digest, buf);
				String sha = (String) objMeta.getExtendedMetadata().get(CONTENT_SHA256_KEY);
				String objectKey = objectKeyForPath(DATA_OBJECT_KEY_PREFIX + sha);

				// see if already exists
				if ( !allDataObjects.contains(new S3ObjectRef(objectKey)) ) {
					log.info("Saving resource to S3: {}", rsrc.getBackupPath());
					client.putObject(objectKey, rsrc.getInputStream(),
							new S3ObjectMeta(objMeta.getSize(), objMeta.getModified()), null, null);
				} else {
					log.info("Backup resource already saved to S3: {}", rsrc.getBackupPath());
				}
				meta.addBackupResource(rsrc, objectKey, sha);
			}

			// now save metadata
			meta.setComplete(true);
			meta.setDate(Date.from(now));
			meta.setKey(metaName);
			byte[] metaJsonBytes = OBJECT_MAPPER.writeValueAsBytes(meta);
			try (ByteArrayInputStream in = new ByteArrayInputStream(metaJsonBytes)) {
				S3ObjectReference metaRef = client.putObject(metaObjectKey, in,
						new S3ObjectMeta(metaJsonBytes.length, meta.getDate(),
								MimeType.valueOf("application/json;charset=UTF-8")),
						null, null);
				result = new S3BackupMetadata(metaRef);
				listBackupItem = backupListItem(objectKeyForPath(META_OBJECT_KEY_PREFIX)).apply(metaRef);
			}

			// add this backup to the cached data
			CachedResult<List<Backup>> cached = cachedBackupList.get();
			if ( cached != null && listBackupItem != null ) {
				List<Backup> list = cached.getResult();
				List<Backup> newList = new ArrayList<>(list);
				newList.add(listBackupItem);
				updateCachedBackupList(newList);
			}
			if ( additionalBackupCount > 0 ) {
				// clean out older backups
				List<Backup> knownBackups = getAvailableBackupsInternal();
				List<String> backupsForNode = knownBackups.stream()
						.filter(b -> nodeId.equals(b.getNodeId())).map(b -> b.getKey())
						.collect(Collectors.toList());
				if ( backupsForNode.size() > additionalBackupCount + 1 ) {
					Set<String> keysToDelete = backupsForNode.stream()
							.limit(backupsForNode.size() - additionalBackupCount - 1)
							.map(k -> canonicalObjectKeyForBackupKey(k)).collect(Collectors.toSet());
					log.info("Deleting {} expired backups for node {}: {}", keysToDelete.size(), nodeId,
							keysToDelete);
					Set<String> keysDeleted = client.deleteObjects(keysToDelete);
					if ( !keysToDelete.equals(keysDeleted) ) {
						log.warn("Expected to delete expired backups {} but actually deleted {}",
								keysToDelete, keysDeleted);
					}

					// update cache
					knownBackups = knownBackups.stream().filter(b -> !keysDeleted.contains(b.getKey()))
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
			boolean updated = cachedBackupList.compareAndSet(cached, new CachedResult<List<Backup>>(
					newList, cached.getExpires() - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
			log.debug("Cached backup list {} updated", updated ? "was" : "was not");
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
		} catch ( RemoteServiceException | IOException e ) {
			log.warn("Error listing S3 objects with prefix {}: {}", backupKey, e.getMessage());
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

	@Override
	public FilterResults<Backup, String> findBackups(BackupFilter filter) {
		if ( EnumSet.of(BackupStatus.Unconfigured, BackupStatus.Error).contains(status.get()) ) {
			return new BasicFilterResults<>(List.of());
		}
		if ( filter == null || !filter.hasNodeCriteria() ) {
			return super.findBackups(filter);
		}
		final List<Backup> allMatching = getBackupsForPrefix(
				NODE_PREFIX_FORMAT.formatted(filter.getNodeId()));
		if ( filter.getOffset() != null || filter.getMax() != null ) {
			int from = filter.getOffset() != null ? filter.getOffset().intValue() : 0;
			int to = filter.getMax() != null ? Math.min(from + filter.getMax(), allMatching.size())
					: allMatching.size();
			List<Backup> filtered = List.of();
			if ( from < allMatching.size() ) {
				filtered = allMatching.subList(from, to);
			}
			return new BasicFilterResults<>(filtered, (long) allMatching.size(), from, filtered.size());
		}
		return new BasicFilterResults<>(allMatching);
	}

	private List<Backup> getAvailableBackupsInternal() {
		CachedResult<List<Backup>> cached = cachedBackupList.get();
		if ( cached != null && cached.isValid() ) {
			return cached.getResult();
		}
		List<Backup> result = getBackupsForPrefix(null);
		if ( result != null && !result.isEmpty() ) {
			cachedBackupList.compareAndSet(cached,
					new CachedResult<>(result, cacheSeconds, TimeUnit.SECONDS));
		}
		return result;
	}

	private List<Backup> getBackupsForPrefix(String prefix) {
		if ( EnumSet.of(BackupStatus.Unconfigured, BackupStatus.Error).contains(status.get()) ) {
			return Collections.emptyList();
		}
		final S3Client client = this.s3Client;
		final String objectKeyPrefix = objectKeyForPath(META_OBJECT_KEY_PREFIX);
		final String searchPrefix = (prefix != null ? objectKeyPrefix + prefix : objectKeyPrefix);
		try {
			Set<S3ObjectReference> objs = client.listObjects(searchPrefix);
			List<Backup> result = objs.stream()
					.filter(o -> NODE_AND_DATE_BACKUP_KEY_PATTERN.matcher(o.getKey()).find())
					.map(backupListItem(objectKeyPrefix)).collect(Collectors.toList());
			return result;
		} catch ( RemoteServiceException | IOException e ) {
			log.warn("Error listing S3 avaialble backups with prefix {}: {}", objectKeyPrefix,
					e.getMessage());
			return Collections.emptyList();
		}
	}

	private Function<S3ObjectReference, Backup> backupListItem(String objectKeyPrefix) {
		return o -> new SimpleBackup(
				identityFromBackupKey(pathWithoutPrefix(o.getKey(), objectKeyPrefix)), null, true);
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
		final Instant ts = backupDate.toInstant().truncatedTo(ChronoUnit.SECONDS);
		return performBackupInternal(resources, ts, props);
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
	public String getSettingUid() {
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
		result.add(new BasicTextFieldSettingSpecifier("storageClass", DEFAULT_STORAGE_CLASS));
		result.add(new BasicSliderSettingSpecifier("additionalBackupCount",
				(double) DEFAULT_ADDITIONAL_BACKUP_COUNT, 0.0, 20.0, 1.0));
		result.add(new BasicTextFieldSettingSpecifier("cacheSeconds",
				String.valueOf(DEFAULT_CACHE_SECONDS)));
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
		s3Client.setAccessToken(accessToken);
	}

	/**
	 * Set the AWS access token secret to use.
	 *
	 * @param accessSecret
	 *        the access secret to set
	 */
	public void setAccessSecret(String accessSecret) {
		s3Client.setAccessSecret(accessSecret);
	}

	/**
	 * Set the AWS region to connect to.
	 *
	 * @param regionName
	 *        the region name to set
	 */
	public void setRegionName(String regionName) {
		s3Client.setRegionName(regionName);
	}

	/**
	 * Set the S3 bucket name to use.
	 *
	 * @param bucketName
	 *        the name to set
	 */
	public void setBucketName(String bucketName) {
		s3Client.setBucketName(bucketName);
	}

	/**
	 * Get the S3 object key prefix to use.
	 *
	 * @return the prefix; defaults to {@link #DEFAULT_OBJECT_KEY_PREFIX}
	 * @since 1.3
	 */
	public String getObjectKeyPrefix() {
		return objectKeyPrefix;
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
	 * Get the S3 client.
	 *
	 * @return the S3 client
	 * @since 1.3
	 */
	public S3Client getS3Client() {
		return s3Client;
	}

	/**
	 * Set the S3 client.
	 *
	 * @param s3Client
	 *        the client
	 */
	public void setS3Client(SdkS3Client s3Client) {
		this.s3Client = s3Client;
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

	/**
	 * Get the S3 storage class to use.
	 *
	 * @return the S3 storage class; defaults to {@link #DEFAULT_STORAGE_CLASS}
	 * @since 1.2
	 */
	public String getStorageClass() {
		return storageClass;
	}

	/**
	 * Set the S3 storage class to use.
	 *
	 * @param storageClass
	 *        the S3 storage class to set
	 * @since 1.2
	 */
	public void setStorageClass(String storageClass) {
		this.storageClass = storageClass;
	}

}
