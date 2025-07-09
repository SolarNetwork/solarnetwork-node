/* ==================================================================
 * DemandBalancer.java - Mar 23, 2014 3:58:26 PM
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

package net.solarnetwork.node.control.demandbalancer;

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.EnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleEnergyDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.OptionalServiceCollection.OptionalFilterableServiceCollection;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Basic service to monitor demand conditions (consumption) and generation
 * (power) and send out {@link InstructionHandler#TOPIC_DEMAND_BALANCE}
 * instructions to a specific control to limit generation to an amount that
 * keeps generation at or below current consumption levels.
 *
 * @author matt
 * @version 2.1
 */
public class DemandBalancer implements SettingSpecifierProvider {

	private static final String ERROR_NO_DATA_RETURNED = "No data returned.";

	/**
	 * The EventAdmin topic used to post events with statistics on balance
	 * execution.
	 */
	public static final String EVENT_TOPIC_STATISTICS = "net/solarnetwork/node/control/demandbalancer/DemandBalancer/STATISTICS";

	/** The last consumption collection date stat key. */
	public static final String STAT_LAST_CONSUMPTION_COLLECTION_DATE = "ConsumptionCollectionDate";

	/** The last consumption collection error stat key. */
	public static final String STAT_LAST_CONSUMPTION_COLLECTION_ERROR = "ConsumptionCollectionError";

	/** The last power collection date stat key. */
	public static final String STAT_LAST_POWER_COLLECTION_DATE = "PowerCollectionDate";

	/** The last power collection error stat key. */
	public static final String STAT_LAST_POWER_COLLECTION_ERROR = "PowerCollectionError";

	/** The last power control collection date stat key. */
	public static final String STAT_LAST_POWER_CONTROL_COLLECTION_DATE = "PowerControlCollectionDate";

	/** The last power control collection date stat key. */
	public static final String STAT_LAST_POWER_CONTROL_COLLECTION_ERROR = "PowerControlCollectionError";

	/** The last power control modify date stat key. */
	public static final String STAT_LAST_POWER_CONTROL_MODIFY_DATE = "PowerControlModifyDate";

	/** The last power control modify error stat key. */
	public static final String STAT_LAST_POWER_CONTROL_MODIFY_ERROR = "PowerControlModifyError";

	/** The {@code collectPower} property default value. */
	public static final boolean DEFAULT_COLLECT_POWER = false;

	/** The {@code powerMaximumWatts} property default value. */
	public static final int DEFAULT_POWER_MAXIMUM_WATTS = 1000;

	private final OptionalService<InstructionExecutionService> instructionExecutionService;
	private String powerControlId;
	private OptionalService<EventAdmin> eventAdmin;
	private OptionalFilterableService<NodeControlProvider> powerControl;
	private OptionalFilterableServiceCollection<DatumDataSource> powerDataSource;
	private int powerMaximumWatts = DEFAULT_POWER_MAXIMUM_WATTS;
	private OptionalFilterableServiceCollection<DatumDataSource> consumptionDataSource;
	private OptionalFilterableService<DemandBalanceStrategy> balanceStrategy = new StaticOptionalService<>(
			new SimpleDemandBalanceStrategy());
	private Collection<InstructionHandler> instructionHandlers = Collections.emptyList();
	private MessageSource messageSource;
	private boolean collectPower = DEFAULT_COLLECT_POWER;
	private Set<AcPhase> acEnergyPhaseFilter;

	final Map<String, Object> stats = new LinkedHashMap<>(8);

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 *
	 * @param instructionExecutionService
	 *        the service to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DemandBalancer(OptionalService<InstructionExecutionService> instructionExecutionService) {
		super();
		if ( instructionExecutionService == null ) {
			throw new IllegalArgumentException(
					"The instructionExecutionService argument must not be null.");
		}
		this.instructionExecutionService = instructionExecutionService;
	}

	/**
	 * Evaluate current demand (consumption) and generation (power) and attempt
	 * to maximize power generation up to the current demand level.
	 */
	public void evaluateBalance() {
		final Integer demandWatts = collectDemandWatts();
		final Integer generationWatts = collectGenerationWatts();
		final Integer generationLimitPercent = readCurrentGenerationLimitPercent();
		log.debug("Current demand: {}, generation: {}, capacity: {}, limit: {}",
				(demandWatts == null ? "N/A" : demandWatts.toString()),
				(generationWatts == null ? "N/A" : generationWatts.toString()), powerMaximumWatts,
				(generationLimitPercent == null ? "N/A" : generationLimitPercent + "%"));
		executeDemandBalanceStrategy(demandWatts, generationWatts, generationLimitPercent);
		postStatisticsEvent();
	}

	/**
	 * Get a message for an exception. This will try to return the root cause's
	 * message. If that is not available the name of the root cause's class will
	 * be returned.
	 *
	 * @param t
	 *        the exception
	 * @return message
	 */
	private String messageForException(Throwable t) {
		Throwable root = t;
		while ( root.getCause() != null ) {
			root = root.getCause();
		}
		String msg = root.getMessage();
		if ( msg == null || msg.length() < 1 ) {
			msg = t.getMessage();
			if ( msg == null || msg.length() < 1 ) {
				msg = root.getClass().getName();
			}
		}
		return msg;
	}

	private Integer collectDemandWatts() {
		log.debug("Collecting current consumption data to inform demand balancer...");
		Iterable<EnergyDatum> demand = null;
		try {
			demand = getCurrentDatum(consumptionDataSource);
			if ( demand.iterator().hasNext() ) {
				stats.put(STAT_LAST_CONSUMPTION_COLLECTION_DATE, System.currentTimeMillis());
				stats.remove(STAT_LAST_CONSUMPTION_COLLECTION_ERROR);
			} else {
				stats.put(STAT_LAST_CONSUMPTION_COLLECTION_ERROR, ERROR_NO_DATA_RETURNED);
			}
		} catch ( RuntimeException e ) {
			log.error("Error collecting consumption data: {}", e.getMessage());
			stats.put(STAT_LAST_CONSUMPTION_COLLECTION_ERROR, messageForException(e));
		}
		return wattsForEnergyDatum(demand);
	}

	private Integer collectGenerationWatts() {
		final Integer generationWatts;
		if ( collectPower ) {
			log.debug("Collecting current generation data to inform demand balancer...");
			Iterable<EnergyDatum> generation = null;
			try {
				generation = getCurrentDatum(powerDataSource);
				if ( generation.iterator().hasNext() ) {
					stats.put(STAT_LAST_POWER_COLLECTION_DATE, System.currentTimeMillis());
					stats.remove(STAT_LAST_POWER_COLLECTION_ERROR);
				} else {
					stats.put(STAT_LAST_POWER_COLLECTION_ERROR, ERROR_NO_DATA_RETURNED);
				}
			} catch ( RuntimeException e ) {
				log.error("Error collecting generation data: {}", e.getMessage());
				stats.put(STAT_LAST_POWER_COLLECTION_ERROR, messageForException(e));
			}
			generationWatts = wattsForEnergyDatum(generation);
		} else {
			generationWatts = null;
		}
		return generationWatts;
	}

	private Integer readCurrentGenerationLimitPercent() {
		log.debug("Reading current {} value to inform demand balancer...", powerControlId);
		NodeControlInfo generationLimit = null;
		try {
			generationLimit = getCurrentControlValue(powerControl, powerControlId);
			if ( generationLimit != null ) {
				stats.put(STAT_LAST_POWER_CONTROL_COLLECTION_DATE, System.currentTimeMillis());
				stats.remove(STAT_LAST_POWER_CONTROL_COLLECTION_ERROR);
			} else {
				stats.put(STAT_LAST_POWER_CONTROL_COLLECTION_ERROR, ERROR_NO_DATA_RETURNED);
			}
		} catch ( RuntimeException e ) {
			log.error("Error collecting {} data: {}", powerControlId, e.getMessage());
			stats.put(STAT_LAST_POWER_CONTROL_COLLECTION_ERROR, messageForException(e));
		}
		return percentForLimit(generationLimit);
	}

	private void executeDemandBalanceStrategy(final Integer demandWatts, final Integer generationWatts,
			final Integer generationLimitPercent) {
		try {
			InstructionStatus.InstructionState result = evaluateBalance(
					(demandWatts == null ? -1 : demandWatts.intValue()),
					(generationWatts == null ? -1 : generationWatts.intValue()),
					(generationLimitPercent == null ? -1 : generationLimitPercent.intValue()));
			if ( result != null ) {
				stats.put(STAT_LAST_POWER_CONTROL_MODIFY_DATE, System.currentTimeMillis());
			}
			if ( result == null || result == InstructionStatus.InstructionState.Completed ) {
				stats.remove(STAT_LAST_POWER_CONTROL_MODIFY_ERROR);
			} else {
				stats.put(STAT_LAST_POWER_CONTROL_MODIFY_ERROR,
						"Instruction result not Completed: " + result);
			}
		} catch ( RuntimeException e ) {
			log.error("Error modifying power control {}: {}", powerControlId, e.getMessage());
			stats.put(STAT_LAST_POWER_CONTROL_MODIFY_ERROR, messageForException(e));
		}
	}

	/**
	 * Evaluate current demand and generation conditions, and apply an
	 * adjustment if necessary.
	 *
	 * @param demandWatts
	 *        the current demand, in watts
	 * @param generationWatts
	 *        the current generation, in watts
	 * @param currentLimit
	 *        the current generation limit, as an integer percentage
	 * @return the result of adjusting the generation limit, or {@literal null}
	 *         if no adjustment was made
	 */
	private InstructionStatus.InstructionState evaluateBalance(final int demandWatts,
			final int generationWatts, final int currentLimit) {
		DemandBalanceStrategy strategy = getDemandBalanceStrategy();
		if ( strategy == null ) {
			throw new RuntimeException("No DemandBalanceStrategy configured.");
		}
		int desiredLimit = strategy.evaluateBalance(powerControlId, demandWatts, generationWatts,
				powerMaximumWatts, currentLimit);
		if ( desiredLimit > 0 && desiredLimit != currentLimit ) {
			log.info("Demand of {} with generation {} (capacity {}) will be adjusted from {}% to {}%",
					demandWatts, powerControlId, powerMaximumWatts, currentLimit, desiredLimit);
			InstructionStatus.InstructionState result = adjustLimit(desiredLimit);
			log.info("Demand adjumstment instruction result: {}", result);
			return result;
		}
		return null;
	}

	/**
	 * Adjust the generation limit. If no handlers are available, or no handlers
	 * acknowledge handling the instruction,
	 * {@link InstructionStatus.InstructionState#Declined} will be returned.
	 *
	 * @param desiredLimit
	 *        the desired limit, as an integer percentage
	 * @return the result of handling the adjustment instruction, never
	 *         {@literal null}
	 */
	private InstructionState adjustLimit(final int desiredLimit) {
		final InstructionExecutionService service = service(instructionExecutionService);
		InstructionStatus result = null;
		if ( service != null && powerControlId != null && !powerControlId.isBlank() ) {
			final Instruction instr = InstructionUtils.createLocalInstruction(
					InstructionHandler.TOPIC_DEMAND_BALANCE, powerControlId,
					String.valueOf(desiredLimit));
			result = service.executeInstruction(instr);
		}
		return (result != null ? result.getInstructionState() : InstructionState.Declined);
	}

	private void postStatisticsEvent() {
		if ( eventAdmin == null ) {
			return;
		}
		final EventAdmin admin = eventAdmin.service();
		if ( admin == null ) {
			return;
		}
		admin.postEvent(new Event(EVENT_TOPIC_STATISTICS, stats));
	}

	private Integer percentForLimit(NodeControlInfo limit) {
		if ( limit == null || limit.getValue() == null ) {
			return null;
		}
		try {
			return Integer.valueOf(limit.getValue());
		} catch ( NumberFormatException e ) {
			log.warn("Error parsing limit value as integer percentage: {}", e.getMessage());
		}
		return null;
	}

	private Integer wattsForEnergyDatum(EnergyDatum datum) {
		if ( datum == null ) {
			return null;
		}
		if ( datum.getWatts() != null ) {
			return datum.getWatts();
		}
		return null;
	}

	private Integer wattsForEnergyDatum(Iterable<? extends EnergyDatum> datums) {
		if ( datums == null ) {
			return null;
		}
		int total = -1;
		for ( EnergyDatum datum : datums ) {
			if ( datum instanceof AcEnergyDatum && acEnergyPhaseFilter != null
					&& acEnergyPhaseFilter.size() > 0 ) {
				AcPhase phase = ((AcEnergyDatum) datum).getAcPhase();
				if ( !acEnergyPhaseFilter.contains(phase) ) {
					continue;
				}
			}
			Integer w = wattsForEnergyDatum(datum);
			if ( w != null ) {
				if ( total < 0 ) {
					total = w;
				} else {
					total += w;
				}
			}
		}
		return (total < 0 ? null : total);
	}

	private NodeControlInfo getCurrentControlValue(OptionalService<NodeControlProvider> service,
			String controlId) {
		if ( service == null ) {
			return null;
		}
		NodeControlProvider provider = service.service();
		if ( provider == null ) {
			return null;
		}
		return provider.getCurrentControlInfo(controlId);
	}

	private Iterable<EnergyDatum> getCurrentDatum(OptionalServiceCollection<DatumDataSource> service) {
		if ( service == null ) {
			return null;
		}
		Iterable<DatumDataSource> dataSources = service.services();
		List<EnergyDatum> results = new ArrayList<>();
		for ( DatumDataSource dataSource : dataSources ) {
			if ( dataSource instanceof MultiDatumDataSource ) {
				Collection<NodeDatum> datums = ((MultiDatumDataSource) dataSource).readMultipleDatum();
				if ( datums != null ) {
					for ( NodeDatum datum : datums ) {
						EnergyDatum eDatum = asEnergyDatum(datum);
						results.add(eDatum);
					}
				}
			} else {
				NodeDatum datum = dataSource.readCurrentDatum();
				if ( datum != null ) {
					results.add(asEnergyDatum(datum));
				}
			}
		}
		return results;
	}

	private static EnergyDatum asEnergyDatum(NodeDatum datum) {
		if ( datum instanceof EnergyDatum ) {
			return (EnergyDatum) datum;
		}
		DatumSamples s = new DatumSamples(datum.asSampleOperations());
		return new SimpleEnergyDatum(datum.getSourceId(), datum.getTimestamp(), s);

	}

	private DemandBalanceStrategy getDemandBalanceStrategy() {
		if ( balanceStrategy == null ) {
			return null;
		}
		return balanceStrategy.service();
	}

	/**
	 * Getter for the current {@link DemandBalanceStrategy}.
	 *
	 * @return the strategy
	 */
	public DemandBalanceStrategy getStrategy() {
		return getDemandBalanceStrategy();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.demandbalancer";
	}

	@Override
	public String getDisplayName() {
		return "Demand Balancer";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(6);
		results.add(new BasicTextFieldSettingSpecifier("balanceStrategy.propertyFilters['uid']", null,
				false,
				"(objectClass=net.solarnetwork.node.control.demandbalancer.DemandBalanceStrategy)"));
		results.add(new BasicTextFieldSettingSpecifier("consumptionDataSource.propertyFilters['uid']",
				null, false, "(objectClass=net.solarnetwork.node.service.DatumDataSource)"));
		results.add(new BasicTextFieldSettingSpecifier(
				"consumptionDataSource.propertyFilters['groupUid']", ""));
		results.add(new BasicTextFieldSettingSpecifier("acEnergyPhaseFilter",
				commaDelimitedStringFromCollection(null)));
		results.add(new BasicToggleSettingSpecifier("collectPower", DEFAULT_COLLECT_POWER));
		results.add(new BasicTextFieldSettingSpecifier("powerDataSource.propertyFilters['uid']", null,
				false, "(objectClass=net.solarnetwork.node.service.DatumDataSource)"));
		results.add(
				new BasicTextFieldSettingSpecifier("powerDataSource.propertyFilters['groupUid']", ""));
		results.add(new BasicTextFieldSettingSpecifier("powerControlId", null));
		results.add(new BasicTextFieldSettingSpecifier("powerMaximumWatts",
				String.valueOf(DEFAULT_POWER_MAXIMUM_WATTS)));

		DemandBalanceStrategy strategy = getDemandBalanceStrategy();
		if ( strategy instanceof SettingSpecifierProvider ) {
			SettingSpecifierProvider stratSettingProvider = (SettingSpecifierProvider) strategy;
			List<SettingSpecifier> strategySpecifiers = stratSettingProvider.getSettingSpecifiers();
			if ( strategySpecifiers != null && strategySpecifiers.size() > 0 ) {
				for ( SettingSpecifier spec : strategySpecifiers ) {
					if ( spec instanceof KeyedSettingSpecifier<?> ) {
						KeyedSettingSpecifier<?> keyedSpec = (KeyedSettingSpecifier<?>) spec;
						results.add(keyedSpec.mappedTo("strategy."));
					} else {
						results.add(spec);
					}
				}
			}
		}

		return results;
	}

	// Accessors

	/**
	 * Get the power control ID.
	 *
	 * @return the power control ID
	 */
	public String getPowerControlId() {
		return powerControlId;
	}

	/**
	 * Set the ID of the control that should respond to the
	 * {@link InstructionHandler#TOPIC_DEMAND_BALANCE} instruction to match
	 * generation levels to consumption levels.
	 *
	 * @param powerControlId
	 *        the power control ID
	 */
	public void setPowerControlId(String powerControlId) {
		this.powerControlId = powerControlId;
		if ( this.powerControl != null ) {
			// automatically enforce filter
			this.powerControl.setPropertyFilter("availableControlIds", this.powerControlId);
		}
	}

	/**
	 * Get the power control.
	 *
	 * @return the power control
	 */
	public OptionalFilterableService<NodeControlProvider> getPowerControl() {
		return powerControl;
	}

	/**
	 * Set the {@link NodeControlProvider} that manages the configured
	 * {@code powerControlId}, and can report back its current status, whose
	 * value must be provided as an integer percentage of the maximum allowable
	 * generation level.
	 *
	 * <p>
	 * <b>Note</b> that this object must also implement
	 * {@link FilterableService} and will automatically have a filter property
	 * set for the {@code availableControlIds} property to match the
	 * {@code powerControlId} value.
	 * </p>
	 *
	 * @param powerControl
	 *        the power control
	 */
	public void setPowerControl(OptionalFilterableService<NodeControlProvider> powerControl) {
		powerControl.setPropertyFilter("availableControlIds", this.powerControlId);
		this.powerControl = powerControl;

	}

	/**
	 * Get the power data source collection.
	 *
	 * @return the collection
	 */
	public OptionalFilterableServiceCollection<DatumDataSource> getPowerDataSource() {
		return powerDataSource;
	}

	/**
	 * Set the collection of {@link DatumDataSource} that provide real-time
	 * power generation data.
	 *
	 * <p>
	 * If more than one {@code DatumDataSource} is configured the effective
	 * generation will be aggregated as a sum total of all of them.
	 * </p>
	 *
	 * @param powerDataSource
	 *        the power data sources
	 */
	public void setPowerDataSource(
			OptionalFilterableServiceCollection<DatumDataSource> powerDataSource) {
		this.powerDataSource = powerDataSource;
	}

	/**
	 * Get the power maximum watts.
	 *
	 * @return the maximum watts
	 */
	public int getPowerMaximumWatts() {
		return powerMaximumWatts;
	}

	/**
	 * Set the maximum watts the configured {@code powerDataSource} is capable
	 * of producing.
	 *
	 * <p>
	 * This value is used to calculate the output percentage level passed on
	 * {@link InstructionHandler#TOPIC_DEMAND_BALANCE} instructions. For
	 * example, if the {@code powerMaximumWatts} is {@literal 1000} and the
	 * current consumption is {@literal 800} then the demand balance will be
	 * requested as <b>80%</b>.
	 * </p>
	 *
	 * @param powerMaximumWatts
	 *        the maximum watts
	 */
	public void setPowerMaximumWatts(int powerMaximumWatts) {
		this.powerMaximumWatts = powerMaximumWatts;
	}

	/**
	 * Get the collection of {@link DatumDataSource} that provide real-time
	 * consumption generation data.
	 *
	 * @return the consumption data sources
	 */
	public OptionalFilterableServiceCollection<DatumDataSource> getConsumptionDataSource() {
		return consumptionDataSource;
	}

	/**
	 * Set the collection of {@link DatumDataSource} that provide real-time
	 * consumption generation data.
	 *
	 * <p>
	 * If more than one {@code DatumDataSource} is configured the effective
	 * demand will be aggregated as a sum total of all of them.
	 * </p>
	 *
	 * @param consumptionDataSource
	 *        the consumption data sources
	 */
	public void setConsumptionDataSource(
			OptionalFilterableServiceCollection<DatumDataSource> consumptionDataSource) {
		this.consumptionDataSource = consumptionDataSource;
	}

	/**
	 * Get the strategy implementation to use to decide how to balance the
	 * demand and generation.
	 *
	 * @return the strategy; defaults to {@link SimpleDemandBalanceStrategy}.
	 */
	public OptionalFilterableService<DemandBalanceStrategy> getBalanceStrategy() {
		return balanceStrategy;
	}

	/**
	 * Set the strategy implementation to use to decide how to balance the
	 * demand and generation.
	 *
	 * @param balanceStrategy
	 *        the strategy to use
	 */
	public void setBalanceStrategy(OptionalFilterableService<DemandBalanceStrategy> balanceStrategy) {
		this.balanceStrategy = balanceStrategy;
	}

	/**
	 * Set the message source.
	 *
	 * @param messageSource
	 *        the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Get the instruction handlers.
	 *
	 * @return the handlers
	 */
	public Collection<InstructionHandler> getInstructionHandlers() {
		return instructionHandlers;
	}

	/**
	 * If {@literal true} then collect datum from all configured power data
	 * sources for passing to the {@link DemandBalanceStrategy}.
	 *
	 * <p>
	 * Not all strategies need power information, and it may take too long to
	 * collect this information, however, so this can be turned off by setting
	 * to {@literal false}. When disabled, <b>-1</b> is passed for the
	 * {@code generationWatts} parameter on
	 * {@link DemandBalanceStrategy#evaluateBalance(String, int, int, int, int)}.
	 * Defaults to {@literal false}.
	 * </p>
	 *
	 * @return {@literal true} to collect power datum
	 */
	public boolean isCollectPower() {
		return collectPower;
	}

	/**
	 * Set the collect power mode.
	 *
	 * @param collectPower
	 *        the mode to set
	 */
	public void setCollectPower(boolean collectPower) {
		this.collectPower = collectPower;
	}

	/**
	 * Get the event admin service.
	 *
	 * @return the service
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set the event admin service.
	 *
	 * @param eventAdmin
	 *        the service to set
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Get the AC energy phase filter.
	 *
	 * @return the filter
	 */
	public Set<AcPhase> getAcEnergyPhaseFilter() {
		return acEnergyPhaseFilter;
	}

	/**
	 * Set the AC energy phase filter.
	 *
	 * @param acEnergyPhaseFilter
	 *        the filter to set
	 */
	public void setAcEnergyPhaseFilter(Set<AcPhase> acEnergyPhaseFilter) {
		this.acEnergyPhaseFilter = acEnergyPhaseFilter;
	}

	/**
	 * Get the value of the {@code acEnergyPhaseFilter} property as a
	 * comma-delimited string.
	 *
	 * @return the AC phase as a delimited string
	 */
	public String getAcEnergyPhaseFilterValue() {
		return commaDelimitedStringFromCollection(acEnergyPhaseFilter);
	}

	/**
	 * Set the {@code acEnergyPhaseFilter} property via a comma-delimited
	 * string.
	 *
	 * @param value
	 *        the comma delimited string
	 * @see #getAcEnergyPhaseFilterValue()
	 */
	public void getAcEnergyPhaseFilterValue(String value) {
		Set<String> set = StringUtils.commaDelimitedStringToSet(value);
		if ( set == null ) {
			acEnergyPhaseFilter = null;
			return;
		}
		Set<AcPhase> result = new LinkedHashSet<AcPhase>(set.size());
		for ( String phase : set ) {
			try {
				AcPhase p = AcPhase.valueOf(phase);
				result.add(p);
			} catch ( IllegalArgumentException e ) {
				log.warn("Ignoring unsupported AcPhase value [{}]", phase);
			}
		}
		acEnergyPhaseFilter = EnumSet.copyOf(result);
	}

}
