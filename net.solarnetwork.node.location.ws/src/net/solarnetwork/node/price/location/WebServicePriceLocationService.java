/* ==================================================================
 * WebServicePriceLocationService.java - Feb 19, 2011 2:43:42 PM
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

package net.solarnetwork.node.price.location;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;

import net.solarnetwork.node.PriceLocation;
import net.solarnetwork.node.PriceLocationService;
import net.solarnetwork.node.support.XmlServiceSupport;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Web service implementation of {@link PriceLocationService}.
 * 
 * @author matt
 * @version $Revision$
 */
public class WebServicePriceLocationService extends XmlServiceSupport
implements PriceLocationService {

	/** Default value for the <code>cacheTtl</code> property. */
	public static final Long DEFAULT_CACHE_TTL = 1000L * 60 * 60 * 4;
	
	private String url;
	private Map<String, String> datumXPathMapping = null;
	private Long cacheTtl = DEFAULT_CACHE_TTL;
	
	private ConcurrentHashMap<String, CachedPriceLocation> cache 
		= new ConcurrentHashMap<String, CachedPriceLocation>(2);
	
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
		if ( datumXPathMapping == null ) {
			// create default XML response mapping
			Map<String, String> defaults = new LinkedHashMap<String, String>(3);
			defaults.put("locationId", "/*/@id");
			defaults.put("currency", "/*/@currency");
			defaults.put("unit", "/*/@unit");
			datumXPathMapping = defaults;
		}
		datumXPathExpMap = getXPathExpressionMap(datumXPathMapping);
	}
	
	@Override
	public PriceLocation findLocation(String sourceName, String locationName) {
		String cacheKey = sourceName +"-" +locationName;
		CachedPriceLocation cachedLocation = cache.get(cacheKey);
		if ( cachedLocation != null ) {
			if ( cachedLocation.expires > System.currentTimeMillis() ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Found cached " +cachedLocation.location 
							+" (expires in " 
							+(System.currentTimeMillis() - cachedLocation.expires)
							+"ms)");
				}
				return cachedLocation.location;
			}
		}
		BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(
				new PriceLocationQuery(sourceName, locationName));
		String postUrl = getIdentityService().getSolarInBaseUrl() +url;
		try {
			if ( log.isInfoEnabled() ) {
				log.info("Looking up PriceLocation for source [" +sourceName 
						+"] location [" +locationName +"] from [" +postUrl +"]");
			}
			PriceLocation loc = new PriceLocation();
			webFormGetForBean(bean, loc, postUrl, null, datumXPathExpMap);
			if ( loc.getLocationId() == null ) {
				if ( log.isWarnEnabled() ) {
					log.warn("PriceLocation not found for source [" +sourceName 
							+"] location [" +locationName +']');
				}
				return null;
			}
			
			if ( log.isDebugEnabled() ) {
				log.debug("Caching " +loc +" for up to " +cacheTtl +"ms");
			}
			
			cachedLocation = new CachedPriceLocation();
			cachedLocation.location = loc;
			cachedLocation.expires = Long.valueOf(System.currentTimeMillis() + cacheTtl);
			cache.put(cacheKey, cachedLocation);
			return loc;
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
					if ( log.isWarnEnabled() ) {
						log.warn("IOException looking up PriceLocation from [" +postUrl 
								+"], returning cached data even though cache has expired.");
					}
					return cachedLocation.location;
				}
			} else {
				throw e;
			}
		}
		return null;
	}
	
	private static class CachedPriceLocation {
		private Long expires;
		private PriceLocation location;
	}

	public static class PriceLocationQuery {
		
		private final String sourceName;
		private final String locationName;
		
		private PriceLocationQuery(String sourceName, String locationName) {
			this.sourceName = sourceName;
			this.locationName = locationName;
		}
		
		/**
		 * @return the sourceName
		 */
		public String getSourceName() {
			return sourceName;
		}
		
		/**
		 * @return the locationName
		 */
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
	 * @param url the url to set
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
	 * @param datumXPathMapping the datumXPathMapping to set
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
	 * @param cacheTtl the cacheTtl to set
	 */
	public void setCacheTtl(Long cacheTtl) {
		this.cacheTtl = cacheTtl;
	}
	
}
