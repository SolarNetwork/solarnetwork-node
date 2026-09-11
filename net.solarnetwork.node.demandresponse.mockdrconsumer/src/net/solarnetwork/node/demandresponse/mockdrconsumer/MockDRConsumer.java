/* ==================================================================
 * MockMeterDataSource.java - 10/06/2015 1:28:07 pm
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.demandresponse.mockdrconsumer;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.demandresponse.dretargetcost.DRSupportTools;
import net.solarnetwork.node.job.SimpleManagedTriggerAndJobDetail;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * Mock plugin to be the source of values for a GeneralNodeACEnergyDatum, this
 * mock tries to simulate a AC circuit containing a resister and inductor in
 * series.
 * 
 * <p>
 * This class implements {@link SettingSpecifierProvider} and
 * {@link DatumDataSource}
 * </p>
 * 
 * @author robert
 * @version 1.0
 */
public class MockDRConsumer extends SimpleManagedTriggerAndJobDetail implements FeedbackInstructionHandler {

	@Override
	public boolean handlesTopic(String topic) {
		if (topic.equals(InstructionHandler.TOPIC_SHED_LOAD)) {
			return true;
		} else if (topic.equals(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER)) {
			return true;
		} else if (topic.equals(DRSupportTools.DRPARAMS_INSTRUCTION)) {
			return true;
		}
		return false;
	}

	@Override
	// process an instruction just returning the status and not any feedback
	public InstructionState processInstruction(Instruction instruction) {
		InstructionStatus status = processInstructionWithFeedback(instruction);
		return status.getAcknowledgedInstructionState();
	}

	@Override
	/**
	 * List of supported Instructions getDRDeviceInstance, Shed load, set
	 * control parameter
	 */
	public InstructionStatus processInstructionWithFeedback(Instruction instruction) {
		InstructionState state;
		MockDRConsumerDatumDataSource settings = getSettings();

		if (instruction.getTopic().equals(DRSupportTools.DRPARAMS_INSTRUCTION)) {
			Map<String, Object> map = new Hashtable<String, Object>();

			// check that this instruction came from the accepted source
			if (instruction.getParameterValue(settings.getDrsource()) == null) {
				// if not decline the instruction
				state = InstructionState.Declined;
			} else {

				state = InstructionState.Completed;

				// put values in the parameter map
				map.put(DRSupportTools.DRREADY_PARAM, "true");
				map.put(DRSupportTools.WATTS_PARAM, settings.getWatts().toString());
				map.put(DRSupportTools.ENERGY_DEPRECIATION, settings.getEnergycost().toString());
				map.put(DRSupportTools.MINWATTS_PARAM, settings.getMinwatts().toString());
				map.put(DRSupportTools.MAXWATTS_PARAM, settings.getMaxwatts().toString());
				map.put(DRSupportTools.SOURCEID_PARAM, settings.getUID());
			}

			InstructionStatus status = new BasicInstructionStatus(instruction.getId(), state, new Date(), null, map);
			return status;

		}

		// The shed load instruction reduces the wattage value by the set amount
		if (instruction.getTopic().equals(InstructionHandler.TOPIC_SHED_LOAD)) {
			// the value to shed should be mapped to the name of the drsource as
			// was the convention used
			// by this instruction in classes not written by me.
			String param = instruction.getParameterValue(settings.getDrsource());
			if (param != null) {
				try {

					// I did not see anywhere it previous uses of this
					// instruction the requirment it had to be an interger
					// while DRDevice is set to integer values I read a double
					// and turn it to an int
					double value = Double.parseDouble(param) + 0.5;// 0.5 for
																	// rounding
					value = settings.getWatts() - value;
					if (value < settings.getMinwatts()) {
						settings.setWatts(settings.getMinwatts());
					} else if (value > settings.getMaxwatts()) {
						settings.setWatts(settings.getMaxwatts());
					} else {
						settings.setWatts((int) value);
					}

					state = InstructionState.Completed;
				} catch (NumberFormatException e) {

					// if we cannot parse any number decline the instruction
					// because something went wrong
					state = InstructionState.Declined;
				}

			} else {

				// instruction came from an untrusted source decline the
				// instruction
				state = InstructionState.Declined;
			}

			// this instruction sets the wattage to a specific value rather than
			// subtracting it like shed load
			// it is mainly used for increasing the wattage reading however it
			// can be used to reduce
		} else if (instruction.getTopic().equals(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER)) {

			String param = instruction.getParameterValue("watts");
			// be sure the instruction came from the accepted DR Engine
			if (instruction.getParameterValue(settings.getDrsource()) != null && param != null) {
				try {
					double value = Double.parseDouble(param);
					if (value < 0) {
						settings.setWatts(0);

					} else if (value > settings.getMaxwatts()) {
						settings.setWatts(settings.getMaxwatts());

					} else {
						settings.setWatts((int) value);
					}
					state = InstructionState.Completed;
				} catch (NumberFormatException e) {
					state = InstructionState.Declined;
				}

			} else {
				state = InstructionState.Declined;
			}
		} else {
			state = InstructionState.Declined;
		}

		InstructionStatus status = new BasicInstructionStatus(instruction.getId(), state, new Date());

		return status;
	}

	public MockDRConsumerDatumDataSource getSettings() {

		return (MockDRConsumerDatumDataSource) getSettingSpecifierProvider();
	}

}
