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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.OptionalService;

/**
 * Default implementation of {@link OperationalModesService}.
 * 
 * @author matt
 * @version 1.3
 */
public class DefaultOperationalModesService extends BaseIdentifiable
		implements OperationalModesService, InstructionHandler, SettingSpecifierProvider {

	/** The setting key for operational modes. */
	public static final String SETTING_OP_MODE = "solarnode.opmode";

	/**
	 * The setting key for operational mode expiration dates.
	 *
	 * @since 1.1
	 */
	public static final String SETTING_OP_MODE_EXPIRE = "solarnode.opmode.expire";

	/** The default startup delay value. */
	public static final long DEFAULT_STARTUP_DELAY = TimeUnit.SECONDS.toMillis(10);

	/** The default rate at which to look for auto-expired modes, in seconds. */
	public static final int DEFAULT_AUTO_EXPIRE_MODES_FREQUENCY = 20;

	/**
	 * An expiration value meaning "no expiration".
	 * 
	 * @since 1.3
	 */
	public static final Long NO_EXPIRATION = -1L;

	private static final TransactionDefinition TX_RW;
	static {
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setReadOnly(false);
		TX_RW = def;
	}

	private final ConcurrentMap<String, Long> activeModes;
	private final OptionalService<SettingDao> settingDao;
	private final OptionalService<EventAdmin> eventAdmin;
	private long startupDelay = DEFAULT_STARTUP_DELAY;
	private TaskScheduler taskScheduler;
	private OptionalService<PlatformTransactionManager> transactionManager;
	private int autoExpireModesFrequency = DEFAULT_AUTO_EXPIRE_MODES_FREQUENCY;

	private ScheduledFuture<?> autoExpireScheduledFuture;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param settingDao
	 *        the setting DAO to persist operational mode changes with
	 * @param eventAdmin
	 *        the event service to post notifications with
	 * @throws IllegalArgumentException
	 *         if {@code settingDao} is {@literal null}
	 */
	public DefaultOperationalModesService(OptionalService<SettingDao> settingDao,
			OptionalService<EventAdmin> eventAdmin) {
		this(new ConcurrentHashMap<>(16, 0.9f, 2), settingDao, eventAdmin);
	}

	/**
	 * Constructor.
	 * 
	 * @param modeCache
	 *        the cache to use for active mode tracking
	 * @param settingDao
	 *        the setting DAO to persist operational mode changes with
	 * @param eventAdmin
	 *        the event service to post notifications with
	 * @throws IllegalArgumentException
	 *         if {@code settingDao} or {@code modeCache} is {@literal null}
	 * @since 1.3
	 */
	public DefaultOperationalModesService(ConcurrentMap<String, Long> modeCache,
			OptionalService<SettingDao> settingDao, OptionalService<EventAdmin> eventAdmin) {
		super();
		if ( modeCache == null ) {
			throw new IllegalArgumentException("The modeCache argument must not be null.");
		}
		this.activeModes = modeCache;
		if ( settingDao == null ) {
			throw new IllegalArgumentException("The settingDao argument must not be null.");
		}
		this.settingDao = settingDao;
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Call to initialize the service after properties configured.
	 */
	public synchronized void init() {
		// post current modes, i.e. shift from "default" to whatever is active
		final Set<String> modes = initActiveModes();

		Runnable task = new Runnable() {

			@Override
			public void run() {
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
		if ( taskScheduler != null && autoExpireScheduledFuture == null ) {
			autoExpireScheduledFuture = taskScheduler.scheduleWithFixedDelay(new AutoExpireModesTask(),
					new Date(System.currentTimeMillis() + startupDelay
							+ this.autoExpireModesFrequency * 1000L),
					this.autoExpireModesFrequency * 1000L);
		}
	}

	private synchronized Set<String> initActiveModes() {
		activeModes.clear();
		SettingDao dao = settingDao.service();
		if ( dao == null ) {
			return emptySet();
		}
		PlatformTransactionManager txManager = OptionalService.service(transactionManager);
		TransactionStatus tx = null;
		if ( txManager != null ) {
			tx = txManager.getTransaction(TX_RW);
		}
		try {
			List<KeyValuePair> modes = dao.getSettingValues(SETTING_OP_MODE);
			if ( modes == null ) {
				return emptySet();
			}
			Set<String> active = new LinkedHashSet<>();
			final long now = System.currentTimeMillis();
			for ( KeyValuePair kv : modes ) {
				String mode = kv.getValue();
				Long exp = modeExpiration(dao, mode);
				if ( exp == null || exp.longValue() > now ) {
					activeModes.put(mode, exp != null ? exp : NO_EXPIRATION);
					active.add(mode);
				}
			}
			return active;
		} finally {
			if ( tx != null ) {
				txManager.commit(tx);
			}
		}
	}

	private final class AutoExpireModesTask implements Runnable {

		@Override
		public void run() {
			final long now = System.currentTimeMillis();
			Set<String> toDeactivate = null;
			for ( Map.Entry<String, Long> me : activeModes.entrySet() ) {
				Long exp = me.getValue();
				if ( !NO_EXPIRATION.equals(exp) && exp.longValue() < now ) {
					if ( toDeactivate == null ) {
						toDeactivate = new HashSet<>(4);
					}
					toDeactivate.add(me.getKey());
				}
			}
			if ( toDeactivate == null ) {
				// no expired values
				return;
			}
			SettingDao dao = settingDao.service();
			if ( dao == null ) {
				return;
			}
			PlatformTransactionManager txManager = OptionalService.service(transactionManager);
			TransactionStatus tx = null;
			if ( txManager != null ) {
				tx = txManager.getTransaction(TX_RW);
			}
			try {
				for ( String mode : toDeactivate ) {
					dao.deleteSetting(SETTING_OP_MODE, mode);
					dao.deleteSetting(SETTING_OP_MODE_EXPIRE, mode);
					activeModes.remove(mode);
				}
				Set<String> newActive = activeOperationalModes();
				log.info("Expired operational modes [{}]; active modes now [{}]",
						commaDelimitedStringFromCollection(toDeactivate),
						commaDelimitedStringFromCollection(newActive));
				postOperationalModesChangedEvent(newActive);
			} finally {
				if ( tx != null ) {
					txManager.commit(tx);
				}
			}
		}
	}

	/**
	 * Manually run the auto-expire task.
	 * 
	 * <p>
	 * This is primarily designed to support testing.
	 * </p>
	 */
	public void expireNow() {
		new AutoExpireModesTask().run();
	}

	/**
	 * Call to close internal resources.
	 */
	public synchronized void close() {
		if ( autoExpireScheduledFuture != null ) {
			autoExpireScheduledFuture.cancel(true);
			autoExpireScheduledFuture = null;
		}
	}

	@Override
	public String getSettingUID() {
		return DefaultOperationalModesService.class.getName();
	}

	@Override
	public String getDisplayName() {
		return "Operational Modes Servcie";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MessageSource messages = getMessageSource();
		StringBuilder buf = new StringBuilder();
		Set<String> modes = new TreeSet<>(activeModes.keySet());
		for ( String mode : modes ) {
			Long exp = activeModes.get(mode);
			buf.append(messages.getMessage("activeModes.row",
					new Object[] { mode,
							!NO_EXPIRATION.equals(exp) ? DateTimeFormatter
									.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
									.format(ZonedDateTime.ofInstant(
											Instant.ofEpochMilli(exp.longValue()),
											ZoneId.systemDefault()))
									: "-" },
					null));
		}
		String status = messages.getMessage("activeModes.msg", new Object[] { buf }, null);
		return Collections
				.singletonList(new BasicTitleSettingSpecifier("activeModes", status, true, true));
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
		DateTime expire = OperationalModesService.expirationDate(instruction);
		Set<String> opModes = new LinkedHashSet<>(Arrays.asList(modes));
		switch (topic) {
			case TOPIC_ENABLE_OPERATIONAL_MODES:
				enableOperationalModes(opModes, expire);
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
		boolean inverted = false;
		mode = mode.toLowerCase();
		if ( mode.charAt(0) == '!' ) {
			mode = mode.substring(1);
			inverted = true;
		}
		Long exp = activeModes.get(mode);
		boolean result = exp != null
				&& (NO_EXPIRATION.equals(exp) || exp.longValue() < System.currentTimeMillis());
		return (inverted ? !result : result);
	}

	@Override
	public Set<String> activeOperationalModes() {
		final long now = System.currentTimeMillis();
		return activeModes.entrySet().stream().filter(e -> {
			Long exp = e.getValue();
			return (NO_EXPIRATION.equals(exp) || exp.longValue() > now);
		}).map(Map.Entry::getKey).collect(Collectors.toSet());
	}

	private Long modeExpiration(SettingDao dao, String mode) {
		if ( dao == null ) {
			return null;
		}
		String exp = dao.getSetting(SETTING_OP_MODE_EXPIRE, mode);
		if ( exp != null ) {
			try {
				return Long.valueOf(exp);
			} catch ( NumberFormatException e ) {
				// ignore
			}
		}
		return null;
	}

	@Override
	public Set<String> enableOperationalModes(Set<String> modes) {
		return enableOperationalModes(modes, null);
	}

	@Override
	public synchronized Set<String> enableOperationalModes(Set<String> modes, DateTime expire) {
		if ( modes == null || modes.isEmpty() ) {
			return activeOperationalModes();
		}
		Set<String> toActivate = null;
		Long expireMs = (expire != null ? expire.getMillis() : null);
		for ( String mode : modes ) {
			if ( mode == null ) {
				continue;
			}
			mode = mode.toLowerCase();
			Long exp = activeModes.get(mode);
			if ( (expireMs == null && !NO_EXPIRATION.equals(exp))
					|| (expireMs != null && (exp == null || !expireMs.equals(exp))) ) {
				if ( toActivate == null ) {
					toActivate = new HashSet<>();
				}
				toActivate.add(mode);
			}
		}
		if ( toActivate == null ) {
			// no chnage
			return activeOperationalModes();
		}
		SettingDao dao = settingDao.service();
		if ( dao == null ) {
			return activeOperationalModes();
		}
		PlatformTransactionManager txManager = OptionalService.service(transactionManager);
		TransactionStatus tx = null;
		if ( txManager != null ) {
			tx = txManager.getTransaction(TX_RW);
		}
		try {
			for ( String mode : toActivate ) {
				activeModes.put(mode, expireMs != null ? expireMs : NO_EXPIRATION);
				dao.storeSetting(SETTING_OP_MODE, mode, mode);
				if ( expire != null ) {
					dao.storeSetting(SETTING_OP_MODE_EXPIRE, mode, String.valueOf(expireMs));
				} else {
					dao.deleteSetting(SETTING_OP_MODE_EXPIRE, mode);
				}
			}
			Set<String> active = activeOperationalModes();
			if ( log.isInfoEnabled() ) {
				log.info("Enabled operational modes [{}], expiring [{}]; active modes now [{}]",
						commaDelimitedStringFromCollection(modes), (expire != null ? expire : "never"),
						commaDelimitedStringFromCollection(active));
			}
			postOperationalModesChangedEvent(active);
			return active;
		} finally {
			if ( tx != null ) {
				txManager.commit(tx);
			}
		}
	}

	@Override
	public synchronized Set<String> disableOperationalModes(Set<String> modes) {
		Set<String> active = activeOperationalModes();
		if ( modes == null || modes.isEmpty() ) {
			return active;
		}
		Set<String> toDeactivate = null;
		for ( String mode : modes ) {
			if ( mode == null ) {
				continue;
			}
			mode = mode.toLowerCase();
			if ( active.contains(mode) ) {
				if ( toDeactivate == null ) {
					toDeactivate = new LinkedHashSet<>(modes.size());
				}
				toDeactivate.add(mode);
			}
		}
		if ( toDeactivate == null ) {
			// no chnage
			return active;
		}
		SettingDao dao = settingDao.service();
		if ( dao == null ) {
			return emptySet();
		}
		PlatformTransactionManager txManager = OptionalService.service(transactionManager);
		TransactionStatus tx = null;
		if ( txManager != null ) {
			tx = txManager.getTransaction(TX_RW);
		}
		try {
			for ( String mode : toDeactivate ) {
				activeModes.remove(mode);
				dao.deleteSetting(SETTING_OP_MODE, mode);
				active.remove(mode);
			}
			if ( log.isInfoEnabled() ) {
				log.info("Disabled operational modes [{}]; active modes now [{}]",
						commaDelimitedStringFromCollection(modes),
						commaDelimitedStringFromCollection(active));
			}
			postOperationalModesChangedEvent(active);
			return active;
		} finally {
			if ( tx != null ) {
				txManager.commit(tx);
			}
		}
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
	 * Note this requires a {@link #setTaskScheduler(TaskScheduler)} to be
	 * configured if set to anything &gt; {@literal 0}.
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
	 * This is required by {@link #setStartupDelay(long)} as well as for
	 * supporting auto-expiring modes.
	 * </p>
	 * 
	 * @param taskScheduler
	 *        a task executor
	 * @see #setStartupDelay(long)
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Set the frequency, in seconds, at which to look for auto-expired
	 * operational modes.
	 * 
	 * @param autoExpireModesFrequency
	 *        the frequency, in seconds
	 * @throws IllegalArgumentException
	 *         if {@code autoExpireModesFrequency} is less than {@literal 1}
	 * @since 1.1
	 */
	public void setAutoExpireModesFrequency(int autoExpireModesFrequency) {
		if ( autoExpireModesFrequency < 1 ) {
			throw new IllegalArgumentException("autoExpireModesFrequency must be > 0");
		}
		this.autoExpireModesFrequency = autoExpireModesFrequency;
	}

	/**
	 * Get the transaction manager.
	 * 
	 * @return the transaction manager to use
	 * @since 1.3
	 */
	public OptionalService<PlatformTransactionManager> getTransactionManager() {
		return transactionManager;
	}

	/**
	 * Set the transaction manager.
	 * 
	 * @param transactionManager
	 *        the transaction manager to use
	 * @since 1.3
	 */
	public void setTransactionManager(OptionalService<PlatformTransactionManager> transactionManager) {
		this.transactionManager = transactionManager;
	}

}
