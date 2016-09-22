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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.UriUtils;
import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.node.setup.SetupResourceService;

/**
 * Tag to generate HTML tags for supported {@link SetupResource} values.
 * 
 * When rendering inline content, if any properties are provided via
 * {@link #setProperties(Map)} then the content will be wrapped in a
 * {@code &lt;div&gt;} element (or whatever is configured via
 * {@link #setPropertiesWrapperElement(String)}) and all properties added as
 * {@code data-} attributes.
 * 
 * @author matt
 * @version 1.0
 */
public class SetupResourcesTag extends HtmlEscapingAwareTag {

	private static final long serialVersionUID = 6291858407097698593L;

	private SetupResourceService setupResourceService;

	private String role;
	private String type = SetupResource.JAVASCRIPT_CONTENT_TYPE;
	private SetupResourceProvider provider;
	private boolean inline = false;
	private Map<String, ?> properties;
	private String wrapperElement = "div";
	private String wrapperClass;

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
		if ( inline && properties != null && wrapperElement != null ) {
			tagWriter.startTag(wrapperElement);
			if ( wrapperClass != null ) {
				tagWriter.writeAttribute("class", htmlEscape(wrapperClass));
			}
			for ( Map.Entry<String, ?> me : properties.entrySet() ) {
				String value = (me.getValue() == null ? "" : me.getValue().toString());
				tagWriter.writeAttribute("data-" + htmlEscape(me.getKey()), htmlEscape(value));
			}
			tagWriter.forceBlock();
		}
		for ( SetupResource rsrc : resources ) {
			if ( !type.equals(rsrc.getContentType()) ) {
				continue;
			}
			if ( !hasRequiredyRole(rsrc) ) {
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
				} catch ( UnsupportedEncodingException e ) {
					// should not be here ever
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
		if ( inline && properties != null && wrapperElement != null ) {
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

	public void setSetupResourceService(SetupResourceService setupResourceService) {
		this.setupResourceService = setupResourceService;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setProvider(SetupResourceProvider provider) {
		this.provider = provider;
	}

	public void setInline(boolean inline) {
		this.inline = inline;
	}

	public void setProperties(Map<String, ?> properties) {
		this.properties = properties;
	}

	public void setWrapperElement(String propertiesWrapperElement) {
		this.wrapperElement = propertiesWrapperElement;
	}

	public void setWrapperClass(String wrapperClass) {
		this.wrapperClass = wrapperClass;
	}

}
