
package net.solarnetwork.node.hw.linux.spi.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import net.solarnetwork.node.hw.linux.spi.jna.JnaSpiDevice;

/**
 * Manage bundle JNA resources.
 *
 * @author matt
 * @version 1.0
 */
public class Activator implements BundleActivator {

	/**
	 * Constructor.
	 */
	public Activator() {
		super();
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		// nadda
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		JnaSpiDevice.unregisterNativeMethods();
	}

}
