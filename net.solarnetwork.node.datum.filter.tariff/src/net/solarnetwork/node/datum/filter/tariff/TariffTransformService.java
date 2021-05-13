/* ==================================================================
 * TariffTransformService.java - 12/05/2021 6:58:01 AM
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

import static net.solarnetwork.domain.tariff.SimpleTemporalRangesTariffEvaluator.DEFAULT_EVALUATOR;
import static net.solarnetwork.util.OptionalService.service;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.tariff.CsvTemporalRangeTariffParser;
import net.solarnetwork.domain.tariff.SimpleTariffRate;
import net.solarnetwork.domain.tariff.SimpleTemporalTariffSchedule;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.Tariff.Rate;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TemporalRangesTariff;
import net.solarnetwork.domain.tariff.TemporalRangesTariffEvaluator;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.MetadataService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.support.BaseSamplesTransformSupport;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.OptionalService.OptionalFilterableService;

/**
 * Transform service that can resolve a time-of-use based tarrif from
 * spreadsheet style tariff metadata.
 * 
 * @author matt
 * @version 1.0
 */
public class TariffTransformService extends BaseSamplesTransformSupport implements
		GeneralDatumSamplesTransformService, SettingSpecifierProvider, SettingsChangeObserver {

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
	public TariffTransformService(OptionalFilterableService<MetadataService> metadataService,
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
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, Object> parameters) {
		if ( !sourceIdMatches(datum) ) {
			return samples;
		}
		final LocalDateTime now = (datum.getCreated() != null
				? LocalDateTime.ofInstant(datum.getCreated().toInstant(), ZoneId.systemDefault())
				: LocalDateTime.now());
		Tariff tariff = resolveTariff(now, parameters);

		if ( tariff != null ) {
			GeneralDatumSamples s = new GeneralDatumSamples(samples);
			for ( Rate rate : tariff.getRates().values() ) {
				String id = rate.getId();
				if ( !s.hasSampleValue(GeneralDatumSamplesType.Instantaneous, id) ) {
					log.debug("Populating tariff property [{}] with {} for datum {}", id,
							rate.getAmount(), datum);
					s.putSampleValue(GeneralDatumSamplesType.Instantaneous, id, rate.getAmount());
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
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.filter.tariff";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		// TODO Auto-generated method stub
		return null;
	}

	private Tariff resolveTariff(LocalDateTime date, Map<String, ?> parameters) {
		TariffSchedule schedule = schedule();
		if ( schedule == null ) {
			log.debug("No TariffSchedule available for filter {}, unable to resolve tariffs.", getUid());
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
						"No MetadataService available in tariff filter {}, unable to resolve tariff schedule.",
						getUid());
				return null;
			}
			Object o = service.metadataAtPath(tariffMetadataPath);
			if ( o == null ) {
				log.warn(
						"No tariff schedule found in tariff filter {} at metadata, path [{}], unable to resolve tariff schedule.",
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
						"Error parsing tariff filter {} from metadata at path [{}], unable to resolve tariff schedule.",
						getUid(), tariffMetadataPath);
				return null;
			}
		});
		return (r != null ? r.getResult() : null);
	}

	private TariffSchedule parseSchedule(Object o) throws IOException {
		List<TemporalRangesTariff> tariffs;
		if ( o instanceof String ) {
			// parse as CSV
			tariffs = new CsvTemporalRangeTariffParser(locale)
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
		TemporalRangesTariffEvaluator e = service(evaluator, DEFAULT_EVALUATOR);
		SimpleTemporalTariffSchedule s = new SimpleTemporalTariffSchedule(tariffs, e);
		s.setFirstMatchOnly(firstMatchOnly);
		return s;
	}

	/**
	 * Get the tariff metadata path.
	 * 
	 * @return the path; defaults to {@link #DEFAULT_TARIFF_METADATA_PATH}
	 */
	public String getTariffMetadataPath() {
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
	public void setTariffMetadataPath(String tariffMetadataPath) {
		this.tariffMetadataPath = tariffMetadataPath;
	}

	/**
	 * Get the tariff schedule cache seconds.
	 * 
	 * @return the maximum number of seconds to cache the resolved tariff
	 *         schedule; defaults to {@link #DEFAULT_SCHEDULE_CACHE_SECONDS}
	 */
	public int getScheduleCacheSeconds() {
		return scheduleCacheSeconds;
	}

	/**
	 * Set the tariff schedule cache seconds.
	 * 
	 * @param scheduleCacheSeconds
	 *        the seconds to set
	 */
	public void setScheduleCacheSeconds(int scheduleCacheSeconds) {
		this.scheduleCacheSeconds = scheduleCacheSeconds;
	}

	/**
	 * Get the locale to use for parsing/formatting schedule data.
	 * 
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Get the locale to use for parsing/formatting schedule data.
	 * 
	 * @param locale
	 *        the locale to set
	 */
	public void setLocale(Locale locale) {
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
	public String getLanguage() {
		return getLocale().toLanguageTag();
	}

	/**
	 * Set the locale as a IETF BCP 47 language tag.
	 * 
	 * @param lang
	 *        the language tag to set
	 */
	public void setLanguage(String lang) {
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
	public boolean isFirstMatchOnly() {
		return firstMatchOnly;
	}

	/**
	 * Set the first-match-only flag.
	 * 
	 * @param firstMatchOnly
	 *        {@literal true} if only the first tariff rule that matches should
	 *        be returned
	 */
	public void setFirstMatchOnly(boolean firstMatchOnly) {
		this.firstMatchOnly = firstMatchOnly;
	}

}
