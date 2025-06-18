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
import static net.solarnetwork.node.setup.web.WebConstants.X_FORWARDED_PATH_MODEL_ATTR;
import static net.solarnetwork.node.setup.web.thymeleaf.ThymeleafUtils.DEFAULT_PROCESSOR_PRECEDENCE;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.util.UriUtils;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.extras.springsecurity6.auth.AuthUtils;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.spring6.context.SpringContextUtils;
import org.thymeleaf.templatemode.TemplateMode;
import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.node.setup.SetupResourceScope;
import net.solarnetwork.node.setup.SetupResourceService;
import net.solarnetwork.util.StringUtils;

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
public class ResourcesElementTagProcessor extends AbstractElementTagProcessor
		implements IElementTagProcessor {

	/** The element name. */
	public static final String ELEMENT_NAME = "resources";

	/** The type attribute name. */
	public static final String TYPE_ATTRIBUTE_NAME = "type";

	/** The role attribute name. */
	public static final String ROLE_ATTRIBUTE_NAME = "role";

	/** The scope attribute name. */
	public static final String SCOPE_ATTRIBUTE_NAME = "scope";

	/** The inline attribute name. */
	public static final String INLINE_ATTRIBUTE_NAME = "inline";

	/** The provider attribute name. */
	public static final String PROVIDER_ATTRIBUTE_NAME = "provider";

	/** The properties attribute name. */
	public static final String PROPERTIES_ATTRIBUTE_NAME = "properties";

	/** The wrapper element attribute name. */
	public static final String WRAPPER_ELEMENT_ATTRIBUTE_NAME = "wrapperElement";

	/** The wrapper class element attribute name. */
	public static final String WRAPPER_CLASS_ATTRIBUTE_NAME = "wrapperClass";

	/** The id element attribute name. */
	public static final String ID_ATTRIBUTE_NAME = "id";

	/** The data element attribute prefix. */
	public static final String DATA_ATTRIBUTE_PREFIX = "data-";

	/**
	 * Constructor.
	 *
	 * @param dialectPrefix
	 *        the dialect prefix
	 */
	public ResourcesElementTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, dialectPrefix, ELEMENT_NAME, true, null, false,
				DEFAULT_PROCESSOR_PRECEDENCE);
	}

	@Override
	protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {

		final ApplicationContext appCtx = SpringContextUtils.getApplicationContext(context);

		final SetupResourceService setupResourceService = appCtx.getBean(SetupResourceService.class);
		if ( setupResourceService == null ) {
			structureHandler.removeElement();
			return;
		}

		final SetupResourceProvider provider = setupResourceProvider(context, tag);

		Collection<SetupResource> resources = (provider != null
				? provider.getSetupResourcesForConsumer(WEB_CONSUMER_TYPE, context.getLocale())
				: setupResourceService.getSetupResourcesForConsumer(WEB_CONSUMER_TYPE,
						context.getLocale()));

		if ( resources == null || resources.isEmpty() ) {
			structureHandler.removeElement();
			return;
		}

		final String role = tag.getAttributeValue(ROLE_ATTRIBUTE_NAME);

		final String type = tag.getAttributeValue(TYPE_ATTRIBUTE_NAME);

		final boolean inline = StringUtils.parseBoolean(tag.getAttributeValue(INLINE_ATTRIBUTE_NAME));

		final String wrapperElementName = tag.getAttributeValue(WRAPPER_ELEMENT_ATTRIBUTE_NAME);

		final String baseUrl = (role == null ? "/rsrc/" : "/a/rsrc/");

		final String forwardedPath = context.containsVariable(X_FORWARDED_PATH_MODEL_ATTR)
				? context.getVariable(X_FORWARDED_PATH_MODEL_ATTR).toString()
				: null;

		final IModelFactory modelFactory = context.getModelFactory();

		if ( inline && wrapperElementName != null ) {
			Map<String, String> dynamicAttributes = dynamicAttributes(context, tag);
			String wrapperClass = tag.getAttributeValue(WRAPPER_CLASS_ATTRIBUTE_NAME);
			if ( wrapperClass != null ) {
				dynamicAttributes.put("class", wrapperClass);
			}
			structureHandler.removeElement();
			structureHandler
					.insertImmediatelyAfter(
							modelFactory
									.createModel(modelFactory.createOpenElementTag(wrapperElementName,
											dynamicAttributes, AttributeValueQuotes.DOUBLE, false)),
							false);
		}

		final IModel output = modelFactory.createModel();

		for ( SetupResource rsrc : resources ) {
			if ( !type.equals(rsrc.getContentType()) ) {
				continue;
			}
			if ( !hasRequiredyRole(context, rsrc) ) {
				continue;
			}
			if ( !hasRequiredScope(context, tag, rsrc) ) {
				continue;
			}
			if ( inline ) {
				String body = generateInlineResource(structureHandler, rsrc);
				if ( wrapperElementName == null ) {
					structureHandler.replaceWith(body, false);
				} else {
					structureHandler.insertImmediatelyAfter(modelFactory
							.createModel(modelFactory.createCloseElementTag(wrapperElementName)), false);
				}
				// only one inline resource allowed, so stop here
				return;
			}

			String url = context.buildLink(
					baseUrl + UriUtils.encodePathSegment(rsrc.getResourceUID(), StandardCharsets.UTF_8),
					null);

			if ( forwardedPath != null && forwardedPath.startsWith("/") ) {
				StringBuilder buf = new StringBuilder(forwardedPath);
				if ( !(forwardedPath.endsWith("/") || url.startsWith("/")) ) {
					buf.append('/');
				}
				buf.append(url);
				url = buf.toString();
			}

			if ( SetupResource.JAVASCRIPT_CONTENT_TYPE.equals(rsrc.getContentType()) ) {
				output.add(modelFactory.createOpenElementTag("script",
						Map.of("type", rsrc.getContentType(), "src", url), AttributeValueQuotes.DOUBLE,
						false));
				output.add(modelFactory.createCloseElementTag("script"));
			} else if ( SetupResource.CSS_CONTENT_TYPE.equals(rsrc.getContentType()) ) {
				output.add(modelFactory.createStandaloneElementTag("link",
						Map.of("type", "text/css", "rel", "stylesheet", "href", url),
						AttributeValueQuotes.DOUBLE, false, false));
			} else if ( rsrc.getContentType().startsWith("image/") ) {
				output.add(modelFactory.createStandaloneElementTag("img", Map.of("src", url),
						AttributeValueQuotes.DOUBLE, false, false));
			}
		}

		structureHandler.replaceWith(output, false);
	}

	private Map<String, String> dynamicAttributes(ITemplateContext context, IProcessableElementTag tag) {
		Map<String, String> result = new HashMap<>(8);
		if ( tag.hasAttribute(PROPERTIES_ATTRIBUTE_NAME) ) {
			AttributeName propAttrName = tag.getAttribute(PROPERTIES_ATTRIBUTE_NAME)
					.getAttributeDefinition().getAttributeName();
			Object propVal = ThymeleafUtils.evaulateAttributeExpression(context, tag, propAttrName,
					tag.getAttributeValue(propAttrName), false);
			if ( propVal instanceof Map<?, ?> m ) {
				for ( Entry<?, ?> e : m.entrySet() ) {
					result.put(e.getKey().toString(), e.getValue().toString());
				}
			}
		}
		if ( tag.hasAttribute(ID_ATTRIBUTE_NAME) ) {
			result.put(ID_ATTRIBUTE_NAME, tag.getAttributeValue(ID_ATTRIBUTE_NAME));
		}
		for ( IAttribute attr : tag.getAllAttributes() ) {
			if ( attr.getAttributeCompleteName().startsWith(DATA_ATTRIBUTE_PREFIX) ) {
				result.put(attr.getAttributeCompleteName(), attr.getValue());
			}
		}
		return result;
	}

	private SetupResourceProvider setupResourceProvider(ITemplateContext context,
			IProcessableElementTag tag) {
		if ( !tag.hasAttribute(PROVIDER_ATTRIBUTE_NAME) ) {
			return null;
		}
		String providerExpr = tag.getAttributeValue(PROVIDER_ATTRIBUTE_NAME);
		Object providerVal = ThymeleafUtils.evaulateAttributeExpression(context, tag,
				tag.getAttribute(PROVIDER_ATTRIBUTE_NAME).getAttributeDefinition().getAttributeName(),
				providerExpr, false);
		if ( providerVal instanceof SetupResourceProvider p ) {
			return p;
		}
		return null;
	}

	private String generateInlineResource(IElementTagStructureHandler structureHandler,
			SetupResource rsrc) {
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

	private boolean hasRequiredyRole(ITemplateContext context, SetupResource rsrc) {
		Set<String> roles = rsrc.getRequiredRoles();
		if ( roles == null || roles.isEmpty() ) {
			return true;
		}

		final Authentication authentication = AuthUtils.getAuthenticationObject(context);
		if ( authentication == null || authentication.getAuthorities() == null ) {
			return false;
		}

		for ( String role : roles ) {
			for ( var authority : authentication.getAuthorities() ) {
				if ( role.equalsIgnoreCase(authority.getAuthority())
						|| ("ROLE_" + role).equalsIgnoreCase(authority.getAuthority()) ) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasRequiredScope(ITemplateContext context, IProcessableElementTag tag,
			SetupResource rsrc) {
		final SetupResourceScope rsrcScope = rsrc.getScope();
		final String scope = tag.hasAttribute(SCOPE_ATTRIBUTE_NAME)
				? tag.getAttributeValue(SCOPE_ATTRIBUTE_NAME)
				: null;

		if ( (scope == null || scope.isBlank())
				&& (rsrcScope == null || rsrcScope.equals(SetupResourceScope.Default)) ) {
			return true;
		} else if ( scope != null && scope.equalsIgnoreCase(rsrcScope.toString()) ) {
			return true;
		}
		return false;
	}

}
