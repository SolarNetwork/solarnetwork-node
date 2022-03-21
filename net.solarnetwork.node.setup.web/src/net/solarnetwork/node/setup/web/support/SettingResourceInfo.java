/* ==================================================================
 * SettingResourceInfo.java - 10/03/2022 2:20:55 PM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.support;

/**
 * Information about a setting resource.
 * 
 * @author matt
 * @version 1.0
 * @since 2.3
 */
public class SettingResourceInfo {

	private final String name;
	private final String handlerKey;
	private final String instanceKey;
	private final String key;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        the name
	 * @param handlerKey
	 *        the handler key (factory ID)
	 * @param instanceKey
	 *        the instance key
	 * @param key
	 *        the key
	 */
	public SettingResourceInfo(String name, String handlerKey, String instanceKey, String key) {
		super();
		this.name = name;
		this.handlerKey = handlerKey;
		this.instanceKey = instanceKey;
		this.key = key;
	}

	/**
	 * Get the display name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the handler key.
	 * 
	 * @return the handlerKey
	 */
	public String getHandlerKey() {
		return handlerKey;
	}

	/**
	 * Get the instance key.
	 * 
	 * @return the instanceKey
	 */
	public String getInstanceKey() {
		return instanceKey;
	}

	/**
	 * Get the setting resource key.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

}
