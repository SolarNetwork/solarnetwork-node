/* ==================================================================
 * BulkJsonWebPostUploadService.java - Aug 25, 2014 10:40:24 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.bulkjsonwebpost;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.node.BulkUploadResult;
import net.solarnetwork.node.BulkUploadService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionAcknowledgementService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.support.JsonHttpClientSupport;
import net.solarnetwork.util.OptionalService;

/**
 * {@link BulkUploadService} that uses an HTTP POST with body content formed as
 * a JSON document containing all data to upload.
 * 
 * @author matt
 * @version 1.4
 */
public class BulkJsonWebPostUploadService extends JsonHttpClientSupport
		implements BulkUploadService, InstructionAcknowledgementService, SettingSpecifierProvider {

	private String url = "/bulkUpload.do";
	private OptionalService<ReactorService> reactorService;
	private boolean uploadEmptyDataset = false;
	private MessageSource messageSource;

	/**
	 * Default constructor.
	 * 
	 * <p>
	 * This sets the {@code compress} flag to {@literal true}.
	 * </p>
	 */
	public BulkJsonWebPostUploadService() {
		super();
		setCompress(true);
	}

	@Override
	public String getKey() {
		return "BulkJsonWebPostUploadService:" + getIdentityService().getSolarNetHostName();
	}

	@Override
	public List<BulkUploadResult> uploadBulkDatum(Collection<Datum> data) {
		if ( (data == null || data.size() < 1) && uploadEmptyDataset == false ) {
			return Collections.emptyList();
		}
		boolean success = false;
		try {
			success = upload(data);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		List<BulkUploadResult> results = new ArrayList<BulkUploadResult>(data.size());
		if ( success ) {
			for ( Datum datum : data ) {
				results.add(new BulkUploadResult(datum, DigestUtils.md5DigestAsHex(
						String.format("%tQ;%s", datum.getCreated(), datum.getSourceId()).getBytes())));
			}
		}
		return results;
	}

	@Override
	public void acknowledgeInstructions(Collection<Instruction> instructions) {
		try {
			upload(instructions);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Upload a collection of data objects, and parse the response into
	 * {@link UploadResult} objects.
	 * 
	 * <p>
	 * The response is expected to be structured like this:
	 * </p>
	 * 
	 * <pre>
	 * {
	 * 	"success" : true,
	 *  "message" : "some message",
	 * 	"data" : {
	 * 		"datum" : [
	 * 			{ "created": 123, sourceId: "abc" ... },
	 * 			...
	 * 		],
	 * 		"instructions" : [
	 * 
	 * 		]
	 * }
	 * </pre>
	 * 
	 * @param data
	 *        Datum or Instruction objects to upload
	 * @param instructions
	 *        {@literal true} if instructions are getting uploaded
	 * @return true if the data is uploaded successfully
	 * @throws IOException
	 *         if any processing error occurs
	 */
	private boolean upload(Collection<?> data) throws IOException {
		InputStream response = handlePost(data);
		boolean result = false;
		try {
			JsonNode root = getObjectMapper().readTree(response);
			if ( log.isDebugEnabled() ) {
				log.debug("Got JSON response: {}",
						getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(root));
			}
			if ( root.isObject() ) {
				JsonNode child = root.path("success");
				result = child.asBoolean();
				if ( result ) {
					child = root.path("data");
					if ( child.isObject() ) {
						// look for instructions to process
						JsonNode instrArray = child.path("instructions");
						ReactorService reactor = (reactorService == null ? null
								: reactorService.service());
						if ( reactor != null && instrArray.isArray() ) {
							List<InstructionStatus> status = reactor.processInstruction(
									getIdentityService().getSolarInBaseUrl(), instrArray, JSON_MIME_TYPE,
									null);
							log.debug("Instructions processed: {}", status);
						}
					} else {
						log.debug("Upload returned no data.");
					}
				} else {
					log.warn("Upload not successful: {}",
							root.get("message") == null ? "(no message)" : root.get("message").asText());
				}
			}
		} finally {
			if ( response != null ) {
				response.close();
			}
		}
		return result;
	}

	private InputStream handlePost(Collection<?> data) {
		final String postUrl = getIdentityService().getSolarInBaseUrl() + url;
		try {
			return doJson(postUrl, HTTP_METHOD_POST, data);
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException bulk posting data to " + postUrl, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	// Settings

	@Override
	public String getSettingUID() {
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		return "Bulk JSON Upload Service";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		BulkJsonWebPostUploadService defaults = new BulkJsonWebPostUploadService();
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.add(
				new BasicToggleSettingSpecifier("uploadEmptyDataset", defaults.isUploadEmptyDataset()));
		return result;
	}

	// Accessors

	public String getUrl() {
		return url;
	}

	/**
	 * The SolarIn relative URL path to post data to.
	 * 
	 * <p>
	 * Defaults to {@literal /bulkUpload.do}.
	 * </p>
	 * 
	 * @param url
	 *        the path
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	public OptionalService<ReactorService> getReactorService() {
		return reactorService;
	}

	/**
	 * Set the optional {@link ReactorService} to use for processing
	 * instructions.
	 * 
	 * @param reactorService
	 *        the service to use
	 */
	public void setReactorService(OptionalService<ReactorService> reactorService) {
		this.reactorService = reactorService;
	}

	public boolean isUploadEmptyDataset() {
		return uploadEmptyDataset;
	}

	/**
	 * Flag to make HTTP POST requests even if there isn't any datum data to
	 * upload. This can be useful in situations where we want to be able to
	 * receive instructions in the HTTP response even if the node has not
	 * produced any data to upload.
	 * 
	 * @param uploadEmptyDataset
	 *        The upload empty data flag to set.
	 * @since 1.2
	 */
	public void setUploadEmptyDataset(boolean uploadEmptyDataset) {
		this.uploadEmptyDataset = uploadEmptyDataset;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
