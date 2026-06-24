package net.solarnetwork.node.demandresponse.mockbattery;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import net.solarnetwork.node.demandresponse.dretargetcost.DRSupportTools;
import net.solarnetwork.node.job.SimpleManagedTriggerAndJobDetail;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;

/**
 * Class to handle demand response instructions for a battery. This class
 * follows the demand response rules described in the DRSupportTools class. This
 * implementation is an example of a chargeable device, this means it can change
 * from charging and discharging states. To be able to send demand response
 * instructions to this device you need to include the name of the DR Engine as
 * a parameter to each instruction. An in-depth explanation on how instruction
 * parameters are formatted see DRSupportTools.
 * 
 * @author robert
 *
 */
public class DRBattery extends SimpleManagedTriggerAndJobDetail implements FeedbackInstructionHandler {

	@Override
	public boolean handlesTopic(String topic) {
		if (topic.equals(InstructionHandler.TOPIC_SHED_LOAD)) {
			return true;
		}
		if (topic.equals(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER)) {
			return true;
		}
		if (topic.equals(DRSupportTools.DRPARAMS_INSTRUCTION)) {
			return true;
		}
		return false;
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		// use the with feedback method and remove the feedback
		InstructionStatus status = processInstructionWithFeedback(instruction);
		return status.getAcknowledgedInstructionState();

	}

	@Override
	public InstructionStatus processInstructionWithFeedback(Instruction instruction) {
		DRBatteryDatumDataSource settings = getDRBatterySettings();
		InstructionState state;
		MockBattery battery = settings.getMockBattery();
		if (instruction.getTopic().equals(DRSupportTools.DRPARAMS_INSTRUCTION)) {

			Map<String, Object> map = new Hashtable<String, Object>();

			// this mock is always ready for demand response
			map.put(DRSupportTools.DRREADY_PARAM, "true");

			map.put(DRSupportTools.WATTS_PARAM, new Double(Math.abs(settings.getMockBattery().readDraw())).toString());

			// This device is chargeable,
			map.put(DRSupportTools.CHARGEABLE_PARAM, "true");
			map.put(DRSupportTools.DISCHARGING_PARAM, new Boolean(settings.getMockBattery().readDraw() > 0).toString());

			// A Battery only has a limited number of charge and discharge
			// cycles. The price of using energy is related to the cost of the
			// battery and the number of cycles it can do and the maximum
			// capacity of a charge.
			map.put(DRSupportTools.ENERGY_DEPRECIATION,
					new Integer((int) (settings.getBatteryCost().doubleValue()
							/ (settings.getBatteryCycles().doubleValue() * settings.getBatteryMaxCharge() * 2.0)))
									.toString());

			// This device is allowed to completly power down
			map.put(DRSupportTools.MINWATTS_PARAM, "0");

			map.put(DRSupportTools.MAXWATTS_PARAM, settings.getMaxDraw().toString());
			map.put(DRSupportTools.SOURCEID_PARAM, settings.getUID());
			map.put(DRSupportTools.CHARGE_PERCENTAGE_PARAM,
					new Integer((int) settings.getMockBattery().capacityFraction() * 100).toString());
			map.put(DRSupportTools.MAXCHARGE_PARAM, settings.getBatteryMaxCharge().toString());
			map.put(DRSupportTools.MAXCHARGEWATTS_PARAM, settings.getMaxChargeDraw().toString());

			// send the feedback of the instruction
			state = InstructionState.Completed;
			InstructionStatus status = new BasicInstructionStatus(instruction.getId(), state, new Date(), null, map);

			return status;

		}

		// This instruction is a little strange in that you have to map the DR
		// Engine name with the shed amount see DRSupportTools for better
		// explanation.
		if (instruction.getTopic().equals(InstructionHandler.TOPIC_SHED_LOAD)) {
			// the shed load value should be here
			String param = instruction.getParameterValue(settings.getDREngineName());

			if (param != null) {
				try {
					double value = Double.parseDouble(param);

					double draw = battery.readDraw();

					// negative draw for a mockbattery means we are charging
					if (draw < 0) {
						// to shed out load we need to add
						draw = draw + value;
					} else {
						draw = draw - value;
					}

					battery.setDraw(draw);
					state = InstructionState.Completed;

				} catch (NumberFormatException e) {
					// Incorrectly formatted parameters should be declined
					state = InstructionState.Declined;
				}

			} else {
				state = InstructionState.Declined;
			}
		} else {
			state = InstructionState.Declined;
		}

		if (instruction.getTopic().equals(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER)) {
			// verify the instruction came from the accepted DREngine
			if (instruction.getParameterValue(settings.getDREngineName()) != null) {

				// for a battery we need two parameters one is what the watt
				// level needs to be set to the other is whether we are charging
				// or discharging.
				String param = instruction.getParameterValue(DRSupportTools.WATTS_PARAM);
				String param2 = instruction.getParameterValue(DRSupportTools.DISCHARGING_PARAM);

				// check we got both parameters
				if (param != null && param2 != null) {
					try {

						double value = Double.parseDouble(param);

						// the discharge parameter should be in form "true" or
						// "false"
						boolean discharge = Boolean.parseBoolean(param2);

						if (value < 0) {
							// negative value does not make sense decline rather
							// than assume positive
							state = InstructionState.Declined;

						} else if (value > settings.getMaxDraw()) {
							// the implementation of mockbattery has a negative
							// draw
							// as charging and positive draw as discharging
							if (discharge) {
								battery.setDraw(settings.getMaxDraw());
							} else {
								battery.setDraw(-settings.getMaxDraw());
							}
							state = InstructionState.Completed;

						} else {
							// the implementation of mockbattery has a negative
							// draw
							// as charging and positive draw as discharging
							if (discharge) {
								battery.setDraw(value);
							} else {
								battery.setDraw(-value);
							}
							state = InstructionState.Completed;

						}

					} catch (NumberFormatException e) {
						state = InstructionState.Declined;
					}

				} else {
					state = InstructionState.Declined;
				}
			}

		}

		InstructionStatus status = new BasicInstructionStatus(instruction.getId(), state, new Date());

		return status;
	}

	public DRBatteryDatumDataSource getDRBatterySettings() {
		// Because of the way the OSGI is configured this is the best way I know
		// to get access to the settings
		return (DRBatteryDatumDataSource) getSettingSpecifierProvider();
	}

}
