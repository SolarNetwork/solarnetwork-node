/* ==================================================================
 * ManagedHMACHandler.java - 4/08/2016 7:37:14 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.ocpp.web;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import ocpp.v15.support.HMACHandler;

/**
 * Extension of {@link HMACHandler} to support {@link SettingSpecifierProvider}.
 * 
 * @author matt
 * @version 1.0
 */
public class ManagedHMACHandler extends HMACHandler implements SettingSpecifierProvider {

	private MessageSource messageSource;

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.ocpp.web";
	}

	@Override
	public String getDisplayName() {
		return getClass().getName();
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ManagedHMACHandler defaults = new ManagedHMACHandler();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);
		results.add(new BasicTextFieldSettingSpecifier("hmacHandler.secret", DEFAULT_SECRET));
		results.add(new BasicTextFieldSettingSpecifier("hmacHandler.maximumTimeSkew",
				String.valueOf(defaults.getMaximumTimeSkew())));
		results.add(new BasicToggleSettingSpecifier("hmacHandler.required", defaults.isRequired()));
		return results;
	}

	/**
	 * Getter to support setting keys prefixed by {@code hmacHandler.}.
	 * 
	 * @return This instance.
	 */
	public HMACHandler getHmacHandler() {
		return this;
	}

	/**
	 * Set the {@link MessageSource} to use with settings.
	 * 
	 * @param messageSource
	 *        The message source.
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
