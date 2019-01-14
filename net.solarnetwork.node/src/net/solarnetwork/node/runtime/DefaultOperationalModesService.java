/* ==================================================================
 * DefaultOperationalModesService.java - 20/12/2018 10:16:02 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.support.KeyValuePair;
import net.solarnetwork.util.OptionalService;

/**
 * Default implementation of {@link OperationalModesService}.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("deprecation")
public class DefaultOperationalModesService implements OperationalModesService, InstructionHandler {

	/** The setting key for operational modes. */
	public static final String SETTING_OP_MODE = "solarnode.opmode";

	/** The default startup delay value. */
	public static final long DEFAULT_STARTUP_DELAY = TimeUnit.SECONDS.toMillis(10);

	private final OptionalService<SettingDao> settingDao;
	private final OptionalService<EventAdmin> eventAdmin;
	private long startupDelay = DEFAULT_STARTUP_DELAY;
	private TaskScheduler taskScheduler;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param settingDao
	 *        the setting DAO to persist operational mode changes with
	 * @param eventAdmin
	 *        the event service to post notifications with
	 */
	public DefaultOperationalModesService(OptionalService<SettingDao> settingDao,
			OptionalService<EventAdmin> eventAdmin) {
		super();
		this.settingDao = settingDao;
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Call to initialize the service after properties configured.
	 */
	public void init() {
		// post current modes, i.e. shift from "default" to whatever is active
		Runnable task = new Runnable() {

			@Override
			public void run() {
				Set<String> modes = activeOperationalModes();
				if ( !modes.isEmpty() ) {
					if ( log.isInfoEnabled() ) {
						log.info("Initial active operational modes [{}]",
								commaDelimitedStringFromCollection(modes));
					}
					postOperationalModesChangedEvent(modes);
				}
			}
		};
		if ( taskScheduler != null && startupDelay > 0 ) {
			taskScheduler.schedule(task, new Date(System.currentTimeMillis() + startupDelay));
		} else {
			task.run();
		}
	}

	@Override
	public boolean handlesTopic(String topic) {
		return (TOPIC_ENABLE_OPERATIONAL_MODES.equals(topic)
				|| TOPIC_DISABLE_OPERATIONAL_MODES.equals(topic));
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		if ( instruction == null ) {
			return null;
		}
		String topic = instruction.getTopic();
		if ( !handlesTopic(topic) ) {
			return null;
		}
		String[] modes = instruction.getAllParameterValues(INSTRUCTION_PARAM_OPERATIONAL_MODE);
		if ( modes == null || modes.length < 1 ) {
			return InstructionState.Declined;
		}
		Set<String> opModes = new LinkedHashSet<>(Arrays.asList(modes));
		switch (topic) {
			case TOPIC_ENABLE_OPERATIONAL_MODES:
				enableOperationalModes(opModes);
				break;

			case TOPIC_DISABLE_OPERATIONAL_MODES:
				disableOperationalModes(opModes);
				break;

		}
		return InstructionState.Completed;
	}

	@Override
	public boolean isOperationalModeActive(String mode) {
		if ( mode == null || mode.isEmpty() ) {
			return true;
		}
		mode = mode.toLowerCase();
		SettingDao dao = settingDao.service();
		if ( dao == null ) {
			return false;
		}
		String active = dao.getSetting(SETTING_OP_MODE, mode);
		return active != null;
	}

	@Override
	public Set<String> activeOperationalModes() {
		SettingDao dao = settingDao.service();
		if ( dao == null ) {
			return emptySet();
		}
		return activeModesFromSettings(dao);
	}

	private Set<String> activeModesFromSettings(SettingDao dao) {
		List<KeyValuePair> modes = dao.getSettings(SETTING_OP_MODE);
		if ( modes == null || modes.isEmpty() ) {
			return emptySet();
		}
		return modes.stream().map(m -> m.getValue()).sorted(String::compareToIgnoreCase)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@Override
	public Set<String> enableOperationalModes(Set<String> modes) {
		SettingDao dao = settingDao.service();
		if ( dao == null ) {
			return emptySet();
		}
		if ( modes != null && !modes.isEmpty() ) {
			for ( String mode : modes ) {
				if ( mode == null ) {
					continue;
				}
				mode = mode.toLowerCase();
				dao.storeSetting(SETTING_OP_MODE, mode, mode);
			}
		}
		Set<String> active = activeModesFromSettings(dao);
		if ( log.isInfoEnabled() ) {
			log.info("Enabled operational modes [{}]; active modes now [{}]",
					commaDelimitedStringFromCollection(modes),
					commaDelimitedStringFromCollection(active));
		}
		postOperationalModesChangedEvent(active);
		return active;
	}

	@Override
	public Set<String> disableOperationalModes(Set<String> modes) {
		SettingDao dao = settingDao.service();
		if ( dao == null ) {
			return emptySet();
		}
		if ( modes != null && !modes.isEmpty() ) {
			for ( String mode : modes ) {
				if ( mode == null ) {
					continue;
				}
				mode = mode.toLowerCase();
				dao.deleteSetting(SETTING_OP_MODE, mode);
			}
		}
		Set<String> active = activeModesFromSettings(dao);
		if ( log.isInfoEnabled() ) {
			log.info("Disabled operational modes [{}]; active modes now [{}]",
					commaDelimitedStringFromCollection(modes),
					commaDelimitedStringFromCollection(active));
		}
		postOperationalModesChangedEvent(active);
		return active;
	}

	private void postOperationalModesChangedEvent(Set<String> activeModes) {
		if ( activeModes == null ) {
			return;
		}
		Event event = createOperationalModesChangedEvent(activeModes);
		postEvent(event);
	}

	protected Event createOperationalModesChangedEvent(Set<String> activeModes) {
		if ( activeModes == null ) {
			activeModes = emptySet();
		}
		Map<String, ?> props = singletonMap(EVENT_PARAM_ACTIVE_OPERATIONAL_MODES, activeModes);
		return new Event(EVENT_TOPIC_OPERATIONAL_MODES_CHANGED, props);
	}

	protected final void postEvent(Event event) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || event == null ) {
			return;
		}
		ea.postEvent(event);
	}

	/**
	 * A startup delay before posting an event of the active operational modes.
	 * 
	 * <p>
	 * Note this requires a {@link #setTaskScheduler(SchedulingTaskExecutor)} to
	 * be configured if set to anything > {@literal 0}.
	 * </p>
	 * 
	 * @param startupDelay
	 *        a startup delay, in milliseconds, or {@literal 0} for no delay;
	 *        defaults to {@link #DEFAULT_STARTUP_DELAY}
	 */
	public void setStartupDelay(long startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * Configure a task scheduler.
	 * 
	 * <p>
	 * This is required by {@link #setStartupDelay(long)}.
	 * </p>
	 * 
	 * @param taskScheduler
	 *        a task executor
	 * @see #setStartupDelay(long)
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

}
