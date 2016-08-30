/* ==================================================================
 * PrefixedMessageSource.java - Mar 25, 2012 3:27:31 PM
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
 */

package net.solarnetwork.node.util;

import java.util.Locale;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

/**
 * Delegating {@link MessageSource} that dynamically removes a pre-configured
 * prefix from all message codes.
 * 
 * <p>
 * The inspiration for this class was to support messages for objects that might
 * be nested in other objects used in
 * {@link net.solarnetwork.node.settings.SettingSpecifierProvider}
 * implementations. When one provider proxies another, or uses nested bean
 * paths, this class can be used to dynamically re-map message codes. For
 * example a code <code>delegate.url</code> could be re-mapped to
 * <code>url</code>.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>prefix</dt>
 * <dd>The message code prefix to dynamically remove from all message
 * codes.</dd>
 * 
 * <dt>delegate</dt>
 * <dd>The {@link MessageSource} to delegate to. If that object implements
 * {@link HierarchicalMessageSource} then those methods will be supported by
 * instances of this class as well.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 */
public class PrefixedMessageSource implements MessageSource, HierarchicalMessageSource {

	private String prefix = "";
	private MessageSource delegate;

	@Override
	public void setParentMessageSource(MessageSource parent) {
		if ( delegate instanceof HierarchicalMessageSource ) {
			((HierarchicalMessageSource) delegate).setParentMessageSource(parent);
		} else {
			throw new UnsupportedOperationException(
					"Delegate does not implement HierarchicalMessageSource");
		}
	}

	@Override
	public MessageSource getParentMessageSource() {
		if ( delegate instanceof HierarchicalMessageSource ) {
			return ((HierarchicalMessageSource) delegate).getParentMessageSource();
		}
		throw new UnsupportedOperationException("Delegate does not implement HierarchicalMessageSource");
	}

	@Override
	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		if ( delegate == null ) {
			return null;
		}
		if ( prefix != null && prefix.length() > 0 && code.startsWith(prefix) ) {
			// remove prefix
			code = code.substring(prefix.length());
		}
		return delegate.getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		if ( delegate == null ) {
			return null;
		}
		if ( prefix != null && prefix.length() > 0 && code.startsWith(prefix) ) {
			// remove prefix
			code = code.substring(prefix.length());
		}
		return delegate.getMessage(code, args, locale);
	}

	@Override
	public String getMessage(final MessageSourceResolvable resolvable, Locale locale)
			throws NoSuchMessageException {
		if ( delegate == null ) {
			return null;
		}
		final String[] codes = resolvable.getCodes();
		if ( prefix != null && prefix.length() > 0 ) {
			for ( int i = 0; i < codes.length; i++ ) {
				if ( codes[i].startsWith(prefix) ) {
					codes[i] = codes[i].substring(prefix.length());
				}
			}
		}
		return delegate.getMessage(new MessageSourceResolvable() {

			@Override
			public String getDefaultMessage() {
				return resolvable.getDefaultMessage();
			}

			@Override
			public String[] getCodes() {
				return codes;
			}

			@Override
			public Object[] getArguments() {
				return resolvable.getArguments();
			}
		}, locale);
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public MessageSource getDelegate() {
		return delegate;
	}

	public void setDelegate(MessageSource delegate) {
		this.delegate = delegate;
	}

}
