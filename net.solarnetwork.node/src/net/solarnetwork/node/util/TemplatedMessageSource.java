/* ==================================================================
 * TemplatedMessageSource.java - Jul 22, 2013 3:27:31 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

/**
 * Delegating {@link MessageSource} that dynamically extracts a pre-configured
 * regular expression match from all message codes.
 * 
 * <p>
 * The inspiration for this class was to support messages for objects that might
 * be nested in other objects used in
 * {@link net.solarnetwork.node.settings.SettingSpecifierProvider}
 * implementations. When one provider proxies another, or uses nested bean
 * paths, this class can be used to dynamically re-map message codes. For
 * example a code <code>mapProperty['url']</code> could be re-mapped to
 * <code>url</code>.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>regex</dt>
 * <dd>The regular expression to match against message codes. The regular
 * expression must provide at least one capture group; all capture groups are
 * combined into the final message code.</dd>
 * 
 * <dt>delegate</dt>
 * <dd>The {@link MessageSource} to delegate to. If that object implements
 * {@link HierarchicalMessageSource} then those methods will be supported by
 * instances of this class as well.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class TemplatedMessageSource implements MessageSource, HierarchicalMessageSource {

	private String regex;
	private Pattern pat;
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
		if ( pat != null ) {
			Matcher m = pat.matcher(code);
			final int count = m.groupCount();
			if ( m.matches() && count > 0 ) {
				// remap using regex capture groups
				StringBuilder buf = new StringBuilder();
				for ( int i = 1; i <= count; i++ ) {
					buf.append(m.group(i));
				}
				code = buf.toString();
			}
		}
		return delegate.getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		if ( pat != null ) {
			Matcher m = pat.matcher(code);
			final int count = m.groupCount();
			if ( m.matches() && count > 0 ) {
				// remap using regex capture groups
				StringBuilder buf = new StringBuilder();
				for ( int i = 1; i <= count; i++ ) {
					buf.append(m.group(i));
				}
				code = buf.toString();
			}
		}
		return delegate.getMessage(code, args, locale);
	}

	@Override
	public String getMessage(final MessageSourceResolvable resolvable, Locale locale)
			throws NoSuchMessageException {
		final String[] codes = resolvable.getCodes();
		if ( pat != null ) {
			for ( int i = 0; i < codes.length; i++ ) {
				Matcher m = pat.matcher(codes[i]);
				final int count = m.groupCount();
				if ( m.matches() && count > 0 ) {
					// remap using regex capture group
					StringBuilder buf = new StringBuilder();
					for ( int j = 1; j <= count; j++ ) {
						buf.append(m.group(j));
					}
					codes[i] = buf.toString();
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

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
		if ( regex != null && regex.length() > 0 ) {
			pat = Pattern.compile(regex);
		} else {
			pat = null;
		}
	}

	public MessageSource getDelegate() {
		return delegate;
	}

	public void setDelegate(MessageSource delegate) {
		this.delegate = delegate;
	}

}
