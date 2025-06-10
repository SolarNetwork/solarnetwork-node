/* ==================================================================
 * ForwardedPathUrlTag.java - 25/06/2017 7:52:14 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import org.springframework.web.servlet.tags.UrlTag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;

/**
 * Extension of Spring's {@code UrlTag} to support automatically injecting a
 * reverse proxy forward path.
 * 
 * <p>
 * If the request has a HTTP {@literal X-Forwarded-Path} value, that will be set
 * as the prefix to the request {@code contextPath} onto the {@code context}
 * property of the tag automatically.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ForwardedPathUrlTag extends UrlTag {

	private static final long serialVersionUID = 5059647940462361128L;

	/**
	 * Default constructor.
	 */
	public ForwardedPathUrlTag() {
		super();
	}

	@Override
	public int doStartTagInternal() throws JspException {
		int result = super.doStartTagInternal();
		HttpServletRequest req = (HttpServletRequest) this.pageContext.getRequest();
		String forwardedPath = req.getHeader("X-Forwarded-Path");
		String contextPath = req.getContextPath();
		if ( forwardedPath != null && forwardedPath.startsWith("/") ) {
			StringBuilder buf = new StringBuilder(forwardedPath);
			if ( contextPath.length() > 0
					&& !(forwardedPath.endsWith("/") || contextPath.startsWith("/")) ) {
				buf.append('/');
			}
			buf.append(contextPath);
			contextPath = buf.toString();
		}

		// always set the context value; for tag caching
		setContext(contextPath);

		return result;
	}

}
