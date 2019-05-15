/* ==================================================================
 * OperationalStateManager.java - 15/05/2019 6:48:01 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.opmode;

import static net.solarnetwork.node.OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES;
import static net.solarnetwork.node.OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_OPERATING_STATE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.util.OptionalService;

/**
 * Service that listens for operational mode changes and can respond by setting
 * the operational state on a set of devices.
 * 
 * <p>
 * For example, one idea with this service is to be able to respond to an
 * operational state <code>inverters-off</code> by setting the state of a set of
 * inverters to <code>Shutdown</code> (to turn them off). When the
 * <code>inverters-off</code> mode is disabled, the state of the inverters is
 * set to <code>Normal</code> (to turn them back on again).
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class OperationalStateManager implements EventHandler {

	/** The default value for the {@code taskTimeoutSecs} property. */
	public static final long DEFAULT_TASK_TIMEOUT_SECS = 60L;

	private String mode;
	private DeviceOperatingState enabledState;
	private DeviceOperatingState disabledState;
	private long taskTimeoutSecs = DEFAULT_TASK_TIMEOUT_SECS;
	private Set<String> controlIds;

	private AsyncTaskExecutor taskExecutor;

	private final OptionalService<InstructionExecutionService> instructionService;
	private final AtomicBoolean active = new AtomicBoolean(false);
	private final Lock oneAtATimeLock = new ReentrantLock(true);
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param instructionService
	 *        the instruction execution service to use
	 */
	public OperationalStateManager(OptionalService<InstructionExecutionService> instructionService) {
		super();
		this.instructionService = instructionService;
	}

	private static class StateChangeResult {

		private final String controlId;
		private final boolean result;
		private final Throwable t;

		public StateChangeResult(String controlId, boolean result, Throwable t) {
			super();
			this.controlId = controlId;
			this.result = result;
			this.t = t;
		}
	}

	private class StateChanger implements Callable<StateChangeResult> {

		private final CountDownLatch latch;
		private final String controlId;
		private final DeviceOperatingState state;
		private final Date date;

		public StateChanger(CountDownLatch latch, String controlId, DeviceOperatingState state,
				Date date) {
			super();
			this.latch = latch;
			this.controlId = controlId;
			this.state = state;
			this.date = date;
		}

		@Override
		public StateChangeResult call() throws Exception {
			try {
				BasicInstruction instr = new BasicInstruction(TOPIC_SET_OPERATING_STATE, date, null,
						null, null);
				instr.addParameter(controlId, String.valueOf(state.getCode()));
				InstructionExecutionService instrService = getInstructionExecutionService();
				if ( instrService == null ) {
					throw new RuntimeException("No InstructionExecutionService available.");
				}
				InstructionStatus status = instrService.executeInstruction(instr);
				return new StateChangeResult(controlId,
						status != null && status.getInstructionState() != InstructionState.Declined,
						null);
			} catch ( Exception e ) {
				return new StateChangeResult(controlId, false, e);
			} finally {
				latch.countDown();
			}
		}

	}

	private class StateChangeManager implements Runnable {

		private final String mode;
		private final DeviceOperatingState state;
		private final Set<String> controlIds;

		private final CountDownLatch latch;

		/**
		 * Constructor.
		 * 
		 * @param mode
		 *        the operational mode that initiated this change
		 * @param state
		 *        the desired operational state to apply
		 * @param controlIds
		 *        the controls to apply the operational state to
		 */
		public StateChangeManager(String mode, DeviceOperatingState state, Set<String> controlIds) {
			super();
			this.mode = mode;
			this.state = state;
			this.controlIds = controlIds;
			this.latch = new CountDownLatch(controlIds.size());
		}

		@Override
		public void run() {
			// we only want one state change in flight at a time, so control execution with a lock
			final TaskExecutor executor = getTaskExecutor();
			if ( executor != null ) {
				oneAtATimeLock.lock();
			}
			try {
				final Date start = new Date();
				log.info("Executing [{}] operational mode change operating state to [{}] on {} @ {}",
						mode, state, controlIds, start);
				final List<Future<StateChangeResult>> results = new ArrayList<>(controlIds.size());
				for ( String controlId : controlIds ) {
					StateChanger task = new StateChanger(latch, controlId, state, start);
					if ( executor != null ) {
						results.add(taskExecutor.submit(task));
					} else {
						CompletableFuture<StateChangeResult> f = new CompletableFuture<>();
						try {
							f.complete(task.call());
						} catch ( Exception e ) {
							f.complete(new StateChangeResult(controlId, false, e));
						}
						results.add(f);
					}
				}
				try {
					latch.await(taskTimeoutSecs, TimeUnit.SECONDS);
					log.info("Completed [{}] operational mode change operating state to [{}] on {} @ {}",
							mode, state, controlIds, start);
				} catch ( InterruptedException e ) {
					log.warn(
							"Interrupted waiting for [{}] operational mode change operating state to [{}] on {} @ {}",
							mode, state, controlIds, start);
				}

				StringBuilder buf = new StringBuilder();
				buf.append("Results for [").append(mode)
						.append("] operational mode change operating state to [").append(state)
						.append("] @ ").append(start).append(":\n");
				for ( Future<StateChangeResult> f : results ) {
					if ( f.isDone() ) {
						try {
							StateChangeResult r = f.get();
							buf.append("\t").append(r.controlId).append(" result: ");
							if ( r.t != null ) {
								buf.append("ERROR: ").append(r.t.toString());
							} else if ( r.result ) {
								buf.append("OK");
							} else {
								buf.append("FAIL");
							}
							buf.append("\n");
						} catch ( Exception e ) {
							Throwable root = e;
							while ( root.getCause() != null ) {
								root = root.getCause();
							}
							buf.append("\tException executing task: ").append(root.toString())
									.append("\n");
						}
					}
				}
				log.info(buf.toString());
			} finally {
				if ( executor != null ) {
					oneAtATimeLock.unlock();
				}
			}
		}
	}

	private InstructionExecutionService getInstructionExecutionService() {
		return (instructionService != null ? instructionService.service() : null);
	}

	/**
	 * Get an immutable, never {@literal null}, set of control IDs to manage.
	 * 
	 * @return the control IDs, never {@literal null}
	 */
	private Set<String> immutableControlIds() {
		return (this.controlIds != null ? Collections.unmodifiableSet(new LinkedHashSet<>(controlIds))
				: Collections.emptySet());
	}

	@Override
	public void handleEvent(Event event) {
		final String topic = (event != null ? event.getTopic() : null);
		final String mode = getMode();
		if ( EVENT_TOPIC_OPERATIONAL_MODES_CHANGED.equals(topic) && mode != null ) {
			@SuppressWarnings("unchecked")
			Set<String> activeModes = (Set<String>) event
					.getProperty(EVENT_PARAM_ACTIVE_OPERATIONAL_MODES);
			boolean isActive = activeModes.contains(mode);
			Set<String> controlIds = immutableControlIds();
			DeviceOperatingState desiredState = null;
			if ( !controlIds.isEmpty() && isActive && active.compareAndSet(false, true) ) {
				// mode has been enabled
				desiredState = enabledState;
			} else if ( !controlIds.isEmpty() && !isActive && active.compareAndSet(true, false) ) {
				// mode has been disabled
				desiredState = disabledState;
			}
			if ( desiredState != null ) {
				StateChangeManager mgr = new StateChangeManager(mode, desiredState, controlIds);
				TaskExecutor executor = getTaskExecutor();
				if ( executor != null ) {
					executor.execute(mgr);
				} else {
					mgr.run();
				}
			}
		}
	}

	/**
	 * Get the active state.
	 * 
	 * @return {@literal true} if the configured mode is considered as enabled,
	 *         {@literal false} otherwise
	 */
	public boolean isModeActive() {
		return active.get();
	}

	/**
	 * Set the task executor.
	 * 
	 * @return the task executor
	 */
	public AsyncTaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Get the task executor.
	 * 
	 * @param taskExecutor
	 *        the task executor
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Get the operational mode to listen for.
	 * 
	 * @return the operational mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * Set the operational mode to listen for.
	 * 
	 * @param mode
	 *        the operational mode
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Get the operating state to apply when the operational mode is enabled.
	 * 
	 * @return the state to apply
	 */
	public DeviceOperatingState getEnabledState() {
		return enabledState;
	}

	/**
	 * Set the operating state to apply when the operational mode is enabled.
	 * 
	 * @param enabledState
	 *        the state to apply
	 */
	public void setEnabledState(DeviceOperatingState enabledState) {
		this.enabledState = enabledState;
	}

	/**
	 * Get the operating state to apply when the operational mode is disabled.
	 * 
	 * @return the state to apply
	 */
	public DeviceOperatingState getDisabledState() {
		return disabledState;
	}

	/**
	 * Set the operating state to apply when the operational mode is disabled.
	 * 
	 * @param disabledState
	 *        the state to apply
	 */
	public void setDisabledState(DeviceOperatingState disabledState) {
		this.disabledState = disabledState;
	}

	/**
	 * Get the maximum number of seconds to wait for all configured devices to
	 * handle an operational state change.
	 * 
	 * @return the maximum seconds
	 */
	public long getTaskTimeoutSecs() {
		return taskTimeoutSecs;
	}

	/**
	 * Set the maximum number of seconds to wait for all configured devices to
	 * handle an operational state change.
	 * 
	 * <p>
	 * This method forces any value less than {@literal 1} to {@literal 1}.
	 * </p>
	 * 
	 * @param taskTimeoutSecs
	 *        the maximum seconds
	 */
	public void setTaskTimeoutSecs(long taskTimeoutSecs) {
		if ( taskTimeoutSecs < 1 ) {
			taskTimeoutSecs = 1;
		}
		this.taskTimeoutSecs = taskTimeoutSecs;
	}

	/**
	 * Get the control IDs to manage.
	 * 
	 * @return the control IDs
	 */
	public Set<String> getControlIds() {
		return controlIds;
	}

	/**
	 * Set the control IDs to manage.
	 * 
	 * @param controlIds
	 *        the control IDs to manage
	 */
	public void setControlIds(Set<String> controlIds) {
		this.controlIds = controlIds;
	}

}
