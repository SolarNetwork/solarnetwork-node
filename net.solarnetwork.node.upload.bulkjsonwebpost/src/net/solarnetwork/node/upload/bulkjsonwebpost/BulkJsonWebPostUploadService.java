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
import java.util.Iterator;
import java.util.List;
import org.springframework.context.MessageSource;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import net.solarnetwork.util.OptionalServiceTracker;

/**
 * {@link BulkUploadService} that uses an HTTP POST with body content formed as
 * a JSON document containing all data to upload.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>objectMapper</dt>
 * <dd>The {@link ObjectMapper} to marshall objects to JSON with and parse the
 * response with.</dd>
 * 
 * <dt>uploadEmptyDataset</dt>
 * <dd>If <em>true</em> then make a POST request to SolarIn even if there isn't
 * any datum data to upload. This can be useful in situations where we want to
 * be able to receive instructions in the HTTP response even if the node has not
 * produced any data to upload. Defaults to <em>false</em>.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.4
 */
public class BulkJsonWebPostUploadService extends JsonHttpClientSupport
		implements BulkUploadService, InstructionAcknowledgementService, SettingSpecifierProvider {

	private String url = "/bulkUpload.do";
	private OptionalServiceTracker<ReactorService> reactorService;
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
		List<UploadResult> uploadResults;
		try {
			uploadResults = upload(data, false);
		} catch ( JsonParseException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		List<BulkUploadResult> results = new ArrayList<BulkUploadResult>(uploadResults.size());
		Iterator<Datum> dataIterator = data.iterator();
		for ( UploadResult r : uploadResults ) {
			if ( !dataIterator.hasNext() ) {
				break;
			}
			Datum datum = dataIterator.next();
			results.add(new BulkUploadResult(datum, r.getId()));
		}
		return results;
	}

	@Override
	public void acknowledgeInstructions(Collection<Instruction> instructions) {
		try {
			upload(instructions, true);
		} catch ( JsonParseException e ) {
			throw new RuntimeException(e);
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
	 * @return
	 * @throws IOException
	 * @throws JsonParseException
	 */
	private List<UploadResult> upload(Collection<?> data, boolean instructions)
			throws IOException, JsonParseException {
		InputStream response = handlePost(data);
		List<UploadResult> result = new ArrayList<UploadResult>(data.size());
		try {
			JsonNode root = getObjectMapper().readTree(response);
			if ( root.isObject() ) {
				JsonNode child = root.get("success");
				if ( child != null && child.asBoolean() ) {
					child = root.get("data");
					if ( child != null && child.isObject() ) {
						JsonNode datumArray = child.get("datum");
						if ( datumArray != null && datumArray.isArray() ) {

							// Some datum may have been filtered out from upload; so check for matching IDs
							// and pad the returned results so it effectively appears to have been uploaded.
							// The returned results will be in the same order as the posted data, so we
							// can simply do a parallel iteration over the results and posted data, adding
							// padding response objects when the current two objects don't have matching IDs

							Iterator<?> dataItr = (instructions ? null : data.iterator());
							Object dataObj = null;

							for ( JsonNode element : datumArray ) {
								UploadResult r = new UploadResult();
								if ( dataItr == null ) {
									if ( element.has("id") ) {
										r.setId(element.get("id").asText());
									}
								} else {
									dataObj = dataItr.next();
									long created = element.path("created").longValue();
									String sourceId = element.path("sourceId").textValue();
									while ( (dataObj instanceof Datum)
											&& !(created == ((Datum) dataObj).getCreated().getTime()
													&& sourceId
															.equals(((Datum) dataObj).getSourceId())) ) {
										// pad with effectively uploaded result
										result.add(new UploadResult());
										if ( dataItr.hasNext() ) {
											dataObj = dataItr.next();
										} else {
											dataObj = null;
										}
									}
								}
								result.add(r);
							}
						}
						JsonNode instrArray = child.get("instructions");
						ReactorService reactor = (reactorService == null ? null
								: reactorService.service());
						if ( instrArray != null && instrArray.isArray() && reactor != null ) {
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

	public void setUrl(String url) {
		this.url = url;
	}

	public OptionalServiceTracker<ReactorService> getReactorService() {
		return reactorService;
	}

	public void setReactorService(OptionalServiceTracker<ReactorService> reactorService) {
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
