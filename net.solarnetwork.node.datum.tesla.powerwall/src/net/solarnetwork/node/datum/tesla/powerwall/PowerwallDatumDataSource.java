/* ==================================================================
 * PowerwallDatumDataSource.java - 8/11/2023 7:46:12 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.tesla.powerwall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link MultiDatumDataSource} for Tesla Powerwall devices.
 * 
 * @author matt
 * @version 1.0
 */
public class PowerwallDatumDataSource extends DatumDataSourceSupport implements MultiDatumDataSource,
		ServiceLifecycleObserver, SettingsChangeObserver, SettingSpecifierProvider {

	/** A default timeout for connections reads. */
	public static final int DEFAULT_CONNECT_TIMEOUT = 60_000;

	/** A default timeout for connections reads. */
	public static final int DEFAULT_READ_TIMEOUT = 60_000;

	/** A default timeout for connections requests. */
	public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10_000;

	/** The {@code hostName} property default value. */
	public static final String DEFAULT_HOST_NAME = "powerwall";

	/** The {@code username} property default value. */
	public static final String DEFAULT_USERNAME = "customer";

	private String sourceId;
	private String hostName = DEFAULT_HOST_NAME;
	private String username = DEFAULT_USERNAME;
	private String password;
	private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
	private int readTimeout = DEFAULT_READ_TIMEOUT;
	private int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;

	private PowerwallOperations ops;

	/**
	 * Constructor.
	 */
	public PowerwallDatumDataSource() {
		super();
	}

	@Override
	public synchronized void serviceDidStartup() {
		if ( this.ops == null ) {
			this.ops = createOperations();
		}
	}

	@Override
	public synchronized void serviceDidShutdown() {
		closeOperations();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		closeOperations();
		this.ops = createOperations();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.tesla.powerwall";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier("hostName", DEFAULT_HOST_NAME));
		results.add(new BasicTextFieldSettingSpecifier("username", DEFAULT_USERNAME));
		results.add(new BasicTextFieldSettingSpecifier("password", null, true));
		results.addAll(getDeviceInfoMetadataSettingSpecifiers());
		return results;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PowerwallDatumDataSource{");
		if ( hostName != null ) {
			builder.append("hostName=");
			builder.append(hostName);
			builder.append(", ");
		}
		if ( sourceId != null ) {
			builder.append("sourceId=");
			builder.append(sourceId);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return NodeDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		final PowerwallOperations ops = this.ops;
		if ( ops != null ) {
			return ops.datum(sourceId);
		}
		return Collections.emptyList();
	}

	private synchronized PowerwallOperations createOperations() {
		try {
			return new PowerwallOperations(hostName, username, password, buildRequestConfig(),
					JsonUtils.newObjectMapper());
		} catch ( IllegalArgumentException e ) {
			// configuration not fully specified yet
			return null;
		}
	}

	private synchronized void closeOperations() {
		if ( this.ops != null ) {
			try {
				this.ops.close();
			} catch ( IOException e ) {
				// ignore
			} finally {
				this.ops = null;
			}
		}
	}

	private RequestConfig buildRequestConfig() {
		// @formatter:off
		return RequestConfig.custom()
				.setRedirectsEnabled(true)
				.setCookieSpec(CookieSpecs.DEFAULT)
				.setConnectTimeout(connectTimeout)
				.setSocketTimeout(readTimeout)
				.setConnectionRequestTimeout(connectionRequestTimeout)
				.build();
		// @formatter:on
	}

	/**
	 * Get the source ID to assign to generated datum.
	 * 
	 * @return the source ID to use
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to assign to generated datum.
	 * 
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the host name of the Powerwall device to connect to.
	 * 
	 * @return the host name; defaults to {@link #DEFAULT_HOST_NAME}
	 */
	public String getHostName() {
		return hostName;
	}

	/**
	 * Set the host name of the Powerwall device to connect to.
	 * 
	 * @param hostName
	 *        the host name to set; can include a custom port using a colon
	 *        delimiter
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Get the Powerwall username to use.
	 * 
	 * @return the username; defaults to {@link #DEFAULT_USERNAME}
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the Powerwall username to use.
	 * 
	 * @param username
	 *        the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Get the Powerwall password.
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the Powerwall password.
	 * 
	 * @param password
	 *        the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Get the connection timeout.
	 * 
	 * @return the timeout, in milliseconds, or {@literal 0} for no timeout or
	 *         {@literal -1} for a "system default"; defaults to
	 *         {@link #DEFAULT_CONNECT_TIMEOUT}
	 */
	public int getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * Set the connection timeout.
	 * 
	 * @param connectTimeout
	 *        the timeout to set, in milliseconds, or {@literal 0} for no
	 *        timeout or {@literal -1} for a "system default"
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Get the socket read timeout.
	 * 
	 * @return the timeout, in milliseconds, or {@literal 0} for no timeout or
	 *         {@literal -1} for a "system default"; defaults to
	 *         {@link #DEFAULT_READ_TIMEOUT}
	 */
	public int getReadTimeout() {
		return readTimeout;
	}

	/**
	 * Set the socket read timeout.
	 * 
	 * <p>
	 * This timeout defines the socket timeout ({@code SO_TIMEOUT}) in
	 * milliseconds, which is the timeout for waiting for data or, put
	 * differently, a maximum period inactivity between two consecutive data
	 * packets).
	 * </p>
	 * 
	 * @param readTimeout
	 *        the timeout to set, in milliseconds, or {@literal 0} for no
	 *        timeout or {@literal -1} for a "system default"
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * Get the connection request timeout.
	 * 
	 * @return the timeout, in milliseconds, or {@literal 0} for no timeout or
	 *         {@literal -1} for a "system default"; defaults to
	 *         {@link #DEFAULT_CONNECTION_REQUEST_TIMEOUT}
	 */
	public int getConnectionRequestTimeout() {
		return connectionRequestTimeout;
	}

	/**
	 * Set the connection request timeout.
	 * 
	 * <p>
	 * This timeout is used when requesting a connection from the connection
	 * manager.
	 * </p>
	 * 
	 * @param connectionRequestTimeout
	 *        the timeout to set, in milliseconds, or {@literal 0} for no
	 *        timeout or {@literal -1} for a "system default"
	 */
	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.connectionRequestTimeout = connectionRequestTimeout;
	}

}
