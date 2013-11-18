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
import net.solarnetwork.node.Location;
import net.solarnetwork.node.LocationService;
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
	public static final Long DEFAULT_CACHE_TTL = 1000L * 60 * 60 * 4;

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
			//defaults.put("currency", "/*/@currency");
			//defaults.put("unit", "/*/@unit");
			datumXPathMapping = defaults;
		}
		datumXPathExpMap = getXPathExpressionMap(datumXPathMapping);
	}

	@Override
	public <T extends Location> Collection<T> findLocations(Class<T> locationType, String sourceName,
			String locationName) {
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
		String queryType = locationType.getSimpleName();
		if ( queryType.endsWith("Location") ) {
			queryType = queryType.substring(0, queryType.length() - 8);
		}
		BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(new LocationQuery(queryType,
				sourceName, locationName));
		try {
			if ( log.isInfoEnabled() ) {
				log.info("Looking up {} for source [{}] location [{}] from [{}]",
						locationType.getSimpleName(), sourceName, locationName, url);
			}
			T loc = locationType.newInstance();
			webFormGetForBean(bean, loc, postUrl, null, datumXPathExpMap);
			if ( loc.getLocationId() == null ) {
				log.warn("{} not found for source [{}] location [{}]", locationType.getSimpleName(),
						sourceName, locationName);
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

	private static class CachedLocation {

		private Long expires;
		private Location location;
	}

	public static class LocationQuery {

		private final String type;
		private final String sourceName;
		private final String locationName;

		private LocationQuery(String type, String sourceName, String locationName) {
			this.type = type;
			this.sourceName = sourceName;
			this.locationName = locationName;
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

	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 *        the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the datumXPathMapping
	 */
	public Map<String, String> getDatumXPathMapping() {
		return datumXPathMapping;
	}

	/**
	 * @param datumXPathMapping
	 *        the datumXPathMapping to set
	 */
	public void setDatumXPathMapping(Map<String, String> datumXPathMapping) {
		this.datumXPathMapping = datumXPathMapping;
	}

	/**
	 * @return the cacheTtl
	 */
	public Long getCacheTtl() {
		return cacheTtl;
	}

	/**
	 * @param cacheTtl
	 *        the cacheTtl to set
	 */
	public void setCacheTtl(Long cacheTtl) {
		this.cacheTtl = cacheTtl;
	}

}
