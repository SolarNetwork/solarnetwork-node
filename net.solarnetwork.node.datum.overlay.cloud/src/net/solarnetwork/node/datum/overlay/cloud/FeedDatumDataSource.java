/* ==================================================================
 * FeedDatumDataSource.java - 7/07/2022 2:10:18 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.overlay.cloud;

import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * {@link DatumDataSource} based on {@link FeedData}.
 * 
 * @author matt
 * @version 1.0
 */
public class FeedDatumDataSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

	private final ConfigurableOverlayCloudService service;
	private String sourceId;
	private Long gridId;
	private Long feedId;

	private FeedDataAccessor latestData;

	/**
	 * Constructor.
	 */
	public FeedDatumDataSource() {
		super();
		this.service = new RestTemplateOverlayCloudService();
		setDisplayName("Overlay Feed");
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return FeedDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		final Long gridId = getGridId();
		final Long feedId = getFeedId();
		final String sourceId = this.sourceId;
		if ( gridId == null || feedId == null || sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		FeedData data = service.getFeedLatest(gridId, feedId);
		if ( data != null ) {
			latestData = data;
		}
		Map<String, Object> sourceIdParams = new HashMap<>(2);
		sourceIdParams.put("gridId", gridId);
		sourceIdParams.put("feedId", feedId);
		return new FeedDatum(data, resolvePlaceholders(sourceId, sourceIdParams));
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.overlay.feed";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(latestData), true));
		results.addAll(getIdentifiableSettingSpecifiers());

		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier("gridId", null));
		results.add(new BasicTextFieldSettingSpecifier("feedId", null));

		results.addAll(service.getSettingSpecifiers("service."));

		return results;
	}

	private String getSampleMessage(FeedDataAccessor data) {
		if ( data == null || data.getDataTimestamp() == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(data.getActivePower());
		buf.append(", V = ").append(data.getVoltage());
		buf.append(", SOC = ").append(data.getAvailableEnergyPercentage());
		buf.append(", SOH = ").append(data.getStateOfHealthPercentage());
		buf.append("; sampled at ").append(formatForLocalDisplay(data.getDataTimestamp()));
		return buf.toString();
	}

	/**
	 * Get the grid ID.
	 * 
	 * @return the gridId
	 */
	public Long getGridId() {
		return gridId;
	}

	/**
	 * Set the grid ID to collect from.
	 * 
	 * @param gridId
	 *        the grid ID to set
	 */
	public void setGridId(Long gridId) {
		this.gridId = gridId;
	}

	/**
	 * Get the feed ID.
	 * 
	 * @return the feed ID
	 */
	public Long getFeedId() {
		return feedId;
	}

	/**
	 * Set the feed ID to collect from.
	 * 
	 * @param feedId
	 *        the ID to set
	 */
	public void setFeedId(Long feedId) {
		this.feedId = feedId;
	}

	/**
	 * Get the service.
	 * 
	 * @return the service
	 */
	public ConfigurableOverlayCloudService getService() {
		return service;
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param sourceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
}
