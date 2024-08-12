/* ==================================================================
 * ChainHttpRequestCustomizerService.java - 5/08/2024 5:58:39 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.http.req;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import net.solarnetwork.settings.GroupSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.web.service.HttpRequestCustomizerService;

/**
 * Node-specific extension of
 * {@link net.solarnetwork.web.service.support.ChainHttpRequestCustomizerService}.
 *
 * @author matt
 * @version 1.0
 */
public class ChainHttpRequestCustomizerService
		extends net.solarnetwork.web.service.support.ChainHttpRequestCustomizerService {

	/**
	 * Constructor.
	 *
	 * @param services
	 *        the services to include
	 */
	public ChainHttpRequestCustomizerService(List<HttpRequestCustomizerService> services) {
		super(services);
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		// TODO Auto-generated method stub
		List<SettingSpecifier> result = super.getSettingSpecifiers();

		for ( ListIterator<SettingSpecifier> itr = result.listIterator(); itr.hasNext(); ) {
			SettingSpecifier s = itr.next();
			if ( s instanceof GroupSettingSpecifier
					&& "serviceUids".equals(((GroupSettingSpecifier) s).getKey()) ) {
				// replace list of UIDs with service filter
				String[] uids = getServiceUids();
				List<String> uidsList = (uids != null ? Arrays.asList(uids) : Collections.emptyList());
				BasicGroupSettingSpecifier uidsGroup = SettingUtils.dynamicListSettingSpecifier(
						"serviceUids", uidsList, new SettingUtils.KeyedListCallback<String>() {

							@Override
							public Collection<SettingSpecifier> mapListSettingKey(String value,
									int index, String key) {
								return Collections.singletonList(new BasicTextFieldSettingSpecifier(key,
										null, false,
										"(objectClass=net.solarnetwork.web.service.HttpRequestCustomizerService)"));
							}
						});
				itr.set(uidsGroup);
			}
		}

		return result;
	}

}
