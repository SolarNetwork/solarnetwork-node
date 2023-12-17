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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.datum.GeneralLocationSourceMetadata;
import net.solarnetwork.node.domain.datum.DatumLocation;
import net.solarnetwork.node.domain.datum.SimpleDatumLocation;
import net.solarnetwork.node.service.LocationService;
import net.solarnetwork.node.settings.LocationLookupSettingSpecifier;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingValueBean;
import net.solarnetwork.node.settings.SettingsCommand;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.node.settings.support.BasicFileSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicLocationLookupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicSetupResourceSettingSpecifier;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicRadioGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicSliderSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextAreaSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * A test bed experiment for the settings framework.
 * 
 * @author matt
 * @version 2.2
 */
public class SettingsPlaypen implements SettingSpecifierProvider, SettingResourceHandler {

	private static final String PLAYPEN_SETTING_FILE_TXT = "var/playpen-setting-file-%d.txt";
	private static final String PLAYPEN_SETTING_FILE_DAT = "var/playpen-setting-file.dat";
	private static final String RESOURCE_KEY_FILE = "file";
	private static final String RESOURCE_KEY_TEXT_FILES = "textFiles";
	private static final String RESOURCE_KEY_TEXT_AREA = "textArea";
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
	private String textAreaContent = null;
	private String fileContent = null;
	private String textAreaDirect = null;

	// group support
	private List<String> listString = new ArrayList<String>(4);
	private List<ComplexListItem> listComplex = new ArrayList<ComplexListItem>(4);
	private List<String> textFilesContent = new ArrayList<>(2);

	private OptionalService<LocationService> locationService;
	private Long locationId;
	private String sourceId;
	private DatumLocation location;

	private Long weatherLocationId;
	private String weatherSourceId;
	private DatumLocation weatherLocation;

	private Long co2LocationId;
	private String co2SourceId;
	private DatumLocation co2Location;

	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private SetupResourceProvider customSettingResourceProvider;

	@Override
	public String getSettingUid() {
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
		return settingSpecifiers(false);
	}

	@Override
	public List<SettingSpecifier> templateSettingSpecifiers() {
		return settingSpecifiers(true);
	}

	private List<SettingSpecifier> settingSpecifiers(final boolean template) {
		List<SettingSpecifier> results = new ArrayList<>();

		results.add(new BasicTextFieldSettingSpecifier("string", DEFAULT_STRING));
		results.add(new BasicTextFieldSettingSpecifier("password", null, true));
		results.add(new BasicTextFieldSettingSpecifier("integer", DEFAULT_INTEGER.toString()));
		results.add(new BasicToggleSettingSpecifier("toggle", false));
		results.add(new BasicSliderSettingSpecifier("slide", DEFAULT_SLIDE, 0.0, 10.0, 0.5));

		BasicRadioGroupSettingSpecifier radioSpec = new BasicRadioGroupSettingSpecifier("radio",
				DEFAULT_RADIO[0]);
		Map<String, String> radioValues = new LinkedHashMap<>(3);
		for ( String s : DEFAULT_RADIO ) {
			radioValues.put(s, s);
		}
		radioSpec.setValueTitles(radioValues);
		results.add(radioSpec);

		// drop-down menu
		BasicMultiValueSettingSpecifier menuSpec = new BasicMultiValueSettingSpecifier("menu",
				DEFAULT_MENU[0]);
		Map<String, String> menuValues = new LinkedHashMap<>(3);
		for ( String s : DEFAULT_MENU ) {
			menuValues.put(s, s);
		}
		menuSpec.setValueTitles(menuValues);
		results.add(menuSpec);

		results.add(getLocationSettingSpecifier());
		results.add(getWeatherLocationSettingSpecifier());
		results.add(getCo2LocationSettingSpecifier());

		// text area (direct)
		results.add(new BasicTextAreaSettingSpecifier("textAreaDirect", "", true));

		// text area
		results.add(new BasicTextAreaSettingSpecifier(RESOURCE_KEY_TEXT_AREA, ""));
		results.add(new BasicTitleSettingSpecifier(RESOURCE_KEY_TEXT_AREA + "Content", textAreaContent,
				true));

		// file
		results.add(new BasicFileSettingSpecifier(RESOURCE_KEY_FILE, null,
				new LinkedHashSet<>(asList(".txt", "text/*")), false));
		results.add(new BasicTitleSettingSpecifier(RESOURCE_KEY_FILE + "Content", fileContent, true));

		// text files
		results.add(new BasicFileSettingSpecifier(RESOURCE_KEY_TEXT_FILES, null,
				new LinkedHashSet<>(asList(".txt", "text/*")), true));
		if ( textFilesContent != null ) {
			for ( String content : textFilesContent ) {
				results.add(new BasicTitleSettingSpecifier(RESOURCE_KEY_TEXT_FILES + "Content", content,
						true));
			}
		}

		// custom UI
		results.add(new BasicSetupResourceSettingSpecifier(customSettingResourceProvider,
				Collections.singletonMap("foo", "bar")));

		// basic dynamic list of strings
		List<String> listStrings = (template ? singletonList("") : getListString());
		BasicGroupSettingSpecifier listStringGroup = SettingUtils.dynamicListSettingSpecifier(
				"listString", listStrings, (String value, int index, String key) -> Collections
						.singletonList(new BasicTextFieldSettingSpecifier(key, "")));
		results.add(listStringGroup);

		// dynamic list of objects
		Collection<ComplexListItem> listComplexes = (template ? singletonList(new ComplexListItem())
				: getListComplex());
		BasicGroupSettingSpecifier listComplexGroup = SettingUtils.dynamicListSettingSpecifier(
				"listComplex", listComplexes,
				(ComplexListItem value, int index, String key) -> Collections
						.singletonList(new BasicGroupSettingSpecifier(value.settings(key + "."))));
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
					SimpleDatumLocation loc = new SimpleDatumLocation();
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
		return new BasicLocationLookupSettingSpecifier("locationKey", DatumLocation.PRICE_TYPE,
				location);
	}

	private LocationLookupSettingSpecifier getWeatherLocationSettingSpecifier() {
		if ( weatherLocation == null && locationService != null && weatherLocationId != null
				&& weatherSourceId != null ) {
			LocationService service = locationService.service();
			if ( service != null ) {
				try {
					GeneralLocationSourceMetadata meta = service.getLocationMetadata(weatherLocationId,
							weatherSourceId);
					SimpleDatumLocation loc = new SimpleDatumLocation();
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
		return new BasicLocationLookupSettingSpecifier("weatherLocationKey", DatumLocation.WEATHER_TYPE,
				weatherLocation);
	}

	private LocationLookupSettingSpecifier getCo2LocationSettingSpecifier() {
		if ( co2Location == null && locationService != null && co2LocationId != null
				&& co2SourceId != null ) {
			LocationService service = locationService.service();
			if ( service != null ) {
				try {
					GeneralLocationSourceMetadata meta = service.getLocationMetadata(co2LocationId,
							co2SourceId);
					SimpleDatumLocation loc = new SimpleDatumLocation();
					loc.setLocationId(co2LocationId);
					loc.setSourceId(co2SourceId);
					loc.setSourceMetadata(meta);
					co2Location = loc;
				} catch ( RuntimeException e ) {
					log.error("Error getting co2 location metadata for location {} source {}",
							co2LocationId, co2SourceId, e);
				}
			}
		}
		return new BasicLocationLookupSettingSpecifier("co2LocationKey", "co2", co2Location);
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

	/**
	 * Set the CO2 location ID and source ID as a single string value. The
	 * format of the key is {@code locationId:sourceId}.
	 * 
	 * @param key
	 *        the location and source ID key
	 */
	public void setCo2LocationKey(String key) {
		Long newLocationId = null;
		String newSourceId = null;
		if ( key != null ) {
			int idx = key.indexOf(':');
			if ( idx > 0 && idx + 1 < key.length() ) {
				newLocationId = Long.valueOf(key.substring(0, idx));
				newSourceId = key.substring(idx + 1);
			}
		}
		setCo2LocationId(newLocationId);
		setCo2SourceId(newSourceId);
	}

	// SettingResourceHandler -----

	@Override
	public Iterable<Resource> currentSettingResources(String settingKey) {
		List<Resource> result = new ArrayList<>(2);
		if ( RESOURCE_KEY_FILE.equals(settingKey) ) {
			Resource r = new FileSystemResource(PLAYPEN_SETTING_FILE_DAT);
			if ( r.exists() ) {
				result.add(r);
			}
		} else if ( RESOURCE_KEY_TEXT_FILES.equals(settingKey) ) {
			int i = 1;
			while ( true ) {
				Resource r = new FileSystemResource(String.format(PLAYPEN_SETTING_FILE_TXT, i));
				if ( !r.exists() ) {
					break;
				}
				result.add(r);
				i++;
			}
		}
		return result;
	}

	@Override
	public SettingsUpdates applySettingResources(String settingKey, Iterable<Resource> resources)
			throws IOException {
		if ( resources == null ) {
			return null;
		}
		if ( RESOURCE_KEY_TEXT_AREA.equals(settingKey) ) {
			for ( Resource r : resources ) {
				String s = FileCopyUtils
						.copyToString(new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8));
				return new SettingsCommand(
						Arrays.asList(new SettingValueBean(RESOURCE_KEY_TEXT_AREA + "Content", s)));
			}
		} else if ( RESOURCE_KEY_FILE.equals(settingKey) ) {
			for ( Resource r : resources ) {
				String s = FileCopyUtils
						.copyToString(new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8));
				return new SettingsCommand(
						Arrays.asList(new SettingValueBean(RESOURCE_KEY_FILE + "Content", s)));
			}
		} else if ( RESOURCE_KEY_TEXT_FILES.equals(settingKey) ) {
			int i = 0;
			SettingsCommand updates = new SettingsCommand(null,
					asList(Pattern.compile("textFilesContent\\[.*")));
			for ( Resource r : resources ) {
				String s = FileCopyUtils
						.copyToString(new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8));
				updates.getValues().add(new SettingValueBean(
						String.format("%sContent[%d]", RESOURCE_KEY_TEXT_FILES, i), s));
				i++;
			}
			updates.getValues().add(0,
					new SettingValueBean(RESOURCE_KEY_TEXT_FILES + "ContentCount", String.valueOf(i)));
			return updates;
		} else {
			log.warn("Ignoring setting resource key [{}]", settingKey);
		}
		return null;
	}

	// Accessors -----

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

	public OptionalService<LocationService> getLocationService() {
		return locationService;
	}

	public void setLocationService(OptionalService<LocationService> locationService) {
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

	public Long getCo2LocationId() {
		return co2LocationId;
	}

	public void setCo2LocationId(Long co2LocationId) {
		if ( this.co2Location != null && co2LocationId != null
				&& !co2LocationId.equals(this.co2Location.getLocationId()) ) {
			this.co2Location = null; // set to null so we re-fetch from server
		}
		this.co2LocationId = co2LocationId;
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

	public String getCo2SourceId() {
		return co2SourceId;
	}

	public void setCo2SourceId(String co2SourceId) {
		if ( this.co2SourceId != null && co2SourceId != null
				&& !co2SourceId.equals(this.co2Location.getSourceId()) ) {
			this.co2Location = null;
		}
		this.co2SourceId = co2SourceId;
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

	public String getTextAreaContent() {
		return textAreaContent;
	}

	public void setTextAreaContent(String textArea) {
		this.textAreaContent = textArea;
	}

	public String getFileContent() {
		return fileContent;
	}

	public void setFileContent(String textFile) {
		this.fileContent = textFile;
	}

	public List<String> getTextFilesContent() {
		return textFilesContent;
	}

	public void setTextFilesContent(List<String> textFiles) {
		this.textFilesContent = textFiles;
	}

	/**
	 * Get the number of configured {@code listString} elements.
	 * 
	 * @return The number of {@code listString} elements.
	 */
	public int getTextFilesContentCount() {
		List<String> l = getTextFilesContent();
		return (l == null ? 0 : l.size());
	}

	/**
	 * Adjust the number of configured {@code textFilesContent} elements. Any
	 * newly added element values will be empty strings.
	 * 
	 * @param count
	 *        The desired number of {@code listString} elements.
	 */
	public void setTextFilesContentCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		List<String> l = getTextFilesContent();
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

	/**
	 * Get the text area direct value.
	 * 
	 * @return the value
	 * @since 2.1
	 */
	public String getTextAreaDirect() {
		return textAreaDirect;
	}

	/**
	 * Set the text area direct value.
	 * 
	 * @param textAreaDirect
	 *        the value to set
	 * @since 2.1
	 */
	public void setTextAreaDirect(String textAreaDirect) {
		this.textAreaDirect = textAreaDirect;
	}

}
