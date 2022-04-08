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

import static net.solarnetwork.node.service.DatumEvents.datumEvent;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.InstructionAcknowledgementService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.service.BulkUploadResult;
import net.solarnetwork.node.service.BulkUploadService;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.UploadService;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.DateUtils;

/**
 * {@link BulkUploadService} that uses an HTTP POST with body content formed as
 * a JSON document containing all data to upload.
 * 
 * <p>
 * The request will be a JSON array of possibly mixed nested array and object
 * values. {@link NodeDatum} objects will be posted as stream datum (JSON array)
 * objects if stream metadata is available for the given datum. Otherwise a
 * traditional datum (JSON object) will be used. Instruction status will be
 * posted as a status JSON object.
 * </p>
 * 
 * @author matt
 * @version 2.1
 */
public class BulkJsonWebPostUploadService extends JsonHttpClientSupport
		implements BulkUploadService, InstructionAcknowledgementService, SettingSpecifierProvider {

	private String url = "/bulkUpload.do";
	private final OptionalService<ReactorService> reactorServiceOpt;
	private final OptionalService<EventAdmin> eventAdminOpt;
	private final OptionalService<DatumMetadataService> datumMetadataServiceOpt;
	private boolean uploadEmptyDataset = false;

	/**
	 * Default constructor.
	 * 
	 * <p>
	 * This sets the {@code compress} flag to {@literal true}.
	 * </p>
	 * 
	 * @param reactorService
	 *        the optional reactor service
	 * @param eventAdmin
	 *        the event admin service
	 * @param datumMetadataService
	 *        the datum metadata service
	 */
	public BulkJsonWebPostUploadService(OptionalService<ReactorService> reactorService,
			OptionalService<EventAdmin> eventAdmin,
			OptionalService<DatumMetadataService> datumMetadataService) {
		super();
		this.reactorServiceOpt = reactorService;
		this.eventAdminOpt = eventAdmin;
		this.datumMetadataServiceOpt = datumMetadataService;
		setCompress(true);
	}

	@Override
	public String getKey() {
		return "BulkJsonWebPostUploadService:" + getIdentityService().getSolarNetHostName();
	}

	@Override
	public String uploadDatum(NodeDatum data) {
		List<BulkUploadResult> results = uploadBulkDatum(Collections.singleton(data));
		if ( results != null && !results.isEmpty() ) {
			return results.get(0).getId();
		}
		return null;
	}

	@Override
	public List<BulkUploadResult> uploadBulkDatum(Collection<NodeDatum> data) {
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
	public void acknowledgeInstructions(Collection<InstructionStatus> instructions) {
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
	 * The response is expected to be structured like this, where both
	 * traditional datum and stream datum results are supported:
	 * </p>
	 * 
	 * <pre>
	 * {
	 * 	"success" : true,
	 *  "message" : "some message",
	 * 	"data" : {
	 * 		"datum" : [
	 * 			{ "id": "123abc", "created": 123, sourceId: "abc" ... },
	 *          { "streamId": "123abc", "timestamp": "2021-09-10 20:43:21.123Z" },
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
		final DatumMetadataService datumMetadataService = OptionalService
				.service(datumMetadataServiceOpt);
		final ObjectMapper objectMapper = getObjectMapper();
		final Long nodeId = getIdentityService().getNodeId();
		// NOTE: serializing JSON into intermediate tree, because of possibility of
		// datum filtering during serialization, to prevent logging of tree from
		// inadvertently triggering serialization changes. This also allows us
		// to verify how many datum we actually upload (i.e. after filtering).
		JsonNode jsonData;
		if ( datumMetadataService != null ) {
			ArrayNode rootArray = objectMapper.createArrayNode();
			jsonData = rootArray;
			// try to post as stream datum, if metadata available
			for ( Object d : data ) {
				if ( d instanceof NodeDatum ) {
					NodeDatum datum = (NodeDatum) d;
					ObjectDatumKind kind = datum.getKind();
					Long objectId = (kind == ObjectDatumKind.Node ? nodeId : datum.getObjectId());
					ObjectDatumStreamMetadata meta = datumMetadataService.getDatumStreamMetadata(kind,
							objectId, datum.getSourceId());
					if ( meta != null ) {
						// we've got stream metadata: post as this if all properties accounted for
						try {
							DatumProperties datumProps = DatumProperties.propertiesFrom(datum, meta);
							if ( datumProps != null ) {
								d = new BasicStreamDatum(meta.getStreamId(), datum.getTimestamp(),
										datumProps);
							}
						} catch ( IllegalArgumentException e ) {
							log.debug(
									"Unable to post datum as stream datum, falling back to general datum: "
											+ e.getMessage());
						}
					}
				}
				rootArray.add(objectMapper.valueToTree(d));
			}
		} else {
			jsonData = objectMapper.valueToTree(data);
		}

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
							NodeDatum datum = null;
							if ( obj instanceof InstructionStatus ) {
								InstructionStatus status = (InstructionStatus) obj;
								if ( currJsonNode != null ) {
									id = currJsonNode.path("id").textValue();
									if ( id != null
											&& id.equals(status.getInstructionId().toString()) ) {
										currReqJsonNode = reqJsonItr.hasNext() ? reqJsonItr.next()
												: null;
										currJsonNode = jsonItr.hasNext() ? jsonItr.next() : null;
									}
								}
								if ( id == null ) {
									id = status.getInstructionId().toString();
								}
							} else {
								// assume Datum here
								datum = (NodeDatum) obj;
								if ( currJsonNode != null ) {
									JsonNode createdObj = currJsonNode.path("created");
									if ( createdObj.isMissingNode() ) {
										createdObj = currJsonNode.path("timestamp");
									}
									Instant created = null;
									if ( createdObj.isNumber() ) {
										created = Instant.ofEpochMilli(createdObj.longValue());
									} else if ( createdObj.isTextual() ) {
										try {
											// parse as strict ISO8601 (SN returns space date/time delimiter)
											created = DateUtils.ISO_DATE_TIME_ALT_UTC
													.parse(createdObj.textValue(), Instant::from)
													.truncatedTo(ChronoUnit.MILLIS);
										} catch ( DateTimeParseException e ) {
											log.debug("Unexpected created date format: {}", createdObj);
										}
									}
									String sourceId = currJsonNode.path("sourceId").textValue();
									if ( sourceId == null ) {
										String streamId = currJsonNode.path("streamId").textValue();
										if ( streamId != null ) {
											ObjectDatumKind kind = datum.getKind();
											Long objectId = (kind == ObjectDatumKind.Node ? nodeId
													: datum.getObjectId());
											ObjectDatumStreamMetadata meta = datumMetadataService
													.getDatumStreamMetadata(kind, objectId,
															datum.getSourceId());
											if ( meta.getStreamId().toString().equals(streamId) ) {
												sourceId = datum.getSourceId();
											}
										}
									}
									if ( created != null
											&& created.compareTo(datum.getTimestamp()
													.truncatedTo(ChronoUnit.MILLIS)) == 0
											&& datum.getSourceId().equals(sourceId) ) {
										id = currJsonNode.path("id").textValue();
										if ( id == null ) {
											// generate a synthetic ID string
											id = DigestUtils.md5DigestAsHex(String
													.format("%tQ;%s", created, sourceId).getBytes());
										}
										postDatumUploadedEvent(datum, currReqJsonNode);
										currReqJsonNode = reqJsonItr.hasNext() ? reqJsonItr.next()
												: null;
										currJsonNode = jsonItr.hasNext() ? jsonItr.next() : null;
									}
								}
								if ( id == null ) {
									log.warn("Unknown datum result: {}", currJsonNode);
								}
							}
							result.add(new BulkUploadResult(datum, id));
						}

						// look for instructions to process
						JsonNode instrArray = child.path("instructions");
						if ( instrArray.isArray() ) {
							processResponseInstructions(instrArray);
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

	private void processResponseInstructions(JsonNode instrArray) {
		ReactorService reactor = OptionalService.service(reactorServiceOpt);
		if ( reactor == null ) {
			return;
		}
		final String instructorId = getIdentityService().getSolarInBaseUrl();
		for ( JsonNode instrNode : instrArray ) {
			try {
				net.solarnetwork.domain.Instruction instr = getObjectMapper().treeToValue(instrNode,
						net.solarnetwork.domain.Instruction.class);
				if ( instr != null ) {
					InstructionStatus status = reactor
							.processInstruction(BasicInstruction.from(instr, instructorId));
					log.debug("Instruction {} processed: {}", instr, status);
				}
			} catch ( Exception e ) {
				log.warn("Unable to accept instruction JSON [{}]: {}", instrNode, e.toString());
			}
		}
	}

	// post DATUM_UPLOADED events; but with the (possibly transformed) uploaded data so we show just
	// what was actually uploaded
	@SuppressWarnings("unchecked")
	private void postDatumUploadedEvent(NodeDatum datum, JsonNode node) {
		Map<String, Object> props;
		if ( node.isArray() ) {
			// posted as Stream, so just convert original datum to event data
			props = (Map<String, Object>) datum.asSimpleMap();
			// for compatibility with serialized node format, convert time stamp to string
			props.put("created", DateUtils.ISO_DATE_TIME_ALT_UTC
					.format(datum.getTimestamp().truncatedTo(ChronoUnit.MILLIS)));
		} else {
			props = JsonUtils.getStringMapFromTree(node);
		}
		Event event = datumEvent(UploadService.EVENT_TOPIC_DATUM_UPLOADED, datum.getClass(), props);
		postEvent(event);
	}

	private void postEvent(Event event) {
		EventAdmin ea = OptionalService.service(eventAdminOpt);
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
	public String getSettingUid() {
		return "net.solarnetwork.node.upload.bulkjsonwebpost.BulkJsonWebPostUploadService";
	}

	@Override
	public String getDisplayName() {
		return "Bulk JSON Upload Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.add(new BasicToggleSettingSpecifier("uploadEmptyDataset", true));
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

	/**
	 * Get the reactor service.
	 * 
	 * @return the reactor service
	 */
	public OptionalService<ReactorService> getReactorService() {
		return reactorServiceOpt;
	}

	/**
	 * Get the "upload empty dataset" flag.
	 * 
	 * @return {@literal true} to make connections to SolarIn even when there is
	 *         no data to post
	 */
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

	/**
	 * Get the {@link EventAdmin} service.
	 * 
	 * @return the EventAdmin service
	 * @since 1.5
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdminOpt;
	}

}
