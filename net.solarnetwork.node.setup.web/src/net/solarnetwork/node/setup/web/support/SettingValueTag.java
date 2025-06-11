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
 */

package net.solarnetwork.node.setup.web.support;

import static org.apache.taglibs.standard.functions.Functions.escapeXml;
import java.io.IOException;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * Expose the current value of a setting.
 *
 * @author matt
 * @version 1.2
 */
public class SettingValueTag extends TagSupport {

	private static final long serialVersionUID = 3625222283623204656L;

	/** The settings service. */
	private SettingsService service;

	/** The setting specifier provider. */
	private SettingSpecifierProvider provider;

	/** The setting. */
	private SettingSpecifier setting;

	/** The escape XML toggle. */
	private boolean escapeXml = true;

	/**
	 * Default constructor.
	 */
	public SettingValueTag() {
		super();
	}

	@Override
	public int doEndTag() throws JspException {
		assert service != null;
		assert provider != null;
		assert setting != null;
		if ( service == null || provider == null || setting == null ) {
			return EVAL_PAGE;
		}
		Object val = service.getSettingValue(provider, setting);
		if ( val != null ) {
			try {
				pageContext.getOut().write(escapeXml ? escapeXml(val.toString()) : val.toString());
			} catch ( IOException e ) {
				throw new JspException(e);
			}
		}
		return EVAL_PAGE;
	}

	/**
	 * Set the settings service.
	 *
	 * @param service
	 *        the service to set
	 */
	public void setService(SettingsService service) {
		this.service = service;
	}

	/**
	 * Set the provider.
	 *
	 * @param provider
	 *        the provider to set
	 */
	public void setProvider(SettingSpecifierProvider provider) {
		this.provider = provider;
	}

	/**
	 * Set the setting.
	 *
	 * @param setting
	 *        the setting to set
	 */
	public void setSetting(SettingSpecifier setting) {
		this.setting = setting;
	}

	/**
	 * Get the XML escape mode.
	 *
	 * @return {@literal true} to escape XML characters
	 */
	public boolean isEscapeXml() {
		return escapeXml;
	}

	/**
	 * Set the XML escape mode.
	 *
	 * @param escapeXml
	 *        {@literal true} to escape XML characters
	 */
	public void setEscapeXml(boolean escapeXml) {
		this.escapeXml = escapeXml;
	}

}
