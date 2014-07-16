/* ==================================================================
 * YasdiMasterDeviceFactory.java - Mar 7, 2013 9:33:17 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.yasdi4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import de.michaeldenk.yasdi4j.YasdiDevice;
import de.michaeldenk.yasdi4j.YasdiDriver;

/**
 * Factory for {@link YasdiMaster} instances configured to use a serial port.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>device</dt>
 * <dd>The serial port to use, e.g. {@code /dev/ttyS0}.</dd>
 * 
 * <dt>baud</dt>
 * <dd>The port speed. Defaults to <b>1200</b>.</dd>
 * 
 * <dt>media</dt>
 * <dd>The YASDI media type. Defaults to <b>RS485</b>.</dd>
 * 
 * <dt>protocol</dt>
 * <dd>The packet format. Defaults to <b>SMANet</b>.</dd>
 * 
 * <dt>expectedDeviceCount</dt>
 * <dd>The number of expected devices for a given device. Defaults to <b>1</b>.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 */
public class YasdiMasterDeviceFactory implements SettingSpecifierProvider, ObjectFactory<YasdiMaster> {

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;
	private static de.michaeldenk.yasdi4j.YasdiMaster MASTER;
	private static List<YasdiDevice> DEVICES = new CopyOnWriteArrayList<YasdiDevice>();
	private static Map<YasdiMasterDeviceFactory, Object> FACTORIES = new WeakHashMap<YasdiMasterDeviceFactory, Object>(
			2);
	private static File INI_FILE = null;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String driver = "libyasdi_drv_serial";
	private String device = "/dev/ttyS0";
	private int baud = 1200;
	private String media = "RS485";
	private String protocol = "SMANet";
	private int expectedDeviceCount = 1;
	private boolean debugYasdi = false;

	private YasdiMasterDevice master;

	/**
	 * Default constructor.
	 */
	public YasdiMasterDeviceFactory() {
		super();
		synchronized ( FACTORIES ) {
			FACTORIES.put(this, new WeakReference<Object>(MONITOR));
		}
	}

	/**
	 * Get a UID for this factory, based on device. value.
	 * 
	 * @return the UID
	 */
	public String getUID() {
		return device;
	}

	@Override
	public synchronized YasdiMaster getObject() throws BeansException {
		if ( master != null ) {
			return master;
		}

		if ( MASTER != null ) {
			MASTER.reset();
		} else {
			// generate our configuration, based on all configured factories
			setupConfigIniFile();

			log.debug("Initializing YASDI from config {}", INI_FILE.getAbsolutePath());
			MASTER = de.michaeldenk.yasdi4j.YasdiMaster.getInstance();
			try {
				MASTER.initialize(INI_FILE.getAbsolutePath());
			} catch ( IOException e ) {
				throw new RuntimeException("Unable to initialize YasdiMaster", e);
			}
		}
		try {
			YasdiDriver[] drivers = MASTER.getDrivers();
			for ( YasdiDriver d : MASTER.getDrivers() ) {
				MASTER.setDriverOnline(d);
			}
			log.debug("Initialized {} drivers", drivers.length);
		} catch ( IOException e ) {
			throw new RuntimeException("Unable to initialize YasdiMaster", e);
		}

		// detect devices
		int expectedCount = 0;
		for ( YasdiMasterDeviceFactory factory : FACTORIES.keySet() ) {
			expectedCount += factory.expectedDeviceCount;
		}
		log.debug("Detecting devices, looking for {}", expectedCount);
		try {
			MASTER.detectDevices(expectedCount);
		} catch ( IOException e ) {
			throw new RuntimeException("Unable to detect devices", e);
		}

		if ( log.isInfoEnabled() ) {
			List<String> deviceNames = new ArrayList<String>();
			for ( YasdiDevice dev : MASTER.getDevices() ) {
				deviceNames.add(dev.getName());
			}
			log.info("Detected {} SMA devices: {}", deviceNames.size(),
					StringUtils.commaDelimitedStringFromCollection(deviceNames));
		}

		DEVICES.clear();
		DEVICES.addAll(Arrays.asList(MASTER.getDevices()));

		master = new YasdiMasterDevice(DEVICES, this.device);
		return master;
	}

	private void setupConfigIniFile() {
		Set<String> drivers = new LinkedHashSet<String>(2);
		List<YasdiMasterDeviceFactory> comDevices = new ArrayList<YasdiMasterDeviceFactory>(2);
		List<YasdiMasterDeviceFactory> ipDevices = new ArrayList<YasdiMasterDeviceFactory>(2);

		for ( YasdiMasterDeviceFactory factory : FACTORIES.keySet() ) {
			drivers.add(factory.driver);
			if ( "libyasdi_drv_serial".equals(factory.driver) ) {
				comDevices.add(factory);
			} else {
				ipDevices.add(factory);
			}
		}

		PrintWriter writer = null;
		try {
			if ( INI_FILE == null ) {
				String filePath = System.getProperty("sn.home", "");
				if ( filePath.length() > 0 ) {
					filePath += '/';
				}
				filePath += "var/yasdi.ini";
				INI_FILE = new File(filePath);
				INI_FILE.deleteOnExit();
			}
			writer = new PrintWriter(new BufferedWriter(new FileWriter(INI_FILE)), false);
		} catch ( IOException e ) {
			throw new RuntimeException("Unable to create YASDI ini file", e);
		}

		log.debug("Generating YASDI configuration file {}", INI_FILE.getAbsolutePath());

		int i = 0;
		try {
			writer.println("[DriverModules]");
			for ( String driver : drivers ) {
				writer.printf("Driver%d=%s\n", i++, driver);
			}
			writer.println();

			if ( comDevices.size() > 0 ) {
				i = 1;
				for ( YasdiMasterDeviceFactory factory : comDevices ) {
					writer.printf("[COM%d]\n", i++);
					writer.printf("Device=%s\n", factory.device);
					writer.printf("Media=%s\n", factory.media);
					writer.printf("Baudrate=%d\n", factory.baud);
					writer.printf("Protocol=%s\n", factory.protocol);
				}
				writer.println();
			}

			if ( ipDevices.size() > 0 ) {
				i = 1;
				for ( YasdiMasterDeviceFactory factory : ipDevices ) {
					writer.printf("[IP%d]\n", i++);
					writer.printf("Device=%s\n", factory.device);
					writer.printf("Protocol=%s\n", factory.protocol);
				}
				writer.println();
			}

			if ( debugYasdi ) {
				writer.println("[Misc]");
				writer.println("DebugOutput=/dev/stderr");
			}
		} finally {
			writer.flush();
			writer.close();
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.yasdi4j";
	}

	@Override
	public String getDisplayName() {
		return "YASDI Master";
	}

	@Override
	public MessageSource getMessageSource() {
		synchronized ( MONITOR ) {
			if ( MESSAGE_SOURCE == null ) {
				ResourceBundleMessageSource source = new ResourceBundleMessageSource();
				source.setBundleClassLoader(getClass().getClassLoader());
				source.setBasename(getClass().getName());
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}

	public static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(4);
		YasdiMasterDeviceFactory defaults = new YasdiMasterDeviceFactory();
		results.add(new BasicTextFieldSettingSpecifier("expectedDeviceCount", String
				.valueOf(defaults.expectedDeviceCount)));
		results.add(new BasicTextFieldSettingSpecifier("driver", defaults.driver));
		results.add(new BasicTextFieldSettingSpecifier("device", defaults.device));
		results.add(new BasicTextFieldSettingSpecifier("baud", String.valueOf(defaults.baud)));
		results.add(new BasicTextFieldSettingSpecifier("media", String.valueOf(defaults.media)));
		results.add(new BasicTextFieldSettingSpecifier("protocol", String.valueOf(defaults.protocol)));
		results.add(new BasicToggleSettingSpecifier("debugYasdi", Boolean.FALSE));
		return results;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((device == null) ? 0 : device.hashCode());
		result = prime * result + ((driver == null) ? 0 : driver.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		YasdiMasterDeviceFactory other = (YasdiMasterDeviceFactory) obj;
		if ( device == null ) {
			if ( other.device != null )
				return false;
		} else if ( !device.equals(other.device) )
			return false;
		if ( driver == null ) {
			if ( other.driver != null )
				return false;
		} else if ( !driver.equals(other.driver) )
			return false;
		return true;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getDefaultSettingSpecifiers();

		// add in read-only device UIDs
		Set<String> deviceNames = new TreeSet<String>();

		try {
			// call getObject() to initialize
			getObject();

			for ( YasdiMasterDeviceFactory factory : FACTORIES.keySet() ) {
				YasdiMaster master = factory.getObject();
				deviceNames.add(master.getName());
			}

			for ( String deviceName : deviceNames ) {
				results.add(0, new BasicTitleSettingSpecifier("availableDevice", deviceName, true));
			}
		} catch ( RuntimeException e ) {
			log.warn("Exception getting YASDI device names: {}", e.getMessage());
		}

		return results;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	public void setBaud(int baud) {
		this.baud = baud;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setExpectedDeviceCount(int expectedDeviceCount) {
		this.expectedDeviceCount = expectedDeviceCount;
	}

	public void setDebugYasdi(boolean debugYasdi) {
		this.debugYasdi = debugYasdi;
	}

}
