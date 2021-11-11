/* ==================================================================
 * SimpleWebProxyConfiguration.java - 25/03/2019 10:47:41 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.settings.support.BasicSetupResourceSettingSpecifier;
import net.solarnetwork.node.setup.SetupResourceProvider;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Simple implementation of {@link WebProxyConfiguration}.
 * 
 * @author matt
 * @version 2.0
 */
public class SimpleWebProxyConfiguration extends BaseIdentifiable
		implements WebProxyConfiguration, SettingSpecifierProvider {

	private String proxyPath;
	private String proxyTargetUri;
	private boolean contentLinksRewrite = true;
	private SetupResourceProvider settingResourceProvider;

	public SimpleWebProxyConfiguration() {
		super();
		setUid(UUID.randomUUID().toString());
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(3);

		results.add(new BasicTextFieldSettingSpecifier("proxyPath", ""));
		results.add(new BasicTextFieldSettingSpecifier("proxyTargetUri", ""));
		results.add(new BasicToggleSettingSpecifier("contentLinksRewrite", Boolean.TRUE));

		if ( settingResourceProvider != null ) {
			Map<String, Object> setupProps = Collections.singletonMap("config-id", getProxyPath());
			results.add(new BasicSetupResourceSettingSpecifier(settingResourceProvider, setupProps));
		}

		return results;
	}

	@Override
	public String getProxyTargetUri() {
		return proxyTargetUri;
	}

	public void setProxyTargetUri(String proxyTargetUri) {
		this.proxyTargetUri = proxyTargetUri;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.setup.web.proxy.config";
	}

	@Override
	public String getDisplayName() {
		return "Web Proxy Configuration";
	}

	public SetupResourceProvider getSettingResourceProvider() {
		return settingResourceProvider;
	}

	public void setSettingResourceProvider(SetupResourceProvider settingResourceProvider) {
		this.settingResourceProvider = settingResourceProvider;
	}

	@Override
	public String getProxyPath() {
		return proxyPath != null ? proxyPath : getUid();
	}

	public void setProxyPath(String proxyPath) {
		this.proxyPath = proxyPath;
	}

	@Override
	public boolean isContentLinksRewrite() {
		return contentLinksRewrite;
	}

	public void setContentLinksRewrite(boolean contentLinksRewrite) {
		this.contentLinksRewrite = contentLinksRewrite;
	}

}
