/* ==================================================================
 * ManagedTriggerAndJobDetail.java - Jul 21, 2013 1:08:03 PM
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

package net.solarnetwork.node.job;

import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * Extension of {@link SimpleTriggerAndJobDetail} that supports a
 * {@link SettingSpecifierProvider} to manage the job at runtime.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl>
 * <dt>settingSpecifierProvider</dt>
 * <dd>The {@link SettingSpecifierProvider} that this class proxies all methods
 * for.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class ManagedTriggerAndJobDetail extends SimpleTriggerAndJobDetail implements
		SettingSpecifierProvider {

	private SettingSpecifierProvider settingSpecifierProvider;

	@Override
	public String getSettingUID() {
		return settingSpecifierProvider.getSettingUID();
	}

	@Override
	public String getDisplayName() {
		return settingSpecifierProvider.getDisplayName();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return settingSpecifierProvider.getSettingSpecifiers();
	}

	public SettingSpecifierProvider getSettingSpecifierProvider() {
		return settingSpecifierProvider;
	}

	public void setSettingSpecifierProvider(SettingSpecifierProvider settingSpecifierProvider) {
		this.settingSpecifierProvider = settingSpecifierProvider;
	}

}
