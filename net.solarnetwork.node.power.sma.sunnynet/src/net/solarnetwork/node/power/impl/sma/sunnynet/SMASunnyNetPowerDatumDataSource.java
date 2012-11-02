/* ===================================================================
 * SMASunnyNetPowerDatumDataSource.java
 * 
 * Created Aug 19, 2009 1:21:11 PM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.power.impl.sma.sunnynet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.solarnetwork.node.ConversationalDataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.power.PowerDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.hw.sma.sunnynet.SmaChannel;
import net.solarnetwork.node.hw.sma.sunnynet.SmaChannelParam;
import net.solarnetwork.node.hw.sma.sunnynet.SmaCommand;
import net.solarnetwork.node.hw.sma.sunnynet.SmaControl;
import net.solarnetwork.node.hw.sma.sunnynet.SmaPacket;
import net.solarnetwork.node.hw.sma.sunnynet.SmaUserDataField;
import net.solarnetwork.node.hw.sma.sunnynet.SmaUtils;
import net.solarnetwork.util.DynamicServiceTracker;
import net.solarnetwork.util.StringUtils;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Implementation of {@link GenerationDataSource} for SMA controllers.
 * 
 * <p>In limited testing, the following 
 * {@code SerialPortConversationalDataCollectorFactory} property values work
 * well for communicating with SMA over a RS-232 serial connection:</p>
 * 
 * <dl>
 *   <dt>serialPort</dt>
 *   <dd>/dev/ttyS0 (this will vary depending on system)</dd>
 *   
 *   <dt>baud</dt>
 *   <dd>1200</dd>
 *   
 *   <dt>rts</dt>
 *   <dd>false</dd>
 *   
 *   <dt>dtr</dt>
 *   <dd>false</dt>
 *   
 *   <dt>receiveThreshold</dt>
 *   <dd>-1</dd>
 *   
 *   <dt>receiveTimeout</dt>
 *   <dd>2000</dd>
 *   
 *   <dt>maxWait</dt>
 *   <dd>60000</dd>
 * </dl>
 * 
 * <p>This class is not generally not thread-safe. Only one thread should execute
 * {@link #readCurrentDatum()} at a time.</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>dataCollectorFactory</dt>
 *   <dd>The factory for creating {@link ConversationalDataCollector} instances with. 
 *   {@link GenericObjectFactory#getObject()} will be called on each invocation
 *   of {@link #readCurrentPowerDatum()}.</dd>
 *   
 *   <dt>synOnlineWaitMs</dt>
 *   <dd>Number of milliseconds to wait after issuing the SynOnline command.
 *   A wait seems to be necessary otherwise the first data request fails.
 *   Defaults to {@link #DEFAULT_SYN_ONLINE_WAIT_MS}.</dd>
 *   
 *   <dt>channelNamesToResetDaily</dt>
 *   <dd>If configured, a set of channels to reset each day to a zero value.
 *   This is useful for resetting accumulative counter values, such as E-Total,
 *   on a daily basis for tracking the total kWh generated each day. Requires
 *   the {@code settingDao} property to also be configured.</dd>
 *   
 *   <dt>channelNamesToOffsetDaily</dt>
 *   <dd>If configured, a set of channels to treat as ever-accumulating numbers
 *   that should be treated as daily-resetting values. This can be used, for
 *   example, to calculate a "kWh generated today" value from a "E-Total"
 *   channel that is not reset by the inverter itself. When reading values
 *   on the start of a new day, the value of that channel is persisted so
 *   subsequent readings on the same day can be calculated as an offset from
 *   that initial value. Requires the {@code settingDao} property to also be
 *   configured.</dd>
 *   
 *   <dt>settingDao</dt>
 *   <dd>The {@link SettingDao} to use, required by the 
 *   {@code channelNamesToResetDaily} property.</dd>
 * </dl>
 * 
 * <p>Based on code from Gray Watson's sma.pl script, copyright included here:</p>
 * 
 * <pre># Copyright 2004 by Gray Watson
 * #
 * # Permission to use, copy, modify, and distribute this software for
 * # any purpose and without fee is hereby granted, provided that the
 * # above copyright notice and this permission notice appear in all
 * # copies, and that the name of Gray Watson not be used in advertising
 * # or publicity pertaining to distribution of the document or software
 * # without specific, written prior permission.
 * #
 * # Gray Watson makes no representations about the suitability of the
 * # software described herein for any purpose.  It is provided "as is"
 * # without express or implied warranty.
 * #
 * # The author may be contacted via http://256.com/gray/</pre>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class SMASunnyNetPowerDatumDataSource 
implements DatumDataSource<PowerDatum>, ConversationalDataCollector.Moderator<PowerDatum>, SettingSpecifierProvider {

	/** The default value for the {@code sourceId} property. */
	public static final String DEFAULT_SOURCE_ID = "Main";

	/** The PV current channel name. */
	public static final String CHANNEL_NAME_PV_AMPS = "Ipv";

	/** The PV voltage channel name. */
	public static final String CHANNEL_NAME_PV_VOLTS = "Vpv";
	
	/** The accumulative kWh channel name. */
	public static final String CHANNEL_NAME_KWH = "E-Total";
	
	
	/** The {@link SettingDao} key for the last-known-day value. */
	public static final String SETTING_LAST_KNOWN_DAY 
		= "SMASunnyNetPowerDatumDataSource.known.day";
	
	/** The {@link SettingDao} key for a channel last-known-value value. */
	public static final String SETTING_LAST_KNOWN_VALUE_PREFIX 
		= "SMASunnyNetPowerDatumDataSource.value:";
	
	/** The {@link SettingDao} key for a channel day-start-value value. */
	public static final String SETTING_DAY_START_VALUE_PREFIX 
		= "SMASunnyNetPowerDatumDataSource.start:";
	
	/** 
	 * Default value for the {@code channelNamesToMonitor} property.
	 * 
	 * <p>Contains the PV voltage, PV current, and kWh channels.</p>
	 */
	public static final Set<String> DEFAULT_CHANNEL_NAMES_TO_MONITOR
		= Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
			CHANNEL_NAME_PV_AMPS, CHANNEL_NAME_PV_VOLTS, CHANNEL_NAME_KWH)));
	
	/** The default value for the {@code synOnlineWaitMs} property. */
	public static final long DEFAULT_SYN_ONLINE_WAIT_MS = 5000;
	
	private static final String DEFAULT_SERIAL_PORT = "/dev/ttyS0";

	private static final SerialPortBeanParameters DEFAULT_SERIAL_PARAMS = new SerialPortBeanParameters();

	static {
		DEFAULT_SERIAL_PARAMS.setBaud(1200);
		DEFAULT_SERIAL_PARAMS.setDataBits(8);
		DEFAULT_SERIAL_PARAMS.setStopBits(1);
		DEFAULT_SERIAL_PARAMS.setParity(0);
		DEFAULT_SERIAL_PARAMS.setDtrFlag(0);
		DEFAULT_SERIAL_PARAMS.setRtsFlag(0);
		DEFAULT_SERIAL_PARAMS.setReceiveThreshold(-1);
		DEFAULT_SERIAL_PARAMS.setReceiveTimeout(2000);
		DEFAULT_SERIAL_PARAMS.setMaxWait(65000);
	}

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	private Set<String> channelNamesToMonitor = DEFAULT_CHANNEL_NAMES_TO_MONITOR;
	private Set<String> channelNamesToResetDaily = null;
	private Set<String> channelNamesToOffsetDaily = null;
	private String pvVoltsChannelName = CHANNEL_NAME_PV_VOLTS;
	private String pvAmpsChannelName = CHANNEL_NAME_PV_AMPS;
	private String kWhChannelName = CHANNEL_NAME_KWH;
	private long synOnlineWaitMs = DEFAULT_SYN_ONLINE_WAIT_MS;
	private SettingDao settingDao = null;
	private String sourceId = DEFAULT_SOURCE_ID;

	private DynamicServiceTracker<DataCollectorFactory<SerialPortBeanParameters>> dataCollectorFactory;
	private SerialPortBeanParameters serialParams = getDefaultSerialParameters();

	private int smaAddress = -1;
	private Map<String, SmaChannel> channelMap = null;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Get the default serial parameters used for SMA inverters.
	 * @return
	 */
	public static final SerialPortBeanParameters getDefaultSerialParameters() {
		return (SerialPortBeanParameters)DEFAULT_SERIAL_PARAMS.clone();
	}
	
	@Override
	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	private ConversationalDataCollector getDataCollectorInstance() {
		final DataCollectorFactory<SerialPortBeanParameters> df = getDataCollectorFactory().service();
		if ( df == null ) {
			return null;
		}
		return df.getConversationalDataCollectorInstance(getSerialParams());
	}
	
	private void setupChannelNamesToMonitor() {
		Set<String> s = new LinkedHashSet<String>(3);
		s.add(getPvVoltsChannelName());
		s.add(getPvAmpsChannelName());
		s.add(getkWhChannelName());
		if ( !s.equals(this.channelNamesToMonitor) ) {
			this.channelNamesToMonitor = s;
			this.channelMap = null;
		}
	}

	@Override
	public PowerDatum readCurrentDatum() {
		ConversationalDataCollector dataCollector = null;
		try {
			dataCollector = getDataCollectorInstance();
			if ( dataCollector != null ) {
				return dataCollector.collectData(this);
			}
		} finally {
			if ( dataCollector != null ) {
				dataCollector.stopCollecting();
			}
		}
		return null;
	}
	
	public PowerDatum conductConversation(ConversationalDataCollector dataCollector) {
		SmaPacket req = null;
		SmaPacket resp = null;
		if ( this.smaAddress < 0 || this.channelMap == null ) {
			// Issue NetStart command to find SMA address
			req = writeCommand(dataCollector, SmaCommand.NetStart, 0, 0, 
					SmaControl.RequestGroup, SmaPacket.EMPTY_DATA);
			resp = decodeResponse(dataCollector, req);
			if ( log.isTraceEnabled() ) {
				log.trace("Got decoded NetStart response: " +resp);
			}
			if ( !resp.isValid() ) {
				log.warn("Invalid response to NetStart command, cannot continue: " +resp);
				return null;
			}
			// TODO handle multiple device responses, for now we only accept one
			
			// Issue GetChannelInfo command, to get full list of available channels
			// This returns a lot of data... so we just do it once and cache the 
			// results for subsequent use
			this.smaAddress = resp.getSrcAddress();
			req = writeCommand(dataCollector, SmaCommand.GetChannelInfo, 
					this.smaAddress, 0, SmaControl.RequestSingle, SmaPacket.EMPTY_DATA);
			resp = decodeResponse(dataCollector, req);
			if ( !resp.isValid() ) {
				log.warn("Invalid response to GetChannelInfo command, cannot continue: " +resp);
				return null;
			}
			Map<String, SmaChannel> channels = getSmaChannelMap(resp);
			if ( log.isTraceEnabled() ) {
				log.trace("Got decoded GetChannelInfo response: " +resp +", with " 
						+channels.size() +" channels decoded");
			}
			this.channelMap = channels;
		}
		
		
		// Issue SynOnline command
		int pollTime = (int)Math.ceil(System.currentTimeMillis() / 1000.0);
		req = writeProclamation(dataCollector, SmaCommand.SynOnline, 0, 0, 
				SmaControl.RequestGroup, SmaUtils.littleEndianBytes(pollTime));
		
		// pause for a few secs, as first channel may not respond otherwise
		try {
			Thread.sleep(this.synOnlineWaitMs);
		} catch ( InterruptedException e ) {
			// ignore this one
		}
		
		if ( channelNamesToResetDaily != null && settingDao != null ) {
			handleDailyChannelReset(dataCollector);
		}
		
		PowerDatum datum = new PowerDatum();
		
		final boolean newDay = isNewDay();
		
		// Issue GetData command for each channel we're interested in
		getNumericDataValue(dataCollector, this.pvVoltsChannelName, "pvVolts", datum, newDay);
		getNumericDataValue(dataCollector, this.pvAmpsChannelName, "pvAmps", datum, newDay);
		getNumericDataValue(dataCollector, this.kWhChannelName, "KWattHoursToday", datum, newDay);

		if ( newDay ) {
			storeLastKnownDay();
		}
		return datum;
	}
	
	/**
	 * Issue a GetData command for a specific channel that returns a numeric value
	 * and set that value onto a PowerDatum instance.
	 * 
	 * @param dataCollector the ConversationalDataCollector to collect the data from
	 * @param channelName the name of the channel to read
	 * @param beanProperty the PowerDatum bean property to set with the numeric data value
	 * @param datum the datum to update
	 * @param newDay flag if today is considered a "new day" for purposes of calling
	 * {@link #handleDailyChannelOffset(String, Number, boolean)}
	 */
	private void getNumericDataValue(ConversationalDataCollector dataCollector, 
			String channelName, String beanProperty, PowerDatum datum, final boolean newDay) {
		if ( this.channelMap.containsKey(channelName) ) {
			SmaChannel channel = this.channelMap.get(channelName);
			SmaPacket resp = issueGetData(dataCollector, channel, this.smaAddress);
			if ( resp.isValid() ) {
				Number n = (Number)resp.getUserDataField(SmaUserDataField.Value);
				n = handleDailyChannelOffset(channelName, n, newDay);
				if ( n != null ) {
					PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(datum);
					Class<?> propType = accessor.getPropertyType(beanProperty);
					Number value = n;
					Object unit = channel.getParameterValue(SmaChannelParam.Unit);
					if ( unit != null && "mA".equals(unit.toString()) ) {
						value = divide(propType, n, Integer.valueOf(1000));
					}
					Object gain = channel.getParameterValue(SmaChannelParam.Gain);
					if ( gain instanceof Number ) {
						value = mult((Number)gain, value);
					}
					accessor.setPropertyValue(beanProperty, value);
				}
			} else {
				log.warn("Invalid response to [" +beanProperty 
						+"] GetData command for channel [" +channelName +"]");
			}
		}
	}
	
	/**
	 * Divide two {@link Number} instances using a specific implementation of Number.
	 * 
	 * <p>Really the {@code numberType} argument should be considered a
	 * {@code Class<? extends Number>} but to simplify calling this method any Class
	 * is allowed.</p>
	 * 
	 * @param numberType the type of Number to treat the dividend and divisor as
	 * @param dividend the dividend value
	 * @param divisor the divisor value
	 * @return a Number instance of type {@code numberType}
	 */
	private Number divide(Class<?> numberType, Number dividend, Number divisor) {
		if ( Integer.class.isAssignableFrom(numberType) ) {
			return dividend.intValue() / divisor.intValue();
		} else if ( Float.class.isAssignableFrom(numberType) ) {
			return dividend.floatValue() / divisor.floatValue();
		} else if ( Long.class.isAssignableFrom(numberType) ) {
			return dividend.longValue() / divisor.longValue();
		}
		return dividend.doubleValue() / divisor.doubleValue();
	}

	/**
	 * Handle channels that accumulate overall as if they reset daily.
	 * 
	 * @param channelName the channel name to calculate the offset for
	 * @param currValue the current value reported for this channel
	 * @param newDay <em>true</em> if this is a different day from the last known day
	 */
	private Number handleDailyChannelOffset(String channelName, Number currValue, 
			final boolean newDay) {
		if ( currValue == null || this.channelNamesToOffsetDaily == null
				|| !this.channelNamesToOffsetDaily.contains(channelName) ) {
			return currValue;
		}
		
		String dayStartKey = SETTING_DAY_START_VALUE_PREFIX +channelName;
		String lastKnownKey = SETTING_LAST_KNOWN_VALUE_PREFIX +channelName;
		Number result;
		
		String lastKnownValueStr = settingDao.getSetting(lastKnownKey);
		Number lastKnownValue = lastKnownValueStr == null
			? currValue : parseNumber(currValue.getClass(), lastKnownValueStr);
		
		// we've seen values reported less than last known value after
		// a power outage (i.e. after inverter turns off, then back on)
		// on single day, so we verify that current value is not less 
		// than last known value
		if ( currValue.doubleValue() < lastKnownValue.doubleValue() ) {
			// just return last known value, not curr value
			log.warn("Channel [" +channelName +"] reported value ["
					+currValue +"] -- less than last known value ["
					+lastKnownValue +"]. "
					+"Using last known value in place of reported value.");
			currValue = lastKnownValue;
		}
		
		if ( newDay ) {
			result = diff(currValue, lastKnownValue);

			if ( log.isDebugEnabled() ) {
				log.debug("Last known day has changed, resetting offset value for channel ["
						+channelName +"] to [" +lastKnownValue +']');
			}
			settingDao.storeSetting(dayStartKey, lastKnownValue.toString());
		} else {
			String dayStartValueStr = settingDao.getSetting(dayStartKey);
			Number dayStartValue = dayStartValueStr == null
				? currValue : parseNumber(currValue.getClass(), dayStartValueStr);
			result = diff(currValue, dayStartValue);
		}
		
		settingDao.storeSetting(lastKnownKey, currValue.toString());
		
		// we've seen negative values calculated sometimes at the start of the day,
		// so we prevent that from happening here
		if ( result.doubleValue() < 0 ) {
			result = Double.valueOf(0.0);
		}
		
		return result;
	}
	
	private void storeLastKnownDay() {
		if ( settingDao == null ) {
			return;
		}
		Calendar now = Calendar.getInstance();
		String dayOfYear = String.valueOf(now.get(Calendar.YEAR))
			+"." +String.valueOf(now.get(Calendar.DAY_OF_YEAR));
		if ( log.isDebugEnabled() ) {
			log.debug("Saving last known day as [" +dayOfYear +']');
		}
		settingDao.storeSetting(SETTING_LAST_KNOWN_DAY, dayOfYear);
	}
	
	private Calendar getLastKnownDay() {
		Calendar day = null;
		String lastKnownDayOfYear = settingDao.getSetting(SETTING_LAST_KNOWN_DAY);
		if ( lastKnownDayOfYear != null ) {
			int dot = lastKnownDayOfYear.indexOf('.');
			day = Calendar.getInstance();
			day.set(Calendar.YEAR, 
					Integer.parseInt(lastKnownDayOfYear.substring(0, dot)));
			day.set(Calendar.DAY_OF_YEAR, 
					Integer.parseInt(lastKnownDayOfYear.substring(dot+1)));
		}
		return day;
	}

	/**
	 * Test if today is a different day from the last known day.
	 * 
	 * <p>If the {@code settingDao} to be configured, this method will use that
	 * to load a "last known day" value. If that value is not found, or is different
	 * from the current execution day, <em>true</em> will be returned. Otherwise,
	 * <em>false</em> is returned.</p>
	 * 
	 * @return boolean
	 */
	private boolean isNewDay() {
		if ( this.settingDao == null ) {
			return false;
		}
		Calendar now = Calendar.getInstance();
		Calendar then = getLastKnownDay();
		if ( then == null || (now.get(Calendar.YEAR) != then.get(Calendar.YEAR))
				|| (now.get(Calendar.DAY_OF_YEAR) != then.get(Calendar.DAY_OF_YEAR)) ) {
			return true;
		}
		return false;
	}

	/**
	 * Parse a String into a Number of a specific type.
	 * 
	 * @param numberType the type of Number to return
	 * @param numberString the String to parse
	 * @return the new Number instance
	 */
	private Number parseNumber(Class<? extends Number> numberType, String numberString) {
		if ( Integer.class.isAssignableFrom(numberType) ) {
			return Integer.valueOf(numberString);
		} else if ( Float.class.isAssignableFrom(numberType) ) {
			return Float.valueOf(numberString);
		} else if ( Long.class.isAssignableFrom(numberType) ) {
			return Long.valueOf(numberString);
		}
		return Double.valueOf(numberString);
	}

	/**
	 * Subtract two Number instances.
	 * 
	 * <p>The returned Number will be an instance of the {@code start} class.</p>
	 * 
	 * @param start the starting number to subtract from
	 * @param offset the amount to subtract
	 * @return a Number instance of the same type as {@code start}
	 */
	private Number diff(Number start, Number offset) {
		if ( start instanceof Integer ) {
			return Integer.valueOf(start.intValue() - offset.intValue());
		} else if ( start instanceof Float ) {
			return Float.valueOf(start.floatValue() - offset.floatValue());
		} else if ( start instanceof Long ) {
			return Long.valueOf(start.longValue() - offset.longValue());
		}
		return Double.valueOf(start.doubleValue() - offset.doubleValue());
	}

	/**
	 * Multiply two Number instances.
	 * 
	 * <p>The returned Number will be an instance of the {@code a} class.</p>
	 * 
	 * @param a first number
	 * @param b second number
	 * @return a Number instance of the same type as {@code a}
	 */
	private Number mult(Number a, Number b) {
		if ( a instanceof Integer ) {
			return Integer.valueOf(a.intValue() * b.intValue());
		} else if ( a instanceof Float ) {
			return Float.valueOf(a.floatValue() * b.floatValue());
		} else if ( a instanceof Long ) {
			return Long.valueOf(a.longValue() * b.longValue());
		}
		return Double.valueOf(a.doubleValue() * b.doubleValue());
	}

	/**
	 * Issue a SetData command for channels configured to reset daily.
	 * 
	 * <p>This class maintains a key/value pair in the {@link SettingDao} for the
	 * "last known" day of the year. If this value differs from the current day
	 * (or is not found) then this method will issue a {@code SetData} command for
	 * each channel configured in the {@link #getChannelNamesToResetDaily()} set
	 * with a value of zero. If the reset is successful, the current day value
	 * will be persistd back into {@link SettingDao} so the reset will not be
	 * attempted again until the next day.</p>
	 * 
	 * @param dataCollector the data collector to issue the SetData commands with
	 */
	private void handleDailyChannelReset(ConversationalDataCollector dataCollector) {
		Calendar now = Calendar.getInstance();
		String dayOfYear = String.valueOf(now.get(Calendar.DAY_OF_YEAR));
		
		String lastKnownDayOfYear = settingDao.getSetting(SETTING_LAST_KNOWN_DAY);
		if ( dayOfYear.equals(lastKnownDayOfYear) ) {
			if ( log.isTraceEnabled() ) {
				log.trace("Last known day of year [" +lastKnownDayOfYear 
						+"] has not changed, will not reset daily channels");
			}
			return;
		}
		
		
		if ( log.isInfoEnabled() ) {
			log.info("Day changed from [" +lastKnownDayOfYear +"] to [ " +dayOfYear 
					+"], will reset daily channels now");
		}
		for ( String channelName : this.channelNamesToResetDaily ) {
			if ( !this.channelMap.containsKey(channelName) ) {
				continue;
			}
			SmaChannel channel = this.channelMap.get(channelName);
			SmaPacket resp = issueSetData(dataCollector, channel, this.smaAddress, 0);
			if ( !resp.isValid() ) {
				log.warn("Invalid response to SetData reset command for channel [" 
						+channelName +"], aborting channel resets");
				return;
			}
		}
	}

	private SmaPacket issueGetData(
			ConversationalDataCollector dataCollector,
			SmaChannel channel, int address) {
		if ( log.isTraceEnabled() ) {
			log.trace("Getting data for channel " +channel);
		}
		byte[] data = SmaUtils.encodeGetDataRequestUserData(channel);
		SmaPacket req = writeCommand(dataCollector, SmaCommand.GetData, address, 0, 
			SmaControl.RequestSingle, data);
		
		return decodeResponse(dataCollector, req);
	}

	private SmaPacket issueSetData(
			ConversationalDataCollector dataCollector,
			SmaChannel channel, int address, int word) {
		if ( log.isTraceEnabled() ) {
			log.trace("Setting data for channel " +channel);
		}
		byte[] data = SmaUtils.encodeSetDataRequestUserData(channel, word);
		SmaPacket req = writeCommand(dataCollector, SmaCommand.SetData, address, 0, 
			SmaControl.RequestSingle, data);
		
		return decodeResponse(dataCollector, req);
	}

	@SuppressWarnings("unchecked")
	private Map<String, SmaChannel> getSmaChannelMap(SmaPacket resp) {
		Map<String, SmaChannel> channels = new LinkedHashMap<String, SmaChannel>();
		Object o = resp.getUserDataField(SmaUserDataField.Channels);
		if ( o instanceof List<?> ) {
			List<SmaChannel> list = (List<SmaChannel>)o;
			if ( log.isDebugEnabled() ) {
				log.debug("Available SMA channels:\n{}", StringUtils.delimitedStringFromCollection(list, ",\n"));
			}
			for ( SmaChannel channel : list ) {
				// prune out channels to only those we are interested in
				if ( !this.channelNamesToMonitor.contains(channel.getName()) ) {
					continue;
				}
				channels.put(channel.getName(), channel);
			}
		}
		return channels;
	}
		
	/**
	 * Write an SmaPacket and listen for a response.
	 * 
	 * <p>The returned {@link SmaPacket} can be passed to 
	 * {@link #decodeResponse(ConversationalDataCollector, SmaPacket)}
	 * to obtain the response value.</p>
	 * 
	 * @param dataCollector the data collector to use
	 * @param cmd the command to write
	 * @param destAddr the device destination address
	 * @param count the packet count (usually this will be 0)
	 * @param control the request control type (usually RequestSingle or RequestGroup)
	 * @param data the user data to include in the command
	 * @return the command request packet
	 */
	private SmaPacket writeCommand(ConversationalDataCollector dataCollector, 
			SmaCommand cmd, int destAddr, int count, SmaControl control, byte[] data) {
		SmaPacket packet = createRequestPacket(cmd, destAddr, count, control, data);
		dataCollector.speakAndListen(packet.getPacket());
		return packet;
	}

	/**
	 * Write an SmaPacket without listening for a response.
	 * 
	 * @param dataCollector the data collector to use
	 * @param cmd the command to write
	 * @param destAddr the device destination address
	 * @param count the packet count (usually this will be 0)
	 * @param control the request control type (usually RequestGroup)
	 * @param data the user data to include in the command
	 * @return the command request packet
	 */
	private SmaPacket writeProclamation(ConversationalDataCollector dataCollector, 
			SmaCommand cmd, int destAddr, int count, SmaControl control, byte[] data) {
		SmaPacket packet = createRequestPacket(cmd, destAddr, count, control, data);
		dataCollector.speak(packet.getPacket());
		return packet;
	}
	
	/**
	 * Create a new SmaPacket instance.
	 * 
	 * @param cmd the command to create
	 * @param destAddr the device destination address
	 * @param count the packet counter (requests usually use 0)
	 * @param control the request control type
	 * @param data the user data to add to the packet
	 * @return the new packet
	 */
	private SmaPacket createRequestPacket(SmaCommand cmd, int destAddr,
			int count, SmaControl control, byte[] data) {
		SmaPacket packet = new SmaPacket(0, destAddr, count, control, cmd, data);
		if ( log.isTraceEnabled() ) {
			log.trace("CRC: " +packet.getCrc());
		}
		if ( log.isDebugEnabled() ) {
			log.debug("Sending SMA request " +cmd +": " 
					+String.valueOf(Hex.encodeHex(packet.getPacket())));
		}
		return packet;
	}
	
	/**
	 * Decode a response to a request SmaPacket.
	 * 
	 * <p>This is usually called after 
	 * {@link #writeCommand(ConversationalDataCollector, SmaCommand, int, int, SmaControl, byte[])}
	 * to decode the response into a response SmaPacket instance.</p>
	 * 
	 * <p>The response might consist of many individual packets. This happens when the 
	 * first response packet contains a {@code packetCounter} value greater than 0. In this
	 * situation, this method will create new request packets based on the original request packet
	 * passed into this method, and call {@link ConversationalDataCollector#speakAndListen(byte[])}
	 * repeatedly until the {@code packetCounter} gets to 0. The {@code userData} values for each
	 * response packet will be combined into one byte array and returned with the final response
	 * packet as the {@code userData} value.</p>
	 * 
	 * @param dataCollector the data collector
	 * @param originalRequest the original request packet
	 * @return the response packet
	 */
	private SmaPacket decodeResponse(ConversationalDataCollector dataCollector, 
			SmaPacket originalRequest) {
		ByteArrayOutputStream byos = null;
		SmaPacket curr = null;
		// the packetCounter in the response is used to say "there are more packets of data coming"
		// so we loop here, calling getCollectedData() for the first packet and then if more
		// packets are available we write the original request command again but with the new count
		while ( curr == null || curr.getPacketCounter() > 0  ) {
			byte[] data = dataCollector.getCollectedData();
			if ( log.isDebugEnabled() ) {
				log.debug("Got response data: " +String.valueOf(Hex.encodeHex(data)));
			}
			curr = new SmaPacket(data);
			if ( curr.getPacketCounter() > 0 || byos != null ) {
				// this is a multi-packet response... store userData into BYOS
				if ( byos == null ) {
					byos = new ByteArrayOutputStream();
				}
				try {
					byos.write(curr.getUserData());
				} catch ( IOException e ) {
					// should not get here for BYOS
				}
				if ( curr.getPacketCounter() > 0 ) {
					SmaPacket packet = new SmaPacket(originalRequest.getSrcAddress(), 
							originalRequest.getDestAddress(), 
							curr.getPacketCounter(), originalRequest.getControl(), 
							originalRequest.getCommand(), originalRequest.getUserData());
					dataCollector.speakAndListen(packet.getPacket());
				}
			}
		}
		if ( byos == null ) {
			curr.decodeUserDataFields();
			return curr;
		}
		
		// this was a multi-packet response... we just replace the final userData value with
		// the data collected in the BYOS
		curr.setUserData(byos.toByteArray());
		curr.decodeUserDataFields();
		return curr;
	}
	
	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.power.sma.sunnynet";
	}

	@Override
	public String getDisplayName() {
		return "SMA SunnyNet inverter";
	}

	@Override
	public MessageSource getMessageSource() {
		synchronized (MONITOR) {
			if ( MESSAGE_SOURCE == null ) {
				ResourceBundleMessageSource serial = new ResourceBundleMessageSource();
				serial.setBundleClassLoader(SerialPortBeanParameters.class.getClassLoader());
				serial.setBasename(SerialPortBeanParameters.class.getName());

				PrefixedMessageSource serialSource = new PrefixedMessageSource();
				serialSource.setDelegate(serial);
				serialSource.setPrefix("serialParams.");

				ResourceBundleMessageSource source = new ResourceBundleMessageSource();
				source.setBundleClassLoader(SMASunnyNetPowerDatumDataSource.class.getClassLoader());
				source.setBasename(SMASunnyNetPowerDatumDataSource.class.getName());
				source.setParentMessageSource(serialSource);
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTitleSettingSpecifier("address", 
				(smaAddress < 0 ? "N/A" : String.valueOf(smaAddress)), true));

		results.add(new BasicTextFieldSettingSpecifier(
				"dataCollectorFactory.propertyFilters['UID']", DEFAULT_SERIAL_PORT));
		
		results.add(new BasicTextFieldSettingSpecifier("sourceId", DEFAULT_SOURCE_ID));
		
		results.add(new BasicTextFieldSettingSpecifier("pvVoltsChannelName", CHANNEL_NAME_PV_VOLTS));
		results.add(new BasicTextFieldSettingSpecifier("pvAmpsChannelName", CHANNEL_NAME_PV_AMPS));
		results.add(new BasicTextFieldSettingSpecifier("kWhChannelName", CHANNEL_NAME_KWH));
		
		results.add(new BasicTextFieldSettingSpecifier("channelNamesToOffsetDailyValue", ""));
		results.add(new BasicTextFieldSettingSpecifier("channelNamesToResetDailyValue", ""));
		
		results.add(new BasicTextFieldSettingSpecifier("synOnlineWaitMs", 
				String.valueOf(DEFAULT_SYN_ONLINE_WAIT_MS)));

		results.addAll(SerialPortBeanParameters.getDefaultSettingSpecifiers(
				SMASunnyNetPowerDatumDataSource.getDefaultSerialParameters(), "serialParams."));
		return results;
	}
	
	/**
	 * Set the channel names to monitor via a comma-delimited string.
	 * 
	 * @param list comma-delimited list of channel names to monitor
	 */
	public void setChannelNamesToOffsetDailyValue(String list) {
		setChannelNamesToOffsetDaily(StringUtils.commaDelimitedStringToSet(list));
	}

	/**
	 * Set the channel names to monitor via a comma-delimited string.
	 * 
	 * @param list comma-delimited list of channel names to monitor
	 */
	public void setChannelNamesToResetDailyValue(String list) {
		setChannelNamesToResetDaily(StringUtils.commaDelimitedStringToSet(list));
	}
	
	public long getSynOnlineWaitMs() {
		return synOnlineWaitMs;
	}
	
	public void setSynOnlineWaitMs(long synOnlineWaitMs) {
		this.synOnlineWaitMs = synOnlineWaitMs;
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
	
	public Set<String> getChannelNamesToResetDaily() {
		return channelNamesToResetDaily;
	}
	
	public void setChannelNamesToResetDaily(Set<String> channelNamesToResetDaily) {
		this.channelNamesToResetDaily = channelNamesToResetDaily;
	}
	
	public String getkWhChannelName() {
		return kWhChannelName;
	}
	
	public void setkWhChannelName(String kWhChannelName) {
		this.kWhChannelName = kWhChannelName;
		setupChannelNamesToMonitor();
	}
	
	public SettingDao getSettingDao() {
		return settingDao;
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}
	
	public Set<String> getChannelNamesToOffsetDaily() {
		return channelNamesToOffsetDaily;
	}
	
	public void setChannelNamesToOffsetDaily(Set<String> channelNamesToOffsetDaily) {
		this.channelNamesToOffsetDaily = channelNamesToOffsetDaily;
	}

	public SerialPortBeanParameters getSerialParams() {
		return serialParams;
	}

	public void setSerialParams(SerialPortBeanParameters serialParams) {
		this.serialParams = serialParams;
	}
	
	public DynamicServiceTracker<DataCollectorFactory<SerialPortBeanParameters>> getDataCollectorFactory() {
		return dataCollectorFactory;
	}
	
	public void setDataCollectorFactory(
			DynamicServiceTracker<DataCollectorFactory<SerialPortBeanParameters>> dataCollectorFactory) {
		this.dataCollectorFactory = dataCollectorFactory;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
