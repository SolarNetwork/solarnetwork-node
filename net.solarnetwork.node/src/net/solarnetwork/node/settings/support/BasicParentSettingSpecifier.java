/* ==================================================================
 * BasicParentSettingSpecifier.java - Mar 12, 2012 10:00:55 AM
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

import java.util.Collections;
import java.util.List;

import net.solarnetwork.node.settings.ParentSettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifier;

/**
 * Basic implementation of {@link ParentSettingSpecifier}.
 * 
 * @author matt
 * @version $Revision$
 */
public class BasicParentSettingSpecifier extends BaseSettingSpecifier implements
		ParentSettingSpecifier {

	private List<SettingSpecifier> childSettings;

	@Override
	public List<SettingSpecifier> getChildSettings() {
		return this.childSettings;
	}

	public void setChildSettings(List<SettingSpecifier> childSettings) {
		this.childSettings = Collections.unmodifiableList(childSettings);
	}

}
