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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.solarnetwork.domain.GeneralLocationSourceMetadata;
import net.solarnetwork.node.LocationService;
import net.solarnetwork.node.domain.BasicGeneralLocation;
import net.solarnetwork.node.domain.BasicLocation;
import net.solarnetwork.node.domain.GeneralLocation;
import net.solarnetwork.node.domain.Location;
import net.solarnetwork.node.domain.PriceLocation;
import net.solarnetwork.node.domain.WeatherLocation;
import net.solarnetwork.node.support.JsonHttpClientSupport;

/**
 * Web service implementation of {@link WebServiceLocationService}.
 * 
 * @author matt
 * @version 1.1
 */
public class WebServiceLocationService extends JsonHttpClientSupport implements LocationService {

	/** Default value for the <code>cacheTtl</code> property. */
	public static final Long DEFAULT_CACHE_TTL = 1000L * 60 * 60 * 24 * 7;

	private String url = "/api/v1/sec/location";
	private Long cacheTtl = DEFAULT_CACHE_TTL;

	private final ConcurrentHashMap<String, CachedLocation> cache = new ConcurrentHashMap<String, CachedLocation>(
			2);

	@Override
	public <T extends Location> T getLocation(Class<T> locationType, Long locationId) {
		Set<String> tags = new HashSet<String>(1);
		if ( PriceLocation.class.isAssignableFrom(locationType) ) {
			tags.add(Location.PRICE_TYPE);
		} else if ( WeatherLocation.class.isAssignableFrom(locationType) ) {
			tags.add(Location.WEATHER_TYPE);
		} else {
			throw new IllegalArgumentException("The locationType " + locationType.getName()
					+ " is not supported");
		}
		final String url = locationSourceMetadataUrl(null, locationId, null, tags);
		try {
			final InputStream in = jsonGET(url);
			Collection<GeneralLocationSourceMetadata> results = extractCollectionResponseData(in,
					GeneralLocationSourceMetadata.class);
			if ( results != null && results.size() > 0 ) {
				GeneralLocationSourceMetadata meta = results.iterator().next();
				return convertLocationSourceMetadata(locationType, meta);
			}
			return null;
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException querying for location source metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	private <T extends Location> T convertLocationSourceMetadata(Class<T> locationType,
			GeneralLocationSourceMetadata meta) {
		T result;
		try {
			result = locationType.newInstance();
		} catch ( InstantiationException e ) {
			throw new RuntimeException(e);
		} catch ( IllegalAccessException e ) {
			throw new RuntimeException(e);
		}
		if ( result instanceof BasicLocation ) {
			BasicLocation loc = (BasicLocation) result;
			loc.setLocationId(meta.getLocationId());
			loc.setSourceId(meta.getSourceId());
			if ( meta.getMeta() != null ) {
				loc.setLocationName(meta.getMeta().getInfoString("name"));
			}
		}
		return result;
	}

	@Override
	public <T extends Location> Collection<T> findLocations(final Class<T> locationType,
			final String sourceName, final String locationName) {
		Set<String> tags = new HashSet<String>(1);
		if ( PriceLocation.class.isAssignableFrom(locationType) ) {
			tags.add(Location.PRICE_TYPE);
		} else if ( WeatherLocation.class.isAssignableFrom(locationType) ) {
			tags.add(Location.WEATHER_TYPE);
		} else {
			throw new IllegalArgumentException("The locationType " + locationType.getName()
					+ " is not supported");
		}
		final String url = locationSourceMetadataUrl(sourceName + ' ' + locationName, null, null, tags);
		try {
			final InputStream in = jsonGET(url);
			Collection<GeneralLocationSourceMetadata> results = extractCollectionResponseData(in,
					GeneralLocationSourceMetadata.class);
			Collection<T> col = new ArrayList<T>();
			if ( results != null ) {
				for ( GeneralLocationSourceMetadata meta : results ) {
					col.add(convertLocationSourceMetadata(locationType, meta));
				}
			}
			return col;
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException querying for location source metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	private String locationSourceMetadataUrl(String query, Long locationId, String sourceId,
			Set<String> tags) {
		StringBuilder buf = new StringBuilder(getIdentityService().getSolarInBaseUrl());
		buf.append(url);
		StringBuilder q = new StringBuilder();
		if ( query != null ) {
			appendXWWWFormURLEncodedValue(q, "query", query);
		}
		if ( locationId != null ) {
			appendXWWWFormURLEncodedValue(q, "locationId", locationId);
		}
		if ( sourceId != null ) {
			appendXWWWFormURLEncodedValue(q, "sourceId", sourceId);
		}
		if ( tags != null ) {
			for ( String tag : tags ) {
				appendXWWWFormURLEncodedValue(q, "tags", tag);
			}
		}
		if ( q.length() > 0 ) {
			buf.append('?').append(q);
		}
		return buf.toString();
	}

	private CachedLocation getCachedLocation(String key) {
		CachedLocation cachedLocation = cache.get(key);
		if ( cachedLocation != null ) {
			if ( cachedLocation.expires > System.currentTimeMillis() ) {
				log.debug("Found cached location {} (expires in {}ms)", cachedLocation,
						(System.currentTimeMillis() - cachedLocation.expires));
				return cachedLocation;
			}
		}
		return null;
	}

	@Override
	public Collection<GeneralLocationSourceMetadata> findLocationMetadata(String query, String sourceId,
			Set<String> tags) {
		final String url = locationSourceMetadataUrl(query, null, sourceId, tags);
		try {
			final InputStream in = jsonGET(url);
			return extractCollectionResponseData(in, GeneralLocationSourceMetadata.class);
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException querying for location source metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	private String locationSourceMetadataUrl(Long locationId, String sourceId) {
		StringBuilder buf = new StringBuilder(getIdentityService().getSolarInBaseUrl());
		buf.append(url);
		if ( locationId != null ) {
			buf.append('/').append(locationId);
		}
		StringBuilder q = new StringBuilder();
		if ( sourceId != null ) {
			appendXWWWFormURLEncodedValue(q, "sourceId", sourceId);
		}
		if ( q.length() > 0 ) {
			buf.append('?').append(q);
		}
		return buf.toString();
	}

	@Override
	public GeneralLocationSourceMetadata getLocationMetadata(Long locationId, String sourceId) {
		final String url = locationSourceMetadataUrl(locationId, sourceId);
		final String cacheKey = url;
		CachedLocation cached = getCachedLocation(cacheKey);
		if ( cached != null && cached instanceof GeneralLocation ) {
			return cached.asGeneralLocationSourceMetadata();
		}

		try {
			final InputStream in = jsonGET(url);
			GeneralLocationSourceMetadata meta = extractResponseData(in,
					GeneralLocationSourceMetadata.class);
			CachedLocation cachedLocation = new CachedLocation(meta);
			cachedLocation.expires = Long.valueOf(System.currentTimeMillis() + cacheTtl);
			cache.put(cacheKey, cachedLocation);
			return cachedLocation.asGeneralLocationSourceMetadata();
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException querying for location source metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	private static class CachedLocation {

		private Long expires;
		private final BasicGeneralLocation location;

		private CachedLocation(GeneralLocationSourceMetadata meta) {
			super();
			BasicGeneralLocation loc = new BasicGeneralLocation();
			loc.setLocationId(meta.getLocationId());
			loc.setSourceId(meta.getSourceId());
			loc.setSourceMetadata(meta);
			this.location = loc;
		}

		public GeneralLocationSourceMetadata asGeneralLocationSourceMetadata() {
			if ( location != null && location.getSourceMetadata() != null ) {
				return location.getSourceMetadata();
			}
			return new GeneralLocationSourceMetadata();
		}

		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder("CachedLocation{locationId=");
			if ( location != null ) {
				buf.append(location.getLocationId());
			}
			buf.append(",sourceId=");
			if ( location != null ) {
				buf.append(location.getSourceId());
			}
			buf.append('}');
			return buf.toString();
		}

	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Long getCacheTtl() {
		return cacheTtl;
	}

	public void setCacheTtl(Long cacheTtl) {
		this.cacheTtl = cacheTtl;
	}

}
