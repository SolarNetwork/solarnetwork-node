
package net.solarnetwork.node.dao.jdbc.con;

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
 * @version 1.0
 */
public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		ServiceReference<ConfigurationAdmin> caRef = bundleContext
				.getServiceReference(ConfigurationAdmin.class);
		if ( caRef != null ) {
			ConfigurationAdmin ca = bundleContext.getService(caRef);
			if ( ca != null ) {
				DefaultDataSourceConfigurer configurer = new DefaultDataSourceConfigurer(ca);
				configurer.init();
			}
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		// nothing to do
	}

}
