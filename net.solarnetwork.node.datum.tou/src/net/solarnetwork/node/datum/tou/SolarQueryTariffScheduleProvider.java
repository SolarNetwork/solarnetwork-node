/* ==================================================================
 * SolarQueryTariffScheduleProvider.java - 10/08/2024 8:30:58 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.tou;

import static net.solarnetwork.domain.tariff.SimpleTemporalRangesTariffEvaluator.DEFAULT_EVALUATOR;
import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.NetworkIdentity;
import net.solarnetwork.domain.datum.AggregateStreamDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumDateFunctions;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumStreamDataSet;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.tariff.SimpleTariffRate;
import net.solarnetwork.domain.tariff.SimpleTemporalTariffSchedule;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TemporalRangeSetsTariff;
import net.solarnetwork.io.UrlUtils;
import net.solarnetwork.node.domain.NodeAppConfiguration;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.node.service.TariffScheduleProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.service.support.TariffScheduleUtils;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.web.service.HttpRequestCustomizerService;

/**
 * Query SolarNetwork for a datum stream and turn it into a
 * {@link TariffSchedule}.
 *
 * @author matt
 * @version 1.0
 */
public class SolarQueryTariffScheduleProvider extends BaseIdentifiable implements TariffScheduleProvider,
		SettingSpecifierProvider, DatumDateFunctions, SettingsChangeObserver {

	/** The default value for the {@code connectionTimeout} property. */
	public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(15);

	/** The {@code readingMode} property default value. */
	public static final boolean DEFAULT_READING_MODE = false;

	/** The {@code publicMode} property default value. */
	public static final boolean DEFAULT_PUBLIC_MODE = false;

	/** The {@code tariffScheduleCacheTtl} property default value (12 hours). */
	public static final Duration DEFAULT_TARIFF_SCHEDULE_CACHE_TTL = Duration.ofDays(1);

	/** The {@code aggregation} property default value. */
	public static final Aggregation DEFAULT_AGGREGATION = Aggregation.Hour;

	/** The the SolarQuery stream datum path. */
	public static final String STREAM_DATUM_PATH = "/api/v1/sec/datum/stream/datum";

	/** The the SolarQuery stream datum path. */
	public static final String STREAM_READING_PATH = "/api/v1/sec/datum/stream/reading";

	private final Clock clock;
	private final ObjectMapper objectMapper;
	private final String httpAccept;
	private final OptionalService<SetupService> setupService;
	private final OptionalService<IdentityService> identityService;
	private final OptionalService<ClientHttpRequestFactory> httpRequestFactory;
	private OptionalFilterableService<HttpRequestCustomizerService> httpRequestCustomizer;
	private Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
	private boolean readingMode = DEFAULT_READING_MODE;
	private boolean publicMode = DEFAULT_READING_MODE;
	private Duration tariffScheduleCacheTtl = DEFAULT_TARIFF_SCHEDULE_CACHE_TTL;
	private Long nodeId;
	private String sourceId;
	private Aggregation aggregation = DEFAULT_AGGREGATION;
	private Set<String> datumStreamPropertyNames;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private TemporalAmount startDateOffset;
	private TemporalUnit startDateOffsetTruncateUnit;
	private TemporalAmount endDateOffset;
	private TemporalUnit endDateOffsetTruncateUnit;
	private ZoneId timeZone;

	private final AtomicReference<CachedResult<TariffSchedule>> tariffSchedule = new AtomicReference<>();
	private Executor executor;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The system clock will be used, and CBOR will be requested.
	 * </p>
	 *
	 * @param setupService
	 *        the setup service
	 * @param identityService
	 *        the identity service
	 * @param httpRequestFactory
	 *        the HTTP request factory
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SolarQueryTariffScheduleProvider(OptionalService<SetupService> setupService,
			OptionalService<IdentityService> identityService,
			OptionalService<ClientHttpRequestFactory> httpRequestFactory) {
		this(Clock.systemUTC(), JsonUtils.newDatumObjectMapper(new CBORFactory()), "application/cbor",
				setupService, identityService, httpRequestFactory);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param objectMapper
	 *        the mapper to use
	 * @param httpAccept
	 *        the expected content type; should be either
	 *        {@code application/cbor} or {@code application/json}
	 * @param setupService
	 *        the setup service
	 * @param identityService
	 *        the identity service
	 * @param httpRequestFactory
	 *        the HTTP request factory
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SolarQueryTariffScheduleProvider(Clock clock, ObjectMapper objectMapper, String httpAccept,
			OptionalService<SetupService> setupService, OptionalService<IdentityService> identityService,
			OptionalService<ClientHttpRequestFactory> httpRequestFactory) {
		super();
		this.clock = ObjectUtils.requireNonNullArgument(clock, "clock");
		this.objectMapper = ObjectUtils.requireNonNullArgument(objectMapper, "objectMapper");
		this.httpAccept = ObjectUtils.requireNonNullArgument(httpAccept, "httpAccept");
		this.setupService = ObjectUtils.requireNonNullArgument(setupService, "setupService");
		this.identityService = ObjectUtils.requireNonNullArgument(identityService, "identityService");
		this.httpRequestFactory = ObjectUtils.requireNonNullArgument(httpRequestFactory,
				"httpRequestFactory");
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		tariffSchedule.set(null);

		// re-load the tariff schedule
		final Executor exec = getExecutor();
		if ( exec != null ) {
			exec.execute(this::tariffSchedule);
		} else {
			tariffSchedule();
		}
	}

	@Override
	public synchronized TariffSchedule tariffSchedule() {
		CachedResult<TariffSchedule> r = tariffSchedule.updateAndGet(c -> {
			if ( c != null && c.isValid() ) {
				return c;
			}
			try {
				TariffSchedule s = queryForSchedule();
				if ( s == null ) {
					return null;
				}
				return new CachedResult<>(s, tariffScheduleCacheTtl.getSeconds(), TimeUnit.SECONDS);
			} catch ( Exception e ) {
				log.error(
						"Error querying SolarQuery for tariff schedule, [{}] service unable to resolve tariff schedule: {}",
						getUid(), e.getMessage(), e);
				return null;
			}
		});
		return (r != null ? r.getResult() : null);
	}

	private TariffSchedule queryForSchedule() {
		final Long nodeId = nodeId();
		if ( nodeId == null ) {
			log.warn("Node ID not available, [{}] service unable to resolve tariff schedule.", getUid());
			return null;
		}

		final String sourceId = getSourceId();
		if ( sourceId == null ) {
			log.warn("Source ID not available, [{}] service unable to resolve tariff schedule.",
					getUid());
			return null;
		}

		final Aggregation agg = aggregation();
		if ( agg == null ) {
			log.warn("Aggregation not available, [{}] service unable to resolve tariff schedule.",
					getUid());
			return null;
		}

		final LocalDateTime now = LocalDateTime.now(clock);

		final LocalDateTime startDate = startDate(now);
		if ( startDate == null ) {
			log.warn("Start date not available, [{}] service unable to resolve tariff schedule.",
					getUid());
			return null;
		}

		final LocalDateTime endDate = endDate(now);
		if ( endDate == null ) {
			log.warn("End date not available, [{}] service unable to resolve tariff schedule.",
					getUid());
			return null;
		}

		final Set<String> propNames = getDatumStreamPropertyNames();
		if ( propNames == null || propNames.isEmpty() ) {
			log.warn(
					"Datum stream property names not configured, [{}] service unable to resolve tariff schedule.",
					getUid());
			return null;
		}
		final ClientHttpRequestFactory reqFactory = service(httpRequestFactory);
		if ( reqFactory == null ) {
			log.warn(
					"ClientHttpRequestFactory not available, [{}] service unable to resolve tariff schedule.",
					getUid());
			return null;
		}
		URI uri = uri(nodeId, sourceId, agg, startDate, endDate);
		if ( uri == null ) {
			return null;
		}
		final ClientHttpRequest req;
		try {
			req = reqFactory.createRequest(uri, HttpMethod.GET);
		} catch ( IOException e ) {
			log.error(
					"Failed to create SolarQuery reqeust [{}], [{}] service unable to resolve tariff schedule: {}",
					uri, getUid(), e.toString());
			return null;
		}
		req.getHeaders().add(HttpHeaders.ACCEPT, httpAccept);
		req.getHeaders().add(HttpHeaders.ACCEPT_ENCODING, "gzip");
		final HttpRequestCustomizerService cust = service(httpRequestCustomizer);
		if ( cust != null ) {
			PlaceholderService phs = service(getPlaceholderService());
			Map<String, Object> parameters;
			if ( phs != null ) {
				parameters = new HashMap<>();
				phs.copyPlaceholders(parameters);
			} else {
				parameters = Collections.emptyMap();
			}
			cust.customize(req, null, parameters);
		}
		try (ClientHttpResponse res = req.execute()) {
			if ( !res.getStatusCode().is2xxSuccessful() ) {
				log.error(
						"SolarQuery request [{}] returned {} status code, [{}] service unable to resolve tariff schedule.",
						uri, res.getStatusCode(), getUid());
				return null;
			}

			@SuppressWarnings("unchecked")
			ObjectDatumStreamDataSet<AggregateStreamDatum> datum = objectMapper.readValue(
					UrlUtils.getInputStreamFromUrlResponseStream(res.getBody(),
							res.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING)),
					ObjectDatumStreamDataSet.class);
			return generateSchedule(nodeId, sourceId, agg, propNames, datum);
		} catch ( IOException e ) {
			log.warn(
					"Communication error on SolarQuery request [{}], [{}] service unable to resolve tariff schedule: {}",
					uri, getUid(), e.toString());
			return null;
		}
	}

	private URI uri(final Long nodeId, final String sourceId, final Aggregation agg,
			LocalDateTime startDate, LocalDateTime endDate) {
		final SetupService s = service(setupService);
		final NodeAppConfiguration cfg = (s != null ? s.getAppConfiguration() : null);
		final Map<String, String> urls = (cfg != null ? cfg.getNetworkServiceUrls() : null);
		final String baseUrl = (urls != null ? urls.get(NetworkIdentity.SOLARQUERY_NETWORK_SERVICE_KEY)
				: null);
		if ( baseUrl == null ) {
			log.warn("SolarQuery URL not available, [{}] service unable to resolve tariff schedule.",
					getUid());
			return null;
		}

		final String path = (readingMode ? STREAM_READING_PATH : STREAM_DATUM_PATH);

		final StringBuilder buf = new StringBuilder(baseUrl);
		buf.append(publicMode ? path.replace("/sec/", "/pub/") : path);
		buf.append("?nodeId=").append(nodeId);
		UrlUtils.appendURLEncodedValue(buf, "sourceId", sourceId);
		buf.append("&aggregation=").append(agg.name());
		if ( readingMode ) {
			buf.append("&readingType=Difference");
		}
		UrlUtils.appendURLEncodedValue(buf, "localStartDate",
				DateUtils.ISO_DATE_OPT_TIME.format(startDate));
		UrlUtils.appendURLEncodedValue(buf, "localEndDate", DateUtils.ISO_DATE_OPT_TIME.format(endDate));

		try {
			return new URI(buf.toString());
		} catch ( URISyntaxException e ) {
			log.error("Invalid URI [{}], [{}] service unable to resolve tariff schedule: {}", buf,
					getUid(), e.getMessage());
			return null;
		}
	}

	private Aggregation aggregation() {
		final Aggregation agg = getAggregation();
		if ( readingMode ) {
			switch (agg) {
				case Hour:
				case Day:
				case Month:
				case Year:
					return agg;

				default:
					return null;
			}
		}
		if ( isAggregationSupported(agg) ) {
			return agg;
		}
		return null;
	}

	private boolean isAggregationSupported(Aggregation agg) {
		switch (agg) {
			case FiveMinute:
			case TenMinute:
			case FifteenMinute:
			case ThirtyMinute:
			case Hour:
			case HourOfDay:
			case SeasonalHourOfDay:
			case HourOfYear:
			case Day:
			case DayOfWeek:
			case SeasonalDayOfWeek:
			case DayOfYear:
			case Week:
			case WeekOfYear:
			case Month:
			case Year:
				return true;

			default:
				return false;
		}
	}

	private Long nodeId() {
		Long nodeId = getNodeId();
		if ( nodeId == null ) {
			final IdentityService ident = service(identityService);
			nodeId = (ident != null ? ident.getNodeId() : null);
		}
		return nodeId;
	}

	private LocalDateTime startDate(LocalDateTime now) {
		return date(now, getStartDate(), getStartDateOffset(), getStartDateOffsetTruncateUnit());
	}

	private LocalDateTime endDate(LocalDateTime now) {
		return date(now, getEndDate(), getEndDateOffset(), getEndDateOffsetTruncateUnit());
	}

	private LocalDateTime date(final LocalDateTime now, final LocalDateTime date, final TemporalAmount t,
			final TemporalUnit u) {
		LocalDateTime result = date;
		if ( result == null ) {
			if ( t != null ) {
				result = (LocalDateTime) datePlus(now, t);
			}
			if ( u != null ) {
				result = (LocalDateTime) dateTruncate(result, u);
			}
		}
		return result;
	}

	private TariffSchedule generateSchedule(final Long nodeId, final String sourceId,
			final Aggregation agg, Set<String> propNames,
			final ObjectDatumStreamDataSet<AggregateStreamDatum> datum) {
		final int propNamesCount = propNames.size();
		final ObjectDatumStreamMetadata meta = datum.metadataForObjectSource(nodeId, sourceId);
		if ( meta == null ) {
			log.error(
					"Metadata for node {} source [{}] stream not available, [{}] service unable to resolve tariff schedule.",
					nodeId, sourceId, getUid());
			return null;
		}

		// create accessors for each property so we don't have to search within datum loop
		final Map<String, Function<DatumProperties, BigDecimal>> propFunctions = new HashMap<>(
				propNamesCount);
		for ( String propName : propNames ) {
			final int aIdx = meta.propertyIndex(DatumSamplesType.Accumulating, propName);
			if ( aIdx >= 0 ) {
				propFunctions.put(propName, p -> {
					return p.accumulatingValue(aIdx);
				});
				continue;
			}
			final int iIdx = meta.propertyIndex(DatumSamplesType.Instantaneous, propName);
			if ( iIdx >= 0 ) {
				propFunctions.put(propName, p -> {
					return p.instantaneousValue(iIdx);
				});
				continue;
			}
			log.error(
					"Datum stream property [{}] not found on stream {}, [{}] service unable to resolve tariff schedule.",
					propName, meta.getStreamId(), getUid());
			return null;
		}

		final ZoneId zone = timeZone(agg);

		final List<Tariff> tariffs = new ArrayList<>(
				datum.getReturnedResultCount() != null ? datum.getReturnedResultCount() : 32);
		for ( AggregateStreamDatum d : datum ) {
			List<Tariff.Rate> rates = new ArrayList<>(propNamesCount);
			for ( String propName : propNames ) {
				BigDecimal propVal = propFunctions.get(propName).apply(d.getProperties());
				if ( propVal != null ) {
					rates.add(new SimpleTariffRate(propName, propName, propVal));
				}
			}
			final ZonedDateTime zdt = d.getTimestamp().atZone(zone);
			Map<Integer, IntRangeSet> monthRangeCache = null;
			IntRangeSet month = null;
			if ( agg == Aggregation.SeasonalDayOfWeek || agg == Aggregation.SeasonalHourOfDay ) {
				// create month range out of the 3 months in the current season
				// use a cache to avoid creating new instance for each tariff
				if ( monthRangeCache == null ) {
					monthRangeCache = new HashMap<>(4);
				}
				month = monthRangeCache.computeIfAbsent(zdt.getMonthValue(), m -> {
					IntRangeSet result = new IntRangeSet(3);
					LocalDate date = zdt.toLocalDate();
					for ( int i = 0; i < 3; i++ ) {
						result.add(date.getMonthValue());
						date = date.plusMonths(1);
					}
					return result;
				});
			} else if ( agg.compareLevel(Aggregation.Month) <= 0 && agg != Aggregation.DayOfWeek
					&& agg != Aggregation.HourOfDay ) {
				// use a cache to avoid creating new instance for each tariff
				if ( monthRangeCache == null ) {
					monthRangeCache = new HashMap<>(4);
				}
				month = monthRangeCache.computeIfAbsent(zdt.getMonthValue(),
						m -> new IntRangeSet(IntRange.rangeOf(m)));
			}

			Map<Integer, IntRangeSet> domRangeCache = null;
			Map<Integer, IntRangeSet> dowRangeCache = null;
			IntRangeSet dom = null;
			IntRangeSet dow = null;
			if ( agg == Aggregation.DayOfWeek || agg == Aggregation.SeasonalDayOfWeek ) {
				if ( dowRangeCache == null ) {
					dowRangeCache = new HashMap<>(7);
				}
				dow = dowRangeCache.computeIfAbsent(zdt.getDayOfWeek().getValue(),
						wd -> new IntRangeSet(IntRange.rangeOf(wd)));
			} else if ( agg.compareLevel(Aggregation.Day) <= 0 && agg != Aggregation.HourOfDay
					&& agg != Aggregation.SeasonalHourOfDay ) {
				if ( domRangeCache == null ) {
					domRangeCache = new HashMap<>(31);
				}
				dom = domRangeCache.computeIfAbsent(zdt.getDayOfMonth(),
						md -> new IntRangeSet(IntRange.rangeOf(md)));
			}

			Map<Integer, IntRangeSet> modRangeCache = null;
			IntRangeSet mod = null;
			if ( agg.compareLevel(Aggregation.Hour) < 0 ) {
				if ( modRangeCache == null ) {
					modRangeCache = new HashMap<>(24);
				}
				int minutesPerAgg = agg.getLevel() / 60;
				int minuteOfDayStart = zdt.getHour() * 60 + zdt.getMinute() / minutesPerAgg;
				mod = modRangeCache.computeIfAbsent(minuteOfDayStart,
						mods -> new IntRangeSet(IntRange.rangeOf(mods, mods + minutesPerAgg)));
			} else if ( agg.compareLevel(Aggregation.Hour) == 0 ) {
				if ( modRangeCache == null ) {
					modRangeCache = new HashMap<>(24);
				}
				int minuteOfDayStart = zdt.getHour() * 60;
				mod = modRangeCache.computeIfAbsent(minuteOfDayStart,
						mods -> new IntRangeSet(IntRange.rangeOf(mods, mods + 60)));
			}

			tariffs.add(new TemporalRangeSetsTariff(month, dom, dow, mod, rates));
		}
		return new SimpleTemporalTariffSchedule(tariffs);
	}

	private ZoneId timeZone(Aggregation agg) {
		switch (agg) {
			case DayOfWeek:
			case DayOfYear:
			case HourOfDay:
			case HourOfYear:
			case SeasonalDayOfWeek:
			case SeasonalHourOfDay:
			case WeekOfYear:
				// all XOfY use UTC to represent stream local zone
				return ZoneOffset.UTC;

			default:
				// continue
		}
		ZoneId zone = getTimeZone();
		if ( zone == null ) {
			zone = ZoneId.systemDefault();
		}
		return zone;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.tou.solarquery";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		result.add(0, new BasicTitleSettingSpecifier("status", statusMessage(), true, true));
		result.add(new BasicToggleSettingSpecifier("readingMode", DEFAULT_READING_MODE));
		result.add(new BasicToggleSettingSpecifier("publicMode", DEFAULT_PUBLIC_MODE));
		result.add(new BasicTextFieldSettingSpecifier("nodeId", null));
		result.add(new BasicTextFieldSettingSpecifier("sourceId", null));

		// drop-down menu for aggregation
		BasicMultiValueSettingSpecifier aggSpec = new BasicMultiValueSettingSpecifier("aggregationKey",
				DEFAULT_AGGREGATION.getKey());
		Map<String, String> aggTitles = new LinkedHashMap<>(12);
		for ( Aggregation agg : Aggregation.values() ) {
			if ( !isAggregationSupported(agg) ) {
				continue;
			}
			aggTitles.put(agg.getKey(),
					getMessageSource().getMessage(agg.name() + ".key", null, Locale.getDefault()));
		}
		aggSpec.setValueTitles(aggTitles);
		result.add(aggSpec);

		result.add(new BasicTextFieldSettingSpecifier("startDateValue", null));
		result.add(new BasicTextFieldSettingSpecifier("endDateValue", null));

		result.add(new BasicTextFieldSettingSpecifier("startDateOffsetValue", null));

		// drop-down menu for start date offset truncate units
		Map<String, String> chronoUnitTitles = new LinkedHashMap<>(8);
		chronoUnitTitles.put("", "");
		for ( ChronoUnit unit : ChronoUnit.values() ) {
			if ( !isChronoUnitSupported(unit) ) {
				continue;
			}
			chronoUnitTitles.put(unit.name(),
					getMessageSource().getMessage(unit.name() + ".key", null, Locale.getDefault()));
		}
		BasicMultiValueSettingSpecifier startOffsetTruncateUnitSpec = new BasicMultiValueSettingSpecifier(
				"startDateOffsetTruncateUnitValue", null);
		startOffsetTruncateUnitSpec.setValueTitles(chronoUnitTitles);
		result.add(startOffsetTruncateUnitSpec);

		result.add(new BasicTextFieldSettingSpecifier("endDateOffsetValue", null));

		// drop-down menu for end date offset truncate units
		BasicMultiValueSettingSpecifier endOffsetTruncateUnitSpec = new BasicMultiValueSettingSpecifier(
				"endDateOffsetTruncateUnitValue", null);
		endOffsetTruncateUnitSpec.setValueTitles(chronoUnitTitles);
		result.add(endOffsetTruncateUnitSpec);

		result.add(new BasicTextFieldSettingSpecifier("datumStreamPropertyNamesValue", null));

		result.add(new BasicTextFieldSettingSpecifier("timeZoneId", null));

		result.add(new BasicTextFieldSettingSpecifier("httpRequestCustomizerUid", null, false,
				"(objectClass=net.solarnetwork.web.service.HttpRequestCustomizerService)"));
		result.add(new BasicTextFieldSettingSpecifier("tariffScheduleCacheTtlSecs",
				String.valueOf(DEFAULT_TARIFF_SCHEDULE_CACHE_TTL.getSeconds())));

		result.add(new BasicTextFieldSettingSpecifier("connectionTimeoutMs",
				String.valueOf(DEFAULT_CONNECTION_TIMEOUT.toMillis())));

		return result;
	}

	private boolean isChronoUnitSupported(ChronoUnit unit) {
		switch (unit) {
			case MINUTES:
			case HOURS:
			case HALF_DAYS:
			case DAYS:
			case WEEKS:
			case MONTHS:
			case YEARS:
				return true;

			default:
				return false;
		}
	}

	private String statusMessage() {
		StringBuilder buf = new StringBuilder();
		TariffSchedule schedule = tariffSchedule();
		CachedResult<TariffSchedule> cached = this.tariffSchedule.get();
		MessageSource messageSource = getMessageSource();
		if ( schedule != null ) {
			Collection<? extends Tariff> rules = schedule.rules();
			if ( rules.isEmpty() ) {
				buf.append("<p>").append(messageSource.getMessage("rules.empty", null, null))
						.append("</p>");
			} else {
				final LocalDateTime now = LocalDateTime.now();
				Map<Integer, Tariff> active = TariffScheduleUtils.renderTariffScheduleTable(
						messageSource, schedule, now, DEFAULT_EVALUATOR, false, Locale.getDefault(),
						buf);
				TariffScheduleUtils.renderActiveTariffList(messageSource, active, now, buf);
			}
		} else {
			buf.append("<p>").append(messageSource.getMessage("schedule.none", null, null))
					.append("</p>");
		}
		if ( cached != null ) {
			buf.append("<p>");
			buf.append(messageSource.getMessage(cached.isValid() ? "cached.valid" : "cached.invalid",
					new Object[] { new Date(cached.getCreated()), new Date(cached.getExpires()) },
					null));
			buf.append("</p>");
		}
		return buf.toString();
	}

	/**
	 * Get the executor.
	 *
	 * @return the executor
	 */
	public final Executor getExecutor() {
		return executor;
	}

	/**
	 * Set the executor.
	 *
	 * @param executor
	 *        the executor to set
	 */
	public final void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * An optional HTTP request customizer service.
	 *
	 * @return the service
	 */
	public OptionalFilterableService<HttpRequestCustomizerService> getHttpRequestCustomizer() {
		return httpRequestCustomizer;
	}

	/**
	 * An optional HTTP request customizer service.
	 *
	 * <p>
	 * If a {@link #getPlaceholderService()} is configured, all placeholder
	 * values will be provided to the customizer as parameters to the
	 * {@link HttpRequestCustomizerService#customize(org.springframework.http.HttpRequest, net.solarnetwork.util.ByteList, Map)}
	 * method.
	 * </p>
	 *
	 * @param httpRequestCustomizer
	 *        the service to set
	 */
	public void setHttpRequestCustomizer(
			OptionalFilterableService<HttpRequestCustomizerService> httpRequestCustomizer) {
		this.httpRequestCustomizer = httpRequestCustomizer;
	}

	/**
	 * Get the UID of the {@code HttpRequestCustomizerService} service to use.
	 *
	 * @return the service UID
	 */
	public String getHttpRequestCustomizerUid() {
		final OptionalFilterableService<HttpRequestCustomizerService> s = getHttpRequestCustomizer();
		return (s != null ? s.getPropertyValue(UID_PROPERTY) : null);
	}

	/**
	 * Set the UID of the {@code HttpRequestCustomizerService} service to use.
	 *
	 * @param uid
	 *        the service UID to set
	 */
	public void setHttpRequestCustomizerUid(String uid) {
		final OptionalFilterableService<HttpRequestCustomizerService> s = getHttpRequestCustomizer();
		if ( s != null ) {
			s.setPropertyFilter(UID_PROPERTY, uid);
		}
	}

	/**
	 * Get the URL connection timeout to apply when requesting the data.
	 *
	 * @return the connection timeout, never {@literal null}; defaults to
	 *         {@link #DEFAULT_CONNECTION_TIMEOUT}
	 */
	public Duration getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * Set the URL connection timeout to apply when requesting the data.
	 *
	 * @param connectionTimeout
	 *        the timeout; if {@literal null} then {@code 0} will be used
	 */
	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = (connectionTimeout != null ? connectionTimeout : Duration.ZERO);
	}

	/**
	 * Get the URL connection timeout to apply when requesting the data, in
	 * milliseconds.
	 *
	 * @return the connection timeout in milliseconds; defaults to
	 *         {@link #DEFAULT_CONNECTION_TIMEOUT}
	 */
	public long getConnectionTimeoutMs() {
		final Duration d = getConnectionTimeout();
		return d.toMillis();
	}

	/**
	 * Set the URL connection timeout to apply when requesting the data, in
	 * milliseconds.
	 *
	 * @param connectionTimeout
	 *        the timeout, in milliseconds
	 */
	public void setConnectionTimeoutMs(long connectionTimeout) {
		setConnectionTimeout(Duration.ofMillis(connectionTimeout));
	}

	/**
	 * Get the reading mode.
	 *
	 * @return {@literal true} if the {@link #STREAM_READING_PATH} query API
	 *         should be used, or {@literal false} to use
	 *         {@link #STREAM_DATUM_PATH}; defaults to
	 *         {@link #DEFAULT_READING_MODE}
	 */
	public final boolean isReadingMode() {
		return readingMode;
	}

	/**
	 * Set the reading mode.
	 *
	 * @param readingMode
	 *        {@literal true} if the {@link #STREAM_READING_PATH} query API
	 *        should be used, or {@literal false} to use
	 *        {@link #STREAM_DATUM_PATH}
	 */
	public final void setReadingMode(boolean readingMode) {
		this.readingMode = readingMode;
	}

	/**
	 * Get the tariff schedule cache time-to-live.
	 *
	 * @return the TTL, never {@literal null}
	 */
	public final Duration getTariffScheduleCacheTtl() {
		return tariffScheduleCacheTtl;
	}

	/**
	 * Set the tariff schedule cache time-to-live.
	 *
	 * @param tariffScheduleCacheTtl
	 *        the TTL to set
	 */
	public final void setTariffScheduleCacheTtl(Duration tariffScheduleCacheTtl) {
		this.tariffScheduleCacheTtl = (tariffScheduleCacheTtl != null ? tariffScheduleCacheTtl
				: Duration.ZERO);
	}

	/**
	 * Get the tariff schedule cache time-to-live, in seconds.
	 *
	 * @return the TTL in seconds
	 */
	public final long getTariffScheduleCacheTtlSecs() {
		return tariffScheduleCacheTtl.getSeconds();
	}

	/**
	 * Set the tariff schedule cache time-to-live, in seconds.
	 *
	 * @param seconds
	 *        the TTL to set, in seconds
	 */
	public final void setTariffScheduleCacheTtlSecs(long seconds) {
		this.tariffScheduleCacheTtl = Duration.ofSeconds(seconds);
	}

	/**
	 * Get the public mode.
	 *
	 * @return {@literal true} to query with the {@code /pub} API style that
	 *         does not require authentication, {@literal false} to query with
	 *         {@code /sec} style; defaults to {@link #DEFAULT_PUBLIC_MODE}
	 */
	public final boolean isPublicMode() {
		return publicMode;
	}

	/**
	 * Set the public mode.
	 *
	 * @param publicMode
	 *        {@literal true} to query with the {@code /pub} API style that does
	 *        not require authentication, {@literal false} to query with
	 *        {@code /sec} style
	 */
	public final void setPublicMode(boolean publicMode) {
		this.publicMode = publicMode;
	}

	/**
	 * Get the custom node ID to query.
	 *
	 * @return the node ID, or {@literal null} to use the current node ID
	 */
	public final Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the custom node ID to query.
	 *
	 * @param nodeId
	 *        the node ID to set, or {@literal null} to use the current node ID
	 */
	public final void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the source ID to query.
	 *
	 * @return the source ID
	 */
	public final String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to query.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the aggregation to query.
	 *
	 * @return the aggregation; defaults to {@link #DEFAULT_AGGREGATION}
	 */
	public final Aggregation getAggregation() {
		return aggregation;
	}

	/**
	 * Set the aggregation to query.
	 *
	 * <p>
	 * This also determines the tariff schedule duration used.
	 * </p>
	 *
	 * @param aggregation
	 *        the aggregation to set; if {@literal null} then
	 *        {@link #DEFAULT_AGGREGATION} will be used
	 */
	public final void setAggregation(Aggregation aggregation) {
		this.aggregation = (aggregation != null ? aggregation : DEFAULT_AGGREGATION);
	}

	/**
	 * Get the aggregation to query, as a key value.
	 *
	 * @return the aggregation key; defaults to {@link #DEFAULT_AGGREGATION}
	 */
	public final String getAggregationKey() {
		return aggregation.getKey();
	}

	/**
	 * Set the aggregation to query, as a key value.
	 *
	 * @param aggregation
	 *        the aggregation key to set; if {@literal null} then
	 *        {@link #DEFAULT_AGGREGATION} will be used
	 */
	public final void setAggregationKey(String aggregation) {
		Aggregation agg;
		try {
			agg = Aggregation.forKey(aggregation);
		} catch ( IllegalArgumentException e ) {
			agg = null;
		}
		setAggregation(agg);
	}

	/**
	 * Get the datum stream property names to use for tariff rates.
	 *
	 * @return the datum stream property names to use
	 */
	public final Set<String> getDatumStreamPropertyNames() {
		return datumStreamPropertyNames;
	}

	/**
	 * Set the datum stream property names to use for tariff rates.
	 *
	 * @param datumStreamPropertyNames
	 *        the datum stream property names to use
	 */
	public final void setDatumStreamPropertyNames(Set<String> datumStreamPropertyNames) {
		this.datumStreamPropertyNames = datumStreamPropertyNames;
	}

	/**
	 * Get the datum stream property names to use for tariff rates as a
	 * comma-delimited string.
	 *
	 * @return the datum stream property names to use, as a comma-delimited
	 *         string
	 */
	public final String getDatumStreamPropertyNamesValue() {
		return StringUtils.commaDelimitedStringFromCollection(datumStreamPropertyNames);
	}

	/**
	 * Set the datum stream property names to use for tariff rates as a
	 * comma-delimited string..
	 *
	 * @param datumStreamPropertyNames
	 *        the datum stream property names to use, as a comma-delimited
	 *        string
	 */
	public final void setDatumStreamPropertyNamesValue(String datumStreamPropertyNames) {
		setDatumStreamPropertyNames(StringUtils.commaDelimitedStringToSet(datumStreamPropertyNames));
	}

	/**
	 * Get the absolute start date to query.
	 *
	 * @return the date
	 */
	public final LocalDateTime getStartDate() {
		return startDate;
	}

	/**
	 * Set the absolute start date to query.
	 *
	 * @param startDate
	 *        the date to set
	 */
	public final void setStartDate(LocalDateTime startDate) {
		this.startDate = startDate;
	}

	/**
	 * Get the absolute start date to query as an ISO 8601 string.
	 *
	 * @return the date
	 */
	public final String getStartDateValue() {
		final LocalDateTime date = getStartDate();
		return (date != null ? DateUtils.ISO_DATE_OPT_TIME_ALT_LOCAL.format(date) : null);
	}

	/**
	 * Set the absolute start date to query as an ISO 8601 string.
	 *
	 * @param startDate
	 *        the date to set
	 */
	public final void setStartDateValue(String startDate) {
		LocalDateTime date = null;
		try {
			TemporalAccessor ta = DateUtils.ISO_DATE_OPT_TIME_ALT_LOCAL.parse(startDate);
			date = LocalDateTime.from(ta);
		} catch ( DateTimeParseException e ) {
			log.warn("Invalid startDate value [{}], ignoring.", startDate);
		}
		setStartDate(date);
	}

	/**
	 * Get the absolute end date to query.
	 *
	 * @return the date
	 */
	public final LocalDateTime getEndDate() {
		return endDate;
	}

	/**
	 * Set the absolute end date to query.
	 *
	 * @param endDate
	 *        the date to set
	 */
	public final void setEndDate(LocalDateTime endDate) {
		this.endDate = endDate;
	}

	/**
	 * Get the absolute end date to query as an ISO 8601 string.
	 *
	 * @return the date
	 */
	public final String getEndDateValue() {
		final LocalDateTime date = getEndDate();
		return (date != null ? DateUtils.ISO_DATE_OPT_TIME_ALT_LOCAL.format(date) : null);
	}

	/**
	 * Set the absolute end date to query as an ISO 8601 string.
	 *
	 * @param endDate
	 *        the date to set
	 */
	public final void setEndDateValue(String endDate) {
		LocalDateTime date = null;
		try {
			TemporalAccessor ta = DateUtils.ISO_DATE_OPT_TIME_ALT_LOCAL.parse(endDate);
			date = LocalDateTime.from(ta);
		} catch ( DateTimeParseException e ) {
			log.warn("Invalid endDate value [{}], ignoring.", endDate);
		}
		setEndDate(date);
	}

	/**
	 * Get the start date offset.
	 *
	 * @return the offset
	 */
	public final TemporalAmount getStartDateOffset() {
		return startDateOffset;
	}

	/**
	 * Set the start date offset.
	 *
	 * @param startDateOffset
	 *        the offset to set
	 */
	public final void setStartDateOffset(TemporalAmount startDateOffset) {
		this.startDateOffset = startDateOffset;
	}

	/**
	 * Get the start date offset, as an ISO 8601 duration or period.
	 *
	 * @return the offset
	 */
	public final String getStartDateOffsetValue() {
		final TemporalAmount t = getStartDateOffset();
		return (t != null ? t.toString() : null);
	}

	/**
	 * Set the start date offset, as an ISO 8601 duration or period.
	 *
	 * @param startDateOffset
	 *        the offset to set
	 * @see DatumDateFunctions#duration(String)
	 */
	public final void setStartDateOffsetValue(String startDateOffset) {
		TemporalAmount t = null;
		if ( startDateOffset != null && !startDateOffset.isEmpty() ) {
			try {
				t = duration(startDateOffset);
			} catch ( IllegalArgumentException e ) {
				log.warn("Invalid startDateOffset value [{}], ignoring.", startDateOffset);
			}
		}
		setStartDateOffset(t);
	}

	/**
	 * Get the end date offset.
	 *
	 * @return the offset
	 */
	public final TemporalAmount getEndDateOffset() {
		return endDateOffset;
	}

	/**
	 * Set the end date offset.
	 *
	 * @param endDateOffset
	 *        the offset to set
	 */
	public final void setEndDateOffset(TemporalAmount endDateOffset) {
		this.endDateOffset = endDateOffset;
	}

	/**
	 * Get the end date offset, as an ISO 8601 duration or period.
	 *
	 * @return the offset
	 */
	public final String getEndDateOffsetValue() {
		final TemporalAmount t = getEndDateOffset();
		return (t != null ? t.toString() : null);
	}

	/**
	 * Set the end date offset, as an ISO 8601 duration or period.
	 *
	 * @param endDateOffset
	 *        the offset to set
	 * @see DatumDateFunctions#duration(String)
	 */
	public final void setEndDateOffsetValue(String endDateOffset) {
		TemporalAmount t = null;
		if ( endDateOffset != null && !endDateOffset.isEmpty() ) {
			try {
				t = duration(endDateOffset);
			} catch ( IllegalArgumentException e ) {
				log.warn("Invalid endDateOffset value [{}], ignoring.", endDateOffset);
			}
		}
		setEndDateOffset(t);
	}

	/**
	 * Get the unit to truncate the start date offset by.
	 *
	 * @return the unit, or {@literal null} for no truncation
	 */
	public final TemporalUnit getStartDateOffsetTruncateUnit() {
		return startDateOffsetTruncateUnit;
	}

	/**
	 * Set the unit to truncate the start date offset by.
	 *
	 * @param startDateOffsetTruncateUnit
	 *        the unit to set, or {@literal null} for no truncation
	 */
	public final void setStartDateOffsetTruncateUnit(TemporalUnit startDateOffsetTruncateUnit) {
		this.startDateOffsetTruncateUnit = startDateOffsetTruncateUnit;
	}

	/**
	 * Get the unit to truncate the start date offset by, as a
	 * {@link ChronoUnit} name.
	 *
	 * @return the unit, or {@literal null} for no truncation
	 */
	public final String getStartDateOffsetTruncateUnitValue() {
		TemporalUnit u = getStartDateOffsetTruncateUnit();
		return (u != null ? u.toString() : null);
	}

	/**
	 * Set the unit to truncate the start date offset by.
	 *
	 * @param startDateOffsetTruncateUnit
	 *        the unit to set, or {@literal null} for no truncation
	 */
	public final void setStartDateOffsetTruncateUnitValue(String startDateOffsetTruncateUnit) {
		TemporalUnit u = null;
		try {
			u = chronoUnit(startDateOffsetTruncateUnit);
		} catch ( IllegalArgumentException e ) {
			log.warn("Invalid startDateOffsetTruncateUnit value [{}], ignoring.",
					startDateOffsetTruncateUnit);
		}
		setStartDateOffsetTruncateUnit(u);
	}

	/**
	 * Get the unit to truncate the end date offset by.
	 *
	 * @return the unit or {@literal null} for no truncation
	 */
	public final TemporalUnit getEndDateOffsetTruncateUnit() {
		return endDateOffsetTruncateUnit;
	}

	/**
	 * Set the unit to truncate the end date offset by.
	 *
	 * @param endDateOffsetTruncateUnit
	 *        the unit to set, or {@literal null} for no truncation
	 */
	public final void setEndDateOffsetTruncateUnit(TemporalUnit endDateOffsetTruncateUnit) {
		this.endDateOffsetTruncateUnit = endDateOffsetTruncateUnit;
	}

	/**
	 * Get the unit to truncate the end date offset by, as a {@link ChronoUnit}
	 * name.
	 *
	 * @return the unit, or {@literal null} for no truncation
	 */
	public final String getEndDateOffsetTruncateUnitValue() {
		TemporalUnit u = getEndDateOffsetTruncateUnit();
		return (u != null ? u.toString() : null);
	}

	/**
	 * Set the unit to truncate the end date offset by.
	 *
	 * @param endDateOffsetTruncateUnit
	 *        the unit to set, or {@literal null} for no truncation
	 */
	public final void setEndDateOffsetTruncateUnitValue(String endDateOffsetTruncateUnit) {
		TemporalUnit u = null;
		try {
			u = chronoUnit(endDateOffsetTruncateUnit);
		} catch ( IllegalArgumentException e ) {
			log.warn("Invalid endDateOffsetTruncateUnit value [{}], ignoring.",
					endDateOffsetTruncateUnit);
		}
		setEndDateOffsetTruncateUnit(u);
	}

	/**
	 * Get the explicit time zone to interpret the datum stream as.
	 *
	 * @return the zone, or {@literal null} to use the system zone
	 */
	public final ZoneId getTimeZone() {
		return timeZone;
	}

	/**
	 * Set the explicit time zone to interpret the datum stream as.
	 *
	 * @param timeZone
	 *        the zone to set, or {@literal null} to use the system zone
	 */
	public final void setTimeZone(ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Get the explicit time zone to interpret the datum stream as, as a zone ID
	 * value.
	 *
	 * @return the zone ID, or {@literal null} to use the system zone
	 */
	public final String getTimeZoneId() {
		ZoneId zone = getTimeZone();
		return (zone != null ? zone.getId() : null);
	}

	/**
	 * Set the explicit time zone to interpret the datum stream as, as a zone ID
	 * value.
	 *
	 * @param timeZoneId
	 *        the zone ID to set, or {@literal null} to use the system zone
	 */
	public final void setTimeZoneId(String timeZoneId) {
		ZoneId zone = null;
		if ( timeZoneId != null && !timeZoneId.isEmpty() ) {
			try {
				zone = ZoneId.of(timeZoneId);
			} catch ( DateTimeException e ) {
				log.warn("Invalid timeZoneId value [{}], ignoring.", timeZoneId);
			}
		}
		setTimeZone(zone);
	}

}
