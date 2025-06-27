/* ==================================================================
 * MessageElementTagProcessor.java - 18/06/2025 8:00:24 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.thymeleaf;

import static net.solarnetwork.node.setup.web.thymeleaf.ThymeleafUtils.DEFAULT_PROCESSOR_PRECEDENCE;
import static net.solarnetwork.node.setup.web.thymeleaf.ThymeleafUtils.evaulateIntegerAttributeExpression;
import static net.solarnetwork.node.setup.web.thymeleaf.ThymeleafUtils.evaulateStringAttributeExpression;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Render a setting message.
 *
 * @author matt
 * @version 1.0
 */
public class MessageElementTagProcessor extends AbstractElementTagProcessor
		implements IElementTagProcessor {

	/** The element name. */
	public static final String ELEMENT_NAME = "message";

	/** The key attribute name. */
	public static final String KEY_ATTRIBUTE_NAME = "key";

	/** The message source attribute name. */
	public static final String MESSAGE_SOURCE_ATTRIBUTE_NAME = "messageSource";

	/** The text attribute name. */
	public static final String TEXT_ATTRIBUTE_NAME = "text";

	/** The index attribute name. */
	public static final String INDEX_ATTRIBUTE_NAME = "index";

	/** The arguments attribute name. */
	public static final String ARGUMENTS_ATTRIBUTE_NAME = "arguments";

	private static final Pattern INDEX_KEYS_PATTERN = Pattern.compile("\\[\\d+\\]");

	/**
	 * Constructor.
	 *
	 * @param dialectPrefix
	 *        the dialect prefix
	 */
	public MessageElementTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, dialectPrefix, ELEMENT_NAME, true, null, false,
				DEFAULT_PROCESSOR_PRECEDENCE);
	}

	@Override
	protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		final MessageSource messageSource = messageSource(context, tag);
		final String key = evaulateStringAttributeExpression(context, tag, KEY_ATTRIBUTE_NAME, false);
		if ( messageSource == null || key == null || key.isBlank() ) {
			structureHandler.removeElement();
			return;
		}

		final Integer index = evaulateIntegerAttributeExpression(context, tag, INDEX_ATTRIBUTE_NAME,
				false);
		final Object[] arguments = arguments(context, tag);

		String msg = null;

		final Locale locale = context.getLocale();
		try {
			msg = messageSource.getMessage(key, arguments, locale);
		} catch ( NoSuchMessageException e ) {
			// try with index subscripts removed
			String keyNoIndcies = INDEX_KEYS_PATTERN.matcher(key).replaceAll("Item");
			if ( !keyNoIndcies.equals(key) ) {
				try {
					Object[] params = arguments;
					if ( index != null ) {
						params = new Object[1 + (arguments == null ? 0 : arguments.length)];
						params[0] = index;
						if ( arguments != null ) {
							System.arraycopy(arguments, 0, params, 1, arguments.length);
						}
					}
					msg = messageSource.getMessage(keyNoIndcies, params, locale);
				} catch ( NoSuchMessageException e2 ) {
					// give up
				}
			}
		}

		if ( msg == null ) {
			final String text = evaulateStringAttributeExpression(context, tag, TEXT_ATTRIBUTE_NAME,
					false);
			if ( text != null ) {
				msg = text;
			} else {
				msg = "???" + key + "???";
			}
		}
		if ( msg != null && msg.length() > 0 ) {
			structureHandler.replaceWith(msg, false);
		} else {
			structureHandler.removeElement();
		}
	}

	private MessageSource messageSource(ITemplateContext context, IProcessableElementTag tag) {
		Object val = ThymeleafUtils.evaulateAttributeExpression(context, tag,
				MESSAGE_SOURCE_ATTRIBUTE_NAME, false);
		if ( val instanceof MessageSource ms ) {
			return ms;
		}
		return null;
	}

	private Object[] arguments(ITemplateContext context, IProcessableElementTag tag) {
		Object val = ThymeleafUtils.evaulateAttributeExpression(context, tag, ARGUMENTS_ATTRIBUTE_NAME,
				false);
		if ( val != null && val.getClass().isArray() ) {
			return (Object[]) val;
		}
		return null;
	}

}
