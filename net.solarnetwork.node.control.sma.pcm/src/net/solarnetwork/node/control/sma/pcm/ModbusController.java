/* ==================================================================
 * ModbusController.java - Jul 10, 2013 7:14:40 AM
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

package net.solarnetwork.node.control.sma.pcm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.io.modbus.ModbusSerialConnectionFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.NodeControlInfoDatum;
import net.solarnetwork.util.OptionalService;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.net.SerialConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Toggle four Modbus "coil" type addresses to control the SMA Power Control
 * Module.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>d1Address</dt>
 * <dd>The Modbus address for the PCM D1 input.</dd>
 * <dt>d2Address</dt>
 * <dd>The Modbus address for the PCM D2 input.</dd>
 * <dt>d3Address</dt>
 * <dd>The Modbus address for the PCM D3 input.</dd>
 * <dt>d4Address</dt>
 * <dd>The Modbus address for the PCM D4 input.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusController implements SettingSpecifierProvider, NodeControlProvider {

	private static MessageSource MESSAGE_SOURCE;

	private Integer d1Address = 0x4000;
	private Integer d2Address = 0x4002;
	private Integer d3Address = 0x4006;
	private Integer d4Address = 0x4008;

	private Integer unitId = 1;
	private String controlId = "/power/pcm/1";

	private OptionalService<ModbusSerialConnectionFactory> connectionFactory;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get the values of the D1 - D4 discreet values, as a BitSet.
	 * 
	 * @return BitSet, with index 0 representing D1 and index 1 representing D2,
	 *         etc.
	 */
	private synchronized BitSet currentDiscreetValue() {
		BitSet result = new BitSet(4);
		ModbusSerialConnectionFactory factory = (connectionFactory == null ? null : connectionFactory
				.service());
		if ( factory != null ) {
			SerialConnection conn = factory.getSerialConnection();
			Integer[] addresses = new Integer[] { d1Address, d2Address, d3Address, d4Address };
			for ( int i = 0; i < addresses.length; i++ ) {
				ModbusSerialTransaction trans = new ModbusSerialTransaction(conn);
				ReadCoilsRequest req = new ReadCoilsRequest(addresses[i], 1);
				req.setUnitID(this.unitId);
				req.setHeadless();
				trans.setRequest(req);
				try {
					trans.execute();
				} catch ( ModbusException e ) {
					throw new RuntimeException(e);
				}
				ReadCoilsResponse res = (ReadCoilsResponse) trans.getResponse();
				if ( log.isDebugEnabled() ) {
					log.debug("Got {} response [{}]", addresses[i], res);
				}
				result.set(i, res.getCoilStatus(0));
			}
		}
		return result;
	}

	/**
	 * Get the current value of the PCM, as an Integer.
	 * 
	 * <p>
	 * This returns the overall vale of the PCM, as an integer between 0 and 15.
	 * A value of 0 represent a 0% output setting, while 15 represents 100%.
	 * </p>
	 * 
	 * @return an integer between 0 and 15
	 */
	private Integer integerValueForBitSet(BitSet bits) {
		return ((bits.get(0) ? 1 : 0) | ((bits.get(1) ? 1 : 0) << 1) | ((bits.get(2) ? 1 : 0) << 2) | ((bits
				.get(3) ? 1 : 0) << 3));
	}

	// NodeControlProvider

	@Override
	public List<String> getAvailableControlIds() {
		return Collections.singletonList(controlId);
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		// read the control's current status
		log.debug("Reading PCM {} status", controlId);
		NodeControlInfoDatum result = null;
		try {
			Integer value = integerValueForBitSet(currentDiscreetValue());
			result = newNodeControlInfoDatum(controlId, value);
		} catch ( RuntimeException e ) {
			log.error("Error reading PCM {} status: {}", controlId, e.getMessage());
		}
		return result;
	}

	private NodeControlInfoDatum newNodeControlInfoDatum(String controlId, Integer status) {
		NodeControlInfoDatum info = new NodeControlInfoDatum();
		info.setCreated(new Date());
		info.setSourceId(controlId);
		info.setType(NodeControlPropertyType.Integer);
		info.setReadonly(false);
		info.setValue(status.toString());
		return info;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.sma.pcm";
	}

	@Override
	public String getDisplayName() {
		return "SMA Power Control Module";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ModbusController defaults = new ModbusController();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);

		// get current value
		BasicTitleSettingSpecifier status = new BasicTitleSettingSpecifier("status", "N/A", true);
		try {
			BitSet bits = currentDiscreetValue();
			Integer val = integerValueForBitSet(bits);
			status.setDefaultValue(String.format("%s - %.0f%%", Integer.toBinaryString(val), val));
		} catch ( RuntimeException e ) {
			log.debug("Error reading PCM status: {}", e.getMessage());
		}
		results.add(status);

		results.add(new BasicTextFieldSettingSpecifier("connectionFactory.propertyFilters['UID']",
				"/dev/ttyUSB0"));
		results.add(new BasicTextFieldSettingSpecifier("d1Address", defaults.d1Address.toString()));
		results.add(new BasicTextFieldSettingSpecifier("d2Address", defaults.d2Address.toString()));
		results.add(new BasicTextFieldSettingSpecifier("d3Address", defaults.d3Address.toString()));
		results.add(new BasicTextFieldSettingSpecifier("d4Address", defaults.d4Address.toString()));

		return results;
	}

	@Override
	public MessageSource getMessageSource() {
		if ( MESSAGE_SOURCE == null ) {
			ResourceBundleMessageSource source = new ResourceBundleMessageSource();
			source.setBundleClassLoader(getClass().getClassLoader());
			source.setBasename(getClass().getName());
			MESSAGE_SOURCE = source;
		}
		return MESSAGE_SOURCE;
	}

	public Integer getD1Address() {
		return d1Address;
	}

	public void setD1Address(Integer d1Address) {
		this.d1Address = d1Address;
	}

	public Integer getD2Address() {
		return d2Address;
	}

	public void setD2Address(Integer d2Address) {
		this.d2Address = d2Address;
	}

	public Integer getD3Address() {
		return d3Address;
	}

	public void setD3Address(Integer d3Address) {
		this.d3Address = d3Address;
	}

	public Integer getD4Address() {
		return d4Address;
	}

	public void setD4Address(Integer d4Address) {
		this.d4Address = d4Address;
	}

	public OptionalService<ModbusSerialConnectionFactory> getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(OptionalService<ModbusSerialConnectionFactory> connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public Integer getUnitId() {
		return unitId;
	}

	public void setUnitId(Integer unitId) {
		this.unitId = unitId;
	}

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

}
