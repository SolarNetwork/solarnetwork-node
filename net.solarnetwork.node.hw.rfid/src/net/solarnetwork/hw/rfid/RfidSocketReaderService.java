/* ==================================================================
 * RfidService.java - 29/07/2016 3:32:45 PM
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

package net.solarnetwork.hw.rfid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.OptionalService;

/**
 * Service that configures a {@link RfidSocketReader} and posts any RFID
 * messages as {@link EventAdmin} events.
 * 
 * @author matt
 * @version 1.0
 */
public class RfidSocketReaderService implements SettingSpecifierProvider, Runnable {

	/** The "heartbeat" message sent by the server after read timeouts. */
	public static final String HEARTBEAT_MSG = "ping";

	/** Topic for when a network association has been accepted. */
	public static final String TOPIC_RFID_MESSAGE_RECEIVED = "net/solarnetwork/node/hw/rfid/MESSAGE_RECEIVED";

	/** Event parameter for the RFID message value. */
	public static final String EVENT_PARAM_MESSAGE = "message";

	/**
	 * Event parameter for the RFID message date, as milliseconds since the
	 * epoch.
	 */
	public static final String EVENT_PARAM_DATE = "date";

	/** Event parameter for the RFID message counter value. */
	public static final String EVENT_PARAM_COUNT = "count";

	/** Event parameter for the configured {@code uid}. */
	public static final String EVENT_PARAM_UID = "uid";

	/** Event parameter for the configured {@code groupUID}. */
	public static final String EVENT_PARAM_GROUP_UID = "groupUID";

	private OptionalService<EventAdmin> eventAdmin;
	private MessageSource messageSource;

	private int port = 9090;
	private String host = "localhost";
	private String uid;
	private String groupUID;
	private int connectRetryMinutes = 1;

	private Thread readerThread;
	private Thread tryLaterThread;

	// stats
	private boolean initialized = false;
	private long lastHeartbeatDate;
	private long lastMessageDate;
	private long messageCount = 0;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Initialize after all properties are configured.
	 */
	public void init() {
		initialized = true;
		tryStartReaderLater();
	}

	private synchronized void startReader() {
		tryLaterThread = null;
		if ( readerThread != null ) {
			log.debug("RfidSocketReader already started, not starting again.");
			return;
		}
		if ( getPort() < 1 || getHost() == null || getHost().length() < 1 ) {
			log.debug("RFID server details not configured ({}:{}); cannot start.", getHost(), getPort());
			return;
		}
		readerThread = new Thread(this);
		readerThread.setDaemon(true);
		readerThread.setName("RFID Scanner Client");
		readerThread.start();
	}

	private synchronized void stopReader(boolean tryAgain) {
		if ( readerThread != null ) {
			if ( readerThread.isAlive() && Thread.currentThread() != readerThread ) {
				try {
					readerThread.interrupt();
				} catch ( Exception e ) {
					log.debug("Exception interrupting RfidSocketReader thread", e);
				}
			}
			readerThread = null;
		}
		if ( tryAgain ) {
			tryStartReaderLater();
		}
	}

	private synchronized void tryStartReaderLater() {
		if ( !(initialized && tryLaterThread == null) ) {
			return;
		}
		log.info("Will try to connect to RFID server in {} minutes", connectRetryMinutes);
		tryLaterThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(TimeUnit.MINUTES.toMillis(connectRetryMinutes));
					startReader();
				} catch ( InterruptedException e ) {
					// ignore this;
				}
			}
		});
		tryLaterThread.setDaemon(true);
		tryLaterThread.setName("RFID Server Connector");
		tryLaterThread.start();
	}

	@Override
	public void run() {
		BufferedReader in = null;
		Socket s = null;
		boolean tryAgain = false;
		try {
			s = SocketFactory.getDefault().createSocket(host, port);
			s.setKeepAlive(true);
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			boolean readSomething = false;
			String line;
			while ( true ) {
				line = in.readLine();
				if ( line == null ) {
					tryAgain = true;
					break;
				}
				// the first line read is a status line...
				if ( readSomething && !HEARTBEAT_MSG.equalsIgnoreCase(line) ) {
					lastMessageDate = System.currentTimeMillis();
					messageCount += 1;
					postRfidMessageReceivedEvent(line);
				} else {
					lastHeartbeatDate = System.currentTimeMillis();
					log.debug("RFID status message: {}", line);
				}
				readSomething = true;
			}
			if ( !readSomething ) {
				return;
			}
		} catch ( IOException e ) {
			log.error("RFID server communication error: " + e.getMessage());
			tryAgain = true;
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
			if ( s != null ) {
				try {
					s.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
			stopReader(tryAgain);
		}
	}

	private void postRfidMessageReceivedEvent(String msg) {
		OptionalService<EventAdmin> eaService = eventAdmin;
		EventAdmin ea = (eaService == null ? null : eaService.service());
		if ( ea == null ) {
			return;
		}
		log.debug("RFID client posting message received event: {}", msg);
		Map<String, Object> props = new HashMap<String, Object>(5);
		props.put(EVENT_PARAM_COUNT, messageCount);
		props.put(EVENT_PARAM_DATE, System.currentTimeMillis());
		if ( msg != null ) {
			props.put(EVENT_PARAM_MESSAGE, msg);
		}
		if ( uid != null ) {
			props.put(EVENT_PARAM_UID, uid);
		}
		if ( groupUID != null ) {
			props.put(EVENT_PARAM_GROUP_UID, groupUID);
		}
		Event event = new Event(TOPIC_RFID_MESSAGE_RECEIVED, props);
		ea.postEvent(event);
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.hw.rfid.client";
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
		RfidSocketReaderService defaults = new RfidSocketReaderService();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(Locale.getDefault()), true));
		results.add(new BasicToggleSettingSpecifier("uid", defaults.uid));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.groupUID));
		results.add(new BasicTextFieldSettingSpecifier("host", defaults.host));
		results.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(defaults.port)));
		return results;
	}

	private String getInfoMessage(Locale local) {
		Thread t = readerThread;
		StringBuilder buf = new StringBuilder();
		if ( t != null && t.isAlive() ) {
			buf.append("Connected and listening.");
			long count = messageCount;
			if ( count > 0 ) {
				buf.append(count).append(" messages received.");
				long mDate = lastMessageDate;
				if ( mDate > 0 ) {
					buf.append(" Last message received at ").append(new Date(mDate)).append(".");
				}
			}
			long hDate = lastHeartbeatDate;
			if ( hDate > 0 ) {
				buf.append(" Last heartbeat received at ").append(new Date(hDate)).append(".");
			}
		}
		return buf.toString();
	}

	/**
	 * The RFID server port to connect to.
	 * 
	 * @return The configured port. Defaults to {@code 9090}.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * The RFID server port to connect to.
	 * 
	 * @param port
	 *        The port to set.
	 */
	public void setPort(int port) {
		if ( port != this.port ) {
			this.port = port;
			stopReader(true);
		}
	}

	/**
	 * The host name of the RFID server to connect to.
	 * 
	 * @return The configured host. Defaults to {@link localhost}.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the host name of the RFID server to connect to.
	 * 
	 * @param host
	 *        The host to set.
	 */
	public void setHost(String host) {
		if ( host == null ) {
			return;
		}
		if ( !host.equalsIgnoreCase(this.host) ) {
			this.host = host;
			stopReader(true);
		}
	}

	/**
	 * Get the number of minutes between retrying to connect to the RFID server.
	 * 
	 * @return The configured number of minutes. Defaults to {@code 1}.
	 */
	public int getConnectRetryMinutes() {
		return connectRetryMinutes;
	}

	/**
	 * Set the number of minutes to wait before retrying to connect to the RFID
	 * server.
	 * 
	 * @param connectRetryMinutes
	 *        The number of minutes to set.
	 */
	public void setConnectRetryMinutes(int connectRetryMinutes) {
		if ( connectRetryMinutes < 0 ) {
			return;
		}
		this.connectRetryMinutes = connectRetryMinutes;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
