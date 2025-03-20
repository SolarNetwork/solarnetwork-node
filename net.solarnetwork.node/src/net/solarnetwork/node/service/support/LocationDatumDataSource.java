/* ==================================================================
 * LocationDatumDataSource.java - Feb 21, 2011 5:23:28 PM
 *
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.GeneralLocationSourceMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.domain.datum.DatumLocation;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.PriceLocation;
import net.solarnetwork.node.domain.datum.SimpleDatumLocation;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.LocationService;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.settings.LocationLookupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicLocationLookupSettingSpecifier;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.MappableSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.support.PrefixedMessageSource;

/**
 * {@link DatumDataSource} that augments some other data source's datum values
 * with location IDs.
 *
 * <p>
 * This is to be used to easily augment various datum that relate to a location
 * with the necessary {@link DatumLocation#getLocationId()} ID. This class also
 * implements the {@link MultiDatumDataSource} API, and will call the methods of
 * that API on the configured {@code delegate} if that also implements
 * {@link MultiDatumDataSource}. If the {@code delegate} does not implement
 * {@link MultiDatumDataSource} this class will "fake" that API by calling
 * {@link DatumDataSource#readCurrentDatum()} and returning that object in a
 * Collection.
 * </p>
 *
 * @author matt
 * @version 1.2
 * @since 2.0
 */
public class LocationDatumDataSource implements DatumDataSource, MultiDatumDataSource,
		SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver {

	/** Default value for the {@code locationIdPropertyName} property. */
	public static final String DEFAULT_LOCATION_ID_PROP_NAME = "locationId";

	/** Default value for the {@code sourceIdPropertyName} property. */
	public static final String DEFAULT_SOURCE_ID_PROP_NAME = "locationSourceId";

	/** Bundle name for price location lookup messages. */
	public static final String PRICE_LOCATION_MESSAGE_BUNDLE = "net.solarnetwork.node.service.support.PriceLocationDatumDataSource";

	private DatumDataSource delegate;
	private OptionalService<LocationService> locationService;
	private String locationType = DatumLocation.PRICE_TYPE;
	private String locationIdPropertyName = DEFAULT_LOCATION_ID_PROP_NAME;
	private String sourceIdPropertyName = DEFAULT_SOURCE_ID_PROP_NAME;
	private boolean requireLocationService = false;
	private String messageBundleBasename = PRICE_LOCATION_MESSAGE_BUNDLE;
	private Long locationId = null;
	private String sourceId = null;
	private Set<String> datumClassNameIgnore;
	private boolean includeLocationTypeSetting;

	private DatumLocation location = null;
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Default constructor.
	 */
	public LocationDatumDataSource() {
		super();
	}

	/**
	 * Handle service startup.
	 *
	 * <p>
	 * This method will delegate to the configured {@code delegate} if that also
	 * implements {@link ServiceLifecycleObserver}.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void serviceDidStartup() {
		ServiceLifecycleObserver delegate = serviceLifecycleObserver();
		if ( delegate != null ) {
			delegate.serviceDidStartup();
		}
	}

	/**
	 * Handle service shutdown.
	 *
	 * <p>
	 * This method will delegate to the configured {@code delegate} if that also
	 * implements {@link ServiceLifecycleObserver}.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void serviceDidShutdown() {
		ServiceLifecycleObserver delegate = serviceLifecycleObserver();
		if ( delegate != null ) {
			delegate.serviceDidShutdown();
		}
	}

	/**
	 * Handle configuration changes.
	 *
	 * <p>
	 * This method will delegate to the configured {@code delegate} if that also
	 * implements {@link SettingsChangeObserver}.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void configurationChanged(Map<String, Object> properties) {
		if ( delegate instanceof SettingsChangeObserver ) {
			((SettingsChangeObserver) delegate).configurationChanged(properties);
		}
	}

	private ServiceLifecycleObserver serviceLifecycleObserver() {
		if ( delegate instanceof ServiceLifecycleObserver ) {
			return (ServiceLifecycleObserver) delegate;
		}
		return null;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return delegate.getDatumType();
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		if ( delegate instanceof MultiDatumDataSource ) {
			return ((MultiDatumDataSource) delegate).getMultiDatumType();
		}
		return delegate.getDatumType();
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		Collection<NodeDatum> datumList = null;
		if ( delegate instanceof MultiDatumDataSource ) {
			datumList = ((MultiDatumDataSource) delegate).readMultipleDatum();
		} else {
			// fake multi API
			datumList = new ArrayList<>(1);
			NodeDatum datum = delegate.readCurrentDatum();
			if ( datum != null ) {
				datumList.add(datum);
			}
		}
		List<NodeDatum> result = new ArrayList<>();
		if ( datumList != null && locationId != null ) {
			for ( NodeDatum datum : datumList ) {
				NodeDatum d = populateLocation(datum);
				if ( d != null ) {
					result.add(d);
				}
			}
		} else if ( datumList != null && !datumList.isEmpty() && locationId == null
				&& requireLocationService ) {
			log.warn("Location required but not available, discarding datum: {}", datumList);
		}
		return result;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		NodeDatum datum = delegate.readCurrentDatum();
		if ( datum != null && locationId != null ) {
			datum = populateLocation(datum);
		} else if ( datum != null && locationId == null && requireLocationService ) {
			log.warn("Location required but not available, discarding datum: {}", datum);
			datum = null;
		}
		return datum;
	}

	private NodeDatum populateLocation(NodeDatum datum) {
		if ( locationId != null && sourceId != null && !shouldIgnoreDatum(datum) ) {
			log.debug("Augmenting datum {} with locaiton ID {} ({})", datum, locationId, sourceId);
			return datum.copyWithId(
					new DatumId(ObjectDatumKind.Location, locationId, sourceId, datum.getTimestamp()));
		}
		return datum;
	}

	private boolean shouldIgnoreDatum(NodeDatum datum) {
		return (datum == null || (datumClassNameIgnore != null
				&& datumClassNameIgnore.contains(datum.getClass().getName())));
	}

	@Override
	public String toString() {
		return delegate != null ? delegate.toString() + "[LocationDatumDataSource proxy]"
				: "LocationDatumDataSource";
	}

	@Override
	public String getUid() {
		return delegate.getUid();
	}

	@Override
	public String getGroupUid() {
		return delegate.getGroupUid();
	}

	@Override
	public String getSettingUid() {
		if ( delegate instanceof SettingSpecifierProvider ) {
			return ((SettingSpecifierProvider) delegate).getSettingUid();
		}
		return getClass().getName();
	}

	@Override
	public String getDisplayName() {
		if ( delegate instanceof SettingSpecifierProvider ) {
			return ((SettingSpecifierProvider) delegate).getDisplayName();
		}
		return null;
	}

	@Override
	public synchronized MessageSource getMessageSource() {
		if ( messageSource == null ) {
			MessageSource other = null;
			if ( delegate instanceof SettingSpecifierProvider ) {
				other = ((SettingSpecifierProvider) delegate).getMessageSource();
			}
			PrefixedMessageSource delegateSource = null;
			if ( other != null ) {
				delegateSource = new PrefixedMessageSource();
				delegateSource.setDelegate(other);
				delegateSource.setPrefix("delegate.");
			}

			ResourceBundleMessageSource proxySource = new ResourceBundleMessageSource();
			proxySource.setBundleClassLoader(getClass().getClassLoader());
			proxySource.setBasename(messageBundleBasename);
			if ( delegateSource != null ) {
				proxySource.setParentMessageSource(delegateSource);
			}

			messageSource = proxySource;
		}
		return messageSource;
	}

	/**
	 * Set the message source.
	 *
	 * @param messageSource
	 *        the message source to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.add(getLocationSettingSpecifier());
		if ( includeLocationTypeSetting ) {
			result.add(new BasicTextFieldSettingSpecifier("locationType", DatumLocation.PRICE_TYPE));
		}
		if ( delegate instanceof SettingSpecifierProvider ) {
			List<SettingSpecifier> delegateResult = ((SettingSpecifierProvider) delegate)
					.getSettingSpecifiers();
			if ( delegateResult != null ) {
				for ( SettingSpecifier spec : delegateResult ) {
					if ( spec instanceof MappableSpecifier ) {
						MappableSpecifier keyedSpec = (MappableSpecifier) spec;
						result.add(keyedSpec.mappedTo("delegate."));
					} else {
						result.add(spec);
					}
				}
			}
		}
		return result;
	}

	private LocationLookupSettingSpecifier getLocationSettingSpecifier() {
		if ( location == null && locationService != null && locationId != null && sourceId != null ) {
			LocationService service = locationService.service();
			if ( service != null ) {
				try {
					GeneralLocationSourceMetadata meta = service.getLocationMetadata(locationId,
							sourceId);
					SimpleDatumLocation loc = new SimpleDatumLocation();
					loc.setLocationId(locationId);
					loc.setSourceId(sourceId);
					loc.setSourceMetadata(meta);
					this.location = loc;
				} catch ( Exception e ) {
					Throwable root = e;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					log.error("Error looking up location {} for source [{}]: {}", locationId, sourceId,
							root.toString());
				}
			}
		}
		return new BasicLocationLookupSettingSpecifier("locationKey", locationType, location);
	}

	/**
	 * Get the delegate.
	 *
	 * @return the delegate
	 */
	public DatumDataSource getDelegate() {
		return delegate;
	}

	/**
	 * Set the delegate.
	 *
	 * @param delegate
	 *        the delegate to set
	 */
	public void setDelegate(DatumDataSource delegate) {
		this.delegate = delegate;
	}

	/**
	 * Get the {@link LocationService} to use to lookup {@link DatumLocation}
	 * instances via the configured {@code locationId} property.
	 *
	 * @return the location service
	 */
	public OptionalService<LocationService> getLocationService() {
		return locationService;
	}

	/**
	 * Set the {@link LocationService} to use to lookup {@link DatumLocation}
	 * instances via the configured {@code locationId} property.
	 *
	 * @param locationService
	 *        the service to use
	 */
	public void setLocationService(OptionalService<LocationService> locationService) {
		this.locationService = locationService;
	}

	/**
	 * Get the JavaBean property name to set the found
	 * {@link DatumLocation#getLocationId()} to on the {@link NodeDatum}
	 * returned from the configured {@code delegate}.
	 *
	 * @return the location ID property name; defaults to
	 *         {@link #DEFAULT_LOCATION_ID_PROP_NAME}
	 */
	public String getLocationIdPropertyName() {
		return locationIdPropertyName;
	}

	/**
	 * Set the JavaBean property name to set the found
	 * {@link DatumLocation#getLocationId()} to on the {@link NodeDatum}
	 * returned from the configured {@code delegate}.
	 *
	 * <p>
	 * The object must support a JavaBean setter method for this property.
	 * </p>
	 *
	 * @param locationIdPropertyName
	 *        the property name to use
	 */
	public void setLocationIdPropertyName(String locationIdPropertyName) {
		this.locationIdPropertyName = locationIdPropertyName;
	}

	/**
	 * Get the "location service required" flag.
	 *
	 * @return the location service reqiured flag; defaults to {@literal false}
	 */
	public boolean isRequireLocationService() {
		return requireLocationService;
	}

	/**
	 * Get the "location service required" flag.
	 *
	 * <p>
	 * If configured as {@literal true} then return {@literal null} data only
	 * instead of calling the {@code delegate}. This is designed for services
	 * that require a location ID to be set, for example a Location Datum
	 * logger.
	 * </p>
	 *
	 * @param requireLocationService
	 *        the required setting to use
	 */
	public void setRequireLocationService(boolean requireLocationService) {
		this.requireLocationService = requireLocationService;
	}

	/**
	 * Get the type of location to search for.
	 *
	 * @return the type; defaults to {@link PriceLocation}
	 */
	public String getLocationType() {
		return locationType;
	}

	/**
	 * Set the type of location to search for.
	 *
	 * @param locationType
	 *        the location type
	 */
	public void setLocationType(String locationType) {
		this.locationType = locationType;
	}

	/**
	 * Get the message bundle basename to use.
	 *
	 * @return the basename; defaults to {@link #PRICE_LOCATION_MESSAGE_BUNDLE}
	 */
	public String getMessageBundleBasename() {
		return messageBundleBasename;
	}

	/**
	 * Set the message bundle basename to use.
	 *
	 * <p>
	 * This can be customized so different messages can be shown for different
	 * uses of this proxy.
	 * </p>
	 *
	 * @param messageBundleBaseName
	 *        the basename to use
	 */
	public void setMessageBundleBasename(String messageBundleBaseName) {
		this.messageBundleBasename = messageBundleBaseName;
	}

	/**
	 * Get the location ID and source ID as a single string value. The format of
	 * the key is {@code locationId:sourceId}.
	 *
	 * @return the location key, or {@literal null} if both the location ID and
	 *         source ID values are {@literal null}
	 */
	public String getLocationKey() {
		StringBuilder buf = new StringBuilder();
		Long locId = getLocationId();
		String sourceId = getSourceId();
		if ( locId == null && (sourceId == null || sourceId.isEmpty()) ) {
			return null;
		}
		if ( locId != null ) {
			buf.append(locId.toString());
		}
		buf.append(":");
		buf.append(sourceId);
		return buf.toString();
	}

	/**
	 * Set the location ID and source ID as a single string value. The format of
	 * the key is {@code locationId:sourceId}.
	 *
	 * @param key
	 *        the location and source ID key
	 */
	public void setLocationKey(String key) {
		Long newLocationId = null;
		String newSourceId = null;
		if ( key != null ) {
			int idx = key.indexOf(':');
			if ( idx > 0 && idx + 1 < key.length() ) {
				newLocationId = Long.valueOf(key.substring(0, idx));
				newSourceId = key.substring(idx + 1);
			}
		}
		setLocationId(newLocationId);
		setSourceId(newSourceId);
	}

	/**
	 * Get the {@link DatumLocation} ID to assign to datum.
	 *
	 * @return the location ID
	 */
	public Long getLocationId() {
		return locationId;
	}

	/**
	 * Set the {@link DatumLocation} ID to assign to datum.
	 *
	 * @param locationId
	 *        the location ID
	 */
	public void setLocationId(Long locationId) {
		if ( this.location != null && locationId != null
				&& !locationId.equals(this.location.getLocationId()) ) {
			this.location = null; // set to null so we re-fetch from server
		}
		this.locationId = locationId;
	}

	/**
	 * Get the location.
	 *
	 * @return the location
	 */
	public DatumLocation getLocation() {
		return location;
	}

	/**
	 * Get the datum class names to ignore.
	 *
	 * @return the ignore set
	 */
	public Set<String> getDatumClassNameIgnore() {
		return datumClassNameIgnore;
	}

	/**
	 * Set the datum class names to ignore.
	 *
	 * @param datumClassNameIgnore
	 *        the ignore set
	 */
	public void setDatumClassNameIgnore(Set<String> datumClassNameIgnore) {
		this.datumClassNameIgnore = datumClassNameIgnore;
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 *
	 * @param sourceId
	 *        the source ID to use
	 */
	public void setSourceId(String sourceId) {
		if ( this.location != null && sourceId != null
				&& !sourceId.equals(this.location.getSourceId()) ) {
			this.location = null; // set to null so we re-fetch from server
		}
		this.sourceId = sourceId;
	}

	/**
	 * Get the JavaBean property name to set the found
	 * {@link DatumLocation#getSourceId()} to on the {@link NodeDatum} returned
	 * from the configured {@code delegate}.
	 *
	 * @return the source ID property name; defaults to
	 *         {@link #DEFAULT_SOURCE_ID_PROP_NAME}
	 */
	public String getSourceIdPropertyName() {
		return sourceIdPropertyName;
	}

	/**
	 * Set the JavaBean property name to set the found
	 * {@link DatumLocation#getSourceId()} to on the {@link NodeDatum} returned
	 * from the configured {@code delegate}.
	 *
	 * <p>
	 * The object must support a JavaBean setter method for this property.
	 * </p>
	 *
	 * @param sourceIdPropertyName
	 *        the source ID property name to use
	 */
	public void setSourceIdPropertyName(String sourceIdPropertyName) {
		this.sourceIdPropertyName = sourceIdPropertyName;
	}

	/**
	 * Get the flag to include a location type setting.
	 *
	 * @return {@literal true} to include a {@code locationType} setting in
	 *         {@link #getSettingSpecifiers()}
	 * @since 1.1
	 */
	public boolean isIncludeLocationTypeSetting() {
		return includeLocationTypeSetting;
	}

	/**
	 * Set the flag to include a location type setting.
	 *
	 * @param includeLocationTypeSetting
	 *        {@literal true} to include a {@code locationType} setting in
	 *        {@link #getSettingSpecifiers()}
	 * @since 1.1
	 */
	public void setIncludeLocationTypeSetting(boolean includeLocationTypeSetting) {
		this.includeLocationTypeSetting = includeLocationTypeSetting;
	}

}
