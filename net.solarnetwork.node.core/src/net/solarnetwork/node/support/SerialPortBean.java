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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.support;

import java.util.ArrayList;
import java.util.List;

import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A basic JavaBean class for serial port configuration elements.
 * 
 * <p>The {@code dataBits}, {@code stopBits}, and {@code parity} class properties
 * should be initialized to values corresponding to the constants defined in the
 * {@link SerialPort} class (e.g. {@link SerialPort#DATABITS_8}, etc.).</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>baud</dt>
 *   <dd>The SerialPort communication speed. Defaults to {@code 19200}.</dd>
 *   
 *   <dt>dataBits</dt>
 *   <dd>The SerialPort number of data bits. Defaults to 
 *   {@link SerialPort#DATABITS_8}.</dd>
 *   
 *   <dt>stopBits</dt>
 *   <dd>The SerialPort number of stop bits. Defaults to 
 *   {@link SerialPort#STOPBITS_1}.</dd>
 *   
 *   <dt>parity</dt>
 *   <dd>The SerialPort parity setting to use. Defaults to 
 *   {@link SerialPort#PARITY_NONE}.</dd>
 *   
 *   <dt>flowControl</dt>
 *   <dd>The SerialPort flow control setting to use. If less than 0 will not be 
 *   configured. Defaults to -1.</dd>
 *   
 *   <dt>receiveThreshold</dt>
 *   <dd>The SerialPort receive threshold setting. Defaults to {@code 40}. If 
 *   set to anything less than 0 then the receive threshold will be disabled.</dd>
 *   
 *   <dt>receiveTimeout</dt>
 *   <dd>The SerialPort receive timeout setting. Defaults to {@code -1}. If 
 *   set to anything less than 0 then the receive timeout will be disabled.</dd>
 *   
 *   <dt>receiveFraming</dt>
 *   <dd>The SerialPort receive framing setting. Defaults to {@code -1}. If
 *   set to anything less than 0 then the receive framing will be disabled.</dd>
 *   
 *   <dt>dtrFlag</dt>
 *   <dd>The SerialPort DTR setting to use. Defaults to {@code 1}. When set 
 *   to {@code 0} DTR will be set to <em>false</em>. When set to {@code 1}
 *   DTR will be set to <em>true</em>. When configured as less than zero
 *   the DTR setting will not be changed. The {@code dtr} property can
 *   also be used to set this as a boolean.</dd>
 *   
 *   <dt>rtsFlag</dt>
 *   <dd>The SerialPort RTS setting to use. Defaults to {@code 0}. When set 
 *   to {@code 0} RTS will be set to <em>false</em>. When set to {@code 1}
 *   RTS will be set to <em>true</em>. When configured as less than zero
 *   the RTS setting will not be changed. The {@code rts} property can
 *   also be used to set this as a boolean.</dd>
 *   
 * </dl>
 *
 * @author matt
 * @version $Revision$
 */
public class SerialPortBean {

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
	 * Get a list of setting specifiers for this bean.
	 * 
	 * @param the
	 *            bean prefix to use
	 * @return setting specifiers
	 */
	public static List<SettingSpecifier> getDefaultSettingSpecifiers(String prefix) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "baud", "19200"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "dataBits", "8"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "stopBits", "1"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "parity", "0"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "flowControl", "-1"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "receiveThreshold", "40"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "receiveTimeout", "-1"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "receiveFraming", "-1"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "dtrFlag", "1"));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "rtsFlag", "0"));
		return results;
	}

	public boolean isDtr() {
		return dtrFlag > 0;
	}
	public void setDtr(boolean dtr) {
		this.dtrFlag = (dtr ? 1 : 0);
	}
	public boolean isRts() {
		return rtsFlag > 0;
	}
	public void setRts(boolean rts) {
		this.rtsFlag = (rts ? 1 : 0);
	}

	public int getBaud() {
		return baud;
	}
	public void setBaud(int baud) {
		this.baud = baud;
	}
	public int getDataBits() {
		return dataBits;
	}
	public void setDataBits(int dataBits) {
		this.dataBits = dataBits;
	}
	public int getStopBits() {
		return stopBits;
	}
	public void setStopBits(int stopBits) {
		this.stopBits = stopBits;
	}
	public int getParity() {
		return parity;
	}
	public void setParity(int parity) {
		this.parity = parity;
	}
	public int getReceiveThreshold() {
		return receiveThreshold;
	}
	public void setReceiveThreshold(int receiveThreshold) {
		this.receiveThreshold = receiveThreshold;
	}
	public int getFlowControl() {
		return flowControl;
	}
	public void setFlowControl(int flowControl) {
		this.flowControl = flowControl;
	}
	public int getReceiveTimeout() {
		return receiveTimeout;
	}
	public void setReceiveTimeout(int receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}
	public int getReceiveFraming() {
		return receiveFraming;
	}
	public void setReceiveFraming(int receiveFraming) {
		this.receiveFraming = receiveFraming;
	}
	public int getDtrFlag() {
		return dtrFlag;
	}
	public void setDtrFlag(int dtrFlag) {
		this.dtrFlag = dtrFlag;
	}
	public int getRtsFlag() {
		return rtsFlag;
	}
	public void setRtsFlag(int rtsFlag) {
		this.rtsFlag = rtsFlag;
	}

}
