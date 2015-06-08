/* ==================================================================
 * CentralSystemServiceFactorySupport.java - 9/06/2015 11:05:18 am
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

package net.solarnetwork.node.ocpp.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.solarnetwork.node.ocpp.CentralSystemServiceFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.FilterableService;
import org.springframework.context.MessageSource;

/**
 * A base helper class for services that require use of
 * {@link CentralSystemServiceFactory}.
 * 
 * <p>
 * The {@link FilterableService} API can be used to allow dynamic runtime
 * resolution of which central service to use, if more than one are deployed.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class CentralSystemServiceFactorySupport implements SettingSpecifierProvider {

	private CentralSystemServiceFactory centralSystem;
	private MessageSource messageSource;

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(3);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(Locale.getDefault()), true));
		results.add(new BasicTextFieldSettingSpecifier("filterableCentralSystem.propertyFilters['UID']",
				"OCPP Central System"));
		return results;
	}

	/**
	 * Get a status message to display as a read-only setting.
	 * 
	 * @param locale
	 *        The desired locale of the message.
	 * @return The status message.
	 */
	protected abstract String getInfoMessage(Locale locale);

	/**
	 * Get the {@link CentralSystemServiceFactory} to use.
	 * 
	 * @return The configured central system.
	 */
	public final CentralSystemServiceFactory getCentralSystem() {
		return centralSystem;
	}

	/**
	 * Set the {@link CentralSystemServiceFactory} to use. If the provided
	 * object also implements {@link FilterableService} then it will be passed
	 * to {@link #setFilterableCentralSystem(FilterableService)} as well.
	 * 
	 * @param centralSystem
	 *        The central system to use.
	 */
	public final void setCentralSystem(CentralSystemServiceFactory centralSystem) {
		this.centralSystem = centralSystem;
	}

	/**
	 * Get the central system to use, as a {@link FilterableService}. If the
	 * configured central system does not also implement
	 * {@link FilterableService} this method returns <em>null</em>. This method
	 * is designed to assist with dynamic runtime configuration, where more than
	 * one {@link CentralSystemServiceFactory} may be available and a filter is
	 * needed to choose the appropriate one to use.
	 * 
	 * @return The central system, as a {@link FilterableService}.
	 */
	public final FilterableService getFilterableCentralSystem() {
		CentralSystemServiceFactory central = centralSystem;
		if ( central instanceof FilterableService ) {
			return (FilterableService) central;
		}
		return null;
	}

	@Override
	public final MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set the {@link MessageSource} to use for resolving settings messages.
	 * 
	 * @param messageSource
	 *        The message source to use.
	 */
	public final void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
