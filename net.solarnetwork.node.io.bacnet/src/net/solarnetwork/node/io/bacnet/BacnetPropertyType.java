/* ==================================================================
 * BacnetPropertyType.java - 4/11/2022 4:51:32 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.bacnet;

import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.util.ObjectUtils;

/**
 * Enumeration of BACnet property identifiers.
 * 
 * @author matt
 * @version 1.0
 */
public enum BacnetPropertyType implements CodedValue {

	/** AckedTransitions. */
	AckedTransitions(0),

	/** AckRequired. */
	AckRequired(1),

	/** Action. */
	Action(2),

	/** ActionText. */
	ActionText(3),

	/** ActiveText. */
	ActiveText(4),

	/** ActiveVtSessions. */
	ActiveVtSessions(5),

	/** AlarmValue. */
	AlarmValue(6),

	/** AlarmValues. */
	AlarmValues(7),

	/** All. */
	All(8),

	/** AllWritesSuccessful. */
	AllWritesSuccessful(9),

	/** ApduSegmentTimeout. */
	ApduSegmentTimeout(10),

	/** ApduTimeout. */
	ApduTimeout(11),

	/** ApplicationSoftwareVersion. */
	ApplicationSoftwareVersion(12),

	/** Archive. */
	Archive(13),

	/** Bias. */
	Bias(14),

	/** ChangeOfStateCount. */
	ChangeOfStateCount(15),

	/** ChangeOfStateTime. */
	ChangeOfStateTime(16),

	/** NotificationClass. */
	NotificationClass(17),

	/** ControlledVariableReference. */
	ControlledVariableReference(19),

	/** ControlledVariableUnits. */
	ControlledVariableUnits(20),

	/** ControlledVariableValue. */
	ControlledVariableValue(21),

	/** CovIncrement. */
	CovIncrement(22),

	/** DateList. */
	DateList(23),

	/** DaylightSavingsStatus. */
	DaylightSavingsStatus(24),

	/** Deadband. */
	Deadband(25),

	/** DerivativeConstant. */
	DerivativeConstant(26),

	/** DerivativeConstantUnits. */
	DerivativeConstantUnits(27),

	/** Description. */
	Description(28),

	/** DescriptionOfHalt. */
	DescriptionOfHalt(29),

	/** DeviceAddressBinding. */
	DeviceAddressBinding(30),

	/** DeviceType. */
	DeviceType(31),

	/** EffectivePeriod. */
	EffectivePeriod(32),

	/** ElapsedActiveTime. */
	ElapsedActiveTime(33),

	/** ErrorLimit. */
	ErrorLimit(34),

	/** EventEnable. */
	EventEnable(35),

	/** EventState. */
	EventState(36),

	/** EventType. */
	EventType(37),

	/** ExceptionSchedule. */
	ExceptionSchedule(38),

	/** FaultValues. */
	FaultValues(39),

	/** FeedbackValue. */
	FeedbackValue(40),

	/** FileAccessMethod. */
	FileAccessMethod(41),

	/** FileSize. */
	FileSize(42),

	/** FileType. */
	FileType(43),

	/** FirmwareRevision. */
	FirmwareRevision(44),

	/** HighLimit. */
	HighLimit(45),

	/** InactiveText. */
	InactiveText(46),

	/** InProcess. */
	InProcess(47),

	/** InstanceOf. */
	InstanceOf(48),

	/** IntegralConstant. */
	IntegralConstant(49),

	/** IntegralConstantUnits. */
	IntegralConstantUnits(50),

	/** LimitEnable. */
	LimitEnable(52),

	/** ListOfGroupMembers. */
	ListOfGroupMembers(53),

	/** ListOfObjectPropertyReferences. */
	ListOfObjectPropertyReferences(54),

	/** LocalDate. */
	LocalDate(56),

	/** LocalTime. */
	LocalTime(57),

	/** Location. */
	Location(58),

	/** LowLimit. */
	LowLimit(59),

	/** ManipulatedVariableReference. */
	ManipulatedVariableReference(60),

	/** MaximumOutput. */
	MaximumOutput(61),

	/** MaxApduLengthAccepted. */
	MaxApduLengthAccepted(62),

	/** MaxInfoFrames. */
	MaxInfoFrames(63),

	/** MaxMaster. */
	MaxMaster(64),

	/** MaxPresValue. */
	MaxPresValue(65),

	/** MinimumOffTime. */
	MinimumOffTime(66),

	/** MinimumOnTime. */
	MinimumOnTime(67),

	/** MinimumOutput. */
	MinimumOutput(68),

	/** MinPresValue. */
	MinPresValue(69),

	/** ModelName. */
	ModelName(70),

	/** ModificationDate. */
	ModificationDate(71),

	/** NotifyType. */
	NotifyType(72),

	/** NumberOfApduRetries. */
	NumberOfApduRetries(73),

	/** NumberOfStates. */
	NumberOfStates(74),

	/** ObjectIdentifier. */
	ObjectIdentifier(75),

	/** ObjectList. */
	ObjectList(76),

	/** ObjectName. */
	ObjectName(77),

	/** ObjectPropertyReference. */
	ObjectPropertyReference(78),

	/** ObjectType. */
	ObjectType(79),

	/** Optional. */
	Optional(80),

	/** OutOfService. */
	OutOfService(81),

	/** OutputUnits. */
	OutputUnits(82),

	/** EventParameters. */
	EventParameters(83),

	/** Polarity. */
	Polarity(84),

	/** PresentValue. */
	PresentValue(85),

	/** Priority. */
	Priority(86),

	/** PriorityArray. */
	PriorityArray(87),

	/** PriorityForWriting. */
	PriorityForWriting(88),

	/** ProcessIdentifier. */
	ProcessIdentifier(89),

	/** ProgramChange. */
	ProgramChange(90),

	/** ProgramLocation. */
	ProgramLocation(91),

	/** ProgramState. */
	ProgramState(92),

	/** ProportionalConstant. */
	ProportionalConstant(93),

	/** ProportionalConstantUnits. */
	ProportionalConstantUnits(94),

	/** ProtocolObjectTypesSupported. */
	ProtocolObjectTypesSupported(96),

	/** ProtocolServicesSupported. */
	ProtocolServicesSupported(97),

	/** ProtocolVersion. */
	ProtocolVersion(98),

	/** ReadOnly. */
	ReadOnly(99),

	/** ReasonForHalt. */
	ReasonForHalt(100),

	/** RecipientList. */
	RecipientList(102),

	/** Reliability. */
	Reliability(103),

	/** RelinquishDefault. */
	RelinquishDefault(104),

	/** Required. */
	Required(105),

	/** Resolution. */
	Resolution(106),

	/** SegmentationSupported. */
	SegmentationSupported(107),

	/** Setpoint. */
	Setpoint(108),

	/** SetpointReference. */
	SetpointReference(109),

	/** StateText. */
	StateText(110),

	/** StatusFlags. */
	StatusFlags(111),

	/** SystemStatus. */
	SystemStatus(112),

	/** TimeDelay. */
	TimeDelay(113),

	/** TimeOfActiveTimeReset. */
	TimeOfActiveTimeReset(114),

	/** TimeOfStateCountReset. */
	TimeOfStateCountReset(115),

	/** TimeSynchronizationRecipients. */
	TimeSynchronizationRecipients(116),

	/** Units. */
	Units(117),

	/** UpdateInterval. */
	UpdateInterval(118),

	/** UtcOffset. */
	UtcOffset(119),

	/** VendorIdentifier. */
	VendorIdentifier(120),

	/** VendorName. */
	VendorName(121),

	/** VtClassesSupported. */
	VtClassesSupported(122),

	/** WeeklySchedule. */
	WeeklySchedule(123),

	/** AttemptedSamples. */
	AttemptedSamples(124),

	/** AverageValue. */
	AverageValue(125),

	/** BufferSize. */
	BufferSize(126),

	/** ClientCovIncrement. */
	ClientCovIncrement(127),

	/** CovResubscriptionInterval. */
	CovResubscriptionInterval(128),

	/** EventTimeStamps. */
	EventTimeStamps(130),

	/** LogBuffer. */
	LogBuffer(131),

	/** LogDeviceObjectProperty. */
	LogDeviceObjectProperty(132),

	/** Enable. */
	Enable(133),

	/** LogInterval. */
	LogInterval(134),

	/** MaximumValue. */
	MaximumValue(135),

	/** MinimumValue. */
	MinimumValue(136),

	/** NotificationThreshold. */
	NotificationThreshold(137),

	/** ProtocolRevision. */
	ProtocolRevision(139),

	/** RecordsSinceNotification. */
	RecordsSinceNotification(140),

	/** RecordCount. */
	RecordCount(141),

	/** StartTime. */
	StartTime(142),

	/** StopTime. */
	StopTime(143),

	/** StopWhenFull. */
	StopWhenFull(144),

	/** TotalRecordCount. */
	TotalRecordCount(145),

	/** ValidSamples. */
	ValidSamples(146),

	/** WindowInterval. */
	WindowInterval(147),

	/** WindowSamples. */
	WindowSamples(148),

	/** MaximumValueTimestamp. */
	MaximumValueTimestamp(149),

	/** MinimumValueTimestamp. */
	MinimumValueTimestamp(150),

	/** VarianceValue. */
	VarianceValue(151),

	/** ActiveCovSubscriptions. */
	ActiveCovSubscriptions(152),

	/** BackupFailureTimeout. */
	BackupFailureTimeout(153),

	/** ConfigurationFiles. */
	ConfigurationFiles(154),

	/** DatabaseRevision. */
	DatabaseRevision(155),

	/** DirectReading. */
	DirectReading(156),

	/** LastRestoreTime. */
	LastRestoreTime(157),

	/** MaintenanceRequired. */
	MaintenanceRequired(158),

	/** MemberOf. */
	MemberOf(159),

	/** Mode. */
	Mode(160),

	/** OperationExpected. */
	OperationExpected(161),

	/** Setting. */
	Setting(162),

	/** Silenced. */
	Silenced(163),

	/** TrackingValue. */
	TrackingValue(164),

	/** ZoneMembers. */
	ZoneMembers(165),

	/** LifeSafetyAlarmValues. */
	LifeSafetyAlarmValues(166),

	/** MaxSegmentsAccepted. */
	MaxSegmentsAccepted(167),

	/** ProfileName. */
	ProfileName(168),

	/** AutoSlaveDiscovery. */
	AutoSlaveDiscovery(169),

	/** ManualSlaveAddressBinding. */
	ManualSlaveAddressBinding(170),

	/** SlaveAddressBinding. */
	SlaveAddressBinding(171),

	/** SlaveProxyEnable. */
	SlaveProxyEnable(172),

	/** LastNotifyRecord. */
	LastNotifyRecord(173),

	/** ScheduleDefault. */
	ScheduleDefault(174),

	/** AcceptedModes. */
	AcceptedModes(175),

	/** AdjustValue. */
	AdjustValue(176),

	/** Count. */
	Count(177),

	/** CountBeforeChange. */
	CountBeforeChange(178),

	/** CountChangeTime. */
	CountChangeTime(179),

	/** CovPeriod. */
	CovPeriod(180),

	/** InputReference. */
	InputReference(181),

	/** LimitMonitoringInterval. */
	LimitMonitoringInterval(182),

	/** LoggingObject. */
	LoggingObject(183),

	/** LoggingRecord. */
	LoggingRecord(184),

	/** Prescale. */
	Prescale(185),

	/** PulseRate. */
	PulseRate(186),

	/** Scale. */
	Scale(187),

	/** ScaleFactor. */
	ScaleFactor(188),

	/** UpdateTime. */
	UpdateTime(189),

	/** ValueBeforeChange. */
	ValueBeforeChange(190),

	/** ValueSet. */
	ValueSet(191),

	/** ValueChangeTime. */
	ValueChangeTime(192),

	/** AlignIntervals. */
	AlignIntervals(193),

	/** IntervalOffset. */
	IntervalOffset(195),

	/** LastRestartReason. */
	LastRestartReason(196),

	/** LoggingType. */
	LoggingType(197),

	/** RestartNotificationRecipients. */
	RestartNotificationRecipients(202),

	/** TimeOfDeviceRestart. */
	TimeOfDeviceRestart(203),

	/** TimeSynchronizationInterval. */
	TimeSynchronizationInterval(204),

	/** Trigger. */
	Trigger(205),

	/** UtcTimeSynchronizationRecipients. */
	UtcTimeSynchronizationRecipients(206),

	/** NodeSubtype. */
	NodeSubtype(207),

	/** NodeType. */
	NodeType(208),

	/** StructuredObjectList. */
	StructuredObjectList(209),

	/** SubordinateAnnotations. */
	SubordinateAnnotations(210),

	/** SubordinateList. */
	SubordinateList(211),

	/** ActualShedLevel. */
	ActualShedLevel(212),

	/** DutyWindow. */
	DutyWindow(213),

	/** ExpectedShedLevel. */
	ExpectedShedLevel(214),

	/** FullDutyBaseline. */
	FullDutyBaseline(215),

	/** RequestedShedLevel. */
	RequestedShedLevel(218),

	/** ShedDuration. */
	ShedDuration(219),

	/** ShedLevelDescriptions. */
	ShedLevelDescriptions(220),

	/** ShedLevels. */
	ShedLevels(221),

	/** StateDescription. */
	StateDescription(222),

	/** DoorAlarmState. */
	DoorAlarmState(226),

	/** DoorExtendedPulseTime. */
	DoorExtendedPulseTime(227),

	/** DoorMembers. */
	DoorMembers(228),

	/** DoorOpenTooLongTime. */
	DoorOpenTooLongTime(229),

	/** DoorPulseTime. */
	DoorPulseTime(230),

	/** DoorStatus. */
	DoorStatus(231),

	/** DoorUnlockDelayTime. */
	DoorUnlockDelayTime(232),

	/** LockStatus. */
	LockStatus(233),

	/** MaskedAlarmValues. */
	MaskedAlarmValues(234),

	/** SecuredStatus. */
	SecuredStatus(235),

	/** AbsenteeLimit. */
	AbsenteeLimit(244),

	/** AccessAlarmEvents. */
	AccessAlarmEvents(245),

	/** AccessDoors. */
	AccessDoors(246),

	/** AccessEvent. */
	AccessEvent(247),

	/** AccessEventAuthenticationFactor. */
	AccessEventAuthenticationFactor(248),

	/** AccessEventCredential. */
	AccessEventCredential(249),

	/** AccessEventTime. */
	AccessEventTime(250),

	/** AccessTransactionEvents. */
	AccessTransactionEvents(251),

	/** Accompaniment. */
	Accompaniment(252),

	/** AccompanimentTime. */
	AccompanimentTime(253),

	/** ActivationTime. */
	ActivationTime(254),

	/** ActiveAuthenticationPolicy. */
	ActiveAuthenticationPolicy(255),

	/** AssignedAccessRights. */
	AssignedAccessRights(256),

	/** AuthenticationFactors. */
	AuthenticationFactors(257),

	/** AuthenticationPolicyList. */
	AuthenticationPolicyList(258),

	/** AuthenticationPolicyNames. */
	AuthenticationPolicyNames(259),

	/** AuthenticationStatus. */
	AuthenticationStatus(260),

	/** AuthorizationMode. */
	AuthorizationMode(261),

	/** BelongsTo. */
	BelongsTo(262),

	/** CredentialDisable. */
	CredentialDisable(263),

	/** CredentialStatus. */
	CredentialStatus(264),

	/** Credentials. */
	Credentials(265),

	/** CredentialsInZone. */
	CredentialsInZone(266),

	/** DaysRemaining. */
	DaysRemaining(267),

	/** EntryPoints. */
	EntryPoints(268),

	/** ExitPoints. */
	ExitPoints(269),

	/** ExpirationTime. */
	ExpirationTime(270),

	/** ExtendedTimeEnable. */
	ExtendedTimeEnable(271),

	/** FailedAttemptEvents. */
	FailedAttemptEvents(272),

	/** FailedAttempts. */
	FailedAttempts(273),

	/** FailedAttemptsTime. */
	FailedAttemptsTime(274),

	/** LastAccessEvent. */
	LastAccessEvent(275),

	/** LastAccessPoint. */
	LastAccessPoint(276),

	/** LastCredentialAdded. */
	LastCredentialAdded(277),

	/** LastCredentialAddedTime. */
	LastCredentialAddedTime(278),

	/** LastCredentialRemoved. */
	LastCredentialRemoved(279),

	/** LastCredentialRemovedTime. */
	LastCredentialRemovedTime(280),

	/** LastUseTime. */
	LastUseTime(281),

	/** Lockout. */
	Lockout(282),

	/** LockoutRelinquishTime. */
	LockoutRelinquishTime(283),

	/** MaxFailedAttempts. */
	MaxFailedAttempts(285),

	/** Members. */
	Members(286),

	/** MusterPoint. */
	MusterPoint(287),

	/** NegativeAccessRules. */
	NegativeAccessRules(288),

	/** NumberOfAuthenticationPolicies. */
	NumberOfAuthenticationPolicies(289),

	/** OccupancyCount. */
	OccupancyCount(290),

	/** OccupancyCountAdjust. */
	OccupancyCountAdjust(291),

	/** OccupancyCountEnable. */
	OccupancyCountEnable(292),

	/** OccupancyLowerLimit. */
	OccupancyLowerLimit(294),

	/** OccupancyLowerLimitEnforced. */
	OccupancyLowerLimitEnforced(295),

	/** OccupancyState. */
	OccupancyState(296),

	/** OccupancyUpperLimit. */
	OccupancyUpperLimit(297),

	/** OccupancyUpperLimitEnforced. */
	OccupancyUpperLimitEnforced(298),

	/** PassbackMode. */
	PassbackMode(300),

	/** PassbackTimeout. */
	PassbackTimeout(301),

	/** PositiveAccessRules. */
	PositiveAccessRules(302),

	/** ReasonForDisable. */
	ReasonForDisable(303),

	/** SupportedFormats. */
	SupportedFormats(304),

	/** SupportedFormatClasses. */
	SupportedFormatClasses(305),

	/** ThreatAuthority. */
	ThreatAuthority(306),

	/** ThreatLevel. */
	ThreatLevel(307),

	/** TraceFlag. */
	TraceFlag(308),

	/** TransactionNotificationClass. */
	TransactionNotificationClass(309),

	/** UserExternalIdentifier. */
	UserExternalIdentifier(310),

	/** UserInformationReference. */
	UserInformationReference(311),

	/** UserName. */
	UserName(317),

	/** UserType. */
	UserType(318),

	/** UsesRemaining. */
	UsesRemaining(319),

	/** ZoneFrom. */
	ZoneFrom(320),

	/** ZoneTo. */
	ZoneTo(321),

	/** AccessEventTag. */
	AccessEventTag(322),

	/** GlobalIdentifier. */
	GlobalIdentifier(323),

	/** VerificationTime. */
	VerificationTime(326),

	/** BaseDeviceSecurityPolicy. */
	BaseDeviceSecurityPolicy(327),

	/** DistributionKeyRevision. */
	DistributionKeyRevision(328),

	/** DoNotHide. */
	DoNotHide(329),

	/** KeySets. */
	KeySets(330),

	/** LastKeyServer. */
	LastKeyServer(331),

	/** NetworkAccessSecurityPolicies. */
	NetworkAccessSecurityPolicies(332),

	/** PacketReorderTime. */
	PacketReorderTime(333),

	/** SecurityPduTimeout. */
	SecurityPduTimeout(334),

	/** SecurityTimeWindow. */
	SecurityTimeWindow(335),

	/** SupportedSecurityAlgorithms. */
	SupportedSecurityAlgorithms(336),

	/** UpdateKeySetTimeout. */
	UpdateKeySetTimeout(337),

	/** BackupAndRestoreState. */
	BackupAndRestoreState(338),

	/** BackupPreparationTime. */
	BackupPreparationTime(339),

	/** RestoreCompletionTime. */
	RestoreCompletionTime(340),

	/** RestorePreparationTime. */
	RestorePreparationTime(341),

	/** BitMask. */
	BitMask(342),

	/** BitText. */
	BitText(343),

	/** IsUtc. */
	IsUtc(344),

	/** GroupMembers. */
	GroupMembers(345),

	/** GroupMemberNames. */
	GroupMemberNames(346),

	/** MemberStatusFlags. */
	MemberStatusFlags(347),

	/** RequestedUpdateInterval. */
	RequestedUpdateInterval(348),

	/** CovuPeriod. */
	CovuPeriod(349),

	/** CovuRecipients. */
	CovuRecipients(350),

	/** EventMessageTexts. */
	EventMessageTexts(351),

	/** EventMessageTextsConfig. */
	EventMessageTextsConfig(352),

	/** EventDetectionEnable. */
	EventDetectionEnable(353),

	/** EventAlgorithmInhibit. */
	EventAlgorithmInhibit(354),

	/** EventAlgorithmInhibitRef. */
	EventAlgorithmInhibitRef(355),

	/** TimeDelayNormal. */
	TimeDelayNormal(356),

	/** ReliabilityEvaluationInhibit. */
	ReliabilityEvaluationInhibit(357),

	/** FaultParameters. */
	FaultParameters(358),

	/** FaultType. */
	FaultType(359),

	/** LocalForwardingOnly. */
	LocalForwardingOnly(360),

	/** ProcessIdentifierFilter. */
	ProcessIdentifierFilter(361),

	/** SubscribedRecipients. */
	SubscribedRecipients(362),

	/** PortFilter. */
	PortFilter(363),

	/** AuthorizationExemptions. */
	AuthorizationExemptions(364),

	/** AllowGroupDelayInhibit. */
	AllowGroupDelayInhibit(365),

	/** ChannelNumber. */
	ChannelNumber(366),

	/** ControlGroups. */
	ControlGroups(367),

	/** ExecutionDelay. */
	ExecutionDelay(368),

	/** LastPriority. */
	LastPriority(369),

	/** WriteStatus. */
	WriteStatus(370),

	/** PropertyList. */
	PropertyList(371),

	/** SerialNumber. */
	SerialNumber(372),

	/** BlinkWarnEnable. */
	BlinkWarnEnable(373),

	/** DefaultFadeTime. */
	DefaultFadeTime(374),

	/** DefaultRampRate. */
	DefaultRampRate(375),

	/** DefaultStepIncrement. */
	DefaultStepIncrement(376),

	/** EgressTime. */
	EgressTime(377),

	/** InProgress. */
	InProgress(378),

	/** InstantaneousPower. */
	InstantaneousPower(379),

	/** LightingCommand. */
	LightingCommand(380),

	/** LightingCommandDefaultPriority. */
	LightingCommandDefaultPriority(381),

	/** MaxActualValue. */
	MaxActualValue(382),

	/** MinActualValue. */
	MinActualValue(383),

	/** Power. */
	Power(384),

	/** Transition. */
	Transition(385),

	/** EgressActive. */
	EgressActive(386),

	/** InterfaceValue. */
	InterfaceValue(387),

	/** FaultHighLimit. */
	FaultHighLimit(388),

	/** FaultLowLimit. */
	FaultLowLimit(389),

	/** LowDiffLimit. */
	LowDiffLimit(390),

	/** StrikeCount. */
	StrikeCount(391),

	/** TimeOfStrikeCountReset. */
	TimeOfStrikeCountReset(392),

	/** DefaultTimeout. */
	DefaultTimeout(393),

	/** InitialTimeout. */
	InitialTimeout(394),

	/** LastStateChange. */
	LastStateChange(395),

	/** StateChangeValues. */
	StateChangeValues(396),

	/** TimerRunning. */
	TimerRunning(397),

	/** TimerState. */
	TimerState(398),

	/** ApduLength. */
	ApduLength(399),

	/** IpAddress. */
	IpAddress(400),

	/** IpDefaultGateway. */
	IpDefaultGateway(401),

	/** IpDhcpEnable. */
	IpDhcpEnable(402),

	/** IpDhcpLeaseTime. */
	IpDhcpLeaseTime(403),

	/** IpDhcpLeaseTimeRemaining. */
	IpDhcpLeaseTimeRemaining(404),

	/** IpDhcpServer. */
	IpDhcpServer(405),

	/** IpDnsServer. */
	IpDnsServer(406),

	/** BacnetIpGlobalAddress. */
	BacnetIpGlobalAddress(407),

	/** BacnetIpMode. */
	BacnetIpMode(408),

	/** BacnetIpMulticastAddress. */
	BacnetIpMulticastAddress(409),

	/** BacnetIpNatTraversal. */
	BacnetIpNatTraversal(410),

	/** IpSubnetMask. */
	IpSubnetMask(411),

	/** BacnetIpUdpPort. */
	BacnetIpUdpPort(412),

	/** BbmdAcceptFdRegistrations. */
	BbmdAcceptFdRegistrations(413),

	/** BbmdBroadcastDistributionTable. */
	BbmdBroadcastDistributionTable(414),

	/** BbmdForeignDeviceTable. */
	BbmdForeignDeviceTable(415),

	/** ChangesPending. */
	ChangesPending(416),

	/** Command. */
	Command(417),

	/** FdBbmdAddress. */
	FdBbmdAddress(418),

	/** FdSubscriptionLifetime. */
	FdSubscriptionLifetime(419),

	/** LinkSpeed. */
	LinkSpeed(420),

	/** LinkSpeeds. */
	LinkSpeeds(421),

	/** LinkSpeedAutonegotiate. */
	LinkSpeedAutonegotiate(422),

	/** MacAddress. */
	MacAddress(423),

	/** NetworkInterfaceName. */
	NetworkInterfaceName(424),

	/** NetworkNumber. */
	NetworkNumber(425),

	/** NetworkNumberQuality. */
	NetworkNumberQuality(426),

	/** NetworkType. */
	NetworkType(427),

	/** RoutingTable. */
	RoutingTable(428),

	/** VirtualMacAddressTable. */
	VirtualMacAddressTable(429),

	/** CommandTimeArray. */
	CommandTimeArray(430),

	/** CurrentCommandPriority. */
	CurrentCommandPriority(431),

	/** LastCommandTime. */
	LastCommandTime(432),

	/** ValueSource. */
	ValueSource(433),

	/** ValueSourceArray. */
	ValueSourceArray(434),

	/** BacnetIpv. */
	BacnetIpv6Mode(435),

	/** Ipv. */
	Ipv6Address(436),

	/** Ipv. */
	Ipv6PrefixLength(437),

	/** BacnetIpv. */
	BacnetIpv6UdpPort(438),

	/** Ipv. */
	Ipv6DefaultGateway(439),

	/** BacnetIpv. */
	BacnetIpv6MulticastAddress(440),

	/** Ipv. */
	Ipv6DnsServer(441),

	/** Ipv. */
	Ipv6AutoAddressingEnable(442),

	/** Ipv. */
	Ipv6DhcpLeaseTime(443),

	/** Ipv. */
	Ipv6DhcpLeaseTimeRemaining(444),

	/** Ipv. */
	Ipv6DhcpServer(445),

	/** Ipv. */
	Ipv6ZoneIndex(446),

	/** AssignedLandingCalls. */
	AssignedLandingCalls(447),

	/** CarAssignedDirection. */
	CarAssignedDirection(448),

	/** CarDoorCommand. */
	CarDoorCommand(449),

	/** CarDoorStatus. */
	CarDoorStatus(450),

	/** CarDoorText. */
	CarDoorText(451),

	/** CarDoorZone. */
	CarDoorZone(452),

	/** CarDriveStatus. */
	CarDriveStatus(453),

	/** CarLoad. */
	CarLoad(454),

	/** CarLoadUnits. */
	CarLoadUnits(455),

	/** CarMode. */
	CarMode(456),

	/** CarMovingDirection. */
	CarMovingDirection(457),

	/** CarPosition. */
	CarPosition(458),

	/** ElevatorGroup. */
	ElevatorGroup(459),

	/** EnergyMeter. */
	EnergyMeter(460),

	/** EnergyMeterRef. */
	EnergyMeterRef(461),

	/** EscalatorMode. */
	EscalatorMode(462),

	/** FaultSignals. */
	FaultSignals(463),

	/** FloorText. */
	FloorText(464),

	/** GroupId. */
	GroupId(465),

	/** GroupMode. */
	GroupMode(467),

	/** HigherDeck. */
	HigherDeck(468),

	/** InstallationId. */
	InstallationId(469),

	/** LandingCalls. */
	LandingCalls(470),

	/** LandingCallControl. */
	LandingCallControl(471),

	/** LandingDoorStatus. */
	LandingDoorStatus(472),

	/** LowerDeck. */
	LowerDeck(473),

	/** MachineRoomId. */
	MachineRoomId(474),

	/** MakingCarCall. */
	MakingCarCall(475),

	/** NextStoppingFloor. */
	NextStoppingFloor(476),

	/** OperationDirection. */
	OperationDirection(477),

	/** PassengerAlarm. */
	PassengerAlarm(478),

	/** PowerMode. */
	PowerMode(479),

	/** RegisteredCarCall. */
	RegisteredCarCall(480),

	/** ActiveCovMultipleSubscriptions. */
	ActiveCovMultipleSubscriptions(481),

	/** ProtocolLevel. */
	ProtocolLevel(482),

	/** ReferencePort. */
	ReferencePort(483),

	/** DeployedProfileLocation. */
	DeployedProfileLocation(484),

	/** ProfileLocation. */
	ProfileLocation(485),

	/** Tags. */
	Tags(486),

	/** SubordinateNodeTypes. */
	SubordinateNodeTypes(487),

	/** SubordinateTags. */
	SubordinateTags(488),

	/** SubordinateRelationships. */
	SubordinateRelationships(489),

	/** DefaultSubordinateRelationship. */
	DefaultSubordinateRelationship(490),

	/** Represents. */
	Represents(491),

	/** Default present value. */
	DefaultPresentValue(492),

	/** Present stage. */
	PresentStage(493),

	/** Stages. */
	Stages(494),

	/** Stage names. */
	StageNames(495),

	/** Target references. */
	TargetReferences(496),

	/** Audit source level. */
	AuditSourceLevel(497),

	/** Audit level. */
	AuditLevel(498),

	/** Audit notification recipient. */
	AuditNotificationRecipient(499),

	/** Audit priority filter. */
	AuditPriorityFilter(500),

	/** Auditable operations. */
	AuditableOperations(501),

	/** Delete on forward. */
	DeleteOnForward(502),

	/** Maximum send delay. */
	MaximumSendDelay(503),

	/** Monitored objects. */
	MonitoredObjects(504),

	/** Send now. */
	SendNow(505),

	/** Floor number. */
	FloorNumber(506),

	/** Device UUID. */
	DeviceUuid(507),

	;

	private final int code;

	private BacnetPropertyType(int code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get an enumeration value for a string key.
	 * 
	 * @param value
	 *        the value to parse into an enumeration value; can be either an
	 *        integer {@code code} or an enumeration name
	 * @return the enumeration value
	 * @throws IllegalArgumentException
	 *         if the value cannot be parsed into an enumeration value
	 * @see CodedValue#forCodeValue(int, Class, Enum)
	 */
	public static BacnetPropertyType forKey(String value) {
		ObjectUtils.requireNonNullArgument(value, "value");
		try {
			int code = Integer.parseInt(value);
			BacnetPropertyType result = CodedValue.forCodeValue(code, BacnetPropertyType.class, null);
			if ( result != null ) {
				return result;
			}
			throw new IllegalArgumentException(
					String.format("Unsupported BacnetPropertyType value [%s]", value));
		} catch ( NumberFormatException e ) {
			// ignore and try by name
			try {
				return BacnetPropertyType.valueOf(value);
			} catch ( IllegalArgumentException e2 ) {
				try {
					// ignore and try by name with train-case
					return BacnetPropertyType.valueOf(BacnetUtils.trainToCamelCase(value));
				} catch ( IllegalArgumentException e3 ) {
					throw new IllegalArgumentException(
							String.format("Unsupported BacnetPropertyType value [%s]", value));
				}
			}
		}
	}

}
