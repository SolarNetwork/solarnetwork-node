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

package net.solarnetwork.node.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.LocationService;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.domain.Location;
import net.solarnetwork.node.domain.PriceLocation;
import net.solarnetwork.node.domain.PricedDatum;
import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.LocationLookupSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicLocationLookupSettingSpecifier;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * {@link DatumDataSource} that augments some other data source's datum values
 * with location IDs.
 * 
 * <p>
 * This is to be used to easily augment various datum that relate to a location
 * with the necessary {@link Location#getLocationId()} ID. This class also
 * implements the {@link MultiDatumDataSource} API, and will call the methods of
 * that API on the configured {@code delegate} if that also implements
 * {@link MultiDatumDataSource}. If the {@code delegate} does not implement
 * {@link MultiDatumDataSource} this class will "fake" that API by calling
 * {@link DatumDataSource#readCurrentDatum()} and returning that object in a
 * Collection.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>delegate</dt>
 * <dd>The {@link DatumDataSource} to delegate to.</dd>
 * 
 * <dt>locationType</dt>
 * <dd>The type of location to search for. Defaults to {@link PriceLocation}.</dd>
 * 
 * <dt>locationService</dt>
 * <dd>The {@link LocationService} to use to lookup {@link Location} instances
 * via the configured {@code locationId} property.</dd>
 * 
 * <dt>locationId</dt>
 * <dd>The {@link Location} ID to assign.</dd>
 * 
 * <dt>locationIdPropertyName</dt>
 * <dd>The JavaBean property name to set the found
 * {@link Location#getLocationId()} to on the {@link Datum} returned from the
 * configured {@code delegate}. The object must support a JavaBean setter method
 * for this property. Defaults to {@link #DEFAULT_LOCATION_ID_PROP_NAME}.</dd>
 * 
 * <dt>requireLocationService</dt>
 * <dd>If configured as <em>true</em> then return <em>null</em> data only
 * instead of calling the delegate. This is designed for services that require a
 * location ID to be set, for example a Location Datum logger. Defaults to
 * <em>false</em>.</dd>
 * 
 * <dt>messageBundleBasename</dt>
 * <dd>The message bundle basename to use. This can be customized so different
 * messages can be shown for different uses of this proxy. Defaults to
 * {@link #PRICE_LOCATION_MESSAGE_BUNDLE}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.4
 */
public class LocationDatumDataSource<T extends Datum> implements DatumDataSource<T>,
		MultiDatumDataSource<T>, SettingSpecifierProvider {

	/** Default value for the {@code locationIdPropertyName} property. */
	public static final String DEFAULT_LOCATION_ID_PROP_NAME = "locationId";

	/** Bundle name for price location lookup messages. */
	public static final String PRICE_LOCATION_MESSAGE_BUNDLE = "net.solarnetwork.node.support.PriceLocationDatumDataSource";

	private DatumDataSource<T> delegate;
	private OptionalService<LocationService> locationService;
	private Class<? extends Location> locationType = PriceLocation.class;
	private String locationIdPropertyName = DEFAULT_LOCATION_ID_PROP_NAME;
	private boolean requireLocationService = false;
	private String messageBundleBasename = PRICE_LOCATION_MESSAGE_BUNDLE;
	private Long locationId = null;
	private Set<String> datumClassNameIgnore;

	private Location location = null;
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Factory method.
	 * 
	 * <p>
	 * This method exists to work around issues with wiring this class via
	 * Gemini Blueprint 2.2. It throws a
	 * {@code SpringBlueprintConverterService$BlueprintConverterException} if
	 * the delegate parameter is defined as {@code DatumDataSource}.
	 * </p>
	 * 
	 * @param delegate
	 *        the delegate, must implement
	 *        {@code DatumDataSource<? extends Datum>}
	 * @param locationService
	 *        the location service
	 * @return the data source
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static LocationDatumDataSource<? extends Datum> getInstance(Object delegate,
			OptionalServiceTracker<LocationService> locationService) {
		LocationDatumDataSource<? extends Datum> ds = new LocationDatumDataSource<Datum>();
		ds.setDelegate((DatumDataSource) delegate);
		ds.setLocationService(locationService);
		return ds;
	}

	@Override
	public Class<? extends T> getDatumType() {
		return delegate.getDatumType();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends T> getMultiDatumType() {
		if ( delegate instanceof MultiDatumDataSource ) {
			return ((MultiDatumDataSource<T>) delegate).getMultiDatumType();
		}
		return delegate.getDatumType();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<T> readMultipleDatum() {
		Collection<T> results = null;
		if ( delegate instanceof MultiDatumDataSource ) {
			results = ((MultiDatumDataSource<T>) delegate).readMultipleDatum();
		} else {
			// fake multi API
			results = new ArrayList<T>(1);
			T datum = delegate.readCurrentDatum();
			if ( datum != null ) {
				results.add(datum);
			}
		}
		if ( results != null && locationId != null ) {
			for ( T datum : results ) {
				populateLocation(datum);
			}
		} else if ( results != null && results.size() > 0 && locationId == null
				&& requireLocationService ) {
			log.warn("Location required but not available, discarding datum: {}", results);
			results = Collections.emptyList();
		}
		return results;
	}

	@Override
	public T readCurrentDatum() {
		T datum = delegate.readCurrentDatum();
		if ( datum != null && locationId != null ) {
			populateLocation(datum);
		} else if ( datum != null && locationId == null && requireLocationService ) {
			log.warn("LocationService required but not available, discarding datum: {}", datum);
			datum = null;
		}
		return datum;
	}

	private void populateLocation(T datum) {
		if ( locationId != null && !shouldIgnoreDatum(datum) ) {
			log.debug("Augmenting datum {} with Locaiton ID {}", datum, locationId);
			if ( datum instanceof GeneralNodeDatum ) {
				((GeneralNodeDatum) datum).putStatusSampleValue(PricedDatum.PRICE_LOCATION_KEY,
						locationId);
			} else {
				BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(datum);
				bean.setPropertyValue(locationIdPropertyName, locationId);
			}
		}
	}

	private boolean shouldIgnoreDatum(T datum) {
		return (datumClassNameIgnore != null && datumClassNameIgnore
				.contains(datum.getClass().getName()));
	}

	@Override
	public String toString() {
		return delegate != null ? delegate.toString() + "[LocationDatumDataSource proxy]"
				: "LocationDatumDataSource";
	}

	@Override
	public String getUID() {
		return delegate.getUID();
	}

	@Override
	public String getGroupUID() {
		return delegate.getGroupUID();
	}

	@Override
	public String getSettingUID() {
		if ( delegate instanceof SettingSpecifierProvider ) {
			return ((SettingSpecifierProvider) delegate).getSettingUID();
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

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.add(getLocationSettingSpecifier());
		if ( delegate instanceof SettingSpecifierProvider ) {
			List<SettingSpecifier> delegateResult = ((SettingSpecifierProvider) delegate)
					.getSettingSpecifiers();
			if ( delegateResult != null ) {
				for ( SettingSpecifier spec : delegateResult ) {
					if ( spec instanceof KeyedSettingSpecifier<?> ) {
						KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
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
		if ( location == null && locationService != null && locationId != null ) {
			LocationService service = locationService.service();
			if ( service != null ) {
				location = service.getLocation(locationType, locationId);
			}
		}
		return new BasicLocationLookupSettingSpecifier("locationId", locationType, location);
	}

	public DatumDataSource<T> getDelegate() {
		return delegate;
	}

	public void setDelegate(DatumDataSource<T> delegate) {
		this.delegate = delegate;
	}

	public OptionalService<LocationService> getLocationService() {
		return locationService;
	}

	public void setLocationService(OptionalService<LocationService> locationService) {
		this.locationService = locationService;
	}

	public String getLocationIdPropertyName() {
		return locationIdPropertyName;
	}

	public void setLocationIdPropertyName(String locationIdPropertyName) {
		this.locationIdPropertyName = locationIdPropertyName;
	}

	public boolean isRequireLocationService() {
		return requireLocationService;
	}

	public void setRequireLocationService(boolean requireLocationService) {
		this.requireLocationService = requireLocationService;
	}

	public Class<? extends Location> getLocationType() {
		return locationType;
	}

	public void setLocationType(Class<? extends Location> locationType) {
		this.locationType = locationType;
	}

	public String getMessageBundleBasename() {
		return messageBundleBasename;
	}

	public void setMessageBundleBasename(String messageBundleBaseName) {
		this.messageBundleBasename = messageBundleBaseName;
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		if ( this.location != null && locationId != null
				&& !locationId.equals(this.location.getLocationId()) ) {
			this.location = null; // set to null so we re-fetch from server
		}
		this.locationId = locationId;
	}

	public Location getLocation() {
		return location;
	}

	public Set<String> getDatumClassNameIgnore() {
		return datumClassNameIgnore;
	}

	public void setDatumClassNameIgnore(Set<String> datumClassNameIgnore) {
		this.datumClassNameIgnore = datumClassNameIgnore;
	}

}
