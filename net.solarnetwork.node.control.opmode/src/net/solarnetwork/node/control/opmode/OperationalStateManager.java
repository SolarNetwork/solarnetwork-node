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

import static java.util.Collections.singleton;
import static net.solarnetwork.node.OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES;
import static net.solarnetwork.node.OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_OPERATING_STATE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

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
public class OperationalStateManager extends BaseIdentifiable
		implements EventHandler, SettingSpecifierProvider {

	/** The default value for the {@code taskTimeoutSecs} property. */
	public static final long DEFAULT_TASK_TIMEOUT_SECS = 60L;

	/** The default value for the {@code retryCount} property. */
	public static final int DEFAULT_RETRY_COUNT = 3;

	private String mode;
	private DeviceOperatingState enabledState;
	private DeviceOperatingState disabledState;
	private long taskTimeoutSecs = DEFAULT_TASK_TIMEOUT_SECS;
	private Set<String> controlIds;
	private int retryCount = DEFAULT_RETRY_COUNT;
	private AsyncTaskExecutor taskExecutor;

	private final OptionalService<OperationalModesService> opModesService;
	private final OptionalService<InstructionExecutionService> instructionService;
	private final AtomicBoolean active = new AtomicBoolean(false);
	private final Lock oneAtATimeLock = new ReentrantLock(true);
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param opModesService
	 *        the operational modes service to use
	 * @param instructionService
	 *        the instruction execution service to use
	 */
	public OperationalStateManager(OptionalService<OperationalModesService> opModesService,
			OptionalService<InstructionExecutionService> instructionService) {
		super();
		this.opModesService = opModesService;
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
				log.info("Set operating state instruction for [{}] to [{}] result: {}", controlId, state,
						status != null ? status.getInstructionState()
								: "Not handled (no matching control?)");
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
		private final int attemptNumber;
		private final boolean enabling;

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
		 * @param enabling
		 *        {@literal true} if enabling the active state
		 */
		public StateChangeManager(String mode, DeviceOperatingState state, Set<String> controlIds,
				boolean enabling) {
			super();
			this.mode = mode;
			this.state = state;
			this.controlIds = controlIds;
			this.latch = new CountDownLatch(controlIds.size());
			this.enabling = enabling;
			this.attemptNumber = 0;
		}

		private StateChangeManager(StateChangeManager other, Set<String> controlIds) {
			this.mode = other.mode;
			this.state = other.state;
			this.controlIds = controlIds;
			this.latch = new CountDownLatch(controlIds.size());
			this.enabling = other.enabling;
			this.attemptNumber = other.attemptNumber + 1;
		}

		@Override
		public void run() {
			// we only want one state change in flight at a time, so control execution with a lock
			final TaskExecutor executor = getTaskExecutor();
			if ( executor != null ) {
				oneAtATimeLock.lock();
			}
			StateChangeManager retry = null;
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
				Set<String> okControlIds = new LinkedHashSet<>(results.size());
				for ( Future<StateChangeResult> f : results ) {
					if ( f.isDone() ) {
						try {
							StateChangeResult r = f.get();
							buf.append("\t").append(r.controlId).append(" result: ");
							if ( r.t != null ) {
								buf.append("ERROR: ").append(r.t.toString());
							} else if ( r.result ) {
								buf.append("OK");
								okControlIds.add(r.controlId);
							} else {
								buf.append("FAIL");
							}
							buf.append("\n");
						} catch ( Exception e ) {
							// shouldn't get here... just in case we handle this here
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
				int retryCount = getRetryCount();
				if ( okControlIds.size() < controlIds.size()
						&& (retryCount < 0 || attemptNumber < retryCount) ) {
					// not all controls passed and we can retry
					Set<String> retryControlIds = new LinkedHashSet<>(controlIds.size());
					for ( String controlId : controlIds ) {
						if ( !okControlIds.contains(controlId) ) {
							retryControlIds.add(controlId);
						}
					}
					retry = new StateChangeManager(this, retryControlIds);
					log.info(
							"Error applying [{}] operational mode change operating state to [{}] on {} @ {}; attempted {}/{} times, will try again.",
							mode, state, controlIds, start, attemptNumber + 1, retryCount + 1);
				} else if ( okControlIds.size() < controlIds.size() ) {
					log.error(
							"Error applying [{}] operational mode change operating state to [{}] on {} @ {}; attempted {} times, giving up now.",
							mode, state, controlIds, start, attemptNumber + 1);
					if ( enabling ) {
						// was trying to enable, so disable the mode now to "fall back" to disabled state
						log.info(
								"Disabling [{}] operational mode after failing to change operating state to [{}] on {} @ {}",
								mode, state, controlIds, start);
						OperationalModesService opService = getOpModesService();
						if ( opService != null ) {
							opService.disableOperationalModes(singleton(mode));
						}
					}
				}
			} finally {
				if ( executor != null ) {
					oneAtATimeLock.unlock();
				}
				if ( retry != null ) {
					executeManagerTask(retry);
				}
			}
		}
	}

	private InstructionExecutionService getInstructionExecutionService() {
		return (instructionService != null ? instructionService.service() : null);
	}

	private OperationalModesService getOpModesService() {
		return (opModesService != null ? opModesService.service() : null);
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
			boolean enabling = true;
			if ( !controlIds.isEmpty() && isActive && active.compareAndSet(false, true) ) {
				// mode has been enabled
				desiredState = enabledState;
			} else if ( !controlIds.isEmpty() && !isActive && active.compareAndSet(true, false) ) {
				// mode has been disabled
				desiredState = disabledState;
				enabling = false;
			}
			if ( desiredState != null ) {
				StateChangeManager mgr = new StateChangeManager(mode, desiredState, controlIds,
						enabling);
				executeManagerTask(mgr);
			}
		}
	}

	private void executeManagerTask(StateChangeManager mgr) {
		TaskExecutor executor = getTaskExecutor();
		if ( executor != null ) {
			executor.execute(mgr);
		} else {
			mgr.run();
		}
	}

	// Settings

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.opmode.opstatemgr";
	}

	@Override
	public String getDisplayName() {
		return "Operational State Manager";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(8);

		results.add(
				new BasicTitleSettingSpecifier("info", isModeActive() ? "Active" : "Inactive", true));

		results.addAll(baseIdentifiableSettings(null));
		results.add(new BasicTextFieldSettingSpecifier("mode", ""));
		results.add(new BasicTextFieldSettingSpecifier("controlIdsValue", ""));

		// drop-down menu for enabledState
		Map<String, String> stateTitles = new LinkedHashMap<String, String>(
				DeviceOperatingState.values().length + 1);
		stateTitles.put("", "");
		for ( DeviceOperatingState e : DeviceOperatingState.values() ) {
			stateTitles.put(String.valueOf(e.getCode()), e.toString());
		}
		BasicMultiValueSettingSpecifier enabledStateSpec = new BasicMultiValueSettingSpecifier(
				"enabledStateCode", "");
		enabledStateSpec.setValueTitles(stateTitles);
		results.add(enabledStateSpec);

		// drop-down menu for disabledState
		BasicMultiValueSettingSpecifier disabledStateSpec = new BasicMultiValueSettingSpecifier(
				"disabledStateCode", "");
		disabledStateSpec.setValueTitles(stateTitles);
		results.add(disabledStateSpec);

		results.add(
				new BasicTextFieldSettingSpecifier("retryCount", String.valueOf(DEFAULT_RETRY_COUNT)));
		results.add(new BasicTextFieldSettingSpecifier("taskTimeoutSecs",
				String.valueOf(DEFAULT_TASK_TIMEOUT_SECS)));

		return results;
	}

	// Accessors

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
	 * Get the operating state to apply when the operational mode is enabled,
	 * via its code value.
	 * 
	 * @return the state to apply, as a code
	 */
	public Integer getEnabledStateCode() {
		DeviceOperatingState state = getEnabledState();
		return state != null ? state.getCode() : null;
	}

	/**
	 * Set the operating state to apply when the operational mode is enabled,
	 * via its code value.
	 * 
	 * @param code
	 *        the state to apply, as a code
	 */
	public void setEnabledStateCode(Integer code) {
		DeviceOperatingState state = null;
		if ( code != null ) {
			try {
				state = DeviceOperatingState.forCode(code);
			} catch ( IllegalArgumentException e ) {
				// ignore
				return;
			}
		}
		setEnabledState(state);
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
	 * Get the operating state to apply when the operational mode is disabled,
	 * via its code value.
	 * 
	 * @return the state to apply, as a code
	 */
	public Integer getDisabledStateCode() {
		DeviceOperatingState state = getDisabledState();
		return state != null ? state.getCode() : null;
	}

	/**
	 * Set the operating state to apply when the operational mode is disabled,
	 * via its code value.
	 * 
	 * @param code
	 *        the state to apply, as a code
	 */
	public void setDisabledStateCode(Integer code) {
		DeviceOperatingState state = null;
		if ( code != null ) {
			try {
				state = DeviceOperatingState.forCode(code);
			} catch ( IllegalArgumentException e ) {
				// ignore
				return;
			}
		}
		setDisabledState(state);
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

	/**
	 * Get the control IDs to manage, as a comma-delimited list.
	 * 
	 * @return the control IDs, as a comma-delimited list
	 */
	public String getControlIdsValue() {
		return StringUtils.commaDelimitedStringFromCollection(getControlIds());
	}

	/**
	 * Set the control IDs to manage, as a comma-delimited list.
	 * 
	 * @param controlIds
	 *        the control IDs to manage, as a comma-delimited list
	 */
	public void setControlIdsValue(String controlIds) {
		Set<String> set = StringUtils.commaDelimitedStringToSet(controlIds);
		setControlIds(set);
	}

	/**
	 * Get the retry count.
	 * 
	 * @return the retry count
	 */
	public int getRetryCount() {
		return retryCount;
	}

	/**
	 * Set the retry count.
	 * 
	 * <p>
	 * This count determines how many times the service will re-try applying
	 * state changes if an error occurs. If the retry count is {@literal 0} then
	 * no retries will be attempted. If the retry count is anything less than
	 * {@literal 0}, then the code will attempt an <b>unlimited</b> number of
	 * times. Otherwise up to {@code retryCount} attempts will be performed.
	 * </p>
	 * 
	 * @param retryCount
	 *        the retry count to set
	 */
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

}
