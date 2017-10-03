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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.RemoteServiceException;
import net.solarnetwork.node.backup.Backup;
import net.solarnetwork.node.backup.BackupResource;
import net.solarnetwork.node.backup.BackupResourceIterable;
import net.solarnetwork.node.backup.BackupService;
import net.solarnetwork.node.backup.BackupServiceInfo;
import net.solarnetwork.node.backup.BackupStatus;
import net.solarnetwork.node.backup.SimpleBackup;
import net.solarnetwork.node.backup.SimpleBackupServiceInfo;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.OptionalService;

/**
 * {@link BackupService} implementation using Amazon S3 for storage in the
 * cloud.
 * 
 * @author matt
 * @version 1.0
 */
public class S3BackupService implements BackupService, SettingSpecifierProvider {

	/** The value returned by {@link #getKey()}. */
	public static final String SERVICE_KEY = S3BackupService.class.getName();

	/** The default value for the {@code regionName} property. */
	public static final String DEFAULT_REGION_NAME = Regions.US_WEST_2.getName();

	/** The default value for the {@code objectKeyPrefix} property. */
	public static final String DEFAULT_OBJECT_KEY_PREFIX = "solarnode-backups/";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	private static final String META_NAME_FORMAT = "node-%2$d-backup-%1$tY%1$tm%1$tdT%1$tH%1$tM%1$tS";
	private static final String META_OBJECT_KEY_PREFIX = "backup-meta/";
	private static final String DATA_OBJECT_KEY_PREFIX = "backup-data/";

	private String accessToken;
	private String accessSecret;
	private String regionName = DEFAULT_REGION_NAME;
	private String bucketName = "solarnetwork-dev-testing";
	private String objectKeyPrefix = DEFAULT_OBJECT_KEY_PREFIX;
	private MessageSource messageSource;
	private OptionalService<IdentityService> identityService;

	private S3Client s3Client = null;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicReference<BackupStatus> status = new AtomicReference<>(
			BackupStatus.Unconfigured);

	private final AtomicReference<S3BackupMetadata> inProgressBackup = new AtomicReference<>();

	@Override
	public String getKey() {
		return SERVICE_KEY;
	}

	@Override
	public BackupServiceInfo getInfo() {
		// TODO pass most recent date
		return new SimpleBackupServiceInfo(null, status.get());
	}

	@Override
	public Backup performBackup(Iterable<BackupResource> resources) {
		final Calendar now = new GregorianCalendar();
		now.set(Calendar.MILLISECOND, 0);
		return performBackupInternal(resources, now);
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

	private Backup performBackupInternal(final Iterable<BackupResource> resources, final Calendar now) {
		if ( resources == null ) {
			return null;
		}
		final Iterator<BackupResource> itr = resources.iterator();
		if ( !itr.hasNext() ) {
			log.debug("No resources provided, nothing to backup");
			return null;
		}
		S3Client client = getS3Client();
		if ( !status.compareAndSet(BackupStatus.Configured, BackupStatus.RunningBackup) ) {
			// try to reset from error
			if ( !status.compareAndSet(BackupStatus.Error, BackupStatus.RunningBackup) ) {
				return null;
			}
		}
		S3BackupMetadata result = null;
		try {
			final Long nodeId = nodeId();
			final String metaName = String.format(META_NAME_FORMAT, now, nodeId);
			final String metaObjectKey = objectKeyPrefix(META_OBJECT_KEY_PREFIX + metaName);
			log.info("Starting backup to archive {}", metaObjectKey);

			final Set<S3ObjectReference> allDataObjects = client
					.listObjects(objectKeyPrefix(DATA_OBJECT_KEY_PREFIX));

			S3BackupMetadata meta = new S3BackupMetadata();
			MessageDigest digest = DigestUtils.getSha256Digest();
			byte[] buf = new byte[4096];
			for ( BackupResource rsrc : resources ) {
				ObjectMetadata objectMetadata = new ObjectMetadata();
				if ( rsrc.getModificationDate() > 0 ) {
					objectMetadata.setLastModified(new Date(rsrc.getModificationDate()));
				}
				String sha = calculateContentDigest(rsrc, digest, buf, objectMetadata);
				String objectKey = objectKeyPrefix(DATA_OBJECT_KEY_PREFIX + sha);

				// see if already exists
				if ( !allDataObjects.contains(new S3ObjectReference(objectKey)) ) {
					log.info("Saving resource to S3: {}", rsrc.getBackupPath());
					client.putObject(objectKey, rsrc.getInputStream(), objectMetadata);
				} else {
					log.info("Backup resource already saved to S3: {}", rsrc.getBackupPath());
				}
				meta.addBackupResource(rsrc, objectKey);
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
		} catch ( IOException e ) {
			log.error("IO error performing backup", e);
		} finally {
			status.compareAndSet(BackupStatus.RunningBackup, BackupStatus.Configured);
		}
		return result;
	}

	private final Long nodeId() {
		IdentityService service = (identityService != null ? identityService.service() : null);
		final Long nodeId = (service != null ? service.getNodeId() : null);
		return (nodeId != null ? nodeId : 0L);
	}

	@Override
	public Backup backupForKey(String key) {
		S3BackupMetadata backup = inProgressBackup.get();
		if ( key.equals(backup.getKey()) ) {
			return backup;
		}
		S3Client client = getS3Client();
		if ( EnumSet.of(BackupStatus.Unconfigured, BackupStatus.Error).contains(status.get()) ) {
			return null;
		}
		try {
			Set<S3ObjectReference> objs = client
					.listObjects(objectKeyPrefix(META_OBJECT_KEY_PREFIX + key));
			if ( !objs.isEmpty() ) {
				S3ObjectReference backupMetaObj = objs.iterator().next();
				backup = new S3BackupMetadata(backupMetaObj);
			}
		} catch ( RemoteServiceException e ) {
			log.warn("Error accessing S3: {}", e.getMessage());
		}
		return backup;
	}

	private String objectKeyPrefix(String path) {
		String globalPrefix = this.objectKeyPrefix;
		if ( globalPrefix == null ) {
			return path;
		}
		return globalPrefix + path;
	}

	@Override
	public Collection<Backup> getAvailableBackups() {
		S3Client client = getS3Client();
		if ( EnumSet.of(BackupStatus.Unconfigured, BackupStatus.Error).contains(status.get()) ) {
			return Collections.emptyList();
		}
		try {
			Set<S3ObjectReference> objs = client.listObjects(objectKeyPrefix(META_OBJECT_KEY_PREFIX));
			return objs.stream().map(o -> new SimpleBackup(o.getModified(), o.getKey(), null, true))
					.collect(Collectors.toList());
		} catch ( RemoteServiceException e ) {
			log.warn("Error accessing S3: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	@Override
	public BackupResourceIterable getBackupResources(Backup backup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean markBackupForRestore(Backup backup, Map<String, String> props) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Backup markedBackupForRestore(Map<String, String> props) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Backup importBackup(Date date, BackupResourceIterable resources, Map<String, String> props) {
		// TODO Auto-generated method stub
		return null;
	}

	private synchronized S3Client getS3Client() {
		S3Client c = s3Client;
		if ( c == null && accessToken != null && accessSecret != null && bucketName != null ) {
			SdkS3Client sdkClient = new SdkS3Client();
			sdkClient.setBucketName(bucketName);
			sdkClient.setRegionName(regionName);
			sdkClient.setCredentialsProvider(new AWSStaticCredentialsProvider(
					new BasicAWSCredentials(accessToken, accessSecret)));
			c = sdkClient;
			s3Client = sdkClient;
			status.set(BackupStatus.Configured);
		}
		return c;
	}

	@Override
	public SettingSpecifierProvider getSettingSpecifierProvider() {
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
			this.s3Client = null;
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
			this.s3Client = null;
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
			this.s3Client = null;
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
			this.s3Client = null;
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

}
