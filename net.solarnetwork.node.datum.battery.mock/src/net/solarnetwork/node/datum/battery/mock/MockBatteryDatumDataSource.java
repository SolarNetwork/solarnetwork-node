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
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.time.Instant;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
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
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Simple simulated battery.
 *
 * <p>
 * This class also implements {@link NodeControlProvider} and can expose two
 * controls:
 *
 * <ul>
 * <li><b>target power rate</b> - the {@code targetPowerRateControlId}</li>
 * <li><b>SOC</b> - the {@code socControlId}</li>
 * <li>
 *
 * <p>
 * Both controls can be set with the
 * {@link InstructionHandler#TOPIC_SET_CONTROL_PARAMETER} topic. The target
 * power rate control manages the {@code targetPowerRate} value, while the SOC
 * control manages the {@code availableEnergy} value, as a percentage of the
 * {@code energyCapacity}. The SOC control is really meant for testing purposes
 * rather than simulation.
 * </p>
 *
 * @author matt
 * @version 1.3
 */
public class MockBatteryDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider, NodeControlProvider, InstructionHandler {

	/** A datum property name for the power rate. */
	public static final String POWER_RATE_PROP = "powerRate";

	/** A default maximum power rate value. */
	public static final double DEFAULT_MAX_POWER_RATE = 1000.0;

	/** The {@code energyCapacity} property default value. */
	public static final double DEFAULT_ENERGY_CAPACITY = 10_000.0;

	/** The {@code initialSoc} property default value. */
	public static final double DEFAULT_INITIAL_SOC = 100.0;

	private @Nullable OptionalService<EventAdmin> eventAdmin;
	private @Nullable String sourceId;
	private @Nullable String targetPowerRateControlId;
	private @Nullable String socControlId;
	private double energyCapacity = DEFAULT_ENERGY_CAPACITY;
	private double maxPowerRateCharge = DEFAULT_MAX_POWER_RATE;
	private double maxPowerRateDischarge = -DEFAULT_MAX_POWER_RATE;
	private double initialSoc = DEFAULT_INITIAL_SOC;

	private double targetPowerRate = 0.0;
	private double availableEnergy = DEFAULT_ENERGY_CAPACITY;

	private @Nullable SimpleEnergyStorageDatum lastDatum;

	/**
	 * Constructor.
	 */
	public MockBatteryDatumDataSource() {
		this(InstantSource.system());
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public MockBatteryDatumDataSource(InstantSource clock) {
		super(clock);
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return EnergyStorageDatum.class;
	}

	@Override
	public @Nullable EnergyStorageDatum readCurrentDatum() {
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
		final String socControlId = resolvePlaceholders(this.socControlId, null);
		if ( (targetPowerRateControlId == null || targetPowerRateControlId.isEmpty())
				&& (socControlId == null || socControlId.isEmpty()) ) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>(2);
		if ( targetPowerRateControlId != null && !targetPowerRateControlId.isEmpty() ) {
			result.add(targetPowerRateControlId);
		}
		if ( socControlId != null && !socControlId.isEmpty() ) {
			result.add(socControlId);
		}
		return result;
	}

	@Override
	public @Nullable NodeControlInfo getCurrentControlInfo(String controlId) {
		final String targetPowerRateControlId = resolvePlaceholders(this.targetPowerRateControlId, null);
		if ( targetPowerRateControlId != null && targetPowerRateControlId.equals(controlId) ) {
			EnergyStorageDatum d = nonnull(readCurrentDatum(), "Datum");
			return newSimpleNodeControlInfoDatum(targetPowerRateControlId, NodeControlPropertyType.Float,
					"W", nonnull(d.asSampleOperations().getSampleString(DatumSamplesType.Instantaneous,
							POWER_RATE_PROP), "Power rate"));
		}
		final String socControlId = resolvePlaceholders(this.socControlId, null);
		if ( socControlId != null && socControlId.equals(controlId) ) {
			EnergyStorageDatum d = nonnull(readCurrentDatum(), "Datum");
			return newSimpleNodeControlInfoDatum(socControlId, NodeControlPropertyType.Percent, null,
					nonnull(d.asSampleOperations().getSampleString(DatumSamplesType.Instantaneous,
							EnergyStorageDatum.STATE_OF_CHARGE_PERCENTAGE_KEY), "SOC"));
		}
		return null;
	}

	private SimpleNodeControlInfoDatum newSimpleNodeControlInfoDatum(String controlId,
			NodeControlPropertyType type, @Nullable String unit, String value) {
		// @formatter:off
		NodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(controlId)
				.withType(type)
				.withReadonly(false)
				.withUnit(unit)
				.withValue(value)
				.build();
		// @formatter:on
		return new SimpleNodeControlInfoDatum(info, getClock().instant());
	}

	@Override
	public boolean handlesTopic(@Nullable String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public synchronized @Nullable InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		// look for a parameter name that matches a control ID
		final String targetPowerRateControlId = resolvePlaceholders(this.targetPowerRateControlId, null);
		final String socControlId = resolvePlaceholders(this.socControlId, null);
		InstructionState result = null;
		for ( String paramName : instruction.getParameterNames() ) {
			if ( targetPowerRateControlId != null && targetPowerRateControlId.equals(paramName) ) {
				Number newValue = StringUtils.numberValue(instruction.getParameterValue(paramName));
				if ( newValue != null && updateTargetPowerRate(newValue) ) {
					result = InstructionState.Completed;
				} else {
					result = InstructionState.Declined;
				}
			} else if ( socControlId != null && socControlId.equals(paramName) ) {
				Number newValue = StringUtils.numberValue(instruction.getParameterValue(paramName));
				if ( newValue != null && updateSoc(newValue) ) {
					result = InstructionState.Completed;
				} else {
					result = InstructionState.Declined;
				}
			}
		}
		return (result != null ? InstructionUtils.createStatus(instruction, result) : null);
	}

	private synchronized boolean updateTargetPowerRate(Number newValue) {
		if ( newValue == null ) {
			return false;
		}
		final double oldValue = this.targetPowerRate;

		setTargetPowerRate(newValue.doubleValue());

		// if the value changed, post EVENT_TOPIC_CONTROL_INFO_CHANGED
		final double updatedValue = this.targetPowerRate;
		final String controlId = resolvePlaceholders(this.targetPowerRateControlId);
		if ( controlId != null && Double.compare(oldValue, updatedValue) != 0 ) {
			SimpleNodeControlInfoDatum info = newSimpleNodeControlInfoDatum(controlId,
					NodeControlPropertyType.Float, "W", String.valueOf((int) updatedValue));
			postControlEvent(info, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
		}
		return true;
	}

	/**
	 * Update the state of charge, as a percentage between 0 - 100.
	 *
	 * @param newValued
	 *        the state of charge to set, between 0 and 100
	 * @return {@code true} if {@code newValue} is not {@code null}
	 */
	private synchronized boolean updateSoc(Number newValue) {
		if ( newValue == null ) {
			return false;
		}
		final double soc = Math.max(0, Math.min(1.0, newValue.doubleValue() / 100.0));
		final double newAvailEnergy = soc * energyCapacity;
		final double oldAvailEnergy = this.availableEnergy;
		this.availableEnergy = newAvailEnergy;

		// if the value changed, post EVENT_TOPIC_CONTROL_INFO_CHANGED
		final String controlId = resolvePlaceholders(this.socControlId);
		if ( controlId != null && Double.compare(newAvailEnergy, oldAvailEnergy) != 0 ) {
			SimpleNodeControlInfoDatum info = newSimpleNodeControlInfoDatum(controlId,
					NodeControlPropertyType.Percent, null, String.valueOf((float) soc * 100.0f));
			postControlEvent(info, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
		}

		return true;
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		final EventAdmin admin = service(eventAdmin);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		admin.postEvent(event);
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
		result.add(new BasicTextFieldSettingSpecifier("socControlId", null));
		result.add(new BasicTextFieldSettingSpecifier("energyCapacity",
				String.valueOf(DEFAULT_ENERGY_CAPACITY)));
		result.add(new BasicTextFieldSettingSpecifier("maxPowerRateCharge",
				String.valueOf(DEFAULT_MAX_POWER_RATE)));
		result.add(new BasicTextFieldSettingSpecifier("maxPowerRateDischarge",
				String.valueOf(-DEFAULT_MAX_POWER_RATE)));
		result.add(
				new BasicTextFieldSettingSpecifier("initialSoc", String.valueOf(DEFAULT_INITIAL_SOC)));

		return result;
	}

	private synchronized String statusMessage(Locale locale) {
		final double currentCapacity = this.availableEnergy;
		final double totalCapacity = this.energyCapacity;
		final int rate = (int) this.targetPowerRate;
		final int soc = (int) ((currentCapacity / totalCapacity) * 100.0);
		return messageSource().getMessage("status.msg",
				new Object[] { soc, (int) currentCapacity, rate }, locale);
	}

	private synchronized SimpleEnergyStorageDatum sample() {
		final Instant now = getClock().instant();

		double availCapacity = this.availableEnergy;
		final double totalCapacity = this.energyCapacity;
		final double rate = (int) this.targetPowerRate;
		final double soc = ((availCapacity / totalCapacity) * 100.0);

		final DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue(POWER_RATE_PROP, (int) rate);

		if ( rate != 0 && lastDatum != null ) {
			double hoursDiff = (double) nonnull(lastDatum.getTimestamp(), "Datum timestamp").until(now,
					ChronoUnit.MILLIS) / TimeUnit.HOURS.toMillis(1);
			double newCapacity = Math.max(0.0,
					Math.min(totalCapacity, availCapacity + hoursDiff * rate));
			if ( newCapacity != availCapacity ) {
				this.availableEnergy = newCapacity;
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
	public final @Nullable String getSourceId() {
		return sourceId;
	}

	/**
	 * Set source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the control ID to use for the target power rate.
	 *
	 * @return the control ID
	 */
	public final @Nullable String getTargetPowerRateControlId() {
		return targetPowerRateControlId;
	}

	/**
	 * Set the control ID to use for the target power rate.
	 *
	 * @param targetPowerRateControlId
	 *        the control ID to set
	 */
	public final void setTargetPowerRateControlId(@Nullable String targetPowerRateControlId) {
		this.targetPowerRateControlId = targetPowerRateControlId;
	}

	/**
	 * Get the control ID to use for the state of charge.
	 *
	 * @return the control ID
	 */
	public final @Nullable String getSocControlId() {
		return socControlId;
	}

	/**
	 * Set the control ID to use for the state of charge.
	 *
	 * @param socControlId
	 *        the control ID to set
	 */
	public final void setSocControlId(@Nullable String socControlId) {
		this.socControlId = socControlId;
	}

	/**
	 * Get the starting state of charge.
	 *
	 * @return the starting state of charge, as a percentage from 0 - 100;
	 *         defaults to {@link #DEFAULT_INITIAL_SOC}
	 */
	public final double getInitialSoc() {
		return initialSoc;
	}

	/**
	 * Set the starting state of charge.
	 *
	 * @param initialSoc
	 *        the starting state of charge to set, as a percentage from 0 - 100
	 */
	public final synchronized void setInitialSoc(double initialSoc) {
		this.initialSoc = initialSoc;
		if ( lastDatum == null ) {
			updateSoc(initialSoc);
		}
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
		if ( this.energyCapacity < this.availableEnergy ) {
			this.availableEnergy = this.energyCapacity;
		}
		if ( lastDatum == null ) {
			updateSoc(initialSoc);
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

	/**
	 * Get the event admin service.
	 *
	 * @return the event admin service
	 */
	public final @Nullable OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set the event admin sevice.
	 *
	 * @param eventAdmin
	 *        the service to set
	 * @since 1.3
	 */
	public final void setEventAdmin(@Nullable OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}
}
