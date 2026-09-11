package net.solarnetwork.node.demandresponse.dretargetcost;

import java.util.Map;

/**
 * Helper class to make use of demand response. It is expected that one uses the
 * constants and methods provided in this class to help you with demand response
 * implementation.
 * 
 * Please have a good read of the documentation in this class before using
 * demand response. For examples of classes using this form of demand response
 * see the following packages
 * {@link net.solarnetwork.node.demandresponse.mockbattery}
 * {@link net.solarnetwork.node.demandresponse.dretargetcost}
 * 
 * There are two main concepts for demand response the DR device and the DR
 * engine. The device is meant to be physical hardware while the engine is an
 * algorithm providing instructions to the device.
 * 
 * DR Instructions use the pre-existing Instructions interface. Demand response
 * requires two way communication meaning DR Devices must implement the
 * {@link net.solarnetwork.node.reactor.FeedbackInstructionHandler} interface
 * and must handle the {@value #DRPARAMS_INSTRUCTION} instruction.
 * 
 * There is no set standard for how a DR Engine gets a hold of DR Devices the
 * strategy I have been using is to configure OSGI a reference list of
 * FeedbackInstructionHandlers.
 * 
 * A DR Device should only accept demand response instructions from one DREngine
 * this is done by checking that the source id of the DR Engine is present in
 * every demand response instruction parameters. The value of the parameter does
 * not have to be anything (except for InstructionHandler.TOPIC_SHED_LOAD) it
 * just must be non null. By convention I have been using empty string.
 * 
 * The procedure for demand response is as follows. The DR Engine sends to its
 * devices a DRPARAMS_INSTRUCTION those devices respond by sending back a map of
 * parameters. The map is of datatype <String,?> due to implementation of
 * FeedbackInstructionHandler. However the parameters should all come as
 * <String,String>. The following parameters are expected in the responding map
 * in order for a device to claim it supports DRPARAMS_INSTRUCTION.
 * 
 * WATTS_PARAM = The current power in watts the device is operating at. This
 * value should always be positive.
 * 
 * MAXWATTS_PARAM = The maximum amount of power the device can run at. For
 * chargeable devices this parameter is used for the maximum discharge rate.
 * 
 * MINWATTS_PARAM = The minimum amount of power the device can run at. This
 * parameter does not represent a physical constraint of the device. Instead
 * just tells the DR Engine that this device is too important to be powered
 * below this specific level.
 * 
 * ENERGY_DEPRECIATION = The cost of running this device per watt hour. This
 * cost can reflect a resource consumption or simply deprecation from use.
 * 
 * DRREADY_PARAM = setting this parameter to "true" tells the DREngine that it
 * can expect all of these above parameters in this map. Set it to "false" when
 * the device is not ready to handle demand response at that time.
 * 
 * CHARGEABLE_PARAM = Set this to "true" if your device support demand response
 * of charging and discharging other set to "false"
 * 
 * If CHARGEABLE_PARAM was set to true the next parameter is required otherwise
 * this one can be ignored
 * 
 * DISCHARGING_PARAM = Set to "true" if the device is discharging otherwise
 * "false". Not being powered is seen as not discharging.
 * 
 * MAXCHARGEWATTS_PARAM = Should only be present when device is chargeable the
 * value of this parameter is the maximum number of watt hours the chargeable
 * device can charge at. This is different to MAXWATTS_PARAM which is the
 * maximum discharge rate.
 * 
 * CHARGE_PERCENTAGE_PARAM = Should only be present when device is chargeable
 * the value of this parameter should be a number between 0-100 representing the
 * percentage of remaining capacity.
 * 
 * MAXCHARGE_PARAM = Should only be present when device is chargeable. The
 * number of watt hours of charge the device will have when fully charged.
 * 
 * NOTE do not assume that any of these parameters are constant eg
 * MAXWATTS_PARAM could change depending on environmental factors. You should
 * query for these parameters every time you want to calculate a demand
 * response.
 * 
 * 
 * To send a demand response you have the choice of sending
 * InstructionHandler.TOPIC_SHED_LOAD or
 * InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.
 * 
 * When sending a TOPIC_SHED_LOAD instruction you map the DR Engine's sourceID
 * to the shedamount as per the convention already in place by this instruction.
 * The DR Device should reduce its consumption by the shedamount so long as it
 * does not conflict with its minimum wattage. It is up to your implemention on
 * how to handle this instruction when the parameters are invalid. You may
 * choose to make assumptions and error correct or decline the instruction in
 * your instruction status.
 * 
 * When sending a TOPIC_SET_CONTROL_PARAMETER instruction remember to include
 * the source ID of the DR Engine in the parameters. The parameters that are
 * allowed to be changed with this instruction are WATTS_PARAM and if
 * CHARGEABLE_PARAM was true then DISCHARGING_PARAM can be configured in this
 * instruction. You are allow to configure multiple parameters in a single
 * instruction. map the parameter you want changed to the value you want set.
 * 
 * @author robert
 *
 */
public class DRSupportTools {

	// The name of the instruction a DREngine sends to a DRDevice. The DRDevice
	// should respond with a map of parameters
	public static final String DRPARAMS_INSTRUCTION = "getDRDeviceInstance";

	// Parameters that must be in response to a DRPARAMS_INSTRUCTION
	public static final String WATTS_PARAM = "watts";
	public static final String MAXWATTS_PARAM = "maxwatts";
	public static final String MINWATTS_PARAM = "minwatts";
	public static final String ENERGY_DEPRECIATION = "energycost";
	public static final String DRREADY_PARAM = "drready";
	public static final String CHARGEABLE_PARAM = "chargeable";
	public static final String SOURCEID_PARAM = "sourceID";

	// These parameters are required if CHARGEABLE_PARAM is true
	public static final String DISCHARGING_PARAM = "isDischarging";
	public static final String MAXCHARGEWATTS_PARAM = "chargePower";
	public static final String CHARGE_PERCENTAGE_PARAM = "chargePercent";
	public static final String MAXCHARGE_PARAM = "maxcharge";

	public static String readSourceID(Map<String, ?> params) {
		try {
			return (String) params.get(SOURCEID_PARAM);
		} catch (ClassCastException e) {
			return null;
		}

	}

	/**
	 * Exception for when trying to access a parameter for chargeable device
	 * when CHARGEABLE_PARAM is false
	 * 
	 * @author robert
	 *
	 */
	public static class NotChargeableDeviceException extends RuntimeException {

		private static final long serialVersionUID = 1L;

	}

	/**
	 * Reads the wattage reading from the parameters. If there is no wattage
	 * parameter the method returns null.
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return The wattage reading of the device
	 */
	public static Integer readWatts(Map<String, ?> params) {
		return readValue(WATTS_PARAM, params);
	}

	/**
	 * Reads the max watts reading from the parameters. If no wattage reading is
	 * present the method returns null.
	 * 
	 * A max wattage reading is the maximum amount of watts the device can
	 * operate on.
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return The wattage reading of the device
	 */
	public static Integer readMaxWatts(Map<String, ?> params) {
		return readValue(MAXWATTS_PARAM, params);
	}

	/**
	 * Reads the maximum watts a chargeable device can charge at.
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return
	 */
	public static Integer readMaxChargingWatts(Map<String, ?> params) {
		if (!isChargeable(params)) {
			throw new NotChargeableDeviceException();
		}
		return readValue(MAXCHARGEWATTS_PARAM, params);
	}

	/**
	 * Reads a number from 0-100 representing the charge percentage of the
	 * chargeable device. Returns null if parameter is missing.
	 * 
	 * throws NotChargeableDeviceException if method is called when device is
	 * not a chargeable device
	 * 
	 * throws NumberFormatException if the read value is not between (inclusive)
	 * 0 and 100
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return
	 */
	public static Integer readChargePercent(Map<String, ?> params) {
		if (!isChargeable(params)) {
			throw new NotChargeableDeviceException();
		}
		Integer value = readValue(CHARGE_PERCENTAGE_PARAM, params);
		if (value != null && value < 0 || value > 100) {
			throw new NumberFormatException();
		}
		return readValue(CHARGE_PERCENTAGE_PARAM, params);
	}

	/**
	 * Reads the number of watt hours this chargeable device has at maximum
	 * charge.
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return
	 */
	public static Integer readMaxCharge(Map<String, ?> params) {
		if (!isChargeable(params)) {
			throw new NotChargeableDeviceException();
		}
		return readValue(MAXCHARGE_PARAM, params);
	}

	/**
	 * Reads the min watts reading from the paramters. If no wattage reading is
	 * present the method returns null.
	 * 
	 * A min wattage reading is the minimum amount of the watts the device can
	 * operate on.
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return
	 */
	public static Integer readMinWatts(Map<String, ?> params) {
		return readValue(MINWATTS_PARAM, params);
	}

	/**
	 * Reads the energy cost from the parameters. If no energy cost reading is
	 * present the method returns null.
	 * 
	 * The energy cost is the price per watt to operate the device.
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return
	 */
	public static Integer readEnergyDepreciationCost(Map<String, ?> params) {
		return readValue(ENERGY_DEPRECIATION, params);
	}

	/**
	 * Checks whether the parameters specify whether the device can handle
	 * demand size response.
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return
	 */
	public static boolean isDRCapable(Map<String, ?> params) {
		return readBoolean(DRREADY_PARAM, params);
	}

	/**
	 * Checks the parameters to see if the type of device supports charging for
	 * example a battery
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return
	 */
	public static boolean isChargeable(Map<String, ?> params) {
		return readBoolean(CHARGEABLE_PARAM, params);
	}

	/**
	 * Checks the parameters to see if the device is discharging. Should only be
	 * called after verifying the device is chargeable
	 * 
	 * 
	 * @param params
	 *            The parameters of the device
	 * @return
	 */
	public static boolean isDischarging(Map<String, ?> params) {
		if (!isChargeable(params)) {
			throw new NotChargeableDeviceException();
		}
		return readBoolean(DISCHARGING_PARAM, params);
	}

	// TODO fix behaviour
	private static boolean readBoolean(String paramname, Map<String, ?> params) {
		// cast to string then check for boolean
		// just realised this does not check for nulls or non strings
		return Boolean.parseBoolean((String) params.get(paramname));
	}

	private static Integer readValue(String paramname, Map<String, ?> params) {
		// set variable to null to avoid uninitalised warnings
		Integer result = null;
		try {
			result = (int) Double.parseDouble((String) params.get(paramname));
		} finally {
			// Do nothing, in the case there was an exception the returned value
			// will be null
		}
		return result;
	}
}
