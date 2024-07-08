/* ==================================================================
 * SMAyasdi4jPowerDatumDataSource.java - Mar 7, 2013 11:57:06 AM
 *
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.power.sma.yasdi4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.MessageSource;
import de.michaeldenk.yasdi4j.YasdiChannel;
import de.michaeldenk.yasdi4j.YasdiDevice;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.sma.SMAInverterDataSourceSupport;
import net.solarnetwork.node.io.yasdi4j.YasdiMaster;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * SMA {@link DatumDataSource} for {@link AcEnergyDatum}, using the
 * {@code yasdi4j} library.
 *
 * <p>
 * This class is not generally not thread-safe. Only one thread should execute
 * {@link #readCurrentDatum()} at a time.
 * </p>
 *
 * @author matt
 * @version 2.1
 */
public class SMAyasdi4jPowerDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

	/** The default value for the {@code sourceId} property. */
	public static final String DEFAULT_SOURCE_ID = "Main";

	/** The watts output channel name. */
	public static final String CHANNEL_NAME_WATTS = "Pac";

	/** The PV current channel name. */
	public static final String CHANNEL_NAME_PV_AMPS = "Ipv";

	/** The PV voltage channel name. */
	public static final String CHANNEL_NAME_PV_VOLTS = "Upv-Ist";

	/** The accumulative kWh channel name. */
	public static final String CHANNEL_NAME_KWH = "E-Total";

	/** The default device serial number value. */
	public static final Long DEFAULT_SERIAL_NUMBER = 1000L;

	/**
	 * Default value for the {@code channelNamesToMonitor} property.
	 *
	 * <p>
	 * Contains the PV voltage, PV current, and kWh channels.
	 * </p>
	 */
	public static final Set<String> DEFAULT_CHANNEL_NAMES_TO_MONITOR = Collections
			.unmodifiableSet(new LinkedHashSet<String>(
					Arrays.asList(CHANNEL_NAME_PV_AMPS, CHANNEL_NAME_PV_VOLTS, CHANNEL_NAME_KWH)));

	private final SMAInverterDataSourceSupport smaSupport;
	private String pvVoltsChannelName = CHANNEL_NAME_PV_VOLTS;
	private String pvAmpsChannelName = CHANNEL_NAME_PV_AMPS;
	private Set<String> pvWattsChannelNames = Collections.singleton(CHANNEL_NAME_WATTS);
	private String kWhChannelName = CHANNEL_NAME_KWH;
	private Set<String> otherChannelNames = null;
	private int channelMaxAgeSeconds = 30;
	private long deviceSerialNumber = DEFAULT_SERIAL_NUMBER;
	private long deviceLockTimeoutSeconds = 20;

	private OptionalFilterableService<ObjectFactory<YasdiMaster>> yasdi;
	private MessageSource messageSource;

	/**
	 * Constructor.
	 */
	public SMAyasdi4jPowerDatumDataSource() {
		super();
		smaSupport = new SMAInverterDataSourceSupport();
		setChannelNamesToMonitor(DEFAULT_CHANNEL_NAMES_TO_MONITOR);
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return SMAPowerDatum.class;
	}

	private YasdiDevice getYasdiDevice() {
		final ObjectFactory<YasdiMaster> service = yasdi.service();
		if ( service == null ) {
			log.debug("No YASDI service available.");
			return null;
		}
		final YasdiMaster master = service.getObject();
		YasdiDevice device = master.getDevice(this.deviceSerialNumber);
		if ( device == null ) {
			log.info("YASDI device {} not available", this.deviceSerialNumber);
		}
		return device;
	}

	private void releaseYasdiDevice(YasdiDevice device) {
		if ( device == null ) {
			return;
		}
		final ObjectFactory<YasdiMaster> service = yasdi.service();
		if ( service == null ) {
			log.debug("No YASDI service available.");
			return;
		}
		final YasdiMaster master = service.getObject();
		master.releaseDeviceLock(device);
	}

	@Override
	public AcDcEnergyDatum readCurrentDatum() {
		YasdiDevice device = null;
		final SMAPowerDatum datum = new SMAPowerDatum(resolvePlaceholders(getSourceId()));
		try {
			device = getYasdiDevice();
			if ( device == null ) {
				return null;
			}

			if ( this.pvWattsChannelNames != null && this.pvWattsChannelNames.size() > 0 ) {
				// we sum up all channels into a single value
				int totalWatts = 0;
				for ( String channelName : this.pvWattsChannelNames ) {
					Object v = captureChannelValue(device, channelName);
					if ( v instanceof Number ) {
						totalWatts += ((Number) v).intValue();
					}
				}
				datum.setWatts(totalWatts);
			} else {
				Object volts = captureChannelValue(device, this.pvVoltsChannelName);
				Object amps = captureChannelValue(device, this.pvAmpsChannelName);
				if ( volts instanceof Number && amps instanceof Number ) {
					datum.setWatts(((Number) volts).intValue() * ((Number) amps).intValue());
				}
			}
			Object wh = captureChannelValue(device, this.kWhChannelName);
			if ( wh instanceof Number ) {
				datum.setWattHourReading(((Number) wh).longValue());
			}

			if ( otherChannelNames != null ) {
				Map<String, Object> map = new LinkedHashMap<String, Object>(otherChannelNames.size());
				for ( String channelName : otherChannelNames ) {
					captureDataValue(device, channelName, channelName, map);
				}
				if ( map.size() > 0 ) {
					if ( datum.getChannelData() == null ) {
						datum.setChannelData(map);
					} else {
						datum.getChannelData().putAll(map);
					}
				}
			}
		} finally {
			releaseYasdiDevice(device);
		}

		if ( !isValidDatum(datum) ) {
			log.debug("No valid data available.");
			return null;
		}

		addEnergyDatumSourceMetadata(datum);

		return datum;
	}

	private void addEnergyDatumSourceMetadata(NodeDatum d) {
		// associate generation tags with this source
		GeneralDatumMetadata sourceMeta = new GeneralDatumMetadata();
		sourceMeta.addTag(AcDcEnergyDatum.TAG_GENERATION);
		addSourceMetadata(d.getSourceId(), sourceMeta);
	}

	private boolean isValidDatum(SMAPowerDatum d) {
		if ( (d.getWatts() == null || d.getWatts() < 1)
				&& (d.getWattHourReading() == null || d.getWattHourReading() < 1) ) {
			return false;
		}
		return true;
	}

	private Object captureChannelValue(YasdiDevice device, String channelName) {
		if ( channelName == null || channelName.length() < 1 ) {
			return null;
		}
		log.trace("Reading SMA channel {}", channelName);
		YasdiChannel channel = device.getChannel(channelName);
		if ( channel == null ) {
			log.warn("Channel {} not available from YASDI device", channelName);
			return null;
		}
		try {
			// get updated value, at most channelMaxAgeSeconds old
			channel.updateValue(channelMaxAgeSeconds);
		} catch ( IOException e ) {
			log.debug("Exception updating channel {} value: {}", channelName, e.toString());
			return null;
		}
		Object value = null;
		if ( channel.hasText() ) {
			value = channel.getValueText();
		} else {
			double v = channel.getValue();
			String unit = channel.getUnit();
			if ( unit != null ) {
				if ( unit.startsWith("m") ) {
					v /= 1000.0;
				} else if ( unit.startsWith("k") ) {
					v *= 1000;
				}
			}
			value = Double.valueOf(v);
		}
		log.trace("Read SMA channel {}: {}", channelName, value);
		return value;
	}

	/**
	 * Read a specific channel and set that value into a Map.
	 *
	 * @param device
	 *        the YasdiDevice to collect the data from
	 * @param channelName
	 *        the name of the channel to read
	 * @param key
	 *        the Map key to set with the data value
	 * @param accessor
	 *        the datum to update
	 */
	private void captureDataValue(YasdiDevice device, String channelName, String key,
			Map<String, Object> map) {
		Object value = captureChannelValue(device, channelName);
		if ( value != null ) {
			map.put(key, value);
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.power.sma.yasdi4j";
	}

	@Override
	public String getDisplayName() {
		return "SMA inverter (YASDI)";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		SMAyasdi4jPowerDatumDataSource defaults = new SMAyasdi4jPowerDatumDataSource();

		String yasdiDeviceName = "N/A";
		YasdiDevice device = null;
		try {
			device = getYasdiDevice();
			if ( device != null ) {
				yasdiDeviceName = device.getName();
			}
		} catch ( RuntimeException e ) {
			log.warn("Exception getting YASDI device name: {}", e.getMessage());
		} finally {
			releaseYasdiDevice(device);
		}
		results.addAll(basicIdentifiableSettings());
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.getSourceId()));

		results.add(new BasicTitleSettingSpecifier("address", yasdiDeviceName, true));
		results.add(new BasicTextFieldSettingSpecifier("deviceSerialNumber",
				String.valueOf(defaults.deviceSerialNumber)));

		results.add(new BasicTextFieldSettingSpecifier("channelMaxAgeSeconds",
				String.valueOf(defaults.getChannelMaxAgeSeconds())));
		results.add(new BasicTextFieldSettingSpecifier("deviceLockTimeoutSeconds",
				String.valueOf(defaults.getDeviceLockTimeoutSeconds())));

		results.add(new BasicTextFieldSettingSpecifier("pvWattsChannelNamesValue",
				defaults.getPvWattsChannelNamesValue()));
		results.add(new BasicTextFieldSettingSpecifier("pvVoltsChannelName",
				defaults.getPvVoltsChannelName()));
		results.add(new BasicTextFieldSettingSpecifier("pvAmpsChannelName",
				defaults.getPvAmpsChannelName()));
		results.add(new BasicTextFieldSettingSpecifier("kWhChannelName", defaults.getkWhChannelName()));
		results.add(new BasicTextFieldSettingSpecifier("otherChannelNamesValue",
				defaults.getOtherChannelNamesValue()));

		return results;
	}

	private void setupChannelNamesToMonitor() {
		Set<String> s = new LinkedHashSet<String>(3);
		if ( getPvWattsChannelNames() != null && getPvWattsChannelNames().size() > 0 ) {
			s.addAll(getPvWattsChannelNames());
		} else {
			if ( getPvVoltsChannelName() != null ) {
				s.add(getPvVoltsChannelName());
			}
			if ( getPvAmpsChannelName() != null ) {
				s.add(getPvAmpsChannelName());
			}
		}
		s.add(getkWhChannelName());
		if ( otherChannelNames != null ) {
			s.addAll(otherChannelNames);
		}

		if ( !s.equals(this.getChannelNamesToMonitor()) ) {
			setChannelNamesToMonitor(s);
		}
	}

	/**
	 * Get the PV voltage channel name.
	 *
	 * @return the name
	 */
	public String getPvVoltsChannelName() {
		return pvVoltsChannelName;
	}

	/**
	 * Set the PV voltage channel name.
	 *
	 * @param pvVoltsChannelName
	 *        the name to set
	 */
	public void setPvVoltsChannelName(String pvVoltsChannelName) {
		this.pvVoltsChannelName = pvVoltsChannelName;
		setupChannelNamesToMonitor();
	}

	/**
	 * Get the PV current channel name.
	 *
	 * @return the name
	 */
	public String getPvAmpsChannelName() {
		return pvAmpsChannelName;
	}

	/**
	 * Set the PV current channel name.
	 *
	 * @param pvAmpsChannelName
	 *        the name to set
	 */
	public void setPvAmpsChannelName(String pvAmpsChannelName) {
		this.pvAmpsChannelName = pvAmpsChannelName;
		setupChannelNamesToMonitor();
	}

	/**
	 * Get the kWh channel name.
	 *
	 * @return the name
	 */
	public String getkWhChannelName() {
		return kWhChannelName;
	}

	/**
	 * Set the kWh channel name.
	 *
	 * @param kWhChannelName
	 *        the name to set
	 */
	public void setkWhChannelName(String kWhChannelName) {
		this.kWhChannelName = kWhChannelName;
		setupChannelNamesToMonitor();
	}

	/**
	 * Get the configured {@link YasdiMaster} service.
	 *
	 * @return the service
	 */
	public OptionalFilterableService<ObjectFactory<YasdiMaster>> getYasdi() {
		return yasdi;
	}

	/**
	 * Set the dynamic service for the {@link YasdiMaster} instance to use.
	 *
	 * @param yasdi
	 *        the service to use
	 */
	public void setYasdi(OptionalFilterableService<ObjectFactory<YasdiMaster>> yasdi) {
		this.yasdi = yasdi;
	}

	/**
	 * Get {@code pvWattsChannelNames} as a comma-delimited string value.
	 *
	 * @return the channel names, as a delimited string
	 */
	public String getPvWattsChannelNamesValue() {
		return (pvWattsChannelNames == null ? null
				: StringUtils.commaDelimitedStringFromCollection(pvWattsChannelNames));
	}

	/**
	 * Set {@code pvWattsChannelNames} as a comma-delimited string value.
	 *
	 * @param value
	 *        the channel names, as a delimited string
	 */
	public void setPvWattsChannelNamesValue(String value) {
		setPvWattsChannelNames(StringUtils.commaDelimitedStringToSet(value));
	}

	/**
	 * Get the PV watts channel names.
	 *
	 * @return the names
	 */
	public Set<String> getPvWattsChannelNames() {
		return pvWattsChannelNames;
	}

	/**
	 * Set the PV watts channel names.
	 *
	 * @param pvWattsChannelNames
	 *        the names to set
	 */
	public void setPvWattsChannelNames(Set<String> pvWattsChannelNames) {
		this.pvWattsChannelNames = pvWattsChannelNames;
		setupChannelNamesToMonitor();
	}

	/**
	 * Get the device serial number.
	 *
	 * @return the deviceSerialNumber the number
	 */
	public final long getDeviceSerialNumber() {
		return deviceSerialNumber;
	}

	/**
	 * Set the device serial number.
	 *
	 * @param deviceSerialNumber
	 *        the serial number to set
	 */
	public void setDeviceSerialNumber(long deviceSerialNumber) {
		this.deviceSerialNumber = deviceSerialNumber;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the other channel names.
	 *
	 * @return the other names
	 */
	public Set<String> getOtherChannelNames() {
		return otherChannelNames;
	}

	/**
	 * Set the other channel names.
	 *
	 * @param otherChannelNames
	 *        the other names to set
	 */
	public void setOtherChannelNames(Set<String> otherChannelNames) {
		this.otherChannelNames = otherChannelNames;
	}

	/**
	 * Get {@code otherChannelNames} as a comma-delimited string value.
	 *
	 * @return the other channel names, as a delimited string
	 */
	public String getOtherChannelNamesValue() {
		return (otherChannelNames == null ? null
				: StringUtils.commaDelimitedStringFromCollection(otherChannelNames));
	}

	/**
	 * Set {@code otherChannelNames} as a comma-delimited string value.
	 *
	 * @param value
	 *        the channel names, as a delimited string
	 */
	public void setOtherChannelNamesValue(String value) {
		setOtherChannelNames(StringUtils.commaDelimitedStringToSet(value));
	}

	/**
	 * Get the device lock timeout.
	 *
	 * @return the timeout, in seconds
	 */
	public long getDeviceLockTimeoutSeconds() {
		return deviceLockTimeoutSeconds;
	}

	/**
	 * Set the device lock timeout.
	 *
	 * @param deviceLockTimeoutSeconds
	 *        the timeout, in seconds
	 */
	public void setDeviceLockTimeoutSeconds(long deviceLockTimeoutSeconds) {
		this.deviceLockTimeoutSeconds = deviceLockTimeoutSeconds;
	}

	/**
	 * Get the channel max age.
	 *
	 * @return the max age, in seconds
	 */
	public int getChannelMaxAgeSeconds() {
		return channelMaxAgeSeconds;
	}

	/**
	 * Set the channel max age.
	 *
	 * @param channelMaxAgeSeconds
	 *        the max age, in seconds
	 */
	public void setChannelMaxAgeSeconds(int channelMaxAgeSeconds) {
		this.channelMaxAgeSeconds = channelMaxAgeSeconds;
	}

	/**
	 * Get the channel names to monitor.
	 *
	 * @return the names to monitor
	 */
	public Set<String> getChannelNamesToMonitor() {
		return smaSupport.getChannelNamesToMonitor();
	}

	/**
	 * Set the channel names to monitor.
	 *
	 * @param channelNamesToMonitor
	 *        the names to monitor
	 */
	public void setChannelNamesToMonitor(Set<String> channelNamesToMonitor) {
		smaSupport.setChannelNamesToMonitor(channelNamesToMonitor);
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return smaSupport.getSourceId();
	}

	/**
	 * Set the source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		smaSupport.setSourceId(sourceId);
	}

	/**
	 * Set the setting DAO.
	 *
	 * @param settingDao
	 *        the DAO to set
	 */
	public void setSettingDao(SettingDao settingDao) {
		smaSupport.setSettingDao(settingDao);
	}

}
