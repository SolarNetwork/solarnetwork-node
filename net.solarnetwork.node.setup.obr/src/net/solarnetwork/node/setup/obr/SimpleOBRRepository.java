/* ==================================================================
 * SimpleOBRRepository.java - Apr 21, 2014 2:49:06 PM
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import org.springframework.context.MessageSource;

/**
 * Simple implementation of {@link OBRRepository}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleOBRRepository implements OBRRepository {

	private URL url;
	private MessageSource messageSource;

	@Override
	public URL getURL() {
		return url;
	}

	public void setURL(URL url) {
		this.url = url;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.setup.obr.repo";
	}

	@Override
	public String getDisplayName() {
		return "OBR Plugin Repository";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(1);
		results.add(new BasicTextFieldSettingSpecifier("url", ""));
		return results;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
