/* ==================================================================
 * MessageTag.java - Mar 12, 2012 7:34:10 PM
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

package net.solarnetwork.node.setup.web.support;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * JSP tag for resolving a message from an existing {@link MessageSource}.
 * 
 * <p>
 * TODO: support nested parameters.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>messageSource</dt>
 * <dd>The {@link MessageSource} to resolve messages from.</dd>
 * 
 * <dt>key</dt>
 * <dd>The message key to resolve.</dd>
 * 
 * <dt>text</dt>
 * <dd>An optional default value to use if message can't be resolved.</dd>
 * 
 * <dt>arguments</dt>
 * <dd>An optional array of objects to pass to the message when formatting.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.3
 */
public class MessageTag extends TagSupport {

	private static final long serialVersionUID = 5738525496721788477L;

	private MessageSource messageSource;
	private String key;
	private String text;
	private Integer index;
	private Object[] arguments;

	private static final Pattern IndexKeysPattern = Pattern.compile("\\[\\d+\\]");

	@Override
	public int doEndTag() throws JspException {
		String msg = null;
		if ( messageSource != null && key != null ) {
			Locale locale = this.pageContext.getRequest().getLocale();
			try {
				msg = this.messageSource.getMessage(this.key, arguments, locale);
			} catch ( NoSuchMessageException e ) {
				// try with index subscripts removed
				String keyNoIndcies = IndexKeysPattern.matcher(this.key).replaceAll("Item");
				if ( !keyNoIndcies.equals(this.key) ) {
					try {
						Object[] params = arguments;
						if ( index != null ) {
							params = new Object[1 + (arguments == null ? 0 : arguments.length)];
							params[0] = index;
							if ( arguments != null ) {
								System.arraycopy(arguments, 0, params, 1, arguments.length);
							}
						}
						msg = this.messageSource.getMessage(keyNoIndcies, params, locale);
					} catch ( NoSuchMessageException e2 ) {
						// give up
					}
				}
			}
		}
		if ( msg == null ) {
			if ( text != null ) {
				msg = text;
			} else {
				msg = "???" + this.key + "???";
			}
		}
		if ( msg != null && msg.length() > 0 ) {
			try {
				pageContext.getOut().write(msg);
			} catch ( IOException e ) {
				throw new JspException(e);
			}
		}
		return EVAL_PAGE;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}

}
