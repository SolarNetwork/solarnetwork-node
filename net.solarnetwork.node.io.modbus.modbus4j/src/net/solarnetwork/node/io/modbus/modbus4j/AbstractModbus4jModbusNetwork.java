/* ==================================================================
 * AbstractModbus4jModbusNetwork.java - 24/11/2022 3:33:03 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.modbus4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.sero.log.RollingIOLog;
import com.serotonin.modbus4j.sero.messaging.MessagingExceptionHandler;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.AbstractModbusNetwork;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Base class for Modbus4j implementations of {@link ModbusNetwork}.
 * 
 * @param <T>
 *        the network type
 * @author matt
 * @version 1.0
 */
public abstract class AbstractModbus4jModbusNetwork<T extends ModbusMaster> extends AbstractModbusNetwork
		implements SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver {

	/** The {@code keepOpenSeconds} property default value. */
	public static final int DEFAULT_KEEP_OPEN_SECONDS = 0;

	/** The {@code ioLogDir} property default property. */
	public static final String DEFAULT_IO_LOG_DIR = System.getProperty("java.io.tmpdir", "/tmp");

	/** The {@code ioLogMaxFileSize} property default value. */
	public static final int DEFAULT_IO_LOG_MAX_FILE_SIZE = 1024 * 1024;

	/** The {@code ioLogMaxFileSize} property default value. */
	public static final int DEFAULT_IO_LOG_MAX_FILE_COUNT = 5;

	/** The Modbus controller. */
	protected T controller;

	private int keepOpenSeconds = DEFAULT_KEEP_OPEN_SECONDS;
	private String ioLogDir = DEFAULT_IO_LOG_DIR;
	private int ioLogMaxFileSize = DEFAULT_IO_LOG_MAX_FILE_SIZE;
	private int ioLogMaxFileCount = DEFAULT_IO_LOG_MAX_FILE_COUNT;

	private Modbus4jCachedModbusConnection cachedConnection;

	/**
	 * Constructor.
	 */
	public AbstractModbus4jModbusNetwork() {
		super();
		setUid(null);
	}

	@Override
	public void serviceDidStartup() {
		configurationChanged(null);
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( controller != null ) {
			controller.destroy();
			controller = null;
		}
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		try {
			if ( cachedConnection != null ) {
				cachedConnection.forceClose();
				cachedConnection = null;
			}
			if ( controller != null ) {
				controller.destroy();
			}
			if ( isConfigured() ) {
				controller = createController();
				configureController(controller);
			}
		} catch ( Exception e ) {
			log.error("Error applying configuration change: {}", e.toString(), e);
		}
	}

	/**
	 * Test if the network is fully configured.
	 * 
	 * @return {@literal true} if the configuration of the network is complete,
	 *         and can be used
	 */
	protected abstract boolean isConfigured();

	/**
	 * Create a new controller instance.
	 * 
	 * @return the new controller instance
	 */
	protected abstract T createController();

	/**
	 * Configure a new controller instance.
	 * 
	 * @param controller
	 *        the instance to configure
	 */
	protected void configureController(T controller) {
		controller.setRetries(getRetries());
		if ( log.isTraceEnabled() ) {
			Path p = Paths.get(ioLogDir, "solarnode-modbus-io.log");
			log.info("Modbus IO tracing enabled to {}", p);
			controller.setIoLog(new RollingIOLog(p.getFileName().toString(), new File(ioLogDir),
					ioLogMaxFileSize, ioLogMaxFileCount - 1));
		} else {
			controller.setIoLog(null);
		}
		controller.setExceptionHandler(new MessagingExceptionHandler() {

			@Override
			public void receivedException(Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public synchronized ModbusConnection createConnection(int unitId) {
		if ( !isConfigured() || controller == null ) {
			return null;
		}

		if ( keepOpenSeconds > 0 ) {
			if ( cachedConnection == null ) {
				cachedConnection = new Modbus4jCachedModbusConnection(unitId, isHeadless(), controller,
						this::getNetworkDescription, keepOpenSeconds);
			}

			return createLockingConnection(cachedConnection);
		}

		return createLockingConnection(new Modbus4jModbusConnection(unitId, isHeadless(), controller,
				this::getNetworkDescription));
	}

	/**
	 * Get basic network settings.
	 * 
	 * @param retries
	 *        the default retries value
	 * @param keepOpenSeconds
	 *        the default keep open seconds value
	 * @return the settings
	 */
	public static List<SettingSpecifier> baseModbus4jModbusNetworkSettings(int retries,
			int keepOpenSeconds) {
		List<SettingSpecifier> results = new ArrayList<>(1);
		results.add(new BasicTextFieldSettingSpecifier("retries", String.valueOf(retries)));
		results.add(
				new BasicTextFieldSettingSpecifier("keepOpenSeconds", String.valueOf(keepOpenSeconds)));
		return results;
	}

	/**
	 * Get the number of seconds to keep the TCP connection open, for repeated
	 * transaction use.
	 * 
	 * @return the number of seconds; defaults to
	 *         {@link #DEFAULT_KEEP_OPEN_SECONDS}
	 */
	public int getKeepOpenSeconds() {
		return keepOpenSeconds;
	}

	/**
	 * Set the number of seconds to keep the TCP connection open, for repeated
	 * transaction use.
	 * 
	 * @param keepOpenSeconds
	 *        the number of seconds, or anything less than {@literal 1} to not
	 *        keep connections open
	 */
	public void setKeepOpenSeconds(int keepOpenSeconds) {
		this.keepOpenSeconds = keepOpenSeconds;
	}

	/**
	 * Get the IO trace log directory.
	 * 
	 * @return the directory path
	 */
	public String getIoLogDir() {
		return ioLogDir;
	}

	/**
	 * Set the IO trace log directory.
	 * 
	 * @param ioLogDir
	 *        the directory path to set
	 */
	public void setIoLogDir(String ioLogDir) {
		this.ioLogDir = ioLogDir;
	}

	/**
	 * Get the IO trace log maximum file size.
	 * 
	 * @return the maximum size, in bytes
	 */
	public int getIoLogMaxFileSize() {
		return ioLogMaxFileSize;
	}

	/**
	 * Set the IO trace log maximum file size.
	 * 
	 * @param ioLogMaxFileSize
	 *        the maximum size to set, in bytes; if anything less than
	 *        {@literal 1024} then {@literal 1024} will be set instead
	 */
	public void setIoLogMaxFileSize(int ioLogMaxFileSize) {
		this.ioLogMaxFileSize = (ioLogMaxFileSize < 1024 ? 1024 : ioLogMaxFileSize);
	}

	/**
	 * Get the IO trace log maximum file count.
	 * 
	 * @return the maximum number of IO trace logs
	 */
	public int getIoLogMaxFileCount() {
		return ioLogMaxFileCount;
	}

	/**
	 * Set the IO trace log maximum file count.
	 * 
	 * @param ioLogMaxFileCount
	 *        the maximum number of IO trace logs to set; if anything less than
	 *        {@literal 1} then {@literal 1} will be set instead
	 */
	public void setIoLogMaxFileCount(int ioLogMaxFileCount) {
		this.ioLogMaxFileCount = (ioLogMaxFileCount < 1 ? 1 : ioLogMaxFileCount);
	}

}
