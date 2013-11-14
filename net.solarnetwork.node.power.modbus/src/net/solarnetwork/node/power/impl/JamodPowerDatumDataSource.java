/* ===================================================================
 * JamodPowerDatumDataSource.java
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
 */

package net.solarnetwork.node.power.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.io.modbus.ModbusSerialConnectionFactory;
import net.solarnetwork.node.power.PowerDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.DynamicServiceTracker;
import net.solarnetwork.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * {@link GenerationDataSource} implementation using the Jamod modbus serial
 * communication implementation.
 * 
 * <p>
 * This implementation was written to support one specific Morningstar TS-45
 * charge controller, but ideally this class could be used to support any
 * Modbus-based controller.
 * </p>
 * 
 * <p>
 * Pass -Dnet.wimpi.modbus.debug=true to the JVM to enable Jamod debug
 * communication output to STDOUT.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>address</dt>
 * <dd>The Modbus address of the coil-type register to read from.</dd>
 * 
 * <dt>unitId</dt>
 * <dd>The Modbus unit ID to use.</dd>
 * 
 * <dt>connectionFactory</dt>
 * <dd>The {@link ModbusSerialConnectionFactory} to use.</dd>
 * 
 * @author matt.magoffin
 * @version 1.2
 */
public class JamodPowerDatumDataSource implements DatumDataSource<PowerDatum>, SettingSpecifierProvider {

	private static MessageSource MESSAGE_SOURCE;

	private DynamicServiceTracker<ModbusSerialConnectionFactory> connectionFactory;

	private Integer unitId = 1;
	private Integer[] addresses = new Integer[] { 0x8, 0x10 };
	private Integer count = 5;
	private String sourceId = "Main";
	private Map<Integer, String> registerMapping = defaultRegisterMapping();
	private Map<Integer, Double> registerScaleFactor = defaultRegisterScaleFactor();
	private Map<Integer, String> hiLoRegisterMapping = defaultHiLoRegisterMapping();
	private SettingDao settingDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public Class<? extends PowerDatum> getDatumType() {
		return PowerDatum.class;
	}

	@Override
	public PowerDatum readCurrentDatum() {
		Map<Integer, Integer> words = ModbusHelper.readInputValues(connectionFactory, addresses, count,
				unitId);
		if ( words == null ) {
			return null;
		}
		PowerDatum datum = new PowerDatum();
		datum.setSourceId(sourceId);
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(datum);
		if ( registerMapping != null ) {
			for ( Map.Entry<Integer, String> me : registerMapping.entrySet() ) {
				final Integer addr = me.getKey();
				if ( words.containsKey(addr) ) {
					final Integer word = words.get(addr);
					setRegisterAddressValue(bean, addr, me.getValue(), word);
				} else {
					log.warn("Register address 0x{} not available", Integer.toHexString(addr));
				}
			}
		}
		if ( hiLoRegisterMapping != null ) {
			for ( Map.Entry<Integer, String> me : hiLoRegisterMapping.entrySet() ) {
				final int hiAddr = me.getKey();
				final int loAddr = hiAddr + 1;
				if ( words.containsKey(hiAddr) && words.containsKey(loAddr) ) {
					final int hiWord = words.get(hiAddr);
					final int loWord = words.get(loAddr);
					final int word = ModbusHelper.getLongWord(hiWord, loWord);
					setRegisterAddressValue(bean, hiAddr, me.getValue(), word);
				} else {
					log.warn("Register address 0x{} out of bounds, {} available",
							Integer.toHexString(me.getKey()), words.size());
				}
			}
		}

		return datum;
	}

	private void setRegisterAddressValue(final PropertyAccessor bean, final Integer addr,
			final String propertyName, final Integer propertyValue) {
		if ( bean.isWritableProperty(propertyName) ) {
			Number value = propertyValue;
			if ( registerScaleFactor != null && registerScaleFactor.containsKey(addr) ) {
				value = Double.valueOf(value.intValue() * registerScaleFactor.get(addr));
			}
			log.trace("Setting property {} for address 0x{} to [{}]", propertyName,
					Integer.toHexString(addr), value);
			try {
				bean.setPropertyValue(propertyName, value);
			} catch ( PropertyAccessException e ) {
				log.warn("Unable to set property {} to {} for address 0x{}: {}", propertyName, value,
						Integer.toHexString(addr), e.getMostSpecificCause().getMessage());
			}
		} else {
			log.warn("Property {} not available; bad configuration", propertyName);
		}
	}

	private static Map<Integer, Double> defaultRegisterScaleFactor() {
		// these are for the Morningstar TS-45
		Map<Integer, Double> map = new LinkedHashMap<Integer, Double>(5);
		map.put(0x8, 96.667 / 32768.0); // battery volts
		map.put(0xA, 139.15 / 32768.0); // pv volts
		map.put(0XB, 66.667 / 32768.0); // pv amps
		map.put(0xC, 316.667 / 32768.0); // dc output amps
		map.put(0x13, 0.1); // amp hours total
		return map;
	}

	private static Map<Integer, String> defaultHiLoRegisterMapping() {
		Map<Integer, String> map = new LinkedHashMap<Integer, String>(1);
		map.put(0x13, "ampHourReading");
		return map;
	}

	private static Map<Integer, String> defaultRegisterMapping() {
		Map<Integer, String> map = new LinkedHashMap<Integer, String>(1);
		map.put(0x8, "batteryVolts");
		map.put(0xA, "pvVolts");
		map.put(0xB, "pvAmps");
		map.put(0xC, "dcOutputAmps");
		return map;
	}

	/**
	 * Set the hi/lo register mapping via a comma and equal delimited string.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 * 
	 * @param value
	 *        the mapping value
	 */
	public void setHiLoRegisterMappingValue(String value) {
		Map<String, String> map = StringUtils.commaDelimitedStringToMap(value);
		Map<Integer, String> regMap = new LinkedHashMap<Integer, String>(map.size());
		for ( Map.Entry<String, String> me : map.entrySet() ) {
			try {
				regMap.put(Integer.valueOf(me.getKey(), 16), me.getValue());
			} catch ( NumberFormatException e ) {
				log.warn("HiLo register mapping keys must be hexidecimal integers); {} ignored",
						me.getKey());
			}
		}
		setHiLoRegisterMapping(regMap);
	}

	/**
	 * Get the high/low register mapping as a comma and equal delimited string.
	 * 
	 * @return
	 * @see #setHiLoRegisterMappingValue(String)
	 */
	public String getHiLoRegisterMappingValue() {
		return hexAddressMappingValue(hiLoRegisterMapping);
	}

	/**
	 * Set the register mapping via a comma and equal delimited string.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 * 
	 * @param value
	 *        the mapping value
	 */
	public void setRegisterMappingValue(String value) {
		Map<String, String> map = StringUtils.commaDelimitedStringToMap(value);
		Map<Integer, String> regMap = new LinkedHashMap<Integer, String>(map.size());
		for ( Map.Entry<String, String> me : map.entrySet() ) {
			try {
				regMap.put(Integer.valueOf(me.getKey(), 16), me.getValue());
			} catch ( NumberFormatException e ) {
				log.warn("Register mapping keys must be hexideciaml integers); {} ignored", me.getKey());
			}
		}
		setRegisterMapping(regMap);
	}

	/**
	 * Get the register mapping as a comma and equal delimited string.
	 * 
	 * @return
	 * @see #setRegisterMappingValue(String)
	 */
	public String getRegisterMappingValue() {
		return hexAddressMappingValue(registerMapping);
	}

	/**
	 * Set the register mapping via a comma and equal delimited string.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 * 
	 * @param value
	 *        the mapping value
	 */
	public void setRegisterScaleFactorValue(String value) {
		Map<String, String> map = StringUtils.commaDelimitedStringToMap(value);
		Map<Integer, Double> regMap = new LinkedHashMap<Integer, Double>(map.size());
		for ( Map.Entry<String, String> me : map.entrySet() ) {
			try {
				regMap.put(Integer.valueOf(me.getKey(), 16), Double.valueOf(me.getValue()));
			} catch ( NumberFormatException e ) {
				log.warn(
						"Register mapping keys must be hexidecimal integers and values doubles); {} -> {} ignored",
						me.getKey(), me.getValue());
			}
		}
		setRegisterScaleFactor(regMap);
	}

	/**
	 * Get the register mapping as a comma and equal delimited string.
	 * 
	 * @return
	 * @see #setRegisterScaleFactorValue(String)
	 */
	public String getRegisterScaleFactorValue() {
		return hexAddressMappingValue(registerScaleFactor);
	}

	/**
	 * Set the Modbus addresses to read via a comma-delimited string of
	 * hexidecimal numbers.
	 * 
	 * @param value
	 *        the list of addresses
	 */
	public void setAddressesValue(String value) {
		Set<String> addressSet = StringUtils.commaDelimitedStringToSet(value);
		List<Integer> addressList = new ArrayList<Integer>(addressSet.size());
		for ( String addr : addressSet ) {
			try {
				addressList.add(Integer.valueOf(addr, 16));
			} catch ( NumberFormatException e ) {
				log.warn("Address values must be hexidecimal integers; {} ignored", addr);
			}
		}
		setAddresses(addressList.toArray(new Integer[addressList.size()]));
	}

	/**
	 * Get the Modbus addresses to read as a comma-delimited string of
	 * hexidecimal numbers.
	 * 
	 * @return the list of addresses
	 */
	public String getAddressessValue() {
		List<String> addressList = new ArrayList<String>(addresses == null ? 0 : addresses.length);
		if ( addresses != null ) {
			for ( Integer addr : addresses ) {
				addressList.add(Integer.toHexString(addr));
			}
		}
		return StringUtils.delimitedStringFromCollection(addressList, ",");
	}

	private String hexAddressMappingValue(Map<Integer, ?> map) {
		StringBuilder buf = new StringBuilder();
		if ( map != null ) {
			for ( Map.Entry<Integer, ?> me : map.entrySet() ) {
				if ( buf.length() > 0 ) {
					buf.append(",");
				}
				buf.append(Integer.toHexString(me.getKey())).append('=')
						.append(me.getValue().toString());
			}
		}
		return buf.toString();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.power.modbus";
	}

	@Override
	public String getDisplayName() {
		return "Modbus power generation";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		JamodPowerDatumDataSource defaults = new JamodPowerDatumDataSource();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		results.add(new BasicTextFieldSettingSpecifier("connectionFactory.propertyFilters['UID']",
				"/dev/ttyUSB0"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", defaults.unitId.toString()));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", (defaults.sourceId == null ? ""
				: defaults.sourceId.toString())));
		results.add(new BasicTextFieldSettingSpecifier("addressesValue", defaults.getAddressessValue()));
		results.add(new BasicTextFieldSettingSpecifier("count", (defaults.getCount() == null ? ""
				: defaults.getCount().toString())));
		results.add(new BasicTextFieldSettingSpecifier("registerMappingValue", defaults
				.getRegisterMappingValue()));
		results.add(new BasicTextFieldSettingSpecifier("hiLoRegisterMappingValue", defaults
				.getHiLoRegisterMappingValue()));
		results.add(new BasicTextFieldSettingSpecifier("registerScaleFactorValue", defaults
				.getRegisterScaleFactorValue()));
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

	public Integer getUnitId() {
		return unitId;
	}

	public void setUnitId(Integer unitId) {
		this.unitId = unitId;
	}

	public Integer[] getAddresses() {
		return addresses;
	}

	public void setAddresses(Integer[] addresses) {
		this.addresses = addresses;
	}

	public DynamicServiceTracker<ModbusSerialConnectionFactory> getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(
			DynamicServiceTracker<ModbusSerialConnectionFactory> connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public Map<Integer, String> getRegisterMapping() {
		return registerMapping;
	}

	public void setRegisterMapping(Map<Integer, String> registerMapping) {
		this.registerMapping = registerMapping;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public Map<Integer, Double> getRegisterScaleFactor() {
		return registerScaleFactor;
	}

	public void setRegisterScaleFactor(Map<Integer, Double> registerScaleFactor) {
		this.registerScaleFactor = registerScaleFactor;
	}

	public Map<Integer, String> getHiLoRegisterMapping() {
		return hiLoRegisterMapping;
	}

	public void setHiLoRegisterMapping(Map<Integer, String> hiLoRegisterMapping) {
		this.hiLoRegisterMapping = hiLoRegisterMapping;
	}

	public SettingDao getSettingDao() {
		return settingDao;
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

}
