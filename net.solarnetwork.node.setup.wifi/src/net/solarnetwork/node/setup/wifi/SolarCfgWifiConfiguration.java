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
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.settings.SettingsChangeObserver;

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
	public void configurationChanged(Map<String, Object> properties) {
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
		final Settings s = currentSettings();

		final List<SettingSpecifier> result = new ArrayList<>(8);
		result.add(new BasicTextFieldSettingSpecifier("command", DEFAULT_COMMAND));

		// note how all WiFi settings have transient = true; this prevents them from getting copied into
		// the settings database; thus we rely on the OS-configured values

		BasicTextFieldSettingSpecifier f = new BasicTextFieldSettingSpecifier("country", s.country);
		f.setTransient(true);
		result.add(f);
		f = new BasicTextFieldSettingSpecifier("ssid", s.ssid);
		f.setTransient(true);
		result.add(f);
		f = new BasicTextFieldSettingSpecifier("password", "", true);
		f.setTransient(true);
		result.add(f);
		return result;
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
