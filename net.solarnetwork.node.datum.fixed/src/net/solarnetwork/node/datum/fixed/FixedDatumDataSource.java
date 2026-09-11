/* ==================================================================
 * FixedDatumDataSource.java - 10/07/2026 2:39:19 pm
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

package net.solarnetwork.node.datum.fixed;

import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.domain.tariff.SimpleTemporalRangesTariffEvaluator.DEFAULT_EVALUATOR;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.tariff.Tariff;
import net.solarnetwork.domain.tariff.TariffSchedule;
import net.solarnetwork.domain.tariff.TariffUtils;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.NumberUtils;

/**
 * Generate datum based on static configuration.
 *
 * @author matt
 * @version 1.1
 */
public class FixedDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

	/** The setting UID. */
	public static final String SETTING_UID = "net.solarnetwork.node.datum.fixed";

	/** The {@code touScheduleCacheTtl} default value (12 hours). */
	public static final Duration DEFAULT_TOU_SCHEDULE_CACHE_TTL = Duration.ofHours(12);

	private @Nullable String sourceId;
	private PropertyConfig @Nullable [] propertyConfigs;

	private Duration touScheduleCacheTtl = DEFAULT_TOU_SCHEDULE_CACHE_TTL;
	private @Nullable String touMetadataPath;
	private Locale touLocale = Locale.UK;
	private ZoneId touZone = ZoneId.systemDefault();
	private BigDecimal touScaleFactor = BigDecimal.ONE;

	private final Clock clock;

	private final AtomicReference<@Nullable CachedResult<TariffSchedule>> touSchedule = new AtomicReference<>();
	private final AtomicReference<@Nullable NodeDatum> lastDatum = new AtomicReference<>();

	/**
	 * Constructor.
	 *
	 * <p>
	 * The system UTC clock will be used.
	 * </p>
	 */
	public FixedDatumDataSource() {
		this(Clock.tickMillis(UTC));
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public FixedDatumDataSource(Clock clock) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		setDisplayName("Fixed Datum Data Source");
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return NodeDatum.class;
	}

	@Override
	public @Nullable NodeDatum readCurrentDatum() {
		final String sourceId = sourceId();
		final PropertyConfig[] propConfigs = getPropertyConfigs();
		final TariffSchedule schedule = touSchedule();
		if ( sourceId == null || (propConfigs == null || propConfigs.length < 1) && schedule == null ) {
			return null;
		}

		final Instant timestamp = clock.instant();
		final DatumSamples samples = new DatumSamples();

		if ( propConfigs != null ) {
			for ( PropertyConfig propConfig : propConfigs ) {
				final String propName = propConfig.getPropertyKey();
				if ( propName == null || propName.isEmpty() ) {
					continue;
				}
				final DatumSamplesType propType = propConfig.getPropertyType();
				if ( propType == null ) {
					continue;
				}
				final Object propValue = propConfig.value();
				if ( propValue == null ) {
					continue;
				}
				samples.putSampleValue(propType, propName, propValue);
			}
		}

		if ( schedule != null ) {
			final LocalDateTime now = timestamp.atZone(touZone).toLocalDateTime();
			final Tariff t = schedule.resolveTariff(now, Map.of());
			if ( t != null ) {
				for ( Tariff.Rate r : t.getRates().values() ) {
					final Number val = NumberUtils.narrow(r.getAmount().multiply(touScaleFactor), 2);
					samples.putInstantaneousSampleValue(r.getId(), val);
				}
			}
		}

		SimpleDatum result = SimpleDatum.nodeDatum(sourceId, timestamp, samples);
		lastDatum.set(result);
		return result;
	}

	private @Nullable TariffSchedule touSchedule() {
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

	private @Nullable TariffSchedule parseSchedule(Object o) throws IOException {
		return TariffUtils.parseCsvTemporalRangeSchedule(touLocale, true, true, DEFAULT_EVALUATOR, o);
	}

	private @Nullable String sourceId() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId != null && !sourceId.isEmpty() ? sourceId : null);
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = sourceId();
		return (sourceId != null ? List.of(sourceId) : List.of());
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return settings(false);
	}

	@Override
	public List<SettingSpecifier> templateSettingSpecifiers() {
		return settings(true);
	}

	private List<SettingSpecifier> settings(final boolean template) {
		final List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTitleSettingSpecifier("datumStatus",
				datumPropertiesHtmlMessage(lastDatum.get(), Locale.getDefault()), true, true));
		if ( touSchedule() != null ) {
			result.add(new BasicTitleSettingSpecifier("touStatus", touStatusMessage(), true, true));
		}

		result.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		result.add(new BasicTextFieldSettingSpecifier("metadataServiceUid", null, false,
				"(objectClass=net.solarnetwork.node.service.MetadataService)"));

		result.add(new BasicTextFieldSettingSpecifier("touMetadataPath", null));
		result.add(new BasicTextFieldSettingSpecifier("touScheduleCacheTtlSecs",
				String.valueOf(DEFAULT_TOU_SCHEDULE_CACHE_TTL.getSeconds())));
		result.add(new BasicTextFieldSettingSpecifier("touLanguage", getTouLanguage()));
		result.add(new BasicTextFieldSettingSpecifier("touZoneId", getTouZoneId()));
		result.add(new BasicTextFieldSettingSpecifier("touScaleFactor", BigDecimal.ONE.toPlainString()));

		final PropertyConfig[] propertyConfs = getPropertyConfigs();
		final List<PropertyConfig> propertyConfsList = (template ? List.of(new PropertyConfig())
				: (propertyConfs != null ? List.of(propertyConfs) : List.of()));
		result.add(SettingUtils.dynamicListSettingSpecifier("propertyConfigs", propertyConfsList,
				new SettingUtils.KeyedListCallback<>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(@Nullable PropertyConfig value,
							int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								PropertyConfig.settings(key + "."));
						return List.of(configGroup);
					}
				}));

		return result;
	}

	private String touStatusMessage() {
		touSchedule(); // load
		return touStatusHtmlMessage(this.touSchedule.get(), Locale.getDefault(), touLocale, true);
	}

	/**
	 * Get the {@link MetadataService} service filter UID.
	 *
	 * @return the service UID
	 */
	public final @Nullable String getMetadataServiceUid() {
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
	public final void setMetadataServiceUid(@Nullable String uid) {
		final OptionalService<MetadataService> service = getMetadataService();
		if ( service instanceof FilterableService ) {
			((FilterableService) service).setPropertyFilter(UID_PROPERTY, uid);
		}
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public final @Nullable String getSourceId() {
		return sourceId;
	}

	/**
	 * Set source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the property configurations
	 */
	public final PropertyConfig @Nullable [] getPropertyConfigs() {
		return propertyConfigs;
	}

	/**
	 * Set the property configurations to use.
	 *
	 * @param propertyConfigs
	 *        the configs to use
	 */
	public final void setPropertyConfigs(PropertyConfig @Nullable [] propertyConfigs) {
		this.propertyConfigs = propertyConfigs;
	}

	/**
	 * Get the number of configured {@code propertyConfigs} elements.
	 *
	 * @return the number of {@code propertyConfigs} elements
	 */
	public final int getPropertyConfigsCount() {
		PropertyConfig[] confs = this.propertyConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code PropertyConfig} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new {@link PropertyConfig}
	 * instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code propertyConfigs} elements.
	 */
	public final void setPropertyConfigsCount(int count) {
		this.propertyConfigs = ArrayUtils.arrayWithLength(this.propertyConfigs, count,
				PropertyConfig.class, null);
	}

	/**
	 * Get the locale to use for parsing/formatting TOU data.
	 *
	 * @return the locale
	 */
	public final Locale getTouLocale() {
		return touLocale;
	}

	/**
	 * Get the locale to use for parsing/formatting TOU data.
	 *
	 * @param locale
	 *        the locale to set; if {@code null} the system default will be used
	 */
	public final void setTouLocale(@Nullable Locale locale) {
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
	public final String getTouLanguage() {
		return getTouLocale().toLanguageTag();
	}

	/**
	 * Set the TOU locale as a IETF BCP 47 language tag.
	 *
	 * @param lang
	 *        the language tag to set
	 */
	public final void setTouLanguage(String lang) {
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
	public final @Nullable String getTouMetadataPath() {
		return touMetadataPath;
	}

	/**
	 * Set the TOU metadata path.
	 *
	 * @param touMetadataPath
	 *        the metadata path to set
	 */
	public final void setTouMetadataPath(@Nullable String touMetadataPath) {
		this.touMetadataPath = touMetadataPath;
	}

	/**
	 * Get the TOU time zone.
	 *
	 * @return the zone; defaults to the system default
	 */
	public final ZoneId getTouZone() {
		return touZone;
	}

	/**
	 * Set the TOU time zone.
	 *
	 * @param touZone
	 *        the zone to set; of {@literal null} then the system default will
	 *        be used
	 */
	public final void setTouZone(ZoneId touZone) {
		this.touZone = (touZone != null ? touZone : ZoneId.systemDefault());
	}

	/**
	 * Get the TOU time zone, as an ID value.
	 *
	 * @return the TOU time zone ID
	 */
	public final String getTouZoneId() {
		return getTouZone().getId();
	}

	/**
	 * Set the TOU time zone, as an ID value.
	 *
	 * @param touZoneId
	 *        the TOU time zone ID to set
	 */
	public final void setTouZoneId(String touZoneId) {
		setTouZone(ZoneId.of(touZoneId));
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
