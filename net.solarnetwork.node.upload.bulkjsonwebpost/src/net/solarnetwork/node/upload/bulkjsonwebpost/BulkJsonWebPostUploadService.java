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
import net.solarnetwork.node.BulkUploadResult;
import net.solarnetwork.node.BulkUploadService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionAcknowledgementService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.support.JsonHttpClientSupport;
import net.solarnetwork.util.OptionalServiceTracker;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;

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
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class BulkJsonWebPostUploadService extends JsonHttpClientSupport implements BulkUploadService,
		InstructionAcknowledgementService {

	private String url = "/bulkUpload.do";
	private OptionalServiceTracker<ReactorService> reactorService;

	@Override
	public String getKey() {
		return "BulkJsonWebPostUploadService:" + getIdentityService().getSolarNetHostName();
	}

	@Override
	public List<BulkUploadResult> uploadBulkDatum(Collection<Datum> data) {
		if ( data == null ) {
			return Collections.emptyList();
		}
		List<UploadResult> uploadResults;
		try {
			uploadResults = upload(data);
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
			upload(instructions);
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
	 * 			{ "id" : "abc" ... },
	 * 			...
	 * 		],
	 * 		"instructions" : [
	 * 
	 * 		]
	 * }
	 * </pre>
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 * @throws JsonParseException
	 */
	private List<UploadResult> upload(Collection<?> data) throws IOException, JsonParseException {
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
							for ( JsonNode element : datumArray ) {
								UploadResult r = new UploadResult();
								if ( element.has("id") ) {
									r.setId(element.get("id").asText());
								}
								result.add(r);
							}
						}
						JsonNode instrArray = child.get("instructions");
						ReactorService reactor = (reactorService == null ? null : reactorService
								.service());
						if ( instrArray != null && instrArray.isArray() && reactor != null ) {
							List<InstructionStatus> status = reactor.processInstruction(
									getIdentityService().getSolarInBaseUrl(), instrArray,
									JSON_MIME_TYPE, null);
							log.debug("Instructions processed: {}", status);
						}
					} else {
						log.debug("Upload returned no data.");
					}
				} else {
					log.warn("Upload not successful: {}", root.get("message") == null ? "(no message)"
							: root.get("message").asText());
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

}
