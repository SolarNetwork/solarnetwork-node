/* ==================================================================
 * SettingValueTag.java - Mar 12, 2012 8:48:11 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.setup.web.support;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingsService;

import org.apache.taglibs.standard.tag.common.core.OutSupport;

/**
 * Expose the current value of a setting.
 * 
 * @author matt
 * @version $Revision$
 */
public class SettingValueTag extends TagSupport {

	private static final long serialVersionUID = 3625222283623204656L;

	private SettingsService service;
	private SettingSpecifierProvider provider;
	private SettingSpecifier setting;
	private boolean escapeXml = true;

	@Override
	public int doEndTag() throws JspException {
		assert service != null;
		assert provider != null;
		assert setting != null;
		Object val = service.getSettingValue(provider, setting);
		if ( val != null ) {
			try {
				OutSupport.out(pageContext, escapeXml, val.toString());
			} catch (IOException e) {
				throw new JspException(e);
			}
		}
		return EVAL_PAGE;
	}

	public void setService(SettingsService service) {
		this.service = service;
	}

	public void setProvider(SettingSpecifierProvider provider) {
		this.provider = provider;
	}

	public void setSetting(SettingSpecifier setting) {
		this.setting = setting;
	}

	public boolean isEscapeXml() {
		return escapeXml;
	}

	public void setEscapeXml(boolean escapeXml) {
		this.escapeXml = escapeXml;
	}

}
