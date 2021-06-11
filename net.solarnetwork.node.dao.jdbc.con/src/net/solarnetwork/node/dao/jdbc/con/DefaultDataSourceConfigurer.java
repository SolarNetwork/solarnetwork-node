/* ==================================================================
 * DefaultDataSourceConfigurer.java - 11/06/2021 10:49:53 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.con;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publish a data source managed service factory configuration, if one does not
 * already exist.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultDataSourceConfigurer {

	/** The default properties path to use. */
	public static final String DEFAULT_PROPERTIES_PATH = "net.solarnetwork.jdbc.pool.default.properties";

	/** The default {@literal db} service property name to use. */
	public static final String DEFAULT_DB_SERVICE_NAME = "node";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ConfigurationAdmin configurationAdmin;
	private final Class<?> propertiesClass;
	private final String propertiesPath;
	private final String dbServiceName;

	/**
	 * Constructor.
	 * 
	 * @param configurationAdmin
	 *        the configuration admin
	 */
	public DefaultDataSourceConfigurer(ConfigurationAdmin configurationAdmin) {
		this(configurationAdmin, DefaultDataSourceConfigurer.class, DEFAULT_PROPERTIES_PATH,
				DEFAULT_DB_SERVICE_NAME);
	}

	/**
	 * Constructor.
	 * 
	 * @param configurationAdmin
	 *        the configuration admin
	 * @param propertiesClass
	 *        the class from which to load {@code propertiesPath}
	 * @param propertiesPath
	 *        the properties path
	 * @param dbServiceName
	 *        the {@literal db} service property name
	 */
	public DefaultDataSourceConfigurer(ConfigurationAdmin configurationAdmin, Class<?> propertiesClass,
			String propertiesPath, String dbServiceName) {
		super();
		this.configurationAdmin = configurationAdmin;
		this.propertiesClass = propertiesClass;
		this.propertiesPath = propertiesPath;
		this.dbServiceName = dbServiceName;
	}

	/**
	 * Call once properties have been configured.
	 */
	public void init() {
		Properties props = new Properties();
		try (InputStream in = propertiesClass.getResourceAsStream(propertiesPath)) {
			props.load(in);
		} catch ( IOException e ) {
			log.error("Properties path [{}] not available: {}", propertiesPath, e.toString());
			return;
		}
		String factoryPid = props.getProperty("service.factoryPid");
		if ( factoryPid == null ) {
			log.error("Properties path [{}] missing service.factoryPid key.", propertiesPath);
			return;
		}
		try {
			Configuration cfg = findExistingConfiguration(factoryPid);
			if ( cfg == null ) {
				createConfiguration(factoryPid, props);
			}
		} catch ( IOException | InvalidSyntaxException e ) {
			log.error("Error finding Configuration for factory PID [{}] and db service name [{}]: {}",
					factoryPid, dbServiceName, e.toString());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void createConfiguration(String factoryPid, Properties props) throws IOException {
		Configuration cfg;
		log.info("Registering default JDBC pool configuration [{}] from [{}]", dbServiceName,
				propertiesPath);
		cfg = configurationAdmin.createFactoryConfiguration(factoryPid, null);
		cfg.update((Hashtable) props); // making assumption Properties has only String keys here
	}

	private Configuration findExistingConfiguration(String pid)
			throws IOException, InvalidSyntaxException {
		String filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + pid
				+ ")(serviceProperty.db=" + dbServiceName + "))";
		Configuration[] configurations = configurationAdmin.listConfigurations(filter);
		if ( configurations != null && configurations.length > 0 ) {
			return configurations[0];
		} else {
			return null;
		}
	}

}
