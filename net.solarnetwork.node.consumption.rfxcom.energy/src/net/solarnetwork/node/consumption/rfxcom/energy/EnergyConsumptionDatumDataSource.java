/* ==================================================================
 * EnergyConsumptionDatumDataSource.java - Jul 8, 2012 4:44:30 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.consumption.rfxcom.energy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.solarnetwork.node.ConversationalDataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.rfxcom.Command;
import net.solarnetwork.node.rfxcom.CommandMessage;
import net.solarnetwork.node.rfxcom.EnergyMessage;
import net.solarnetwork.node.rfxcom.Message;
import net.solarnetwork.node.rfxcom.MessageFactory;
import net.solarnetwork.node.rfxcom.StatusMessage;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.solarnetwork.util.DynamicServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * {@link MultiDatumDataSource} for {@link ConsumptionDatum} entities
 * read from the supported energy formats of the RFXCOM transceiver.
 * 
 * @author matt
 * @version $Revision$
 */
public class EnergyConsumptionDatumDataSource 
implements DatumDataSource<ConsumptionDatum>, MultiDatumDataSource<ConsumptionDatum>, 
ConversationalDataCollector.Moderator<List<EnergyMessage>>, SettingSpecifierProvider {

	/** The default value for the {@code collectAllSourceIdsTimeout} property. */
	public static final int DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT = 55;
	
	/** The default value for the {@code voltage} property. */
	public static final float DEFAULT_VOLTAGE = 230.0F;
	
	private static final SerialPortBeanParameters DEFAULT_SERIAL_PARAMS = new SerialPortBeanParameters();
	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;
	
	static {
		DEFAULT_SERIAL_PARAMS.setBaud(38400);
		DEFAULT_SERIAL_PARAMS.setDataBits(8);
		DEFAULT_SERIAL_PARAMS.setStopBits(1);
		DEFAULT_SERIAL_PARAMS.setParity(0);
		DEFAULT_SERIAL_PARAMS.setDtrFlag(1);
		DEFAULT_SERIAL_PARAMS.setRtsFlag(1);
		DEFAULT_SERIAL_PARAMS.setReceiveThreshold(-1);
		DEFAULT_SERIAL_PARAMS.setReceiveTimeout(60000);
		DEFAULT_SERIAL_PARAMS.setMaxWait(65000);
	}

	private DynamicServiceTracker<DataCollectorFactory<SerialPortBeanParameters>> dataCollectorFactory;
	private SerialPortBeanParameters serialParams = (SerialPortBeanParameters)DEFAULT_SERIAL_PARAMS.clone();
	private Map<String,String> addressSourceMapping = null;
	private Set<String> sourceIdFilter = null;
	private boolean collectAllSourceIds = true;
	private int collectAllSourceIdsTimeout = DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT;
	private float voltage = DEFAULT_VOLTAGE;
	
	private boolean rfxcomInitialized = false;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public Class<? extends ConsumptionDatum> getMultiDatumType() {
		return ConsumptionDatum.class;
	}

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return ConsumptionDatum.class;
	}

	private ConsumptionDatum getConsumptionDatumInstance(EnergyMessage msg) {
		final ConsumptionDatum d = new ConsumptionDatum();
		d.setSourceId(msg.getAddress());
		final double wh = msg.getUsageWattHours();
		final double w = msg.getInstantWatts();
		if ( wh > 0 ) {
			d.setWattHourReading(Math.round(wh));
		} else {
			d.setAmps((float)w / voltage);
			d.setVolts(voltage);
		}
		d.setCreated(new Date());
		return d;
	}

	@Override
	public ConsumptionDatum readCurrentDatum() {
		final Collection<ConsumptionDatum> results = readMultipleDatum();
		if ( results != null && results.size() > 0 ) {
			return results.iterator().next();
		}
		return null;
	}

	@Override
	public Collection<ConsumptionDatum> readMultipleDatum() {
		final DataCollectorFactory<SerialPortBeanParameters> df = getDataCollectorFactory().service();
		if ( df == null ) {
			return null;
		}
		
		final List<EnergyMessage> messages;
		ConversationalDataCollector dc = null;
		try {
			dc = df.getConversationalDataCollectorInstance(getSerialParams());
			messages = dc.collectData(this);
		} finally {
			if ( dc != null ) {
				dc.stopCollecting();
			}
		}
		
		if ( messages == null ) {
			return null;
		}
		
		final List<ConsumptionDatum> results = new ArrayList<ConsumptionDatum>(messages.size());
		for ( EnergyMessage msg : messages ) {
			results.add(getConsumptionDatumInstance(msg));
		}
		return results;
	}

	@Override
	public List<EnergyMessage> conductConversation(ConversationalDataCollector dc) {
		final List<EnergyMessage> result = new ArrayList<EnergyMessage>(3);
		final long endTime = (isCollectAllSourceIds() 
				&& getSourceIdFilter() != null 
				&& getSourceIdFilter().size() > 1
					? System.currentTimeMillis() + (getCollectAllSourceIdsTimeout() * 1000)
					: 0);
		final Set<String> sourceIdSet = new HashSet<String>(
				getSourceIdFilter() == null ? 0 : getSourceIdFilter().size());
		
		final MessageFactory mf = new MessageFactory();
		
		if ( !rfxcomInitialized ) {
			// send reset, followed by status to see how rfxcom is configured
			dc.speak(new CommandMessage(Command.Reset).getMessagePacket());
			
			// wait at least 50ms
			try {
				Thread.sleep(100);
			} catch ( InterruptedException e ) {
				// ignore
			}
			
			dc.speakAndListen(new CommandMessage(Command.Status, 
					mf.incrementAndGetSequenceNumber()).getMessagePacket());
			
			Message msg = mf.parseMessage(dc.getCollectedData(), 0);
			if ( msg instanceof StatusMessage && log.isDebugEnabled() ) {
				StatusMessage status = (StatusMessage)msg;
				log.debug("RFXCOM status: firmware {}, product {}, Oregon {}", new Object[] {
						status.getFirmwareVersion(),
						status.getTransceiverType().getDescription(),
						status.isOregonEnabled()
				});
			}
			// TODO: add settings UI for configuring which RFXCOM devices are enabled
		}
		
		do {
			dc.listen();
			byte[] data = dc.getCollectedData();
			if ( data == null ) {
				log.warn("Null serial data received, serial communications problem");
				return null;
			}
			Message msg = mf.parseMessage(data, 0);
			if ( msg instanceof EnergyMessage ) {
				EnergyMessage emsg = (EnergyMessage)msg;
				if ( !sourceIdSet.contains(emsg.getAddress()) ) {
					result.add(emsg);
					sourceIdSet.add(emsg.getAddress());
				}
			}
		} while ( System.currentTimeMillis() < endTime && sourceIdSet.size() < 
				(getSourceIdFilter() == null ? 0 : getSourceIdFilter().size()) );
		return result;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.consumption.rfxcom.energy";
	}

	@Override
	public String getDisplayName() {
		return "RFXCOM energy consumption meter";
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
				source.setBundleClassLoader(EnergyConsumptionDatumDataSource.class.getClassLoader());
				source.setBasename(EnergyConsumptionDatumDataSource.class.getName());
				source.setParentMessageSource(serialSource);
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}
	
	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier(
				"dataCollectorFactory.propertyFilters['UID']", "/dev/ttyUSB0"));
		
		EnergyConsumptionDatumDataSource defaults = new EnergyConsumptionDatumDataSource();
		
		results.add(new BasicTextFieldSettingSpecifier("addressSourceMappingValue", ""));
		results.add(new BasicTextFieldSettingSpecifier("sourceIdFilterValue", ""));
		results.add(new BasicToggleSettingSpecifier("collectAllSourceIds", 
				defaults.collectAllSourceIds));
		results.add(new BasicTextFieldSettingSpecifier("collectAllSourceIdsTimeout", 
				String.valueOf(defaults.collectAllSourceIdsTimeout)));
		
		results.addAll(SerialPortBeanParameters.getDefaultSettingSpecifiers(
				DEFAULT_SERIAL_PARAMS, "serialParams."));
		return results;
	}

	/**
	 * Set a {@code addressSourceMapping} Map via an encoded String value.
	 * 
	 * <p>The format of the {@code mapping} String should be:</p>
	 * 
	 * <pre>key=val[,key=val,...]</pre>
	 * 
	 * <p>Whitespace is permitted around all delimiters, and will be stripped
	 * from the keys and values.</p>
	 * 
	 * @param mapping
	 */
	public void setAddressSourceMappingValue(String mapping) {
		if ( mapping == null || mapping.length() < 1 ) {
			setAddressSourceMapping(null);
			return;
		}
		String[] pairs = mapping.split("\\s*,\\s*");
		Map<String, String> map = new LinkedHashMap<String, String>();
		for ( String pair : pairs ) {
			String[] kv = pair.split("\\s*=\\s*");
			if ( kv == null || kv.length != 2 ) {
				continue;
			}
			map.put(kv[0], kv[1]);
		}
		setAddressSourceMapping(map);
	}
	
	/**
	 * Set a {@link sourceIdFilter} List via an encoded String value.
	 * 
	 * <p>The format of the {@code filters} String should be a comma-delimited
	 * list of values. Whitespace is permitted around the commas, and will be
	 * stripped from the values.</p>
	 * 
	 * @param filters
	 */
	public void setSourceIdFilterValue(String filters) {
		if ( filters == null || filters.length() < 1 ) {
			setSourceIdFilter(null);
			return;
		}
		String[] data = filters.split("\\s*,\\s*");
		Set<String> s = new LinkedHashSet<String>(data.length);
		for ( String d : data ) {
			s.add(d);
		}
		setSourceIdFilter(s);
	}

	public DynamicServiceTracker<DataCollectorFactory<SerialPortBeanParameters>> getDataCollectorFactory() {
		return dataCollectorFactory;
	}

	public void setDataCollectorFactory(
			DynamicServiceTracker<DataCollectorFactory<SerialPortBeanParameters>> dataCollectorFactory) {
		this.dataCollectorFactory = dataCollectorFactory;
	}

	public SerialPortBeanParameters getSerialParams() {
		return serialParams;
	}

	public void setSerialParams(SerialPortBeanParameters serialParams) {
		this.serialParams = serialParams;
	}

	public Map<String, String> getAddressSourceMapping() {
		return addressSourceMapping;
	}

	public void setAddressSourceMapping(Map<String, String> addressSourceMapping) {
		this.addressSourceMapping = addressSourceMapping;
	}

	public Set<String> getSourceIdFilter() {
		return sourceIdFilter;
	}

	public void setSourceIdFilter(Set<String> sourceIdFilter) {
		this.sourceIdFilter = sourceIdFilter;
	}

	public boolean isCollectAllSourceIds() {
		return collectAllSourceIds;
	}

	public void setCollectAllSourceIds(boolean collectAllSourceIds) {
		this.collectAllSourceIds = collectAllSourceIds;
	}

	public int getCollectAllSourceIdsTimeout() {
		return collectAllSourceIdsTimeout;
	}

	public void setCollectAllSourceIdsTimeout(int collectAllSourceIdsTimeout) {
		this.collectAllSourceIdsTimeout = collectAllSourceIdsTimeout;
	}

	public float getVoltage() {
		return voltage;
	}

	public void setVoltage(float voltage) {
		this.voltage = voltage;
	}
	
}
