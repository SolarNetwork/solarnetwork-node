/* ==================================================================
 * TouDatumDataSource.java - 24/07/2024 5:45:36 pm
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

import static java.time.format.TextStyle.SHORT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static net.solarnetwork.domain.tariff.SimpleTemporalRangesTariffEvaluator.DEFAULT_EVALUATOR;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.tariff.ChronoFieldsTariff;
import net.solarnetwork.domain.tariff.CompositeTariff;
import net.solarnetwork.domain.tariff.SimpleTemporalTariffSchedule;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.Tariff.Rate;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TariffUtils;
import net.solarnetwork.domain.tariff.TemporalRangesTariffEvaluator;
import net.solarnetwork.domain.tariff.TemporalTariffEvaluator;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.CachedResult;

/**
 * {@link DatumDataSource} for time-of-use schedules devices.
 *
 * @author matt
 * @version 1.0
 */
public class TouDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingsChangeObserver, SettingSpecifierProvider {

	/** The {@code scheduleCacheTtl} default value (12 hours). */
	public static final Duration DEFAULT_SCHEDULE_CACHE_TTL = Duration.ofHours(12);

	/** The {@code preserveRateCase} property default value. */
	public static final boolean DEFAULT_PRESERVE_RATE_CASE = true;

	private final AtomicReference<CachedResult<TariffSchedule>> schedule = new AtomicReference<>();

	private final Clock clock;
	private final OptionalFilterableService<MetadataService> metadataService;
	private final OptionalFilterableService<TemporalTariffEvaluator> evaluator;

	private String sourceId;
	private Duration scheduleCacheTtl = DEFAULT_SCHEDULE_CACHE_TTL;
	private String metadataPath;
	private Locale locale = Locale.getDefault();
	private boolean firstMatchOnly = SimpleTemporalTariffSchedule.DEFAULT_FIRST_MATCH_ONLY;
	private boolean preserveRateCase = DEFAULT_PRESERVE_RATE_CASE;
	private TouPropertyConfig[] propertyConfigs;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The system clock will be used.
	 * </p>
	 *
	 * @param metadataService
	 *        the metadata service
	 * @param evaluator
	 *        the evaluator service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TouDatumDataSource(OptionalFilterableService<MetadataService> metadataService,
			OptionalFilterableService<TemporalTariffEvaluator> evaluator) {
		this(Clock.systemUTC(), metadataService, evaluator);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param metadataService
	 *        the metadata service
	 * @param evaluator
	 *        the evaluator service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TouDatumDataSource(Clock clock, OptionalFilterableService<MetadataService> metadataService,
			OptionalFilterableService<TemporalTariffEvaluator> evaluator) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.metadataService = requireNonNullArgument(metadataService, "metadataService");
		this.evaluator = requireNonNullArgument(evaluator, "evaluator");
		setMetadataService(metadataService);
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		if ( properties != null ) {
			// remove cached schedule
			schedule.set(null);
		}
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		final String sourceId = resolvePlaceholders(getSourceId());
		if ( sourceId == null ) {
			return null;
		}
		final TouPropertyConfig[] configs = getPropertyConfigs();
		if ( configs == null || configs.length < 1 ) {
			return null;
		}
		final TariffSchedule schedule = schedule();
		if ( schedule == null ) {
			return null;
		}
		final Instant now = clock.instant();
		final Tariff tariff = schedule.resolveTariff(
				now.atZone(ZoneId.systemDefault()).toLocalDateTime(), Collections.emptyMap());
		if ( tariff == null || tariff.getRates().isEmpty() ) {
			return null;
		}
		final Map<String, Rate> rates = tariff.getRates();
		final DatumSamples samples = new DatumSamples();
		int configNum = 0;
		for ( TouPropertyConfig config : configs ) {
			configNum++;
			if ( !config.isValid() ) {
				continue;
			}
			final Rate r = findRate(rates, config.getRateName());
			if ( r == null ) {
				log.warn(
						"TOU datum source [{}] property config #{} specified rate [{}] is not available. Available rates are: {}",
						getSourceId(), configNum, config.getRateName(), rates.keySet());
				continue;
			}
			samples.putSampleValue(config.getPropertyType(), config.getPropertyKey(),
					config.applyTransformations(r.getAmount()));
		}
		if ( samples.isEmpty() ) {
			return null;
		}
		return SimpleDatum.nodeDatum(sourceId, now, samples);
	}

	private Rate findRate(Map<String, Rate> rates, String rateName) {
		// try exact match first
		Rate r = rates.get(rateName);
		if ( r != null ) {
			return r;
		}
		for ( Entry<String, Rate> e : rates.entrySet() ) {
			if ( rateName.equalsIgnoreCase(e.getKey()) ) {
				return e.getValue();
			}
		}
		return null;
	}

	private TariffSchedule schedule() {
		final String metadataPath = getMetadataPath();
		if ( metadataPath == null || metadataPath.isEmpty() ) {
			return null;
		}
		final MetadataService service = service(getMetadataService());
		if ( service == null ) {
			log.warn(
					"No MetadataService available in TOU datum source [{}], unable to resolve TOU schedule.",
					getSourceId());
			return null;
		}
		CachedResult<TariffSchedule> r = schedule.updateAndGet(c -> {
			if ( c != null && c.isValid() ) {
				return c;
			}
			Object o = service.metadataAtPath(metadataPath);
			if ( o == null ) {
				log.warn(
						"No TOU schedule found in TOU datum source [{}] at metadata path [{}], unable to resolve TOU schedule.",
						getSourceId(), metadataPath);
				return null;
			}
			try {
				TariffSchedule s = TariffUtils.parseCsvTemporalRangeSchedule(locale, preserveRateCase,
						firstMatchOnly, service(evaluator, DEFAULT_EVALUATOR), o);
				if ( s == null ) {
					return null;
				}
				return new CachedResult<>(s, scheduleCacheTtl.getSeconds(), TimeUnit.SECONDS);
			} catch ( Exception e ) {
				log.warn(
						"TOU datum source [{}] error parsing TOU schedule from metadata at path [{}]: {}",
						getSourceId(), metadataPath, e.getMessage(), e);
				return null;
			}
		});
		return (r != null ? r.getResult() : null);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.tou";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		result.add(0, new BasicTitleSettingSpecifier("status", statusMessage(), true, true));
		result.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		result.add(new BasicTextFieldSettingSpecifier("metadataServiceUid", null, false,
				"(objectClass=net.solarnetwork.node.service.MetadataService)"));
		result.add(new BasicTextFieldSettingSpecifier("metadataPath", null));
		result.add(new BasicTextFieldSettingSpecifier("language", null));
		result.add(new BasicToggleSettingSpecifier("firstMatchOnly",
				SimpleTemporalTariffSchedule.DEFAULT_FIRST_MATCH_ONLY));
		result.add(new BasicTextFieldSettingSpecifier("scheduleCacheTtlSecs",
				String.valueOf(DEFAULT_SCHEDULE_CACHE_TTL.getSeconds())));
		result.add(new BasicTextFieldSettingSpecifier("evaluatorUid", null, false,
				"(objectClass=net.solarnetwork.domain.tariff.TemporalRangesTariffEvaluator)"));

		TouPropertyConfig[] propConfs = getPropertyConfigs();
		List<TouPropertyConfig> propConfList = (propConfs != null ? asList(propConfs) : emptyList());
		result.add(SettingUtils.dynamicListSettingSpecifier("propertyConfigs", propConfList,
				new SettingUtils.KeyedListCallback<TouPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(TouPropertyConfig value,
							int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								TouPropertyConfig.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return result;
	}

	private String statusMessage() {
		StringBuilder buf = new StringBuilder();
		TariffSchedule schedule = schedule();
		CachedResult<TariffSchedule> cached = this.schedule.get();
		MessageSource messageSource = getMessageSource();
		if ( schedule != null ) {
			Collection<? extends Tariff> rules = schedule.rules();
			if ( rules.isEmpty() ) {
				buf.append("<p>").append(messageSource.getMessage("rules.empty", null, null))
						.append("</p>");
			} else {
				final LocalDateTime now = LocalDateTime.now();
				Map<Integer, Tariff> active = renderRulesTable(schedule, now, buf);
				if ( !active.isEmpty() ) {
					Map<String, Rate> activeRates = new CompositeTariff(active.values()).getRates();
					DateTimeFormatter dateFormat = DateTimeFormatter
							.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);
					buf.append("<p>").append(messageSource.getMessage("rates.active",
							new Object[] { dateFormat.format(now) }, null)).append("</p><ol>");
					for ( Map.Entry<Integer, Tariff> me : active.entrySet() ) {
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

	private Map<Integer, Tariff> renderRulesTable(TariffSchedule schedule, LocalDateTime date,
			StringBuilder buf) {
		final Collection<? extends Tariff> tariffs = schedule.rules();
		final Map<Integer, Tariff> active = new TreeMap<>();
		final TemporalTariffEvaluator e = service(evaluator, DEFAULT_EVALUATOR);
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
		for ( Tariff tariff : tariffs ) {
			if ( !(tariff instanceof ChronoFieldsTariff) ) {
				continue;
			}
			ChronoFieldsTariff t = (ChronoFieldsTariff) tariff;
			if ( (active.isEmpty() || !firstOnly) && e.applies(t, date, null) ) {
				active.put(i, tariff);
			}
			buf.append("<tr>");
			buf.append("<th>").append(++i).append("</th>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.MONTH_OF_YEAR, t)).append("</td>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.DAY_OF_MONTH, t)).append("</td>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.DAY_OF_WEEK, t)).append("</td>");
			buf.append("<td>").append(rangeDisplayString(ChronoField.MINUTE_OF_DAY, t)).append("</td>");
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

	private String rangeDisplayString(ChronoField field, ChronoFieldsTariff tariff) {
		String r = tariff.formatChronoField(field, locale, SHORT);
		return (r != null ? r : "*");
	}

	/**
	 * Get the source ID to assign to generated datum.
	 *
	 * @return the source ID to use
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to assign to generated datum.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
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
	 * Get the tariff metadata path.
	 *
	 * @return the path
	 */
	public final String getMetadataPath() {
		return metadataPath;
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
	 * @param metadataPath
	 *        the path to set
	 */
	public final void setMetadataPath(String metadataPath) {
		this.metadataPath = metadataPath;
	}

	/**
	 * Get the schedule cache time-to-live.
	 *
	 * @return the TTL
	 */
	public final Duration getScheduleCacheTtl() {
		return scheduleCacheTtl;
	}

	/**
	 * Set the schedule cache time-to-live.
	 *
	 * @param scheduleCacheTtl
	 *        the TTL to set
	 */
	public final void setScheduleCacheTtl(Duration scheduleCacheTtl) {
		this.scheduleCacheTtl = scheduleCacheTtl;
	}

	/**
	 * Get the schedule cache time-to-live, in seconds.
	 *
	 * @return the TTL in seconds
	 */
	public final long getScheduleCacheTtlSecs() {
		return scheduleCacheTtl.getSeconds();
	}

	/**
	 * Set the schedule cache time-to-live, in seconds.
	 *
	 * @param seconds
	 *        the TTL to set, in seconds
	 */
	public final void setScheduleCacheTtlSecs(long seconds) {
		this.scheduleCacheTtl = Duration.ofSeconds(seconds);
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
	 * @return {@literal true} to preserve the case of rate names; defaults to
	 *         {@link #DEFAULT_PRESERVE_RATE_CASE}
	 */
	public final boolean isPreserveRateCase() {
		return preserveRateCase;
	}

	/**
	 * Set the "preserve rate case" mode.
	 *
	 * @param preserveRateCase
	 *        {@literal true} to preserve the case of rate names
	 */
	public final void setPreserveRateCase(boolean preserveRateCase) {
		this.preserveRateCase = preserveRateCase;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the property configurations
	 */
	public TouPropertyConfig[] getPropertyConfigs() {
		return propertyConfigs;
	}

	/**
	 * Set the property configurations to use.
	 *
	 * @param propertyConfigs
	 *        the configurations to use
	 */
	public void setPropertyConfigs(TouPropertyConfig[] propertyConfigs) {
		this.propertyConfigs = propertyConfigs;
	}

	/**
	 * Get the number of configured {@code propertyConfigs} elements.
	 *
	 * @return the number of {@code propertyConfigs} elements
	 */
	public int getPropertyConfigsCount() {
		TouPropertyConfig[] confs = this.propertyConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propertyConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link TouPropertyConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        the desired number of {@code propertyConfigs} elements
	 */
	public void setPropertyConfigsCount(int count) {
		this.propertyConfigs = ArrayUtils.arrayWithLength(this.propertyConfigs, count,
				TouPropertyConfig.class, TouPropertyConfig::new);
	}

}
