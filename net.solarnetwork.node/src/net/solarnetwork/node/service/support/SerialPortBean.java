/* ==================================================================
 * SerialPortBean.java - Oct 27, 2011 3:43:27 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A basic JavaBean class for serial port configuration elements.
 * 
 * <p>
 * The {@code dataBits}, {@code stopBits}, and {@code parity} class properties
 * should be initialized to values corresponding to the constants defined in the
 * {@code gnu.io.SerialPort} class (e.g. {@code gnu.io.SerialPort#DATABITS_8},
 * etc.).
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class SerialPortBean implements Cloneable {

	private static final SerialPortBean DEFAULTS = new SerialPortBean();

	private int baud = 19200;
	private int dataBits = 8;
	private int stopBits = 1;
	private int parity = 0;
	private int flowControl = -1;
	private int receiveThreshold = 40;
	private int receiveTimeout = -1;
	private int receiveFraming = -1;
	private int dtrFlag = 1;
	private int rtsFlag = 0;

	/**
	 * Default constructor.
	 */
	public SerialPortBean() {
		super();
	}

	/**
	 * Get a list of setting specifiers for this bean.
	 * 
	 * @param prefix
	 *        bean prefix to use
	 * @return setting specifiers
	 */
	public static List<SettingSpecifier> getDefaultSettingSpecifiers(String prefix) {
		return getDefaultSettingSpecifiers(DEFAULTS, prefix);
	}

	/**
	 * Get a list of setting specifiers for this bean.
	 * 
	 * @param defaults
	 *        the default values to use
	 * @param prefix
	 *        the bean prefix to use
	 * @return setting specifiers
	 */
	public static List<SettingSpecifier> getDefaultSettingSpecifiers(SerialPortBean defaults,
			String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "baud", String.valueOf(defaults.getBaud())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "dataBits",
				String.valueOf(defaults.getDataBits())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "stopBits",
				String.valueOf(defaults.getStopBits())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "parity",
				String.valueOf(defaults.getParity())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "flowControl",
				String.valueOf(defaults.getFlowControl())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "receiveThreshold",
				String.valueOf(defaults.getReceiveThreshold())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "receiveTimeout",
				String.valueOf(defaults.getReceiveTimeout())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "receiveFraming",
				String.valueOf(defaults.getReceiveFraming())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "dtrFlag",
				String.valueOf(defaults.getDtrFlag())));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "rtsFlag",
				String.valueOf(defaults.getRtsFlag())));
		return results;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the DTR toggle.
	 * 
	 * @return the DTR toggle
	 */
	public boolean isDtr() {
		return dtrFlag > 0;
	}

	/**
	 * Set the DTR toggle.
	 * 
	 * @param dtr
	 *        the DTR toggle to set
	 */
	public void setDtr(boolean dtr) {
		this.dtrFlag = (dtr ? 1 : 0);
	}

	/**
	 * Get the RTS toggle.
	 * 
	 * @return the RTS toggle
	 */
	public boolean isRts() {
		return rtsFlag > 0;
	}

	/**
	 * Set the RTS toggle.
	 * 
	 * @param rts
	 *        the RTS toggle to set
	 */
	public void setRts(boolean rts) {
		this.rtsFlag = (rts ? 1 : 0);
	}

	/**
	 * Get the SerialPort communication speed.
	 * 
	 * @return the baud; defaults to {@literal 19200}
	 */
	public int getBaud() {
		return baud;
	}

	/**
	 * Set the SerialPort communication speed.
	 * 
	 * @param baud
	 *        the baud to use
	 */
	public void setBaud(int baud) {
		this.baud = baud;
	}

	/**
	 * Get the serial port data bits.
	 * 
	 * @return the number of data bits; defaults to {@literal 8}
	 */
	public int getDataBits() {
		return dataBits;
	}

	/**
	 * Set the serial port number of data bits.
	 * 
	 * @param dataBits
	 *        the number of data bits to use
	 */
	public void setDataBits(int dataBits) {
		this.dataBits = dataBits;
	}

	/**
	 * Get the serial port number of stop bits.
	 * 
	 * @return the stop bits; defaults to {@literal 1}
	 */
	public int getStopBits() {
		return stopBits;
	}

	/**
	 * Set the serial port number of stop bits.
	 * 
	 * @param stopBits
	 *        the number of stop bits to use
	 */
	public void setStopBits(int stopBits) {
		this.stopBits = stopBits;
	}

	/**
	 * Get the serial port parity.
	 * 
	 * @return the parity; defaults to {@literal 0}
	 */
	public int getParity() {
		return parity;
	}

	/**
	 * The serial port parity setting to use.
	 * 
	 * <p>
	 * Valid values are:
	 * </p>
	 * 
	 * <dl>
	 * <dt>0</dt>
	 * <dd>None</dd>
	 * <dt>1</dt>
	 * <dd>Odd</dd>
	 * <dt>2</dt>
	 * <dd>Even</dd>
	 * <dt>3</dt>
	 * <dd>Mark</dd>
	 * <dt>4</dt>
	 * <dd>Space</dd>
	 * </dl>
	 * 
	 * @param parity
	 *        the parity
	 */
	public void setParity(int parity) {
		this.parity = parity;
	}

	/**
	 * Get the parity as a string value.
	 * 
	 * @return the parity, or {@literal null} if not supported
	 * @see #setParityString(String)
	 * @since 1.1
	 */
	public String getParityString() {
		switch (getParity()) {
			case 0:
				return "none";

			case 1:
				return "odd";

			case 2:
				return "even";

			default:
				return null;
		}
	}

	/**
	 * Set the parity as a string value.
	 * 
	 * <p>
	 * This method accepts the following values:
	 * </p>
	 * 
	 * <ul>
	 * <li>none</li>
	 * <li>odd</li>
	 * <li>even</li>
	 * </ul>
	 * 
	 * @param parity
	 *        the parity value to set
	 * @since 1.1
	 */
	public void setParityString(String parity) {
		parity = parity.toLowerCase();
		if ( parity.equals("none") ) {
			setParity(0);
		} else if ( parity.equals("odd") ) {
			setParity(1);
		} else if ( parity.equals("even") ) {
			setParity(2);
		}
	}

	/**
	 * Get the receive threshold.
	 * 
	 * @return the receive threshold; defaults to {@literal 40}
	 */
	public int getReceiveThreshold() {
		return receiveThreshold;
	}

	/**
	 * Set the SerialPort receive threshold setting.
	 * 
	 * <p>
	 * If set to anything less than 0 then the receive threshold will be
	 * disabled.
	 * </p>
	 * 
	 * @param receiveThreshold
	 *        the receive threshold to use
	 */
	public void setReceiveThreshold(int receiveThreshold) {
		this.receiveThreshold = receiveThreshold;
	}

	/**
	 * Get the SerialPort flow control setting to use.
	 * 
	 * @return the flow control; defaults to -1
	 */
	public int getFlowControl() {
		return flowControl;
	}

	/**
	 * Set the SerialPort flow control setting to use.
	 * 
	 * <p>
	 * If less than 0 flow control will not be configured. The settings are:
	 * </p>
	 * 
	 * <dl>
	 * <dt>0</dt>
	 * <dd>None</dd>
	 * <dt>1</dt>
	 * <dd>RTS CTS in</dd>
	 * <dt>2</dt>
	 * <dd>RTS CTS out</dd>
	 * <dt>4</dt>
	 * <dd>XON XOFF in</dd>
	 * <dt>8</dt>
	 * <dd>XON XOFF out</dd>
	 * </dl>
	 * 
	 * @param flowControl
	 *        the flow control to use
	 */
	public void setFlowControl(int flowControl) {
		this.flowControl = flowControl;
	}

	/**
	 * Get the receive timeout.
	 * 
	 * @return the receive timeout; defaults to {@literal -1}
	 */
	public int getReceiveTimeout() {
		return receiveTimeout;
	}

	/**
	 * Set the SerialPort receive timeout setting.
	 * 
	 * <p>
	 * If set to anything less than 0 then the receive timeout will be disabled.
	 * </p>
	 * 
	 * @param receiveTimeout
	 *        the receive timeout to use
	 */
	public void setReceiveTimeout(int receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Get the receive framing.
	 * 
	 * @return the receive framing; defaults to {@literal -1}
	 */
	public int getReceiveFraming() {
		return receiveFraming;
	}

	/**
	 * Set the SerialPort receive framing setting.
	 * 
	 * <p>
	 * If set to anything less than 0 then the receive framing will be disabled.
	 * </p>
	 * 
	 * @param receiveFraming
	 *        the receive framing to use
	 */
	public void setReceiveFraming(int receiveFraming) {
		this.receiveFraming = receiveFraming;
	}

	/**
	 * Get the DTR flag.
	 * 
	 * @return the DTR flag; defaults to {@literal 1}
	 */
	public int getDtrFlag() {
		return dtrFlag;
	}

	/**
	 * Set the SerialPort DTR setting to use.
	 * 
	 * <p>
	 * When set to {@code 0} DTR will be set to {@literal false}. When set to
	 * {@literal 1} DTR will be set to {@literal true}. When configured as less
	 * than zero the DTR setting will not be changed. The {@code dtr} property
	 * can also be used to set this as a boolean.
	 * </p>
	 * 
	 * @param dtrFlag
	 *        the DTR flag to use
	 */
	public void setDtrFlag(int dtrFlag) {
		this.dtrFlag = dtrFlag;
	}

	/**
	 * Get the RTS flag.
	 * 
	 * @return the RTS flag; defaults to {@literal 0}
	 */
	public int getRtsFlag() {
		return rtsFlag;
	}

	/**
	 * Set the SerialPort RTS setting to use.
	 * 
	 * <p>
	 * When set to {@literal 0} RTS will be set to {@literal false}. When set to
	 * {@literal 1} RTS will be set to {@literal true}. When configured as less
	 * than zero the RTS setting will not be changed. The {@code rts} property
	 * can also be used to set this as a boolean.
	 * </p>
	 * 
	 * @param rtsFlag
	 *        the RTS flag to use
	 */
	public void setRtsFlag(int rtsFlag) {
		this.rtsFlag = rtsFlag;
	}

}
