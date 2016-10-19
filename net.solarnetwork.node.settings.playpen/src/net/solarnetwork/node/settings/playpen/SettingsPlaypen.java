/* ==================================================================
 * SettingsPlaypen.java - Nov 2, 2012 4:16:05 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.settings.playpen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.GeneralLocationSourceMetadata;
import net.solarnetwork.node.LocationService;
import net.solarnetwork.node.domain.BasicGeneralLocation;
import net.solarnetwork.node.domain.Location;
import net.solarnetwork.node.settings.LocationLookupSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicLocationLookupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicRadioGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicSetupResourceSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicSliderSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.util.OptionalServiceTracker;

/**
 * A test bed experiment for the settings framework.
 * 
 * @author matt
 * @version 1.3
 */
public class SettingsPlaypen implements SettingSpecifierProvider {

	private static final String DEFAULT_STRING = "simple";
	private static final Integer DEFAULT_INTEGER = 42;
	private static final Double DEFAULT_SLIDE = 5.0;
	private static final String[] DEFAULT_RADIO = new String[] { "One", "Two", "Three" };
	private static final String[] DEFAULT_MENU = new String[] { "Option 1", "Option 2", "Option 3" };

	private String string = DEFAULT_STRING;
	private String password = null;
	private Integer integer = DEFAULT_INTEGER;
	private Boolean toggle = Boolean.FALSE;
	private Double slide = DEFAULT_SLIDE;
	private String radio = DEFAULT_RADIO[0];
	private String menu = DEFAULT_MENU[0];

	// group support
	private List<String> listString = new ArrayList<String>(4);
	private List<ComplexListItem> listComplex = new ArrayList<ComplexListItem>(4);

	private OptionalServiceTracker<LocationService> locationService;
	private Long locationId;
	private String sourceId;
	private Location location;

	private Long weatherLocationId;
	private String weatherSourceId;
	private Location weatherLocation;

	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private SetupResourceProvider customSettingResourceProvider;

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.settings.playpen";
	}

	@Override
	public String getDisplayName() {
		return "Settings Playpen";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>();

		SettingsPlaypen defaults = new SettingsPlaypen();

		results.add(new BasicTextFieldSettingSpecifier("string", defaults.getString()));
		results.add(new BasicTextFieldSettingSpecifier("password", defaults.getPassword(), true));
		results.add(new BasicTextFieldSettingSpecifier("integer", defaults.getInteger().toString()));
		results.add(new BasicToggleSettingSpecifier("toggle", defaults.getToggle()));
		results.add(new BasicSliderSettingSpecifier("slide", defaults.getSlide(), 0.0, 10.0, 0.5));

		BasicRadioGroupSettingSpecifier radioSpec = new BasicRadioGroupSettingSpecifier("radio",
				defaults.getRadio());
		Map<String, String> radioValues = new LinkedHashMap<String, String>(3);
		for ( String s : DEFAULT_RADIO ) {
			radioValues.put(s, s);
		}
		radioSpec.setValueTitles(radioValues);
		results.add(radioSpec);

		// drop-down menu
		BasicMultiValueSettingSpecifier menuSpec = new BasicMultiValueSettingSpecifier("menu",
				defaults.getMenu());
		Map<String, String> menuValues = new LinkedHashMap<String, String>(3);
		for ( String s : DEFAULT_MENU ) {
			menuValues.put(s, s);
		}
		menuSpec.setValueTitles(menuValues);
		results.add(menuSpec);

		results.add(getLocationSettingSpecifier());
		results.add(getWeatherLocationSettingSpecifier());

		// custom UI
		results.add(new BasicSetupResourceSettingSpecifier(customSettingResourceProvider,
				Collections.singletonMap("foo", "bar")));

		// basic dynamic list of strings
		Collection<String> listStrings = getListString();
		BasicGroupSettingSpecifier listStringGroup = SettingsUtil.dynamicListSettingSpecifier(
				"listString", listStrings, new SettingsUtil.KeyedListCallback<String>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(String value, int index,
							String key) {
						return Collections.<SettingSpecifier> singletonList(
								new BasicTextFieldSettingSpecifier(key, ""));
					}
				});
		results.add(listStringGroup);

		// dynamic list of objects
		Collection<ComplexListItem> listComplexes = getListComplex();
		BasicGroupSettingSpecifier listComplexGroup = SettingsUtil.dynamicListSettingSpecifier(
				"listComplex", listComplexes, new SettingsUtil.KeyedListCallback<ComplexListItem>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(ComplexListItem value,
							int index, String key) {
						BasicGroupSettingSpecifier personGroup = new BasicGroupSettingSpecifier(
								value.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(personGroup);
					}
				});
		results.add(listComplexGroup);

		return results;
	}

	private LocationLookupSettingSpecifier getLocationSettingSpecifier() {
		if ( location == null && locationService != null && locationId != null && sourceId != null ) {
			LocationService service = locationService.service();
			if ( service != null ) {
				try {
					GeneralLocationSourceMetadata meta = service.getLocationMetadata(locationId,
							sourceId);
					BasicGeneralLocation loc = new BasicGeneralLocation();
					loc.setLocationId(locationId);
					loc.setSourceId(sourceId);
					loc.setSourceMetadata(meta);
					location = loc;
				} catch ( RuntimeException e ) {
					log.error("Error getting location metadata for location {} source {}", locationId,
							sourceId, e);
				}
			}
		}
		return new BasicLocationLookupSettingSpecifier("locationKey", Location.PRICE_TYPE, location);
	}

	private LocationLookupSettingSpecifier getWeatherLocationSettingSpecifier() {
		if ( weatherLocation == null && locationService != null && weatherLocationId != null
				&& weatherSourceId != null ) {
			LocationService service = locationService.service();
			if ( service != null ) {
				try {
					GeneralLocationSourceMetadata meta = service.getLocationMetadata(weatherLocationId,
							weatherSourceId);
					BasicGeneralLocation loc = new BasicGeneralLocation();
					loc.setLocationId(weatherLocationId);
					loc.setSourceId(weatherSourceId);
					loc.setSourceMetadata(meta);
					weatherLocation = loc;
				} catch ( RuntimeException e ) {
					log.error("Error getting weather location metadata for location {} source {}",
							weatherLocationId, weatherSourceId, e);
				}
			}
		}
		return new BasicLocationLookupSettingSpecifier("weatherLocationKey", Location.WEATHER_TYPE,
				weatherLocation);
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
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
	 * Set the weather location ID and source ID as a single string value. The
	 * format of the key is {@code locationId:sourceId}.
	 * 
	 * @param key
	 *        the location and source ID key
	 */
	public void setWeatherLocationKey(String key) {
		Long newLocationId = null;
		String newSourceId = null;
		if ( key != null ) {
			int idx = key.indexOf(':');
			if ( idx > 0 && idx + 1 < key.length() ) {
				newLocationId = Long.valueOf(key.substring(0, idx));
				newSourceId = key.substring(idx + 1);
			}
		}
		setWeatherLocationId(newLocationId);
		setWeatherSourceId(newSourceId);
	}

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getInteger() {
		return integer;
	}

	public void setInteger(Integer integer) {
		this.integer = integer;
	}

	public Boolean getToggle() {
		return toggle;
	}

	public void setToggle(Boolean toggle) {
		this.toggle = toggle;
	}

	public Double getSlide() {
		return slide;
	}

	public void setSlide(Double slide) {
		this.slide = slide;
	}

	public String getRadio() {
		return radio;
	}

	public void setRadio(String radio) {
		this.radio = radio;
	}

	public OptionalServiceTracker<LocationService> getLocationService() {
		return locationService;
	}

	public void setLocationService(OptionalServiceTracker<LocationService> locationService) {
		this.locationService = locationService;
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

	public Long getWeatherLocationId() {
		return weatherLocationId;
	}

	public void setWeatherLocationId(Long weatherLocationId) {
		if ( this.weatherLocation != null && weatherLocationId != null
				&& !weatherLocationId.equals(this.weatherLocation.getLocationId()) ) {
			this.weatherLocation = null; // set to null so we re-fetch from server
		}
		this.weatherLocationId = weatherLocationId;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		if ( this.location != null && sourceId != null
				&& !sourceId.equals(this.location.getSourceId()) ) {
			this.location = null;
		}
		this.sourceId = sourceId;
	}

	public String getWeatherSourceId() {
		return weatherSourceId;
	}

	public void setWeatherSourceId(String weatherSourceId) {
		if ( this.weatherLocation != null && weatherSourceId != null
				&& !weatherSourceId.equals(this.weatherLocation.getSourceId()) ) {
			this.weatherLocation = null;
		}
		this.weatherSourceId = weatherSourceId;
	}

	public List<String> getListString() {
		return listString;
	}

	public void setListString(List<String> listString) {
		this.listString = listString;
	}

	/**
	 * Get the number of configured {@code listString} elements.
	 * 
	 * @return The number of {@code listString} elements.
	 */
	public int getListStringCount() {
		List<String> l = getListString();
		return (l == null ? 0 : l.size());
	}

	/**
	 * Adjust the number of configured {@code listString} elements. Any newly
	 * added element values will be empty strings.
	 * 
	 * @param count
	 *        The desired number of {@code listString} elements.
	 */
	public void setListStringCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		List<String> l = getListString();
		int lCount = (l == null ? 0 : l.size());
		while ( lCount > count ) {
			l.remove(l.size() - 1);
			lCount--;
		}
		while ( lCount < count ) {
			l.add("");
			lCount++;
		}
	}

	public List<ComplexListItem> getListComplex() {
		return listComplex;
	}

	public void setListComplex(List<ComplexListItem> listComplex) {
		this.listComplex = listComplex;
	}

	/**
	 * Get the number of configured {@code listComplex} elements.
	 * 
	 * @return The number of {@code listComplex} elements.
	 */
	public int getListComplexCount() {
		List<ComplexListItem> l = getListComplex();
		return (l == null ? 0 : l.size());
	}

	/**
	 * Adjust the number of configured {@code listComplex} elements. Any newly
	 * added element values will be empty strings.
	 * 
	 * @param count
	 *        The desired number of {@code listComplex} elements.
	 */
	public void setListComplexCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		List<ComplexListItem> l = getListComplex();
		int lCount = (l == null ? 0 : l.size());
		while ( lCount > count ) {
			l.remove(l.size() - 1);
			lCount--;
		}
		while ( lCount < count ) {
			l.add(new ComplexListItem());
			lCount++;
		}
	}

	public String getMenu() {
		return menu;
	}

	public void setMenu(String menu) {
		this.menu = menu;
	}

	public SetupResourceProvider getCustomSettingResourceProvider() {
		return customSettingResourceProvider;
	}

	public void setCustomSettingResourceProvider(SetupResourceProvider customSettingResourceProvider) {
		this.customSettingResourceProvider = customSettingResourceProvider;
	}

}
