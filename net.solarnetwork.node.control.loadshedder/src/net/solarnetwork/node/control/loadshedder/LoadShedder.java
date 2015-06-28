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
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.InstructionUtils;
import net.solarnetwork.node.settings.MappableSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StaticOptionalService;
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

	private OptionalService<LoadShedderStrategy> shedStrategy = new StaticOptionalService<LoadShedderStrategy>(
			new DefaultLoadShedderStrategy());
	private OptionalService<DatumDataSource<EnergyDatum>> consumptionDataSource;
	private Collection<InstructionHandler> instructionHandlers = Collections.emptyList();
	private List<LoadShedControlConfig> configs = new ArrayList<LoadShedControlConfig>(4);

	private int consumptionSampleLimit = 10;
	private final Deque<EnergyDatum> consumptionSamples = new ArrayDeque<EnergyDatum>(10);
	private final Map<String, LoadShedControlInfo> switchInfos = new HashMap<String, LoadShedControlInfo>(
			4);
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Evaluate current demand (consumption) and attempt to shed load as
	 * necessary.
	 */
	public synchronized InstructionStatus.InstructionState evaluatePowerLoad() {
		final long now = System.currentTimeMillis();
		addPowerSample(consumptionDataSource, consumptionSamples); // adds to sample buffer
		LoadShedderStrategy strategy = getStrategy();
		if ( strategy == null ) {
			log.warn("No LoadShedderStrategy service avaialble");
		}
		Deque<EnergyDatum> samples = consumptionSamples;
		Collection<LoadShedAction> actions = strategy.evaulateRules(getConfigs(), switchInfos, now,
				samples);
		if ( actions == null || actions.size() < 1 ) {
			return null;
		}
		InstructionStatus.InstructionState result = null;
		for ( LoadShedAction action : actions ) {
			final BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_SHED_LOAD,
					new Date(now), Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID,
					null);
			instr.addParameter(action.getControlId(), action.getShedWatts().toString());
			result = InstructionUtils.handleInstruction(instructionHandlers, instr);
			if ( result == InstructionStatus.InstructionState.Completed ) {
				log.info("Switch {} limit released for {}W", action.getControlId(),
						action.getShedWatts());
				updateSwitchInfo(action.getControlId(), action, samples.peek());
			}
		}
		return (result == null ? InstructionStatus.InstructionState.Declined : result);
	}

	private LoadShedControlInfo updateSwitchInfo(String controlId, LoadShedAction action,
			EnergyDatum sample) {
		LoadShedControlInfo info = switchInfos.get(controlId);
		if ( info == null ) {
			info = new LoadShedControlInfo();
			info.setControlId(controlId);
			switchInfos.put(controlId, info);
		}
		info.setActionDate(new Date());
		if ( sample != null ) {
			info.setWattsBeforeAction(sample.getWatts());
		}
		info.setAction(action);
		return info;
	}

	@Override
	public void executeJobService() {
		evaluatePowerLoad();
	}

	public LoadShedderStrategy getStrategy() {
		if ( shedStrategy == null ) {
			return null;
		}
		return shedStrategy.service();
	}

	// Datum support

	private EnergyDatum addPowerSample(OptionalService<DatumDataSource<EnergyDatum>> service,
			Deque<EnergyDatum> samples) {
		if ( service == null ) {
			return null;
		}
		DatumDataSource<EnergyDatum> dataSource = service.service();
		if ( dataSource == null ) {
			return null;
		}
		EnergyDatum datum = dataSource.readCurrentDatum();
		if ( datum == null ) {
			return null;
		}

		// maintain a buffer of samples so we can monitor the effect of limit operations
		// buffer ordered from most recent to oldest

		EnergyDatum previous = samples.peek();
		if ( previous != null && previous.getCreated().equals(datum.getCreated()) ) {
			// sample unchanged
			return previous;
		}

		if ( samples.size() >= consumptionSampleLimit ) {
			samples.removeLast();
		}
		samples.addFirst(datum);

		return datum;
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
		LoadShedderStrategy strat = getStrategy();
		MessageSource stratMessageSource = null;
		if ( strat instanceof SettingSpecifierProvider ) {
			stratMessageSource = ((SettingSpecifierProvider) strat).getMessageSource();
		}
		if ( stratMessageSource == null ) {
			return messageSource;
		}
		PrefixedMessageSource ms = new PrefixedMessageSource();
		ms.setPrefix("strategy.");
		ms.setDelegate(stratMessageSource);
		ms.setParentMessageSource(messageSource);
		return ms;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.add(new BasicTextFieldSettingSpecifier("shedStrategy.propertyFilters['UID']", "Default"));
		results.add(new BasicTextFieldSettingSpecifier("consumptionDataSource.propertyFilters['UID']",
				"Main"));

		LoadShedderStrategy strategy = getStrategy();
		if ( strategy instanceof SettingSpecifierProvider ) {
			SettingSpecifierProvider stratSettingProvider = (SettingSpecifierProvider) strategy;
			List<SettingSpecifier> strategySpecifiers = stratSettingProvider.getSettingSpecifiers();
			if ( strategySpecifiers != null && strategySpecifiers.size() > 0 ) {
				for ( SettingSpecifier spec : strategySpecifiers ) {
					if ( spec instanceof MappableSpecifier ) {
						results.add(((MappableSpecifier) spec).mappedTo("strategy."));
					} else {
						results.add(spec);
					}
				}
			}
		}

		// dynamic list of configs
		Collection<LoadShedControlConfig> configList = getConfigs();
		BasicGroupSettingSpecifier listComplexGroup = SettingsUtil.dynamicListSettingSpecifier(
				"configs", configList, new SettingsUtil.KeyedListCallback<LoadShedControlConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(LoadShedControlConfig value,
							int index, String key) {
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

	public List<LoadShedControlConfig> getConfigs() {
		return configs;
	}

	public void setConfigs(List<LoadShedControlConfig> configs) {
		this.configs = configs;
	}

	/**
	 * Get the number of configured {@code configs} elements.
	 * 
	 * @return The number of {@code configs} elements.
	 */
	public int getConfigsCount() {
		List<LoadShedControlConfig> l = getConfigs();
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
		List<LoadShedControlConfig> l = getConfigs();
		if ( l == null ) {
			l = new ArrayList<LoadShedControlConfig>(count);
			setConfigs(l);
		}
		int lCount = l.size();
		while ( lCount > count ) {
			l.remove(l.size() - 1);
			lCount--;
		}
		while ( lCount < count ) {
			l.add(new LoadShedControlConfig());
			lCount++;
		}
	}

	public void setConsumptionSampleLimit(int consumptionSampleLimit) {
		this.consumptionSampleLimit = consumptionSampleLimit;
	}

	public OptionalService<LoadShedderStrategy> getShedStrategy() {
		return shedStrategy;
	}

	public void setShedStrategy(OptionalService<LoadShedderStrategy> shedStrategy) {
		this.shedStrategy = shedStrategy;
	}

}
