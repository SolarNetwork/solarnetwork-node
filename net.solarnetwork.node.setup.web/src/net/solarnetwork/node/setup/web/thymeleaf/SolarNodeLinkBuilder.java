/* ==================================================================
 * SolarNodeLinkBuilder.java - 18/06/2025 10:12:15 am
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

import java.util.Map;
import org.thymeleaf.context.Contexts;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import net.solarnetwork.node.setup.web.WebConstants;

/**
 * Extension of {@link StandardLinkBuilder} to support {@code X-Forwarded-Path}
 * rewriting.
 *
 * @author matt
 * @version 1.0
 */
public class SolarNodeLinkBuilder extends StandardLinkBuilder {

	/**
	 * Constructor.
	 */
	public SolarNodeLinkBuilder() {
		super();
	}

	@Override
	protected String computeContextPath(IExpressionContext context, String base,
			Map<String, Object> parameters) {
		String contextPath = super.computeContextPath(context, base, parameters);

		String forwardedPath = Contexts.getWebExchange(context).getRequest()
				.getHeaderValue(WebConstants.X_FORWARDED_PATH_HTTP_HEADER);
		if ( forwardedPath != null ) {
			if ( forwardedPath != null && forwardedPath.startsWith("/") ) {
				StringBuilder buf = new StringBuilder(forwardedPath);
				if ( contextPath.length() > 0
						&& !(forwardedPath.endsWith("/") || contextPath.startsWith("/")) ) {
					buf.append('/');
				}
				buf.append(contextPath);
				contextPath = buf.toString();
			}
		}
		return contextPath;
	}

}
