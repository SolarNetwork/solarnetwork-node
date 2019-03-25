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
import org.springframework.context.MessageSource;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicSetupResourceSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.setup.SetupResourceProvider;

/**
 * Simple implementation of {@link WebProxyConfiguration}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleWebProxyConfiguration implements WebProxyConfiguration, SettingSpecifierProvider {

	private final String uid;
	private String proxyPath;
	private String groupUID;
	private String proxyTargetUri;
	private MessageSource messageSource;
	private SetupResourceProvider settingResourceProvider;

	public SimpleWebProxyConfiguration() {
		super();
		this.uid = UUID.randomUUID().toString();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(3);

		results.add(new BasicTextFieldSettingSpecifier("proxyPath", ""));
		results.add(new BasicTextFieldSettingSpecifier("proxyTargetUri", ""));

		if ( settingResourceProvider != null ) {
			Map<String, Object> setupProps = Collections.singletonMap("config-id", getProxyPath());
			results.add(new BasicSetupResourceSettingSpecifier(settingResourceProvider, setupProps));
		}

		return results;
	}

	@Override
	public String getUID() {
		return uid;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	@Override
	public String getProxyTargetUri() {
		return proxyTargetUri;
	}

	public void setProxyTargetUri(String proxyTargetUri) {
		this.proxyTargetUri = proxyTargetUri;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.setup.web.proxy.config";
	}

	@Override
	public String getDisplayName() {
		return "Web Proxy Configuration";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public SetupResourceProvider getSettingResourceProvider() {
		return settingResourceProvider;
	}

	public void setSettingResourceProvider(SetupResourceProvider settingResourceProvider) {
		this.settingResourceProvider = settingResourceProvider;
	}

	@Override
	public String getProxyPath() {
		return proxyPath != null ? proxyPath : uid;
	}

	public void setProxyPath(String proxyPath) {
		this.proxyPath = proxyPath;
	}

}
