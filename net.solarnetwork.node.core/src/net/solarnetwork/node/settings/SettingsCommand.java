/* ==================================================================
 * SettingsCommand.java - Mar 18, 2012 3:37:02 PM
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

import java.util.ArrayList;
import java.util.List;

/**
 * Edit settings command object.
 * 
 * @author matt
 * @version $Revision$
 */
public class SettingsCommand {

	private String providerKey;
	private String instanceKey;

	private List<SettingValueBean> values = new ArrayList<SettingValueBean>();

	public List<SettingValueBean> getValues() {
		return values;
	}

	public void setValues(List<SettingValueBean> values) {
		this.values = values;
	}

	public String getProviderKey() {
		return providerKey;
	}

	public void setProviderKey(String providerKey) {
		this.providerKey = providerKey;
	}

	public String getInstanceKey() {
		return instanceKey;
	}

	public void setInstanceKey(String instanceKey) {
		this.instanceKey = instanceKey;
	}

}
