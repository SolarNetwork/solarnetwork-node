package net.solarnetwork.node.demandresponse.dresimplestrategy;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import net.solarnetwork.node.demandresponse.dretargetcost.DRSupportTools;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.support.BasicInstruction;

/**
 * Expirimental class looking into how a demand responce system my look like.
 * Method names and API use will probably change in refactoring into a propper
 * implementation.
 * 
 * 
 * This class is very simple it turns off as many devices as it can and controls
 * batterys via a mode textbox
 * 
 * 
 * @author robert
 *
 */
public class DRESimpleStrategy {
	private DRESimpleStrategyDatumDataSource settings;
	private Collection<FeedbackInstructionHandler> feedbackInstructionHandlers;

	// status variables for the datum source
	private Integer numdrdevices = 0;

	public DRESimpleStrategyDatumDataSource getSettings() {
		return settings;
	}

	// configured in OSGI
	public void setSettings(DRESimpleStrategyDatumDataSource settings) {
		this.settings = settings;

	}

	protected void drupdate() {
		numdrdevices = 0;
		for (FeedbackInstructionHandler handler : feedbackInstructionHandlers) {
			if (handler.handlesTopic(DRSupportTools.DRPARAMS_INSTRUCTION)) {

				BasicInstruction instr = new BasicInstruction(DRSupportTools.DRPARAMS_INSTRUCTION, new Date(),
						Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);

				// The devices want to know where the instruction came from for
				// verification
				instr.addParameter(settings.getUID(), "");

				Map<String, ?> params = handler.processInstructionWithFeedback(instr).getResultParameters();
				if (DRSupportTools.isDRCapable(params)) {
					numdrdevices++;
					Integer watts = DRSupportTools.readWatts(params);
					Integer minwatts = DRSupportTools.readMinWatts(params);
					Integer maxwatts = DRSupportTools.readMaxWatts(params);
					if (DRSupportTools.isChargeable(params)) {
						String mode = settings.getBatteryMode();
						if (mode.equalsIgnoreCase("DISCHARGE")) {
							// Tell the battery to discharge as much as it can
							sendDRtoBattery(true, handler, maxwatts);
						} else if (mode.equalsIgnoreCase("CHARGE")) {
							// Tell the battery to charge as much as it can. For
							// that we need the max charging watts param
							sendDRtoBattery(false, handler, DRSupportTools.readMaxChargingWatts(params));
						} else {
							// Assume idle tell battery to not discharge and set
							// draw to 0 watts
							// it should be the case for batterys to have
							// minwatts at 0 but check anyways
							if (minwatts.equals(0)) {
								sendDRtoBattery(false, handler, 0);
							}
						}
					} else if (watts > minwatts) {
						Integer shedamount = watts - minwatts;
						// in this strategy we just lower power of all devices
						// as much as we can
						instr = new BasicInstruction(InstructionHandler.TOPIC_SHED_LOAD, new Date(),
								Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);

						instr.addParameter(settings.getUID(), shedamount.toString());
						handler.processInstruction(instr);
					}
				}
			}
		}

	}

	private void sendDRtoBattery(Boolean discharge, FeedbackInstructionHandler handler, Integer wattValue) {
		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, new Date(),
				Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(settings.getUID(), "");
		instr.addParameter(DRSupportTools.WATTS_PARAM, wattValue.toString());
		instr.addParameter(DRSupportTools.DISCHARGING_PARAM, discharge.toString());
		handler.processInstruction(instr);
	}

	public Collection<FeedbackInstructionHandler> getFeedbackInstructionHandlers() {
		return feedbackInstructionHandlers;
	}

	// configured in OSGI
	public void setFeedbackInstructionHandlers(Collection<FeedbackInstructionHandler> feedbackInstructionHandlers) {
		this.feedbackInstructionHandlers = feedbackInstructionHandlers;
	}

	public Integer getNumdrdevices() {
		return numdrdevices;
	}
}
