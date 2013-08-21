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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.hw.sma.SMAInverterDataSourceSupport;
import net.solarnetwork.node.io.yasdi4j.YasdiMaster;
import net.solarnetwork.node.power.PowerDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.DynamicServiceTracker;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import de.michaeldenk.yasdi4j.YasdiChannel;

/**
 * SMA {@link DatumDataSource} for {@link PowerDatum}, using the {@code yasdi4j}
 * library.
 * 
 * <p>
 * This class is not generally not thread-safe. Only one thread should execute
 * {@link #readCurrentDatum()} at a time.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>yasdi</dt>
 * <dd>The dynamic service for the {@link YasdiMaster} instance to use.</dd>
 * 
 * <dt>channelNamesToOffsetDaily</dt>
 * <dd>If configured, a set of channels to treat as ever-accumulating numbers
 * that should be treated as daily-resetting values. This can be used, for
 * example, to calculate a "kWh generated today" value from a "E-Total" channel
 * that is not reset by the inverter itself. When reading values on the start of
 * a new day, the value of that channel is persisted so subsequent readings on
 * the same day can be calculated as an offset from that initial value. Requires
 * the {@code settingDao} property to also be configured.</dd>
 * 
 * <dt>settingDao</dt>
 * <dd>The {@link SettingDao} to use, required by the
 * {@code channelNamesToResetDaily} property.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class SMAyasdi4jPowerDatumDataSource extends SMAInverterDataSourceSupport implements
		DatumDataSource<PowerDatum>, SettingSpecifierProvider {

	/** The default value for the {@code sourceId} property. */
	public static final String DEFAULT_SOURCE_ID = "Main";

	/** The PV current channel name. */
	public static final String CHANNEL_NAME_PV_AMPS = "Ipv";

	/** The PV voltage channel name. */
	public static final String CHANNEL_NAME_PV_VOLTS = "Upv-Ist";

	/** The accumulative kWh channel name. */
	public static final String CHANNEL_NAME_KWH = "E-Total";

	/** The default UID filter value. */
	public static final Long DEFAULT_UID_FILTER = 1000L;

	/**
	 * Default value for the {@code channelNamesToMonitor} property.
	 * 
	 * <p>
	 * Contains the PV voltage, PV current, and kWh channels.
	 * </p>
	 */
	public static final Set<String> DEFAULT_CHANNEL_NAMES_TO_MONITOR = Collections
			.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(CHANNEL_NAME_PV_AMPS,
					CHANNEL_NAME_PV_VOLTS, CHANNEL_NAME_KWH)));

	/**
	 * Default value for the {@code channelNamesToOffsetDaily} property.
	 * 
	 * <p>
	 * Contains the PV voltage, PV current, and kWh channels.
	 * </p>
	 */
	public static final Set<String> DEFAULT_CHANNEL_NAMES_TO_OFFSET_DAILY = Collections
			.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(CHANNEL_NAME_KWH)));

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	private String pvVoltsChannelName = CHANNEL_NAME_PV_VOLTS;
	private String pvAmpsChannelName = CHANNEL_NAME_PV_AMPS;
	private String kWhChannelName = CHANNEL_NAME_KWH;
	private int channelMaxAgeSeconds = 30;

	private DynamicServiceTracker<ObjectFactory<YasdiMaster>> yasdi;

	public SMAyasdi4jPowerDatumDataSource() {
		super();
		setChannelNamesToMonitor(DEFAULT_CHANNEL_NAMES_TO_MONITOR);
		setChannelNamesToOffsetDaily(DEFAULT_CHANNEL_NAMES_TO_OFFSET_DAILY);
	}

	@Override
	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	@Override
	public PowerDatum readCurrentDatum() {
		final ObjectFactory<YasdiMaster> service = yasdi.service();
		if ( service == null ) {
			log.debug("No YASDI service available.");
			return null;
		}
		final YasdiMaster master = service.getObject();

		PowerDatum datum = new PowerDatum();
		datum.setSourceId(getSourceId());
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(datum);

		final boolean newDay = isNewDay();

		// Issue GetData command for each channel we're interested in
		captureNumericDataValue(master, this.pvVoltsChannelName, "pvVolts", bean, newDay);
		captureNumericDataValue(master, this.pvAmpsChannelName, "pvAmps", bean, newDay);
		captureNumericDataValue(master, this.kWhChannelName, "KWattHoursToday", bean, newDay);

		if ( !isValidDatum(datum) ) {
			log.debug("No valid data available.");
			return null;
		}

		if ( newDay ) {
			storeLastKnownDay();
		}
		return datum;
	}

	private boolean isValidDatum(PowerDatum d) {
		if ( (d.getPvVolts() == null || d.getPvVolts() < 0.001)
				&& (d.getPvAmps() == null || d.getPvAmps() < 0.001)
				&& (d.getKWattHoursToday() == null || d.getKWattHoursToday() < 0.001) ) {
			return false;
		}
		return true;
	}

	/**
	 * Read a specific channel that returns numeric values and set that value
	 * onto a PowerDatum instance.
	 * 
	 * @param service
	 *        the YasdiMaster service to collect the data from
	 * @param channelName
	 *        the name of the channel to read
	 * @param beanProperty
	 *        the PowerDatum bean property to set with the numeric data value
	 * @param accessor
	 *        the datum to update
	 * @param newDay
	 *        flag if today is considered a "new day" for purposes of calling
	 *        {@link #handleDailyChannelOffset(String, Number, boolean)}
	 */
	private void captureNumericDataValue(YasdiMaster service, String channelName, String beanProperty,
			PropertyAccessor accessor, final boolean newDay) {
		log.trace("Capturing channel {} as property {}", channelName, beanProperty);
		YasdiChannel channel = service.getDevice().getChannel(channelName);
		if ( channel == null ) {
			log.warn("Channel {} not available from YASDI device", channelName);
			return;
		}
		try {
			// get updated value, at most channelMaxAgeSeconds old
			channel.updateValue(channelMaxAgeSeconds);
		} catch ( IOException e ) {
			log.debug("Exception updating channel {} value: {}", channelName, e.toString());
		}
		Number n = Double.valueOf(channel.getValue());
		log.trace("Captured channel {} for property {}: {}", channelName, beanProperty, n);
		n = handleDailyChannelOffset(channelName, n, newDay);
		log.trace("Captured channel {} for property {}: {}", channelName, beanProperty, n);
		if ( n != null ) {
			Class<?> propType = accessor.getPropertyType(beanProperty);
			Number value = n;
			String unit = channel.getUnit();
			if ( unit != null && "mA".equals(unit.toString()) ) {
				value = divide(propType, n, Integer.valueOf(1000));
			}
			accessor.setPropertyValue(beanProperty, value);
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.power.sma.yasdi4j";
	}

	@Override
	public String getDisplayName() {
		return "SMA inverter (YASDI)";
	}

	@Override
	public MessageSource getMessageSource() {
		synchronized ( MONITOR ) {
			if ( MESSAGE_SOURCE == null ) {
				ResourceBundleMessageSource source = new ResourceBundleMessageSource();
				source.setBundleClassLoader(SMAyasdi4jPowerDatumDataSource.class.getClassLoader());
				source.setBasename(SMAyasdi4jPowerDatumDataSource.class.getName());
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		SMAyasdi4jPowerDatumDataSource defaults = new SMAyasdi4jPowerDatumDataSource();

		String yasdiDeviceName = "N/A";
		try {
			final ObjectFactory<YasdiMaster> service = yasdi.service();
			if ( service != null ) {
				yasdiDeviceName = service.getObject().getDevice().getName();
			}
		} catch ( RuntimeException e ) {
			log.warn("Exception getting YASDI device name: {}", e.getMessage());
		}
		results.add(new BasicTitleSettingSpecifier("address", yasdiDeviceName, true));

		results.add(new BasicTextFieldSettingSpecifier("yasdi.propertyFilters['UID']", String
				.valueOf(DEFAULT_UID_FILTER)));

		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.getSourceId()));

		results.add(new BasicTextFieldSettingSpecifier("pvVoltsChannelName", defaults
				.getPvVoltsChannelName()));
		results.add(new BasicTextFieldSettingSpecifier("pvAmpsChannelName", defaults
				.getPvAmpsChannelName()));
		results.add(new BasicTextFieldSettingSpecifier("kWhChannelName", defaults.getkWhChannelName()));

		results.add(new BasicTextFieldSettingSpecifier("channelNamesToOffsetDailyValue", defaults
				.getChannelNamesToOffsetDailyValue()));

		return results;
	}

	private void setupChannelNamesToMonitor() {
		Set<String> s = new LinkedHashSet<String>(3);
		s.add(getPvVoltsChannelName());
		s.add(getPvAmpsChannelName());
		s.add(getkWhChannelName());
		if ( !s.equals(this.getChannelNamesToMonitor()) ) {
			setChannelNamesToMonitor(s);
		}
	}

	public String getPvVoltsChannelName() {
		return pvVoltsChannelName;
	}

	public void setPvVoltsChannelName(String pvVoltsChannelName) {
		this.pvVoltsChannelName = pvVoltsChannelName;
		setupChannelNamesToMonitor();
	}

	public String getPvAmpsChannelName() {
		return pvAmpsChannelName;
	}

	public void setPvAmpsChannelName(String pvAmpsChannelName) {
		this.pvAmpsChannelName = pvAmpsChannelName;
		setupChannelNamesToMonitor();
	}

	public String getkWhChannelName() {
		return kWhChannelName;
	}

	public void setkWhChannelName(String kWhChannelName) {
		this.kWhChannelName = kWhChannelName;
		setupChannelNamesToMonitor();
	}

	public DynamicServiceTracker<ObjectFactory<YasdiMaster>> getYasdi() {
		return yasdi;
	}

	public void setYasdi(DynamicServiceTracker<ObjectFactory<YasdiMaster>> yasdi) {
		this.yasdi = yasdi;
	}

	public void setChannelMaxAgeSeconds(int channelMaxAgeSeconds) {
		this.channelMaxAgeSeconds = channelMaxAgeSeconds;
	}

}
