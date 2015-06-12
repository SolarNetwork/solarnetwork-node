/* ==================================================================
 * MockIdTagScanner.java - 11/06/2015 7:52:29 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.solarnetwork.node.ocpp.ChargeSession;
import net.solarnetwork.node.ocpp.ChargeSessionManager;
import net.solarnetwork.node.ocpp.OCPPException;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.FilterableService;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;
import ocpp.v15.cs.AuthorizationStatus;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * A mock service that listens for
 * {@link ChargeSessionManager#EVENT_TOPIC_SOCKET_ACTIVATED} and
 * {@link ChargeSessionManager#EVENT_TOPIC_SOCKET_DEACTIVATED} messages to
 * simulate a device that provides an OCPP <em>ID tag</em> and initiates or
 * completes a charging session. This setup is purely for development testing.
 * In the real world some other bundle would be listening for an RFID scan or
 * something to provide ID tag values.
 * 
 * @author matt
 * @version 1.0
 */
public class MockIdTagScanner implements SettingSpecifierProvider {

	private ChargeSessionManager chargeSessionManager;
	private OptionalService<EventAdmin> eventAdmin;
	private MessageSource messageSource;
	private final ExecutorService executor = Executors.newSingleThreadExecutor(); // to kick off the handleEvent() thread
	private String idTag = "MockIdTag";
	private boolean active = false;
	private String socketId = "/socket/mock";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ConcurrentMap<String, String> socketSessionMap = new ConcurrentHashMap<String, String>(
			4);

	private void handleChargingStatusChange(final boolean pluggedIn) {
		EventAdmin ea = (eventAdmin != null ? eventAdmin.service() : null);
		if ( ea == null ) {
			return;
		}
		final String socketId = this.socketId;
		Map<String, Object> props = Collections.singletonMap(
				ChargeSessionManager.EVENT_PROPERTY_SOCKET_ID, (Object) socketId);
		ea.postEvent(new Event(pluggedIn ? ChargeSessionManager.EVENT_TOPIC_SOCKET_ACTIVATED
				: ChargeSessionManager.EVENT_TOPIC_SOCKET_DEACTIVATED, props));
		if ( pluggedIn ) {
			executor.submit(new Runnable() {

				@Override
				public void run() {
					try {
						String sessionId = chargeSessionManager.initiateChargeSession(idTag, socketId,
								null);
						socketSessionMap.put(socketId, sessionId);
					} catch ( OCPPException e ) {
						if ( AuthorizationStatus.CONCURRENT_TX.equals(e.getStatus()) ) {
							ChargeSession session = chargeSessionManager.activeChargeSession(socketId);
							if ( session != null ) {
								socketSessionMap.put(socketId, session.getSessionId());
							}
						}
					} catch ( RuntimeException e ) {
						log.error("Error initiating a charge session on socket {}", socketId, e);
					}
				}
			});
		} else {
			final String sessionId = socketSessionMap.remove(socketId);
			if ( sessionId == null ) {
				log.debug("Unknown session ID for socket {}", socketId);
				return;
			}
			executor.submit(new Runnable() {

				@Override
				public void run() {
					try {
						chargeSessionManager.completeChargeSession(idTag, sessionId);
					} catch ( RuntimeException e ) {
						log.error("Error initiating a charge session on socket {}", socketId, e);
					}
				}
			});
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.mock.rfid";
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
		MockIdTagScanner defaults = new MockIdTagScanner();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(Locale.getDefault()), true));
		results.add(new BasicToggleSettingSpecifier("active", defaults.active));
		results.add(new BasicTextFieldSettingSpecifier("idTag", defaults.idTag));
		results.add(new BasicTextFieldSettingSpecifier("socketId", defaults.socketId));
		results.add(new BasicTextFieldSettingSpecifier(
				"filterableChargeSessionManager.propertyFilters['UID']", "OCPP Central System"));
		return results;
	}

	private String getInfoMessage(Locale locale) {
		String sessions = StringUtils.delimitedStringFromMap(socketSessionMap);
		return "Active sessions: " + (sessions != null && sessions.length() > 0 ? sessions : "none");
	}

	public void setChargeSessionManager(ChargeSessionManager chargeSessionManager) {
		this.chargeSessionManager = chargeSessionManager;
	}

	public FilterableService getFilterableChargeSessionManager() {
		ChargeSessionManager mgr = chargeSessionManager;
		if ( mgr instanceof FilterableService ) {
			return (FilterableService) mgr;
		}
		return null;
	}

	public void setIdTag(String idTag) {
		this.idTag = idTag;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		if ( active != this.active ) {
			this.active = active;
			handleChargingStatusChange(active);
		}
	}

	public String getIdTag() {
		return idTag;
	}

	public String getSocketId() {
		return socketId;
	}

	public void setSocketId(String socketId) {
		this.socketId = socketId;
	}

}
