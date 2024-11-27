/* ==================================================================
 * MockMeterDataSource.java - 10/06/2015 1:28:07 pm
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.energymeter.mock;

import static java.time.format.TextStyle.SHORT;
import static java.util.Collections.emptyMap;
import static net.solarnetwork.domain.tariff.SimpleTemporalRangesTariffEvaluator.DEFAULT_EVALUATOR;
import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.BasicDeviceInfo;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.tariff.ChronoFieldsTariff;
import net.solarnetwork.domain.tariff.CompositeTariff;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.Tariff.Rate;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TariffUtils;
import net.solarnetwork.domain.tariff.TemporalTariffEvaluator;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.IntRangeContainer;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * Mock plugin to be the source of values for a GeneralNodeACEnergyDatum, this
 * mock tries to simulate a AC circuit containing a resister and inductor in
 * series.
 *
 * @author robert
 * @author matt
 * @version 1.7
 */
public class MockEnergyMeterDatumSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

	/** The {@code touScheduleCacheTtl} default value (12 hours). */
	public static final Duration DEFAULT_TOU_SCHEDULE_CACHE_TTL = Duration.ofHours(12);

	/** The {@code voltagerms} property default value. */
	public static final double DEFAULT_VOLTAGE = 230.0;

	/** The {@code frequency} property default value. */
	public static final double DEFAULT_FREQUENCY = 50.0;

	/** The {@code current} property default value. */
	public static final double DEFAULT_CURRENT = 10.0;

	private String sourceId;
	private double voltagerms = DEFAULT_VOLTAGE;
	private double frequency = DEFAULT_FREQUENCY;
	private double current = DEFAULT_CURRENT;
	private boolean randomness;
	private double freqDeviation;
	private double voltDeviation;
	private double currentDeviation;
	private String weatherSourceId;

	private Duration touScheduleCacheTtl = DEFAULT_TOU_SCHEDULE_CACHE_TTL;
	private String touMetadataPath;
	private Locale touLocale = Locale.getDefault();
	private Duration touOffset = Duration.ZERO;
	private BigDecimal touScaleFactor = BigDecimal.ONE;

	private final Clock clock;
	private final Random rng;

	private final AtomicReference<CachedResult<TariffSchedule>> touSchedule = new AtomicReference<>();
	private final AtomicReference<AcEnergyDatum> lastsample = new AtomicReference<>();

	/**
	 * Constructor.
	 */
	public MockEnergyMeterDatumSource() {
		this(Clock.systemUTC(), new Random());
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param rng
	 *        the random instance to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MockEnergyMeterDatumSource(Clock clock, Random rng) {
		super();
		this.clock = ObjectUtils.requireNonNullArgument(clock, "clock");
		this.rng = ObjectUtils.requireNonNullArgument(rng, "rng");
	}

	/**
	 * Get a mock starting value for our meter based on the current time so the
	 * meter back to zero each time the app restarts.
	 *
	 * @return a starting meter value
	 */
	private static long meterStartValue() {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Date now = cal.getTime();
		cal.set(2010, cal.getMinimum(Calendar.MONTH), 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return (now.getTime() - cal.getTimeInMillis()) / (1000L * 60);
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcEnergyDatum.class;
	}

	/***
	 * Returns an {@link AcEnergyDatum} the data in the datum is the state of
	 * the simulated circuit.
	 *
	 * @see net.solarnetwork.node.service.DatumDataSource#readCurrentDatum()
	 * @return an {@link AcEnergyDatum}
	 */
	@Override
	public AcEnergyDatum readCurrentDatum() {
		AcEnergyDatum prev = this.lastsample.get();
		AcEnergyDatum datum = new SimpleAcEnergyDatum(resolvePlaceholders(sourceId), clock.instant(),
				new DatumSamples());

		calcVariables(datum);

		calcWattHours(prev, datum);

		this.lastsample.compareAndSet(prev, datum);

		AcEnergyDatum result = (AcEnergyDatum) applyDatumFilter(datum, null);

		return result;
	}

	@Override
	public String deviceInfoSourceId() {
		return resolvePlaceholders(sourceId);
	}

	@Override
	public DeviceInfo deviceInfo() {
		// @formatter:off
		return BasicDeviceInfo.builder()
				.withName("Mock Energy Meter")
				.withManufacturer("SolarNetwork")
				.withModelName("Simutron")
				.withSerialNumber("ABCDEF123")
				.withVersion("1.4")
				.withManufactureDate(LocalDate.of(2021, 7, 9))
				.withDeviceAddress("localhost")
				.withNameplateRatings(Collections.singletonMap("activePowerRating", 1_000_000))
				.build();
		// @formatter:on
	}

	private TariffSchedule touSchedule() {
		final String metadataPath = getTouMetadataPath();
		if ( metadataPath == null || metadataPath.isEmpty() ) {
			return null;
		}
		final MetadataService service = service(getMetadataService());
		if ( service == null ) {
			log.warn(
					"No MetadataService available in mock energy meter [{}], unable to resolve TOU schedule.",
					getUid());
			return null;
		}
		CachedResult<TariffSchedule> r = touSchedule.updateAndGet(c -> {
			if ( c != null && c.isValid() ) {
				return c;
			}
			Object o = service.metadataAtPath(metadataPath);
			if ( o == null ) {
				log.warn(
						"No TOU schedule found in mock energy meter [{}] at metadata path [{}], unable to resolve TOU schedule.",
						getUid(), metadataPath);
				return null;
			}
			try {
				TariffSchedule s = parseSchedule(o);
				if ( s == null ) {
					return null;
				}
				return new CachedResult<>(s, touScheduleCacheTtl.getSeconds(), TimeUnit.SECONDS);
			} catch ( Exception e ) {
				log.warn(
						"Mock energy meter [{}] error parsing TOU schedule from metadata at path [{}]: {}",
						getUid(), touMetadataPath, e.getMessage(), e);
				return null;
			}
		});
		return (r != null ? r.getResult() : null);
	}

	private TariffSchedule parseSchedule(Object o) throws IOException {
		return TariffUtils.parseCsvTemporalRangeSchedule(touLocale, true, true, DEFAULT_EVALUATOR, o);
	}

	private double readVoltage() {
		return voltagerms + (randomness ? voltDeviation * Math.cos(Math.PI * rng.nextDouble()) : 0);
	}

	private double readFrequency() {
		return frequency + (randomness ? freqDeviation * Math.cos(Math.PI * rng.nextDouble()) : 0);
	}

	private double readCurrent() {
		return current + (randomness ? currentDeviation * Math.cos(Math.PI * rng.nextDouble()) : 0);
	}

	/**
	 * Populate the meter values on the datum.
	 *
	 * @param datum
	 *        the datum to populate
	 */
	private void calcVariables(AcEnergyDatum datum) {
		final double vrms = readVoltage();
		datum.setVoltage((float) vrms);

		final double f = readFrequency();
		datum.setFrequency((float) f);

		// check for TOU power schedule, if available use that
		TariffSchedule schedule = touSchedule();
		if ( schedule != null ) {
			BigDecimal watts = BigDecimal.ZERO;
			LocalDateTime now = clock.instant().plus(touOffset).atZone(TimeZone.getDefault().toZoneId())
					.toLocalDateTime();
			Tariff t = schedule.resolveTariff(now, emptyMap());
			if ( t != null ) {
				Tariff.Rate r = t.getRates().values().iterator().next();
				if ( r != null ) {
					watts = r.getAmount().multiply(touScaleFactor);
					// see if we can interpolate over time
					watts = interpolate(schedule, watts, now, t, r);
				}
			}
			datum.setWatts(watts.intValue());
			datum.setCurrent((float) (watts.doubleValue() / vrms));
		} else {
			final double I = readCurrent();
			datum.setCurrent((float) I);
			datum.setWatts((int) (vrms * I));
		}
	}

	private BigDecimal interpolate(TariffSchedule schedule, BigDecimal watts, LocalDateTime now,
			Tariff t, Tariff.Rate r) {
		if ( t == null ) {
			return watts;
		}
		ChronoFieldsTariff ct = t.unwrap(ChronoFieldsTariff.class);
		IntRangeContainer ranges = ct.rangeForChronoField(ChronoField.MINUTE_OF_DAY);
		if ( ranges == null ) {
			return watts;
		}

		// single minute range; find value at next minute block
		LocalDateTime next = now.with(ChronoField.MINUTE_OF_DAY, ranges.max())
				.truncatedTo(ChronoUnit.MINUTES);
		Tariff tNext = schedule.resolveTariff(next, emptyMap());
		if ( tNext != null && tNext != t ) {
			Tariff.Rate rNext = tNext.getRates().values().iterator().next();
			if ( rNext != null ) {
				BigDecimal wattsNext = rNext.getAmount().multiply(touScaleFactor);
				if ( wattsNext.compareTo(watts) != 0 ) {
					Number y = NumberUtils.linearInterpolate(now.get(ChronoField.MINUTE_OF_DAY),
							ranges.min(), ranges.max(), watts.intValue(), wattsNext.intValue());
					watts = NumberUtils.bigDecimalForNumber(y);
				}
			}
		}

		return watts;
	}

	private void calcWattHours(AcEnergyDatum prev, AcEnergyDatum datum) {
		if ( prev == null ) {
			datum.setWattHourReading(meterStartValue());
		} else {
			double diffHours = prev.getTimestamp().until(datum.getTimestamp(), ChronoUnit.MILLIS)
					/ (double) (1000 * 60 * 60);
			long wh = (long) ((datum.getRealPower() != null ? datum.getRealPower() : datum.getWatts())
					* diffHours);
			long newWh = prev.getWattHourReading() + wh;
			datum.setWattHourReading(newWh);
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.energymeter.mock";
	}

	@Override
	public String getDisplayName() {
		return "Mock Energy Meter";
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptyList()
				: Collections.singleton(sourceId));
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		final List<SettingSpecifier> result = new ArrayList<>(24);

		if ( touSchedule() != null ) {
			result.add(new BasicTitleSettingSpecifier("touStatus", touStatusMessage(), true, true));
		}

		result.addAll(getIdentifiableSettingSpecifiers());
		result.addAll(getDeviceInfoMetadataSettingSpecifiers());

		// user enters text
		result.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		result.add(new BasicTextFieldSettingSpecifier("voltage", String.valueOf(DEFAULT_VOLTAGE)));
		result.add(new BasicTextFieldSettingSpecifier("frequency", String.valueOf(DEFAULT_FREQUENCY)));
		result.add(new BasicTextFieldSettingSpecifier("current", String.valueOf(DEFAULT_CURRENT)));
		result.add(new BasicToggleSettingSpecifier("randomness", Boolean.FALSE));
		result.add(new BasicTextFieldSettingSpecifier("voltdev", String.valueOf(0)));
		result.add(new BasicTextFieldSettingSpecifier("freqdev", String.valueOf(0)));
		result.add(new BasicTextFieldSettingSpecifier("currentDeviation", String.valueOf(0)));

		result.add(new BasicTextFieldSettingSpecifier("metadataServiceUid", null, false,
				"(objectClass=net.solarnetwork.node.service.MetadataService)"));

		result.add(new BasicTextFieldSettingSpecifier("touMetadataPath", null));
		result.add(new BasicTextFieldSettingSpecifier("touScheduleCacheTtlSecs",
				String.valueOf(DEFAULT_TOU_SCHEDULE_CACHE_TTL.getSeconds())));
		result.add(new BasicTextFieldSettingSpecifier("touLanguage", null));
		result.add(new BasicTextFieldSettingSpecifier("touOffsetHours", null));
		result.add(new BasicTextFieldSettingSpecifier("touScaleFactor", BigDecimal.ONE.toPlainString()));

		// TODO: use weather source to influence output (cloudy sky decrease output)
		//result.add(new BasicTextFieldSettingSpecifier("weatherSourceId", defaults.weatherSourceId));

		return result;
	}

	private String touStatusMessage() {
		StringBuilder buf = new StringBuilder();
		TariffSchedule schedule = touSchedule();
		CachedResult<TariffSchedule> cached = this.touSchedule.get();
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
		final TemporalTariffEvaluator e = DEFAULT_EVALUATOR;
		final boolean firstOnly = true;
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
		String r = tariff.formatChronoField(field, touLocale, SHORT);
		return (r != null ? r : "*");
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public final String getSourceId() {
		return sourceId;
	}

	/**
	 * Set source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the weather source ID.
	 *
	 * @return the source ID
	 */
	public final String getWeatherSourceId() {
		return weatherSourceId;
	}

	/**
	 * Get the voltage root-mean-square.
	 *
	 * @return the voltage RMS
	 */
	public final double getVoltage() {
		return voltagerms;
	}

	/**
	 * Set the voltage root-mean-square.
	 *
	 * @param voltage
	 *        the voltage to set
	 */
	public final void setVoltage(double voltage) {
		this.voltagerms = voltage;
	}

	/**
	 * Get the frequency.
	 *
	 * @return the frequency
	 */
	public final double getFrequency() {
		return frequency;
	}

	/**
	 * Set the frequency.
	 *
	 * @param frequency
	 *        the frequency to set
	 */
	public final void setFrequency(double frequency) {
		this.frequency = frequency;
	}

	/**
	 * Get the voltage randomness deviation.
	 *
	 * @return the deviation
	 */
	public final double getVoltdev() {
		return voltDeviation;
	}

	/**
	 * Set the voltage randomness deviation.
	 *
	 * @param voltdev
	 *        the deviation to set
	 */
	public final void setVoltdev(double voltdev) {
		this.voltDeviation = voltdev;
	}

	/**
	 * Get the frequency randomness deviation.
	 *
	 * @return the deviation
	 */
	public final double getFreqdev() {
		return freqDeviation;
	}

	/**
	 * Set the frequency randomness deviation.
	 *
	 * @param freqdev
	 *        the deviation
	 */
	public final void setFreqdev(double freqdev) {
		this.freqDeviation = freqdev;
	}

	/**
	 * Get the randomness mode.
	 *
	 * @return {@literal true} to enable randomness in the generated datum
	 *         values
	 */
	public final boolean getRandomness() {
		return randomness;
	}

	/**
	 * Set the randomness mode.
	 *
	 * @param random
	 *        {@literal true} to enable randomness in the generated datum values
	 */
	public final void setRandomness(boolean random) {
		this.randomness = random;
	}

	/**
	 * Get the inductance randomness deviation.
	 *
	 * @return the deviation
	 */
	public final double getCurrentDeviation() {
		return currentDeviation;
	}

	/**
	 * Set the inductance deviation.
	 *
	 * @param inductanceDeviation
	 *        the deviation to set
	 */
	public final void setCurrentDeviation(double inductanceDeviation) {
		this.currentDeviation = inductanceDeviation;
	}

	/**
	 * Get the {@link MetadataService} service filter UID.
	 *
	 * @return the service UID
	 */
	public final String getMetadataServiceUid() {
		final OptionalService<MetadataService> service = getMetadataService();
		if ( service instanceof FilterableService ) {
			return ((FilterableService) service).getPropertyValue(UID_PROPERTY);
		}
		return null;
	}

	/**
	 * Set the {@link MetadataService} service filter UID.
	 *
	 * @param uid
	 *        the service UID
	 */
	public final void setMetadataServiceUid(String uid) {
		final OptionalService<MetadataService> service = getMetadataService();
		if ( service instanceof FilterableService ) {
			((FilterableService) service).setPropertyFilter(UID_PROPERTY, uid);
		}
	}

	/**
	 * Get the locale to use for parsing/formatting TOU data.
	 *
	 * @return the locale
	 */
	public Locale getTouLocale() {
		return touLocale;
	}

	/**
	 * Get the locale to use for parsing/formatting TOU data.
	 *
	 * @param locale
	 *        the locale to set
	 */
	public void setTouLocale(Locale locale) {
		if ( locale == null ) {
			locale = Locale.getDefault();
		}
		this.touLocale = locale;
	}

	/**
	 * Get the TOU locale IETF BCP 47 language tag.
	 *
	 * @return the language
	 */
	public String getTouLanguage() {
		return getTouLocale().toLanguageTag();
	}

	/**
	 * Set the TOU locale as a IETF BCP 47 language tag.
	 *
	 * @param lang
	 *        the language tag to set
	 */
	public void setTouLanguage(String lang) {
		setTouLocale(lang != null ? Locale.forLanguageTag(lang) : null);
	}

	/**
	 * Get the TOU schedule cache time-to-live.
	 *
	 * @return the TTL
	 */
	public final Duration getTouScheduleCacheTtl() {
		return touScheduleCacheTtl;
	}

	/**
	 * Set the TOU schedule cache time-to-live.
	 *
	 * @param touScheduleCacheTtl
	 *        the TTL to set
	 */
	public final void setTouScheduleCacheTtl(Duration touScheduleCacheTtl) {
		this.touScheduleCacheTtl = touScheduleCacheTtl;
	}

	/**
	 * Get the TOU schedule cache time-to-live, in seconds.
	 *
	 * @return the TTL in seconds
	 */
	public final long getTouScheduleCacheTtlSecs() {
		return touScheduleCacheTtl.getSeconds();
	}

	/**
	 * Set the TOU schedule cache time-to-live, in seconds.
	 *
	 * @param seconds
	 *        the TTL to set, in seconds
	 */
	public final void setTouScheduleCacheTtlSecs(long seconds) {
		this.touScheduleCacheTtl = Duration.ofSeconds(seconds);
	}

	/**
	 * Get the TOU metadata path.
	 *
	 * @return the metadata path
	 */
	public final String getTouMetadataPath() {
		return touMetadataPath;
	}

	/**
	 * Set the TOU metadata path.
	 *
	 * @param touMetadataPath
	 *        the metadata path to set
	 */
	public final void setTouMetadataPath(String touMetadataPath) {
		this.touMetadataPath = touMetadataPath;
	}

	/**
	 * Get the TOU offset.
	 *
	 * @return the offset
	 */
	public final Duration getTouOffset() {
		return touOffset;
	}

	/**
	 * Set the TOU offset.
	 *
	 * <p>
	 * This offset is applied to the clock's current time when resolving the
	 * active TOU rates.
	 * </p>
	 *
	 * @param touOffset
	 *        the offset to set; of {@literal null} then 0 will be used
	 */
	public final void setTouOffset(Duration touOffset) {
		this.touOffset = (touOffset != null ? touOffset : Duration.ZERO);
	}

	/**
	 * Get the TOU offset, in hours.
	 *
	 * @return the offset, in hours
	 */
	public final long getTouOffsetHours() {
		return getTouOffset().get(ChronoUnit.HOURS);
	}

	/**
	 * Set the TOU offset, in hours.
	 *
	 * <p>
	 * This offset is applied to the clock's current time when resolving the
	 * active TOU rates.
	 * </p>
	 *
	 * @param hours
	 *        the offset to set
	 */
	public final void setTouOffsetHours(long hours) {
		this.touOffset = Duration.ofHours(hours);
	}

	/**
	 * Get a multiplication factor to apply to TOU rates.
	 *
	 * @return the multiplication factor; defaults to {@code 1}
	 */
	public final BigDecimal getTouScaleFactor() {
		return touScaleFactor;
	}

	/**
	 * Set a multiplication factor to apply to TOU rates.
	 *
	 * @param touScaleFactor
	 *        the touScaleFactor to set; if {@literal null} then {@code 1} will
	 *        be used
	 */
	public final void setTouScaleFactor(BigDecimal touScaleFactor) {
		this.touScaleFactor = (touScaleFactor != null ? touScaleFactor : BigDecimal.ONE);
	}

}
