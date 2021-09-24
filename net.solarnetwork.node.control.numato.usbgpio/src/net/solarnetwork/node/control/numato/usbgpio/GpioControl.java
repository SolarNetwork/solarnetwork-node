/* ==================================================================
 * UbsGpioControl.java - 24/09/2021 1:18:27 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.numato.usbgpio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.io.serial.support.SerialDeviceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.ArrayUtils;

/**
 * {@link NodeControlProvider} for the Numato USB GPIO module.
 * 
 * @author matt
 * @version 1.0
 */
public class GpioControl extends SerialDeviceSupport
		implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler {

	/** The {@code serialNetworkUid} property default value. */
	public static final String DEFAULT_SERIAL_NETWORK_UID = "Serial Port";

	private final GpioService gpioService;
	private GpioPropertyConfig[] propConfigs;

	/**
	 * Constructor.
	 */
	public GpioControl(OptionalFilterableService<SerialNetwork> serialNetwork, GpioService gpioService) {
		super();
		setSerialNetwork(serialNetwork);
		setDisplayName("Numato USB GPIO Control");
		setSerialNetworkUid(DEFAULT_SERIAL_NETWORK_UID);
		this.gpioService = gpioService;
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> getAvailableControlIds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.numato.usbgpio";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTitleSettingSpecifier("info", getDeviceInfoMessage(), true));
		results.addAll(baseIdentifiableSettings(""));
		results.addAll(serialNetworkSettings("", DEFAULT_SERIAL_NETWORK_UID));
		return results;
	}

	/**
	 * Get the property configurations.
	 * 
	 * @return the property configurations
	 */
	public GpioPropertyConfig[] getPropConfigs() {
		return propConfigs;
	}

	/**
	 * Get the property configurations to use.
	 * 
	 * @param propConfigs
	 *        the configs to use
	 */
	public void setPropConfigs(GpioPropertyConfig[] propConfigs) {
		this.propConfigs = propConfigs;
	}

	/**
	 * Get the number of configured {@code propConfigs} elements.
	 * 
	 * @return the number of {@code propConfigs} elements
	 */
	public int getPropConfigsCount() {
		GpioPropertyConfig[] confs = this.propConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propConfigs} elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link GpioPropertyConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code propConfigs} elements.
	 */
	public void setPropConfigsCount(int count) {
		this.propConfigs = ArrayUtils.arrayWithLength(this.propConfigs, count, GpioPropertyConfig.class,
				null);
	}

}
