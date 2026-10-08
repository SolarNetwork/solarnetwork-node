/* ==================================================================
 * LiveDatumModeManagerTests.java - 10/10/2026 1:42:00 PM
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

package net.solarnetwork.node.setup.stomp.server.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.node.setup.stomp.server.LiveDatumModeManager.DEFAULT_MODE_EXPIRE_SECS;
import static net.solarnetwork.node.setup.stomp.server.LiveDatumModeManager.DEFAULT_MODE_LINGER_SECS;
import static net.solarnetwork.node.setup.stomp.server.LiveDatumModeManager.DEFAULT_MODE_REFRESH_SECS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.setup.stomp.server.LiveDatumModeManager;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link LiveDatumModeManager} class.
 *
 * @author elijah
 * @version 1.0
 */
public class LiveDatumModeManagerTests extends LiveDatumTestSupport {

	private AtomicBoolean active;
	private LiveDatumModeManager manager;

	@Before
	public void setup() {
		active = new AtomicBoolean(false);
		manager = new LiveDatumModeManager(new StaticOptionalService<>(opModes), active::get);
		manager.setTaskScheduler(scheduler);
		manager.setClock(clock);
	}

	private void activate() {
		active.set(true);
		manager.sync();
	}

	private void deactivate() {
		active.set(false);
		manager.sync();
	}

	private void lingerPasses() {
		clock.advance(Duration.ofSeconds(DEFAULT_MODE_LINGER_SECS));
		runScheduledTasks();
	}

	@Test
	public void available() {
		assertThat("Available with service", manager.isAvailable(), is(true));
		LiveDatumModeManager m = new LiveDatumModeManager(new StaticOptionalService<>(null),
				active::get);
		assertThat("Not available without service", m.isAvailable(), is(false));
	}

	@Test
	public void noService_syncDoesNothing() {
		// GIVEN
		manager = new LiveDatumModeManager(new StaticOptionalService<>(null), active::get);

		// WHEN
		activate();
		deactivate();

		// THEN
		assertThat("No tasks", scheduler.getTasks(), hasSize(0));
	}

	@Test
	public void setOpMode_normalized() {
		manager.setOpMode(" Other-Live ");
		assertThat("Trimmed and lower cased", manager.getOpMode(), is("other-live"));
		manager.setOpMode("  ");
		assertThat("Blank uses default", manager.getOpMode(), is(MODE));
		manager.setOpMode(null);
		assertThat("Null uses default", manager.getOpMode(), is(MODE));
	}

	@Test
	public void active_enablesWithExpiration() {
		// WHEN
		activate();

		// THEN
		final long exp = clock.millis() + DEFAULT_MODE_EXPIRE_SECS * 1000L;
		assertThat("Mode enabled with expiration", opModes.enableCalls,
				contains(MODE + "@" + exp));
	}

	@Test
	public void inactive_lingerThenDisable() {
		// GIVEN
		activate();

		// WHEN
		deactivate();

		// THEN
		assertThat("Not disabled during linger", opModes.disableCalls, hasSize(0));
		assertThat("Linger scheduled", scheduler.pendingOneShots(), hasSize(1));

		// WHEN
		lingerPasses();

		// THEN
		assertThat("Disabled after linger", opModes.disableCalls, contains(MODE));
		assertThat("Mode inactive", opModes.isOperationalModeActive(MODE), is(false));
	}

	@Test
	public void inactive_noLinger_disablesImmediately() {
		// GIVEN
		manager.setModeLingerSecs(0);
		activate();

		// WHEN
		deactivate();

		// THEN
		assertThat("Disabled immediately", opModes.disableCalls, contains(MODE));
	}

	@Test
	public void inactive_neverEnabled_nothingToDo() {
		// WHEN
		deactivate();

		// THEN
		assertThat("Nothing scheduled", scheduler.getTasks(), hasSize(0));
		assertThat("Not disabled", opModes.disableCalls, hasSize(0));
	}

	@Test
	public void reactivatedDuringLinger_noDisable() {
		// GIVEN
		activate();
		deactivate();

		// WHEN
		activate();
		lingerPasses();

		// THEN
		assertThat("Never disabled", opModes.disableCalls, hasSize(0));
		assertThat("Mode active", opModes.isOperationalModeActive(MODE), is(true));
	}

	@Test
	public void staleLingerRun_doesNotDisableEarly() {
		// GIVEN
		// schedule linger
		activate();
		deactivate();
		TestTaskScheduler.Task linger1 = scheduler.pendingOneShots().get(0);

		// AND
		// re-schedule linger
		activate();
		deactivate();

		// WHEN
		// stale linger runs
		linger1.forceRun();

		// THEN
		assertThat("Stale linger did not disable", opModes.disableCalls, hasSize(0));

		// WHEN
		// current linger passes
		lingerPasses();

		// THEN
		assertThat("Disabled by current linger", opModes.disableCalls, contains(MODE));
	}

	@Test
	public void operatorOwnedMode_leftAlone() {
		// GIVEN
		opModes.enableOperationalModes(singleton(MODE));
		opModes.enableCalls.clear();

		// WHEN
		activate();
		deactivate();
		lingerPasses();

		// THEN
		assertThat("Not enabled", opModes.enableCalls, hasSize(0));
		assertThat("Not disabled", opModes.disableCalls, hasSize(0));
		assertThat("Still active, no expiration", opModes.expiration(MODE), is(nullValue()));
		assertThat("Still active", opModes.isOperationalModeActive(MODE), is(true));
	}

	@Test
	public void modeChangedToNoExpirationWhileActive_released() {
		// GIVEN
		activate();

		// WHEN
		// operator makes mode permanent
		opModes.enableOperationalModes(singleton(MODE));
		manager.sync();
		deactivate();
		lingerPasses();

		// THEN
		assertThat("Not disabled", opModes.disableCalls, hasSize(0));
		assertThat("Still active", opModes.isOperationalModeActive(MODE), is(true));
	}

	@Test
	public void refresh_onlyBelowThreshold() {
		// GIVEN
		activate();
		opModes.enableCalls.clear();

		// WHEN
		// 60s later
		clock.advance(Duration.ofSeconds(60));
		manager.sync();

		// THEN
		assertThat("Not refreshed", opModes.enableCalls, hasSize(0));

		// WHEN
		// within refresh threshold
		clock.advance(
				Duration.ofSeconds(DEFAULT_MODE_EXPIRE_SECS - DEFAULT_MODE_REFRESH_SECS - 60 + 1));
		manager.sync();

		// THEN
		final long exp = clock.millis() + DEFAULT_MODE_EXPIRE_SECS * 1000L;
		assertThat("Refreshed", opModes.enableCalls, contains(MODE + "@" + exp));
	}

	@Test
	public void reenable_ifRemovedExternally() {
		// GIVEN
		activate();
		opModes.disableOperationalModes(singleton(MODE));
		opModes.enableCalls.clear();

		// WHEN
		manager.sync();

		// THEN
		assertThat("Re-enabled", opModes.enableCalls, hasSize(1));
		assertThat("Mode active", opModes.isOperationalModeActive(MODE), is(true));
	}

	@Test
	public void longerExistingExpiration_notShortened_butReleasedWhenInactive() {
		// GIVEN
		final Instant extExp = clock.instant().plusSeconds(3600);
		opModes.enableOperationalModes(singleton(MODE), extExp);
		opModes.enableCalls.clear();

		// WHEN
		activate();

		// THEN
		assertThat("Not changed", opModes.enableCalls, hasSize(0));
		assertThat("Expiration kept", opModes.expiration(MODE), is(extExp.toEpochMilli()));

		// WHEN
		deactivate();
		lingerPasses();

		// THEN
		assertThat("Expiring mode treated as ours and disabled", opModes.disableCalls,
				contains(MODE));
	}

	@Test
	public void leftoverModeAfterRestart_disabledWhenInactive() {
		// GIVEN
		// mode left over from before restart
		opModes.enableOperationalModes(singleton(MODE),
				clock.instant().plusSeconds(DEFAULT_MODE_REFRESH_SECS + 60));
		opModes.enableCalls.clear();

		// WHEN
		activate();
		deactivate();
		lingerPasses();

		// THEN
		assertThat("Not refreshed", opModes.enableCalls, hasSize(0));
		assertThat("Disabled after linger", opModes.disableCalls, contains(MODE));
	}

	@Test
	public void opModeChanged_whileActive_releasesOldMode() {
		// GIVEN
		activate();

		// WHEN
		manager.setOpMode("other-live");
		manager.sync();

		// THEN
		assertThat("Old mode disabled", opModes.disableCalls, contains(MODE));
		assertThat("New mode enabled", opModes.isOperationalModeActive("other-live"), is(true));
	}

	@Test
	public void shutdown_disables() {
		// GIVEN
		activate();

		// WHEN
		manager.shutdown();

		// THEN
		assertThat("Disabled synchronously", opModes.disableCalls, contains(MODE));
	}

	@Test
	public void shutdown_cancelsLinger_andSyncDoesNothing() {
		// GIVEN
		activate();
		deactivate();
		TestTaskScheduler.Task linger = scheduler.pendingOneShots().get(0);

		// WHEN
		manager.shutdown();
		opModes.disableCalls.clear();
		linger.forceRun();
		activate();

		// THEN
		assertThat("Linger cancelled", linger.isCancelled(), is(true));
		assertThat("Linger run did nothing", opModes.disableCalls, hasSize(0));
		assertThat("Not enabled after shutdown", opModes.isOperationalModeActive(MODE), is(false));
	}

}
