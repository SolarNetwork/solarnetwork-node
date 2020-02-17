/* ==================================================================
 * MessageSourceMessageComparator.java - 18/02/2020 8:45:12 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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
import org.springframework.context.MessageSource;

/**
 * Abstract {@link Comparator} for comparing two resolved messages.
 * 
 * @author matt
 * @version 1.0
 * @since 1.74
 */
public abstract class MessageSourceMessageComparator<T> implements Comparator<T> {

	/** The default {@code messageKey} property value. */
	public static final String DEFAULT_MESSAGE_KEY = "title";

	private final Locale locale;
	private final String messageKey;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This defaults the {@code messageKey} to {@link #DEFAULT_MESSAGE_KEY}.
	 * </p>
	 * 
	 * @param locale
	 *        the desired locale of the messages to compare
	 */
	public MessageSourceMessageComparator(Locale locale) {
		this(locale, DEFAULT_MESSAGE_KEY);
	}

	/**
	 * Constructor.
	 * 
	 * @param locale
	 *        the desired locale of the messages to compare
	 * @param messageKey
	 *        the message key to compare
	 */
	public MessageSourceMessageComparator(Locale locale, String messageKey) {
		super();
		this.locale = locale;
		this.messageKey = messageKey;
	}

	/**
	 * Compare two resolved message keys in a case-insensitive manner.
	 * 
	 * @param leftMessageSource
	 *        the first message source, or {@literal null}
	 * @param leftDefault
	 *        the first default message, if {@code leftMessageSource} is not
	 *        available or does not resolve a value for {@link #getMessageKey()}
	 * @param rightMessageSource
	 *        the second message source, or {@literal null}
	 * @param rightDefault
	 *        the second default message, if {@code rightMessageSource} is not
	 *        available or does not resolve a value for {@link #getMessageKey()}
	 * @return a negative integer, zero, or a positive integer as the first
	 *         argument is less than, equal to, or greater than the second
	 */
	public int compareMessageKeyValues(MessageSource leftMessageSource, String leftDefault,
			MessageSource rightMessageSource, String rightDefault) {
		String leftTitle = leftDefault;
		String rightTitle = rightDefault;
		if ( leftMessageSource != null ) {
			leftTitle = leftMessageSource.getMessage(messageKey, null, leftTitle, locale);
		}
		if ( rightMessageSource != null ) {
			rightTitle = rightMessageSource.getMessage(messageKey, null, rightTitle, locale);
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

	/**
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * @return the messageKey
	 */
	public String getMessageKey() {
		return messageKey;
	}

}
