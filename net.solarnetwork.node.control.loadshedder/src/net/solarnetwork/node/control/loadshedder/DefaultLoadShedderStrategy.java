/* ==================================================================
 * DefaultLoadShedderStrategy.java - 29/06/2015 6:44:30 am
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.node.domain.datum.EnergyDatum;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Default implementation of {@link LoadShedderStrategy}.
 *
 * @author matt
 * @version 2.0
 */
public class DefaultLoadShedderStrategy extends BasicIdentifiable
		implements LoadShedderStrategy, SettingSpecifierProvider {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private int shedThresholdWatts = 9500;
	private int limitExecutionMonitorSeconds = 60;
	private int powerAverageSampleSeconds = 10;
	private Collection<NodeControlProvider> controls = Collections.emptyList();

	/**
	 * Constructor.
	 */
	public DefaultLoadShedderStrategy() {
		super();
		setUid("Default");
	}

	@Override
	public Collection<LoadShedAction> evaulateRules(List<LoadShedControlConfig> rules,
			Map<String, LoadShedControlInfo> limitStatuses, final long date,
			Collection<EnergyDatum> powerSamples) {
		if ( rules == null || rules.size() < 1 ) {
			log.info("No rules defined, no limit placed on power.");
			return null;
		}
		rules = applicableRules(date, rules);
		if ( rules == null || rules.size() < 1 ) {
			log.info("No applicable rules available, no limit placed on power.");
			return null;
		}
		final Integer powerNow = effectivePowerValue(date, powerSamples);
		if ( powerNow == null ) {
			log.info("Power reading not avaialble, no limit placed on power.");
			return null;
		}
		LoadShedAction result = null;
		if ( powerNow > shedThresholdWatts ) {
			// find a switch we can actively limit power on
			log.info("Power limit required: current power {}W > threshold {}W", powerNow,
					shedThresholdWatts);
			final int desiredShedAmount = (powerNow - shedThresholdWatts);
			String controlId = controlIdToExecuteLimit(date, rules, limitStatuses, desiredShedAmount);
			if ( controlId == null ) {
				log.warn("No switch avaialble to shed {}W", desiredShedAmount);
			} else {
				result = shedLoad(date, controlId, desiredShedAmount);
			}
		} else {
			// find if there is a switch we can stop limiting power on
			final int desiredReleaseAmount = (powerNow - shedThresholdWatts);

			// reverse the order of the rules, we want to release in reverse order of limit
			Collections.reverse(rules);

			String controlId = controlIdToRemoveLimit(date, rules, limitStatuses);
			if ( controlId == null ) {
				log.trace("No controls need limit lifted.");
			} else {
				result = removeLoadLimit(date, controlId, desiredReleaseAmount);
			}
		}
		return (result == null ? null : Collections.singletonList(result));
	}

	@Override
	public String getStatusMessage(LoadShedControlInfo info, Locale locale) {
		return null;
	}

	private String controlIdToExecuteLimit(final long date, List<LoadShedControlConfig> rules,
			Map<String, LoadShedControlInfo> limitStatuses, int desiredShedAmount) {
		// we assume rules already sorted by priority here, and filtered to just the applicable ones,
		// so find first rule for a switch that hasn't been switched within limitExecutionMonitorSeconds

		// we don't want to execute a change if ANY switch has been changed within the configured cool down period
		for ( LoadShedControlConfig rule : rules ) {
			String controlId = rule.getControlId();
			LoadShedControlInfo info = (limitStatuses != null ? limitStatuses.get(controlId) : null);
			if ( switchSwitchedTooRecently(date, info, rule) ) {
				log.debug("Switch {} switched too recently to enforce limit now: {}", controlId,
						info.getActionDate());
				return null;
			}
		}

		for ( LoadShedControlConfig rule : rules ) {
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

	private String controlIdToRemoveLimit(final long date, List<LoadShedControlConfig> rules,
			Map<String, LoadShedControlInfo> limitStatuses) {
		// we assume rules already sorted by priority here, and filtered to just the applicable ones,
		// so find first rule for a switch that hasn't been switched within limitExecutionMonitorSeconds

		// we don't want to execute a change if ANY switch has been changed within the configured cool down period
		for ( LoadShedControlConfig rule : rules ) {
			String controlId = rule.getControlId();
			LoadShedControlInfo info = (limitStatuses != null ? limitStatuses.get(controlId) : null);
			if ( switchSwitchedTooRecently(date, info, rule) ) {
				log.trace("Switch {} switched too recently to release any limit now: {}", controlId,
						info.getActionDate());
				return null;
			}
		}

		for ( LoadShedControlConfig rule : rules ) {
			String controlId = rule.getControlId();
			NodeControlProvider switchControl = switchControlForId(controlId);
			if ( switchControl == null ) {
				log.warn("Switch {} not available, cannot use to limit power.", controlId);
				continue;
			}

			LoadShedControlInfo info = (limitStatuses != null ? limitStatuses.get(controlId) : null);
			if ( switchWithinLimitHoldPeriod(date, info, rule) ) {
				log.debug("Switch {} within limit hold period, cannot  release limit now: {}", controlId,
						info.getActionDate());
				continue;
			}

			NodeControlInfo controlInfo = switchControl.getCurrentControlInfo(controlId);
			if ( switchIsLimitingPower(controlInfo) ) {
				log.info("Found switch {} available for removing load shed limit", controlId);
				return controlId;
			} else {
				log.trace("Switch {} already not limiting power, cannot use to remove limit", controlId);
			}
		}
		return null;
	}

	private boolean switchSwitchedTooRecently(final long date, LoadShedControlInfo info,
			LoadShedControlConfig config) {
		if ( info == null ) {
			return false;
		}
		final long switchedDate = info.getActionDate().getTime();

		// don't switch again if we very recently switched the switch (either on or off)
		if ( switchedDate + limitExecutionMonitorSeconds * 1000L > date ) {
			return true;
		}

		return false;
	}

	private boolean switchWithinLimitHoldPeriod(final long date, LoadShedControlInfo info,
			LoadShedControlConfig config) {
		if ( info == null || info.getActionDate() == null ) {
			return false;
		}
		final long switchedDate = info.getActionDate().getTime();
		if ( info.getAction() != null && info.getAction().getShedWatts() != null && config != null
				&& config.getMinimumLimitMinutes() != null && info.getAction().getShedWatts() > 0
				&& switchedDate + config.getMinimumLimitMinutes().longValue() * 60000L > date ) {
			return true;
		}
		return false;
	}

	private boolean switchIsLimitingPower(NodeControlInfo controlInfo) {
		final String value = controlInfo.getValue();
		switch (controlInfo.getType()) {
			case Boolean:
				// TRUE means actively limiting, FALSE means NOT limiting
				if ( value != null && (value.equals("1") || value.equalsIgnoreCase("yes")
						|| value.equalsIgnoreCase("true")) ) {
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
		Collection<NodeControlProvider> providers = controls;
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

	private LoadShedAction removeLoadLimit(final long date, final String controlId,
			final int desiredAmountInWatts) {
		assert desiredAmountInWatts < 1;
		return shedLoad(date, controlId, desiredAmountInWatts);
	}

	private LoadShedAction shedLoad(final long date, final String controlId,
			final int desiredAmountInWatts) {
		LoadShedAction action = new LoadShedAction();
		action.setControlId(controlId);
		action.setShedWatts(desiredAmountInWatts);
		return action;
	}

	/**
	 * Get a list of rules that satisfy all constraints based on the current
	 * time, sorted by priority.
	 *
	 * @param date
	 *        The date at which to evaluate the rules.
	 * @param rules
	 *        The rules to filter.
	 * @return The applicable rules.
	 */
	private List<LoadShedControlConfig> applicableRules(final long date,
			List<LoadShedControlConfig> rules) {
		if ( rules == null ) {
			return null;
		}
		List<LoadShedControlConfig> applicable = new ArrayList<LoadShedControlConfig>(rules.size());
		for ( LoadShedControlConfig rule : rules ) {
			if ( rule.getActive() != null && rule.getActive().booleanValue() == false ) {
				continue;
			}
			if ( rule.fallsWithinTimeWindow(date) ) {
				applicable.add(rule);
			}
		}
		Collections.sort(applicable, LoadShedControlConfigPriorityComparator.COMPARATOR);
		return applicable;
	}

	private Integer effectivePowerValue(final long date,
			final Collection<EnergyDatum> consumptionSamples) {
		final long oldestDate = date - (powerAverageSampleSeconds * 1000L);
		double totalPower = 0;
		double totalSeconds = 0;
		EnergyDatum prevDatum = null;
		for ( EnergyDatum d : consumptionSamples ) {
			if ( d.getTimestamp().toEpochMilli() < oldestDate ) {
				break;
			}
			if ( prevDatum != null ) {
				Integer power = getPowerValue(d);
				Integer prevPower = getPowerValue(prevDatum);
				if ( power != null && prevPower != null ) {
					double ds = (prevDatum.getTimestamp().toEpochMilli()
							- d.getTimestamp().toEpochMilli()) / 1000.0;
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

	private Integer getPowerValue(EnergyDatum datum) {
		if ( datum == null ) {
			return null;
		}
		return datum.getWatts();
	}

	// Settings support

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.loadshedder.DefaultLoadShedderStrategy";
	}

	@Override
	public String getDisplayName() {
		return "Default Load Shedder Strategy";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);
		DefaultLoadShedderStrategy defaults = new DefaultLoadShedderStrategy();
		results.add(new BasicTextFieldSettingSpecifier("shedThresholdWatts",
				String.valueOf(defaults.shedThresholdWatts)));
		results.add(new BasicTextFieldSettingSpecifier("powerAverageSampleSeconds",
				String.valueOf(defaults.powerAverageSampleSeconds)));
		results.add(new BasicTextFieldSettingSpecifier("limitExecutionMonitorSeconds",
				String.valueOf(defaults.limitExecutionMonitorSeconds)));
		return results;
	}

	// Accessors

	/**
	 * Get the shed threshold watts.
	 *
	 * @return the threshold
	 */
	public int getShedThresholdWatts() {
		return shedThresholdWatts;
	}

	/**
	 * Set the shed threshold watts.
	 *
	 * @param shedThresholdWatts
	 *        the threshold to set
	 */
	public void setShedThresholdWatts(int shedThresholdWatts) {
		this.shedThresholdWatts = shedThresholdWatts;
	}

	/**
	 * Get the limit execution monitor seconds.
	 *
	 * @return the seconds
	 */
	public int getLimitExecutionMonitorSeconds() {
		return limitExecutionMonitorSeconds;
	}

	/**
	 * Set the limit execution monitor seconds.
	 *
	 * @param limitExecutionMonitorSeconds
	 *        the seconds to set
	 */
	public void setLimitExecutionMonitorSeconds(int limitExecutionMonitorSeconds) {
		this.limitExecutionMonitorSeconds = limitExecutionMonitorSeconds;
	}

	/**
	 * Get the controls.
	 *
	 * @return the controls
	 */
	public Collection<NodeControlProvider> getControls() {
		return controls;
	}

	/**
	 * Set the controls.
	 *
	 * @param controls
	 *        the controls to set
	 */
	public void setControls(Collection<NodeControlProvider> controls) {
		this.controls = controls;
	}

	/**
	 * Get the power average sample seconds.
	 *
	 * @return the seconds
	 */
	public int getPowerAverageSampleSeconds() {
		return powerAverageSampleSeconds;
	}

	/**
	 * Set the power average sample seconds.
	 *
	 * @param powerAverageSampleSeconds
	 *        the seconds to set
	 */
	public void setPowerAverageSampleSeconds(int powerAverageSampleSeconds) {
		this.powerAverageSampleSeconds = powerAverageSampleSeconds;
	}

}
