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
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
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

	private int powerCeilingWatts = 10000;
	private int shedThresholdWatts = 9500;
	private int limitExecutionMonitorSeconds = 60;
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
		final List<SwitchConfig> rules = getConfigs();
		InstructionStatus.InstructionState result = evaulateRules(rules, powerNow.intValue());
		log.debug("Current demand: {}W", powerNow);
		return result;
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
			log.info("Power limit required: current power {}W > threshold {}W", powerNow,
					shedThresholdWatts);
			// TODO: issue ShedLoad instruction
		} else {
			// TODO: find if there any switches we can turn back on
		}
		return result;
	}

	private Integer effectivePowerValue(final long date) {
		final long oldestDate = date - (limitExecutionMonitorSeconds * 1000L);
		double totalPower = 0;
		double totalSeconds = 0;
		EnergyDatum prevDatum = null;
		for ( EnergyDatum d : consumptionSamples ) {
			if ( d.getCreated().getTime() < oldestDate ) {
				break;
			}
			if ( prevDatum != null ) {
				Integer power = getPowerValue(d);
				if ( power != null ) {
					double ds = (d.getCreated().getTime() - prevDatum.getCreated().getTime()) / 1000.0;
					totalPower += (power.doubleValue() * ds);
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
			if ( rule.fallsWithinTimeWindow(now) ) {
				applicable.add(rule);
			}
		}
		Collections.sort(applicable, SwitchConfigPriorityComparator.COMPARATOR);
		return applicable;
	}

	private SwitchInfo mostRecentSwitch() {
		SwitchInfo result = null;
		for ( SwitchInfo info : switchInfos.values() ) {
			if ( result == null || info.getSwitchedDate() != null
					&& info.getSwitchedDate().before(result.getSwitchedDate()) ) {
				result = info;
			}
		}
		return result;
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
		results.add(new BasicTextFieldSettingSpecifier("powerCeilingWatts", String
				.valueOf(defaults.powerCeilingWatts)));
		results.add(new BasicTextFieldSettingSpecifier("shedThresholdWatts", String
				.valueOf(defaults.shedThresholdWatts)));

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

	public void setPowerCeilingWatts(int powerCeilingWatts) {
		this.powerCeilingWatts = powerCeilingWatts;
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

}
