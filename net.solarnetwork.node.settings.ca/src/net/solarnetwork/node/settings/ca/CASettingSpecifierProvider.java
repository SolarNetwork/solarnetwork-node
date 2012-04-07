/* ==================================================================
 * CASettingSpecifierProvider.java - Mar 12, 2012 4:46:38 PM
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

package net.solarnetwork.node.settings.ca;

import java.util.List;

import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

import org.springframework.context.MessageSource;

/**
 * {@link SettingSpecifierProvider} with additional metadata to work with
 * ConfigurationAdmin.
 * 
 * @author matt
 * @version $Revision$
 */
public class CASettingSpecifierProvider implements SettingSpecifierProvider {

	private String pid;
	private SettingSpecifierProvider delegate;

	public CASettingSpecifierProvider(SettingSpecifierProvider delegate, String pid) {
		super();
		this.delegate = delegate;
		this.pid = pid;
	}

	@Override
	public MessageSource getMessageSource() {
		return delegate.getMessageSource();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return delegate.getSettingSpecifiers();
	}

	@Override
	public String getSettingUID() {
		return (pid != null ? pid : delegate.getSettingUID());
	}

	@Override
	public String getDisplayName() {
		return delegate.getDisplayName();
	}

	public String getPid() {
		return pid;
	}

	public SettingSpecifierProvider getDelegate() {
		return delegate;
	}

}
