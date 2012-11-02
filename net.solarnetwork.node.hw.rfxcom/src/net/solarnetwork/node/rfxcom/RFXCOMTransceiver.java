/* ==================================================================
 * RFXCOMTransceiver.java - Jul 9, 2012 12:05:16 PM
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

package net.solarnetwork.node.rfxcom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.solarnetwork.node.ConversationalDataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.solarnetwork.util.DynamicServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * {@link SettingSpecifierProvider} for RFXCOM transceiver, allowing
 * for the configuration of the transceiver via settings.
 * 
 * @author matt
 * @version $Revision$
 */
public class RFXCOMTransceiver implements RFXCOM, SettingSpecifierProvider {

	private static final SerialPortBeanParameters DEFAULT_SERIAL_PARAMS = new SerialPortBeanParameters();

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

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	private DynamicServiceTracker<DataCollectorFactory<SerialPortBeanParameters>> dataCollectorFactory;
	private SerialPortBeanParameters serialParams = getDefaultSerialParameters();

	private final MessageFactory mf = new MessageFactory();
	private StatusMessage status = null;
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get the default serial parameters used for RFXCOM transceivers.
	 * @return
	 */
	public static final SerialPortBeanParameters getDefaultSerialParameters() {
		return (SerialPortBeanParameters)DEFAULT_SERIAL_PARAMS.clone();
	}
	
	@Override
	public String getUID() {
		return (dataCollectorFactory == null ? null : dataCollectorFactory.getPropertyFilters() == null ? null
				: (String)dataCollectorFactory.getPropertyFilters().get("UID"));
	}

	@Override
	public ConversationalDataCollector getDataCollectorInstance() {
		final DataCollectorFactory<SerialPortBeanParameters> df = getDataCollectorFactory().service();
		if ( df == null ) {
			return null;
		}
		ConversationalDataCollector dc = df.getConversationalDataCollectorInstance(getSerialParams());
		if ( status == null ) {
			status = dc.collectData(new ConversationalDataCollector.Moderator<StatusMessage>() {
				@Override
				public StatusMessage conductConversation(ConversationalDataCollector dataCollector) {
					return getStatus(dataCollector);
				}
			});
		}
		return dc;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.rfxcom";
	}

	@Override
	public String getDisplayName() {
		return "RFXCOM transceiver";
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
				source.setBundleClassLoader(RFXCOMTransceiver.class.getClassLoader());
				source.setBasename(RFXCOMTransceiver.class.getName());
				source.setParentMessageSource(serialSource);
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}
	
	private void addToggleSetting(List<SettingSpecifier> results, PropertyAccessor bean, String name) {
		results.add(new BasicToggleSettingSpecifier(name, 
				(bean == null ? Boolean.FALSE : bean.getPropertyValue(name)), true));
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier(
				"dataCollectorFactory.propertyFilters['UID']", "/dev/ttyUSB0"));
		
		if ( status == null ) {
			try {
				updateStatus();
			} catch ( RuntimeException e ) {
				log.warn("Unable to update RFXCOM status", e.getCause());
			}
		}
		if ( status != null ) {
			log.debug("RFXCOM status: firmware {}, product {}, Oregon {}", new Object[] {
					status.getFirmwareVersion(),
					status.getTransceiverType().getDescription(),
					status.isOregonEnabled()
			});
			
			results.add(new BasicTitleSettingSpecifier("firmwareVersion", 
					(status == null ? "N/A" : String.valueOf(status.getFirmwareVersion())), true));
			results.add(new BasicTitleSettingSpecifier("transceiverType", 
					(status == null ? "N/A" : status.getTransceiverType().getDescription()), true));
			
			PropertyAccessor bean = (status == null ? null : PropertyAccessorFactory.forBeanPropertyAccess(status));
			addToggleSetting(results, bean, "ACEnabled");
			addToggleSetting(results, bean, "ADEnabled");
			addToggleSetting(results, bean, "ARCEnabled");
			addToggleSetting(results, bean, "ATIEnabled");
			addToggleSetting(results, bean, "FS20Enabled");
			addToggleSetting(results, bean, "hidekiEnabled");
			addToggleSetting(results, bean, "homeEasyEUEnabled");
			addToggleSetting(results, bean, "ikeaKopplaEnabled");
			addToggleSetting(results, bean, "laCrosseEnabled");
			addToggleSetting(results, bean, "mertikEnabled");
			addToggleSetting(results, bean, "oregonEnabled");
			addToggleSetting(results, bean, "proGuardEnabled");
			addToggleSetting(results, bean, "visonicEnabled");
			addToggleSetting(results, bean, "x10Enabled");
			
			addToggleSetting(results, bean, "undecodedMode");
		}

		results.addAll(SerialPortBeanParameters.getDefaultSettingSpecifiers(
				RFXCOMTransceiver.getDefaultSerialParameters(), "serialParams."));
		return results;
	}
	
	public void updateModeSetting(String name, Object value) {
		if ( this.status == null ) {
			updateStatus();
		}
		if ( this.status != null ) {
			SetModeMessage msg = new SetModeMessage(mf.incrementAndGetSequenceNumber(), this.status);
			PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(msg);
			Object currValue = bean.getPropertyValue(name);
			if ( value != null && !value.equals(currValue) ) {
				bean.setPropertyValue(name, value);
				setMode(msg);
			}
		}
	}
	
	/**
	 * Update the settings of this class.
	 * 
	 * <p>This method is designed to work with Spring's bean-managed OSGi Configuration
	 * Admin service, rather than the container-managed approach of setting properties
	 * directly. This is because many of the supported properties require communicating
	 * with the RFXCOM device, but those can all be set via a single call. Thus the
	 * supported properties of this method are those properties directly available on
	 * this class itself, and those available on the {@link SetModeMessage} class.
	 * 
	 * @param properties the properties to change
	 */
	public void updateConfiguration(Map<String, ?> properties) {
		Map<String, Object> setModeProperties = new HashMap<String, Object>(properties);
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(this);
		
		// if this is NOT something that must be handled via a SetMode command, apply those directly...
		for ( Map.Entry<String, ?> me : properties.entrySet() ) {
			if ( bean.isWritableProperty(me.getKey()) ) {
				bean.setPropertyValue(me.getKey(), me.getValue());
			} else {
				setModeProperties.put(me.getKey(), me.getValue());
			}
		}
		
		// and now apply remaining properties via single SetMode, so we only have to talk to
		// device one time
		
		if ( this.status == null ) {
			updateStatus();
		}
		if ( this.status != null ) {
			SetModeMessage msg = new SetModeMessage(mf.incrementAndGetSequenceNumber(), this.status);
			bean = PropertyAccessorFactory.forBeanPropertyAccess(msg);
			boolean changed = false;
			for ( Map.Entry<String, Object> me : setModeProperties.entrySet() ) {
				if ( bean.isReadableProperty(me.getKey()) ) {
					Object currValue = bean.getPropertyValue(me.getKey());
					if ( me.getValue() != null && me.getValue().equals(currValue) ) {
						continue;
					}
				}
				if ( bean.isWritableProperty(me.getKey()) ) {
					bean.setPropertyValue(me.getKey(), me.getValue());
					changed = true;
				}
			}
			if ( changed ) {
				log.debug("Updating RFXCOM settings to {}", msg);
				setMode(msg);
			}
		}
		
	}

	private void updateStatus() {
		final ConversationalDataCollector dc = getDataCollectorInstance();
		if ( dc == null ) {
			return;
		}
		try {
			status = dc.collectData(new ConversationalDataCollector.Moderator<StatusMessage>() {
				@Override
				public StatusMessage conductConversation(ConversationalDataCollector dataCollector) {
					return getStatus(dataCollector);
				}
			});
		} finally {
			dc.stopCollecting();
		}
	}

	private void setMode(final SetModeMessage msg) {
		final MessageListener listener = new MessageListener();
		ConversationalDataCollector dc = null;
		try {
			dc = getDataCollectorInstance();
			if ( dc == null ) {
				return;
			}
			StatusMessage result = dc.collectData(new ConversationalDataCollector.Moderator<StatusMessage>() {
				@Override
				public StatusMessage conductConversation(ConversationalDataCollector dc) {
					dc.speakAndListen(msg.getMessagePacket(), listener);
					
					Message msg = mf.parseMessage(dc.getCollectedData(), 0);
					StatusMessage result = null;
					if ( msg instanceof StatusMessage ) {
						result = (StatusMessage)msg;
					}
					return result;
				}
			});
			if ( result != null ) {
				if ( log.isDebugEnabled() ) {
					log.debug("RFXCOM status: firmware {}, product {}, Oregon {}", new Object[] {
							status.getFirmwareVersion(),
							status.getTransceiverType().getDescription(),
							status.isOregonEnabled()
					});
				}
				status = result;
			}
		} finally {
			if ( dc != null ) {
				dc.stopCollecting();
			}
		}
	}
	
	private StatusMessage getStatus(ConversationalDataCollector dc) {
		final MessageListener listener = new MessageListener();

		// send reset, followed by status to see how rfxcom is configured
		dc.speak(new CommandMessage(Command.Reset).getMessagePacket());
		
		// wait at least 50ms
		try {
			Thread.sleep(100);
		} catch ( InterruptedException e ) {
			// ignore
		}
		
		dc.speakAndListen(new CommandMessage(Command.Status, 
				mf.incrementAndGetSequenceNumber()).getMessagePacket(), listener);
		
		Message msg = mf.parseMessage(dc.getCollectedData(), 0);
		StatusMessage result = null;
		if ( msg instanceof StatusMessage ) {
			result = (StatusMessage)msg;
			if ( log.isDebugEnabled() ) {
				log.debug("RFXCOM status: firmware {}, product {}, Oregon {}", new Object[] {
						result.getFirmwareVersion(),
						result.getTransceiverType().getDescription(),
						result.isOregonEnabled()
				});
			}
		}
		return result;
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

}
