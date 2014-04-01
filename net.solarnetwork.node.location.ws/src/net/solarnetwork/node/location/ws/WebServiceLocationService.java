/* ==================================================================
 * WebServiceLocationService.java - Feb 19, 2011 2:43:42 PM
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

package net.solarnetwork.node.location.ws;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import net.solarnetwork.node.LocationService;
import net.solarnetwork.node.domain.Location;
import net.solarnetwork.node.support.XmlServiceSupport;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Web service implementation of {@link PriceLocationService}.
 * 
 * @author matt
 * @version 1.1
 */
public class WebServiceLocationService extends XmlServiceSupport implements LocationService {

	/** Default value for the <code>cacheTtl</code> property. */
	public static final Long DEFAULT_CACHE_TTL = 1000L * 60 * 60 * 24 * 7;

	private String url;
	private Map<String, String> datumXPathMapping = null;
	private Long cacheTtl = DEFAULT_CACHE_TTL;

	private final ConcurrentHashMap<String, CachedLocation> cache = new ConcurrentHashMap<String, CachedLocation>(
			2);

	private Map<String, XPathExpression> datumXPathExpMap = null;

	/**
	 * Initialize this class after properties are set.
	 */
	@Override
	public void init() {
		super.init();
		XPath xp = getXpathFactory().newXPath();
		if ( getNsContext() != null ) {
			xp.setNamespaceContext(getNsContext());
		}
		// FIXME: map element response attributes to bean property names automatically
		if ( datumXPathMapping == null ) {
			// create default XML response mapping
			Map<String, String> defaults = new LinkedHashMap<String, String>(3);
			defaults.put("locationId", "//*[@id][1]/@id"); // grab the first result ID
			defaults.put("sourceName", "//*[@id][1]/@sourceName"); // grab the first result ID
			defaults.put("locationName", "//*[@id][1]/@locationName"); // grab the first result ID
			datumXPathMapping = defaults;
		}
		datumXPathExpMap = getXPathExpressionMap(datumXPathMapping);
	}

	@Override
	public <T extends Location> T getLocation(Class<T> locationType, Long locationId) {
		if ( locationId == null ) {
			Collection<T> results = findLocations(locationType, UNKNOWN_SOURCE, UNKNOWN_LOCATION);
			if ( results.isEmpty() ) {
				return null;
			}
			return results.iterator().next();
		}
		final String postUrl = getIdentityService().getSolarInBaseUrl() + url;
		final String cacheKey = postUrl + ";" + locationType.getName() + ";" + locationId;
		CachedLocation cachedLocation = cache.get(cacheKey);
		if ( cachedLocation != null ) {
			if ( cachedLocation.expires > System.currentTimeMillis() ) {
				log.debug("Found cached {} (expires in {}ms)", cachedLocation.location,
						(System.currentTimeMillis() - cachedLocation.expires));
				@SuppressWarnings("unchecked")
				T result = (T) cachedLocation.location;
				return result;
			}
		}
		final BeanWrapper bean = getQueryBean(locationType, locationId, null, null);
		log.info("Looking up {} for location ID {} from [{}]", locationType.getSimpleName(), locationId,
				url);
		Collection<T> results = postForLocations(locationType, bean, postUrl, cacheKey);
		if ( results.isEmpty() ) {
			return null;
		}
		return results.iterator().next();
	}

	private BeanWrapper getQueryBean(Class<?> locationType, Long id, String sourceName,
			String locationName) {
		String queryType = locationType.getSimpleName();
		if ( queryType.endsWith("Location") ) {
			queryType = queryType.substring(0, queryType.length() - 8);
		}
		LocationQuery q = (id == null ? new LocationQuery(queryType, sourceName, locationName)
				: new LocationQuery(queryType, id));
		BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(q);
		return bean;
	}

	private <T extends Location> Collection<T> postForLocations(final Class<T> locationType,
			BeanWrapper bean, String postUrl, String cacheKey) {
		CachedLocation cachedLocation = null;
		try {
			T loc = locationType.newInstance();
			webFormGetForBean(bean, loc, postUrl, null, datumXPathExpMap);
			if ( loc.getLocationId() == null ) {
				log.warn("{} not found for {}", bean.getWrappedInstance());
				return null;
			}

			log.debug("Caching {} for up to {}ms", loc, cacheTtl);

			cachedLocation = new CachedLocation();
			cachedLocation.location = loc;
			cachedLocation.expires = Long.valueOf(System.currentTimeMillis() + cacheTtl);
			cache.put(cacheKey, cachedLocation);
			return Collections.singleton(loc);
		} catch ( RuntimeException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			if ( root instanceof IOException ) {
				// Perhaps the service is down right now... so if we have a cached location
				// available, let's return that, even though if we reached here the cache
				// has expired. This allows us to keep associating price data while the 
				// service is not available as long as it was available previously.
				if ( cachedLocation != null ) {
					log.warn(
							"IOException looking up {} Location from [{}], returning cached data even though cache has expired.",
							locationType.getSimpleName(), postUrl);
					@SuppressWarnings("unchecked")
					Set<T> locs = (Set<T>) Collections.singleton(cachedLocation.location);
					return locs;
				}
			} else {
				throw e;
			}
		} catch ( InstantiationException e ) {
			throw new RuntimeException(e);
		} catch ( IllegalAccessException e ) {
			throw new RuntimeException(e);
		}
		return Collections.emptyList();
	}

	@Override
	public <T extends Location> Collection<T> findLocations(final Class<T> locationType,
			final String sourceName, final String locationName) {
		final String postUrl = getIdentityService().getSolarInBaseUrl() + url;
		final String cacheKey = postUrl + ";" + locationType.getName() + ";" + sourceName + ";"
				+ locationName;
		CachedLocation cachedLocation = cache.get(cacheKey);
		if ( cachedLocation != null ) {
			if ( cachedLocation.expires > System.currentTimeMillis() ) {
				log.debug("Found cached {} (expires in {}ms)", cachedLocation.location,
						(System.currentTimeMillis() - cachedLocation.expires));

				@SuppressWarnings("unchecked")
				Set<T> locs = (Set<T>) Collections.singleton(cachedLocation.location);
				return locs;
			}
		}
		final BeanWrapper bean = getQueryBean(locationType, null, sourceName, locationName);
		log.info("Looking up {} for source [{}] location [{}] from [{}]", locationType.getSimpleName(),
				sourceName, locationName, url);
		return postForLocations(locationType, bean, postUrl, cacheKey);
	}

	private static class CachedLocation {

		private Long expires;
		private Location location;
	}

	public static class LocationQuery {

		private final String type;
		private final Long id;
		private final String sourceName;
		private final String locationName;

		private LocationQuery(String type, Long id) {
			super();
			this.id = id;
			this.type = type;
			this.sourceName = null;
			this.locationName = null;
		}

		private LocationQuery(String type, String sourceName, String locationName) {
			super();
			this.id = null;
			this.type = type;
			this.sourceName = sourceName;
			this.locationName = locationName;
		}

		@Override
		public String toString() {
			return "LocationQuery{type=" + type + (id != null ? ",id=" + id : "")
					+ (sourceName != null ? ",source=" + sourceName : "")
					+ (locationName != null ? ",location=" + locationName : "") + "}";
		}

		public String getType() {
			return type;
		}

		public String getSourceName() {
			return sourceName;
		}

		public String getLocationName() {
			return locationName;
		}

		public Long getId() {
			return id;
		}

	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Map<String, String> getDatumXPathMapping() {
		return datumXPathMapping;
	}

	public void setDatumXPathMapping(Map<String, String> datumXPathMapping) {
		this.datumXPathMapping = datumXPathMapping;
	}

	public Long getCacheTtl() {
		return cacheTtl;
	}

	public void setCacheTtl(Long cacheTtl) {
		this.cacheTtl = cacheTtl;
	}

}
