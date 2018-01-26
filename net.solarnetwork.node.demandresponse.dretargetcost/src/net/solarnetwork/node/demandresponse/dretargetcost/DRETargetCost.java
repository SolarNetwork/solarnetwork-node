package net.solarnetwork.node.demandresponse.dretargetcost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * @author robert
 *
 */
public class DRETargetCost {
	private DRETargetCostDatumDataSource settings;
	private Collection<FeedbackInstructionHandler> feedbackInstructionHandlers;

	// status variables for the datum source
	private Integer numdrdevices = 0;

	public DRETargetCostDatumDataSource getSettings() {
		return settings;
	}

	// configured in OSGI
	public void setSettings(DRETargetCostDatumDataSource settings) {
		this.settings = settings;

	}

	protected void drupdate() {

		List<FeedbackInstructionHandler> drdevices = new ArrayList<FeedbackInstructionHandler>();

		// the reason the mapping should be in String String is because perhapes
		// in the future it could be JSON
		Map<FeedbackInstructionHandler, Map<String, ?>> instructionMap = new HashMap<FeedbackInstructionHandler, Map<String, ?>>();
		for (FeedbackInstructionHandler handler : feedbackInstructionHandlers) {

			if (handler.handlesTopic(DRSupportTools.DRPARAMS_INSTRUCTION)) {

				BasicInstruction instr = new BasicInstruction(DRSupportTools.DRPARAMS_INSTRUCTION, new Date(),
						Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);

				// The devices want to know where the instruction came from for
				// verification
				instr.addParameter(settings.getUID(), "");

				Map<String, ?> test = handler.processInstructionWithFeedback(instr).getResultParameters();
				if (test != null) {

					if (DRSupportTools.isDRCapable(test) && !DRSupportTools.isChargeable(test)) {

						drdevices.add(handler);
						instructionMap.put(handler, test);

					}
				}

			}

		}

		// This value is used by the datumdatasource
		numdrdevices = drdevices.size();

		// energyConsumption is energy used when not discharging
		Integer energyConsumption = 0;

		// energyProduction is energy used when discharging
		Integer energyProduction = 0;
		for (FeedbackInstructionHandler d : drdevices) {
			Map<String, ?> params = instructionMap.get(d);

			Integer wattValue = DRSupportTools.readWatts(params);

			energyConsumption += wattValue;
		}

		Double newPrice = settings.getEnergyCost().doubleValue();

		// need an object array cause I want to relate double with drdevice
		// the reason im not using mapping is multiple drdevices could have the
		// same double and I need to be able to sort the first column
		Object[][] costArray = new Object[drdevices.size()][2];
		for (int i = 0; i < drdevices.size(); i++) {
			FeedbackInstructionHandler d = drdevices.get(i);
			Map<String, ?> params = instructionMap.get(d);

			Integer wattValue = DRSupportTools.readWatts(params);
			Integer costValue = DRSupportTools.readEnergyDepreciationCost(params);

			costArray[i][0] = (costValue + newPrice) * wattValue;
			costArray[i][1] = d;
		}

		// simple sum to find the cost of running all these devices
		Double totalCost = 0.0;
		for (int i = 0; i < costArray.length; i++) {
			totalCost += (Double) costArray[i][0];
		}

		// sorts the first columb which are doubles and keeps the relationship
		// between cost and DRDevice
		Arrays.sort(costArray, new Comparator<Object[]>() {

			@Override
			public int compare(Object[] o1, Object[] o2) {
				Double d1 = (Double) o1[0];
				Double d2 = (Double) o2[0];
				return d1.compareTo(d2);
			}

		});
		// if this is true we need to reduce demand to get costs down
		if (totalCost > settings.getDrtargetCost()) {

			// going from largest cost to smallest cost (this is just a design
			// decision I made)
			for (int i = drdevices.size() - 1; i >= 0; i--) {
				FeedbackInstructionHandler d = (FeedbackInstructionHandler) costArray[i][1];
				Map<String, ?> params = instructionMap.get(d);

				Integer wattValue = DRSupportTools.readWatts(params);
				Integer minValue = DRSupportTools.readMinWatts(params);
				Integer energyCost = DRSupportTools.readEnergyDepreciationCost(params);

				// check if we can reduce consumption
				if (wattValue > minValue) {

					// if we are here we need to reduce
					Double reduceAmount = totalCost - settings.getDrtargetCost();
					Double energyReduction = reduceAmount / (energyCost + settings.getEnergyCost());
					Double appliedenergyReduction = (wattValue - energyReduction > minValue) ? energyReduction
							: wattValue - minValue;

					// Im annoyed by this instruction because it is only reduce
					// and not gain
					sendShedInstruction(d, appliedenergyReduction);

					// we were able to increase to match demand no need for more
					// devices to have DR
					if (energyReduction.equals(appliedenergyReduction)) {
						break;
					} else {
						// update the cost for the next devices to calcuate with
						totalCost -= appliedenergyReduction * (energyCost + settings.getEnergyCost());
					}

				}
			}

			// if true we need to increase demand
		} else if (totalCost < settings.getDrtargetCost()) {
			// this time we start with the cheapest devices (my reasoning is
			// that these devices most likely have room to power on more) so we
			// can acheive nessisary requirements with little number of
			// instructions
			for (int i = 0; i < drdevices.size(); i++) {
				FeedbackInstructionHandler d = (FeedbackInstructionHandler) costArray[i][1];
				Map<String, ?> params = instructionMap.get(d);

				Integer wattValue = DRSupportTools.readWatts(params);
				Integer maxValue = DRSupportTools.readMaxWatts(params);
				Integer energyCost = DRSupportTools.readEnergyDepreciationCost(params);

				if (wattValue < maxValue) {

					// if we are here it is okay to increase usage
					Double increaseAmount = settings.getDrtargetCost() - totalCost;

					// energy increase is the amount of energy we want to
					// increase by
					Double energyIncrease = increaseAmount / (energyCost + settings.getEnergyCost());
					energyIncrease += wattValue;

					// appliedenergyIncrease is the value we are going to send
					// as the maxValue can limit us to how much we can increase
					// by
					Double appliedenergyIncrease = Math.min(energyIncrease, maxValue);
					Double energydelta = appliedenergyIncrease - wattValue;

					setWattageInstruction(d, appliedenergyIncrease);

					// we were able to increase to match demand no need for more
					// devices to have DR
					if (energyIncrease.equals(appliedenergyIncrease)) {
						break;
					} else {
						// update the cost for the next devices to calcuate with
						totalCost += energydelta * (energyCost + settings.getEnergyCost());
					}

				}

			}
		}

	}

	// Instruction to set the wattage parameter on the device it uses the
	// TOPIC_SET_CONTROL_PARAMETER insrtuction
	private void setWattageInstruction(InstructionHandler handler, Double energyLevel) {
		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, new Date(),
				Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter("watts", energyLevel.toString());

		// add as a param the source ID so devices can verify
		instr.addParameter(settings.getUID(), "");

		handler.processInstruction(instr);
	}

	// Instruction to reduce wattage parameter on the device. The only reason Im
	// using this instead of setWattageInstruction is because this instruction
	// already exists
	private void sendShedInstruction(InstructionHandler handler, Double shedamount) {
		BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SHED_LOAD, new Date(),
				Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);

		// the convention I saw used from other classes is that the value of the
		// shedload is from the UID for verification
		instr.addParameter(settings.getUID(), shedamount.toString());
		handler.processInstruction(instr);
	}

	public Collection<FeedbackInstructionHandler> getFeedbackInstructionHandlers() {
		return feedbackInstructionHandlers;
	}

	// configured in OSGI we automatically get a collection of
	// FeedbackInstructionHandlers as they come threw
	public void setFeedbackInstructionHandlers(Collection<FeedbackInstructionHandler> feedbackInstructionHandlers) {
		this.feedbackInstructionHandlers = feedbackInstructionHandlers;
	}

	// This is for the DatumDataSource
	public Integer getNumdrdevices() {
		return numdrdevices;
	}

}
