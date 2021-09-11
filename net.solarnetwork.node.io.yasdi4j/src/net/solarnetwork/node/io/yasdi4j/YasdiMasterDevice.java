/* ==================================================================
 * YasdiMasterDevice.java - Mar 7, 2013 10:00:07 AM
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.michaeldenk.yasdi4j.YasdiDevice;
import net.solarnetwork.node.service.LockTimeoutException;

/**
 * Implementation of {@link YasdiMaster}.
 * 
 * @author matt
 * @version 2.0
 */
public class YasdiMasterDevice implements YasdiMaster {

	private final Collection<YasdiDevice> devices;
	private final String commDevice;
	private final Map<Long, ReentrantLock> locks;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param device
	 *        the device
	 */
	public YasdiMasterDevice(Collection<YasdiDevice> devices, String commDevice) {
		super();
		this.devices = devices;
		this.commDevice = commDevice;
		this.locks = new HashMap<>(devices == null ? 0 : devices.size());
		if ( devices != null ) {
			for ( YasdiDevice device : devices ) {
				locks.put(device.getSN(), new ReentrantLock(true));
			}
		}
	}

	@Override
	public Collection<YasdiDevice> getDevices() {
		return devices;
	}

	@Override
	public String getUID() {
		// returns the device serial number to uniquely identify the device
		return String.valueOf(commDevice);
	}

	@Override
	public String getCommDevice() {
		return commDevice;
	}

	@Override
	public String getName() {
		return getCommDevice();
	}

	@Override
	public YasdiDevice getDevice(long serialNumber) {
		for ( YasdiDevice d : devices ) {
			if ( d.getSN() == serialNumber ) {
				return d;
			}
		}
		return null;
	}

	@Override
	public YasdiDevice getDeviceMatchingName(String name) {
		Pattern p = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
		for ( YasdiDevice d : devices ) {
			Matcher m = p.matcher(d.getName());
			if ( m.find() ) {
				return d;
			}
		}
		return null;
	}

	@Override
	public void acquireDeviceLock(YasdiDevice device, long timeout, TimeUnit timeoutUnit)
			throws LockTimeoutException {
		assert device != null;
		final Long deviceID = device.getSN();
		ReentrantLock lock = locks.get(deviceID);
		if ( lock == null ) {
			throw new RuntimeException("No lock available for device " + device);
		}
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("YasdiDevice {} lock already acquired", deviceID);
			return;
		}
		log.debug("Acquiring lock on YasdiDevice {}; waiting at most {} {}", deviceID, timeout,
				timeoutUnit);
		try {
			if ( lock.tryLock(timeout, timeoutUnit) ) {
				log.debug("Acquired YasdiDevice {} lock", deviceID);
				return;
			}
			log.debug("Timeout acquiring YasdiDevice {} lock", deviceID);
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for YasdiDevice {} lock", deviceID);
		}
		throw new LockTimeoutException("Could not acquire YasdiDevice " + deviceID + " lock");
	}

	@Override
	public void releaseDeviceLock(YasdiDevice device) {
		assert device != null;
		final Long deviceID = device.getSN();
		ReentrantLock lock = locks.get(deviceID);
		if ( lock == null ) {
			throw new RuntimeException("No lock available for YasdiDevice " + deviceID);
		}
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("Releasing lock on YasdiDevice {}", deviceID);
			lock.unlock();
		} else if ( log.isDebugEnabled() ) {
			log.debug("YasdiDevice {} not locked, nothing to release", deviceID);
		}
	}

}
