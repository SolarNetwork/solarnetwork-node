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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.tags.form.AbstractFormTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.UriUtils;
import net.solarnetwork.node.setup.SetupResource;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.node.setup.SetupResourceService;

/**
 * Tag to generate HTML tags for supported {@link SetupResource} values.
 * 
 * @author matt
 * @version 1.0
 */
public class SetupResourcesTag extends AbstractFormTag {

	private static final long serialVersionUID = -3795271055855930341L;

	@Autowired
	private SetupResourceService setupResourceService;

	private String role;
	private String type = SetupResource.JAVASCRIPT_CONTENT_TYPE;

	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		if ( setupResourceService == null ) {
			setupResourceService = getRequestContext().getWebApplicationContext()
					.getBean(SetupResourceService.class);
			if ( setupResourceService == null ) {
				return SKIP_BODY;
			}
		}
		List<SetupResource> resources = setupResourceService
				.getSetupResourcesForConsumer(SetupResourceProvider.WEB_CONSUMER_TYPE);
		if ( resources == null || resources.isEmpty() ) {
			return SKIP_BODY;
		}
		String baseUrl = (role == null ? "/rsrc/" : "/a/rsrc/");
		for ( SetupResource rsrc : resources ) {
			if ( !type.equals(rsrc.getContentType()) ) {
				continue;
			}
			if ( !hasRequiredyRole(rsrc) ) {
				continue;
			}
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
		return SKIP_BODY;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
