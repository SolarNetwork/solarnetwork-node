/* ==================================================================
 * DatumController.java - 12 Nov 2026 1:17:12 pm
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.api;

import static net.solarnetwork.dao.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.node.setup.web.support.WebServiceControllerSupport.requestDescription;
import static org.springframework.http.MediaType.APPLICATION_CBOR_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.codec.CsvStreamDatumFilteredResultsProcessor;
import net.solarnetwork.codec.jackson.CborUtils;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.codec.jackson.ObjectMapperStreamDatumFilteredResultsProcessor;
import net.solarnetwork.dao.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.DatumReadingType;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.service.FilterableDatumHistorian;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.setup.web.domain.DatumFilterCommand;
import net.solarnetwork.node.setup.web.support.DatumHistorianSelector;
import net.solarnetwork.node.setup.web.support.GlobalExceptionRestController;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.ObjectMapper;

/**
 * API for datum access.
 *
 * <p>
 * This controller offers some compatibility with the SolarQuery API, under both
 * the {@code /api/v1/sec} and {@code /solarquery/api/v1/sec} paths: stream
 * metadata, stream datum (raw or aggregate), and stream readings.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.4
 */
@GlobalExceptionRestController
@RequestMapping({ "/api/v1/sec/datum", "/solarquery/api/v1/sec/datum" })
public class DatumController extends BaseSolarQueryController {

	/** The JSON mapper for stream results. */
	private static final ObjectMapper JSON_STREAM_MAPPER = JsonUtils.JSON_OBJECT_MAPPER.rebuild()
			.enable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();

	/** The CBOR mapper for stream results. */
	private static final ObjectMapper CBOR_STREAM_MAPPER = CborUtils.CBOR_OBJECT_MAPPER.rebuild()
			.enable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();

	/** The media types stream results can be produced in. */
	private static final List<MediaType> STREAM_MEDIA_TYPES = List.of(MediaType.APPLICATION_CBOR,
			MediaType.APPLICATION_JSON, MediaType.valueOf(TEXT_CSV_VALUE));

	private static final Pattern GZIP_ENCODING = Pattern.compile("\\bgzip\\b", Pattern.CASE_INSENSITIVE);

	private static final Logger log = LoggerFactory.getLogger(DatumController.class);

	/**
	 * Constructor.
	 *
	 * @param identityService
	 *        the identity service to use
	 * @param historianSelector
	 *        the datum historian selector to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DatumController(IdentityService identityService, DatumHistorianSelector historianSelector) {
		super(identityService, historianSelector);
	}

	/**
	 * Find node stream metadata.
	 *
	 * @param req
	 *        the HTTP request
	 * @param criteria
	 *        the criteria
	 * @return the matching metadata
	 */
	@GetMapping(value = "/stream/meta/node",
			produces = { APPLICATION_JSON_VALUE, APPLICATION_CBOR_VALUE, TEXT_CSV_VALUE })
	@ResponseBody
	public Result<List<ObjectDatumStreamMetadata>> findNodeMetadata(final HttpServletRequest req,
			final DatumFilterCommand criteria) {
		final HistorianQuery query = historianQuery(criteria, req);
		if ( query == null ) {
			return Result.success(List.of());
		}
		return Result
				.success(new ArrayList<>(query.historian().findDatumStreamMetadata(query.filter())));
	}

	/**
	 * Find datum in stream (array) form.
	 *
	 * <p>
	 * With an {@code aggregation} of {@code Hour}, {@code Day} or
	 * {@code Month}, a historian with stored aggregates provides aggregate
	 * results, which are written in the SolarQuery aggregate form. With an
	 * {@code aggregation} of {@code FiveMinute}, {@code TenMinute},
	 * {@code FifteenMinute} or {@code ThirtyMinute}, a historian with the
	 * aggregate setup computes the aggregates when queried, in the same form;
	 * it may refuse the query, for example for a longer time span than it
	 * allows, and the request then fails with a {@literal 422} status, as in
	 * SolarQuery. Otherwise the datum are provided as they are stored.
	 * </p>
	 *
	 * <p>
	 * The results are written directly to the response, in the first format
	 * supported by the {@code Accept} types in the order given, trying CBOR,
	 * JSON, and then CSV for each type, as SolarQuery does. No {@code Accept}
	 * header is treated as any type, and so gives CBOR. The results are
	 * compressed with gzip when {@code Accept-Encoding} allows it.
	 * </p>
	 *
	 * <p>
	 * An error before any results are written to the response is handled as for
	 * any other request. After that, the error can not be passed to the client,
	 * so it is logged rather than passed on to the servlet container, and the
	 * response ends with the results left incomplete. Incomplete JSON and CBOR
	 * results, or gzip compressed results, can not be parsed, so the client can
	 * tell that they are incomplete.
	 * </p>
	 *
	 * @param req
	 *        the HTTP request
	 * @param res
	 *        the HTTP response
	 * @param criteria
	 *        the criteria
	 * @param accept
	 *        the HTTP {@code Accept} header value
	 * @param acceptEncoding
	 *        the HTTP {@code Accept-Encoding} header value
	 * @throws IOException
	 *         if an IO error occurs before any results are written
	 * @throws HttpMediaTypeNotAcceptableException
	 *         if {@code accept} has no supported type
	 */
	@GetMapping(value = "/stream/datum",
			produces = { APPLICATION_JSON_VALUE, APPLICATION_CBOR_VALUE, TEXT_CSV_VALUE })
	public void findDatum(
	// @formatter:off
			final HttpServletRequest req,
			final HttpServletResponse res,
			final DatumFilterCommand criteria,
			
			@RequestHeader(value = HttpHeaders.ACCEPT, required = false)
			final @Nullable String accept,
			
			@RequestHeader(value = HttpHeaders.ACCEPT_ENCODING, required = false)
			final @Nullable String acceptEncoding
			// @formatter:on
	) throws IOException, HttpMediaTypeNotAcceptableException {
		streamResults(req, res, criteria, accept, acceptEncoding, (historian, filter, processor) -> {
			historian.findFilteredStream(filter, processor, null, null, null);
		});
	}

	/**
	 * Find "reading" results in stream (array) form.
	 *
	 * <p>
	 * The {@code readingType} parameter is required, as it is by SolarQuery:
	 * </p>
	 *
	 * <ul>
	 * <li>When it is missing, the request fails with a {@literal 400} status,
	 * as in SolarQuery, with an error result.</li>
	 * <li>When it is not a {@link DatumReadingType} name, the request fails
	 * with a {@literal 422} status, as in SolarQuery.</li>
	 * <li>When it names a reading type the historian does not support (all but
	 * {@link DatumReadingType#Difference}), the request fails with a
	 * {@literal 422} status. It is not ignored like other unsupported
	 * parameters, as the results of another reading type would be wrong rather
	 * than less complete.</li>
	 * </ul>
	 *
	 * <p>
	 * The {@code tolerance} parameter is accepted and ignored, as the
	 * {@code Difference} reading type does not use it. The other parameters are
	 * those of {@code /stream/datum}, and the results are written in the same
	 * way, in the SolarQuery reading form.
	 * </p>
	 *
	 * <p>
	 * A request that can match nothing, because it names only other nodes, the
	 * node is not associated, or the historian is not available, gets an empty
	 * result without the reading type being checked by the historian.
	 * </p>
	 *
	 * @param req
	 *        the HTTP request
	 * @param res
	 *        the HTTP response
	 * @param criteria
	 *        the criteria
	 * @param readingType
	 *        the reading type
	 * @param accept
	 *        the HTTP {@code Accept} header value
	 * @param acceptEncoding
	 *        the HTTP {@code Accept-Encoding} header value
	 * @throws IOException
	 *         if an IO error occurs before any results are written
	 * @throws HttpMediaTypeNotAcceptableException
	 *         if {@code accept} has no supported type
	 */
	@GetMapping(value = "/stream/reading",
			produces = { APPLICATION_JSON_VALUE, APPLICATION_CBOR_VALUE, TEXT_CSV_VALUE })
	public void findReadings(
	// @formatter:off
			final HttpServletRequest req,
			final HttpServletResponse res,
			final DatumFilterCommand criteria,
			
			@RequestParam("readingType")
			final DatumReadingType readingType,
			
			@RequestHeader(value = HttpHeaders.ACCEPT, required = false)
			final @Nullable String accept,
			
			@RequestHeader(value = HttpHeaders.ACCEPT_ENCODING, required = false)
			final @Nullable String acceptEncoding
			// @formatter:on
	) throws IOException, HttpMediaTypeNotAcceptableException {
		streamResults(req, res, criteria, accept, acceptEncoding, (historian, filter, processor) -> {
			filter.setReadingType(readingType);
			historian.findFilteredStreamReadings(filter, processor, null, null, null);
		});
	}

	@FunctionalInterface
	private interface StreamQuery {

		/**
		 * Run the query.
		 *
		 * @param historian
		 *        the historian to query
		 * @param filter
		 *        the filter, limited to the node's own datum
		 * @param processor
		 *        the processor to pass the results to
		 * @throws IOException
		 *         if an IO error occurs
		 */
		void query(FilterableDatumHistorian historian, BasicNodeDatumFilter filter,
				StreamDatumFilteredResultsProcessor processor) throws IOException;

	}

	private void streamResults(final HttpServletRequest req, final HttpServletResponse res,
			final DatumFilterCommand criteria, final @Nullable String accept,
			final @Nullable String acceptEncoding, final StreamQuery streamQuery)
			throws IOException, HttpMediaTypeNotAcceptableException {
		final ResponseOutputStream out = new ResponseOutputStream(res,
				acceptEncoding != null && GZIP_ENCODING.matcher(acceptEncoding).find());
		final StreamDatumFilteredResultsProcessor processor = processor(accept, out,
				criteria.allowedPropertyNames());
		res.setContentType(processor.getMimeType().toString());
		try {
			final HistorianQuery query = historianQuery(criteria, req);
			if ( query != null ) {
				streamQuery.query(query.historian(), query.filter(), processor);
			} else {
				processor.start(null, null, null,
						Map.of(METADATA_PROVIDER_ATTR, staticProvider(List.of())));
			}
			processor.close();
		} catch ( IOException | RuntimeException e ) {
			if ( !out.isOpened() ) {
				// nothing has been sent, so the error can be returned as for any request
				throw e;
			}
			if ( out.isFailed() ) {
				return;
			}

			// the error can not be passed to the client once the response has started, so only log
			// it, rather than pass it on to the servlet container; the results are not closed, so
			// incomplete JSON or CBOR results can not be mistaken for complete ones
			log.error(
					"Datum stream query [{}] failed after the response started, results incomplete: {}",
					requestDescription(new ServletWebRequest(req, res)), e.toString(), e);
		}
	}

	private static StreamDatumFilteredResultsProcessor processor(final @Nullable String accept,
			final OutputStream out, final @Nullable Set<String> propertyNames)
			throws HttpMediaTypeNotAcceptableException {
		final List<MediaType> acceptTypes = (accept != null && !accept.isBlank()
				? MediaType.parseMediaTypes(accept)
				: List.of(MediaType.ALL));
		for ( MediaType acceptType : acceptTypes ) {
			if ( MediaType.APPLICATION_CBOR.isCompatibleWith(acceptType) ) {
				return new ObjectMapperStreamDatumFilteredResultsProcessor(
						CBOR_STREAM_MAPPER.createGenerator(out),
						CBOR_STREAM_MAPPER._serializationContext(), MediaType.APPLICATION_CBOR,
						propertyNames);
			} else if ( MediaType.APPLICATION_JSON.isCompatibleWith(acceptType) ) {
				return new ObjectMapperStreamDatumFilteredResultsProcessor(
						JSON_STREAM_MAPPER.createGenerator(out),
						JSON_STREAM_MAPPER._serializationContext(), MediaType.APPLICATION_JSON,
						propertyNames);
			} else if ( CsvStreamDatumFilteredResultsProcessor.TEXT_CSV_MIME_TYPE
					.isCompatibleWith(acceptType) ) {
				return new CsvStreamDatumFilteredResultsProcessor(
						new OutputStreamWriter(out, StandardCharsets.UTF_8), propertyNames);
			}
		}
		throw new HttpMediaTypeNotAcceptableException(STREAM_MEDIA_TYPES);
	}

	/**
	 * A response output stream that is opened when first written to, with
	 * optional gzip compression.
	 *
	 * <p>
	 * This tracks if anything has been written to the response, and if writing
	 * to the response has failed.
	 * </p>
	 */
	private static final class ResponseOutputStream extends OutputStream {

		private final HttpServletResponse response;
		private final boolean gzip;
		private @Nullable OutputStream delegate;
		private boolean failed;

		private ResponseOutputStream(HttpServletResponse response, boolean gzip) {
			super();
			this.response = response;
			this.gzip = gzip;
		}

		private boolean isOpened() {
			return (delegate != null || failed);
		}

		private boolean isFailed() {
			return failed;
		}

		private OutputStream delegate() throws IOException {
			OutputStream out = this.delegate;
			if ( out == null ) {
				out = response.getOutputStream();
				if ( gzip ) {
					response.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
					out = new GZIPOutputStream(out);
				}
				this.delegate = out;
			}
			return out;
		}

		@Override
		public void write(int b) throws IOException {
			try {
				delegate().write(b);
			} catch ( IOException e ) {
				failed = true;
				throw e;
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			try {
				delegate().write(b, off, len);
			} catch ( IOException e ) {
				failed = true;
				throw e;
			}
		}

		@Override
		public void flush() throws IOException {
			final OutputStream out = this.delegate;
			if ( out == null ) {
				return;
			}
			try {
				out.flush();
			} catch ( IOException e ) {
				failed = true;
				throw e;
			}
		}

		@Override
		public void close() throws IOException {
			final OutputStream out = this.delegate;
			if ( out == null ) {
				return;
			}
			try {
				out.close();
			} catch ( IOException e ) {
				failed = true;
				throw e;
			}
		}

	}

}
