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

import static java.util.Collections.singletonMap;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.service.OptionalService.service;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.EnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleEnergyDatum;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.settings.MappableSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.support.PrefixedMessageSource;

/**
 * Service to monitor demand (consumption) from a
 * {@link InstructionHandler#TOPIC_SHED_LOAD} instructions to a specific control
 * to limit power draw to below a maximum threshold.
 *
 * @author matt
 * @version 2.1
 */
public class LoadShedder implements SettingSpecifierProvider, JobService {

	private final OptionalService<InstructionExecutionService> instructionExecutionService;
	private OptionalService<LoadShedderStrategy> shedStrategy = new StaticOptionalService<LoadShedderStrategy>(
			new DefaultLoadShedderStrategy());
	private OptionalService<DatumDataSource> consumptionDataSource;
	private List<LoadShedControlConfig> configs = new ArrayList<LoadShedControlConfig>(4);

	private int consumptionSampleLimit = 10;
	private final Deque<EnergyDatum> consumptionSamples = new ArrayDeque<EnergyDatum>(10);
	private final ConcurrentMap<String, LoadShedControlInfo> switchInfos = new ConcurrentHashMap<String, LoadShedControlInfo>(
			4);
	private Date lastEvaluationDate;
	private MessageSource messageSource;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 *
	 * @param instructionExecutionService
	 *        the execution service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LoadShedder(OptionalService<InstructionExecutionService> instructionExecutionService) {
		super();
		if ( instructionExecutionService == null ) {
			throw new IllegalArgumentException(
					"The instructionExecutionService argument must not be null.");
		}
		this.instructionExecutionService = instructionExecutionService;
	}

	/**
	 * Evaluate current demand (consumption) and attempt to shed load as
	 * necessary.
	 *
	 * @return the resulting state
	 */
	public synchronized InstructionState evaluatePowerLoad() {
		final long now = System.currentTimeMillis();
		LoadShedderStrategy strategy = getStrategy();
		if ( strategy == null ) {
			log.warn("No LoadShedderStrategy service avaialble");
			return null;
		}
		lastEvaluationDate = new Date(now);
		addPowerSample(consumptionDataSource, consumptionSamples); // adds to sample buffer
		Deque<EnergyDatum> samples = consumptionSamples;
		Collection<LoadShedAction> actions = strategy.evaulateRules(getConfigs(), switchInfos, now,
				samples);
		if ( actions == null || actions.size() < 1 ) {
			return null;
		}
		InstructionExecutionService service = service(instructionExecutionService);
		if ( service == null ) {
			return InstructionState.Declined;
		}
		InstructionStatus result = null;
		for ( LoadShedAction action : actions ) {
			final Instruction instr = createLocalInstruction(InstructionHandler.TOPIC_SHED_LOAD,
					singletonMap(action.getControlId(), action.getShedWatts().toString()));
			result = service.executeInstruction(instr);
			if ( result != null && result.getInstructionState() == InstructionState.Completed ) {
				log.info("Switch {} limit released for {}W", action.getControlId(),
						action.getShedWatts());
				updateSwitchInfo(action.getControlId(), action, samples.peek());
			}
		}
		return (result != null ? result.getInstructionState()
				: InstructionStatus.InstructionState.Declined);
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

	private EnergyDatum addPowerSample(OptionalService<DatumDataSource> service,
			Deque<EnergyDatum> samples) {
		DatumDataSource dataSource = service(service);
		if ( dataSource == null ) {
			return null;
		}
		NodeDatum nodeDatum = dataSource.readCurrentDatum();
		if ( nodeDatum == null ) {
			return null;
		}
		EnergyDatum datum;
		if ( nodeDatum instanceof EnergyDatum ) {
			datum = (EnergyDatum) nodeDatum;
		} else {
			// convert to EnergyDatum instance
			DatumSamples s = new DatumSamples(nodeDatum.asSampleOperations());
			datum = new SimpleEnergyDatum(nodeDatum.getSourceId(), nodeDatum.getTimestamp(), s);
		}

		// maintain a buffer of samples so we can monitor the effect of limit operations
		// buffer ordered from most recent to oldest

		EnergyDatum previous = samples.peek();
		if ( previous != null && previous.getTimestamp().equals(datum.getTimestamp()) ) {
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
	public String getSettingUid() {
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

		final Locale locale = Locale.getDefault();
		final LoadShedderStrategy strat = getStrategy();

		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(locale), true));

		for ( LoadShedControlInfo info : switchInfos.values() ) {
			String infoMessage = getInfoMessage(info, locale);
			String stratMessage = (strat != null ? strat.getStatusMessage(info, locale) : null);
			if ( stratMessage != null && stratMessage.length() > 0 ) {
				infoMessage += " " + stratMessage;
			}
			results.add(new BasicTitleSettingSpecifier("info.control", infoMessage, true));
		}

		results.add(new BasicTextFieldSettingSpecifier("shedStrategy.propertyFilters['uid']", null,
				false, "(objectClass=net.solarnetwork.node.control.loadshedder.LoadShedderStrategy)"));
		results.add(new BasicTextFieldSettingSpecifier("consumptionDataSource.propertyFilters['uid']",
				null, false, "(objectClass=net.solarnetwork.node.service.DatumDataSource)"));

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
		BasicGroupSettingSpecifier listComplexGroup = SettingUtils.dynamicListSettingSpecifier("configs",
				configList, new SettingUtils.KeyedListCallback<LoadShedControlConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(LoadShedControlConfig value,
							int index, String key) {
						BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								value.settings(key + "."));
						return Collections.<SettingSpecifier> singletonList(configGroup);
					}
				});
		results.add(listComplexGroup);

		return results;
	}

	private String getInfoMessage(final Locale locale) {
		if ( lastEvaluationDate == null ) {
			return messageSource.getMessage("info.noEvaluations", null, locale);
		}
		StringBuilder buf = new StringBuilder();
		buf.append(messageSource.getMessage("info.basic", new Object[] { lastEvaluationDate }, locale));
		EnergyDatum latest = consumptionSamples.peek();
		if ( latest != null ) {
			buf.append(" ").append(messageSource.getMessage("info.reading",
					new Object[] { latest.getWatts(), latest.getTimestamp() }, locale));
		}
		return buf.toString();
	}

	private String getInfoMessage(final LoadShedControlInfo info, final Locale locale) {
		assert info != null;
		final String mode = messageSource
				.getMessage((info.getAction() != null && info.getAction().getShedWatts() != null
						&& info.getAction().getShedWatts() > 0 ? "info.control.shedding"
								: "info.control.notshedding"),
						null, locale);
		StringBuilder buf = new StringBuilder();
		buf.append(messageSource.getMessage("info.control.basic",
				new Object[] { info.getControlId(), mode }, locale));
		if ( info.getActionDate() != null ) {
			buf.append(" ").append(messageSource.getMessage("info.control.action",
					new Object[] { info.getActionDate(), info.getWattsBeforeAction() }, locale));
			LoadShedControlConfig rule = configForControlId(info.getControlId());
			if ( info.getAction() != null && info.getAction().getShedWatts() != null
					&& info.getAction().getShedWatts() > 0 && rule != null
					&& rule.getMinimumLimitMinutes() != null ) {
				long nextActionAllowed = info.getActionDate().getTime()
						+ rule.getMinimumLimitMinutes().longValue() * 60000L;
				if ( nextActionAllowed > System.currentTimeMillis() ) {
					buf.append(" ").append(messageSource.getMessage("info.control.action.lock",
							new Object[] { rule.getMinimumLimitMinutes(), new Date(nextActionAllowed) },
							locale));
				}
			}
		}
		return buf.toString();
	}

	private LoadShedControlConfig configForControlId(String controlId) {
		List<LoadShedControlConfig> list = configs;
		if ( list == null ) {
			return null;
		}
		for ( LoadShedControlConfig c : list ) {
			if ( controlId.equals(c.getControlId()) ) {
				return c;
			}
		}
		return null;
	}

	// Accessors

	public OptionalService<DatumDataSource> getConsumptionDataSource() {
		return consumptionDataSource;
	}

	public void setConsumptionDataSource(OptionalService<DatumDataSource> consumptionDataSource) {
		this.consumptionDataSource = consumptionDataSource;
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
