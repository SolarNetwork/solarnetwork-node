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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
 * @author matt
 * @version 1.3
 */
public class PrefixedMessageSource implements MessageSource, HierarchicalMessageSource {

	private String singlePrefix = "";
	private MessageSource singleDelegate;
	private Map<String, MessageSource> delegates = new LinkedHashMap<String, MessageSource>(2);

	private MessageSource parent;

	@Override
	public void setParentMessageSource(MessageSource parent) {
		this.parent = parent;
		setupParentMessageSource(parent);
	}

	private void setupParentMessageSource(MessageSource parent) {
		for ( MessageSource delegate : delegates.values() ) {
			if ( delegate instanceof HierarchicalMessageSource ) {
				((HierarchicalMessageSource) delegate).setParentMessageSource(parent);
			}
		}
	}

	@Override
	public MessageSource getParentMessageSource() {
		return parent;
	}

	@Override
	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		if ( delegates == null || delegates.isEmpty() ) {
			return defaultMessage;
		}
		for ( Map.Entry<String, MessageSource> me : delegates.entrySet() ) {
			String prefix = me.getKey();
			MessageSource delegate = me.getValue();
			if ( prefix != null && delegate != null
					&& (prefix.length() < 1 || code.startsWith(prefix)) ) {
				// remove prefix
				if ( prefix.length() > 0 ) {
					code = code.substring(prefix.length());
				}
				String result = delegate.getMessage(code, args, null, locale);
				if ( result != null ) {
					return result;
				}
			}
		}
		if ( singleDelegate != null ) {
			String result = singleDelegate.getMessage(code, args, null, locale);
			if ( result != null ) {
				return result;
			}
		}
		if ( parent != null ) {
			return parent.getMessage(code, args, defaultMessage, locale);
		}
		return defaultMessage;
	}

	@Override
	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		if ( delegates == null || delegates.isEmpty() ) {
			throw new NoSuchMessageException(code, locale);
		}
		for ( Map.Entry<String, MessageSource> me : delegates.entrySet() ) {
			String prefix = me.getKey();
			MessageSource delegate = me.getValue();
			if ( prefix != null && delegate != null
					&& (prefix.length() < 1 || code.startsWith(prefix)) ) {
				// remove prefix
				if ( prefix.length() > 0 ) {
					code = code.substring(prefix.length());
				}
				String result = delegate.getMessage(code, args, null, locale);
				if ( result != null ) {
					return result;
				}
			}
		}
		if ( singleDelegate != null ) {
			String result = singleDelegate.getMessage(code, args, null, locale);
			if ( result != null ) {
				return result;
			}
		}
		if ( parent != null ) {
			return parent.getMessage(code, args, locale);
		}
		throw new NoSuchMessageException(code, locale);
	}

	@Override
	public String getMessage(final MessageSourceResolvable resolvable, Locale locale)
			throws NoSuchMessageException {
		if ( delegates == null || delegates.isEmpty() ) {
			return null;
		}
		for ( Map.Entry<String, MessageSource> me : delegates.entrySet() ) {
			String prefix = me.getKey();
			MessageSource delegate = me.getValue();
			final String[] origCodes = resolvable.getCodes();
			final String[] codes = new String[origCodes != null ? origCodes.length : 0];
			for ( int i = 0; i < codes.length; i++ ) {
				String code = origCodes[i];
				if ( prefix != null && delegate != null
						&& (prefix.length() < 1 || code.startsWith(prefix)) ) {
					// remove prefix
					if ( prefix.length() > 0 ) {
						code = code.substring(prefix.length());
					}
					codes[i] = code;
				}
			}
			try {
				String result = delegate.getMessage(new MessageSourceResolvable() {

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
				if ( result != null ) {
					return result;
				}
			} catch ( NoSuchMessageException e ) {
				// skip
			}
		}
		return null;
	}

	/**
	 * Get the singular message code prefix to dynamically remove from all
	 * message codes.
	 * 
	 * @return the singular message code prefix
	 * @see #getDelegate()
	 */
	public String getPrefix() {
		return this.singlePrefix;
	}

	/**
	 * Set the singular message code prefix to dynamically remove from all
	 * message codes.
	 * 
	 * <p>
	 * This prefix will only be used with the singular delegate configured via
	 * {@link #setDelegate(MessageSource)}.
	 * </p>
	 * 
	 * @param prefix
	 *        the singular message code prefix
	 * @see #setDelegate(MessageSource)
	 */
	public void setPrefix(String prefix) {
		this.singlePrefix = prefix;
		delegates.put(prefix, this.singleDelegate);
	}

	/**
	 * Get the singular {@link MessageSource} to use with the singular prefix.
	 * 
	 * @return the singular message source delegate
	 */
	public MessageSource getDelegate() {
		return singleDelegate;
	}

	/**
	 * Set the singular {@link MessageSource} to use with the singular prefix.
	 * 
	 * <p>
	 * Note when this method is used, then messages found in this delegate under
	 * un-prefixed keys will be returned without consulting any parent source
	 * first.
	 * </p>
	 * 
	 * @param delegate
	 *        the singular delegate to use
	 */
	public void setDelegate(MessageSource delegate) {
		this.singleDelegate = delegate;
		delegates.put(this.singlePrefix, delegate);
		if ( this.parent != null ) {
			setupParentMessageSource(this.parent);
		}
	}

	/**
	 * Get the multi-prefix delegate mapping.
	 * 
	 * @return a mapping of message code prefixes to associated
	 *         {@link MessageSource} delegates
	 * @since 1.2
	 */
	public Map<String, MessageSource> getDelegates() {
		return delegates;
	}

	/**
	 * Set the multi-prefix delegate mapping.
	 * 
	 * <p>
	 * This configures any number of {@link MessageSource} delegates to handle
	 * specific message codes with associated prefix values.
	 * </p>
	 * 
	 * @param delegates
	 *        the message code prefix mapping
	 * @since 1.2
	 */
	public void setDelegates(Map<String, MessageSource> delegates) {
		this.delegates = new LinkedHashMap<String, MessageSource>(delegates);
		if ( this.singleDelegate != null ) {
			this.delegates.put(this.singlePrefix, this.singleDelegate);
		}
		if ( this.parent != null ) {
			setupParentMessageSource(this.parent);
		}
	}

}
