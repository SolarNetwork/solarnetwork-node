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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
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
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
public class BluetoothSetupServiceTests {

	private static final String SCHED_HEADER = AvailabilitySchedule.EXAMPLE_HEADER + "\n";

	private static final String WEEKDAY_HOURS = SCHED_HEADER + ",,Mon-Fri,07:00-18:00,1";

	private static final String INVALID_HOURS = SCHED_HEADER + ",,Mon-Fri,25:00-26:00,1";

	private static final String OP_MODE = "bt-setup";

	private static final String OFFLINE_MODE = BluetoothSetupService.DEFAULT_OFFLINE_OP_MODE;
	private static final Instant START = Instant.parse("2026-10-06T00:00:00Z");

	private OperationalModesService opModesService;
	private MetadataService metadataService;
	private SwitchableService metadataServiceRef;
	private BluetoothSetupService service;
	private File tmpFile;
	private File logFile;
	private File stateFile;

	@Before
	public void setup() throws IOException {
		opModesService = EasyMock.createMock(OperationalModesService.class);
		metadataService = EasyMock.createMock(MetadataService.class);
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
		msgSource.setBasenames(BluetoothSetupService.class.getName());
		service.setMessageSource(msgSource);
	}

	@After
	public void teardown() {
		verify(opModesService, metadataService);
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

	private void replayAll() {
		replay(opModesService, metadataService);
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

	private Event modesEvent(String... activeModes) {
		Map<String, Object> props = new HashMap<>();
		props.put(OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES, Set.of(activeModes));
		return new Event(OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED, props);
	}

	@Test
	public void handlesTopic() {
		replayAll();
		assertThat(service.handlesTopic(InstructionHandler.TOPIC_SYSTEM_CONFIGURE), is(true));
		assertThat(service.handlesTopic("Other"), is(false));
	}

	@Test
	public void instruction_otherService_ignored() {
		replayAll();
		Map<String, String> params = new LinkedHashMap<>();
		params.put(InstructionHandler.PARAM_SERVICE, "/setup/network/wifi");
		Instruction instr = InstructionUtils
				.createLocalInstruction(InstructionHandler.TOPIC_SYSTEM_CONFIGURE, params);
		assertThat(service.processInstruction(instr), is(nullValue()));
	}

	@Test
	public void instruction_unsupportedAction_declined() {
		replayAll();
		InstructionStatus status = service.processInstruction(instruction("explode", null));
		assertThat(status, is(notNullValue()));
		assertThat(status.getInstructionState(), is(InstructionState.Declined));
		assertThat(status.getResultParameters().get(InstructionHandler.PARAM_MESSAGE),
				is(notNullValue()));
	}

	@Test
	public void instruction_status() throws IOException {
		replayAll();
		InstructionStatus status = service.processInstruction(instruction(null, null));
		assertThat(status.getInstructionState(), is(InstructionState.Completed));
		Object result = status.getResultParameters().get(InstructionHandler.PARAM_SERVICE_RESULT);
		assertThat(result, instanceOf(BluetoothSetupStatus.class));
		BluetoothSetupStatus s = (BluetoothSetupStatus) result;
		assertThat(s.isRadioActive(), is(false));
		assertThat(s.isOpModeActive(), is(false));
		assertThat(s.isOffline(), is(false));
		assertThat("status does not change the radio", actions(), is(empty()));
	}

	@Test
	public void instruction_enable_defaultDuration() throws IOException {
		service.setDefaultDurationSeconds(600);
		Capture<Instant> expireCapture = Capture.newInstance();
		expect(opModesService.enableOperationalModes(eq(Set.of(OP_MODE)), capture(expireCapture)))
				.andReturn(Set.of(OP_MODE));
		expect(opModesService.activeOperationalModesWithExpirations())
				.andReturn(Map.of(OP_MODE, START.plusSeconds(600).toEpochMilli())).anyTimes();
		replayAll();

		InstructionStatus status = service.processInstruction(instruction("enable", null));

		assertThat(status.getInstructionState(), is(InstructionState.Completed));
		assertThat(expireCapture.getValue(), is(START.plusSeconds(600)));
		BluetoothSetupStatus s = (BluetoothSetupStatus) status.getResultParameters()
				.get(InstructionHandler.PARAM_SERVICE_RESULT);
		assertThat(s.isRadioActive(), is(true));
		assertThat(s.isOpModeActive(), is(true));
		assertThat(s.getOpModeExpires(), is(START.plusSeconds(600).toEpochMilli()));
		assertThat(actions(), contains("enable"));
		assertThat(radioState(), is(true));
	}

	@Test
	public void instruction_enable_capsDuration() throws IOException {
		service.setMaxDurationSeconds(100);
		Capture<Instant> expireCapture = Capture.newInstance();
		expect(opModesService.enableOperationalModes(eq(Set.of(OP_MODE)), capture(expireCapture)))
				.andReturn(Set.of(OP_MODE));
		expect(opModesService.activeOperationalModesWithExpirations()).andReturn(Map.of()).anyTimes();
		replayAll();

		service.processInstruction(instruction("enable", Map.of("duration", "99999")));

		assertThat(expireCapture.getValue(), is(START.plusSeconds(100)));
	}

	@Test
	public void instruction_enable_invalidDuration_declined() throws IOException {
		replayAll();
		InstructionStatus status = service
				.processInstruction(instruction("enable", Map.of("duration", "soon")));
		assertThat(status.getInstructionState(), is(InstructionState.Declined));
		assertThat(actions(), is(empty()));
	}

	@Test
	public void instruction_disable() throws IOException {
		// GIVEN radio currently on via op mode
		expect(opModesService.enableOperationalModes(eq(Set.of(OP_MODE)), anyObject()))
				.andReturn(Set.of(OP_MODE));
		expect(opModesService.activeOperationalModesWithExpirations()).andReturn(Map.of()).anyTimes();
		expect(opModesService.disableOperationalModes(Set.of(OP_MODE))).andReturn(Set.of());
		replayAll();
		service.processInstruction(instruction("enable", null));
		assertThat(radioState(), is(true));

		// WHEN
		InstructionStatus status = service.processInstruction(instruction("disable", null));

		// THEN
		assertThat(status.getInstructionState(), is(InstructionState.Completed));
		BluetoothSetupStatus s = (BluetoothSetupStatus) status.getResultParameters()
				.get(InstructionHandler.PARAM_SERVICE_RESULT);
		assertThat(s.isRadioActive(), is(false));
		assertThat(s.isOpModeActive(), is(false));
		assertThat(actions(), contains("enable", "disable"));
		assertThat(radioState(), is(false));
	}

	@Test
	public void instruction_restart_whenOff_leavesRadioOff() throws IOException {
		replayAll();
		InstructionStatus status = service.processInstruction(instruction("restart", null));
		assertThat(status.getInstructionState(), is(InstructionState.Completed));
		BluetoothSetupStatus s = (BluetoothSetupStatus) status.getResultParameters()
				.get(InstructionHandler.PARAM_SERVICE_RESULT);
		assertThat("restart never turns a gated-off radio on", s.isRadioActive(), is(false));
		assertThat(radioState(), is(false));
		assertThat(actions(), contains("restart"));
	}

	@Test
	public void instruction_restart_whenOn_keepsRadioOn() throws IOException {
		expect(opModesService.enableOperationalModes(eq(Set.of(OP_MODE)), anyObject()))
				.andReturn(Set.of(OP_MODE));
		expect(opModesService.activeOperationalModesWithExpirations()).andReturn(Map.of()).anyTimes();
		replayAll();
		service.processInstruction(instruction("enable", null));
		assertThat(radioState(), is(true));

		InstructionStatus status = service.processInstruction(instruction("restart", null));

		assertThat(status.getInstructionState(), is(InstructionState.Completed));
		BluetoothSetupStatus s = (BluetoothSetupStatus) status.getResultParameters()
				.get(InstructionHandler.PARAM_SERVICE_RESULT);
		assertThat(s.isRadioActive(), is(true));
		assertThat(radioState(), is(true));
		assertThat(actions(), contains("enable", "restart"));
	}

	@Test
	public void executeAction_hang_timesOut() throws IOException {
		replayAll();
		service.setCommandTimeoutSeconds(1);
		File hangFile = new File(tmpFile.getAbsolutePath() + ".hang");
		try {
			Files.writeString(hangFile.toPath(), "");
			final long start = System.nanoTime();
			BluetoothSetupStatus s = service.currentStatus();
			final long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
			assertThat("hung helper reported as unknown status", s, is(nullValue()));
			assertThat("timeout enforced from process start, not after output", elapsedMs < 10_000L,
					is(true));
		} finally {
			hangFile.delete();
		}
	}

	@Test
	public void executeAction_largeStderr_doesNotDeadlock() throws IOException {
		replayAll();
		service.setCommandTimeoutSeconds(10);
		File noisyFile = new File(tmpFile.getAbsolutePath() + ".noisy");
		try {
			Files.writeString(noisyFile.toPath(), "");
			BluetoothSetupStatus s = service.currentStatus();
			assertThat("status parsed despite large stderr output", s, is(notNullValue()));
			assertThat(s.isRadioActive(), is(false));
		} finally {
			noisyFile.delete();
		}
	}

	@Test
	public void modesChanged_enablesThenDisables() throws IOException {
		expect(opModesService.activeOperationalModesWithExpirations()).andReturn(Map.of()).anyTimes();
		replayAll();

		service.handleEvent(modesEvent(OP_MODE, "other"));
		assertThat(radioState(), is(true));

		service.handleEvent(modesEvent("other"));
		assertThat(radioState(), is(false));

		assertThat(actions(), contains("enable", "disable"));
	}

	@Test
	public void modesChanged_otherMode_ignored() throws IOException {
		replayAll();
		service.handleEvent(modesEvent("quiet"));
		assertThat(actions(), is(empty()));
		assertThat(radioState(), is(false));
	}

	@Test
	public void reconcile_idempotent() throws IOException {
		expect(opModesService.activeOperationalModesWithExpirations()).andReturn(Map.of()).anyTimes();
		replayAll();

		service.handleEvent(modesEvent(OP_MODE));
		service.handleEvent(modesEvent(OP_MODE));
		service.reconcile();
		service.reconcile();

		assertThat("only one enable despite repeated events", actions(), contains("enable"));
	}

	@Test
	public void alwaysOn_turnsRadioOn() throws IOException {
		replayAll();
		service.setAlwaysOn(true);
		service.reconcile();
		assertThat(actions(), contains("enable"));
		assertThat(radioState(), is(true));
	}

	@Test
	public void defaultAlwaysOn_turnsRadioOn() throws IOException {
		replayAll();
		// a fresh instance with no configuration at all must turn the radio on
		BluetoothSetupService fresh = new BluetoothSetupService(opModesService, null, null);
		fresh.setCommand(tmpFile.getAbsolutePath());
		assertThat(fresh.isAlwaysOn(), is(true));
		fresh.reconcile();
		assertThat(actions(), contains("enable"));
		assertThat(radioState(), is(true));
	}

	@Test
	public void offline_triggersOnlyAfterThreshold() throws IOException {
		service.setOfflineThresholdMinutes(2);
		expect(opModesService.isOperationalModeActive(OFFLINE_MODE)).andReturn(true).anyTimes();
		replayAll();

		service.watchdogTick();
		assertThat("not yet offline", radioState(), is(false));

		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();
		assertThat("still under threshold", radioState(), is(false));

		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();
		assertThat("threshold reached", radioState(), is(true));
		assertThat(actions(), contains("enable"));

		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.isOffline(), is(true));
		assertThat(s.getOfflineSince(), is(START));
	}

	@Test
	public void offline_clearsOnlyAfterGrace() throws IOException {
		service.setOfflineThresholdMinutes(1);
		service.setOnlineGraceMinutes(3);
		expect(opModesService.isOperationalModeActive(OFFLINE_MODE)).andReturn(true).times(2);
		expect(opModesService.isOperationalModeActive(OFFLINE_MODE)).andReturn(false).anyTimes();
		replayAll();

		service.watchdogTick();
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();
		assertThat(radioState(), is(true));

		// back online
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();
		assertThat("grace period started", radioState(), is(true));

		advanceClock(Duration.ofMinutes(2));
		service.watchdogTick();
		assertThat("still within grace", radioState(), is(true));

		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();
		assertThat("grace elapsed", radioState(), is(false));
		assertThat(actions(), contains("enable", "disable"));
	}

	@Test
	public void offline_graceResetsOnNewFailure() throws IOException {
		service.setOfflineThresholdMinutes(1);
		service.setOnlineGraceMinutes(2);
		expect(opModesService.isOperationalModeActive(OFFLINE_MODE)).andReturn(true).times(2);
		expect(opModesService.isOperationalModeActive(OFFLINE_MODE)).andReturn(false).times(1);
		expect(opModesService.isOperationalModeActive(OFFLINE_MODE)).andReturn(true).times(1);
		expect(opModesService.isOperationalModeActive(OFFLINE_MODE)).andReturn(false).times(2);
		replayAll();

		service.watchdogTick();
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick();
		assertThat(radioState(), is(true));

		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick(); // online, grace starts
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick(); // offline again, grace reset
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick(); // online, grace restarts
		advanceClock(Duration.ofMinutes(1));
		service.watchdogTick(); // only 1 minute of grace since restart
		assertThat("grace restarted so radio still on", radioState(), is(true));
		assertThat(actions(), contains("enable"));
	}

	@Test
	public void offline_modeNeverActive_neverTriggers() throws IOException {
		service.setOfflineThresholdMinutes(1);
		expect(opModesService.isOperationalModeActive(OFFLINE_MODE)).andReturn(false).anyTimes();
		replayAll();

		service.watchdogTick();
		advanceClock(Duration.ofMinutes(10));
		service.watchdogTick();

		assertThat(radioState(), is(false));
		assertThat(actions(), is(empty()));
	}

	@Test
	public void offline_triggerDisabled_neverTriggers() throws IOException {
		service.setOfflineTriggerEnabled(false);
		service.setOfflineThresholdMinutes(1);
		replayAll();

		service.watchdogTick();
		advanceClock(Duration.ofMinutes(10));
		service.watchdogTick();

		assertThat(radioState(), is(false));
		assertThat(actions(), is(empty()));
	}

	private String statusTitle() {
		SettingSpecifier title = service.getSettingSpecifiers().get(0);
		return String.valueOf(((TitleSettingSpecifier) title).getDefaultValue());
	}

	@Test
	public void offline_eventStartsThreshold() throws IOException {
		service.setOfflineThresholdMinutes(2);
		service.setOnlineGraceMinutes(3);
		expect(opModesService.isOperationalModeActive(OFFLINE_MODE)).andReturn(true).times(1);
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE));
		assertThat("not yet offline", radioState(), is(false));
		assertThat("threshold started", service.currentStatus().getOfflineSince(), is(START));

		advanceClock(Duration.ofMinutes(2));
		service.watchdogTick();
		assertThat("threshold reached", radioState(), is(true));

		service.handleEvent(modesEvent("other"));
		assertThat("grace started", radioState(), is(true));

		advanceClock(Duration.ofMinutes(3));
		service.handleEvent(modesEvent("other"));
		assertThat("grace elapsed", radioState(), is(false));
		assertThat(actions(), contains("enable", "disable"));
	}

	@Test
	public void offline_blankMode_neverTriggers() throws IOException {
		service.setOfflineThresholdMinutes(0);
		service.setOfflineOpMode("");
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE, "other"));
		service.watchdogTick();
		advanceClock(Duration.ofMinutes(10));
		service.watchdogTick();

		assertThat(radioState(), is(false));
		assertThat(actions(), is(empty()));
		assertThat(service.currentStatus().getOfflineSince(), is(nullValue()));

		service.setOfflineOpMode(null);
		assertThat(service.getOfflineOpMode(), equalTo(""));
	}

	@Test
	public void offline_modeNormalised() {
		replayAll();
		service.setOfflineOpMode(" Net-Offline ");
		assertThat(service.getOfflineOpMode(), equalTo("net-offline"));
		service.setOfflineOpMode("!net-online");
		assertThat("inverted mode not supported", service.getOfflineOpMode(), equalTo(""));
	}

	@Test
	public void offline_customMode() throws IOException {
		service.setOfflineThresholdMinutes(0);
		service.setOfflineOpMode("WAN-Down");
		expect(opModesService.isOperationalModeActive("wan-down")).andReturn(true).times(1);
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE));
		assertThat("default mode ignored", radioState(), is(false));

		service.handleEvent(modesEvent("wan-down"));
		assertThat("custom mode event", radioState(), is(true));

		service.watchdogTick();
		assertThat("custom mode tick", radioState(), is(true));
		assertThat(actions(), contains("enable"));
	}

	@Test
	public void offline_triggerDisabled_eventIgnored() throws IOException {
		service.setOfflineTriggerEnabled(false);
		service.setOfflineThresholdMinutes(0);
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE));
		advanceClock(Duration.ofMinutes(10));
		service.handleEvent(modesEvent(OFFLINE_MODE, "other"));

		assertThat(radioState(), is(false));
		assertThat(actions(), is(empty()));
		assertThat(service.currentStatus().getOfflineSince(), is(nullValue()));
	}

	@Test
	public void offline_watchdogDisabled_eventIgnored() throws IOException {
		service.setWatchdogIntervalSeconds(0);
		service.setOfflineThresholdMinutes(1);
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE));
		advanceClock(Duration.ofMinutes(10));
		service.handleEvent(modesEvent(OFFLINE_MODE, "other"));

		assertThat(radioState(), is(false));
		assertThat(actions(), is(empty()));
		assertThat(service.currentStatus().getOfflineSince(), is(nullValue()));
	}

	@Test
	public void offline_configChange_blankMode_clearsState() throws IOException {
		service.setOfflineThresholdMinutes(0);
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE));
		assertThat(radioState(), is(true));

		service.setOfflineOpMode("");
		service.configurationChanged(Map.of());

		assertThat("stale offline state cleared", radioState(), is(false));
		assertThat(service.currentStatus().isOffline(), is(false));
		assertThat(actions(), contains("enable", "disable"));
	}

	@Test
	public void offline_configChange_modeChanged_clearsState() throws IOException {
		service.setOfflineThresholdMinutes(0);
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE));
		assertThat(radioState(), is(true));

		service.setOfflineOpMode("wan-down");
		service.configurationChanged(Map.of());

		assertThat("state from the old mode cleared", radioState(), is(false));
		assertThat(service.currentStatus().getOfflineSince(), is(nullValue()));
	}

	@Test
	public void offline_configChange_triggerToggled_noImmediateTurnOn() throws IOException {
		service.setOfflineThresholdMinutes(0);
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE));
		assertThat(radioState(), is(true));

		service.setOfflineTriggerEnabled(false);
		service.configurationChanged(Map.of());
		assertThat(radioState(), is(false));

		service.setOfflineTriggerEnabled(true);
		service.configurationChanged(Map.of());
		assertThat("no stale offline state", radioState(), is(false));
		assertThat(actions(), contains("enable", "disable"));
	}

	@Test
	public void status_offlinePending() throws IOException {
		service.setOfflineThresholdMinutes(15);
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE));

		assertThat(statusTitle(), containsString(
				"offline mode 'net-offline' active since 2026-10-06T00:00:00Z; radio turns on at 2026-10-06T00:15:00Z"));
	}

	@Test
	public void status_offline() throws IOException {
		service.setOfflineThresholdMinutes(0);
		replayAll();

		service.handleEvent(modesEvent(OFFLINE_MODE));

		String title = statusTitle();
		assertThat(title,
				containsString("offline mode 'net-offline' active since 2026-10-06T00:00:00Z"));
		assertThat(title, org.hamcrest.Matchers.not(containsString("radio turns on at")));
	}

	@Test
	public void status_commandFailure_returnsNull() throws IOException {
		replayAll();
		service.setCommand("/nonexistent/solarcfg");
		assertThat(service.currentStatus(), is(nullValue()));
	}

	@Test
	public void executeAction_nonZeroExit_declined() throws IOException {
		replayAll();
		// the fake script exits 3 from "restart" when a ".fail" marker exists; the
		// non-zero exit must surface as a Declined instruction, not Completed
		File failFile = new File(tmpFile.getAbsolutePath() + ".fail");
		try {
			Files.writeString(failFile.toPath(), "");
			InstructionStatus status = service.processInstruction(instruction("restart", null));
			assertThat(status.getInstructionState(), is(InstructionState.Declined));
			assertThat(
					String.valueOf(status.getResultParameters().get(InstructionHandler.PARAM_MESSAGE)),
					org.hamcrest.Matchers.containsString("exit code 3"));
		} finally {
			failFile.delete();
		}
	}

	@Test
	public void settings() {
		replayAll();
		List<SettingSpecifier> specs = service.getSettingSpecifiers();
		assertThat(specs, is(notNullValue()));
		assertThat(specs.size(), equalTo(17));
		KeyedSettingSpecifier<?> metaUid = specs.stream().filter(KeyedSettingSpecifier.class::isInstance)
				.map(KeyedSettingSpecifier.class::cast)
				.filter(k -> "metadataServiceUid".equals(k.getKey())).findFirst().orElse(null);
		assertThat(metaUid, instanceOf(TextFieldSettingSpecifier.class));
		assertThat("metadata service default", metaUid.getDefaultValue(),
				equalTo(BluetoothSetupService.DEFAULT_METADATA_SERVICE_UID));
		assertThat("metadata service lookup",
				((TextFieldSettingSpecifier) metaUid).getRelatedServiceFilter(),
				equalTo("(objectClass=net.solarnetwork.node.service.MetadataService)"));
		KeyedSettingSpecifier<?> metaPath = specs.stream()
				.filter(KeyedSettingSpecifier.class::isInstance).map(KeyedSettingSpecifier.class::cast)
				.filter(k -> "metadataPath".equals(k.getKey())).findFirst().orElse(null);
		assertThat(metaPath, is(notNullValue()));
		assertThat("metadata path default", metaPath.getDefaultValue(), equalTo(""));
		KeyedSettingSpecifier<?> alwaysOn = specs.stream()
				.filter(KeyedSettingSpecifier.class::isInstance).map(KeyedSettingSpecifier.class::cast)
				.filter(k -> "alwaysOn".equals(k.getKey())).findFirst().orElse(null);
		assertThat(alwaysOn, is(notNullValue()));
		assertThat("always on by default", alwaysOn.getDefaultValue(), equalTo(Boolean.TRUE));
		KeyedSettingSpecifier<?> schedule = specs.stream()
				.filter(KeyedSettingSpecifier.class::isInstance).map(KeyedSettingSpecifier.class::cast)
				.filter(k -> "schedule".equals(k.getKey())).findFirst().orElse(null);
		assertThat(schedule, instanceOf(TextAreaSettingSpecifier.class));
		assertThat(((TextAreaSettingSpecifier) schedule).isDirect(), is(true));
		KeyedSettingSpecifier<?> offlineMode = specs.stream()
				.filter(KeyedSettingSpecifier.class::isInstance).map(KeyedSettingSpecifier.class::cast)
				.filter(k -> "offlineOpMode".equals(k.getKey())).findFirst().orElse(null);
		assertThat(offlineMode, is(notNullValue()));
		assertThat("offline mode default", offlineMode.getDefaultValue(), equalTo("net-offline"));
		assertThat("no ping test setting",
				specs.stream().filter(KeyedSettingSpecifier.class::isInstance)
						.map(KeyedSettingSpecifier.class::cast)
						.anyMatch(k -> "pingTestIdRegex".equals(k.getKey())),
				is(false));
	}

	private void startupWithSchedule(String schedule, String timeZone) {
		service.setSchedule(schedule);
		service.setTimeZone(timeZone);
		service.startup();
	}

	@Test
	public void schedule_insideWindow_turnsOn() throws IOException {
		// START is 2026-10-06T00:00Z, a Tuesday; 13:00 NZDT in Pacific/Auckland
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		startupWithSchedule(WEEKDAY_HOURS, "Pacific/Auckland");

		assertThat(radioState(), is(true));
		assertThat(actions(), contains("enable"));
		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.isScheduleActive(), is(true));
		assertThat(s.getScheduleError(), is(nullValue()));
		assertThat(s.getTimeZone(), equalTo("Pacific/Auckland"));
	}

	@Test
	public void schedule_outsideWindow_staysOff() throws IOException {
		// 00:00 UTC is outside the window
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		startupWithSchedule(WEEKDAY_HOURS, "UTC");

		assertThat(radioState(), is(false));
		assertThat(actions(), is(empty()));
		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.isScheduleActive(), is(false));
		assertThat("next window start", s.getScheduleNext(),
				equalTo(START.plus(Duration.ofHours(7)).toEpochMilli()));
	}

	@Test
	public void schedule_blankZone_usesSystemDefault() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		startupWithSchedule(WEEKDAY_HOURS, "");

		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getTimeZone(), equalTo(ZoneId.systemDefault().getId()));
		boolean expected = AvailabilitySchedule.parse(WEEKDAY_HOURS)
				.applies(LocalDateTime.ofInstant(START, ZoneId.systemDefault()));
		assertThat(radioState(), is(expected));
	}

	@Test
	public void schedule_invalidZone_failsClosed() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		startupWithSchedule(SCHED_HEADER + ",,,,1", "Mars/Olympus");

		assertThat(radioState(), is(false));
		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleError(), containsString("Mars/Olympus"));
		assertThat(s.getTimeZone(), is(nullValue()));
		assertThat(s.isScheduleActive(), is(false));
	}

	@Test
	public void schedule_invalid_failsClosed() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		startupWithSchedule(INVALID_HOURS, "UTC");

		assertThat(radioState(), is(false));
		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleError(), containsString("CSV row 2"));
		SettingSpecifier title = service.getSettingSpecifiers().get(0);
		assertThat(title, instanceOf(TitleSettingSpecifier.class));
		assertThat(String.valueOf(((TitleSettingSpecifier) title).getDefaultValue()),
				containsString("invalid"));
		// the instruction result carries the error too
		InstructionStatus status = service.processInstruction(instruction("status", null));
		BluetoothSetupStatus result = (BluetoothSetupStatus) status.getResultParameters()
				.get(InstructionHandler.PARAM_SERVICE_RESULT);
		assertThat(result.getScheduleError(), containsString("CSV row 2"));
	}

	@Test
	public void schedule_emptyWithAlwaysOnOff_behavesLikeBefore() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		startupWithSchedule("", "UTC");

		assertThat(radioState(), is(false));
		assertThat(actions(), is(empty()));
		assertThat(service.currentStatus().getSchedule(), equalTo(""));
	}

	@Test
	public void schedule_withAlwaysOn_ignored() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		TaskScheduler scheduler = EasyMock.createStrictMock(TaskScheduler.class);
		replayAll();
		replay(scheduler);

		BluetoothSetupService s = new BluetoothSetupService(opModesService, scheduler, null);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setWatchdogIntervalSeconds(0);
		s.setSchedule(WEEKDAY_HOURS);
		s.setTimeZone("UTC"); // window closed at 00:00 UTC
		s.startup();

		assertThat("always on wins over a closed window", radioState(), is(true));
		verify(scheduler); // no schedule tick registered while always on
		s.shutdown();
	}

	@Test
	public void scheduleTick_reconcilesOnlyOnEdge() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();
		advanceClock(Duration.ofHours(6).plusMinutes(59));
		startupWithSchedule(WEEKDAY_HOURS, "UTC");
		service.scheduleTick();
		assertThat(radioState(), is(false));

		advanceClock(Duration.ofMinutes(1)); // 07:00
		service.scheduleTick();
		assertThat(radioState(), is(true));

		advanceClock(Duration.ofMinutes(1)); // 07:01
		int linesBefore = rawLogLines();
		service.scheduleTick();
		assertThat(radioState(), is(true));
		assertThat("no helper call at all on a non-edge tick", rawLogLines(), equalTo(linesBefore));

		advanceClock(Duration.ofHours(10).plusMinutes(59)); // 18:00
		service.scheduleTick();
		assertThat(radioState(), is(false));

		assertThat("exactly one enable and one disable", actions(), contains("enable", "disable"));
	}

	@Test
	public void startup_insideWindow_turnsOn() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();
		advanceClock(Duration.ofHours(12)); // 12:00 UTC, a reboot mid-window

		startupWithSchedule(WEEKDAY_HOURS, "UTC");

		assertThat(radioState(), is(true));
		assertThat(actions(), contains("enable"));
	}

	@Test
	public void dst_springForward_gapWindowNeverApplies() throws IOException {
		// NZ clocks go from 02:00 NZST to 03:00 NZDT on Sunday 2026-09-27
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();
		service.setClock(Clock.fixed(Instant.parse("2026-09-26T13:59:00Z"), ZoneOffset.UTC)); // 01:59 NZST
		startupWithSchedule(SCHED_HEADER + ",,Sun,02:00-03:00,1", "Pacific/Auckland");
		assertThat(radioState(), is(false));

		service.setClock(Clock.fixed(Instant.parse("2026-09-26T14:00:00Z"), ZoneOffset.UTC)); // 03:00 NZDT
		service.scheduleTick();
		assertThat(radioState(), is(false));

		service.setClock(Clock.fixed(Instant.parse("2026-09-26T14:30:00Z"), ZoneOffset.UTC)); // 03:30 NZDT
		service.scheduleTick();
		assertThat("window inside the gap never applies", radioState(), is(false));
		assertThat(actions(), is(empty()));
	}

	@Test
	public void dst_fallBack_repeatedHourAppliesTwice() throws IOException {
		// NZ clocks go from 03:00 NZDT back to 02:00 NZST on Sunday 2027-04-04
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();
		service.setClock(Clock.fixed(Instant.parse("2027-04-03T13:30:00Z"), ZoneOffset.UTC)); // 02:30 NZDT
		startupWithSchedule(SCHED_HEADER + ",,Sun,02:00-03:00,1", "Pacific/Auckland");
		assertThat("first 02:30", radioState(), is(true));

		service.setClock(Clock.fixed(Instant.parse("2027-04-03T14:30:00Z"), ZoneOffset.UTC)); // 02:30 NZST
		service.scheduleTick();
		assertThat("second 02:30", radioState(), is(true));

		service.setClock(Clock.fixed(Instant.parse("2027-04-03T15:00:00Z"), ZoneOffset.UTC)); // 03:00 NZST
		service.scheduleTick();
		assertThat(radioState(), is(false));
		assertThat(actions(), contains("enable", "disable"));
	}

	@Test
	public void instruction_enable_overridesClosedWindow() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		Capture<Instant> expireCapture = Capture.newInstance();
		expect(opModesService.enableOperationalModes(eq(Set.of(OP_MODE)), capture(expireCapture)))
				.andReturn(Set.of(OP_MODE));
		expect(opModesService.activeOperationalModesWithExpirations()).andReturn(Map.of()).anyTimes();
		replayAll();
		startupWithSchedule(WEEKDAY_HOURS, "UTC"); // closed at 00:00 UTC
		assertThat(radioState(), is(false));

		InstructionStatus status = service
				.processInstruction(instruction("enable", Map.of("duration", "600")));

		assertThat(status.getInstructionState(), is(InstructionState.Completed));
		assertThat(expireCapture.getValue(), is(START.plusSeconds(600)));
		assertThat("override turns the radio on outside the window", radioState(), is(true));
		BluetoothSetupStatus s = (BluetoothSetupStatus) status.getResultParameters()
				.get(InstructionHandler.PARAM_SERVICE_RESULT);
		assertThat(s.isScheduleActive(), is(false));
		assertThat(s.isOpModeActive(), is(true));
	}

	@Test
	public void instruction_disable_insideWindow_keepsRadioOn() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(opModesService.disableOperationalModes(Set.of(OP_MODE))).andReturn(Set.of());
		replayAll();
		startupWithSchedule(WEEKDAY_HOURS, "Pacific/Auckland"); // 13:00 NZDT, open
		assertThat(radioState(), is(true));

		InstructionStatus status = service.processInstruction(instruction("disable", null));

		assertThat(status.getInstructionState(), is(InstructionState.Completed));
		assertThat("schedule keeps the radio on", radioState(), is(true));
		assertThat(actions(), contains("enable"));
	}

	@Test
	public void status_includesScheduleFields() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();
		advanceClock(Duration.ofHours(13)); // 13:00 UTC, inside
		startupWithSchedule(SCHED_HEADER + ",,mon-fri,7-18,1", "UTC");

		BluetoothSetupStatus s = service.currentStatus();

		assertThat(s.getSchedule(), equalTo(",,Mon-Fri,7-18,1"));
		assertThat(s.isScheduleActive(), is(true));
		assertThat(s.getScheduleError(), is(nullValue()));
		assertThat(s.getTimeZone(), equalTo("UTC"));
		assertThat("next boundary is the window end", s.getScheduleNext(),
				equalTo(START.plus(Duration.ofHours(18)).toEpochMilli()));
		assertThat(s.isInstallEnabled(), is(true));
	}

	@Test
	public void scheduleTick_retriesAfterHelperFailure() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();
		advanceClock(Duration.ofHours(7)); // 07:00, inside the window
		File failFile = new File(tmpFile.getAbsolutePath() + ".failenable");
		try {
			Files.writeString(failFile.toPath(), "");
			startupWithSchedule(WEEKDAY_HOURS, "UTC");
			assertThat("enable failed", radioState(), is(false));
			service.scheduleTick();
			assertThat("still failing", radioState(), is(false));
		} finally {
			failFile.delete();
		}
		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();
		assertThat("edge retried once the helper works", radioState(), is(true));
	}

	@Test
	public void configurationChanged_registersAndCancelsTick() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		TaskScheduler scheduler = EasyMock.createMock(TaskScheduler.class);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ScheduledFuture future1 = EasyMock.createMock(ScheduledFuture.class);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ScheduledFuture future2 = EasyMock.createMock(ScheduledFuture.class);
		expect(scheduler.scheduleWithFixedDelay(anyObject(Runnable.class), anyObject(Instant.class),
				eq(Duration.ofSeconds(BluetoothSetupService.SCHEDULE_TICK_SECONDS)))).andReturn(future1)
						.andReturn(future2);
		expect(future1.cancel(false)).andReturn(true).times(1);
		expect(future2.cancel(false)).andReturn(true).times(1);
		replayAll();
		replay(scheduler, future1, future2);

		BluetoothSetupService s = new BluetoothSetupService(opModesService, scheduler, null);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setWatchdogIntervalSeconds(0);
		s.setAlwaysOn(false);
		s.setSchedule(WEEKDAY_HOURS);
		s.setTimeZone("UTC");
		s.startup(); // registers future1

		s.setAlwaysOn(true);
		s.configurationChanged(Map.of()); // cancels future1, radio on
		assertThat(radioState(), is(true));

		s.setAlwaysOn(false);
		s.configurationChanged(Map.of()); // registers future2, radio off (00:00 UTC)
		assertThat(radioState(), is(false));

		s.setSchedule(INVALID_HOURS);
		s.configurationChanged(Map.of()); // invalid: cancels future2, stays off
		assertThat(radioState(), is(false));
		assertThat(s.currentStatus().getScheduleError(), is(notNullValue()));

		s.shutdown();
		verify(scheduler, future1, future2);
	}

	@Test
	public void watchdogDisabled_scheduleStillTicks() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		TaskScheduler scheduler = EasyMock.createMock(TaskScheduler.class);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ScheduledFuture future = EasyMock.createMock(ScheduledFuture.class);
		expect(scheduler.scheduleWithFixedDelay(anyObject(Runnable.class), anyObject(Instant.class),
				eq(Duration.ofSeconds(BluetoothSetupService.SCHEDULE_TICK_SECONDS)))).andReturn(future)
						.times(1);
		expect(future.cancel(false)).andReturn(true).times(1);
		replayAll();
		replay(scheduler, future);

		BluetoothSetupService s = new BluetoothSetupService(opModesService, scheduler, null);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setAlwaysOn(false);
		s.setWatchdogIntervalSeconds(0);
		s.setSchedule(WEEKDAY_HOURS);
		s.setTimeZone("UTC");
		s.startup();
		s.shutdown();

		verify(scheduler, future);
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
		service.startup();
	}

	private String statusTitleText() {
		SettingSpecifier title = service.getSettingSpecifiers().get(0);
		return String.valueOf(((TitleSettingSpecifier) title).getDefaultValue());
	}

	@Test
	public void metadata_loadsAndOverridesInline() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(ALWAYS)).times(1);
		replayAll();

		// the inline schedule is closed at 00:00 UTC Tuesday; the metadata one is open
		startWithMetadata(WEEKDAY_HOURS);

		assertThat("metadata schedule turns radio on", radioState(), is(true));
		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_METADATA));
		assertThat(s.getScheduleMetadataError(), is(nullValue()));
		assertThat(s.getScheduleError(), is(nullValue()));
		assertThat(s.getSchedule(), equalTo(",,,,1"));
		assertThat(statusTitleText(), containsString("schedule from metadata '" + META_PATH + "'"));

		InstructionStatus status = service.processInstruction(instruction("status", null));
		BluetoothSetupStatus result = (BluetoothSetupStatus) status.getResultParameters()
				.get(InstructionHandler.PARAM_SERVICE_RESULT);
		assertThat(result.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_METADATA));
	}

	@Test
	public void metadata_notConfigured_noRead() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		replayAll();

		// a path with no service UID is not configured either
		service.setMetadataServiceUid("");
		startWithMetadata(WEEKDAY_HOURS);
		service.scheduleTick();

		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_SETTING));
		assertThat(s.getScheduleMetadataError(), is(nullValue()));
		assertThat(radioState(), is(false));
	}

	@Test
	public void metadata_noService_retriesWithoutBackoff() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(ALWAYS)).times(1);
		replayAll();

		metadataServiceRef.service = null;
		startWithMetadata(WEEKDAY_HOURS);
		BluetoothSetupStatus s = service.currentStatus();
		assertThat("inline used", s.getScheduleSource(),
				equalTo(BluetoothSetupService.SCHEDULE_SOURCE_SETTING));
		assertThat(s.getScheduleMetadataError(), containsString("no Metadata Service"));
		assertThat(radioState(), is(false));

		metadataServiceRef.service = metadataService;
		service.scheduleTick();
		assertThat("loaded on the next tick", radioState(), is(true));
	}

	@Test
	public void metadata_unreachableBeforeLoad_usesInline() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(null).times(1);
		expect(metadataService.getAllMetadata()).andThrow(new RuntimeException("boom")).times(1);
		replayAll();

		startWithMetadata(ALWAYS);
		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_SETTING));
		assertThat(s.getScheduleMetadataError(), containsString("metadata unavailable"));
		assertThat("inline schedule applies", radioState(), is(true));
		assertThat(statusTitleText(), containsString("not loaded"));

		advanceClock(Duration.ofSeconds(BluetoothSetupService.METADATA_RETRY_SECONDS + 1));
		service.scheduleTick();
		s = service.currentStatus();
		assertThat(s.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_SETTING));
		assertThat(s.getScheduleMetadataError(), containsString("boom"));
		assertThat(radioState(), is(true));
	}

	@Test
	public void metadata_unreachableAfterLoad_keepsLastSchedule() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(ALWAYS)).times(1);
		expect(metadataService.getAllMetadata()).andThrow(new RuntimeException("offline")).times(1);
		replayAll();

		startWithMetadata(WEEKDAY_HOURS);
		assertThat(radioState(), is(true));

		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();

		BluetoothSetupStatus s = service.currentStatus();
		assertThat("last result kept", s.getScheduleSource(),
				equalTo(BluetoothSetupService.SCHEDULE_SOURCE_METADATA));
		assertThat(s.getScheduleMetadataError(), containsString("offline"));
		assertThat(radioState(), is(true));
		assertThat(statusTitleText(), containsString("keeping the last result"));
	}

	@Test
	public void metadata_invalidThenUnreachable_staysClosed() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(ALWAYS))
				.andReturn(meta("Mon-Fri 07:00-18:00")).andThrow(new RuntimeException("offline"));
		replayAll();

		startWithMetadata(ALWAYS);
		assertThat("valid metadata schedule", radioState(), is(true));

		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();
		assertThat("invalid push closes the radio", radioState(), is(false));
		assertThat(service.currentStatus().getScheduleError(),
				containsString("metadata schedule invalid"));

		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();
		BluetoothSetupStatus s = service.currentStatus();
		assertThat("outage does not restore the older schedule", radioState(), is(false));
		assertThat(s.getScheduleError(), containsString("metadata schedule invalid"));
		assertThat(s.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_METADATA));
	}

	@Test
	public void metadata_pathAbsent_usesInlineWithoutBackoff() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(null)).times(2);
		replayAll();

		startWithMetadata(ALWAYS);
		service.scheduleTick();

		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_SETTING));
		assertThat(s.getScheduleMetadataError(), equalTo("no schedule at " + META_PATH));
		assertThat(radioState(), is(true));
	}

	@Test
	public void metadata_invalid_failsClosedDespiteInline() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(",,Mon-Fri,07:00-18:00,1")).times(1);
		replayAll();

		startWithMetadata(ALWAYS);

		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_METADATA));
		assertThat(s.getScheduleError(), containsString("header row"));
		assertThat("invalid metadata never applies", radioState(), is(false));
	}

	@Test
	public void metadata_headerOnly_closesRadio() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata())
				.andReturn(meta(List.of(List.of("Month", "Day", "Weekday", "Time", "Available"))))
				.times(1);
		replayAll();

		startWithMetadata(ALWAYS);

		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_METADATA));
		assertThat(s.getScheduleError(), is(nullValue()));
		assertThat("empty metadata schedule turns scheduled availability off", radioState(), is(false));
	}

	@Test
	public void metadata_validWithInvalidInline() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(ALWAYS)).times(1);
		replayAll();

		startWithMetadata(INVALID_HOURS);

		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleError(), is(nullValue()));
		assertThat(radioState(), is(true));
	}

	@Test
	public void metadata_arrayForm() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata())
				.andReturn(meta(List.of(List.of("Month", "Day", "Weekday", "Time", "Available"),
						Arrays.asList(null, null, "Mon-Fri", "00:00-06:00", 1))))
				.times(1);
		replayAll();

		startWithMetadata(WEEKDAY_HOURS);

		assertThat(service.currentStatus().getSchedule(), equalTo(",,Mon-Fri,0-6,1"));
		assertThat(radioState(), is(true));
	}

	@Test
	public void metadata_valueChange_pickedUpOnNextTick() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(ALWAYS)).andReturn(meta(WEEKDAY_HOURS));
		replayAll();

		startWithMetadata("");
		assertThat(radioState(), is(true));

		// as after a clear-cache signal: the next read returns the new value
		advanceClock(Duration.ofSeconds(15));
		service.scheduleTick();
		assertThat(radioState(), is(false));
		assertThat(actions(), contains("enable", "disable"));
	}

	@Test
	public void metadata_backoffAfterUnreachable() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(null).andReturn(meta(ALWAYS));
		replayAll();

		startWithMetadata(WEEKDAY_HOURS);

		advanceClock(Duration.ofSeconds(BluetoothSetupService.METADATA_RETRY_SECONDS - 1));
		service.scheduleTick();
		assertThat("no read during back-off", radioState(), is(false));

		advanceClock(Duration.ofSeconds(2));
		service.scheduleTick();
		assertThat("read after back-off", radioState(), is(true));
	}

	@Test
	public void metadata_configChange_clearsBackoff() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(null).andReturn(meta(ALWAYS));
		replayAll();

		startWithMetadata(WEEKDAY_HOURS);
		service.configurationChanged(Map.of());

		assertThat(radioState(), is(true));
	}

	@Test
	public void metadata_pathChange_dropsOldSchedule() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(ALWAYS)).times(2);
		replayAll();

		startWithMetadata(WEEKDAY_HOURS);
		assertThat(radioState(), is(true));

		service.setMetadataPath("/pm/bluetooth/other");
		service.configurationChanged(Map.of());

		BluetoothSetupStatus s = service.currentStatus();
		assertThat("old path's schedule not kept", s.getScheduleSource(),
				equalTo(BluetoothSetupService.SCHEDULE_SOURCE_SETTING));
		assertThat(s.getScheduleMetadataError(), equalTo("no schedule at /pm/bluetooth/other"));
		assertThat(radioState(), is(false));
	}

	@Test
	public void metadata_pathCleared_revertsToInline() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(ALWAYS)).times(1);
		replayAll();

		startWithMetadata(WEEKDAY_HOURS);
		assertThat(radioState(), is(true));

		service.setMetadataPath("");
		service.configurationChanged(Map.of());

		BluetoothSetupStatus s = service.currentStatus();
		assertThat(s.getScheduleSource(), equalTo(BluetoothSetupService.SCHEDULE_SOURCE_SETTING));
		assertThat(s.getScheduleMetadataError(), is(nullValue()));
		assertThat(radioState(), is(false));
	}

	@Test
	public void metadata_staleResultDropped() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		final GeneralDatumMetadata both = meta(ALWAYS);
		both.putInfoValue("bluetooth", "other", SCHED_HEADER);
		// the first read is overtaken by a reconfiguration to another path
		expect(metadataService.getAllMetadata()).andAnswer(() -> {
			service.setMetadataPath("/pm/bluetooth/other");
			service.configurationChanged(Map.of());
			return both;
		});
		expect(metadataService.getAllMetadata()).andReturn(both);
		replayAll();

		startWithMetadata(WEEKDAY_HOURS);

		BluetoothSetupStatus s = service.currentStatus();
		assertThat("newer path applies", s.getScheduleSource(),
				equalTo(BluetoothSetupService.SCHEDULE_SOURCE_METADATA));
		assertThat("old path's open schedule dropped", s.getSchedule(), equalTo(""));
		assertThat(radioState(), is(false));
	}

	@Test
	public void metadata_readsWithoutHoldingLock() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andAnswer(() -> {
			assertThat("service lock not held while reading metadata", Thread.holdsLock(service),
					is(false));
			return meta(ALWAYS);
		}).times(3);
		replayAll();

		startWithMetadata(WEEKDAY_HOURS);
		service.configurationChanged(Map.of());
		service.scheduleTick();
		assertThat(radioState(), is(true));
	}

	@Test
	public void metadata_alwaysOn_noScheduleTickOrRead() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		TaskScheduler scheduler = EasyMock.createMock(TaskScheduler.class);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ScheduledFuture future = EasyMock.createMock(ScheduledFuture.class);
		// with Always On nothing is scheduled and no metadata is read
		replayAll();
		replay(scheduler, future);

		BluetoothSetupService s = new BluetoothSetupService(opModesService, scheduler,
				metadataServiceRef);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setWatchdogIntervalSeconds(0);
		s.setAlwaysOn(true);
		s.setMetadataPath(META_PATH);
		s.startup();

		verify(scheduler, future);
	}

	@Test
	public void metadata_pathOnly_registersScheduleTick() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		TaskScheduler scheduler = EasyMock.createMock(TaskScheduler.class);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ScheduledFuture future = EasyMock.createMock(ScheduledFuture.class);
		expect(scheduler.scheduleWithFixedDelay(anyObject(Runnable.class), anyObject(Instant.class),
				eq(Duration.ofSeconds(BluetoothSetupService.SCHEDULE_TICK_SECONDS)))).andReturn(future)
						.times(1);
		expect(scheduler.schedule(anyObject(Runnable.class), anyObject(Instant.class))).andReturn(future)
				.times(1);
		replayAll();
		replay(scheduler, future);

		BluetoothSetupService s = new BluetoothSetupService(opModesService, scheduler,
				metadataServiceRef);
		s.setCommand(tmpFile.getAbsolutePath());
		s.setClock(Clock.fixed(START, ZoneOffset.UTC));
		s.setWatchdogIntervalSeconds(0);
		s.setAlwaysOn(false);
		s.setTimeZone("UTC");
		s.setMetadataPath(META_PATH);
		s.startup();

		verify(scheduler, future);
	}

	@Test
	public void metadata_errorDoesNotBlockLaterReads() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andThrow(new LinkageError("bundle refreshed"))
				.andReturn(meta(ALWAYS));
		replayAll();

		try {
			startWithMetadata(WEEKDAY_HOURS);
			org.junit.Assert.fail("Error should propagate");
		} catch ( LinkageError e ) {
			// expected
		}
		assertThat(radioState(), is(false));

		service.scheduleTick();
		assertThat("the next tick reads again", radioState(), is(true));
	}

	@Test
	public void metadata_shutdown_stopsTick() throws IOException {
		expect(opModesService.isOperationalModeActive(OP_MODE)).andReturn(false).anyTimes();
		expect(metadataService.getAllMetadata()).andReturn(meta(ALWAYS)).times(1);
		replayAll();

		startWithMetadata(WEEKDAY_HOURS);
		assertThat(radioState(), is(true));
		final int lines = rawLogLines();

		service.shutdown();
		service.scheduleTick();

		assertThat("no metadata read or helper call after shutdown", rawLogLines(), equalTo(lines));
	}

}
