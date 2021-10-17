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

import java.util.Locale;
import org.springframework.context.MessageSource;
import net.solarnetwork.settings.SettingSpecifierProviderFactory;

/**
 * Sort {@link SettingSpecifierProviderFactory} by their localized titles.
 * 
 * @author matt
 * @version 2.0
 * @since 1.61
 */
public class SettingSpecifierProviderFactoryMessageComparator
		extends MessageSourceMessageComparator<SettingSpecifierProviderFactory> {

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This defaults the {@code messageKey} to {link
	 * {@link MessageSourceMessageComparator#DEFAULT_MESSAGE_KEY}}.
	 * </p>
	 * 
	 * @param locale
	 *        the desired locale of the messages to compare
	 */
	public SettingSpecifierProviderFactoryMessageComparator(Locale locale) {
		super(locale);
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
		super(locale, messageKey);
	}

	@Override
	public int compare(SettingSpecifierProviderFactory left, SettingSpecifierProviderFactory right) {
		String leftDefault = null;
		MessageSource leftMessageSource = null;
		String rightDefault = null;
		MessageSource rightMessageSource = null;
		if ( left != null ) {
			leftDefault = left.getDisplayName();
			leftMessageSource = left.getMessageSource();
		}
		if ( right != null ) {
			rightDefault = right.getDisplayName();
			rightMessageSource = right.getMessageSource();
		}
		return compareMessageKeyValues(leftMessageSource, leftDefault, rightMessageSource, rightDefault);
	}

}
