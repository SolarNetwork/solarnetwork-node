/* ==================================================================
 * SolarCfgWifiConfiguration.java - 22/06/2020 5:09:13 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.wifi;

import static net.solarnetwork.node.Constants.solarNodeHome;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.util.StringUtils;

/**
 * Settings provider for WiFi.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarCfgWifiConfiguration extends BaseIdentifiable
		implements SettingSpecifierProvider, SettingsChangeObserver {

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** The default value for the {@code command} property. */
	public static final String DEFAULT_COMMAND = solarNodeHome() + "/bin/solarcfg";

	private String command = DEFAULT_COMMAND;
	private String country;
	private String ssid;
	private String password;

	/**
	 * Constructor.
	 */
	public SolarCfgWifiConfiguration() {
		setUID("net.solarnetwork.node.setup.wifi.SolarCfgWifiConfiguration");
		setDisplayName("WiFi Setup");
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( country == null || country.isEmpty() || ssid == null || ssid.isEmpty() ) {
			// in case we haven't loaded current settings ever, do that now
			Settings s = currentSettings();
			if ( country == null || country.isEmpty() ) {
				country = s.country;
			}
			if ( ssid == null || ssid.isEmpty() ) {
				ssid = s.ssid;
			}
		}
		if ( command == null || command.isEmpty() || country == null || country.isEmpty() || ssid == null
				|| ssid.isEmpty() || password == null || password.isEmpty() ) {
			return;
		}
		log.info("WiFi configuration updated: country = {}, ssid = [{}]", country, ssid);
		updateConfiguration();
	}

	@Override
	public String getSettingUID() {
		return getUID();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		final Settings settings = currentSettings();
		final Status status = currentStatus();

		final List<SettingSpecifier> result = new ArrayList<>(4);
		result.add(new BasicTitleSettingSpecifier("status", statusMessage(status)));

		// note how all WiFi settings have transient = true; this prevents them from getting copied into
		// the settings database; thus we rely on the OS-configured values

		BasicTextFieldSettingSpecifier f = new BasicTextFieldSettingSpecifier("country",
				settings.country);
		f.setTransient(true);
		result.add(f);
		f = new BasicTextFieldSettingSpecifier("ssid", settings.ssid);
		f.setTransient(true);
		result.add(f);
		f = new BasicTextFieldSettingSpecifier("password", "", true);
		f.setTransient(true);
		result.add(f);
		return result;
	}

	private String statusMessage(Status status) {
		StringBuffer buf = new StringBuffer();
		MessageSource messageSource = getMessageSource();
		if ( messageSource != null ) {
			if ( status.active ) {
				buf.append(
						messageSource.getMessage("active.label", null, "Active", Locale.getDefault()));
			} else {
				buf.append(messageSource.getMessage("inactive.label", null, "Inactive",
						Locale.getDefault()));
			}
			if ( status.addresses != null && !status.addresses.isEmpty() ) {
				buf.append("; ");
				if ( status.addresses.size() == 1 ) {
					buf.append(messageSource
							.getMessage("address.label", null, "address", Locale.getDefault())
							.toLowerCase());
				} else {
					buf.append(messageSource
							.getMessage("addresses.label", null, "addresses", Locale.getDefault())
							.toLowerCase());
				}
				buf.append(": ");
				buf.append(StringUtils.delimitedStringFromCollection(status.addresses, ", "));
			}
		}
		return buf.toString();
	}

	private static final class Settings {

		private final String country;
		private final String ssid;

		private Settings(String country, String ssid) {
			super();
			this.country = country;
			this.ssid = ssid;
		}
	}

	private static final class Status {

		private final boolean active;
		private final List<String> addresses;

		private Status(boolean active, List<String> addresses) {
			super();
			this.active = active;
			this.addresses = addresses;
		}
	}

	private Status currentStatus() {
		boolean active = false;
		List<String> addresses = Collections.emptyList();
		try {
			List<String> result = executeAction("status");
			if ( result != null && !result.isEmpty() ) {
				active = "active".equalsIgnoreCase(result.get(0));
				result.remove(0);
				addresses = result;
			}
		} catch ( Throwable t ) {
			log.warn("Error getting current WiFi settings: {}", t.getMessage());
		}
		return new Status(active, addresses);
	}

	private Settings currentSettings() {
		String country = "";
		String ssid = "";
		try {
			List<String> result = executeAction("settings");
			if ( result != null && !result.isEmpty() ) {
				country = result.get(0);
				if ( result.size() > 1 ) {
					ssid = result.get(1);
				}
			}
		} catch ( Throwable t ) {
			log.warn("Error getting current WiFi settings: {}", t.getMessage());
		}
		return new Settings(country, ssid);
	}

	/**
	 * Update the WiFi configuration.
	 * 
	 * @return the output of the configured command
	 */
	public List<String> updateConfiguration() {
		return executeAction("configure", "-c", country, "-s", ssid, "-p", password);
	}

	private synchronized List<String> executeAction(final String action, String... args) {
		log.debug("Executing action {}", action);
		List<String> cmd = new ArrayList<>(8);
		cmd.add(command);
		cmd.add("wifi");
		cmd.add(action);
		if ( args != null && args.length > 0 ) {
			for ( String arg : args ) {
				cmd.add(arg);
			}
		}
		List<String> result = new ArrayList<>(8);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			Process pr = pb.start();
			BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = null;
			while ( (line = in.readLine()) != null ) {
				result.add(line);
			}

			BufferedReader err = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
			StringBuilder buf = new StringBuilder();
			line = null;
			while ( (line = err.readLine()) != null ) {
				if ( buf.length() > 0 ) {
					buf.append('\n');
				}
				buf.append(line);
			}
			if ( buf.length() > 0 ) {
				log.error("Error executing action {}: {}", action, buf);
			}
			return result;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Set the command to use.
	 * 
	 * @param command
	 *        the command to set; defaults to {@link #DEFAULT_COMMAND}
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * Set the WiFi country code.
	 * 
	 * @param country
	 *        the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * Set the WiFi SSID to connect to.
	 * 
	 * @param ssid
	 *        the SSID to set
	 */
	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	/**
	 * Set the WiFi password to use.
	 * 
	 * @param password
	 *        the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
