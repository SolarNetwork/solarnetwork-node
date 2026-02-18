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

import static java.lang.String.valueOf;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.SimpleLocation;
import net.solarnetwork.domain.datum.GeneralLocationSourceMetadata;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.datum.SimpleDatumLocation;
import net.solarnetwork.node.service.LocationService;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.CachedResult;

/**
 * Web service implementation of {@link WebServiceLocationService}.
 *
 * @author matt
 * @version 2.1
 */
public class WebServiceLocationService extends JsonHttpClientSupport
		implements LocationService, SettingSpecifierProvider {

	/** Default value for the <code>cacheTtl</code> property. */
	public static final Long DEFAULT_CACHE_TTL = 1000L * 60 * 60 * 24 * 7;

	/**
	 * The setting group used for location properties.
	 *
	 * @since 1.2
	 */
	public static final String SETTING_GROUP_LOCATION = "solarnode.location";

	/**
	 * The setting key for latitude.
	 *
	 * @since 1.2
	 */
	public static final String SETTING_LATITUDE = "latitude";

	/**
	 * The setting key for longitude.
	 *
	 * @since 1.2
	 */
	public static final String SETTING_LONGITUDE = "longitude";

	/**
	 * The setting key for elevation.
	 *
	 * @since 1.2
	 */
	public static final String SETTING_ELEVATION = "elevation";

	/**
	 * The default {@code minLatLonDeviation} property value.
	 *
	 * @since 1.2
	 */
	public static final double DEFAULT_MIN_LAT_LON_DEVIATION = 20.0;

	private final ConcurrentHashMap<String, CachedLocation> cache = new ConcurrentHashMap<String, CachedLocation>(
			2);

	private final SettingDao settingDao;
	private String url = "/api/v1/sec/location";
	private Long cacheTtl = DEFAULT_CACHE_TTL;
	private double minLatLonDeviation = DEFAULT_MIN_LAT_LON_DEVIATION;

	private CachedResult<Location> cachedNodeLocation;
	private Location nodeLocation;

	/**
	 * Constructor.
	 *
	 * @param settingDao
	 *        the setting DAO to use
	 * @throws IllegalArgumentException
	 *         if {@code settingDao} is {@literal null}
	 */
	public WebServiceLocationService(SettingDao settingDao) {
		super();
		if ( settingDao == null ) {
			throw new IllegalArgumentException("The settingDao argument must not be null.");
		}
		this.settingDao = settingDao;
	}

	/**
	 * Call after properties configured.
	 */
	public void startup() {
		SimpleLocation loc = new SimpleLocation();
		loc.setLatitude(BigDecimal.ZERO);
		loc.setLongitude(BigDecimal.ZERO);
		loc.setElevation(BigDecimal.ZERO);
		for ( KeyValuePair kv : settingDao.getSettingValues(SETTING_GROUP_LOCATION) ) {
			try {
				if ( SETTING_LATITUDE.equals(kv.getKey()) ) {
					loc.setLatitude(new BigDecimal(kv.getValue()));
				} else if ( SETTING_LONGITUDE.equals(kv.getKey()) ) {
					loc.setLongitude(new BigDecimal(kv.getValue()));
				} else if ( SETTING_ELEVATION.equals(kv.getKey()) ) {
					loc.setElevation(new BigDecimal(kv.getValue()));
				}
			} catch ( NumberFormatException e ) {
				log.warn("Ignoring invalid node location setting {} value [{}]: {}", kv.getKey(),
						kv.getValue(), e.getMessage());
			}
		}
		nodeLocation = new net.solarnetwork.domain.BasicLocation(loc);
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
		if ( cached != null ) {
			return cached.asGeneralLocationSourceMetadata();
		}

		try {
			final InputStream in = jsonGET(url);
			GeneralLocationSourceMetadata meta = extractResponseData(in,
					GeneralLocationSourceMetadata.class);
			CachedLocation cachedLocation = new CachedLocation(meta,
					System.currentTimeMillis() + cacheTtl);
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

	private String locationUpdateUrl() {
		StringBuilder buf = new StringBuilder(getIdentityService().getSolarInBaseUrl());
		buf.append(url);
		buf.append("/update");
		return buf.toString();
	}

	private String locationViewUrl() {
		StringBuilder buf = new StringBuilder(getIdentityService().getSolarInBaseUrl());
		buf.append(url);
		buf.append("/view");
		return buf.toString();
	}

	@Override
	public synchronized Location getNodeLocation() {
		if ( cachedNodeLocation != null && cachedNodeLocation.isValid() ) {
			return cachedNodeLocation.getResult();
		}
		final String url = locationViewUrl();
		log.debug("Fetching node location");
		try (InputStream in = jsonGET(url)) {
			Location loc = extractResponseData(in, Location.class);
			if ( loc != null && cacheTtl != null ) {
				cachedNodeLocation = new CachedResult<>(loc, cacheTtl, TimeUnit.MILLISECONDS);
			}
			return loc;
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException fetching node location at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to fetch node location: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void updateNodeLocation(net.solarnetwork.domain.Location location) {
		final double dist = distanceBetween(nodeLocation, location);
		if ( dist < minLatLonDeviation ) {
			log.debug("Ignoring node location change becauase delta distance {} less than minimum {}",
					dist, minLatLonDeviation);
			return;
		}
		final String url = locationUpdateUrl();
		log.info("Updating node location ({}, {}, {})", location.getLatitude(), location.getLongitude(),
				location.getElevation());
		try (InputStream in = jsonPOST(url, location)) {
			extractResponseData(in, Object.class);
			settingDao.storeSetting(SETTING_GROUP_LOCATION, SETTING_LATITUDE,
					location.getLatitude().toPlainString());
			settingDao.storeSetting(SETTING_GROUP_LOCATION, SETTING_LONGITUDE,
					location.getLongitude().toPlainString());
			settingDao.storeSetting(SETTING_GROUP_LOCATION, SETTING_ELEVATION,
					location.getElevation().toPlainString());
			this.nodeLocation = new net.solarnetwork.domain.BasicLocation(location);
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException updating location at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to update location: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	private static final double EARTH_RADIUS = 6_378_137.0;

	private double distanceBetween(net.solarnetwork.domain.Location loc1,
			net.solarnetwork.domain.Location loc2) {
		if ( loc1 == null || loc2 == null || loc1.getLatitude() == null || loc1.getLongitude() == null
				|| loc2.getLatitude() == null || loc2.getLongitude() == null ) {
			return -1;
		}

		// Adapted from https://www.movable-type.co.uk/scripts/latlong.html
		double lat1 = loc1.getLatitude().doubleValue();
		double lon1 = loc1.getLongitude().doubleValue();
		double lat2 = loc2.getLatitude().doubleValue();
		double lon2 = loc2.getLongitude().doubleValue();

		double phi1 = lat1 * Math.PI / 180.0;
		double phi2 = lat2 * Math.PI / 180.0;
		double deltaPhi = (lat2 - lat1) * Math.PI / 180.0;
		double deltaLambda = (lon2 - lon1) * Math.PI / 180.0;

		double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) + Math.cos(phi1) * Math.cos(phi2)
				* Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return EARTH_RADIUS * c;
	}

	private static class CachedLocation {

		private final long expires;
		private final SimpleDatumLocation location;

		private CachedLocation(GeneralLocationSourceMetadata meta, long expires) {
			super();
			SimpleDatumLocation loc = new SimpleDatumLocation();
			loc.setLocationId(meta.getLocationId());
			loc.setSourceId(meta.getSourceId());
			loc.setSourceMetadata(meta);
			this.location = loc;
			this.expires = expires;
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

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.location.ws";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>();
		result.add(new BasicTextFieldSettingSpecifier("minLatLonDeviation",
				valueOf(DEFAULT_MIN_LAT_LON_DEVIATION)));
		return result;
	}

	/**
	 * Get the URL.
	 *
	 * @return the URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL.
	 *
	 * @param url
	 *        the URL to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get the cache TTL.
	 *
	 * @return the cache TTL
	 */
	public Long getCacheTtl() {
		return cacheTtl;
	}

	/**
	 * Set the cache TTL.
	 *
	 * @param cacheTtl
	 *        the cache TTL to set
	 */
	public void setCacheTtl(Long cacheTtl) {
		this.cacheTtl = cacheTtl;
	}

	/**
	 * Get the minimum lat/lon deviation.
	 *
	 * @return the deviation, in meters
	 */
	public double getMinLatLonDeviation() {
		return minLatLonDeviation;
	}

	/**
	 * Set the maximum lat/lon deviation.
	 *
	 * @param minLatLonDeviation
	 *        the deviation to set, in meters
	 */
	public void setMinLatLonDeviation(double minLatLonDeviation) {
		this.minLatLonDeviation = minLatLonDeviation;
	}

}
