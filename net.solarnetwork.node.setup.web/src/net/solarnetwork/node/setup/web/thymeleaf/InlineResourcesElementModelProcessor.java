/* ==================================================================
 * ResourcesElementTagProcessor.java - 17/06/2025 5:57:18 am
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.node.setup.SetupResourceProvider.WEB_CONSUMER_TYPE;
import static net.solarnetwork.node.setup.web.thymeleaf.ThymeleafUtils.DEFAULT_PROCESSOR_PRECEDENCE;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.context.ApplicationContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.spring6.context.SpringContextUtils;
import org.thymeleaf.templatemode.TemplateMode;
import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.node.setup.SetupResourceService;

/**
 * Element processor to generate HTML tags for supported {@link SetupResource}
 * values.
 *
 * <p>
 * When rendering inline content, all properties provided via the
 * {@code properties} attribute will be added as {@code data-} attributes to
 * whatever element name is configured via {@code wrapperElement} attribute).
 * Similarly, any {@code data-} attributes added directly to the tag will be
 * added as well, as will any {@code id} attribute.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class InlineResourcesElementModelProcessor extends AbstractElementModelProcessor {

	/** The element name. */
	public static final String ELEMENT_NAME = "inlineResources";

	/** The type attribute name. */
	public static final String TYPE_ATTRIBUTE_NAME = "type";

	/** The scope attribute name. */
	public static final String SCOPE_ATTRIBUTE_NAME = "scope";

	/** The provider attribute name. */
	public static final String PROVIDER_ATTRIBUTE_NAME = "provider";

	/** The ID attribute name. */
	public static final String ID_ATTRIBUTE_NAME = "id";

	/** The properties attribute name. */
	public static final String PROPERTIES_ATTRIBUTE_NAME = "properties";

	/** The wrapper element attribute name. */
	public static final String WRAPPER_ELEMENT_ATTRIBUTE_NAME = "wrapperElement";

	/** The wrapper class element attribute name. */
	public static final String WRAPPER_CLASS_ATTRIBUTE_NAME = "wrapperClass";

	/** The data element attribute prefix. */
	public static final String DATA_ATTRIBUTE_PREFIX = "data-";

	/**
	 * Constructor.
	 *
	 * @param dialectPrefix
	 *        the dialect prefix
	 */
	public InlineResourcesElementModelProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, dialectPrefix, ELEMENT_NAME, true, null, false,
				DEFAULT_PROCESSOR_PRECEDENCE);
	}

	@Override
	protected void doProcess(ITemplateContext context, IModel model,
			IElementModelStructureHandler structureHandler) {

		final ApplicationContext appCtx = SpringContextUtils.getApplicationContext(context);

		final SetupResourceService setupResourceService = appCtx.getBean(SetupResourceService.class);
		if ( setupResourceService == null ) {
			model.reset();
			return;
		}

		final IProcessableElementTag tag = (IProcessableElementTag) model.get(0);
		final Map<String, Object> attributes = ThymeleafUtils.dynamicAttributes(context, tag);

		final SetupResourceProvider provider = (attributes
				.get(PROVIDER_ATTRIBUTE_NAME) instanceof SetupResourceProvider p ? p : null);

		Collection<SetupResource> resources = (provider != null
				? provider.getSetupResourcesForConsumer(WEB_CONSUMER_TYPE, context.getLocale())
				: setupResourceService.getSetupResourcesForConsumer(WEB_CONSUMER_TYPE,
						context.getLocale()));

		model.reset();

		if ( resources == null || resources.isEmpty() ) {
			return;
		}

		final String type = attributes.get(TYPE_ATTRIBUTE_NAME) instanceof String s ? s : null;

		final IModelFactory modelFactory = context.getModelFactory();

		for ( SetupResource rsrc : resources ) {
			if ( !type.equals(rsrc.getContentType()) ) {
				continue;
			}
			if ( !ResourcesElementTagProcessor.hasRequiredyRole(context, rsrc) ) {
				continue;
			}
			if ( !ResourcesElementTagProcessor.hasRequiredScope(
					attributes.get(SCOPE_ATTRIBUTE_NAME) instanceof String s ? s : null, rsrc) ) {
				continue;
			}

			setupWrapper(attributes, modelFactory, model);

			String body = generateInlineResource(rsrc);
			IModel bodyModel = modelFactory.parse(context.getTemplateData(), body);
			if ( bodyModel != null ) {
				model.addModel(bodyModel);
			}
		}

		finishWrapper(attributes, modelFactory, model);
	}

	private void setupWrapper(Map<String, Object> attributes, IModelFactory modelFactory, IModel model) {
		if ( model.size() > 0 ) {
			return;
		}
		if ( attributes.get(WRAPPER_ELEMENT_ATTRIBUTE_NAME) instanceof String wrapperElementName ) {
			Map<String, String> wrapperAttributes = new HashMap<>(8);
			for ( Entry<String, Object> e : attributes.entrySet() ) {
				if ( PROPERTIES_ATTRIBUTE_NAME.equals(e.getKey())
						&& e.getValue() instanceof Map<?, ?> m ) {
					for ( Entry<?, ?> pe : m.entrySet() ) {
						wrapperAttributes.put(DATA_ATTRIBUTE_PREFIX + pe.getKey().toString(),
								pe.getValue().toString());
					}
				} else if ( ID_ATTRIBUTE_NAME.equals(e.getKey()) ) {
					wrapperAttributes.put(ID_ATTRIBUTE_NAME, e.getValue().toString());
				} else if ( e.getKey().startsWith(DATA_ATTRIBUTE_PREFIX) ) {
					wrapperAttributes.put(e.getKey(), e.getValue().toString());
				}
			}

			model.add(modelFactory.createOpenElementTag(wrapperElementName, wrapperAttributes,
					AttributeValueQuotes.DOUBLE, false));
		}
	}

	private void finishWrapper(Map<String, Object> attributes, IModelFactory modelFactory,
			IModel model) {
		if ( model.size() < 1 || !(attributes
				.get(WRAPPER_ELEMENT_ATTRIBUTE_NAME) instanceof String wrapperElementName) ) {
			return;
		}
		model.add(modelFactory.createCloseElementTag(wrapperElementName));
	}

	private String generateInlineResource(SetupResource rsrc) {
		try (Reader in = new InputStreamReader(rsrc.getInputStream(), UTF_8)) {
			StringBuilder tmp = new StringBuilder(4096);
			char[] buffer = new char[4096];
			int bytesRead = -1;
			while ( (bytesRead = in.read(buffer)) != -1 ) {
				tmp.append(buffer, 0, bytesRead);
			}
			return tmp.toString();
		} catch ( IOException e ) {
			throw new RuntimeException(
					"Error generating inline SetupResource [%s]: %s".formatted(rsrc, e.toString()));
		}
	}

}
