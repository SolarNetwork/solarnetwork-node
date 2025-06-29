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
import static org.springframework.web.util.UriUtils.encodePathSegment;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.extras.springsecurity6.auth.AuthUtils;
import org.thymeleaf.model.AttributeValueQuotes;
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

	/** The provider attribute name. */
	public static final String PROVIDER_ATTRIBUTE_NAME = "provider";

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

		final Map<String, Object> attributes = ThymeleafUtils.dynamicAttributes(context, tag);

		final SetupResourceProvider provider = (attributes
				.get(PROVIDER_ATTRIBUTE_NAME) instanceof SetupResourceProvider p ? p : null);

		Collection<SetupResource> resources = (provider != null
				? provider.getSetupResourcesForConsumer(WEB_CONSUMER_TYPE, context.getLocale())
				: setupResourceService.getSetupResourcesForConsumer(WEB_CONSUMER_TYPE,
						context.getLocale()));

		if ( resources == null || resources.isEmpty() ) {
			structureHandler.removeElement();
			return;
		}

		final String role = attributes.get(ROLE_ATTRIBUTE_NAME) instanceof String s ? s : null;

		final String type = attributes.get(TYPE_ATTRIBUTE_NAME) instanceof String s ? s : null;

		final String baseUrl = (role == null ? "/rsrc/" : "/a/rsrc/");

		final String forwardedPath = context.containsVariable(X_FORWARDED_PATH_MODEL_ATTR)
				? context.getVariable(X_FORWARDED_PATH_MODEL_ATTR).toString()
				: null;

		final IModelFactory modelFactory = context.getModelFactory();

		final IModel output = modelFactory.createModel();

		for ( SetupResource rsrc : resources ) {
			if ( !type.equals(rsrc.getContentType()) ) {
				continue;
			}
			if ( !hasRequiredyRole(context, rsrc) ) {
				continue;
			}
			if ( !hasRequiredScope(attributes.get(SCOPE_ATTRIBUTE_NAME) instanceof String s ? s : null,
					rsrc) ) {
				continue;
			}
			String url = context.buildLink(baseUrl + encodePathSegment(rsrc.getResourceUID(), UTF_8),
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

	/**
	 * Test if the active user has a role required by a setup resource.
	 *
	 * @param context
	 *        the context
	 * @param rsrc
	 *        the resource to test
	 * @return {@code true} if {@code rsrc} requires a role that the active user
	 *         is authorized for
	 */
	public static boolean hasRequiredyRole(ITemplateContext context, SetupResource rsrc) {
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

	/**
	 * Test if a setup resource matches a required scope.
	 *
	 * @param scope
	 *        the required scope, or {@code null} for the default scope
	 * @param rsrc
	 *        the resource to test
	 * @return {@code true} if the resource matches the {@code scope}
	 */
	public static boolean hasRequiredScope(String scope, SetupResource rsrc) {
		final SetupResourceScope rsrcScope = rsrc.getScope();
		if ( (scope == null || scope.isBlank())
				&& (rsrcScope == null || rsrcScope.equals(SetupResourceScope.Default)) ) {
			return true;
		} else if ( scope != null && scope.equalsIgnoreCase(rsrcScope.toString()) ) {
			return true;
		}
		return false;
	}

}
