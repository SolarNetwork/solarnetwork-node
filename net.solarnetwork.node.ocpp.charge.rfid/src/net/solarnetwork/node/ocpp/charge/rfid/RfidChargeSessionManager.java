/* ==================================================================
 * RfidChargeSessionManager.java - 30/07/2016 8:25:21 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.charge.rfid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.OCPPException;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.InstructionUtils;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.FilterableService;
import net.solarnetwork.util.OptionalService;

/**
 * Listen for RFID "message received" events and initiate/conclude OCPP charge
 * sessions accordingly.
 * 
 * This service coordinates OCPP charge sessions with RFID cards. The typical
 * use case goes like this:
 * 
 * <ol>
 * <li>Person arrives in EV and scans their OCPP provider's supplied RFID
 * card.</li>
 * <li>This service requests authorization from the OCPP server to start a
 * charge session.</li>
 * <li>Assuming the charge session is authorized, this service instructs the
 * first available socket to be <b>enabled</b> to start the flow of power to the
 * EV.</li>
 * <li>The ends the charging session by scanning the same RFID card from the
 * first step.</li>
 * <li>This service ends the OCPP charge session with the OCPP server.</li>
 * <li>This service instructs the same socket from before to be <b>disabled</b>
 * and cut off any power.</li>
 * </ol>
 * 
 * To prevent open-ended charge sessions, a configurable time limit is enforced
 * on charge sessions. If a session goes past this limit, the session will be
 * automatically ended and the socket for that session disabled.
 * 
 * @author matt
 * @version 1.0
 */
public class RfidChargeSessionManager implements EventHandler, SettingSpecifierProvider {

	/** Topic for when a RFID message has been received. */
	public static final String TOPIC_RFID_MESSAGE_RECEIVED = "net/solarnetwork/node/hw/rfid/MESSAGE_RECEIVED";

	/** Event parameter for the RFID message value. */
	public static final String EVENT_PARAM_MESSAGE = "message";

	/** Event parameter for the configured {@code uid}. */
	public static final String EVENT_PARAM_UID = "uid";

	/** Event parameter for the configured {@code groupUID}. */
	public static final String EVENT_PARAM_GROUP_UID = "groupUID";

	private ChargeSessionManager chargeSessionManager;
	private OptionalService<EventAdmin> eventAdmin;
	private Collection<InstructionHandler> instructionHandlers = Collections.emptyList();
	private MessageSource messageSource;
	private int chargeSessionExpirationMinutes = 6 * 60;

	private ExecutorService executor = Executors.newSingleThreadExecutor(); // to kick off the handleEvent() thread
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Initialize after all properties configured.
	 */
	public void startup() {
		// look for active change sessions, and enable those sockets
		executor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					verifyAllSockets();
				} catch ( Throwable t ) {
					log.error("Error enabling sockets for active sessions", t);
					if ( t instanceof RuntimeException ) {
						throw (RuntimeException) t;
					}
					throw new RuntimeException(t);
				}
			}
		});
	}

	/**
	 * Call to stop handling charge sessions.
	 */
	public void shutdown() {
		executor.shutdown();
	}

	/**
	 * Find all active charge sessions and make sure the sockets associated with
	 * those sessions are enabled. This should be called when the service is
	 * first initialized, to make sure the socket state is synchronized with
	 * charge session state.
	 */
	private void verifyAllSockets() {
		Collection<String> availableSockets = chargeSessionManager.availableSocketIds();
		for ( String socketId : availableSockets ) {
			ChargeSession session = chargeSessionManager.activeChargeSession(socketId);
			boolean expired = chargeSessionExpired(session);
			if ( expired ) {
				log.info("OCPP charge session {} for IdTag {} has expired", session.getSessionId(),
						session.getIdTag());
				try {
					chargeSessionManager.completeChargeSession(session.getIdTag(),
							session.getSessionId());
				} catch ( OCPPException e ) {
					log.warn("Error completing expired OCPP session: {}", e.getMessage());
				}
			}
			adjustSocketEnabledState(socketId, (session != null && !expired));
		}
	}

	/**
	 * Test if a session has expired.
	 * 
	 * @param session
	 *        The session, or <em>null</em>.
	 * @return <em>true</em> if {@code session} is non-null and the current time
	 *         is greater than {@link ChargeSession#getExpiryDate()} or the
	 *         {@link ChargeSession#getCreated()} plus
	 *         {@code chargeSessionExpirationMinutes}.
	 */
	private boolean chargeSessionExpired(ChargeSession session) {
		if ( session == null ) {
			return false;
		}
		final long now = System.currentTimeMillis();
		if ( session.getExpiryDate() != null ) {
			long expireTime = session.getExpiryDate().toGregorianCalendar().getTimeInMillis();
			if ( expireTime < now ) {
				return true;
			}
		}
		if ( session.getCreated() != null ) {
			long expireTime = session.getCreated().getTime()
					+ (chargeSessionExpirationMinutes * 60 * 1000L);
			if ( expireTime < now ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void handleEvent(Event event) {
		if ( !TOPIC_RFID_MESSAGE_RECEIVED.equals(event.getTopic()) ) {
			return;
		}
		final Object rfidMessage = event.getProperty(EVENT_PARAM_MESSAGE);
		if ( rfidMessage == null ) {
			log.warn("Ignoring MESSAGE_RECEIVED event missing required message value");
			return;
		}

		// kick off to new thread so we don't block the event thread
		executor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					handleRfidScan(rfidMessage.toString());
				} catch ( Throwable t ) {
					log.error("Error handling RFID message {}", rfidMessage, t);
					if ( t instanceof RuntimeException ) {
						throw (RuntimeException) t;
					}
					throw new RuntimeException(t);
				}
			}
		});

	}

	/**
	 * In response to a RFID card scan, look for an available socket for a new
	 * charge session, or an existing charge session to end.
	 * 
	 * @param idTag
	 *        The RFID card ID value to treat as the OCPP IdTag.
	 */
	private void handleRfidScan(String idTag) {
		// see if there is any active charge session for that tag
		Collection<String> availableSockets = chargeSessionManager.availableSocketIds();
		Set<String> freeSockets = new LinkedHashSet<String>(availableSockets);
		for ( String socketId : availableSockets ) {
			ChargeSession session = chargeSessionManager.activeChargeSession(socketId);
			if ( session != null ) {
				if ( session.getIdTag().equals(idTag) ) {
					// found session with same IdTag... use that socket
					handleChargeSessionStateChange(socketId, idTag);
					return;
				}
				freeSockets.remove(socketId);
			}
		}
		if ( freeSockets.isEmpty() ) {
			// all sockets in use
			log.info("No free sockets to enable charge session for IdTag {}", idTag);
		} else {
			// use first available socket
			String socketId = freeSockets.iterator().next();
			log.info("Socket {} free for charge session with IdTag {}", socketId, idTag);
			handleChargeSessionStateChange(socketId, idTag);
		}
	}

	private void handleChargeSessionStateChange(final String socketId, final String idTag) {
		// is there an existing charge session available for this socket ID?
		ChargeSession session = chargeSessionManager.activeChargeSession(socketId);
		if ( session == null ) {
			// start a new session
			try {
				String sessionId = chargeSessionManager.initiateChargeSession(idTag, socketId, null);
				log.info("OCPP charge session {} for IdTag {} initiated on socket {}", sessionId, idTag,
						socketId);
				InstructionStatus.InstructionState socketEnabledState = adjustSocketEnabledState(
						socketId, true);
				if ( socketEnabledState == InstructionStatus.InstructionState.Declined ) {
					log.error("Unable to enable socket {} for charge session {}", socketId, sessionId);
					chargeSessionManager.completeChargeSession(idTag, sessionId);
				}
			} catch ( OCPPException e ) {
				log.error("Unable to initiate change session on {} for IdTag {}: {}", socketId, idTag,
						e.getStatus());
			}
		} else if ( session.getIdTag().equals(idTag) ) {
			// end existing session
			try {
				chargeSessionManager.completeChargeSession(idTag, socketId);
			} finally {
				InstructionStatus.InstructionState socketEnabledState = adjustSocketEnabledState(
						socketId, false);
				if ( socketEnabledState == InstructionStatus.InstructionState.Declined ) {
					log.error("Unable to disable socket {} for charge session {}", socketId,
							session.getSessionId());
				}
			}
			log.info("OCPP charge session {} for IdTag {} completed on socket {}",
					session.getSessionId(), idTag, socketId);
		} else {
			// not allowed to modify existing session with different RFID card
			log.info(
					"Cannot start new charge session on socket {} for IdTag {} because session already active for IdTag {}",
					socketId, idTag, session.getIdTag());
		}
	}

	private InstructionStatus.InstructionState adjustSocketEnabledState(String socketId,
			boolean enabled) {
		final BasicInstruction instr = new BasicInstruction(
				InstructionHandler.TOPIC_SET_CONTROL_PARAMETER, new Date(),
				Instruction.LOCAL_INSTRUCTION_ID, Instruction.LOCAL_INSTRUCTION_ID, null);
		instr.addParameter(socketId, String.valueOf(enabled));
		log.debug("Requesting socket {} to be {}", socketId, enabled ? "enabled" : "disabled");
		InstructionStatus.InstructionState result = InstructionUtils
				.handleInstruction(instructionHandlers, instr);
		log.debug("Request for socket {} to be {} resulted in {}", socketId,
				enabled ? "enabled" : "disabled", result);
		if ( result == null ) {
			result = InstructionStatus.InstructionState.Declined;
		}
		if ( result == InstructionStatus.InstructionState.Completed ) {
			String eventTopic = (enabled ? ChargeSessionManager.EVENT_TOPIC_SOCKET_ACTIVATED
					: ChargeSessionManager.EVENT_TOPIC_SOCKET_DEACTIVATED);
			Map<String, Object> eventProps = Collections
					.singletonMap(ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID, (Object) socketId);
			postEvent(eventTopic, eventProps);
		}
		return result;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.charge.rfid";
	}

	@Override
	public String getDisplayName() {
		return getClass().getSimpleName();
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		RfidChargeSessionManager defaults = new RfidChargeSessionManager();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);
		results.add(new BasicTextFieldSettingSpecifier(
				"filterableChargeSessionManager.propertyFilters['UID']", "OCPP Central System"));
		results.add(new BasicTextFieldSettingSpecifier("chargeSessionExpirationMinutes",
				String.valueOf(defaults.chargeSessionExpirationMinutes)));
		return results;
	}

	private void postEvent(String topic, Map<String, Object> props) {
		OptionalService<EventAdmin> eaService = eventAdmin;
		EventAdmin ea = (eaService == null ? null : eaService.service());
		if ( ea == null ) {
			return;
		}
		log.debug("Posting message {}: {}", topic, props);
		Event event = new Event(topic, props);
		ea.postEvent(event);
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setInstructionHandlers(Collection<InstructionHandler> instructionHandlers) {
		if ( instructionHandlers == null ) {
			instructionHandlers = Collections.emptyList();
		}
		this.instructionHandlers = instructionHandlers;
	}

	public void setChargeSessionManager(ChargeSessionManager chargeSessionManager) {
		this.chargeSessionManager = chargeSessionManager;
	}

	/**
	 * Set the minimum number of minutes a charge session is allowed to go
	 * before expiring.
	 * 
	 * @param chargeSessionExpirationMinutes
	 *        The minimum number of minutes before a charge session is
	 *        considered expired. Defaults to {@code 360} (6 hours).
	 */
	public void setChargeSessionExpirationMinutes(int chargeSessionExpirationMinutes) {
		if ( chargeSessionExpirationMinutes < 0 ) {
			chargeSessionExpirationMinutes = 0;
		}
		this.chargeSessionExpirationMinutes = chargeSessionExpirationMinutes;
	}

	/**
	 * Get the {@link ChargeSessionManager} as a {@link FilterableService}.
	 * 
	 * @return The filterable {@link ChargeSessionManager}, or <em>null</em> if
	 *         it is not filterable.
	 */
	public FilterableService getFilterableChargeSessionManager() {
		ChargeSessionManager mgr = chargeSessionManager;
		if ( mgr instanceof FilterableService ) {
			return (FilterableService) mgr;
		}
		return null;
	}

	/**
	 * Set an {@link ExecutorService} to run tasks with.
	 * 
	 * @param executor
	 *        The executor service to use. If <em>null</em> a default
	 *        implementation will be configured.
	 */
	public void setExecutor(ExecutorService executor) {
		if ( executor == null ) {
			executor = Executors.newSingleThreadExecutor();
		}
		this.executor = executor;
	}

	/**
	 * Get the configured {@link ExecutorService} for running tasks with.
	 * 
	 * @return The configured service.
	 */
	public ExecutorService getExecutor() {
		return executor;
	}

}
