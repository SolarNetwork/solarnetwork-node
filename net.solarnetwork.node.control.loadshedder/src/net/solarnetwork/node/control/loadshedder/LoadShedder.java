/* ==================================================================
 * LoadShedder.java - 24/06/2015 6:55:15 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.loadshedder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.InstructionUtils;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.util.OptionalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * Service to monitor demand (consumption) from a
 * {@link InstructionHandler#TOPIC_SHED_LOAD} instructions to a specific control
 * to limit power draw to below a maximum threshold.
 * 
 * @author matt
 * @version 1.0
 */
public class LoadShedder implements SettingSpecifierProvider, JobService {

	private int shedThresholdWatts = 9500;
	private int powerAverageSampleSeconds = 10;
	private int limitExecutionMonitorSeconds = 60;
	private Collection<NodeControlProvider> switches;
	private OptionalService<DatumDataSource<EnergyDatum>> consumptionDataSource;
	private Collection<InstructionHandler> instructionHandlers = Collections.emptyList();
	private List<SwitchConfig> configs = new ArrayList<SwitchConfig>(4);

	private int consumptionSampleLimit = 10;
	private final Deque<EnergyDatum> consumptionSamples = new ArrayDeque<EnergyDatum>(10);
	private final Map<String, SwitchInfo> switchInfos = new HashMap<String, SwitchInfo>(4);
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Evaluate current demand (consumption) and attempt to shed load as
	 * necessary.
	 */
	public synchronized InstructionStatus.InstructionState evaluatePowerLoad() {
		final long now = System.currentTimeMillis();
		getPowerReading(); // adds to sample buffer
		final Integer powerNow = effectivePowerValue(now);
		if ( powerNow == null ) {
			log.info("No power reading available.");
			return null;
		}
		log.debug("Current effective load: {}W", powerNow);
		final List<SwitchConfig> rules = getConfigs();
		InstructionStatus.InstructionState result = evaulateRules(rules, powerNow.intValue());
		return result;
	}

	private SwitchInfo updateSwitchInfo(String controlId, int watts) {
		SwitchInfo info = switchInfos.get(controlId);
		if ( info == null ) {
			info = new SwitchInfo();
			info.setControlId(controlId);
			switchInfos.put(controlId, info);
		}
		info.setSwitchedDate(new Date());
		info.setWattsBeforeSwitch(watts);
		return info;
	}

	private InstructionState evaulateRules(List<SwitchConfig> rules, final int powerNow) {
		if ( rules == null || rules.size() < 1 ) {
			log.info("No rules defined, no limit placed on power.");
			return null;
		}
		rules = applicableRules(rules);
		if ( rules == null || rules.size() < 1 ) {
			log.info("No applicable rules available, no limit placed on power.");
			return null;
		}
		InstructionState result = null;
		if ( powerNow > shedThresholdWatts ) {
			// find a switch we can actively limit power on
			log.info("Power limit required: current power {}W > threshold {}W", powerNow,
					shedThresholdWatts);
			final int desiredShedAmount = (powerNow - shedThresholdWatts);
			do {
				String controlId = controlIdToExecuteLimit(rules, desiredShedAmount);
				if ( controlId == null ) {
					log.warn("No switch avaialble to shed {}W", desiredShedAmount);
				} else {
					result = shedLoad(controlId, desiredShedAmount);
					if ( InstructionState.Completed == result ) {
						log.info("Switch {} limit executed for {}W", controlId, desiredShedAmount);
						updateSwitchInfo(controlId, powerNow);
					}
				}
			} while ( result == InstructionState.Declined );
		} else {
			// find if there is a switch we can stop limiting power on
			final int desiredReleaseAmount = (powerNow - shedThresholdWatts);

			// reverse the order of the rules, we want to release in reverse order of limit
			Collections.reverse(rules);

			do {
				String controlId = controlIdToRemoveLimit(rules);
				if ( controlId == null ) {
					log.trace("No switches need limit lifted.");
				} else {
					result = removeLoadLimit(controlId, desiredReleaseAmount);
					if ( InstructionState.Completed == result ) {
						log.info("Switch {} limit released for {}W", controlId, desiredReleaseAmount);
						updateSwitchInfo(controlId, powerNow);
					}
				}
			} while ( result == InstructionState.Declined );
		}
		return result;
	}

	private String controlIdToExecuteLimit(List<SwitchConfig> rules, int desiredShedAmount) {
		// we assume rules already sorted by priority here, and filtered to just the applicable ones,
		// so find first rule for a switch that hasn't been switched within limitExecutionMonitorSeconds

		// we don't want to execute a change if ANY switch has been changed within the configured cool down period
		for ( SwitchConfig rule : rules ) {
			String controlId = rule.getControlId();
			SwitchInfo info = switchInfos.get(controlId);
			if ( switchSwitchedTooRecently(info) ) {
				log.debug("Switch {} switched too recently to enforce limit now: {}", controlId,
						info.getSwitchedDate());
				return null;
			}
		}

		for ( SwitchConfig rule : rules ) {
			String controlId = rule.getControlId();
			NodeControlProvider switchControl = switchControlForId(controlId);
			if ( switchControl == null ) {
				log.warn("Switch {} not available, cannot use to limit power.", controlId);
				continue;
			}
			NodeControlInfo controlInfo = switchControl.getCurrentControlInfo(controlId);
			if ( switchIsLimitingPower(controlInfo) ) {
				log.debug("Switch {} already limiting power, cannot use to shed {}W", controlId,
						desiredShedAmount);
			} else {
				log.info("Found switch {} available for executing load shed of {}W", controlId,
						desiredShedAmount);
				return controlId;
			}
		}
		return null;
	}

	private String controlIdToRemoveLimit(List<SwitchConfig> rules) {
		// we assume rules already sorted by priority here, and filtered to just the applicable ones,
		// so find first rule for a switch that hasn't been switched within limitExecutionMonitorSeconds

		// we don't want to execute a change if ANY switch has been changed within the configured cool down period
		for ( SwitchConfig rule : rules ) {
			String controlId = rule.getControlId();
			SwitchInfo info = switchInfos.get(controlId);
			if ( switchSwitchedTooRecently(info) ) {
				log.debug("Switch {} switched too recently to release any limit now: {}", controlId,
						info.getSwitchedDate());
				return null;
			}
		}

		for ( SwitchConfig rule : rules ) {
			String controlId = rule.getControlId();
			NodeControlProvider switchControl = switchControlForId(controlId);
			if ( switchControl == null ) {
				log.warn("Switch {} not available, cannot use to limit power.", controlId);
				continue;
			}
			NodeControlInfo controlInfo = switchControl.getCurrentControlInfo(controlId);
			if ( switchIsLimitingPower(controlInfo) ) {
				log.info("Found switch {} available for removing load shed limit", controlId);
				return controlId;
			} else {
				log.debug("Switch {} already not limiting power, cannot use to remove limit", controlId);
			}
		}
		return null;
	}

	private boolean switchSwitchedTooRecently(SwitchInfo info) {
		if ( info != null
				&& info.getSwitchedDate().getTime() + limitExecutionMonitorSeconds * 1000L > System
						.currentTimeMillis() ) {
			return true;
		}
		return false;
	}

	private boolean switchIsLimitingPower(NodeControlInfo controlInfo) {
		final String value = controlInfo.getValue();
		switch (controlInfo.getType()) {
			case Boolean:
				// TRUE means actively limiting, FALSE means NOT limiting
				if ( value != null
						&& (value.equals("1") || value.equalsIgnoreCase("yes") || value
								.equalsIgnoreCase("true")) ) {
					return true;
				}
				break;

			default:
				// for now, other types are not supported
				log.warn("Switch {} data type {} not supported, cannot use to limit power",
						controlInfo.getControlId(), controlInfo.getType());
				break;
		}
		return false;
	}

	private NodeControlProvider switchControlForId(final String controlId) {
		Collection<NodeControlProvider> providers = switches;
		if ( providers == null ) {
			return null;
		}
		for ( NodeControlProvider p : providers ) {
			List<String> ids = p.getAvailableControlIds();
			if ( ids != null && ids.contains(controlId) ) {
				return p;
			}
		}
		return null;
	}

	private InstructionStatus.InstructionState removeLoadLimit(final String controlId,
			final int desiredAmountInWatts) {
		assert desiredAmountInWatts < 1;
		return shedLoad(controlId, desiredAmountInWatts);
	}

	private InstructionStatus.InstructionState shedLoad(final String controlId,
			final int desiredAmountInWatts) {
		final BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SHED_LOAD,
				new Date(), Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(controlId, String.valueOf(desiredAmountInWatts));
		final InstructionStatus.InstructionState result = InstructionUtils.handleInstruction(
				instructionHandlers, instr);
		return (result == null ? InstructionStatus.InstructionState.Declined : result);
	}

	private Integer effectivePowerValue(final long date) {
		final long oldestDate = date - (powerAverageSampleSeconds * 1000L);
		double totalPower = 0;
		double totalSeconds = 0;
		EnergyDatum prevDatum = null;
		for ( EnergyDatum d : consumptionSamples ) {
			if ( d.getCreated().getTime() < oldestDate ) {
				break;
			}
			if ( prevDatum != null ) {
				Integer power = getPowerValue(d);
				Integer prevPower = getPowerValue(prevDatum);
				if ( power != null && prevPower != null ) {
					double ds = (prevDatum.getCreated().getTime() - d.getCreated().getTime()) / 1000.0;
					totalPower += (power.doubleValue() + prevPower.doubleValue()) * 0.5 * ds;
					totalSeconds += ds;
				}
			}
			prevDatum = d;
		}
		if ( totalSeconds < 1.0 ) {
			// at most 1 sample
			return (prevDatum != null ? prevDatum.getWatts() : null);
		}
		return (int) Math.round(totalPower / totalSeconds);
	}

	/**
	 * Get a list of rules that satisfy all constraints based on the current
	 * time, sorted by priority.
	 * 
	 * @param rules
	 *        The rules to filter.
	 * @return The applicable rules.
	 */
	private List<SwitchConfig> applicableRules(List<SwitchConfig> rules) {
		if ( rules == null ) {
			return null;
		}
		List<SwitchConfig> applicable = new ArrayList<SwitchConfig>(rules.size());
		final long now = System.currentTimeMillis();
		for ( SwitchConfig rule : rules ) {
			if ( rule.getActive() != null && rule.getActive().booleanValue() == false ) {
				continue;
			}
			if ( rule.fallsWithinTimeWindow(now) ) {
				applicable.add(rule);
			}
		}
		Collections.sort(applicable, SwitchConfigPriorityComparator.COMPARATOR);
		return applicable;
	}

	@Override
	public void executeJobService() {
		evaluatePowerLoad();
	}

	// Datum support

	private EnergyDatum getPowerReading() {
		OptionalService<DatumDataSource<EnergyDatum>> service = consumptionDataSource;
		if ( service == null ) {
			return null;
		}
		DatumDataSource<EnergyDatum> dataSource = service.service();
		if ( dataSource == null ) {
			return null;
		}
		EnergyDatum datum = dataSource.readCurrentDatum();

		// maintain a buffer of samples so we can monitor the effect of limit operations
		// buffer ordered from most recent to oldest

		if ( consumptionSamples.size() >= consumptionSampleLimit ) {
			consumptionSamples.removeLast();
		}
		consumptionSamples.addFirst(datum);

		return datum;
	}

	private Integer getPowerValue(EnergyDatum datum) {
		if ( datum == null ) {
			return null;
		}
		return datum.getWatts();
	}

	// Settings support

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.loadshedder";
	}

	@Override
	public String getDisplayName() {
		return "Load Shedder";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		LoadShedder defaults = new LoadShedder();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.add(new BasicTextFieldSettingSpecifier("consumptionDataSource.propertyFilters['UID']",
				"Main"));
		results.add(new BasicTextFieldSettingSpecifier("shedThresholdWatts", String
				.valueOf(defaults.shedThresholdWatts)));
		results.add(new BasicTextFieldSettingSpecifier("powerAverageSampleSeconds", String
				.valueOf(defaults.powerAverageSampleSeconds)));
		results.add(new BasicTextFieldSettingSpecifier("limitExecutionMonitorSeconds", String
				.valueOf(defaults.limitExecutionMonitorSeconds)));

		// dynamic list of configs
		Collection<SwitchConfig> configList = getConfigs();
		BasicGroupSettingSpecifier listComplexGroup = SettingsUtil.dynamicListSettingSpecifier(
				"configs", configList, new SettingsUtil.KeyedListCallback<SwitchConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(SwitchConfig value, int index,
							String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(value
								.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				});
		results.add(listComplexGroup);

		return results;
	}

	// Accessors

	public OptionalService<DatumDataSource<EnergyDatum>> getConsumptionDataSource() {
		return consumptionDataSource;
	}

	public void setConsumptionDataSource(
			OptionalService<DatumDataSource<EnergyDatum>> consumptionDataSource) {
		this.consumptionDataSource = consumptionDataSource;
	}

	public void setInstructionHandlers(Collection<InstructionHandler> instructionHandlers) {
		this.instructionHandlers = instructionHandlers;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setShedThresholdWatts(int shedThresholdWatts) {
		this.shedThresholdWatts = shedThresholdWatts;
	}

	public List<SwitchConfig> getConfigs() {
		return configs;
	}

	public void setConfigs(List<SwitchConfig> configs) {
		this.configs = configs;
	}

	/**
	 * Get the number of configured {@code configs} elements.
	 * 
	 * @return The number of {@code configs} elements.
	 */
	public int getConfigsCount() {
		List<SwitchConfig> l = getConfigs();
		return (l == null ? 0 : l.size());
	}

	/**
	 * Adjust the number of configured {@code configs} elements.
	 * 
	 * @param count
	 *        The desired number of {@code configs} elements.
	 */
	public void setConfigsCount(int count) {
		if ( count < 0 ) {
			count = 0;
		}
		List<SwitchConfig> l = getConfigs();
		if ( l == null ) {
			l = new ArrayList<SwitchConfig>(count);
			setConfigs(l);
		}
		int lCount = l.size();
		while ( lCount > count ) {
			l.remove(l.size() - 1);
			lCount--;
		}
		while ( lCount < count ) {
			l.add(new SwitchConfig());
			lCount++;
		}
	}

	public void setLimitExecutionMonitorSeconds(int limitExecutionMonitorSeconds) {
		this.limitExecutionMonitorSeconds = limitExecutionMonitorSeconds;
	}

	public void setConsumptionSampleLimit(int consumptionSampleLimit) {
		this.consumptionSampleLimit = consumptionSampleLimit;
	}

	public void setSwitches(Collection<NodeControlProvider> switches) {
		this.switches = switches;
	}

	public void setPowerAverageSampleSeconds(int powerAverageSampleSeconds) {
		this.powerAverageSampleSeconds = powerAverageSampleSeconds;
	}

}
