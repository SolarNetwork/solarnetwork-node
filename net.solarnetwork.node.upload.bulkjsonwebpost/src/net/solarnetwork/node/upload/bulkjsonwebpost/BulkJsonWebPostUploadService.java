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
import java.util.Map;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.context.MessageSource;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.node.BulkUploadResult;
import net.solarnetwork.node.BulkUploadService;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.domain.BaseDatum;
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
 * @version 1.6
 */
public class BulkJsonWebPostUploadService extends JsonHttpClientSupport
		implements BulkUploadService, InstructionAcknowledgementService, SettingSpecifierProvider {

	private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime();

	private String url = "/bulkUpload.do";
	private OptionalService<ReactorService> reactorService;
	private boolean uploadEmptyDataset = false;
	private MessageSource messageSource;
	private OptionalService<EventAdmin> eventAdmin;

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
	public String uploadDatum(Datum data) {
		List<BulkUploadResult> results = uploadBulkDatum(Collections.singleton(data));
		if ( results != null && !results.isEmpty() ) {
			return results.get(0).getId();
		}
		return null;
	}

	@Override
	public List<BulkUploadResult> uploadBulkDatum(Collection<Datum> data) {
		if ( (data == null || data.size() < 1) && uploadEmptyDataset == false ) {
			return Collections.emptyList();
		}
		List<BulkUploadResult> results = null;
		try {
			results = upload(data);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
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
	 * 			{ "id": "123abc", "created": 123, sourceId: "abc" ... },
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
	private List<BulkUploadResult> upload(Collection<?> data) throws IOException {
		// NOTE: serializing JSON into intermediate tree, because of possibility of
		// datum filtering during serialization, to prevent logging of tree from
		// inadvertently triggering serialization changes. This also allows us
		// to verify how many datum we actually upload (i.e. after filtering).
		JsonNode jsonData = getObjectMapper().valueToTree(data);
		InputStream response = handlePost(jsonData);

		List<BulkUploadResult> result = null;
		try {
			JsonNode root = getObjectMapper().readTree(response);
			if ( log.isDebugEnabled() ) {
				log.debug("Got JSON response: {}",
						getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(root));
			}
			if ( root.isObject() ) {
				JsonNode child = root.path("success");
				if ( child.asBoolean() ) {
					result = new ArrayList<BulkUploadResult>(data.size());
					child = root.path("data");
					if ( child.isObject() ) {
						JsonNode datumArray = child.get("datum");
						Iterator<JsonNode> reqJsonItr = null;
						JsonNode currReqJsonNode = null;
						Iterator<JsonNode> jsonItr = null;
						JsonNode currJsonNode = null;
						if ( datumArray != null && datumArray.isArray() ) {
							assert datumArray.size() == jsonData.size();
							reqJsonItr = jsonData.iterator();
							currReqJsonNode = reqJsonItr.hasNext() ? reqJsonItr.next() : null;
							jsonItr = datumArray.iterator();
							currJsonNode = jsonItr.hasNext() ? jsonItr.next() : null;
						}

						for ( Object obj : data ) {
							String id = null;
							Datum datum = null;
							if ( obj instanceof Instruction ) {
								Instruction instr = (Instruction) obj;
								if ( currJsonNode != null ) {
									id = currJsonNode.path("id").textValue();
									if ( instr.getRemoteInstructionId().equals(id) ) {
										currReqJsonNode = reqJsonItr.hasNext() ? reqJsonItr.next()
												: null;
										currJsonNode = jsonItr.hasNext() ? jsonItr.next() : null;
									}
								}
								if ( id == null ) {
									id = instr.getRemoteInstructionId();
								}
							} else {
								// assume Datum here
								datum = (Datum) obj;
								if ( currJsonNode != null ) {
									JsonNode createdObj = currJsonNode.path("created");
									long created = 0;
									if ( createdObj.isNumber() ) {
										created = createdObj.longValue();
									} else if ( createdObj.isTextual() ) {
										try {
											// parse as strict ISO8601 (SN returns space date/time delimiter)
											created = ISO_DATE_TIME_FORMATTER
													.parseDateTime(
															createdObj.textValue().replace(' ', 'T'))
													.getMillis();
										} catch ( IllegalArgumentException e ) {
											log.debug("Unexpected created date format: {}", createdObj);
										}
									}
									String sourceId = currJsonNode.path("sourceId").textValue();
									if ( datum.getCreated().getTime() == created
											&& datum.getSourceId().equals(sourceId) ) {
										id = currJsonNode.path("id").textValue();
										postDatumUploadedEvent(datum, currReqJsonNode);
										currReqJsonNode = reqJsonItr.hasNext() ? reqJsonItr.next()
												: null;
										currJsonNode = jsonItr.hasNext() ? jsonItr.next() : null;
									}
								}
								if ( id == null ) {
									// generate a synthetic ID string
									id = DigestUtils.md5DigestAsHex(String
											.format("%tQ;%s", datum.getCreated(), datum.getSourceId())
											.getBytes());
								}
							}
							result.add(new BulkUploadResult(datum, id));
						}

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

	// post DATUM_UPLOADED events; but with the (possibly transformed) uploaded data so we show just
	// what was actually uploaded
	private void postDatumUploadedEvent(Datum datum, JsonNode node) {
		Map<String, Object> props = JsonUtils.getStringMapFromTree(node);
		if ( props != null && !props.isEmpty() ) {
			if ( !(props.get("samples") instanceof Map<?, ?>) ) {
				// no sample data; this must have been filtered out via transform
				return;
			}

			// convert samples, which can contain nested maps for a/i/s 
			@SuppressWarnings("unchecked")
			Map<String, ?> samples = (Map<String, ?>) props.get("samples");
			props.remove("samples");
			for ( Map.Entry<String, ?> me : samples.entrySet() ) {
				Object val = me.getValue();
				if ( val instanceof Map<?, ?> ) {
					@SuppressWarnings("unchecked")
					Map<String, ?> subMap = (Map<String, ?>) val;
					props.putAll(subMap);
				} else {
					props.put(me.getKey(), val);
				}
			}

			String[] types = BaseDatum.getDatumTypes(datum.getClass());
			if ( types != null && types.length > 0 ) {
				props.put(Datum.DATUM_TYPE_PROPERTY, types[0]);
				props.put(Datum.DATUM_TYPES_PROPERTY, types);
			}
			log.debug("Created {} event with props {}", UploadService.EVENT_TOPIC_DATUM_UPLOADED, props);
			postEvent(new Event(UploadService.EVENT_TOPIC_DATUM_UPLOADED, props));
		}
	}

	private void postEvent(Event event) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || event == null ) {
			return;
		}
		ea.postEvent(event);
	}

	private InputStream handlePost(Object data) {
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

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the {@link EventAdmin} service.
	 * 
	 * @return the EventAdmin service
	 * @since 1.5
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set an {@link EventAdmin} service to use.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin to use
	 * @since 1.5
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}
}
