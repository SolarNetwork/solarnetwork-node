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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.power.PowerDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.InstructionUtils;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.FilterableService;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StaticOptionalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * Basic service to monitor demand conditions (consumption) and generation
 * (power) and send out {@link InstructionHandler#TOPIC_DEMAND_BALANCE}
 * instructions to a specific control to limit generation to an amount that
 * keeps generation at or below current consumption levels.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>powerControlId</dt>
 * <dd>The ID of the control that should respond to the
 * {@link InstructionHandler#TOPIC_DEMAND_BALANCE} instruction to match
 * generation levels to consumption levels.</dd>
 * <dt>powerControl</dt>
 * <dd>The {@link NodeControlProvider} that manages the configured
 * {@code powerControlId}, and can report back its current status, whose value
 * must be provided as an integer percentage of the maximum allowable generation
 * level. <b>Note</b> that this object must also implement
 * {@link FilterableService} and will automatically have a filter property set
 * for the {@code availableControlIds} property to match the
 * {@code powerControlId} value.</dd>
 * <dt>powerDataSource</dt>
 * <dd>The collection of {@link DatumDataSource} that provide real-time power
 * generation data. If more than one {@code DatumDataSource} is configured the
 * effective generation will be aggregated as a sum total of all of them.</dd>
 * <dt>powerMaximumWatts</dt>
 * <dd>The maximum watts the configured {@code powerDataSource} is capable of
 * producing. This value is used to calculate the output percentage level passed
 * on {@link InstructionHandler#TOPIC_DEMAND_BALANCE} instructions. For example,
 * if the {@code powerMaximumWatts} is {@bold 1000} and the current
 * consumption is {@bold 800} then the demand balance will be requested
 * as {@bold 80%}.</dd
 * <dt>consumptionDataSource</dt>
 * <dd>The collection of {@link DatumDataSource} that provide real-time
 * consumption generation data. If more than one {@code DatumDataSource} is
 * configured the effective demand will be aggregated as a sum total of all of
 * them.</dd>
 * <dt>balanceStrategy</dt>
 * <dd>The strategy implementation to use to decide how to balance the demand
 * and generation. Defaults to {@link SimpleDemandBalanceStrategy}.</dd>
 * <dt>instructionHandlers</dt>
 * <dd>A collection of {@link InstructionHandler} instances. When
 * {@link #evaluateBalance()} is called, if a balancing adjustment is necessary
 * then the instruction will be passed to each of these handlers, with the first
 * to process it being assumed the only handler that need respond.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class DemandBalancer implements SettingSpecifierProvider {

	private String powerControlId = "/power/pcm/1?percent";
	private OptionalService<NodeControlProvider> powerControl;
	private OptionalServiceCollection<DatumDataSource<PowerDatum>> powerDataSource;
	private int powerMaximumWatts = 1000;
	private OptionalServiceCollection<DatumDataSource<ConsumptionDatum>> consumptionDataSource;
	private OptionalService<DemandBalanceStrategy> balanceStrategy = new StaticOptionalService<DemandBalanceStrategy>(
			new SimpleDemandBalanceStrategy());
	private Collection<InstructionHandler> instructionHandlers = Collections.emptyList();
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Evaluate current demand (consumption) and generation (power) and attempt
	 * to maximize power generation up to the current demand level.
	 */
	public void evaluateBalance() {
		final Iterable<ConsumptionDatum> demand = getCurrentDatum(consumptionDataSource);
		final Integer demandWatts = wattsForEnergyDatum(demand);
		final Iterable<PowerDatum> generation = getCurrentDatum(powerDataSource);
		final Integer generationWatts = wattsForEnergyDatum(generation);
		final NodeControlInfo generationLimit = getCurrentControlValue(powerControl, powerControlId);
		final Integer generationLimitPercent = percentForLimit(generationLimit);
		log.debug("Current demand: {}, generation: {}, capacity: {}, limit: {}",
				(demandWatts == null ? "N/A" : demandWatts.toString()), (generationWatts == null ? "N/A"
						: generationWatts.toString()), powerMaximumWatts,
				(generationLimitPercent == null ? "N/A" : generationLimitPercent + "%"));
		if ( demandWatts != null && generationWatts != null ) {
			evaluateBalance(demandWatts.intValue(), generationWatts.intValue(),
					(generationLimitPercent == null ? -1 : generationLimitPercent.intValue()));
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
	 * @return the result of adjusting the generation limit, or <em>null</em> if
	 *         no adjustment was made
	 */
	private InstructionStatus.InstructionState evaluateBalance(final int demandWatts,
			final int generationWatts, final int currentLimit) {
		DemandBalanceStrategy strategy = getDemandBalanceStrategy();
		if ( strategy == null ) {
			throw new RuntimeException("No DemandBalanceStrategy configured.");
		}
		int desiredLimit = strategy.evaluateBalance(powerControlId, demandWatts, generationWatts,
				powerMaximumWatts, currentLimit);
		if ( desiredLimit != currentLimit ) {
			log.info("Demand of {} with generation {} (capacity {}) will be adjusted from {}% to {}%",
					demandWatts, powerControlId, powerMaximumWatts, currentLimit, desiredLimit);
			return adjustLimit(desiredLimit);
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
	 *         <em>null</em>
	 */
	private InstructionStatus.InstructionState adjustLimit(final int desiredLimit) {
		final BasicInstruction instr = new BasicInstruction(InstructionHandler.TOPIC_DEMAND_BALANCE,
				new Date(), Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(powerControlId, String.valueOf(desiredLimit));
		final InstructionStatus.InstructionState result = InstructionUtils.handleInstruction(
				instructionHandlers, instr);
		return (result == null ? InstructionStatus.InstructionState.Declined : result);
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
		return provider.getCurrentControlInfo(controlId);
	}

	private <T extends Datum> Iterable<T> getCurrentDatum(
			OptionalServiceCollection<DatumDataSource<T>> service) {
		if ( service == null ) {
			return null;
		}
		Iterable<DatumDataSource<T>> dataSources = service.services();
		List<T> results = new ArrayList<T>();
		for ( DatumDataSource<T> dataSource : dataSources ) {
			T datum = dataSource.readCurrentDatum();
			if ( datum != null ) {
				results.add(datum);
			}
		}
		return results;
	}

	private DemandBalanceStrategy getDemandBalanceStrategy() {
		if ( balanceStrategy == null ) {
			return null;
		}
		return balanceStrategy.service();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
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
		DemandBalancer defaults = new DemandBalancer();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(6);
		results.add(new BasicTextFieldSettingSpecifier("balanceStrategy.propertyFilters['UID']",
				"Default"));
		results.add(new BasicTextFieldSettingSpecifier("consumptionDataSource.propertyFilters['UID']",
				"Main"));
		results.add(new BasicTextFieldSettingSpecifier(
				"consumptionDataSource.propertyFilters['groupUID']", ""));
		results.add(new BasicTextFieldSettingSpecifier("powerDataSource.propertyFilters['UID']", "Main"));
		results.add(new BasicTextFieldSettingSpecifier("powerDataSource.propertyFilters['groupUID']", ""));
		results.add(new BasicTextFieldSettingSpecifier("powerControlId", defaults.powerControlId));
		results.add(new BasicTextFieldSettingSpecifier("powerMaximumWatts", String
				.valueOf(defaults.powerMaximumWatts)));
		return results;
	}

	// Accessors

	public void setPowerControlId(String powerControlId) {
		this.powerControlId = powerControlId;
		if ( this.powerControl != null ) {
			// automatically enforce filter
			((FilterableService) this.powerControl).setPropertyFilter("availableControlIds",
					this.powerControlId);
		}
	}

	public void setPowerControl(OptionalService<NodeControlProvider> powerControl) {
		if ( !(powerControl instanceof FilterableService) ) {
			throw new IllegalArgumentException("OptionalService must also implement "
					+ FilterableService.class.getName());
		}
		((FilterableService) powerControl).setPropertyFilter("availableControlIds", this.powerControlId);
		this.powerControl = powerControl;

	}

	public void setPowerDataSource(OptionalServiceCollection<DatumDataSource<PowerDatum>> powerDataSource) {
		this.powerDataSource = powerDataSource;
	}

	public void setPowerMaximumWatts(int powerMaximumWatts) {
		this.powerMaximumWatts = powerMaximumWatts;
	}

	public void setConsumptionDataSource(
			OptionalServiceCollection<DatumDataSource<ConsumptionDatum>> consumptionDataSource) {
		this.consumptionDataSource = consumptionDataSource;
	}

	public void setBalanceStrategy(OptionalService<DemandBalanceStrategy> balanceStrategy) {
		this.balanceStrategy = balanceStrategy;
	}

	public void setInstructionHandlers(Collection<InstructionHandler> instructionHandlers) {
		this.instructionHandlers = instructionHandlers;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getPowerControlId() {
		return powerControlId;
	}

	public OptionalService<NodeControlProvider> getPowerControl() {
		return powerControl;
	}

	public OptionalServiceCollection<DatumDataSource<PowerDatum>> getPowerDataSource() {
		return powerDataSource;
	}

	public int getPowerMaximumWatts() {
		return powerMaximumWatts;
	}

	public OptionalServiceCollection<DatumDataSource<ConsumptionDatum>> getConsumptionDataSource() {
		return consumptionDataSource;
	}

	public OptionalService<DemandBalanceStrategy> getBalanceStrategy() {
		return balanceStrategy;
	}

	public Collection<InstructionHandler> getInstructionHandlers() {
		return instructionHandlers;
	}

}
