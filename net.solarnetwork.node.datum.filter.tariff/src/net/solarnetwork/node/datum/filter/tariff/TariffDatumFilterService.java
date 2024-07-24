/* ==================================================================
 * TariffDatumFilterService.java - 12/05/2021 6:58:01 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.tariff;

import static java.time.format.TextStyle.SHORT;
import static net.solarnetwork.domain.tariff.SimpleTemporalRangesTariffEvaluator.DEFAULT_EVALUATOR;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.DateUtils.formatRange;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.tariff.CompositeTariff;
import net.solarnetwork.domain.tariff.CsvTemporalRangeTariffParser;
import net.solarnetwork.domain.tariff.SimpleTariffRate;
import net.solarnetwork.domain.tariff.SimpleTemporalTariffSchedule;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.Tariff.Rate;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TemporalRangesTariff;
import net.solarnetwork.domain.tariff.TemporalRangesTariffEvaluator;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.CachedResult;

/**
 * Transform service that can resolve a time-of-use based tarrif from
 * spreadsheet style tariff metadata.
 *
 * @author matt
 * @version 1.4
 * @since 2.0
 */
public class TariffDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider, SettingsChangeObserver {

	/** The {@code tariffMetadataPath} default value. */
	public static final String DEFAULT_TARIFF_METADATA_PATH = "/pm/tariffs/schedule";

	/** The {@code scheduleCacheSeconds} default value (12 hours). */
	public static final int DEFAULT_SCHEDULE_CACHE_SECONDS = 60 * 60 * 12;

	private final OptionalFilterableService<MetadataService> metadataService;
	private final OptionalFilterableService<TemporalRangesTariffEvaluator> evaluator;
	private String tariffMetadataPath = DEFAULT_TARIFF_METADATA_PATH;
	private Locale locale = Locale.getDefault();
	private int scheduleCacheSeconds = DEFAULT_SCHEDULE_CACHE_SECONDS;
	private boolean firstMatchOnly = SimpleTemporalTariffSchedule.DEFAULT_FIRST_MATCH_ONLY;
	private boolean preserveRateCase;

	private final AtomicReference<CachedResult<TariffSchedule>> schedule = new AtomicReference<>();

	/**
	 * Constructor.
	 *
	 * @param metadataService
	 *        the metadata service
	 * @param evaluator
	 *        the tariff evaluator
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TariffDatumFilterService(OptionalFilterableService<MetadataService> metadataService,
			OptionalFilterableService<TemporalRangesTariffEvaluator> evaluator) {
		super();
		if ( metadataService == null ) {
			throw new IllegalArgumentException("The metadataService argument must not be null.");
		}
		this.metadataService = metadataService;
		if ( evaluator == null ) {
			throw new IllegalArgumentException("The evaluator argument must not be null.");
		}
		this.evaluator = evaluator;
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		if ( !conditionsMatch(datum, samples, parameters) ) {
			return samples;
		}
		final LocalDateTime date = (datum.getTimestamp() != null
				? LocalDateTime.ofInstant(datum.getTimestamp(), ZoneId.systemDefault())
				: LocalDateTime.now());
		Tariff tariff = resolveTariff(date, parameters);

		if ( tariff != null ) {
			DatumSamples s = new DatumSamples(samples);
			for ( Rate rate : tariff.getRates().values() ) {
				String id = rate.getId();
				if ( !s.hasSampleValue(DatumSamplesType.Instantaneous, id) ) {
					log.debug("Populating tariff property [{}] with {} for datum {}", id,
							rate.getAmount(), datum);
					s.putSampleValue(DatumSamplesType.Instantaneous, id, rate.getAmount());
				} else {
					log.debug(
							"Instantenous property [{}] already exists for datum {}, will not populate tariff rate {}",
							id, datum, rate.getAmount());
				}
			}
			if ( !s.equals(samples) ) {
				return s;
			}
		}
		return samples;
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		if ( properties != null ) {
			// remove cached schedule
			schedule.set(null);
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.tariff";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		result.add(0, new BasicTitleSettingSpecifier("status", getStatusMessage(), true, true));
		populateBaseSampleTransformSupportSettings(result);
		result.add(new BasicTextFieldSettingSpecifier("metadataServiceUid", null, false,
				"(objectClass=net.solarnetwork.node.service.MetadataService)"));
		result.add(
				new BasicTextFieldSettingSpecifier("tariffMetadataPath", DEFAULT_TARIFF_METADATA_PATH));
		result.add(new BasicToggleSettingSpecifier("firstMatchOnly",
				SimpleTemporalTariffSchedule.DEFAULT_FIRST_MATCH_ONLY));
		result.add(new BasicToggleSettingSpecifier("preserveRateCase", Boolean.FALSE));
		result.add(new BasicTextFieldSettingSpecifier("scheduleCacheSeconds",
				String.valueOf(DEFAULT_SCHEDULE_CACHE_SECONDS)));
		result.add(new BasicTextFieldSettingSpecifier("evaluatorUid", null, false,
				"(objectClass=net.solarnetwork.domain.tariff.TemporalRangesTariffEvaluator)"));
		result.add(new BasicTextFieldSettingSpecifier("language", null));
		return result;
	}

	@Override
	protected String getStatusMessage() {
		StringBuilder buf = new StringBuilder();
		TariffSchedule schedule = schedule();
		CachedResult<TariffSchedule> cached = this.schedule.get();
		MessageSource messageSource = getMessageSource();
		if ( schedule instanceof SimpleTemporalTariffSchedule ) {
			List<TemporalRangesTariff> rules = ((SimpleTemporalTariffSchedule) schedule).getRules();
			if ( rules.isEmpty() ) {
				buf.append("<p>").append(messageSource.getMessage("rules.empty", null, null))
						.append("</p>");
			} else {
				final LocalDateTime now = LocalDateTime.now();
				Map<Integer, TemporalRangesTariff> active = renderRulesTable(
						(SimpleTemporalTariffSchedule) schedule, now, buf);
				if ( !active.isEmpty() ) {
					Map<String, Rate> activeRates = new CompositeTariff(active.values()).getRates();
					DateTimeFormatter dateFormat = DateTimeFormatter
							.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);
					buf.append("<p>").append(messageSource.getMessage("rates.active",
							new Object[] { dateFormat.format(now) }, null)).append("</p><ol>");
					for ( Map.Entry<Integer, TemporalRangesTariff> me : active.entrySet() ) {
						buf.append("<li value=\"").append(me.getKey() + 1).append("\">");
						int rateCount = 0;
						for ( Rate rate : me.getValue().getRates().values() ) {
							if ( rate == activeRates.get(rate.getId()) ) {
								// this rate active for this rule
								if ( rateCount++ > 0 ) {
									buf.append("; ");
								}
								buf.append("<b>").append(rate.getDescription()).append("</b>: ")
										.append(rate.getAmount().toPlainString());
							}
							buf.append("</li>");
						}
					}
					buf.append("</ol>");
				}
			}
		} else if ( schedule == null ) {
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

	private Map<Integer, TemporalRangesTariff> renderRulesTable(SimpleTemporalTariffSchedule schedule,
			LocalDateTime date, StringBuilder buf) {
		final List<TemporalRangesTariff> tariffs = schedule.getRules();
		final Map<Integer, TemporalRangesTariff> active = new TreeMap<>();
		final TemporalRangesTariffEvaluator e = evaluator();
		final boolean firstOnly = isFirstMatchOnly();
		final CompositeTariff ct = new CompositeTariff(tariffs);
		final Map<String, Rate> rates = ct.getRates();
		buf.append(
				"<table class=\"table counts\"><thead><tr><th>Rule</th><th>Month</th><th>Day</th><th>Weekday</th><th>Time</th>");
		for ( Rate r : rates.values() ) {
			buf.append("<th>").append(r.getDescription()).append("</th>");
		}
		buf.append("</tr></thead><tbody>");

		int i = 0;
		for ( TemporalRangesTariff tariff : tariffs ) {
			if ( (active.isEmpty() || !firstOnly) && tariff.applies(e, date, null) ) {
				active.put(i, tariff);
			}
			buf.append("<tr>");
			buf.append("<th>").append(++i).append("</th>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.MONTH_OF_YEAR, tariff))
					.append("</td>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.DAY_OF_MONTH, tariff))
					.append("</td>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.DAY_OF_WEEK, tariff))
					.append("</td>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.MINUTE_OF_DAY, tariff))
					.append("</td>");
			Map<String, Rate> tariffRates = tariff.getRates();
			// iterate over global rates, to keep order consistent in case rows vary
			for ( String id : rates.keySet() ) {
				Rate r = tariffRates.get(id);
				buf.append("<td>");
				if ( r != null ) {
					buf.append(r.getAmount().toPlainString());
				}
				buf.append("</td>");
			}
			buf.append("</tr>");
		}
		buf.append("</tbody></table>");
		return active;
	}

	private String rangeDisplayString(ChronoField field, TemporalRangesTariff tariff) {
		String r = formatRange(field, tariff.rangeForField(field), locale, SHORT);
		return (r != null ? r : "*");
	}

	private Tariff resolveTariff(LocalDateTime date, Map<String, ?> parameters) {
		TariffSchedule schedule = schedule();
		if ( schedule == null ) {
			log.debug("No TariffSchedule available for filter [{}], unable to resolve tariffs.",
					getUid());
			return null;
		}
		return schedule.resolveTariff(date, parameters);
	}

	private TariffSchedule schedule() {
		CachedResult<TariffSchedule> r = schedule.updateAndGet(c -> {
			if ( c != null && c.isValid() ) {
				return c;
			}
			MetadataService service = service(metadataService);
			if ( service == null ) {
				log.debug(
						"No MetadataService available in tariff filter [{}], unable to resolve tariff schedule.",
						getUid());
				return null;
			}
			Object o = service.metadataAtPath(tariffMetadataPath);
			if ( o == null ) {
				log.warn(
						"No tariff schedule found in tariff filter [{}] at metadata, path [{}], unable to resolve tariff schedule.",
						getUid(), tariffMetadataPath);
				return null;
			}
			try {
				TariffSchedule s = parseSchedule(o);
				if ( s == null ) {
					return null;
				}
				return new CachedResult<>(s, scheduleCacheSeconds, TimeUnit.SECONDS);
			} catch ( Exception e ) {
				log.warn(
						"Error parsing tariff filter [{}] from metadata at path [{}], unable to resolve tariff schedule: {}",
						getUid(), tariffMetadataPath, e.getMessage());
				return null;
			}
		});
		return (r != null ? r.getResult() : null);
	}

	private TariffSchedule parseSchedule(Object o) throws IOException {
		List<TemporalRangesTariff> tariffs;
		if ( o instanceof String ) {
			// parse as CSV
			tariffs = new CsvTemporalRangeTariffParser(locale, preserveRateCase)
					.parseTariffs(new StringReader(o.toString()));
		} else if ( o instanceof String[][] ) {
			tariffs = new ArrayList<>();
			String[][] data = (String[][]) o;
			if ( data.length > 1 ) {
				String[] headers = data[0];
				if ( headers.length < 5 ) {
					return null;
				}
				for ( int i = 1; i < data.length; i++ ) {
					String[] row = data[i];
					if ( row.length < 5 ) {
						continue;
					}
					List<Tariff.Rate> rates = new ArrayList<>(row.length - 4);
					for ( int j = 4; j < row.length; j++ ) {
						rates.add(new SimpleTariffRate(headers[j], new BigDecimal(row[j])));
					}
					TemporalRangesTariff tariff = new TemporalRangesTariff(row[0], row[1], row[2],
							row[3], rates, locale);
					tariffs.add(tariff);
				}
			}
		} else {
			return null;
		}
		TemporalRangesTariffEvaluator e = evaluator();
		SimpleTemporalTariffSchedule s = new SimpleTemporalTariffSchedule(tariffs, e);
		s.setFirstMatchOnly(firstMatchOnly);
		return s;
	}

	private TemporalRangesTariffEvaluator evaluator() {
		return service(evaluator, DEFAULT_EVALUATOR);
	}

	/**
	 * Get the tariff metadata path.
	 *
	 * @return the path; defaults to {@link #DEFAULT_TARIFF_METADATA_PATH}
	 */
	public final String getTariffMetadataPath() {
		return tariffMetadataPath;
	}

	/**
	 * Set the tariff metadata path.
	 *
	 * <p>
	 * The path must resolve to either a string value or a two-dimensional array
	 * of strings. If a string, it will be assumed to be a CSV formatted
	 * two-dimensional array of strings.
	 * </p>
	 *
	 * @param tariffMetadataPath
	 *        the path to set
	 */
	public final void setTariffMetadataPath(String tariffMetadataPath) {
		this.tariffMetadataPath = tariffMetadataPath;
	}

	/**
	 * Get the tariff schedule cache seconds.
	 *
	 * @return the maximum number of seconds to cache the resolved tariff
	 *         schedule; defaults to {@link #DEFAULT_SCHEDULE_CACHE_SECONDS}
	 */
	public final int getScheduleCacheSeconds() {
		return scheduleCacheSeconds;
	}

	/**
	 * Set the tariff schedule cache seconds.
	 *
	 * @param scheduleCacheSeconds
	 *        the seconds to set
	 */
	public final void setScheduleCacheSeconds(int scheduleCacheSeconds) {
		this.scheduleCacheSeconds = scheduleCacheSeconds;
	}

	/**
	 * Get the locale to use for parsing/formatting schedule data.
	 *
	 * @return the locale
	 */
	public final Locale getLocale() {
		return locale;
	}

	/**
	 * Get the locale to use for parsing/formatting schedule data.
	 *
	 * @param locale
	 *        the locale to set
	 */
	public final void setLocale(Locale locale) {
		if ( locale == null ) {
			locale = Locale.getDefault();
		}
		this.locale = locale;
	}

	/**
	 * Get the locale IETF BCP 47 language tag.
	 *
	 * @return the language
	 */
	public final String getLanguage() {
		return getLocale().toLanguageTag();
	}

	/**
	 * Set the locale as a IETF BCP 47 language tag.
	 *
	 * @param lang
	 *        the language tag to set
	 */
	public final void setLanguage(String lang) {
		setLocale(lang != null ? Locale.forLanguageTag(lang) : null);
	}

	/**
	 * Get the first-match-only flag.
	 *
	 * @return {@literal true} if only the first tariff rule that matches should
	 *         be returned, {@literal false} to return a composite rule of all
	 *         matches; defaults to
	 *         {@link SimpleTemporalTariffSchedule#DEFAULT_FIRST_MATCH_ONLY}
	 */
	public final boolean isFirstMatchOnly() {
		return firstMatchOnly;
	}

	/**
	 * Set the first-match-only flag.
	 *
	 * @param firstMatchOnly
	 *        {@literal true} if only the first tariff rule that matches should
	 *        be returned
	 */
	public final void setFirstMatchOnly(boolean firstMatchOnly) {
		this.firstMatchOnly = firstMatchOnly;
	}

	/**
	 * Get the {@link MetadataService} service filter UID.
	 *
	 * @return the service UID
	 */
	public final String getMetadataServiceUid() {
		return metadataService.getPropertyValue(UID_PROPERTY);
	}

	/**
	 * Set the {@link MetadataService} service filter UID.
	 *
	 * @param uid
	 *        the service UID
	 */
	public final void setMetadataServiceUid(String uid) {
		metadataService.setPropertyFilter(UID_PROPERTY, uid);
	}

	/**
	 * Get the {@link TemporalRangesTariffEvaluator} service filter UID.
	 *
	 * @return the service UID
	 */
	public final String getEvaluatorUid() {
		return evaluator.getPropertyValue(UID_PROPERTY);
	}

	/**
	 * Set the {@link TemporalRangesTariffEvaluator} service filter UID.
	 *
	 * @param uid
	 *        the service UID
	 */
	public final void setEvaluatorUid(String uid) {
		evaluator.setPropertyFilter(UID_PROPERTY, uid);
	}

	/**
	 * Get the "preserve rate case" mode.
	 *
	 * @return {@literal true} to preserve the case of rate names
	 * @since 1.2
	 */
	public final boolean isPreserveRateCase() {
		return preserveRateCase;
	}

	/**
	 * Set the "preserve rate case" mode.
	 *
	 * @param preserveRateCase
	 *        {@literal true} to preserve the case of rate names
	 * @since 1.4
	 */
	public final void setPreserveRateCase(boolean preserveRateCase) {
		this.preserveRateCase = preserveRateCase;
	}
}
