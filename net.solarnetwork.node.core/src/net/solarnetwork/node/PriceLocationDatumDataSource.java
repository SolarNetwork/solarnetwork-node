/* ==================================================================
 * PriceLocationDatumDataSource.java - Feb 21, 2011 5:23:28 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.solarnetwork.node.settings.KeyedSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.solarnetwork.util.OptionalServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * {@link DatumDataSource} that augments some other data source's datum values
 * with price location IDs.
 * 
 * <p>This is to be used to easily augment various datum that relate to price
 * with the necessary {@link PriceLocation} ID. This class also implements the
 * {@link MultiDatumDataSource} API, and will call the methods of that API on
 * the configured {@code delegate} if that also implements {@link MultiDatumDataSource}.
 * If the {@code delegate} does not implement {@link MultiDatumDataSource} this
 * class will "fake" that API by calling {@link DatumDataSource#readCurrentDatum()}
 * and returning that object in a Collection.</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>delegate</dt>
 *   <dd>The {@link DatumDataSource} to delegate to.</dd>
 *   
 *   <dt>priceLocationService</dt>
 *   <dd>The {@link PriceLocationService} to use to lookup {@link PriceLocation}
 *   instances via the configured {@code priceSource} and {@code priceLocation}
 *   properties.</dd>
 *   
 *   <dt>priceSource</dt>
 *   <dd>The {@link PriceLocation} source to look up.</dd>
 *   
 *   <dt>priceLocation</dt>
 *   <dd>The {@link PriceLocation} location to look up.</dd>
 *   
 *   <dt>priceLocationIdPropertyName</dt>
 *   <dd>The JavaBean property name to set the found {@link PriceLocation#getLocationId()}
 *   to on the {@link Datum} returned from the configured {@code delegate}. The object
 *   must support a JavaBean setter method for this property. Defaults to 
 *   {@link #DEFAULT_PRICE_LOCATION_ID_PROP_NAME}.</dd>
 *   
 *   <dt>requirePriceLocationService</dt>
 *   <dd>If configured as <em>true</em> then return <em>null</em> data only instead
 *   of calling the delegate. This is designed for services that require a price
 *   location ID to be set, for example a PriceDatum logger. Defaults to <em>false</em>.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class PriceLocationDatumDataSource<T extends Datum>
 implements DatumDataSource<T>,
		MultiDatumDataSource<T>, SettingSpecifierProvider {
	
	/** Default value for the {@code priceLocationIdPropertyName} property. */
	public static final String DEFAULT_PRICE_LOCATION_ID_PROP_NAME = "locationId";
	
	private DatumDataSource<T> delegate;
	private OptionalServiceTracker<PriceLocationService> priceLocationService;
	private String priceSource = PriceLocationService.UNKNOWN_SOURCE;
	private String priceLocation = PriceLocationService.UNKNOWN_LOCATION;
	private String priceLocationIdPropertyName = DEFAULT_PRICE_LOCATION_ID_PROP_NAME;
	private boolean requirePriceLocationService = false;

	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public Class<? extends T> getDatumType() {
		return delegate.getDatumType();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends T> getMultiDatumType() {
		if ( delegate instanceof MultiDatumDataSource ) {
			return ((MultiDatumDataSource<T>)delegate).getMultiDatumType();
		}
		return delegate.getDatumType();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<T> readMultipleDatum() {
		Collection<T> results = null;
		if ( delegate instanceof MultiDatumDataSource ) {
			results = ((MultiDatumDataSource<T>)delegate).readMultipleDatum();
		} else {
			// fake multi API
			results = new ArrayList<T>(1);
			T datum = delegate.readCurrentDatum();
			if ( datum != null ) {
				results.add(datum);
			}
		}
		
		if ( results != null && priceLocationService.isAvailable() 
				&& priceLocation != null && priceLocation.length() > 0
				&& priceSource != null && priceLocation.length() > 0 ) {
			for ( T datum : results ) {
				populatePriceLocation(datum);
			}
		} else if ( results != null && results.size() > 0 && requirePriceLocationService ) {
			log.warn("PriceLocationService required but not available, discarding datum: {}", results);
			results = Collections.emptyList();
		}
		return results;
	}

	@Override
	public T readCurrentDatum() {
		T datum = delegate.readCurrentDatum();
		if ( datum != null && priceLocationService.isAvailable() ) {
			populatePriceLocation(datum);
		} else if ( datum != null && requirePriceLocationService ) {
			log.warn("PriceLocationService required but not available, discarding datum: {}", datum);
			datum = null;
		}
		return datum;
	}
	
	private void populatePriceLocation(T datum) {
		PriceLocation loc = priceLocationService.getService().findLocation(
				priceSource, priceLocation);
		if ( loc != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug("Augmenting datum " +datum +" with PriceLocation " +loc);
			}
			BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(datum);
			bean.setPropertyValue(priceLocationIdPropertyName, loc.getLocationId());
		}
	}

	@Override
	public String toString() {
		return delegate != null 
				? delegate.toString()+"[PriceLocationDatumDataSource proxy]"
				: "PriceLocationDatumDataSource";
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
		MessageSource other = null;
		if ( delegate instanceof SettingSpecifierProvider ) {
			other = ((SettingSpecifierProvider) delegate).getMessageSource();
		}
		if ( messageSource == null ) {
			PrefixedMessageSource delegateSource = null;
			if ( other != null ) {
				delegateSource = new PrefixedMessageSource();
				delegateSource.setDelegate(other);
				delegateSource.setPrefix("delegate.");
			}

			ResourceBundleMessageSource priceSource = new ResourceBundleMessageSource();
			priceSource.setBundleClassLoader(getClass().getClassLoader());
			priceSource.setBasename(getClass().getName());
			if ( delegateSource != null ) {
				priceSource.setParentMessageSource(delegateSource);
			}

			messageSource = priceSource;
		}
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.add(new BasicTextFieldSettingSpecifier("priceSource", ""));
		result.add(new BasicTextFieldSettingSpecifier("priceLocation", ""));
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

	public DatumDataSource<T> getDelegate() {
		return delegate;
	}
	public void setDelegate(DatumDataSource<T> delegate) {
		this.delegate = delegate;
	}
	public OptionalServiceTracker<PriceLocationService> getPriceLocationService() {
		return priceLocationService;
	}
	public void setPriceLocationService(
			OptionalServiceTracker<PriceLocationService> priceLocationService) {
		this.priceLocationService = priceLocationService;
	}
	public String getPriceSource() {
		return priceSource;
	}
	public void setPriceSource(String priceSource) {
		this.priceSource = priceSource;
	}
	public String getPriceLocation() {
		return priceLocation;
	}
	public void setPriceLocation(String priceLocation) {
		this.priceLocation = priceLocation;
	}
	public String getPriceLocationIdPropertyName() {
		return priceLocationIdPropertyName;
	}
	public void setPriceLocationIdPropertyName(String priceLocationIdPropertyName) {
		this.priceLocationIdPropertyName = priceLocationIdPropertyName;
	}
	public boolean isRequirePriceLocationService() {
		return requirePriceLocationService;
	}
	public void setRequirePriceLocationService(boolean requirePriceLocationService) {
		this.requirePriceLocationService = requirePriceLocationService;
	}

}
