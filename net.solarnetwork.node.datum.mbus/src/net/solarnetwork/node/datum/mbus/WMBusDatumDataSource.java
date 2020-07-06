/* ==================================================================
 * WMBusDatumDataSource.java - 06/07/2020 13:09:29 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.mbus;

import java.util.List;
import net.solarnetwork.node.io.mbus.support.WMBusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

public class WMBusDatumDataSource extends WMBusDeviceDatumDataSourceSupport
		implements SettingSpecifierProvider {

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.mbus";
	}

	@Override
	public String getDisplayName() {
		return "Generic Wireless M-Bus Device";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();

		//results.add(0, new BasicTitleSettingSpecifier("sample", getSampleMessage(sample.copy()), true));

		results.addAll(getWMBusNetworkSettingSpecifiers());

		/*
		 * WMBusDatumDataSource defaults = new WMBusDatumDataSource();
		 * results.add(new BasicTextFieldSettingSpecifier("sourceId",
		 * defaults.sourceId)); results.add(new
		 * BasicTextFieldSettingSpecifier("sampleCacheMs",
		 * String.valueOf(defaults.sampleCacheMs))); results.add(new
		 * BasicTextFieldSettingSpecifier("maxReadWordCount",
		 * String.valueOf(defaults.maxReadWordCount)));
		 * 
		 * // drop-down menu for word order BasicMultiValueSettingSpecifier
		 * wordOrderSpec = new BasicMultiValueSettingSpecifier( "wordOrderKey",
		 * String.valueOf(defaults.getWordOrder().getKey())); Map<String,
		 * String> wordOrderTitles = new LinkedHashMap<String, String>(2); for (
		 * ModbusWordOrder e : ModbusWordOrder.values() ) {
		 * wordOrderTitles.put(String.valueOf(e.getKey()), e.toDisplayString());
		 * } wordOrderSpec.setValueTitles(wordOrderTitles);
		 * results.add(wordOrderSpec);
		 */

		return results;
	}

}
