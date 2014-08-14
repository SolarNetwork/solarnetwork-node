/* ==================================================================
 * ModbusHeartbeatJob.java - Mar 22, 2014 4:02:14 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.modbus.heartbeat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.job.AbstractJob;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.DynamicServiceTracker;
import org.joda.time.DateTime;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;
import org.springframework.context.MessageSource;

/**
 * Periodically set a Modbus "coil" type register to a specific value, to act as
 * a "heartbeat" to the device so it knows the SolarNode is alive and well.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>address</dt>
 * <dd>The Modbus address of the coil-type register to use.</dd>
 * <dt>unitId</dt>
 * <dd>The Modbus unit ID to use.</dd>
 * <dt>registerValue</dt>
 * <dd>The value to set the Modbus register to.</dd>
 * <dt>modbusNetwork</dt>
 * <dd>The {@link ModbusNetwork} service to use.</dd>
 * <dt>messageSource</dt>
 * <dd>The {@link MessageSource} to use to support
 * {@link SettingSpecifierProvider}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 2.0
 */
public class ModbusHeartbeatJob extends AbstractJob implements StatefulJob, SettingSpecifierProvider {

	private Integer address = 0x4008;
	private Integer unitId = 1;
	private Boolean registerValue = Boolean.TRUE;
	private DynamicServiceTracker<ModbusNetwork> modbusNetwork;
	private MessageSource messageSource;

	// static map to keep track of job execution status info
	private static final ConcurrentMap<Long, JobStatus> STATUS_MAP = new ConcurrentHashMap<Long, JobStatus>(
			8);

	@Override
	protected void executeInternal(JobExecutionContext jobContext) throws Exception {
		final DateTime heartbeatDate = new DateTime();
		String heartbeatMessage = null;
		boolean heartbeatSuccess = false;
		try {
			final Boolean executed = setValue(registerValue);
			if ( executed == null ) {
				// hmm, how can this be localized?
				heartbeatMessage = "No Modbus connection available.";
			} else if ( executed.booleanValue() == false ) {
				heartbeatMessage = "Unknown Modbus error.";
			} else {
				// good
				heartbeatSuccess = true;
			}
		} catch ( RuntimeException e ) {
			log.error("Error sending heartbeat message: {}", e.toString());
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			heartbeatMessage = "Error: " + root.getMessage();
		}
		STATUS_MAP.put(getStatusKey(), new JobStatus(heartbeatDate, heartbeatSuccess, heartbeatMessage));
	}

	private Long getStatusKey() {
		return ((unitId == null ? 0L : unitId.longValue()) << 32)
				| (address == null ? 0L : address.longValue());
	}

	private synchronized Boolean setValue(Boolean desiredValue) throws IOException {
		final ModbusNetwork network = (modbusNetwork == null ? null : modbusNetwork.service());
		if ( network == null ) {
			log.debug("No ModbusNetwork avaialble");
			return Boolean.FALSE;
		}
		final BitSet bits = new BitSet(1);
		bits.set(0, desiredValue);
		log.info("Setting modbus unit {} register {} value to {}", unitId, address, desiredValue);
		final Integer[] addresses = new Integer[] { address };
		return network.performAction(new ModbusConnectionAction<Boolean>() {

			@Override
			public Boolean doWithConnection(ModbusConnection conn) throws IOException {
				return conn.writeDiscreetValues(addresses, bits);
			}
		}, unitId);
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.modbus.heartbeat";
	}

	@Override
	public String getDisplayName() {
		return "Modbus Heartbeat";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ModbusHeartbeatJob defaults = new ModbusHeartbeatJob();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(6);

		// add info on last execution state
		BasicTitleSettingSpecifier lhDate = new BasicTitleSettingSpecifier("lastHeartbeatDate", "N/A",
				true);

		JobStatus lastHeartbeatStatus = STATUS_MAP.get(getStatusKey());
		if ( lastHeartbeatStatus != null ) {
			lhDate.setDefaultValue(lastHeartbeatStatus.getDate().toString());
		}
		results.add(lhDate);
		if ( lastHeartbeatStatus != null && lastHeartbeatStatus.getMessage() != null ) {
			results.add(new BasicTitleSettingSpecifier("lastHeartbeatMessage", lastHeartbeatStatus
					.getMessage(), true));
		}

		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", defaults.unitId.toString()));
		results.add(new BasicTextFieldSettingSpecifier("address", defaults.address.toString()));
		results.add(new BasicToggleSettingSpecifier("registerValue", defaults.registerValue.toString()));
		return results;
	}

	// Accessors

	public void setAddress(Integer address) {
		this.address = address;
	}

	public void setUnitId(Integer unitId) {
		this.unitId = unitId;
	}

	public void setRegisterValue(Boolean registerValue) {
		this.registerValue = registerValue;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setModbusNetwork(DynamicServiceTracker<ModbusNetwork> modbusNetwork) {
		this.modbusNetwork = modbusNetwork;
	}

}
