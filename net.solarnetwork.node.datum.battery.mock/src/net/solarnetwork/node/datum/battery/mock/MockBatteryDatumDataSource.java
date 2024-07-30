/* ==================================================================
 * MockBatteryDatumDataSource.java - 30/07/2024 11:58:52 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.battery.mock;

import static java.util.Collections.emptyList;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.EnergyStorageDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleEnergyStorageDatum;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Simple simulated battery.
 *
 * @author matt
 * @version 1.0
 */
public class MockBatteryDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider, NodeControlProvider, InstructionHandler {

	/** A datum property name for the power rate. */
	public static final String POWER_RATE_PROP = "powerRate";

	/** A default maximum power rate value. */
	public static final double DEFAULT_MAX_POWER_RATE = 1000.0;

	/** The {@code energyCapacity} property default value. */
	public static final double DEFAULT_ENERGY_CAPACITY = 10_000.0;

	private final Clock clock;

	private String sourceId;
	private String targetPowerRateControlId;
	private double energyCapacity = DEFAULT_ENERGY_CAPACITY;
	private double maxPowerRateCharge = DEFAULT_MAX_POWER_RATE;
	private double maxPowerRateDischarge = -DEFAULT_MAX_POWER_RATE;

	private double targetPowerRate = 0.0;
	private double availableEnergyCapacity = DEFAULT_ENERGY_CAPACITY;

	private SimpleEnergyStorageDatum lastDatum;

	/**
	 * Constructor.
	 */
	public MockBatteryDatumDataSource() {
		this(Clock.systemUTC());
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MockBatteryDatumDataSource(Clock clock) {
		super();
		this.clock = ObjectUtils.requireNonNullArgument(clock, "clock");
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return EnergyStorageDatum.class;
	}

	@Override
	public EnergyStorageDatum readCurrentDatum() {
		return sample();
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId(), null);
		return (sourceId == null || sourceId.isEmpty() ? emptyList()
				: Collections.singletonList(sourceId));
	}

	@Override
	public List<String> getAvailableControlIds() {
		final String targetPowerRateControlId = resolvePlaceholders(this.targetPowerRateControlId, null);
		return (targetPowerRateControlId == null || targetPowerRateControlId.isEmpty()
				? Collections.emptyList()
				: Collections.singletonList(targetPowerRateControlId));
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		final String targetPowerRateControlId = resolvePlaceholders(this.targetPowerRateControlId, null);
		if ( targetPowerRateControlId == null || !targetPowerRateControlId.equals(controlId) ) {
			return null;
		}
		EnergyStorageDatum d = readCurrentDatum();
		return newSimpleNodeControlInfoDatum(targetPowerRateControlId,
				d.asSampleOperations().getSampleString(DatumSamplesType.Instantaneous, POWER_RATE_PROP));
	}

	private SimpleNodeControlInfoDatum newSimpleNodeControlInfoDatum(String controlId, String value) {
		// @formatter:off
		NodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(resolvePlaceholders(controlId))
				.withType(NodeControlPropertyType.Float)
				.withReadonly(false)
				.withUnit("W")
				.withValue(value)
				.build();
		// @formatter:on
		return new SimpleNodeControlInfoDatum(info, Instant.now());
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public synchronized InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		// look for a parameter name that matches a control ID
		final String targetPowerRateControlId = this.targetPowerRateControlId;
		InstructionState result = null;
		for ( String paramName : instruction.getParameterNames() ) {
			if ( targetPowerRateControlId.equals(paramName) ) {
				Number newValue = StringUtils
						.numberValue(instruction.getParameterValue(targetPowerRateControlId));
				if ( updateTargetPowerRate(newValue) ) {
					result = InstructionState.Completed;
				} else {
					result = InstructionState.Declined;
				}
			}
		}
		return (result != null ? InstructionUtils.createStatus(instruction, result) : null);
	}

	private boolean updateTargetPowerRate(Number newValue) {
		if ( newValue == null ) {
			return false;
		}
		setTargetPowerRate(newValue.doubleValue());
		return true;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.battery.mock";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		final List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTitleSettingSpecifier("status", statusMessage(Locale.getDefault()), true,
				true));

		result.addAll(getIdentifiableSettingSpecifiers());

		result.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		result.add(new BasicTextFieldSettingSpecifier("targetPowerRateControlId", null));
		result.add(new BasicTextFieldSettingSpecifier("energyCapacity",
				String.valueOf(DEFAULT_ENERGY_CAPACITY)));
		result.add(new BasicTextFieldSettingSpecifier("maxPowerRateCharge",
				String.valueOf(DEFAULT_MAX_POWER_RATE)));
		result.add(new BasicTextFieldSettingSpecifier("maxPowerRateDischarge",
				String.valueOf(-DEFAULT_MAX_POWER_RATE)));

		return result;
	}

	private String statusMessage(Locale locale) {
		final double currentCapacity = this.availableEnergyCapacity;
		final double totalCapacity = this.energyCapacity;
		final int rate = (int) this.targetPowerRate;
		final int soc = (int) ((currentCapacity / totalCapacity) * 100.0);
		return getMessageSource().getMessage("status.msg",
				new Object[] { soc, (int) currentCapacity, rate }, locale);
	}

	private synchronized SimpleEnergyStorageDatum sample() {
		final Instant now = clock.instant();

		double availCapacity = this.availableEnergyCapacity;
		final double totalCapacity = this.energyCapacity;
		final double rate = (int) this.targetPowerRate;
		final double soc = ((availCapacity / totalCapacity) * 100.0);

		final DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue(POWER_RATE_PROP, (int) rate);

		if ( rate != 0 && lastDatum != null ) {
			double hoursDiff = (double) lastDatum.getTimestamp().until(now, ChronoUnit.MILLIS)
					/ TimeUnit.HOURS.toMillis(1);
			double newCapacity = Math.max(0.0,
					Math.min(totalCapacity, availCapacity + hoursDiff * rate));
			if ( newCapacity != availCapacity ) {
				this.availableEnergyCapacity = newCapacity;
			}
		}

		final SimpleEnergyStorageDatum newDatum = new SimpleEnergyStorageDatum(sourceId, now, s);
		newDatum.setAvailableEnergy((long) availCapacity);
		newDatum.setEnergyCapacity((long) totalCapacity);
		newDatum.setAvailableEnergyPercentage((float) soc);

		this.lastDatum = newDatum;
		return newDatum;
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public final String getSourceId() {
		return sourceId;
	}

	/**
	 * Set source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the control ID to use for the target power rate.
	 *
	 * @return the control ID
	 */
	public final String getTargetPowerRateControlId() {
		return targetPowerRateControlId;
	}

	/**
	 * Set the control ID to use for the target power rate.
	 *
	 * @param targetPowerRateControlId
	 *        the control ID to set
	 */
	public final void setTargetPowerRateControlId(String targetPowerRateControlId) {
		this.targetPowerRateControlId = targetPowerRateControlId;
	}

	/**
	 * Get the target power rate, in watts.
	 *
	 * @return the rate in watts; defaults to {@code 0}
	 */
	public final double getTargetPowerRate() {
		return targetPowerRate;
	}

	/**
	 * Set the target power rate, in watts.
	 *
	 * <p>
	 * A positive value represents a <em>charge</em> (receive) rate while a
	 * negative value represents a <em>discharge</em> (supply) rate.
	 * </p>
	 *
	 * @param targetPowerRate
	 *        the rate to set in watts
	 */
	public synchronized final void setTargetPowerRate(double targetPowerRate) {
		this.targetPowerRate = (targetPowerRate > 0 ? Math.min(targetPowerRate, maxPowerRateCharge)
				: targetPowerRate < 0 ? Math.max(targetPowerRate, maxPowerRateDischarge)
						: targetPowerRate);
	}

	/**
	 * Get the total energy capacity, in watt-hours.
	 *
	 * @return the energy capacity, in watt-hours
	 */
	public final double getEnergyCapacity() {
		return energyCapacity;
	}

	/**
	 * Set the total energy capacity, in watt-hours.
	 *
	 * @param energyCapacity
	 *        the energyCapacity to set
	 */
	public synchronized final void setEnergyCapacity(double energyCapacity) {
		this.energyCapacity = (energyCapacity < 0.0 ? 0.0 : energyCapacity);
		if ( this.energyCapacity < this.availableEnergyCapacity ) {
			this.availableEnergyCapacity = this.energyCapacity;
		}
	}

	/**
	 * Get the maximum power rate for charging, in watts.
	 *
	 * @return the maximum power rate, in watts
	 */
	public final double getMaxPowerRateCharge() {
		return maxPowerRateCharge;
	}

	/**
	 * Set the maximum power rate for charging, in watts.
	 *
	 * @param maxPowerRateCharge
	 *        the maximum power rate to set, in watts
	 */
	public synchronized final void setMaxPowerRateCharge(double maxPowerRateCharge) {
		this.maxPowerRateCharge = (maxPowerRateCharge < 0 ? -maxPowerRateCharge : maxPowerRateCharge);
		setTargetPowerRate(targetPowerRate); // enforce new max
	}

	/**
	 * Get the maximum power rate for discharging, in watts.
	 *
	 * @return the maximum power rate, in watts
	 */
	public final double getMaxPowerRateDischarge() {
		return maxPowerRateDischarge;
	}

	/**
	 * Set the maximum power rate for discharging, in watts.
	 *
	 * @param maxPowerRateDischarge
	 *        the maximum power rate to set, in watts
	 */
	public synchronized final void setMaxPowerRateDischarge(double maxPowerRateDischarge) {
		this.maxPowerRateDischarge = (maxPowerRateDischarge > 0 ? -maxPowerRateDischarge
				: maxPowerRateDischarge);
		setTargetPowerRate(targetPowerRate); // enforce new max
	}

}
