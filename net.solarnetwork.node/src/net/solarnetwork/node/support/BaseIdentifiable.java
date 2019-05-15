/* ==================================================================
 * BaseIdentifiable.java - 15/05/2019 3:42:21 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.support;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.Identifiable;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Basic implementation of {@link Identifiable} and
 * {@link net.solarnetwork.domain.Identifiable} combined.
 * 
 * <p>
 * This class is meant to be extended by more useful services.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.67
 */
public abstract class BaseIdentifiable implements Identifiable, net.solarnetwork.domain.Identifiable {

	private String uid;
	private String groupUid;
	private MessageSource messageSource;

	/**
	 * Get settings for the configurable properties of {@link BaseIdentifiable}.
	 * 
	 * @param prefix
	 *        an optional prefix to include in all setting keys
	 * @return the settings
	 */
	public static List<SettingSpecifier> baseIdentifiableSettings(String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<>(8);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "uid", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "groupUid", ""));
		return results;
	}

	@Override
	public String getUid() {
		return uid;
	}

	/**
	 * Set the UID.
	 * 
	 * @param uid
	 *        the UID to set
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public String getGroupUid() {
		return groupUid;
	}

	/**
	 * Set the group UID.
	 * 
	 * @param groupUid
	 *        the group UID to set
	 */
	public void setGroupUid(String groupUid) {
		this.groupUid = groupUid;
	}

	/**
	 * Alias for {@link #getUid()}.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public String getUID() {
		return getUid();
	}

	/**
	 * Set the UID.
	 * 
	 * <p>
	 * This is an alias for {@link #setUid(String)}.
	 * </p>
	 * 
	 * @param uid
	 *        the UID to set
	 */
	public void setUID(String uid) {
		setUid(uid);
	}

	/**
	 * Alias for {@link #getGroupUid()}.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public String getGroupUID() {
		return getGroupUid();
	}

	/**
	 * Set the group UID.
	 * 
	 * <p>
	 * This is an alias for {@link #setGroupUid(String)}.
	 * </p>
	 * 
	 * @param groupUid
	 *        the group UID to set
	 */
	public void setGroupUID(String groupUid) {
		setGroupUid(groupUid);
	}

	/**
	 * Get a message source, to use for localizing this service with.
	 * 
	 * @return a message source
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a message source, to use for localizing this service with.
	 * 
	 * @param messageSource
	 *        the message source to use
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}