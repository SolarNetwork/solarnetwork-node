/* ==================================================================
 * ChargeSessionManager_v15Settings.java - 18/06/2015 10:57:37 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.charge;

import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.ocpp.AuthorizationManager;
import net.solarnetwork.util.FilterableService;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * API to expose the configurable bean properties of
 * {@link ChargeSessionManager_v15}, so that Spring's AOP transaction proxy
 * picks up these methods to allow Configuration Admin to call them.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargeSessionManager_v15Settings {

	/**
	 * Initialize the OCPP client. Call this once after all properties
	 * configured.
	 */
	public void startup();

	/**
	 * Shutdown the OCPP client, releasing any associated resources.
	 */
	public void shutdown();

	/**
	 * Get a configurable collection of data sources for meter readings.
	 * 
	 * @return The configurable collection of data sources.
	 */
	public OptionalServiceCollection<DatumDataSource<ACEnergyDatum>> getMeterDataSource();

	/**
	 * Get a {@link FilterableService} for the {@link AuthorizationManager}.
	 * 
	 * @return A version of the AuthorizationManager that also implements
	 *         {@link FilterableService}.
	 */
	public FilterableService getFilterableAuthManager();

	/**
	 * Set a {@code socketConnectorMapping} Map via an encoded String value.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 * 
	 * @param mapping
	 *        The encoding mapping to set.
	 * @see #getSocketConnectorMappingValue()
	 * @see #setSocketConnectorMapping(Map)
	 */
	void setSocketConnectorMappingValue(String mapping);

	/**
	 * Set a {@code socketMeterSourceMapping} Map via an encoded String value.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 * 
	 * @param mapping
	 *        The encoding mapping to set.
	 * @see #getSocketMeterSourceMappingValue()
	 * @see #setSocketMeterSourceMapping(Map)
	 */
	void setSocketMeterSourceMappingValue(String mapping);

}
