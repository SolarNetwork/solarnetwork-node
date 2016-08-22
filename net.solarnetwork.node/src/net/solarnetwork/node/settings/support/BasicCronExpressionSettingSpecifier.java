/* ==================================================================
 * BasicCronExpressionSettingSpecifier.java - 22/08/2016 1:21:02 PM
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

package net.solarnetwork.node.settings.support;

import java.io.IOException;
import java.util.Properties;
import net.solarnetwork.node.settings.CronExpressionSettingSpecifier;

/**
 * Basic implementation of {@link CronExpressionSettingSpecifier}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicCronExpressionSettingSpecifier extends BasicTextFieldSettingSpecifier
		implements CronExpressionSettingSpecifier {

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param defaultValue
	 *        the default value
	 */
	public BasicCronExpressionSettingSpecifier(String key, String defaultValue) {
		super(key, defaultValue);
		setDescriptionArguments(new Object[] { getCronSyntaxHelpLink() });
	}

	/**
	 * Get a URL to link to for help on cron expression syntax.
	 * 
	 * @return A URL to link to.
	 */
	public static final String getCronSyntaxHelpLink() {
		String result = "https://github.com/SolarNetwork/solarnetwork/wiki/SolarNode-Cron-Job-Syntax";
		Properties props = new Properties();
		try {
			props.load(net.solarnetwork.node.settings.support.BasicCronExpressionSettingSpecifier.class
					.getResourceAsStream("BasicCronExpressionSettingSpecifier.properties"));
			if ( props.containsKey("help.url") ) {
				result = props.getProperty("help.url");
			}
		} catch ( IOException e ) {
			// ignore this
		}
		return result;
	}

}
