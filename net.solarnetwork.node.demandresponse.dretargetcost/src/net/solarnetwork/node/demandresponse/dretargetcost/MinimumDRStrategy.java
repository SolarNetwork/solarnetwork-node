package net.solarnetwork.node.demandresponse.dretargetcost;

import java.util.Date;
import java.util.List;
import java.util.Map;

import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.support.BasicInstruction;

/**
 * another DRStrategy this one just powers off as much as it can. Using this to
 * test being able to change strategys
 * 
 * @author robert
 *
 */
public class MinimumDRStrategy {
	private List<FeedbackInstructionHandler> handlers;

	public void drupdate() {
		for (FeedbackInstructionHandler handler : handlers) {
			if (handler.handlesTopic("getDRDeviceInstance")) {

				BasicInstruction instr = new BasicInstruction("getDRDeviceInstance", new Date(),
						Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);

				// The devices want to know where the instruction came from for
				// verification
				// TODO work on verification
				// instr.addParameter(settings.getUID(), "");

				Map<String, ?> params = handler.processInstructionWithFeedback(instr).getResultParameters();
				if (DRSupportTools.isDRCapable(params)) {
					Integer watts = DRSupportTools.readWatts(params);
					Integer minwatts = DRSupportTools.readMinWatts(params);
					if (watts > minwatts) {
						Integer shedamount = watts - minwatts;
						// in this strategy we just lower power of all devices
						// as much as we can
						instr = new BasicInstruction(InstructionHandler.TOPIC_SHED_LOAD, new Date(),
								Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
						// TODO figure out verification
						instr.addParameter("temp", shedamount.toString());
						handler.processInstruction(instr);
					}
				}
			}
		}
	}

	public void setHandlers(List<FeedbackInstructionHandler> handlers) {
		this.handlers = handlers;

	}
}
