/* ==================================================================
 * OBRPluginService.java - Apr 21, 2014 2:36:06 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.obr;

import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.setup.Plugin;
import net.solarnetwork.node.setup.PluginService;
import net.solarnetwork.util.OptionalService;
import org.osgi.service.obr.RepositoryAdmin;
import org.springframework.context.MessageSource;

/**
 * OBR implementation of {@link PluginService}.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class OBRPluginService implements PluginService, SettingSpecifierProvider {

	private OptionalService<RepositoryAdmin> repositoryAdmin;
	private MessageSource messageSource;

	@Override
	public List<Plugin> availablePlugins() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.setup.obr";
	}

	@Override
	public String getDisplayName() {
		return "OBR Plugin Service";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		// TODO Auto-generated method stub
		return null;
	}

}
