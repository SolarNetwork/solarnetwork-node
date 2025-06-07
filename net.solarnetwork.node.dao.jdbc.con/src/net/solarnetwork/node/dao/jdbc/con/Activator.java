
package net.solarnetwork.node.dao.jdbc.con;

import java.util.concurrent.TimeUnit;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Bundle activator.
 *
 * <p>
 * This activator will look for a {@link ConfigurationAdmin} service, and if
 * found initialize a {@link DefaultDataSourceConfigurer} to set up a default
 * data source configuration if one does not already exist.
 * </p>
 *
 * @author matt
 * @version 1.2
 */
public class Activator implements BundleActivator {

	/**
	 * A system property key for the startup sleep time, in milliseconds.
	 *
	 * @since 1.1
	 */
	private static final String SYS_PROP_STARTUP_SLEEP = "net.solarnetwork.node.dao.jdbc.con.startupSleepMs";

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		final long sleepTime = sleepTime();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(sleepTime);
				} catch ( InterruptedException e ) {
					// ignore and continue
				} finally {
					ServiceReference<ConfigurationAdmin> caRef = bundleContext
							.getServiceReference(ConfigurationAdmin.class);
					if ( caRef != null ) {
						ConfigurationAdmin ca = bundleContext.getService(caRef);
						if ( ca != null ) {
							DefaultDataSourceConfigurer configurer = new DefaultDataSourceConfigurer(
									bundleContext, ca);
							configurer.init();
						}
					}

				}

			}
		}).start();
	}

	private long sleepTime() {
		long sleepTime = TimeUnit.SECONDS.toMillis(15);
		String propSleep = System.getProperty(SYS_PROP_STARTUP_SLEEP);
		if ( propSleep != null && !propSleep.isEmpty() ) {
			try {
				sleepTime = Long.parseLong(propSleep);
			} catch ( Exception e ) {
				// ignore
			}
		}
		return sleepTime;
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		// nothing to do
	}

}
