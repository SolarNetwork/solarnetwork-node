/* ==================================================================
 * BasicSettingSpecifierProviderFactory.java - Mar 23, 2012 8:56:33 AM
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

package net.solarnetwork.node.settings.support;

import net.solarnetwork.node.settings.SettingSpecifierProviderFactory;

import org.springframework.context.MessageSource;

/**
 * Basic implementation of {@link SettingSpecifierProviderFactory}.
 * 
 * @author matt
 * @version $Revision$
 */
public class BasicSettingSpecifierProviderFactory implements SettingSpecifierProviderFactory {

	private String factoryUID;
	private String displayName;
	private MessageSource messageSource;

	@Override
	public String getFactoryUID() {
		return factoryUID;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setFactoryUID(String factoryUID) {
		this.factoryUID = factoryUID;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
