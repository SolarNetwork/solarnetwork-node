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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.MessageSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Simple implementation of {@link OBRRepository}.
 * 
 * @author matt
 * @version 2.1
 */
public class SimpleOBRRepository implements OBRRepository {

	/**
	 * The default OBR URL to use.
	 * 
	 * @since 1.1
	 */
	public static final String DEFAULT_URL = "https://data.solarnetwork.net/obr/solarnetwork/metadata.xml";

	private static URL defaultUrl() {
		try {
			return new URI(DEFAULT_URL).toURL();
		} catch ( MalformedURLException | URISyntaxException e ) {
			// no way, dude
			return null;
		}
	}

	private URL url = defaultUrl();
	private MessageSource messageSource;

	@Override
	public URL getURL() {
		return url;
	}

	public void setURL(URL url) {
		this.url = url;
	}

	@Override
	public String getSettingUid() {
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
		results.add(new BasicTextFieldSettingSpecifier("URL", DEFAULT_URL));
		return results;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
