/* ==================================================================
 * ControlGroup.java - 4 Nov 2026 4:16:45 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.group;

import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.node.reactor.InstructionUtils.createErrorResultParameters;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.StringUtils;

/**
 * A virtual control that operates on a set of other controls, making a group of
 * controls act as one.
 *
 * <p>
 * This can be useful in situations where a set of similar hardware devices need
 * to be controlled together, such as a set of switches that need to be toggled
 * together as if they were a single switch, or a group of devices need to
 * "split" some control value, such as a site-wide active power constraint split
 * across multiple inverters.
 * </p>
 *
 * <p>
 * When an update to the group completes, a
 * {@link NodeControlProvider#EVENT_TOPIC_CONTROL_INFO_CHANGED} event is posted.
 * The event's value is what the coordination strategy combines from the values
 * sent to the members, which can differ from the requested value because of
 * rounding.
 * </p>
 *
 * <p>
 * A member control can be another control group. When a member control refers
 * back to the group, directly or through other groups, the group declines an
 * update that comes back to it, and gives no value to a read that comes back to
 * it, so neither loops forever.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class ControlGroup extends BaseIdentifiable
		implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler {

	/** The setting UID. */
	public static final String SETTING_UID = "net.solarnetwork.node.control.group.ControlGroup";

	/** The {@code controlPropertyType} property default value. */
	public static final NodeControlPropertyType DEFAULT_CONTROL_PROPERTY_TYPE = NodeControlPropertyType.Float;

	/** The {@code instructionTopics} property default value. */
	public static final Set<String> DEFAULT_INSTRUCTION_TOPICS = Collections.unmodifiableSet(
			new LinkedHashSet<>(List.of(TOPIC_SET_CONTROL_PARAMETER, TOPIC_DEMAND_BALANCE)));

	private final InstantSource clock;
	private final OptionalFilterableService<ControlGroupCoordinationStrategy> coordinationStrategy;
	private final OptionalFilterableService<ControlGroupConsensusStrategy> consensusStrategy;
	private final Collection<NodeControlProvider> controls;
	private final OptionalService<InstructionExecutionService> instructionService;
	private final OptionalService<EventAdmin> eventAdmin;
	private @Nullable String controlId;
	private NodeControlPropertyType controlPropertyType = DEFAULT_CONTROL_PROPERTY_TYPE;
	private @Nullable String controlPropertyName;
	private @Nullable String controlUnit;
	private boolean controlReadOnly;
	private Set<String> instructionTopics = DEFAULT_INSTRUCTION_TOPICS;
	private MemberControlConfig @Nullable [] memberConfigs;

	// set while this group updates or reads a member on a thread, so a member control that refers
	// back to this group stops instead of looping
	private final ThreadLocal<Boolean> updatingMembers = new ThreadLocal<>();
	private final ThreadLocal<Boolean> readingMembers = new ThreadLocal<>();

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 *
	 * @param controls
	 *        all available controls
	 * @param instructionService
	 *        the instruction service
	 * @param eventAdmin
	 *        the event admin
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ControlGroup(Collection<NodeControlProvider> controls,
			OptionalService<InstructionExecutionService> instructionService,
			OptionalService<EventAdmin> eventAdmin) {
		this(InstantSource.system(),
				new StaticOptionalService<>(SimpleControlGroupCoordinationStrategy.INSTANCE),
				new StaticOptionalService<>(AllOrNothingControlGroupConsensusStrategy.INSTANCE),
				controls, instructionService, eventAdmin);
	}

	/**
	 * Constructor.
	 *
	 * @param coordinationStrategy
	 *        the coordination strategy
	 * @param consensusStrategy
	 *        the consensus strategy
	 * @param controls
	 *        all available controls
	 * @param instructionService
	 *        the instruction service
	 * @param eventAdmin
	 *        the event admin
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ControlGroup(OptionalFilterableService<ControlGroupCoordinationStrategy> coordinationStrategy,
			OptionalFilterableService<ControlGroupConsensusStrategy> consensusStrategy,
			Collection<NodeControlProvider> controls,
			OptionalService<InstructionExecutionService> instructionService,
			OptionalService<EventAdmin> eventAdmin) {
		this(InstantSource.system(), coordinationStrategy, consensusStrategy, controls,
				instructionService, eventAdmin);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param coordinationStrategy
	 *        the coordination strategy
	 * @param consensusStrategy
	 *        the consensus strategy
	 * @param controls
	 *        all available controls
	 * @param instructionService
	 *        the instruction service
	 * @param eventAdmin
	 *        the event admin
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ControlGroup(InstantSource clock,
			OptionalFilterableService<ControlGroupCoordinationStrategy> coordinationStrategy,
			OptionalFilterableService<ControlGroupConsensusStrategy> consensusStrategy,
			Collection<NodeControlProvider> controls,
			OptionalService<InstructionExecutionService> instructionService,
			OptionalService<EventAdmin> eventAdmin) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.coordinationStrategy = requireNonNullArgument(coordinationStrategy, "coordinationStrategy");
		this.consensusStrategy = requireNonNullArgument(consensusStrategy, "consensusStrategy");
		this.controls = requireNonNullArgument(controls, "controls");
		this.instructionService = requireNonNullArgument(instructionService, "instructionService");
		this.eventAdmin = requireNonNullArgument(eventAdmin, "eventAdmin");
		setDisplayName("Control Group");
	}

	@Override
	public boolean handlesTopic(@Nullable String topic) {
		return (topic != null && instructionTopics.contains(topic));
	}

	@Override
	public synchronized @Nullable InstructionStatus processInstruction(Instruction instruction) {
		final String controlId = controlId(instruction != null ? instruction.getParameterMap() : null);
		if ( instruction == null || !handlesTopic(instruction.getTopic()) || controlId == null ) {
			return null;
		}

		final String paramValue = instruction.getParameterValue(controlId);
		if ( paramValue == null ) {
			return null;
		} else if ( controlReadOnly ) {
			return declined(instruction, controlId, paramValue, messageSource().getMessage(
					"error.update.controlReadOnly", null, "Control is read only.", Locale.getDefault()));
		} else if ( Boolean.TRUE.equals(updatingMembers.get()) ) {
			// this group is updating a member, which refers back to it
			return declined(instruction, controlId, paramValue,
					messageSource().getMessage("error.update.loop", null,
							"Member control refers back to group.", Locale.getDefault()));
		}

		final NodeControlInfo group = groupInfo(controlId, null);
		final ControlGroupCoordinationStrategy coordStrategy = service(coordinationStrategy);
		if ( coordStrategy == null ) {
			final String coordStrategyUid = nonEmptyString(getCoordinationStrategyUid());
			if ( coordStrategyUid == null ) {
				// not configured yet
				return declined(instruction, controlId, paramValue,
						messageSource().getMessage("error.coordinationStrategyNotConfigured", null,
								"Coordination Strategy not configured.", Locale.getDefault()));
			}
			// configured service not available
			return declined(instruction, controlId, paramValue, messageSource().getMessage(
					"error.coordinationStrategyNotAvailable", new Object[] { coordStrategyUid },
					"Configured Coordination Strategy [{0}] not available.", Locale.getDefault()));
		}
		final ControlGroupConsensusStrategy consensStrategy = service(consensusStrategy);
		if ( consensStrategy == null ) {
			final String consensStrategyUid = nonEmptyString(getConsensusStrategyUid());
			if ( consensStrategyUid == null ) {
				// not configured yet
				return declined(instruction, controlId, paramValue,
						messageSource().getMessage("error.consensusStrategyNotConfigured", null,
								"Consensus Strategy not configured.", Locale.getDefault()));
			}
			// configured service not available
			return declined(instruction, controlId, paramValue,
					messageSource().getMessage("error.consensusStrategyNotAvailable",
							new Object[] { consensStrategyUid },
							"Configured Consensus Strategy [{0}] not available.", Locale.getDefault()));
		}
		final List<MemberControlConfig> members = resolvedMemberConfigs(controlId);
		try {
			final Map<String, Instruction> instructions = coordStrategy.distribute(group, members,
					instruction, controls);
			if ( instructions == null ) {
				return declined(instruction, controlId, paramValue,
						messageSource().getMessage("error.update.notDistributed", null,
								"Update was not distributed.", Locale.getDefault()));
			}

			final InstructionExecutionService exec = service(getInstructionService());
			if ( exec == null ) {
				return declined(instruction, controlId, paramValue,
						messageSource().getMessage("error.update.noExecutionService", null,
								"No InstructionExecutionService available.", Locale.getDefault()));
			}

			final Map<String, InstructionExecution> memberStatuses = new LinkedHashMap<>(
					instructions.size());
			for ( Entry<String, Instruction> e : instructions.entrySet() ) {
				final String memberControlId = e.getKey();
				final Instruction instr = e.getValue();
				InstructionStatus status;
				try {
					status = executeMember(exec, instr);
				} catch ( Exception ex ) {
					// treat as declined so the remaining members are still updated
					log.warn("Control group [{}] error updating member control [{}]: {}", controlId,
							memberControlId, ex.getMessage());
					status = createStatus(instr, Declined,
							createErrorResultParameters(ex.getMessage(), null));
				}
				memberStatuses.put(memberControlId, new InstructionExecution(instr, status));
			}
			final InstructionStatus result = consensStrategy.evaluate(group, members, instruction,
					memberStatuses);
			if ( result != null && result.getInstructionState() == Completed ) {
				postControlChangedEvent(coordStrategy, group, members, instructions);
			} else if ( result != null && result.getInstructionState() == Declined ) {
				final Map<String, ?> params = result.getResultParameters();
				log.warn("Control group [{}] update to [{}] declined: {}", controlId, paramValue,
						params != null ? params.get(InstructionStatus.MESSAGE_RESULT_PARAM) : null);
			}
			return result;
		} catch ( Exception e ) {
			log.error("Control group [{}] error processing instruction {}: {}", controlId,
					instruction.getIdentifier(), e.getMessage(), e);
			return createStatus(instruction, Declined,
					createErrorResultParameters(messageSource().getMessage("error.update.exception",
							new Object[] { e.getMessage() }, "Exception while processing.",
							Locale.getDefault()), null));
		}
	}

	private InstructionStatus declined(Instruction instruction, String controlId, String value,
			@Nullable String message) {
		log.warn("Control group [{}] update to [{}] declined: {}", controlId, value, message);
		return createStatus(instruction, Declined, createErrorResultParameters(message, null));
	}

	/**
	 * Execute a member control instruction.
	 *
	 * <p>
	 * While it runs, an update to this group on the same thread is declined, as
	 * that means the member control refers back to this group.
	 * </p>
	 *
	 * @param exec
	 *        the execution service
	 * @param instruction
	 *        the member control instruction
	 * @return the status, or {@code null} if the instruction was not handled
	 */
	private @Nullable InstructionStatus executeMember(InstructionExecutionService exec,
			Instruction instruction) {
		updatingMembers.set(Boolean.TRUE);
		try {
			return exec.executeInstruction(instruction);
		} finally {
			updatingMembers.remove();
		}
	}

	/**
	 * Read a member control value.
	 *
	 * <p>
	 * While it runs, a read of this group on the same thread gets no value, as
	 * that means the member control refers back to this group.
	 * </p>
	 *
	 * @param control
	 *        the control provider
	 * @param memberControlId
	 *        the member control ID
	 * @return the value, or {@code null} if not available
	 */
	private @Nullable NodeControlInfo readMember(NodeControlProvider control, String memberControlId) {
		readingMembers.set(Boolean.TRUE);
		try {
			return control.getCurrentControlInfo(memberControlId);
		} finally {
			readingMembers.remove();
		}
	}

	private @Nullable String controlId(@Nullable Map<String, ?> parameters) {
		return nonEmptyString(resolvePlaceholders(getControlId(), parameters));
	}

	private NodeControlInfo groupInfo(String controlId, @Nullable String value) {
		// @formatter:off
		return BasicNodeControlInfo.builder()
				.withControlId(controlId)
				.withPropertyName(controlPropertyName)
				.withReadonly(controlReadOnly)
				.withType(controlPropertyType)
				.withUnit(controlUnit)
				.withValue(value)
				.build()
				;
		// @formatter:on
	}

	@Override
	public List<String> getAvailableControlIds() {
		final String controlId = controlId(null);
		return controlId == null ? Collections.emptyList() : Collections.singletonList(controlId);
	}

	@Override
	public @Nullable NodeControlInfo getCurrentControlInfo(String id) {
		final String controlId = controlId(null);
		if ( controlId == null || !controlId.equals(id) ) {
			return null;
		} else if ( Boolean.TRUE.equals(readingMembers.get()) ) {
			// this group is reading a member, which refers back to it
			log.warn("Control group [{}] not read: a member control refers back to the group.",
					controlId);
			return null;
		}

		final ControlGroupCoordinationStrategy coordStrategy = service(coordinationStrategy);
		if ( coordStrategy == null ) {
			return null;
		}
		final List<MemberControlConfig> members = resolvedMemberConfigs(controlId);

		final Set<String> memberControlIds = new LinkedHashSet<>(members.size());
		for ( MemberControlConfig member : members ) {
			memberControlIds.add(member.controlId());
		}

		final List<NodeControlInfo> memberValues = new ArrayList<>(members.size());
		CONTROLS: for ( NodeControlProvider control : controls ) {
			for ( String aControlId : control.getAvailableControlIds() ) {
				if ( memberControlIds.remove(aControlId) ) {
					NodeControlInfo memberValue = readMember(control, aControlId);
					if ( memberValue != null ) {
						memberValues.add(memberValue);
					}
					if ( memberControlIds.isEmpty() ) {
						break CONTROLS;
					}
				}
			}
		}

		final NodeControlInfo info = coordStrategy.combine(groupInfo(controlId, null), members,
				memberValues);

		SimpleNodeControlInfoDatum result = new SimpleNodeControlInfoDatum(info, clock.instant());
		postControlEvent(result, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
		return result;
	}

	/**
	 * Get the valid member configurations, with placeholders resolved in their
	 * control IDs.
	 *
	 * <p>
	 * Strategies are given these configurations, so they do not need to resolve
	 * placeholders themselves. A configuration is only copied if resolving
	 * placeholders changes its control ID.
	 * </p>
	 *
	 * @param groupControlId
	 *        the resolved group control ID; a member that resolves to this is
	 *        excluded, to avoid recursing back into this group
	 * @return the member configurations, never {@code null}
	 */
	private List<MemberControlConfig> resolvedMemberConfigs(String groupControlId) {
		final MemberControlConfig[] configs = getMemberConfigs();
		if ( configs == null ) {
			return List.of();
		}
		final List<MemberControlConfig> result = new ArrayList<>(configs.length);
		for ( MemberControlConfig config : configs ) {
			if ( !config.isValid() ) {
				continue;
			}
			final String memberControlId = config.controlId();
			final String resolvedControlId = nonEmptyString(resolvePlaceholders(memberControlId));
			if ( resolvedControlId == null || groupControlId.equals(resolvedControlId) ) {
				continue;
			}
			result.add(resolvedControlId.equals(memberControlId) ? config
					: config.withControlId(resolvedControlId));
		}
		return result;
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		log.debug("Posting [{}] event with {}", topic, info);
		admin.postEvent(event);
	}

	private void postControlChangedEvent(ControlGroupCoordinationStrategy strategy,
			NodeControlInfo group, List<MemberControlConfig> members,
			Map<String, Instruction> instructions) {
		try {
			// combine the distributed member values, so the group value reflects what was applied
			final List<NodeControlInfo> memberValues = new ArrayList<>(instructions.size());
			for ( Entry<String, Instruction> e : instructions.entrySet() ) {
				final String memberControlId = e.getKey();
				// @formatter:off
				memberValues.add(BasicNodeControlInfo.builder()
						.withControlId(memberControlId)
						.withValue(e.getValue().getParameterValue(memberControlId))
						.build());
				// @formatter:on
			}
			final NodeControlInfo info = strategy.combine(group, members, memberValues);
			if ( log.isInfoEnabled() ) {
				log.info("Control group [{}] updated to [{}]; member controls updated as: [{}]",
						group.getControlId(), info.getValue(),
						instructions.entrySet().stream().map(
								e -> e.getKey() + " -> " + e.getValue().getParameterValue(e.getKey()))
								.collect(Collectors.joining("\n\t", "\n\t", "\n")));
			}
			postControlEvent(new SimpleNodeControlInfoDatum(info, clock.instant()),
					NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
		} catch ( Exception ex ) {
			// the update itself succeeded, so do not fail because of the event
			log.warn("Control group [{}] error posting control changed event: {}", group.getControlId(),
					ex.getMessage(), ex);
		}
	}

	// Settings

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		final List<SettingSpecifier> results = new ArrayList<>(8);

		results.addAll(basicIdentifiableSettings());
		results.add(new BasicTextFieldSettingSpecifier("controlId", null));

		// drop-down menu for controlPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				"controlPropertyTypeKey", String.valueOf(DEFAULT_CONTROL_PROPERTY_TYPE.getKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( NodeControlPropertyType e : NodeControlPropertyType.values() ) {
			propTypeTitles.put(String.valueOf(e.getKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier("controlPropertyName", null));
		results.add(new BasicTextFieldSettingSpecifier("controlUnit", null));
		results.add(new BasicToggleSettingSpecifier("controlReadOnly", false));
		results.add(new BasicTextFieldSettingSpecifier("instructionTopicsValue",
				commaDelimitedStringFromCollection(DEFAULT_INSTRUCTION_TOPICS)));
		results.add(new BasicTextFieldSettingSpecifier("coordinationStrategyUid", null, false,
				"(objectClass=net.solarnetwork.node.control.group.ControlGroupCoordinationStrategy)"));
		results.add(new BasicTextFieldSettingSpecifier("consensusStrategyUid", null, false,
				"(objectClass=net.solarnetwork.node.control.group.ControlGroupConsensusStrategy)"));

		// member configurations dynamic list
		final MemberControlConfig[] confs = getMemberConfigs();
		List<MemberControlConfig> confsList = (confs != null ? List.of(confs) : List.of());
		results.add(SettingUtils.dynamicListSettingSpecifier("memberConfigs", confsList,
				(@Nullable MemberControlConfig value, int index, String key) -> {
					var configGroup = new BasicGroupSettingSpecifier(
							MemberControlConfig.settings(key + "."));
					return List.of(configGroup);
				}));

		return results;
	}

	/**
	 * Get the event admin.
	 *
	 * @return the event admin
	 */
	public final OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Get the controls to manage.
	 *
	 * @return the controls to manage
	 */
	public final Collection<NodeControlProvider> getControls() {
		return controls;
	}

	/**
	 * Get the instruction execution service.
	 *
	 * @return the service
	 */
	public final OptionalService<InstructionExecutionService> getInstructionService() {
		return instructionService;
	}

	/**
	 * Get the coordination coordination strategy.
	 *
	 * @return the coordination strategy
	 */
	public final OptionalFilterableService<ControlGroupCoordinationStrategy> getCoordinationStrategy() {
		return coordinationStrategy;
	}

	/**
	 * Get the consensus strategy.
	 *
	 * @return the consensusS strategy
	 */
	public final OptionalFilterableService<ControlGroupConsensusStrategy> getConsensusStrategy() {
		return consensusStrategy;
	}

	/**
	 * Get the control ID.
	 *
	 * @return the control ID
	 */
	public final @Nullable String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 *
	 * @param controlId
	 *        the controlId to set
	 */
	public final void setControlId(@Nullable String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the control property type.
	 *
	 * @return the controlPropertyType
	 */
	public final NodeControlPropertyType getControlPropertyType() {
		return controlPropertyType;
	}

	/**
	 * Set the control property type.
	 *
	 * @param controlPropertyType
	 *        the controlPropertyType to set
	 */
	public final void setControlPropertyType(NodeControlPropertyType controlPropertyType) {
		if ( controlPropertyType == null ) {
			controlPropertyType = DEFAULT_CONTROL_PROPERTY_TYPE;
		}
		this.controlPropertyType = controlPropertyType;
	}

	/**
	 * Get the control property type key.
	 *
	 * <p>
	 * This returns the configured {@link #getControlPropertyType()}
	 * {@link NodeControlPropertyType#getKey()} value as a string.
	 * </p>
	 *
	 * @return the property type key
	 */
	public final String getControlPropertyTypeKey() {
		NodeControlPropertyType type = getControlPropertyType();
		return Character.toString(type.getKey());
	}

	/**
	 * Set the datum property type via a key value.
	 *
	 * <p>
	 * This uses the first character of {@code key} as a
	 * {@link NodeControlPropertyType} key value to call
	 * {@link #setControlPropertyType(NodeControlPropertyType)}. If {@code key}
	 * is not recognized, then {@link #DEFAULT_CONTROL_PROPERTY_TYPE} will be
	 * set instead.
	 * </p>
	 *
	 * @param key
	 *        the property type key to set
	 */
	public final void setControlPropertyTypeKey(String key) {
		if ( key == null || key.length() < 1 ) {
			return;
		}
		NodeControlPropertyType type;
		try {
			type = NodeControlPropertyType.forKey(key.charAt(0));
		} catch ( IllegalArgumentException e ) {
			type = DEFAULT_CONTROL_PROPERTY_TYPE;
		}
		setControlPropertyType(type);
	}

	/**
	 * Get the instruction topics to handle.
	 *
	 * @return the instruction topics to handle
	 */
	public final Set<String> getInstructionTopics() {
		return instructionTopics;
	}

	/**
	 * Set the instruction topics to handle.
	 *
	 * @param instructionTopics
	 *        the instruction topics to set; if {@code null} then
	 *        {@link #DEFAULT_INSTRUCTION_TOPICS} will be used
	 */
	public final void setInstructionTopics(@Nullable Set<String> instructionTopics) {
		this.instructionTopics = (instructionTopics != null ? instructionTopics
				: DEFAULT_INSTRUCTION_TOPICS);
	}

	/**
	 * Get the instruction topics to handle as a comma-delimited string.
	 *
	 * @return the instruction topics to handle
	 */
	public final String getInstructionTopicsValue() {
		return nonnull(StringUtils.commaDelimitedStringFromCollection(getInstructionTopics()), "Topics");
	}

	/**
	 * Set the instruction topics to handle as a comma-delimited string.
	 *
	 * @param instructionTopics
	 *        the instruction topics to set; if {@code null} then
	 *        {@link #DEFAULT_INSTRUCTION_TOPICS} will be used
	 */
	public final void setInstructionTopicsValue(@Nullable String instructionTopics) {
		setInstructionTopics(StringUtils.commaDelimitedStringToSet(instructionTopics));
	}

	/**
	 * Get the optional control property name.
	 *
	 * @return the name
	 */
	public final @Nullable String getControlPropertyName() {
		return controlPropertyName;
	}

	/**
	 * Set the optional control property name.
	 *
	 * @param controlPropertyName
	 *        the controlPropertyName to set
	 */
	public final void setControlPropertyName(@Nullable String controlPropertyName) {
		this.controlPropertyName = controlPropertyName;
	}

	/**
	 * Get the control unit of measurement.
	 *
	 * @return the control unit of measurement
	 */
	public final @Nullable String getControlUnit() {
		return controlUnit;
	}

	/**
	 * Set the control unit of measurement.
	 *
	 * @param controlUnit
	 *        the control unit of measurement to set
	 */
	public final void setControlUnit(@Nullable String controlUnit) {
		this.controlUnit = controlUnit;
	}

	/**
	 * Get the control read-only flag.
	 *
	 * @return the control read-only flag
	 */
	public final boolean isControlReadOnly() {
		return controlReadOnly;
	}

	/**
	 * Set the control read-only flag.
	 *
	 * @param controlReadOnly
	 *        the control read-only flag to set
	 */
	public final void setControlReadOnly(boolean controlReadOnly) {
		this.controlReadOnly = controlReadOnly;
	}

	/**
	 * Get the coordination strategy {@code uid} service filter property value.
	 *
	 * @return the UID filter property value
	 */
	public final @Nullable String getCoordinationStrategyUid() {
		return FilterableService.filterPropValue((FilterableService) coordinationStrategy,
				BaseIdentifiable.UID_PROPERTY);
	}

	/**
	 * Set the coordination strategy {@code uid} service filter property value.
	 *
	 * @param uid
	 *        the UID filter property value to set
	 */
	public final void setCoordinationStrategyUid(@Nullable String uid) {
		FilterableService.setFilterProp((FilterableService) coordinationStrategy,
				BaseIdentifiable.UID_PROPERTY, uid);
	}

	/**
	 * Get the consensus strategy {@code uid} service filter property value.
	 *
	 * @return the UID filter property value
	 */
	public final @Nullable String getConsensusStrategyUid() {
		return FilterableService.filterPropValue((FilterableService) consensusStrategy,
				BaseIdentifiable.UID_PROPERTY);
	}

	/**
	 * Set the consensus strategy {@code uid} service filter property value.
	 *
	 * @param uid
	 *        the UID filter property value to set
	 */
	public final void setConsensusStrategyUid(@Nullable String uid) {
		FilterableService.setFilterProp((FilterableService) consensusStrategy,
				BaseIdentifiable.UID_PROPERTY, uid);
	}

	/**
	 * Get the member configurations.
	 *
	 * @return the configurations
	 */
	public final MemberControlConfig @Nullable [] getMemberConfigs() {
		return memberConfigs;
	}

	/**
	 * Set the member configurations.
	 *
	 * @param memberConfigs
	 *        the configurations to set
	 */
	public final void setMemberConfigs(MemberControlConfig @Nullable [] memberConfigs) {
		this.memberConfigs = memberConfigs;
	}

	/**
	 * Get the number of configured {@code memberConfigs} elements.
	 *
	 * @return the number of {@code memberConfigs} elements
	 */
	public final int getMemberConfigsCount() {
		MemberControlConfig[] confs = this.memberConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code memberConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link MemberControlConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code memberConfigs} elements.
	 */
	public final void setMemberConfigsCount(int count) {
		this.memberConfigs = ArrayUtils.arrayWithLength(this.memberConfigs, count,
				MemberControlConfig.class, null);
	}

}
