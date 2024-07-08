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
 */

package net.solarnetwork.node.rfxcom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.ByteList;

/**
 * {@link SettingSpecifierProvider} for RFXCOM transceiver, allowing for the
 * configuration of the transceiver via settings.
 *
 * @author matt
 * @version 2.1
 */
public class RFXCOMTransceiver extends BasicIdentifiable implements RFXCOM, SettingSpecifierProvider {

	/*-
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
	*/

	private OptionalFilterableService<SerialNetwork> serialNetwork;

	private final MessageFactory mf = new MessageFactory();
	private StatusMessage status = null;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	public RFXCOMTransceiver() {
		super();
		setDisplayName("RFXCOM transceiver");
	}

	@Override
	public void listenForMessages(MessageHandler handler) throws IOException {
		SerialNetwork network = OptionalService.service(serialNetwork);
		if ( network == null ) {
			return;
		}
		network.performAction(new SerialConnectionAction<Void>() {

			@Override
			public Void doWithConnection(SerialConnection conn) throws IOException {
				ByteList data = new ByteList();
				while ( true ) {
					byte[] bytes = conn.drainInputBuffer();
					if ( bytes.length < 1 ) {
						try {
							Thread.sleep(200);
						} catch ( InterruptedException e ) {
							// ignore
						}
					}
					data.addAll(bytes);
					Message msg = mf.parseMessage(data.toArrayValue(), 0);
					if ( msg != null ) {
						int len = msg.getPacketSize() + 1;
						data.remove(0, len);
						if ( !handler.handleMessage(msg) ) {
							break;
						}
					}
				}
				return null;
			}
		});
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.rfxcom";
	}

	private void addToggleSetting(List<SettingSpecifier> results, PropertyAccessor bean, String name) {
		results.add(new BasicToggleSettingSpecifier(name,
				(bean == null ? Boolean.FALSE : bean.getPropertyValue(name)), true));
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(20);
		results.addAll(basicIdentifiableSettings());
		results.add(new BasicTextFieldSettingSpecifier("serialNetwork.propertyFilters['uid']", null,
				false, "(objectClass=net.solarnetwork.node.io.serial.SerialNetwork)"));

		if ( status == null ) {
			try {
				updateStatus();
			} catch ( Exception e ) {
				log.warn("Unable to update RFXCOM status", e.getCause());
			}
		}
		if ( status != null ) {
			log.debug("RFXCOM status: firmware {}, product {}, Oregon {}",
					new Object[] { status.getFirmwareVersion(),
							status.getTransceiverType().getDescription(), status.isOregonEnabled() });

			results.add(new BasicTitleSettingSpecifier("firmwareVersion",
					(status == null ? "N/A" : String.valueOf(status.getFirmwareVersion())), true));
			results.add(new BasicTitleSettingSpecifier("transceiverType",
					(status == null ? "N/A" : status.getTransceiverType().getDescription()), true));

			PropertyAccessor bean = (status == null ? null
					: PropertyAccessorFactory.forBeanPropertyAccess(status));
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

		return results;
	}

	public void updateModeSetting(String name, Object value) throws IOException {
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
	 * <p>
	 * This method is designed to work with Spring's bean-managed OSGi
	 * Configuration Admin service, rather than the container-managed approach
	 * of setting properties directly. This is because many of the supported
	 * properties require communicating with the RFXCOM device, but those can
	 * all be set via a single call. Thus the supported properties of this
	 * method are those properties directly available on this class itself, and
	 * those available on the {@link SetModeMessage} class.
	 *
	 * @param properties
	 *        the properties to change
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
		try {
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
		} catch ( IOException e ) {
			log.warn("Error communicating with RFCOM transceiver: " + e.getMessage(), e);
		}
	}

	private void updateStatus() throws IOException {
		SerialNetwork network = OptionalService.service(serialNetwork);
		if ( network == null ) {
			return;
		}
		this.status = network.performAction(new SerialConnectionAction<StatusMessage>() {

			@Override
			public StatusMessage doWithConnection(SerialConnection conn) throws IOException {
				return getStatus(conn);
			}
		});
	}

	private void setMode(final SetModeMessage msg) throws IOException {
		SerialNetwork network = OptionalService.service(serialNetwork);
		if ( network == null ) {
			return;
		}
		StatusMessage result = network.performAction(new SerialConnectionAction<StatusMessage>() {

			@Override
			public StatusMessage doWithConnection(SerialConnection conn) throws IOException {
				conn.writeMessage(msg.getMessagePacket());
				ByteList data = new ByteList();
				while ( true ) {
					byte[] bytes = conn.drainInputBuffer();
					if ( bytes.length < 1 ) {
						break;
					}
					data.addAll(bytes);
				}
				Message msg = mf.parseMessage(data.toArrayValue(), 0);
				StatusMessage result = null;
				if ( msg instanceof StatusMessage ) {
					result = (StatusMessage) msg;
				}
				return result;
			}
		});
		if ( result != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug("RFXCOM status: firmware {}, product {}, Oregon {}",
						new Object[] { status.getFirmwareVersion(),
								status.getTransceiverType().getDescription(),
								status.isOregonEnabled() });
			}
			status = result;
		}
	}

	private StatusMessage getStatus(SerialConnection conn) throws IOException {
		// send reset, followed by status to see how rfxcom is configured
		conn.writeMessage(new CommandMessage(Command.Reset).getMessagePacket());

		// wait at least 50ms
		try {
			Thread.sleep(100);
		} catch ( InterruptedException e ) {
			// ignore
		}

		conn.writeMessage(new CommandMessage(Command.Status, mf.incrementAndGetSequenceNumber())
				.getMessagePacket());
		ByteList data = new ByteList();
		while ( true ) {
			byte[] bytes = conn.drainInputBuffer();
			if ( bytes.length < 1 ) {
				break;
			}
			data.addAll(bytes);
		}
		Message msg = mf.parseMessage(data.toArrayValue(), 0);
		StatusMessage result = null;
		if ( msg instanceof StatusMessage ) {
			result = (StatusMessage) msg;
			if ( log.isDebugEnabled() ) {
				log.debug("RFXCOM status: firmware {}, product {}, Oregon {}",
						new Object[] { result.getFirmwareVersion(),
								result.getTransceiverType().getDescription(),
								result.isOregonEnabled() });
			}
		}
		return result;
	}

	/**
	 * Get the serial network.
	 *
	 * @return the serial network
	 */
	public OptionalFilterableService<SerialNetwork> getSerialNetwork() {
		return serialNetwork;
	}

	/**
	 * Set the serial network.
	 *
	 * @param serialNetwork
	 *        the serial network
	 */
	public void setSerialNetwork(OptionalFilterableService<SerialNetwork> serialNetwork) {
		this.serialNetwork = serialNetwork;
	}

}
