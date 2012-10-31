/* ==================================================================
 * SettingSpecifierProvider.java - Mar 12, 2012 9:11:50 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.settings;

import java.util.List;

import org.springframework.context.MessageSource;

/**
 * API for a provider of {@link SettingSpecifier} instances, to 
 * publish application-managed settings.
 * 
 * @author matt
 * @version $Revision$
 */
public interface SettingSpecifierProvider {

	/**
	 * Get a unique, application-wide setting ID.
	 * 
	 * <p>
	 * This ID must be unique across all setting providers registered within the
	 * system.
	 * </p>
	 * 
	 * @return unique ID
	 */
	String getSettingUID();

	/**
	 * Get a non-localized display name.
	 * 
	 * @return non-localized display name
	 */
	String getDisplayName();

	/**
	 * Get a MessageSource to localize the setting text.
	 * 
	 * <p>
	 * This method can return <em>null</em> if the provider does not have any
	 * localized resources.
	 * </p>
	 * 
	 * @return the MessageSource, or <em>null</em>
	 */
	MessageSource getMessageSource();
	
	/**
	 * Get a list of {@link SettingSpecifier} instances.
	 * 
	 * @return list of {@link SettingSpecifier}
	 */
	List<SettingSpecifier> getSettingSpecifiers();

}
