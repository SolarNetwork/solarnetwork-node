/* ==================================================================
 * LiveDatumModeManager.java - 10/10/2026 1:42:00 PM
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

package net.solarnetwork.node.setup.stomp.server;

import static java.util.Collections.singleton;
import static net.solarnetwork.service.OptionalService.service;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.OptionalService;

/**
 * Manage the operational mode that is active while live datum subscriptions
 * exist.
 *
 * <p>
 * While the {@code active} condition given to the constructor is true,
 * {@link #sync()} enables an operational mode (by default
 * {@link #DEFAULT_OP_MODE}) with an expiration date, and extends that date as
 * it gets close. Once the condition becomes false the mode is disabled after a
 * short linger, so a client switching sources does not cause the mode to be
 * disabled and enabled again.
 * </p>
 *
 * <p>
 * Changing an operational mode performs database I/O, so callers must only
 * call {@link #sync()} from a thread where blocking is acceptable, never from
 * the Netty event loop or event admin threads. All methods are serialized on
 * this instance's monitor.
 * </p>
 *
 * @author elijah
 * @version 1.0
 * @since 4.1
 */
public class LiveDatumModeManager {

	/** The default {@code opMode} property value. */
	public static final String DEFAULT_OP_MODE = "setup-live";

	/** The default {@code modeExpireSecs} property value. */
	public static final int DEFAULT_MODE_EXPIRE_SECS = 300;

	/** The default {@code modeRefreshSecs} property value. */
	public static final int DEFAULT_MODE_REFRESH_SECS = 180;

	/** The default {@code modeLingerSecs} property value. */
	public static final int DEFAULT_MODE_LINGER_SECS = 10;

	private static final Logger log = LoggerFactory.getLogger(LiveDatumModeManager.class);

	private final OptionalService<OperationalModesService> opModesService;
	private final BooleanSupplier active;

	// the following are guarded by this
	private boolean shutdown;
	private String enabledMode;
	private ScheduledFuture<?> lingerFuture;
	private long lingerGeneration;

	private TaskScheduler taskScheduler;
	private Clock clock = Clock.systemUTC();
	private String opMode = DEFAULT_OP_MODE;
	private int modeExpireSecs = DEFAULT_MODE_EXPIRE_SECS;
	private int modeRefreshSecs = DEFAULT_MODE_REFRESH_SECS;
	private int modeLingerSecs = DEFAULT_MODE_LINGER_SECS;

	/**
	 * Constructor.
	 *
	 * @param opModesService
	 *        the operational modes service
	 * @param active
	 *        a condition that is {@literal true} while the mode should be
	 *        active, i.e. while live subscriptions exist
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LiveDatumModeManager(OptionalService<OperationalModesService> opModesService,
			BooleanSupplier active) {
		super();
		if ( opModesService == null ) {
			throw new IllegalArgumentException("The opModesService argument must not be null.");
		}
		this.opModesService = opModesService;
		if ( active == null ) {
			throw new IllegalArgumentException("The active argument must not be null.");
		}
		this.active = active;
	}

	/**
	 * Test if the operational modes service is available.
	 *
	 * @return {@literal true} if the operational modes service is available
	 */
	public boolean isAvailable() {
		return service(opModesService) != null;
	}

	/**
	 * Bring the operational mode in line with the {@code active} condition.
	 *
	 * <p>
	 * When active, any pending linger is cancelled and the mode is enabled or
	 * its expiration extended as needed. When not active, and this manager
	 * enabled the mode, the mode is disabled after {@code modeLingerSecs}.
	 * </p>
	 */
	public synchronized void sync() {
		if ( shutdown ) {
			return;
		}
		final OperationalModesService ops = service(opModesService);
		if ( active.getAsBoolean() ) {
			cancelLinger();
			if ( ops != null ) {
				try {
					if ( enabledMode != null && !enabledMode.equals(opMode) ) {
						// mode setting changed while active: release the old mode
						disableMode(ops);
					}
					enableOrRefreshMode(ops);
				} catch ( RuntimeException e ) {
					log.warn("Error enabling live datum operational mode [{}]: {}", opMode,
							e.toString());
				}
			}
		} else if ( enabledMode != null && lingerFuture == null ) {
			final TaskScheduler scheduler = this.taskScheduler;
			if ( modeLingerSecs < 1 || scheduler == null ) {
				if ( ops != null ) {
					disableMode(ops);
				}
			} else {
				final long gen = ++lingerGeneration;
				lingerFuture = scheduler.schedule(() -> lingerExpired(gen),
						clock.instant().plusSeconds(modeLingerSecs));
			}
		}
	}

	/**
	 * Shut down the manager.
	 *
	 * <p>
	 * Any pending linger is cancelled and the mode is disabled if this manager
	 * enabled it. After this, {@link #sync()} does nothing.
	 * </p>
	 */
	public synchronized void shutdown() {
		shutdown = true;
		cancelLinger();
		if ( enabledMode != null ) {
			final OperationalModesService ops = service(opModesService);
			if ( ops != null ) {
				disableMode(ops);
			}
		}
	}

	private synchronized void lingerExpired(long generation) {
		if ( generation != lingerGeneration ) {
			// a newer linger has been scheduled, or this one was cancelled while
			// already running
			return;
		}
		lingerFuture = null;
		if ( shutdown || active.getAsBoolean() || enabledMode == null ) {
			return;
		}
		final OperationalModesService ops = service(opModesService);
		if ( ops != null ) {
			disableMode(ops);
		}
	}

	/**
	 * Enable the operational mode, or extend its expiration if needed.
	 *
	 * <p>
	 * If the mode is active <b>without</b> an expiration, it has been enabled
	 * by something else (e.g. an operator) and is left alone. Otherwise the mode
	 * is enabled with an expiration of {@code modeExpireSecs} from now, unless
	 * it already expires more than {@code modeRefreshSecs} from now. An
	 * existing later expiration date is never shortened. Any active mode with
	 * an expiration is considered owned by this manager, and will be disabled
	 * once the {@code active} condition becomes false.
	 * </p>
	 */
	private void enableOrRefreshMode(OperationalModesService ops) {
		final String mode = opMode;
		final Map<String, Long> withExp = ops.activeOperationalModesWithExpirations();
		final Long exp = (withExp != null ? withExp.get(mode) : null);
		if ( exp == null && ops.isOperationalModeActive(mode) ) {
			// active with no expiration: not ours to manage
			if ( mode.equals(enabledMode) ) {
				log.info("Live datum operational mode [{}] now has no expiration; releasing",
						mode);
				enabledMode = null;
			}
			return;
		}
		final long now = clock.millis();
		if ( exp != null && (exp.longValue() - now) > (modeRefreshSecs * 1000L) ) {
			// plenty of time left; an expiring mode is treated as ours (e.g. left over
			// from before a restart) so it is disabled when subscriptions end
			enabledMode = mode;
			return;
		}
		final long target = now + (modeExpireSecs * 1000L);
		final long newExp = (exp != null ? Math.max(exp.longValue(), target) : target);
		ops.enableOperationalModes(singleton(mode), Instant.ofEpochMilli(newExp));
		enabledMode = mode;
	}

	/**
	 * Disable the operational mode previously enabled by this manager.
	 */
	private void disableMode(OperationalModesService ops) {
		final String mode = enabledMode;
		if ( mode == null ) {
			return;
		}
		enabledMode = null;
		try {
			final Map<String, Long> withExp = ops.activeOperationalModesWithExpirations();
			if ( ops.isOperationalModeActive(mode)
					&& (withExp == null || !withExp.containsKey(mode)) ) {
				// changed to no expiration by something else: leave it alone
				log.info("Not disabling live datum operational mode [{}]: it has no expiration",
						mode);
				return;
			}
			ops.disableOperationalModes(singleton(mode));
		} catch ( RuntimeException e ) {
			log.warn("Error disabling live datum operational mode [{}]: {}", mode, e.toString());
		}
	}

	private void cancelLinger() {
		// invalidate any linger task, even one that is already running
		lingerGeneration++;
		if ( lingerFuture != null ) {
			lingerFuture.cancel(false);
			lingerFuture = null;
		}
	}

	/**
	 * Get the task scheduler.
	 *
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 *
	 * <p>
	 * The linger task is scheduled on this scheduler. If not configured the
	 * mode is disabled as soon as the {@code active} condition becomes false.
	 * </p>
	 *
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the clock.
	 *
	 * @return the clock
	 */
	public Clock getClock() {
		return clock;
	}

	/**
	 * Set the clock.
	 *
	 * @param clock
	 *        the clock to set; if {@literal null} then the system UTC clock will
	 *        be used
	 */
	public void setClock(Clock clock) {
		this.clock = (clock != null ? clock : Clock.systemUTC());
	}

	/**
	 * Get the operational mode to enable while active.
	 *
	 * @return the mode; defaults to {@link #DEFAULT_OP_MODE}
	 */
	public String getOpMode() {
		return opMode;
	}

	/**
	 * Set the operational mode to enable while active.
	 *
	 * @param opMode
	 *        the mode to set; will be trimmed and converted to lower case; if
	 *        {@literal null} or empty then {@link #DEFAULT_OP_MODE} will be used
	 */
	public void setOpMode(String opMode) {
		String m = (opMode != null ? opMode.trim().toLowerCase() : "");
		this.opMode = (m.isEmpty() ? DEFAULT_OP_MODE : m);
	}

	/**
	 * Get the operational mode expiration, in seconds.
	 *
	 * @return the expiration; defaults to {@link #DEFAULT_MODE_EXPIRE_SECS}
	 */
	public int getModeExpireSecs() {
		return modeExpireSecs;
	}

	/**
	 * Set the operational mode expiration, in seconds.
	 *
	 * <p>
	 * This bounds how long the mode stays active if SolarNode stops without
	 * this manager disabling it.
	 * </p>
	 *
	 * @param modeExpireSecs
	 *        the expiration to set
	 */
	public void setModeExpireSecs(int modeExpireSecs) {
		this.modeExpireSecs = modeExpireSecs;
	}

	/**
	 * Get the operational mode refresh threshold, in seconds.
	 *
	 * @return the threshold; defaults to {@link #DEFAULT_MODE_REFRESH_SECS}
	 */
	public int getModeRefreshSecs() {
		return modeRefreshSecs;
	}

	/**
	 * Set the operational mode refresh threshold, in seconds.
	 *
	 * <p>
	 * The mode expiration is extended when less than this amount of time
	 * remains.
	 * </p>
	 *
	 * @param modeRefreshSecs
	 *        the threshold to set
	 */
	public void setModeRefreshSecs(int modeRefreshSecs) {
		this.modeRefreshSecs = modeRefreshSecs;
	}

	/**
	 * Get the operational mode linger time, in seconds.
	 *
	 * @return the linger time; defaults to {@link #DEFAULT_MODE_LINGER_SECS}
	 */
	public int getModeLingerSecs() {
		return modeLingerSecs;
	}

	/**
	 * Set the operational mode linger time, in seconds.
	 *
	 * <p>
	 * After the {@code active} condition becomes false the mode stays active
	 * for this long.
	 * </p>
	 *
	 * @param modeLingerSecs
	 *        the linger time to set; anything less than {@literal 1} disables
	 *        the mode immediately
	 */
	public void setModeLingerSecs(int modeLingerSecs) {
		this.modeLingerSecs = modeLingerSecs;
	}

}
