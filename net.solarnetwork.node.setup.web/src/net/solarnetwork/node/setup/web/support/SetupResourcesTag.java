/* ==================================================================
 * SetupResourcesTag.java - 21/09/2016 10:44:07 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.UriUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.DynamicAttributes;
import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.node.setup.SetupResourceScope;
import net.solarnetwork.node.setup.SetupResourceService;

/**
 * Tag to generate HTML tags for supported {@link SetupResource} values.
 * 
 * When rendering inline content, all properties provided via
 * {@link #setProperties(Map)} will be added as {@code data-} attributes to
 * whatever element name is configured via {@link #setWrapperElement(String)}).
 * Similarly, any {@code data-} attributes added directly to the tag will be
 * added as well, as will any {@code id} attribute.
 * 
 * @author matt
 * @version 1.3
 */
public class SetupResourcesTag extends HtmlEscapingAwareTag implements DynamicAttributes {

	private static final long serialVersionUID = 5353893185255879473L;

	/** The setup resource service. */
	private SetupResourceService setupResourceService;

	/** The role. */
	private String role;

	/** The type. */
	private String type = SetupResource.JAVASCRIPT_CONTENT_TYPE;

	/** The provider. */
	private SetupResourceProvider provider;

	/** The scope. */
	private SetupResourceScope scope;

	/** The inline mode. */
	private boolean inline = false;

	/** The properties. */
	private Map<String, ?> properties;

	/** The wrapper HTML element name. */
	private String wrapperElement;

	/** The wrapper CSS class. */
	private String wrapperClass;

	/** The dynamic attributes. */
	private Map<String, Object> dynamicAttributes;

	/**
	 * Default constructor.
	 */
	public SetupResourcesTag() {
		super();
	}

	@Override
	public void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
		// we accept all data- attributes and id
		if ( !(localName.startsWith("data-") || localName.equals("id")) ) {
			throw new JspException("Unsupported attribute [" + uri + ":" + localName + "]");
		}
		if ( dynamicAttributes == null ) {
			dynamicAttributes = new LinkedHashMap<String, Object>();
		}
		dynamicAttributes.put(localName, value);
	}

	@Override
	protected int doStartTagInternal() throws Exception {
		writeContent(new TagWriter(this.pageContext));
		return SKIP_BODY;
	}

	private void writeContent(TagWriter tagWriter) throws JspException {
		if ( setupResourceService == null && provider == null ) {
			setupResourceService = getRequestContext().getWebApplicationContext()
					.getBean(SetupResourceService.class);
			if ( setupResourceService == null ) {
				return;
			}
		}
		Collection<SetupResource> resources = (provider != null
				? provider.getSetupResourcesForConsumer(SetupResourceProvider.WEB_CONSUMER_TYPE,
						pageContext.getRequest().getLocale())
				: setupResourceService.getSetupResourcesForConsumer(
						SetupResourceProvider.WEB_CONSUMER_TYPE, pageContext.getRequest().getLocale()));
		if ( resources == null || resources.isEmpty() ) {
			return;
		}

		String baseUrl = (role == null ? "/rsrc/" : "/a/rsrc/");
		if ( inline && wrapperElement != null ) {
			tagWriter.startTag(wrapperElement);
			if ( wrapperClass != null ) {
				tagWriter.writeAttribute("class", htmlEscape(wrapperClass));
			}
			if ( properties != null ) {
				for ( Map.Entry<String, ?> me : properties.entrySet() ) {
					String value = (me.getValue() == null ? "" : me.getValue().toString());
					tagWriter.writeAttribute("data-" + htmlEscape(me.getKey()), htmlEscape(value));
				}
			}
			if ( dynamicAttributes != null ) {
				for ( Map.Entry<String, ?> me : dynamicAttributes.entrySet() ) {
					if ( properties != null && me.getKey().startsWith("data-")
							&& properties.containsKey(me.getKey().substring(5)) ) {
						continue;
					}
					String value = (me.getValue() == null ? "" : me.getValue().toString());
					tagWriter.writeAttribute(htmlEscape(me.getKey()), htmlEscape(value));
				}
			}

			tagWriter.forceBlock();
		}

		HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		String forwardedPath = request.getHeader("X-Forwarded-Path");

		for ( SetupResource rsrc : resources ) {
			if ( !type.equals(rsrc.getContentType()) ) {
				continue;
			}
			if ( !hasRequiredyRole(rsrc) ) {
				continue;
			}
			if ( !hasRequiredScope(rsrc) ) {
				continue;
			}
			if ( inline ) {
				try {
					writeInlineResource(rsrc);
				} catch ( IOException e ) {
					throw new JspException(e);
				}
			} else {
				String url = "";
				try {
					url = getRequestContext().getContextUrl(
							baseUrl + UriUtils.encodePathSegment(rsrc.getResourceUID(), "UTF-8"));
				} catch ( IllegalArgumentException e ) {
					// should not be here ever
				}

				if ( forwardedPath != null && forwardedPath.startsWith("/") ) {
					StringBuilder buf = new StringBuilder(forwardedPath);
					if ( !(forwardedPath.endsWith("/") || url.startsWith("/")) ) {
						buf.append('/');
					}
					buf.append(url);
					url = buf.toString();
				}

				if ( SetupResource.JAVASCRIPT_CONTENT_TYPE.equals(rsrc.getContentType()) ) {
					tagWriter.startTag("script");
					tagWriter.writeAttribute("type", rsrc.getContentType());
					tagWriter.writeAttribute("src", url);
					tagWriter.endTag(true);
				} else if ( SetupResource.CSS_CONTENT_TYPE.equals(rsrc.getContentType()) ) {
					tagWriter.startTag("link");
					tagWriter.writeAttribute("rel", "stylesheet");
					tagWriter.writeAttribute("type", "text/css");
					tagWriter.writeAttribute("href", url);
				} else if ( rsrc.getContentType().startsWith("image/") ) {
					tagWriter.startTag("img");
					tagWriter.writeAttribute("src", url);
				}
			}
		}
		if ( inline && wrapperElement != null ) {
			tagWriter.endTag(true);
		}
	}

	private void writeInlineResource(SetupResource rsrc) throws IOException {
		Reader in = new InputStreamReader(rsrc.getInputStream(), "UTF-8");
		Writer out = this.pageContext.getOut();
		try {
			char[] buffer = new char[4096];
			int bytesRead = -1;
			while ( (bytesRead = in.read(buffer)) != -1 ) {
				out.write(buffer, 0, bytesRead);
			}
			out.flush();
		} finally {
			try {
				in.close();
			} catch ( IOException ex ) {
				// ignore this
			}
		}
	}

	private boolean hasRequiredyRole(SetupResource rsrc) {
		Set<String> roles = rsrc.getRequiredRoles();
		if ( roles == null || roles.isEmpty() ) {
			return true;
		}
		for ( String role : roles ) {
			if ( ((HttpServletRequest) this.pageContext.getRequest()).isUserInRole(role) ) {
				return true;
			}
		}
		return false;
	}

	private boolean hasRequiredScope(SetupResource rsrc) {
		SetupResourceScope rsrcScope = rsrc.getScope();
		if ( scope == null && (rsrcScope == null || rsrcScope.equals(SetupResourceScope.Default)) ) {
			return true;
		} else if ( scope != null && scope.equals(rsrcScope) ) {
			return true;
		}
		return false;
	}

	/**
	 * Set the setup resource service.
	 * 
	 * @param setupResourceService
	 *        the service to set
	 */
	public void setSetupResourceService(SetupResourceService setupResourceService) {
		this.setupResourceService = setupResourceService;
	}

	/**
	 * Set the role.
	 * 
	 * @param role
	 *        the role
	 */
	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * Set the type.
	 * 
	 * @param type
	 *        the type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Set the provider.
	 * 
	 * @param provider
	 *        the provider
	 */
	public void setProvider(SetupResourceProvider provider) {
		this.provider = provider;
	}

	/**
	 * Set the inline toggle.
	 * 
	 * @param inline
	 *        {@literal true} to inline
	 */
	public void setInline(boolean inline) {
		this.inline = inline;
	}

	/**
	 * Set the properties.
	 * 
	 * @param properties
	 *        the properties
	 */
	public void setProperties(Map<String, ?> properties) {
		this.properties = properties;
	}

	/**
	 * Set the wrapper element name.
	 * 
	 * @param propertiesWrapperElement
	 *        the wrapper element name
	 */
	public void setWrapperElement(String propertiesWrapperElement) {
		this.wrapperElement = propertiesWrapperElement;
	}

	/**
	 * Set the wrapper CSS class.
	 * 
	 * @param wrapperClass
	 *        the wrapper CSS class name
	 */
	public void setWrapperClass(String wrapperClass) {
		this.wrapperClass = wrapperClass;
	}

	/**
	 * Set a scope to restrict resolved resources to.
	 * 
	 * @param scope
	 *        the scope to set
	 * @since 1.1
	 */
	public void setScope(SetupResourceScope scope) {
		this.scope = scope;
	}

}
