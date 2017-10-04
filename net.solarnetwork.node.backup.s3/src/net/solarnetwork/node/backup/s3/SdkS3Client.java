/* ==================================================================
 * SdkS3Client.java - 3/10/2017 2:11:41 PM
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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import net.solarnetwork.node.RemoteServiceException;

/**
 * {@link S3Client} using the AWS SDK.
 * 
 * @author matt
 * @version 1.0
 */
public class SdkS3Client implements S3Client {

	private String bucketName;
	private String regionName = Regions.US_WEST_2.getName();
	private int maximumKeysPerRequest = 500;
	private AWSCredentialsProvider credentialsProvider;

	private AmazonS3 s3Client;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private synchronized AmazonS3 getClient() {
		AmazonS3 result = s3Client;
		if ( result == null ) {
			result = AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider)
					.withRegion(regionName).build();
			s3Client = result;
		}
		return result;
	}

	@Override
	public Set<S3ObjectReference> listObjects(String prefix) {
		AmazonS3 client = getClient();
		Set<S3ObjectReference> result = new LinkedHashSet<>(100);
		try {
			final ListObjectsV2Request req = new ListObjectsV2Request();
			req.setBucketName(bucketName);
			req.setMaxKeys(maximumKeysPerRequest);
			req.setPrefix(prefix);
			ListObjectsV2Result listResult;
			do {
				listResult = client.listObjectsV2(req);

				for ( S3ObjectSummary objectSummary : listResult.getObjectSummaries() ) {
					result.add(new S3ObjectReference(objectSummary.getKey(), objectSummary.getSize(),
							objectSummary.getLastModified()));
				}
				req.setContinuationToken(listResult.getNextContinuationToken());
			} while ( listResult.isTruncated() == true );

		} catch ( AmazonServiceException e ) {
			log.warn("AWS error: {}; HTTP code {}; AWS code {}; type {}; request ID {}", e.getMessage(),
					e.getStatusCode(), e.getErrorCode(), e.getErrorType(), e.getRequestId());
			throw new RemoteServiceException("Error listing S3 objects at " + prefix, e);
		} catch ( AmazonClientException e ) {
			log.debug("Error communicating with AWS: {}", e.getMessage());
			throw new RemoteServiceException("Error communicating with AWS", e);
		}
		return result;
	}

	@Override
	public String getObjectAsString(String key) {
		AmazonS3 client = getClient();
		try {
			return client.getObjectAsString(bucketName, key);
		} catch ( AmazonServiceException e ) {
			log.warn("AWS error: {}; HTTP code {}; AWS code {}; type {}; request ID {}", e.getMessage(),
					e.getStatusCode(), e.getErrorCode(), e.getErrorType(), e.getRequestId());
			throw new RemoteServiceException("Error getting S3 object at " + key, e);
		} catch ( AmazonClientException e ) {
			log.debug("Error communicating with AWS: {}", e.getMessage());
			throw new RemoteServiceException("Error communicating with AWS", e);
		}
	}

	@Override
	public S3Object getObject(String key) {
		AmazonS3 client = getClient();
		try {
			return client.getObject(bucketName, key);
		} catch ( AmazonServiceException e ) {
			log.warn("AWS error: {}; HTTP code {}; AWS code {}; type {}; request ID {}", e.getMessage(),
					e.getStatusCode(), e.getErrorCode(), e.getErrorType(), e.getRequestId());
			throw new RemoteServiceException("Error getting S3 object at " + key, e);
		} catch ( AmazonClientException e ) {
			log.debug("Error communicating with AWS: {}", e.getMessage());
			throw new RemoteServiceException("Error communicating with AWS", e);
		}
	}

	@Override
	public S3ObjectReference putObject(String key, InputStream in, ObjectMetadata objectMetadata)
			throws IOException {
		AmazonS3 client = getClient();
		try {
			PutObjectRequest req = new PutObjectRequest(bucketName, key, in, objectMetadata);
			client.putObject(req);
			return new S3ObjectReference(key, objectMetadata.getContentLength(),
					objectMetadata.getLastModified());
		} catch ( AmazonServiceException e ) {
			log.warn("AWS error: {}; HTTP code {}; AWS code {}; type {}; request ID {}", e.getMessage(),
					e.getStatusCode(), e.getErrorCode(), e.getErrorType(), e.getRequestId());
			throw new RemoteServiceException("Error putting S3 object at " + key, e);
		} catch ( AmazonClientException e ) {
			log.debug("Error communicating with AWS: {}", e.getMessage());
			throw new RemoteServiceException("Error communicating with AWS", e);
		}
	}

	/**
	 * Set the bucket name to connect to.
	 * 
	 * @param bucketName
	 *        the bucketName to set
	 */
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	/**
	 * Set the AWS region to use.
	 * 
	 * @param regionName
	 *        the region name to set; defaults to us-west-2
	 */
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	/**
	 * Set the maximum number of S3 object keys to request in one request.
	 * 
	 * @param maximumKeysPerRequest
	 *        the maximum to set
	 */
	public void setMaximumKeysPerRequest(int maximumKeysPerRequest) {
		this.maximumKeysPerRequest = maximumKeysPerRequest;
	}

	/**
	 * Set the credentials provider to authenticate with.
	 * 
	 * @param credentialsProvider
	 *        the provider to set
	 */
	public void setCredentialsProvider(AWSCredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

}
