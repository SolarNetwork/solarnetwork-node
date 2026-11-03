/* ==================================================================
 * BluetoothSetupServiceTests.java - 6/10/2026 8:30:00 AM
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

package net.solarnetwork.node.setup.bluetooth.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.event.Event;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.setup.bluetooth.AvailabilitySchedule;
import net.solarnetwork.node.setup.bluetooth.BluetoothSetupService;
import net.solarnetwork.node.setup.bluetooth.BluetoothSetupStatus;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.TextAreaSettingSpecifier;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.TextFieldSettingSpecifier;
import net.solarnetwork.settings.TitleSettingSpecifier;

/**
 * Test cases for the {@link BluetoothSetupService} class.
 *
 * @author elijah
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class BluetoothSetupServiceTests {

	private static final String SCHED_HEADER = AvailabilitySchedule.EXAMPLE_HEADER + "\n";

	private static final String WEEKDAY_HOURS = SCHED_HEADER + ",,Mon-Fri,07:00-18:00,1";

	private static final String INVALID_HOURS = SCHED_HEADER + ",,Mon-Fri,25:00-26:00,1";

	private static final String OP_MODE = "bt-setup";

	private static final String OFFLINE_MODE = BluetoothSetupService.DEFAULT_OFFLINE_OP_MODE;
	private static final Instant START = Instant.parse("2026-10-06T00:00:00Z");

	private static final Duration SCHEDULE_TICK = Duration
			.ofSeconds(BluetoothSetupService.SCHEDULE_TICK_SECONDS);

	@Mock
	private OperationalModesService opModesService;

	@Mock
	private MetadataService metadataService;

	@Mock
	private TaskScheduler taskScheduler;

	@Mock
	private ScheduledFuture<?> future;

	@Mock
	private ScheduledFuture<?> future2;

	@Captor
	private ArgumentCaptor<Instant> expireCaptor;

	private SwitchableService metadataServiceRef;
	private BluetoothSetupService service;
	private File tmpFile;
	private File logFile;
	private File stateFile;

	@Before
	public void setup() throws IOException {
		metadataServiceRef = new SwitchableService(metadataService);
		// no task scheduler: reconcile runs inline, which keeps the tests deterministic
		service = new BluetoothSetupService(opModesService, null, metadataServiceRef);
		// the production default is always on; these tests exercise the gated
		// behaviour so start with the gate closed
		service.setAlwaysOn(false);

		// copy script to file so test can run via JAR (i.e. Ant)
		tmpFile = File.createTempFile("solarcfg-", ".sh");
		tmpFile.setExecutable(true, true);
		FileCopyUtils.copy(getClass().getResourceAsStream("solarcfg.sh"), new FileOutputStream(tmpFile));
		logFile = new File(tmpFile.getAbsolutePath() + ".log");
		stateFile = new File(tmpFile.getAbsolutePath() + ".state");
		service.setCommand(tmpFile.getAbsolutePath());
		service.setClock(Clock.fixed(START, ZoneOffset.UTC));

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(BluetoothSetupService.class.getName(),
				"net.solarnetwork.node.service.support.TariffScheduleUtils");
		service.setMessageSource(msgSource);
	}

	@After
	public void teardown() {
		if ( tmpFile != null ) {
			tmpFile.delete();
		}
		if ( logFile != null ) {
			logFile.delete();
		}
		if ( stateFile != null ) {
			stateFile.delete();
		}
	}

	private List<String> actions() throws IOException {
		if ( !logFile.exists() ) {
			return Collections.emptyList();
		}
		return Files.readAllLines(logFile.toPath()).stream().map(l -> l.split(" ")[1])
				.filter(a -> !"status".equals(a)).toList();
	}

	private int rawLogLines() throws IOException {
		return (logFile.exists() ? Files.readAllLines(logFile.toPath()).size() : 0);
	}

	private boolean radioState() throws IOException {
		return stateFile.exists() && "true".equals(Files.readString(stateFile.toPath()).trim());
	}

	private Instant now = START;

	private void advanceClock(Duration d) {
		now = now.plus(d);
		service.setClock(Clock.fixed(now, ZoneOffset.UTC));
	}

	private Instruction instruction(String action, Map<String, String> extra) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put(InstructionHandler.PARAM_SERVICE, BluetoothSetupService.BLUETOOTH_SERVICE_NAME);
		if ( action != null ) {
			params.put(BluetoothSetupService.PARAM_ACTION, action);
		}
		if ( extra != null ) {
			params.putAll(extra);
		}
		return InstructionUtils.createLocalInstruction(InstructionHandler.TOPIC_SYSTEM_CONFIGURE,
				params);
	}

	private static BluetoothSetupStatus serviceResult(InstructionStatus status) {
		return (BluetoothSetupStatus) status.getResultParameters()
				.get(InstructionHandler.PARAM_SERVICE_RESULT);
	}

	private Event modesEvent(String... activeModes) {
		Map<String, Object> props = new HashMap<>();
		props.put(OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES, Set.of(activeModes));
		return new Event(OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED, props);
	}

	@Test
	public void handlesTopic() {
		// @formatter:off
		and.then(service.handlesTopic(InstructionHandler.TOPIC_SYSTEM_CONFIGURE))
			.as("System configure topic handled")
			.isTrue()
			;
		and.then(service.handlesTopic("Other"))
			.as("Other topic not handled")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void instruction_otherService_ignored() {
		// GIVEN
		Map<String, String> params = new LinkedHashMap<>();
		params.put(InstructionHandler.PARAM_SERVICE, "/setup/network/wifi");
		Instruction instr = InstructionUtils
				.createLocalInstruction(InstructionHandler.TOPIC_SYSTEM_CONFIGURE, params);

		// WHEN
		InstructionStatus status = service.processInstruction(instr);

		// THEN
		// @formatter:off
		and.then(status)
			.as("Instruction for another service ignored")
			.isNull()
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void instruction_unsupportedAction_declined() {
		// WHEN
		InstructionStatus status = service.processInstruction(instruction("explode", null));

		// THEN
		// @formatter:off
		and.then(status)
			.as("Status returned")
			.isNotNull()
			.as("Unsupported action declined")
			.returns(InstructionState.Declined, from(InstructionStatus::getInstructionState))
			;
		and.then(status.getResultParameters())
			.as("Message provided")
			.extractingByKey(InstructionHandler.PARAM_MESSAGE)
			.isNotNull()
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void instruction_status() throws IOException {
		// WHEN
		InstructionStatus status = service.processInstruction(instruction(null, null));

		// THEN
		// @formatter:off
		and.then(status)
			.as("Status completed")
			.returns(InstructionState.Completed, from(InstructionStatus::getInstructionState))
			;
		and.then(status.getResultParameters().get(InstructionHandler.PARAM_SERVICE_RESULT))
			.as("Result is Bluetooth status")
			.asInstanceOf(type(BluetoothSetupStatus.class))
			.as("Radio not active")
			.returns(false, from(BluetoothSetupStatus::isRadioActive))
			.as("Op mode not active")
			.returns(false, from(BluetoothSetupStatus::isOpModeActive))
			.as("Not offline")
			.returns(false, from(BluetoothSetupStatus::isOffline))
			;
		and.then(actions())
			.as("status does not change the radio")
			.isEmpty()
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void instruction_enable_defaultDuration() throws IOException {
		// GIVEN
		service.setDefaultDurationSeconds(600);
		given(opModesService.activeOperationalModesWithExpirations())
				.willReturn(Map.of(OP_MODE, START.plusSeconds(600).toEpochMilli()));

		// WHEN
		InstructionStatus status = service.processInstruction(instruction("enable", null));

		// THEN
		// @formatter:off
		then(opModesService).should()
			.enableOperationalModes(eq(Set.of(OP_MODE)), expireCaptor.capture())
			;
		and.then(expireCaptor.getValue())
			.as("Op mode enabled for default duration")
			.isEqualTo(START.plusSeconds(600))
			;
		and.then(status)
			.as("Status completed")
			.returns(InstructionState.Completed, from(InstructionStatus::getInstructionState))
			;
		and.then(serviceResult(status))
			.as("Radio active")
			.returns(true, from(BluetoothSetupStatus::isRadioActive))
			.as("Op mode active")
			.returns(true, from(BluetoothSetupStatus::isOpModeActive))
			.as("Op mode expiration")
			.returns(START.plusSeconds(600).toEpochMilli(), from(BluetoothSetupStatus::getOpModeExpires))
			;
		and.then(actions())
			.as("Radio enabled")
			.containsExactly("enable")
			;
		and.then(radioState())
			.as("Radio on")
			.isTrue()
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void instruction_enable_capsDuration() throws IOException {
		// GIVEN
		service.setMaxDurationSeconds(100);

		// WHEN
		service.processInstruction(instruction("enable", Map.of("duration", "99999")));

		// THEN
		// @formatter:off
		then(opModesService).should()
			.enableOperationalModes(eq(Set.of(OP_MODE)), expireCaptor.capture())
			;
		and.then(expireCaptor.getValue())
			.as("Duration capped at maximum")
			.isEqualTo(START.plusSeconds(100))
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void instruction_enable_invalidDuration_declined() throws IOException {
		// WHEN
		InstructionStatus status = service
				.processInstruction(instruction("enable", Map.of("duration", "soon")));

		// THEN
		// @formatter:off
		and.then(status)
			.as("Invalid duration declined")
			.returns(InstructionState.Declined, from(InstructionStatus::getInstructionState))
			;
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void instruction_disable() throws IOException {
		// GIVEN radio currently on via op mode
		service.processInstruction(instruction("enable", null));
		// @formatter:off
		and.then(radioState())
			.as("Radio on after enable")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		InstructionStatus status = service.processInstruction(instruction("disable", null));

		// THEN
		// @formatter:off
		and.then(status)
			.as("Status completed")
			.returns(InstructionState.Completed, from(InstructionStatus::getInstructionState))
			;
		and.then(serviceResult(status))
			.as("Radio not active")
			.returns(false, from(BluetoothSetupStatus::isRadioActive))
			.as("Op mode not active")
			.returns(false, from(BluetoothSetupStatus::isOpModeActive))
			;
		and.then(actions())
			.as("Radio enabled then disabled")
			.containsExactly("enable", "disable")
			;
		and.then(radioState())
			.as("Radio off")
			.isFalse()
			;
		then(opModesService).should()
			.enableOperationalModes(eq(Set.of(OP_MODE)), any())
			;
		then(opModesService).should()
			.disableOperationalModes(Set.of(OP_MODE))
			;
		// @formatter:on
	}

	@Test
	public void instruction_restart_whenOff_leavesRadioOff() throws IOException {
		// WHEN
		InstructionStatus status = service.processInstruction(instruction("restart", null));

		// THEN
		// @formatter:off
		and.then(status)
			.as("Status completed")
			.returns(InstructionState.Completed, from(InstructionStatus::getInstructionState))
			;
		and.then(serviceResult(status))
			.as("restart never turns a gated-off radio on")
			.returns(false, from(BluetoothSetupStatus::isRadioActive))
			;
		and.then(radioState())
			.as("Radio off")
			.isFalse()
			;
		and.then(actions())
			.as("Only restart executed")
			.containsExactly("restart")
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void instruction_restart_whenOn_keepsRadioOn() throws IOException {
		// GIVEN
		service.processInstruction(instruction("enable", null));
		// @formatter:off
		and.then(radioState())
			.as("Radio on after enable")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		InstructionStatus status = service.processInstruction(instruction("restart", null));

		// THEN
		// @formatter:off
		and.then(status)
			.as("Status completed")
			.returns(InstructionState.Completed, from(InstructionStatus::getInstructionState))
			;
		and.then(serviceResult(status))
			.as("Radio still active")
			.returns(true, from(BluetoothSetupStatus::isRadioActive))
			;
		and.then(radioState())
			.as("Radio on")
			.isTrue()
			;
		and.then(actions())
			.as("Radio enabled then restarted")
			.containsExactly("enable", "restart")
			;
		then(opModesService).should()
			.enableOperationalModes(eq(Set.of(OP_MODE)), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void executeAction_hang_timesOut() throws IOException {
		// GIVEN
		service.setCommandTimeoutSeconds(1);
		File hangFile = new File(tmpFile.getAbsolutePath() + ".hang");
		try {
			Files.writeString(hangFile.toPath(), "");

			// WHEN
			final long start = System.nanoTime();
			BluetoothSetupStatus s = service.currentStatus();
			final long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

			// THEN
			// @formatter:off
			and.then(s)
				.as("hung helper reported as unknown status")
				.isNull()
				;
			and.then(elapsedMs)
				.as("timeout enforced from process start, not after output")
				.isLessThan(10_000L)
				;
			// @formatter:on
		} finally {
			hangFile.delete();
		}
	}

	@Test
	public void executeAction_largeStderr_doesNotDeadlock() throws IOException {
		// GIVEN
		service.setCommandTimeoutSeconds(10);
		File noisyFile = new File(tmpFile.getAbsolutePath() + ".noisy");
		try {
			Files.writeString(noisyFile.toPath(), "");

			// WHEN
			BluetoothSetupStatus s = service.currentStatus();

			// THEN
			// @formatter:off
			and.then(s)
				.as("status parsed despite large stderr output")
				.isNotNull()
				.as("Radio not active")
				.returns(false, from(BluetoothSetupStatus::isRadioActive))
				;
			// @formatter:on
		} finally {
			noisyFile.delete();
		}
	}

	@Test
	public void modesChanged_enablesThenDisables() throws IOException {
		// WHEN
		service.handleEvent(modesEvent(OP_MODE, "other"));

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Op mode active turns radio on")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.handleEvent(modesEvent("other"));

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Op mode inactive turns radio off")
			.isFalse()
			;
		and.then(actions())
			.as("Radio enabled then disabled")
			.containsExactly("enable", "disable")
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void modesChanged_otherMode_ignored() throws IOException {
		// WHEN
		service.handleEvent(modesEvent("quiet"));

		// THEN
		// @formatter:off
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		and.then(radioState())
			.as("Radio off")
			.isFalse()
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void reconcile_idempotent() throws IOException {
		// WHEN
		service.handleEvent(modesEvent(OP_MODE));
		service.handleEvent(modesEvent(OP_MODE));
		service.reconcile();
		service.reconcile();

		// THEN
		// @formatter:off
		and.then(actions())
			.as("only one enable despite repeated events")
			.containsExactly("enable")
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void alwaysOn_turnsRadioOn() throws IOException {
		// GIVEN
		service.setAlwaysOn(true);

		// WHEN
		service.reconcile();

		// THEN
		// @formatter:off
		and.then(actions())
			.as("Radio enabled")
			.containsExactly("enable")
			;
		and.then(radioState())
			.as("Radio on")
			.isTrue()
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void defaultAlwaysOn_turnsRadioOn() throws IOException {
		// GIVEN
		// a fresh instance with no configuration at all must turn the radio on
		BluetoothSetupService fresh = new BluetoothSetupService(opModesService, null, null);
		fresh.setCommand(tmpFile.getAbsolutePath());
		// @formatter:off
		and.then(fresh.isAlwaysOn())
			.as("Always on by default")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		fresh.reconcile();

		// THEN
		// @formatter:off
		and.then(actions())
			.as("Radio enabled")
			.containsExactly("enable")
			;
		and.then(radioState())
			.as("Radio on")
			.isTrue()
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void offline_triggersOnlyAfterThreshold() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(2);
		given(opModesService.isOperationalModeActive(OFFLINE_MODE)).willReturn(true);

		// WHEN
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("not yet offline")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("still under threshold")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("threshold reached")
			.isTrue()
			;
		and.then(actions())
			.as("Radio enabled")
			.containsExactly("enable")
			;
		and.then(service.currentStatus())
			.as("Offline")
			.returns(true, from(BluetoothSetupStatus::isOffline))
			.as("Offline since first offline tick")
			.returns(START, from(BluetoothSetupStatus::getOfflineSince))
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void offline_clearsOnlyAfterGrace() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(1);
		service.setOnlineGraceMinutes(3);
		given(opModesService.isOperationalModeActive(OFFLINE_MODE)).willReturn(true, true, false);

		// WHEN
		service.watchdogTick();
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("threshold reached")
			.isTrue()
			;
		// @formatter:on

		// WHEN back online
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("grace period started")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofMinutes(2));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("still within grace")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("grace elapsed")
			.isFalse()
			;
		and.then(actions())
			.as("Radio enabled then disabled")
			.containsExactly("enable", "disable")
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void offline_graceResetsOnNewFailure() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(1);
		service.setOnlineGraceMinutes(2);
		given(opModesService.isOperationalModeActive(OFFLINE_MODE)).willReturn(true, true, false, true,
				false, false);

		// WHEN
		service.watchdogTick();
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("threshold reached")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick(); // online, grace starts
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick(); // offline again, grace reset
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick(); // online, grace restarts
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick(); // only 1 minute of grace since restart

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("grace restarted so radio still on")
			.isTrue()
			;
		and.then(actions())
			.as("Radio only enabled")
			.containsExactly("enable")
			;
		then(opModesService).should(times(6))
			.isOperationalModeActive(OFFLINE_MODE)
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void offline_modeNeverActive_neverTriggers() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(1);

		// WHEN
		service.watchdogTick();
		advanceClock(Duration.ofMinutes(10));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off")
			.isFalse()
			;
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void offline_triggerDisabled_neverTriggers() throws IOException {
		// GIVEN
		service.setOfflineTriggerEnabled(false);
		service.setOfflineThresholdMinutes(1);

		// WHEN
		service.watchdogTick();
		advanceClock(Duration.ofMinutes(10));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off")
			.isFalse()
			;
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		then(opModesService).should(never())
			.isOperationalModeActive(any())
			;
		// @formatter:on
	}

	private String statusTitle() {
		SettingSpecifier title = service.getSettingSpecifiers().get(0);
		return String.valueOf(((TitleSettingSpecifier) title).getDefaultValue());
	}

	@Test
	public void offline_eventStartsThreshold() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(2);
		service.setOnlineGraceMinutes(3);
		given(opModesService.isOperationalModeActive(OFFLINE_MODE)).willReturn(true);

		// WHEN
		service.handleEvent(modesEvent(OFFLINE_MODE));

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("not yet offline")
			.isFalse()
			;
		and.then(service.currentStatus())
			.as("threshold started")
			.returns(START, from(BluetoothSetupStatus::getOfflineSince))
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofMinutes(2));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("threshold reached")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.handleEvent(modesEvent("other"));

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("grace started")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofMinutes(3));
		service.handleEvent(modesEvent("other"));

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("grace elapsed")
			.isFalse()
			;
		and.then(actions())
			.as("Radio enabled then disabled")
			.containsExactly("enable", "disable")
			;
		then(opModesService).should()
			.isOperationalModeActive(OFFLINE_MODE)
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void offline_blankMode_neverTriggers() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(0);
		service.setOfflineOpMode("");

		// WHEN
		service.handleEvent(modesEvent(OFFLINE_MODE, "other"));
		service.watchdogTick();
		advanceClock(Duration.ofMinutes(10));
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off")
			.isFalse()
			;
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		and.then(service.currentStatus())
			.as("No offline state")
			.returns(null, from(BluetoothSetupStatus::getOfflineSince))
			;
		then(opModesService).should(never())
			.isOperationalModeActive(any())
			;
		// @formatter:on

		// WHEN
		service.setOfflineOpMode(null);

		// THEN
		// @formatter:off
		and.then(service.getOfflineOpMode())
			.as("Null mode normalised to empty")
			.isEqualTo("")
			;
		// @formatter:on
	}

	@Test
	public void offline_modeNormalised() {
		// WHEN
		service.setOfflineOpMode(" Net-Offline ");

		// THEN
		// @formatter:off
		and.then(service.getOfflineOpMode())
			.as("Mode trimmed and lower-cased")
			.isEqualTo("net-offline")
			;
		// @formatter:on

		// WHEN
		service.setOfflineOpMode("!net-online");

		// THEN
		// @formatter:off
		and.then(service.getOfflineOpMode())
			.as("inverted mode not supported")
			.isEqualTo("")
			;
		// @formatter:on
	}

	@Test
	public void offline_customMode() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(0);
		service.setOfflineOpMode("WAN-Down");
		given(opModesService.isOperationalModeActive("wan-down")).willReturn(true);

		// WHEN
		service.handleEvent(modesEvent(OFFLINE_MODE));

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("default mode ignored")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		service.handleEvent(modesEvent("wan-down"));

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("custom mode event")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.watchdogTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("custom mode tick")
			.isTrue()
			;
		and.then(actions())
			.as("Radio enabled")
			.containsExactly("enable")
			;
		then(opModesService).should()
			.isOperationalModeActive("wan-down")
			;
		then(opModesService).should(never())
			.isOperationalModeActive(OFFLINE_MODE)
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void offline_triggerDisabled_eventIgnored() throws IOException {
		// GIVEN
		service.setOfflineTriggerEnabled(false);
		service.setOfflineThresholdMinutes(0);

		// WHEN
		service.handleEvent(modesEvent(OFFLINE_MODE));
		advanceClock(Duration.ofMinutes(10));
		service.handleEvent(modesEvent(OFFLINE_MODE, "other"));

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off")
			.isFalse()
			;
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		and.then(service.currentStatus())
			.as("No offline state")
			.returns(null, from(BluetoothSetupStatus::getOfflineSince))
			;
		then(opModesService).should(never())
			.isOperationalModeActive(any())
			;
		// @formatter:on
	}

	@Test
	public void offline_watchdogDisabled_eventIgnored() throws IOException {
		// GIVEN
		service.setWatchdogIntervalSeconds(0);
		service.setOfflineThresholdMinutes(1);

		// WHEN
		service.handleEvent(modesEvent(OFFLINE_MODE));
		advanceClock(Duration.ofMinutes(10));
		service.handleEvent(modesEvent(OFFLINE_MODE, "other"));

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off")
			.isFalse()
			;
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		and.then(service.currentStatus())
			.as("No offline state")
			.returns(null, from(BluetoothSetupStatus::getOfflineSince))
			;
		then(opModesService).should(never())
			.isOperationalModeActive(any())
			;
		// @formatter:on
	}

	@Test
	public void offline_configChange_blankMode_clearsState() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(0);
		service.handleEvent(modesEvent(OFFLINE_MODE));
		// @formatter:off
		and.then(radioState())
			.as("Offline turns radio on")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.setOfflineOpMode("");
		service.configurationChanged(Map.of());

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("stale offline state cleared")
			.isFalse()
			;
		and.then(service.currentStatus())
			.as("Not offline")
			.returns(false, from(BluetoothSetupStatus::isOffline))
			;
		and.then(actions())
			.as("Radio enabled then disabled")
			.containsExactly("enable", "disable")
			;
		then(opModesService).should(never())
			.isOperationalModeActive(OFFLINE_MODE)
			;
		// @formatter:on
	}

	@Test
	public void offline_configChange_modeChanged_clearsState() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(0);
		service.handleEvent(modesEvent(OFFLINE_MODE));
		// @formatter:off
		and.then(radioState())
			.as("Offline turns radio on")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.setOfflineOpMode("wan-down");
		service.configurationChanged(Map.of());

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("state from the old mode cleared")
			.isFalse()
			;
		and.then(service.currentStatus())
			.as("No offline state")
			.returns(null, from(BluetoothSetupStatus::getOfflineSince))
			;
		then(opModesService).should(never())
			.isOperationalModeActive(OFFLINE_MODE)
			;
		then(opModesService).should(never())
			.isOperationalModeActive("wan-down")
			;
		// @formatter:on
	}

	@Test
	public void offline_configChange_triggerToggled_noImmediateTurnOn() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(0);
		service.handleEvent(modesEvent(OFFLINE_MODE));
		// @formatter:off
		and.then(radioState())
			.as("Offline turns radio on")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.setOfflineTriggerEnabled(false);
		service.configurationChanged(Map.of());

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Disabling the trigger turns radio off")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		service.setOfflineTriggerEnabled(true);
		service.configurationChanged(Map.of());

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("no stale offline state")
			.isFalse()
			;
		and.then(actions())
			.as("Radio enabled then disabled")
			.containsExactly("enable", "disable")
			;
		then(opModesService).should(never())
			.isOperationalModeActive(OFFLINE_MODE)
			;
		// @formatter:on
	}

	@Test
	public void status_offlinePending() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(15);

		// WHEN
		service.handleEvent(modesEvent(OFFLINE_MODE));

		// THEN
		// @formatter:off
		and.then(statusTitle())
			.as("Status shows pending offline turn on")
			.contains("offline mode 'net-offline' active since 2026-10-06T00:00:00Z; radio turns on at 2026-10-06T00:15:00Z")
			;
		// @formatter:on
	}

	@Test
	public void status_offline() throws IOException {
		// GIVEN
		service.setOfflineThresholdMinutes(0);

		// WHEN
		service.handleEvent(modesEvent(OFFLINE_MODE));

		// THEN
		// @formatter:off
		and.then(statusTitle())
			.as("Status shows offline mode")
			.contains("offline mode 'net-offline' active since 2026-10-06T00:00:00Z")
			.as("Status has no pending turn on")
			.doesNotContain("radio turns on at")
			;
		// @formatter:on
	}

	@Test
	public void status_commandFailure_returnsNull() throws IOException {
		// GIVEN
		service.setCommand("/nonexistent/solarcfg");

		// WHEN
		BluetoothSetupStatus s = service.currentStatus();

		// THEN
		// @formatter:off
		and.then(s)
			.as("Command failure reported as unknown status")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void executeAction_nonZeroExit_declined() throws IOException {
		// GIVEN
		// the fake script exits 3 from "restart" when a ".fail" marker exists; the
		// non-zero exit must surface as a Declined instruction, not Completed
		File failFile = new File(tmpFile.getAbsolutePath() + ".fail");
		try {
			Files.writeString(failFile.toPath(), "");

			// WHEN
			InstructionStatus status = service.processInstruction(instruction("restart", null));

			// THEN
			// @formatter:off
			and.then(status)
				.as("Non-zero exit declined")
				.returns(InstructionState.Declined, from(InstructionStatus::getInstructionState))
				;
			and.then(status.getResultParameters())
				.as("Message includes exit code")
				.extractingByKey(InstructionHandler.PARAM_MESSAGE)
				.asString()
				.contains("exit code 3")
				;
			// @formatter:on
		} finally {
			failFile.delete();
		}
	}

	private static KeyedSettingSpecifier<?> keyedSetting(List<SettingSpecifier> specs, String key) {
		return specs.stream().filter(KeyedSettingSpecifier.class::isInstance)
				.map(KeyedSettingSpecifier.class::cast).filter(k -> key.equals(k.getKey())).findFirst()
				.orElse(null);
	}

	@Test
	public void settings() {
		// WHEN
		List<SettingSpecifier> specs = service.getSettingSpecifiers();

		// THEN
		// @formatter:off
		and.then(specs)
			.as("Settings provided")
			.isNotNull()
			.as("Setting count")
			.hasSize(17)
			;
		and.then(keyedSetting(specs, "metadataServiceUid"))
			.as("metadata service setting is a text field")
			.isInstanceOf(TextFieldSettingSpecifier.class)
			.as("metadata service default")
			.returns(BluetoothSetupService.DEFAULT_METADATA_SERVICE_UID, from(KeyedSettingSpecifier::getDefaultValue))
			.asInstanceOf(type(TextFieldSettingSpecifier.class))
			.as("metadata service lookup")
			.returns("(objectClass=net.solarnetwork.node.service.MetadataService)", from(TextFieldSettingSpecifier::getRelatedServiceFilter))
			;
		and.then(keyedSetting(specs, "metadataPath"))
			.as("metadata path setting")
			.isNotNull()
			.as("metadata path default")
			.returns("", from(KeyedSettingSpecifier::getDefaultValue))
			;
		and.then(keyedSetting(specs, "alwaysOn"))
			.as("always on setting")
			.isNotNull()
			.as("always on by default")
			.returns(Boolean.TRUE, from(KeyedSettingSpecifier::getDefaultValue))
			;
		and.then(keyedSetting(specs, "schedule"))
			.as("schedule setting is a text area")
			.asInstanceOf(type(TextAreaSettingSpecifier.class))
			.as("schedule text area is direct")
			.returns(true, from(TextAreaSettingSpecifier::isDirect))
			;
		and.then(keyedSetting(specs, "offlineOpMode"))
			.as("offline mode setting")
			.isNotNull()
			.as("offline mode default")
			.returns("net-offline", from(KeyedSettingSpecifier::getDefaultValue))
			;
		and.then(keyedSetting(specs, "pingTestIdRegex"))
			.as("no ping test setting")
			.isNull()
			;
		// @formatter:on
	}

	private void startupWithSchedule(String schedule, String timeZone) {
		service.setSchedule(schedule);
		service.setTimeZone(timeZone);
		service.serviceDidStartup();
	}

	@Test
	public void schedule_insideWindow_turnsOn() throws IOException {
		// GIVEN
		// START is 2026-10-06T00:00Z, a Tuesday; 13:00 NZDT in Pacific/Auckland

		// WHEN
		startupWithSchedule(WEEKDAY_HOURS, "Pacific/Auckland");

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio on inside window")
			.isTrue()
			;
		and.then(actions())
			.as("Radio enabled")
			.containsExactly("enable")
			;
		and.then(service.currentStatus())
			.as("Schedule active")
			.returns(true, from(BluetoothSetupStatus::isScheduleActive))
			.as("No schedule error")
			.returns(null, from(BluetoothSetupStatus::getScheduleError))
			.as("Time zone")
			.returns("Pacific/Auckland", from(BluetoothSetupStatus::getTimeZone))
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void schedule_outsideWindow_staysOff() throws IOException {
		// GIVEN
		// 00:00 UTC is outside the window

		// WHEN
		startupWithSchedule(WEEKDAY_HOURS, "UTC");

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off outside window")
			.isFalse()
			;
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		and.then(service.currentStatus())
			.as("Schedule not active")
			.returns(false, from(BluetoothSetupStatus::isScheduleActive))
			.as("next window start")
			.returns(START.plus(Duration.ofHours(7)).toEpochMilli(), from(BluetoothSetupStatus::getScheduleNext))
			;
		// @formatter:on
	}

	@Test
	public void schedule_blankZone_usesSystemDefault() throws IOException {
		// WHEN
		startupWithSchedule(WEEKDAY_HOURS, "");

		// THEN
		final boolean expected = AvailabilitySchedule.parse(WEEKDAY_HOURS)
				.applies(LocalDateTime.ofInstant(START, ZoneId.systemDefault()));
		// @formatter:off
		and.then(service.currentStatus())
			.as("System default time zone used")
			.returns(ZoneId.systemDefault().getId(), from(BluetoothSetupStatus::getTimeZone))
			;
		and.then(radioState())
			.as("Radio follows schedule in system time zone")
			.isEqualTo(expected)
			;
		// @formatter:on
	}

	@Test
	public void schedule_invalidZone_failsClosed() throws IOException {
		// WHEN
		startupWithSchedule(SCHED_HEADER + ",,,,1", "Mars/Olympus");

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off with invalid zone")
			.isFalse()
			;
		and.then(service.currentStatus())
			.as("No time zone")
			.returns(null, from(BluetoothSetupStatus::getTimeZone))
			.as("Schedule not active")
			.returns(false, from(BluetoothSetupStatus::isScheduleActive))
			.as("Schedule error names the zone")
			.extracting(BluetoothSetupStatus::getScheduleError, STRING)
			.contains("Mars/Olympus")
			;
		// @formatter:on
	}

	@Test
	public void schedule_invalid_failsClosed() throws IOException {
		// WHEN
		startupWithSchedule(INVALID_HOURS, "UTC");

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off with invalid schedule")
			.isFalse()
			;
		and.then(service.currentStatus())
			.as("Schedule error names the row")
			.extracting(BluetoothSetupStatus::getScheduleError, STRING)
			.contains("CSV row 2")
			;
		and.then(service.getSettingSpecifiers().get(0))
			.as("Status title setting first")
			.asInstanceOf(type(TitleSettingSpecifier.class))
			.as("Status title shows the schedule is invalid")
			.extracting(TitleSettingSpecifier::getDefaultValue, STRING)
			.contains("invalid")
			;
		// @formatter:on

		// WHEN
		// the instruction result carries the error too
		InstructionStatus status = service.processInstruction(instruction("status", null));

		// THEN
		// @formatter:off
		and.then(serviceResult(status))
			.as("Instruction result schedule error names the row")
			.extracting(BluetoothSetupStatus::getScheduleError, STRING)
			.contains("CSV row 2")
			;
		// @formatter:on
	}

	@Test
	public void schedule_emptyWithAlwaysOnOff_behavesLikeBefore() throws IOException {
		// WHEN
		startupWithSchedule("", "UTC");

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off")
			.isFalse()
			;
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		and.then(service.currentStatus())
			.as("Empty schedule")
			.returns("", from(BluetoothSetupStatus::getSchedule))
			;
		// @formatter:on
	}

	@Test
	public void schedule_withAlwaysOn_ignored() throws IOException {
		// GIVEN
		BluetoothSetupService s = new BluetoothSetupService(opModesService, taskScheduler, null);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setWatchdogIntervalSeconds(0);
		s.setSchedule(WEEKDAY_HOURS);
		s.setTimeZone("UTC"); // window closed at 00:00 UTC

		// WHEN
		s.serviceDidStartup();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("always on wins over a closed window")
			.isTrue()
			;
		// no schedule tick registered while always on
		then(taskScheduler)
			.shouldHaveNoInteractions()
			;
		// @formatter:on
		s.serviceDidShutdown();
	}

	@Test
	public void scheduleTick_reconcilesOnlyOnEdge() throws IOException {
		// GIVEN
		advanceClock(Duration.ofHours(6).plusMinutes(59));
		startupWithSchedule(WEEKDAY_HOURS, "UTC");

		// WHEN
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off before window")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofMinutes(1)); // 07:00
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio on at window start")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofMinutes(1)); // 07:01
		int linesBefore = rawLogLines();
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio still on")
			.isTrue()
			;
		and.then(rawLogLines())
			.as("no helper call at all on a non-edge tick")
			.isEqualTo(linesBefore)
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofHours(10).plusMinutes(59)); // 18:00
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off at window end")
			.isFalse()
			;
		and.then(actions())
			.as("exactly one enable and one disable")
			.containsExactly("enable", "disable")
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void startup_insideWindow_turnsOn() throws IOException {
		// GIVEN
		advanceClock(Duration.ofHours(12)); // 12:00 UTC, a reboot mid-window

		// WHEN
		startupWithSchedule(WEEKDAY_HOURS, "UTC");

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio on inside window")
			.isTrue()
			;
		and.then(actions())
			.as("Radio enabled")
			.containsExactly("enable")
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void dst_springForward_gapWindowNeverApplies() throws IOException {
		// GIVEN
		// NZ clocks go from 02:00 NZST to 03:00 NZDT on Sunday 2026-09-27
		service.setClock(Clock.fixed(Instant.parse("2026-09-26T13:59:00Z"), ZoneOffset.UTC)); // 01:59 NZST

		// WHEN
		startupWithSchedule(SCHED_HEADER + ",,Sun,02:00-03:00,1", "Pacific/Auckland");

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off before gap")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		service.setClock(Clock.fixed(Instant.parse("2026-09-26T14:00:00Z"), ZoneOffset.UTC)); // 03:00 NZDT
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off after gap")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		service.setClock(Clock.fixed(Instant.parse("2026-09-26T14:30:00Z"), ZoneOffset.UTC)); // 03:30 NZDT
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("window inside the gap never applies")
			.isFalse()
			;
		and.then(actions())
			.as("Radio not changed")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void dst_fallBack_repeatedHourAppliesTwice() throws IOException {
		// GIVEN
		// NZ clocks go from 03:00 NZDT back to 02:00 NZST on Sunday 2027-04-04
		service.setClock(Clock.fixed(Instant.parse("2027-04-03T13:30:00Z"), ZoneOffset.UTC)); // 02:30 NZDT

		// WHEN
		startupWithSchedule(SCHED_HEADER + ",,Sun,02:00-03:00,1", "Pacific/Auckland");

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("first 02:30")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.setClock(Clock.fixed(Instant.parse("2027-04-03T14:30:00Z"), ZoneOffset.UTC)); // 02:30 NZST
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("second 02:30")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.setClock(Clock.fixed(Instant.parse("2027-04-03T15:00:00Z"), ZoneOffset.UTC)); // 03:00 NZST
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Radio off at 03:00 NZST")
			.isFalse()
			;
		and.then(actions())
			.as("Radio enabled then disabled")
			.containsExactly("enable", "disable")
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void instruction_enable_overridesClosedWindow() throws IOException {
		// GIVEN
		startupWithSchedule(WEEKDAY_HOURS, "UTC"); // closed at 00:00 UTC
		// @formatter:off
		and.then(radioState())
			.as("Radio off outside window")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		InstructionStatus status = service
				.processInstruction(instruction("enable", Map.of("duration", "600")));

		// THEN
		// @formatter:off
		then(opModesService).should()
			.enableOperationalModes(eq(Set.of(OP_MODE)), expireCaptor.capture())
			;
		and.then(expireCaptor.getValue())
			.as("Op mode enabled for requested duration")
			.isEqualTo(START.plusSeconds(600))
			;
		and.then(status)
			.as("Status completed")
			.returns(InstructionState.Completed, from(InstructionStatus::getInstructionState))
			;
		and.then(radioState())
			.as("override turns the radio on outside the window")
			.isTrue()
			;
		and.then(serviceResult(status))
			.as("Schedule not active")
			.returns(false, from(BluetoothSetupStatus::isScheduleActive))
			.as("Op mode active")
			.returns(true, from(BluetoothSetupStatus::isOpModeActive))
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void instruction_disable_insideWindow_keepsRadioOn() throws IOException {
		// GIVEN
		startupWithSchedule(WEEKDAY_HOURS, "Pacific/Auckland"); // 13:00 NZDT, open
		// @formatter:off
		and.then(radioState())
			.as("Radio on inside window")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		InstructionStatus status = service.processInstruction(instruction("disable", null));

		// THEN
		// @formatter:off
		and.then(status)
			.as("Status completed")
			.returns(InstructionState.Completed, from(InstructionStatus::getInstructionState))
			;
		and.then(radioState())
			.as("schedule keeps the radio on")
			.isTrue()
			;
		and.then(actions())
			.as("Radio only enabled")
			.containsExactly("enable")
			;
		then(opModesService).should()
			.disableOperationalModes(Set.of(OP_MODE))
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		// @formatter:on
	}

	@Test
	public void status_includesScheduleFields() throws IOException {
		// GIVEN
		advanceClock(Duration.ofHours(13)); // 13:00 UTC, inside
		startupWithSchedule(SCHED_HEADER + ",,mon-fri,7-18,1", "UTC");

		// WHEN
		BluetoothSetupStatus s = service.currentStatus();

		// THEN
		// @formatter:off
		and.then(s)
			.as("Normalised schedule")
			.returns(",,Mon-Fri,7-18,1", from(BluetoothSetupStatus::getSchedule))
			.as("Schedule active")
			.returns(true, from(BluetoothSetupStatus::isScheduleActive))
			.as("No schedule error")
			.returns(null, from(BluetoothSetupStatus::getScheduleError))
			.as("Time zone")
			.returns("UTC", from(BluetoothSetupStatus::getTimeZone))
			.as("next boundary is the window end")
			.returns(START.plus(Duration.ofHours(18)).toEpochMilli(), from(BluetoothSetupStatus::getScheduleNext))
			.as("Install enabled")
			.returns(true, from(BluetoothSetupStatus::isInstallEnabled))
			;
		// @formatter:on
	}

	@Test
	public void scheduleTick_retriesAfterHelperFailure() throws IOException {
		// GIVEN
		advanceClock(Duration.ofHours(7)); // 07:00, inside the window
		File failFile = new File(tmpFile.getAbsolutePath() + ".failenable");
		try {
			Files.writeString(failFile.toPath(), "");

			// WHEN
			startupWithSchedule(WEEKDAY_HOURS, "UTC");

			// THEN
			// @formatter:off
			and.then(radioState())
				.as("enable failed")
				.isFalse()
				;
			// @formatter:on

			// WHEN
			service.scheduleTick();

			// THEN
			// @formatter:off
			and.then(radioState())
				.as("still failing")
				.isFalse()
				;
			// @formatter:on
		} finally {
			failFile.delete();
		}

		// WHEN
		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("edge retried once the helper works")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void configurationChanged_registersAndCancelsTick() throws IOException {
		// GIVEN
		willReturn(future, future2).given(taskScheduler).scheduleWithFixedDelay(any(Runnable.class),
				any(Instant.class), any(Duration.class));

		BluetoothSetupService s = new BluetoothSetupService(opModesService, taskScheduler, null);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setWatchdogIntervalSeconds(0);
		s.setAlwaysOn(false);
		s.setSchedule(WEEKDAY_HOURS);
		s.setTimeZone("UTC");
		s.serviceDidStartup(); // registers future

		// WHEN
		s.setAlwaysOn(true);
		s.configurationChanged(Map.of()); // cancels future, radio on

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Always on turns radio on")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		s.setAlwaysOn(false);
		s.configurationChanged(Map.of()); // registers future2, radio off (00:00 UTC)

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Closed window turns radio off")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		s.setSchedule(INVALID_HOURS);
		s.configurationChanged(Map.of()); // invalid: cancels future2, stays off

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Invalid schedule keeps radio off")
			.isFalse()
			;
		and.then(s.currentStatus())
			.as("Schedule error reported")
			.extracting(BluetoothSetupStatus::getScheduleError)
			.isNotNull()
			;
		// @formatter:on

		// WHEN
		s.serviceDidShutdown();

		// THEN
		final InOrder order = inOrder(taskScheduler, future, future2);
		// @formatter:off
		then(taskScheduler).should(order)
			.scheduleWithFixedDelay(any(Runnable.class), any(Instant.class), eq(SCHEDULE_TICK))
			;
		then(future).should(order)
			.cancel(false)
			;
		then(taskScheduler).should(order)
			.scheduleWithFixedDelay(any(Runnable.class), any(Instant.class), eq(SCHEDULE_TICK))
			;
		then(future2).should(order)
			.cancel(false)
			;
		then(taskScheduler)
			.shouldHaveNoMoreInteractions()
			;
		then(future)
			.shouldHaveNoMoreInteractions()
			;
		then(future2)
			.shouldHaveNoMoreInteractions()
			;
		// @formatter:on
	}

	@Test
	public void watchdogDisabled_scheduleStillTicks() throws IOException {
		// GIVEN
		willReturn(future).given(taskScheduler).scheduleWithFixedDelay(any(Runnable.class),
				any(Instant.class), any(Duration.class));

		BluetoothSetupService s = new BluetoothSetupService(opModesService, taskScheduler, null);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setAlwaysOn(false);
		s.setWatchdogIntervalSeconds(0);
		s.setSchedule(WEEKDAY_HOURS);
		s.setTimeZone("UTC");

		// WHEN
		s.serviceDidStartup();
		s.serviceDidShutdown();

		// THEN
		// @formatter:off
		then(taskScheduler).should()
			.scheduleWithFixedDelay(any(Runnable.class), any(Instant.class), eq(SCHEDULE_TICK))
			;
		then(future).should()
			.cancel(false)
			;
		then(taskScheduler)
			.shouldHaveNoMoreInteractions()
			;
		then(future)
			.shouldHaveNoMoreInteractions()
			;
		// @formatter:on
	}

	/** An optional metadata service whose service can be swapped in a test. */
	private static final class SwitchableService implements OptionalFilterableService<MetadataService> {

		private MetadataService service;
		private final Map<String, Object> filters = new ConcurrentHashMap<>(2);

		private SwitchableService(MetadataService service) {
			super();
			this.service = service;
		}

		@Override
		public MetadataService service() {
			return service;
		}

		@Override
		public Map<String, ?> getPropertyFilters() {
			return filters;
		}

		@Override
		public void setPropertyFilter(String key, Object value) {
			if ( value == null ) {
				filters.remove(key);
			} else {
				filters.put(key, value);
			}
		}

		@Override
		public Object removePropertyFilter(String key) {
			return filters.remove(key);
		}
	}

	private static final String META_PATH = "/pm/bluetooth/schedule";

	private static final String ALWAYS = SCHED_HEADER + ",,,,1";

	private static GeneralDatumMetadata meta(Object schedule) {
		GeneralDatumMetadata m = new GeneralDatumMetadata();
		if ( schedule != null ) {
			m.putInfoValue("bluetooth", "schedule", schedule);
		}
		return m;
	}

	private void startWithMetadata(String inline) {
		service.setMetadataPath(META_PATH);
		service.setSchedule(inline);
		service.setTimeZone("UTC");
		service.serviceDidStartup();
	}

	private String statusTitleText() {
		SettingSpecifier title = service.getSettingSpecifiers().get(0);
		return String.valueOf(((TitleSettingSpecifier) title).getDefaultValue());
	}

	@Test
	public void metadata_loadsAndOverridesInline() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(ALWAYS));

		// WHEN
		// the inline schedule is closed at 00:00 UTC Tuesday; the metadata one is open
		startWithMetadata(WEEKDAY_HOURS);

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("metadata schedule turns radio on")
			.isTrue()
			;
		and.then(service.currentStatus())
			.as("Schedule from metadata")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_METADATA, from(BluetoothSetupStatus::getScheduleSource))
			.as("No metadata error")
			.returns(null, from(BluetoothSetupStatus::getScheduleMetadataError))
			.as("No schedule error")
			.returns(null, from(BluetoothSetupStatus::getScheduleError))
			.as("Metadata schedule in effect")
			.returns(",,,,1", from(BluetoothSetupStatus::getSchedule))
			;
		and.then(statusTitleText())
			.as("Status title shows metadata source")
			.contains("schedule from metadata '" + META_PATH + "'")
			;
		// @formatter:on

		// WHEN
		InstructionStatus status = service.processInstruction(instruction("status", null));

		// THEN
		// @formatter:off
		and.then(serviceResult(status))
			.as("Instruction result schedule from metadata")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_METADATA, from(BluetoothSetupStatus::getScheduleSource))
			;
		then(metadataService).should()
			.getAllMetadata()
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void metadata_notConfigured_noRead() throws IOException {
		// GIVEN
		// a path with no service UID is not configured either
		service.setMetadataServiceUid("");

		// WHEN
		startWithMetadata(WEEKDAY_HOURS);
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("Schedule from setting")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_SETTING, from(BluetoothSetupStatus::getScheduleSource))
			.as("No metadata error")
			.returns(null, from(BluetoothSetupStatus::getScheduleMetadataError))
			;
		and.then(radioState())
			.as("Inline schedule closed")
			.isFalse()
			;
		then(metadataService).should(never())
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_noService_retriesWithoutBackoff() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(ALWAYS));
		metadataServiceRef.service = null;

		// WHEN
		startWithMetadata(WEEKDAY_HOURS);

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("inline used")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_SETTING, from(BluetoothSetupStatus::getScheduleSource))
			.as("Metadata error reports no service")
			.extracting(BluetoothSetupStatus::getScheduleMetadataError, STRING)
			.contains("no Metadata Service")
			;
		and.then(radioState())
			.as("Inline schedule closed")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		metadataServiceRef.service = metadataService;
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("loaded on the next tick")
			.isTrue()
			;
		then(metadataService).should()
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_unreachableBeforeLoad_usesInline() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(null).willThrow(new RuntimeException("boom"));

		// WHEN
		startWithMetadata(ALWAYS);

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("Schedule from setting")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_SETTING, from(BluetoothSetupStatus::getScheduleSource))
			.as("Metadata error reports unavailable")
			.extracting(BluetoothSetupStatus::getScheduleMetadataError, STRING)
			.contains("metadata unavailable")
			;
		and.then(radioState())
			.as("inline schedule applies")
			.isTrue()
			;
		and.then(statusTitleText())
			.as("Status title shows metadata not loaded")
			.contains("not loaded")
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofSeconds(BluetoothSetupService.METADATA_RETRY_SECONDS + 1));
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("Schedule from setting")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_SETTING, from(BluetoothSetupStatus::getScheduleSource))
			.as("Metadata error reports exception")
			.extracting(BluetoothSetupStatus::getScheduleMetadataError, STRING)
			.contains("boom")
			;
		and.then(radioState())
			.as("inline schedule still applies")
			.isTrue()
			;
		then(metadataService).should(times(2))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_unreachableAfterLoad_keepsLastSchedule() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(ALWAYS))
				.willThrow(new RuntimeException("offline"));
		startWithMetadata(WEEKDAY_HOURS);
		// @formatter:off
		and.then(radioState())
			.as("metadata schedule turns radio on")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("last result kept")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_METADATA, from(BluetoothSetupStatus::getScheduleSource))
			.as("Metadata error reports exception")
			.extracting(BluetoothSetupStatus::getScheduleMetadataError, STRING)
			.contains("offline")
			;
		and.then(radioState())
			.as("Radio still on")
			.isTrue()
			;
		and.then(statusTitleText())
			.as("Status title shows last result kept")
			.contains("keeping the last result")
			;
		then(metadataService).should(times(2))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_invalidThenUnreachable_staysClosed() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(ALWAYS), meta("Mon-Fri 07:00-18:00"))
				.willThrow(new RuntimeException("offline"));
		startWithMetadata(ALWAYS);
		// @formatter:off
		and.then(radioState())
			.as("valid metadata schedule")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("invalid push closes the radio")
			.isFalse()
			;
		and.then(service.currentStatus())
			.as("Schedule error reports invalid metadata")
			.extracting(BluetoothSetupStatus::getScheduleError, STRING)
			.contains("metadata schedule invalid")
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("outage does not restore the older schedule")
			.isFalse()
			;
		and.then(service.currentStatus())
			.as("Schedule from metadata")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_METADATA, from(BluetoothSetupStatus::getScheduleSource))
			.as("Schedule error still reports invalid metadata")
			.extracting(BluetoothSetupStatus::getScheduleError, STRING)
			.contains("metadata schedule invalid")
			;
		then(metadataService).should(times(3))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_pathAbsent_usesInlineWithoutBackoff() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(null));

		// WHEN
		startWithMetadata(ALWAYS);
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("Schedule from setting")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_SETTING, from(BluetoothSetupStatus::getScheduleSource))
			.as("Metadata error reports absent path")
			.returns("no schedule at " + META_PATH, from(BluetoothSetupStatus::getScheduleMetadataError))
			;
		and.then(radioState())
			.as("Inline schedule applies")
			.isTrue()
			;
		then(metadataService).should(times(2))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_invalid_failsClosedDespiteInline() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata())
				.willReturn(meta(SCHED_HEADER + ",,Mon-Fri,22:00-06:00,1"));

		// WHEN
		startWithMetadata(ALWAYS);

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("Schedule from metadata")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_METADATA, from(BluetoothSetupStatus::getScheduleSource))
			.as("Schedule error reports the invalid range")
			.extracting(BluetoothSetupStatus::getScheduleError, STRING)
			.contains("metadata schedule invalid")
			.as("Schedule error names the descending range")
			.contains("is descending")
			;
		and.then(statusTitleText())
			.as("Status title shows the schedule is invalid")
			.contains("schedule invalid")
			.as("No rule table for an invalid schedule")
			.doesNotContain("<table")
			.as("No empty schedule message for an invalid schedule")
			.doesNotContain("The schedule has no rules.")
			;
		and.then(radioState())
			.as("invalid metadata never applies")
			.isFalse()
			;
		then(metadataService).should()
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_headerOnly_closesRadio() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata())
				.willReturn(meta(List.of(List.of("Month", "Day", "Weekday", "Time", "Available"))));

		// WHEN
		startWithMetadata(ALWAYS);

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("Schedule from metadata")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_METADATA, from(BluetoothSetupStatus::getScheduleSource))
			.as("No schedule error")
			.returns(null, from(BluetoothSetupStatus::getScheduleError))
			;
		and.then(radioState())
			.as("empty metadata schedule turns scheduled availability off")
			.isFalse()
			;
		then(metadataService).should()
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_validWithInvalidInline() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(ALWAYS));

		// WHEN
		startWithMetadata(INVALID_HOURS);

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("No schedule error")
			.returns(null, from(BluetoothSetupStatus::getScheduleError))
			;
		and.then(radioState())
			.as("Metadata schedule applies")
			.isTrue()
			;
		then(metadataService).should()
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_arrayForm() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata())
				.willReturn(meta(List.of(List.of("Month", "Day", "Weekday", "Time", "Available"),
						Arrays.asList(null, null, "Mon-Fri", "00:00-06:00", 1))));

		// WHEN
		startWithMetadata(WEEKDAY_HOURS);

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("Array form normalised")
			.returns(",,Mon-Fri,0-6,1", from(BluetoothSetupStatus::getSchedule))
			;
		and.then(radioState())
			.as("Metadata schedule applies")
			.isTrue()
			;
		then(metadataService).should()
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_valueChange_pickedUpOnNextTick() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(ALWAYS), meta(WEEKDAY_HOURS));
		startWithMetadata("");
		// @formatter:off
		and.then(radioState())
			.as("Metadata schedule applies")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		// as after a clear-cache signal: the next read returns the new value
		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("New metadata schedule applies")
			.isFalse()
			;
		and.then(actions())
			.as("Radio enabled then disabled")
			.containsExactly("enable", "disable")
			;
		then(metadataService).should(times(2))
			.getAllMetadata()
			;
		then(opModesService).should(never())
			.enableOperationalModes(any(), any())
			;
		then(opModesService).should(never())
			.disableOperationalModes(any())
			;
		// @formatter:on
	}

	@Test
	public void metadata_backoffAfterUnreachable() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(null, meta(ALWAYS));
		startWithMetadata(WEEKDAY_HOURS);

		// WHEN
		advanceClock(Duration.ofSeconds(BluetoothSetupService.METADATA_RETRY_SECONDS - 1));
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("no read during back-off")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		advanceClock(Duration.ofSeconds(2));
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("read after back-off")
			.isTrue()
			;
		then(metadataService).should(times(2))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_configChange_clearsBackoff() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(null, meta(ALWAYS));
		startWithMetadata(WEEKDAY_HOURS);

		// WHEN
		service.configurationChanged(Map.of());

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Metadata read again right away")
			.isTrue()
			;
		then(metadataService).should(times(2))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_pathChange_dropsOldSchedule() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(ALWAYS));
		startWithMetadata(WEEKDAY_HOURS);
		// @formatter:off
		and.then(radioState())
			.as("Metadata schedule applies")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.setMetadataPath("/pm/bluetooth/other");
		service.configurationChanged(Map.of());

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("old path's schedule not kept")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_SETTING, from(BluetoothSetupStatus::getScheduleSource))
			.as("Metadata error reports absent new path")
			.returns("no schedule at /pm/bluetooth/other", from(BluetoothSetupStatus::getScheduleMetadataError))
			;
		and.then(radioState())
			.as("Inline schedule closed")
			.isFalse()
			;
		then(metadataService).should(times(2))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_pathCleared_revertsToInline() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(ALWAYS));
		startWithMetadata(WEEKDAY_HOURS);
		// @formatter:off
		and.then(radioState())
			.as("Metadata schedule applies")
			.isTrue()
			;
		// @formatter:on

		// WHEN
		service.setMetadataPath("");
		service.configurationChanged(Map.of());

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("Schedule from setting")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_SETTING, from(BluetoothSetupStatus::getScheduleSource))
			.as("No metadata error")
			.returns(null, from(BluetoothSetupStatus::getScheduleMetadataError))
			;
		and.then(radioState())
			.as("Inline schedule closed")
			.isFalse()
			;
		then(metadataService).should()
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_staleResultDropped() throws IOException {
		// GIVEN
		final GeneralDatumMetadata both = meta(ALWAYS);
		both.putInfoValue("bluetooth", "other", SCHED_HEADER);
		// the first read is overtaken by a reconfiguration to another path
		given(metadataService.getAllMetadata()).willAnswer(invocation -> {
			service.setMetadataPath("/pm/bluetooth/other");
			service.configurationChanged(Map.of());
			return both;
		}).willReturn(both);

		// WHEN
		startWithMetadata(WEEKDAY_HOURS);

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("newer path applies")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_METADATA, from(BluetoothSetupStatus::getScheduleSource))
			.as("old path's open schedule dropped")
			.returns("", from(BluetoothSetupStatus::getSchedule))
			;
		and.then(radioState())
			.as("Newer path's empty schedule closed")
			.isFalse()
			;
		then(metadataService).should(times(2))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_readsWithoutHoldingLock() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willAnswer(invocation -> {
			// @formatter:off
			and.then(Thread.holdsLock(service))
				.as("service lock not held while reading metadata")
				.isFalse()
				;
			// @formatter:on
			return meta(ALWAYS);
		});

		// WHEN
		startWithMetadata(WEEKDAY_HOURS);
		service.configurationChanged(Map.of());
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Metadata schedule applies")
			.isTrue()
			;
		then(metadataService).should(times(3))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_alwaysOn_noScheduleTickOrRead() throws IOException {
		// GIVEN
		BluetoothSetupService s = new BluetoothSetupService(opModesService, taskScheduler,
				metadataServiceRef);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setWatchdogIntervalSeconds(0);
		s.setAlwaysOn(true);
		s.setMetadataPath(META_PATH);

		// WHEN
		s.serviceDidStartup();

		// THEN
		// with Always On nothing is scheduled and no metadata is read
		// @formatter:off
		then(taskScheduler)
			.shouldHaveNoInteractions()
			;
		then(future)
			.shouldHaveNoInteractions()
			;
		then(metadataService).should(never())
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_pathOnly_registersScheduleTick() throws IOException {
		// GIVEN
		willReturn(future).given(taskScheduler).scheduleWithFixedDelay(any(Runnable.class),
				any(Instant.class), any(Duration.class));
		willReturn(future).given(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

		BluetoothSetupService s = new BluetoothSetupService(opModesService, taskScheduler,
				metadataServiceRef);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setWatchdogIntervalSeconds(0);
		s.setAlwaysOn(false);
		s.setTimeZone("UTC");
		s.setMetadataPath(META_PATH);

		// WHEN
		s.serviceDidStartup();

		// THEN
		// @formatter:off
		then(taskScheduler).should()
			.scheduleWithFixedDelay(any(Runnable.class), any(Instant.class), eq(SCHEDULE_TICK))
			;
		then(taskScheduler).should()
			.schedule(any(Runnable.class), any(Instant.class))
			;
		then(taskScheduler)
			.shouldHaveNoMoreInteractions()
			;
		then(future)
			.shouldHaveNoInteractions()
			;
		then(metadataService).should(never())
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_errorDoesNotBlockLaterReads() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willThrow(new LinkageError("bundle refreshed"))
				.willReturn(meta(ALWAYS));

		// WHEN
		// @formatter:off
		and.thenThrownBy(() -> startWithMetadata(WEEKDAY_HOURS))
			.as("Error should propagate")
			.isInstanceOf(LinkageError.class)
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("Inline schedule closed")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(radioState())
			.as("the next tick reads again")
			.isTrue()
			;
		then(metadataService).should(times(2))
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_shutdown_stopsTick() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata()).willReturn(meta(ALWAYS));
		startWithMetadata(WEEKDAY_HOURS);
		// @formatter:off
		and.then(radioState())
			.as("Metadata schedule applies")
			.isTrue()
			;
		// @formatter:on
		final int lines = rawLogLines();

		// WHEN
		service.serviceDidShutdown();
		service.scheduleTick();

		// THEN
		// @formatter:off
		and.then(rawLogLines())
			.as("no metadata read or helper call after shutdown")
			.isEqualTo(lines)
			;
		then(metadataService).should()
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void metadata_missingHeader_firstRowIsHeader() throws IOException {
		// GIVEN
		// without a header row the only rule is taken as the header
		given(metadataService.getAllMetadata()).willReturn(meta(",,,,1"));

		// WHEN
		startWithMetadata(ALWAYS);

		// THEN
		// @formatter:off
		and.then(service.currentStatus())
			.as("Schedule from metadata")
			.returns(BluetoothSetupService.SCHEDULE_SOURCE_METADATA, from(BluetoothSetupStatus::getScheduleSource))
			.as("Not an error")
			.returns(null, from(BluetoothSetupStatus::getScheduleError))
			.as("No rules left")
			.returns("", from(BluetoothSetupStatus::getSchedule))
			;
		and.then(statusTitleText())
			.as("Status title shows the schedule has no rules, so the lost rule can be noticed")
			.contains("<p>The schedule has no rules.</p>")
			.as("No rule table")
			.doesNotContain("<table")
			;
		and.then(radioState())
			.as("Radio off: the lost rule never applies")
			.isFalse()
			;
		then(metadataService).should()
			.getAllMetadata()
			;
		// @formatter:on
	}

	@Test
	public void status_scheduleTable() {
		// GIVEN
		// START is 2026-10-06T00:00Z, a Tuesday; 13:00 NZDT in Pacific/Auckland
		final String sched = WEEKDAY_HOURS + "\n,,\"Sat,Sun\",08:00-12:00,1";

		// WHEN
		startupWithSchedule(sched, "Pacific/Auckland");

		// THEN
		// @formatter:off
		and.then(service.getSettingSpecifiers().get(0))
			.as("Status title setting first")
			.asInstanceOf(type(TitleSettingSpecifier.class))
			.as("Status title is markup")
			.returns(true, from(TitleSettingSpecifier::isMarkup))
			;
		and.then(statusTitle())
			.as("Status message first")
			.startsWith("<p>On (schedule ")
			.as("Rule table rendered")
			.contains("<table class=\"table counts\">")
			.as("Rule column header")
			.contains("<th>Rule</th>")
			.as("Availability column header")
			.contains("<th>Available</th>")
			.as("First rule row")
			.contains("<tr><th>1</th>")
			.as("Second rule row")
			.contains("<tr><th>2</th>")
			.as("No third rule row")
			.doesNotContain("<tr><th>3</th>")
			.as("Weekday range shown")
			.contains("<td>Mon-Fri</td>")
			.as("Time range shown")
			.contains("<td>7-18</td>")
			.as("Matching rule in the time zone")
			.contains("<p>Rule 1 matches at ")
			;
		// @formatter:on
	}

	@Test
	public void status_scheduleTable_noRuleMatches() {
		// GIVEN
		// START is 2026-10-06T00:00Z, a Tuesday, outside the window in UTC

		// WHEN
		startupWithSchedule(WEEKDAY_HOURS, "UTC");

		// THEN
		// @formatter:off
		and.then(statusTitle())
			.as("Rule table rendered")
			.contains("<tr><th>1</th>")
			.as("No rule matches now")
			.contains("<p>No rule matches at ")
			.as("No active rule")
			.doesNotContain("Rule 1 matches")
			;
		// @formatter:on
	}

	@Test
	public void status_scheduleTable_invalidZone() {
		// WHEN
		startupWithSchedule(WEEKDAY_HOURS, "Mars/Olympus");

		// THEN
		// @formatter:off
		and.then(statusTitle())
			.as("Rule table rendered")
			.contains("<tr><th>1</th>")
			.as("No matching rule without a valid time zone")
			.doesNotContain("matches at")
			;
		// @formatter:on
	}

	@Test
	public void status_noSchedule() {
		// WHEN
		startupWithSchedule("", "UTC");

		// THEN
		// @formatter:off
		and.then(statusTitle())
			.as("Only the status message")
			.isEqualTo("<p>Off</p>")
			;
		// @formatter:on
	}

	@Test
	public void status_scheduleEmpty() {
		// GIVEN
		// without a header row the only rule is taken as the header
		final String sched = ",,Mon-Fri,07:00-18:00,1";

		// WHEN
		startupWithSchedule(sched, "UTC");

		// THEN
		// @formatter:off
		and.then(statusTitle())
			.as("Empty schedule message")
			.contains("<p>The schedule has no rules.</p>")
			.as("No rule table")
			.doesNotContain("<table")
			;
		// @formatter:on
	}

	@Test
	public void status_alwaysOn_noSchedule() {
		// GIVEN
		service.setAlwaysOn(true);

		// WHEN
		startupWithSchedule("", "UTC");

		// THEN
		// @formatter:off
		and.then(statusTitle())
			.as("Always on, with no schedule shown")
			.isEqualTo("<p>On (always on)</p>")
			;
		// @formatter:on
	}

	@Test
	public void status_alwaysOn_scheduleTable() {
		// GIVEN
		service.setAlwaysOn(true);

		// WHEN
		startupWithSchedule(WEEKDAY_HOURS, "UTC");

		// THEN
		// @formatter:off
		and.then(statusTitle())
			.as("Always on first")
			.startsWith("<p>On (always on; schedule (UTC) next allows at ")
			.as("Schedule rules not inline")
			.doesNotContain("Mon-Fri,7-18")
			.as("Rule table still rendered")
			.contains("<tr><th>1</th>")
			.as("Weekday range shown in the table")
			.contains("<td>Mon-Fri</td>")
			;
		// @formatter:on
	}

	@Test
	public void status_escapesText() throws IOException {
		// GIVEN
		given(metadataService.getAllMetadata())
				.willReturn(meta(List.of(List.of("Month", "Day", "Weekday", "Time", "<i>Available</i>"),
						List.of("", "", "", "", "1"))));
		service.setMetadataPath(META_PATH);
		service.setTimeZone("UTC");

		// WHEN
		service.serviceDidStartup();

		// THEN
		// @formatter:off
		and.then(statusTitleText())
			.as("Rule table rendered")
			.contains("<tr><th>1</th>")
			.as("Rate header from metadata escaped")
			.contains("<th>&lt;i&gt;Available&lt;/i&gt;</th>")
			.as("Raw rate header markup not present")
			.doesNotContain("<i>")
			;
		// @formatter:on

		// WHEN
		service.setMetadataPath("/pm/<b>/schedule");
		service.configurationChanged(Map.of());

		// THEN
		// @formatter:off
		and.then(statusTitleText())
			.as("Status message escaped")
			.contains("/pm/&lt;b&gt;/schedule")
			.as("Raw path markup not present")
			.doesNotContain("<b>")
			;
		// @formatter:on
	}

}
