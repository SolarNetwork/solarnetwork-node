/* ==================================================================
 * FactoryTitleComparator.java - 23/11/2018 11:07:38 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

import java.util.Comparator;
import java.util.Locale;
import net.solarnetwork.node.settings.SettingSpecifierProviderFactory;

/**
 * Sort {@link SettingSpecifierProviderFactory} by their localized titles.
 * 
 * @author matt
 * @version 1.0
 * @since 1.61
 */
public class SettingSpecifierProviderFactoryMessageComparator
		implements Comparator<SettingSpecifierProviderFactory> {

	private final Locale locale;
	private final String messageKey;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This defaults the {@code messageKey} to {@literal title}.
	 * </p>
	 * 
	 * @param locale
	 *        the desired locale of the messages to compare
	 */
	public SettingSpecifierProviderFactoryMessageComparator(Locale locale) {
		this(locale, "title");
	}

	/**
	 * Constructor.
	 * 
	 * @param locale
	 *        the desired locale of the messages to compare
	 * @param messageKey
	 *        the message key to compare
	 */
	public SettingSpecifierProviderFactoryMessageComparator(Locale locale, String messageKey) {
		super();
		this.locale = locale;
		this.messageKey = messageKey;
	}

	@Override
	public int compare(SettingSpecifierProviderFactory left, SettingSpecifierProviderFactory right) {
		String leftTitle = null;
		String rightTitle = null;
		if ( left != null ) {
			leftTitle = left.getDisplayName();
			if ( left.getMessageSource() != null ) {
				leftTitle = left.getMessageSource().getMessage(messageKey, null, leftTitle, locale);
			}
		}
		if ( right != null ) {
			rightTitle = right.getDisplayName();
			if ( right.getMessageSource() != null ) {
				rightTitle = right.getMessageSource().getMessage(messageKey, null, rightTitle, locale);
			}
		}
		if ( leftTitle == rightTitle ) {
			return 0;
		} else if ( leftTitle == null ) {
			return -1;
		} else if ( rightTitle == null ) {
			return 1;
		}
		return leftTitle.compareToIgnoreCase(rightTitle);
	}

}
